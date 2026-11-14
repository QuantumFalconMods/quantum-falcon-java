package com.matt.quantumfalcon.entity.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Rift Warden Minion (Matthew's rift_warden_minion Blockbench model). Summoned by the Rift Warden
 * on each phase change; fragile melee adds that despawn when their Warden dies.
 */
public class RiftWardenMinionEntity extends Monster implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rift_warden_minion.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.rift_warden_minion.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.rift_warden_minion.attack");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.rift_warden_minion.hurt");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.rift_warden_minion.death");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int ownerId = -1;

    public RiftWardenMinionEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 4.0);
    }

    public void setOwnerWarden(RiftWardenEntity warden) { this.ownerId = warden.getId(); }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, RiftWardenMinionEntity.class, RiftWardenEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && ownerId >= 0 && tickCount % 20 == 0) {
            var owner = level().getEntity(ownerId);
            if (owner == null || !owner.isAlive()) { // fade out with their Warden
                ((ServerLevel) level()).sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 20, 0.3, 0.5, 0.3, 0.05);
                discard();
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        triggerAnim("action", "attack");
        boolean landed = super.doHurtTarget(target);
        if (landed) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "minion", 1f);
        return landed;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof RiftWardenEntity) return false;
        boolean r = super.hurt(source, amount);
        if (r && isAlive()) triggerAnim("action", "hurt");
        return r;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 4, state -> {
            if (isDeadOrDying()) return state.setAndContinue(DEATH);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
        controllers.add(new AnimationController<>(this, "action", 2, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK).triggerableAnim("hurt", HURT));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("minion"); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("minion"); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("minion"); }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.step("minion"), 0.6f, 1f); }
}
