package com.matt.quantumfalcon.entity.util;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

/**
 * Utility class for common goal registration patterns.
 * Provides helper methods to register standard goal sets.
 */
public class EntityGoalUtils {

    // ========================================
    // BASIC HOSTILE MOB GOALS
    // ========================================
    
    /**
     * Register standard goals for a basic hostile mob
     */
    public static void registerBasicHostileGoals(Monster mob) {
        mob.goalSelector.addGoal(0, new FloatGoal(mob));
        mob.goalSelector.addGoal(1, new MeleeAttackGoal(mob, 1.2, false));
        mob.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(mob, 1.0));
        mob.goalSelector.addGoal(3, new LookAtPlayerGoal(mob, Player.class, 8.0f));
        mob.goalSelector.addGoal(4, new RandomLookAroundGoal(mob));
        
        mob.targetSelector.addGoal(1, new HurtByTargetGoal(mob));
        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, true));
    }
    
    /**
     * Register goals for a flying hostile mob
     */
    public static void registerFlyingHostileGoals(Monster mob) {
        mob.goalSelector.addGoal(0, new FloatGoal(mob));
        mob.goalSelector.addGoal(1, new MeleeAttackGoal(mob, 1.2, false));
        mob.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(mob, 1.0));
        mob.goalSelector.addGoal(3, new LookAtPlayerGoal(mob, Player.class, 8.0f));
        mob.goalSelector.addGoal(4, new RandomLookAroundGoal(mob));
        
        mob.targetSelector.addGoal(1, new HurtByTargetGoal(mob));
        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, true));
    }
    
    /**
     * Register goals for a tank mob (slower, more defensive)
     */
    public static void registerTankGoals(Monster mob) {
        mob.goalSelector.addGoal(0, new FloatGoal(mob));
        mob.goalSelector.addGoal(1, new MeleeAttackGoal(mob, 1.0, false));
        mob.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(mob, 0.8));
        mob.goalSelector.addGoal(3, new LookAtPlayerGoal(mob, Player.class, 8.0f));
        mob.goalSelector.addGoal(4, new RandomLookAroundGoal(mob));
        
        mob.targetSelector.addGoal(1, new HurtByTargetGoal(mob));
        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, true));
    }

    // ========================================
    // BOSS MOB GOALS
    // ========================================
    
    /**
     * Register standard goals for a boss mob
     * Note: Custom attack goals should be added before this
     */
    public static void registerBossGoals(Monster mob) {
        mob.goalSelector.addGoal(0, new FloatGoal(mob));
        // Custom attack goals should be added at priority 1-2
        mob.goalSelector.addGoal(3, new MeleeAttackGoal(mob, 1.2, false));
        mob.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(mob, 1.0));
        mob.goalSelector.addGoal(5, new LookAtPlayerGoal(mob, Player.class, 16.0f));
        mob.goalSelector.addGoal(6, new RandomLookAroundGoal(mob));
        
        mob.targetSelector.addGoal(1, new HurtByTargetGoal(mob));
        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, true));
    }

    // ========================================
    // AMBIENT MOB GOALS
    // ========================================
    
    /**
     * Register goals for a peaceful ambient mob
     */
    public static void registerAmbientGoals(PathfinderMob mob) {
        mob.goalSelector.addGoal(0, new FloatGoal(mob));
        mob.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(mob, 0.8));
        mob.goalSelector.addGoal(2, new LookAtPlayerGoal(mob, Player.class, 6.0f));
        mob.goalSelector.addGoal(3, new RandomLookAroundGoal(mob));
    }
    
    /**
     * Register goals for a flying ambient mob
     */
    public static void registerFlyingAmbientGoals(PathfinderMob mob) {
        mob.goalSelector.addGoal(0, new FloatGoal(mob));
        mob.goalSelector.addGoal(1, new WaterAvoidingRandomFlyingGoal(mob, 0.8));
        mob.goalSelector.addGoal(2, new LookAtPlayerGoal(mob, Player.class, 6.0f));
        mob.goalSelector.addGoal(3, new RandomLookAroundGoal(mob));
    }

    // ========================================
    // TAMEABLE COMPANION GOALS
    // ========================================
    
    /**
     * Register goals for a tameable companion (like falcon)
     */
    public static void registerCompanionGoals(net.minecraft.world.entity.TamableAnimal mob) {
        mob.goalSelector.addGoal(1, new FloatGoal(mob));
        mob.goalSelector.addGoal(2, new SitWhenOrderedToGoal(mob));
        mob.goalSelector.addGoal(3, new MeleeAttackGoal(mob, 1.5, true));
        mob.goalSelector.addGoal(4, new FollowOwnerGoal(mob, 1.2, 10.0f, 3.0f));
        mob.goalSelector.addGoal(5, new BreedGoal(mob, 1.0));
        mob.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(mob, 1.0));
        mob.goalSelector.addGoal(7, new LookAtPlayerGoal(mob, Player.class, 8.0f));
        mob.goalSelector.addGoal(8, new RandomLookAroundGoal(mob));
        
        mob.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal(mob));
        mob.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal(mob));
        mob.targetSelector.addGoal(3, new HurtByTargetGoal(mob));
    }
}
