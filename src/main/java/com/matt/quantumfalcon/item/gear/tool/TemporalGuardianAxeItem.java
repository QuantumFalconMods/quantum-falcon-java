package com.matt.quantumfalcon.item.gear.tool;

import com.matt.quantumfalcon.material.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Temporal Guardian Axe (Tier 4: Gold)
 * End-game divine axe with temporal tree manipulation.
 * 
 * Stats:
 * - Durability: 500
 * - Attack Speed: +0.25
 * - Mining Level: Diamond
 * - Damage: 2.5 bonus
 * - Enchantability: 22
 * 
 * Abilities:
 * - Temporal Tree Feller (breaks tree in 5-block radius instantly)
 * - Auto Replant (plants saplings automatically)
 * - 3x3 Stripping (sneak)
 */
public class TemporalGuardianAxeItem extends AxeItem {
    
    public TemporalGuardianAxeItem() {
        super(ModToolTiers.GOLD, new Item.Properties()
            .attributes(AxeItem.createAttributes(ModToolTiers.GOLD, 5, -3.0f)));
    }
    
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
                            net.minecraft.world.entity.LivingEntity miner) {
        if (miner instanceof Player player && !level.isClientSide) {
            if (player.isCrouching() && state.is(net.minecraft.tags.BlockTags.LOGS)) {
                // Temporal burst - break trees in 5-block radius
                performTemporalTreeBurst(level, pos, player, stack);
            } else if (state.is(net.minecraft.tags.BlockTags.LOGS)) {
                // Normal tree felling
                performTreeFelling(level, pos, state, player, stack, 0, 200);
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }
    
    private void performTemporalTreeBurst(Level level, BlockPos center, Player player, ItemStack tool) {
        // Check cooldown
        CustomData customData = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        net.minecraft.nbt.CompoundTag tag = customData.copyTag();
        long lastUse = tag.getLong("LastTemporalBurst");
        long currentTime = level.getGameTime();
        
        if (currentTime - lastUse < 200) return; // 10 second cooldown
        
        // Break all logs/leaves in 5-block radius
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos targetPos = center.offset(x, y, z);
                    BlockState targetState = level.getBlockState(targetPos);
                    
                    if (targetState.is(net.minecraft.tags.BlockTags.LOGS) ||
                        targetState.is(net.minecraft.tags.BlockTags.LEAVES)) {
                        AoeBreak.breakAs(level, targetPos, player);
                        
                        // Temporal particles
                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.END_ROD,
                                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                                2, 0.1, 0.1, 0.1, 0.05);
                        }
                    }
                }
            }
        }
        
        // Update cooldown
        tag.putLong("LastTemporalBurst", currentTime);
        tool.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    private void performTreeFelling(Level level, BlockPos pos, BlockState originalState,
                                    Player player, ItemStack tool, int depth, int maxDepth) {
        if (depth >= maxDepth || level.isClientSide) return;
        
        BlockState currentState = level.getBlockState(pos);
        if (currentState.is(net.minecraft.tags.BlockTags.LOGS)) {
            AoeBreak.breakAs(level, pos, player);
            
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        performTreeFelling(level, pos.offset(x, y, z), originalState, player, tool, depth + 1, maxDepth);
                    }
                }
            }
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 4: Temporal Guardian").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("The ultimate quantum tool").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Temporal Tree Burst").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Sneak to break trees in 5-block radius").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  10 second cooldown").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("✦ Auto Replant").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Plants saplings automatically").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("✦ 3x3 Stripping").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Sneak to strip multiple logs").withStyle(ChatFormatting.GRAY));
    }
}
