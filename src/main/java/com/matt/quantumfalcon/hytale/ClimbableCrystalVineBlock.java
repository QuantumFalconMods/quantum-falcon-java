package com.matt.quantumfalcon.hytale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Hytale-inspired Climbable Crystal Vines - Can be climbed like a ladder.
 * Glows and has particle effects. Grows naturally on certain blocks.
 */
public class ClimbableCrystalVineBlock extends Block {
    public static final BooleanProperty GLOWING = BooleanProperty.create("glowing");
    
    // Thin vine shape
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
    
    // Climb speed multiplier
    private static final double CLIMB_SPEED = 0.2;

    public ClimbableCrystalVineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(GLOWING, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLOWING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // No collision - can walk through
    }

    /**
     * This makes the block climbable like a ladder.
     */
    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return true;
    }

    /**
     * Custom climbing behavior with speed boost.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (entity instanceof LivingEntity living) {
            // Reset fall distance when on vine
            living.resetFallDistance();
            
            // Speed boost when climbing
            if (living.horizontalCollision) {
                double yVelocity = living.getDeltaMovement().y;
                
                // Boost upward movement when moving up (jumping detected by positive Y velocity intent)
                // Players holding jump while on ladder naturally get upward movement
                if (yVelocity > 0) {
                    yVelocity = Math.min(yVelocity + 0.05, CLIMB_SPEED);
                } else if (yVelocity < 0) {
                    // Slow descent
                    yVelocity = Math.max(yVelocity, -0.1);
                }
                
                living.setDeltaMovement(
                    living.getDeltaMovement().x * 0.95,
                    yVelocity,
                    living.getDeltaMovement().z * 0.95
                );
            }
        }
    }

    /**
     * Light level when glowing.
     */
    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(GLOWING) ? 8 : 0;
    }

    /**
     * Ambient particle effects.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(GLOWING)) return;
        
        // Crystal sparkle particles
        if (random.nextInt(10) == 0) {
            double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
            
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.02, 0);
        }
        
        // Occasional ambient sound
        if (random.nextInt(200) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS,
                0.2f, 0.8f + random.nextFloat() * 0.4f, false);
        }
    }

    /**
     * Random tick for growth/glow changes.
     */
    /** the longest a vine grows by itself. */
    public static final int MAX_LENGTH = 8;

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Toggle glow based on light level
        int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        boolean shouldGlow = skyLight < 8; // Glow in darkness
        
        if (state.getValue(GLOWING) != shouldGlow) {
            level.setBlock(pos, state.setValue(GLOWING, shouldGlow), 3);
        }
        
        // Try to grow upward
        if (random.nextInt(5) == 0) {
            BlockPos above = pos.above();
            // it only climbs a wall (a solid block beside the new segment), and never past MAX_LENGTH in one run; a free
            // vine used to grow to the build limit
            int run = 1; BlockPos b = pos.below();
            while (run <= MAX_LENGTH && level.getBlockState(b).is(this)) { run++; b = b.below(); }
            boolean wall = false;
            for (Direction dir : Direction.Plane.HORIZONTAL) if (level.getBlockState(above.relative(dir)).isSolid()) { wall = true; break; }
            if (run < MAX_LENGTH && wall && level.isEmptyBlock(above) && canSupportVine(level, above)) {
                // Grow new vine segment
                level.setBlock(above, this.defaultBlockState(), 3);
                
                // Particles
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    above.getX() + 0.5, above.getY() + 0.5, above.getZ() + 0.5,
                    5, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    /**
     * Check if position can support vine growth.
     */
    private boolean canSupportVine(Level level, BlockPos pos) {
        // Check for nearby solid blocks
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = level.getBlockState(pos.relative(dir));
            if (neighbor.isSolid()) {
                return true;
            }
        }
        
        // Or vine below
        BlockState below = level.getBlockState(pos.below());
        return below.is(this);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }
}
