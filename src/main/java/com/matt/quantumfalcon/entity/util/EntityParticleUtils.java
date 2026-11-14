package com.matt.quantumfalcon.entity.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for common particle effects used by entities.
 * Reusable particle spawning methods.
 */
public class EntityParticleUtils {

    // ========================================
    // AMBIENT PARTICLES
    // ========================================
    
    /**
     * Spawn ambient glow particles around an entity
     */
    public static void spawnAmbientGlow(LivingEntity entity, ParticleOptions particle, int frequency) {
        if (entity.level().isClientSide && entity.tickCount % frequency == 0) {
            double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 0.8;
            double y = entity.getY() + entity.getRandom().nextDouble() * 1.8;
            double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 0.8;
            entity.level().addParticle(particle, x, y, z, 0, 0.05, 0);
        }
    }
    
    /**
     * Spawn quantum-themed ambient particles
     */
    public static void spawnQuantumAmbient(LivingEntity entity) {
        spawnAmbientGlow(entity, ParticleTypes.REVERSE_PORTAL, 3);
    }
    
    /**
     * Spawn void-themed ambient particles
     */
    public static void spawnVoidAmbient(LivingEntity entity) {
        spawnAmbientGlow(entity, ParticleTypes.PORTAL, 2);
    }
    
    /**
     * Spawn crystal-themed ambient particles
     */
    public static void spawnCrystalAmbient(LivingEntity entity) {
        spawnAmbientGlow(entity, ParticleTypes.ENCHANT, 5);
    }
    
    /**
     * Spawn digital/tech-themed ambient particles
     */
    public static void spawnDigitalAmbient(LivingEntity entity) {
        spawnAmbientGlow(entity, ParticleTypes.ELECTRIC_SPARK, 10);
    }

    // ========================================
    // TELEPORT PARTICLES
    // ========================================
    
    /**
     * Spawn teleport particles at entity location
     */
    public static void spawnTeleportParticles(LivingEntity entity, int count) {
        Level level = entity.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                entity.getX(), entity.getY() + 1, entity.getZ(), count, 0.5, 1, 0.5, 0.1);
        } else if (level.isClientSide) {
            for (int i = 0; i < count; i++) {
                double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5);
                double y = entity.getY() + entity.getRandom().nextDouble() * 2;
                double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5);
                level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0, 0);
            }
        }
    }
    
    /**
     * Spawn teleport particles at specific position
     */
    public static void spawnTeleportParticlesAt(Level level, double x, double y, double z, int count) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, count, 0.5, 1, 0.5, 0.1);
        } else if (level.isClientSide) {
            for (int i = 0; i < count; i++) {
                double px = x + (level.random.nextDouble() - 0.5);
                double py = y + level.random.nextDouble() * 2;
                double pz = z + (level.random.nextDouble() - 0.5);
                level.addParticle(ParticleTypes.REVERSE_PORTAL, px, py, pz, 0, 0, 0);
            }
        }
    }

    // ========================================
    // DAMAGE PARTICLES
    // ========================================
    
    /**
     * Spawn damage reflection particles (for crystallized mobs)
     */
    public static void spawnDamageReflectionParticles(LivingEntity entity) {
        if (entity.level().isClientSide) {
            for (int i = 0; i < 10; i++) {
                double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5);
                double y = entity.getY() + entity.getRandom().nextDouble() * 2;
                double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5);
                entity.level().addParticle(ParticleTypes.CRIT, x, y, z, 0, 0.1, 0);
            }
        }
    }
    
    /**
     * Spawn explosion particles
     */
    public static void spawnExplosionParticles(Level level, double x, double y, double z, int count) {
        if (level.isClientSide) {
            for (int i = 0; i < count; i++) {
                double px = x + (level.random.nextDouble() - 0.5) * 2;
                double py = y + level.random.nextDouble() * 2;
                double pz = z + (level.random.nextDouble() - 0.5) * 2;
                level.addParticle(ParticleTypes.REVERSE_PORTAL, px, py, pz, 0, 0.1, 0);
            }
        }
    }

    // ========================================
    // BEAM/ATTACK PARTICLES
    // ========================================
    
    /**
     * Spawn beam particles between two points
     */
    public static void spawnBeamParticles(Level level, Vec3 start, Vec3 end, ParticleOptions particle, int count) {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 direction = end.subtract(start).normalize();
            double distance = start.distanceTo(end);
            double step = distance / count;
            
            for (int i = 0; i < count; i++) {
                Vec3 pos = start.add(direction.scale(step * i));
                serverLevel.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }
    
    /**
     * Spawn circular attack particles around entity
     */
    public static void spawnCircularAttackParticles(LivingEntity entity, ParticleOptions particle, double radius, int count) {
        Level level = entity.level();
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < count; i++) {
                double angle = (2 * Math.PI / count) * i;
                double x = entity.getX() + Math.cos(angle) * radius;
                double z = entity.getZ() + Math.sin(angle) * radius;
                serverLevel.sendParticles(particle, x, entity.getY(), z, 10, 0.5, 1, 0.5, 0);
            }
        }
    }

    // ========================================
    // SPLIT/SPAWN PARTICLES
    // ========================================
    
    /**
     * Spawn split particles (for entities that split on death)
     */
    public static void spawnSplitParticles(Level level, double x, double y, double z, int count) {
        if (level.isClientSide) {
            for (int i = 0; i < count; i++) {
                double px = x + (level.random.nextDouble() - 0.5) * 2;
                double py = y + level.random.nextDouble() * 1.5;
                double pz = z + (level.random.nextDouble() - 0.5) * 2;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0, 0.1, 0);
            }
        }
    }
}
