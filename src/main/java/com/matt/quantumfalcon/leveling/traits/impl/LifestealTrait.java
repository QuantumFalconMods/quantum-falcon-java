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
 * Lifesteal trait - heals player for a percentage of damage dealt.
 * Unlocks at level 10 for weapons.
 */
public class LifestealTrait extends Trait {

    private final float healPercent;

    public LifestealTrait(int unlockLevel, float healPercent) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "lifesteal"),
            "Lifesteal",
            String.format("Heal %.0f%% of damage dealt", healPercent * 100),
            unlockLevel,
            Set.of(ItemCategory.WEAPON, ItemCategory.RANGED),
            1
        );
        this.healPercent = healPercent;
    }

    @Override
    public void onDamageDealt(Player player, LivingEntity target, ItemStack weapon,
                              float damage, ItemLevelData data) {
        if (damage > 0) {
            float heal = damage * healPercent;
            if (heal >= 0.5f) {
                player.heal(heal);
            }
        }
    }
}
