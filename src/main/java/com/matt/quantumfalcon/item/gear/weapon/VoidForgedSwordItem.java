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
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Void Forged Sword (Tier 2: Purple)
 * Mid-game void-infused weapon with cleave attack.
 * 
 * Stats:
 * - Durability: 800
 * - Attack Speed: +0.0
 * - Mining Level: Netherite
 * - Damage: 3.0 bonus
 * - Enchantability: 15
 * 
 * Ability: Cleave - Attacks hit all enemies in 3-block radius (30% chance)
 */
public class VoidForgedSwordItem extends SwordItem {
    
    public VoidForgedSwordItem() {
        super(ModToolTiers.PURPLE, new Item.Properties()
            .attributes(SwordItem.createAttributes(ModToolTiers.PURPLE, 3, -2.4f)));
    }
    
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide) {
            // 30% chance for cleave attack
            if (player.getRandom().nextFloat() < 0.30f) {
                performCleaveAttack(player, target, stack);
            }
            
            // Void particles
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL,
                    target.getX(), target.getY() + 1, target.getZ(),
                    8, 0.5, 0.5, 0.5, 0.1);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }
    
    private void performCleaveAttack(Player player, LivingEntity primaryTarget, ItemStack stack) {
        AABB cleaveBox = primaryTarget.getBoundingBox().inflate(3.0);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, cleaveBox,
            e -> e != player && e != primaryTarget && e.isAlive());
        
        float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        for (LivingEntity entity : nearby) {
            entity.hurt(player.damageSources().playerAttack(player), baseDamage * 0.5f);
        }
        
        if (!nearby.isEmpty()) {
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 2: Void Forged").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("✦ Cleave Attack").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("  30% chance to hit all nearby enemies").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  3 block radius, 50% damage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
