package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.arena.BossSummoner;
import com.matt.quantumfalcon.arena.fight.ArenaInteraction;
import com.matt.quantumfalcon.arena.fight.ArenaPylonBlock;
import com.matt.quantumfalcon.arena.fight.ArenaRiftTileBlock;
import com.matt.quantumfalcon.arena.fight.ModArenaBlocks;
import com.matt.quantumfalcon.entity.boss.BossCutscenes;
import com.matt.quantumfalcon.entity.boss.EntangledTwinsEntity;
import com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity;
import com.matt.quantumfalcon.network.CutscenePayload;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** Stage 4: boss cutscene freeze, arena seal, shield pylons, pylon break stagger, and each boss's arena signature move. */
public final class BossFightTests {
    private BossFightTests() {}

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("boss_cutscene_freeze", BossFightTests::testCutscene);
        tests.put("boss_cutscene_multiplayer", BossFightTests::testCutsceneMp);
        int i = 0;
        for (ArenaType t : ArenaType.values()) { final int idx = i++; tests.put("arena_fight_" + short_(t), l -> testFight(l, t, idx)); }
    }

    static String short_(ArenaType t) {
        return switch (t) { case QUANTUM_GUARDIAN -> "overseer"; case VOID_HORROR -> "warden"; case ENTANGLED_TWINS -> "twins"; case SINGULARITY_SOVEREIGN -> "sovereign"; };
    }

    static BlockPos site(ServerLevel level, int x, int z) {
        for (int cx = -6; cx <= 6; cx++) for (int cz = -6; cz <= 6; cz++) level.setChunkForced((x >> 4) + cx, (z >> 4) + cz, true);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 10, z);
    }

    static void testCutscene(ServerLevel level) {
        BlockPos c = site(level, 8600, 9000 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, ArenaType.QUANTUM_GUARDIAN);
        if (boss == null) { result("boss_cutscene_freeze", false, "summon failed"); return; }
        later(10, () -> {
            boolean before = boss.isNoAi();
            BossCutscenes.play(boss, CutscenePayload.INTRO, 0x43E6FF);
            boolean frozen = boss.isNoAi() && BossCutscenes.isFrozen(boss);
            later(BossCutscenes.INTRO_TICKS + 5, () -> {
                boolean resumed = !boss.isNoAi() && !BossCutscenes.isFrozen(boss);
                boss.discard();
                ArenaInteraction.endFight(level, c);
                result("boss_cutscene_freeze", !before && frozen && resumed,
                    "AI before=" + !before + ", frozen during intro=" + frozen + ", resumed after " + BossCutscenes.INTRO_TICKS + "t=" + resumed);
            });
        });
    }

    /** Cutscene rules with two players: no free hits on the frozen boss, watchers neither take nor deal damage, a skip ends
     *  only the skipper's protection, and a server stop releases the frozen boss. */
    static void testCutsceneMp(ServerLevel level) {
        BlockPos c = site(level, 8600, 9400 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, ArenaType.QUANTUM_GUARDIAN);
        if (boss == null) { result("boss_cutscene_multiplayer", false, "summon failed"); return; }
        later(10, () -> {
            var a = AuditTests.freshPlayer(level, "QFWatcherA"); var b = AuditTests.freshPlayer(level, "QFWatcherB");
            a.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); b.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            a.moveTo(boss.getX() + 4, boss.getY(), boss.getZ()); b.moveTo(boss.getX() - 4, boss.getY(), boss.getZ());
            BossCutscenes.play(boss, CutscenePayload.INTRO, 0x43E6FF);
            BossCutscenes.testProtect(a, BossCutscenes.INTRO_TICKS); BossCutscenes.testProtect(b, BossCutscenes.INTRO_TICKS);
            StringBuilder d = new StringBuilder();
            float hp0 = boss.getHealth();
            boss.invulnerableTime = 0;
            boss.hurt(a.damageSources().playerAttack(a), 10f);
            boolean bossSafe = boss.getHealth() == hp0;
            var z = MovesetTests.dummy(level, new net.minecraft.world.phys.Vec3(boss.getX() + 6, boss.getY(), boss.getZ() + 6));
            float z0 = z.getHealth();
            z.hurt(a.damageSources().playerAttack(a), 5f);
            boolean watcherDealsNone = z.getHealth() == z0;
            float b0 = b.getHealth();
            b.hurt(z.damageSources().mobAttack(z), 5f);
            boolean watcherTakesNone = b.getHealth() == b0;
            BossCutscenes.skipped(a);
            z.invulnerableTime = 0;
            z.hurt(a.damageSources().playerAttack(a), 5f);
            boolean skipperFree = z.getHealth() < z0 && !BossCutscenes.isProtected(a);
            boolean otherStill = BossCutscenes.isProtected(b) && BossCutscenes.isFrozen(boss);
            BossCutscenes.releaseAll();
            boolean released = !boss.isNoAi() && !BossCutscenes.isFrozen(boss) && !BossCutscenes.isProtected(b);
            d.append("frozen boss took no hit=").append(bossSafe).append(" watcher dealt none=").append(watcherDealsNone)
             .append(" watcher took none=").append(watcherTakesNone).append(" skipper unprotected+can hit=").append(skipperFree)
             .append(" other watcher still protected, boss still frozen=").append(otherStill).append(" stop releases boss=").append(released);
            z.discard(); boss.discard();
            ArenaInteraction.endFight(level, c);
            result("boss_cutscene_multiplayer", bossSafe && watcherDealsNone && watcherTakesNone && skipperFree && otherStill && released, d.toString());
        });
    }

    static int sealCount(ServerLevel level, ArenaInteraction.Fight f) {
        int n = 0; for (BlockPos p : f.seals) if (level.getBlockState(p).is(ModArenaBlocks.SEAL.get())) n++; return n;
    }

    static void testFight(ServerLevel level, ArenaType type, int idx) {
        String name = "arena_fight_" + short_(type);
        BlockPos c = site(level, 9000 + idx * 400, 9000 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, type);
        if (boss == null) { result(name, false, "summon failed"); return; }
        StringBuilder d = new StringBuilder();
        later(40, () -> {
            ArenaInteraction.Fight f = ArenaInteraction.startFight(level, boss); ArenaInteraction.finishSeal(level, f);   // seals go up over ~9 ticks; this test inspects them at once
            if (f == null) { result(name, false, "no arena registered at boss"); return; }
            int seals = sealCount(level, f), pylons = ArenaInteraction.chargedPylons(level, f);
            d.append("seal blocks=").append(seals).append(", charged pylons=").append(pylons);
            // shield: magic damage with 4 pylons vs none
            boss.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.REGENERATION, 1, 0)); // no-op, keeps effect map warm
            if (boss instanceof SingularitySovereignEntity sv) sv.clearInvulnerability();
            float h0 = boss.getHealth(); boss.invulnerableTime = 0;
            boss.hurt(level.damageSources().magic(), 10f);
            float shielded = h0 - boss.getHealth();
            // target dummy for signatures
            Zombie dummy = EntityType.ZOMBIE.create(level);
            Vec3 bp = boss.position();
            BlockPos fl = ArenaInteraction.floorAt(level, (int) Math.floor(bp.x) + 7, c.getY(), (int) Math.floor(bp.z));
            if (fl == null) fl = ArenaInteraction.floorAt(level, (int) Math.floor(bp.x), c.getY(), (int) Math.floor(bp.z) + 7);
            BlockPos stand = fl != null ? fl.above() : BlockPos.containing(bp).offset(7, 0, 0);
            dummy.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
            dummy.finalizeSpawn(level, level.getCurrentDifficultyAt(stand), MobSpawnType.COMMAND, null);
            dummy.setNoAi(true); dummy.setPersistenceRequired(); dummy.setInvulnerable(false);
            dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200); dummy.setHealth(200);
            level.addFreshEntity(dummy);
            float dummyHp = dummy.getHealth();
            int phase = 4;
            Vec3[] twinPos = new Vec3[2];
            EntangledTwinsEntity partner = boss instanceof EntangledTwinsEntity tw ? tw.getTwin() : null;
            if (partner != null) { twinPos[0] = boss.position(); twinPos[1] = partner.position(); }
            List<BlockPos> tilesBefore = riftTiles(level, c, f.radius);
            boolean sig = ArenaInteraction.signature(level, f, boss, dummy, phase);
            List<BlockPos> tiles = riftTiles(level, c, f.radius);
            tiles.removeAll(tilesBefore);
            d.append(", signature fired=").append(sig);
            if (partner != null) d.append(", twins swapped=").append(boss.position().distanceTo(twinPos[1]) < 0.6 && partner.position().distanceTo(twinPos[0]) < 0.6);
            if (!tiles.isEmpty()) d.append(", rift tiles cracked=").append(tiles.size());
            later(45, () -> {
                long open = tiles.stream().filter(p -> level.getBlockState(p).is(ModArenaBlocks.RIFT_TILE.get()) && level.getBlockState(p).getValue(ArenaRiftTileBlock.STAGE) == ArenaRiftTileBlock.Stage.OPEN).count();
                if (!tiles.isEmpty()) d.append(", open after 45t=").append(open);
                float dummyDmg = dummyHp - dummy.getHealth();
                if (type == ArenaType.QUANTUM_GUARDIAN) d.append(", surge hit dummy=").append(dummyDmg > 0).append(" (").append(dummyDmg).append(")");
                // break pylons as a player: the first break must stagger the boss
                var fp = tester(level);
                fp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                boolean stagger = false; int broken = 0;
                for (BlockPos p : new ArrayList<>(f.pylons)) {
                    fp.moveTo(p.getX() + 1.5, p.getY(), p.getZ() + 0.5);
                    if (fp.gameMode.destroyBlock(p)) broken++;
                    if (broken == 1) stagger = boss.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && boss.hasEffect(MobEffects.WEAKNESS);
                }
                int left = ArenaInteraction.chargedPylons(level, f);
                if (boss instanceof SingularitySovereignEntity sv) sv.clearInvulnerability();
                float h1 = boss.getHealth(); boss.invulnerableTime = 0;
                if (partner != null) partner.invulnerableTime = 0;
                boss.hurt(level.damageSources().magic(), 10f);
                float bare = h1 - boss.getHealth();
                d.append(", pylons broken=").append(broken).append(" stagger=").append(stagger).append(" left=").append(left)
                 .append(", dmg 10 shielded=").append(String.format("%.1f", shielded)).append(" bare=").append(String.format("%.1f", bare));
                boolean shieldOk = bare > 0 && shielded < bare * 0.6f;
                final int brokenF = broken; final boolean staggerF = stagger;
                later(180, () -> {
                    long restored = tiles.stream().filter(p -> !level.getBlockState(p).is(ModArenaBlocks.RIFT_TILE.get())).count();
                    if (!tiles.isEmpty()) d.append(", floor restored=").append(restored).append("/").append(tiles.size());
                    // kill: fight must end, seal must drop
                    List<Mob> targets = new ArrayList<>(List.of(boss)); if (partner != null) targets.add(partner);
                    for (Mob m : targets) for (int i = 0; i < 40 && m.isAlive() && !m.isDeadOrDying(); i++) {
                        m.invulnerableTime = 0; if (m instanceof SingularitySovereignEntity sv) sv.clearInvulnerability();
                        m.setHealth(Math.min(m.getHealth(), 2f)); m.hurt(level.damageSources().playerAttack(fp), 100f);
                    }
                    later(30, () -> {
                        boolean ended = !ArenaInteraction.FIGHTS.containsKey(f.center);
                        int sealsLeft = sealCount(level, f);
                        d.append(", fight ended on death=").append(ended).append(" seal left=").append(sealsLeft);
                        dummy.discard();
                        boolean sigOk = sig && switch (type) {
                            case QUANTUM_GUARDIAN -> dummyDmg > 0;
                            case ENTANGLED_TWINS -> partner != null;
                            default -> !tiles.isEmpty() && open > 0 && restored == tiles.size();
                        };
                        boolean ok = seals > 20 && pylons == 4 && sigOk && brokenF == 4 && staggerF && left == 0 && shieldOk && ended && sealsLeft == 0;
                        result(name, ok, d.toString());
                    });
                });
            });
        });
    }

    static List<BlockPos> riftTiles(ServerLevel level, BlockPos c, int r) {
        List<BlockPos> out = new ArrayList<>();
        Block tile = ModArenaBlocks.RIFT_TILE.get();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r - 2, -10, -r - 2), c.offset(r + 2, 6, r + 2))) {
            BlockState s = level.getBlockState(p);
            if (s.is(tile)) out.add(p.immutable());
        }
        return out;
    }
}
