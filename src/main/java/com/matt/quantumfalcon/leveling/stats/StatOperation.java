package com.matt.quantumfalcon.leveling.stats;

/**
 * Operations for stat modifiers, inspired by Silent Gear.
 * Operations are applied in order: AVG -> MAX -> MUL1 -> MUL2 -> ADD
 */
public enum StatOperation {
    /**
     * Weighted average of all AVG modifiers.
     * Used for base stat calculations.
     */
    AVG(0),
    
    /**
     * Maximum value - raises base to at least this value.
     * Applied after AVG.
     */
    MAX(1),
    
    /**
     * Multiply base - multiplies the base value.
     * Multiple MUL1 operations do NOT stack with each other.
     */
    MUL1(2),
    
    /**
     * Multiply total - multiplies after MUL1.
     * Multiple MUL2 operations DO stack multiplicatively.
     */
    MUL2(3),
    
    /**
     * Add/subtract flat value.
     * Applied last.
     */
    ADD(4);

    private final int order;

    StatOperation(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    /**
     * Get the next operation in the calculation order.
     */
    public StatOperation next() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values().length) {
            return null;
        }
        return values()[nextOrdinal];
    }
}
