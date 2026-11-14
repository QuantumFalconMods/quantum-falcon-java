package com.matt.quantumfalcon.worldgen.structure.pieces;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.boss.*;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Structure pieces for boss arenas.
 * Uses NBT templates for fixed arena layouts.
 * 
 * Data markers in templates:
 * - "boss_spawn" - Where the boss entity spawns
 * - "pillar_top" - Top of energy pillars (for particle effects)
 * - "portal_frame" - Portal frame locations
 * - "void_center" - Center of singularity/void hole
 * - "loot_chest" - Reward chest positions
 */
public class BossArenaPieces {
    
    // Registry for structure piece types
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES = 
        DeferredRegister.create(Registries.STRUCTURE_PIECE, QuantumFalcon.MOD_ID);
    
    // Boss Arena Piece Type
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BOSS_ARENA_PIECE = 
        PIECE_TYPES.register("boss_arena_piece", () -> BossArenaPiece::new);

    /** Procedural arena (stage 4); the template piece above stays registered so older worlds still load. */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ARENA_PIECE =
        PIECE_TYPES.register("arena_piece", () -> (StructurePieceType) ArenaPiece::new);
    
    /**
     * Main boss arena structure piece.
     * Handles NBT template loading and data marker processing.
     */
    public static class BossArenaPiece extends TemplateStructurePiece {
        
        private static final String TAG_ARENA_TYPE = "ArenaType";
        private final BossArenaStructure.ArenaType arenaType;
        
        /**
         * Constructor for generation.
         */
        public BossArenaPiece(StructureTemplateManager manager, ResourceLocation templateId, 
                             BlockPos pos, Rotation rotation, BossArenaStructure.ArenaType arenaType) {
            super(BOSS_ARENA_PIECE.get(), 0, manager, templateId, templateId.toString(), 
                  makeSettings(rotation), pos);
            this.arenaType = arenaType;
        }
        
        /**
         * Constructor for loading from NBT.
         */
        public BossArenaPiece(StructurePieceSerializationContext context, CompoundTag tag) {
            super(BOSS_ARENA_PIECE.get(), tag, context.structureTemplateManager(), 
                  id -> makeSettings(Rotation.valueOf(tag.getString("Rot"))));
            this.arenaType = BossArenaStructure.ArenaType.valueOf(tag.getString(TAG_ARENA_TYPE));
        }
        
        /**
         * Create placement settings for the template.
         */
        private static StructurePlaceSettings makeSettings(Rotation rotation) {
            return new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setLiquidSettings(net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.IGNORE_WATERLOGGING)   // no terrain water in arena blocks
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK) // Ignore structure blocks
                .setIgnoreEntities(false); // Keep marker entities
        }
        
        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putString("Rot", this.placeSettings.getRotation().name());
            tag.putString(TAG_ARENA_TYPE, this.arenaType.name());
        }
        
        /**
         * Handle data markers in the structure template.
         * These are Structure Block markers with custom names.
         */
        @Override
        protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, 
                                        RandomSource random, BoundingBox box) {
            switch (marker) {
                case "boss_spawn" -> handleBossSpawn(pos, level);
                case "pillar_top" -> handlePillarTop(pos, level);
                case "portal_frame" -> handlePortalFrame(pos, level);
                case "void_center" -> handleVoidCenter(pos, level);
                case "loot_chest" -> handleLootChest(pos, level, random);
                case "energy_node" -> handleEnergyNode(pos, level);
                default -> {
                    if (marker.startsWith("custom_")) {
                        handleCustomMarker(marker, pos, level);
                    }
                }
            }
            
            // Clear the marker block
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        
        /**
         * Handle boss spawn marker - spawns the appropriate boss for this arena.
         */
        private void handleBossSpawn(BlockPos pos, ServerLevelAccessor level) {
            QuantumFalcon.LOGGER.debug("Arena {} registered at {} (boss summoned on entry)", arenaType, pos);
            
            if (!(level.getLevel() instanceof ServerLevel serverLevel)) return;
            
            // The boss is no longer spawned during world generation: the arena is registered and the
            // boss is summoned when a player first steps inside (ArenaMechanics -> BossSummoner).
            int r = arenaType.templateRadius();
            ArenaMechanics.registerArena(serverLevel, pos, arenaType, r, 8);
        }
        
        /**
         * Handle pillar top marker - for energy bridge particles.
         */
        private void handlePillarTop(BlockPos pos, ServerLevelAccessor level) {
            QuantumFalcon.LOGGER.debug("Pillar top at {} for {}", pos, arenaType);
            
            // Place glowstone as visual indicator
            level.setBlock(pos, Blocks.GLOWSTONE.defaultBlockState(), 2);
        }
        
        /**
         * Handle portal frame marker.
         */
        private void handlePortalFrame(BlockPos pos, ServerLevelAccessor level) {
            QuantumFalcon.LOGGER.debug("Portal frame at {} for {}", pos, arenaType);
            
            // Place end portal frame (or custom rift frame when available)
            level.setBlock(pos, Blocks.END_PORTAL_FRAME.defaultBlockState(), 2);
        }
        
        /**
         * Handle void center marker - center of singularity.
         */
        private void handleVoidCenter(BlockPos pos, ServerLevelAccessor level) {
            QuantumFalcon.LOGGER.debug("Void center at {} for {}", pos, arenaType);
            
            // Create void hole (air surrounded by barrier for visual)
            int radius = arenaType.getVoidCenterRadius();
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        // Inner void (air)
                        level.setBlock(pos.offset(x, 0, z), Blocks.AIR.defaultBlockState(), 2);
                        // Bottom barrier to prevent falling through world
                        level.setBlock(pos.offset(x, -5, z), Blocks.BARRIER.defaultBlockState(), 2);
                    }
                }
            }
        }
        
        /**
         * Handle loot chest marker.
         */
        private void handleLootChest(BlockPos pos, ServerLevelAccessor level, RandomSource random) {
            QuantumFalcon.LOGGER.debug("Loot chest at {} for {}", pos, arenaType);
            
            // Place chest with boss loot table
            level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
            // TODO: Set loot table when chest block entity is available
        }
        
        /**
         * Handle energy node marker - for particle emitters.
         */
        private void handleEnergyNode(BlockPos pos, ServerLevelAccessor level) {
            QuantumFalcon.LOGGER.debug("Energy node at {} for {}", pos, arenaType);
            
            // Place amethyst cluster as energy visual
            level.setBlock(pos, Blocks.AMETHYST_CLUSTER.defaultBlockState(), 2);
        }
        
        /**
         * Handle custom markers with prefix.
         */
        private void handleCustomMarker(String marker, BlockPos pos, ServerLevelAccessor level) {
            String customType = marker.substring(7); // Remove "custom_" prefix
            QuantumFalcon.LOGGER.debug("Custom marker '{}' at {} for {}", customType, pos, arenaType);
        }
        
        public BossArenaStructure.ArenaType getArenaType() {
            return arenaType;
        }
    }
    
    public static void register(IEventBus modEventBus) {
        PIECE_TYPES.register(modEventBus);
        QuantumFalcon.LOGGER.info("⚡ Registered {} structure piece types", PIECE_TYPES.getEntries().size());
    }
}
