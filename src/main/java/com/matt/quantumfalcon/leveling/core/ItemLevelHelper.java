package com.matt.quantumfalcon.leveling.core;

import com.matt.quantumfalcon.leveling.api.ILevelableItem;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.config.LevelingConfig;
import com.matt.quantumfalcon.leveling.rarity.Rarity;
import com.matt.quantumfalcon.leveling.rarity.RarityManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;

/**
 * Utility class for accessing and modifying item level data.
 * Handles DataComponent read/write and item type detection.
 * 
 * NeoForge 1.21.3: Uses DataComponents.CUSTOM_DATA instead of deprecated getTag()
 * @see <a href="https://docs.neoforged.net/docs/1.21.3/items/datacomponents">NeoForge Data Components</a>
 */
public class ItemLevelHelper {

    /**
     * Check if an item can be leveled
     */
    public static boolean canLevel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!LevelingConfig.isEnabled()) return false;

        Item item = stack.getItem();

        // Check if item implements our interface
        if (item instanceof ILevelableItem levelable) {
            return levelable.canBeLeveled(stack);
        }

        // Check config blacklist (safe with try-catch for early access)
        try {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId != null) {
                String idStr = itemId.toString();
                if (LevelingConfig.ITEM_BLACKLIST.get().contains(idStr)) {
                    return false;
                }
                // Check whitelist if enabled
                if (LevelingConfig.USE_WHITELIST.get()) {
                    return LevelingConfig.ITEM_WHITELIST.get().contains(idStr);
                }
            }
        } catch (IllegalStateException e) {
            // Config not loaded yet - use default detection
        }

        // Default detection by item class
        return isWeapon(stack) || isArmor(stack) || isTool(stack) || isRanged(stack) || isShield(stack);
    }

    /**
     * Determine item category
     */
    public static ItemCategory getCategory(ItemStack stack) {
        if (stack.isEmpty()) return ItemCategory.UNKNOWN;

        Item item = stack.getItem();

        // Check interface first
        if (item instanceof ILevelableItem levelable) {
            return levelable.getItemCategory(stack);
        }

        // Default detection
        if (isWeapon(stack)) return ItemCategory.WEAPON;
        if (isArmor(stack)) return ItemCategory.ARMOR;
        if (isRanged(stack)) return ItemCategory.RANGED;
        if (isShield(stack)) return ItemCategory.SHIELD;
        if (isTool(stack)) return ItemCategory.TOOL;

        return ItemCategory.UNKNOWN;
    }

    /**
     * Check if item is a melee weapon
     */
    public static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof SwordItem ||
               item instanceof AxeItem ||
               item instanceof TridentItem;
    }

    /**
     * Check if item is armor
     */
    public static boolean isArmor(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof ArmorItem;
    }

    /**
     * Check if item is a tool (not weapon)
     */
    public static boolean isTool(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof PickaxeItem ||
               item instanceof ShovelItem ||
               item instanceof HoeItem;
    }

    /**
     * Check if item is a ranged weapon
     */
    public static boolean isRanged(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof BowItem ||
               item instanceof CrossbowItem;
    }

    /**
     * Check if item is a shield
     */
    public static boolean isShield(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof ShieldItem;
    }

    /**
     * Get level data from an item, creating if not present.
     * NeoForge 1.21.3: Uses DataComponents.CUSTOM_DATA for NBT storage.
     */
    @Nullable
    public static ItemLevelData getLevelData(ItemStack stack) {
        if (!canLevel(stack)) return null;

        // Use DataComponents.CUSTOM_DATA to get custom NBT data (1.21.3 API)
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        ItemLevelData data;

        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(ItemLevelData.TAG_ROOT)) {
                data = ItemLevelData.load(tag.getCompound(ItemLevelData.TAG_ROOT));
            } else {
                data = new ItemLevelData();
            }
        } else {
            data = new ItemLevelData();
        }

        return data;
    }

    /**
     * Save level data to an item.
     * NeoForge 1.21.3: Uses DataComponents.CUSTOM_DATA for NBT storage.
     */
    public static void saveLevelData(ItemStack stack, ItemLevelData data) {
        if (!canLevel(stack) || data == null) return;

        // Get existing custom data or create new
        CustomData existingData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag;
        
        if (existingData != null) {
            tag = existingData.copyTag();
        } else {
            tag = new CompoundTag();
        }
        
        // Store level data in the tag
        tag.put(ItemLevelData.TAG_ROOT, data.save());
        
        // Set the updated custom data back to the stack
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Check if item already has level data.
     * NeoForge 1.21.3: Uses DataComponents.CUSTOM_DATA for NBT storage.
     */
    public static boolean hasLevelData(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        
        CompoundTag tag = customData.copyTag();
        return tag.contains(ItemLevelData.TAG_ROOT);
    }

    /**
     * Initialize level data on a new item (assigns rarity, etc.)
     */
    public static void initializeItem(ItemStack stack, @Nullable Player player) {
        if (!canLevel(stack)) return;

        ItemLevelData data = getLevelData(stack);
        if (data == null || data.isInitialized()) return;

        // Roll random rarity
        Rarity rarity;
        if (player != null) {
            rarity = RarityManager.rollRarity(player.getRandom());
        } else {
            rarity = RarityManager.rollRarity(new java.util.Random());
        }
        data.setRarity(rarity);

        // Apply rarity bonuses and traits
        RarityManager.applyRarityBonuses(data);

        data.setInitialized(true);
        saveLevelData(stack, data);
    }

    /**
     * Initialize item if not already initialized
     */
    public static void ensureInitialized(ItemStack stack, @Nullable Player player) {
        if (!canLevel(stack)) return;
        if (hasLevelData(stack)) {
            ItemLevelData data = getLevelData(stack);
            if (data != null && data.isInitialized()) {
                return;
            }
        }
        initializeItem(stack, player);
    }

    // ==================== QUICK ACCESS METHODS ====================

    /**
     * Get item level (0 if not levelable)
     */
    public static int getLevel(ItemStack stack) {
        ItemLevelData data = getLevelData(stack);
        return data != null ? data.getLevel() : 0;
    }

    /**
     * Get item XP (0 if not levelable)
     */
    public static int getXp(ItemStack stack) {
        ItemLevelData data = getLevelData(stack);
        return data != null ? data.getXp() : 0;
    }

    /**
     * Get item rarity
     */
    public static Rarity getRarity(ItemStack stack) {
        ItemLevelData data = getLevelData(stack);
        return data != null ? data.getRarity() : Rarity.COMMON;
    }

    /**
     * Get total XP earned by item
     */
    public static long getTotalXp(ItemStack stack) {
        ItemLevelData data = getLevelData(stack);
        return data != null ? data.getTotalXp() : 0;
    }

    /**
     * Get kill count for item
     */
    public static int getKills(ItemStack stack) {
        ItemLevelData data = getLevelData(stack);
        return data != null ? data.getKills() : 0;
    }

    /**
     * Check if item has a specific trait
     */
    public static boolean hasTrait(ItemStack stack, ResourceLocation traitId) {
        ItemLevelData data = getLevelData(stack);
        return data != null && data.hasTrait(traitId);
    }

    /**
     * Get a bonus stat value
     */
    public static float getBonusStat(ItemStack stack, String statId) {
        ItemLevelData data = getLevelData(stack);
        return data != null ? data.getBonusStat(statId) : 0f;
    }

    /**
     * Check if item is soulbound (kept on death)
     */
    public static boolean isSoulbound(ItemStack stack) {
        ItemLevelData data = getLevelData(stack);
        return data != null && data.isSoulbound();
    }

    /**
     * Get max level for an item
     */
    public static int getMaxLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        Item item = stack.getItem();
        if (item instanceof ILevelableItem levelable) {
            return levelable.getMaxLevel(stack);
        }

        return LevelingConfig.MAX_LEVEL.get();
    }

    /**
     * Get XP multiplier for an item
     */
    public static float getXpMultiplier(ItemStack stack) {
        if (stack.isEmpty()) return 1.0f;

        Item item = stack.getItem();
        if (item instanceof ILevelableItem levelable) {
            return levelable.getXpMultiplier(stack);
        }

        return 1.0f;
    }
}
