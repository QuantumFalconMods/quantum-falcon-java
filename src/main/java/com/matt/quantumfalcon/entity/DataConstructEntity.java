package com.matt.quantumfalcon.entity;

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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Data Construct - Digital realm enemy.
 * Appears in data dungeons and quantum structures.
 * Splits into smaller constructs when killed.
 */
public class DataConstructEntity extends Monster implements software.bernie.geckolib.animatable.GeoEntity {
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    
    private static final EntityDataAccessor<Integer> DATA_SIZE = 
        SynchedEntityData.defineId(DataConstructEntity.class, EntityDataSerializers.INT);
    
    private static final int MAX_SIZE = 3;
    private static final int MIN_SIZE = 1;

    public DataConstructEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    @Override
    protected void registerGoals() {
        EntityGoalUtils.registerBasicHostileGoals(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityAttributeBuilders.createMediumHostileAttributes()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SIZE, MAX_SIZE);
    }

    public int getConstructSize() {
        return this.entityData.get(DATA_SIZE);
    }
    
    public void setConstructSize(int size) {
        this.entityData.set(DATA_SIZE, Math.max(MIN_SIZE, Math.min(MAX_SIZE, size)));
        refreshDimensions();
        updateStats();
    }
    
    private void updateStats() {
        int size = getConstructSize();
        // Scale stats based on size
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(15.0 * size);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3.0 + size * 2);
        this.setHealth(this.getMaxHealth());
        this.xpReward = size <= MIN_SIZE ? 5 : 3;   // a size-3 family pays 4x5 + 3x3 = 29 XP, not 7x10
    }

    /** only the smallest pieces carry loot, so one spawn pays one family's worth (4 rolls), not 7. */
    @Override
    protected boolean shouldDropLoot() {
        return getConstructSize() <= MIN_SIZE && super.shouldDropLoot();
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        updateStats();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        float scale = 0.4f + (getConstructSize() * 0.3f);
        return EntityDimensions.scalable(scale, scale * 1.5f);
    }

    @Override
    public void die(DamageSource source) {
        // Split into smaller constructs on death using utility
        if (getConstructSize() > MIN_SIZE) {
            int newSize = getConstructSize() - 1;
            com.matt.quantumfalcon.entity.util.QFMobSounds.play(this, "entity_construct_split", 1.0f, 1.0f);
            EntityBehaviorUtils.splitOnDeath(this, this.getType(), 2, newSize);
        }
        
        super.die(source);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        
        // Digital glitch particles using utility
        EntityParticleUtils.spawnDigitalAmbient(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Size", getConstructSize());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setConstructSize(tag.getInt("Size"));
    }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("construct"); }

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("construct"); }

    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("construct"); }

    // ----- GeckoLib body (original model: geo/entity/mob/data_construct.geo.json) -----
    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        QFMobAnimations.register(this, "data_construct", controllers);
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        triggerAnim("action", "attack");
        boolean landed = super.doHurtTarget(target);
        if (landed) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "construct", 1f);
        return landed;
    }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.step("construct"), 0.6f, 1f); }
}
