package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;

/**
 * Chain Lightning trait - damage chains to nearby enemies.
 * Unlocks at level 65 for weapons.
 */
public class ChainLightningTrait extends Trait {

    private final int maxTargets;
    private final float damageMultiplier;

    public ChainLightningTrait(int unlockLevel, int maxTargets, float damageMultiplier) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "chain_lightning"),
            "Chain Lightning",
            String.format("Chains to %d nearby enemies for %.0f%% damage", maxTargets, damageMultiplier * 100),
            unlockLevel,
            Set.of(ItemCategory.WEAPON),
            1
        );
        this.maxTargets = maxTargets;
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public void onDamageDealt(Player player, LivingEntity target, ItemStack weapon,
                              float damage, ItemLevelData data) {
        if (target.level().isClientSide()) return;

        float chainDamage = damage * damageMultiplier;
        if (chainDamage < 1) return;

        // Find nearby hostile entities
        AABB searchBox = target.getBoundingBox().inflate(5.0);
        List<LivingEntity> nearbyEntities = target.level().getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            e -> e != target && e != player && e.isAlive() && !e.isAlliedTo(player)
        );

        // Chain to up to maxTargets
        int chained = 0;
        for (LivingEntity entity : nearbyEntities) {
            if (chained >= maxTargets) break;
            if (entity.isInvulnerable()) continue;

            entity.hurt(player.damageSources().playerAttack(player), chainDamage);
            chained++;

            // Visual effect (optional particle spawn)
            // Can be added via particle spawning here
        }
    }
}
