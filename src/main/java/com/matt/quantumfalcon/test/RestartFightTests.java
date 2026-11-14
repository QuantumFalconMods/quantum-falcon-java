package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.arena.BossSummoner;
import com.matt.quantumfalcon.arena.fight.ArenaInteraction;
import com.matt.quantumfalcon.story.BossCredit;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/**
 * over a REAL restart: a fight with a participant is saved with the world and comes back on the next JVM with the
 * same participants and the seal standing; the participant who left (died, respawned) is let back in at the wall; with
 * nobody inside, the fight still resets. Run as a pair: restart_fight_a, stop, restart_fight_b (run_suite does this).
 */
public final class RestartFightTests {
    private RestartFightTests() {}

    static final Path STATE = Paths.get("qf_restart_fight_state.txt");   // server cwd (run/)
    static final int RX = 64000, RZ = 64000;
    static final String WHO = "QFRestartFighter";
    static final UUID WHO_ID = UUID.fromString("00000000-0000-4000-8000-00000000f147");

    /** The same fighter on both JVMs (fixed UUID), registered as a test player. */
    static FakePlayer fighter(ServerLevel level) {
        FakePlayer fp = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(WHO_ID, WHO));
        fp.getInventory().clearContent();
        BossCredit.TEST_PLAYERS.put(fp.getUUID(), fp);
        fp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return fp;
    }

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("restart_fight_a", RestartFightTests::a);
        tests.put("restart_fight_b", RestartFightTests::b);
    }

    private static void a(ServerLevel level) {
        BlockPos c = BossFightTests.site(level, RX, RZ);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, ArenaType.VOID_HORROR);
        if (boss == null) { result("restart_fight_a", false, "summon failed"); return; }
        later(40, () -> {
            boss.getPersistentData().putBoolean("qf_intro", true);
            ArenaInteraction.Fight f = ArenaInteraction.startFight(level, boss);
            if (f == null) { result("restart_fight_a", false, "no fight"); return; }
            ArenaInteraction.finishSeal(level, f);
            FakePlayer fp = fighter(level);
            BlockPos in = ArenaInteraction.floorAt(level, c.getX() + 5, c.getY(), c.getZ());
            if (in == null) in = c;
            fp.moveTo(in.getX() + 0.5, in.getY() + 1, in.getZ() + 0.5);
            later(30, () -> {
                boolean part = f.participants.contains(fp.getUUID());
                int seals = BossFightTests.sealCount(level, f);
                String line = f.center.getX() + " " + f.center.getY() + " " + f.center.getZ() + " " + boss.getUUID() + " " + f.radius + " " + seals + " " + fp.getUUID();
                try { Files.writeString(STATE, line); } catch (Exception ex) { result("restart_fight_a", false, "state " + ex); return; }
                // the fighter stays registered and inside, so the fight can't reset before the server stops
                result("restart_fight_a", part && seals > 0, "fight up, fighter is a participant=" + part + " seals=" + seals + " | saved " + line);
            });
        });
    }

    private static void b(ServerLevel level) {
        String[] s;
        try { s = Files.readString(STATE).trim().split(" "); } catch (Exception ex) { result("restart_fight_b", false, "no state (run restart_fight_a first) " + ex); return; }
        BlockPos c = new BlockPos(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]));
        UUID id = UUID.fromString(s[3]); int r = Integer.parseInt(s[4]); int sealsBefore = Integer.parseInt(s[5]); UUID who = UUID.fromString(s[6]);
        // straight after start, before the arena even ticks: the fight is back from the save
        ArenaInteraction.Fight early = ArenaInteraction.FIGHTS.get(c);
        boolean restored = early != null && id.equals(early.boss);
        boolean partKept = early != null && early.participants.contains(who);
        BossFightTests.site(level, c.getX(), c.getZ());
        // waits past the 30 s empty timer: nobody can be inside right after a restart, the login grace keeps the fight
        WorldDropsTests.whenTicking(level, c, 0, () -> later(ArenaInteraction.EMPTY_RESET_TICKS + 100, () -> {
            StringBuilder d = new StringBuilder("REAL restart: fight restored at start=" + restored + " participant kept=" + partKept);
            ArenaInteraction.Fight f = ArenaInteraction.FIGHTS.get(c);
            int seals = f == null ? -1 : BossFightTests.sealCount(level, f);
            boolean bossThere = level.getEntity(id) instanceof Mob;
            d.append(" | 35 s after load (login grace): fight=").append(f != null).append(" boss loaded=").append(bossThere)
             .append(" seals standing=").append(seals).append(" (before ").append(sealsBefore).append(")");
            // the same fighter (re-created: test players are not saved) walks up to the wall from outside: let back in
            FakePlayer fp = fighter(level);
            boolean sameId = fp.getUUID().equals(who);
            BlockPos out = ArenaInteraction.floorAt(level, c.getX() + r + 2, c.getY(), c.getZ());
            BlockPos op = out != null ? out.above() : new BlockPos(c.getX() + r + 2, c.getY(), c.getZ());
            fp.moveTo(op.getX() + 0.5, op.getY(), op.getZ() + 0.5);
            later(40, () -> {
                double dist = Math.hypot(fp.getX() - c.getX() - 0.5, fp.getZ() - c.getZ() - 0.5);
                boolean letIn = dist < r;
                d.append(" | fighter (same id=").append(sameId).append(") at the wall let back in=").append(letIn).append(String.format(" (d=%.1f)", dist));
                // everyone leaves: the restored fight still resets
                fp.moveTo(c.getX() + 400.5, c.getY(), c.getZ() + 0.5);
                BossCredit.TEST_PLAYERS.remove(fp.getUUID());
                later(ArenaInteraction.EMPTY_RESET_TICKS + 80, () -> {
                    boolean reset = !ArenaInteraction.FIGHTS.containsKey(c);
                    d.append(" | nobody inside: fight reset=").append(reset);
                    if (level.getEntity(id) != null) level.getEntity(id).discard();
                    ArenaInteraction.endFight(level, c);
                    CheeseTests.unforceSite(level, c);
                    try { Files.deleteIfExists(STATE); } catch (Exception ignored) {}
                    boolean ok = restored && partKept && f != null && bossThere && seals > 0 && seals >= sealsBefore * 9 / 10 && sameId && letIn && reset;
                    result("restart_fight_b", ok, d.toString());
                });
            });
        }));
    }
}
