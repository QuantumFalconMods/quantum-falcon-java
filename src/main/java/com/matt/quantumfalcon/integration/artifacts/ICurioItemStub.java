package com.matt.quantumfalcon.integration.artifacts;

import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Stub interface for Curios ICurioItem when Curios API is not available at compile time.
 * This allows the artifact items to compile without Curios dependency.
 * At runtime, if Curios is loaded, the items will be registered as Curios items via reflection.
 */
public interface ICurioItemStub {
    default void curioTick(Object ctx, ItemStack stack) {}
    default void onEquip(Object ctx, ItemStack prevStack, ItemStack stack) {}
    default void onUnequip(Object ctx, ItemStack prevStack, ItemStack stack) {}
    default boolean canEquipFromUse(Object ctx, ItemStack stack) { return true; }
    default boolean canSync(Object ctx, ItemStack stack) { return false; }
    default java.util.List<net.minecraft.network.chat.Component> getSlotsTooltip(Object ctx, ItemStack stack) { return java.util.Collections.emptyList(); }
    default java.util.List<net.minecraft.network.chat.Component> getAttributesTooltip(Object ctx, ItemStack stack) { return java.util.Collections.emptyList(); }
    default com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(Object ctx, UUID uuid, ItemStack stack) {
        return com.google.common.collect.LinkedHashMultimap.create();
    }
}
