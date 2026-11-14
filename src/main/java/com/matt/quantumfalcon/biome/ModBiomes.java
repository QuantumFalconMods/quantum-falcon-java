package com.matt.quantumfalcon.biome;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Biome Registry Keys for Quantum Falcon.
 * Contains all 55 biome resource keys.
 */
public class ModBiomes {

    // ============ Tier 0: Safe/Overworld Extensions (15 biomes) ============
    public static final ResourceKey<Biome> QUANTUM_PLAINS = register("quantum_plains");
    public static final ResourceKey<Biome> QUANTUM_FOREST = register("quantum_forest");
    public static final ResourceKey<Biome> CRYSTAL_CAVES = register("crystal_caves");
    public static final ResourceKey<Biome> QUANTUM_MOUNTAINS = register("quantum_mountains");
    public static final ResourceKey<Biome> LUMINOUS_GROVE = register("luminous_grove");
    public static final ResourceKey<Biome> PRISMATIC_MEADOW = register("prismatic_meadow");
    public static final ResourceKey<Biome> QUANTUM_RIVER = register("quantum_river");
    public static final ResourceKey<Biome> CRYSTAL_BEACH = register("crystal_beach");
    public static final ResourceKey<Biome> AURORA_TUNDRA = register("aurora_tundra");
    public static final ResourceKey<Biome> PHASE_FOREST = register("phase_forest");
    public static final ResourceKey<Biome> QUANTUM_JUNGLE = register("quantum_jungle");
    public static final ResourceKey<Biome> CORAL_QUANTUM = register("coral_quantum");
    public static final ResourceKey<Biome> MUSHROOM_QUANTUM = register("mushroom_quantum");
    public static final ResourceKey<Biome> DESERT_QUANTUM = register("desert_quantum");
    public static final ResourceKey<Biome> SAVANNA_QUANTUM = register("savanna_quantum");

    // ============ Tier 1: Echo/Medium Instability (9 biomes) ============
    public static final ResourceKey<Biome> ECHO_DEPTHS = register("echo_depths");
    public static final ResourceKey<Biome> TEMPORAL_SHORES = register("temporal_shores");
    public static final ResourceKey<Biome> PHASE_SHIFTED_FOREST = register("phase_shifted_forest");
    public static final ResourceKey<Biome> RESONANCE_CAVES = register("resonance_caves");
    public static final ResourceKey<Biome> ECHO_MOUNTAINS = register("echo_mountains");
    public static final ResourceKey<Biome> DISTORTION_PLAINS = register("distortion_plains");
    public static final ResourceKey<Biome> HARMONIC_GROVE = register("harmonic_grove");
    public static final ResourceKey<Biome> FREQUENCY_PEAKS = register("frequency_peaks");
    public static final ResourceKey<Biome> WAVE_COLLAPSE_ZONE = register("wave_collapse_zone");

    // ============ Tier 2: Void/High Instability (15 biomes) ============
    public static final ResourceKey<Biome> VOID_TOUCHED_PLAINS = register("void_touched_plains");
    public static final ResourceKey<Biome> ABYSSAL_DEPTHS = register("abyssal_depths");
    public static final ResourceKey<Biome> REALITY_FRACTURE = register("reality_fracture");
    public static final ResourceKey<Biome> VOID_FOREST = register("void_forest");
    public static final ResourceKey<Biome> ENTROPY_WASTES = register("entropy_wastes");
    public static final ResourceKey<Biome> DARK_MATTER_SEA = register("dark_matter_sea");
    public static final ResourceKey<Biome> GRAVITATIONAL_ANOMALY = register("gravitational_anomaly");
    public static final ResourceKey<Biome> VOID_PEAKS = register("void_peaks");
    public static final ResourceKey<Biome> COLLAPSED_TIMELINE = register("collapsed_timeline");
    public static final ResourceKey<Biome> PARADOX_ZONE = register("paradox_zone");
    public static final ResourceKey<Biome> ANNIHILATION_BORDER = register("annihilation_border");
    public static final ResourceKey<Biome> VOID_RIFT = register("void_rift");
    public static final ResourceKey<Biome> ANTIMATTER_FIELDS = register("antimatter_fields");
    public static final ResourceKey<Biome> SHADOW_REALM = register("shadow_realm");
    public static final ResourceKey<Biome> NEGATIVE_SPACE = register("negative_space");

