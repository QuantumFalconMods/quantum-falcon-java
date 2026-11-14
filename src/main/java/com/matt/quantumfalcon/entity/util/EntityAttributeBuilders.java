package com.matt.quantumfalcon.entity.util;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.PathfinderMob;

/**
 * Common attribute builders for Quantum Falcon entities.
 * Reusable attribute configurations for different mob types.
 */
public class EntityAttributeBuilders {

    // ========================================
    // BASIC HOSTILE MOBS
    // ========================================
    
    /**
     * Standard hostile mob attributes (weak)
     */
    public static AttributeSupplier.Builder createBasicHostileAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }
    
    /**
     * Standard hostile mob attributes (medium)
     */
    public static AttributeSupplier.Builder createMediumHostileAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }
    
    /**
     * Standard hostile mob attributes (strong)
     */
    public static AttributeSupplier.Builder createStrongHostileAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 60.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    // ========================================
    // FLYING MOBS
    // ========================================
    
    /**
     * Flying hostile mob attributes
     */
    public static AttributeSupplier.Builder createFlyingHostileAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.FLYING_SPEED, 0.4)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }
    
    /**
     * Flying ambient mob attributes
     */
    public static AttributeSupplier.Builder createFlyingAmbientAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.FLYING_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    // ========================================
    // TANK MOBS
    // ========================================
    
    /**
     * Tank mob attributes (high health, high armor, slow)
     */
    public static AttributeSupplier.Builder createTankAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 60.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ARMOR, 15.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    // ========================================
    // BOSS MOBS
    // ========================================
    
    /**
     * Minor boss attributes
     */
    public static AttributeSupplier.Builder createMinorBossAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 350.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }
    
    /**
     * Major boss attributes
     */
    public static AttributeSupplier.Builder createMajorBossAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 500.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.ATTACK_DAMAGE, 16.0)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }
    
    /**
     * Elite boss attributes
     */
    public static AttributeSupplier.Builder createEliteBossAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 600.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 18.0)
            .add(Attributes.ARMOR, 12.0)
            .add(Attributes.ARMOR_TOUGHNESS, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }
    
    /**
     * Final boss attributes
     */
    public static AttributeSupplier.Builder createFinalBossAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1000.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 25.0)
            .add(Attributes.ARMOR, 20.0)
            .add(Attributes.ARMOR_TOUGHNESS, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    // ========================================
    // COMPANION MOBS
    // ========================================
    
    /**
     * Tameable companion attributes
     */
    public static AttributeSupplier.Builder createCompanionAttributes() {
        return net.minecraft.world.entity.animal.Animal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.FLYING_SPEED, 0.6)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.ARMOR, 4.0);
    }
}
