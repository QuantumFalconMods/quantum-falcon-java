/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.Item$TooltipContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TooltipFlag
 */
package com.matt.quantumfalcon.item.special;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class QuantumEggItem
extends Item {
    public QuantumEggItem(Item.Properties properties) {
        super(properties);
    }

    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add((Component)Component.literal((String)"Contains a Quantum Falcon").withStyle(ChatFormatting.AQUA));
        tooltipComponents.add((Component)Component.literal((String)""));
        tooltipComponents.add((Component)Component.literal((String)"Place in Quantum Incubator to hatch").withStyle(ChatFormatting.GRAY));
    }

    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
