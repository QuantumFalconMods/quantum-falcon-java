package com.matt.quantumfalcon.item;

import com.matt.quantumfalcon.breeding.BreedingEngine;
import com.matt.quantumfalcon.breeding.FalconGenome;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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

/**
 * a Quantum Reroll Token re-rolls the stats and abilities of a falcon egg held in the other hand. The variant
 * and the tier never change, so a token can't lift an egg past the egg limits.
 */
public class RerollTokenItem extends Item {
    public RerollTokenItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack token = player.getItemInHand(hand);
        ItemStack egg = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (level.isClientSide) return InteractionResultHolder.success(token);
        return reroll((ServerPlayer) player, token, egg) ? InteractionResultHolder.consume(token) : InteractionResultHolder.fail(token);
    }

    /** Re-rolls {@code egg}'s genome at the same variant and tier and uses one token. False if egg isn't a rolled egg. */
    public static boolean reroll(ServerPlayer player, ItemStack token, ItemStack egg) {
        FalconGenome g = egg.getItem() instanceof FalconEggItem ? FalconEggItem.child(egg) : null;
        CompoundTag data = g == null ? null : FalconEggItem.eggData(egg);
        if (g == null || data == null) {
            player.displayClientMessage(Component.literal("Hold a falcon egg in your other hand to re-roll it.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        FalconGenome fresh = BreedingEngine.wild(g.variant, g.tier, player.getRandom());
        CompoundTag d = data.copy();
        d.put("Child", fresh.save());
        FalconEggItem.of(egg, d);
        if (!player.getAbilities().instabuild) token.shrink(1);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.4f);
        player.displayClientMessage(Component.literal("The egg's future shifts: stats and abilities re-rolled (same variant and tier).").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Use with a falcon egg in the other hand:").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("re-rolls its stats and abilities (variant and tier stay)").withStyle(ChatFormatting.GRAY));
    }
}
