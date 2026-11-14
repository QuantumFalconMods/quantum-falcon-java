package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Critical Boost trait - increases critical hit damage multiplier.
 * Unlocks at level 25 for weapons.
 */
public class CriticalBoostTrait extends Trait {

    private final float bonusMult;

    public CriticalBoostTrait(int unlockLevel, float bonusMultiplier) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "critical_boost"),
            "Critical Boost",
            String.format("+%.0f%% critical damage", bonusMultiplier * 100),
            unlockLevel,
            Set.of(ItemCategory.WEAPON, ItemCategory.RANGED),
            1
        );
        this.bonusMult = bonusMultiplier;
    }

    @Override
    public float onCriticalHit(Player player, LivingEntity target, ItemStack weapon,
                               float critMultiplier, ItemLevelData data) {
        // Add bonus to critical multiplier
        return critMultiplier + bonusMult;
    }
}
