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
 * Berserk trait - bonus damage at low health.
 * Unlocks at level 90 for weapons.
 */
public class BerserkTrait extends Trait {

    private final float healthThreshold;
    private final float bonusDamage;

    public BerserkTrait(int unlockLevel, float healthThreshold, float bonusDamage) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "berserk"),
            "Berserk",
            String.format("+%.0f%% damage when below %.0f%% HP", bonusDamage * 100, healthThreshold * 100),
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
        float playerHealthPercent = player.getHealth() / player.getMaxHealth();
        if (playerHealthPercent <= healthThreshold) {
            return damage * (1f + bonusDamage);
        }
        return damage;
    }
}
