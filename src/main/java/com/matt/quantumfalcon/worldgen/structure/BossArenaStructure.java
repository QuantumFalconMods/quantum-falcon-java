package com.matt.quantumfalcon.worldgen.structure;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.worldgen.structure.pieces.BossArenaPieces;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * Boss Arena Structure - Fixed NBT template based structure for epic boss fights.
 *
 * Arena types and their bosses:
 * - QUANTUM_GUARDIAN  (Boss 1): Circular cyan platform, 4 energy pillars,    Y=120
 * - VOID_HORROR       (Boss 2): Scattered void islands, 6 spires,            Y=90
 * - ENTANGLED_TWINS   (Boss 3): Twin mirrored platforms, crystal bridge,     Y=110
 * - SINGULARITY_SOVEREIGN (Boss 4): Accretion disk, central void, 8 pillars, Y=150
 */
public class BossArenaStructure extends Structure {
    
    // Codec for serialization
    public static final MapCodec<BossArenaStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            settingsCodec(instance),
            ArenaType.CODEC.fieldOf("arena_type").forGetter(s -> s.arenaType)
        ).apply(instance, BossArenaStructure::new)
    );
    
    private final ArenaType arenaType;
    
    public BossArenaStructure(StructureSettings settings, ArenaType arenaType) {
        super(settings);
        this.arenaType = arenaType;
    }
    
    /**
     * Find valid generation point for the arena.
     * Arenas spawn at high Y (100-150) for floating island effect.
     */
    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // Get chunk position
        ChunkPos chunkPos = context.chunkPos();
        
        // Sit on the terrain: the floor goes one block above the mean ground height under the arena, and the
        // plinth/terrace in ArenaDesign fills or cuts the difference.
        int r = arenaType.templateRadius();
        java.util.List<Integer> hs = new java.util.ArrayList<>();
        for (int dx = -r; dx <= r; dx += r / 3) for (int dz = -r; dz <= r; dz += r / 3) {
            if (dx * dx + dz * dz > r * r) continue;
            hs.add(context.chunkGenerator().getFirstOccupiedHeight(chunkPos.getMiddleBlockX() + dx, chunkPos.getMiddleBlockZ() + dz,
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()));
        }
        java.util.Collections.sort(hs);
        int min = hs.get(0), max = hs.get(hs.size() - 1);
        int bottom = context.heightAccessor().getMinBuildHeight(), ceiling = context.heightAccessor().getMaxBuildHeight();
        // every placement cell must yield an arena, or whole regions (one biome band, void, mountains) end up
        // thousands of blocks from the nearest boss. No ground at all -> a pedestal over the void at y 96 (the foundation is
        // capped and tapered, ArenaDropGuard keeps drops out of the drop); rugged ground -> the median height, and the
        // terraces cut the hills back.
        int spawnY = min <= bottom + 40 ? 96 : hs.get(hs.size() / 2) + 1;
        spawnY = Math.min(spawnY, ceiling - 24);

        // Center position
        BlockPos centerPos = new BlockPos(
            chunkPos.getMiddleBlockX(),
            spawnY,
            chunkPos.getMiddleBlockZ()
        );
        
        return Optional.of(new GenerationStub(centerPos, builder -> 
            generatePieces(builder, context, centerPos)));
    }
    
    /**
     * Generate the structure pieces.
     */
    private void generatePieces(StructurePiecesBuilder builder, GenerationContext context, BlockPos pos) {
        // Get template name based on arena type
        ResourceLocation templateId = arenaType.getTemplateId();
        
        builder.addPiece(new com.matt.quantumfalcon.worldgen.structure.pieces.ArenaPiece(pos, arenaType));
        QuantumFalcon.LOGGER.debug("Generated {} arena at {}", arenaType.name(), pos);
    }
    
    @Override
    public StructureType<?> type() {
        return ModStructures.BOSS_ARENA_TYPE.get();
    }
    
    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }
    
    // Note: In 1.21.x, structure settings are defined via JSON datapacks.
    // The createSettings() method is no longer needed - settings come from CODEC.
    
    /**
     * Arena type enum with associated properties.
     */
    public enum ArenaType {
        // Boss 1: Quantum Overseer — cyan energy platform, 4 pillars, no void
        QUANTUM_GUARDIAN("quantum_guardian_arena", 64, 4, false),
        // Boss 2: Rift Warden — scattered void islands, 6 dark spires, small void center
        VOID_HORROR("void_horror_arena", 48, 6, true),
        // Boss 3: Entangled Twins — twin mirrored platforms joined by crystal bridge, 8 pillars
        ENTANGLED_TWINS("entangled_twins_arena", 56, 8, false),
        // Boss 4: Singularity Sovereign — accretion disk, 8 massive pillars, large void singularity
        SINGULARITY_SOVEREIGN("singularity_sovereign_arena", 80, 8, true);

        public static final Codec<ArenaType> CODEC = Codec.STRING.xmap(
            ArenaType::valueOf,
            ArenaType::name
        );

        private final String templateName;
        private final int radius;
        private final int pillarCount;
        private final boolean hasVoidCenter;

        /** Half-width of the generated arena template (tools/gen_dungeons.py). */
        public int templateRadius() {
            return switch (this) { case QUANTUM_GUARDIAN, VOID_HORROR -> 22; case ENTANGLED_TWINS -> 25; case SINGULARITY_SOVEREIGN -> 30; };
        }

        ArenaType(String templateName, int radius, int pillarCount, boolean hasVoidCenter) {
            this.templateName = templateName;
            this.radius = radius;
            this.pillarCount = pillarCount;
            this.hasVoidCenter = hasVoidCenter;
        }

        public ResourceLocation getTemplateId() {
            return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, templateName);
        }

        public int getRadius() { return radius; }
        public int getPillarCount() { return pillarCount; }
        public boolean hasVoidCenter() { return hasVoidCenter; }

        public int getDeathY(int arenaY) {
            return switch (this) {
                case QUANTUM_GUARDIAN      -> arenaY - 10;
                case VOID_HORROR           -> arenaY - 20;
                case ENTANGLED_TWINS       -> arenaY - 15;
                case SINGULARITY_SOVEREIGN -> arenaY - 5;
            };
        }

        public int getVoidCenterRadius() {
            return switch (this) {
                case QUANTUM_GUARDIAN      -> 0;
                case VOID_HORROR           -> 4;
                case ENTANGLED_TWINS       -> 0;
                case SINGULARITY_SOVEREIGN -> 12;
            };
        }

        /** Instability threshold (0–100) required to fight this boss via the quantum progression. */
        public int getRequiredInstability() {
            return switch (this) {
                case QUANTUM_GUARDIAN      -> 20;
                case VOID_HORROR           -> 40;
                case ENTANGLED_TWINS       -> 60;
                case SINGULARITY_SOVEREIGN -> 85;
            };
        }
    }
}
