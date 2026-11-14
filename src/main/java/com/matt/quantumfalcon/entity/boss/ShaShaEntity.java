package com.matt.quantumfalcon.entity.boss;

import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.entity.VoidEchoEntity;
import com.matt.quantumfalcon.registry.ModEntities;
import com.matt.quantumfalcon.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Sha'Sha, the Hollow Falconer: Echo Rift mini-boss (Matthew's sha_sha Blockbench model).
 *
 * <p>Lore: the echo of the first falconer who tried to tame a rift. The rift kept him; only his
 * spectral falcon still remembers his name. He wanders the echo biomes, never harms a tamed falcon,
 * and guards the Echo Key that the story needs.
 *
 * <p>Kit (readable, telegraphed): melee swipe; Echo Slam (0.6s wind-up with a particle ring, 3.5-block
 * frontal shockwave: damage, knock-up, slowness); Echo Step (1s particle telegraph, then blinks behind a
 * distant target). Below 50% HP he calls two Void Echoes once and slams more often.
 */
public class ShaShaEntity extends Monster implements GeoEntity, QFBoss {
    // bosses never ride (minecarts hauled them out of their arenas) and never take portals
    @Override protected boolean canRide(net.minecraft.world.entity.Entity vehicle) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public int qfPhase() { return (phaseTwo ? 2 : 1); }
    @Override public net.minecraft.server.level.ServerBossEvent qfBossBar() { return bossEvent; }
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.sha_sha.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.sha_sha.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.sha_sha.attack");
    private static final RawAnimation SLAM = RawAnimation.begin().thenPlay("animation.sha_sha.slam");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.sha_sha.death");

    public static final int DEATH_TICKS = 80;
    private static final int SLAM_WINDUP = 12, BLINK_WINDUP = 20;
    private static final double SLAM_RANGE = 3.5;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.quantumfalcon.sha_sha"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_6);

    private int slamCooldown = 60, slamWindup = -1, blinkCooldown = 100, blinkWindup = -1;
    private Vec3 blinkTarget = null;
    private boolean phaseTwo = false;
    // counters exposed for /qftest
    public int slamsLanded = 0, blinks = 0, echoesCalled = 0;

