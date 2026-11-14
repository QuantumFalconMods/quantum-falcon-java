package com.matt.quantumfalcon.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall plaque at a dungeon entrance showing its danger tier. FACING is the side the plaque faces (the wall is behind it). */
public class DangerSignBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DangerSignBlock> CODEC = simpleCodec(DangerSignBlock::new);

    public enum Tier implements StringRepresentable {
        COMMON(1, "Iron", ChatFormatting.GRAY), UNCOMMON(2, "Iron", ChatFormatting.GREEN), RARE(3, "Diamond", ChatFormatting.BLUE),
        EPIC(4, "Diamond", ChatFormatting.DARK_PURPLE), MYTHIC(5, "Netherite", ChatFormatting.LIGHT_PURPLE),
        LEGENDARY(6, "Netherite", ChatFormatting.GOLD), QUANTUM(7, "Quantum", ChatFormatting.AQUA);
        public final int level; public final String gear; public final ChatFormatting color;
        Tier(int level, String gear, ChatFormatting color) { this.level = level; this.gear = gear; this.color = color; }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static final EnumProperty<Tier> TIER = EnumProperty.create("tier", Tier.class);
    private static final VoxelShape N = Block.box(0, 0, 14, 16, 16, 16), S = Block.box(0, 0, 0, 16, 16, 2),
        E = Block.box(0, 0, 0, 2, 16, 16), W = Block.box(14, 0, 0, 16, 16, 16);

    public DangerSignBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER, Tier.COMMON));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, TIER); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace().getAxis().isHorizontal() ? ctx.getClickedFace() : ctx.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, face);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) { case SOUTH -> S; case EAST -> E; case WEST -> W; default -> N; };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            Tier t = state.getValue(TIER);
            player.displayClientMessage(Component.literal("Danger tier " + t.level + " (" + t.getSerializedName() + "): bring " + t.gear + " gear or better.").withStyle(t.color), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
