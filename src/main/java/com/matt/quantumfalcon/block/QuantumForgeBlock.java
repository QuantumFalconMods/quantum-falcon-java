package com.matt.quantumfalcon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Quantum Forge Block - A crafting station for quantum items.
 * Simple block for now - can be expanded with GUI later.
 */
public class QuantumForgeBlock extends Block {
    
    public static final MapCodec<QuantumForgeBlock> CODEC = simpleCodec(QuantumForgeBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    
    public QuantumForgeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }
    
    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
    
    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        // Activate when powered by redstone
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(ACTIVE)) {
            level.setBlock(pos, state.setValue(ACTIVE, powered), 3);
        }
    }
}
