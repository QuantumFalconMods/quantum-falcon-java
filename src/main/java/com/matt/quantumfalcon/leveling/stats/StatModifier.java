package com.matt.quantumfalcon.leveling.stats;

/**
 * Represents a stat modifier with a source, value, operation, and optional priority.
 * Used in stat calculations for leveling bonuses.
 */
public class StatModifier {
    
    private final String source;
    private final float value;
    private final StatOperation operation;
    private final int priority;

    /**
     * Create a stat modifier with default priority.
     */
    public StatModifier(String source, float value, StatOperation operation) {
        this(source, value, operation, 0);
    }

    /**
     * Create a stat modifier with specified priority.
     */
    public StatModifier(String source, float value, StatOperation operation, int priority) {
        this.source = source != null ? source : "unknown";
        this.value = value;
        this.operation = operation != null ? operation : StatOperation.ADD;
        this.priority = priority;
    }

    // ==================== GETTERS ====================

    public String getSource() {
        return source;
    }

    public float getValue() {
        return value;
    }

    public StatOperation getOperation() {
        return operation;
    }

    public int getPriority() {
        return priority;
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create an additive modifier.
     */
    public static StatModifier flat(String source, float value) {
        return new StatModifier(source, value, StatOperation.ADD);
    }

    /**
     * Create a multiplicative modifier (stacks with others).
     */
    public static StatModifier multiplier(String source, float value) {
        return new StatModifier(source, value, StatOperation.MUL2);
    }

    /**
     * Create a base multiplier (doesn't stack).
     */
    public static StatModifier baseMultiplier(String source, float value) {
        return new StatModifier(source, value, StatOperation.MUL1);
    }

    /**
     * Create a max value modifier.
     */
    public static StatModifier max(String source, float value) {
        return new StatModifier(source, value, StatOperation.MAX);
    }

    /**
     * Create an average value modifier with priority.
     */
    public static StatModifier average(String source, float value, int weight) {
        return new StatModifier(source, value, StatOperation.AVG, weight);
    }

    // ==================== UTILITY ====================

    @Override
    public String toString() {
        return String.format("StatModifier[%s: %s %.2f (priority %d)]",
                source, operation, value, priority);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StatModifier other)) return false;
        return source.equals(other.source) &&
               Float.compare(value, other.value) == 0 &&
               operation == other.operation &&
               priority == other.priority;
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + Float.hashCode(value);
        result = 31 * result + operation.hashCode();
        result = 31 * result + priority;
        return result;
    }
}
