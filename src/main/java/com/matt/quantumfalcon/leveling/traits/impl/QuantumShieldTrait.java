package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Quantum Shield trait - chance to negate damage completely.
 * Unlocks at level 100 for armor (legendary capstone).
 */
public class QuantumShieldTrait extends Trait {

    private final float negateChance;

    public QuantumShieldTrait(int unlockLevel, float negateChance) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "quantum_shield"),
            "Quantum Shield",
            String.format("%.0f%% chance to negate damage", negateChance * 100),
            unlockLevel,
            Set.of(ItemCategory.ARMOR),
            1
        );
        this.negateChance = negateChance;
    }

    @Override
    public float onDamageTaken(Player player, DamageSource source, ItemStack armor,
                               float damage, ItemLevelData data) {
        if (player.getRandom().nextFloat() < negateChance) {
            // TODO: Add quantum particle effect
            return 0;
        }
        return damage;
    }
}
