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
 * Void Strike trait - ignores a percentage of target's armor.
 * Unlocks at level 50 for weapons.
 * Note: Actual armor penetration is implemented in event handler.
 */
public class VoidStrikeTrait extends Trait {

    private final float penetration;

    public VoidStrikeTrait(int unlockLevel, float penetration) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "void_strike"),
            "Void Strike",
            String.format("Ignore %.0f%% of armor", penetration * 100),
            unlockLevel,
            Set.of(ItemCategory.WEAPON),
            1
        );
        this.penetration = penetration;
    }

    public float getPenetration() {
        return penetration;
    }

    @Override
    public float onDamageCalculation(Player player, LivingEntity target, ItemStack weapon,
                                     float damage, ItemLevelData data) {
        // Bonus damage to compensate for armor - crude approximation
        // Actual implementation should modify the damage source
        float targetArmor = target.getArmorValue();
        if (targetArmor > 0) {
            float bonusDamage = damage * (targetArmor / 20f) * penetration;
            return damage + bonusDamage;
        }
        return damage;
    }
}
