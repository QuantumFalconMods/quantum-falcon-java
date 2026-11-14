package com.matt.quantumfalcon.entity.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

/**
 * Utility class for boss entity common functionality.
 * Handles boss bars, phase transitions, and common boss behaviors.
 */
public class BossEntityUtils {

    // ========================================
    // BOSS BAR CREATION
    // ========================================
    
    /**
     * Create a standard boss bar for a boss entity
     */
    public static ServerBossEvent createBossBar(Component name, BossEvent.BossBarColor color) {
        return new ServerBossEvent(name, color, BossEvent.BossBarOverlay.NOTCHED_10);
    }
    
    /**
     * Create a purple boss bar (quantum-themed)
     */
    public static ServerBossEvent createQuantumBossBar(Component name) {
        return createBossBar(name, BossEvent.BossBarColor.PURPLE);
    }
    
    /**
     * Create a red boss bar (void-themed)
     */
    public static ServerBossEvent createVoidBossBar(Component name) {
        return createBossBar(name, BossEvent.BossBarColor.RED);
    }
    
    /**
     * Create a blue boss bar (crystal-themed)
     */
    public static ServerBossEvent createCrystalBossBar(Component name) {
        return createBossBar(name, BossEvent.BossBarColor.BLUE);
    }
    
    /**
     * Create a yellow boss bar (singularity-themed)
     */
    public static ServerBossEvent createSingularityBossBar(Component name) {
        return createBossBar(name, BossEvent.BossBarColor.YELLOW);
    }

    // ========================================
    // PHASE MANAGEMENT
    // ========================================
    
    /**
     * Calculate current phase based on health percentage
     * @param entity The boss entity
     * @param maxPhases Maximum number of phases
     * @return Current phase (1-based)
     */
    public static int calculatePhase(LivingEntity entity, int maxPhases) {
        float healthPercent = entity.getHealth() / entity.getMaxHealth();
        int phase = 1;
        
        for (int i = maxPhases; i > 1; i--) {
            float threshold = 1.0f - ((float)(i - 1) / maxPhases);
            if (healthPercent <= threshold) {
                phase = i;
                break;
            }
        }
        
        return phase;
    }
    
    /**
     * Check if entity should transition to a new phase
     * @param entity The boss entity
     * @param currentPhase Current phase
     * @param maxPhases Maximum number of phases
     * @return New phase if transition needed, or current phase
     */
    public static int checkPhaseTransition(LivingEntity entity, int currentPhase, int maxPhases) {
        int newPhase = calculatePhase(entity, maxPhases);
        return Math.max(currentPhase, newPhase);
    }

    // ========================================
    // BOSS BAR MANAGEMENT
    // ========================================
    
    /**
     * Update boss bar progress
     */
    public static void updateBossBar(ServerBossEvent bossEvent, LivingEntity entity) {
        bossEvent.setProgress(entity.getHealth() / entity.getMaxHealth());
    }
    
    /**
     * Add player to boss bar
     */
    public static void addPlayerToBossBar(ServerBossEvent bossEvent, ServerPlayer player) {
        bossEvent.addPlayer(player);
    }
    
    /**
     * Remove player from boss bar
     */
    public static void removePlayerFromBossBar(ServerBossEvent bossEvent, ServerPlayer player) {
        bossEvent.removePlayer(player);
    }

    // ========================================
    // MINION SUMMONING
    // ========================================
    
    /**
     * Spawn minions in a circle around the boss
     * @param boss The boss entity
     * @param minionType Entity type to spawn
     * @param count Number of minions
     * @param radius Radius of the circle
     */
    public static void summonMinionsInCircle(LivingEntity boss, net.minecraft.world.entity.EntityType<?> minionType, int count, double radius) {
        if (!(boss.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            double x = boss.getX() + Math.cos(angle) * radius;
            double z = boss.getZ() + Math.sin(angle) * radius;
            double y = boss.getY();
            
            // Spawn particles
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, 
                x, y, z, 20, 0.5, 1, 0.5, 0);
            
            // Spawn minion (if entity type provided)
            if (minionType != null) {
                net.minecraft.world.entity.Entity minion = minionType.create(serverLevel);
                if (minion != null) {
                    minion.moveTo(x, y, z, boss.getRandom().nextFloat() * 360, 0);
                    serverLevel.addFreshEntity(minion);
                }
            }
        }
        
        boss.level().playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f);
    }
}
