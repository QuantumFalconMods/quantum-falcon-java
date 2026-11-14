package com.matt.quantumfalcon.entity;

import com.matt.quantumfalcon.entity.ai.ModAIGoals;
import com.matt.quantumfalcon.entity.util.EntityAttributeBuilders;
import com.matt.quantumfalcon.entity.util.EntityGoalUtils;
import com.matt.quantumfalcon.entity.util.EntityParticleUtils;
import com.matt.quantumfalcon.entity.util.EntitySoundUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Quantum Phantom - Flying hostile mob.
 * Spawns at night or in dark areas.
 * Phases through blocks briefly.
 */
public class QuantumPhantomEntity extends Monster implements software.bernie.geckolib.animatable.GeoEntity {
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    
    private int phaseTimer = 0;
    private boolean phasing = false;

    public QuantumPhantomEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.xpReward = 10;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        EntityGoalUtils.registerFlyingHostileGoals(this);
        // Add phase-through goal if needed
        // this.goalSelector.addGoal(1, new ModAIGoals.PhaseThroughGoal(this, this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityAttributeBuilders.createFlyingHostileAttributes();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        
        // Phase through blocks when hurt
        if (phaseTimer > 0) {
            phaseTimer--;
            if (phaseTimer <= 0) {
                phasing = false;
                this.noPhysics = false;
            }
        }
        
        // Ambient particles using utility
        EntityParticleUtils.spawnAmbientGlow(this, ParticleTypes.SMOKE, 10);   // ART_STYLE: sparse dark wisps (SOUL sprites read as brown blobs on the model)
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Start phasing when hurt
        if (!this.level().isClientSide && !phasing) {
            phasing = true;
            phaseTimer = 40; // 2 seconds
            this.noPhysics = true;
            
            // Quick teleport
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 10;
            double y = this.getY() + this.random.nextDouble() * 5;
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 10;
            this.teleportTo(x, y, z);
        }
        
        return super.hurt(source, amount);
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // No fall damage
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // getMobType removed in 1.21.x - undead behavior handled differently

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("phantom"); }

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("phantom"); }

    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("phantom"); }

    // ----- GeckoLib body (original model: geo/entity/mob/quantum_phantom.geo.json) -----
    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        QFMobAnimations.register(this, "quantum_phantom", controllers);
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        triggerAnim("action", "attack");
        boolean landed = super.doHurtTarget(target);
        if (landed) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "phantom", 1f);
        return landed;
    }
}
