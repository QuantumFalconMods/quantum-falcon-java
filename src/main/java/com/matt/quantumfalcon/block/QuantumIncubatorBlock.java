package com.matt.quantumfalcon.block;

import com.matt.quantumfalcon.blockentity.QuantumIncubatorBlockEntity;
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

import javax.annotation.Nullable;

/**
 * Quantum Incubator Block - Hatches falcon eggs with fuel.
 * Place an egg and fuel (quantum shards) to start incubation.
 */
public class QuantumIncubatorBlock extends BaseEntityBlock {
    
    public static final MapCodec<QuantumIncubatorBlock> CODEC = simpleCodec(QuantumIncubatorBlock::new);
    
    public QuantumIncubatorBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof QuantumIncubatorBlockEntity be) player.openMenu(be);
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof QuantumIncubatorBlockEntity be)
            net.minecraft.world.Containers.dropContents(level, pos, be);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumIncubatorBlockEntity(ModBlockEntities.QUANTUM_INCUBATOR.get(), pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.QUANTUM_INCUBATOR.get(), 
            QuantumIncubatorBlockEntity::serverTick);
    }
}
