package com.matt.quantumfalcon.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Field-bounded text (gui_specs.md section 2.8): every string is drawn inside a rect, in one of three sizes (default 1.0, 9 px
 * line; small 0.75, 7 px; tiny 0.5, 5 px). Wraps on spaces up to floor(h / line) lines, then ellipsizes the last line; a field
 * narrower than "..." shows only the glyphs that fit. Nothing is scaled arbitrarily and nothing paints outside the field.
 */
public final class QfText {
    public enum Size {
        DEFAULT(1f, 9), SMALL(0.75f, 7), TINY(0.5f, 5);
        final float scale; final int line;
        Size(float scale, int line) { this.scale = scale; this.line = line; }
    }
    public static final int LEFT = 0, CENTRE = 1, RIGHT = 2;

    private QfText() {}

    /** Lines as they will be drawn (for tests and layout checks). */
    public static List<String> layout(Font font, String text, int w, int h, Size size) {
        int maxW = (int) Math.floor(w / size.scale);
        int maxLines = Math.max(1, h / size.line);
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            if (cur.isEmpty()) cur.append(word);
            else if (font.width(cur + " " + word) <= maxW) cur.append(' ').append(word);
            else { out.add(cur.toString()); cur = new StringBuilder(word); }
        }
        out.add(cur.toString());
        if (out.size() > maxLines) {
            List<String> kept = new ArrayList<>(out.subList(0, maxLines - 1));
            kept.add(String.join(" ", out.subList(maxLines - 1, out.size())));
            out = kept;
        }
        for (int k = 0; k < out.size(); k++) out.set(k, ellipsize(font, out.get(k), maxW));
        return out;
    }

    static String ellipsize(Font font, String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        if (font.width("...") > maxW) return font.plainSubstrByWidth(s, maxW);
        String t = s;
        while (!t.isEmpty() && font.width(t + "...") > maxW) t = t.substring(0, t.length() - 1);
        return t.stripTrailing() + "...";
    }

    public static void draw(GuiGraphics g, Font font, String text, int x, int y, int w, int h, Size size, int argb, int align) {
        if (text == null || text.isEmpty() || w <= 0) return;
        if ((argb & 0xFF000000) == 0) argb |= 0xFF000000;
        List<String> lines = layout(font, text, w, Math.max(h, size.line), size);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(size.scale, size.scale, 1f);
        float fw = w / size.scale;
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            int lx = align == CENTRE ? Math.round((fw - font.width(l)) / 2f) : align == RIGHT ? Math.round(fw - font.width(l)) : 0;
            g.drawString(font, l, lx, Math.round(i * size.line / size.scale), argb, false);
        }
        g.pose().popPose();
    }
    public static void draw(GuiGraphics g, Font font, String text, int x, int y, int w, int h, Size size, int argb) {
        draw(g, font, text, x, y, w, h, size, argb, LEFT);
    }
}
