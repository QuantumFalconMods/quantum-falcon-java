package com.matt.quantumfalcon.integration;

import com.matt.quantumfalcon.QuantumFalcon;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central manager for all mod integrations.
 * Handles soft dependency detection and conditional loading.
 */
public class ModIntegrations {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModIntegrations.class);

    // Mod IDs for soft dependencies
    public static final String CURIOS_ID = "curios";
    public static final String ARTIFACTS_ID = "artifacts";
    public static final String CREATE_ID = "create";
    public static final String WAYSTONES_ID = "waystones";
    public static final String JEI_ID = "jei";
    public static final String JADE_ID = "jade";

    // Integration status flags
    private static boolean curiosLoaded = false;
    private static boolean artifactsLoaded = false;
    private static boolean createLoaded = false;
    private static boolean waystonesLoaded = false;
    private static boolean jeiLoaded = false;
    private static boolean jadeLoaded = false;

    /**
     * Initialize all integrations. Call during mod setup.
     */
    public static void init() {
        LOGGER.info("Quantum Falcon: Detecting mod integrations...");

        curiosLoaded = ModList.get().isLoaded(CURIOS_ID);
        artifactsLoaded = ModList.get().isLoaded(ARTIFACTS_ID);
        createLoaded = ModList.get().isLoaded(CREATE_ID);
        waystonesLoaded = ModList.get().isLoaded(WAYSTONES_ID);
        jeiLoaded = ModList.get().isLoaded(JEI_ID);
        jadeLoaded = ModList.get().isLoaded(JADE_ID);

        if (curiosLoaded) {
            LOGGER.info("  ✓ Curios detected - Quantum trinkets enabled");
            com.matt.quantumfalcon.integration.curios.CuriosIntegration.init();
        }

        if (artifactsLoaded) {
            LOGGER.info("  ✓ Artifacts detected - Boss artifacts enabled");
            com.matt.quantumfalcon.integration.artifacts.ArtifactsIntegration.init();
        }

        if (createLoaded) {
            LOGGER.info("  ✓ Create detected - Kinetic machines enabled");
            com.matt.quantumfalcon.integration.create.CreateIntegration.init();
        }

        if (waystonesLoaded) {
            LOGGER.info("  ✓ Waystones detected - Quantum rift network enabled");
            com.matt.quantumfalcon.integration.waystones.WaystonesIntegration.init();
        }

        if (jeiLoaded) {
            LOGGER.info("  ✓ JEI detected - Recipe integration enabled");
        }

        if (jadeLoaded) {
            LOGGER.info("  ✓ Jade detected - Block tooltips enabled");
        }

        int integrationCount = (curiosLoaded ? 1 : 0) + (artifactsLoaded ? 1 : 0) + 
                               (createLoaded ? 1 : 0) + (waystonesLoaded ? 1 : 0) +
                               (jeiLoaded ? 1 : 0) + (jadeLoaded ? 1 : 0);
        
        LOGGER.info("Quantum Falcon: Integration detection complete ({} mods found)", integrationCount);
    }

    /**
     * Register integration items/blocks. Call during registry events.
     */
    public static void registerItems(net.neoforged.bus.api.IEventBus modBus) {
        if (curiosLoaded) {
            com.matt.quantumfalcon.integration.curios.CuriosIntegration.registerItems(modBus);
        }
        if (artifactsLoaded) {
            com.matt.quantumfalcon.integration.artifacts.ArtifactsIntegration.registerItems(modBus);
        }
        if (createLoaded) {
            com.matt.quantumfalcon.integration.create.CreateIntegration.registerBlocks(modBus);
        }
        if (waystonesLoaded) {
            com.matt.quantumfalcon.integration.waystones.WaystonesIntegration.registerBlocks(modBus);
        }
    }

    /**
     * guard: ids an optional integration would register that already exist natively (a duplicate registry key, which
     * in production silently replaces one object). Only meaningful while the integration mod is absent, as in the qftest run.
     */
    public static java.util.List<String> duplicateIds() {
        java.util.List<String> out = new java.util.ArrayList<>();
        java.util.List<net.neoforged.neoforge.registries.DeferredRegister<?>> regs = java.util.List.of(
            com.matt.quantumfalcon.integration.curios.CuriosIntegration.CURIO_ITEMS,
            com.matt.quantumfalcon.integration.create.CreateIntegration.CREATE_ITEMS,
            com.matt.quantumfalcon.integration.create.CreateIntegration.CREATE_BLOCKS,
            com.matt.quantumfalcon.integration.waystones.WaystonesIntegration.WAYSTONE_ITEMS,
            com.matt.quantumfalcon.integration.waystones.WaystonesIntegration.WAYSTONE_BLOCKS);
        for (var reg : regs) {
            boolean loaded = reg == com.matt.quantumfalcon.integration.curios.CuriosIntegration.CURIO_ITEMS ? curiosLoaded
                : reg == com.matt.quantumfalcon.integration.waystones.WaystonesIntegration.WAYSTONE_ITEMS || reg == com.matt.quantumfalcon.integration.waystones.WaystonesIntegration.WAYSTONE_BLOCKS ? waystonesLoaded
                : createLoaded;
            if (loaded) continue;
            for (var h : reg.getEntries()) {
                var id = h.getId();
                if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id) || net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id)) out.add(id.toString());
            }
        }
        return out;
    }

    // ============ Status Checks ============

    public static boolean isCuriosLoaded() {
        return curiosLoaded;
    }

    public static boolean isArtifactsLoaded() {
        return artifactsLoaded;
    }

    public static boolean isCreateLoaded() {
        return createLoaded;
    }

    public static boolean isWaystonesLoaded() {
        return waystonesLoaded;
    }

    public static boolean isJeiLoaded() {
        return jeiLoaded;
    }

    public static boolean isJadeLoaded() {
        return jadeLoaded;
    }

    /**
     * Check if any integration is active
     */
    public static boolean hasAnyIntegration() {
        return curiosLoaded || artifactsLoaded || createLoaded || waystonesLoaded || jeiLoaded || jadeLoaded;
    }
}
