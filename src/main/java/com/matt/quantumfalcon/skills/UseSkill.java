package com.matt.quantumfalcon.skills;

import java.util.Locale;

/**
 * A9: the eight use-based skills. They level by doing and NEVER restrict a tool, weapon or armour: a level-1 player can
 * use everything; levels only make the activity a little better. Pure data + maths (shared by server and client).
 */
public enum UseSkill {
    MINING("Mining", 0x9AA7B8),
    WOODCUTTING("Woodcutting", 0x8B6A3E),
    COMBAT("Combat", 0xE04848),
    ARCHERY("Archery", 0x7FC45A),
    FALCONRY("Falconry", 0xF2C14E),
    BREEDING("Breeding", 0xF07FB0),
    EXPLORATION("Exploration", 0x4FB3E8),
    ARCANA("Arcana", 0xB36BFF);

    public static final int MAX_LEVEL = 50;
    public final String label;
    public final int colour;

    UseSkill(String label, int colour) { this.label = label; this.colour = colour; }

    public String key() { return name().toLowerCase(Locale.ROOT); }

    /** XP needed to go from level L to L+1: 40 * L^1.6 (gentle curve; L49 -> 50 is about 20k). */
    public static long xpForNext(int level) { return Math.round(40.0 * Math.pow(level, 1.6)); }

    /** Total XP needed to reach a level from 0 XP. */
    public static long totalFor(int level) { long t = 0; for (int l = 1; l < level; l++) t += xpForNext(l); return t; }

    public static int levelOf(long xp) {
        int l = 1;
        while (l < MAX_LEVEL && xp >= totalFor(l + 1)) l++;
        return l;
    }

    /** 0 at level 1, 1 at level 50: every bonus scales linearly with this. */
    public static double frac(int level) { return Math.max(0, Math.min(MAX_LEVEL, level) - 1) / (double) (MAX_LEVEL - 1); }

    // ------------------------------------------------------------------ bonuses (all small and capped)
    public double primary(int level) {
        double f = frac(level);
        return switch (this) {
            case MINING, WOODCUTTING -> 0.25 * f;      // break speed
            case COMBAT -> 0.10 * f;                   // melee damage
            case ARCHERY -> 0.15 * f;                  // arrow damage
            case FALCONRY -> 0.15 * f;                 // your falcons' damage
            case BREEDING -> 0.05 * f;                 // rare-trait chance on eggs (added before breeding pity, never past its cap)
            case EXPLORATION -> 0.20 * f;              // chance a first-opened loot chest gives one extra stack
            case ARCANA -> 0.20 * f;                   // instability builds slower
        };
    }

    public double secondary(int level) {
        double f = frac(level);
        return switch (this) {
            case MINING, WOODCUTTING -> 0.10 * f;      // bonus drop chance
            case COMBAT -> 0.05 * f;                   // extra crit chance
            default -> 0;
        };
    }

    private static String pct(double v) {
        double p = v * 100;
        return (p == Math.rint(p) ? String.valueOf((int) p) : String.format(Locale.ROOT, "%.1f", p)) + "%";
    }

    /** The current bonus in plain words, e.g. "Stone and ore break 8% faster; 3% chance of a bonus ore". */
    public String describe(int level) {
        String a = pct(primary(level)), b = pct(secondary(level));
        return switch (this) {
            case MINING -> "Stone and ore break " + a + " faster; " + b + " chance of a bonus ore";
            case WOODCUTTING -> "Logs chop " + a + " faster; " + b + " chance of an extra log";
            case COMBAT -> "Melee hits deal " + a + " more damage; " + b + " extra chance to crit";
            case ARCHERY -> "Arrows you shoot deal " + a + " more damage";
            case FALCONRY -> "Your falcons deal " + a + " more damage";
            case BREEDING -> "Eggs your falcons lay get +" + a + " rare-trait chance";
            case EXPLORATION -> a + " chance of an extra item from a new loot chest";
            case ARCANA -> "Rift instability builds " + a + " slower";
        };
    }

    public String howToEarn() {
        return switch (this) {
            case MINING -> "Mine natural stone and ore";
            case WOODCUTTING -> "Chop natural logs";
            case COMBAT -> "Hit hostile mobs in melee";
            case ARCHERY -> "Hit hostile mobs with arrows (longer shots give more)";
            case FALCONRY -> "Let your falcons fight with you";
            case BREEDING -> "Breed animals and falcons (new pairs only)";
            case EXPLORATION -> "Discover new biomes, structures and loot chests";
            case ARCANA -> "Brave the Rift while instability rises";
        };
    }
}
