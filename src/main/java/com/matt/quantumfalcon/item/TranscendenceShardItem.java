package com.matt.quantumfalcon.item;

import com.matt.quantumfalcon.story.StoryManager;
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
 * Transcendence Shard - The key item for choosing endings.
 * Dropped by Singularity Sovereign, used to select final ending.
 * 
 * Right-click cycles through ending options.
 * Shift+Right-click confirms the current selection.
 */
public class TranscendenceShardItem extends Item {
    
    // Track selected ending per player (not persisted - intentional)
    private static final java.util.Map<java.util.UUID, Integer> SELECTED_ENDING = new java.util.HashMap<>();
    
    public TranscendenceShardItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        
        // Check if player can make ending choice
        if (!StoryManager.canMakeEndingChoice(player)) {
            player.displayClientMessage(
                Component.literal("⚠ The shard lies dormant. Defeat the Singularity Sovereign first.")
                    .withStyle(ChatFormatting.RED), true);
            player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.5f);
            return InteractionResultHolder.fail(stack);
        }
        
        // Get current selection
        int currentSelection = SELECTED_ENDING.getOrDefault(player.getUUID(), 0);
        
        if (player.isShiftKeyDown()) {
            // Confirm selection
            StoryManager.Ending ending = StoryManager.Ending.values()[currentSelection];
            
            player.displayClientMessage(
                Component.literal("Confirming: " + ending.title + "...").withStyle(ChatFormatting.GOLD), true);
            player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
            
            // Execute ending
            StoryManager.executeEnding(serverPlayer, ending);
            
            // Consume the shard
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            
            SELECTED_ENDING.remove(player.getUUID());
            return InteractionResultHolder.consume(stack);
            
        } else {
            // Cycle through options
            currentSelection = (currentSelection + 1) % 3;
            SELECTED_ENDING.put(player.getUUID(), currentSelection);
            
            StoryManager.Ending ending = StoryManager.Ending.values()[currentSelection];
            
            // Display option
            Component message = switch (ending) {
                case LIGHT -> Component.literal("☀ LIGHT - Let the Morning Come")
                    .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
                case DARK -> Component.literal("⚫ DARK - Keep the Hour")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
                case TRUE -> Component.literal("✧ TRUE - Braid the Hours")
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
            };
            
            player.displayClientMessage(message, true);
            player.displayClientMessage(
                Component.literal("(Shift+Right-Click to confirm)").withStyle(ChatFormatting.GRAY), false);
            
            // Sound
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 
                1.0f + currentSelection * 0.2f);
            
            return InteractionResultHolder.success(stack);
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Hold it to choose what the morning means.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("Right-Click: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal("Cycle Endings").withStyle(ChatFormatting.YELLOW)));
        tooltipComponents.add(Component.literal("Shift+Right-Click: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal("Confirm Choice").withStyle(ChatFormatting.GOLD)));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("★ Only usable after defeating").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.literal("  the Singularity Sovereign").withStyle(ChatFormatting.DARK_GRAY));
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Always enchanted glow
    }
}
