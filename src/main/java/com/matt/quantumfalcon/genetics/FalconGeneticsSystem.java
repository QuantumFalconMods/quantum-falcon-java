package com.matt.quantumfalcon.genetics;

import net.minecraft.util.RandomSource;

/**
 * Mendelian genetics breeding system for Quantum Falcons.
 * Inspired by Better Animals Plus / Primal variant breeding.
 * Features:
 * - Mendelian inheritance with dominant/recessive alleles
 * - Rare mutation chance (2%)
 * - Legendary roll based on parent genetics
 */
public class FalconGeneticsSystem {
    private static final float MUTATION_CHANCE = 0.02f;
    private static final float BASE_LEGENDARY_CHANCE = 0.001f;
    
    // Common color pool
    private static final FalconColor[] COMMON_COLORS = {
        FalconColor.WHITE, FalconColor.BLACK, FalconColor.GRAY, FalconColor.BROWN,
        FalconColor.GOLDEN, FalconColor.CRIMSON, FalconColor.AZURE, FalconColor.EMERALD
    };
    
    // Rare color pool
    private static final FalconColor[] RARE_COLORS = {
        FalconColor.VOID, FalconColor.QUANTUM
    };
    
    // Common pattern pool
    private static final FalconPattern[] COMMON_PATTERNS = {
        FalconPattern.SOLID, FalconPattern.STRIPED, FalconPattern.SPOTTED,
        FalconPattern.GRADIENT, FalconPattern.MOTTLED, FalconPattern.BARRED, FalconPattern.SPECKLED
    };

    /**
     * Breed two falcons to produce offspring genetics.
     * Uses Mendelian inheritance with mutation chance and legendary roll.
     */
    public static Genetics breedFalcons(Genetics parent1, Genetics parent2, RandomSource random) {
        // Check for legendary offspring first
        float legendaryChance = getLegendaryChance(parent1, parent2);
        if (random.nextFloat() < legendaryChance) {
            return createLegendaryGenetics(random);
        }
        
        // Mendelian inheritance: pick one allele from each parent
        FalconColor color1 = pickAllele(random, parent1.colorAllele1, parent1.colorAllele2);
        FalconColor color2 = pickAllele(random, parent2.colorAllele1, parent2.colorAllele2);
        FalconPattern pattern1 = pickAllele(random, parent1.patternAllele1, parent1.patternAllele2);
        FalconPattern pattern2 = pickAllele(random, parent2.patternAllele1, parent2.patternAllele2);
        
        // Rare mutation chance: override one allele with random rare
        if (random.nextFloat() < MUTATION_CHANCE) {
            color1 = getRandomColor(random, true);
        }
        if (random.nextFloat() < MUTATION_CHANCE) {
            pattern1 = getRandomPattern(random, true);
        }
        
        return new Genetics(color1, color2, pattern1, pattern2);
    }

    /**
     * Pick one allele randomly from a pair (50/50 chance)
     */
    private static FalconColor pickAllele(RandomSource random, FalconColor a, FalconColor b) {
        return random.nextBoolean() ? a : b;
    }

    private static FalconPattern pickAllele(RandomSource random, FalconPattern a, FalconPattern b) {
        return random.nextBoolean() ? a : b;
    }

    /**
     * Create random genetics for a wild/spawned falcon
     */
    public static Genetics createRandomGenetics(RandomSource random) {
        FalconColor color1 = getRandomColor(random, false);
        FalconColor color2 = getRandomColor(random, false);
        FalconPattern pattern1 = getRandomPattern(random, false);
        FalconPattern pattern2 = getRandomPattern(random, false);
        
        // Small chance for wild falcon to have one rare allele
        if (random.nextFloat() < 0.05f) {
            color1 = getRandomColor(random, true);
        }
        
        return new Genetics(color1, color2, pattern1, pattern2);
    }

    /**
     * Create guaranteed legendary genetics
     */
    public static Genetics createLegendaryGenetics(RandomSource random) {
        FalconColor color1 = FalconColor.QUANTUM;
        FalconColor color2 = getRandomColor(random, true);
        FalconPattern pattern1 = FalconPattern.MARBLED;
        FalconPattern pattern2 = getRandomPattern(random, true);
        return new Genetics(color1, color2, pattern1, pattern2);
    }

    /**
     * Get a random color, optionally allowing rare colors
     */
    private static FalconColor getRandomColor(RandomSource random, boolean allowRare) {
        if (allowRare && random.nextFloat() < 0.01f) {
            return RARE_COLORS[random.nextInt(RARE_COLORS.length)];
        }
        return COMMON_COLORS[random.nextInt(COMMON_COLORS.length)];
    }

    /**
     * Get a random pattern, optionally allowing rare patterns
     */
    private static FalconPattern getRandomPattern(RandomSource random, boolean allowRare) {
        if (allowRare && random.nextFloat() < 0.01f) {
            return FalconPattern.MARBLED;
        }
        return COMMON_PATTERNS[random.nextInt(COMMON_PATTERNS.length)];
    }

    /**
     * Calculate legendary offspring chance based on parent genetics.
     * Factors:
     * - Base chance: 0.1%
     * - Quantum allele in either parent: +5%
     * - Marbled pattern in either parent: +3%
     * - Both parents legendary: +20%
     * - Max capped at 50%
     */
    public static float getLegendaryChance(Genetics parent1, Genetics parent2) {
        float chance = BASE_LEGENDARY_CHANCE;
        
        // Check for Quantum color alleles
        if (hasQuantumAllele(parent1) || hasQuantumAllele(parent2)) {
            chance += 0.05f;
        }
        
        // Check for Marbled pattern alleles
        if (hasMarbledAllele(parent1) || hasMarbledAllele(parent2)) {
            chance += 0.03f;
        }
        
        // Both parents legendary gives big boost
        if (parent1.isLegendary() && parent2.isLegendary()) {
            chance += 0.20f;
        }
        
        return Math.min(chance, 0.5f);
    }
    
    private static boolean hasQuantumAllele(Genetics g) {
        return g.colorAllele1 == FalconColor.QUANTUM || g.colorAllele2 == FalconColor.QUANTUM;
    }
    
    private static boolean hasMarbledAllele(Genetics g) {
        return g.patternAllele1 == FalconPattern.MARBLED || g.patternAllele2 == FalconPattern.MARBLED;
    }
}
