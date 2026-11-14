package com.matt.quantumfalcon.integration.waystones;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;

/**
 * Waystones integration for Quantum Falcon.
 * Provides quantum waystones that integrate with the Waystones network.
 */
public class WaystonesIntegration {

    public static final DeferredRegister.Blocks WAYSTONE_BLOCKS = 
        DeferredRegister.createBlocks(QuantumFalcon.MOD_ID);
    public static final DeferredRegister.Items WAYSTONE_ITEMS = 
        DeferredRegister.createItems(QuantumFalcon.MOD_ID);

    // ============ Block Registrations ============

    public static final DeferredBlock<Block> QUANTUM_WAYSTONE = WAYSTONE_BLOCKS.register("quantum_waystone",
        () -> new QuantumWaystoneBlock(Block.Properties.of()
            .strength(3.5f)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(QuantumWaystoneBlock.ACTIVE) ? 15 : 8)));

    public static final DeferredItem<Item> QUANTUM_WAYSTONE_ITEM = WAYSTONE_ITEMS.register("quantum_waystone",
        () -> new BlockItem(QUANTUM_WAYSTONE.get(), new Item.Properties()));

    /**
     * Initialize Waystones integration
     */
    public static void init() {
        // Waystones handles registration via IMC or data
    }

    /**
     * Register blocks to the mod event bus
     */
    public static void registerBlocks(IEventBus modBus) {
        WAYSTONE_BLOCKS.register(modBus);
        WAYSTONE_ITEMS.register(modBus);
    }

    // ============ Block Implementation ============

    /**
     * Quantum Waystone Block
     * Integrates with Waystones network for cross-dimensional travel.
     */
    public static class QuantumWaystoneBlock extends Block {
        public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

        public QuantumWaystoneBlock(Properties props) {
            super(props);
            registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(ACTIVE);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            // Activate waystone
            if (!state.getValue(ACTIVE)) {
                level.setBlock(pos, state.setValue(ACTIVE, true), 3);
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.5f);
                player.displayClientMessage(
                    Component.literal("Quantum Waystone activated! Connected to network."),
                    true
                );

                // Particles
                if (level instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 20; i++) {
                        double angle = (i / 20.0) * Math.PI * 2;
                        double radius = 1.5;
                        serverLevel.sendParticles(ParticleTypes.PORTAL,
                            pos.getX() + 0.5 + Math.cos(angle) * radius,
                            pos.getY() + 1.5,
                            pos.getZ() + 0.5 + Math.sin(angle) * radius,
                            1, 0, 0.1, 0, 0.1);
                    }
                }
            } else {
                // Open Waystones GUI (if Waystones mod provides API)
                player.displayClientMessage(
                    Component.literal("Right-click to open Waystone network"),
                    true
                );
            }

            return InteractionResult.SUCCESS;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(ACTIVE, false);
        }
    }
}
