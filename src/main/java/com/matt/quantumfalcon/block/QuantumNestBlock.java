package com.matt.quantumfalcon.block;

import com.matt.quantumfalcon.blockentity.QuantumNestBlockEntity;
import com.matt.quantumfalcon.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nullable;

/**
 * Quantum Nest Block - A special block where falcons can rest and breed.
 * Detects when two falcons are in love nearby and activates.
 */
public class QuantumNestBlock extends BaseEntityBlock {
    
    public static final MapCodec<QuantumNestBlock> CODEC = simpleCodec(QuantumNestBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    
    public QuantumNestBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }
    
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    
    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ACTIVE);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumNestBlockEntity(ModBlockEntities.QUANTUM_NEST.get(), pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.QUANTUM_NEST.get(), 
            QuantumNestBlockEntity::tick);
    }
}
