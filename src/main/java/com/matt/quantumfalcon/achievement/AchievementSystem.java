package com.matt.quantumfalcon.achievement;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.skill.PlayerSkillData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;

import java.util.*;

/**
 * Achievement System for Quantum Falcon mod.
 * 30+ achievements across multiple categories with skill point rewards.
 */
public class AchievementSystem {
    private static final Map<String, Achievement> ACHIEVEMENTS = new HashMap<>();
    private static final String NBT_KEY = "quantumfalcon_achievements";

    static {
        registerAchievements();
    }

    private static void registerAchievements() {
        // ========================================
        // FALCON ACHIEVEMENTS (5)
        // ========================================
        register(new Achievement("first_tame", "Falcon Friend", 
            "Tame your first falcon", AchievementTier.BRONZE, 1));
        register(new Achievement("breed_falcon", "Falcon Breeder", 
            "Breed two falcons", AchievementTier.BRONZE, 1));
        register(new Achievement("rare_falcon", "Rare Collector", 
            "Obtain a rare falcon variant", AchievementTier.SILVER, 2));
        register(new Achievement("legendary_falcon", "Legendary Master", 
            "Obtain a legendary falcon", AchievementTier.GOLD, 5));
        register(new Achievement("all_legendaries", "Ultimate Collector", 
            "Obtain all 4 legendary falcons", AchievementTier.DIAMOND, 10));

        // ========================================
        // EXPLORATION ACHIEVEMENTS (4)
        // ========================================
        register(new Achievement("first_biome", "Explorer", 
            "Discover a Quantum biome", AchievementTier.BRONZE, 1));
        register(new Achievement("all_biomes", "World Wanderer", 
            "Visit all 10 custom biomes", AchievementTier.GOLD, 5));
        register(new Achievement("find_structure", "Archaeologist", 
            "Find a custom structure", AchievementTier.BRONZE, 1));
        register(new Achievement("all_structures", "Master Explorer", 
            "Find all 7 structure types", AchievementTier.DIAMOND, 10));

        // ========================================
        // EVENT ACHIEVEMENTS (4)
        // ========================================
        register(new Achievement("see_aurora", "Stargazer", 
            "Witness the Aurora Borealis", AchievementTier.BRONZE, 1));
        register(new Achievement("survive_storm", "Storm Chaser", 
            "Survive a Quantum Storm", AchievementTier.SILVER, 2));
        register(new Achievement("meteor_loot", "Meteor Hunter", 
            "Collect loot from a meteor", AchievementTier.BRONZE, 1));
        register(new Achievement("all_events", "Event Master", 
            "Experience all 4 dynamic events", AchievementTier.GOLD, 5));

        // ========================================
        // SKILL ACHIEVEMENTS (4)
        // ========================================
        register(new Achievement("first_skill", "Skill Learner", 
            "Learn your first skill", AchievementTier.BRONZE, 1));
        register(new Achievement("max_skill", "Skill Master", 
            "Max out a skill to level 10", AchievementTier.SILVER, 2));
        register(new Achievement("ultimate_skill", "Ultimate Power", 
            "Unlock an Ultimate skill", AchievementTier.GOLD, 5));
        register(new Achievement("all_skills", "Omnipotent", 
            "Max out all 50 skills", AchievementTier.DIAMOND, 20));

        // ========================================
        // COMBAT ACHIEVEMENTS (4)
        // ========================================
        register(new Achievement("first_boss", "Boss Slayer", 
            "Defeat your first boss", AchievementTier.SILVER, 2));
        register(new Achievement("all_bosses", "Boss Hunter", 
            "Defeat all legendary bosses", AchievementTier.DIAMOND, 15));
        register(new Achievement("enchant_weapon", "Enchanter", 
            "Apply a custom enchantment", AchievementTier.BRONZE, 1));
        register(new Achievement("full_enchants", "Master Enchanter", 
            "Apply all 8 custom enchantments", AchievementTier.GOLD, 5));

        // ========================================
        // QUEST ACHIEVEMENTS (4)
        // ========================================
        register(new Achievement("first_quest", "Quest Taker", 
            "Complete your first quest", AchievementTier.BRONZE, 1));
        register(new Achievement("quest_chain", "Chain Breaker", 
            "Complete a quest chain", AchievementTier.SILVER, 2));
        register(new Achievement("all_main_quests", "Hero's Journey", 
            "Complete all main story quests", AchievementTier.GOLD, 10));
        register(new Achievement("all_quests", "Quest Master", 
            "Complete all available quests", AchievementTier.DIAMOND, 15));

        // ========================================
        // STORY ACHIEVEMENTS (4)
        // ========================================
        register(new Achievement("light_ending", "Morning", 
            "Achieve the Light Ending", AchievementTier.GOLD, 5));
        register(new Achievement("dark_ending", "The Last Night", 
            "Achieve the Dark Ending", AchievementTier.GOLD, 5));
        register(new Achievement("true_ending", "The Braided Hour", 
            "Achieve the True Ending", AchievementTier.DIAMOND, 20));
        register(new Achievement("all_endings", "Reality Master", 
            "Experience all three endings", AchievementTier.DIAMOND, 25));

        // ========================================
        // MISC ACHIEVEMENTS (5)
        // ========================================
        register(new Achievement("quantum_ore", "Quantum Miner", 
            "Mine your first Quantum Ore", AchievementTier.BRONZE, 1));
        register(new Achievement("void_ore", "Void Delver", 
            "Mine your first Void Ore", AchievementTier.BRONZE, 1));
        register(new Achievement("full_armor", "Quantum Warrior", 
            "Craft full Quantum armor set", AchievementTier.SILVER, 2));
        register(new Achievement("heavy_fog", "Lost in Fog", 
            "Experience heavy fog", AchievementTier.BRONZE, 1));
        register(new Achievement("die_to_meteor", "Stardust", 
            "Get killed by a meteor impact", AchievementTier.BRONZE, 1));
    }

