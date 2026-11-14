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
 * Fire Aspect trait - sets targets on fire.
 * Unlocks at level 40 for weapons.
 */
public class FireAspectTrait extends Trait {

    private final int burnSeconds;

    public FireAspectTrait(int unlockLevel, int burnSeconds) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "fire_aspect"),
            "Fire Aspect",
            String.format("Set targets on fire for %ds", burnSeconds),
            unlockLevel,
            Set.of(ItemCategory.WEAPON),
            1
        );
        this.burnSeconds = burnSeconds;
    }

    @Override
    public void onDamageDealt(Player player, LivingEntity target, ItemStack weapon,
                              float damage, ItemLevelData data) {
        if (!target.fireImmune()) {
            target.setRemainingFireTicks(burnSeconds * 20);
        }
    }
}
