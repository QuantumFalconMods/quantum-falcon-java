package com.matt.quantumfalcon.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * A10 Frozen Hours: one authored template, never rotated (Eastwatch's birds face east), sunk so its surface floor
 * ({@code depth} blocks above the template's base) sits on the terrain. The same flatness/height checks as
 * echo_dungeon; then/now volumes, bells and gates come from data/quantumfalcon/frozen_hours/&lt;id&gt;.json at runtime
 * ({@link com.matt.quantumfalcon.dungeon.FrozenHours}).
 */
public class FrozenHourStructure extends Structure {
    public static final MapCodec<FrozenHourStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        settingsCodec(i),
        ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
        Codec.INT.fieldOf("depth").forGetter(s -> s.depth),
        Codec.INT.optionalFieldOf("min_surface", 50).forGetter(s -> s.minSurface),
        Codec.INT.optionalFieldOf("max_surface", 230).forGetter(s -> s.maxSurface),
        Codec.INT.optionalFieldOf("max_slope", 12).forGetter(s -> s.maxSlope)
    ).apply(i, FrozenHourStructure::new));

    final ResourceLocation template;
    final int depth, minSurface, maxSurface, maxSlope;

    public FrozenHourStructure(StructureSettings settings, ResourceLocation template, int depth, int minSurface, int maxSurface, int maxSlope) {
        super(settings);
        this.template = template; this.depth = depth; this.minSurface = minSurface; this.maxSurface = maxSurface; this.maxSlope = maxSlope;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x0 = ctx.chunkPos().getMinBlockX(), z0 = ctx.chunkPos().getMinBlockZ();
        if (!ctx.validBiome().test(ctx.biomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x0 + 15), net.minecraft.core.QuartPos.fromBlock(64),
                net.minecraft.core.QuartPos.fromBlock(z0 + 9), ctx.randomState().sampler()))) return Optional.empty();
        int floor = ctx.heightAccessor().getMinBuildHeight(), ceil = ctx.heightAccessor().getMaxBuildHeight();
        int minOk = Math.max(minSurface, floor + depth + 8), maxOk = Math.min(maxSurface, ceil - 24);
        // the surface hut sits at template x 11..19, z 5..13: sample it (centre first)
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, centre = 0;
        int[][] pts = {{15, 9}, {11, 5}, {19, 13}, {11, 13}, {19, 5}};
        for (int[] p : pts) {
            int h = ctx.chunkGenerator().getFirstFreeHeight(x0 + p[0], z0 + p[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            lo = Math.min(lo, h); hi = Math.max(hi, h);
            if (p[0] == 15 && p[1] == 9) centre = h;
            if (lo < minOk || hi > maxOk || hi - lo > maxSlope) return Optional.empty();
        }
        BlockPos origin = new BlockPos(x0, centre - depth, z0);
        return Optional.of(new GenerationStub(origin, b -> b.addPiece(new Piece(ctx.structureTemplateManager(), template, origin))));
    }

    public ResourceLocation templateId() { return template; }

    @Override
    public StructureType<?> type() { return ModStructures.FROZEN_HOUR_TYPE.get(); }

    static StructurePlaceSettings place() {
        // Blocker 1: IGNORE_WATERLOGGING, so an authored waterloggable block isn't waterlogged by terrain water
        return new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setLiquidSettings(net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.IGNORE_WATERLOGGING).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    /** The template, placed unrotated; its bounding box min corner is the template origin. */
    public static class Piece extends TemplateStructurePiece {
        public Piece(StructureTemplateManager mgr, ResourceLocation loc, BlockPos pos) {
            super(ModStructures.FROZEN_HOUR_PIECE.get(), 0, mgr, loc, loc.toString(), place(), pos);
        }
        public Piece(StructurePieceSerializationContext ctx, CompoundTag tag) {
            super(ModStructures.FROZEN_HOUR_PIECE.get(), tag, ctx.structureTemplateManager(), l -> place());
        }
        public BlockPos origin() { return templatePosition; }
        public ResourceLocation templateId() { return ResourceLocation.parse(templateName); }
        @Override
        public void postProcess(net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.StructureManager sm, net.minecraft.world.level.chunk.ChunkGenerator gen,
                                RandomSource random, BoundingBox box, net.minecraft.world.level.ChunkPos chunk, BlockPos pivot) {
            super.postProcess(level, sm, gen, random, box, chunk, pivot);
            String path = templateId().getPath();
            com.matt.quantumfalcon.dungeon.FrozenHours.afterPlace(level, ResourceLocation.fromNamespaceAndPath(templateId().getNamespace(), path.substring(path.lastIndexOf('/') + 1)), templatePosition, box);
        }
        @Override protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {}
    }
}
