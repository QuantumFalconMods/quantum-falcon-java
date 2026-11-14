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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Singularity Pickaxe (Tier 1: Aqua)
 * Entry-level quantum pickaxe with 3x3 mining.
 * 
 * Stats:
 * - Durability: 600
 * - Attack Speed: +0.5
 * - Mining Level: Diamond
 * - Damage: 2.5 bonus
 * - Enchantability: 20
 * 
 * Abilities:
 * - 3x3 Mining (hold sneak)
 */
public class SingularityPickaxeItem extends PickaxeItem {
    
    public SingularityPickaxeItem() {
        super(ModToolTiers.AQUA, new Item.Properties()
            .attributes(PickaxeItem.createAttributes(ModToolTiers.AQUA, 1, -2.8f)));
    }
    
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, 
                            net.minecraft.world.entity.LivingEntity miner) {
        if (miner instanceof Player player && !level.isClientSide && player.isCrouching()) {
            // 3x3 mining when sneaking
            perform3x3Mining(level, pos, player, stack);
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }
    
    private void perform3x3Mining(Level level, BlockPos center, Player player, ItemStack tool) {
        if (level.isClientSide) return;
        
        // Mine 3x3 area
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Skip center (already mined)
                    
                    BlockPos targetPos = center.offset(x, y, z);
                    BlockState targetState = level.getBlockState(targetPos);
                    
                    if (targetState.getDestroySpeed(level, targetPos) > 0 && 
                        tool.isCorrectToolForDrops(targetState)) {
                        // Break block
                        AoeBreak.breakAs(level, targetPos, player);
                        
                        // Particles
                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.CRIT,
                                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                                3, 0.2, 0.2, 0.2, 0.1);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 1: Singularity").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ 3x3 Mining").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Sneak while mining for 3x3 area").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Highlights ores within 16 blocks").withStyle(ChatFormatting.GRAY));
    }
}
