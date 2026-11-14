package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.arena.BossSummoner;
import com.matt.quantumfalcon.arena.fight.ArenaInteraction;
import com.matt.quantumfalcon.entity.boss.BossDirector;
import com.matt.quantumfalcon.entity.boss.Telegraph;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/**
 * Stage 4 movesets: for each boss move, the boss must show a ground telegraph of at least {@link #MIN_WINDUP} ticks, and the
 * move must land on exactly what the telegraph marks: a dummy standing in the zone is hit, a dummy that steps out of the
 * zone during the wind-up is not.
 */
public final class MovesetTests {
    private MovesetTests() {}

    public static final int MIN_WINDUP = 14;
    /** DAMAGE: hurts what is in the zone (dodge-checked). OTHER: telegraph and timing only (summons, player-only effects). */
    public enum Hit { DAMAGE, OTHER }
    public record Move(String id, Hit hit, Consumer<Mob> fire) {}

    static final Map<ArenaType, List<Move>> MOVES = new EnumMap<>(ArenaType.class);

    public static void moves(ArenaType type, List<Move> list) { MOVES.put(type, list); }

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        MovesetMoves.init();
        int i = 0;
        for (ArenaType t : ArenaType.values()) { final int idx = i++; tests.put("boss_moveset_" + BossFightTests.short_(t), l -> test(l, t, idx)); }
    }

    static String fmt(Vec3 v) { return String.format("%.1f,%.1f,%.1f", v.x, v.y, v.z); }

    static Zombie dummy(ServerLevel level, Vec3 at) {
        Zombie z = EntityType.ZOMBIE.create(level);
        z.moveTo(at.x, at.y, at.z, 0, 0);
        z.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.COMMAND, null);
        z.setNoAi(true); z.setPersistenceRequired(); z.setSilent(true);
        z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CARVED_PUMPKIN));   // no sunburn
        z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000); z.setHealth(1000);
        z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        level.addFreshEntity(z);
        return z;
    }

    /** A spot near {@code from} (same height, on the arena floor) that the telegraph does not cover. */
    static Vec3 escape(ServerLevel level, Telegraph t, Vec3 from, BlockPos c, Zombie z) {
        for (double r = 3; r <= 26; r += 1.5)
            for (int k = 0; k < 16; k++) {
                double a = k * Math.PI / 8;
                Vec3 p = new Vec3(from.x + Math.cos(a) * r, from.y, from.z + Math.sin(a) * r);
                if (!t.contains(p) && p.distanceTo(Vec3.atCenterOf(c)) < 24 && level.noCollision(z, z.getType().getSpawnAABB(p.x, p.y, p.z))
                    && level.getBlockState(BlockPos.containing(p).below()).isSolid()) return p;
            }
        return from.add(40, 0, 0);
    }

    static void test(ServerLevel level, ArenaType type, int idx) {
        String name = "boss_moveset_" + BossFightTests.short_(type);
        List<Move> moves = MOVES.get(type);
        if (moves == null || moves.isEmpty()) { result(name, false, "no moves registered"); return; }
        BlockPos c = BossFightTests.site(level, 12000 + idx * 400, 9000 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, type);
        if (boss == null) { result(name, false, "summon failed"); return; }
        StringBuilder d = new StringBuilder();
        List<String> bad = new ArrayList<>();
        later(30, () -> {
            boss.setNoAi(true);
            List<Mob> bosses = new ArrayList<>(List.of(boss));
            if (boss instanceof com.matt.quantumfalcon.entity.boss.EntangledTwinsEntity tw && tw.getTwin() != null) { tw.getTwin().setNoAi(true); bosses.add(tw.getTwin()); }
            BlockPos fl = ArenaInteraction.floorAt(level, c.getX() + 5, c.getY(), c.getZ());
            Vec3 home = fl != null ? Vec3.atBottomCenterOf(fl.above()) : Vec3.atBottomCenterOf(c).add(5, 0, 0);
            // the Warden arena has an r4 void pit at the centre: stand the boss on solid floor beside it, still 5 blocks from the
            // dummy, or it drops 10 blocks into the pit the moment its AI is switched on (the Sovereign hovers, so it is left alone)
            int pr = type.getVoidCenterRadius();
            BlockPos bf = pr > 0 && !boss.isNoGravity() ? ArenaInteraction.floorAt(level, c.getX() + 5, c.getY(), c.getZ() + 5) : null;
            Vec3 bpos = bf != null ? Vec3.atBottomCenterOf(bf.above()) : new Vec3(c.getX() + 0.5, home.y, c.getZ() + 0.5);
            boss.teleportTo(bpos.x, bpos.y + (boss.isNoGravity() ? 4 : 0), bpos.z);
            Zombie dz = dummy(level, home);
            boss.setTarget(dz);
            runMove(level, name, boss, dz, home, c, moves, 0, false, d, bad, () -> aiPhase(level, boss, bosses, dz, home, d, bad, () -> {
                for (Mob b : bosses) b.discard();
                dz.discard();
                level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(c).inflate(40), e -> true).forEach(Mob::discard);
                ArenaInteraction.endFight(level, c);
                result(name, bad.isEmpty(), moves.size() + " moves: " + d + (bad.isEmpty() ? "" : " | FAIL: " + String.join("; ", bad)));
            }));
        });
    }

    /** Fires move i (first standing in the zone, then for DAMAGE moves again while stepping out), then recurses. */
    static void runMove(ServerLevel level, String name, Mob boss, Zombie dz, Vec3 home, BlockPos c, List<Move> moves, int i, boolean dodgePass,
                        StringBuilder d, List<String> bad, Runnable done) {
        if (i >= moves.size()) { done.run(); return; }
        Move m = moves.get(i);
        dz.teleportTo(home.x, home.y, home.z);
        dz.setHealth(dz.getMaxHealth()); dz.removeAllEffects(); dz.clearFire(); dz.invulnerableTime = 0;
        boss.setTarget(dz);
        BossDirector.clearTelegraph(boss);
        m.fire().accept(boss);
        BossDirector.TeleRecord tr = BossDirector.lastTelegraph(boss);
        if (tr == null || !tr.move().equals(m.id())) {
            bad.add(m.id() + ": no telegraph (last=" + (tr == null ? "none" : tr.move()) + ")");
            later(10, () -> runMove(level, name, boss, dz, home, c, moves, i + 1, false, d, bad, done));
            return;
        }
        Telegraph tele = tr.tele();
        if (tr.ticks() < MIN_WINDUP) bad.add(m.id() + ": wind-up " + tr.ticks() + "t < " + MIN_WINDUP);
        boolean inside = tele != null && tele.contains(dz.position());
        if (dodgePass && tele != null) {
            Vec3 out = escape(level, tele, dz.position(), c, dz);
            later(Math.max(1, tr.ticks() / 2), () -> dz.teleportTo(out.x, out.y, out.z));   // step out halfway through the wind-up
        }
        float hp0 = dz.getHealth();
        later(tr.ticks() + 4, () -> {
            boolean hurt = dz.getHealth() < hp0 - 0.01f;
            String src = dz.getLastDamageSource() == null ? "-" : dz.getLastDamageSource().getMsgId() + "/" + (dz.getLastDamageSource().getEntity() == null ? "-" : dz.getLastDamageSource().getEntity().getType().toShortString()) + " " + String.format("%.1f", hp0 - dz.getHealth());
            if (!dodgePass) {
                d.append(m.id()).append("[").append(tr.shape()).append(", ").append(tr.ticks()).append("t");
                if (m.hit() == Hit.DAMAGE) {
                    d.append(", in zone=").append(inside).append(" hit=").append(hurt);
                    if (inside != hurt) bad.add(m.id() + ": in zone=" + inside + " but hit=" + hurt + " (" + src + ")");
                }
            } else {
                d.append(", dodged=").append(!hurt);
                if (hurt) bad.add(m.id() + ": hit after stepping out of the telegraph (" + src + ", at " + fmt(dz.position()) + ")");
            }
            boolean willDodge = !dodgePass && m.hit() == Hit.DAMAGE && inside;
            if (!willDodge) d.append("] ");
            level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(c).inflate(40), e -> e != dz && !(e instanceof com.matt.quantumfalcon.entity.boss.QFBoss)).forEach(Mob::discard);
            boolean nextDodge = willDodge;
            later(30, () -> runMove(level, name, boss, dz, home, c, moves, nextDodge ? i : i + 1, nextDodge, d, bad, done));
        });
    }

    /**
     * Review: the scripted pass fires moves by hand with the AI off. Afterwards the boss is put at 35% HP (where its full
     * move pool is open) and the AI is switched on for 30 s against the dummy: the director has to choose and telegraph at least two different moves by itself, and the dummy has to
     * take damage from the boss side.
     */
    static void aiPhase(ServerLevel level, Mob boss, List<Mob> bosses, Zombie dz, Vec3 home, StringBuilder d, List<String> bad, Runnable done) {
        dz.teleportTo(home.x, home.y, home.z);
        dz.setHealth(dz.getMaxHealth()); dz.invulnerableTime = 0;
        // wake the fight the way a player does: one real melee hit from a player (bosses aggro on player damage)
        var fp = AuditTests.freshPlayer(level, "QFMovesetHitter");
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        for (Mob b : bosses) {
            b.setNoAi(false);
            b.setHealth(b.getMaxHealth() * 0.35f);   // most moves unlock below an HP threshold (the Overseer only beams at full HP)
            fp.moveTo(b.getX() + 1.5, b.getY(), b.getZ());
            fp.resetAttackStrengthTicker(); b.invulnerableTime = 0;
            fp.attack(b);
            b.setTarget(dz);
        }
        BossDirector.clearTelegraph(boss);
        Set<String> chosen = new LinkedHashSet<>();
        float hp0 = dz.getHealth();
        pollAi(level, boss, bosses, dz, home, chosen, 0, () -> {
            boolean hurt = dz.getHealth() < hp0 - 0.01f || dz.isDeadOrDying();
            d.append("| AI on 30s: director chose ").append(chosen).append(", dummy hurt=").append(hurt).append(' ');
            if (chosen.size() < 2) bad.add("AI on: director telegraphed only " + chosen);
            if (!hurt) bad.add("AI on: nothing hit the dummy in 30 s");
            for (Mob b : bosses) b.setNoAi(true);
            done.run();
        });
    }

    private static void pollAi(ServerLevel level, Mob boss, List<Mob> bosses, Zombie dz, Vec3 home, Set<String> chosen, int t, Runnable done) {
        if (t >= 600) { done.run(); return; }
        for (Mob b : bosses) { var tr = BossDirector.lastTelegraph(b); if (tr != null) chosen.add(tr.move()); }
        for (Mob b : bosses) if (b.getTarget() != dz && dz.isAlive()) b.setTarget(dz);
        if (dz.position().distanceTo(home) > 12) dz.teleportTo(home.x, home.y, home.z);   // keep the dummy in the arena
        later(5, () -> pollAi(level, boss, bosses, dz, home, chosen, t + 5, done));
    }
}
