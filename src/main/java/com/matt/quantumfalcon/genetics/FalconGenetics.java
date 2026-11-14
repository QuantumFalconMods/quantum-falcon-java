package com.matt.quantumfalcon.genetics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Full genetics for Quantum Falcon breeding (egg-based).
 * Four genes: color (variant), pattern, glow, ability.
 * Mendelian inheritance with mutation chance.
 */
public class FalconGenetics {

    /** Colour gene = falcon variant (all 18, matches QuantumFalconEntity.VARIANTS). */
    public static final String[] COLOR_GENES = {
        "peregrine", "prairie", "tundra", "stormrider", "crimson", "warped", "deepslate", "voidstalker", "aetherwing",
        "coral", "mushroom", "entangled_prism", "soulfire", "tempest", "abyssal", "singularity", "cosmic", "quantum_phoenix"
    };
    /** Overworld variants: what a 5% breeding mutation can produce. */
    public static final String[] COMMON_COLORS = { "peregrine", "prairie", "tundra", "stormrider", "crimson", "deepslate", "coral", "mushroom", "soulfire", "tempest" };
    /** Echo Rift wild variants. */
    public static final String[] RIFT_COLORS = { "voidstalker", "aetherwing", "warped", "entangled_prism", "abyssal" };
    public static boolean isRift(String c) { for (String r : RIFT_COLORS) if (r.equals(c)) return true; return false; }
    private static final int[] COLOR_DOMINANCE = { 0, 1, 2, 3, 4, 5 }; // index = dominance

    public static final String[] PATTERN_GENES = { "solid", "striped", "spotted", "gradient" };
    public static final String[] ABILITY_GENES = {
        "speed_boost", "fire_resist", "night_vision", "strength", "quantum_dash", "void_step"
    };

    private final String colorGene;
    private final String patternGene;
    private final int glowGene;   // 0-3
    private final String abilityGene;

    public FalconGenetics(String colorGene, String patternGene, int glowGene, String abilityGene) {
        this.colorGene = validColor(colorGene);
        this.patternGene = validPattern(patternGene);
        this.glowGene = Math.max(0, Math.min(3, glowGene));
        this.abilityGene = validAbility(abilityGene);
    }

    public String getColorGene() { return colorGene; }
    public String getPatternGene() { return patternGene; }
    public int getGlowGene() { return glowGene; }
    public String getAbilityGene() { return abilityGene; }

    /**
     * Maps color gene to entity variant name (must match QuantumFalconEntity.VARIANTS).
     */
    public String getVariantFromGenes() {
        return getVariantFromColorGene(colorGene);
    }

    public static String getVariantFromColorGene(String color) {
        if (color == null || color.isEmpty()) return "peregrine";
        String c = color.toLowerCase(Locale.ROOT);
        for (String v : COLOR_GENES) {
            if (v.equals(c)) return v;
        }
        if ("quantum".equals(c)) return "quantum_phoenix";
        if ("void".equals(c)) return "voidstalker";
        if ("prismatic".equals(c)) return "entangled_prism";
        return "peregrine";
    }

    /**
     * Breed two parents with Mendelian rules + 5% mutation per gene.
     */
    public static FalconGenetics breed(FalconGenetics parent1, FalconGenetics parent2, RandomSource random) {
        float mutationChance = 0.05f;
        boolean breedingMaster = false; // Caller can pass player context for 1.5x hatch / mutation

        String color = pickColorAllele(parent1.colorGene, parent2.colorGene, random, mutationChance);
        String pattern = pickPatternAllele(parent1.patternGene, parent2.patternGene, random, mutationChance);
        int glow = pickGlowGene(parent1.glowGene, parent2.glowGene, random, mutationChance);
        String ability = pickAbilityAllele(parent1.abilityGene, parent2.abilityGene, random);

        return new FalconGenetics(color, pattern, glow, ability);
    }

