package com.matt.quantumfalcon.client.screen;

import com.matt.quantumfalcon.network.EndingChoicePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import static com.matt.quantumfalcon.story.StoryChoicePalette.*;

/**
 * The final choice after the Sovereign falls. the client only sends the pick ({@link EndingChoicePayload}); the server
 * validates it, stores it in the player's story attachment and plays the ending. Whether the True path is open, and the
 * relic / bond progress behind it, come from the server in the open-screen payload.
 * picking a path asks for confirmation first; all text sits on an 85% black plate with at least 4.5:1 contrast
 * (see StoryChoicePalette and the audit_n4_choice_contrast qftest); the True path's lock reason is always visible.
 * Layout fits 640x360 (1080p at GUI scale 3) and 480x270 (scale 4).
 */
public class StoryChoiceScreen extends Screen {
    private static final int BW = 220, BH = 20, GAP = 38, PLATE_W = 340, PLATE_H = 172;
    private final boolean trueUnlocked;
    private final int relics, bonds;
    private String pending;   // the path awaiting confirmation, or null

    public StoryChoiceScreen(boolean trueUnlocked, int relics, int bonds) {
        super(Component.literal("The Final Choice").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        this.trueUnlocked = trueUnlocked;
        this.relics = relics;
        this.bonds = bonds;
    }

    private int top() { return Math.max(4, this.height / 2 - PLATE_H / 2); }

    private static String pathName(String ending) {
        return switch (ending) { case "LIGHT" -> "Light Path"; case "DARK" -> "Dark Path"; default -> "True Path"; };
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2, y = top() + 40;
        if (pending != null) {
            addRenderableWidget(Button.builder(Component.literal("Confirm: " + pathName(pending)).withStyle(ChatFormatting.BOLD), b -> send(pending))
                .bounds(cx - 135, y + 54, 130, BH).build());
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> pick(null))
                .bounds(cx + 5, y + 54, 130, BH).build());
            return;
        }
        addRenderableWidget(Button.builder(Component.literal("\u2605 LIGHT PATH").withStyle(ChatFormatting.BOLD), b -> pick("LIGHT"))
            .bounds(cx - BW / 2, y, BW, BH).build());
        addRenderableWidget(Button.builder(Component.literal("\u2605 DARK PATH").withStyle(ChatFormatting.BOLD), b -> pick("DARK"))
            .bounds(cx - BW / 2, y + GAP, BW, BH).build());
        Button t = addRenderableWidget(Button.builder(trueUnlocked
                ? Component.literal("\u2605 TRUE PATH").withStyle(ChatFormatting.BOLD)
                : Component.literal("\u2605 TRUE PATH (locked)"), b -> pick("TRUE"))
            .bounds(cx - BW / 2, y + 2 * GAP, BW, BH).build());
        t.active = trueUnlocked;
    }

    /** Ask for confirmation (or go back with null). Public for the dev smoke script. */
    public void devPick(String ending) { pick(ending); }

    private void pick(String ending) {
        if ("TRUE".equals(ending) && !trueUnlocked) return;
        this.pending = ending;
        this.rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);   // dimmed world + buttons
        int cx = this.width / 2, y = top();
        g.drawCenteredString(font, Component.literal("THE HOUR IS YOURS").withStyle(ChatFormatting.BOLD), cx, y + 8, TITLE);
        if (pending != null) {
            g.drawCenteredString(font, "Choose the " + pathName(pending) + "?", cx, y + 40, CONFIRM);
            g.drawCenteredString(font, "This can't be undone.", cx, y + 54, WARNING);
            g.drawCenteredString(font, describe(pending), cx, y + 72, colour(pending));
            return;
        }
        g.drawCenteredString(font, "Choose your ending. It is permanent.", cx, y + 22, SUBTITLE);
        int by = y + 40 + BH + 3;
        g.drawCenteredString(font, describe("LIGHT"), cx, by, LIGHT);
        g.drawCenteredString(font, describe("DARK"), cx, by + GAP, DARK);
        g.drawCenteredString(font, describe("TRUE"), cx, by + 2 * GAP, colour("TRUE"));
        if (!trueUnlocked) {
            int ly = by + 2 * GAP + 12;
            String rel = "Relics " + relics + "/4", bon = "Max-bond falcon types " + bonds + "/4";
            String sep = "   \u00b7   ";
            int w = font.width("Locked: ") + font.width(rel) + font.width(sep) + font.width(bon), x = cx - w / 2;
            x = g.drawString(font, "Locked: ", x, ly, LOCK_REASON);
            x = g.drawString(font, rel, x, ly, relics >= 4 ? MET : LOCK_REASON);
            x = g.drawString(font, sep, x, ly, LOCK_REASON);
            g.drawString(font, bon, x, ly, bonds >= 4 ? MET : LOCK_REASON);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int cx = this.width / 2, y = top();
        g.fill(cx - PLATE_W / 2, y, cx + PLATE_W / 2, y + PLATE_H, PLATE);
        g.renderOutline(cx - PLATE_W / 2, y, PLATE_W, PLATE_H, 0xFF7A5C1E);
    }

    private static String describe(String ending) {
        return switch (ending) {
            case "LIGHT" -> "Let the morning come. The night ends, and so does its hold on you.";
            case "DARK" -> "Keep the hour. Wear the crown the Overseer made.";
            default -> "Braid the hours. Morning comes, and the doors stay.";
        };
    }

    private int colour(String ending) {
        return switch (ending) { case "LIGHT" -> LIGHT; case "DARK" -> DARK; default -> trueUnlocked ? TRUE_OPEN : TRUE_LOCKED; };
    }

    private void send(String ending) {
        PacketDistributor.sendToServer(new EndingChoicePayload(ending));
        this.onClose();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && pending != null) { pick(null); return true; }   // Esc backs out of the confirm step
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }   // the server must keep running to receive the choice

    @Override
    public boolean shouldCloseOnEsc() { return false; }   // a choice is required; logging out keeps it pending (re-offered on login)
}
