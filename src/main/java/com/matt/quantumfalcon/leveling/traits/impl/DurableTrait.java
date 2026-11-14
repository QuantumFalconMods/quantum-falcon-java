package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Durable trait - granted by Uncommon+ rarity.
 * Increases item durability (implemented via durability multiplier).
 */
public class DurableTrait extends Trait {

    public DurableTrait() {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "durable"),
            "Durable",
            "+5% durability",
            Set.of(ItemCategory.WEAPON, ItemCategory.ARMOR, ItemCategory.TOOL, ItemCategory.RANGED, ItemCategory.SHIELD)
        );
    }
}
