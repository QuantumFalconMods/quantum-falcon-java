package com.matt.quantumfalcon.item.gear.tool;

import com.matt.quantumfalcon.material.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Temporal Guardian Pickaxe (Tier 4: Gold)
 * End-game divine pickaxe with time manipulation.
 * 
 * Stats:
 * - Durability: 500
 * - Attack Speed: +0.25
 * - Mining Level: Diamond
 * - Damage: 2.5 bonus
 * - Enchantability: 22
 * 
 * Abilities:
 * - Temporal Mining (breaks a 5x5x5 area)
 * - 3x3 Mining (hold sneak)
 */
public class TemporalGuardianPickaxeItem extends PickaxeItem {
    
    public TemporalGuardianPickaxeItem() {
        super(ModToolTiers.GOLD, new Item.Properties()
            .attributes(PickaxeItem.createAttributes(ModToolTiers.GOLD, 1, -2.8f)));
    }
    
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, net.minecraft.core.BlockPos pos,
                            net.minecraft.world.entity.LivingEntity miner) {
        if (miner instanceof Player player && !level.isClientSide) {
            if (player.isCrouching()) {
                // Temporal burst - breaks a 5x5x5 area (cooldown)
                performTemporalBurst(level, pos, player, stack);
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }
    
    private void performTemporalBurst(Level level, net.minecraft.core.BlockPos center, Player player, ItemStack tool) {
        // Check cooldown (stored in NBT)
        CustomData customData = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        net.minecraft.nbt.CompoundTag tag = customData.copyTag();
        long lastUse = tag.getLong("LastTemporalBurst");
        long currentTime = level.getGameTime();
        
        if (currentTime - lastUse < 200) return; // 10 second cooldown
        
        // Break blocks in a 2-block radius (5x5x5; it was 11x11x11 at no durability cost)
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    
                    net.minecraft.core.BlockPos targetPos = center.offset(x, y, z);
                    BlockState targetState = level.getBlockState(targetPos);
                    
                    if (targetState.getDestroySpeed(level, targetPos) > 0 && 
                        tool.isCorrectToolForDrops(targetState)) {
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
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 4: Temporal Guardian").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("The ultimate quantum tool").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Temporal Burst").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Sneak-mine: breaks a 5x5x5 area (10 s cooldown)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  10 second cooldown").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("  Ores smelt automatically").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("✦ 3x3 Mining").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Sneak for 3x3 area").withStyle(ChatFormatting.GRAY));
    }
}
