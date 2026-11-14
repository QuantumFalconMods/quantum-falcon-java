package com.matt.quantumfalcon.dungeon;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A7: deep dungeons are crawls, not dig sites. Inside any structure in {@code #quantumfalcon:dungeons} (the protected volume is
 * the union of the structure's piece boxes), survival players cannot:
 * <ul>
 * <li>break walls, floors or ceilings (only {@code #quantumfalcon:dungeon_breakable}: crumbling secret walls, cobwebs, growth,
 *     spawners, chests, torches);</li>
 * <li>place blocks to bridge or pillar past traps (only {@code #quantumfalcon:dungeon_placeable}: torches and lanterns),
 *     or empty buckets;</li>
 * <li>pearl, chorus-fruit or portal out of (or into) the dungeon.</li>
 * </ul>
 * Explosions, pistons, mob griefing and fluid-made blocks can't change dungeon blocks for anyone. Creative players and
 * operators (permission 2+) are exempt from the player rules. Doors, keys, levers and the return rift are the way forward.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class DungeonWard {
    private DungeonWard() {}

    public static final TagKey<Block> BREAKABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "dungeon_breakable"));
    public static final TagKey<Block> PLACEABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "dungeon_placeable"));

    /** True when {@code pos} lies inside a piece of a protected dungeon. */
    public static boolean inDungeon(LevelAccessor level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!sl.isLoaded(pos)) return false;
        var start = sl.structureManager().getStructureWithPieceAt(pos, ReturnRiftBlock.DUNGEONS);
        if (!start.isValid()) return false;
        // a Frozen Hour's single piece spans its surface hut and the ground around it: only its rooms, shafts and hut are warded
        if (start.getStructure() instanceof com.matt.quantumfalcon.worldgen.structure.FrozenHourStructure) return FrozenHours.inWard(sl, start, pos);
        return true;
    }

    public static boolean inDungeon(LevelAccessor level, Vec3 v) { return inDungeon(level, BlockPos.containing(v)); }

    /** Creative and operators walk through the ward. */
    public static boolean exempt(Entity e) {
        return e instanceof Player p && (p.isCreative() || p.isSpectator() || p.hasPermissions(2));
    }

    // ------------------------------------------------------------------ feedback
    private static final Map<UUID, Long> LAST_MSG = new HashMap<>();
    static final String[] WHY = {
        "The dungeon's stonework is warded. Only crumbling walls give way.",
        "The ward rejects your blocks: no bridging or pillaring in a dungeon.",
        "The ward swallows the fluid.",
        "The ward folds space back on itself: no pearls, chorus or portals here.",
        "The ward stills your wings: no gliding in a dungeon.",
        "The ward swallows the gust.",
    };

    static void tell(Entity e, int why) {
        if (!(e instanceof ServerPlayer p)) return;
        long now = p.level().getGameTime();
        Long last = LAST_MSG.get(p.getUUID());
        if (last != null && now - last < 20) return;   // at most once a second: holding the mouse doesn't spam
        LAST_MSG.put(p.getUUID(), now);
        p.displayClientMessage(Component.literal(WHY[why]).withStyle(ChatFormatting.DARK_AQUA), true);
    }

    // ------------------------------------------------------------------ breaking
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!com.matt.quantumfalcon.WorldConfig.wardProtectsBlocks()) return;   // config: ward.protectBlocks
        if (exempt(e.getPlayer()) || e.getState().is(BREAKABLE) || !inDungeon(e.getLevel(), e.getPos())) return;
        e.setCanceled(true);
        tell(e.getPlayer(), 0);
    }

    /** Warded blocks don't even crack: the player sees at once that mining is pointless. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        var pos = e.getPosition();
        if (pos.isEmpty() || exempt(e.getEntity()) || e.getState().is(BREAKABLE)) return;
        if (e.getEntity().level().isClientSide) {   // no ghost cracks; the client knows the nearby ward boxes (WardSync)
            if (WardSync.Client.inside(e.getEntity().level().dimension().location().toString(), pos.get())) e.setNewSpeed(0f);
            return;
        }
        if (!inDungeon(e.getEntity().level(), pos.get())) return;
        e.setNewSpeed(0f);
        tell(e.getEntity(), 0);
    }

    /**
     * a small fireball (dispensed fire charge, a blaze outside) that hits warded stone doesn't light it: the ward
     * swallows it. Hits on mobs and players still burn them; big (ghast) fireballs are covered by the explosion guard.
     */
    @SubscribeEvent
    public static void onImpact(net.neoforged.neoforge.event.entity.ProjectileImpactEvent e) {
        if (!(e.getProjectile() instanceof net.minecraft.world.entity.projectile.SmallFireball fb) || fb.level().isClientSide) return;
        if (!(e.getRayTraceResult() instanceof net.minecraft.world.phys.BlockHitResult bh) || bh.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        if (fb.getOwner() != null && exempt(fb.getOwner())) return;
        BlockPos hit = bh.getBlockPos(), fire = hit.relative(bh.getDirection());
        if (!inDungeon(fb.level(), hit) && !inDungeon(fb.level(), fire)) return;
        e.setCanceled(true);
        fb.discard();
    }

    /** Elytra: no gliding over a dungeon's chasms (Eastwatch's flight-run is crossed in Then, not on wings). */
    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer sp) groundGlider(sp);
    }
    /** Stops a non-exempt player gliding inside a dungeon. True if it did. */
    public static boolean groundGlider(Player p) {
        if (!p.isFallFlying() || exempt(p) || p.level().isClientSide || !inDungeon(p.level(), p.blockPosition())) return false;
        p.stopFallFlying();
        tell(p, 4);
        return true;
    }

    /** Wind charges: a player's own gust doesn't launch players inside a dungeon (no wind-jumping across the chasms). */
    public static boolean windShielded(Level level, Entity affected, Entity source) {
        return source instanceof net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge && affected instanceof Player p && !exempt(p)
            && (inDungeon(level, p.blockPosition()) || inDungeon(level, source.blockPosition()));
    }

    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate e) {
        if (!com.matt.quantumfalcon.WorldConfig.wardProtectsBlocks()) return;   // config: ward.protectBlocks
        Level level = e.getLevel();
        if (level.isClientSide) return;
        Entity src = e.getExplosion().getDirectSourceEntity();
        if (src != null) e.getAffectedEntities().removeIf(en -> { boolean w = windShielded(level, en, src); if (w) tell(en, 5); return w; });
        e.getAffectedBlocks().removeIf(p -> !level.getBlockState(p).is(BREAKABLE) && inDungeon(level, p));
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre e) {
        if (!(e.getLevel() instanceof ServerLevel sl)) return;
        BlockPos base = e.getPos(), head = base.relative(e.getDirection());
        if (inDungeon(sl, base) || inDungeon(sl, head)) { e.setCanceled(true); return; }
        var helper = e.getStructureHelper();
        if (helper != null && helper.resolve()) {
            for (BlockPos p : helper.getToPush()) if (inDungeon(sl, p) || inDungeon(sl, p.relative(helper.getPushDirection()))) { e.setCanceled(true); return; }
            for (BlockPos p : helper.getToDestroy()) if (inDungeon(sl, p)) { e.setCanceled(true); return; }
        }
    }

    @SubscribeEvent
    public static void onMobGrief(EntityMobGriefingEvent e) {
        Entity m = e.getEntity();
        if (m instanceof Player || m.level().isClientSide) return;
        if (inDungeon(m.level(), m.blockPosition())) e.setCanGrief(false);
    }

    @SubscribeEvent
    public static void onMobDestroy(LivingDestroyBlockEvent e) {
        if (!com.matt.quantumfalcon.WorldConfig.wardProtectsBlocks()) return;   // config: ward.protectBlocks
        if (exempt(e.getEntity()) || e.getState().is(BREAKABLE)) return;
        if (inDungeon(e.getEntity().level(), e.getPos())) e.setCanceled(true);
    }

    /** Lava meeting water (cobble/obsidian/basalt bridges) can't build inside a dungeon. */
    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent e) {
        if (inDungeon(e.getLevel(), e.getPos()) || inDungeon(e.getLevel(), e.getLiquidPos())) e.setNewState(e.getOriginalState());
    }

    /** fluid from outside a dungeon can't flow into it, so no generator can form inside. */
    public static boolean blocksFluidInto(LevelAccessor level, BlockPos to, net.minecraft.core.Direction dir) {
        if (!(level instanceof ServerLevel)) return false;
        return inDungeon(level, to) && !inDungeon(level, to.relative(dir.getOpposite()));
    }

    /**
     * sand, gravel, concrete powder or anvils can't land as blocks in a dungeon: a falling block inside, or about
     * to fall into one, drops as an item instead.
     */
    @SubscribeEvent
    public static void onFallingTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre e) {
        if (!(e.getEntity() instanceof net.minecraft.world.entity.item.FallingBlockEntity f) || !(f.level() instanceof ServerLevel sl)) return;
        BlockPos p = f.blockPosition();
        if (!inDungeon(sl, p) && !inDungeon(sl, p.below()) && !inDungeon(sl, p.below(2))) return;
        e.setCanceled(true);
        if (sl.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS)) f.spawnAtLocation(f.getBlockState().getBlock());
        f.discard();
    }

    @SubscribeEvent
    public static void onServerAboutToStart(net.neoforged.neoforge.event.server.ServerAboutToStartEvent e) {
        DungeonDispenseGuard.install();
    }

    // ------------------------------------------------------------------ portal arrival
    private static final int PORTAL_CLEARANCE = 20;   // the portal search and frame reach ~16 blocks around the target

    /**
     * Where a portal arriving at {@code pos} may be built: null if nothing within {@value #PORTAL_CLEARANCE} blocks
     * (horizontally, any height) belongs to a dungeon; otherwise the nearest point that far outside every such dungeon.
     */
    public static BlockPos portalSiteOutside(ServerLevel sl, BlockPos pos) {
        BlockPos p = pos;
        for (int i = 0; i < 8; i++) {
            var hit = dungeonNear(sl, p, PORTAL_CLEARANCE);
            if (hit == null) return i == 0 ? null : p;
            int c = PORTAL_CLEARANCE + 2;
            int west = p.getX() - (hit.minX() - c), east = (hit.maxX() + c) - p.getX();
            int north = p.getZ() - (hit.minZ() - c), south = (hit.maxZ() + c) - p.getZ();
            int best = Math.min(Math.min(west, east), Math.min(north, south));
            if (best == west) p = new BlockPos(hit.minX() - c, p.getY(), p.getZ());
            else if (best == east) p = new BlockPos(hit.maxX() + c, p.getY(), p.getZ());
            else if (best == north) p = new BlockPos(p.getX(), p.getY(), hit.minZ() - c);
            else p = new BlockPos(p.getX(), p.getY(), hit.maxZ() + c);
        }
        return p;
    }

    /** Bounding box of a dungeon with a piece within {@code r} blocks (horizontally) of pos, or null. */
    static net.minecraft.world.level.levelgen.structure.BoundingBox dungeonNear(ServerLevel sl, BlockPos pos, int r) {
        var zone = new net.minecraft.world.level.levelgen.structure.BoundingBox(pos.getX() - r, sl.getMinBuildHeight(), pos.getZ() - r,
            pos.getX() + r, sl.getMaxBuildHeight(), pos.getZ() + r);
        var reg = sl.registryAccess().registryOrThrow(Registries.STRUCTURE);
        int cx0 = (pos.getX() - r) >> 4, cx1 = (pos.getX() + r) >> 4, cz0 = (pos.getZ() - r) >> 4, cz1 = (pos.getZ() + r) >> 4;
        for (int cx = cx0; cx <= cx1; cx++) for (int cz = cz0; cz <= cz1; cz++) {
            for (var start : sl.structureManager().startsForStructure(new net.minecraft.world.level.ChunkPos(cx, cz), s -> reg.wrapAsHolder(s).is(ReturnRiftBlock.DUNGEONS))) {
                for (var piece : start.getPieces()) if (piece.getBoundingBox().intersects(zone)) return start.getBoundingBox();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ building
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!com.matt.quantumfalcon.WorldConfig.wardProtectsBlocks()) return;   // config: ward.protectBlocks
        Entity who = e.getEntity();
        if (exempt(who) || e.getPlacedBlock().is(PLACEABLE)) return;
        if (!inDungeon(e.getLevel(), e.getPos())) return;
        // players: no bridging/pillaring; anything else (dispensers, endermen, snow golems): no changes either
        e.setCanceled(true);
        tell(who, 1);
    }

    @SubscribeEvent
    public static void onUseBucket(PlayerInteractEvent.RightClickBlock e) {
        if (!com.matt.quantumfalcon.WorldConfig.wardProtectsBlocks()) return;   // config: ward.protectBlocks
        if (exempt(e.getEntity()) || !(e.getItemStack().getItem() instanceof BucketItem)) return;
        if (e.getLevel().isClientSide) return;
        BlockPos at = e.getPos(), side = at.relative(e.getFace() == null ? net.minecraft.core.Direction.UP : e.getFace());
        if (inDungeon(e.getLevel(), at) || inDungeon(e.getLevel(), side)) {
            e.setCanceled(true);
            tell(e.getEntity(), 2);
        }
    }

    // ------------------------------------------------------------------ escapes
    static boolean crossesWard(Entity e, Vec3 from, Vec3 to) {
        if (exempt(e)) return false;
        return inDungeon(e.level(), from) || inDungeon(e.level(), to);
    }

    /** Config ward.allowForeignTeleports=false: commands and other mods' teleports can't cross a ward either. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAnyTeleport(EntityTeleportEvent e) {
        if (e instanceof EntityTeleportEvent.EnderPearl || e instanceof EntityTeleportEvent.ChorusFruit || com.matt.quantumfalcon.WorldConfig.wardAllowsForeignTeleports()) return;
        if (e.getEntity().level().isClientSide || exempt(e.getEntity())) return;
        if (crossesWard(e.getEntity(), e.getPrev(), e.getTarget())) { e.setCanceled(true); tell(e.getEntity(), 3); }
    }

    /**
     * a plain {@code teleportTo} (another mod's homes or warps, a server plugin) fires no teleport event. Watch
     * each player's position instead: a jump of more than {@link #JUMP} blocks in one tick, or a dimension change,
     * that lands inside a ward from outside it is undone. Our own short blinks obey the same rule (no blinking through
     * a dungeon wall). Operators and creative players are exempt.
     */
    record LastPos(net.minecraft.resources.ResourceKey<Level> dim, Vec3 pos) {}
    private static final Map<UUID, LastPos> LAST_POS = new HashMap<>();
    static final double JUMP = 6.0;

    @SubscribeEvent
    public static void onPlayerTickJump(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer sp && !(sp instanceof net.neoforged.neoforge.common.util.FakePlayer)) guardJump(sp);
    }

    /** Test hook: forget a player's last position (the next tick starts a fresh watch). */
    public static void forgetPos(UUID id) { LAST_POS.remove(id); }

    /** One tick of the jump watch; returns true if it undid a teleport into a ward. Public for tests (fake players don't tick). */
    public static boolean guardJump(ServerPlayer sp) {
        LastPos last = LAST_POS.get(sp.getUUID());
        LastPos now = new LastPos(sp.level().dimension(), sp.position());
        if (last != null && !com.matt.quantumfalcon.WorldConfig.wardAllowsForeignTeleports() && !exempt(sp) && sp.isAlive()
                && (last.dim() != now.dim() || last.pos().distanceToSqr(now.pos()) > JUMP * JUMP)
                && inDungeon(sp.level(), sp.blockPosition())) {
            ServerLevel from = sp.server.getLevel(last.dim());
            if (from != null && !inDungeon(from, BlockPos.containing(last.pos()))) {
                if (sp instanceof net.neoforged.neoforge.common.util.FakePlayer && from == sp.level()) sp.moveTo(last.pos().x, last.pos().y, last.pos().z);   // fake connections ignore teleports
                else sp.teleportTo(from, last.pos().x, last.pos().y, last.pos().z, sp.getYRot(), sp.getXRot());
                sp.resetFallDistance();
                tell(sp, 3);
                return true;   // keep the outside position as the last good one
            }
        }
        LAST_POS.put(sp.getUUID(), now);
        return false;
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) { LAST_POS.remove(e.getEntity().getUUID()); }

    @SubscribeEvent
    public static void onRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e) { LAST_POS.remove(e.getEntity().getUUID()); }

    @SubscribeEvent
    public static void onPearl(EntityTeleportEvent.EnderPearl e) {
        if (crossesWard(e.getEntity(), e.getPrev(), e.getTarget())) { e.setCanceled(true); tell(e.getEntity(), 3); }
    }

    @SubscribeEvent
    public static void onChorus(EntityTeleportEvent.ChorusFruit e) {
        if (crossesWard(e.getEntity(), e.getPrev(), e.getTarget())) { e.setCanceled(true); tell(e.getEntity(), 3); }
    }

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent e) {
        Entity en = e.getEntity();
        if (exempt(en) || en.level().isClientSide) return;
        if (inDungeon(en.level(), en.blockPosition())) { e.setCanceled(true); tell(en, 3); }
    }

    // ------------------------------------------------------------------ machines and breakers that skip the break event
    /** Warded blocks inside a ward read as unbreakable (-1) on the server, so hardness-respecting machines leave them alone. */
    public static boolean wardsHardness(net.minecraft.world.level.BlockGetter level, BlockPos pos, BlockState st) {
        if (!(level instanceof ServerLevel sl) || st.isAir() || st.is(BREAKABLE)) return false;
        if (!com.matt.quantumfalcon.WorldConfig.wardProtectsBlocks() || !com.matt.quantumfalcon.WorldConfig.wardHardensBlocks()) return false;
        return inDungeon(sl, pos);
    }

    /** setBlock guard: a warded block inside a ward can't be turned into air or fluid by code outside this mod, vanilla player breaking, commands, explosions (already filtered) and worldgen. */
    public static boolean wardsRemoval(Level level, BlockPos pos, BlockState next) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!next.isAir() && next.getFluidState().isEmpty()) return false;
        if (!com.matt.quantumfalcon.WorldConfig.wardProtectsBlocks() || !com.matt.quantumfalcon.WorldConfig.wardGuardsMachines()) return false;
        if (!sl.isLoaded(pos)) return false;
        BlockState old = sl.getBlockState(pos);
        if (old.isAir() || old.canBeReplaced() || old.is(BREAKABLE)) return false;
        if (!inDungeon(sl, pos)) return false;
        return !trustedCaller();
    }

    private static final java.util.List<String> TRUSTED = java.util.List.of(
        "com.matt.quantumfalcon.",                          // our own dungeon mechanics (Then/Now flips, doors, tests)
        "net.minecraft.server.level.ServerPlayerGameMode",  // a player break that already passed onBreak (exempt players, crumbling walls) or a use
        "net.minecraft.server.commands.",                   // /setblock, /fill, /clone by operators
        "net.minecraft.world.level.Explosion",              // explosions are filtered by onExplode
        "net.minecraft.world.level.levelgen.",              // structure and feature placement
        "net.minecraft.gametest.");

    /** Walks the stack past the setBlock frames and our own guard; trusted when one of the next 12 frames (the caller and its
     *  helpers) belongs to a trusted caller. Machines from other mods sit in that window; a deep server tick frame doesn't count. */
    static boolean trustedCaller() {
        return java.lang.StackWalker.getInstance().walk(frames -> frames
            .map(java.lang.StackWalker.StackFrame::getClassName)
            .dropWhile(c -> !c.equals("net.minecraft.world.level.Level") && !c.equals("net.minecraft.server.level.ServerLevel"))
            .dropWhile(c -> c.equals("net.minecraft.world.level.Level") || c.equals("net.minecraft.server.level.ServerLevel"))
            .limit(12)
            .anyMatch(c -> TRUSTED.stream().anyMatch(c::startsWith)));
    }

    /** Debug helper for tests and the smoke harness: does the ward apply to this state at pos for this entity? */
    public static boolean wardsBreak(Level level, BlockPos pos, BlockState st, Entity who) {
        return !exempt(who) && !st.is(BREAKABLE) && inDungeon(level, pos);
    }
}
