package com.matt.quantumfalcon.worldgen.structure.pieces;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.worldgen.structure.ArenaDesign;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Natural-worldgen boss arena: built procedurally by {@link ArenaDesign}, one chunk slice at a time. */
public class ArenaPiece extends StructurePiece {
    private final BlockPos centre;
    private final BossArenaStructure.ArenaType type;

    public ArenaPiece(BlockPos centre, BossArenaStructure.ArenaType type) {
        super(BossArenaPieces.ARENA_PIECE.get(), 0, box(centre, type));
        this.centre = centre;
        this.type = type;
    }

    public ArenaPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(BossArenaPieces.ARENA_PIECE.get(), tag);
        this.centre = NbtUtils.readBlockPos(tag, "Centre").orElse(BlockPos.ZERO);
        this.type = BossArenaStructure.ArenaType.valueOf(tag.getString("ArenaType"));
    }

    static BoundingBox box(BlockPos c, BossArenaStructure.ArenaType t) {
        int r = ArenaDesign.reach(t);
        return new BoundingBox(c.getX() - r, c.getY() - ArenaDesign.MAX_FOUNDATION - 22, c.getZ() - r,
                c.getX() + r, c.getY() + ArenaDesign.CLEAR_UP + 2, c.getZ() + r);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.put("Centre", NbtUtils.writeBlockPos(centre));
        tag.putString("ArenaType", type.name());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator gen, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        ArenaDesign.build(new ArenaDesign.Sink() {
            @Override public BlockState get(BlockPos p) { return level.getBlockState(p); }
            @Override public void set(BlockPos p, BlockState s) { if (chunkBox.isInside(p)) level.setBlock(p, s, 2); }
        }, centre, type, chunkBox);
        if (chunkBox.isInside(centre) && level.getLevel() instanceof ServerLevel sl) {
            // the boss is summoned when a player first steps inside (ArenaMechanics -> BossSummoner)
            ArenaMechanics.registerArena(sl, centre, type, ArenaDesign.floorRadius(type), 8);
            QuantumFalcon.LOGGER.debug("Arena {} generated at {} ({})", type, centre, sl.dimension().location());
        }
    }
}
