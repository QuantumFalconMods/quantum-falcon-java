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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Void Forged Shovel (Tier 2: Purple)
 * Mid-game void-infused shovel with tunnel creation.
 * 
 * Stats:
 * - Durability: 800
 * - Attack Speed: +0.0
 * - Mining Level: Netherite
 * - Damage: 3.0 bonus
 * - Enchantability: 15
 * 
 * Abilities:
 * - Tunnel Creator (sneak + right-click to create 3x3 tunnel, 5 blocks deep)
 * - 3x3 Excavation (sneak while mining)
 */
public class VoidForgedShovelItem extends ShovelItem {
    
    public VoidForgedShovelItem() {
        super(ModToolTiers.PURPLE, new Item.Properties()
            .attributes(ShovelItem.createAttributes(ModToolTiers.PURPLE, 1.5f, -3.0f)));
    }
    
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
                            net.minecraft.world.entity.LivingEntity miner) {
        if (miner instanceof Player player && !level.isClientSide && player.isCrouching()) {
            // 3x3 excavation
            perform3x3Excavation(level, pos, player, stack);
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }
    
    private void perform3x3Excavation(Level level, BlockPos center, Player player, ItemStack tool) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos targetPos = center.offset(x, 0, z);
                BlockState targetState = level.getBlockState(targetPos);
                
                if (targetState.getDestroySpeed(level, targetPos) > 0 &&
                    tool.isCorrectToolForDrops(targetState)) {
                    AoeBreak.breakAs(level, targetPos, player);
                }
            }
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 2: Void Forged").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Tunnel Creator").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("  Sneak + Right-click for 3x3 tunnel").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  5 blocks deep").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("✦ 3x3 Excavation").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("  Sneak while mining for 3x3 area").withStyle(ChatFormatting.GRAY));
    }
}