    public ShaShaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    /** Natural spawn rule: not peaceful, solid ground, 1-in-3 roll, and never a second Sha'Sha within 64 blocks. */
    public static boolean checkShaShaSpawnRules(EntityType<ShaShaEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (reason == MobSpawnType.NATURAL && random.nextInt(3) != 0) return false;
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) return false;
        return level.getEntitiesOfClass(ShaShaEntity.class, new AABB(pos).inflate(64)).isEmpty();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
            @Override public boolean canUse() { return slamWindup < 0 && blinkWindup < 0 && super.canUse(); }
            @Override public boolean canContinueToUse() { return slamWindup < 0 && blinkWindup < 0 && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ShaShaEntity.class, VoidEchoEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> !(p instanceof Player pl) || !"DARK".equals(com.matt.quantumfalcon.story.PlayerStoryData.get(pl).getChosenEnding())));   // story: after the Dark ending he nods instead of attacking
    }

    /** The Hollow Falconer never harms a tamed falcon (nor his own echoes). */
    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof QuantumFalconEntity f && f.isTame()) return false;
        if (target instanceof VoidEchoEntity) return false;
        return super.canAttack(target);
    }

    public boolean isPhaseTwo() { return phaseTwo; }
    public boolean isSlamming() { return slamWindup >= 0; }
    public boolean isBlinking() { return blinkWindup >= 0; }

    /** Start the telegraphed Echo Slam now (also used by /qftest). */
    public void startSlam() {
        if (slamWindup >= 0 || level().isClientSide) return;
        slamWindup = SLAM_WINDUP;
        getNavigation().stop();
        triggerAnim("action", "slam");
        if (level() instanceof ServerLevel sl) {
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI * 2 / 24;
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX() + Math.cos(a) * SLAM_RANGE, getY() + 0.1, getZ() + Math.sin(a) * SLAM_RANGE, 2, 0, 0.05, 0, 0.01);
            }
        }
    }

    /** Start an Echo Step toward a spot just behind {@code target} (also used by /qftest). */
    public void startBlink(LivingEntity target) {
        if (blinkWindup >= 0 || level().isClientSide || target == null) return;
        Vec3 look = target.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-4) look = target.position().subtract(position()).multiply(1, 0, 1);
        look = look.normalize();
        blinkTarget = target.position().subtract(look.scale(2.0));
        blinkWindup = BLINK_WINDUP;
        getNavigation().stop();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) return;
        ServerLevel sl = (ServerLevel) level();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (slamCooldown > 0) slamCooldown--;
        if (blinkCooldown > 0) blinkCooldown--;

        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && slamWindup < 0 && blinkWindup < 0) {
            double dist = distanceTo(target);
            if (dist <= SLAM_RANGE + 0.5 && slamCooldown == 0) startSlam();
            else if (dist > 8 && blinkCooldown == 0) startBlink(target);
        }
        if (slamWindup >= 0) {
            if (slamWindup-- == 0) doSlam(sl);
        }
        if (blinkWindup >= 0) {
            if (blinkTarget != null && tickCount % 2 == 0) {
                sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 6, 0.3, 0.7, 0.3, 0.2);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, blinkTarget.x, blinkTarget.y + 0.2, blinkTarget.z, 3, 0.25, 0.05, 0.25, 0.01);
            }
            if (blinkWindup-- == 0) doBlink(sl);
        }
        if (!phaseTwo && getHealth() <= getMaxHealth() * 0.5f) enterPhaseTwo(sl);
        if (tickCount % 10 == 0) sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.6, getZ(), 1, 0.3, 0.3, 0.3, 0.01);
    }

    private void doSlam(ServerLevel sl) {
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        float dmg = phaseTwo ? 12f : 10f;
        int hits = 0;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SLAM_RANGE, 1.5, SLAM_RANGE))) {
            if (e == this || !canAttack(e) || e instanceof ShaShaEntity) continue;
            Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
            double d = to.length();
            if (d > SLAM_RANGE + e.getBbWidth() / 2) continue;
            if (d > 1.5 && to.normalize().dot(fwd) < 0.2) continue;   // frontal cone, except point-blank
            if (e.hurt(damageSources().mobAttack(this), dmg)) {
                hits++;
                Vec3 push = (d < 1e-3 ? fwd : to.normalize()).scale(1.1);
                e.push(push.x, 0.55, push.z); e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
            }
        }
        if (hits > 0) slamsLanded++;
        playSound(ModSounds.SHA_SHA_SLAM.get(), 1.6f, 0.9f + random.nextFloat() * 0.2f);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, getX() + fwd.x * 2, getY() + 0.5, getZ() + fwd.z * 2, 1, 0, 0, 0, 0);
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI * 2 / 32;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(a) * 2.5, getY() + 0.1, getZ() + Math.sin(a) * 2.5, 1, 0, 0.05, 0, 0.02);
        }
        slamCooldown = phaseTwo ? 120 : 200;
    }

    private void doBlink(ServerLevel sl) {
        Vec3 dest = blinkTarget; blinkTarget = null;
        blinkCooldown = 160;
        if (dest == null) return;
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        boolean ok = false;
        for (int dy = 0; dy <= 3 && !ok; dy++) {
            Vec3 p = dest.add(0, dy, 0);
            if (sl.noCollision(this, getDimensions(getPose()).makeBoundingBox(p))) { teleportTo(p.x, p.y, p.z); ok = true; }
        }
        if (!ok) { var t = getTarget(); if (t != null) ok = randomTeleport(t.getX(), t.getY(), t.getZ(), true); }
        if (ok) {
            blinks++;
            playSound(ModSounds.SHA_SHA_BLINK.get(), 1.0f, 1.0f);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            var t = getTarget(); if (t != null) getLookControl().setLookAt(t);
        }
    }

    private void enterPhaseTwo(ServerLevel sl) {
        phaseTwo = true;
        bossEvent.setColor(BossEvent.BossBarColor.RED);
        playSound(ModSounds.BOSS_PHASE_HORN.get(), 1.5f, 1.2f);
        for (int i = 0; i < 2; i++) {
            VoidEchoEntity echo = ModEntities.VOID_ECHO.get().create(sl);
            if (echo == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            echo.moveTo(getX() + Math.cos(a) * 2.5, getY(), getZ() + Math.sin(a) * 2.5, random.nextFloat() * 360f, 0);
            net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(echo, sl, sl.getCurrentDifficultyAt(echo.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            if (getTarget() != null) echo.setTarget(getTarget());
            sl.addFreshEntity(echo);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, echo.getX(), echo.getY() + 1, echo.getZ(), 25, 0.3, 0.8, 0.3, 0.05);
            echoesCalled++;
        }
        for (ServerPlayer p : bossEvent.getPlayers())
            p.displayClientMessage(Component.translatable("message.quantumfalcon.sha_sha.phase2").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim("action", "attack");
        boolean landed = super.doHurtTarget(target);
        if (landed) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "sha_sha", 1f);
        return landed;
    }

    // ----- death: let the 4s falcon fly-away animation play -----
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (level() instanceof ServerLevel sl && deathTime % 4 == 0)
            sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.2, getZ(), 4, 0.4, 0.6, 0.4, 0.02);
        if (this.deathTime >= DEATH_TICKS && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        bossEvent.setProgress(0f);
    }

    // ----- sounds -----
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("sha_sha"); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("sha_sha"); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("sha_sha"); }
    @Override public int getAmbientSoundInterval() { return 200; }

    // ----- boss bar -----
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override public void remove(RemovalReason r) { bossEvent.removeAllPlayers(); super.remove(r); }
    @Override public void setCustomName(Component name) { super.setCustomName(name); bossEvent.setName(getDisplayName()); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PhaseTwo", phaseTwo);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phaseTwo = tag.getBoolean("PhaseTwo");
        if (hasCustomName()) bossEvent.setName(getDisplayName());
        if (phaseTwo) bossEvent.setColor(BossEvent.BossBarColor.RED);
    }

    // ----- GeckoLib -----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { controllers.add(BossDirector.presentation(this, "sha_sha"));
        controllers.add(new AnimationController<>(this, "move", 4, state -> {
            if (isDeadOrDying()) return state.setAndContinue(DEATH);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
        controllers.add(new AnimationController<>(this, "action", 2, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK).triggerableAnim("slam", SLAM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.step("sha_sha"), 0.6f, 1f); }
}
