package com.matt.quantumfalcon.dimension;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.registry.ModBlocks;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Handles teleportation to/from Echo Dimensions.
 * Creates platforms and finds safe spawn locations.
 */
public class EchoTeleporter {
    private final BlockPos targetPos;
    private final boolean enteringEcho;
    
    public EchoTeleporter(BlockPos targetPos, boolean enteringEcho) {
        this.targetPos = targetPos;
        this.enteringEcho = enteringEcho;
    }

    /**
     * Create a DimensionTransition for the entity.
     */
    public DimensionTransition createTransition(Entity entity, ServerLevel destLevel) {
        // Find safe spawn position
        BlockPos safePos = findSafeSpawn(destLevel, targetPos);
        
        // Create spawn platform if needed
        ensureSafeSpawn(destLevel, safePos);
        
        Vec3 spawnPos = Vec3.atBottomCenterOf(safePos);
        
        // Get spawn angle
        float yaw = entity.getYRot();
        
        // Log teleport
        QuantumFalcon.LOGGER.info("EchoTeleporter: {} {} at {}", 
            enteringEcho ? "Entering Echo Depths" : "Exiting to Overworld",
            entity.getName().getString(),
            safePos);
        
        // Notify instability system
        if (enteringEcho && entity instanceof ServerPlayer player) {
            int instability = EchoDimensionRegistry.getInstabilityLevel(destLevel.dimension());
            InstabilityManager.onPlayerEnterEcho(player, instability);
        }
        
        return new DimensionTransition(
            destLevel,
            spawnPos,
            entity.getDeltaMovement(),
            yaw,
            entity.getXRot(),
            DimensionTransition.DO_NOTHING
        );
    }

    /**
     * Find a safe spawn position near the target.
     */
    private BlockPos findSafeSpawn(ServerLevel level, BlockPos target) {
        WorldBorder border = level.getWorldBorder();
        
        // Clamp to world border
        double x = Math.max(border.getMinX() + 16, Math.min(border.getMaxX() - 16, target.getX()));
        double z = Math.max(border.getMinZ() + 16, Math.min(border.getMaxZ() - 16, target.getZ()));
        
        BlockPos clampedPos = new BlockPos((int) x, target.getY(), (int) z);
        
        // Search for existing portal
        Optional<BlockPos> existingPortal = findNearbyPortal(level, clampedPos, 128);
        if (existingPortal.isPresent()) {
            return existingPortal.get().above();
        }
        
        // Search for safe ground
        BlockPos safeGround = findSafeGround(level, clampedPos);
        if (safeGround != null) {
            return safeGround;
        }
        
        // Default to creating platform at Y=100
        return new BlockPos(clampedPos.getX(), 100, clampedPos.getZ());
    }

    /**
     * Find nearby portal blocks.
     */
    private Optional<BlockPos> findNearbyPortal(ServerLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        
        for (int y = -radius / 4; y <= radius / 4; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    
                    if (mutable.getY() > level.getMinBuildHeight() && 
                        mutable.getY() < level.getMaxBuildHeight()) {
                        if (level.getBlockState(mutable).is(ModBlocks.RIFT_PORTAL.get())) {
                            // Found portal, return position next to it
                            return Optional.of(mutable.immutable());
                        }
                    }
                }
            }
        }
        
        return Optional.empty();
    }

    /**
     * Find safe ground (solid below, air above).
     */
    private BlockPos findSafeGround(ServerLevel level, BlockPos start) {
        int searchY = Math.min(level.getMaxBuildHeight() - 10, Math.max(start.getY(), 64));
        
        // Search downward from start position
        for (int y = searchY; y > level.getMinBuildHeight() + 5; y--) {
            BlockPos checkPos = new BlockPos(start.getX(), y, start.getZ());
            
            BlockState below = level.getBlockState(checkPos.below());
            BlockState at = level.getBlockState(checkPos);
            BlockState above = level.getBlockState(checkPos.above());
            
            if (below.isSolid() && !below.liquid() && 
                (at.isAir() || at.canBeReplaced()) && 
                (above.isAir() || above.canBeReplaced())) {
                return checkPos;
            }
        }
        
        // Search upward if nothing below
        for (int y = searchY; y < level.getMaxBuildHeight() - 10; y++) {
            BlockPos checkPos = new BlockPos(start.getX(), y, start.getZ());
            
            BlockState below = level.getBlockState(checkPos.below());
            BlockState at = level.getBlockState(checkPos);
            BlockState above = level.getBlockState(checkPos.above());
            
            if (below.isSolid() && !below.liquid() && 
                (at.isAir() || at.canBeReplaced()) && 
                (above.isAir() || above.canBeReplaced())) {
                return checkPos;
            }
        }
        
        return null;
    }

    /**
     * Ensure the spawn position is safe (create platform if needed).
     */
    private void ensureSafeSpawn(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        
        // Check if we need to create a platform
        if (!below.isSolid() || below.liquid()) {
            createSpawnPlatform(level, pos);
        }
        
        // Clear air above spawn
        for (int y = 0; y < 3; y++) {
            BlockPos clearPos = pos.above(y);
            BlockState state = level.getBlockState(clearPos);
            if (!state.isAir() && !state.is(ModBlocks.RIFT_PORTAL.get())) {
                level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Create a safe spawn platform.
     */
    private void createSpawnPlatform(ServerLevel level, BlockPos center) {
        // 5x5 platform
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = center.below().offset(x, 0, z);
                BlockState current = level.getBlockState(pos);
                
                if (!current.isSolid() || current.liquid()) {
                    // Use quantum-themed block or obsidian
                    BlockState platformBlock = ModBlocks.QUANTUM_STONE.get() != null ?
                        ModBlocks.QUANTUM_STONE.get().defaultBlockState() :
                        Blocks.OBSIDIAN.defaultBlockState();
                    level.setBlock(pos, platformBlock, 3);
                }
            }
        }
        
        QuantumFalcon.LOGGER.info("Created spawn platform at {}", center);
    }

    /**
     * Create a new portal at the destination.
     */
    public void createReturnPortal(ServerLevel level, BlockPos pos, Direction.Axis axis) {
        // Place frame blocks
        Direction horizontal = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        
        // Bottom row
        for (int i = 0; i < 4; i++) {
            BlockPos framePos = pos.relative(horizontal, i);
            level.setBlock(framePos, ModBlocks.RIFT_FRAME.get().defaultBlockState(), 3);
        }
        
        // Top row
        for (int i = 0; i < 4; i++) {
            BlockPos framePos = pos.above(4).relative(horizontal, i);
            level.setBlock(framePos, ModBlocks.RIFT_FRAME.get().defaultBlockState(), 3);
        }
        
        // Left and right columns
        for (int y = 1; y < 4; y++) {
            level.setBlock(pos.above(y), ModBlocks.RIFT_FRAME.get().defaultBlockState(), 3);
            level.setBlock(pos.above(y).relative(horizontal, 3), 
                ModBlocks.RIFT_FRAME.get().defaultBlockState(), 3);
        }
        
        // Portal interior
        for (int x = 1; x < 3; x++) {
            for (int y = 1; y < 4; y++) {
                BlockPos portalPos = pos.above(y).relative(horizontal, x);
                level.setBlock(portalPos, ModBlocks.RIFT_PORTAL.get().defaultBlockState()
                    .setValue(RiftPortalBlock.AXIS, axis), 3);
            }
        }
        
        QuantumFalcon.LOGGER.info("Created return portal at {}", pos);
    }
}
