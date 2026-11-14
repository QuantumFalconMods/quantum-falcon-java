package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Fortune Boost trait - increases fortune enchantment effect.
 * Unlocks at level 50 for tools.
 * Note: Actual implementation is in block break events.
 */
public class FortuneBoostTrait extends Trait {

    private final int bonusFortuneLevel;

    public FortuneBoostTrait(int unlockLevel, int bonusFortuneLevel) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "fortune_boost"),
            "Fortune Boost",
            String.format("+%d effective Fortune level", bonusFortuneLevel),
            unlockLevel,
            Set.of(ItemCategory.TOOL),
            1
        );
        this.bonusFortuneLevel = bonusFortuneLevel;
    }

    public int getBonusFortuneLevel() {
        return bonusFortuneLevel;
    }
}
