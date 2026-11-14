package com.matt.quantumfalcon.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Client-only: show a dungeon lore fragment with the vanilla book view. */
public final class LoreBookScreenOpener {
    private LoreBookScreenOpener() {}

    public static void open(String title, List<String> pages) {
        List<Component> out = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            Component page = i == 0
                ? Component.literal(title + "\n\n").withStyle(ChatFormatting.BOLD).append(Component.literal(pages.get(i)).withStyle(s -> s.withBold(false)))
                : Component.literal(pages.get(i));
            out.add(page);
        }
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess(out)));
    }
}
