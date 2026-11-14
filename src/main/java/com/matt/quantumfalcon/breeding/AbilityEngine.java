package com.matt.quantumfalcon.breeding;

import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the 42 genome abilities. Passives tick once a second; actives auto-cast in combat when their cooldown is ready
 * (the variant's signature ability stays on the falcon ability key). Cooldowns shrink with Rift Affinity.
 */
public final class AbilityEngine {
    private AbilityEngine() {}

    /** Per-falcon cooldowns in ticks, keyed by ability id. */
    public static final class Cooldowns {
        final Map<String, Integer> left = new HashMap<>();
        public int get(String id) { return left.getOrDefault(id, 0); }
        public void set(String id, int t) { left.put(id, t); }
        void tick(int dt) { left.replaceAll((k, v) -> Math.max(0, v - dt)); }
    }

    static int cooldownTicks(FalconGenome g, String id) {
        var d = BreedingEngine.abilityDef(id);
        float red = 1f - 0.4f * g.statFraction(FalconGenome.RIFT);
        return Math.max(20, (int) (d.cooldownSeconds() * 20 * red));
    }

    // ── once per second ─────────────────────────────────────────────────────────────────────────────────────────
    public static void tickSecond(QuantumFalconEntity f, FalconGenome g, Cooldowns cd) {
        if (!(f.level() instanceof ServerLevel sl)) return;
        cd.tick(20);
        LivingEntity owner = f.getOwner();
        boolean near = owner != null && owner.level() == f.level() && owner.distanceToSqr(f) < 24 * 24;
        for (String id : g.abilities) {
            switch (id) {
                case "keen_eye" -> { if (near) give(owner, MobEffects.NIGHT_VISION, 260, 0); }
                case "prismatic_veil" -> { if (near) give(owner, MobEffects.NIGHT_VISION, 260, 0); }
                case "tailwind" -> { if (near && !f.isOrderedToSit()) give(owner, MobEffects.MOVEMENT_SPEED, 40, 0); }
                case "featherfall" -> { if (near && owner.fallDistance > 4) give(owner, MobEffects.SLOW_FALLING, 60, 0); }
                case "regen_down" -> { if (f.getLastHurtByMob() == null || f.tickCount - f.getLastHurtByMobTimestamp() > 100) { if (f.tickCount % 60 < 20) f.heal(1f); } }
                case "aquatic_wings" -> { if (near && owner.isInWater()) give(owner, MobEffects.WATER_BREATHING, 60, 0); }
                case "heat_shield" -> { if (near && hotNearby(sl, owner.blockPosition())) { give(owner, MobEffects.FIRE_RESISTANCE, 60, 0); give(f, MobEffects.FIRE_RESISTANCE, 60, 0); } }
                case "lucky_feather" -> { if (near) give(owner, MobEffects.LUCK, 40, 0); }
                case "forager" -> { if (near && owner instanceof Player p) forage(sl, f, p); }
                case "treasure_sense" -> { if (near && cd.get(id) == 0 && owner instanceof Player p) { treasure(sl, p); cd.set(id, cooldownTicks(g, id)); } }
                case "scout" -> { if (near && cd.get(id) == 0 && mark(sl, owner, 24, 160) > 0) cd.set(id, cooldownTicks(g, id)); }
                case "beacon_heart" -> { if (near && cd.get(id) == 0 && owner.getAbsorptionAmount() < 1) { give(owner, MobEffects.ABSORPTION, 600, 1); cd.set(id, cooldownTicks(g, id)); burst(sl, owner.position().add(0, 1, 0), dust(0xFFD25A), 16); } }
                case "void_cloak" -> { if (near && cd.get(id) == 0 && owner.getHealth() < owner.getMaxHealth() * 0.3f) { give(owner, MobEffects.INVISIBILITY, 120, 0); cd.set(id, cooldownTicks(g, id)); burst(sl, owner.position().add(0, 1, 0), ParticleTypes.PORTAL, 30); } }
                case "time_dilation" -> { if (cd.get(id) == 0 && f.getTarget() != null) { int n = 0; for (LivingEntity e : hostiles(sl, f.position(), 8)) { give(e, MobEffects.MOVEMENT_SLOWDOWN, 80, 2); n++; } if (n > 0) { cd.set(id, cooldownTicks(g, id)); burst(sl, f.position(), dust(0x7DD8FF), 30); } } }
                default -> {}
            }
        }
        // actives auto-cast against the current target
        LivingEntity t = f.getTarget();
        if (t != null && t.isAlive() && t.distanceToSqr(f) < 16 * 16) {
            for (String id : g.abilities) {
                var d = BreedingEngine.abilityDef(id);
                if (d.cooldownSeconds() == 0 || cd.get(id) > 0) continue;
                if (cast(sl, f, g, owner, t, id)) { cd.set(id, cooldownTicks(g, id)); break; }
            }
        }
    }

    /** One active ability. Returns true if it did something. */
    public static boolean cast(ServerLevel sl, QuantumFalconEntity f, FalconGenome g, LivingEntity owner, LivingEntity t, String id) {
        float str = 1f + g.statFraction(FalconGenome.STRENGTH);
        switch (id) {
            case "swift_dive" -> { hurt(f, t, 6f * str); t.push(0, 0.4, 0); burst(sl, t.position().add(0, 1, 0), ParticleTypes.CRIT, 16); return true; }
            case "thunder_dive" -> { hurt(f, t, 9f * str); lightning(sl, t, owner); return true; }
            case "gale_burst", "tempest_eye" -> {
                Vec3 look = t.position().subtract(f.position()).normalize();
                int n = 0;
                for (LivingEntity e : hostiles(sl, f.position(), 5)) {
                    Vec3 dir = e.position().subtract(f.position()).normalize();
                    if (dir.dot(look) < 0.3) continue;
                    e.push(dir.x * 1.4, 0.5, dir.z * 1.4); e.hurtMarked = true; n++;
                    if (id.equals("tempest_eye")) give(e, MobEffects.GLOWING, 160, 0);
                }
                burst(sl, f.position().add(0, 0.5, 0), ParticleTypes.CLOUD, 20); return n > 0;
            }
            case "frost_screech" -> {
                int n = 0; for (LivingEntity e : hostiles(sl, f.position(), 6)) { give(e, MobEffects.MOVEMENT_SLOWDOWN, 120, 1); e.setTicksFrozen(Math.max(e.getTicksFrozen(), 160)); n++; }
                burst(sl, f.position(), ParticleTypes.SNOWFLAKE, 30); return n > 0;
            }
            case "thermal_shock" -> { hurt(f, t, 8f * str); give(t, MobEffects.MOVEMENT_SLOWDOWN, 80, 2); t.igniteForSeconds(3); burst(sl, t.position().add(0, 1, 0), ParticleTypes.SNOWFLAKE, 12); burst(sl, t.position().add(0, 1, 0), ParticleTypes.FLAME, 12); return true; }
            case "ember_strike" -> { t.igniteForSeconds(5); hurt(f, t, 3f * str); burst(sl, t.position().add(0, 1, 0), ParticleTypes.FLAME, 16); return true; }
            case "storm_call" -> { lightning(sl, t, owner); return true; }
            case "phase_blink" -> {
                if (owner == null || owner.distanceToSqr(f) > 24 * 24 || owner.getHealth() > owner.getMaxHealth() * 0.5f) return false;
                Vec3 to = owner.position().add(owner.getLookAngle().multiply(1, 0, 1).normalize().scale(-8));   // blink away from danger
                BlockPos bp = BlockPos.containing(to);
                if (!sl.getBlockState(bp).isAir() || !sl.getBlockState(bp.above()).isAir()) return false;
                owner.teleportTo(to.x, to.y, to.z); burst(sl, to.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 24); return true;
            }
            case "talon_grapple" -> {
                if (owner == null || owner.distanceToSqr(f) > 20 * 20 || owner.onGround()) return false;
                Vec3 d = f.position().subtract(owner.position()).normalize();
                owner.push(d.x * 1.2, Math.max(0.4, d.y), d.z * 1.2); owner.hurtMarked = true; owner.fallDistance = 0; return true;
            }
            case "gravity_well", "abyss_maw" -> {
                int n = 0;
                for (LivingEntity e : hostiles(sl, t.position(), 10)) {
                    Vec3 d = t.position().subtract(e.position()).scale(0.25);
                    e.push(d.x, 0.2, d.z); e.hurtMarked = true; n++;
                    if (id.equals("abyss_maw")) { hurt(f, e, 3f); f.heal(3f); }
                }
                burst(sl, t.position().add(0, 1, 0), ParticleTypes.PORTAL, 40); return n > 0;
            }
            case "singularity_pulse" -> {
                int n = 0; for (LivingEntity e : hostiles(sl, f.position(), 6)) { hurt(f, e, 12f * str); n++; }
                burst(sl, f.position().add(0, 0.5, 0), ParticleTypes.REVERSE_PORTAL, 60);
                sl.playSound(null, f.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.NEUTRAL, 0.6f, 1.4f); return n > 0;
            }
            case "prismatic_veil" -> {
                if (owner == null || owner.getHealth() > owner.getMaxHealth() * 0.4f) return false;
                give(owner, MobEffects.INVISIBILITY, 160, 0); give(f, MobEffects.INVISIBILITY, 160, 0); return true;
            }
            default -> { return false; }
        }
    }

    // ── on hit ──────────────────────────────────────────────────────────────────────────────────────────────────
    public static void onHit(QuantumFalconEntity f, FalconGenome g, LivingEntity t, float dealt) {
        if (!(f.level() instanceof ServerLevel sl)) return;
        float extra = 0;
        float crit = 0.2f * g.statFraction(FalconGenome.HUNT) + (g.has("crit_strike") ? 0.15f : 0f);
        if (f.getRandom().nextFloat() < crit) { extra += dealt; burst(sl, t.position().add(0, 1, 0), ParticleTypes.CRIT, 10); }
        if (g.has("razor_talons")) extra += 2;
        if (g.has("executioner") && t.getHealth() < t.getMaxHealth() * 0.25f) extra += dealt;
        if (extra > 0 && t.isAlive()) { t.invulnerableTime = 0; t.hurt(f.damageSources().mobAttack(f), extra); }
        if (g.has("bleed")) give(t, MobEffects.WITHER, 80, 0);
        if (g.has("armor_rend")) give(t, MobEffects.WEAKNESS, 100, 0);
        if (g.has("venom_beak")) give(t, MobEffects.POISON, 80, 0);
        if (g.has("vampiric")) f.heal(0.3f * (dealt + extra));
        if (g.has("entangle")) {
            for (LivingEntity e : hostiles(sl, t.position(), 5)) if (e != t) { e.invulnerableTime = 0; e.hurt(f.damageSources().mobAttack(f), dealt * 0.5f); burst(sl, e.position().add(0, 1, 0), dust(0xE07CFF), 8); break; }
        }
        if (g.has("quantum_echo") && t.isAlive()) { t.invulnerableTime = 0; t.hurt(f.damageSources().magic(), dealt * 0.5f); burst(sl, t.position().add(0, 1, 0), dust(0xFFFFFF), 8); }
    }

    /** Lethal damage check: Phoenix Rebirth / Eternal Bond. Returns true if the death was prevented. */
    public static boolean preventDeath(QuantumFalconEntity f, FalconGenome g, Cooldowns cd) {
        String id = g.has("eternal_bond") ? "eternal_bond" : g.has("phoenix_rebirth") ? "phoenix_rebirth" : null;
        if (id == null || cd.get(id) > 0 || !(f.level() instanceof ServerLevel sl)) return false;
        f.setHealth(id.equals("eternal_bond") ? f.getMaxHealth() : f.getMaxHealth() * 0.5f);
        if (id.equals("eternal_bond") && f.getOwner() != null) give(f.getOwner(), MobEffects.ABSORPTION, 600, 2);
        cd.set(id, cooldownTicks(g, id));
        burst(sl, f.position().add(0, 0.6, 0), ParticleTypes.FLAME, 40);
        sl.playSound(null, f.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.NEUTRAL, 0.8f, 1.2f);
        return true;
    }

    /** Damage multiplier for hits the falcon takes near its owner (Loyalty). */
    public static float loyaltyReduction(QuantumFalconEntity f, FalconGenome g) {
        LivingEntity o = f.getOwner();
        if (o == null || o.distanceToSqr(f) > 12 * 12) return 1f;
        return 1f - 0.3f * g.statFraction(FalconGenome.LOYALTY);
    }

    /** Rift Step: owners gain instability 40% slower when one of their falcons nearby has it. */
    public static float instabilityMultiplier(Player p) {
        for (QuantumFalconEntity f : p.level().getEntitiesOfClass(QuantumFalconEntity.class, p.getBoundingBox().inflate(32), x -> x.isOwnedBy(p)))
            if (f.getGenome().has("rift_step")) return 0.6f;
        return 1f;
    }

    /** Egg Warmer: eggs near a falcon with it incubate 25% faster. */
    public static boolean eggWarmerNear(ServerLevel sl, BlockPos pos) {
        return !sl.getEntitiesOfClass(QuantumFalconEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(6), x -> x.getGenome().has("egg_warmer")).isEmpty();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────────────────
    static List<LivingEntity> hostiles(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(r), e -> e instanceof Enemy && e.isAlive());
    }
    static int mark(ServerLevel sl, LivingEntity owner, double r, int ticks) {
        int n = 0; for (LivingEntity e : hostiles(sl, owner.position(), r)) { give(e, MobEffects.GLOWING, ticks, 0); n++; } return n;
    }
    static void give(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> eff, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(eff, ticks, amp, true, false, true));
    }
    static void hurt(QuantumFalconEntity f, LivingEntity t, float dmg) { t.invulnerableTime = 0; t.hurt(f.damageSources().mobAttack(f), dmg); }
    static void lightning(ServerLevel sl, LivingEntity t, LivingEntity owner) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt == null) return;
        bolt.moveTo(t.getX(), t.getY(), t.getZ()); bolt.setVisualOnly(true); sl.addFreshEntity(bolt);
        t.invulnerableTime = 0; t.hurt(sl.damageSources().lightningBolt(), 8f); t.igniteForSeconds(3);
    }
    static boolean hotNearby(ServerLevel sl, BlockPos p) {
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-3, -2, -3), p.offset(3, 2, 3)))
            if (sl.getBlockState(q).is(BlockTags.FIRE) || sl.getFluidState(q).is(net.minecraft.tags.FluidTags.LAVA)) return true;
        return false;
    }
    static void forage(ServerLevel sl, QuantumFalconEntity f, Player p) {
        for (ItemEntity it : sl.getEntitiesOfClass(ItemEntity.class, f.getBoundingBox().inflate(6), i -> i.isAlive() && !i.hasPickUpDelay())) {
            var stack = it.getItem().copy();
            if (p.getInventory().add(stack)) it.discard(); else it.setItem(stack);
        }
    }
    static void treasure(ServerLevel sl, Player p) {
        BlockPos c = p.blockPosition(); int n = 0;
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-12, -8, -12), c.offset(12, 8, 12))) {
            var st = sl.getBlockState(q);
            boolean hit = st.is(net.neoforged.neoforge.common.Tags.Blocks.ORES) || sl.getBlockEntity(q) instanceof ChestBlockEntity;
            if (hit && n++ < 24 && p instanceof net.minecraft.server.level.ServerPlayer sp)
                sl.sendParticles(sp, ParticleTypes.GLOW, true, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
        }
    }
    static ParticleOptions dust(int rgb) { return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 1.2f); }
    static void burst(ServerLevel sl, Vec3 p, ParticleOptions o, int n) { sl.sendParticles(o, p.x, p.y, p.z, n, 0.4, 0.4, 0.4, 0.02); }
}
