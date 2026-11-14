package com.matt.quantumfalcon.leveling.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for the Quantum Falcon item leveling system.
 * All values are server-side (common config - syncs to clients).
 */
public class LevelingConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // ==================== GENERAL ====================
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue MAX_LEVEL;
    public static final ModConfigSpec.IntValue BASE_XP;
    public static final ModConfigSpec.DoubleValue XP_SCALE;

    // ==================== XP REWARDS ====================
    // xpPerHit, alwaysShowStats and showXpBar were never read; removed
    public static final ModConfigSpec.IntValue XP_KILL_PASSIVE;
    public static final ModConfigSpec.IntValue XP_KILL_HOSTILE;
    public static final ModConfigSpec.IntValue XP_KILL_MINIBOSS;
    public static final ModConfigSpec.IntValue XP_KILL_BOSS;
    public static final ModConfigSpec.IntValue XP_PER_BLOCK;
    public static final ModConfigSpec.DoubleValue HIT_XP_CHANCE;
    public static final ModConfigSpec.DoubleValue ARMOR_XP_CHANCE;
    public static final ModConfigSpec.DoubleValue ARMOR_XP_MULTIPLIER;

    // ==================== STAT BONUSES ====================
    public static final ModConfigSpec.DoubleValue DAMAGE_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue ATTACK_SPEED_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue ARMOR_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue TOUGHNESS_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue KNOCKBACK_RESIST_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue EFFICIENCY_PER_LEVEL;

    // ==================== RARITY WEIGHTS ====================
    public static final ModConfigSpec.DoubleValue RARITY_WEIGHT_COMMON;
    public static final ModConfigSpec.DoubleValue RARITY_WEIGHT_UNCOMMON;
    public static final ModConfigSpec.DoubleValue RARITY_WEIGHT_RARE;
    public static final ModConfigSpec.DoubleValue RARITY_WEIGHT_EPIC;
    public static final ModConfigSpec.DoubleValue RARITY_WEIGHT_LEGENDARY;
    public static final ModConfigSpec.DoubleValue RARITY_WEIGHT_MYTHIC;
    public static final ModConfigSpec.DoubleValue RARITY_WEIGHT_QUANTUM;

    // ==================== ITEM LISTS ====================
    public static final ModConfigSpec.BooleanValue USE_WHITELIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_WHITELIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_BLACKLIST;

    // ==================== DISPLAY ====================
    public static final ModConfigSpec.BooleanValue SHOW_PARTICLES;

    // Track if config is loaded
    private static boolean configLoaded = false;
    
    /**
     * Safely check if leveling is enabled.
     * Returns false if config not loaded yet.
     */
    public static boolean isEnabled() {
        if (!configLoaded) return false;
        try {
            return ENABLED.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }
    
    /**
     * Mark config as loaded. Called from mod init.
     */
    public static void markLoaded() {
        configLoaded = true;
    }

    static {
        BUILDER.comment("Quantum Falcon Item Leveling System Configuration")
               .comment("Balances progression, XP gain, stat scaling, and rarity for quantum-themed items");

        // ==================== GENERAL ====================
        BUILDER.push("general");
        ENABLED = BUILDER
            .comment("Master switch for the entire leveling system")
            .define("enabled", true);

        MAX_LEVEL = BUILDER
            .comment("Maximum level an item can reach (endgame cap)")
            .defineInRange("maxLevel", 100, 1, 1000);

        BASE_XP = BUILDER
            .comment("Base XP required for level 2")
            .comment("Example: Level 10 with scale 1.5: ~576 XP needed")
            .defineInRange("baseXp", 100, 10, 100000);

        XP_SCALE = BUILDER
            .comment("Exponential scaling factor")
            .comment("Formula: XP to next level = baseXp * (currentLevel ^ xpScale)")
            .comment("1.0 = linear, 1.5 = standard RPG curve, 2.0 = very grindy")
            .defineInRange("xpScale", 1.5, 1.0, 3.0);
        BUILDER.pop();

        // ==================== XP REWARDS ====================
        BUILDER.push("xp_rewards");
        XP_KILL_PASSIVE = BUILDER
            .comment("XP for passive mobs (cow, pig)")
            .defineInRange("xpKillPassive", 5, 0, 1000);

        XP_KILL_HOSTILE = BUILDER
            .comment("XP for hostile mobs (zombie, skeleton)")
            .defineInRange("xpKillHostile", 15, 0, 1000);

        XP_KILL_MINIBOSS = BUILDER
            .comment("XP for mini-bosses (Warden, Elder Guardian)")
            .defineInRange("xpKillMiniboss", 50, 0, 5000);

        XP_KILL_BOSS = BUILDER
            .comment("XP for bosses (Wither, Dragon)")
            .defineInRange("xpKillBoss", 200, 0, 10000);

        XP_PER_BLOCK = BUILDER
            .comment("XP per block mined with tool")
            .defineInRange("xpPerBlock", 1, 0, 100);

        HIT_XP_CHANCE = BUILDER
            .comment("Chance (0.0-1.0) for weapon XP per hit")
            .defineInRange("hitXpChance", 0.3, 0.0, 1.0);

        ARMOR_XP_CHANCE = BUILDER
            .comment("Chance for armor XP when hit")
            .defineInRange("armorXpChance", 0.5, 0.0, 1.0);

        ARMOR_XP_MULTIPLIER = BUILDER
            .comment("XP multiplier based on damage taken")
            .defineInRange("armorXpMultiplier", 2.0, 0.1, 10.0);
        BUILDER.pop();

        // ==================== STAT BONUSES ====================
        BUILDER.push("stat_bonuses");
        DAMAGE_PER_LEVEL = BUILDER
            .comment("Attack damage bonus per weapon level")
            .defineInRange("damagePerLevel", 0.1, 0.0, 1.0);

        ATTACK_SPEED_PER_LEVEL = BUILDER
            .comment("Attack speed bonus per level")
            .defineInRange("attackSpeedPerLevel", 0.005, 0.0, 0.1);

        ARMOR_PER_LEVEL = BUILDER
            .comment("Armor points per armor level")
            .defineInRange("armorPerLevel", 0.05, 0.0, 1.0);

        TOUGHNESS_PER_LEVEL = BUILDER
            .comment("Armor toughness per level")
            .defineInRange("toughnessPerLevel", 0.02, 0.0, 0.5);

        KNOCKBACK_RESIST_PER_LEVEL = BUILDER
            .comment("Knockback resistance per level")
            .defineInRange("knockbackResistPerLevel", 0.002, 0.0, 0.05);

        EFFICIENCY_PER_LEVEL = BUILDER
            .comment("Mining speed bonus per tool level")
            .defineInRange("efficiencyPerLevel", 0.1, 0.0, 1.0);
        BUILDER.pop();

        // ==================== RARITY WEIGHTS ====================
        BUILDER.push("rarity_weights");
        BUILDER.comment("Relative weights for item rarity on level-up/forge")
               .comment("Higher = more common. Total weight determines quantum rarity chance")
               .comment("Example: Common 100, Quantum 1 -> Quantum ~1% chance");
        
        RARITY_WEIGHT_COMMON = BUILDER.defineInRange("common", 100.0, 0.0, 1000.0);
        RARITY_WEIGHT_UNCOMMON = BUILDER.defineInRange("uncommon", 50.0, 0.0, 1000.0);
        RARITY_WEIGHT_RARE = BUILDER.defineInRange("rare", 20.0, 0.0, 1000.0);
        RARITY_WEIGHT_EPIC = BUILDER.defineInRange("epic", 10.0, 0.0, 1000.0);
        RARITY_WEIGHT_LEGENDARY = BUILDER.defineInRange("legendary", 5.0, 0.0, 1000.0);
        RARITY_WEIGHT_MYTHIC = BUILDER.defineInRange("mythic", 2.0, 0.0, 1000.0);
        RARITY_WEIGHT_QUANTUM = BUILDER.defineInRange("quantum", 1.0, 0.0, 1000.0);
        BUILDER.pop();

        // ==================== ITEM LISTS ====================
        BUILDER.push("item_lists");
        USE_WHITELIST = BUILDER
            .comment("True: Only whitelisted items level; False: All except blacklisted")
            .define("useWhitelist", false);

        ITEM_WHITELIST = BUILDER
            .comment("Levelable items (modid:itemname) - examples for QuantumFalcon")
            .defineListAllowEmpty("whitelist", Arrays.asList(
                "quantumfalcon:quantum_sword",
                "quantumfalcon:quantum_pickaxe",
                "quantumfalcon:singularity_helmet",
                "minecraft:diamond_sword"
            ), () -> "", o -> o instanceof String);

        ITEM_BLACKLIST = BUILDER
            .comment("Non-levelable items")
            .defineListAllowEmpty("blacklist", Arrays.asList(
                "minecraft:wooden_sword",
                "minecraft:stick"
            ), () -> "", o -> o instanceof String);
        BUILDER.pop();

        // ==================== DISPLAY ====================
        BUILDER.push("display");
        SHOW_PARTICLES = BUILDER
            .comment("Level-up particle effects")
            .define("showParticles", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
