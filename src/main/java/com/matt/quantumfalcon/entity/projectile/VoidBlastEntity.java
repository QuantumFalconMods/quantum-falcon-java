package com.matt.quantumfalcon.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Void Blast - Dark energy projectile.
 * Applies weakness and deals damage.
 */
public class VoidBlastEntity extends Fireball {
    
    private float damage = 8.0f;

    public VoidBlastEntity(EntityType<? extends Fireball> type, Level level) {
        super(type, level);
    }

    public VoidBlastEntity(EntityType<? extends Fireball> type, 
                           LivingEntity shooter, Vec3 movement, Level level) {
        super(type, shooter, movement, level);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity living) {
            living.hurt(this.damageSources().magic(), damage);
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        
        if (!this.level().isClientSide) {
            // Explosion particles
            for (int i = 0; i < 20; i++) {
                double x = this.getX() + (this.random.nextDouble() - 0.5) * 2;
                double y = this.getY() + this.random.nextDouble() * 2;
                double z = this.getZ() + (this.random.nextDouble() - 0.5) * 2;
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.1, 0);
            }
            this.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        
        // Trail particles
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.PORTAL, 
                this.getX(), this.getY(), this.getZ(), 
                (this.random.nextDouble() - 0.5) * 0.2, 
                this.random.nextDouble() * 0.2, 
                (this.random.nextDouble() - 0.5) * 0.2);
        }
        
        // Despawn after 5 seconds
        if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }
}
