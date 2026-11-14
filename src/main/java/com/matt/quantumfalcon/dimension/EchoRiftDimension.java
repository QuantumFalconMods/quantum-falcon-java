package com.matt.quantumfalcon.dimension;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.biome.ModBiomes;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.List;
import java.util.OptionalLong;

/**
 * Echo Rift Dimension Registration for Quantum Falcon.
 * Handles dimension type, level stem, and biome source configuration.
 */
public class EchoRiftDimension {

    // ============ Resource Keys ============
    
    public static final ResourceKey<Level> ECHO_RIFT_LEVEL_KEY = ResourceKey.create(
        Registries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "echo_rift")
    );
    
    public static final ResourceKey<DimensionType> ECHO_RIFT_TYPE_KEY = ResourceKey.create(
        Registries.DIMENSION_TYPE,
        ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "echo_rift_type")
    );
    
    public static final ResourceKey<LevelStem> ECHO_RIFT_STEM_KEY = ResourceKey.create(
        Registries.LEVEL_STEM,
        ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "echo_rift")
    );
    
    public static final ResourceKey<NoiseGeneratorSettings> ECHO_RIFT_NOISE_KEY = ResourceKey.create(
        Registries.NOISE_SETTINGS,
        ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "echo_rift_noise")
    );

    // ============ Dimension Type Bootstrap ============
    
    /**
     * Bootstrap the Echo Rift dimension type
     */
    public static void bootstrapDimensionType(BootstrapContext<DimensionType> context) {
        context.register(ECHO_RIFT_TYPE_KEY, new DimensionType(
            OptionalLong.of(18000L),  // Fixed time (perpetual twilight)
            true,                      // Has skylight
            false,                     // Has ceiling
            false,                     // Ultrawarm (like nether)
            true,                      // Natural (for spawning)
            1.0,                       // Coordinate scale
            true,                      // Bed works
            false,                     // Respawn anchor works
            -64,                       // Min Y
            384,                       // Height
            384,                       // Logical height
            BlockTags.INFINIBURN_OVERWORLD,  // Infiniburn
            ResourceLocation.fromNamespaceAndPath("quantumfalcon", "echo_rift"),  // own sky effects (client EchoRiftSky)
            0.1f,                      // Ambient light
            new DimensionType.MonsterSettings(
                false,                 // Piglin safe
                false,                 // Has raids
                UniformInt.of(0, 7),   // Monster spawn light level
                7                      // Monster spawn block light limit
            )
        ));
    }

    // ============ Level Stem Bootstrap ============
    
    /**
     * Bootstrap the Echo Rift level stem (dimension + generator)
     */
    public static void bootstrapLevelStem(BootstrapContext<LevelStem> context) {
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        
        // Create multi-noise biome source with quantum biomes
        BiomeSource biomeSource = createEchoRiftBiomeSource(biomes);
        
        // Create chunk generator
        NoiseBasedChunkGenerator chunkGenerator = new NoiseBasedChunkGenerator(
            biomeSource,
            noiseSettings.getOrThrow(ECHO_RIFT_NOISE_KEY)
        );
        
        // Register the level stem
        context.register(ECHO_RIFT_STEM_KEY, new LevelStem(
            dimensionTypes.getOrThrow(ECHO_RIFT_TYPE_KEY),
            chunkGenerator
        ));
    }

    // ============ Biome Source Creation ============
    
    /**
     * Create the multi-noise biome source for Echo Rift
     * Uses instability-based biome distribution
     */
    private static BiomeSource createEchoRiftBiomeSource(HolderGetter<Biome> biomes) {
        // Climate parameters: temperature, humidity, continentalness, erosion, depth, weirdness
        List<com.mojang.datafixers.util.Pair<Climate.ParameterPoint, net.minecraft.core.Holder<Biome>>> biomeList = List.of(
            // Low Instability Biomes (safe zones)
            createBiomeEntry(biomes, ModBiomes.QUANTUM_PLAINS,
                Climate.parameters(-0.5f, -0.5f, -0.5f, -0.5f, 0.0f, -0.5f, 0.0f)),
            createBiomeEntry(biomes, ModBiomes.QUANTUM_FOREST,
                Climate.parameters(-0.3f, 0.0f, -0.3f, -0.3f, 0.0f, -0.3f, 0.0f)),
            createBiomeEntry(biomes, ModBiomes.CRYSTAL_CAVES,
                Climate.parameters(0.0f, 0.3f, -0.2f, 0.0f, 0.5f, 0.0f, 0.0f)),
            
            // Medium Instability Biomes (echo zones)
            createBiomeEntry(biomes, ModBiomes.ECHO_DEPTHS,
                Climate.parameters(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)),
            createBiomeEntry(biomes, ModBiomes.TEMPORAL_SHORES,
                Climate.parameters(0.3f, 0.5f, 0.3f, 0.3f, 0.0f, 0.3f, 0.0f)),
            createBiomeEntry(biomes, ModBiomes.PHASE_SHIFTED_FOREST,
                Climate.parameters(-0.2f, 0.2f, 0.2f, 0.2f, 0.0f, 0.2f, 0.0f)),
            
            // High Instability Biomes (void zones)
            createBiomeEntry(biomes, ModBiomes.VOID_TOUCHED_PLAINS,
                Climate.parameters(0.5f, -0.3f, 0.5f, 0.5f, 0.0f, 0.5f, 0.0f)),
            createBiomeEntry(biomes, ModBiomes.ABYSSAL_DEPTHS,
                Climate.parameters(0.7f, 0.0f, 0.7f, 0.7f, 0.5f, 0.7f, 0.0f)),
            createBiomeEntry(biomes, ModBiomes.REALITY_FRACTURE,
                Climate.parameters(0.8f, 0.3f, 0.8f, 0.8f, 0.0f, 0.8f, 0.0f)),
            
            // Extreme Instability Biomes (endgame)
            createBiomeEntry(biomes, ModBiomes.SINGULARITY_CORE,
                Climate.parameters(1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f)),
            createBiomeEntry(biomes, ModBiomes.QUANTUM_STORM_CENTER,
                Climate.parameters(0.9f, 0.5f, 0.9f, 0.9f, 0.0f, 0.9f, 0.0f)),
            createBiomeEntry(biomes, ModBiomes.SOVEREIGNS_DOMAIN,
                Climate.parameters(1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f))
        );
        
        return MultiNoiseBiomeSource.createFromList(
            new Climate.ParameterList<>(biomeList)
        );
    }
    
    /**
     * Helper to create biome entries for the parameter list
     */
    private static com.mojang.datafixers.util.Pair<Climate.ParameterPoint, net.minecraft.core.Holder<Biome>> 
            createBiomeEntry(HolderGetter<Biome> biomes, ResourceKey<Biome> biomeKey, Climate.ParameterPoint params) {
        return com.mojang.datafixers.util.Pair.of(params, biomes.getOrThrow(biomeKey));
    }

    // ============ Utility Methods ============
    
    /**
     * Check if a level is the Echo Rift dimension
     */
    public static boolean isEchoRift(Level level) {
        return level.dimension().equals(ECHO_RIFT_LEVEL_KEY);
    }
    
    /**
     * Get the instability tier for a position in the Echo Rift
     * Based on distance from center and biome
     */
    public static int getInstabilityTier(Level level, net.minecraft.core.BlockPos pos) {
        if (!isEchoRift(level)) {
            return 0;
        }
        
        // Base instability from distance to world center
        double distance = Math.sqrt(pos.getX() * pos.getX() + pos.getZ() * pos.getZ());
        int distanceTier = (int) Math.min(3, distance / 1000.0);
        
        // Biome modifier
        var biome = level.getBiome(pos);
        int biomeTier = getBiomeInstabilityTier(biome.unwrapKey().orElse(null));
        
        // Return highest tier
        return Math.max(distanceTier, biomeTier);
    }
    
    /**
     * Get instability tier from biome
     */
    private static int getBiomeInstabilityTier(ResourceKey<Biome> biomeKey) {
        if (biomeKey == null) return 1;
        
        String path = biomeKey.location().getPath();
        
        // Tier 0: Safe zones
        if (path.contains("quantum_plains") || path.contains("quantum_forest") || 
            path.contains("crystal_caves")) {
            return 0;
        }
        
        // Tier 1: Echo zones
        if (path.contains("echo") || path.contains("temporal") || 
            path.contains("phase_shifted")) {
            return 1;
        }
        
        // Tier 2: Void zones
        if (path.contains("void") || path.contains("abyssal") || 
            path.contains("reality_fracture")) {
            return 2;
        }
        
        // Tier 3: Extreme zones
        if (path.contains("singularity") || path.contains("storm_center") || 
            path.contains("sovereign")) {
            return 3;
        }
        
        return 1; // Default to tier 1
    }
}
