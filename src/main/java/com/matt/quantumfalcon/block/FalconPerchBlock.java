package com.matt.quantumfalcon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Falcon perch: a wooden T-perch. Tamed falcons ordered to sit near one land on it,
 * and falcons set to WANDER treat the last perch they used as home.
 */
public class FalconPerchBlock extends Block {
    public static final MapCodec<FalconPerchBlock> CODEC = simpleCodec(FalconPerchBlock::new);
    /** Height (in blocks) of the top of the crossbar a falcon stands on. */
    public static final double PERCH_TOP = 14.0 / 16.0;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 2, 12),
            Block.box(7, 2, 7, 9, 12, 9),
            Block.box(2, 12, 7, 14, 14, 9));

    public FalconPerchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
