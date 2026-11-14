package com.matt.quantumfalcon.dimension;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.registry.ModBlocks;
import com.matt.quantumfalcon.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.particles.ParticleTypes;

import java.util.UUID;

/**
 * Quantum Rift Frame Block - Forms the frame for Echo Dimension portals.
 * Right-click with Rift Igniter to form portal (like Nether portal).
 */
public class RiftFrameBlock extends Block {
    public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    
    // Portal frame dimensions
    private static final int PORTAL_WIDTH = 4;
    private static final int PORTAL_HEIGHT = 5;

    public RiftFrameBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(ACTIVATED, false)
            .setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVATED, AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Rift Igniter: let CustomPortalAPI handle lighting (ModPortals registers frame + igniter).
        // Pass through so CPA's RightClickItem can run and attemptPortalLight().
        if (stack.is(ModBlocks.RIFT_IGNITER.asItem())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Check if a valid portal frame exists and form it.
     */
    private PortalFrameResult checkAndFormPortal(Level level, BlockPos clickedPos, Direction.Axis axis) {
        // Find the bottom-left corner of the frame
        BlockPos cornerPos = findFrameCorner(level, clickedPos, axis);
        if (cornerPos == null) {
            return new PortalFrameResult(false, "could not find frame corner");
        }
        
        // Validate frame structure
        String validationError = validateFrame(level, cornerPos, axis);
        if (validationError != null) {
            return new PortalFrameResult(false, validationError);
        }
        
        // Fill portal interior
        fillPortalInterior(level, cornerPos, axis);
        
        // Activate frame blocks
        activateFrameBlocks(level, cornerPos, axis);
        
        return new PortalFrameResult(true, null);
    }

    /**
     * Find the bottom-left corner of the portal frame.
     */
    private BlockPos findFrameCorner(Level level, BlockPos start, Direction.Axis axis) {
        // Search downward and to the side to find corner
        BlockPos current = start;
        
        // Move down
        while (isFrameBlock(level, current.below())) {
            current = current.below();
        }
        
        // Move to the side (depending on axis)
        Direction sideDir = axis == Direction.Axis.X ? Direction.NORTH : Direction.WEST;
        while (isFrameBlock(level, current.relative(sideDir))) {
            current = current.relative(sideDir);
        }
        
        return current;
    }

    /**
     * Validate the portal frame structure.
     */
    private String validateFrame(Level level, BlockPos corner, Direction.Axis axis) {
        Direction horizontal = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        
        // Check bottom row (PORTAL_WIDTH blocks)
        for (int i = 0; i < PORTAL_WIDTH; i++) {
            BlockPos pos = corner.relative(horizontal, i);
            if (!isFrameBlock(level, pos)) {
                return "missing bottom frame at " + i;
            }
        }
        
        // Check top row
        for (int i = 0; i < PORTAL_WIDTH; i++) {
            BlockPos pos = corner.above(PORTAL_HEIGHT - 1).relative(horizontal, i);
            if (!isFrameBlock(level, pos)) {
                return "missing top frame at " + i;
            }
        }
        
        // Check left column (excluding corners)
        for (int i = 1; i < PORTAL_HEIGHT - 1; i++) {
            BlockPos pos = corner.above(i);
            if (!isFrameBlock(level, pos)) {
                return "missing left frame at height " + i;
            }
        }
        
        // Check right column (excluding corners)
        for (int i = 1; i < PORTAL_HEIGHT - 1; i++) {
            BlockPos pos = corner.above(i).relative(horizontal, PORTAL_WIDTH - 1);
            if (!isFrameBlock(level, pos)) {
                return "missing right frame at height " + i;
            }
        }
        
        // Check interior is empty or replaceable
        for (int x = 1; x < PORTAL_WIDTH - 1; x++) {
            for (int y = 1; y < PORTAL_HEIGHT - 1; y++) {
                BlockPos pos = corner.above(y).relative(horizontal, x);
                if (!level.getBlockState(pos).isAir() && 
                    !level.getBlockState(pos).is(ModBlocks.RIFT_PORTAL.get())) {
                    return "interior blocked at " + x + "," + y;
                }
            }
        }
        
        return null; // Valid
    }

    /**
     * Fill the portal interior with portal blocks.
     */
    private void fillPortalInterior(Level level, BlockPos corner, Direction.Axis axis) {
        Direction horizontal = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        
        UUID portalId = UUID.randomUUID();
        
        for (int x = 1; x < PORTAL_WIDTH - 1; x++) {
            for (int y = 1; y < PORTAL_HEIGHT - 1; y++) {
                BlockPos pos = corner.above(y).relative(horizontal, x);
                BlockState portalState = ModBlocks.RIFT_PORTAL.get().defaultBlockState()
                    .setValue(RiftPortalBlock.AXIS, axis);
                level.setBlock(pos, portalState, 3);
            }
        }
        
        QuantumFalcon.LOGGER.info("Formed portal at {} with ID {}", corner, portalId);
    }

    /**
     * Activate the frame blocks (visual effect).
     */
    private void activateFrameBlocks(Level level, BlockPos corner, Direction.Axis axis) {
        Direction horizontal = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        
        // Activate all frame blocks
        for (int i = 0; i < PORTAL_WIDTH; i++) {
            activateBlock(level, corner.relative(horizontal, i));
            activateBlock(level, corner.above(PORTAL_HEIGHT - 1).relative(horizontal, i));
        }
        for (int i = 1; i < PORTAL_HEIGHT - 1; i++) {
            activateBlock(level, corner.above(i));
            activateBlock(level, corner.above(i).relative(horizontal, PORTAL_WIDTH - 1));
        }
    }

    private void activateBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.RIFT_FRAME.get())) {
            level.setBlock(pos, state.setValue(ACTIVATED, true), 3);
        }
    }

    private boolean isFrameBlock(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.RIFT_FRAME.get());
    }

    // Result record for frame checking
    private record PortalFrameResult(boolean success, String reason) {}
}
