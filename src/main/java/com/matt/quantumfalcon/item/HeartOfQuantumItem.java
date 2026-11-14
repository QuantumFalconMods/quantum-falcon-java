package com.matt.quantumfalcon.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Heart of Quantum: crafted from the four boss relics. Works from anywhere in the inventory (no accessory mod needed):
 * night vision and slow falling, refreshed every 4 s, with a faint particle trail.
 */
public class HeartOfQuantumItem extends Item {
    public HeartOfQuantumItem(Properties props) { super(props); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof Player player)) return;
        long t = level.getGameTime();
        if (!level.isClientSide && t % 80 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, true));
        }
        if (level.isClientSide && t % 6 == 0) {
            level.addParticle(ParticleTypes.END_ROD, player.getX() + (level.random.nextDouble() - 0.5) * 0.6,
                player.getY() + 1.0 + (level.random.nextDouble() - 0.5) * 0.4, player.getZ() + (level.random.nextDouble() - 0.5) * 0.6, 0, 0.04, 0);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Four pieces of a held hour, made whole.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("While carried: night vision, slow falling").withStyle(ChatFormatting.GRAY));
    }
}
