package com.matt.quantumfalcon.arena.fight;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A floor block mid-collapse. CRACKED: solid, glowing cracks (the warning). OPEN: no collision and nothing drawn, a hole
 * into the void below. Either way it remembers the block it replaced and puts it back on schedule; scheduled ticks are
 * saved with the chunk, so the floor heals even across a restart.
 */
public class ArenaRiftTileBlock extends BaseEntityBlock {
    public enum Stage implements StringRepresentable {
        CRACKED, OPEN;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    public static final com.mojang.serialization.MapCodec<ArenaRiftTileBlock> CODEC = simpleCodec(ArenaRiftTileBlock::new);

    public ArenaRiftTileBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(STAGE, Stage.CRACKED));
    }

    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(STAGE); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RiftTileBlockEntity(pos, state); }

    @Override
    protected RenderShape getRenderShape(BlockState s) { return s.getValue(STAGE) == Stage.OPEN ? RenderShape.INVISIBLE : RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return s.getValue(STAGE) == Stage.OPEN ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return getShape(s, l, p, c); }

    /** Client flourish: a cracked tile leaks cyan light and grit; an open tile breathes void motes up out of the hole. */
    @Override
    public void animateTick(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, RandomSource r) {
        double x = pos.getX() + 0.5, y = pos.getY() + 1.02, z = pos.getZ() + 0.5;
        if (state.getValue(STAGE) == Stage.CRACKED) {
            for (int i = 0; i < 2; i++)
                level.addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.31f, 0.89f, 1f), 0.8f),
                    x + (r.nextDouble() - 0.5) * 0.9, y, z + (r.nextDouble() - 0.5) * 0.9, 0, 0.04, 0);
            if (r.nextInt(3) == 0) level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, x + (r.nextDouble() - 0.5) * 0.6, y, z + (r.nextDouble() - 0.5) * 0.6, 0, 0.03 + r.nextDouble() * 0.03, 0);
            if (r.nextInt(4) == 0) level.addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state), x + (r.nextDouble() - 0.5), y, z + (r.nextDouble() - 0.5), 0, 0.1, 0);
        } else {
            for (int i = 0; i < 3; i++)
                level.addParticle(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, x + (r.nextDouble() - 0.5) * 0.9, pos.getY() + 0.2, z + (r.nextDouble() - 0.5) * 0.9, 0, 0.05, 0);
            if (r.nextInt(3) == 0) level.addParticle(net.minecraft.core.particles.ParticleTypes.SQUID_INK, x, pos.getY() + 0.5, z, 0, -0.05, 0);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof RiftTileBlockEntity be)) { level.removeBlock(pos, false); return; }
        if (state.getValue(STAGE) == Stage.CRACKED) {
            level.setBlock(pos, state.setValue(STAGE, Stage.OPEN), Block.UPDATE_ALL);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.02);
            level.scheduleTick(pos, this, Math.max(1, be.openTicks));
        } else {
            BlockState orig = be.original;
            level.removeBlockEntity(pos);
            level.setBlock(pos, orig, Block.UPDATE_ALL);
        }
    }
}
