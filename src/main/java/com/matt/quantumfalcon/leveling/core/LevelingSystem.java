package com.matt.quantumfalcon.leveling.core;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.config.LevelingConfig;
import com.matt.quantumfalcon.leveling.rarity.RarityManager;
import com.matt.quantumfalcon.leveling.traits.Trait;
import com.matt.quantumfalcon.leveling.traits.TraitManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Core leveling system - handles XP calculation, level ups, and trait unlocks.
 * Central API for the item leveling system.
 */
public class LevelingSystem {

    /**
     * Add XP to an item and handle level ups
     * @return true if a level up occurred
     */
    public static boolean addXp(ItemStack stack, int amount, Player player) {
        if (!LevelingConfig.isEnabled()) return false;
        if (!ItemLevelHelper.canLevel(stack)) return false;
        if (amount <= 0) return false;

        ItemLevelData data = ItemLevelHelper.getLevelData(stack);
        if (data == null) return false;

        ItemLevelHelper.ensureInitialized(stack, player);

        // Apply rarity XP multiplier
        amount = RarityManager.calculateXpWithRarity(amount, data);

        // Apply trait XP multipliers
        float traitMult = 1.0f;
        for (ResourceLocation traitId : data.getActiveTraits()) {
            Trait trait = TraitManager.getTrait(traitId);
            if (trait != null) {
                traitMult *= trait.getXpMultiplier(stack, data);
            }
        }
        amount = Math.round(amount * traitMult);

        // Apply item's own multiplier
        amount = Math.round(amount * ItemLevelHelper.getXpMultiplier(stack));

        // Add XP
        data.addXp(amount);

        // Check for level up(s)
        boolean leveledUp = false;
        int maxLevel = ItemLevelHelper.getMaxLevel(stack);

        while (data.getLevel() < maxLevel && data.getXp() >= getXpForNextLevel(data.getLevel())) {
            int xpNeeded = getXpForNextLevel(data.getLevel());
            data.setXp(data.getXp() - xpNeeded);
            data.setLevel(data.getLevel() + 1);
            leveledUp = true;

            // Check for trait unlocks at new level
            checkTraitUnlocks(stack, data, player);

            // Level up effects
            if (player != null) {
                onLevelUp(stack, data, player);
            }
        }

        // Save data
        ItemLevelHelper.saveLevelData(stack, data);

        return leveledUp;
    }

    /**
     * Calculate XP required for next level
     */
    public static int getXpForNextLevel(int currentLevel) {
        double baseXp = LevelingConfig.BASE_XP.get();
        double scale = LevelingConfig.XP_SCALE.get();
        return (int) Math.ceil(baseXp * Math.pow(currentLevel, scale));
    }

    /**
     * Calculate total XP required to reach a specific level from level 1
     */
    public static long getTotalXpForLevel(int targetLevel) {
        long total = 0;
        for (int i = 1; i < targetLevel; i++) {
            total += getXpForNextLevel(i);
        }
        return total;
    }

    /**
     * Get progress percentage to next level (0.0 - 1.0)
     */
    public static float getLevelProgress(ItemStack stack) {
        ItemLevelData data = ItemLevelHelper.getLevelData(stack);
        if (data == null) return 0f;

        int maxLevel = ItemLevelHelper.getMaxLevel(stack);
        if (data.getLevel() >= maxLevel) return 1.0f;

        int xpNeeded = getXpForNextLevel(data.getLevel());
        if (xpNeeded <= 0) return 0f;

        return Math.min(1.0f, (float) data.getXp() / xpNeeded);
    }

    /**
     * Check and unlock any new traits at the current level
     */
    private static void checkTraitUnlocks(ItemStack stack, ItemLevelData data, Player player) {
        ItemCategory category = ItemLevelHelper.getCategory(stack);
        List<Trait> newTraits = TraitManager.getTraitsForLevel(data.getLevel(), category);

        for (Trait trait : newTraits) {
            if (!data.hasUnlockedTrait(trait.getId())) {
                data.unlockTrait(trait.getId());

                // Notify player
                if (player != null) {
                    notifyTraitUnlock(player, trait);
                }
            }
        }
    }

    /**
     * Handle level up effects (sound, particles, etc.)
     */
    private static void onLevelUp(ItemStack stack, ItemLevelData data, Player player) {
        // Play level up sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.2f);

        // Update stats based on new level
        updateLevelStats(stack, data);

        // Send level up message
        int level = data.getLevel();
        String itemName = stack.getHoverName().getString();

        QuantumFalcon.LOGGER.debug("Item '{}' leveled up to {}", itemName, level);

        // Milestone messages at 10, 25, 50, 75, 100
        if (level == 10 || level == 25 || level == 50 || level == 75 || level == 100) {
            // TODO: Send chat message for milestone
        }

        // Spawn particles if enabled
        if (LevelingConfig.SHOW_PARTICLES.get() && player instanceof ServerPlayer serverPlayer) {
            // TODO: Send particle packet
        }
    }

