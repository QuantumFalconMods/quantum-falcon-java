package com.matt.quantumfalcon.breeding;

/** Nest tiers (BREEDING.md "Nests"): hatch-speed multiplier, extra mutation chance, which incubation needs the nest
 * satisfies on its own, and the egg seat height (px): where the egg rests, near the rim so it reads clearly on top of the nest. */
public enum NestQuality {
    TWIG("twig_nest", "Twig Nest", 1, 1.10f, 0.00f, false, false, false, 5.0f),
    QUANTUM("quantum_nest", "Quantum Nest", 2, 1.15f, 0.01f, true, false, false, 7.0f),
    CRYSTAL("crystal_nest", "Crystal Nest", 3, 1.25f, 0.03f, true, false, false, 7.0f),
    VOIDFORGED("voidforged_nest", "Void-forged Nest", 5, 1.40f, 0.06f, true, true, false, 6.0f),
    AERIE("quantum_aerie", "Quantum Aerie", 7, 1.60f, 0.10f, true, true, true, 8.0f);

    public final String id, displayName;
    public final int tier;
    public final float speed, mutation, seatPx;
    public final boolean heat, riftEnergy, quantum;

    NestQuality(String id, String name, int tier, float speed, float mutation, boolean heat, boolean rift, boolean quantum, float seat) {
        this.id = id; this.displayName = name; this.tier = tier; this.speed = speed; this.mutation = mutation;
        this.heat = heat; this.riftEnergy = rift; this.quantum = quantum; this.seatPx = seat;
    }

    /** Glow accent per nest (egg_nest_specs.md "Nest palettes"); FalconNestRenderer tints the grayscale glow with it. */
    public int accent() {
        return switch (this) {
            case TWIG -> 0x00E5FF;
            case QUANTUM -> 0x28ECFF;
            case CRYSTAL -> 0xFF4FD8;
            case VOIDFORGED -> 0xB04CFF;
            case AERIE -> 0x78F0FF;
        };
    }

    /** Nest a wild site of the given danger tier uses. */
    public static NestQuality forTier(int tier) {
        return tier >= 7 ? AERIE : tier >= 5 ? VOIDFORGED : tier >= 3 ? CRYSTAL : TWIG;
    }
}
