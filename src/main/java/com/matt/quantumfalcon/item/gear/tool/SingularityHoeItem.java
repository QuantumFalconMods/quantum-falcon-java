package com.matt.quantumfalcon.item.gear.tool;

import com.matt.quantumfalcon.material.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Singularity Hoe (Tier 1: Aqua)
 * Entry-level quantum hoe with 3x3 tilling.
 * 
 * Stats:
 * - Durability: 600
 * - Attack Speed: +0.5
 * - Mining Level: Diamond
 * - Damage: 2.5 bonus
 * - Enchantability: 20
 * 
 * Abilities:
 * - 3x3 Tilling (sneak + right-click to till 3x3 area)
 * - Auto Replant (harvests and replants crops)
 */
public class SingularityHoeItem extends HoeItem {
    
    public SingularityHoeItem() {
        super(ModToolTiers.AQUA, new Item.Properties()
            .attributes(HoeItem.createAttributes(ModToolTiers.AQUA, -2, 0f)));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 1: Singularity").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ 3x3 Tilling").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Sneak + Right-click to till 3x3 area").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("✦ Auto Replant").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Harvests and replants crops").withStyle(ChatFormatting.GRAY));
    }
}
