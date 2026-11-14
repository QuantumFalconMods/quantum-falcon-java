package com.matt.quantumfalcon.arena.fight;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shield pylon. While charged it feeds the arena's boss a damage shield (see {@link ArenaInteraction}); breaking it
 * staggers the boss. The fight raises fresh pylons each time a boss is engaged.
 */
public class ArenaPylonBlock extends Block {
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public ArenaPylonBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(CHARGED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(CHARGED); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel sl && state.getValue(CHARGED)) ArenaInteraction.onPylonBroken(sl, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
}
