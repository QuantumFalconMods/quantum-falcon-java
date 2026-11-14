package com.matt.quantumfalcon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Perch stand: decor only (no POI, never a job site; the Falconer's job site stays {@link FalconPerchBlock}).
 * hooded=true shows a hooded falcon on the dowel; in v1 that state comes from worldgen only, and the item and loot are always
 * the empty stand. The hooded outline reaches y=21 so the bird can be targeted; collision stays inside the block.
 */
public class PerchStandBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PerchStandBlock> CODEC = simpleCodec(PerchStandBlock::new);
    public static final BooleanProperty HOODED = BooleanProperty.create("hooded");
    private static final VoxelShape BASE = Shapes.or(Block.box(5, 0, 5, 11, 2, 11), Block.box(6, 2, 6, 10, 12, 10));
    private static final VoxelShape NS = Shapes.or(BASE, Block.box(4, 11, 6, 12, 14, 10));   // dowel across x when facing north/south
    private static final VoxelShape EW = Shapes.or(BASE, Block.box(6, 11, 4, 10, 14, 12));
    private static final VoxelShape BIRD = Block.box(5, 14, 5, 11, 21, 11);

    public PerchStandBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HOODED, false));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, HOODED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }

    private static VoxelShape stand(BlockState s) { return s.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : EW; }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return s.getValue(HOODED) ? Shapes.or(stand(s), BIRD) : stand(s);
    }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return stand(s); }
}