    /**
     * Update item stats based on current level
     */
    public static void updateLevelStats(ItemStack stack, ItemLevelData data) {
        int level = data.getLevel();
        ItemCategory category = ItemLevelHelper.getCategory(stack);

        // Calculate level-based stat bonuses
        switch (category) {
            case WEAPON -> {
                float damage = level * LevelingConfig.DAMAGE_PER_LEVEL.get().floatValue();
                float attackSpeed = level * LevelingConfig.ATTACK_SPEED_PER_LEVEL.get().floatValue();
                data.setBonusStat("level_damage", damage);
                data.setBonusStat("level_attack_speed", attackSpeed);
            }
            case ARMOR -> {
                float armor = level * LevelingConfig.ARMOR_PER_LEVEL.get().floatValue();
                float toughness = level * LevelingConfig.TOUGHNESS_PER_LEVEL.get().floatValue();
                float knockbackResist = level * LevelingConfig.KNOCKBACK_RESIST_PER_LEVEL.get().floatValue();
                data.setBonusStat("level_armor", armor);
                data.setBonusStat("level_toughness", toughness);
                data.setBonusStat("level_knockback_resist", knockbackResist);
            }
            case TOOL -> {
                float efficiency = level * LevelingConfig.EFFICIENCY_PER_LEVEL.get().floatValue();
                data.setBonusStat("level_efficiency", efficiency);
            }
            case RANGED -> {
                float damage = level * LevelingConfig.DAMAGE_PER_LEVEL.get().floatValue() * 0.5f;
                data.setBonusStat("level_damage", damage);
            }
            case SHIELD -> {
                float armor = level * LevelingConfig.ARMOR_PER_LEVEL.get().floatValue() * 0.5f;
                data.setBonusStat("level_armor", armor);
            }
            default -> {}
        }
    }

    /**
     * Notify player of trait unlock
     */
    private static void notifyTraitUnlock(Player player, Trait trait) {
        // Play unlock sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 1.5f);

        // TODO: Send chat/toast notification
        QuantumFalcon.LOGGER.debug("Player {} unlocked trait: {}", player.getName().getString(), trait.getName());
    }

    /**
     * Set item to a specific level (admin command)
     */
    public static void setLevel(ItemStack stack, int level, Player player) {
        if (!ItemLevelHelper.canLevel(stack)) return;

        ItemLevelData data = ItemLevelHelper.getLevelData(stack);
        if (data == null) return;

        ItemLevelHelper.ensureInitialized(stack, player);

        int maxLevel = ItemLevelHelper.getMaxLevel(stack);
        level = Math.max(1, Math.min(maxLevel, level));

        data.setLevel(level);
        data.setXp(0);

        // Recalculate all stats
        updateLevelStats(stack, data);

        // Unlock all traits up to this level
        ItemCategory category = ItemLevelHelper.getCategory(stack);
        List<Trait> traits = TraitManager.getTraitsUpToLevel(level, category);
        for (Trait trait : traits) {
            data.unlockTrait(trait.getId());
        }

        ItemLevelHelper.saveLevelData(stack, data);
    }

    /**
     * Add XP with source tracking for stats
     */
    public static boolean addXpFromKill(ItemStack stack, int baseXp, Player player) {
        ItemLevelData data = ItemLevelHelper.getLevelData(stack);
        if (data != null) {
            data.addKill();
            ItemLevelHelper.saveLevelData(stack, data);
        }
        return addXp(stack, baseXp, player);
    }

    /**
     * Add XP from damage with source tracking
     */
    public static boolean addXpFromDamage(ItemStack stack, float damage, Player player) {
        ItemLevelData data = ItemLevelHelper.getLevelData(stack);
        if (data != null) {
            data.addDamageDealt(damage);
            ItemLevelHelper.saveLevelData(stack, data);
        }
        int xp = (int) Math.ceil(damage * 0.5);
        return addXp(stack, Math.max(1, xp), player);
    }

    /**
     * Add XP from mining with source tracking
     */
    public static boolean addXpFromMining(ItemStack stack, Player player) {
        ItemLevelData data = ItemLevelHelper.getLevelData(stack);
        if (data != null) {
            data.addBlockMined();
            ItemLevelHelper.saveLevelData(stack, data);
        }
        return addXp(stack, LevelingConfig.XP_PER_BLOCK.get(), player);
    }
}
