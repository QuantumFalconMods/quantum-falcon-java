package com.matt.quantumfalcon.entity.util;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

/**
 * Utility class for common sound effects used by entities.
 * Reusable sound selection methods.
 */
public class EntitySoundUtils {

    // ========================================
    // MOB TYPE SOUNDS
    // ========================================
    
    /**
     * Get ambient sound for phantom-like mobs
     */
    public static SoundEvent getPhantomAmbient() {
        return SoundEvents.PHANTOM_AMBIENT;
    }
    
    /**
     * Get hurt sound for phantom-like mobs
     */
    public static SoundEvent getPhantomHurt() {
        return SoundEvents.PHANTOM_HURT;
    }
    
    /**
     * Get death sound for phantom-like mobs
     */
    public static SoundEvent getPhantomDeath() {
        return SoundEvents.PHANTOM_DEATH;
    }
    
    /**
     * Get ambient sound for enderman-like mobs
     */
    public static SoundEvent getEndermanAmbient() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }
    
    /**
     * Get hurt sound for enderman-like mobs
     */
    public static SoundEvent getEndermanHurt() {
        return SoundEvents.ENDERMAN_HURT;
    }
    
    /**
     * Get death sound for enderman-like mobs
     */
    public static SoundEvent getEndermanDeath() {
        return SoundEvents.ENDERMAN_DEATH;
    }
    
    /**
     * Get ambient sound for golem-like mobs
     */
    public static SoundEvent getGolemAmbient() {
        return SoundEvents.IRON_GOLEM_STEP;
    }
    
    /**
     * Get hurt sound for golem-like mobs
     */
    public static SoundEvent getGolemHurt() {
        return SoundEvents.IRON_GOLEM_HURT;
    }
    
    /**
     * Get death sound for golem-like mobs
     */
    public static SoundEvent getGolemDeath() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }
    
    /**
     * Get ambient sound for allay-like mobs
     */
    public static SoundEvent getAllayAmbient() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }
    
    /**
     * Get hurt sound for allay-like mobs
     */
    public static SoundEvent getAllayHurt() {
        return SoundEvents.ALLAY_HURT;
    }
    
    /**
     * Get death sound for allay-like mobs
     */
    public static SoundEvent getAllayDeath() {
        return SoundEvents.ALLAY_DEATH;
    }
    
    /**
     * Get ambient sound for boss mobs
     */
    public static SoundEvent getBossAmbient() {
        return SoundEvents.WITHER_AMBIENT;
    }
    
    /**
     * Get hurt sound for boss mobs
     */
    public static SoundEvent getBossHurt() {
        return SoundEvents.WITHER_HURT;
    }
    
    /**
     * Get death sound for boss mobs
     */
    public static SoundEvent getBossDeath() {
        return SoundEvents.WITHER_DEATH;
    }
    
    /**
     * Get ambient sound for digital/tech mobs
     */
    public static SoundEvent getDigitalAmbient() {
        return SoundEvents.BEACON_AMBIENT;
    }
    
    /**
     * Get hurt sound for digital/tech mobs
     */
    public static SoundEvent getDigitalHurt() {
        return SoundEvents.BEACON_DEACTIVATE;
    }
    
    /**
     * Get death sound for digital/tech mobs
     */
    public static SoundEvent getDigitalDeath() {
        return SoundEvents.BEACON_POWER_SELECT;
    }
}
