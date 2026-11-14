package com.matt.quantumfalcon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Resonance bell: the Stitchring's bell (plaza arch, chapel belfry). Vanilla bell state language (attachment, facing, powered)
 * so structures swap 1:1, but a static model and no block entity. Right-click or a rising redstone signal rings it: a bell
 * sound and a few cyan motes; with a Rift Stabilizer within 8 blocks the motes trace the stabilizer's safe ring.
 */
public class ResonanceBellBlock extends Block {
    public static final MapCodec<ResonanceBellBlock> CODEC = simpleCodec(ResonanceBellBlock::new);
    public static final EnumProperty<BellAttachType> ATTACHMENT = BlockStateProperties.BELL_ATTACHMENT;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape BODY = Shapes.or(Block.box(5, 6, 5, 11, 13, 11), Block.box(4, 4, 4, 12, 6, 12));
    private static final VoxelShape CEILING = Shapes.or(BODY, Block.box(7, 13, 7, 9, 16, 9));
    private static final VoxelShape FLOOR_NS = Shapes.or(BODY, Block.box(0, 0, 6, 16, 16, 10));
    private static final VoxelShape FLOOR_EW = Shapes.or(BODY, Block.box(6, 0, 0, 10, 16, 16));

    public ResonanceBellBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ATTACHMENT, BellAttachType.FLOOR).setValue(POWERED, false));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, ATTACHMENT, POWERED); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        Direction look = ctx.getHorizontalDirection();
        if (face == Direction.DOWN) return defaultBlockState().setValue(ATTACHMENT, BellAttachType.CEILING).setValue(FACING, look);
        if (face == Direction.UP) return defaultBlockState().setValue(ATTACHMENT, BellAttachType.FLOOR).setValue(FACING, look);
        BlockPos other = ctx.getClickedPos().relative(face);
        boolean both = ctx.getLevel().getBlockState(other).isFaceSturdy(ctx.getLevel(), other, face.getOpposite());
        return defaultBlockState().setValue(ATTACHMENT, both ? BellAttachType.DOUBLE_WALL : BellAttachType.SINGLE_WALL).setValue(FACING, face.getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(ATTACHMENT)) {
            case CEILING -> CEILING;
            case FLOOR -> s.getValue(FACING).getAxis() == Direction.Axis.Z ? FLOOR_NS : FLOOR_EW;
            default -> BODY;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) ring((ServerLevel) level, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState s, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        boolean on = level.hasNeighborSignal(pos);
        if (on != s.getValue(POWERED)) {
            if (on && !level.isClientSide) ring((ServerLevel) level, pos);
            level.setBlock(pos, s.setValue(POWERED, on), 3);
        }
    }

    /** Bell sound plus motes; returns the stabilizer it traced, or null. */
    public static BlockPos ring(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.6f, 0.8f);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 0.7f);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 6, 0.25, 0.2, 0.25, 0.02);
        BlockPos stab = null;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-8, -8, -8), pos.offset(8, 8, 8)))
            if (level.getBlockState(p).is(com.matt.quantumfalcon.registry.ModFamilyBlocks.RIFT_STABILIZER.get())) { stab = p.immutable(); break; }
        if (stab != null) {
            var cyan = new DustParticleOptions(new org.joml.Vector3f(0.16f, 0.93f, 1.0f), 1.2f);
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI * 2 / 48;
                level.sendParticles(cyan, stab.getX() + 0.5 + Math.cos(a) * 8, stab.getY() + 1.2, stab.getZ() + 0.5 + Math.sin(a) * 8, 1, 0, 0.1, 0, 0);
            }
        }
        return stab;
    }
}
