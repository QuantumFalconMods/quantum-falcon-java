package com.matt.quantumfalcon.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Commands for controlling tamed falcons.
 * These commands change falcon AI behavior.
 */
public enum FalconCommand {
    FOLLOW("Follow", "Return to owner and follow closely", ChatFormatting.GREEN, "🏠"),
    STAY("Stay", "Hold position and guard the area", ChatFormatting.YELLOW, "⚓"),
    WANDER("Wander", "Explore nearby area freely", ChatFormatting.AQUA, "🦅"),
    ATTACK("Attack", "Aggressively attack nearby hostiles", ChatFormatting.RED, "⚔"),
    GATHER("Gather", "Collect nearby dropped items", ChatFormatting.GOLD, "💎"),
    SCOUT("Scout", "Fly high and mark nearby points of interest", ChatFormatting.DARK_PURPLE, "👁"),
    ABILITY("Quantum Burst", "Perform special quantum ability (cooldown)", ChatFormatting.DARK_PURPLE, "✨");

    private final String name;
    private final String description;
    private final ChatFormatting color;
    private final String icon;

    FalconCommand(String name, String description, ChatFormatting color, String icon) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public Component getDisplayName() {
        return Component.literal(icon + " " + name).withStyle(color);
    }

    public Component getTooltip() {
        return Component.literal(description).withStyle(ChatFormatting.GRAY);
    }
}
