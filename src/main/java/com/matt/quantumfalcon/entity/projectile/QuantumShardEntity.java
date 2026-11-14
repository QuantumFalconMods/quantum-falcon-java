package com.matt.quantumfalcon.entity.projectile;

import com.matt.quantumfalcon.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Homing projectile fired by Quantum Overseer.
 * Cyan END_ROD trail, configurable damage and homing strength.
 */
public class QuantumShardEntity extends Projectile {

    private static final EntityDataAccessor<Float> DATA_DAMAGE =
        SynchedEntityData.defineId(QuantumShardEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HOMING =
        SynchedEntityData.defineId(QuantumShardEntity.class, EntityDataSerializers.FLOAT);

    private static final int MAX_LIFETIME = 60; // 3 seconds

    public QuantumShardEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public QuantumShardEntity(EntityType<? extends Projectile> type, LivingEntity owner,
                              double x, double y, double z, Level level,
                              float damage, float homingStrength) {
        this(type, level);
        setOwner(owner);
        setPos(x, y, z);
        setDamage(damage);
        setHomingStrength(homingStrength);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Projectile does not provide defineSynchedData; do not call super.
        builder.define(DATA_DAMAGE, 6.0f);
        builder.define(DATA_HOMING, 0.02f);
    }

    public void setDamage(float damage) {
        entityData.set(DATA_DAMAGE, damage);
    }

    public float getDamage() {
        return entityData.get(DATA_DAMAGE);
    }

    public void setHomingStrength(float strength) {
        entityData.set(DATA_HOMING, strength);
    }

    public float getHomingStrength() {
        return entityData.get(DATA_HOMING);
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount > MAX_LIFETIME) {
            discard();
            return;
        }

        if (!level().isClientSide) {
            Entity owner = getOwner();
            LivingEntity target = null;
            if (owner instanceof Mob mob && mob.getTarget() != null) {
                target = mob.getTarget();
            }
            if (target != null && target.isAlive()) {
                Vec3 toTarget = target.getEyePosition(1f).subtract(position());
                double dist = toTarget.length();
                if (dist > 0.1) {
                    float homing = getHomingStrength();
                    Vec3 vel = getDeltaMovement();
                    Vec3 desired = toTarget.normalize().scale(vel.length());
                    Vec3 newVel = vel.add(desired.subtract(vel).scale(homing));
                    setDeltaMovement(newVel);
                }
            }
        }

        // Move + collide. Projectile.tick() (unlike ThrowableProjectile/AbstractArrow) does neither, so before this the
        // shards hung where they spawned for 3 s and never hit anyone.
        HitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this, hit)) {
            hitTargetOrDeflectSelf(hit);
            if (isRemoved()) return;
        }
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        net.minecraft.world.entity.projectile.ProjectileUtil.rotateTowardsMovement(this, 0.5f);

        // Trail particles: RIFT_BURST (allowed Rift Warden particle; no rift_vortex)
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ModParticles.RIFT_BURST.get(),
                getX(), getY() + 0.25, getZ(), 1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;
        Entity hit = result.getEntity();
        if (hit instanceof LivingEntity living && hit != getOwner()) {
            living.hurt(damageSources().mobAttack(getOwner() instanceof LivingEntity o ? o : null), getDamage());
            if (getOwner() instanceof com.matt.quantumfalcon.entity.boss.RiftWardenEntity)   // the Warden's shards lift; the Overseer's (first boss) don't
                living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 15, 0));
            level().broadcastEntityEvent(this, (byte) 4); // CRIT effect
        }
        spawnHitParticles();
        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        spawnHitParticles();
        discard();
    }

    private void spawnHitParticles() {
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ModParticles.RIFT_BURST.get(), getX(), getY() + 0.5, getZ(), 12, 0.3, 0.3, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 8, 0.2, 0.2, 0.2, 0.05);
        }
        level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.GLASS_BREAK,
            net.minecraft.sounds.SoundSource.HOSTILE, 0.6f, 1.2f);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4 && level().isClientSide) {
            for (int i = 0; i < 8; i++) {
                level().addParticle(ParticleTypes.CRIT, getX(), getY() + 0.5, getZ(),
                    (random.nextDouble() - 0.5) * 0.5, 0.3, (random.nextDouble() - 0.5) * 0.5);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
}
