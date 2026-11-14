package com.matt.quantumfalcon.leveling.rarity;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.config.LevelingConfig;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.Random;

/**
 * Manages rarity assignment and bonuses.
 */
public class RarityManager {

    // Trait IDs granted by rarity
    public static final ResourceLocation TRAIT_DURABLE = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "durable");
    public static final ResourceLocation TRAIT_SHARP = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "sharp");
    public static final ResourceLocation TRAIT_REINFORCED = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "reinforced");
    public static final ResourceLocation TRAIT_BLESSED = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "blessed");
    public static final ResourceLocation TRAIT_SOULBOUND = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "soulbound");
    public static final ResourceLocation TRAIT_QUANTUM_INFUSED = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "quantum_infused");

    /**
     * Roll a random rarity based on weighted chances (Java Random)
     */
    public static Rarity rollRarity(Random random) {
        float totalWeight = calculateTotalWeight();
        float roll = random.nextFloat() * totalWeight;
        float cumulative = 0;

        for (Rarity r : Rarity.values()) {
            cumulative += r.getWeight() * getConfigMultiplier(r);
            if (roll < cumulative) {
                return r;
            }
        }

        return Rarity.COMMON;
    }

    /**
     * Roll a random rarity based on weighted chances (Minecraft RandomSource)
     */
    public static Rarity rollRarity(RandomSource random) {
        float totalWeight = calculateTotalWeight();
        float roll = random.nextFloat() * totalWeight;
        float cumulative = 0;

        for (Rarity r : Rarity.values()) {
            cumulative += r.getWeight() * getConfigMultiplier(r);
            if (roll < cumulative) {
                return r;
            }
        }

        return Rarity.COMMON;
    }

    /**
     * Roll rarity with luck bonus (higher luck = better chance for rare items)
     */
    public static Rarity rollRarityWithLuck(RandomSource random, float luck) {
        float totalWeight = calculateTotalWeight();
        float roll = random.nextFloat() * totalWeight;

        // Modify roll based on luck (higher luck = lower roll = better rarity)
        if (luck > 1.0f) {
            roll = roll / luck;
        }

        float cumulative = 0;
        for (Rarity r : Rarity.values()) {
            cumulative += r.getWeight() * getConfigMultiplier(r);
            if (roll < cumulative) {
                return r;
            }
        }

        return Rarity.COMMON;
    }

    /**
     * Calculate total weight of all rarities
     */
    private static float calculateTotalWeight() {
        float total = 0;
        for (Rarity r : Rarity.values()) {
            total += r.getWeight() * getConfigMultiplier(r);
        }
        return total;
    }

    /**
     * Get config-based multiplier for a rarity's weight
     */
    private static float getConfigMultiplier(Rarity rarity) {
        // Return config values if available, otherwise use defaults
        return switch (rarity) {
            case COMMON -> LevelingConfig.RARITY_WEIGHT_COMMON.get().floatValue();
            case UNCOMMON -> LevelingConfig.RARITY_WEIGHT_UNCOMMON.get().floatValue();
            case RARE -> LevelingConfig.RARITY_WEIGHT_RARE.get().floatValue();
            case EPIC -> LevelingConfig.RARITY_WEIGHT_EPIC.get().floatValue();
            case LEGENDARY -> LevelingConfig.RARITY_WEIGHT_LEGENDARY.get().floatValue();
            case MYTHIC -> LevelingConfig.RARITY_WEIGHT_MYTHIC.get().floatValue();
            case QUANTUM -> LevelingConfig.RARITY_WEIGHT_QUANTUM.get().floatValue();
        };
    }

    /**
     * Apply rarity-based bonuses and traits to item data
     */
    public static void applyRarityBonuses(ItemLevelData data) {
        if (data == null) return;

        Rarity rarity = data.getRarity();

        // Store multipliers as bonus stats
        data.setBonusStat("rarity_damage_mult", rarity.getDamageMultiplier());
        data.setBonusStat("rarity_armor_mult", rarity.getArmorMultiplier());
        data.setBonusStat("rarity_xp_mult", rarity.getXpMultiplier());
        data.setBonusStat("rarity_durability_mult", rarity.getDurabilityMultiplier());

        // Grant rarity-based traits (cumulative with higher rarities)
        switch (rarity) {
            case QUANTUM:
                data.addTrait(TRAIT_QUANTUM_INFUSED);
                // Fall through
            case MYTHIC:
                data.addTrait(TRAIT_SOULBOUND);
                data.setSoulbound(true);
                // Fall through
            case LEGENDARY:
                data.addTrait(TRAIT_BLESSED);
                // Fall through
            case EPIC:
                data.addTrait(TRAIT_REINFORCED);
                // Fall through
            case RARE:
                data.addTrait(TRAIT_SHARP);
                // Fall through
            case UNCOMMON:
                data.addTrait(TRAIT_DURABLE);
                break;
            case COMMON:
            default:
                // No rarity traits
                break;
        }
    }

    /**
     * Calculate effective XP gain with rarity multiplier
     */
    public static int calculateXpWithRarity(int baseXp, ItemLevelData data) {
        if (data == null) return baseXp;

        float mult = data.getBonusStat("rarity_xp_mult");
        if (mult <= 0) mult = 1.0f;

        return Math.round(baseXp * mult);
    }

    /**
     * Check if a rarity grants soulbound
     */
    public static boolean grantsSoulbound(Rarity rarity) {
        return rarity.isAtLeast(Rarity.MYTHIC);
    }

    /**
     * Get the minimum level bonus for a rarity
     */
    public static int getMinLevelBonus(Rarity rarity) {
        return switch (rarity) {
            case QUANTUM -> 10;
            case MYTHIC -> 5;
            case LEGENDARY -> 3;
            case EPIC -> 2;
            case RARE -> 1;
            default -> 0;
        };
    }

    /**
     * Get chance text for display
     */
    public static String getChanceText(Rarity rarity) {
        float totalWeight = Rarity.getTotalWeight();
        float chance = (rarity.getWeight() / totalWeight) * 100;

        if (chance < 1) {
            return String.format("%.2f%%", chance);
        } else if (chance < 10) {
            return String.format("%.1f%%", chance);
        } else {
            return String.format("%.0f%%", chance);
        }
    }
}
