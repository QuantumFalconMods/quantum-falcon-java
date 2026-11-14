package com.matt.quantumfalcon.entity;

import com.matt.quantumfalcon.entity.ai.ModAIGoals;
import com.matt.quantumfalcon.entity.util.EntityAttributeBuilders;
import com.matt.quantumfalcon.entity.util.EntityGoalUtils;
import com.matt.quantumfalcon.entity.util.EntityParticleUtils;
import com.matt.quantumfalcon.entity.util.EntitySoundUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Void Echo - Teleporting hostile mob.
 * Spawns in void biomes or after dimensional rifts.
 * Teleports behind targets for backstab attacks.
 */
public class VoidEchoEntity extends Monster implements software.bernie.geckolib.animatable.GeoEntity {
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    
    // Teleport cooldown now handled by ModAIGoals.TeleportBehindGoal

    public VoidEchoEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    @Override
    protected void registerGoals() {
        EntityGoalUtils.registerBasicHostileGoals(this);
        // Add teleport goal at higher priority
        this.goalSelector.addGoal(1, new ModAIGoals.TeleportBehindGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityAttributeBuilders.createMediumHostileAttributes()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.35)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 8.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Ambient particles using utility
        EntityParticleUtils.spawnVoidAmbient(this);
    }

    // Teleport functionality now handled by ModAIGoals.TeleportBehindGoal

    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim("action", "attack");
        boolean hit = super.doHurtTarget(target);
        if (hit) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "void_echo", 1f);
        
        // Apply weakness on hit
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }
        
        return hit;
    }

    // getMobType removed in 1.21.x - undead behavior handled differently

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("void_echo"); }

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("void_echo"); }

    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("void_echo"); }

    // ----- GeckoLib body (original model: geo/entity/mob/void_echo.geo.json) -----
    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        QFMobAnimations.register(this, "void_echo", controllers);
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.step("void_echo"), 0.6f, 1f); }
}
