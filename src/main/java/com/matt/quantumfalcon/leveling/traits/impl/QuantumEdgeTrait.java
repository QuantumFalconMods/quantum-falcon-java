package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Quantum Edge trait - small chance to instantly kill non-boss enemies.
 * Unlocks at level 100 for weapons (legendary capstone).
 */
public class QuantumEdgeTrait extends Trait {

    private final float instantKillChance;

    public QuantumEdgeTrait(int unlockLevel, float chance) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "quantum_edge"),
            "Quantum Edge",
            String.format("%.0f%% chance to instantly kill non-bosses", chance * 100),
            unlockLevel,
            Set.of(ItemCategory.WEAPON),
            1
        );
        this.instantKillChance = chance;
    }

    @Override
    public float onDamageCalculation(Player player, LivingEntity target, ItemStack weapon,
                                     float damage, ItemLevelData data) {
        // Don't work on bosses
        if (isBoss(target)) return damage;

        // Roll for instant kill
        if (player.getRandom().nextFloat() < instantKillChance) {
            // Deal damage equal to remaining health + 1
            return target.getHealth() + 1;
        }

        return damage;
    }

    private boolean isBoss(LivingEntity entity) {
        // Check for vanilla bosses and high-health mini-bosses
        return entity instanceof WitherBoss ||
               entity instanceof EnderDragon ||
               entity instanceof net.minecraft.world.entity.monster.warden.Warden ||
               entity.getMaxHealth() >= 100; // Mini-boss threshold
    }
}
