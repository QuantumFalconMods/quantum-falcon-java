package com.matt.quantumfalcon.item.gear.tool;

import com.matt.quantumfalcon.material.ModToolTiers;
import net.minecraft.ChatFormatting;
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
 * Quantum Crystal Axe (Tier 3: Magenta)
 * Late-game crystalline axe with instant tree felling.
 * 
 * Stats:
 * - Durability: 400
 * - Attack Speed: +1.0 (fastest)
 * - Mining Level: Iron
 * - Damage: 2.0 bonus
 * - Enchantability: 25 (highest)
 * 
 * Abilities:
 * - Instant Tree Feller (breaks entire tree instantly, max 256 blocks)
 * - Auto Leaf Decay
 * - 3x3 Stripping (sneak)
 * - Fastest chopping speed
 */
public class QuantumCrystalAxeItem extends AxeItem {
    
    public QuantumCrystalAxeItem() {
        super(ModToolTiers.MAGENTA, new Item.Properties()
            .attributes(AxeItem.createAttributes(ModToolTiers.MAGENTA, 5, -3.0f)));
    }
    
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // Fastest chopping speed
        return super.getDestroySpeed(stack, state) * 1.3f; // 30% speed boost
    }
    
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, net.minecraft.core.BlockPos pos,
                            net.minecraft.world.entity.LivingEntity miner) {
        if (miner instanceof Player player && !level.isClientSide) {
            if (state.is(net.minecraft.tags.BlockTags.LOGS)) {
                // Instant tree felling (entire tree at once)
                performInstantTreeFelling(level, pos, state, player, stack);
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }
    
    private void performInstantTreeFelling(Level level, net.minecraft.core.BlockPos startPos, BlockState originalState,
                                          Player player, ItemStack tool) {
        // Break entire tree instantly (BFS algorithm)
        java.util.Queue<net.minecraft.core.BlockPos> queue = new java.util.LinkedList<>();
        java.util.Set<net.minecraft.core.BlockPos> visited = new java.util.HashSet<>();
        queue.add(startPos);
        visited.add(startPos);
        int broken = 0;
        int maxBlocks = 256;
        
        while (!queue.isEmpty() && broken < maxBlocks) {
            net.minecraft.core.BlockPos current = queue.poll();
            BlockState currentState = level.getBlockState(current);
            
            if (currentState.is(net.minecraft.tags.BlockTags.LOGS) || 
                currentState.is(net.minecraft.tags.BlockTags.LEAVES)) {
                AoeBreak.breakAs(level, current, player);
                broken++;
                
                // Add adjacent blocks
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            net.minecraft.core.BlockPos next = current.offset(x, y, z);
                            if (!visited.contains(next)) {
                                visited.add(next);
                                queue.add(next);
                            }
                        }
                    }
                }
                
                // Quantum particles
                if (level instanceof ServerLevel serverLevel && broken % 10 == 0) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                        current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5,
                        5, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 3: Quantum Crystal").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Instant Tree Feller").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Breaks entire tree instantly").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Max 256 blocks per tree").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("✦ Fastest Speed").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("  Highest chopping speed in game").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Highest enchantability (25)").withStyle(ChatFormatting.GOLD));
    }
}
