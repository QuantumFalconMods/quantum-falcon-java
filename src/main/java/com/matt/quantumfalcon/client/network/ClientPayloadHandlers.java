package com.matt.quantumfalcon.client.network;

import com.matt.quantumfalcon.client.screen.StoryChoiceScreen;
import net.minecraft.client.Minecraft;

/**
 * Client-only payload handling. Kept in a separate class so that dedicated
 * servers never load client classes (Screen, Minecraft) when packet classes
 * are verified. (Added during reconstruction: fixes dedicated-server crash
 * "Attempted to load class net/minecraft/client/gui/screens/Screen for invalid dist DEDICATED_SERVER".)
 */
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {}

    public static void openStoryChoiceScreen(boolean trueUnlocked, int relics, int bonds) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new StoryChoiceScreen(trueUnlocked, relics, bonds));
        }
    }
}
