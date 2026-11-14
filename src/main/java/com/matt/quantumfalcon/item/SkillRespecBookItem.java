package com.matt.quantumfalcon.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** the Skill Respec Book (data archive loot) refunds every spent skill point once, then is used up. */
public class SkillRespecBookItem extends Item {
    public SkillRespecBookItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            var data = com.matt.quantumfalcon.skill.PlayerSkillData.get(sp);
            if (data == null || data.getTotalSpentPoints() <= 0) {
                sp.displayClientMessage(Component.literal("No skill points spent yet \u2014 nothing to refund.").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            com.matt.quantumfalcon.skill.SkillPointSystem.resetSkillPoints(sp);
            level.playSound(null, sp.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.2f);
            if (!sp.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Right-click to refund every spent skill point").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Used up on reading").withStyle(ChatFormatting.DARK_GRAY));
    }
}
