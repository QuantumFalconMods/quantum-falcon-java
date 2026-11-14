package com.matt.quantumfalcon.item.gear.tool;

import com.matt.quantumfalcon.material.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Void Forged Axe (Tier 2: Purple)
 * Mid-game void-infused axe with enhanced tree felling.
 * 
 * Stats:
 * - Durability: 800
 * - Attack Speed: +0.0
 * - Mining Level: Netherite
 * - Damage: 3.0 bonus
 * - Enchantability: 15
 * 
 * Abilities:
 * - Enhanced Tree Feller (breaks connected logs + leaves, max 128 blocks)
 * - Auto Leaf Decay (leaves decay instantly)
 * - 3x3 Stripping (sneak)
 */
public class VoidForgedAxeItem extends AxeItem {
    
    public VoidForgedAxeItem() {
        super(ModToolTiers.PURPLE, new Item.Properties()
            .attributes(AxeItem.createAttributes(ModToolTiers.PURPLE, 5, -3.0f)));
    }
    
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
                            net.minecraft.world.entity.LivingEntity miner) {
        if (miner instanceof Player player && !level.isClientSide) {
            if (state.is(net.minecraft.tags.BlockTags.LOGS)) {
                // Enhanced tree felling (logs + leaves)
                performEnhancedTreeFelling(level, pos, state, player, stack, 0, 128);
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }
    
    private void performEnhancedTreeFelling(Level level, BlockPos pos, BlockState originalState,
                                          Player player, ItemStack tool, int depth, int maxDepth) {
        if (depth >= maxDepth || level.isClientSide) return;
        
        BlockState currentState = level.getBlockState(pos);
        
        // Break logs
        if (currentState.is(net.minecraft.tags.BlockTags.LOGS)) {
            AoeBreak.breakAs(level, pos, player);
            
            // Break adjacent logs and leaves
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        BlockPos targetPos = pos.offset(x, y, z);
                        BlockState targetState = level.getBlockState(targetPos);
                        
                        if (targetState.is(net.minecraft.tags.BlockTags.LOGS)) {
                            performEnhancedTreeFelling(level, targetPos, originalState, player, tool, depth + 1, maxDepth);
                        } else if (targetState.is(net.minecraft.tags.BlockTags.LEAVES)) {
                            // Auto-decay leaves
                            AoeBreak.breakAs(level, targetPos, player);
                        }
                    }
                }
            }
            
            // Void particles
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 2: Void Forged").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Enhanced Tree Feller").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("  Breaks logs + leaves automatically").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Max 128 blocks per tree").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("✦ Auto Leaf Decay").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("  Leaves decay instantly").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("✦ 3x3 Stripping").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("  Sneak to strip multiple logs").withStyle(ChatFormatting.GRAY));
    }
}
