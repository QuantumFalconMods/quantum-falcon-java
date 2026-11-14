package com.matt.quantumfalcon.arena.fight;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.boss.EntangledTwinsEntity;
import com.matt.quantumfalcon.entity.boss.QFBoss;
import com.matt.quantumfalcon.item.weapon.WeaponFx;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.*;

/**
 * Arena interaction during a boss fight (stage 4):
 * <ul>
 * <li><b>Seal</b>: when the boss is engaged an energy wall rises around the arena edge; it falls when the boss dies
 *     (and dissolves on its own if the fight is abandoned, see {@link ArenaSealBlock}).</li>
 * <li><b>Pylons</b>: four shield pylons rise on the floor. Each charged pylon takes 15% off every hit on the boss and
 *     beams to it; breaking one staggers the boss and chips its health. The Overseer also fires them at you.</li>
 * <li><b>Signature moves</b> that use the arena: Overseer "Pylon Surge" (beams from every charged pylon converge on
 *     your position), Rift Warden "Rift Fault" (a line of floor cracks towards you, then opens into the void), Twins
 *     "Entangled Swap" (the twins trade places with a shockwave at both ends), Sovereign "Horizon Collapse" (a ring
 *     of floor caves in around the centre and the singularity pulls).</li>
 * <li><b>Rift tiles</b> ({@link ArenaRiftTileBlock}) always put the original floor back.</li>
 * </ul>
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class ArenaInteraction {
    private ArenaInteraction() {}

    public static final class Fight {
        public final BlockPos center; public final ArenaType type; public final int radius; public UUID boss;
        public final List<BlockPos> pylons = new ArrayList<>(), seals = new ArrayList<>();
        /** seal blocks still to place, bottom rows first (spread over a few ticks instead of one long hitch). */
        final java.util.ArrayDeque<BlockPos> pendingSeal = new java.util.ArrayDeque<>();
        final Set<BlockPos> roofSeal = new HashSet<>();
        public int ticks, nextSignature = 120, signatures;
        public String lastSignature = "";
        /** players who have stood inside this fight; they may walk back in through the seal after dying. */
        public final Set<UUID> participants = new HashSet<>();
        /** ticks with no participant inside; at {@link #EMPTY_RESET_TICKS} the fight resets. */
        public int emptyTicks;
        final Map<UUID, Integer> warned = new HashMap<>();
        /** rebuilt from the save; seals and pylons are adopted from the world once the arena is loaded. */
        boolean restored, needsAdopt;
        int restoredAtTick = -100000;
        /** a participant has been inside since the restore (ends the login grace). */
        boolean participantSeen;
        Fight(BlockPos c, ArenaType t, int r) { center = c; type = t; radius = r; }
    }
    public static final Map<BlockPos, Fight> FIGHTS = new HashMap<>();
    private static final Map<BlockPos, ServerLevel> LEVELS = new HashMap<>();
    private record Delayed(long at, Runnable r) {}
    private static final List<Delayed> DELAYED = new ArrayList<>();
    private static long clock;
    static void later(int ticks, Runnable r) { DELAYED.add(new Delayed(clock + ticks, r)); }

    static ServerLevel levelOf(BlockPos center) { return LEVELS.get(center); }

    /** registers a fight restored from the save; its blocks are adopted when the arena loads. */
    static void adoptLater(ServerLevel level, Fight f) {
        f.needsAdopt = true;
        FIGHTS.put(f.center, f); LEVELS.put(f.center, level);
    }

    static void clearAll() { FIGHTS.clear(); LEVELS.clear(); SEAL_DROPS.clear(); }

    public static Fight fightFor(Entity boss) {
        for (Fight f : FIGHTS.values()) if (boss.getUUID().equals(f.boss)) return f;
        return null;
    }

    /** Engage: seal the arena and raise charged pylons. Safe to call again (refreshes the boss link only). */
    public static Fight startFight(ServerLevel level, Mob boss) {
        ArenaMechanics.ActiveArena arena = ArenaMechanics.getArenaAt(level, boss.blockPosition());
        if (arena == null) return null;
        Fight f = FIGHTS.get(arena.center);
        if (f != null) { f.boss = boss.getUUID(); return f; }
        f = new Fight(arena.center, arena.type, arena.radius());
        f.boss = boss.getUUID();
        FIGHTS.put(arena.center, f); LEVELS.put(arena.center, level);
        // fights are not saved, so a restart / Save & Quit mid-fight rebuilds this one from the world: adopt the
        // seal and pylon blocks still standing (broken pylons stay broken) instead of raising a fresh set.
        boolean resumed = boss.getPersistentData().getBoolean("qf_fight_started");
        if (resumed) adopt(level, f);
        clearSnow(level, f);
        raiseSeal(level, f);
        if (!resumed) raisePylons(level, f);
        boss.getPersistentData().putBoolean("qf_fight_started", true);
        if (resumed) return f;
        Component msg = Component.literal("The arena seals. Four shield pylons rise: break them to strip the boss's shield.").withStyle(ChatFormatting.AQUA);
        for (ServerPlayer p : players(level, f)) p.displayClientMessage(msg, false);
        level.playSound(null, f.center, com.matt.quantumfalcon.entity.util.QFMobSounds.get("arena_seal_up"), SoundSource.HOSTILE, 2.0f, 1.0f);
        return f;
    }

    /** a fight with nobody inside for this long resets (seal down, boss back to full health, fresh intro next time). */
    public static final int EMPTY_RESET_TICKS = 600;
    /** a fight restored from the save is not reset for being empty until a fighter is back or this many ticks pass. */
    public static final int RESTORE_GRACE_TICKS = 2400;

    static boolean inside(Fight f, Entity e) {
        double dx = e.getX() - (f.center.getX() + 0.5), dz = e.getZ() - (f.center.getZ() + 0.5);
        // the wall stands at radius+1: everything short of it is inside (the old radius cut-off teleported players standing
        // against the inner face of the wall); same depth as the seal band, so the Sovereign's void well still counts
        double r = f.radius + 0.75;
        return dx * dx + dz * dz <= r * r && e.getY() > f.center.getY() - 24 && e.getY() < f.center.getY() + ROOF;
    }

    /** Real players count; FakePlayers only when a test registers them (they are in no player list otherwise). */
    static boolean counts(ServerPlayer p) {
        return !p.isSpectator() && (!(p instanceof net.neoforged.neoforge.common.util.FakePlayer) || com.matt.quantumfalcon.story.BossCredit.TEST_PLAYERS.containsKey(p.getUUID()));
    }

    /** is this position inside the band around the seal wall (where digging or blasting could open a gap)? */
    public static boolean inSealBand(BlockPos pos) {
        for (Fight f : FIGHTS.values()) {
            double dx = pos.getX() + 0.5 - (f.center.getX() + 0.5), dz = pos.getZ() + 0.5 - (f.center.getZ() + 0.5);
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d >= f.radius - 1.5 && d <= f.radius + 3.5 && pos.getY() >= f.center.getY() - 24 && pos.getY() <= f.center.getY() + ROOF) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e) {
        if (FIGHTS.isEmpty() || !(e.getLevel() instanceof ServerLevel)) return;
        if (e.getPlayer() != null && e.getPlayer().isCreative()) return;
        if (inSealBand(e.getPos())) {
            e.setCanceled(true);
            if (e.getPlayer() != null) e.getPlayer().displayClientMessage(Component.literal("The seal's field holds this ground while the fight is on.").withStyle(ChatFormatting.DARK_AQUA), true);
        }
    }

    @SubscribeEvent
    public static void onDetonate(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate e) {
        if (FIGHTS.isEmpty()) return;
        e.getAffectedBlocks().removeIf(ArenaInteraction::inSealBand);
    }

    @SubscribeEvent
    public static void onPlayerDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        for (Fight f : FIGHTS.values()) if (f.participants.contains(p.getUUID())) {
            p.displayClientMessage(Component.literal("You fell in the arena. Walk back to the seal to step inside again; if nobody is left inside for "
                + EMPTY_RESET_TICKS / 20 + " s, the fight resets.").withStyle(ChatFormatting.GOLD), false);
        }
    }

    /** participants pressing against the wall from outside are let back in; others are told why it is shut. */
    static void tickEntry(ServerLevel level, Fight f, Mob boss) {
        boolean anyInside = false;
        List<ServerPlayer> near = new ArrayList<>(level.getEntitiesOfClass(ServerPlayer.class, new AABB(f.center).inflate(f.radius + 6, 32, f.radius + 6), ArenaInteraction::counts));
        for (var tp : com.matt.quantumfalcon.story.BossCredit.TEST_PLAYERS.values())   // test FakePlayers are not in the level's entity lists
            if (tp.level() == level && !near.contains(tp) && tp.distanceToSqr(Vec3.atCenterOf(f.center)) < (f.radius + 40) * (f.radius + 40)) near.add(tp);
        for (ServerPlayer p : near) {
            if (inside(f, p)) { f.participants.add(p.getUUID()); if (p.isAlive()) anyInside = true; continue; }
            if (!p.isAlive()) continue;
            double dx = p.getX() - (f.center.getX() + 0.5), dz = p.getZ() - (f.center.getZ() + 0.5), d = Math.sqrt(dx * dx + dz * dz);
            if (d < f.radius + 1.5 || d > f.radius + 4 || p.getY() < f.center.getY() - 16 || p.getY() > f.center.getY() + ROOF) continue;   // only from beyond the wall
            if (f.participants.contains(p.getUUID())) {
                double ir = Math.max(2, f.radius - 3);
                int x = (int) Math.floor(f.center.getX() + 0.5 + dx / d * ir), z = (int) Math.floor(f.center.getZ() + 0.5 + dz / d * ir);
                BlockPos fl = floorAt(level, x, f.center.getY(), z);
                if (fl == null) continue;
                if (p instanceof net.neoforged.neoforge.common.util.FakePlayer) p.moveTo(fl.getX() + 0.5, fl.getY() + 1, fl.getZ() + 0.5);   // no connection to teleport through
                else p.teleportTo(fl.getX() + 0.5, fl.getY() + 1, fl.getZ() + 0.5);
                p.displayClientMessage(Component.literal("The seal recognises you and lets you back in.").withStyle(ChatFormatting.AQUA), false);
                anyInside = true;
            } else if (f.warned.getOrDefault(p.getUUID(), -1000) < f.ticks - 200) {
                f.warned.put(p.getUUID(), f.ticks);
                p.displayClientMessage(Component.literal("This arena is sealed: a fight is on inside. It opens when the boss falls or the fighters leave.").withStyle(ChatFormatting.DARK_AQUA), true);
            }
        }
        if (anyInside) f.participantSeen = true;
        if (anyInside || f.participants.isEmpty()) { f.emptyTicks = 0; return; }
        // after a restart the fighters need time to log back in; the empty timer waits until one is back or 2 min pass
        if (f.restored && !f.participantSeen && f.ticks - f.restoredAtTick < RESTORE_GRACE_TICKS) { f.emptyTicks = 0; return; }
        f.emptyTicks += 10;
        if (f.emptyTicks >= EMPTY_RESET_TICKS) resetFight(level, f, boss);
    }

    /** nobody left inside: drop the seal, heal the boss and let the next arrival start a fresh fight. */
    public static void resetFight(ServerLevel level, Fight f, Mob boss) {
        Set<UUID> who = new HashSet<>(f.participants);
        List<Mob> bosses = new ArrayList<>();
        bosses.add(boss);
        if (boss instanceof EntangledTwinsEntity tw && tw.getTwin() != null) bosses.add(tw.getTwin());
        for (Mob b : bosses) {
            b.setHealth(b.getMaxHealth());
            b.setTarget(null);
            b.getPersistentData().remove("qf_intro");
            b.getPersistentData().remove("qf_fight_started");
        }
        endFight(level, f.center);
        Component msg = Component.literal("Nobody is left inside the arena: the fight resets and the seal opens.").withStyle(ChatFormatting.GOLD);
        for (UUID u : who) if (level.getServer().getPlayerList().getPlayer(u) instanceof ServerPlayer sp) sp.displayClientMessage(msg, false);
        QuantumFalcon.LOGGER.info("Arena fight at {} reset: empty for {} ticks", f.center, f.emptyTicks);
    }

    private static void adopt(ServerLevel level, Fight f) {
        int r = f.radius + 2;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            if (x * x + z * z > r * r) continue;
            for (int y = -16; y <= 24; y++) {
                m.set(f.center.getX() + x, f.center.getY() + y, f.center.getZ() + z);
                BlockState s = level.getBlockState(m);
                if (s.is(ModArenaBlocks.PYLON.get())) f.pylons.add(m.immutable());
                else if (s.is(ModArenaBlocks.SEAL.get())) f.seals.add(m.immutable());
            }
        }
    }

    public static boolean fightNear(BlockPos pos) {
        for (Fight f : FIGHTS.values()) { int r = f.radius + 8; if (f.center.distSqr(pos) <= (double) (r + ROOF) * (r + ROOF)) return true; }
        return false;
    }

    /** called from the boss tick; brings back a fight that a restart dropped. */
    public static void resumeIfDropped(ServerLevel level, Mob boss) {
        if (fightFor(boss) != null) return;
        var a = ArenaMechanics.getArenaAt(level, boss.blockPosition());
        if (a == null || FIGHTS.containsKey(a.center)) return;   // the twin's partner already owns it
        startFight(level, boss);
    }

    /** Cold-biome arenas collect snow layers over the dressed floor; the fight sweeps them away. */
    private static void clearSnow(ServerLevel level, Fight f) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int r = f.radius;
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            if (x * x + z * z > r * r) continue;
            for (int y = -3; y <= 20; y++) {
                m.set(f.center.getX() + x, f.center.getY() + y, f.center.getZ() + z);
                if (level.getBlockState(m).is(Blocks.SNOW)) level.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Victory or abandonment: drop the seal and clear leftover pylons. */
    public static void endFight(ServerLevel level, BlockPos center) {
        Fight f = FIGHTS.remove(center); LEVELS.remove(center);
        if (f == null) return;
        f.pendingSeal.clear();
        List<BlockPos> drop = new ArrayList<>(f.seals);
        drop.sort(java.util.Comparator.comparingInt(BlockPos::getY));   // bottom rows first: the way out opens at once
        SEAL_DROPS.add(new Object[]{level, new java.util.ArrayDeque<>(drop)});
        pumpDrops();
        for (BlockPos p : f.pylons) if (level.getBlockState(p).is(ModArenaBlocks.PYLON.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, center, com.matt.quantumfalcon.entity.util.QFMobSounds.get("arena_seal_down"), SoundSource.HOSTILE, 2.0f, 1.0f);
    }

    /** A boss died in this arena: hand the fight to a surviving partner (twins), or end it. */
    public static void onBossDeath(ServerLevel level, BlockPos center, Entity dead) {
        Fight f = FIGHTS.get(center);
        if (f == null) return;
        List<Entity> others = level.getEntities(dead, new AABB(center).inflate(f.radius + 16), x -> x instanceof QFBoss && x.isAlive() && !((LivingEntity) x).isDeadOrDying());
        if (others.isEmpty()) endFight(level, center);
        else f.boss = others.get(0).getUUID();
    }

    // ------------------------------------------------------------------ geometry
    /** Top solid block of the arena floor in column (x,z), searched around {@code y}; null if the column is open. */
    public static BlockPos floorAt(ServerLevel level, int x, int y, int z) {
        for (int dy = 8; dy >= -14; dy--) {
            BlockPos q = new BlockPos(x, y + dy, z);
            BlockState s = level.getBlockState(q);
            if (!s.isAir() && s.isCollisionShapeFullBlock(level, q) && level.getBlockState(q.above()).isAir() && level.getBlockState(q.above(2)).isAir()) return q;
        }
        return null;
    }

    /** seal blocks placed per server tick (the Guardian's ~25k seals go up in ~9 ticks, bottom rows first). */
    public static int SEAL_PER_TICK = 3000;

    static void raiseSeal(ServerLevel level, Fight f) {
        int r = f.radius + 1;
        int steps = (int) Math.ceil(2 * Math.PI * r * 1.6);
        Set<Long> done = new HashSet<>();
        List<BlockPos> wall = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            double a = 2 * Math.PI * i / steps;
            int x = f.center.getX() + (int) Math.round(Math.cos(a) * r), z = f.center.getZ() + (int) Math.round(Math.sin(a) * r);
            if (!done.add(((long) x << 32) ^ (z & 0xffffffffL))) continue;
            BlockPos fl = sealFloorAt(level, x, f.center.getY(), z);
            int base = fl != null ? fl.getY() : f.center.getY() - 1;
            // a full-height wall up to the roof, and grass / flowers / snow / water no longer leave gaps in it
            for (int y = base + 1; y < f.center.getY() + ROOF; y++) {
                BlockPos q = new BlockPos(x, y, z);
                if (sealable(level, q)) wall.add(q);
            }
        }
        wall.sort(java.util.Comparator.comparingInt(BlockPos::getY));   // close the walkable gap first
        Set<BlockPos> known = new HashSet<>(f.seals);
        for (BlockPos q : wall) if (known.add(q)) { f.seals.add(q); f.pendingSeal.add(q); }
        // and a roof, so nobody pillars or elytras over the wall
        int rr = r * r;
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            if (x * x + z * z > rr) continue;
            BlockPos q = new BlockPos(f.center.getX() + x, f.center.getY() + ROOF, f.center.getZ() + z);
            if (sealable(level, q) && known.add(q)) { f.seals.add(q); f.pendingSeal.add(q); f.roofSeal.add(q); }
        }
        pumpSeal(level, f, SEAL_PER_TICK);
    }

    /** Places up to {@code budget} pending seal blocks. Client-only updates: a seal has no neighbour logic to trigger. */
    static void pumpSeal(ServerLevel level, Fight f, int budget) {
        ArenaSealBlock seal = ModArenaBlocks.SEAL.get();
        while (budget-- > 0 && !f.pendingSeal.isEmpty()) {
            BlockPos q = f.pendingSeal.poll();
            if (!sealable(level, q)) { if (!level.getBlockState(q).is(seal)) f.seals.remove(q); continue; }
            level.setBlock(q, seal.stateAt(q, f.roofSeal.contains(q)), Block.UPDATE_CLIENTS);
        }
    }

    /** Test hook / resume: place every pending seal block now. */
    public static void finishSeal(ServerLevel level, Fight f) { if (f != null) pumpSeal(level, f, Integer.MAX_VALUE); }

    /** seals queued for removal after a fight ends (dropped over a few ticks too). */
    private static final java.util.ArrayDeque<Object[]> SEAL_DROPS = new java.util.ArrayDeque<>();

    static void pumpDrops() {
        int budget = SEAL_PER_TICK;
        while (budget > 0 && !SEAL_DROPS.isEmpty()) {
            Object[] d = SEAL_DROPS.peek();
            ServerLevel level = (ServerLevel) d[0];
            @SuppressWarnings("unchecked") java.util.ArrayDeque<BlockPos> q = (java.util.ArrayDeque<BlockPos>) d[1];
            while (budget-- > 0 && !q.isEmpty()) {
                BlockPos p = q.poll();
                if (level.getBlockState(p).is(ModArenaBlocks.SEAL.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            if (q.isEmpty()) SEAL_DROPS.poll();
        }
    }

    /** Test hook: finish every queued seal removal now. */
    public static void finishDrops() { int keep = SEAL_PER_TICK; SEAL_PER_TICK = Integer.MAX_VALUE; pumpDrops(); SEAL_PER_TICK = keep; }

    /** Like {@link #floorAt}, but grass, flowers, snow, torches and other replaceable blocks count as open space. */
    public static BlockPos sealFloorAt(ServerLevel level, int x, int y, int z) {
        for (int dy = 8; dy >= -14; dy--) {
            BlockPos q = new BlockPos(x, y + dy, z);
            BlockState s = level.getBlockState(q);
            if (!s.isAir() && !s.canBeReplaced() && s.isCollisionShapeFullBlock(level, q) && sealable(level, q.above()) && sealable(level, q.above(2))) return q;
        }
        return null;
    }

    /** Height of the seal roof above the arena centre. */
    public static final int ROOF = 18;

    static boolean sealable(ServerLevel level, BlockPos q) {
        BlockState s = level.getBlockState(q);
        return s.isAir() || (s.canBeReplaced() && !s.is(ModArenaBlocks.SEAL.get()));
    }

    static void raisePylons(ServerLevel level, Fight f) {
        double[] fracs = {0.55, 0.45, 0.65, 0.35, 0.75, 0.25};
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            BlockPos fl = null;
            // arena floors have gaps, pillars and void rings: walk inwards/outwards and a little sideways until a clear floor spot turns up
            search:
            for (double fr : fracs) for (int side = 0; side <= 4; side++) for (int sgn = -1; sgn <= 1; sgn += 2) {
                double d = Math.max(5, f.radius * fr), aa = a + sgn * side * 0.06;
                int x = f.center.getX() + (int) Math.round(Math.cos(aa) * d), z = f.center.getZ() + (int) Math.round(Math.sin(aa) * d);
                BlockPos q = floorAt(level, x, f.center.getY(), z);
                if (q != null && !level.getBlockState(q.above()).is(ModArenaBlocks.PYLON.get())) { fl = q; break search; }
                if (side == 0) break;
            }
            if (fl == null) continue;
            BlockPos p = fl.above();
            level.setBlock(p, ModArenaBlocks.PYLON.get().defaultBlockState(), Block.UPDATE_ALL);
            f.pylons.add(p);
            level.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 20, 0.3, 0.8, 0.3, 0.05);
        }
    }

    public static int chargedPylons(ServerLevel level, Fight f) {
        int n = 0;
        for (BlockPos p : f.pylons) { BlockState s = level.getBlockState(p); if (s.is(ModArenaBlocks.PYLON.get()) && s.getValue(ArenaPylonBlock.CHARGED)) n++; }
        return n;
    }

    static List<ServerPlayer> players(ServerLevel level, Fight f) {
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(f.center).inflate(f.radius + 12, 24, f.radius + 12), p -> !p.isSpectator());
    }

    static Mob boss(ServerLevel level, Fight f) {
        return f.boss != null && level.getEntity(f.boss) instanceof Mob m && m.isAlive() ? m : null;
    }

    // ------------------------------------------------------------------ pylons
    static void onPylonBroken(ServerLevel level, BlockPos pos, Player breaker) {
        for (Fight f : FIGHTS.values()) {
            if (!f.pylons.contains(pos)) continue;
            Mob boss = boss(level, f);
            int left = chargedPylons(level, f) - 1;
            if (boss != null) {
                boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                boss.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                boss.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                boss.invulnerableTime = 0;
                boss.hurt(level.damageSources().magic(), boss.getMaxHealth() * 0.03f);
                WeaponFx.trail(boss, Vec3.atCenterOf(pos), boss.position().add(0, boss.getBbHeight() * 0.5, 0), 0x6FF0FF);
            }
            level.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
            level.playSound(null, pos, com.matt.quantumfalcon.entity.util.QFMobSounds.get("arena_pylon_break"), SoundSource.BLOCKS, 2.0f, 1.0f);
            Component msg = Component.literal(left > 0 ? "Pylon shattered: the boss staggers (" + left + " left)." : "The last pylon shatters: the boss's shield is down!")
                .withStyle(left > 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD);
            for (ServerPlayer p : players(level, f)) p.displayClientMessage(msg, false);
        }
    }

    @SubscribeEvent
    public static void onBossHurt(LivingIncomingDamageEvent e) {
        LivingEntity ent = e.getEntity();
        if (!(ent instanceof QFBoss) || !(ent.level() instanceof ServerLevel level)) return;
        Fight f = fightFor(ent);
        if (f == null) return;
        int n = chargedPylons(level, f);
        if (n > 0) e.setAmount(e.getAmount() * (1f - 0.15f * n));
    }

    // ------------------------------------------------------------------ ticking + signature moves
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        clock++;
        if (!DELAYED.isEmpty()) {
            List<Delayed> due = new ArrayList<>();
            DELAYED.removeIf(d -> { if (d.at() <= clock) { due.add(d); return true; } return false; });
            for (Delayed d : due) d.r().run();
        }
        if (!SEAL_DROPS.isEmpty()) pumpDrops();
        if (FIGHTS.isEmpty()) return;
        for (BlockPos c : new ArrayList<>(FIGHTS.keySet())) {
            Fight f = FIGHTS.get(c); ServerLevel level = LEVELS.get(c);
            if (f == null || level == null) continue;
            if (f.needsAdopt) {
                // wait until the arena and its entities are loaded, then take back the standing seal and pylons
                if (!level.isLoaded(c) || !level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(c))) continue;
                f.needsAdopt = false;
                adopt(level, f);
                f.ticks = Math.max(f.ticks, 1);
                f.restoredAtTick = f.ticks;
            }
            if (!f.pendingSeal.isEmpty()) pumpSeal(level, f, SEAL_PER_TICK);
            Mob boss = boss(level, f);
            f.ticks++;
            if (boss == null) {
                // boss gone: unloaded chunks keep the fight; a dead or vanished boss ends it after 5 s
                if (f.ticks % 100 == 0 && f.ticks - f.restoredAtTick > 200 && level.isLoaded(c) && level.getEntities((Entity) null, new AABB(c).inflate(f.radius + 16), x -> x instanceof QFBoss && x.isAlive()).isEmpty()) endFight(level, c);
                continue;
            }
            if (f.ticks % 10 == 0) beams(level, f, boss);
            if (f.ticks % 10 == 0) { tickEntry(level, f, boss); if (!FIGHTS.containsKey(c)) continue; }
            if (f.ticks >= f.nextSignature && !boss.isNoAi()) {
                ServerPlayer target = nearest(level, f, boss);
                int phase = Math.max(1, ((QFBoss) boss).qfPhase());
                boolean fired = target != null && signature(level, f, boss, target, phase);
                f.nextSignature = f.ticks + (fired ? interval(f.type) : 40);
            }
        }
    }

    static int interval(ArenaType t) {
        return switch (t) { case QUANTUM_GUARDIAN -> 240; case VOID_HORROR -> 200; case ENTANGLED_TWINS -> 260; case SINGULARITY_SOVEREIGN -> 260; };
    }

    static ServerPlayer nearest(ServerLevel level, Fight f, Mob boss) {
        ServerPlayer best = null; double bd = Double.MAX_VALUE;
        for (ServerPlayer p : players(level, f)) { double d = p.distanceToSqr(boss); if (d < bd && !p.isCreative()) { bd = d; best = p; } }
        return best;
    }

    static void beams(ServerLevel level, Fight f, Mob boss) {
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.4f, 0.95f, 1f), 0.9f);
        Vec3 to = boss.position().add(0, boss.getBbHeight() * 0.55, 0);
        for (BlockPos p : f.pylons) {
            BlockState s = level.getBlockState(p);
            if (!s.is(ModArenaBlocks.PYLON.get()) || !s.getValue(ArenaPylonBlock.CHARGED)) continue;
            Vec3 from = Vec3.atCenterOf(p).add(0, 0.5, 0);
            double len = from.distanceTo(to);
            for (double t = 0; t < len; t += 1.2) { Vec3 q = from.lerp(to, t / len); level.sendParticles(dust, q.x, q.y, q.z, 1, 0, 0, 0, 0); }
        }
    }

    /** Runs the arena signature for this boss type if its phase allows. Public for tests. */
    public static boolean signature(ServerLevel level, Fight f, Mob boss, LivingEntity target, int phase) {
        boolean ok = switch (f.type) {
            case QUANTUM_GUARDIAN -> phase >= 2 && pylonSurge(level, f, target);
            case VOID_HORROR -> phase >= 2 && riftFault(level, f, boss, target) > 0;
            case ENTANGLED_TWINS -> boss instanceof EntangledTwinsEntity tw && entangledSwap(level, tw);
            case SINGULARITY_SOVEREIGN -> phase >= 3 && horizonCollapse(level, f) > 0;
        };
        if (ok) { f.signatures++; f.lastSignature = f.type.name(); }
        return ok;
    }

    /** Overseer: every charged pylon telegraphs a beam at the target's current spot, then fires 40 ticks later. */
    public static boolean pylonSurge(ServerLevel level, Fight f, LivingEntity target) {
        List<Vec3> from = new ArrayList<>();
        for (BlockPos p : f.pylons) { BlockState s = level.getBlockState(p); if (s.is(ModArenaBlocks.PYLON.get()) && s.getValue(ArenaPylonBlock.CHARGED)) from.add(Vec3.atCenterOf(p).add(0, 0.5, 0)); }
        if (from.isEmpty()) return false;
        Vec3 aim = target.position().add(0, 1, 0);
        DustParticleOptions warn = new DustParticleOptions(new Vector3f(1f, 0.3f, 0.35f), 0.7f);
        for (int k = 0; k < 4; k++) {
            int delay = k * 10;
            later(delay, () -> { for (Vec3 a : from) line(level, a, aim, warn, 1.0); });
        }
        level.playSound(null, BlockPos.containing(aim), com.matt.quantumfalcon.entity.util.QFMobSounds.get("arena_pylon_charge"), SoundSource.HOSTILE, 2.0f, 1.0f);
        later(40, () -> {
            for (Vec3 a : from) {
                line(level, a, aim, ParticleTypes.END_ROD, 0.5);
                for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, new AABB(a, aim).inflate(1.5), x -> !(x instanceof QFBoss))) {
                    if (distToSegment(v.position().add(0, v.getBbHeight() * 0.5, 0), a, aim) < 1.3) { v.invulnerableTime = 0; v.hurt(level.damageSources().magic(), 3f); }
                }
            }
            level.playSound(null, BlockPos.containing(aim), com.matt.quantumfalcon.entity.util.QFMobSounds.get("arena_pylon_beam"), SoundSource.HOSTILE, 1.4f, 1.0f);
        });
        return true;
    }

    /** Rift Warden: a 12-block line of floor cracks from the boss towards the target, then opens for 5 s. */
    public static int riftFault(ServerLevel level, Fight f, Mob boss, LivingEntity target) {
        Vec3 dir = target.position().subtract(boss.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(1, 0, 0);
        dir = dir.normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        int n = 0;
        for (int i = 2; i <= 13; i++) {
            Vec3 q = boss.position().add(dir.scale(i));
            for (int w = -1; w <= 1; w++) {
                if (w != 0 && i % 2 == 1) continue;
                Vec3 qq = q.add(side.scale(w));
                BlockPos fl = floorAt(level, (int) Math.floor(qq.x), (int) Math.floor(boss.getY()), (int) Math.floor(qq.z));
                if (fl != null && crack(level, fl, 30 + i * 2, 100)) n++;
            }
        }
        if (n > 0) { level.playSound(null, boss.blockPosition(), SoundEvents.DEEPSLATE_BREAK, SoundSource.HOSTILE, 1.4f, 0.5f); level.playSound(null, boss.blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("rift_shatter"), SoundSource.HOSTILE, 1.8f, 1.0f); }
        return n;
    }

    /** Twins: swap places with a shockwave at both ends; anyone within 3 blocks of either is thrown back. */
    public static boolean entangledSwap(ServerLevel level, EntangledTwinsEntity a) {
        EntangledTwinsEntity b = a.getTwin();
        if (b == null || !b.isAlive()) return false;
        Vec3 pa = a.position(), pb = b.position();
        a.teleportTo(pb.x, pb.y, pb.z); b.teleportTo(pa.x, pa.y, pa.z);
        for (Vec3 c : List.of(pa, pb)) {
            WeaponFx.shock(a, c, 0xC86BFF, 3.5);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1, c.z, 30, 0.6, 0.8, 0.6, 0.1);
            for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(c)).inflate(3), x -> !(x instanceof QFBoss))) {
                Vec3 push = v.position().subtract(c).multiply(1, 0, 1);
                if (push.lengthSqr() < 1e-4) push = new Vec3(1, 0, 0);
                v.push(push.normalize().x * 1.2, 0.45, push.normalize().z * 1.2); v.hurtMarked = true;
                v.invulnerableTime = 0; v.hurt(level.damageSources().magic(), 4f);
            }
        }
        level.playSound(null, BlockPos.containing(pa), com.matt.quantumfalcon.entity.util.QFMobSounds.get("ability_entanglement"), SoundSource.HOSTILE, 2.0f, 1.0f);
        return true;
    }

    /** Sovereign: a ring of floor at 45% of the radius cracks, then caves in for 6 s while the singularity pulls inwards. */
    public static int horizonCollapse(ServerLevel level, Fight f) {
        double r = Math.max(4, f.radius * 0.45);
        int steps = (int) Math.ceil(2 * Math.PI * r * 1.5), n = 0;
        Set<Long> done = new HashSet<>();
        for (int ring = 0; ring < 2; ring++) for (int i = 0; i < steps; i++) {
            double a = 2 * Math.PI * i / steps, rr = r + ring;
            int x = f.center.getX() + (int) Math.round(Math.cos(a) * rr), z = f.center.getZ() + (int) Math.round(Math.sin(a) * rr);
            if (!done.add(((long) x << 32) ^ (z & 0xffffffffL))) continue;
            BlockPos fl = floorAt(level, x, f.center.getY(), z);
            if (fl != null && crack(level, fl, 40, 120)) n++;
        }
        later(40, () -> {
            for (ServerPlayer p : players(level, f)) {
                Vec3 in = Vec3.atCenterOf(f.center).subtract(p.position()).multiply(1, 0, 1);
                if (in.lengthSqr() > 1e-4) { p.push(in.normalize().x * 0.6, 0.1, in.normalize().z * 0.6); p.hurtMarked = true; }
            }
            level.playSound(null, f.center, com.matt.quantumfalcon.entity.util.QFMobSounds.get("gravity_well"), SoundSource.HOSTILE, 1.8f, 1.0f);
        });
        return n;
    }

    /** Turns a solid, plain floor block into a rift tile (cracked for {@code warn} ticks, open for {@code open} ticks, then restored). */
    public static boolean crack(ServerLevel level, BlockPos pos, int warn, int open) {
        BlockState orig = level.getBlockState(pos);
        if (orig.isAir() || orig.hasBlockEntity() || !orig.isCollisionShapeFullBlock(level, pos)
            || orig.is(ModArenaBlocks.RIFT_TILE.get()) || orig.is(ModArenaBlocks.SEAL.get()) || orig.is(ModArenaBlocks.PYLON.get())
            || orig.is(Blocks.BEDROCK) || orig.is(Blocks.BARRIER)) return false;
        level.setBlock(pos, ModArenaBlocks.RIFT_TILE.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof RiftTileBlockEntity be) { be.original = orig; be.openTicks = open; be.setChanged(); }
        level.scheduleTick(pos, ModArenaBlocks.RIFT_TILE.get(), warn);
        return true;
    }

    static void line(ServerLevel level, Vec3 a, Vec3 b, net.minecraft.core.particles.ParticleOptions p, double step) {
        double len = a.distanceTo(b);
        for (double t = 0; t <= len; t += step) { Vec3 q = a.lerp(b, t / len); level.sendParticles(p, q.x, q.y, q.z, 1, 0, 0, 0, 0); }
    }

    static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a); double t = Math.max(0, Math.min(1, p.subtract(a).dot(ab) / Math.max(1e-6, ab.lengthSqr())));
        return p.distanceTo(a.add(ab.scale(t)));
    }
}
