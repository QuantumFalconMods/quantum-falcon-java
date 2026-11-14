/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.network.chat.Component
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.Item$TooltipContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Rarity
 *  net.minecraft.world.item.TooltipFlag
 *  net.minecraft.world.item.UseAnim
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.matt.quantumfalcon.item.gear.weapon;


import net.minecraft.world.InteractionResult;
import com.matt.quantumfalcon.entity.projectile.VoidBlastEntity;
import net.minecraft.server.level.ServerLevel;
import com.matt.quantumfalcon.registry.ModParticles;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Holder;

public class VoidCannonItem
extends Item {
    private static final int MAX_CHARGE_TIME = 60;

    public VoidCannonItem() {
        super(new Item.Properties().durability(500).rarity(Rarity.EPIC).fireResistant());
    }

    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(stack);
    }

    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (entity instanceof Player) {
            Player player = (Player)entity;
            int chargeTime = this.getUseDuration(stack, entity) - remainingUseDuration;
            if (chargeTime % 5 == 0) {
                Vec3 look = player.getLookAngle();
                Vec3 particlePos = player.position().add(0.0, (double)player.getEyeHeight(), 0.0).add(look);
                level.addParticle((ParticleOptions)ModParticles.VOID_FLAME.get(), particlePos.x, particlePos.y, particlePos.z, 0.0, 0.0, 0.0);
                player.playSound(SoundEvents.BEACON_POWER_SELECT, 0.3f, 0.5f + (float)chargeTime / 60.0f);
            }
            float chargePercent = Math.min((float)chargeTime / 60.0f, 1.0f) * 100.0f;
            player.displayClientMessage((Component)Component.literal((String)("Charging: " + (int)chargePercent + "%")).withStyle(chargePercent >= 100.0f ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        }
    }

    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player) {
            int chargeTime;
            float power;
            Player player = (Player)entity;
            if (!level.isClientSide() && (power = Math.min((float)(chargeTime = this.getUseDuration(stack, entity) - timeLeft) / 60.0f, 1.0f)) >= 0.3f) {
                VoidBlastEntity blast = new VoidBlastEntity(com.matt.quantumfalcon.registry.ModEntities.VOID_BLAST.get(), (LivingEntity)player, player.getLookAngle().scale(power), level);
                Vec3 look = player.getLookAngle();
                blast.setPos(player.getX() + look.x * 2.0, player.getEyeY(), player.getZ() + look.z * 2.0);
                blast.shoot(look.x, look.y, look.z, 2.0f, 0.0f);
                level.addFreshEntity((Entity)blast);
                player.playSound(SoundEvents.WITHER_SHOOT, 2.0f, 0.5f);
                player.setDeltaMovement(player.getDeltaMovement().add(-look.x * (double)power * 0.5, 0.0, -look.z * (double)power * 0.5));
                stack.hurtAndBreak((int)(power * 5.0f), (LivingEntity)player, LivingEntity.getSlotForHand((InteractionHand)player.getUsedItemHand()));
            }
        }
    }

    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.BOW;
    }

    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add((Component)Component.literal((String)"Charge to fire devastating void blasts").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add((Component)Component.literal((String)"Higher charge = more damage & AOE").withStyle(ChatFormatting.GRAY));
        tooltip.add((Component)Component.empty());
        tooltip.add((Component)Component.literal((String)"Min charge: 30%").withStyle(ChatFormatting.YELLOW));
        tooltip.add((Component)Component.literal((String)"Max charge: 3 seconds").withStyle(ChatFormatting.YELLOW));
    }
}
