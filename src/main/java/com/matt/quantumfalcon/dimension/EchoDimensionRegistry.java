package com.matt.quantumfalcon.dimension;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Echo Dimension instances and portal mappings.
 * Supports multiple instability tiers (0-5) with unique dimension variants.
 */
public class EchoDimensionRegistry {
    // Portal to dimension mappings
    private static final Map<UUID, ResourceKey<Level>> PORTAL_DIMENSIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Integer> DIMENSION_INSTABILITY = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Long> DIMENSION_CREATION_TIME = new ConcurrentHashMap<>();

    // Dimension keys for each instability tier
    public static final ResourceKey<Level> ECHO_DIM_0 = createDimensionKey("echo_rift_0");
    public static final ResourceKey<Level> ECHO_DIM_1 = createDimensionKey("echo_rift_1");
    public static final ResourceKey<Level> ECHO_DIM_2 = createDimensionKey("echo_rift_2");
    public static final ResourceKey<Level> ECHO_DIM_3 = createDimensionKey("echo_rift_3");
    public static final ResourceKey<Level> ECHO_DIM_4 = createDimensionKey("echo_rift_4");
    public static final ResourceKey<Level> ECHO_DIM_5 = createDimensionKey("echo_rift_5");

    // Main Echo Depths dimension
    public static final ResourceKey<Level> ECHO_DEPTHS = createDimensionKey("echo_depths");

    private static ResourceKey<Level> createDimensionKey(String name) {
        return ResourceKey.create(Registries.DIMENSION, 
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, name));
    }

    /**
     * Get or create an echo dimension for a specific portal.
     */
    public static ResourceKey<Level> getOrCreateEchoDimension(UUID portalId, int instability) {
        if (PORTAL_DIMENSIONS.containsKey(portalId)) {
            return PORTAL_DIMENSIONS.get(portalId);
        }
        
        ResourceKey<Level> dimensionKey = getDimensionKeyForInstability(instability);
        PORTAL_DIMENSIONS.put(portalId, dimensionKey);
        DIMENSION_INSTABILITY.put(dimensionKey, instability);
        DIMENSION_CREATION_TIME.put(dimensionKey, System.currentTimeMillis());
        
        QuantumFalcon.LOGGER.info("Created echo dimension {} for portal {} with instability {}", 
            dimensionKey.location(), portalId, instability);
        return dimensionKey;
    }

    /**
     * Get dimension key based on instability level.
     */
    private static ResourceKey<Level> getDimensionKeyForInstability(int instability) {
        return switch (Math.min(5, Math.max(0, instability))) {
            case 0 -> ECHO_DIM_0;
            case 1 -> ECHO_DIM_1;
            case 2 -> ECHO_DIM_2;
            case 3 -> ECHO_DIM_3;
            case 4 -> ECHO_DIM_4;
            case 5 -> ECHO_DIM_5;
            default -> ECHO_DIM_0;
        };
    }

    /**
     * Get instability level for a dimension.
     */
    public static int getInstabilityLevel(ResourceKey<Level> dimension) {
        return DIMENSION_INSTABILITY.getOrDefault(dimension, 0);
    }

    /**
     * Get instability level for a portal.
     */
    public static int getInstabilityForPortal(UUID portalId) {
        ResourceKey<Level> dimension = PORTAL_DIMENSIONS.get(portalId);
        return dimension == null ? 0 : getInstabilityLevel(dimension);
    }

    /**
     * Check if a dimension key is an echo dimension.
     */
    public static boolean isEchoDimension(ResourceKey<Level> dimension) {
        String path = dimension.location().getPath();
        return dimension.location().getNamespace().equals(QuantumFalcon.MOD_ID) && (path.startsWith("echo_rift") || path.equals("echo_depths"));
    }

    /**
     * Check if level is an echo dimension.
     */
    public static boolean isInEchoDimension(Level level) {
        return isEchoDimension(level.dimension());
    }

    /**
     * Get echo dimension server level.
     */
    public static ServerLevel getEchoDimension(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        return server.getLevel(dimensionKey);
    }

    /**
     * Get the main Echo Depths dimension.
     */
    public static ServerLevel getEchoDepths(MinecraftServer server) {
        return server.getLevel(ECHO_DEPTHS);
    }

    /**
     * Remove portal mapping.
     */
    public static void removePortal(UUID portalId) {
        ResourceKey<Level> dimension = PORTAL_DIMENSIONS.remove(portalId);
        if (dimension != null) {
            QuantumFalcon.LOGGER.info("Removed portal {} mapping to dimension {}", 
                portalId, dimension.location());
        }
    }

    /**
     * Get dimension age in milliseconds.
     */
    public static long getDimensionAge(ResourceKey<Level> dimension) {
        Long creationTime = DIMENSION_CREATION_TIME.get(dimension);
        return creationTime == null ? 0L : System.currentTimeMillis() - creationTime;
    }

    /**
     * Cleanup old dimensions.
     */
    public static void cleanupOldDimensions(long maxAgeMillis) {
        DIMENSION_CREATION_TIME.entrySet().removeIf(entry -> {
            long age = System.currentTimeMillis() - entry.getValue();
            if (age > maxAgeMillis) {
                ResourceKey<Level> dimension = entry.getKey();
                DIMENSION_INSTABILITY.remove(dimension);
                PORTAL_DIMENSIONS.entrySet().removeIf(e -> e.getValue().equals(dimension));
                QuantumFalcon.LOGGER.info("Cleaned up old echo dimension {}", dimension.location());
                return true;
            }
            return false;
        });
    }

    /**
     * Get all active echo dimensions.
     */
    public static Map<ResourceKey<Level>, Integer> getActiveEchoDimensions() {
        return Map.copyOf(DIMENSION_INSTABILITY);
    }

    /**
     * Print debug information.
     */
    public static void printDebugInfo() {
        QuantumFalcon.LOGGER.info("=== Echo Dimension Registry Debug ===");
        QuantumFalcon.LOGGER.info("Active portals: {}", PORTAL_DIMENSIONS.size());
        QuantumFalcon.LOGGER.info("Active dimensions: {}", DIMENSION_INSTABILITY.size());
        for (Map.Entry<ResourceKey<Level>, Integer> entry : DIMENSION_INSTABILITY.entrySet()) {
            long age = getDimensionAge(entry.getKey());
            QuantumFalcon.LOGGER.info("  - {} (Instability: {}, Age: {}s)", 
                entry.getKey().location(), entry.getValue(), age / 1000L);
        }
    }

    /**
     * Register dimension system.
     */
    public static void register(IEventBus modEventBus) {
        QuantumFalcon.LOGGER.info("⚡ Echo Dimension Registry initialized");
    }
}
