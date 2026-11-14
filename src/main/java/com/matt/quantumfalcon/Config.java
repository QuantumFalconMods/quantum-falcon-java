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

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_BIOMES;
    public static final ModConfigSpec.IntValue BIOME_RARITY;
    public static final ModConfigSpec.BooleanValue ENABLE_GENETICS;
    public static final ModConfigSpec.DoubleValue LEGENDARY_SPAWN_CHANCE;
    public static final ModConfigSpec.BooleanValue ENABLE_AURORA;
    public static final ModConfigSpec.DoubleValue AURORA_CHANCE;
    public static final ModConfigSpec.BooleanValue ENABLE_QUANTUM_STORM;
    public static final ModConfigSpec.DoubleValue STORM_CHANCE;
    public static final ModConfigSpec.BooleanValue ENABLE_NPC_MEMORY;
    public static final ModConfigSpec.IntValue MEMORY_DURATION_DAYS;
    public static final ModConfigSpec.IntValue MAX_MEMORIES;
    public static final ModConfigSpec.BooleanValue ENABLE_FOG;
    public static final ModConfigSpec.BooleanValue ENABLE_ROLLING_BOULDERS;

    static {
        BUILDER.push("Quantum Falcon Configuration");
        BUILDER.push("biomes");
        ENABLE_BIOMES = BUILDER.comment("Enable custom biome generation").define("enableBiomes", true);
        BIOME_RARITY = BUILDER.comment("Biome spawn rarity (higher = rarer)").defineInRange("biomeRarity", 3, 1, 10);
        BUILDER.pop();
        BUILDER.push("genetics");
        ENABLE_GENETICS = BUILDER.comment("Enable falcon genetics system").define("enableGenetics", true);
        LEGENDARY_SPAWN_CHANCE = BUILDER.comment("Chance for legendary falcon spawn (0.0-1.0)").defineInRange("legendarySpawnChance", 1.0E-4, 0.0, 1.0);
        BUILDER.pop();
        BUILDER.push("events");
        ENABLE_AURORA = BUILDER.comment("Enable Aurora Borealis events").define("enableAurora", true);
        AURORA_CHANCE = BUILDER.comment("Aurora trigger chance per night (0.0-1.0)").defineInRange("auroraChance", 0.05, 0.0, 1.0);
        ENABLE_QUANTUM_STORM = BUILDER.comment("Enable Quantum Storm events").define("enableQuantumStorm", true);
        STORM_CHANCE = BUILDER.comment("Storm trigger chance per day (0.0-1.0)").defineInRange("stormChance", 0.02, 0.0, 1.0);
        BUILDER.pop();
        BUILDER.push("npc_memory");
        ENABLE_NPC_MEMORY = BUILDER.comment("Enable NPC memory system").define("enableNPCMemory", true);
        MEMORY_DURATION_DAYS = BUILDER.comment("How many days NPCs remember (1-30)").defineInRange("memoryDurationDays", 7, 1, 30);
        MAX_MEMORIES = BUILDER.comment("Max memories per player per NPC (10-100)").defineInRange("maxMemories", 50, 10, 100);
        BUILDER.pop();
        BUILDER.push("physics");
        ENABLE_FOG = BUILDER.comment("Enable dynamic fog system").define("enableFog", true);
        ENABLE_ROLLING_BOULDERS = BUILDER.comment("Enable rolling boulder traps").define("enableRollingBoulders", true);
        BUILDER.pop();
        BUILDER.push("instability");
        INSTABILITY_SPEED = BUILDER.comment("Instability decay speed multiplier (0.5 = slower, 2.0 = faster)").defineInRange("instabilitySpeed", 1.0, 0.5, 2.0);
        BUILDER.pop();
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    
    public static final ModConfigSpec.DoubleValue INSTABILITY_SPEED;
}
