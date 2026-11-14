package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.blockentity.EntangledOreBlockEntity;
import com.matt.quantumfalcon.blockentity.FalconEggBlockEntity;
import com.matt.quantumfalcon.blockentity.QuantumNestBlockEntity;
import com.matt.quantumfalcon.blockentity.QuantumIncubatorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block Entity Registry for Quantum Falcon Mod
 * 
 * IMPLEMENTED BLOCK ENTITIES:
 * - ENTANGLED_ORE: Quantum-linked ore nodes (qCraft inspired)
 * - QUANTUM_NEST: Falcon nesting/resting block
 * - QUANTUM_INCUBATOR: Egg incubation with progress
 * 
 * PLANNED BLOCK ENTITIES:
 * - ENTANGLEMENT_CHEST: Linked storage across dimensions
 * - PEDESTAL: Display and power focus
 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuantumFalcon.MOD_ID);

    // ========================================
    // ENTANGLED ORE - Quantum-linked blocks
    // ========================================
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EntangledOreBlockEntity>> ENTANGLED_ORE = 
        BLOCK_ENTITIES.register("entangled_ore", 
            () -> BlockEntityType.Builder.of(
                (pos, state) -> new EntangledOreBlockEntity(ModBlockEntities.ENTANGLED_ORE.get(), pos, state),
                ModBlocks.ENTANGLED_ORE.get()
            ).build(null));

    // ========================================
    // QUANTUM NEST - Falcon breeding/resting
    // ========================================
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuantumNestBlockEntity>> QUANTUM_NEST = 
        BLOCK_ENTITIES.register("quantum_nest", 
            () -> BlockEntityType.Builder.of(
                (pos, state) -> new QuantumNestBlockEntity(ModBlockEntities.QUANTUM_NEST.get(), pos, state),
                ModBlocks.QUANTUM_NEST.get()
            ).build(null));

    // ========================================
    // QUANTUM INCUBATOR - Egg hatching
    // ========================================
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuantumIncubatorBlockEntity>> QUANTUM_INCUBATOR = 
        BLOCK_ENTITIES.register("quantum_incubator", 
            () -> BlockEntityType.Builder.of(
                (pos, state) -> new QuantumIncubatorBlockEntity(ModBlockEntities.QUANTUM_INCUBATOR.get(), pos, state),
                ModBlocks.QUANTUM_INCUBATOR.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.matt.quantumfalcon.block.entity.EntanglementChestBlockEntity>> ENTANGLEMENT_CHEST =
        BLOCK_ENTITIES.register("entanglement_chest",
            () -> BlockEntityType.Builder.of(com.matt.quantumfalcon.block.entity.EntanglementChestBlockEntity::new,
                ModFamilyBlocks.ENTANGLEMENT_CHEST.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FalconEggBlockEntity>> FALCON_EGG = 
        BLOCK_ENTITIES.register("falcon_egg", 
            () -> BlockEntityType.Builder.of(
                (pos, state) -> new FalconEggBlockEntity(ModBlockEntities.FALCON_EGG.get(), pos, state),
                ModBlocks.FALCON_EGG_BLOCK.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.matt.quantumfalcon.blockentity.FalconNestBlockEntity>> FALCON_NEST =
        BLOCK_ENTITIES.register("falcon_nest", () -> BlockEntityType.Builder.of(com.matt.quantumfalcon.blockentity.FalconNestBlockEntity::new,
            ModBlocks.TWIG_NEST.get(), ModBlocks.CRYSTAL_NEST.get(), ModBlocks.VOIDFORGED_NEST.get(), ModBlocks.QUANTUM_AERIE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
        QuantumFalcon.LOGGER.info("⚡ Registered {} block entity types", BLOCK_ENTITIES.getEntries().size());
    }
}
