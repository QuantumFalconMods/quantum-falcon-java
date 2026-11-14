package com.matt.quantumfalcon.item;

import com.matt.quantumfalcon.capability.PlayerInstabilityCapability;
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

/**
 * a Stability Shard is a small, stackable step toward calm: crush one to drain {@value #DRAIN}% instability
 * (30 s cooldown). The Quantum Stabilizer remains the full reset.
 */
public class StabilityShardItem extends Item {
    public static final float DRAIN = 15f;
    public static final int COOLDOWN = 600;

    public StabilityShardItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        return crush((ServerPlayer) player, stack) ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    /** Server side: drains instability and uses one shard. False (nothing used) when already calm. */
    public static boolean crush(ServerPlayer player, ItemStack stack) {
        var cap = PlayerInstabilityCapability.get(player);
        if (cap.getInstability() <= 0.01f) {
            player.displayClientMessage(Component.literal("You are already stable.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        cap.setInstability(cap.getInstability() - DRAIN);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0f, 1.3f);
        player.displayClientMessage(Component.literal("The shard steadies you (-" + (int) DRAIN + "% instability).").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Use: -" + (int) DRAIN + "% instability (30 s cooldown)").withStyle(ChatFormatting.GRAY));
    }
}
