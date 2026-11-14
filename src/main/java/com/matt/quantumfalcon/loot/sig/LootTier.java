package com.matt.quantumfalcon.loot.sig;

import net.minecraft.ChatFormatting;

/** A8: WoW-style rarity tiers. Each has one colour used for the item name, the drop message and the Codex. */
public enum LootTier {
    COMMON("Common", ChatFormatting.WHITE, 0xFFFFFF, 1.0f),
    UNCOMMON("Uncommon", ChatFormatting.GREEN, 0x1EFF00, 1.5f),
    RARE("Rare", ChatFormatting.BLUE, 0x0070DD, 2.2f),
    EPIC("Epic", ChatFormatting.DARK_PURPLE, 0xA335EE, 3.2f),
    LEGENDARY("Legendary", ChatFormatting.GOLD, 0xFF8000, 4.5f);

    public final String label;
    public final ChatFormatting format;
    public final int rgb;
    /** Stat multiplier applied to the archetype template's base bonus. */
    public final float power;

    LootTier(String label, ChatFormatting format, int rgb, float power) { this.label = label; this.format = format; this.rgb = rgb; this.power = power; }

    public boolean epicOrBetter() { return ordinal() >= EPIC.ordinal(); }
}
