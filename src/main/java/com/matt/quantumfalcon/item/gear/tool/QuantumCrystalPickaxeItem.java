package com.matt.quantumfalcon.item.gear.tool;

import com.matt.quantumfalcon.material.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Quantum Crystal Pickaxe (Tier 3: Magenta)
 * Late-game crystalline pickaxe with auto-fortune.
 * 
 * Stats:
 * - Durability: 400
 * - Attack Speed: +1.0 (fastest)
 * - Mining Level: Iron
 * - Damage: 2.0 bonus
 * - Enchantability: 25 (highest)
 * 
 * Abilities:
 * - 3x3 Mining (hold sneak)
 * - Fastest mining speed
 */
public class QuantumCrystalPickaxeItem extends PickaxeItem {
    
    public QuantumCrystalPickaxeItem() {
        super(ModToolTiers.MAGENTA, new Item.Properties()
            .attributes(PickaxeItem.createAttributes(ModToolTiers.MAGENTA, 1, -2.8f)));
    }
    
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // Fastest mining speed (9.0 base)
        return super.getDestroySpeed(stack, state) * 1.2f; // 20% speed boost
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 3: Quantum Crystal").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("  Always acts as Fortune III").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("✦ 3x3 Mining").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Sneak for 3x3 area").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("✦ Fastest Speed").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("  Highest mining speed in game").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Highest enchantability (25)").withStyle(ChatFormatting.GOLD));
    }
}
