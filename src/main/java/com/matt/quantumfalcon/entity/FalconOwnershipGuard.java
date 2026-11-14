package com.matt.quantumfalcon.entity;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * a tamed falcon answers only to its owner. Anyone else can't feed it into love mode, leash it, equip it or ride it
 * (vanilla handles leads before {@code mobInteract}, so this runs on the interact event). Creative players and name tags
 * are allowed.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class FalconOwnershipGuard {
    private FalconOwnershipGuard() {}

    public static boolean refuses(QuantumFalconEntity f, Player p, ItemStack held) {
        return f.isTame() && f.getOwnerUUID() != null && !f.isOwnedBy(p) && !p.isCreative() && !held.is(Items.NAME_TAG);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getTarget() instanceof QuantumFalconEntity f)) return;
        if (!refuses(f, e.getEntity(), e.getItemStack())) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.PASS);
        if (!e.getLevel().isClientSide && e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND)
            e.getEntity().displayClientMessage(Component.literal("This falcon answers only to its falconer.").withStyle(ChatFormatting.GRAY), true);
    }
}
