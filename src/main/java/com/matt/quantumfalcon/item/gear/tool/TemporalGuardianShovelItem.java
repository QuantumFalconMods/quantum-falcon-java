package com.matt.quantumfalcon.item.gear.tool;

import com.matt.quantumfalcon.material.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Temporal Guardian Shovel (Tier 4: Gold)
 * End-game divine shovel with temporal excavation.
 * 
 * Stats:
 * - Durability: 500
 * - Attack Speed: +0.25
 * - Mining Level: Diamond
 * - Damage: 2.5 bonus
 * - Enchantability: 22
 * 
 * Abilities:
 * - Temporal Excavation (sneak + right-click for 7x7 area, 5 blocks deep, 10s cooldown)
 * - 3x3 Excavation (sneak while mining)
 */
public class TemporalGuardianShovelItem extends ShovelItem {
    
    public TemporalGuardianShovelItem() {
        super(ModToolTiers.GOLD, new Item.Properties()
            .attributes(ShovelItem.createAttributes(ModToolTiers.GOLD, 1.5f, -3.0f)));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 4: Temporal Guardian").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("The ultimate quantum tool").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Temporal Excavation").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Sneak + Right-click for 7x7 area").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  5 blocks deep, 10 second cooldown").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("✦ 3x3 Excavation").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Sneak while mining for 3x3 area").withStyle(ChatFormatting.GRAY));
    }
}
