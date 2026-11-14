package com.matt.quantumfalcon.item;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Quantum Stabilizer - Boss drop item that fully resets instability.
 * Dropped by: Rift Warden (guaranteed on first kill)
 * 
 * Usage: Right-click to instantly reset instability to 0.
 * Provides emergency escape from high-instability situations.
 */
public class QuantumStabilizerItem extends Item {
    
    public QuantumStabilizerItem(Properties props) {
        super(props);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            // real effect: meter to 0 and held there for 5 minutes
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) com.matt.quantumfalcon.dimension.InstabilityEffects.useStabilizerItem(sp);
            player.getCooldowns().addCooldown(this, 100);
            player.displayClientMessage(
                Component.literal("Instability cleared and held for 5 minutes").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                true
            );
            
            // Sound effect
            level.playSound(null, player.blockPosition(), 
                ModSounds.STABILIZER_ACTIVATE.get(), 
                SoundSource.PLAYERS, 1.0f, 1.5f);
            
            // Particle burst
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 30; i++) {
                    double angle = (i / 30.0) * Math.PI * 2;
                    double radius = 1.5;
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                        player.getX() + Math.cos(angle) * radius,
                        player.getY() + 1.5,
                        player.getZ() + Math.sin(angle) * radius,
                        1, 0.1, 0.1, 0.1, 0.05);
                }
                
                // Cyan glow particles
                for (int i = 0; i < 20; i++) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX() + (Math.random() - 0.5) * 2,
                        player.getY() + Math.random() * 2,
                        player.getZ() + (Math.random() - 0.5) * 2,
                        1, 0, 0.1, 0, 0.1);
                }
            }
            
            // Consume item (single use)
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("✦ Emergency Stabilizer").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Right-click: instability to 0, no build-up for 5 minutes").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Single use").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("Crafted from echo shards, rift shards and a quantum crystal").withStyle(ChatFormatting.DARK_GRAY));
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Enchanted glint effect
    }
}
