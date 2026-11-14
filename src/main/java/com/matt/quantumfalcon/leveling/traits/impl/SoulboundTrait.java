package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Soulbound trait - granted by Mythic+ rarity.
 * Item is kept on death. Implementation is in death event handler.
 */
public class SoulboundTrait extends Trait {

    public SoulboundTrait() {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "soulbound"),
            "Soulbound",
            "Keep on death",
            Set.of(ItemCategory.WEAPON, ItemCategory.ARMOR, ItemCategory.TOOL, ItemCategory.RANGED, ItemCategory.SHIELD)
        );
    }
}
