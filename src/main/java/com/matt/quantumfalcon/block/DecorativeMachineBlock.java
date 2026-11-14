package com.matt.quantumfalcon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Themed machine blocks whose art exists but whose machine logic was never written
 * (aurora_synthesizer, echo_altar, temporal_workbench, void_anvil, falcon_nest).
 * Placeable/decorative; right-click shows what it will do. See ASSET_INVENTORY.md.
 */
public class DecorativeMachineBlock extends Block {
    public static final MapCodec<DecorativeMachineBlock> CODEC = simpleCodec(DecorativeMachineBlock::new);
    public DecorativeMachineBlock(Properties properties) { super(properties); }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable(this.getDescriptionId() + ".hint"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
