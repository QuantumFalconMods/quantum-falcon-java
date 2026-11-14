package com.matt.quantumfalcon.client;

import com.matt.quantumfalcon.registry.ModGearItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** "pull"/"pulling" model predicates for the custom bows (vanilla only registers them for minecraft:bow). */
public final class QFItemProperties {
    private QFItemProperties() {}
    public static void register() {
        ItemProperties.register(com.matt.quantumfalcon.registry.ModItems.FALCON_EGG.get(),
            ResourceLocation.fromNamespaceAndPath(com.matt.quantumfalcon.QuantumFalcon.MOD_ID, "tier"),
            (stack, level, e, seed) -> com.matt.quantumfalcon.item.FalconEggItem.tierOf(stack) / 10f);
        for (Item bow : new Item[]{ModGearItems.QUANTUM_BOW.get(), ModGearItems.SINGULARITY_BOW.get()}) {
            ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"), (stack, level, e, seed) ->
                    e == null || e.getUseItem() != stack ? 0f : (stack.getUseDuration(e) - e.getUseItemRemainingTicks()) / 20f);
            ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, e, seed) ->
                    e != null && e.isUsingItem() && e.getUseItem() == stack ? 1f : 0f);
        }
    }
}
