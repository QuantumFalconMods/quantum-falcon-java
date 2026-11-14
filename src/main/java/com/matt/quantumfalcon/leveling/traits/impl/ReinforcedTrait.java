package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Reinforced trait - granted by Epic+ rarity.
 * Increases armor value (implemented via armor multiplier).
 */
public class ReinforcedTrait extends Trait {

    public ReinforcedTrait() {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "reinforced"),
            "Reinforced",
            "+5% armor",
            Set.of(ItemCategory.ARMOR, ItemCategory.SHIELD)
        );
    }
}
