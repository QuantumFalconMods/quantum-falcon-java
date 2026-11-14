/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.neoforged.neoforge.common.ModConfigSpec
 *  net.neoforged.neoforge.common.ModConfigSpec$BooleanValue
 *  net.neoforged.neoforge.common.ModConfigSpec$Builder
 *  net.neoforged.neoforge.common.ModConfigSpec$DoubleValue
 *  net.neoforged.neoforge.common.ModConfigSpec$IntValue
 */
package com.matt.quantumfalcon;

import net.neoforged.neoforge.common.ModConfigSpec;

public class QFConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue LEGENDARY_MUTATION_CHANCE;
    public static final ModConfigSpec.DoubleValue EPIC_MUTATION_CHANCE;
    public static final ModConfigSpec.DoubleValue HYBRID_MUTATION_CHANCE;
    public static final ModConfigSpec.IntValue BREEDING_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue INCUBATION_TIME_TICKS;
    public static final ModConfigSpec.DoubleValue BREEDING_BACKLASH_CHANCE;
    public static final ModConfigSpec.IntValue RIFT_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue WILD_FALCON_SPAWN_WEIGHT;
    public static final ModConfigSpec.IntValue MIN_SPAWN_HEIGHT;
    public static final ModConfigSpec.IntValue BLINK_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue BLINK_RANGE_BLOCKS;
    public static final ModConfigSpec.DoubleValue DIVE_CRIT_CHANCE;
    public static final ModConfigSpec.DoubleValue BACKLASH_DAMAGE;
    public static final ModConfigSpec.BooleanValue ENABLE_PERSONALITY_SYSTEM;
    public static final ModConfigSpec.IntValue MAX_BOND_LEVEL;
    public static final ModConfigSpec.IntValue BOND_PER_FEED;
    public static final ModConfigSpec.BooleanValue ENABLE_QUANTUM_STORMS;
    public static final ModConfigSpec.IntValue STORM_FREQUENCY_TICKS;
    public static final ModConfigSpec.BooleanValue ENABLE_DAILY_RIFTS;
    public static final ModConfigSpec.IntValue MAX_FALCONS_PER_PLAYER;
    public static final ModConfigSpec.BooleanValue ENABLE_SPATIAL_CACHE;

    static {
        BUILDER.comment("Quantum Falcon Configuration").comment("Adjust breeding rates, spawn chances, and gameplay balance");
        BUILDER.push("breeding");
        LEGENDARY_MUTATION_CHANCE = BUILDER.comment("Base chance for legendary mutation (1/4096 = 0.000244)").defineInRange("legendaryMutationChance", 2.44E-4, 0.0, 1.0);
        EPIC_MUTATION_CHANCE = BUILDER.comment("Base chance for epic mutation").defineInRange("epicMutationChance", 0.01, 0.0, 1.0);
        HYBRID_MUTATION_CHANCE = BUILDER.comment("Chance for hybrid variant when breeding different types").defineInRange("hybridMutationChance", 0.15, 0.0, 1.0);
        BREEDING_COOLDOWN_TICKS = BUILDER.comment("Cooldown between breeding attempts (20 ticks = 1 second)").defineInRange("breedingCooldown", 6000, 100, 72000);
        INCUBATION_TIME_TICKS = BUILDER.comment("Time for egg to hatch in incubator (6000 = 5 minutes)").defineInRange("incubationTime", 6000, 200, 72000);
        BREEDING_BACKLASH_CHANCE = BUILDER.comment("Chance for breeding to cause quantum backlash").defineInRange("backlashChance", 0.05, 0.0, 1.0);
        BUILDER.pop();
        BUILDER.push("spawning");
        RIFT_SPAWN_CHANCE = BUILDER.comment("Chance per chunk for quantum rift structure (1/500)").defineInRange("riftSpawnChance", 500, 10, 10000);
        WILD_FALCON_SPAWN_WEIGHT = BUILDER.comment("Spawn weight for wild falcons in rifts").defineInRange("wildFalconWeight", 5, 1, 100);
        MIN_SPAWN_HEIGHT = BUILDER.comment("Minimum Y level for wild falcon spawns").defineInRange("minSpawnHeight", 100, 0, 320);
        BUILDER.pop();
        BUILDER.push("abilities");
        BLINK_COOLDOWN_TICKS = BUILDER.comment("Cooldown for quantum blink ability").defineInRange("blinkCooldown", 100, 20, 1200);
        BLINK_RANGE_BLOCKS = BUILDER.comment("Maximum blink teleport distance").defineInRange("blinkRange", 16, 5, 64);
        DIVE_CRIT_CHANCE = BUILDER.comment("Base critical hit chance during dive attacks").defineInRange("diveCritChance", 0.2, 0.0, 1.0);
        BACKLASH_DAMAGE = BUILDER.comment("Damage taken on ability backlash").defineInRange("backlashDamage", 4.0, 0.0, 20.0);
        BUILDER.pop();
        BUILDER.push("personality");
        ENABLE_PERSONALITY_SYSTEM = BUILDER.comment("Enable falcon personality traits and memories").define("enablePersonality", true);
        MAX_BOND_LEVEL = BUILDER.comment("Maximum bond level with falcon").defineInRange("maxBondLevel", 100, 10, 1000);
        BOND_PER_FEED = BUILDER.comment("Bond points gained per feeding").defineInRange("bondPerFeed", 5, 1, 50);
        BUILDER.pop();
        BUILDER.push("events");
        ENABLE_QUANTUM_STORMS = BUILDER.comment("Enable random quantum storm events").define("enableQuantumStorms", true);
        STORM_FREQUENCY_TICKS = BUILDER.comment("Average ticks between quantum storms (72000 = 1 hour)").defineInRange("stormFrequency", 72000, 12000, 432000);
        ENABLE_DAILY_RIFTS = BUILDER.comment("Enable daily rift spawn events").define("enableDailyRifts", true);
        BUILDER.pop();
        BUILDER.push("performance");
        MAX_FALCONS_PER_PLAYER = BUILDER.comment("Maximum owned falcons per player").defineInRange("maxFalconsPerPlayer", 50, 1, 500);
        ENABLE_SPATIAL_CACHE = BUILDER.comment("Enable spatial hashing for falcon lookups (improves performance)").define("enableSpatialCache", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
