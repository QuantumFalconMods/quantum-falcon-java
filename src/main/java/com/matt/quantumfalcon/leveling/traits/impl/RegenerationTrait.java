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
 * Regeneration trait - slowly heals player while armor is worn.
 * Unlocks at level 25 for armor.
 */
public class RegenerationTrait extends Trait {

    private final float healAmount;
    private final int tickInterval;

    public RegenerationTrait(int unlockLevel, float healAmount, int tickInterval) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "regeneration"),
            "Regeneration",
            String.format("Heal %.1f HP every %ds", healAmount, tickInterval / 20),
            unlockLevel,
            Set.of(ItemCategory.ARMOR),
            1
        );
        this.healAmount = healAmount;
        this.tickInterval = tickInterval;
    }

    @Override
    public void onTick(Player player, ItemStack stack, ItemLevelData data, boolean isSelected) {
        if (!isSelected) return; // Only when worn
        if (player.level().isClientSide()) return;

        // Heal at interval
        if (player.tickCount % tickInterval == 0) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(healAmount);
            }
        }
    }
}
