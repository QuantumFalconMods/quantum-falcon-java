package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Quantum Harvest trait - chance to duplicate drops.
 * Unlocks at level 100 for tools (legendary capstone).
 */
public class QuantumHarvestTrait extends Trait {

    private final float dupeChance;

    public QuantumHarvestTrait(int unlockLevel, float dupeChance) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "quantum_harvest"),
            "Quantum Harvest",
            String.format("%.0f%% chance to duplicate drops", dupeChance * 100),
            unlockLevel,
            Set.of(ItemCategory.TOOL),
            1
        );
        this.dupeChance = dupeChance;
    }

    public float getDupeChance() {
        return dupeChance;
    }
}
