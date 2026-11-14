package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Magnetic trait - pulls nearby items towards player.
 * Unlocks at level 10 for tools.
 */
public class MagneticTrait extends Trait {

    private static final double RANGE = 5.0;
    private static final double PULL_SPEED = 0.15;

    public MagneticTrait(int unlockLevel) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "magnetic"),
            "Magnetic",
            "Pull nearby items towards you",
            unlockLevel,
            Set.of(ItemCategory.TOOL),
            1
        );
    }

    @Override
    public void onTick(Player player, ItemStack stack, ItemLevelData data, boolean isSelected) {
        if (!isSelected) return;
        if (player.level().isClientSide()) return;
        if (player.tickCount % 5 != 0) return; // Only check every 5 ticks

        AABB searchBox = player.getBoundingBox().inflate(RANGE);
        List<ItemEntity> nearbyItems = player.level().getEntitiesOfClass(
            ItemEntity.class,
            searchBox,
            item -> !item.hasPickUpDelay()
        );

        for (ItemEntity item : nearbyItems) {
            Vec3 direction = player.position().subtract(item.position()).normalize();
            Vec3 currentMotion = item.getDeltaMovement();
            item.setDeltaMovement(
                currentMotion.x + direction.x * PULL_SPEED,
                currentMotion.y + direction.y * PULL_SPEED * 0.5,
                currentMotion.z + direction.z * PULL_SPEED
            );
        }
    }
}
