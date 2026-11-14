package com.matt.quantumfalcon.leveling.traits;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.traits.impl.*;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Central manager for trait registration, lookup, and progression queries.
 * Thread-safe for server use; call registerTraits() once during mod init.
 */
public class TraitManager {

    private static final Map<ResourceLocation, Trait> TRAITS = new HashMap<>();
    private static final Map<Integer, List<Trait>> TRAITS_BY_LEVEL = new HashMap<>();
    private static final Map<ItemCategory, List<Trait>> TRAITS_BY_CATEGORY = new HashMap<>();
    private static final List<Trait> RARITY_TRAITS = new ArrayList<>();
    private static boolean initialized = false;

    /**
     * Register all built-in traits. Call this during mod initialization.
     */
    public static void registerTraits() {
        if (initialized) {
            QuantumFalcon.LOGGER.warn("TraitManager.registerTraits() called multiple times - ignoring");
            return;
        }
        initialized = true;

        // ========== WEAPON TRAITS (combat) ==========

        // Level 10 - Lifesteal
        register(new LifestealTrait(10, 0.05f));

        // Level 25 - Critical Boost
        register(new CriticalBoostTrait(25, 0.25f));

        // Level 40 - Fire Aspect
        register(new FireAspectTrait(40, 4));

        // Level 50 - Void Strike (armor penetration)
        register(new VoidStrikeTrait(50, 0.15f));

        // Level 65 - Chain Lightning
        register(new ChainLightningTrait(65, 3, 0.5f));

        // Level 75 - Execute
        register(new ExecuteTrait(75, 0.20f, 0.50f));

        // Level 90 - Berserk
        register(new BerserkTrait(90, 0.30f, 0.25f));

        // Level 100 - Quantum Edge (instant kill chance)
        register(new QuantumEdgeTrait(100, 0.05f));

        // ========== ARMOR TRAITS (defense) ==========

        // Level 10 - Thorns
        register(new ThornsTrait(10, 0.10f));

        // Level 25 - Regeneration
        register(new RegenerationTrait(25, 0.5f, 100));

        // Level 40 - Fire Resist
        register(new FireResistTrait(40, 0.25f));

        // Level 50 - Hardened (damage reduction)
        register(new HardenedTrait(50, 0.10f));

        // Level 65 - Magic Resist
        register(new MagicResistTrait(65, 0.20f));

        // Level 75 - Last Stand
        register(new LastStandTrait(75, 0.25f, 0.50f));

        // Level 90 - Reflect
        register(new ReflectTrait(90, 0.15f));

        // Level 100 - Quantum Shield
        register(new QuantumShieldTrait(100, 0.10f));

        // ========== TOOL TRAITS ==========

        // Level 10 - Magnetic (auto-pickup)
        register(new MagneticTrait(10));

        // Level 25 - Efficient
        register(new EfficientTrait(25, 0.15f));

        // Level 50 - Fortune Boost
        register(new FortuneBoostTrait(50, 1));

        // Level 75 - Vein Miner
        register(new VeinMinerTrait(75, 8));

        // Level 100 - Quantum Harvest
        register(new QuantumHarvestTrait(100, 0.10f));

        // ========== RARITY TRAITS (granted by rarity) ==========

        // Durable - increased durability
        register(new DurableTrait());

        // Sharp - slight damage increase
        register(new SharpTrait());

        // Reinforced - slight armor increase
        register(new ReinforcedTrait());

        // Blessed - increased XP gain
        register(new BlessedTrait());

        // Soulbound - keep on death
        register(new SoulboundTrait());

        // Quantum Infused - special effects
        register(new QuantumInfusedTrait());

        // Post-registration: Sort lists for consistent UI
        sortAllLists();

        QuantumFalcon.LOGGER.info("Registered {} traits ({} level-based, {} rarity-based)",
            TRAITS.size(), TRAITS.size() - RARITY_TRAITS.size(), RARITY_TRAITS.size());
    }

    /**
     * Register a trait
     */
    public static void register(Trait trait) {
        if (trait == null) {
            QuantumFalcon.LOGGER.error("Attempted to register null trait");
            return;
        }

        ResourceLocation id = trait.getId();
        if (TRAITS.containsKey(id)) {
            QuantumFalcon.LOGGER.warn("Duplicate trait registration ignored: {}", id);
            return;
        }

        TRAITS.put(id, trait);

        if (trait.isRarityTrait()) {
            RARITY_TRAITS.add(trait);
        } else {
            TRAITS_BY_LEVEL.computeIfAbsent(trait.getUnlockLevel(), k -> new ArrayList<>())
                .add(trait);
        }

        // Index by category
        for (ItemCategory cat : trait.getValidCategories()) {
            TRAITS_BY_CATEGORY.computeIfAbsent(cat, k -> new ArrayList<>())
                .add(trait);
        }
    }

