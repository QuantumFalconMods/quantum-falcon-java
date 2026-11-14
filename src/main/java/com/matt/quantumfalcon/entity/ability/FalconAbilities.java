package com.matt.quantumfalcon.entity.ability;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.registry.ModSounds;
import com.matt.quantumfalcon.skill.PlayerSkillData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Server-side logic for the 18 falcon variant abilities. Every method takes the owner explicitly so the
 * in-game /qftest suite can drive them with a FakePlayer or a stand-in mob.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class FalconAbilities {
    private FalconAbilities() {}

    public static final float BASE_PASSIVE_RADIUS = 16f;
    public static final float ACTIVE_STAMINA_COST = 20f;
    public static final int PHOENIX_REBIRTH_COOLDOWN = 20 * 60 * 10;

    public record Result(boolean ok, int affected, String detail) {
        static Result fail(String why) { return new Result(false, 0, why); }
    }

    // ------------------------------------------------------------------ modifiers (skills, perches, berries, barding)
    static boolean skill(LivingEntity owner, String id) {
        if (!(owner instanceof Player p)) return false;
        PlayerSkillData sd = PlayerSkillData.get(p);
        return sd != null && sd.hasSkill(id);
    }

    public static float passiveRadius(QuantumFalconEntity f, LivingEntity owner) {
        float r = BASE_PASSIVE_RADIUS;
        if (skill(owner, "rift_sight")) r *= com.matt.quantumfalcon.skill.SkillEffects.SIGHT_MULT;
        if (f.currentPerch() != null) r *= 2f;                 // perched falcons keep watch over a wider area
        return r;
    }

    public static int cooldownTicks(QuantumFalconEntity f, LivingEntity owner) {
        float t = f.ability().cooldownSeconds * 20f;
        if (skill(owner, "rift_focus")) t *= com.matt.quantumfalcon.skill.SkillEffects.FOCUS_COOLDOWN_MULT;
        if (f.getBerryBoostTicks() > 0) t *= 0.5f;
        t *= f.bardingCooldownMultiplier();
        if (com.matt.quantumfalcon.item.gear.FamilySetBonuses.pieces(owner, "quantum") >= 4) t *= 0.8f;   // Quantum set: Quantum Link
        return Math.max(20, Math.round(t));
    }

    public static float staminaCost(QuantumFalconEntity f, LivingEntity owner) {
        if (f.getBerryBoostTicks() > 0) return 0f;
        return skill(owner, "bond_endurance") ? ACTIVE_STAMINA_COST / 2f : ACTIVE_STAMINA_COST;
    }

    // ------------------------------------------------------------------ helpers
    static List<LivingEntity> hostiles(Level level, Vec3 c, double r, LivingEntity owner) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e instanceof Enemy && e.isAlive() && e != owner && e.position().distanceTo(c) <= r);
    }

    static LivingEntity singleTarget(QuantumFalconEntity f, LivingEntity owner, double r) {
        LivingEntity t = owner.getLastHurtMob();
        if (t != null && t.isAlive() && t.distanceTo(f) <= r && !(t instanceof QuantumFalconEntity)) return t;
        t = f.getTarget();
        if (t != null && t.isAlive() && t.distanceTo(f) <= r && t != owner) return t;
        return hostiles(f.level(), owner.position(), r, owner).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(owner))).orElse(null);
    }

    static void effect(LivingEntity e, Holder<MobEffect> eff, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(eff, ticks, amp, true, false, true));
    }

    static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }

    static void burst(ServerLevel sl, Vec3 p, ParticleOptions type, int n, double spread) {
        sl.sendParticles(type, p.x, p.y, p.z, n, spread, spread * 0.6, spread, 0.05);
    }

    static void ring(ServerLevel sl, Vec3 c, double r, ParticleOptions type) {
        for (int i = 0; i < 28; i++) {
            double a = i * Math.PI * 2 / 28;
            sl.sendParticles(type, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0, 0.02, 0, 0.01);
        }
    }

    static boolean hurt(QuantumFalconEntity f, LivingEntity target, float dmg) {
        target.invulnerableTime = 0;
        return target.hurt(f.damageSources().mobAttack(f), dmg);
    }

    static boolean safeTeleport(LivingEntity e, Vec3 p) {
        if (!e.level().noCollision(e, e.getDimensions(e.getPose()).makeBoundingBox(p))) return false;
        e.teleportTo(p.x, p.y, p.z);
        e.resetFallDistance();
        return true;
    }

    // ------------------------------------------------------------------ passives (called once a second by the falcon)
    /** Returns true if the passive did something this second. */
    public static boolean tickPassive(QuantumFalconEntity f, LivingEntity owner) {
        if (!(f.level() instanceof ServerLevel sl) || owner == null || !owner.isAlive()) return false;
        float r = passiveRadius(f, owner);
        if (owner.level() != f.level() || owner.distanceTo(f) > r) return false;
        FalconAbility a = f.ability();
        boolean did = switch (a) {
            case PEREGRINE -> { effect(owner, MobEffects.MOVEMENT_SPEED, 60, 0); yield true; }
            case PRAIRIE -> {
                int period = skill(owner, "treasure_hunter") ? 30 : 60;
                if (f.bumpPassiveTimer() >= period) { f.resetPassiveTimer(); forage(f, owner); }
                yield true;
            }
            case TUNDRA -> {
                owner.setTicksFrozen(0);
                var hs = hostiles(sl, owner.position(), 6, owner);
                hs.forEach(h -> { effect(h, MobEffects.MOVEMENT_SLOWDOWN, 60, 0); burst(sl, h.position().add(0, 1, 0), ParticleTypes.SNOWFLAKE, 3, 0.3); });
                yield true;
            }
            case STORMRIDER -> { effect(owner, MobEffects.JUMP, 60, 1); yield true; }
            case CRIMSON -> {
                if (owner.getHealth() < owner.getMaxHealth() * 0.5f) { effect(owner, MobEffects.DAMAGE_BOOST, 60, 0); yield true; }
                yield false;
            }
            case WARPED -> {
                var hs = hostiles(sl, owner.position(), 6, owner);
                hs.forEach(h -> { effect(h, MobEffects.WEAKNESS, 60, 0); burst(sl, h.position().add(0, 1, 0), ParticleTypes.WARPED_SPORE, 4, 0.3); });
                yield !hs.isEmpty();
            }
            case DEEPSLATE -> {
                boolean under = !sl.canSeeSky(owner.blockPosition()) && owner.getY() < sl.getSeaLevel();
                if (under) effect(owner, MobEffects.DIG_SPEED, 60, 0);
                yield under;
            }
            case VOIDSTALKER -> {
                boolean dark = sl.getMaxLocalRawBrightness(owner.blockPosition()) < 7;
                if (dark) effect(owner, MobEffects.NIGHT_VISION, 260, 0);
                yield dark;
            }
            case AETHERWING -> {
                if (owner.fallDistance > 3f && !owner.onGround()) { effect(owner, MobEffects.SLOW_FALLING, 60, 0); yield true; }
                yield false;
            }
            case CORAL -> {
                if (owner.isInWater()) { effect(owner, MobEffects.WATER_BREATHING, 260, 0); effect(owner, MobEffects.DOLPHINS_GRACE, 60, 0); yield true; }
                yield false;
            }
            case MUSHROOM -> {
                if (f.bumpPassiveTimer() >= 30) { f.resetPassiveTimer(); sporeSnack(owner); }
                yield true;
            }
            case ENTANGLED_PRISM -> true;   // handled in onOwnerDamaged
            case SOULFIRE -> {
                if (owner.isOnFire() || owner.isInLava()) { effect(owner, MobEffects.FIRE_RESISTANCE, 100, 0); yield true; }
                yield false;
            }
            case TEMPEST -> true;            // handled in onOwnerDamaged
            case ABYSSAL -> {
                var hs = hostiles(sl, owner.position(), 12, owner);
                hs.forEach(h -> effect(h, MobEffects.GLOWING, 60, 0));
                yield !hs.isEmpty();
            }
            case SINGULARITY -> eventHorizon(owner) > 0;
            case COSMIC -> {
                if (sl.isNight() || sl.dimensionType().hasFixedTime()) { effect(owner, MobEffects.REGENERATION, 60, 0); effect(owner, MobEffects.LUCK, 60, 0); yield true; }
                yield false;
            }
            case QUANTUM_PHOENIX -> { f.tickRebirthCooldown(20); yield true; }
        };
        if (did && f.tickCount % 40 < 20) burst(sl, f.position().add(0, 0.5, 0), dust(a.color, 1.0f), 3, 0.25);
        return did;
    }

    /** Prairie: a forage find dropped at the owner's feet. */
    public static ItemStack forage(QuantumFalconEntity f, LivingEntity owner) {
        var rnd = f.getRandom();
        ItemStack s = rnd.nextFloat() < 0.05f ? new ItemStack(ModItems.QUANTUM_BERRY.get())
                : new ItemStack(switch (rnd.nextInt(5)) { case 0 -> Items.WHEAT_SEEDS; case 1 -> Items.SWEET_BERRIES; case 2 -> Items.CARROT; case 3 -> Items.POTATO; default -> Items.BEETROOT_SEEDS; }, 1 + rnd.nextInt(2));
        ItemEntity ie = new ItemEntity(owner.level(), owner.getX(), owner.getY() + 0.5, owner.getZ(), s.copy());
        ie.setPickUpDelay(10);
        owner.level().addFreshEntity(ie);
        if (owner.level() instanceof ServerLevel sl) burst(sl, ie.position(), ParticleTypes.HAPPY_VILLAGER, 6, 0.3);
        f.playSound(ModSounds.FALCON_ABILITY_PASSIVE.get(), 0.6f, 1.2f);
        return s;
    }

    /** Mushroom: +1 hunger, +0.5 saturation. */
    public static boolean sporeSnack(LivingEntity owner) {
        if (owner instanceof Player p && p.getFoodData().getFoodLevel() < 20) {
            p.getFoodData().eat(1, 0.5f);
            if (p.level() instanceof ServerLevel sl) burst(sl, p.position().add(0, 1, 0), ParticleTypes.SPORE_BLOSSOM_AIR, 6, 0.4);
            return true;
        }
        return false;
    }

    /** Singularity: pull items and XP within 8 blocks toward the owner. Returns how many were pulled. */
    public static int eventHorizon(LivingEntity owner) {
        int n = 0;
        AABB box = owner.getBoundingBox().inflate(8);
        List<Entity> pulled = new ArrayList<>();
        pulled.addAll(owner.level().getEntitiesOfClass(ItemEntity.class, box, Entity::isAlive));
        pulled.addAll(owner.level().getEntitiesOfClass(ExperienceOrb.class, box, Entity::isAlive));
        for (Entity e : pulled) {
            Vec3 to = owner.position().add(0, 0.5, 0).subtract(e.position());
            double d = to.length();
            if (d < 1.2) continue;
            Vec3 step = to.normalize().scale(Math.min(d - 1.0, 2.5));
            e.setPos(e.getX() + step.x, e.getY() + step.y, e.getZ() + step.z);
            e.setDeltaMovement(to.normalize().scale(0.2));
            if (owner.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 0.2, e.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ actives
    /** Use the falcon's active ability (checks cooldown and stamina). */
    public static Result activate(QuantumFalconEntity f, LivingEntity owner) {
        if (!(f.level() instanceof ServerLevel sl)) return Result.fail("client");
        if (owner == null) return Result.fail("no owner");
        if (f.getAbilityCooldown() > 0) return notify(f, owner, Result.fail("cooldown " + (f.getAbilityCooldown() + 19) / 20 + "s"));
        float cost = staminaCost(f, owner);
        if (f.getStamina() < cost) return notify(f, owner, Result.fail("too tired"));
        Result r = perform(f, owner, sl);
        if (r.ok()) {
            f.setStamina(f.getStamina() - cost);
            f.setAbilityCooldown(cooldownTicks(f, owner));
            f.playSound(f.ability().sound(), 1.2f, 0.95f + f.getRandom().nextFloat() * 0.1f);
            burst(sl, f.position().add(0, 0.6, 0), dust(f.ability().color, 1.6f), 24, 0.6);
            f.triggerAbilityAnimation();
        }
        return notify(f, owner, r);
    }

    private static Result notify(QuantumFalconEntity f, LivingEntity owner, Result r) {
        if (owner instanceof Player p) {
            FalconAbility a = f.ability();
            Component msg = r.ok()
                    ? Component.empty().append(a.variantName()).append(": ").append(a.activeName()).append("!").withStyle(ChatFormatting.LIGHT_PURPLE)
                    : Component.empty().append(a.activeName()).append(Component.translatable("falcon_ability.quantumfalcon.not_ready", r.detail())).withStyle(ChatFormatting.GRAY);
            p.displayClientMessage(msg, true);
        }
        return r;
    }

    static Result perform(QuantumFalconEntity f, LivingEntity owner, ServerLevel sl) {
        Vec3 o = owner.position();
        switch (f.ability()) {
            case PEREGRINE -> {   // Stoop Dive
                LivingEntity t = singleTarget(f, owner, 16);
                if (t == null) return Result.fail("no target");
                f.teleportTo(t.getX(), t.getY() + t.getBbHeight() + 0.5, t.getZ());
                boolean hit = hurt(f, t, skill(owner, "hunt_dive") ? com.matt.quantumfalcon.skill.SkillEffects.DIVE_SKILLED : com.matt.quantumfalcon.skill.SkillEffects.DIVE_BASE);
                t.push(0, 0.5, 0); t.hurtMarked = true;
                burst(sl, t.position().add(0, 1, 0), ParticleTypes.CRIT, 20, 0.4);
                return new Result(hit, hit ? 1 : 0, "dove on " + t.getType().getDescriptionId());
            }
            case PRAIRIE -> {     // Hunt
                Animal prey = sl.getEntitiesOfClass(Animal.class, owner.getBoundingBox().inflate(16), a -> a.isAlive() && !a.isBaby()
                                && !(a instanceof QuantumFalconEntity) && !(a instanceof TamableAnimal ta && ta.isTame()))
                        .stream().min(Comparator.comparingDouble(a -> a.distanceToSqr(owner))).orElse(null);
                if (prey == null) return Result.fail("no prey");
                f.teleportTo(prey.getX(), prey.getY() + prey.getBbHeight() + 0.3, prey.getZ());
                hurt(f, prey, 100f);
                burst(sl, prey.position().add(0, 0.5, 0), ParticleTypes.CRIT, 12, 0.3);
                return new Result(!prey.isAlive(), 1, "hunted " + prey.getType().getDescriptionId());
            }
            case TUNDRA -> {      // Blizzard Burst
                var hs = hostiles(sl, o, 8, owner);
                for (LivingEntity h : hs) { hurt(f, h, 4f); effect(h, MobEffects.MOVEMENT_SLOWDOWN, 100, 3); h.setTicksFrozen(300); }
                ring(sl, o, 4, ParticleTypes.SNOWFLAKE); burst(sl, o.add(0, 1.5, 0), ParticleTypes.SNOWFLAKE, 60, 3);
                return new Result(true, hs.size(), hs.size() + " frozen");
            }
            case STORMRIDER -> {  // Thunder Call
                var hs = hostiles(sl, o, 16, owner);
                hs.sort(Comparator.comparingDouble(h -> h.distanceToSqr(owner)));
                int n = 0;
                for (LivingEntity h : hs.subList(0, Math.min(3, hs.size()))) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                    if (bolt != null) { bolt.moveTo(h.getX(), h.getY(), h.getZ()); bolt.setVisualOnly(true); sl.addFreshEntity(bolt); }
                    if (hurt(f, h, 7f)) n++;
                }
                if (hs.isEmpty()) return Result.fail("no target");
                return new Result(true, n, n + " struck");
            }
            case CRIMSON -> {     // Crimson Rend
                LivingEntity t = singleTarget(f, owner, 12);
                if (t == null) return Result.fail("no target");
                hurt(f, t, 6f); effect(t, MobEffects.WITHER, 120, 0); owner.heal(4f);
                burst(sl, t.position().add(0, 1, 0), dust(0xC0283A, 1.4f), 30, 0.4);
                burst(sl, o.add(0, 1, 0), ParticleTypes.HEART, 3, 0.4);
                return new Result(true, 1, "rend");
            }
            case WARPED -> {      // Warp Step
                Vec3 look = owner.getLookAngle().multiply(1, 0, 1);
                if (look.lengthSqr() < 1e-4) look = new Vec3(0, 0, 1);
                look = look.normalize();
                for (int d = 8; d >= 2; d--) {
                    Vec3 p = o.add(look.scale(d));
                    for (int dy = 0; dy <= 2; dy++) {
                        Vec3 q = p.add(0, dy, 0);
                        if (safeTeleport(owner, q)) {
                            burst(sl, o.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 30, 0.4);
                            burst(sl, q.add(0, 1, 0), ParticleTypes.WARPED_SPORE, 30, 0.4);
                            return new Result(true, 1, "warped " + d + " blocks");
                        }
                    }
                }
                return Result.fail("no room");
            }
            case DEEPSLATE -> {   // Ore Sense
                int r = skill(owner, "treasure_hunter") ? 18 : 12, found = 0;
                BlockPos c = owner.blockPosition();
                for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
                    if (sl.getBlockState(p).is(Tags.Blocks.ORES)) {
                        found++;
                        if (found <= 24) sl.sendParticles(ParticleTypes.GLOW, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
                    }
                }
                if (owner instanceof Player pl) pl.displayClientMessage(Component.translatable("falcon_ability.quantumfalcon.deepslate.found", found).withStyle(ChatFormatting.GOLD), false);
                return new Result(true, found, found + " ores");
            }
            case VOIDSTALKER -> { // Void Snare
                var hs = hostiles(sl, o, 10, owner);
                for (LivingEntity h : hs) {
                    Vec3 to = f.position().subtract(h.position());
                    h.push(to.x * 0.25, 0.25, to.z * 0.25); h.hurtMarked = true;
                    hurt(f, h, 4f); effect(h, MobEffects.MOVEMENT_SLOWDOWN, 80, 1);
                    burst(sl, h.position().add(0, 1, 0), ParticleTypes.PORTAL, 15, 0.3);
                }
                return new Result(true, hs.size(), hs.size() + " snared");
            }
            case AETHERWING -> {  // Updraft
                effect(owner, MobEffects.LEVITATION, 30, 3); effect(owner, MobEffects.SLOW_FALLING, 200, 0);
                burst(sl, o, ParticleTypes.CLOUD, 30, 0.6);
                return new Result(true, 1, "updraft");
            }
            case CORAL -> {       // Healing Spray
                owner.heal(6f); effect(owner, MobEffects.REGENERATION, 100, 0); owner.clearFire();
                burst(sl, o.add(0, 1, 0), ParticleTypes.SPLASH, 40, 0.6); burst(sl, o.add(0, 1, 0), ParticleTypes.HEART, 4, 0.5);
                return new Result(true, 1, "healed");
            }
            case MUSHROOM -> {    // Mycelium Bloom
                int n = 0;
                effect(owner, MobEffects.REGENERATION, 160, 1);
                for (QuantumFalconEntity ally : sl.getEntitiesOfClass(QuantumFalconEntity.class, owner.getBoundingBox().inflate(8), q -> q.isOwnedBy(owner))) effect(ally, MobEffects.REGENERATION, 160, 1);
                for (LivingEntity h : hostiles(sl, o, 6, owner)) {
                    // undead shrug off poison, so the spores weaken them instead
                    if (!h.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, true, false, true))) effect(h, MobEffects.WEAKNESS, 160, 1);
                    n++;
                }
                ring(sl, o, 3, ParticleTypes.SPORE_BLOSSOM_AIR); burst(sl, o.add(0, 0.3, 0), ParticleTypes.MYCELIUM, 40, 2);
                return new Result(true, n, n + " poisoned");
            }
            case ENTANGLED_PRISM -> { // Prism Swap
                Vec3 fp = f.position();
                f.teleportTo(o.x, o.y, o.z);
                owner.teleportTo(fp.x, fp.y, fp.z); owner.resetFallDistance();
                effect(owner, MobEffects.DAMAGE_RESISTANCE, 60, 1);
                burst(sl, o.add(0, 1, 0), dust(0xE07CFF, 1.5f), 30, 0.4); burst(sl, fp.add(0, 1, 0), dust(0x7CF3FF, 1.5f), 30, 0.4);
                return new Result(true, 1, "swapped");
            }
            case SOULFIRE -> {    // Soulfire Ring
                var hs = hostiles(sl, o, 6, owner);
                for (LivingEntity h : hs) { h.igniteForSeconds(5); hurt(f, h, 4f); }
                ring(sl, o, 3, ParticleTypes.SOUL_FIRE_FLAME); ring(sl, o, 5, ParticleTypes.SOUL_FIRE_FLAME);
                return new Result(true, hs.size(), hs.size() + " ignited");
            }
            case TEMPEST -> {     // Cyclone
                var hs = hostiles(sl, o, 8, owner);
                for (LivingEntity h : hs) {
                    Vec3 away = h.position().subtract(o).multiply(1, 0, 1);
                    away = away.lengthSqr() < 1e-3 ? new Vec3(1, 0, 0) : away.normalize();
                    h.push(away.x * 1.5, 0.6, away.z * 1.5); h.hurtMarked = true; hurt(f, h, 2f);
                }
                for (int i = 0; i < 3; i++) ring(sl, o.add(0, i, 0), 1.5 + i, ParticleTypes.CLOUD);
                return new Result(true, hs.size(), hs.size() + " blown away");
            }
            case ABYSSAL -> {     // Abyss Maw
                LivingEntity t = singleTarget(f, owner, 12);
                if (t == null) return Result.fail("no target");
                float dmg = Math.min(20f, (t.getMaxHealth() - t.getHealth()) * 0.3f + 4f);
                hurt(f, t, dmg);
                burst(sl, t.position().add(0, 1, 0), ParticleTypes.SQUID_INK, 25, 0.4);
                return new Result(true, 1, String.format("maw %.1f", dmg));
            }
            case SINGULARITY -> { // Gravity Well
                Vec3 c = f.position();
                var hs = hostiles(sl, o, 12, owner);
                for (LivingEntity h : hs) {
                    Vec3 to = c.subtract(h.position());
                    double d = to.length();
                    if (d > 1.5) h.teleportTo(h.getX() + to.x * 0.6, h.getY(), h.getZ() + to.z * 0.6);
                    hurt(f, h, 6f); effect(h, MobEffects.MOVEMENT_SLOWDOWN, 80, 2);
                }
                ring(sl, c, 2, ParticleTypes.REVERSE_PORTAL); burst(sl, c, ParticleTypes.REVERSE_PORTAL, 80, 2);
                return new Result(true, hs.size(), hs.size() + " pulled");
            }
            case COSMIC -> {      // Meteor Shower
                var hs = hostiles(sl, o, 12, owner);
                if (hs.isEmpty()) return Result.fail("no target");
                int n = 0;
                for (int i = 0; i < 5; i++) {
                    LivingEntity h = hs.get(f.getRandom().nextInt(hs.size()));
                    if (hurt(f, h, 5f)) n++;
                    for (int k = 0; k < 6; k++) sl.sendParticles(ParticleTypes.END_ROD, h.getX(), h.getY() + 6 - k, h.getZ(), 2, 0.1, 0.1, 0.1, 0);
                    sl.sendParticles(ParticleTypes.FIREWORK, h.getX(), h.getY() + 1, h.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
                }
                return new Result(true, n, n + " meteor hits");
            }
            case QUANTUM_PHOENIX -> { // Phoenix Flare
                owner.heal(8f);
                for (QuantumFalconEntity ally : sl.getEntitiesOfClass(QuantumFalconEntity.class, owner.getBoundingBox().inflate(8), q -> q.isOwnedBy(owner))) ally.heal(8f);
                var hs = hostiles(sl, o, 8, owner);
                for (LivingEntity h : hs) { h.igniteForSeconds(5); hurt(f, h, 6f); }
                ring(sl, o, 4, ParticleTypes.FLAME); burst(sl, o.add(0, 1, 0), ParticleTypes.FLAME, 50, 1.5);
                return new Result(true, hs.size(), hs.size() + " scorched");
            }
        }
        return Result.fail("unknown");
    }

    // ------------------------------------------------------------------ reactive passives
    static List<QuantumFalconEntity> ownedNearby(LivingEntity owner, double r, FalconAbility a) {
        return owner.level().getEntitiesOfClass(QuantumFalconEntity.class, owner.getBoundingBox().inflate(r),
                f -> f.isAlive() && f.isTame() && f.isOwnedBy(owner) && f.ability() == a);
    }

    /** Entangled Prism: 25% of the owner's incoming damage is taken by the falcon instead. Returns the damage left for the owner. */
    public static float entangledBond(LivingEntity owner, float amount) {
        for (QuantumFalconEntity f : ownedNearby(owner, 24, FalconAbility.ENTANGLED_PRISM)) {
            if (f.getHealth() <= 6f) continue;
            float share = amount * 0.25f;
            f.invulnerableTime = 0;
            f.hurt(f.damageSources().magic(), share);
            if (owner.level() instanceof ServerLevel sl) sl.sendParticles(dust(0xE07CFF, 1.2f), owner.getX(), owner.getY() + 1, owner.getZ(), 10, 0.3, 0.4, 0.3, 0);
            return amount - share;
        }
        return amount;
    }

    /** Tempest: melee attackers of the owner take 2 damage and are knocked back (once a second per falcon). */
    public static boolean staticCharge(LivingEntity owner, LivingEntity attacker) {
        for (QuantumFalconEntity f : ownedNearby(owner, 24, FalconAbility.TEMPEST)) {
            if (!f.tryUseStaticCharge()) continue;
            attacker.invulnerableTime = 0;
            attacker.hurt(f.damageSources().mobAttack(f), 2f);
            Vec3 away = attacker.position().subtract(owner.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) { away = away.normalize(); attacker.push(away.x * 0.8, 0.3, away.z * 0.8); attacker.hurtMarked = true; }
            if (owner.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 15, 0.3, 0.5, 0.3, 0.1);
            return true;
        }
        return false;
    }

    /** Quantum Phoenix: saves the owner from death once every 10 minutes. */
    public static boolean tryRebirth(LivingEntity owner) {
        for (QuantumFalconEntity f : ownedNearby(owner, 48, FalconAbility.QUANTUM_PHOENIX)) {
            if (f.getRebirthCooldown() > 0) continue;
            owner.setHealth(owner.getMaxHealth() * 0.5f);
            owner.removeAllEffects();
            effect(owner, MobEffects.FIRE_RESISTANCE, 200, 0);
            effect(owner, MobEffects.REGENERATION, 200, 1);
            f.setRebirthCooldown(PHOENIX_REBIRTH_COOLDOWN);
            if (owner.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLAME, owner.getX(), owner.getY() + 1, owner.getZ(), 80, 0.5, 1, 0.5, 0.1);
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, owner.getX(), owner.getY() + 1, owner.getZ(), 40, 0.5, 1, 0.5, 0.3);
                sl.playSound(null, owner.blockPosition(), ModSounds.FALCON_ABILITY_FLAME.get(), SoundSource.PLAYERS, 1.5f, 0.8f);
            }
            if (owner instanceof Player p) p.displayClientMessage(Component.translatable("falcon_ability.quantumfalcon.quantum_phoenix.rebirth").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onOwnerDamaged(LivingIncomingDamageEvent e) {
        LivingEntity owner = e.getEntity();
        if (owner.level().isClientSide || !(owner instanceof Player)) return;
        if (e.getSource().getEntity() instanceof LivingEntity attacker && e.getSource().getDirectEntity() == attacker && attacker != owner)
            staticCharge(owner, attacker);
        float left = entangledBond(owner, e.getAmount());
        if (left != e.getAmount()) e.setAmount(left);
    }

    @SubscribeEvent
    public static void onOwnerDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof Player p && !p.level().isClientSide && tryRebirth(p)) e.setCanceled(true);
    }
}
