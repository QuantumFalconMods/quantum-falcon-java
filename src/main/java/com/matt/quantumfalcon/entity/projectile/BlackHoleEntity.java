package com.matt.quantumfalcon.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Black Hole - Gravity projectile.
 * Pulls entities towards it and deals damage.
 * Lasts for a short duration before collapsing.
 */
public class BlackHoleEntity extends Projectile {
    
    private int lifetime = 100; // 5 seconds
    private float pullStrength = 0.3f;
    private float damagePerTick = 0.5f;
    private double radius = 8.0;

    public BlackHoleEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public void setLifetime(int ticks) {
        this.lifetime = ticks;
    }
    
    public void setPullStrength(float strength) {
        this.pullStrength = strength;
    }
    
    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public void tick() {
        super.tick();
        
        lifetime--;
        if (lifetime <= 0) {
            collapse();
            return;
        }
        
        // Pull and damage nearby entities
        AABB area = this.getBoundingBox().inflate(radius);
        List<Entity> entities = this.level().getEntities(this, area, 
            e -> e != this && e != this.getOwner() && e instanceof LivingEntity);
        
        for (Entity entity : entities) {
            double distance = entity.distanceTo(this);
            if (distance < 0.5) distance = 0.5; // Prevent division issues
            
            // Calculate pull force (stronger when closer)
            double force = pullStrength * (1 - (distance / radius));
            Vec3 direction = this.position().subtract(entity.position()).normalize();
            
            entity.push(direction.x * force, direction.y * force * 0.5, direction.z * force);
            
            // Damage entities very close
            if (distance < 3 && entity instanceof LivingEntity living) {
                living.hurt(this.damageSources().magic(), damagePerTick);
            }
        }
        
        // Visual effects
        if (this.level().isClientSide) {
            // Swirling particles
            for (int i = 0; i < 5; i++) {
                double angle = (this.tickCount * 0.3 + i * (Math.PI * 2 / 5));
                double r = 1 + Math.sin(this.tickCount * 0.1) * 0.5;
                double x = this.getX() + Math.cos(angle) * r;
                double z = this.getZ() + Math.sin(angle) * r;
                this.level().addParticle(ParticleTypes.PORTAL, x, this.getY(), z, 0, 0.1, 0);
            }
            
            // Center particles
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL, 
                this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }
    
    private void collapse() {
        // Final burst of damage
        AABB area = this.getBoundingBox().inflate(radius * 0.5);
        for (Entity entity : this.level().getEntities(this, area)) {
            if (entity instanceof LivingEntity living && entity != this.getOwner()) {
                living.hurt(this.damageSources().magic(), 10.0f);
            }
        }
        
        // Collapse particles
        if (this.level().isClientSide) {
            for (int i = 0; i < 50; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * radius;
                double x = this.getX() + Math.cos(angle) * r;
                double z = this.getZ() + Math.sin(angle) * r;
                double y = this.getY() + (Math.random() - 0.5) * 2;
                
                Vec3 dir = this.position().subtract(x, y, z).normalize().scale(0.5);
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, dir.x, dir.y, dir.z);
            }
        }
        
        this.discard();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No synched data needed
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Lifetime", lifetime);
        tag.putFloat("PullStrength", pullStrength);
        tag.putDouble("Radius", radius);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifetime = tag.getInt("Lifetime");
        pullStrength = tag.getFloat("PullStrength");
        radius = tag.getDouble("Radius");
    }
}
