package com.matt.quantumfalcon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Rift Stabilizer: powered by redstone it becomes ACTIVE (lit). Active stabilizers within 16 blocks
 * are counted by the instability system to slow instability build-up (see InstabilityManager hooks).
 */
public class RiftStabilizerBlock extends Block {
    public static final MapCodec<RiftStabilizerBlock> CODEC = simpleCodec(RiftStabilizerBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public RiftStabilizerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ACTIVE); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(ACTIVE, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(ACTIVE)) level.setBlock(pos, state.setValue(ACTIVE, powered), 3);
    }

    /** True if an active stabilizer is within radius of pos (used by the instability system). */
    public static boolean isStabilized(Level level, BlockPos pos, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -radius / 2, -radius), pos.offset(radius, radius / 2, radius))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof RiftStabilizerBlock && s.getValue(ACTIVE)) return true;
        }
        return false;
    }
}
