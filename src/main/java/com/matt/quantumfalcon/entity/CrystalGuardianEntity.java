package com.matt.quantumfalcon.entity;

import com.matt.quantumfalcon.entity.ai.ModAIGoals;
import com.matt.quantumfalcon.entity.util.EntityAttributeBuilders;
import com.matt.quantumfalcon.entity.util.EntityBehaviorUtils;
import com.matt.quantumfalcon.entity.util.EntityGoalUtils;
import com.matt.quantumfalcon.entity.util.EntityParticleUtils;
import com.matt.quantumfalcon.entity.util.EntitySoundUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Crystal Guardian - Tanky melee defender mob.
 * Spawns near crystal formations and dungeons.
 * Reflects damage when crystallized.
 */
public class CrystalGuardianEntity extends Monster implements software.bernie.geckolib.animatable.GeoEntity {
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    
    private static final EntityDataAccessor<Boolean> DATA_CRYSTALLIZED = 
        SynchedEntityData.defineId(CrystalGuardianEntity.class, EntityDataSerializers.BOOLEAN);
    
    private int crystallizeTimer = 0;
    private int crystallizeCooldown = 0;

    public CrystalGuardianEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    @Override
    protected void registerGoals() {
        EntityGoalUtils.registerTankGoals(this);
        // Add crystallize goal at higher priority
        this.goalSelector.addGoal(1, new ModAIGoals.CrystallizeGoal(this, new CrystallizableImpl()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityAttributeBuilders.createTankAttributes();
    }
    
    // Wrapper to implement Crystallizable interface
    private class CrystallizableImpl implements ModAIGoals.CrystallizeGoal.Crystallizable {
        @Override
        public boolean isCrystallized() {
            return CrystalGuardianEntity.this.isCrystallized();
        }
        
        @Override
        public void setCrystallized(boolean crystallized) {
            CrystalGuardianEntity.this.setCrystallized(crystallized);
        }
        
        @Override
        public void setCrystallizeTimer(int ticks) {
            CrystalGuardianEntity.this.crystallizeTimer = ticks;
        }
        
        @Override
        public void setCrystallizeCooldown(int ticks) {
            CrystalGuardianEntity.this.crystallizeCooldown = ticks;
        }
        
        @Override
        public int getCrystallizeCooldown() {
            return CrystalGuardianEntity.this.crystallizeCooldown;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CRYSTALLIZED, false);
    }

    public boolean isCrystallized() {
        return this.entityData.get(DATA_CRYSTALLIZED);
    }
    
    public void setCrystallized(boolean crystallized) {
        this.entityData.set(DATA_CRYSTALLIZED, crystallized);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        
        // Crystallize timer
        if (crystallizeTimer > 0) {
            crystallizeTimer--;
            if (crystallizeTimer <= 0) {
                setCrystallized(false);
            }
        }
        
        if (crystallizeCooldown > 0) crystallizeCooldown--;
        
        // Particles when crystallized using utility
        if (isCrystallized()) {
            EntityParticleUtils.spawnCrystalAmbient(this);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Reflect damage when crystallized using utility
        if (isCrystallized()) {
            Entity attacker = source.getEntity();
            if (attacker instanceof LivingEntity living) {
                amount = EntityBehaviorUtils.reflectDamage(this, living, amount, 0.5f, 0.8f);
                EntityParticleUtils.spawnDamageReflectionParticles(this);
            }
        }
        
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Crystallized", isCrystallized());
        tag.putInt("CrystallizeTimer", crystallizeTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCrystallized(tag.getBoolean("Crystallized"));
        crystallizeTimer = tag.getInt("CrystallizeTimer");
    }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("guardian"); }

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("guardian"); } // crystallized shell: the hurt file already has the glassy layer

    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("guardian"); }

    // ----- GeckoLib body (original model: geo/entity/mob/crystal_guardian.geo.json) -----
    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        QFMobAnimations.register(this, "crystal_guardian", controllers);
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        triggerAnim("action", "attack");
        boolean landed = super.doHurtTarget(target);
        if (landed) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "guardian", 1f);
        return landed;
    }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.step("guardian"), 0.6f, 1f); }
}
