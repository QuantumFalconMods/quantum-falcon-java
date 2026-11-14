package com.matt.quantumfalcon.dungeon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.worldgen.structure.FrozenHourStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A10 Frozen Hours runtime. A site's then/now volumes, nest-bells and bell gate are defined in template coordinates in
 * data/quantumfalcon/frozen_hours/&lt;structure&gt;.json (written by tools/gen_eastwatch.py from the same layout as the
 * template) and registered the first time a player stands in the site. The site starts in its Now hour.
 * Bells (Eastwatch "Three Nest-Bells"): exist only in the Then palette; ringing all of them opens the gate, and the gate
 * stays open in both hours (their sound persists). Nothing here touches doors, keys or vaults.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class FrozenHours {
    private FrozenHours() {}
    public static final TagKey<Structure> FROZEN_HOURS = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "frozen_hours"));

    public record Cell(BlockPos rel, String then, String now) {}
    public record VolumeSpec(String id, BlockPos socket, ThenNow.Hour start, List<Cell> cells) {}
    /** {@code ward}: template-relative boxes the dungeon ward holds in (rooms, shafts, the hut); empty = the whole structure. */
    /** A fixed item frame, spawned after placement (template entities log "invalid position": their Tile pos is template-relative). */
    public record Frame(BlockPos rel, net.minecraft.core.Direction facing, ResourceLocation item, int dye) {}
    public record Spec(List<VolumeSpec> volumes, List<BlockPos> bells, List<BlockPos> gate, List<BoundingBox> ward, List<Frame> frames) {
        public Spec(List<VolumeSpec> volumes, List<BlockPos> bells, List<BlockPos> gate, List<BoundingBox> ward) { this(volumes, bells, gate, ward, List.of()); }
    }

    // read from worldgen threads (Piece.postProcess) as well as the server thread
    private static final Map<ResourceLocation, Spec> SPECS = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) { SPECS.clear(); }

    static BlockPos pos(JsonArray a) { return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()); }

    /** The spec for a structure id, or null if it has none. */
    public static Spec spec(ServerLevel level, ResourceLocation structure) {
        return SPECS.computeIfAbsent(structure, id -> {
            var res = level.getServer().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "frozen_hours/" + id.getPath() + ".json"));
            if (res.isEmpty()) return null;
            try (Reader r = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                List<VolumeSpec> vols = new ArrayList<>();
                for (JsonElement ve : o.getAsJsonArray("volumes")) {
                    JsonObject v = ve.getAsJsonObject();
                    List<Cell> cells = new ArrayList<>();
                    for (JsonElement ce : v.getAsJsonArray("cells")) {
                        JsonObject c = ce.getAsJsonObject();
                        BlockPos a = pos(c.getAsJsonArray("from")), b = c.has("to") ? pos(c.getAsJsonArray("to")) : a;
                        for (BlockPos p : BlockPos.betweenClosed(a, b)) cells.add(new Cell(p.immutable(), c.get("then").getAsString(), c.get("now").getAsString()));
                    }
                    vols.add(new VolumeSpec(v.get("id").getAsString(), pos(v.getAsJsonArray("socket")),
                        "then".equals(v.has("start") ? v.get("start").getAsString() : "now") ? ThenNow.Hour.THEN : ThenNow.Hour.NOW, cells));
                }
                List<BlockPos> bells = new ArrayList<>(), gate = new ArrayList<>();
                if (o.has("bells")) for (JsonElement b : o.getAsJsonArray("bells")) bells.add(pos(b.getAsJsonArray()));
                if (o.has("gate")) for (JsonElement g : o.getAsJsonArray("gate")) {
                    JsonObject c = g.getAsJsonObject();
                    for (BlockPos p : BlockPos.betweenClosed(pos(c.getAsJsonArray("from")), pos(c.getAsJsonArray("to")))) gate.add(p.immutable());
                }
                List<BoundingBox> ward = new ArrayList<>();
                if (o.has("ward")) for (JsonElement w : o.getAsJsonArray("ward")) {
                    JsonObject c = w.getAsJsonObject();
                    ward.add(BoundingBox.fromCorners(pos(c.getAsJsonArray("from")), pos(c.getAsJsonArray("to"))));
                }
                List<Frame> frames = new ArrayList<>();
                if (o.has("frames")) for (JsonElement fe : o.getAsJsonArray("frames")) {
                    JsonObject f = fe.getAsJsonObject();
                    frames.add(new Frame(pos(f.getAsJsonArray("pos")), net.minecraft.core.Direction.byName(f.get("facing").getAsString()),
                        ResourceLocation.parse(f.get("item").getAsString()), f.has("dye") ? f.get("dye").getAsInt() : -1));
                }
                return new Spec(vols, bells, gate, ward, frames);
            } catch (Exception ex) { QuantumFalcon.LOGGER.error("Bad frozen_hours spec for {}: {}", id, ex.toString()); return null; }
        });
    }

    /**
     * Blocker 1 + item frames, run for each chunk the template is placed in: no fluid is left inside the ward (a terrain
     * aquifer or a waterlogged authored block), and the spec's frames are hung in this chunk.
     */
    public static void afterPlace(net.minecraft.world.level.WorldGenLevel level, ResourceLocation structure, BlockPos origin, BoundingBox chunkBox) {
        Spec spec = spec(level.getLevel(), structure);
        if (spec == null) return;
        BlockState filler = state(level.getLevel(), "quantumfalcon:riftslate");
        for (BoundingBox w0 : spec.ward()) {
            BoundingBox w = w0.moved(origin.getX(), origin.getY(), origin.getZ());
            if (!w.intersects(chunkBox)) continue;
            BoundingBox i = new BoundingBox(Math.max(w.minX(), chunkBox.minX()), Math.max(w.minY(), chunkBox.minY()), Math.max(w.minZ(), chunkBox.minZ()),
                Math.min(w.maxX(), chunkBox.maxX()), Math.min(w.maxY(), chunkBox.maxY()), Math.min(w.maxZ(), chunkBox.maxZ()));
            for (BlockPos p : BlockPos.betweenClosed(i.minX(), i.minY(), i.minZ(), i.maxX(), i.maxY(), i.maxZ())) {
                var fs = level.getFluidState(p);
                if (fs.isEmpty()) continue;
                BlockState st = level.getBlockState(p);
                if (st.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))
                    level.setBlock(p, st.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, false), 2);
                else if (st.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) level.setBlock(p, filler, 2);
            }
        }
        for (Frame f : spec.frames()) {
            BlockPos p = origin.offset(f.rel());
            if (!chunkBox.isInside(p)) continue;
            var frame = new net.minecraft.world.entity.decoration.ItemFrame(level.getLevel(), p, f.facing());
            var item = new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(f.item()));
            if (f.dye() >= 0) item.set(net.minecraft.core.component.DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(f.dye(), true));
            frame.setItem(item, false);
            var ft = new net.minecraft.nbt.CompoundTag(); frame.saveWithoutId(ft); ft.putBoolean("Fixed", true); ft.putBoolean("Invulnerable", true); frame.load(ft);
            level.addFreshEntity(frame);
        }
    }

    static BlockState state(ServerLevel level, String s) {
        try { return BlockStateParser.parseForBlock(level.holderLookup(Registries.BLOCK), s, false).blockState(); }
        catch (Exception ex) { QuantumFalcon.LOGGER.warn("frozen_hours: bad block state '{}'", s); return Blocks.AIR.defaultBlockState(); }
    }

    /**
     * Whether pos (inside this Frozen Hour's structure) is in its warded volume: the rooms, shafts and hut listed in the spec's
     * {@code ward}, so the trees, stone and soil around a surface hut stay ordinary ground. No spec or no list = all of it.
     */
    public static boolean inWard(ServerLevel level, StructureStart start, BlockPos pos) {
        Spec spec = spec(level, structureId(level, start));
        if (spec == null || spec.ward().isEmpty()) return true;
        BlockPos o = origin(start);
        if (o == null) return true;
        BlockPos rel = pos.subtract(o);
        for (BoundingBox b : spec.ward()) if (b.isInside(rel)) return true;
        return false;
    }

    /** the ward boxes in world coordinates, or null when the whole piece is warded. */
    public static java.util.List<BoundingBox> wardBoxes(ServerLevel level, StructureStart start) {
        Spec spec = spec(level, structureId(level, start));
        BlockPos o = origin(start);
        if (spec == null || spec.ward().isEmpty() || o == null) return null;
        java.util.List<BoundingBox> out = new java.util.ArrayList<>();
        for (BoundingBox b : spec.ward()) out.add(b.moved(o.getX(), o.getY(), o.getZ()));
        return out;
    }

    public static StructureStart startAt(ServerLevel level, BlockPos pos) {
        StructureStart s = level.structureManager().getStructureWithPieceAt(pos, FROZEN_HOURS);
        return s.isValid() ? s : null;
    }

    public static BlockPos origin(StructureStart start) {
        for (StructurePiece p : start.getPieces()) if (p instanceof FrozenHourStructure.Piece fp) return fp.origin();
        return null;
    }

    public static ResourceLocation structureId(ServerLevel level, StructureStart s) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(s.getStructure());
    }

    /** Registers a site's volumes (once). Returns the number of volumes registered now, or -1 if the site has no spec. */
    public static int initSite(ServerLevel level, StructureStart start) {
        String site = DungeonSites.key(level, start);
        ThenNow tn = ThenNow.get(level);
        if (tn.sites.contains(site)) return 0;
        Spec spec = spec(level, structureId(level, start));
        BlockPos o = origin(start);
        if (spec == null || o == null) return -1;
        int n = 0;
        for (VolumeSpec v : spec.volumes()) {
            Map<BlockPos, BlockState[]> pal = new LinkedHashMap<>();
            int[] mn = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, mx = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            for (Cell c : v.cells()) {
                BlockPos p = o.offset(c.rel());
                pal.put(p, new BlockState[]{state(level, c.then()), state(level, c.now())});
                mn[0] = Math.min(mn[0], p.getX()); mn[1] = Math.min(mn[1], p.getY()); mn[2] = Math.min(mn[2], p.getZ());
                mx[0] = Math.max(mx[0], p.getX()); mx[1] = Math.max(mx[1], p.getY()); mx[2] = Math.max(mx[2], p.getZ());
            }
            BoundingBox box = new BoundingBox(mn[0], mn[1], mn[2], mx[0], mx[1], mx[2]);
            String why = tn.register(level, site + "#" + v.id(), box, o.offset(v.socket()), pal, v.start());
            if (why != null) QuantumFalcon.LOGGER.warn("frozen_hours {} volume {} refused: {}", site, v.id(), why); else n++;
        }
        tn.sites.add(site);
        tn.setDirty();
        return n;
    }

    /**
     * Admin/dev: places a Frozen Hour structure with its template origin at {@code origin} (no terrain checks), records the
     * start and references so the ward, keys and vault see it, and registers its volumes. Returns the start or null.
     */
    public static StructureStart placeAt(ServerLevel level, ResourceLocation structureId, BlockPos origin) {
        var reg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Structure st = reg.get(structureId);
        if (!(st instanceof FrozenHourStructure fh)) return null;
        var piece = new FrozenHourStructure.Piece(level.getStructureManager(), fh.templateId(), origin);
        net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(origin);
        StructureStart start = new StructureStart(st, cp, 0, new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(List.of(piece)));
        BoundingBox bb = start.getBoundingBox();
        var gen = level.getChunkSource().getGenerator();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
            var c = level.getChunk(cx, cz);
            var p = new net.minecraft.world.level.ChunkPos(cx, cz);
            start.placeInChunk(level, level.structureManager(), gen, level.getRandom(),
                new BoundingBox(p.getMinBlockX(), level.getMinBuildHeight(), p.getMinBlockZ(), p.getMaxBlockX(), level.getMaxBuildHeight(), p.getMaxBlockZ()), p);
            c.addReferenceForStructure(st, cp.toLong());
            c.setUnsaved(true);
        }
        level.getChunk(cp.x, cp.z).setStartForStructure(st, start);
        ThenNow.get(level).forgetSite(DungeonSites.key(level, start));
        initSite(level, start);
        return start;
    }

    /** Admin/dev: flips the named volume of the Frozen Hour site at pos straight away (no cooldown). */
    public static String flipAt(ServerLevel level, BlockPos pos, String volume) {
        StructureStart start = startAt(level, pos);
        if (start == null) return "not in a Frozen Hour site";
        initSite(level, start);
        ThenNow tn = ThenNow.get(level);
        ThenNow.Volume v = tn.volume(DungeonSites.key(level, start) + "#" + volume);
        if (v == null) return "no volume " + volume;
        v.lastFlip = -100000;
        String why = tn.flip(level, v);
        return why != null ? why : volume + " -> " + v.hour;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.tickCount % 20 != 0) return;
        ServerLevel level = sp.serverLevel();
        StructureStart start = startAt(level, sp.blockPosition());
        if (start != null) initSite(level, start);
    }

    /** Rings a nest-bell for the site. Returns how many of the site's bells are now rung, or -1 if pos is not one. */
    public static int ring(ServerLevel level, BlockPos pos, ServerPlayer who) {
        StructureStart start = startAt(level, pos);
        if (start == null) return -1;
        Spec spec = spec(level, structureId(level, start));
        BlockPos o = origin(start);
        if (spec == null || o == null || !spec.bells().contains(pos.subtract(o))) return -1;
        String site = DungeonSites.key(level, start);
        ThenNow tn = ThenNow.get(level);
        Set<Long> rung = tn.bells.computeIfAbsent(site, k -> new HashSet<>());
        boolean fresh = rung.add(pos.asLong());
        tn.setDirty();
        int n = rung.size(), need = spec.bells().size();
        if (fresh && who != null) who.displayClientMessage(Component.literal(n < need ? "The bell's note hangs in the air, and stays. (" + n + "/" + need + ")"
            : "Three notes, held across the hours. Somewhere below, a gate gives way.").withStyle(ChatFormatting.GOLD), true);
        if (n >= need && tn.gatesOpen.add(site)) {
            for (BlockPos g : spec.gate()) {
                BlockPos p = o.offset(g);
                if (!ThenNow.protectedCell(level.getBlockState(p))) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            level.playSound(null, o.offset(spec.gate().isEmpty() ? BlockPos.ZERO : spec.gate().get(0)), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.5f, 0.6f);
            tn.setDirty();
        }
        return n;
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getEntity() instanceof ServerPlayer sp)) return;
        if (level.getBlockState(e.getPos()).getBlock() instanceof BellBlock) ring(level, e.getPos(), sp);
    }

    /** a nest-bell rung the vanilla ways counts too: an arrow or trident (or any projectile)... */
    @SubscribeEvent
    public static void onProjectile(net.neoforged.neoforge.event.entity.ProjectileImpactEvent e) {
        if (!(e.getProjectile().level() instanceof ServerLevel level)) return;
        if (!(e.getRayTraceResult() instanceof net.minecraft.world.phys.BlockHitResult bh) || bh.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        if (level.getBlockState(bh.getBlockPos()).getBlock() instanceof BellBlock)
            ring(level, bh.getBlockPos(), e.getProjectile().getOwner() instanceof ServerPlayer sp ? sp : null);
    }

    /** ...and redstone: a powered neighbour (lever, button, plate, wire) rings it. */
    @SubscribeEvent
    public static void onNeighbour(net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        for (var d : e.getNotifiedSides()) {
            BlockPos b = e.getPos().relative(d);
            if (level.getBlockState(b).getBlock() instanceof BellBlock && level.hasNeighborSignal(b)) {
                ServerPlayer near = level.getNearestEntity(ServerPlayer.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat(), null, b.getX(), b.getY(), b.getZ(), new net.minecraft.world.phys.AABB(b).inflate(16));
                ring(level, b, near);
            }
        }
    }
}
