package com.matt.quantumfalcon.genetics;

/**
 * Falcon pattern alleles for Mendelian genetics breeding system.
 * Patterns determine the texture overlay applied to the falcon.
 */
public enum FalconPattern {
    // Common patterns (dominance 1-2)
    SOLID(1, false),
    STRIPED(1, false),
    SPOTTED(1, false),
    GRADIENT(2, false),
    MOTTLED(2, false),
    BARRED(2, false),
    SPECKLED(2, false),
    
    // Rare pattern (dominance 3)
    MARBLED(3, true);
    
    private final int dominance;
    private final boolean rare;
    
    FalconPattern(int dominance, boolean rare) {
        this.dominance = dominance;
        this.rare = rare;
    }
    
    public int getDominance() {
        return dominance;
    }
    
    public boolean isDominant() {
        return dominance >= 2;
    }
    
    public boolean isRare() {
        return rare;
    }
    
    /**
     * Get the texture suffix for this pattern
     */
    public String getTextureSuffix() {
        return name().toLowerCase();
    }
}
