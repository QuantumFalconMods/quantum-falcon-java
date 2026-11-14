package com.matt.quantumfalcon.skill;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stage E skill effects. Every number a tooltip shows lives here and is read by both the effect code and
 * {@link #describe} (B4), so the two can't drift. The node list itself is data ({@code data/quantumfalcon/skill_tree/nodes.json});
 * every node has a live check in SkillTreeTests (B3).
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class SkillEffects {
    private SkillEffects() {}

    // ------------------------------------------------------------------ constants (shared by effects and tooltips)
    public static final double NEAR = 16;                       // "one of your falcons is within N blocks"
    public static final float BOND_GAIN_MULT = 1.25f;           // bond_kinship
    public static final double FALCON_HP_BONUS = 20;            // bond_vigor
    public static final double VIGOR_SYNC_RANGE = 64;           // bond_vigor applies to own falcons this close
    public static final float ENDURANCE_DRAIN_MULT = 0.6f;      // bond_endurance (flight); active cost halves (FalconAbilities)
    public static final float SOUL_HEAL_FRAC = 0.5f;            // bond_soul
    public static final int SOUL_COOLDOWN_TICKS = 20 * 60 * 20;
    public static final float TALONS_MULT = 1.15f;              // hunt_talons
    public static final float DIVE_BASE = 8f, DIVE_SKILLED = 11f;   // hunt_dive (FalconAbilities dive strike)
    public static final float PACK_MULT = 1.2f;                 // hunt_pack
    public static final int PACK_WINDOW_TICKS = 60;
    public static final int RAZOR_TICKS = 60;                   // hunt_razor (Poison I)
    public static final float EXECUTE_THRESHOLD = 0.2f, EXECUTE_MULT = 2f;  // hunt_execute
    public static final float FOCUS_COOLDOWN_MULT = 0.7f;       // rift_focus (FalconAbilities)
    public static final float SIGHT_MULT = 1.5f;                // rift_sight (FalconAbilities)
    public static final float WARD_MULT = 0.9f;                 // rift_ward
    public static final float PHASE_CHANCE = 0.15f;             // rift_phase
    public static final float MEND_THRESHOLD = 0.3f, MEND_HEAL = 6f;  // rift_mend
    public static final int MEND_COOLDOWN_TICKS = 60 * 20;
    public static final double WARMTH_RANGE = 8; public static final float WARMTH_MULT = 1.5f;   // lin_warmth (FalconEggBlockEntity)
    public static final int IMPRINT_BOND = 25;                  // lin_imprint
    public static final int BROOD_AGE = -12000;                 // lin_brood: half of vanilla's -24000 baby age
    public static final double LEGACY_HP = 4;                   // lin_legacy
    public static final double MAGNET_RANGE = 8;                // way_magnet
    public static final float FALL_MULT = 0.5f;                 // way_featherfall
    public static final double SCOUT_RANGE = 24;                // way_scout
    public static final int SCOUT_GLOW_TICKS = 60;

    private static final ResourceLocation VIGOR_ID = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "skill_bond_vigor");
    private static final ResourceLocation LEGACY_ID = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "skill_lin_legacy");

    /** Tooltip parameters, all derived from the constants above. */
    public static final Map<String, String> PARAMS = new HashMap<>();
    static {
        PARAMS.put("near", fmt(NEAR));
        PARAMS.put("bond_gain_pct", pct(BOND_GAIN_MULT - 1));
        PARAMS.put("falcon_hp", fmt(FALCON_HP_BONUS));
        PARAMS.put("sync_range", fmt(VIGOR_SYNC_RANGE));
        PARAMS.put("stamina_pct", pct(1 - ENDURANCE_DRAIN_MULT));
        PARAMS.put("soul_pct", pct(SOUL_HEAL_FRAC));
        PARAMS.put("soul_min", fmt(SOUL_COOLDOWN_TICKS / 1200.0));
        PARAMS.put("talons_pct", pct(TALONS_MULT - 1));
        PARAMS.put("dive_base", fmt(DIVE_BASE));
        PARAMS.put("dive_skilled", fmt(DIVE_SKILLED));
        PARAMS.put("pack_pct", pct(PACK_MULT - 1));
        PARAMS.put("pack_sec", fmt(PACK_WINDOW_TICKS / 20.0));
        PARAMS.put("razor_sec", fmt(RAZOR_TICKS / 20.0));
        PARAMS.put("execute_mult", fmt(EXECUTE_MULT));
        PARAMS.put("execute_pct", pct(EXECUTE_THRESHOLD));
        PARAMS.put("focus_pct", pct(1 - FOCUS_COOLDOWN_MULT));
        PARAMS.put("sight_mult", fmt(SIGHT_MULT));
        PARAMS.put("ward_pct", pct(1 - WARD_MULT));
        PARAMS.put("phase_pct", pct(PHASE_CHANCE));
        PARAMS.put("mend_pct", pct(MEND_THRESHOLD));
        PARAMS.put("mend_hp", fmt(MEND_HEAL));
        PARAMS.put("mend_sec", fmt(MEND_COOLDOWN_TICKS / 20.0));
        PARAMS.put("warmth_range", fmt(WARMTH_RANGE));
        PARAMS.put("warmth_mult", fmt(WARMTH_MULT));
        PARAMS.put("imprint_bond", Integer.toString(IMPRINT_BOND));
        PARAMS.put("legacy_hp", fmt(LEGACY_HP));
        PARAMS.put("magnet_range", fmt(MAGNET_RANGE));
        PARAMS.put("fall_pct", pct(1 - FALL_MULT));
        PARAMS.put("scout_range", fmt(SCOUT_RANGE));
    }

    static String fmt(double v) { return v == Math.rint(v) ? Long.toString(Math.round(v)) : Double.toString(Math.round(v * 100) / 100.0); }
    static String pct(double frac) { return Long.toString(Math.round(frac * 100)); }

    /** Fills {param} placeholders; an unknown placeholder is left visible so the tooltip test catches it. */
    public static String describe(String template) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            int a = template.indexOf('{', i);
            if (a < 0) { out.append(template, i, template.length()); break; }
            int b = template.indexOf('}', a);
            if (b < 0) { out.append(template, i, template.length()); break; }
            out.append(template, i, a);
            String key = template.substring(a + 1, b);
            out.append(PARAMS.getOrDefault(key, "{" + key + "}"));
            i = b + 1;
        }
        return out.toString();
    }

    // ------------------------------------------------------------------ helpers
    /** Test players (FakePlayers aren't in the level's player list, so a falcon can't look them up). */
    public static final Map<UUID, Player> TEST_OWNERS = new HashMap<>();

    public static boolean has(Player p, String id) {
        if (p == null) return false;
        PlayerSkillData sd = PlayerSkillData.get(p);
        return sd != null && sd.hasSkill(id);
    }

    public static Player ownerOf(QuantumFalconEntity f) {
        if (f == null || !f.isTame()) return null;
        if (f.getOwner() instanceof Player p) return p;
        UUID id = f.getOwnerUUID();
        return id == null ? null : TEST_OWNERS.get(id);
    }

    /** W17/B4: real count of the player's own tamed falcons within range (the old placeholder always said 1). */
    public static int ownedFalconsNear(Player p, double range) {
        return p.level().getEntitiesOfClass(QuantumFalconEntity.class, p.getBoundingBox().inflate(range),
            f -> f.isAlive() && f.isTame() && p.getUUID().equals(f.getOwnerUUID())).size();
    }

    static QuantumFalconEntity nearestOwnedFalcon(Player p, double range) {
        QuantumFalconEntity best = null; double bd = Double.MAX_VALUE;
        for (var f : p.level().getEntitiesOfClass(QuantumFalconEntity.class, p.getBoundingBox().inflate(range),
                f -> f.isAlive() && f.isTame() && p.getUUID().equals(f.getOwnerUUID()))) {
            double d = f.distanceToSqr(p);
            if (d < bd) { bd = d; best = f; }
        }
        return best;
    }

    // ------------------------------------------------------------------ bond
    /** bond_kinship: scales a positive bond gain, carrying the fraction on the falcon so +1 steps still add up. */
    public static int scaleBondGain(QuantumFalconEntity f, Player owner, int amount) {
        if (amount <= 0 || !has(owner, "bond_kinship")) return amount;
        float exact = amount * BOND_GAIN_MULT + f.getPersistentData().getFloat("qf_bond_frac");
        int whole = (int) Math.floor(exact);
        f.getPersistentData().putFloat("qf_bond_frac", exact - whole);
        return whole;
    }

    /** bond_vigor: adds or removes the max-health modifier on one falcon to match its owner's skills. Public for tests. */
    public static void syncFalconVigor(QuantumFalconEntity f, Player owner) {
        var attr = f.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        boolean want = has(owner, "bond_vigor");
        boolean hasMod = attr.getModifier(VIGOR_ID) != null;
        if (want && !hasMod) attr.addPermanentModifier(new AttributeModifier(VIGOR_ID, FALCON_HP_BONUS, AttributeModifier.Operation.ADD_VALUE));
        else if (!want && hasMod) { attr.removeModifier(VIGOR_ID); if (f.getHealth() > f.getMaxHealth()) f.setHealth(f.getMaxHealth()); }
    }

    /** Called once a second for every player (FalconSystems): tailwind, vigor sync, scout, retriever. Public for tests. */
    public static void playerSecond(Player p) {
        if (p.level().isClientSide) return;
        int near = ownedFalconsNear(p, NEAR);
        if (near > 0 && has(p, "bond_tailwind"))
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, true));
        for (var f : p.level().getEntitiesOfClass(QuantumFalconEntity.class, p.getBoundingBox().inflate(VIGOR_SYNC_RANGE),
                f -> f.isAlive() && f.isTame() && p.getUUID().equals(f.getOwnerUUID()))) syncFalconVigor(f, p);
        if (near > 0 && has(p, "way_scout")) {
            for (var e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(SCOUT_RANGE), e -> e instanceof Enemy && e.isAlive()))
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, SCOUT_GLOW_TICKS, 0, false, false));
        }
        if (near > 0 && has(p, "way_magnet")) {
            for (ItemEntity item : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(MAGNET_RANGE), i -> i.isAlive() && !i.hasPickUpDelay())) {
                var d = p.position().add(0, 0.5, 0).subtract(item.position());
                double len = d.length();
                if (len > 1.0) item.setDeltaMovement(d.scale(Math.min(0.6, 0.25 + len * 0.05) / len).add(0, 0.08, 0));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (!p.level().isClientSide && p.tickCount % 20 == 0) playerSecond(p);
    }

    /** bond_soul: a falcon that would die is saved once per cooldown. Public for tests. */
    public static boolean trySoulSave(QuantumFalconEntity f) {
        Player owner = ownerOf(f);
        if (!has(owner, "bond_soul")) return false;
        long now = f.level().getGameTime();
        if (now < owner.getPersistentData().getLong("qf_soul_ready")) return false;
        owner.getPersistentData().putLong("qf_soul_ready", now + SOUL_COOLDOWN_TICKS);
        f.setHealth(f.getMaxHealth() * SOUL_HEAL_FRAC);
        f.removeAllEffects();
        f.invulnerableTime = 40;
        owner.displayClientMessage(net.minecraft.network.chat.Component.literal("Soul Bond: " + f.getName().getString() + " clings on.")
            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide) return;
        if (e.getEntity() instanceof QuantumFalconEntity f && trySoulSave(f)) e.setCanceled(true);
    }

    // ------------------------------------------------------------------ damage (hunt / rift / wayfinder)
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide) return;
        DamageSource src = e.getSource();
        // falcon -> target: talons, execute, razor, pack mark
        if (src.getEntity() instanceof QuantumFalconEntity f && f.isTame()) {
            Player owner = ownerOf(f);
            if (owner != null && target != owner) {
                e.setAmount(falconOutgoing(f, owner, target, e.getAmount()));
                target.getPersistentData().putUUID("qf_falcon_hit_owner", owner.getUUID());
                target.getPersistentData().putLong("qf_falcon_hit_time", target.level().getGameTime());
                if (has(owner, "hunt_razor")) target.addEffect(new MobEffectInstance(MobEffects.POISON, RAZOR_TICKS, 0));
            }
        }
        // player -> target: pack strike
        if (src.getEntity() instanceof Player p && src.getDirectEntity() == p && has(p, "hunt_pack")) {
            var tag = target.getPersistentData();
            if (tag.hasUUID("qf_falcon_hit_owner") && p.getUUID().equals(tag.getUUID("qf_falcon_hit_owner"))
                    && target.level().getGameTime() - tag.getLong("qf_falcon_hit_time") <= PACK_WINDOW_TICKS)
                e.setAmount(e.getAmount() * PACK_MULT);
        }
        // -> player: phase, ward, featherfall
        if (target instanceof Player p) {
            float out = playerIncoming(p, src, e.getAmount());
            if (out < 0) e.setCanceled(true); else e.setAmount(out);
        }
    }

    /** Damage a player takes after Phase Step, Entangled Ward and Featherfall; negative = phased through. Public for
     *  tests (FakePlayers are invulnerable, so the qftest calls this rather than hurting one). */
    public static float playerIncoming(Player p, DamageSource src, float amount) {
        if (ownedFalconsNear(p, NEAR) == 0) return amount;
        boolean melee = src.getDirectEntity() instanceof LivingEntity && !src.is(DamageTypeTags.IS_PROJECTILE);
        if (melee && has(p, "rift_phase") && p.getRandom().nextFloat() < PHASE_CHANCE) return -1;
        if (has(p, "rift_ward")) amount *= WARD_MULT;
        if (src.is(DamageTypeTags.IS_FALL) && has(p, "way_featherfall")) amount *= FALL_MULT;
        return amount;
    }

    /** Falcon damage after Talon Training and Killing Blow. Public for tests. */
    public static float falconOutgoing(QuantumFalconEntity f, Player owner, LivingEntity target, float amount) {
        if (has(owner, "hunt_talons")) amount *= TALONS_MULT;
        if (has(owner, "hunt_execute") && target.getHealth() < target.getMaxHealth() * EXECUTE_THRESHOLD) amount *= EXECUTE_MULT;
        return amount;
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post e) {
        if (e.getEntity() instanceof Player p && !p.level().isClientSide) tryMend(p);
    }

    /** rift_mend: below the threshold, a nearby falcon heals the owner; cooldown per player. Public for tests. */
    public static boolean tryMend(Player p) {
        if (!p.isAlive() || !has(p, "rift_mend") || p.getHealth() >= p.getMaxHealth() * MEND_THRESHOLD) return false;
        long now = p.level().getGameTime();
        if (now < p.getPersistentData().getLong("qf_mend_ready")) return false;
        if (nearestOwnedFalcon(p, NEAR) == null) return false;
        p.getPersistentData().putLong("qf_mend_ready", now + MEND_COOLDOWN_TICKS);
        p.heal(MEND_HEAL);
        return true;
    }

    // ------------------------------------------------------------------ lineage (called from FalconEggBlockEntity.hatch)
    public static void onHatched(QuantumFalconEntity baby, Player owner) {
        if (owner == null || baby == null) return;
        if (has(owner, "lin_imprint") && baby.getBondLevel() < IMPRINT_BOND) baby.setBondLevel(IMPRINT_BOND);
        if (has(owner, "lin_brood") && baby.isBaby()) baby.setAge(BROOD_AGE);
        if (has(owner, "lin_legacy")) {
            var attr = baby.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null && attr.getModifier(LEGACY_ID) == null) {
                attr.addPermanentModifier(new AttributeModifier(LEGACY_ID, LEGACY_HP, AttributeModifier.Operation.ADD_VALUE));
                baby.setHealth(baby.getMaxHealth());
            }
        }
    }

    /** lin_warmth: an owner of the skill within range of the egg. */
    public static boolean warmthNear(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        for (Player player : level.players())
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= WARMTH_RANGE * WARMTH_RANGE && has(player, "lin_warmth")) return true;
        for (Player player : TEST_OWNERS.values())
            if (player.level() == level && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= WARMTH_RANGE * WARMTH_RANGE && has(player, "lin_warmth")) return true;
        return false;
    }
}
