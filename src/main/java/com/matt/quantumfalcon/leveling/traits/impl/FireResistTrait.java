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
 * Fire Resist trait - reduces fire damage.
 * Unlocks at level 40 for armor.
 */
public class FireResistTrait extends Trait {

    private final float resistPercent;

    public FireResistTrait(int unlockLevel, float resistPercent) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "fire_resist"),
            "Fire Resistance",
            String.format("Reduce fire damage by %.0f%%", resistPercent * 100),
            unlockLevel,
            Set.of(ItemCategory.ARMOR),
            1
        );
        this.resistPercent = resistPercent;
    }

    @Override
    public float onDamageTaken(Player player, DamageSource source, ItemStack armor,
                               float damage, ItemLevelData data) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return damage * (1f - resistPercent);
        }
        return damage;
    }
}
