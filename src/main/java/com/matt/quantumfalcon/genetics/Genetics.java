package com.matt.quantumfalcon.genetics;

import net.minecraft.nbt.CompoundTag;
import javax.annotation.Nullable;

/**
 * Genetics data for a Quantum Falcon using Mendelian inheritance.
 * Each falcon has two alleles for color and two for pattern.
 * The expressed phenotype is determined by dominance rules.
 */
public class Genetics {
    public final FalconColor colorAllele1;
    public final FalconColor colorAllele2;
    public final FalconPattern patternAllele1;
    public final FalconPattern patternAllele2;

    public Genetics(FalconColor colorAllele1, FalconColor colorAllele2, 
                    FalconPattern patternAllele1, FalconPattern patternAllele2) {
        this.colorAllele1 = colorAllele1;
        this.colorAllele2 = colorAllele2;
        this.patternAllele1 = patternAllele1;
        this.patternAllele2 = patternAllele2;
    }

    /**
     * Get the expressed (visible) color based on dominance
     */
    public FalconColor getExpressedColor() {
        if (colorAllele1.getDominance() >= colorAllele2.getDominance()) {
            return colorAllele1;
        }
        return colorAllele2;
    }

    /**
     * Get the expressed (visible) pattern based on dominance
     */
    public FalconPattern getExpressedPattern() {
        if (patternAllele1.getDominance() >= patternAllele2.getDominance()) {
            return patternAllele1;
        }
        return patternAllele2;
    }

    /**
     * Check if this falcon has legendary genetics (Quantum color or Void+Marbled combo)
     */
    public boolean isLegendary() {
        boolean hasQuantum = colorAllele1 == FalconColor.QUANTUM || colorAllele2 == FalconColor.QUANTUM;
        boolean hasVoid = colorAllele1 == FalconColor.VOID || colorAllele2 == FalconColor.VOID;
        boolean hasMarbled = patternAllele1 == FalconPattern.MARBLED || patternAllele2 == FalconPattern.MARBLED;
        return hasQuantum || (hasVoid && hasMarbled);
    }

    /**
     * Check if color alleles are heterozygous (different)
     */
    public boolean isColorHeterozygous() {
        return colorAllele1 != colorAllele2;
    }

    /**
     * Check if pattern alleles are heterozygous (different)
     */
    public boolean isPatternHeterozygous() {
        return patternAllele1 != patternAllele2;
    }
    
    /**
     * Check if this falcon carries any rare alleles
     */
    public boolean hasRareAllele() {
        return colorAllele1.isRare() || colorAllele2.isRare() || 
               patternAllele1.isRare() || patternAllele2.isRare();
    }

    /**
     * Get a unique texture key for this genetics combination
     */
    public String getTextureKey() {
        return getExpressedColor().getTextureSuffix() + "_" + getExpressedPattern().getTextureSuffix();
    }

    /**
     * Save genetics to NBT
     */
    public void writeTo(CompoundTag tag) {
        tag.putString("C1", colorAllele1.name());
        tag.putString("C2", colorAllele2.name());
        tag.putString("P1", patternAllele1.name());
        tag.putString("P2", patternAllele2.name());
    }

    /**
     * Load genetics from NBT
     */
    @Nullable
    public static Genetics fromNbt(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return null;
        if (!tag.contains("C1") || !tag.contains("C2") || !tag.contains("P1") || !tag.contains("P2")) {
            return null;
        }
        try {
            // NeoForge 1.21.1: getString returns String directly
            String c1Str = tag.getString("C1");
            String c2Str = tag.getString("C2");
            String p1Str = tag.getString("P1");
            String p2Str = tag.getString("P2");
            
            if (c1Str.isEmpty() || c2Str.isEmpty() || p1Str.isEmpty() || p2Str.isEmpty()) {
                return null;
            }
            
            FalconColor c1 = FalconColor.valueOf(c1Str);
            FalconColor c2 = FalconColor.valueOf(c2Str);
            FalconPattern p1 = FalconPattern.valueOf(p1Str);
            FalconPattern p2 = FalconPattern.valueOf(p2Str);
            return new Genetics(c1, c2, p1, p2);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("Genetics[Color: %s/%s (%s), Pattern: %s/%s (%s), Legendary: %s]",
            colorAllele1, colorAllele2, getExpressedColor(),
            patternAllele1, patternAllele2, getExpressedPattern(),
            isLegendary());
    }
}
