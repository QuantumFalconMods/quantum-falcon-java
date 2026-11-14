package com.matt.quantumfalcon.worldgen.structure;

import com.matt.quantumfalcon.QuantumFalcon;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for Quantum Falcon structure types and piece types.
 *
 * Structure types (codec registration only — instances defined via JSON datapacks):
 *   boss_arena        — 4 boss arenas (Guardian / Void Horror / Entangled Twins / Sovereign)
 *   utility_structure — 11 world-gen structures across two tiers:
 *       Tier 1 Surface: Ancient Nest, Quantum Well, Rift Shrine, Temporal Nexus,
 *                       Echo Rift Gate, Breeding Sanctum
 *       Tier 2 Deep:    Echo Chamber, Crystal Cavern, Data Archive,
 *                       Quantum Observatory, Void Citadel
 *
 * JSON datapack files (required per structure):
 *   data/quantumfalcon/worldgen/structure/[name].json
 *   data/quantumfalcon/worldgen/structure_set/[name].json
 *   data/quantumfalcon/tags/worldgen/biome/has_structure/[name].json
 */
public class ModStructures {

    // ─── Structure Type DeferredRegister ────────────────────────────────────
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, QuantumFalcon.MOD_ID);

    // ─── Structure Piece Type DeferredRegister ───────────────────────────────
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_PIECE, QuantumFalcon.MOD_ID);

    // ─── Placement types ─────────────────────────────────────────────────────
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>> PLACEMENT_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, QuantumFalcon.MOD_ID);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>, net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<ArenaAvoidingPlacement>> ARENA_AVOIDING =
        PLACEMENT_TYPES.register("arena_avoiding", () -> () -> ArenaAvoidingPlacement.CODEC);

    // ─── Boss Arena (all 4 bosses) ───────────────────────────────────────────
    public static final DeferredHolder<StructureType<?>, StructureType<BossArenaStructure>> BOSS_ARENA_TYPE =
        STRUCTURE_TYPES.register("boss_arena", () -> explicitStructureTypeTyping(BossArenaStructure.CODEC));

    // ─── Echo dungeons (surface-checked jigsaw) ──────────────────────────────
    public static final DeferredHolder<StructureType<?>, StructureType<EchoDungeonStructure>> ECHO_DUNGEON_TYPE =
        STRUCTURE_TYPES.register("echo_dungeon", () -> explicitStructureTypeTyping(EchoDungeonStructure.CODEC));

    // ─── A10 Frozen Hours (authored, unrotated single template) ──────────────
    public static final DeferredHolder<StructureType<?>, StructureType<FrozenHourStructure>> FROZEN_HOUR_TYPE =
        STRUCTURE_TYPES.register("frozen_hour", () -> explicitStructureTypeTyping(FrozenHourStructure.CODEC));
    public static final DeferredHolder<StructurePieceType, StructurePieceType> FROZEN_HOUR_PIECE =
        PIECE_TYPES.register("frozen_hour_piece", () -> (StructurePieceType) FrozenHourStructure.Piece::new);

    // ─── Quantum Utility Structures (5 variants) ─────────────────────────────
    public static final DeferredHolder<StructureType<?>, StructureType<QuantumUtilityStructure>> UTILITY_STRUCTURE_TYPE =
        STRUCTURE_TYPES.register("utility_structure", () -> explicitStructureTypeTyping(QuantumUtilityStructure.CODEC));

    // ─── Utility Structure Piece Type ────────────────────────────────────────
    public static final DeferredHolder<StructurePieceType, StructurePieceType> UTILITY_PIECE_TYPE =
        PIECE_TYPES.register("utility_piece", () -> QuantumUtilityStructure.UtilityStructurePiece::new);

    private static <S extends Structure> StructureType<S> explicitStructureTypeTyping(MapCodec<S> codec) {
        return () -> codec;
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        PLACEMENT_TYPES.register(modEventBus);
        PIECE_TYPES.register(modEventBus);
        QuantumFalcon.LOGGER.info("Registered {} structure type(s), {} piece type(s)",
            STRUCTURE_TYPES.getEntries().size(), PIECE_TYPES.getEntries().size());
    }
}
