package com.matt.quantumfalcon.item.gear.tool;

import com.matt.quantumfalcon.material.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Void Forged Hoe (Tier 2: Purple)
 * Mid-game void-infused hoe with enhanced farming.
 * 
 * Stats:
 * - Durability: 800
 * - Attack Speed: +0.0
 * - Mining Level: Netherite
 * - Damage: 3.0 bonus
 * - Enchantability: 15
 * 
 * Abilities:
 * - 5x5 Tilling (sneak + right-click to till 5x5 area)
 * - Auto Replant + Bone Meal (applies bone meal after replanting)
 */
public class VoidForgedHoeItem extends HoeItem {
    
    public VoidForgedHoeItem() {
        super(ModToolTiers.PURPLE, new Item.Properties()
            .attributes(HoeItem.createAttributes(ModToolTiers.PURPLE, -2, 0f)));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 2: Void Forged").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ 5x5 Tilling").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("  Sneak + Right-click to till 5x5 area").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("✦ Auto Replant + Bone Meal").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("  Harvests, replants, and applies bone meal").withStyle(ChatFormatting.GRAY));
    }
}
