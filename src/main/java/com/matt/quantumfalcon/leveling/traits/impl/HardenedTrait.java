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
 * Hardened trait - flat damage reduction.
 * Unlocks at level 50 for armor.
 */
public class HardenedTrait extends Trait {

    private final float damageReduction;

    public HardenedTrait(int unlockLevel, float damageReduction) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "hardened"),
            "Hardened",
            String.format("Reduce all damage by %.0f%%", damageReduction * 100),
            unlockLevel,
            Set.of(ItemCategory.ARMOR),
            1
        );
        this.damageReduction = damageReduction;
    }

    @Override
    public float onDamageTaken(Player player, DamageSource source, ItemStack armor,
                               float damage, ItemLevelData data) {
        return damage * (1f - damageReduction);
    }
}
