package com.matt.quantumfalcon.genetics;

/**
 * Falcon color alleles for Mendelian genetics breeding system.
 * Each color has dominance level - higher wins when expressing phenotype.
 */
public enum FalconColor {
    // Common colors (dominance 1-3)
    WHITE(1, false),
    GRAY(1, false),
    BLACK(2, false),
    BROWN(2, false),
    GOLDEN(3, false),
    CRIMSON(3, false),
    AZURE(3, false),
    EMERALD(3, false),
    
    // Rare colors (dominance 4-5)
    VOID(4, true),
    QUANTUM(5, true);
    
    private final int dominance;
    private final boolean rare;
    
    FalconColor(int dominance, boolean rare) {
        this.dominance = dominance;
        this.rare = rare;
    }
    
    public int getDominance() {
        return dominance;
    }
    
    public boolean isDominant() {
        return dominance >= 3;
    }
    
    public boolean isRare() {
        return rare;
    }
    
    /**
     * Get the texture suffix for this color
     */
    public String getTextureSuffix() {
        return name().toLowerCase();
    }
}
