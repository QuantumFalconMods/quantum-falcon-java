package com.matt.quantumfalcon.entity;

import com.matt.quantumfalcon.entity.ai.ModAIGoals;
import com.matt.quantumfalcon.entity.util.EntityAttributeBuilders;
import com.matt.quantumfalcon.entity.util.EntityBehaviorUtils;
import com.matt.quantumfalcon.entity.util.EntityGoalUtils;
import com.matt.quantumfalcon.entity.util.EntityParticleUtils;
import com.matt.quantumfalcon.entity.util.EntitySoundUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/**
 * Quantum Wisp - Peaceful ambient creature.
 * Spawns in quantum biomes at night.
 * Can be captured in bottles for crafting.
 * Leads players to treasure when fed.
 * Uses GeckoLib geo/anim (quantum_wisp.geo.json, quantum_wisp.animation.json) with neon glow layer.
 */
public class QuantumWispEntity extends PathfinderMob implements FlyingAnimal, GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private static final EntityDataAccessor<Integer> DATA_COLOR = 
        SynchedEntityData.defineId(QuantumWispEntity.class, EntityDataSerializers.INT);
    
    // Colors: 0=cyan, 1=purple, 2=gold, 3=green, 4=pink
    private static final int[] COLORS = {0x00FFFF, 0x9900FF, 0xFFD700, 0x00FF00, 0xFF69B4};
    
    @Nullable
    private BlockPos treasureLocation;
    private int leadTimer = 0;

    public QuantumWispEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        EntityGoalUtils.registerFlyingAmbientGoals(this);
        // Add lead to treasure goal
        this.goalSelector.addGoal(1, new ModAIGoals.LeadToLocationGoal(this, new LeadableImpl()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityAttributeBuilders.createFlyingAmbientAttributes();
    }
    
    // Wrapper to implement Leadable interface
    private class LeadableImpl implements ModAIGoals.LeadToLocationGoal.Leadable {
        @Override
        public net.minecraft.core.BlockPos getTargetLocation() {
            return QuantumWispEntity.this.treasureLocation;
        }
        
        @Override
        public int getLeadTimer() {
            return QuantumWispEntity.this.leadTimer;
        }
        
        @Override
        public void setLeadTimer(int ticks) {
            QuantumWispEntity.this.leadTimer = ticks;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR, 0);
    }

    public int getWispColor() {
        return COLORS[this.entityData.get(DATA_COLOR) % COLORS.length];
    }
    
    public void setColorIndex(int index) {
        this.entityData.set(DATA_COLOR, index % COLORS.length);
    }

    // ========================================
    // INTERACTION
    // ========================================
    
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Capture with glass bottle
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            
            // Give wisp in a bottle (would need a custom item, using experience bottle as placeholder)
            ItemStack wispBottle = new ItemStack(Items.EXPERIENCE_BOTTLE);
            // Set custom name using DataComponents API (1.21.x)
            wispBottle.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, 
                net.minecraft.network.chat.Component.literal("Quantum Wisp Bottle"));
            
            if (!player.getInventory().add(wispBottle)) {
                player.drop(wispBottle, false);
            }
            
            this.level().playSound(null, this.blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("entity_wisp_bottle"), net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
            this.discard();
            return InteractionResult.SUCCESS;
        }
        
        // Feed glowstone dust to lead to treasure
        if (stack.is(Items.GLOWSTONE_DUST)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            
            // Find a nearby interesting location
            findTreasureLocation();
            if (treasureLocation != null) {
                leadTimer = 600; // Lead for 30 seconds
                this.level().playSound(null, this.blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("entity_wisp_ambient"), SoundSource.NEUTRAL, 1.0f, 1.5f);
            }
            return InteractionResult.SUCCESS;
        }
        
        return super.mobInteract(player, hand);
    }
    
    private void findTreasureLocation() {
        // Use utility to find treasure location
        treasureLocation = EntityBehaviorUtils.findTreasureLocation(this, 32, 100);
    }

    // ========================================
    // TICK
    // ========================================
    
    @Override
    public void aiStep() {
        super.aiStep();
        
        // Initialize color on first tick if not already set
        if (this.tickCount == 1) {
            setColorIndex(this.random.nextInt(COLORS.length));
        }
        
        if (leadTimer > 0) leadTimer--;
        
        // Glow particles using utility
        if (this.level().isClientSide) {
            EntityParticleUtils.spawnAmbientGlow(this, ParticleTypes.END_ROD, 2);
            
            // Extra particles when leading
            if (leadTimer > 0 && this.tickCount % 5 == 0) {
                double x = this.getX() + (this.random.nextDouble() - 0.5) * 0.5;
                double y = this.getY() + 0.3 + this.random.nextDouble() * 0.3;
                double z = this.getZ() + (this.random.nextDouble() - 0.5) * 0.5;
                this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.05, 0);
            }
        }
    }

    // ========================================
    // FLYING
    // ========================================
    
    @Override
    public boolean isFlying() {
        return !this.onGround();
    }
    
    public boolean isFlapping() {
        return true;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // No fall damage
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ========================================
    // SAVE/LOAD
    // ========================================
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Color", this.entityData.get(DATA_COLOR));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setColorIndex(tag.getInt("Color"));
    }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("wisp"); }

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("wisp"); }

    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("wisp"); }

    // ========================================
    // GECKOLIB (geo: quantum_wisp, anim: idle / move / death)
    // ========================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.isDeadOrDying())
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.quantum_wisp.death"));
            if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6)
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.quantum_wisp.move"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.quantum_wisp.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity target) { boolean landed = super.doHurtTarget(target); if (landed) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "wisp", 1f); return landed; }
}
