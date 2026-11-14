package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Thorns trait - reflects damage back to attackers.
 * Unlocks at level 10 for armor.
 */
public class ThornsTrait extends Trait {

    private final float reflectPercent;

    public ThornsTrait(int unlockLevel, float reflectPercent) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "thorns"),
            "Thorns",
            String.format("Reflect %.0f%% damage to attackers", reflectPercent * 100),
            unlockLevel,
            Set.of(ItemCategory.ARMOR, ItemCategory.SHIELD),
            1
        );
        this.reflectPercent = reflectPercent;
    }

    @Override
    public void onHurt(Player player, DamageSource source, ItemStack armor,
                       float damage, ItemLevelData data) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            float reflectedDamage = damage * reflectPercent;
            if (reflectedDamage >= 1f) {
                attacker.hurt(player.damageSources().thorns(player), reflectedDamage);
            }
        }
    }
}
