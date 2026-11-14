package com.matt.quantumfalcon.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Reality Rift - Teleportation projectile.
 * Teleports hit entities to a random location.
 * Can be used offensively or for mobility.
 */
public class RealityRiftEntity extends Projectile {
    
    private int lifetime = 60; // 3 seconds
    private double teleportRange = 15.0;

    public RealityRiftEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public void setTeleportRange(double range) {
        this.teleportRange = range;
    }

    @Override
    public void tick() {
        super.tick();
        
        // Movement
        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
        
        // Check for collision
        HitResult hit = this.level().clip(new net.minecraft.world.level.ClipContext(
            this.position(), this.position().add(motion),
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
        }
        
        lifetime--;
        if (lifetime <= 0) {
            this.discard();
        }
        
        // Trail particles
        if (this.level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                double x = this.getX() + (this.random.nextDouble() - 0.5) * 0.3;
                double y = this.getY() + (this.random.nextDouble() - 0.5) * 0.3;
                double z = this.getZ() + (this.random.nextDouble() - 0.5) * 0.3;
                this.level().addParticle(ParticleTypes.PORTAL, x, y, z, 
                    -motion.x * 0.5, -motion.y * 0.5, -motion.z * 0.5);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        
        Entity target = result.getEntity();
        if (target instanceof LivingEntity living && !this.level().isClientSide) {
            teleportEntity(living);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        
        if (!this.level().isClientSide) {
            // Explosion particles at impact
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY(), this.getZ(),
                    30, 0.5, 0.5, 0.5, 0.1);
            }
            this.discard();
        }
    }
    
    private void teleportEntity(LivingEntity entity) {
        // Calculate random teleport destination
        double angle = this.random.nextDouble() * Math.PI * 2;
        double distance = teleportRange * (0.5 + this.random.nextDouble() * 0.5);
        
        double newX = entity.getX() + Math.cos(angle) * distance;
        double newZ = entity.getZ() + Math.sin(angle) * distance;
        double newY = entity.getY() + (this.random.nextDouble() - 0.5) * 5;
        
        // Particles at start
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                entity.getX(), entity.getY() + 1, entity.getZ(),
                20, 0.5, 1, 0.5, 0.1);
        }
        
        // Teleport
        entity.teleportTo(newX, newY, newZ);
        entity.level().playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f);
        
        // Particles at end
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                entity.getX(), entity.getY() + 1, entity.getZ(),
                20, 0.5, 1, 0.5, 0.1);
        }
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No synched data needed
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Lifetime", lifetime);
        tag.putDouble("TeleportRange", teleportRange);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifetime = tag.getInt("Lifetime");
        teleportRange = tag.getDouble("TeleportRange");
    }
}