    // ============ Tier 3: Quantum Endgame/Extreme Instability (16 biomes) ============
    public static final ResourceKey<Biome> SINGULARITY_CORE = register("singularity_core");
    public static final ResourceKey<Biome> QUANTUM_STORM_CENTER = register("quantum_storm_center");
    public static final ResourceKey<Biome> SOVEREIGNS_DOMAIN = register("sovereigns_domain");
    public static final ResourceKey<Biome> EVENT_HORIZON = register("event_horizon");
    public static final ResourceKey<Biome> SPACETIME_TEAR = register("spacetime_tear");
    public static final ResourceKey<Biome> QUANTUM_NEXUS = register("quantum_nexus");
    public static final ResourceKey<Biome> INFINITY_BRIDGE = register("infinity_bridge");
    public static final ResourceKey<Biome> PROBABILITY_CLOUD = register("probability_cloud");
    public static final ResourceKey<Biome> SUPERPOSITION_FIELD = register("superposition_field");
    public static final ResourceKey<Biome> ENTANGLEMENT_WEB = register("entanglement_web");
    public static final ResourceKey<Biome> COSMIC_STRING = register("cosmic_string");
    public static final ResourceKey<Biome> DIMENSIONAL_APEX = register("dimensional_apex");
    public static final ResourceKey<Biome> REALITY_SEED = register("reality_seed");
    public static final ResourceKey<Biome> CREATION_POINT = register("creation_point");
    public static final ResourceKey<Biome> OMEGA_ZONE = register("omega_zone");
    public static final ResourceKey<Biome> ABSOLUTE_ZERO = register("absolute_zero");

    // ============ Helper Methods ============
    
    private static ResourceKey<Biome> register(String name) {
        return ResourceKey.create(Registries.BIOME, 
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, name));
    }

    /**
     * Get instability tier for a biome
     */
    public static int getInstabilityTier(ResourceKey<Biome> biome) {
        if (biome == null) return 1;
        
        String path = biome.location().getPath();
        
        // Tier 0: Safe zones
        if (path.equals("quantum_plains") || path.equals("quantum_forest") || 
            path.equals("crystal_caves") || path.equals("quantum_mountains") ||
            path.equals("luminous_grove") || path.equals("prismatic_meadow") ||
            path.equals("quantum_river") || path.equals("crystal_beach") ||
            path.equals("aurora_tundra") || path.equals("phase_forest") ||
            path.equals("quantum_jungle") || path.equals("coral_quantum") ||
            path.equals("mushroom_quantum") || path.equals("desert_quantum") ||
            path.equals("savanna_quantum")) {
            return 0;
        }
        
        // Tier 1: Echo zones
        if (path.equals("echo_depths") || path.equals("temporal_shores") ||
            path.equals("phase_shifted_forest") || path.equals("resonance_caves") ||
            path.equals("echo_mountains") || path.equals("distortion_plains") ||
            path.equals("harmonic_grove") || path.equals("frequency_peaks") ||
            path.equals("wave_collapse_zone")) {
            return 1;
        }
        
        // Tier 2: Void zones
        if (path.equals("void_touched_plains") || path.equals("abyssal_depths") ||
            path.equals("reality_fracture") || path.equals("void_forest") ||
            path.equals("entropy_wastes") || path.equals("dark_matter_sea") ||
            path.equals("gravitational_anomaly") || path.equals("void_peaks") ||
            path.equals("collapsed_timeline") || path.equals("paradox_zone") ||
            path.equals("annihilation_border") || path.equals("void_rift") ||
            path.equals("antimatter_fields") || path.equals("shadow_realm") ||
            path.equals("negative_space")) {
            return 2;
        }
        
        // Tier 3: Extreme zones (includes all remaining endgame biomes)
        return 3;
    }

    /**
     * Initialize biome registration
     */
    public static void register() {
        // Registration is handled by resource keys
        // This method exists for explicit initialization if needed
    }
}
