package com.matt.quantumfalcon.leveling.events;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.config.LevelingConfig;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.core.ItemLevelHelper;
import com.matt.quantumfalcon.leveling.traits.Trait;
import com.matt.quantumfalcon.leveling.traits.TraitManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Handles tick-based trait effects (regeneration, magnetic, etc.)
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class TraitTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!LevelingConfig.isEnabled()) return;

        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        // Process main hand item
        ItemStack mainHand = player.getMainHandItem();
        if (ItemLevelHelper.canLevel(mainHand)) {
            tickTraits(player, mainHand, true);
        }

        // Process off hand item
        ItemStack offHand = player.getOffhandItem();
        if (ItemLevelHelper.canLevel(offHand)) {
            tickTraits(player, offHand, true);
        }

        // Process armor
        for (ItemStack armor : player.getArmorSlots()) {
            if (ItemLevelHelper.canLevel(armor)) {
                tickTraits(player, armor, true);
            }
        }
    }

    /**
     * Tick all traits on an item
     */
    private static void tickTraits(Player player, ItemStack stack, boolean isSelected) {
        ItemLevelData data = ItemLevelHelper.getLevelData(stack);
        if (data == null) return;

        for (ResourceLocation traitId : data.getActiveTraits()) {
            Trait trait = TraitManager.getTrait(traitId);
            if (trait != null) {
                trait.onTick(player, stack, data, isSelected);
            }
        }
    }
}
