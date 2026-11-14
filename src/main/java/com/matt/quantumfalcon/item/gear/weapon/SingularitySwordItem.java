package com.matt.quantumfalcon.item.gear.weapon;

import com.matt.quantumfalcon.material.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Singularity Sword (Tier 1: Aqua)
 * Entry-level quantum weapon with basic quantum dash ability.
 * 
 * Stats:
 * - Durability: 600
 * - Attack Speed: +0.5
 * - Mining Level: Diamond
 * - Damage: 2.5 bonus
 * - Enchantability: 20
 * 
 * Ability: Quantum Dash - Sneak + right-click to dash forward 8 blocks
 */
public class SingularitySwordItem extends SwordItem {
    
    public SingularitySwordItem() {
        super(ModToolTiers.AQUA, new Item.Properties()
            .attributes(SwordItem.createAttributes(ModToolTiers.AQUA, 3, -2.4f)));
    }
    
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide) {
            // Quantum dash particles on hit
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + 1, target.getZ(),
                    5, 0.3, 0.3, 0.3, 0.1);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 1: Singularity").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Quantum Dash").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Sneak + Right-click to dash 8 blocks").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Cooldown: 3 seconds").withStyle(ChatFormatting.DARK_GRAY));
    }
}