    /**
     * Child colour: 50/50 from either parent; 5% mutation into an overworld variant; 1% cosmic mutation.
     * Special pairings: two Echo Rift variants -> 10% Cosmic; Cosmic or Singularity x Echo Rift variant -> 10% Quantum Phoenix.
     */
    static String pickColorAllele(String a, String b, RandomSource r, float mutation) {
        // Matthew's egg guide: Stormrider + Voidstalker -> 15% Entangled Prism; Entangled Prism + Aetherwing -> 1% Singularity
        if (pair(a, b, "stormrider", "voidstalker") && r.nextFloat() < 0.15f) return "entangled_prism";
        if (pair(a, b, "entangled_prism", "aetherwing") && r.nextFloat() < 0.01f) return "singularity";
        boolean legendaryParent = "cosmic".equals(a) || "singularity".equals(a) || "cosmic".equals(b) || "singularity".equals(b);
        if (legendaryParent && (isRift(a) || isRift(b)) && r.nextFloat() < 0.10f) return "quantum_phoenix";
        if (isRift(a) && isRift(b) && r.nextFloat() < 0.10f) return "cosmic";
        if (r.nextFloat() < 0.01f) return "cosmic";
        if (r.nextFloat() < mutation) return COMMON_COLORS[r.nextInt(COMMON_COLORS.length)];
        return r.nextBoolean() ? a : b;
    }

    private static boolean pair(String a, String b, String x, String y) { return (a.equals(x) && b.equals(y)) || (a.equals(y) && b.equals(x)); }

    /** Genetics for an existing falcon that keeps its current variant as the colour gene. */
    public static FalconGenetics forVariant(String variant, RandomSource random) {
        FalconGenetics g = random(random);
        return new FalconGenetics(variant, g.patternGene, g.glowGene, g.abilityGene);
    }

    private static String pickPatternAllele(String a, String b, RandomSource r, float mutation) {
        if (r.nextFloat() < mutation) {
            return PATTERN_GENES[r.nextInt(PATTERN_GENES.length)];
        }
        return r.nextBoolean() ? a : b;
    }

    private static int pickGlowGene(int g1, int g2, RandomSource r, float mutation) {
        int avg = (g1 + g2) / 2;
        if (r.nextFloat() < mutation) {
            return Math.min(3, avg + 1);
        }
        return Math.max(0, Math.min(3, avg));
    }

    private static String pickAbilityAllele(String a, String b, RandomSource r) {
        if (r.nextFloat() < 0.10f) {
            return ABILITY_GENES[r.nextInt(ABILITY_GENES.length)];
        }
        return r.nextBoolean() ? a : b;
    }

    private static int dominanceIndex(String gene, String[] genes) {
        for (int i = 0; i < genes.length; i++) {
            if (genes[i].equalsIgnoreCase(gene)) return i;
        }
        return 0;
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("color", colorGene);
        tag.putString("pattern", patternGene);
        tag.putInt("glow", glowGene);
        tag.putString("ability", abilityGene);
        return tag;
    }

    @Nullable
    public static FalconGenetics deserialize(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return null;
        if (!tag.contains("color") || !tag.contains("pattern")) return null;
        String color = tag.getString("color");
        String pattern = tag.getString("pattern");
        int glow = tag.getInt("glow");
        String ability = tag.contains("ability") ? tag.getString("ability") : ABILITY_GENES[0];
        return new FalconGenetics(color, pattern, glow, ability);
    }

    /**
     * Create random genetics for wild spawn or new falcon.
     */
    public static FalconGenetics random(RandomSource random) {
        String color = COMMON_COLORS[random.nextInt(COMMON_COLORS.length)];
        String pattern = PATTERN_GENES[random.nextInt(PATTERN_GENES.length)];
        int glow = random.nextInt(4);
        String ability = ABILITY_GENES[random.nextInt(ABILITY_GENES.length)];
        return new FalconGenetics(color, pattern, glow, ability);
    }

    private static String validColor(String s) {
        if (s == null || s.isEmpty()) return "peregrine";
        String lower = s.toLowerCase(Locale.ROOT);
        for (String c : COLOR_GENES) if (c.equals(lower)) return c;
        return getVariantFromColorGene(lower);
    }

    private static String validPattern(String s) {
        if (s == null || s.isEmpty()) return "solid";
        String lower = s.toLowerCase(Locale.ROOT);
        for (String p : PATTERN_GENES) if (p.equals(lower)) return p;
        return "solid";
    }

    private static String validAbility(String s) {
        if (s == null || s.isEmpty()) return ABILITY_GENES[0];
        String lower = s.toLowerCase(Locale.ROOT);
        for (String a : ABILITY_GENES) if (a.equals(lower)) return a;
        return ABILITY_GENES[0];
    }

    public boolean isRareOrLegendary() {
        return "cosmic".equals(colorGene) || "singularity".equals(colorGene) || "quantum_phoenix".equals(colorGene);
    }
}
