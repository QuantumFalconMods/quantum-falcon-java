package com.matt.quantumfalcon.item.gear.weapon;

import com.matt.quantumfalcon.material.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Quantum Crystal Sword (Tier 3: Magenta)
 * Late-game crystalline weapon with phase attacks.
 * 
 * Stats:
 * - Durability: 400
 * - Attack Speed: +1.0 (fastest)
 * - Mining Level: Iron
 * - Damage: 2.0 bonus
 * - Enchantability: 25 (highest)
 * 
 * Ability: Phase Strike - 20% chance to phase through armor (ignore 50% armor)
 */
public class QuantumCrystalSwordItem extends SwordItem {
    
    public QuantumCrystalSwordItem() {
        super(ModToolTiers.MAGENTA, new Item.Properties()
            .attributes(SwordItem.createAttributes(ModToolTiers.MAGENTA, 3, -2.4f)));
    }
    
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide) {
            // 20% chance for phase strike (armor penetration)
            boolean phaseStrike = player.getRandom().nextFloat() < 0.20f;
            
            if (phaseStrike) {
                // Apply glowing effect to show phase strike
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
                
                // Phase particles
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(com.matt.quantumfalcon.registry.ModParticles.PHASE_SHIFT.get(),
                        target.getX(), target.getY() + 1, target.getZ(),
                        15, 0.5, 0.5, 0.5, 0.1);
                }
                
                player.level().playSound(null, target.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f);
            } else {
                // Normal quantum particles
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                        target.getX(), target.getY() + 1, target.getZ(),
                        5, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 3: Quantum Crystal").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Phase Strike").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  20% chance to ignore 50% armor").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Fastest attack speed").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("  Highest enchantability (25)").withStyle(ChatFormatting.GOLD));
    }
}
