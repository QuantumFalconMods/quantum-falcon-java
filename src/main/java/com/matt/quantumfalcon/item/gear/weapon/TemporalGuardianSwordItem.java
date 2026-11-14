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
 * Temporal Guardian Sword (Tier 4: Gold)
 * End-game divine weapon with temporal abilities.
 * 
 * Stats:
 * - Durability: 500
 * - Attack Speed: +0.25
 * - Mining Level: Diamond
 * - Damage: 2.5 bonus
 * - Enchantability: 22
 * 
 * Ability: Temporal Strike - 15% chance to slow target and grant haste to wielder
 */
public class TemporalGuardianSwordItem extends SwordItem {
    
    public TemporalGuardianSwordItem() {
        super(ModToolTiers.GOLD, new Item.Properties()
            .attributes(SwordItem.createAttributes(ModToolTiers.GOLD, 3, -2.4f)));
    }
    
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide) {
            // 15% chance for temporal strike
            if (player.getRandom().nextFloat() < 0.15f) {
                // Slow target
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
                
                // Haste to wielder
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0, false, false));
                
                // Temporal particles
                if (player.level() instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 20; i++) {
                        double angle = (i / 20.0) * Math.PI * 2;
                        serverLevel.sendParticles(ParticleTypes.END_ROD,
                            target.getX() + Math.cos(angle) * 1.5,
                            target.getY() + 1,
                            target.getZ() + Math.sin(angle) * 1.5,
                            1, 0, 0.1, 0, 0.1);
                    }
                }
                
                player.level().playSound(null, target.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.8f);
            } else {
                // Normal divine particles
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
        tooltip.add(Component.literal("Tier 4: Temporal Guardian").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("The ultimate quantum weapon").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Temporal Strike").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  15% chance to slow target").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Grants Haste to wielder").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Balanced divine protection").withStyle(ChatFormatting.DARK_GRAY));
    }
}
