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
 * Magic Resist trait - reduces magic damage.
 * Unlocks at level 65 for armor.
 */
public class MagicResistTrait extends Trait {

    private final float resistPercent;

    public MagicResistTrait(int unlockLevel, float resistPercent) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "magic_resist"),
            "Magic Resistance",
            String.format("Reduce magic damage by %.0f%%", resistPercent * 100),
            unlockLevel,
            Set.of(ItemCategory.ARMOR),
            1
        );
        this.resistPercent = resistPercent;
    }

    @Override
    public float onDamageTaken(Player player, DamageSource source, ItemStack armor,
                               float damage, ItemLevelData data) {
        // Magic damage detection using damage type tags (NeoForge 1.21.3 compatible)
        // WITCH_RESISTANT_TO covers most magic-type damage
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
            return damage * (1f - resistPercent);
        }
        return damage;
    }
}
