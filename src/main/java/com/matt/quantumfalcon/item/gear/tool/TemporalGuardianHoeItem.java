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
 * Temporal Guardian Hoe (Tier 4: Gold)
 * End-game divine hoe with temporal farming.
 * 
 * Stats:
 * - Durability: 500
 * - Attack Speed: +0.25
 * - Mining Level: Diamond
 * - Damage: 2.5 bonus
 * - Enchantability: 22
 * 
 * Abilities:
 * - Temporal Tilling (sneak + right-click for 9x9 area, 10s cooldown)
 * - Instant Growth + Auto Harvest (harvests mature crops automatically)
 * - Auto Replant + Bone Meal
 */
public class TemporalGuardianHoeItem extends HoeItem {
    
    public TemporalGuardianHoeItem() {
        super(ModToolTiers.GOLD, new Item.Properties()
            .attributes(HoeItem.createAttributes(ModToolTiers.GOLD, -2, 0f)));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 4: Temporal Guardian").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("The ultimate quantum tool").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Temporal Tilling").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Sneak + Right-click for 9x9 area").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  10 second cooldown").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("✦ Instant Growth + Auto Harvest").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Crops grow instantly and harvest automatically").withStyle(ChatFormatting.GRAY));
    }
}
