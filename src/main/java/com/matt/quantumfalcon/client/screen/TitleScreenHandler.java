package com.matt.quantumfalcon.client.screen;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Replaces the vanilla title screen with our custom Quantum Falcon title screen.
 * Same pattern used by ATM10, RLCraft, and other branded modpacks.
 * Only activates on client side.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public class TitleScreenHandler {
    
    private static boolean replaced = false;
    
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        // Replace vanilla TitleScreen with our custom one
        // Only replace once per session (so returning to title from game works too)
        if (event.getScreen() instanceof TitleScreen && !(event.getScreen() instanceof QuantumFalconTitleScreen)) {
            event.setNewScreen(new QuantumFalconTitleScreen());
        }
    }
}
