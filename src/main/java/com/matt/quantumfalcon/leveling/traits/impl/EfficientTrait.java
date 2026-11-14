package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Efficient trait - increases mining speed.
 * Unlocks at level 25 for tools.
 * Note: Actual implementation is in the attribute handler.
 */
public class EfficientTrait extends Trait {

    private final float speedBoost;

    public EfficientTrait(int unlockLevel, float speedBoost) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "efficient"),
            "Efficient",
            String.format("+%.0f%% mining speed", speedBoost * 100),
            unlockLevel,
            Set.of(ItemCategory.TOOL),
            1
        );
        this.speedBoost = speedBoost;
    }

    public float getSpeedBoost() {
        return speedBoost;
    }
}
