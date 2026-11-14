package com.matt.quantumfalcon.client;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.client.gui.FalconCommandScreen;
import com.matt.quantumfalcon.client.gui.QuestLogScreen;
import com.matt.quantumfalcon.client.gui.SkillTreeScreen;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Key Bindings for Quantum Falcon mod.
 * Handles registration and processing of custom keybinds.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public class ModKeyBindings {

    // ============ Key Mappings ============
    
    public static final KeyMapping KEY_SKILL_TREE = new KeyMapping(
        "key.quantumfalcon.skill_tree",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        "key.category.quantumfalcon"
    );

    public static final KeyMapping KEY_QUEST_LOG = new KeyMapping(
        "key.quantumfalcon.quest_log",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_J,
        "key.category.quantumfalcon"
    );

    public static final KeyMapping KEY_FALCON_COMMAND = new KeyMapping(
        "key.quantumfalcon.falcon_command",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        "key.category.quantumfalcon"
    );

    public static final KeyMapping KEY_FALCON_CALL = new KeyMapping(
        "key.quantumfalcon.falcon_call",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        "key.category.quantumfalcon"
    );

    // ============ Registration ============

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_SKILL_TREE);
        event.register(KEY_QUEST_LOG);
        event.register(KEY_FALCON_COMMAND);
        event.register(KEY_FALCON_CALL);
    }
}

/**
 * Client-side tick event handler for processing keybinds.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
class KeyInputHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // Skill Tree
        if (ModKeyBindings.KEY_SKILL_TREE.consumeClick()) {
            mc.setScreen(new SkillTreeScreen());
        }

        // Quest Log
        if (ModKeyBindings.KEY_QUEST_LOG.consumeClick()) {
            mc.setScreen(new QuestLogScreen());
        }

        // Falcon Command Menu
        if (ModKeyBindings.KEY_FALCON_COMMAND.consumeClick()) {
            QuantumFalconEntity falcon = FalconCommandScreen.findNearestOwnedFalcon();
            if (falcon != null) {
                mc.setScreen(new FalconCommandScreen(falcon));
            } else {
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cNo nearby falcon found!"), 
                    true
                );
            }
        }

        // Falcon Call (whistle)
        if (ModKeyBindings.KEY_FALCON_CALL.consumeClick()) {
            // Check if player has whistle
            boolean hasWhistle = mc.player.getInventory().contains(
                stack -> stack.getItem().toString().contains("falcon_whistle")
            );
            
            if (hasWhistle) {
                // Send whistle packet to server
                // This would typically use a packet to call the falcon
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§a*Whistle*"), 
                    true
                );
            } else {
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cYou need a Falcon Whistle!"), 
                    true
                );
            }
        }
    }
}
