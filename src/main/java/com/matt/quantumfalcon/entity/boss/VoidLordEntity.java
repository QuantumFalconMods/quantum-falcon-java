package com.matt.quantumfalcon.entity.boss;

import com.matt.quantumfalcon.entity.QFMobAnimations;
import com.matt.quantumfalcon.entity.VoidEchoEntity;
import com.matt.quantumfalcon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.MobSpawnType;
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
 * Void Lord (Matthew's design): 800 HP boss of the void biomes. Corruption aura (Wither + Weakness), summons Void Echo
 * minions, and below half health blinks next to its target. Immune to wither and poison.
 */
public class VoidLordEntity extends Monster implements GeoEntity, QFBoss {
    // bosses never ride (minecarts hauled them out of their arenas) and never take portals
    @Override protected boolean canRide(net.minecraft.world.entity.Entity vehicle) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public int qfPhase() { return (getHealth() < getMaxHealth() / 2 ? 2 : 1); }
    @Override public net.minecraft.server.level.ServerBossEvent qfBossBar() { return bossEvent; }
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.quantumfalcon.void_lord"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_12);
    private int summonTimer = 200;
    private int teleportCooldown = 0;

    public VoidLordEntity(EntityType<? extends Monster> type, Level level) { super(type, level); xpReward = 400; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 800).add(Attributes.ATTACK_DAMAGE, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ARMOR, 15)
                .add(Attributes.ARMOR_TOUGHNESS, 6).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, VoidEchoEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (tickCount % 2 == 0) level().addParticle(ParticleTypes.PORTAL, getRandomX(1.5), getRandomY(), getRandomZ(1.5), 0, -0.3, 0);
            return;
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 40 == 0) corruptionAura();
        if (teleportCooldown > 0) teleportCooldown--;
        LivingEntity t = getTarget();
        if (t != null && t.isAlive()) {
            if (--summonTimer <= 0) { BossDirector.windup(this, 0, this::summonMinions); summonTimer = getHealth() < getMaxHealth() / 2 ? 240 : 400; }
            if (teleportCooldown == 0 && getHealth() < getMaxHealth() / 2 && (distanceTo(t) > 8 || random.nextFloat() < 0.01f)) teleportNear(t);
        }
    }

    /** Wither I + Weakness on players within 8 blocks. Returns how many were hit. */
    public int corruptionAura() {
        int n = 0;
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(8))) {
            if (p.isCreative() || p.isSpectator()) continue;
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            n++;
        }
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 40, 4, 0.5, 4, 0.02);
        return n;
    }

    /** Summons 3-5 Void Echoes around itself, never more than 6 alive nearby. Returns how many were spawned. */
    public int summonMinions() {
        if (!(level() instanceof ServerLevel sl)) return 0;
        int existing = sl.getEntitiesOfClass(VoidEchoEntity.class, getBoundingBox().inflate(24)).size();
        int count = Math.min(3 + random.nextInt(3), Math.max(0, 6 - existing));
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            VoidEchoEntity m = ModEntities.VOID_ECHO.get().create(sl);
            if (m == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            BlockPos pos = BlockPos.containing(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3);
            m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
            net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(m, sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
            if (getTarget() != null) m.setTarget(getTarget());
            sl.addFreshEntity(m);
            sl.sendParticles(ParticleTypes.PORTAL, m.getX(), m.getY() + 1, m.getZ(), 30, 0.4, 0.8, 0.4, 0.5);
            spawned++;
        }
        if (spawned > 0) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_windup_void_lord"), 2f, 0.6f); triggerAnim("action", "attack"); }
        return spawned;
    }

    /** Blinks to a free spot 2-4 blocks from the target. Returns true on success. */
    public boolean teleportNear(LivingEntity t) {
        for (int tries = 0; tries < 12; tries++) {
            double a = random.nextDouble() * Math.PI * 2, r = 2 + random.nextDouble() * 2;
            Vec3 q = new Vec3(t.getX() + Math.cos(a) * r, t.getY(), t.getZ() + Math.sin(a) * r);
            if (!level().noCollision(this, getDimensions(getPose()).makeBoundingBox(q))) continue;
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 60, 0.6, 1.2, 0.6, 0.2);
            teleportTo(q.x, q.y, q.z);
            playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.get("ability_blink_start"), 2f, 0.4f);
            teleportCooldown = 200;
            return true;
        }
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "void_lord", 1f);
        if (hit) {
            triggerAnim("action", "attack");
            if (target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        }
        return hit;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance e) {
        if (e.getEffect() == MobEffects.WITHER || e.getEffect() == MobEffects.POISON) return false;
        return super.canBeAffected(e);
    }

    @Override public boolean removeWhenFarAway(double d) { return true; }   // natural spawns despawn; summoned ones are setPersistenceRequired
    @Override public boolean canChangeDimensions(Level from, Level to) { return false; }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("void_lord"); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("void_lord"); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("void_lord"); }
    @Override public float getVoicePitch() { return 0.5f; }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override public void remove(RemovalReason r) { bossEvent.removeAllPlayers(); super.remove(r); }
    @Override public void setCustomName(Component name) { super.setCustomName(name); bossEvent.setName(getDisplayName()); }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { super.readAdditionalSaveData(tag); if (hasCustomName()) bossEvent.setName(getDisplayName()); }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { c.add(BossDirector.presentation(this, "void_lord")); QFMobAnimations.register(this, "void_lord", c); c.add(new software.bernie.geckolib.animation.AnimationController<>(this, "fx", 0, e -> e.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.void_lord.fx")))); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.step("void_lord"), 0.6f, 1f); }
}
