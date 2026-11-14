package com.matt.quantumfalcon.entity.boss;

import com.matt.quantumfalcon.entity.QFMobAnimations;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
 * Eclipse Phantom (Matthew's design): 500 HP flying boss of the Shadow Realm. Three phases (66% / 33% health) with
 * rising speed and damage, a darkness aura and a Darkness Wave every 5 seconds. Movement is a custom hover-and-swoop.
 */
public class EclipsePhantomEntity extends Monster implements GeoEntity, QFBoss {
    // bosses never ride (minecarts hauled them out of their arenas) and never take portals
    @Override protected boolean canRide(net.minecraft.world.entity.Entity vehicle) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public int qfPhase() { return getPhase(); }
    @Override public net.minecraft.server.level.ServerBossEvent qfBossBar() { return bossEvent; }
    private static final ResourceLocation PHASE_MOD = ResourceLocation.fromNamespaceAndPath("quantumfalcon", "eclipse_phase");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.quantumfalcon.eclipse_phantom"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int phase = 1;
    private int waveTimer = 100;
    private int attackCooldown = 0;
    private float orbit;

    public EclipsePhantomEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 250;
        bossEvent.setDarkenScreen(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 500).add(Attributes.ATTACK_DAMAGE, 15)
                .add(Attributes.MOVEMENT_SPEED, 0.35).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ARMOR, 10)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FLYING_SPEED, 0.35);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 32f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public int getPhase() { return phase; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (tickCount % 6 == 0) level().addParticle(ParticleTypes.SMOKE, getRandomX(1.2), getRandomY(), getRandomZ(1.2), 0, -0.01, 0);   // ART_STYLE: sparse dark wisps, model stays readable
            return;
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        updatePhase();
        fly();
        if (attackCooldown > 0) attackCooldown--;
        LivingEntity t = getTarget();
        if (t != null && t.isAlive() && attackCooldown == 0 && distanceToSqr(t) < 9) {
            final LivingEntity tt = t;
            int w = BossDirector.windup(this, 0, () -> { if (tt.isAlive() && distanceToSqr(tt) < 16) doHurtTarget(tt); });
            attackCooldown = (phase == 3 ? 15 : 25) + w;
        }
        if (tickCount % 40 == 0) darknessAura();
        if (--waveTimer <= 0) { darknessWave(); waveTimer = 100; }
    }

    private void fly() {
        if (isNoAi()) { setDeltaMovement(Vec3.ZERO); return; }
        LivingEntity t = getTarget();
        Vec3 goal;
        double speed = getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (t != null && t.isAlive()) {
            orbit += 0.05f;
            boolean swoop = attackCooldown < 8;  // dive in when ready to strike, circle otherwise
            goal = swoop ? t.position().add(0, 1, 0) : t.position().add(Math.cos(orbit) * 6, 4, Math.sin(orbit) * 6);
            getLookControl().setLookAt(t, 30f, 30f);
        } else {
            goal = position().add(Math.cos(tickCount / 40.0) * 2, Math.sin(tickCount / 30.0) * 0.5, Math.sin(tickCount / 40.0) * 2);
        }
        Vec3 d = goal.subtract(position());
        Vec3 v = d.lengthSqr() > 1 ? d.normalize().scale(speed) : d.scale(0.2);
        setDeltaMovement(getDeltaMovement().scale(0.6).add(v.scale(0.4)));
        if (d.horizontalDistanceSqr() > 0.01) { setYRot((float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90f); yBodyRot = getYRot(); }
    }

    private void updatePhase() {
        float pct = getHealth() / getMaxHealth();
        int want = pct <= 0.33f ? 3 : pct <= 0.66f ? 2 : 1;
        if (want > phase) setPhase(want, true);
    }

    private void setPhase(int p, boolean announce) {
        phase = p;
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        var dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        speed.removeModifier(PHASE_MOD); dmg.removeModifier(PHASE_MOD);
        if (p >= 2) {
            speed.addPermanentModifier(new AttributeModifier(PHASE_MOD, p == 2 ? 0.10 : 0.20, AttributeModifier.Operation.ADD_VALUE));
            dmg.addPermanentModifier(new AttributeModifier(PHASE_MOD, p == 2 ? 5 : 10, AttributeModifier.Operation.ADD_VALUE));
        }
        bossEvent.setColor(p == 3 ? BossEvent.BossBarColor.RED : p == 2 ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.PURPLE);
        if (announce && level() instanceof ServerLevel sl) {
            playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_roar_eclipse"), 2f, 1.4f);
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.5, getZ(), 120, 2, 1.5, 2, 0.1);
            for (ServerPlayer pl : bossEvent.getPlayers())
                pl.displayClientMessage(Component.translatable("entity.quantumfalcon.eclipse_phantom.phase" + p), true);
        }
    }

    private void darknessAura() {
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(24)))
            if (!p.isCreative() && !p.isSpectator()) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false));
    }

    /** Darkness II + Blindness and a push to every living thing within 15 blocks. Returns the number affected. */
    public int darknessWave() {
        int n = 0;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(15), e -> e != this && e.isAlive() && !(e instanceof EclipsePhantomEntity))) {
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 1));
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40 + 20 * phase, 0));
            Vec3 push = e.position().subtract(position()).multiply(1, 0, 1);
            if (push.lengthSqr() > 1e-4) e.push(push.normalize().x * 0.8, 0.3, push.normalize().z * 0.8);
            n++;
        }
        if (level() instanceof ServerLevel sl) {
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI * 2 / 48;
                sl.sendParticles(ParticleTypes.SCULK_SOUL, getX() + Math.cos(a) * 3, getY() + 1, getZ() + Math.sin(a) * 3, 1, Math.cos(a) * 0.5, 0, Math.sin(a) * 0.5, 0.3);
            }
        }
        playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_windup_eclipse"), 1.5f, 0.6f);
        triggerAnim("action", "attack");
        return n;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "eclipse", 1f);
        if (hit) {
            triggerAnim("action", "attack");
            if (target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
        }
        return hit;
    }

    @Override public boolean causeFallDamage(float d, float m, DamageSource s) { return false; }
    @Override protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState s, net.minecraft.core.BlockPos p) { }
    @Override public boolean removeWhenFarAway(double d) { return true; }   // natural spawns despawn; summoned ones are setPersistenceRequired
    @Override public boolean canChangeDimensions(Level from, Level to) { return false; }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("eclipse"); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("eclipse"); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("eclipse"); }
    @Override public float getVoicePitch() { return 0.4f; }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override public void remove(RemovalReason r) { bossEvent.removeAllPlayers(); super.remove(r); }
    @Override public void setCustomName(Component name) { super.setCustomName(name); bossEvent.setName(getDisplayName()); }

    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("Phase", phase); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Phase")) setPhase(Math.max(1, tag.getInt("Phase")), false);
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { c.add(BossDirector.presentation(this, "eclipse_phantom")); QFMobAnimations.register(this, "eclipse_phantom", c); c.add(new software.bernie.geckolib.animation.AnimationController<>(this, "fx", 0, e -> e.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.eclipse_phantom.fx")))); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
