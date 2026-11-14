/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.chat.Component
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.Item$TooltipContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TooltipFlag
 *  net.minecraft.world.level.Level
 */
package com.matt.quantumfalcon.item.special;


import net.minecraft.world.InteractionResult;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.core.Holder;

public class CosmeticCrateItem
extends Item {
    public CosmeticCrateItem(Item.Properties properties) {
        super(properties);
    }

    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            ChatFormatting color;
            String rarity;
            player.displayClientMessage((Component)Component.literal((String)"Opening Cosmetic Crate...").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            float roll = level.random.nextFloat();
            if (roll < 0.01f) {
                rarity = "LEGENDARY";
                color = ChatFormatting.GOLD;
            } else if (roll < 0.1f) {
                rarity = "EPIC";
                color = ChatFormatting.DARK_PURPLE;
            } else if (roll < 0.3f) {
                rarity = "RARE";
                color = ChatFormatting.BLUE;
            } else {
                rarity = "COMMON";
                color = ChatFormatting.WHITE;
            }
            player.displayClientMessage((Component)Component.literal((String)("You received a " + rarity + " cosmetic!")).withStyle(new ChatFormatting[]{color, ChatFormatting.BOLD}), false);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add((Component)Component.literal((String)"Contains a random cosmetic!").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltipComponents.add((Component)Component.literal((String)""));
        tooltipComponents.add((Component)Component.literal((String)"Possible contents:").withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add((Component)Component.literal((String)"\u2022 Particle Trails").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add((Component)Component.literal((String)"\u2022 Wing Patterns").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add((Component)Component.literal((String)"\u2022 Name Tag Styles").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add((Component)Component.literal((String)""));
        tooltipComponents.add((Component)Component.literal((String)"Drop Rates:").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add((Component)Component.literal((String)"Common: 70% | Rare: 20%").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add((Component)Component.literal((String)"Epic: 9% | Legendary: 1%").withStyle(ChatFormatting.DARK_GRAY));
    }

    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
