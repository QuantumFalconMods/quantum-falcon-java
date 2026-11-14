package com.matt.quantumfalcon.blockentity;

import com.matt.quantumfalcon.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;

/**
 * Entangled Ore Block - blocks linked in quantum entanglement network.
 * Mining one triggers cascade effects on all linked blocks.
 * Inspired by qCraft's quantum blocks.
 */
public class EntangledOreBlock extends BaseEntityBlock {
    
    public static final MapCodec<EntangledOreBlock> CODEC = simpleCodec(EntangledOreBlock::new);

    public EntangledOreBlock(Properties properties) {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EntangledOreBlockEntity(ModBlockEntities.ENTANGLED_ORE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ENTANGLED_ORE.get(), 
            (lvl, pos, blockState, entity) -> entity.tick(lvl, pos, blockState));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EntangledOreBlockEntity entangledOre) {
                if (entangledOre.getNetworkId() != null) {
                    EntangledOreBlockEntity.EntangledNetwork network = 
                        EntangledOreBlockEntity.getNetwork(entangledOre.getNetworkId());
                    if (network != null) {
                        network.onNodeMined(level, pos, player);
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
