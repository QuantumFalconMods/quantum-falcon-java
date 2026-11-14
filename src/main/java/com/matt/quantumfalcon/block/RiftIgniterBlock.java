package com.matt.quantumfalcon.block;

import com.matt.quantumfalcon.dimension.RiftFrameBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Rift Igniter Block - Activates Rift Frame portals.
 * Right-click to attempt to activate a nearby portal frame.
 */
public class RiftIgniterBlock extends Block {
    
    public static final MapCodec<RiftIgniterBlock> CODEC = simpleCodec(RiftIgniterBlock::new);
    
    public RiftIgniterBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
    
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        // Rift Igniter is used by right-clicking on Rift Frame blocks directly
        // The activation logic is in RiftFrameBlock.useItemOn()
        // This block itself doesn't need special logic - it's just the item used to activate frames
        return InteractionResult.PASS;
    }
}
