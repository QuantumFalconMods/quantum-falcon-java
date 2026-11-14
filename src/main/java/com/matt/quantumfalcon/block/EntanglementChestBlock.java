/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.BlockHitResult
 */
package com.matt.quantumfalcon.block;

import com.matt.quantumfalcon.block.entity.EntanglementChestBlockEntity;
import net.minecraft.server.level.ServerLevel;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class EntanglementChestBlock
extends BaseEntityBlock {
    public static final MapCodec<EntanglementChestBlock> CODEC = EntanglementChestBlock.simpleCodec(EntanglementChestBlock::new);
    public static final BooleanProperty LINKED = BooleanProperty.create((String)"linked");
    /** the latch is on the front only; the front faces the placer. */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    public EntanglementChestBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)LINKED, (Comparable)Boolean.valueOf(false)).setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{LINKED, FACING});
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EntanglementChestBlockEntity(pos, state);
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EntanglementChestBlockEntity) {
                EntanglementChestBlockEntity chest = (EntanglementChestBlockEntity)be;
                serverPlayer.openMenu((MenuProvider)chest, pos);
            }
        }
        return level.isClientSide() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
    }

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double z;
        double y;
        double x;
        if (((Boolean)state.getValue((Property)LINKED)).booleanValue() && random.nextInt(3) == 0) {
            x = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
            y = (double)(pos.getY() + 1) + random.nextDouble();
            z = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
            level.addParticle((ParticleOptions)ParticleTypes.END_ROD, x, y, z, 0.0, 0.1, 0.0);
        }
        if (random.nextInt(5) == 0) {
            x = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5);
            y = (double)pos.getY() + 0.5 + random.nextDouble() * 0.5;
            z = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5);
            level.addParticle((ParticleOptions)ParticleTypes.PORTAL, x, y, z, 0.0, 0.02, 0.0);
        }
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity be;
        if (!state.is(newState.getBlock()) && (be = level.getBlockEntity(pos)) instanceof EntanglementChestBlockEntity) {
            EntanglementChestBlockEntity chest = (EntanglementChestBlockEntity)be;
            chest.onRemoved();
            net.minecraft.world.Containers.dropContents(level, pos, chest);   // breaking it used to delete everything inside
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
