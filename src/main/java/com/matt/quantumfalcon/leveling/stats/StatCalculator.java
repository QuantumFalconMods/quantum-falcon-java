package com.matt.quantumfalcon.leveling.stats;

import java.util.*;

/**
 * Calculates final stat values from multiple modifiers.
 * Applies modifiers in order: AVG -> MAX -> MUL1 -> MUL2 -> ADD
 */
public class StatCalculator {

    private final Map<String, List<StatModifier>> modifiersByType = new HashMap<>();

    /**
     * Add a modifier to the calculation
     */
    public void addModifier(String statType, StatModifier modifier) {
        if (statType == null || modifier == null) return;
        modifiersByType.computeIfAbsent(statType, k -> new ArrayList<>()).add(modifier);
    }

    /**
     * Add a simple additive modifier
     */
    public void addFlat(String statType, String source, float value) {
        addModifier(statType, StatModifier.flat(source, value));
    }

    /**
     * Add a multiplicative modifier (stacks with others)
     */
    public void addMultiplier(String statType, String source, float value) {
        addModifier(statType, StatModifier.multiplier(source, value));
    }

    /**
     * Add a base multiplier (doesn't stack)
     */
    public void addBaseMultiplier(String statType, String source, float value) {
        addModifier(statType, StatModifier.baseMultiplier(source, value));
    }

    /**
     * Calculate the final value for a stat type
     */
    public float calculate(String statType) {
        return calculate(statType, 0f);
    }

    /**
     * Calculate the final value for a stat type with a base value
     */
    public float calculate(String statType, float baseValue) {
        List<StatModifier> modifiers = modifiersByType.get(statType);
        if (modifiers == null || modifiers.isEmpty()) {
            return baseValue;
        }

        // Group modifiers by operation
        Map<StatOperation, List<StatModifier>> byOperation = new EnumMap<>(StatOperation.class);
        for (StatModifier mod : modifiers) {
            byOperation.computeIfAbsent(mod.getOperation(), k -> new ArrayList<>()).add(mod);
        }

        float result = baseValue;

        // 1. AVG - Weighted average (if any)
        List<StatModifier> avgMods = byOperation.get(StatOperation.AVG);
        if (avgMods != null && !avgMods.isEmpty()) {
            float totalWeight = 0;
            float weightedSum = 0;
            for (StatModifier mod : avgMods) {
                float weight = mod.getPriority() > 0 ? mod.getPriority() : 1;
                weightedSum += mod.getValue() * weight;
                totalWeight += weight;
            }
            if (totalWeight > 0) {
                result = weightedSum / totalWeight;
            }
        }

        // 2. MAX - Raise to maximum
        List<StatModifier> maxMods = byOperation.get(StatOperation.MAX);
        if (maxMods != null) {
            for (StatModifier mod : maxMods) {
                result = Math.max(result, mod.getValue());
            }
        }

        // 3. MUL1 - Multiply base (take highest)
        List<StatModifier> mul1Mods = byOperation.get(StatOperation.MUL1);
        if (mul1Mods != null && !mul1Mods.isEmpty()) {
            float highestMul1 = 1.0f;
            for (StatModifier mod : mul1Mods) {
                if (mod.getValue() > highestMul1) {
                    highestMul1 = mod.getValue();
                }
            }
            result *= highestMul1;
        }

        // 4. MUL2 - Multiply total (stack multiplicatively)
        List<StatModifier> mul2Mods = byOperation.get(StatOperation.MUL2);
        if (mul2Mods != null) {
            for (StatModifier mod : mul2Mods) {
                result *= mod.getValue();
            }
        }

        // 5. ADD - Add/subtract flat values
        List<StatModifier> addMods = byOperation.get(StatOperation.ADD);
        if (addMods != null) {
            for (StatModifier mod : addMods) {
                result += mod.getValue();
            }
        }

        return result;
    }

    /**
     * Clear all modifiers
     */
    public void clear() {
        modifiersByType.clear();
    }

    /**
     * Clear modifiers for a specific stat type
     */
    public void clear(String statType) {
        modifiersByType.remove(statType);
    }

    /**
     * Get all modifiers for a stat type
     */
    public List<StatModifier> getModifiers(String statType) {
        return modifiersByType.getOrDefault(statType, Collections.emptyList());
    }

    /**
     * Check if there are any modifiers for a stat type
     */
    public boolean hasModifiers(String statType) {
        List<StatModifier> mods = modifiersByType.get(statType);
        return mods != null && !mods.isEmpty();
    }

    /**
     * Get a breakdown of how a stat is calculated (for debugging/tooltips)
     */
    public String getCalculationBreakdown(String statType, float baseValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("Base: ").append(String.format("%.2f", baseValue)).append("\n");

        List<StatModifier> modifiers = modifiersByType.get(statType);
        if (modifiers == null || modifiers.isEmpty()) {
            sb.append("No modifiers\n");
            sb.append("Final: ").append(String.format("%.2f", baseValue));
            return sb.toString();
        }

        // Sort by operation order
        List<StatModifier> sorted = new ArrayList<>(modifiers);
        sorted.sort(Comparator.comparingInt(m -> m.getOperation().getOrder()));

        for (StatModifier mod : sorted) {
            sb.append(String.format("  %s: %s %.2f (%s)\n",
                mod.getOperation(),
                mod.getValue() >= 0 ? "+" : "",
                mod.getValue(),
                mod.getSource()));
        }

        sb.append("Final: ").append(String.format("%.2f", calculate(statType, baseValue)));
        return sb.toString();
    }
}
