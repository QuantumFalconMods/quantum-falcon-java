package com.matt.quantumfalcon.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

/**
 * A random spread placement that keeps away from several structure sets at once (vanilla's exclusion_zone takes only one).
 * Used by the Stitchring waystations so a village never lands within avoid_chunks of any boss arena.
 */
public class ArenaAvoidingPlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<ArenaAvoidingPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.intRange(0, 4096).fieldOf("spacing").forGetter((ArenaAvoidingPlacement p) -> p.spacing()),
        Codec.intRange(0, 4096).fieldOf("separation").forGetter((ArenaAvoidingPlacement p) -> p.separation()),
        RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter((ArenaAvoidingPlacement p) -> p.spreadType()),
        Codec.INT.fieldOf("salt").forGetter((ArenaAvoidingPlacement p) -> p.salt),
        RegistryCodecs.homogeneousList(Registries.STRUCTURE_SET).fieldOf("avoid").forGetter((ArenaAvoidingPlacement p) -> p.avoid),
        Codec.intRange(1, 32).fieldOf("avoid_chunks").forGetter((ArenaAvoidingPlacement p) -> p.avoidChunks)
    ).apply(i, ArenaAvoidingPlacement::new));

    private final int salt;
    public final HolderSet<StructureSet> avoid;
    public final int avoidChunks;

    public ArenaAvoidingPlacement(int spacing, int separation, RandomSpreadType type, int salt, HolderSet<StructureSet> avoid, int avoidChunks) {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0f, salt, Optional.empty(), spacing, separation, type);
        this.salt = salt; this.avoid = avoid; this.avoidChunks = avoidChunks;
    }

    @Override
    public boolean isStructureChunk(ChunkGeneratorStructureState state, int x, int z) {
        if (!super.isStructureChunk(state, x, z)) return false;
        for (Holder<StructureSet> set : avoid) if (state.hasStructureChunkInRange(set, x, z, avoidChunks)) return false;
        return true;
    }

    @Override public StructurePlacementType<?> type() { return ModStructures.ARENA_AVOIDING.get(); }
}
