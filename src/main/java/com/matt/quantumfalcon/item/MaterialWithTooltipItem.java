package com.matt.quantumfalcon.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Simple item that appends two tooltip lines from lang (item.quantumfalcon.{id}.tooltip_1 and tooltip_2).
 */
public class MaterialWithTooltipItem extends Item {

    private final String tooltipKey1;
    private final String tooltipKey2;

    public MaterialWithTooltipItem(Properties properties, String itemId) {
        super(properties);
        String base = "item.quantumfalcon." + itemId;
        this.tooltipKey1 = base + ".tooltip_1";
        this.tooltipKey2 = base + ".tooltip_2";
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable(tooltipKey1).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(tooltipKey2).withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
