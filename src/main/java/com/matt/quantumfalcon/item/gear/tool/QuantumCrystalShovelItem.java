package com.matt.quantumfalcon.item.gear.tool;

import com.matt.quantumfalcon.material.ModToolTiers;
import net.minecraft.ChatFormatting;
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
 * Quantum Crystal Shovel (Tier 3: Magenta)
 * Late-game crystalline shovel with instant excavation.
 * 
 * Stats:
 * - Durability: 400
 * - Attack Speed: +1.0 (fastest)
 * - Mining Level: Iron
 * - Damage: 2.0 bonus
 * - Enchantability: 25 (highest)
 * 
 * Abilities:
 * - Instant Excavation (sneak + right-click for 5x5 area, 3 blocks deep)
 * - 3x3 Excavation (sneak while mining)
 * - Fastest digging speed
 */
public class QuantumCrystalShovelItem extends ShovelItem {
    
    public QuantumCrystalShovelItem() {
        super(ModToolTiers.MAGENTA, new Item.Properties()
            .attributes(ShovelItem.createAttributes(ModToolTiers.MAGENTA, 1.5f, -3.0f)));
    }
    
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // Fastest digging speed
        return super.getDestroySpeed(stack, state) * 1.25f; // 25% speed boost
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 3: Quantum Crystal").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Instant Excavation").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Sneak + Right-click for 5x5 area").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  3 blocks deep").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("✦ Fastest Speed").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("  Highest digging speed in game").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Highest enchantability (25)").withStyle(ChatFormatting.GOLD));
    }
}
