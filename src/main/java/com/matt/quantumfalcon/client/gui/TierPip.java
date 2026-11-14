package com.matt.quantumfalcon.client.gui;

import com.matt.quantumfalcon.breeding.BreedingData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Colour-blind tier cues (art notes: tier_visual_language.md): every tier 1-7 has a unique 5x5 shape drawn in its
 * BreedingData colour, so tiers stay distinguishable without hue. Quantum uses cyan so it reads on white fills.
 */
public final class TierPip {
    private TierPip() {}
    // rows top to bottom, 5 bits each (bit 4 = left column)
    private static final int[][] MASKS = {
        {0b00100, 0b00100, 0b00100, 0b00100, 0b00100},   // 1 tick
        {0b00000, 0b10001, 0b01010, 0b00100, 0b00000},   // 2 chevron
        {0b00100, 0b01010, 0b10001, 0b01010, 0b00100},   // 3 diamond
        {0b01110, 0b10001, 0b10001, 0b10001, 0b01110},   // 4 cut-corner square
        {0b00001, 0b00010, 0b01110, 0b01000, 0b10000},   // 5 rune slash with crossbar
        {0b10101, 0b10101, 0b11111, 0b11111, 0b00000},   // 6 crown
        {0b10101, 0b01110, 0b11111, 0b01110, 0b10101},   // 7 hex star
    };

    public static int colour(int tier) {
        if (tier >= 7) return 0xFF28ECFF;
        return 0xFF000000 | BreedingData.TIERS.get(Mth.clamp(tier, 1, 7) - 1).colour();
    }

    /** Draws the pip with its top-left at (x, y); each mask pixel is {@code scale} GUI pixels. */
    public static void draw(GuiGraphics g, int x, int y, int tier, int scale) {
        int[] m = MASKS[Mth.clamp(tier, 1, 7) - 1];
        int c = colour(tier);
        for (int r = 0; r < 5; r++)
            for (int col = 0; col < 5; col++)
                if ((m[r] >> (4 - col) & 1) != 0) g.fill(x + col * scale, y + r * scale, x + (col + 1) * scale, y + (r + 1) * scale, c);
    }

    /** The same masks for generators/tests: true where the pip is lit. */
    public static boolean lit(int tier, int row, int col) { return (MASKS[Mth.clamp(tier, 1, 7) - 1][row] >> (4 - col) & 1) != 0; }
}
