package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.arena.BossSummoner;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.registry.ModEntities;
import com.matt.quantumfalcon.story.BossCredit;
import com.matt.quantumfalcon.story.PlayerStoryData;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import com.matt.quantumfalcon.worldgen.structure.ArenaTemplateBuilder;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Map;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** (boss credit beyond the killing blow) and (one boss per arena however often you walk in). */
final class BossCreditTests {
    private BossCreditTests() {}

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("audit_n2_single_boss", BossCreditTests::n2);
        tests.put("audit_n1_falcon_kill", BossCreditTests::n1);
        tests.put("pr3_offline_credit", BossCreditTests::pr3);
    }

    static int count(ServerLevel level, BlockPos c) {
        return level.getEntities(ModEntities.QUANTUM_OVERSEER.get(), new AABB(c).inflate(200), e -> e.isAlive()).size();
    }

    static void n2(ServerLevel level) {
        BlockPos c = BossFightTests.site(level, 15000, 9000 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        ArenaTemplateBuilder.buildArena(level, c, ArenaType.QUANTUM_GUARDIAN);
        FakePlayer fp = tester(level);
        fp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        StringBuilder d = new StringBuilder();
        int[] counts = new int[3];
        enter(level, fp, c, 0, counts, d, () -> {
            var arena = ArenaMechanics.arenaAt(c);
            boolean owned = arena != null && arena.bossId != null;
            level.getEntities(ModEntities.QUANTUM_OVERSEER.get(), new AABB(c).inflate(200), e -> true).forEach(Mob::discard);
            ArenaMechanics.testLeave(fp.getUUID());
            ArenaMechanics.unregisterArena(c);
            boolean ok = counts[0] == 1 && counts[1] == 1 && counts[2] == 1 && owned;
            result("audit_n2_single_boss", ok, "Overseers after entry 1/2/3 = " + counts[0] + "/" + counts[1] + "/" + counts[2]
                + " (boss moved 70 blocks up between entries 2 and 3, out of the old search box), arena owns boss UUID=" + owned + d);
        });
    }

    static void enter(ServerLevel level, FakePlayer fp, BlockPos c, int i, int[] counts, StringBuilder d, Runnable done) {
        if (i >= 3) { done.run(); return; }
        fp.moveTo(c.getX() + 3.5, c.getY() + 1, c.getZ() + 0.5);
        ArenaMechanics.testPlayerTick(fp, level);
        later(10, () -> {
            counts[i] = count(level, c);
            ArenaMechanics.testLeave(fp.getUUID());
            fp.moveTo(c.getX() + 200.5, c.getY() + 1, c.getZ() + 0.5);
            if (i == 1) level.getEntities(ModEntities.QUANTUM_OVERSEER.get(), new AABB(c).inflate(200), e -> true)
                .forEach(e -> { e.teleportTo(c.getX() + 0.5, c.getY() + 70, c.getZ() + 0.5); ((Mob) e).setNoAi(true); e.setNoGravity(true); });
            later(10, () -> enter(level, fp, c, i + 1, counts, d, done));
        });
    }

    static void n1(ServerLevel level) {
        BlockPos c = BossFightTests.site(level, 15400, 9000 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, ArenaType.QUANTUM_GUARDIAN);
        if (boss == null) { result("audit_n1_falcon_kill", false, "summon failed"); return; }
        FakePlayer owner = AuditTests.freshPlayer(level, "QFFalconer"), ally = AuditTests.freshPlayer(level, "QFAlly");
        BossCredit.TEST_PLAYERS.put(owner.getUUID(), owner); BossCredit.TEST_PLAYERS.put(ally.getUUID(), ally);
        owner.moveTo(c.getX() + 300.5, c.getY() + 1, c.getZ() + 0.5);   // the owner is far away: only the falcon fights
        ally.moveTo(c.getX() + 300.5, c.getY() + 1, c.getZ() + 2.5);
        later(20, () -> {
            boss.setNoAi(true);
            QuantumFalconEntity f = falcon(level, boss.blockPosition().offset(2, 0, 0), true);
            f.tame(owner);
            boolean before = PlayerStoryData.get(owner).hasDefeatedBoss("QUANTUM_OVERSEER") || PlayerStoryData.get(ally).hasDefeatedBoss("QUANTUM_OVERSEER");
            int ownerPts0 = com.matt.quantumfalcon.skill.SkillPointSystem.getAvailablePoints(owner), allyPts0 = com.matt.quantumfalcon.skill.SkillPointSystem.getAvailablePoints(ally);
            boss.invulnerableTime = 0;
            boss.hurt(ally.damageSources().playerAttack(ally), 2f);            // the ally chips in once from range
            boss.setHealth(1f); boss.invulnerableTime = 0;
            if (boss instanceof com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity) { /* shields from pylons do not matter at 1 hp */ }
            boolean killed = boss.hurt(f.damageSources().mobAttack(f), 500f) && !boss.isAlive();
            later(10, () -> {
                boolean ownerCredited = PlayerStoryData.get(owner).hasDefeatedBoss("QUANTUM_OVERSEER");
                boolean allyCredited = PlayerStoryData.get(ally).hasDefeatedBoss("QUANTUM_OVERSEER");
                boolean retired = ArenaMechanics.arenaAt(c) == null;
                // both credited players get the first-kill points, a re-kill pays nothing
                int pay = com.matt.quantumfalcon.skill.SkillPointSystem.BOSS_FIRST_KILL_POINTS[0];
                int ownerGain = com.matt.quantumfalcon.skill.SkillPointSystem.getAvailablePoints(owner) - ownerPts0;
                int allyGain = com.matt.quantumfalcon.skill.SkillPointSystem.getAvailablePoints(ally) - allyPts0;
                com.matt.quantumfalcon.story.BossDeathHandler.onBossDefeated(owner, level, com.matt.quantumfalcon.story.BossDeathHandler.BossType.QUANTUM_OVERSEER, c, false);
                boolean rekillFree = com.matt.quantumfalcon.skill.SkillPointSystem.getAvailablePoints(owner) - ownerPts0 == ownerGain;
                boolean pointsOk = ownerGain == pay && allyGain == pay && rekillFree;
                f.discard(); if (boss.isAlive()) boss.discard();
                BossCredit.TEST_PLAYERS.remove(owner.getUUID()); BossCredit.TEST_PLAYERS.remove(ally.getUUID());
                com.matt.quantumfalcon.arena.fight.ArenaInteraction.endFight(level, c);
                result("audit_n1_falcon_kill", !before && killed && ownerCredited && allyCredited && retired && pointsOk,
                    "falcon killing blow=" + killed + ", owner (300 blocks away) credited=" + ownerCredited + ", ally who hit once credited=" + allyCredited
                        + ", arena retired after credit=" + retired + ", skill points owner +" + ownerGain + " ally +" + allyGain + " (want +" + pay + " each), re-kill pays 0=" + rekillFree);
            });
        });
    }

    /** a player who hit the boss and logged out before the kill is credited on the next login. */
    static void pr3(ServerLevel level) {
        BlockPos c = BossFightTests.site(level, 15800, 9000 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, ArenaType.QUANTUM_GUARDIAN);
        if (boss == null) { result("pr3_offline_credit", false, "summon failed"); return; }
        FakePlayer killer = AuditTests.freshPlayer(level, "QFStayer");
        BossCredit.TEST_PLAYERS.put(killer.getUUID(), killer);
        java.util.UUID awayId = java.util.UUID.randomUUID();   // fought, then logged out: in no player list
        killer.moveTo(c.getX() + 300.5, c.getY() + 1, c.getZ() + 0.5);
        later(20, () -> {
            boss.setNoAi(true);
            BossCredit.testContribute(boss, awayId);
            boss.setHealth(1f); boss.invulnerableTime = 0;
            boss.hurt(killer.damageSources().playerAttack(killer), 500f);
            later(10, () -> {
                var pend = com.matt.quantumfalcon.story.PendingBossCredit.get(level.getServer());
                int owed = pend.pending(awayId);
                FakePlayer back = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(awayId, "QFCameBack"));
                back.getInventory().clearContent();
                boolean before = PlayerStoryData.get(back).hasDefeatedBoss("QUANTUM_OVERSEER");
                int paid = pend.payOut(back);
                boolean after = PlayerStoryData.get(back).hasDefeatedBoss("QUANTUM_OVERSEER");
                boolean eye = back.getInventory().countItem(com.matt.quantumfalcon.registry.ModItems.OVERSEERS_EYE.get()) == 1;
                boolean cleared = pend.pending(awayId) == 0;
                boolean killerOk = PlayerStoryData.get(killer).hasDefeatedBoss("QUANTUM_OVERSEER");
                if (boss.isAlive()) boss.discard();
                BossCredit.TEST_PLAYERS.remove(killer.getUUID());
                com.matt.quantumfalcon.arena.fight.ArenaInteraction.endFight(level, c);
                result("pr3_offline_credit", owed == 1 && !before && paid == 1 && after && eye && cleared && killerOk,
                    "owed while away=" + owed + ", defeated before login=" + before + ", paid on login=" + paid + ", defeated after=" + after
                        + ", Overseer's Eye given=" + eye + ", ledger cleared=" + cleared + " | killer credited=" + killerOk);
            });
        });
    }
}
