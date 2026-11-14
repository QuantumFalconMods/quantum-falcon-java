package com.matt.quantumfalcon.client.gui;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.breeding.BreedingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Shared GUI kit (art notes: gui_specs.md sections 2-3): the 9-slice neon panel, glow pulse, bars with cap markers, cards,
 * slot glows, tier and skull pips and the 8x8 icon set, all blitted from textures/gui/qf_ui_kit.png (hand-drawn in
 * tools/gen_gui_sheets.py). Colours are ARGB/RGB ints.
 */
public final class QfUi {
    public static final ResourceLocation KIT = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/gui/qf_ui_kit.png");
    public static final int CYAN = 0x28ECFF, MAGENTA = 0xFF38D6, VIOLET = 0xAA50FF, AMBER = 0xFFB43A, GUNMETAL = 0x6A7290;
    public static final int ICON_SPEED = 0, ICON_STRENGTH = 1, ICON_HUNT = 2, ICON_LOYALTY = 3, ICON_RIFT = 4, ICON_BOOK = 5,
            ICON_EGG = 6, ICON_SHARD = 7, ICON_SWORD = 8, ICON_WING = 9, ICON_LOCK = 10, ICON_CLAW = 11;
    public static final int SLOT_IDLE = 0, SLOT_HOVER = 1, SLOT_WORKING = 2, SLOT_ERROR = 3, SLOT_PAUSED = 4;
    public static final int CARD_ACTIVE = 0, CARD_DORMANT = 1, CARD_LOCKED = 2;

    private QfUi() {}

