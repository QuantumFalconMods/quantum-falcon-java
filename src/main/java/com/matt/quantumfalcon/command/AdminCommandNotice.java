package com.matt.quantumfalcon.command;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;

import java.util.List;
import java.util.Set;

/**
 * the admin branches of /qf are hidden from non-operators (vanilla convention), which on its own makes Brigadier answer
 * "Incorrect argument for command". Catch those cases and say plainly that the command needs operator permission.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class AdminCommandNotice {
    private AdminCommandNotice() {}

    private static final Set<String> ADMIN = Set.of(
        "skill give", "skill grant", "skill reset", "skill unlockall",
        "quest complete", "quest reset",
        "falcon spawn", "falcon tame", "falcon bond", "falcon heal",
        "instability", "boss", "debug");

    /** The admin branch named by a /qf command line (without the slash), or null if it is not one. */
    public static String adminBranch(String line) {
        String[] w = line.trim().split("\\s+");
        if (w.length < 2 || !w[0].equals("qf")) return null;
        if (ADMIN.contains(w[1])) return w[1];
        if (w.length >= 3 && ADMIN.contains(w[1] + " " + w[2])) return w[1] + " " + w[2];
        return null;
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        var parse = event.getParseResults();
        var src = parse.getContext().getSource();
        if (ModCommands.admin(src)) return;
        String branch = adminBranch(parse.getReader().getString());
        if (branch == null) return;
        event.setCanceled(true);
        src.sendFailure(Component.literal("/qf " + branch + " is for server operators (permission level 2). ")
            .append(Component.literal("Player commands: " + String.join(", ", List.of("/qf skill list", "/qf skill unlock", "/qf quest list", "/qf quest accept", "/qf falcon info")))
                .withStyle(ChatFormatting.GRAY)));
    }
}
