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
 * Quantum Crystal Hoe (Tier 3: Magenta)
 * Late-game crystalline hoe with instant farming.
 * 
 * Stats:
 * - Durability: 400
 * - Attack Speed: +1.0 (fastest)
 * - Mining Level: Iron
 * - Damage: 2.0 bonus
 * - Enchantability: 25 (highest)
 * 
 * Abilities:
 * - 7x7 Tilling (sneak + right-click to till 7x7 area)
 * - Instant Growth (crops grow instantly when planted)
 * - Auto Replant + Bone Meal
 */
public class QuantumCrystalHoeItem extends HoeItem {
    
    public QuantumCrystalHoeItem() {
        super(ModToolTiers.MAGENTA, new Item.Properties()
            .attributes(HoeItem.createAttributes(ModToolTiers.MAGENTA, -2, 0f)));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 3: Quantum Crystal").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ 7x7 Tilling").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Sneak + Right-click to till 7x7 area").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("✦ Instant Growth").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Crops grow instantly when planted").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("✦ Fastest Speed").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("  Highest enchantability (25)").withStyle(ChatFormatting.GOLD));
    }
}
