package com.matt.quantumfalcon.item;

import com.matt.quantumfalcon.breeding.BreedingData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A breeding catalyst (BREEDING.md "Catalysts"): used on an egg block before it hatches. Effect text comes from BreedingData. */
public class CatalystItem extends Item {
    private final String id;
    public CatalystItem(String id, Properties p) { super(p); this.id = id; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.quantumfalcon.catalyst").withStyle(ChatFormatting.DARK_AQUA));
        for (var c : BreedingData.CATALYSTS) if (c.id().equals(id)) {
            tip.add(Component.literal(c.effect()).withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("Found: " + c.source()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
