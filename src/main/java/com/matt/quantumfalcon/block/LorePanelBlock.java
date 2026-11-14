package com.matt.quantumfalcon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Carved realm lore panel: a facing block whose carved glyph face points at the player who placed it. */
public class LorePanelBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<LorePanelBlock> CODEC = simpleCodec(LorePanelBlock::new);
    public LorePanelBlock(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH)); }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }
}
