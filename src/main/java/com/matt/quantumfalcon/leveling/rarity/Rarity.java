package com.matt.quantumfalcon.leveling.rarity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Item rarity tiers with associated bonuses and colors.
 * 7-tier system: Common -> Uncommon -> Rare -> Epic -> Legendary -> Mythic -> Quantum
 */
public enum Rarity {
    COMMON(
        "common",
        "Common",
        ChatFormatting.GRAY,
        0x9D9D9D,
        1.0f,   // damage multiplier
        1.0f,   // armor multiplier
        1.0f,   // xp multiplier
        1.0f,   // durability multiplier
        100.0f  // weight (chance)
    ),
    UNCOMMON(
        "uncommon",
        "Uncommon",
        ChatFormatting.GREEN,
        0x55FF55,
        1.05f,
        1.05f,
        1.1f,
        1.05f,
        50.0f
    ),
    RARE(
        "rare",
        "Rare",
        ChatFormatting.BLUE,
        0x5555FF,
        1.10f,
        1.10f,
        1.2f,
        1.10f,
        20.0f
    ),
    EPIC(
        "epic",
        "Epic",
        ChatFormatting.DARK_PURPLE,
        0xAA00AA,
        1.20f,
        1.20f,
        1.35f,
        1.15f,
        10.0f
    ),
    LEGENDARY(
        "legendary",
        "Legendary",
        ChatFormatting.GOLD,
        0xFFAA00,
        1.35f,
        1.35f,
        1.5f,
        1.25f,
        5.0f
    ),
    MYTHIC(
        "mythic",
        "Mythic",
        ChatFormatting.LIGHT_PURPLE,
        0xFF55FF,
        1.50f,
        1.50f,
        2.0f,
        1.50f,
        2.0f
    ),
    QUANTUM(
        "quantum",
        "Quantum",
        ChatFormatting.AQUA,
        0x00FFFF,
        2.0f,
        2.0f,
        3.0f,
        2.0f,
        1.0f
    );

    private final String name;
    private final String displayName;
    private final ChatFormatting formatting;
    private final int color;
    private final float damageMultiplier;
    private final float armorMultiplier;
    private final float xpMultiplier;
    private final float durabilityMultiplier;
    private final float weight;

    Rarity(String name, String displayName, ChatFormatting formatting, int color,
           float damageMultiplier, float armorMultiplier,
           float xpMultiplier, float durabilityMultiplier, float weight) {
        this.name = name;
        this.displayName = displayName;
        this.formatting = formatting;
        this.color = color;
        this.damageMultiplier = damageMultiplier;
        this.armorMultiplier = armorMultiplier;
        this.xpMultiplier = xpMultiplier;
        this.durabilityMultiplier = durabilityMultiplier;
        this.weight = weight;
    }

    // ==================== GETTERS ====================

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getFormatting() {
        return formatting;
    }

    public int getColor() {
        return color;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public float getArmorMultiplier() {
        return armorMultiplier;
    }

    public float getXpMultiplier() {
        return xpMultiplier;
    }

    public float getDurabilityMultiplier() {
        return durabilityMultiplier;
    }

    public float getWeight() {
        return weight;
    }

    // ==================== DISPLAY ====================

    /**
     * Get display component with color
     */
    public Component getDisplayComponent() {
        return Component.literal(displayName)
            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    /**
     * Get display component with brackets
     */
    public Component getBracketedComponent() {
        return Component.literal("[")
            .withStyle(ChatFormatting.DARK_GRAY)
            .append(getDisplayComponent())
            .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
    }

    // ==================== LOOKUP ====================

    /**
     * Get rarity by name (case insensitive)
     */
    public static Rarity byName(String name) {
        if (name == null || name.isEmpty()) return COMMON;

        for (Rarity r : values()) {
            if (r.name.equalsIgnoreCase(name)) {
                return r;
            }
        }
        return COMMON;
    }

    /**
     * Get rarity by ordinal (safe)
     */
    public static Rarity byOrdinal(int ordinal) {
        if (ordinal >= 0 && ordinal < values().length) {
            return values()[ordinal];
        }
        return COMMON;
    }

    // ==================== UTILITY ====================

    /**
     * Get next rarity tier (for upgrades)
     */
    public Rarity getNextTier() {
        int next = this.ordinal() + 1;
        if (next >= values().length) {
            return this;
        }
        return values()[next];
    }

    /**
     * Get previous rarity tier
     */
    public Rarity getPreviousTier() {
        int prev = this.ordinal() - 1;
        if (prev < 0) {
            return this;
        }
        return values()[prev];
    }

    /**
     * Check if this rarity is higher than another
     */
    public boolean isHigherThan(Rarity other) {
        return this.ordinal() > other.ordinal();
    }

    /**
     * Check if this rarity is at least as high as another
     */
    public boolean isAtLeast(Rarity other) {
        return this.ordinal() >= other.ordinal();
    }

    /**
     * Get total weight of all rarities
     */
    public static float getTotalWeight() {
        float total = 0;
        for (Rarity r : values()) {
            total += r.weight;
        }
        return total;
    }

    /**
     * Get number of stars for display (1-7)
     */
    public int getStarCount() {
        return ordinal() + 1;
    }

    /**
     * Get star display string
     */
    public String getStars() {
        StringBuilder sb = new StringBuilder();
        int stars = getStarCount();
        for (int i = 0; i < stars; i++) {
            sb.append("★");
        }
        return sb.toString();
    }
}
