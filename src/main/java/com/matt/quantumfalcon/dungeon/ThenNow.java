package com.matt.quantumfalcon.dungeon;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * A10 Frozen Hours: time-shift ("then/now") volumes. A volume is a marked box inside a dungeon with two palettes, the
 * golden-age Then and the ruined Now; only the cells that differ are stored. Flipping restates those cells in the other
 * hour. Rules (IMPLEMENTATION_NOTES "then_now"):
 * <ul>
 * <li>DungeonWard owns both palettes: volumes must lie inside a dungeon, so a Now gap is warded air (no pillaring) and a
 *     Then bridge is a dungeon block, not a player block.</li>
 * <li>A flip never touches a Sealed Vault Door, a treasury vault, any door, a checkpoint or a Return Rift: such cells are
 *     refused when a palette is registered and skipped if one turns up later.</li>
 * <li>The hour and palettes are saved with the world (no second way to lose a room on restart).</li>
 * </ul>
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class ThenNow extends SavedData {
    private static final String NAME = "quantumfalcon_then_now";
    public static final int FLIP_COOLDOWN = 60;
    public static final int SOCKET_REACH = 4;

    public enum Hour { THEN, NOW; Hour other() { return this == THEN ? NOW : THEN; } }

    public static final class Volume {
        public final String id; public final ResourceKey<Level> dim; public final BoundingBox box; public final BlockPos socket;
        public Hour hour = Hour.THEN;
        public long lastFlip = -100000;
        /** cell -> {then, now} */
        public final Map<BlockPos, BlockState[]> cells = new LinkedHashMap<>();
        Volume(String id, ResourceKey<Level> dim, BoundingBox box, BlockPos socket) { this.id = id; this.dim = dim; this.box = box; this.socket = socket; }
        public BlockState stateIn(BlockPos p, Hour h) { BlockState[] s = cells.get(p); return s == null ? null : s[h.ordinal()]; }
    }

    private final Map<String, Volume> volumes = new LinkedHashMap<>();
    /** Frozen Hour sites whose volumes are registered; bells rung per site; gates opened per site. */
    public final Set<String> sites = new HashSet<>(), gatesOpen = new HashSet<>();
    public final Map<String, Set<Long>> bells = new HashMap<>();
    /** players who logged out inside a volume (where their feet were). Their footing counts as occupied until they return. */
    public record Parked(String name, ResourceKey<Level> dim, BlockPos feet) {}
    public final Map<UUID, Parked> parked = new HashMap<>();

    @net.neoforged.bus.api.SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) park(sp);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) unpark(sp);
    }

    /** Remembers a leaving player's footing if they stand in a then/now volume. Public for tests. */
    public static boolean park(net.minecraft.server.level.ServerPlayer sp) {
        ThenNow t = get(sp.serverLevel());
        for (Volume v : t.volumes.values())
            if (v.dim == sp.level().dimension() && v.box.inflatedBy(1).isInside(sp.blockPosition())) {
                t.parked.put(sp.getUUID(), new Parked(sp.getGameProfile().getName(), v.dim, sp.blockPosition()));
                t.setDirty();
                return true;
            }
        return false;
    }

    public static void unpark(net.minecraft.server.level.ServerPlayer sp) {
        ThenNow t = get(sp.serverLevel());
        if (t.parked.remove(sp.getUUID()) != null) t.setDirty();
    }

    /** Would hour {@code h} take away the cell under {@code feet} (or the ledge right under a jump)? */
    static boolean footingVanishes(ServerLevel level, Volume v, Hour h, BlockPos feet) {
        for (int dy = 1; dy <= 3; dy++) {
            BlockState[] c = v.cells.get(feet.below(dy));
            if (c == null) continue;
            return !c[v.hour.ordinal()].isAir() && c[h.ordinal()].getCollisionShape(level, feet.below(dy)).isEmpty();
        }
        return false;
    }

    public static ThenNow get(ServerLevel any) {
        return any.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ThenNow::new, ThenNow::load, null), NAME);
    }

    public Volume volume(String id) { return volumes.get(id); }
    public Collection<Volume> all() { return volumes.values(); }

    /** Cells a flip must never change. */
    public static boolean protectedCell(BlockState st) {
        Block b = st.getBlock();
        return b instanceof DoorBlock || b instanceof TreasuryVaultBlock || b instanceof DungeonCheckpointBlock || b instanceof ReturnRiftBlock
            || b instanceof DungeonKeyDoorBlock;
    }

    /**
     * Registers (or replaces) a volume. Returns null on success, or why it was refused: outside a dungeon, a protected
     * cell (door, vault, checkpoint, return rift) in either palette or currently in the world, or a cell outside the box.
     * The world is set to the Then palette.
     */
    public String register(ServerLevel level, String id, BoundingBox box, BlockPos socket, Map<BlockPos, BlockState[]> palette) {
        return register(level, id, box, socket, palette, Hour.THEN);
    }

    /** As above, starting in the given hour (a Frozen Hour dungeon is found in its Now). */
    public String register(ServerLevel level, String id, BoundingBox box, BlockPos socket, Map<BlockPos, BlockState[]> palette, Hour start) {
        if (!DungeonWard.inDungeon(level, box.getCenter())) return "volume centre is not inside a dungeon";
        for (var e : palette.entrySet()) {
            BlockPos p = e.getKey();
            if (!box.isInside(p)) return "cell " + p.toShortString() + " outside the box";
            if (!DungeonWard.inDungeon(level, p)) return "cell " + p.toShortString() + " outside the dungeon";
            if (protectedCell(level.getBlockState(p)) || protectedCell(e.getValue()[0]) || protectedCell(e.getValue()[1]))
                return "cell " + p.toShortString() + " is a door, vault, checkpoint or return rift";
        }
        Volume v = new Volume(id, level.dimension(), box, socket.immutable());
        palette.forEach((p, s) -> v.cells.put(p.immutable(), new BlockState[]{s[0], s[1]}));
        volumes.put(id, v);
        apply(level, v, start);
        setDirty();
        return null;
    }

    public void remove(String id) { if (volumes.remove(id) != null) setDirty(); }
    /** Drops a site and all its volumes (tests). */
    public void forgetSite(String site) {
        volumes.keySet().removeIf(k -> k.startsWith(site + "#")); sites.remove(site); gatesOpen.remove(site); bells.remove(site); setDirty();
    }

    /** The volume whose socket is at pos, or whose box contains pos. */
    public Volume at(ServerLevel level, BlockPos pos) {
        for (Volume v : volumes.values()) if (v.dim == level.dimension() && v.socket.equals(pos)) return v;
        for (Volume v : volumes.values()) if (v.dim == level.dimension() && v.box.isInside(pos)) return v;
        // a socket within reach of a volume (Eastwatch's far-side socket on the flight-run)
        for (Volume v : volumes.values()) if (v.dim == level.dimension() && v.box.inflatedBy(SOCKET_REACH).isInside(pos)) return v;
        return null;
    }

    /** Flips a volume to the other hour. Returns null on success or why not (cooldown). */
    public String flip(ServerLevel level, Volume v) { return flip(level, v, null); }

    /**
     * in co-op the hour won't turn the floor out from under someone else: a flip that would remove a cell another
     * player stands on (or is about to land on) is refused with a message to both.
     */
    public String flip(ServerLevel level, Volume v, @javax.annotation.Nullable net.minecraft.world.entity.player.Player who) {
        long now = level.getGameTime();
        if (now - v.lastFlip < FLIP_COOLDOWN) return "The hour is still settling.";
        net.minecraft.world.entity.player.Player under = standingOnVanishing(level, v, v.hour.other(), who);
        if (under != null) {
            under.displayClientMessage(net.minecraft.network.chat.Component.literal("Someone is trying to turn the hour under your feet.")
                .withStyle(net.minecraft.ChatFormatting.GOLD), true);
            return under.getName().getString() + " is standing on what this hour would take away. Wait for them.";
        }
        // someone who logged out on a ledge this hour would remove keeps it until they come back
        for (var e : parked.entrySet()) {
            Parked pk = e.getValue();
            if (pk.dim() != level.dimension() || (who != null && e.getKey().equals(who.getUUID()))) continue;
            if (v.box.inflatedBy(1).isInside(pk.feet()) && footingVanishes(level, v, v.hour.other(), pk.feet()))
                return pk.name() + " logged out standing on what this hour would take away. The hour waits for them.";
        }
        v.lastFlip = now;
        apply(level, v, v.hour.other());
        HourSocketBlock.show(level, v.socket, v.hour);
        level.playSound(null, v.socket, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, v.hour == Hour.THEN ? 1.3f : 0.7f);
        setDirty();
        return null;
    }

    /** Another (non-spectator) player whose footing would vanish in hour {@code h}: a cell under them, solid now, empty then. */
    static net.minecraft.world.entity.player.Player standingOnVanishing(ServerLevel level, Volume v, Hour h, @javax.annotation.Nullable net.minecraft.world.entity.player.Player who) {
        var area = new net.minecraft.world.phys.AABB(v.box.minX(), v.box.minY(), v.box.minZ(), v.box.maxX() + 1, v.box.maxY() + 3, v.box.maxZ() + 1);
        java.util.List<net.minecraft.world.entity.player.Player> ps = new java.util.ArrayList<>(level.players());
        ps.addAll(TEST_PLAYERS);
        for (var p : ps) {
            if (p.level() != level || !area.contains(p.position())) continue;
            if (p == who || p.isSpectator() || (who != null && p.getUUID().equals(who.getUUID()))) continue;
            if (footingVanishes(level, v, h, p.blockPosition())) return p;   // what they stand on, or the ledge right under a jump
        }
        return null;
    }

    /** Test hook: fake players (not in level.players()) that count as standing in the world. */
    public static final java.util.List<net.minecraft.world.entity.player.Player> TEST_PLAYERS = new java.util.ArrayList<>();

    /** counters of the last apply (tests). */
    public static int lastPopped, lastPushed;

    void apply(ServerLevel level, Volume v, Hour h) {
        int changed = 0;
        java.util.List<BlockPos> touched = new java.util.ArrayList<>();
        for (var e : v.cells.entrySet()) {
            BlockPos p = e.getKey();
            BlockState cur = level.getBlockState(p);
            if (protectedCell(cur)) continue;   // a vault or door that turned up later is never touched
            BlockState want = e.getValue()[h.ordinal()];
            if (cur != want) {
                level.setBlock(p, want, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                touched.add(p);
                changed++;
                if (changed % 3 == 0) level.sendParticles(h == Hour.THEN ? ParticleTypes.WAX_ON : ParticleTypes.REVERSE_PORTAL,
                    p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
            }
        }
        v.hour = h;
        // anything a player hung on a cell that just went (torch, lantern, ladder) falls off and drops, like vanilla
        int popped = 0;
        for (BlockPos p : touched) for (var d : net.minecraft.core.Direction.values()) {
            BlockPos n = p.relative(d);
            if (v.cells.containsKey(n)) continue;
            BlockState ns = level.getBlockState(n);
            if (ns.isAir() || protectedCell(ns) || ns.canSurvive(level, n)) continue;
            level.destroyBlock(n, true);
            popped++;
        }
        // a cell that turned solid around someone pushes them up onto it instead of leaving them inside
        int pushed = 0;
        var area = new net.minecraft.world.phys.AABB(v.box.minX(), v.box.minY(), v.box.minZ(), v.box.maxX() + 1, v.box.maxY() + 1, v.box.maxZ() + 1);
        for (var en : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area, en -> !en.isSpectator() && !en.noPhysics && !(en instanceof net.minecraft.world.entity.decoration.HangingEntity))) {
            BlockPos feet = en.blockPosition();
            if (!v.cells.containsKey(feet) && !v.cells.containsKey(feet.above())) continue;
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) continue;
            BlockPos up = feet;
            for (int i = 0; i < 8; i++) {
                up = up.above();
                if (level.getBlockState(up).getCollisionShape(level, up).isEmpty() && level.getBlockState(up.above()).getCollisionShape(level, up.above()).isEmpty()) break;
            }
            en.teleportTo(en.getX(), up.getY(), en.getZ());
            pushed++;
        }
        lastPopped = popped; lastPushed = pushed;
    }

    public static ThenNow load(CompoundTag tag, HolderLookup.Provider reg) {
        ThenNow t = new ThenNow();
        var blocks = reg.lookupOrThrow(Registries.BLOCK);
        for (Tag raw : tag.getList("Volumes", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) raw;
            try {
                int[] b = c.getIntArray("Box");
                Volume v = new Volume(c.getString("Id"), ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("Dim"))),
                    new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]), NbtUtils.readBlockPos(c, "Socket").orElse(BlockPos.ZERO));
                v.hour = Hour.valueOf(c.getString("Hour"));
                for (Tag ct : c.getList("Cells", Tag.TAG_COMPOUND)) {
                    CompoundTag cc = (CompoundTag) ct;
                    v.cells.put(BlockPos.of(cc.getLong("P")), new BlockState[]{NbtUtils.readBlockState(blocks, cc.getCompound("T")), NbtUtils.readBlockState(blocks, cc.getCompound("N"))});
                }
                t.volumes.put(v.id, v);
            } catch (RuntimeException ex) { QuantumFalcon.LOGGER.warn("Skipping a then/now volume: {}", ex.toString()); }
        }
        for (Tag x : tag.getList("Sites", Tag.TAG_STRING)) t.sites.add(x.getAsString());
        for (Tag x : tag.getList("Gates", Tag.TAG_STRING)) t.gatesOpen.add(x.getAsString());
        CompoundTag b = tag.getCompound("Bells");
        for (String k : b.getAllKeys()) { Set<Long> set = new HashSet<>(); for (long l : b.getLongArray(k)) set.add(l); t.bells.put(k, set); }
        for (Tag x : tag.getList("Parked", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            try { t.parked.put(c.getUUID("U"), new Parked(c.getString("N"), ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("D"))), BlockPos.of(c.getLong("P")))); }
            catch (RuntimeException ignored) {}
        }
        return t;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        ListTag list = new ListTag();
        for (Volume v : volumes.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", v.id); c.putString("Dim", v.dim.location().toString());
            c.putIntArray("Box", new int[]{v.box.minX(), v.box.minY(), v.box.minZ(), v.box.maxX(), v.box.maxY(), v.box.maxZ()});
            c.put("Socket", NbtUtils.writeBlockPos(v.socket));
            c.putString("Hour", v.hour.name());
            ListTag cells = new ListTag();
            v.cells.forEach((p, s) -> {
                CompoundTag cc = new CompoundTag();
                cc.putLong("P", p.asLong()); cc.put("T", NbtUtils.writeBlockState(s[0])); cc.put("N", NbtUtils.writeBlockState(s[1]));
                cells.add(cc);
            });
            c.put("Cells", cells);
            list.add(c);
        }
        tag.put("Volumes", list);
        ListTag st = new ListTag(); sites.forEach(x -> st.add(net.minecraft.nbt.StringTag.valueOf(x))); tag.put("Sites", st);
        ListTag gt = new ListTag(); gatesOpen.forEach(x -> gt.add(net.minecraft.nbt.StringTag.valueOf(x))); tag.put("Gates", gt);
        CompoundTag bt = new CompoundTag(); bells.forEach((k, v) -> bt.putLongArray(k, v.stream().mapToLong(Long::longValue).toArray())); tag.put("Bells", bt);
        ListTag pk = new ListTag();
        parked.forEach((u, p) -> { CompoundTag c = new CompoundTag(); c.putUUID("U", u); c.putString("N", p.name()); c.putString("D", p.dim().location().toString()); c.putLong("P", p.feet().asLong()); pk.add(c); });
        tag.put("Parked", pk);
        return tag;
    }

    /** Air helper for palettes. */
    public static BlockState air() { return Blocks.AIR.defaultBlockState(); }
}
