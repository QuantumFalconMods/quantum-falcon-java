package com.matt.quantumfalcon.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A10 Eastwatch props (Art v2 textures and model drafts, art notes: frozen_hours/v2): each comes as a
 * Then and a Now block so a then/now volume can swap them. Registered in {@link com.matt.quantumfalcon.registry.ModFrozenHoursBlocks}.
 */
public final class EastwatchProps {
    private EastwatchProps() {}

    /** Perch rail: a timber bar on a post that joins its neighbours like a fence (Then and Now rails join each other). */
    public static class PerchRail extends FenceBlock {
        public PerchRail(BlockBehaviour.Properties p) { super(p); }
    }

    /** Jess and hood rack: a wall plate; {@code facing} is the way its front looks (away from the wall). */
    public static class Rack extends HorizontalDirectionalBlock {
        public static final MapCodec<Rack> CODEC = simpleCodec(Rack::new);
        private static final VoxelShape N = Block.box(1, 1, 13, 15, 15, 16), S = Block.box(1, 1, 0, 15, 15, 3),
            W = Block.box(13, 1, 1, 16, 15, 15), E = Block.box(0, 1, 1, 3, 15, 15);
        public Rack(BlockBehaviour.Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            Direction d = ctx.getClickedFace().getAxis().isHorizontal() ? ctx.getClickedFace() : ctx.getHorizontalDirection().getOpposite();
            return defaultBlockState().setValue(FACING, d);
        }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return switch (s.getValue(FACING)) { case SOUTH -> S; case WEST -> W; case EAST -> E; default -> N; };
        }
    }

    /** Feeder trough: a low timber basin. */
    public static class Trough extends Block {
        private static final VoxelShape SHAPE = Shapes.join(Block.box(1, 0, 1, 15, 8, 15), Block.box(3, 2, 3, 13, 8, 13), net.minecraft.world.phys.shapes.BooleanOp.ONLY_FIRST);
        public Trough(BlockBehaviour.Properties p) { super(p); }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    }

    /** Feather pile: a scatter of moulted feathers. Decoration only: no collision, and it drops nothing (no loot table). */
    public static class FeatherPile extends Block {
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 8, 14);
        public FeatherPile(BlockBehaviour.Properties p) { super(p); }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
        @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
        @Override protected boolean canSurvive(BlockState s, net.minecraft.world.level.LevelReader l, BlockPos p) { return !l.isEmptyBlock(p.below()); }
        @Override protected BlockState updateShape(BlockState s, Direction d, BlockState n, net.minecraft.world.level.LevelAccessor l, BlockPos p, BlockPos np) {
            return d == Direction.DOWN && !s.canSurvive(l, p) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : s;
        }
    }
}
