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
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Void Forged Pickaxe (Tier 2: Purple)
 * Mid-game void-infused pickaxe with vein miner.
 * 
 * Stats:
 * - Durability: 800
 * - Attack Speed: +0.0
 * - Mining Level: Netherite
 * - Damage: 3.0 bonus
 * - Enchantability: 15
 * 
 * Abilities:
 * - Vein Miner (breaks connected ores of same type)
 * - 3x3 Mining (hold sneak)
 */
public class VoidForgedPickaxeItem extends PickaxeItem {
    
    public VoidForgedPickaxeItem() {
        super(ModToolTiers.PURPLE, new Item.Properties()
            .attributes(PickaxeItem.createAttributes(ModToolTiers.PURPLE, 1, -2.8f)));
    }
    
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
                            net.minecraft.world.entity.LivingEntity miner) {
        if (miner instanceof Player player && !level.isClientSide) {
            if (player.isCrouching()) {
                // 3x3 mining
                perform3x3Mining(level, pos, player, stack);
            } else {
                // Vein miner (breaks connected ores)
                performVeinMining(level, pos, state, player, stack, 0, 32); // Max 32 blocks
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }
    
    private void performVeinMining(Level level, BlockPos pos, BlockState originalState, 
                                   Player player, ItemStack tool, int depth, int maxDepth) {
        if (depth >= maxDepth || level.isClientSide) return;
        
        BlockState currentState = level.getBlockState(pos);
        
        // Check if same block type and mineable
        if (currentState.is(originalState.getBlock()) && 
            tool.isCorrectToolForDrops(currentState)) {
            
            AoeBreak.breakAs(level, pos, player);
            
            // Recursively mine adjacent blocks
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        performVeinMining(level, pos.offset(x, y, z), originalState, player, tool, depth + 1, maxDepth);
                    }
                }
            }
            
            // Particles
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }
    
    private void perform3x3Mining(Level level, BlockPos center, Player player, ItemStack tool) {
        // Same as SingularityPickaxeItem
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockPos targetPos = center.offset(x, y, z);
                    BlockState targetState = level.getBlockState(targetPos);
                    if (targetState.getDestroySpeed(level, targetPos) > 0 && 
                        tool.isCorrectToolForDrops(targetState)) {
                        AoeBreak.breakAs(level, targetPos, player);
                    }
                }
            }
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 2: Void Forged").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Vein Miner").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("  Breaks connected ores automatically").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Max 32 blocks per vein").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("✦ 3x3 Mining").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("  Sneak for 3x3 area").withStyle(ChatFormatting.GRAY));
    }
}
