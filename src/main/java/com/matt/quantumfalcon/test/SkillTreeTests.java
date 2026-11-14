package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.blockentity.FalconEggBlockEntity;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.skill.PlayerSkillData;
import com.matt.quantumfalcon.skill.SkillEffects;
import com.matt.quantumfalcon.skill.SkillPointSystem;
import com.matt.quantumfalcon.skill.SkillTreeLayout;
import com.matt.quantumfalcon.skill.SkillTreeSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/**
 * Stage E skill tree. skill_nodes_live runs one check per node and fails if any node has no check (B3); skill_tooltips
 * checks every tooltip is filled from SkillEffects (B4); skill_economy, skill_lineage_gate, skill_migration and
 * skill_connectors cover B5, B6, W18 and NS1.
 */
final class SkillTreeTests {
    private SkillTreeTests() {}

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("skill_nodes_live", SkillTreeTests::nodesLive);
        tests.put("skill_tooltips", SkillTreeTests::tooltips);
        tests.put("skill_economy", SkillTreeTests::economy);
        tests.put("skill_lineage_gate", SkillTreeTests::lineageGate);
        tests.put("skill_migration", SkillTreeTests::migration);
        tests.put("skill_connectors", SkillTreeTests::connectors);
        tests.put("skill_v6_sprites", SkillTreeTests::v6Sprites);
    }

    /** One fixture per node: a fresh player on a pad, registered as a test owner, with an owned falcon 2 blocks away. */
    record Fx(ServerLevel level, FakePlayer fp, BlockPos p, QuantumFalconEntity f) {
        void grant(String id) { PlayerSkillData.get(fp).unlockSkillAdmin(id); }
        void revoke() { PlayerSkillData.get(fp).resetSkills(); }
        Mob dummy(EntityType<? extends Mob> type, double dx) {
            Mob m = type.create(level);
            m.moveTo(p.getX() + 0.5 + dx, p.getY(), p.getZ() + 0.5, 0, 0);
            m.setNoAi(true); m.setPersistenceRequired();
            var hp = m.getAttribute(Attributes.MAX_HEALTH); if (hp != null) hp.setBaseValue(200);
            var ar = m.getAttribute(Attributes.ARMOR); if (ar != null) ar.setBaseValue(0);
            m.setHealth(200);
            level.addFreshEntity(m);
            return m;
        }
        float hit(LivingEntity t, net.minecraft.world.damagesource.DamageSource src, float amount) {
            t.invulnerableTime = 0;
            float before = t.getHealth();
            t.hurt(src, amount);
            return before - t.getHealth();
        }
    }

    static Fx fixture(ServerLevel level, int slot, boolean withFalcon) {
        BlockPos p = pad(level, 5200 + slot * 20, 5200, Blocks.STONE);
        cleanup(level, p, 12);
        FakePlayer fp = AuditTests.freshPlayer(level, "QFSkill" + slot);
        PlayerSkillData.get(fp).resetSkills();
        fp.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        SkillEffects.TEST_OWNERS.put(fp.getUUID(), fp);
        QuantumFalconEntity f = null;
        if (withFalcon) {
            f = falcon(level, p.offset(2, 0, 0), true);
            f.setVariant("peregrine");
            f.setTame(true, false);
            f.setOwnerUUID(fp.getUUID());
            f.setStamina(QuantumFalconEntity.MAX_STAMINA);
            f.setAbilityCooldown(0);
        }
        return new Fx(level, fp, p, f);
    }

    static void done(Fx x) {
        cleanup(x.level, x.p, 30);
        SkillEffects.TEST_OWNERS.remove(x.fp.getUUID());
    }

    // ------------------------------------------------------------------ B3: every node does what it says
    static final Map<String, Function<ServerLevel, String>> CHECKS = new LinkedHashMap<>();
    static int slot = 0;
    static Fx fx(ServerLevel l) { return fixture(l, slot++ % 24, true); }

    static {
        CHECKS.put("bond_kinship", l -> { Fx x = fx(l);
            x.f.setBondLevel(0); for (int i = 0; i < 8; i++) x.f.addBondLevel(1, x.fp); int plain = x.f.getBondLevel();
            x.f.setBondLevel(0); x.grant("bond_kinship"); for (int i = 0; i < 8; i++) x.f.addBondLevel(1, x.fp); int skilled = x.f.getBondLevel();
            done(x); return plain == 8 && skilled == 10 ? null : "8 bond gains: " + plain + " plain, " + skilled + " skilled (want 8 / 10)"; });
        CHECKS.put("bond_vigor", l -> { Fx x = fx(l);
            float base = x.f.getMaxHealth(); x.grant("bond_vigor"); SkillEffects.playerSecond(x.fp); float up = x.f.getMaxHealth();
            x.revoke(); SkillEffects.playerSecond(x.fp); float back = x.f.getMaxHealth();
            done(x); return up == base + SkillEffects.FALCON_HP_BONUS && back == base ? null : "falcon max hp " + base + " -> " + up + " -> " + back; });
        CHECKS.put("bond_tailwind", l -> { Fx x = fixture(l, slot++ % 24, false);
            x.grant("bond_tailwind"); SkillEffects.playerSecond(x.fp); boolean noFalcon = !x.fp.hasEffect(MobEffects.MOVEMENT_SPEED);   // B4: no falcon, no speed
            QuantumFalconEntity f = falcon(l, x.p.offset(3, 0, 0), true); f.setTame(true, false); f.setOwnerUUID(x.fp.getUUID());
            SkillEffects.playerSecond(x.fp); boolean withFalcon = x.fp.hasEffect(MobEffects.MOVEMENT_SPEED);
            x.fp.removeAllEffects(); done(x); return noFalcon && withFalcon ? null : "speed without falcon=" + !noFalcon + " with falcon=" + withFalcon; });
        CHECKS.put("bond_endurance", l -> { Fx x = fx(l);
            float c0 = com.matt.quantumfalcon.entity.ability.FalconAbilities.staminaCost(x.f, x.fp); x.grant("bond_endurance");
            float c1 = com.matt.quantumfalcon.entity.ability.FalconAbilities.staminaCost(x.f, x.fp);
            done(x); return c1 == c0 / 2f ? null : "active stamina cost " + c0 + " -> " + c1; });
        CHECKS.put("bond_soul", l -> { Fx x = fx(l); x.grant("bond_soul");
            x.f.hurt(l.damageSources().generic(), 999f); boolean saved = x.f.isAlive() && Math.abs(x.f.getHealth() - x.f.getMaxHealth() * SkillEffects.SOUL_HEAL_FRAC) < 0.01f;
            x.f.invulnerableTime = 0; x.f.hurt(l.damageSources().generic(), 999f); boolean cooldown = !x.f.isAlive();
            done(x); return saved && cooldown ? null : "saved=" + saved + " died on the 2nd lethal hit inside the cooldown=" + cooldown; });
        CHECKS.put("hunt_talons", l -> { Fx x = fx(l); Mob t = x.dummy(EntityType.IRON_GOLEM, -3);
            float a = x.hit(t, x.f.damageSources().mobAttack(x.f), 10f); x.grant("hunt_talons"); float b = x.hit(t, x.f.damageSources().mobAttack(x.f), 10f);
            done(x); return Math.abs(a - 10f) < 0.01f && Math.abs(b - 10f * SkillEffects.TALONS_MULT) < 0.01f ? null : "falcon hit 10 -> " + a + " plain, " + b + " skilled"; });
        CHECKS.put("hunt_dive", l -> { Fx x = fx(l);
            Mob z = x.dummy(EntityType.ZOMBIE, -3); var r0 = com.matt.quantumfalcon.entity.ability.FalconAbilities.activate(x.f, x.fp); float d0 = 200 - z.getHealth(); z.discard();
            x.f.setAbilityCooldown(0); x.f.setStamina(QuantumFalconEntity.MAX_STAMINA); x.f.moveTo(x.p.getX() + 2.5, x.p.getY(), x.p.getZ() + 0.5);
            x.grant("hunt_dive"); Mob z2 = x.dummy(EntityType.ZOMBIE, -3); var r1 = com.matt.quantumfalcon.entity.ability.FalconAbilities.activate(x.f, x.fp); float d1 = 200 - z2.getHealth();
            done(x); return r0.ok() && r1.ok() && Math.abs(d0 - SkillEffects.DIVE_BASE) < 0.01f && Math.abs(d1 - SkillEffects.DIVE_SKILLED) < 0.01f ? null
                : "Stoop Dive dealt " + d0 + " plain (" + r0.detail() + "), " + d1 + " skilled (" + r1.detail() + ")"; });
        CHECKS.put("hunt_pack", l -> { Fx x = fx(l); Mob t = x.dummy(EntityType.IRON_GOLEM, -3); x.grant("hunt_pack");
            float cold = x.hit(t, x.fp.damageSources().playerAttack(x.fp), 10f);
            x.hit(t, x.f.damageSources().mobAttack(x.f), 1f);
            float marked = x.hit(t, x.fp.damageSources().playerAttack(x.fp), 10f);
            done(x); return Math.abs(cold - 10f) < 0.01f && Math.abs(marked - 10f * SkillEffects.PACK_MULT) < 0.01f ? null : "your hit " + cold + " unmarked, " + marked + " after a falcon hit"; });
        CHECKS.put("hunt_razor", l -> { Fx x = fx(l); Mob t = x.dummy(EntityType.IRON_GOLEM, -3);
            x.hit(t, x.f.damageSources().mobAttack(x.f), 1f); boolean before = t.hasEffect(MobEffects.POISON);
            x.grant("hunt_razor"); x.hit(t, x.f.damageSources().mobAttack(x.f), 1f); boolean after = t.hasEffect(MobEffects.POISON);
            done(x); return !before && after ? null : "poison plain=" + before + " skilled=" + after; });
        CHECKS.put("hunt_execute", l -> { Fx x = fx(l); Mob t = x.dummy(EntityType.IRON_GOLEM, -3); x.grant("hunt_execute");
            float high = x.hit(t, x.f.damageSources().mobAttack(x.f), 5f); t.setHealth(20f);   // 10% of 200
            float low = x.hit(t, x.f.damageSources().mobAttack(x.f), 5f);
            done(x); return Math.abs(high - 5f) < 0.01f && Math.abs(low - 5f * SkillEffects.EXECUTE_MULT) < 0.01f ? null : "falcon hit 5 -> " + high + " at full hp, " + low + " below 20%"; });
        CHECKS.put("rift_focus", l -> { Fx x = fx(l);
            int c0 = com.matt.quantumfalcon.entity.ability.FalconAbilities.cooldownTicks(x.f, x.fp); x.grant("rift_focus");
            int c1 = com.matt.quantumfalcon.entity.ability.FalconAbilities.cooldownTicks(x.f, x.fp);
            done(x); return c1 == (int) (c0 * SkillEffects.FOCUS_COOLDOWN_MULT) || Math.abs(c1 - c0 * SkillEffects.FOCUS_COOLDOWN_MULT) <= 1 ? null : "cooldown " + c0 + " -> " + c1; });
        CHECKS.put("rift_sight", l -> { Fx x = fx(l);
            float r0 = com.matt.quantumfalcon.entity.ability.FalconAbilities.passiveRadius(x.f, x.fp); x.grant("rift_sight");
            float r1 = com.matt.quantumfalcon.entity.ability.FalconAbilities.passiveRadius(x.f, x.fp);
            done(x); return Math.abs(r1 - r0 * SkillEffects.SIGHT_MULT) < 0.01f ? null : "passive radius " + r0 + " -> " + r1; });
        CHECKS.put("rift_ward", l -> { Fx x = fx(l); Mob z = x.dummy(EntityType.ZOMBIE, -3);
            float a = SkillEffects.playerIncoming(x.fp, l.damageSources().mobAttack(z), 10f); x.grant("rift_ward");
            float b = SkillEffects.playerIncoming(x.fp, l.damageSources().mobAttack(z), 10f);
            x.f.discard(); float c = SkillEffects.playerIncoming(x.fp, l.damageSources().mobAttack(z), 10f);
            done(x); return a == 10f && Math.abs(b - 10f * SkillEffects.WARD_MULT) < 0.01f && c == 10f ? null : "damage taken " + a + " plain, " + b + " warded, " + c + " with no falcon near"; });
        CHECKS.put("rift_phase", l -> { Fx x = fx(l); Mob z = x.dummy(EntityType.ZOMBIE, -3); x.grant("rift_phase");
            int phased = 0, n = 600; for (int i = 0; i < n; i++) if (SkillEffects.playerIncoming(x.fp, l.damageSources().mobAttack(z), 4f) < 0) phased++;
            var arrowSrc = l.damageSources().arrow(EntityType.ARROW.create(l), z); int arrows = 0;
            for (int i = 0; i < 200; i++) if (SkillEffects.playerIncoming(x.fp, arrowSrc, 4f) < 0) arrows++;
            double rate = phased / (double) n;
            done(x); return rate > 0.08 && rate < 0.23 && arrows == 0 ? null : "phased " + phased + "/" + n + " melee hits, " + arrows + "/200 arrows"; });
        CHECKS.put("rift_mend", l -> { Fx x = fx(l); x.grant("rift_mend");
            x.fp.setHealth(4f); boolean m1 = SkillEffects.tryMend(x.fp); float h1 = x.fp.getHealth();
            x.fp.setHealth(4f); boolean m2 = SkillEffects.tryMend(x.fp);
            x.fp.setHealth(x.fp.getMaxHealth());
            done(x); return m1 && Math.abs(h1 - (4f + SkillEffects.MEND_HEAL)) < 0.01f && !m2 ? null : "mend=" + m1 + " hp 4 -> " + h1 + ", again inside cooldown=" + m2; });
        CHECKS.put("lin_warmth", l -> { Fx x = fx(l); BlockPos egg = x.p.offset(4, 0, 0);
            boolean before = SkillEffects.warmthNear(l, egg); x.grant("lin_warmth"); boolean near = SkillEffects.warmthNear(l, egg);
            boolean far = SkillEffects.warmthNear(l, x.p.offset(12, 0, 0));
            done(x); return !before && near && !far ? null : "egg speed-up plain=" + before + " skilled 4 blocks=" + near + " 12 blocks=" + far; });
        CHECKS.put("lin_imprint", l -> lineageHatch(l, "lin_imprint", b -> b.getBondLevel() == SkillEffects.IMPRINT_BOND, b -> "bond " + b.getBondLevel()));
        CHECKS.put("lin_brood", l -> lineageHatch(l, "lin_brood", b -> b.isBaby() && b.getAge() == SkillEffects.BROOD_AGE, b -> "age " + b.getAge()));
        CHECKS.put("lin_legacy", l -> lineageHatch(l, "lin_legacy", null, b -> "max hp " + b.getMaxHealth()));
        CHECKS.put("way_magnet", l -> { Fx x = fx(l);
            ItemEntity it = new ItemEntity(l, x.p.getX() + 6.5, x.p.getY() + 0.2, x.p.getZ() + 0.5, new ItemStack(Items.FEATHER)); it.setPickUpDelay(0); it.setNoGravity(true); it.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); l.addFreshEntity(it);
            SkillEffects.playerSecond(x.fp); double v0 = it.getDeltaMovement().x;
            x.grant("way_magnet"); SkillEffects.playerSecond(x.fp); double v1 = it.getDeltaMovement().x;
            it.discard(); done(x); return Math.abs(v0) < 1e-6 && v1 < -0.1 ? null : "item velocity towards you " + (-v0) + " plain, " + (-v1) + " skilled"; });
        CHECKS.put("way_featherfall", l -> { Fx x = fx(l);
            float a = SkillEffects.playerIncoming(x.fp, l.damageSources().fall(), 10f); x.grant("way_featherfall");
            float b = SkillEffects.playerIncoming(x.fp, l.damageSources().fall(), 10f);
            done(x); return a == 10f && Math.abs(b - 10f * SkillEffects.FALL_MULT) < 0.01f ? null : "fall damage " + a + " -> " + b; });
        CHECKS.put("way_scout", l -> { Fx x = fx(l); Mob z = x.dummy(EntityType.ZOMBIE, 10);
            SkillEffects.playerSecond(x.fp); boolean before = z.hasEffect(MobEffects.GLOWING);
            x.grant("way_scout"); SkillEffects.playerSecond(x.fp); boolean after = z.hasEffect(MobEffects.GLOWING);
            done(x); return !before && after ? null : "zombie 10 blocks away glowing plain=" + before + " skilled=" + after; });
    }

    /** Hatches two eggs owned by the fixture player (without, then with the node) and compares the hatchlings. */
    static String lineageHatch(ServerLevel l, String node, java.util.function.Predicate<QuantumFalconEntity> want, Function<QuantumFalconEntity, String> show) {
        Fx x = fx(l);
        QuantumFalconEntity plain = FalconEggBlockEntity.hatchDetached(l, x.p.offset(-2, 0, 0), new CompoundTag(), x.fp.getUUID());
        x.grant(node);
        QuantumFalconEntity skilled = FalconEggBlockEntity.hatchDetached(l, x.p.offset(-2, 0, 2), new CompoundTag(), x.fp.getUUID());
        String out;
        if (plain == null || skilled == null) out = "hatch failed";
        else if (want == null) out = Math.abs(skilled.getMaxHealth() - (plain.getMaxHealth() + SkillEffects.LEGACY_HP)) < 0.01f ? null : show.apply(plain) + " plain, " + show.apply(skilled) + " skilled";
        else out = !want.test(plain) && want.test(skilled) ? null : show.apply(plain) + " plain, " + show.apply(skilled) + " skilled";
        done(x);
        return out;
    }

    private static void nodesLive(ServerLevel level) {
        // Entity lookups only see chunks that are already entity-ticking, so force every fixture chunk first and run the checks later.
        for (int cx = (5200 - 32) >> 4; cx <= (5800 + 32) >> 4; cx++) for (int cz = (5200 - 32) >> 4; cz <= (5200 + 32) >> 4; cz++) level.setChunkForced(cx, cz, true);
        QFTestCommands.later(60, () -> nodesLiveNow(level));
    }

    private static void nodesLiveNow(ServerLevel level) {
        List<String> ok = new ArrayList<>(), bad = new ArrayList<>(), unchecked = new ArrayList<>();
        for (var s : SkillTreeSystem.getAllSkills()) {
            var check = CHECKS.get(s.id);
            if (check == null) { unchecked.add(s.id); continue; }
            String why;
            try { why = check.apply(level); } catch (Exception e) { why = "threw " + e; }
            if (why == null) ok.add(s.id); else bad.add(s.id + ": " + why);
        }
        result("skill_nodes_live", bad.isEmpty() && unchecked.isEmpty() && ok.size() == SkillTreeSystem.getTotalSkillCount(),
            ok.size() + "/" + SkillTreeSystem.getTotalSkillCount() + " nodes live" + (unchecked.isEmpty() ? "" : "; NO CHECK: " + unchecked) + (bad.isEmpty() ? "" : "; FAILED: " + bad));
    }

    // ------------------------------------------------------------------ B4: tooltips come from the constants
    private static void tooltips(ServerLevel level) {
        List<String> bad = new ArrayList<>();
        int params = 0;
        for (var s : SkillTreeSystem.getAllSkills()) {
            if (s.description.contains("{") || s.description.contains("}")) bad.add(s.id + " unfilled: " + s.description);
            var m = java.util.regex.Pattern.compile("\\{(\\w+)}").matcher(s.descTemplate);
            while (m.find()) { params++; String v = SkillEffects.PARAMS.get(m.group(1)); if (v == null || !s.description.contains(v)) bad.add(s.id + " missing " + m.group(1)); }
        }
        boolean nums = SkillEffects.PARAMS.get("talons_pct").equals("15") && SkillEffects.PARAMS.get("dive_skilled").equals("11") && SkillEffects.PARAMS.get("near").equals("16");
        result("skill_tooltips", bad.isEmpty() && nums && params > 20, params + " tooltip numbers filled from SkillEffects" + (bad.isEmpty() ? "" : "; " + bad));
    }

    // ------------------------------------------------------------------ B5: finite supply, about two thirds, a sink
    private static void economy(ServerLevel level) {
        StringBuilder d = new StringBuilder();
        int supply = SkillPointSystem.finiteSupply(), cost = SkillTreeSystem.totalCost();
        double ratio = supply / (double) cost;
        d.append("supply ").append(supply).append(" / tree cost ").append(cost).append(" = ").append(Math.round(ratio * 100)).append("%");
        boolean ratioOk = ratio >= 0.6 && ratio <= 0.75;
        // no uncapped sources left: no XP-drop listener, no advancement listener, levels pay nothing, kills pay nothing
        boolean noListeners = Arrays.stream(SkillPointSystem.class.getDeclaredMethods()).noneMatch(m -> m.getName().equals("onExperienceGain") || m.getName().equals("onAdvancement") || m.getName().equals("onEntityDeath"));
        FakePlayer fp = AuditTests.freshPlayer(level, "QFEconomy");
        var sd = PlayerSkillData.get(fp);
        int p0 = SkillPointSystem.getAvailablePoints(fp);
        sd.addXP(fp, 100000);
        boolean xpFree = SkillPointSystem.getAvailablePoints(fp) == p0;
        // once-per-player sources
        QuantumFalconEntity f1 = falcon(level, pad(level, 5800, 5200, Blocks.STONE), true), f2 = falcon(level, pad(level, 5800, 5200, Blocks.STONE).east(2), true);
        SkillPointSystem.awardMaxBondPoints(fp, f1); SkillPointSystem.awardMaxBondPoints(fp, f2);
        int bond = SkillPointSystem.getAvailablePoints(fp) - p0;
        SkillPointSystem.awardHatchMilestone(fp, 2); SkillPointSystem.awardHatchMilestone(fp, 2);
        int hatch = SkillPointSystem.getAvailablePoints(fp) - p0 - bond;
        f1.discard(); f2.discard();
        // sink: closed until every node is owned
        boolean sinkClosed = !sd.buyMasteryMark();
        for (var s : SkillTreeSystem.getAllSkills()) sd.unlockSkillAdmin(s.id);
        sd.addSkillPoints(2);
        boolean sinkOpen = sd.buyMasteryMark() && sd.getMasteryMarks() == 1;
        d.append(" no XP/advancement/kill listeners=").append(noListeners).append(" levels pay 0=").append(xpFree)
         .append(" max bond twice pays ").append(bond).append(" hatch tier 2 twice pays ").append(hatch)
         .append(" mastery mark closed before all nodes=").append(sinkClosed).append(" open after=").append(sinkOpen);
        result("skill_economy", ratioOk && noListeners && xpFree && bond == SkillPointSystem.MAX_BOND_POINTS && hatch == SkillPointSystem.HATCH_TIER_POINTS && sinkClosed && sinkOpen, d.toString());
    }

    // ------------------------------------------------------------------ B6: Lineage gated on hatched tiers
    private static void lineageGate(ServerLevel level) {
        FakePlayer fp = AuditTests.freshPlayer(level, "QFLineage");
        var sd = PlayerSkillData.get(fp);
        sd.addSkillPoints(20);
        String w0 = sd.unlockReason(fp, "lin_warmth");
        SkillPointSystem.awardHatchMilestone(fp, 1);
        boolean warm = sd.tryUnlockSkill(fp, "lin_warmth");
        String i0 = sd.unlockReason(fp, "lin_imprint");
        SkillPointSystem.awardHatchMilestone(fp, 3);
        boolean imprint = sd.tryUnlockSkill(fp, "lin_imprint");
        boolean legacyBlocked = sd.unlockReason(fp, "lin_legacy") != null;
        long lineage = SkillTreeSystem.getAllSkills().stream().filter(s -> s.category == SkillTreeSystem.Category.LINEAGE).count();
        boolean gated = SkillTreeSystem.getAllSkills().stream().filter(s -> s.category == SkillTreeSystem.Category.LINEAGE).allMatch(s -> s.hatchGate > 0);
        result("skill_lineage_gate", w0 != null && w0.contains("Common") && warm && i0 != null && i0.contains("Rare") && imprint && legacyBlocked && lineage >= 4 && gated,
            "warmth before any hatch: " + w0 + " | after tier 1 bought=" + warm + " | imprint before tier 3: " + i0 + " | after bought=" + imprint
                + " | legacy still blocked=" + legacyBlocked + " | " + lineage + " Lineage nodes, all gated=" + gated);
    }

    // ------------------------------------------------------------------ W18: old saves
    private static void migration(ServerLevel level) {
        FakePlayer fp = AuditTests.freshPlayer(level, "QFMigrate");
        var sd = PlayerSkillData.get(fp);
        int refund = sd.migrateForTest(Set.of("bond_1", "falcon_health", "attack_1", "multi_falcon", "efficient_crafting", "bulk_crafting"), 0);
        Set<String> now = sd.getUnlockedSkills();
        // multi_falcon 2 + crafting 1 + 1 refunded; bond_1/falcon_health/attack_1 map 1:1 at the same cost
        boolean ok = now.equals(Set.of("bond_kinship", "bond_vigor", "hunt_talons")) && refund == 4 && sd.getAvailableSkillPoints() == 4;
        result("skill_migration", ok, "kept " + now + ", refunded " + refund + " (want 4), points now " + sd.getAvailableSkillPoints());
    }

    // ------------------------------------------------------------------ NS1: connectors never cross another node
    private static void connectors(ServerLevel level) {
        List<String> bad = new ArrayList<>();
        int segs = 0;
        // the screen's own layout at GUI scale 3 and 4 on 1920x1080 (same formulas as SkillTreeScreen.init)
        for (int[] wh : new int[][]{{640, 360}, {480, 270}}) {
            int panelW = Math.min(360, wh[0] - 16), panelH = Math.min(250, wh[1] - 12);
            int left = (wh[0] - panelW) / 2, top = (wh[1] - panelH) / 2;
            int treeTop = top + 14 + 16 + 8, infoTop = top + panelH - 18 - 44;
            int rowH = Math.max(32, (infoTop - 4 - treeTop) / 4), colW = Math.min(80, (panelW - 24) / 3);
            var L = new SkillTreeLayout(left + (panelW - colW * 3) / 2, treeTop, colW, rowH, 24);
            if (treeTop + 3 * rowH + 24 > infoTop) bad.add("tree overlaps info strip at " + wh[0] + "x" + wh[1]);
            for (var cat : SkillTreeSystem.Category.values()) {
                var nodes = SkillTreeSystem.getSkillsByCategory(cat);
                for (var child : nodes) for (String pid : child.parents()) {
                    var parent = SkillTreeSystem.getSkill(pid);
                    for (int[] seg : L.connector(parent, child)) {
                        segs++;
                        for (var other : nodes) if (other != parent && other != child && L.crosses(seg, other)) bad.add(pid + "->" + child.id + " crosses " + other.id);
                    }
                }
            }
        }
        result("skill_connectors", bad.isEmpty() && segs > 0, segs + " connector segments checked at GUI 3 and 4" + (bad.isEmpty() ? "" : "; " + bad));
    }

    /** v6 art: every link/* and use_skill/* sprite name the screens can ask for ships (a missing one renders as the purple-black box). */
    static void v6Sprites(net.minecraft.server.level.ServerLevel level) {
        java.util.List<String> names = new java.util.ArrayList<>(), missing = new java.util.ArrayList<>();
        for (var c : com.matt.quantumfalcon.skill.SkillTreeSystem.Category.values()) {
            String br = c.name().toLowerCase(java.util.Locale.ROOT);
            names.add("link/" + br + "_joint");
            for (String st : new String[]{"owned", "active"}) for (String dot : new String[]{"", "_dotted"}) {
                names.add("link/" + br + "_v_" + st + dot);
                names.add("link/" + br + "_h_" + st + dot);
                names.add("link/" + br + "_h_" + st + "_rev" + dot);
            }
        }
        for (String n : new String[]{"link/h_locked", "link/h_locked_dotted", "link/v_locked", "link/v_locked_dotted", "link/joint_locked"}) names.add(n);
        for (var u : com.matt.quantumfalcon.skills.UseSkill.values()) { names.add("use_skill/" + u.key()); names.add("use_skill_hd/" + u.key()); }
        for (String n : names)
            if (SkillTreeTests.class.getResource("/assets/quantumfalcon/textures/gui/sprites/skill_tree/" + n + ".png") == null) missing.add(n);
        result("skill_v6_sprites", missing.isEmpty(), names.size() + " sprite names checked, missing=" + missing);
    }
}
