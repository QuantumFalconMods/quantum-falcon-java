package com.matt.quantumfalcon.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.Optional;

/**
 * Jigsaw dungeon whose entrance hut sits on the surface, but only where the surface leaves room for the hut above and
 * the descending halls below: void columns (floating-island realms, End edges) and build-limit peaks are skipped
 * instead of producing a dungeon below the world or a hut cut off by the height limit.
 */
public class EchoDungeonStructure extends Structure {
    public static final MapCodec<EchoDungeonStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        settingsCodec(i),
        StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
        Codec.intRange(1, 20).fieldOf("size").forGetter(s -> s.size),
        Codec.INT.fieldOf("depth").forGetter(s -> s.depth),
        Codec.intRange(1, 116).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistance),
        Codec.INT.optionalFieldOf("min_surface", -40).forGetter(s -> s.minSurface),
        Codec.INT.optionalFieldOf("max_surface", 280).forGetter(s -> s.maxSurface),
        Codec.INT.optionalFieldOf("floor_margin", 70).forGetter(s -> s.floorMargin)
    ).apply(i, EchoDungeonStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final int size, depth, maxDistance, minSurface, maxSurface, floorMargin;

    public EchoDungeonStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int size, int depth, int maxDistance, int minSurface, int maxSurface, int floorMargin) {
        super(settings);
        this.startPool = startPool; this.size = size; this.depth = depth; this.maxDistance = maxDistance;
        this.minSurface = minSurface; this.maxSurface = maxSurface; this.floorMargin = floorMargin;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x0 = ctx.chunkPos().getMinBlockX(), z0 = ctx.chunkPos().getMinBlockZ();
        // cheap rejections first: /locate walks hundreds of candidates and every height sample runs the terrain noise
        if (!ctx.validBiome().test(ctx.biomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x0 + 4), net.minecraft.core.QuartPos.fromBlock(64),
                net.minecraft.core.QuartPos.fromBlock(z0 + 4), ctx.randomState().sampler()))) return Optional.empty();
        int floor = ctx.heightAccessor().getMinBuildHeight(), ceil = ctx.heightAccessor().getMaxBuildHeight();
        int minOk = Math.max(minSurface, floor + floorMargin), maxOk = Math.min(maxSurface, ceil - 24);
        // sample the hut footprint (the start piece is placed at the chunk corner and may be rotated about it), centre first,
        // and stop at the first sample that rules the spot out
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, centre = 0;
        int[][] pts = {{4, 4}, {0, 0}, {8, 8}, {-8, 8}, {8, -8}, {-8, -8}};
        for (int[] p : pts) {
            int h = ctx.chunkGenerator().getFirstFreeHeight(x0 + p[0], z0 + p[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            lo = Math.min(lo, h); hi = Math.max(hi, h);
            if (p[0] == 4) centre = h;
            if (lo < minOk || hi > maxOk || hi - lo > 14) return Optional.empty();
        }
        // A jigsaw layout can dead-end before the sanctum (found by dungeon_natural: a chronoslate dungeon of 14 pieces with no
        // key doors, guards or vault). Build the layout now and keep it only if it reaches the sanctum; try a few layouts.
        for (int attempt = 0; attempt < 4; attempt++) {
            Optional<GenerationStub> stub = JigsawPlacement.addPieces(ctx, startPool, Optional.empty(), size, new BlockPos(x0, centre - depth, z0),
                false, Optional.empty(), maxDistance, PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING);   // no terrain water waterlogs dungeon blocks
            if (stub.isEmpty()) return stub;
            net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder b = stub.get().getPiecesBuilder();
            if (reachesSanctum(b)) { TESTS_INCOMPLETE_RETRIES += attempt; return Optional.of(new GenerationStub(stub.get().position(), com.mojang.datafixers.util.Either.right(b))); }
        }
        TESTS_INCOMPLETE_DROPPED++;
        return Optional.empty();
    }

    /** Diagnostics for the tests: layouts rebuilt because the first try missed the sanctum, and spots given up on. */
    public static volatile int TESTS_INCOMPLETE_RETRIES = 0, TESTS_INCOMPLETE_DROPPED = 0;

    static boolean reachesSanctum(net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder b) {
        for (var p : b.build().pieces())
            if (p instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pe && pe.getElement().toString().contains("/sanctum")) return true;
        return false;
    }

    @Override
    public StructureType<?> type() { return ModStructures.ECHO_DUNGEON_TYPE.get(); }
}
