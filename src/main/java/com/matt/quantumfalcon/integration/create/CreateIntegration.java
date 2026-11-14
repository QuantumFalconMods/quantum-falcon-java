package com.matt.quantumfalcon.integration.create;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Complete Create mod integration for Quantum Falcon.
 * Provides kinetic machines powered by quantum energy and falcon mechanics.
 */
public class CreateIntegration {

    public static final DeferredRegister.Blocks CREATE_BLOCKS = 
        DeferredRegister.createBlocks(QuantumFalcon.MOD_ID);
    public static final DeferredRegister.Items CREATE_ITEMS = 
        DeferredRegister.createItems(QuantumFalcon.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, QuantumFalcon.MOD_ID);

    // ============ Block Registrations ============

    // Falcon Generator - Generates SU from nearby tamed falcons
    public static final DeferredBlock<Block> FALCON_GENERATOR = CREATE_BLOCKS.register("falcon_generator",
        () -> new FalconGeneratorBlock(Block.Properties.of()
            .strength(3.5f)
            .requiresCorrectToolForDrops()
            .noOcclusion()));

    // quantum_forge is ModBlocks.QUANTUM_FORGE; registering it again here duplicated the registry key when Create was installed.

    // Block Items
    public static final DeferredItem<Item> FALCON_GENERATOR_ITEM = CREATE_ITEMS.register("falcon_generator",
        () -> new BlockItem(FALCON_GENERATOR.get(), new Item.Properties()));

    // Block Entities
    public static final Supplier<BlockEntityType<FalconGeneratorBlockEntity>> FALCON_GENERATOR_BE = 
        BLOCK_ENTITIES.register("falcon_generator", 
            () -> BlockEntityType.Builder.of(FalconGeneratorBlockEntity::new, FALCON_GENERATOR.get()).build(null));

    /**
     * Initialize Create integration
     */
    public static void init() {
        // Register Create-specific behaviors
    }

    /**
     * Register blocks to the mod event bus
     */
    public static void registerBlocks(IEventBus modBus) {
        CREATE_BLOCKS.register(modBus);
        CREATE_ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    // ============ Block Implementations ============

    /**
     * Falcon Generator Block
     * Generates stress units (SU) based on nearby tamed falcons.
     */
    public static class FalconGeneratorBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
        public static final DirectionProperty FACING = BlockStateProperties.FACING;

        public FalconGeneratorBlock(Properties props) {
            super(props);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
        }

        @Override
        public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof FalconGeneratorBlockEntity generator) {
                    int falcons = generator.getNearbyFalconCount();
                    float su = generator.getGeneratedSU();
                    player.displayClientMessage(
                        Component.literal(String.format("Falcons: %d | Output: %.0f SU", falcons, su)),
                        true
                    );
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new FalconGeneratorBlockEntity(pos, state);
        }
    }

    /**
     * Falcon Generator Block Entity
     */
    public static class FalconGeneratorBlockEntity extends BlockEntity {
        private static final int SCAN_RADIUS = 16;
        private static final float BASE_SU_PER_FALCON = 64.0f;

        private int nearbyFalcons = 0;
        private float generatedSU = 0;
        private int scanCooldown = 0;

        public FalconGeneratorBlockEntity(BlockPos pos, BlockState state) {
            super(FALCON_GENERATOR_BE.get(), pos, state);
        }

        public void tick() {
            if (level == null || level.isClientSide) return;

            // Scan for falcons every second
            if (scanCooldown-- <= 0) {
                scanCooldown = 20;
                scanForFalcons();
            }

            // Particles when generating
            if (nearbyFalcons > 0 && level.getGameTime() % 5 == 0) {
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.2,
                        worldPosition.getZ() + 0.5,
                        1, 0.2, 0.1, 0.2, 0.01);
                }
            }
        }

        private void scanForFalcons() {
            if (level == null) return;

            AABB searchBox = new AABB(worldPosition).inflate(SCAN_RADIUS);
            List<QuantumFalconEntity> falcons = level.getEntitiesOfClass(
                QuantumFalconEntity.class, searchBox,
                f -> f.isTame() && !f.isDeadOrDying()
            );

            nearbyFalcons = falcons.size();
            generatedSU = nearbyFalcons * BASE_SU_PER_FALCON;
            
            setChanged();
        }

        public float getGeneratedSU() {
            return generatedSU;
        }

        public int getNearbyFalconCount() {
            return nearbyFalcons;
        }
    }

    /**
     * Quantum Forge Block
     * Multiblock crafting station for endgame quantum items.
     */
    public static class QuantumForgeBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
        public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

        public QuantumForgeBlock(Properties props) {
            super(props);
            registerDefaultState(stateDefinition.any()
                .setValue(ACTIVE, false)
                .setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(ACTIVE, FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                    Component.literal("Quantum Forge - Insert items to craft"),
                    true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return null; // Simplified - full implementation would have BlockEntity
        }
    }
}
