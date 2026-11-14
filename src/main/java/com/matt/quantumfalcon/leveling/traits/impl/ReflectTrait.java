package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Reflect trait - chance to reflect projectiles.
 * Unlocks at level 90 for armor.
 */
public class ReflectTrait extends Trait {

    private final float reflectChance;

    public ReflectTrait(int unlockLevel, float reflectChance) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "reflect"),
            "Reflect",
            String.format("%.0f%% chance to reflect projectiles", reflectChance * 100),
            unlockLevel,
            Set.of(ItemCategory.ARMOR, ItemCategory.SHIELD),
            1
        );
        this.reflectChance = reflectChance;
    }

    @Override
    public float onDamageTaken(Player player, DamageSource source, ItemStack armor,
                               float damage, ItemLevelData data) {
        if (source.getDirectEntity() instanceof Projectile projectile) {
            if (player.getRandom().nextFloat() < reflectChance) {
                // Reverse projectile direction
                Vec3 motion = projectile.getDeltaMovement();
                projectile.setDeltaMovement(motion.reverse());

                // Change owner to player
                projectile.setOwner(player);

                // Negate damage
                return 0;
            }
        }
        return damage;
    }
}
