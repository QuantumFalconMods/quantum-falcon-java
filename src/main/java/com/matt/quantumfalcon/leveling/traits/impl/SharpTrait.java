package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Sharp trait - granted by Rare+ rarity.
 * Increases damage slightly (implemented via damage multiplier).
 */
public class SharpTrait extends Trait {

    public SharpTrait() {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "sharp"),
            "Sharp",
            "+5% damage",
            Set.of(ItemCategory.WEAPON, ItemCategory.RANGED)
        );
    }
}
