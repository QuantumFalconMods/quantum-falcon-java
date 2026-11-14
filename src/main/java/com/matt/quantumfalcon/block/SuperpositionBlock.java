/*
 * Inspired by qCraft's quantum block / observer-dependent collapse; reimplemented for QuantumFalcon.
 * Superposition state collapses to a random outcome on first observation (use, break, or redstone).
 */
package com.matt.quantumfalcon.block;

import com.matt.quantumfalcon.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SuperpositionBlock extends Block {
    public static final MapCodec<SuperpositionBlock> CODEC = SuperpositionBlock.simpleCodec(SuperpositionBlock::new);
    public static final EnumProperty<QuantumState> STATE = EnumProperty.create("quantum_state", QuantumState.class);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty POWERED = net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;

    public SuperpositionBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // qCraft-style: start unobserved; collapse on first interaction.
        this.registerDefaultState(this.stateDefinition.any().setValue(STATE, QuantumState.SUPERPOSITION).setValue(POWERED, false));
    }

    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{STATE, POWERED});
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(STATE) == QuantumState.SUPERPOSITION) {
            collapseOnObservation(level, pos, state);
        } else {
            cycleState(level, pos, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        // react to a rising edge only (power turning on), not to every neighbour update while powered
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), 2);
        if (!powered) return;
        state = level.getBlockState(pos);
        if (state.getValue(STATE) == QuantumState.SUPERPOSITION) {
            collapseOnObservation(level, pos, state);
        } else {
            cycleState(level, pos, state);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(STATE) == QuantumState.SUPERPOSITION) {
            collapseOnObservation(level, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** qCraft-style: collapse unobserved state to random SOLID / TRANSPARENT / PHASED. */
    private void collapseOnObservation(Level level, BlockPos pos, BlockState state) {
        QuantumState[] observed = { QuantumState.SOLID, QuantumState.TRANSPARENT, QuantumState.PHASED };
        QuantumState collapsed = observed[level.getRandom().nextInt(observed.length)];
        BlockState next = state.setValue(STATE, collapsed);
        level.setBlock(pos, next, 3);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.5f,
            collapsed == QuantumState.PHASED ? 1.5f : collapsed == QuantumState.TRANSPARENT ? 1.2f : 1.0f);
        spawnCollapseParticles(level, pos, collapsed);
    }

    private void cycleState(Level level, BlockPos pos, BlockState state) {
        QuantumState current = state.getValue(STATE);
        QuantumState next = current.next();
        level.setBlock(pos, state.setValue(STATE, next), 3);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.5f,
            next == QuantumState.PHASED ? 1.5f : next == QuantumState.TRANSPARENT ? 1.2f : 1.0f);
        spawnCollapseParticles(level, pos, next);
    }

    private void spawnCollapseParticles(Level level, BlockPos pos, QuantumState state) {
        ParticleOptions particle = state == QuantumState.PHASED
            ? ModParticles.QUANTUM_SPARK.get()
            : state == QuantumState.TRANSPARENT
                ? ModParticles.CRYSTAL_SHIMMER.get()
                : ParticleTypes.PORTAL;
        for (int i = 0; i < 10; i++) {
            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5);
            double y = pos.getY() + 0.5 + (level.random.nextDouble() - 0.5);
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5);
            level.addParticle(particle, x, y, z, 0.0, 0.1, 0.0);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        QuantumState s = state.getValue(STATE);
        if (s == QuantumState.PHASED || s == QuantumState.SUPERPOSITION) {
            return Shapes.empty();
        }
        return super.getCollisionShape(state, level, pos, context);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(STATE) == QuantumState.SOLID ? 0.2f : 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(STATE) != QuantumState.SOLID;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        QuantumState s = state.getValue(STATE);
        if (s == QuantumState.SUPERPOSITION) {
            if (random.nextInt(2) == 0) {
                double x = pos.getX() + random.nextDouble();
                double y = pos.getY() + random.nextDouble();
                double z = pos.getZ() + random.nextDouble();
                level.addParticle(ModParticles.QUANTUM_SPARK.get(), x, y, z, (random.nextDouble() - 0.5) * 0.03, 0.02, (random.nextDouble() - 0.5) * 0.03);
            }
        } else if (s == QuantumState.PHASED) {
            if (random.nextInt(3) == 0) {
                double x = pos.getX() + random.nextDouble();
                double y = pos.getY() + random.nextDouble();
                double z = pos.getZ() + random.nextDouble();
                level.addParticle(ParticleTypes.END_ROD, x, y, z, (random.nextDouble() - 0.5) * 0.05, 0.0, (random.nextDouble() - 0.5) * 0.05);
            }
        } else if (s == QuantumState.TRANSPARENT && random.nextInt(10) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
            level.addParticle(ModParticles.CRYSTAL_SHIMMER.get(), x, y, z, 0.0, 0.0, 0.0);
        }
    }

    public enum QuantumState implements StringRepresentable {
        /** Unobserved; collapses to random SOLID/TRANSPARENT/PHASED on first use, break, or redstone. */
        SUPERPOSITION("superposition"),
        SOLID("solid"),
        TRANSPARENT("transparent"),
        PHASED("phased");

        private final String name;

        QuantumState(String name) { this.name = name; }

        @Override
        public String getSerializedName() { return name; }

        /** Cycle through observed states only (excludes SUPERPOSITION). */
        public QuantumState next() {
            QuantumState[] observed = { SOLID, TRANSPARENT, PHASED };
            int i = 0;
            for (int j = 0; j < observed.length; j++) {
                if (observed[j] == this) { i = j; break; }
            }
            return observed[(i + 1) % observed.length];
        }
    }
}
