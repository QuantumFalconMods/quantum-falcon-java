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
 * Last Stand trait - damage reduction when at low health.
 * Unlocks at level 75 for armor.
 */
public class LastStandTrait extends Trait {

    private final float healthThreshold;
    private final float damageReduction;

    public LastStandTrait(int unlockLevel, float healthThreshold, float damageReduction) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "last_stand"),
            "Last Stand",
            String.format("%.0f%% damage reduction below %.0f%% HP", damageReduction * 100, healthThreshold * 100),
            unlockLevel,
            Set.of(ItemCategory.ARMOR),
            1
        );
        this.healthThreshold = healthThreshold;
        this.damageReduction = damageReduction;
    }

    @Override
    public float onDamageTaken(Player player, DamageSource source, ItemStack armor,
                               float damage, ItemLevelData data) {
        float healthPercent = player.getHealth() / player.getMaxHealth();
        if (healthPercent <= healthThreshold) {
            return damage * (1f - damageReduction);
        }
        return damage;
    }
}
