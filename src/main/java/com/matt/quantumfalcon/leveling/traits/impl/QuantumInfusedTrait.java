package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Quantum Infused trait - granted by Quantum rarity only.
 * All stats boosted, special quantum effects, massive XP bonus.
 */
public class QuantumInfusedTrait extends Trait {

    private static final float XP_BONUS = 2.0f;

    public QuantumInfusedTrait() {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "quantum_infused"),
            "Quantum Infused",
            "+100% XP, quantum aura",
            Set.of(ItemCategory.WEAPON, ItemCategory.ARMOR, ItemCategory.TOOL, ItemCategory.RANGED, ItemCategory.SHIELD)
        );
    }

    @Override
    public float getXpMultiplier(ItemStack stack, ItemLevelData data) {
        return XP_BONUS;
    }

    @Override
    public void onTick(Player player, ItemStack stack, ItemLevelData data, boolean isSelected) {
        if (!isSelected) return;
        if (player.level().isClientSide()) return;

        // Quantum aura effect - spawn particles every 10 ticks
        // TODO: Add quantum particle effect
    }
}
