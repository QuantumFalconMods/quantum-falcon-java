package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Echo Rift dimension registration (production-ready).
 * Dimension is used by CustomPortalAPI (ModPortals) for quantumfalcon:echo_rift.
 * Ensure worldgen data is enabled: dimension type (echo_rift_type.json), noise (echo_rift_noise),
 * and dimension (echo_rift.json) must be in data/quantumfalcon/worldgen/ (not _worldgen_disabled).
 */
public final class ModDimensions {

    public static final ResourceKey<Level> ECHO_RIFT = ResourceKey.create(
        Registries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "echo_rift")
    );

    public static void init() {
        QuantumFalcon.LOGGER.info("⚡ Echo Rift dimension key: {}", ECHO_RIFT.location());
    }
}
