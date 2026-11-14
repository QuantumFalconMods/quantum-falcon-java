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
 * Execute trait - bonus damage to low health targets.
 * Unlocks at level 75 for weapons.
 */
public class ExecuteTrait extends Trait {

    private final float healthThreshold;
    private final float bonusDamage;

    public ExecuteTrait(int unlockLevel, float healthThreshold, float bonusDamage) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "execute"),
            "Execute",
            String.format("+%.0f%% damage to targets below %.0f%% HP", bonusDamage * 100, healthThreshold * 100),
            unlockLevel,
            Set.of(ItemCategory.WEAPON),
            1
        );
        this.healthThreshold = healthThreshold;
        this.bonusDamage = bonusDamage;
    }

    @Override
    public float onDamageCalculation(Player player, LivingEntity target, ItemStack weapon,
                                     float damage, ItemLevelData data) {
        float targetHealthPercent = target.getHealth() / target.getMaxHealth();
        if (targetHealthPercent <= healthThreshold) {
            return damage * (1f + bonusDamage);
        }
        return damage;
    }
}