    private static void sortAllLists() {
        // Sort level traits by unlock level
        TRAITS_BY_LEVEL.values().forEach(list -> list.sort(Comparator.comparingInt(Trait::getUnlockLevel)));
        // Sort category lists by name
        TRAITS_BY_CATEGORY.values().forEach(list -> list.sort(Comparator.comparing(Trait::getName)));
        // Sort rarity by name
        RARITY_TRAITS.sort(Comparator.comparing(Trait::getName));
    }

    /**
     * Get a trait by ID
     */
    @Nullable
    public static Trait getTrait(ResourceLocation id) {
        return TRAITS.get(id);
    }

    /**
     * Get a trait by string ID
     */
    @Nullable
    public static Trait getTrait(String id) {
        if (id == null || id.isEmpty()) return null;
        try {
            return TRAITS.get(ResourceLocation.parse(id));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get all traits that unlock at a specific level for a category
     */
    public static List<Trait> getTraitsForLevel(int level, ItemCategory category) {
        List<Trait> result = new ArrayList<>();

        List<Trait> levelTraits = TRAITS_BY_LEVEL.get(level);
        if (levelTraits != null) {
            for (Trait trait : levelTraits) {
                if (trait.isValidFor(category)) {
                    result.add(trait);
                }
            }
        }

        return result;
    }

    /**
     * Get all traits for a category
     */
    public static List<Trait> getTraitsForCategory(ItemCategory category) {
        return TRAITS_BY_CATEGORY.getOrDefault(category, Collections.emptyList());
    }

    /**
     * Get all traits that unlock at or before a specific level
     */
    public static List<Trait> getTraitsUpToLevel(int level, ItemCategory category) {
        List<Trait> result = new ArrayList<>();

        for (Map.Entry<Integer, List<Trait>> entry : TRAITS_BY_LEVEL.entrySet()) {
            if (entry.getKey() <= level) {
                for (Trait trait : entry.getValue()) {
                    if (trait.isValidFor(category)) {
                        result.add(trait);
                    }
                }
            }
        }

        // Sort by unlock level
        result.sort(Comparator.comparingInt(Trait::getUnlockLevel));
        return result;
    }

    /**
     * Get all registered traits
     */
    public static Collection<Trait> getAllTraits() {
        return Collections.unmodifiableCollection(TRAITS.values());
    }

    /**
     * Get all level-unlock traits (not rarity traits)
     */
    public static List<Trait> getLevelTraits() {
        List<Trait> result = new ArrayList<>();
        for (Trait trait : TRAITS.values()) {
            if (!trait.isRarityTrait()) {
                result.add(trait);
            }
        }
        result.sort(Comparator.comparingInt(Trait::getUnlockLevel));
        return result;
    }

    /**
     * Get all rarity-granted traits
     */
    public static List<Trait> getRarityTraits() {
        return Collections.unmodifiableList(RARITY_TRAITS);
    }

    /**
     * Get random rarity trait
     */
    @Nullable
    public static Trait getRandomRarityTrait(Random rand) {
        if (RARITY_TRAITS.isEmpty()) return null;
        return RARITY_TRAITS.get(rand.nextInt(RARITY_TRAITS.size()));
    }

    /**
     * Check if a trait ID exists
     */
    public static boolean exists(ResourceLocation id) {
        return TRAITS.containsKey(id);
    }

    /**
     * Get the unlock level for a trait
     */
    public static int getUnlockLevel(ResourceLocation id) {
        Trait trait = TRAITS.get(id);
        return trait != null ? trait.getUnlockLevel() : -1;
    }

    /**
     * Get all milestone levels (levels where traits unlock)
     */
    public static Set<Integer> getMilestoneLevels() {
        return Collections.unmodifiableSet(TRAITS_BY_LEVEL.keySet());
    }

    /**
     * Get next milestone level after the given level
     */
    public static int getNextMilestone(int currentLevel, ItemCategory category) {
        int next = Integer.MAX_VALUE;

        for (Integer level : TRAITS_BY_LEVEL.keySet()) {
            if (level > currentLevel && level < next) {
                // Check if any traits for this level apply to this category
                List<Trait> traits = TRAITS_BY_LEVEL.get(level);
                for (Trait t : traits) {
                    if (t.isValidFor(category)) {
                        next = level;
                        break;
                    }
                }
            }
        }

        return next == Integer.MAX_VALUE ? -1 : next;
    }
}
