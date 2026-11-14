package com.matt.quantumfalcon.leveling.api;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for items that support the Quantum Falcon leveling system.
 * Implement this on custom items for native support.
 * Vanilla and modded items are automatically detected via class checks.
 */
public interface ILevelableItem {

    /**
     * Check if this specific item stack can be leveled.
     * @param stack The item stack to check
     * @return true if the item can gain XP and level up
     */
    default boolean canBeLeveled(ItemStack stack) {
        return true;
    }

    /**
     * Get the category of this item for trait compatibility.
     * @param stack The item stack
     * @return The item category
     */
    ItemCategory getItemCategory(ItemStack stack);

    /**
     * Get the maximum level this item can reach.
     * @param stack The item stack
     * @return Maximum level (default 100)
     */
    default int getMaxLevel(ItemStack stack) {
        return 100;
    }

    /**
     * Get the XP multiplier for this item type.
     * Higher values mean faster leveling.
     * @param stack The item stack
     * @return XP multiplier (default 1.0)
     */
    default float getXpMultiplier(ItemStack stack) {
        return 1.0f;
    }

    /**
     * Get the base damage bonus per level for weapons.
     * @param stack The item stack
     * @return Damage per level (default uses config)
     */
    default float getDamagePerLevel(ItemStack stack) {
        return -1; // Use config default
    }

    /**
     * Get the base armor bonus per level for armor.
     * @param stack The item stack
     * @return Armor per level (default uses config)
     */
    default float getArmorPerLevel(ItemStack stack) {
        return -1; // Use config default
    }

    /**
     * Item categories for trait compatibility and stat calculations.
     */
    enum ItemCategory {
        WEAPON("weapon"),
        ARMOR("armor"),
        TOOL("tool"),
        RANGED("ranged"),
        SHIELD("shield"),
        UNKNOWN("unknown");

        private final String name;

        ItemCategory(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static ItemCategory byName(String name) {
            for (ItemCategory cat : values()) {
                if (cat.name.equalsIgnoreCase(name)) {
                    return cat;
                }
            }
            return UNKNOWN;
        }
    }
}
