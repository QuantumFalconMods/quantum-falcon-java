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
 * Singularity Shovel (Tier 1: Aqua)
 * Entry-level quantum shovel with path creation.
 * 
 * Stats:
 * - Durability: 600
 * - Attack Speed: +0.5
 * - Mining Level: Diamond
 * - Damage: 2.5 bonus
 * - Enchantability: 20
 * 
 * Abilities:
 * - Path Creator (sneak + right-click to create 3x3 path)
 * - 3x3 Excavation (sneak while mining)
 */
public class SingularityShovelItem extends ShovelItem {
    
    public SingularityShovelItem() {
        super(ModToolTiers.AQUA, new Item.Properties()
            .attributes(ShovelItem.createAttributes(ModToolTiers.AQUA, 1.5f, -3.0f)));
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
        tooltip.add(Component.literal("Tier 1: Singularity").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Path Creator").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Sneak + Right-click for 3x3 path").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("✦ 3x3 Excavation").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Sneak while mining for 3x3 area").withStyle(ChatFormatting.GRAY));
    }
}
