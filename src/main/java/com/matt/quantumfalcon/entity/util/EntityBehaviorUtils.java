package com.matt.quantumfalcon.entity.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Utility class for common entity behaviors.
 * Reusable behavior patterns for mobs.
 */
public class EntityBehaviorUtils {

    // ========================================
    // DAMAGE REFLECTION
    // ========================================
    
    /**
     * Reflect damage back to attacker when in defensive state
     * @param defender The entity being attacked
     * @param attacker The attacking entity
     * @param damageAmount Original damage amount
     * @param reflectionPercent Percentage of damage to reflect (0.0-1.0)
     * @param damageReductionPercent Percentage of damage to reduce (0.0-1.0)
     * @return Modified damage amount after reduction
     */
    public static float reflectDamage(LivingEntity defender, LivingEntity attacker, float damageAmount, 
                                      float reflectionPercent, float damageReductionPercent) {
        if (attacker != null) {
            float reflected = damageAmount * reflectionPercent;
            attacker.hurt(defender.damageSources().thorns(defender), reflected);
        }
        return damageAmount * (1.0f - damageReductionPercent);
    }

    // ========================================
    // TELEPORTATION
    // ========================================
    
    /**
     * Teleport entity behind target
     */
    public static void teleportBehind(Mob mob, LivingEntity target, double distance) {
        Vec3 lookVec = target.getLookAngle().normalize();
        double x = target.getX() - lookVec.x * distance;
        double y = target.getY();
        double z = target.getZ() - lookVec.z * distance;
        
        EntityParticleUtils.spawnTeleportParticles(mob, 15);
        mob.teleportTo(x, y, z);
        mob.setYRot(target.getYRot());
        EntityParticleUtils.spawnTeleportParticles(mob, 15);
        mob.level().playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.HOSTILE, 0.7f, 1.2f);
    }
    
    /**
     * Teleport entity to random nearby position
     */
    public static void teleportRandom(Mob mob, double range) {
        double x = mob.getX() + (mob.getRandom().nextDouble() - 0.5) * range * 2;
        double y = mob.getY() + mob.getRandom().nextDouble() * range;
        double z = mob.getZ() + (mob.getRandom().nextDouble() - 0.5) * range * 2;
        
        EntityParticleUtils.spawnTeleportParticles(mob, 15);
        mob.teleportTo(x, y, z);
        EntityParticleUtils.spawnTeleportParticles(mob, 15);
    }

    // ========================================
    // AREA EFFECTS
    // ========================================
    
    /**
     * Apply effect to all entities in radius
     */
    public static void applyAreaEffect(LivingEntity source, double radius, 
                                      MobEffectInstance effect, 
                                      java.util.function.Predicate<LivingEntity> filter) {
        AABB box = source.getBoundingBox().inflate(radius);
        List<LivingEntity> entities = source.level().getEntitiesOfClass(LivingEntity.class, box,
            e -> e != source && e.distanceToSqr(source) <= radius * radius && filter.test(e));
        
        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(effect));
        }
    }
    
    /**
     * Damage all entities in radius
     */
    public static void damageArea(LivingEntity source, double radius, float damage,
                                 java.util.function.Predicate<LivingEntity> filter) {
        AABB box = source.getBoundingBox().inflate(radius);
        List<LivingEntity> entities = source.level().getEntitiesOfClass(LivingEntity.class, box,
            e -> e != source && e.distanceToSqr(source) <= radius * radius && filter.test(e));
        
        for (LivingEntity entity : entities) {
            entity.hurt(source.damageSources().mobAttack(source), damage);
        }
    }

    // ========================================
    // FALL DAMAGE PREVENTION
    // ========================================
    
    /**
     * Prevent fall damage for flying entities
     */
    public static void preventFallDamage(LivingEntity entity, double y, boolean onGround, 
                                        BlockState state, BlockPos pos) {
        // Override in entity's checkFallDamage method
    }
    
    /**
     * Check if entity should take fall damage (for flying mobs)
     */
    public static boolean shouldTakeFallDamage(LivingEntity entity) {
        return false; // Override in entity's causeFallDamage method
    }

    // ========================================
    // TREASURE FINDING
    // ========================================
    
    /**
     * Find interesting location near entity (chests, spawners, ores)
     */
    @javax.annotation.Nullable
    public static BlockPos findTreasureLocation(LivingEntity entity, int searchRadius, int maxAttempts) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        
        for (int i = 0; i < maxAttempts; i++) {
            pos.set(
                entity.blockPosition().getX() + entity.getRandom().nextInt(searchRadius * 2) - searchRadius,
                entity.blockPosition().getY() + entity.getRandom().nextInt(searchRadius) - searchRadius / 2,
                entity.blockPosition().getZ() + entity.getRandom().nextInt(searchRadius * 2) - searchRadius
            );
            
            BlockState state = entity.level().getBlockState(pos);
            // Look for interesting blocks (chests, spawners, or high hardness blocks)
            if (state.getDestroySpeed(entity.level(), pos) > 0 && entity.getRandom().nextFloat() < 0.1f) {
                return pos.immutable();
            }
        }
        
        // Fallback to random location
        return entity.blockPosition().offset(
            entity.getRandom().nextInt(searchRadius * 2) - searchRadius,
            entity.getRandom().nextInt(searchRadius) - searchRadius / 2,
            entity.getRandom().nextInt(searchRadius * 2) - searchRadius
        );
    }

    // ========================================
    // SPLIT ON DEATH
    // ========================================
    
    /**
     * Split entity into smaller versions on death
     */
    public static void splitOnDeath(LivingEntity entity, net.minecraft.world.entity.EntityType<?> splitType, 
                                   int splitCount, int newSize) {
        if (entity.level().isClientSide) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        
        for (int i = 0; i < splitCount; i++) {
            net.minecraft.world.entity.Entity split = splitType.create(serverLevel);
            if (split != null) {
                split.moveTo(
                    entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 2,
                    entity.getY(),
                    entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 2,
                    entity.getRandom().nextFloat() * 360, 0
                );
                
                // Set size if entity supports it (like DataConstructEntity)
                if (split instanceof com.matt.quantumfalcon.entity.DataConstructEntity construct) {
                    construct.setConstructSize(newSize);
                }
                
                // a persistent parent (dungeon guard, test or named mob) leaves persistent pieces: they must not despawn mid-fight
                if (entity instanceof net.minecraft.world.entity.Mob pm && pm.isPersistenceRequired() && split instanceof net.minecraft.world.entity.Mob sm) sm.setPersistenceRequired();
                if (entity instanceof net.minecraft.world.entity.Mob pm2 && pm2.isNoAi() && split instanceof net.minecraft.world.entity.Mob sm2) sm2.setNoAi(true);   // a frozen (test/display) parent leaves frozen pieces
                serverLevel.addFreshEntity(split);
            }
        }
        
        EntityParticleUtils.spawnSplitParticles(entity.level(), entity.getX(), entity.getY(), entity.getZ(), 20);
    }
}
