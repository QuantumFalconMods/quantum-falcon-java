package com.matt.quantumfalcon.story;

/**
 * every colour drawn on the ending-choice screen, kept in common code so a server-side qftest can check contrast.
 * The text sits on an 85% black plate; the worst case behind it is a white sky, which leaves the plate at #262626.
 */
public final class StoryChoicePalette {
    private StoryChoicePalette() {}

    public static final int PLATE = 0xD9000000;          // 85% black
    public static final int WORST_PLATE_RGB = 0x262626;  // PLATE over pure white
    public static final int TITLE = 0xFFD700, SUBTITLE = 0xFFFF55, LIGHT = 0xE0E0E0, DARK = 0xC8A0FF, TRUE_OPEN = 0xFF99FF,
            TRUE_LOCKED = 0xA8A8A8, LOCK_REASON = 0xFFB060, CONFIRM = 0xFFFFFF, WARNING = 0xFF7070, MET = 0x70FF70;

    public static final int[] TEXT = {TITLE, SUBTITLE, LIGHT, DARK, TRUE_OPEN, TRUE_LOCKED, LOCK_REASON, CONFIRM, WARNING, MET};

    private static double channel(int c) { double s = c / 255.0; return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4); }

    public static double luminance(int rgb) {
        return 0.2126 * channel((rgb >> 16) & 255) + 0.7152 * channel((rgb >> 8) & 255) + 0.0722 * channel(rgb & 255);
    }

    /** WCAG contrast ratio between two opaque colours. */
    public static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }
}
