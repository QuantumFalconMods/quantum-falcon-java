package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Blessed trait - granted by Legendary+ rarity.
 * Increases XP gain from all sources.
 */
public class BlessedTrait extends Trait {

    private static final float XP_BONUS = 1.25f;

    public BlessedTrait() {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "blessed"),
            "Blessed",
            "+25% XP gain",
            Set.of(ItemCategory.WEAPON, ItemCategory.ARMOR, ItemCategory.TOOL, ItemCategory.RANGED, ItemCategory.SHIELD)
        );
    }

    @Override
    public float getXpMultiplier(ItemStack stack, ItemLevelData data) {
        return XP_BONUS;
    }
}
