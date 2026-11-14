package com.matt.quantumfalcon.block;

import com.matt.quantumfalcon.blockentity.FalconEggBlockEntity;
import com.matt.quantumfalcon.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;

/**
 * Falcon Egg block. Placed when two falcons breed; hatches after progress when player is near.
 */
public class FalconEggBlock extends BaseEntityBlock {

    public static final MapCodec<FalconEggBlock> CODEC = simpleCodec(FalconEggBlock::new);

    public FalconEggBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FalconEggBlockEntity(ModBlockEntities.FALCON_EGG.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FALCON_EGG.get(), FalconEggBlockEntity::tick);
    }

    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        ItemStack stack = com.matt.quantumfalcon.registry.ModItems.FALCON_EGG.get().getDefaultInstance();
        if (be instanceof FalconEggBlockEntity eggBe) {
            stack = com.matt.quantumfalcon.item.FalconEggItem.fromBlock(stack, eggBe);
        }
        return stack;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        boolean isCatalyst = com.matt.quantumfalcon.breeding.BreedingData.CATALYSTS.stream().anyMatch(c -> c.id().equals(id));
        if (!isCatalyst) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FalconEggBlockEntity be) {
            boolean had = be.catalyst() != null;
            player.displayClientMessage(be.applyCatalyst(id), true);
            if (!had && be.catalyst() != null && !player.getAbilities().instabuild) stack.shrink(1);
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FalconEggBlockEntity be) player.displayClientMessage(be.status(), true);
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * the egg drops itself however the block goes (a player, an explosion, a piston), carrying its genome. It used to
     * drop only in playerWillDestroy, so a creeper at the nest deleted it.
     */
    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        if (params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof FalconEggBlockEntity be)
            return java.util.List.of(com.matt.quantumfalcon.item.FalconEggItem.fromBlock(com.matt.quantumfalcon.registry.ModItems.FALCON_EGG.get().getDefaultInstance(), be));
        return super.getDrops(state, params);
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, net.minecraft.world.phys.shapes.CollisionContext c) {
        var nq = FalconNestBlock.qualityAt(l, p.below());
        return nq == null ? SHAPE : NEST_SHAPES[nq.ordinal()];
    }
    /** Height of an egg resting in a nest, in blocks above the nest seat. */
    public static final float EGG_SCALE_ON_NEST = 1.0f;
    public static final double EGG_TOP_ON_NEST = 11 * EGG_SCALE_ON_NEST / 16.0;
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = Block.box(4, 0, 4, 12, 11, 12);
    /** On a nest the egg is drawn resting on the nest seat at EGG_SCALE_ON_NEST (FalconEggRenderer); the outline and collision follow it. */
    private static final net.minecraft.world.phys.shapes.VoxelShape[] NEST_SHAPES = java.util.Arrays.stream(com.matt.quantumfalcon.breeding.NestQuality.values())
            .map(q -> Block.box(8 - 4 * EGG_SCALE_ON_NEST, q.seatPx - 16, 8 - 4 * EGG_SCALE_ON_NEST, 8 + 4 * EGG_SCALE_ON_NEST, q.seatPx - 16 + EGG_TOP_ON_NEST * 16, 8 + 4 * EGG_SCALE_ON_NEST)).toArray(net.minecraft.world.phys.shapes.VoxelShape[]::new);
}
