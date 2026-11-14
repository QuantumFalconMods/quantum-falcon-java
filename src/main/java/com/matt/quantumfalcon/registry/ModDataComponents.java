package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

/**
 * Data Component Registry for Quantum Falcon Mod
 * 
 * Data components are used to store custom data on items (replaces old NBT system).
 * These are used for falcon eggs, entangled items, etc.
 */
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = 
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, QuantumFalcon.MOD_ID);

    // ========================================
    // ENTANGLEMENT COMPONENTS
    // ========================================
    /**
     * UUID linking entangled items (chests, bands, etc.)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> ENTANGLEMENT_ID = 
        DATA_COMPONENTS.register("entanglement_id", 
            () -> DataComponentType.<UUID>builder()
                .persistent(UUIDUtil.CODEC)
                .networkSynchronized(UUIDUtil.STREAM_CODEC)
                .build());

    // ========================================
    // FALCON EGG COMPONENTS
    // ========================================
    /**
     * Bond level between falcon and player (0-100)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BOND_LEVEL = 
        DATA_COMPONENTS.register("bond_level", 
            () -> DataComponentType.<Integer>builder()
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.INT)
                .build());

    /**
     * Custom name for a falcon
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> FALCON_NAME = 
        DATA_COMPONENTS.register("falcon_name", 
            () -> DataComponentType.<String>builder()
                .persistent(Codec.STRING)
                .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                .build());

    /**
     * Generation number (for breeding lineage)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GENERATION = 
        DATA_COMPONENTS.register("generation", 
            () -> DataComponentType.<Integer>builder()
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.INT)
                .build());

    /**
     * Parent 1 UUID (for breeding records)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> PARENT_1_ID = 
        DATA_COMPONENTS.register("parent_1_id", 
            () -> DataComponentType.<UUID>builder()
                .persistent(UUIDUtil.CODEC)
                .networkSynchronized(UUIDUtil.STREAM_CODEC)
                .build());

    /**
     * Parent 2 UUID (for breeding records)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> PARENT_2_ID = 
        DATA_COMPONENTS.register("parent_2_id", 
            () -> DataComponentType.<UUID>builder()
                .persistent(UUIDUtil.CODEC)
                .networkSynchronized(UUIDUtil.STREAM_CODEC)
                .build());

    /**
     * Incubation progress percentage (0-100)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> INCUBATION_PROGRESS = 
        DATA_COMPONENTS.register("incubation_progress", 
            () -> DataComponentType.<Integer>builder()
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.INT)
                .build());

    /**
     * Whether legendary outcome is guaranteed (from special breeding)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LEGENDARY_GUARANTEED = 
        DATA_COMPONENTS.register("legendary_guaranteed", 
            () -> DataComponentType.<Boolean>builder()
                .persistent(Codec.BOOL)
                .networkSynchronized(ByteBufCodecs.BOOL)
                .build());

    // ========================================
    // FALCON VARIANT/TRAIT COMPONENTS - TODO
    // ========================================
    // These require FalconVariant and FalconTrait enums to be implemented
    /*
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FalconVariant>> FALCON_VARIANT = 
        DATA_COMPONENTS.register("falcon_variant", 
            () -> DataComponentType.<FalconVariant>builder()
                .persistent(FalconVariant.CODEC)
                .networkSynchronized(FalconVariant.STREAM_CODEC)
                .build());
    */

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
        QuantumFalcon.LOGGER.info("⚡ Registered {} data component types", DATA_COMPONENTS.getEntries().size());
    }
}
