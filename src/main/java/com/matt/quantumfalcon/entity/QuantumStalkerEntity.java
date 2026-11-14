package com.matt.quantumfalcon.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Quantum Stalker (Matthew's design: 30 HP, 8 damage, 0.35 speed; phases behind its target; hits cause Weakness).
 * Found in the Echo Rift.
 */
public class QuantumStalkerEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int phaseCooldown = 60;

    public QuantumStalkerEntity(EntityType<? extends Monster> type, Level level) { super(type, level); xpReward = 10; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.ATTACK_DAMAGE, 8).add(Attributes.MOVEMENT_SPEED, 0.35).add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (tickCount % 4 == 0) level().addParticle(ParticleTypes.REVERSE_PORTAL, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
            return;
        }
        if (phaseCooldown > 0) phaseCooldown--;
        LivingEntity t = getTarget();
        if (t != null && phaseCooldown == 0 && distanceTo(t) < 16 && random.nextFloat() < 0.05f) phaseBehind(t);
    }

    /** Teleports 2.5 blocks behind the target. Returns true on success. */
    public boolean phaseBehind(LivingEntity t) {
        Vec3 back = t.getViewVector(1f).multiply(1, 0, 1);
        if (back.lengthSqr() < 1e-4) back = new Vec3(0, 0, 1);
        Vec3 q = t.position().subtract(back.normalize().scale(2.5));
        if (!level().noCollision(this, getDimensions(getPose()).makeBoundingBox(q))) return false;
        Vec3 from = position();
        teleportTo(q.x, q.y, q.z);
        lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1, from.z, 30, 0.3, 0.6, 0.3, 0.1);
            sl.sendParticles(ParticleTypes.PORTAL, q.x, q.y + 1, q.z, 30, 0.3, 0.6, 0.3, 0.1);
        }
        playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.get("entity_stalker_phase"), 1f, 0.7f);
        phaseCooldown = 160;
        return true;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "stalker", 1f);
        if (hit) {
            triggerAnim("action", "attack");
            if (target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
        return hit;
    }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("stalker"); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("stalker"); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("stalker"); }
    @Override public float getVoicePitch() { return 0.6f; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { QFMobAnimations.register(this, "quantum_stalker", c); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.step("stalker"), 0.6f, 1f); }
}