    private static float now(float pt) {
        Minecraft mc = Minecraft.getInstance();
        return (mc.level != null ? mc.level.getGameTime() : System.currentTimeMillis() / 50L) + pt;
    }
    /** 12 frames x 2 ticks; k = 0.40 + 0.60 * (0.5 - 0.5 cos(2 pi n / 12)). */
    public static float pulse(float pt) {
        float n = (now(pt) / 2f) % 12f;
        return 0.40f + 0.60f * (0.5f - 0.5f * Mth.cos((float) (2 * Math.PI * n / 12f)));
    }
    /** True for the 2-tick glitch frame at the peak (frame 6) of every 8th cycle. */
    public static boolean glitch(float pt) {
        long ticks = (long) now(pt);
        long cycle = ticks / 24L, frame = (ticks / 2L) % 12L;
        return cycle % 8L == 7L && frame == 6L;
    }
    /** Colour for the current pulse: state colour scaled by k; the glitch frame snaps toward white-hot magenta. */
    public static int glow(int rgb, float pt, float strength) {
        if (glitch(pt)) return 0xFFF08AE6;
        float k = 1f - strength * (1f - pulse(pt));
        int r = (int) (((rgb >> 16) & 255) * k), g = (int) (((rgb >> 8) & 255) * k), b = (int) ((rgb & 255) * k);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
    private static void tint(GuiGraphics g, int argb) {
        g.setColor(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
    }
    private static void untint(GuiGraphics g) { g.setColor(1f, 1f, 1f, 1f); }

    /** 9-slice blit of a 24x24 kit tile at (u, v): 8 px corners, edges tiled at an 8 px period, centre filled. */
    private static void nine(GuiGraphics g, int u, int v, int x, int y, int w, int h, boolean centre) {
        g.blit(KIT, x, y, u, v, 8, 8, 256, 256);
        g.blit(KIT, x + w - 8, y, u + 16, v, 8, 8, 256, 256);
        g.blit(KIT, x, y + h - 8, u, v + 16, 8, 8, 256, 256);
        g.blit(KIT, x + w - 8, y + h - 8, u + 16, v + 16, 8, 8, 256, 256);
        for (int ex = x + 8; ex < x + w - 8; ex += 8) {
            int ew = Math.min(8, x + w - 8 - ex);
            g.blit(KIT, ex, y, u + 8, v, ew, 8, 256, 256);
            g.blit(KIT, ex, y + h - 8, u + 8, v + 16, ew, 8, 256, 256);
        }
        for (int ey = y + 8; ey < y + h - 8; ey += 8) {
            int eh = Math.min(8, y + h - 8 - ey);
            g.blit(KIT, x, ey, u, v + 8, 8, eh, 256, 256);
            g.blit(KIT, x + w - 8, ey, u + 16, v + 8, 8, eh, 256, 256);
        }
        if (centre) g.fill(x + 8, y + 8, x + w - 8, y + h - 8, 0xFF0A0A10);
    }

    /** Panel in one state colour (0 = idle gunmetal). The neon is the kit glowmask tinted by colour x pulse. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int rgb, float pt) {
        nine(g, 0, 0, x, y, w, h, true);
        if (rgb == 0) return;
        int gx = glitch(pt) ? 1 : 0;
        tint(g, glow(rgb, pt, 1f));
        nine(g, 120, 0, x + gx, y, w, h, false);
        untint(g);
    }
    /** Split-hue panel: west / south in {@code a}, east / north in {@code b}. */
    public static void panelSplit(GuiGraphics g, int x, int y, int w, int h, int a, int b, float pt) {
        nine(g, 0, 0, x, y, w, h, true);
        int gx = glitch(pt) ? 1 : 0;
        g.enableScissor(x, y + h / 3, x + w / 2, y + h);           // west + south-west
        tint(g, glow(a, pt, 1f)); nine(g, 120, 0, x + gx, y, w, h, false);
        g.disableScissor();
        g.enableScissor(x + w / 2, y + h / 2, x + w, y + h);        // south-east keeps the west colour along the bottom
        nine(g, 120, 0, x + gx, y, w, h, false);
        g.disableScissor();
        g.enableScissor(x, y, x + w, y + h / 3);                    // north
        tint(g, glow(b, pt, 1f)); nine(g, 120, 0, x + gx, y, w, h, false);
        g.disableScissor();
        g.enableScissor(x + w / 2, y, x + w, y + h / 2);            // east upper half
        nine(g, 120, 0, x + gx, y, w, h, false);
        g.disableScissor();
        untint(g);
    }

    /** Horizontal bar: track, fill (0..1) in a kit colour row with pulse and a 3 px glint, optional cap marker (0..1, <0 = none). */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float fill, int rgb, float cap, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF404456);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF06080C);
        int fw = Math.round((w - 2) * Mth.clamp(fill, 0f, 1f));
        if (fw > 0) {
            int c = glow(rgb, pt, 0.5f);
            g.fillGradient(x + 1, y + 1, x + 1 + fw, y + h - 1, c, (c & 0x00FFFFFF) | 0xB0000000);
            g.fill(x + 1, y + 1, x + 1 + fw, y + 2, 0x60FFFFFF);
            float t = (now(pt) % 24f) / 24f;
            int gx = x + 1 + (int) (t * (fw + 3)) - 3;
            g.enableScissor(x + 1, y + 1, x + 1 + fw, y + h - 1);
            g.fill(gx, y + 1, gx + 3, y + h - 1, 0x90FFFFFF);
            g.disableScissor();
        }
        if (cap >= 0f) {
            int cx = x + 1 + Math.round((w - 2) * Mth.clamp(cap, 0f, 1f));
            g.fill(cx, y - 1, cx + 1, y + h + 1, 0xFFFFFFFF);
            g.fill(cx - 1, y - 3, cx + 2, y - 1, 0xFFFFFFFF);
        }
    }

    public static void card(GuiGraphics g, int x, int y, int w, int h, int state, int accent, float pt) {
        int fill = state == CARD_ACTIVE ? 0xFF0A0E16 : state == CARD_DORMANT ? 0xFF0C0A12 : 0xFF08080C;
        int col = state == CARD_ACTIVE ? glow(accent, pt, 1f) : state == CARD_DORMANT ? glow(VIOLET, pt, 0.6f) : 0xFF000000 | GUNMETAL;
        g.fill(x, y, x + w, y + h, fill);
        g.renderOutline(x, y, w, h, (col & 0x00FFFFFF) | 0x99000000);
        g.renderOutline(x + 2, y + 2, w - 4, h - 4, 0xFF1A1E2A);
        for (int t = 0; t < 3; t++) {
            g.fill(x + t, y, x + t + 1, y + 1, col); g.fill(x, y + t, x + 1, y + t + 1, col);
            g.fill(x + w - 1 - t, y, x + w - t, y + 1, col); g.fill(x + w - 1, y + t, x + w, y + t + 1, col);
            g.fill(x + t, y + h - 1, x + t + 1, y + h, col); g.fill(x, y + h - 1 - t, x + 1, y + h - t, col);
            g.fill(x + w - 1 - t, y + h - 1, x + w - t, y + h, col); g.fill(x + w - 1, y + h - 1 - t, x + w, y + h - t, col);
        }
    }

    /** 18x18 slot well; the glow states pulse (hover 40-70%). x, y are the slot's item origin (the well sits at -1, -1). */
    public static void slot(GuiGraphics g, int x, int y, int state, float pt) {
        g.blit(KIT, x - 1, y - 1, 0, 152, 18, 18, 256, 256);
        if (state == SLOT_IDLE) return;
        float k = state == SLOT_HOVER ? 0.4f + 0.3f * (pulse(pt) - 0.4f) / 0.6f : pulse(pt);
        g.setColor(k, k, k, 1f);
        g.blit(KIT, x - 1, y - 1, state * 18, 152, 18, 18, 256, 256);
        untint(g);
    }

    public static void icon(GuiGraphics g, int x, int y, int id, int argb) {
        tint(g, argb); g.blit(KIT, x, y, id * 8, 176, 8, 8, 256, 256); untint(g);
    }
    public static void tierPip(GuiGraphics g, int x, int y, int tier, boolean glow) {
        int n = Mth.clamp(tier, 1, 7) - 1;
        g.blit(KIT, x, y, n * 8, glow ? 88 : 80, 8, 8, 256, 256);
    }
    public static void skull(GuiGraphics g, int x, int y, boolean filled, int rgb, float pt) {
        if (!filled) { g.blit(KIT, x, y, 0, 96, 8, 8, 256, 256); return; }
        tint(g, glow(rgb, pt, 1f)); g.blit(KIT, x, y, 8, 96, 8, 8, 256, 256);
        untint(g);
    }
    public static int tierRgb(int tier) { return BreedingData.TIERS.get(Mth.clamp(tier, 1, 7) - 1).colour(); }

    /** UI cues from the audio pack: ui_click on a node / entry / button, ui_tab on a tab change. */
    public static void uiSound(String id) {
        var ev = com.matt.quantumfalcon.registry.ModSounds.byId(id);
        if (ev != null) net.minecraft.client.Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(ev, 1.0f));
    }
}
