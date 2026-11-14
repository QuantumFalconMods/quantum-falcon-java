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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Singularity Axe (Tier 1: Aqua)
 * Entry-level quantum axe with tree felling.
 * 
 * Stats:
 * - Durability: 600
 * - Attack Speed: +0.5
 * - Mining Level: Diamond
 * - Damage: 2.5 bonus
 * - Enchantability: 20
 * 
 * Abilities:
 * - Tree Feller (breaks connected logs, max 64 blocks)
 * - 3x3 Stripping (sneak to strip multiple logs)
 */
public class SingularityAxeItem extends AxeItem {
    
    public SingularityAxeItem() {
        super(ModToolTiers.AQUA, new Item.Properties()
            .attributes(AxeItem.createAttributes(ModToolTiers.AQUA, 5, -3.0f)));
    }
    
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
                            net.minecraft.world.entity.LivingEntity miner) {
        if (miner instanceof Player player && !level.isClientSide) {
            // Tree feller - breaks connected logs
            if (state.is(net.minecraft.tags.BlockTags.LOGS)) {
                performTreeFelling(level, pos, state, player, stack, 0, 64);
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }
    
    private void performTreeFelling(Level level, BlockPos pos, BlockState originalState,
                                    Player player, ItemStack tool, int depth, int maxDepth) {
        if (depth >= maxDepth || level.isClientSide) return;
        
        BlockState currentState = level.getBlockState(pos);
        
        // Check if same log type
        if (currentState.is(net.minecraft.tags.BlockTags.LOGS)) {
            AoeBreak.breakAs(level, pos, player);
            
            // Recursively break adjacent logs
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) { // Only up, not down
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        performTreeFelling(level, pos.offset(x, y, z), originalState, player, tool, depth + 1, maxDepth);
                    }
                }
            }
            
            // Particles
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    2, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 1: Singularity").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Tree Feller").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Breaks connected logs automatically").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Max 64 blocks per tree").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("✦ 3x3 Stripping").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Sneak to strip multiple logs").withStyle(ChatFormatting.GRAY));
    }
}