    private static void register(Achievement achievement) {
        ACHIEVEMENTS.put(achievement.id, achievement);
    }

    // ========================================
    // PUBLIC API
    // ========================================

    public static boolean unlockAchievement(Player player, String achievementId) {
        if (hasAchievement(player, achievementId)) {
            return false;
        }
        
        Achievement achievement = ACHIEVEMENTS.get(achievementId);
        if (achievement == null) {
            return false;
        }
        
        // Check prerequisite
        if (achievement.prerequisiteId != null && !hasAchievement(player, achievement.prerequisiteId)) {
            return false;
        }
        
        // Save achievement
        CompoundTag nbt = player.getPersistentData();
        ListTag achievementsTag = nbt.contains(NBT_KEY) 
            ? nbt.getList(NBT_KEY, Tag.TAG_STRING) 
            : new ListTag();
        achievementsTag.add(StringTag.valueOf(achievementId));
        nbt.put(NBT_KEY, achievementsTag);
        
        // B5: achievements no longer pay skill points (the supply is the fixed list in SkillPointSystem)
        
        // Announce
        if (player instanceof ServerPlayer serverPlayer) {
            announceAchievement(serverPlayer, achievement);
        }
        
        return true;
    }

    public static boolean hasAchievement(Player player, String achievementId) {
        CompoundTag nbt = player.getPersistentData();
        if (!nbt.contains(NBT_KEY)) {
            return false;
        }
        ListTag achievementsTag = nbt.getList(NBT_KEY, Tag.TAG_STRING);
        for (int i = 0; i < achievementsTag.size(); i++) {
            if (achievementsTag.getString(i).equals(achievementId)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> getUnlockedAchievements(Player player) {
        List<String> unlocked = new ArrayList<>();
        CompoundTag nbt = player.getPersistentData();
        if (!nbt.contains(NBT_KEY)) {
            return unlocked;
        }
        ListTag achievementsTag = nbt.getList(NBT_KEY, Tag.TAG_STRING);
        for (int i = 0; i < achievementsTag.size(); i++) {
            unlocked.add(achievementsTag.getString(i));
        }
        return unlocked;
    }

    public static double getProgress(Player player) {
        int total = ACHIEVEMENTS.size();
        int unlocked = getUnlockedAchievements(player).size();
        return (double) unlocked / total;
    }

    public static int getTotalAchievements() {
        return ACHIEVEMENTS.size();
    }

    public static Achievement getAchievement(String id) {
        return ACHIEVEMENTS.get(id);
    }

    public static Collection<Achievement> getAllAchievements() {
        return ACHIEVEMENTS.values();
    }

    private static void announceAchievement(ServerPlayer player, Achievement achievement) {
        // Header
        player.sendSystemMessage(Component.literal("═══════════════════════════════════")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("   🏆 ACHIEVEMENT UNLOCKED! 🏆")
            .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("═══════════════════════════════════")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.empty());
        
        // Achievement details
        player.sendSystemMessage(Component.literal(achievement.name)
            .withStyle(achievement.tier.color, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal(achievement.description)
            .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.empty());
        
        // Tier and reward
        player.sendSystemMessage(Component.literal("Tier: " + achievement.tier.displayName)
            .withStyle(achievement.tier.color));
        if (achievement.rewardSkillPoints > 0) {
            player.sendSystemMessage(Component.literal("Reward: +" + (achievement.rewardSkillPoints * 100) + " XP")
                .withStyle(ChatFormatting.AQUA));
        }
        
        // Footer
        player.sendSystemMessage(Component.literal("═══════════════════════════════════")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        
        // Sound
        player.level().playSound(null, player.blockPosition(), 
            SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0f, 1.0f);
    }

    public static void register(IEventBus modEventBus) {
        QuantumFalcon.LOGGER.info("⚡ Achievement System initialized with {} achievements", ACHIEVEMENTS.size());
    }

    // ========================================
    // INNER CLASSES
    // ========================================

    public static class Achievement {
        public final String id;
        public final String name;
        public final String description;
        public final AchievementTier tier;
        public final int rewardSkillPoints;
        public final String prerequisiteId;

        public Achievement(String id, String name, String desc, AchievementTier tier, int reward) {
            this(id, name, desc, tier, reward, null);
        }

        public Achievement(String id, String name, String desc, AchievementTier tier, int reward, String prerequisiteId) {
            this.id = id;
            this.name = name;
            this.description = desc;
            this.tier = tier;
            this.rewardSkillPoints = reward;
            this.prerequisiteId = prerequisiteId;
        }
    }

    public enum AchievementTier {
        BRONZE("Bronze", ChatFormatting.GOLD),
        SILVER("Silver", ChatFormatting.GRAY),
        GOLD("Gold", ChatFormatting.YELLOW),
        DIAMOND("Diamond", ChatFormatting.AQUA);

        public final String displayName;
        public final ChatFormatting color;

        AchievementTier(String name, ChatFormatting color) {
            this.displayName = name;
            this.color = color;
        }
    }
}
