package com.matt.quantumfalcon.worldgen.structure;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.registry.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Optional;

/**
 * Quantum Utility Structure — procedurally generated world structures.
 *
 * 11 types across two tiers:
 *
 * TIER 1 — Discovery (early/mid-game, surface):
 *   ANCIENT_NEST      — falcon breeding origin; triggers StorylineManager.CHAPTER_1
 *   QUANTUM_WELL      — energy well landmark; first instability signal
 *   RIFT_SHRINE       — mid-game instability hotspot; triggers +5 instability/min
 *   TEMPORAL_NEXUS    — quantum tech hub; triggers CHAPTER_3 on proximity
 *   ECHO_RIFT_GATE    — Echo Rift dimension entry structure; instability spike +10
 *   BREEDING_SANCTUM  — large indoor falcon genetics facility; genetics loot
 *
 * TIER 2 — Deep/Dangerous (mid/late-game, underground or hidden):
 *   ECHO_CHAMBER      — underground rift dome; Tier-2 instability zone
 *   CRYSTAL_CAVERN    — deep crystal geode; dense ore deposits
 *   DATA_ARCHIVE      — vault of quantum experiments; triggers CHAPTER_2
 *   QUANTUM_OBSERVATORY — signal tower; triggers CHAPTER_8
 *   VOID_CITADEL      — underground void fortress; end-game dungeon
 */
public class QuantumUtilityStructure extends Structure {

    public static final MapCodec<QuantumUtilityStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            settingsCodec(instance),
            UtilityType.CODEC.fieldOf("utility_type").forGetter(s -> s.utilityType)
        ).apply(instance, QuantumUtilityStructure::new)
    );

    private final UtilityType utilityType;

    public QuantumUtilityStructure(StructureSettings settings, UtilityType utilityType) {
        super(settings);
        this.utilityType = utilityType;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();

        int baseY = switch (utilityType) {
            case ANCIENT_NEST, QUANTUM_WELL, RIFT_SHRINE,
                 TEMPORAL_NEXUS, ECHO_RIFT_GATE, BREEDING_SANCTUM,
                 QUANTUM_OBSERVATORY -> context.chunkGenerator()
                    .getFirstOccupiedHeight(chunkPos.getMiddleBlockX(), chunkPos.getMiddleBlockZ(),
                        Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            case ECHO_CHAMBER   -> 40;
            case CRYSTAL_CAVERN -> 20;
            case DATA_ARCHIVE   -> 30;
            case VOID_CITADEL   -> 50;
        };

        BlockPos centerPos = new BlockPos(
            chunkPos.getMiddleBlockX(), baseY, chunkPos.getMiddleBlockZ());

        return Optional.of(new GenerationStub(centerPos, builder ->
            generatePieces(builder, context, centerPos)));
    }

    private void generatePieces(StructurePiecesBuilder builder, GenerationContext context, BlockPos pos) {
        builder.addPiece(new UtilityStructurePiece(pos, utilityType));
        QuantumFalcon.LOGGER.debug("Queued {} utility structure at {}", utilityType.name(), pos);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.UTILITY_STRUCTURE_TYPE.get();
    }

    @Override
    public GenerationStep.Decoration step() {
        return switch (utilityType) {
            case ECHO_CHAMBER, CRYSTAL_CAVERN, DATA_ARCHIVE, VOID_CITADEL ->
                GenerationStep.Decoration.UNDERGROUND_STRUCTURES;
            default -> GenerationStep.Decoration.SURFACE_STRUCTURES;
        };
    }

    // =====================================================================
    // Utility Type Enum
    // =====================================================================

    public enum UtilityType {
        // ── Tier 1: Discovery ──────────────────────────────────────────────
        ANCIENT_NEST("ancient_nest",         24, 35),
        QUANTUM_WELL("quantum_well",         10, 30),
        RIFT_SHRINE("rift_shrine",           14, 30),
        TEMPORAL_NEXUS("temporal_nexus",     26, 40),
        ECHO_RIFT_GATE("echo_rift_gate",     16, 32),
        BREEDING_SANCTUM("breeding_sanctum", 32, 30),
        // ── Tier 2: Deep / Dangerous ────────────────────────────────────
        ECHO_CHAMBER("echo_chamber",         28, 65),
        CRYSTAL_CAVERN("crystal_cavern",     20, 45),
        DATA_ARCHIVE("data_archive",         20, 50),
        QUANTUM_OBSERVATORY("quantum_observatory", 8, 75),
        VOID_CITADEL("void_citadel",         28, 55);

        public static final Codec<UtilityType> CODEC = Codec.STRING.xmap(
            UtilityType::valueOf, UtilityType::name);

        final String id;
        final int radius;
        /** Total vertical extent (used for bounding box height). */
        final int height;

        UtilityType(String id, int radius, int height) {
            this.id = id;
            this.radius = radius;
            this.height = height;
        }

        public int getRadius() { return radius; }
        public int getHeight() { return height; }
    }

    // =====================================================================
    // Inline Piece
    // =====================================================================

    public static class UtilityStructurePiece extends StructurePiece {

        private final UtilityType utilityType;
        private final BlockPos origin;

        public UtilityStructurePiece(BlockPos origin, UtilityType type) {
            super(ModStructures.UTILITY_PIECE_TYPE.get(), 0,
                BoundingBox.fromCorners(
                    origin.offset(-type.radius - 1, -13, -type.radius - 1),
                    origin.offset( type.radius + 1, type.height, type.radius + 16)));   // +16 south: the dresser's approach path
            this.origin = origin;
            this.utilityType = type;
        }

        public UtilityStructurePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
            super(ModStructures.UTILITY_PIECE_TYPE.get(), tag);
            this.utilityType = UtilityType.valueOf(tag.getString("UtilityType"));
            this.origin = new BlockPos(tag.getInt("OX"), tag.getInt("OY"), tag.getInt("OZ"));
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
            tag.putString("UtilityType", utilityType.name());
            tag.putInt("OX", origin.getX());
            tag.putInt("OY", origin.getY());
            tag.putInt("OZ", origin.getZ());
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager,
                                ChunkGenerator generator, RandomSource random,
                                BoundingBox box, ChunkPos chunkPos, BlockPos pivotPos) {
            // Clip every write to the chunk being decorated, and use a position-seeded random so all chunks build the
            // same structure (previously each chunk built the whole thing with its own random, writing into far chunks).
            clip = box;
            random = RandomSource.create(UtilityDresser.seed(this.boundingBox.getCenter().getX(), this.boundingBox.getCenter().getY(), this.boundingBox.getCenter().getZ()));
            switch (utilityType) {
                case ANCIENT_NEST        -> buildAncientNest(level, random);
                case QUANTUM_WELL        -> buildQuantumWell(level, random);
                case RIFT_SHRINE         -> buildRiftShrine(level, random);
                case TEMPORAL_NEXUS      -> buildTemporalNexus(level, random);
                case ECHO_RIFT_GATE      -> buildEchoRiftGate(level, random);
                case BREEDING_SANCTUM    -> buildBreedingSanctum(level, random);
                case ECHO_CHAMBER        -> buildEchoChamber(level, random);
                case CRYSTAL_CAVERN      -> buildCrystalCavern(level, random);
                case DATA_ARCHIVE        -> buildDataArchive(level, random);
                case QUANTUM_OBSERVATORY -> buildQuantumObservatory(level, random);
                case VOID_CITADEL        -> buildVoidCitadel(level, random);
            }
            boolean surface = switch (utilityType) {
                case ECHO_CHAMBER, CRYSTAL_CAVERN, DATA_ARCHIVE, VOID_CITADEL -> false;
                default -> true;
            };
            UtilityDresser.dress(level, box, origin, utilityType, surface);
        }

        // ==================================================================
        // ANCIENT NEST — 48×48 circular platform (r=22 inside 2-thick wall)
        //
        // CONSTRUCTION LOG (concept art blueprint match):
        //   Floor     : r=22 circle, quantum_stone, quantum_tech_cyan accents at arm bases
        //   Outer wall: r=21..22 filled ring, 5 blocks high (WALL_H=5)
        //               sea_lantern beacon at Y+3 on each cardinal + 45° pos (8 beacons total)
        //   8 arms    : every 45°, 2 blocks wide, ARM_H=3, quantum_stone
        //               quantum_tech_cyan floor accent under each arm line
        //   Pedestal  : 4×4 base Y+1-2, 2×2 Y+3-4, quantum_nest at Y+5
        //               sea_lantern on 4 sides of 4×4 base at Y+1
        //   Glass dome: hemisphere r=3 centered at Y+5 (nest height)
        //               quantum_glass_cyan base ring, quantum_glass body, sea_lantern top
        //   Loot chest: placed in east section at (14, 1, 0)
        // ==================================================================
        private void buildAncientNest(WorldGenLevel level, RandomSource rng) {
            final BlockState qStone = ModBlocks.QUANTUM_STONE.get().defaultBlockState();
            final BlockState qBlock = ModBlocks.QUANTUM_BLOCK.get().defaultBlockState();
            final BlockState qGlass = ModBlocks.QUANTUM_GLASS.get().defaultBlockState();
            final BlockState qGlCy  = ModBlocks.QUANTUM_GLASS_CYAN.get().defaultBlockState();
            final BlockState qNest  = ModBlocks.QUANTUM_NEST.get().defaultBlockState();
            final BlockState qTech  = ModBlocks.QUANTUM_TECH_CYAN.get().defaultBlockState();
            final BlockState seaLan = Blocks.SEA_LANTERN.defaultBlockState();
            final int R      = 22;
            final int WALL_H = 5;
            final int ARM_H  = 3;
            final int PED_H  = 4;

            // ── CIRCULAR FLOOR ──
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    if (Math.sqrt(x * x + z * z) <= R) {
                        setBlock(level, origin.offset(x,  0, z), qStone);
                        setBlock(level, origin.offset(x, -1, z), qStone);
                        setBlock(level, origin.offset(x, -2, z), qStone);
                    }
                }
            }

            // ── OUTER WALL: solid filled ring r=21..22, 5 blocks high ──
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist >= R - 1 && dist <= R) {
                        // Sea lantern beacons at 8 positions (every 45°)
                        double angle = Math.atan2(z, x);
                        int octant = (int) Math.round(angle / (Math.PI / 4));
                        boolean isBeacon = Math.abs(Math.atan2(z, x) - octant * Math.PI / 4) < 0.07;
                        for (int y = 1; y <= WALL_H; y++) {
                            setBlock(level, origin.offset(x, y, z),
                                (isBeacon && y == 3) ? seaLan : qBlock);
                        }
                    }
                }
            }

            // ── 8 RADIAL ARM WALLS: every 45°, 2-wide, ARM_H=3 ──
            for (int i = 0; i < 8; i++) {
                double angle   = Math.PI * 2 * i / 8;
                double perpAng = angle + Math.PI / 2;
                int pX = (int) Math.round(Math.cos(perpAng));
                int pZ = (int) Math.round(Math.sin(perpAng));

                for (int dist = 3; dist <= R - 2; dist++) {
                    int bx = (int) Math.round(Math.cos(angle) * dist);
                    int bz = (int) Math.round(Math.sin(angle) * dist);

                    for (int side = 0; side <= 1; side++) {
                        int ax = bx + pX * side;
                        int az = bz + pZ * side;
                        // Floor accent below arm
                        setBlock(level, origin.offset(ax, 0, az), qTech);
                        // Arm wall blocks
                        for (int y = 1; y <= ARM_H; y++)
                            setBlock(level, origin.offset(ax, y, az), qStone);
                    }
                }
            }

            // ── CENTRAL PEDESTAL: 4×4 base (Y+1-2), 2×2 cap (Y+3-4), nest at Y+5 ──
            for (int y = 1; y <= 2; y++) {
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                    setBlock(level, origin.offset(x, y, z), qBlock);
            }
            // Sea lantern ring at Y+1 on 4 sides
            for (int[] c4 : new int[][]{{2,0},{-2,0},{0,2},{0,-2}})
                setBlock(level, origin.offset(c4[0], 1, c4[1]), seaLan);
            for (int y = 3; y <= PED_H; y++) {
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
                    setBlock(level, origin.offset(x, y, z), qBlock);
            }
            setBlock(level, origin.above(PED_H + 1), qNest);  // Y+5

            // ── QUANTUM GLASS DOME: hemisphere r=3 over nest at Y+5 ──
            int domeBase = PED_H + 2;   // Y+6
            for (int x = -4; x <= 4; x++) {
                for (int y = 0; y <= 4; y++) {
                    for (int z = -4; z <= 4; z++) {
                        double dist = Math.sqrt((double)(x*x) + (double)(y*y) + (double)(z*z));
                        if (dist >= 2.7 && dist <= 3.4 && y >= 0) {
                            setBlock(level, origin.offset(x, domeBase + y, z),
                                y == 0 ? qGlCy : qGlass);
                        }
                    }
                }
            }
            // Dome apex sea_lantern crown
            setBlock(level, origin.above(domeBase + 3), seaLan);

            placeChest(level, origin.offset(14, 1, 0), "quantumfalcon:chests/ancient_nest", rng);
        }

        // ==================================================================
        // QUANTUM WELL — 20×20 circular platform (r=9)
        //
        // CONSTRUCTION LOG (concept art blueprint match):
        //   Floor     : r=9 circle, true CHECKERBOARD (alternating qstone/qblock per tile)
        //               From blueprint: isWhite = (x%2 == z%2)
        //   Rim wall  : r=8..9 filled ring, 6 blocks high
        //               sea_lantern beacon at Y+4 every 30° (12 total)
        //   Shaft     : EXACTLY 4×4 = coords (-2,-2) to (1,1), cleared 12 blocks deep
        //               Shaft walls: quantum_tech_cyan with sea_lantern every 4Y
        //               From blueprint: 4-block wide shaft clearly marked
        //   Glass cap : 4×4 quantum_glass at Y+1, quantum_glass_cyan edges
        //   Loot chest: at shaft bottom Y-11 + sea_lantern beacon at Y-12
        // ==================================================================
        private void buildQuantumWell(WorldGenLevel level, RandomSource rng) {
            final BlockState qStone = ModBlocks.QUANTUM_STONE.get().defaultBlockState();
            final BlockState qBlock = ModBlocks.QUANTUM_BLOCK.get().defaultBlockState();
            final BlockState qTech  = ModBlocks.QUANTUM_TECH_CYAN.get().defaultBlockState();
            final BlockState qGlass = ModBlocks.QUANTUM_GLASS.get().defaultBlockState();
            final BlockState qGlCy  = ModBlocks.QUANTUM_GLASS_CYAN.get().defaultBlockState();
            final BlockState seaLan = Blocks.SEA_LANTERN.defaultBlockState();
            final int R       = 9;
            final int RIM_H   = 6;
            final int SHAFT_D = 12;

            // ── TRUE CHECKERBOARD FLOOR ──
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    if (Math.sqrt(x * x + z * z) <= R) {
                        boolean isWhite = (Math.floorMod(x, 2) == Math.floorMod(z, 2));
                        setBlock(level, origin.offset(x,  0, z), isWhite ? qStone : qBlock);
                        setBlock(level, origin.offset(x, -1, z), qStone);
                    }
                }
            }

            // ── RIM WALL (6 high, sea_lantern every 30°) ──
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist >= R - 1 && dist <= R) {
                        int deg = (int) Math.round(Math.toDegrees(Math.atan2(z, x)));
                        boolean isBeacon = (Math.floorMod(deg, 30) < 6);
                        for (int y = 1; y <= RIM_H; y++) {
                            setBlock(level, origin.offset(x, y, z),
                                (isBeacon && y == 4) ? seaLan : qStone);
                        }
                    }
                }
            }

            // ── 4×4 SHAFT: coords x=-2..1, z=-2..1, depth Y=0..-12 ──
            for (int y = -SHAFT_D; y <= 0; y++) {
                for (int x = -2; x <= 1; x++) {
                    for (int z = -2; z <= 1; z++) {
                        boolean isWall = (x == -2 || x == 1 || z == -2 || z == 1);
                        if (isWall) {
                            setBlock(level, origin.offset(x, y, z),
                                (y % 4 == 0) ? seaLan : qTech);
                        } else {
                            put(level, origin.offset(x, y, z),
                                Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }

            // ── QUANTUM GLASS CAP over shaft at Y+1 ──
            for (int x = -2; x <= 1; x++) {
                for (int z = -2; z <= 1; z++) {
                    boolean isEdge = (x == -2 || x == 1 || z == -2 || z == 1);
                    setBlock(level, origin.offset(x, 1, z), isEdge ? qGlCy : qGlass);
                }
            }

            // Beacon glow at shaft base + loot chest
            setBlock(level, origin.below(SHAFT_D - 1), seaLan);
            setBlock(level, origin.below(SHAFT_D),     seaLan);
            placeChest(level, origin.offset(-1, -(SHAFT_D - 2), -1),
                "quantumfalcon:chests/quantum_well", rng);
        }

        // ==================================================================
        // RIFT SHRINE — 28×28 circular platform (r=14)
        //
        // CONSTRUCTION LOG (concept art blueprint match):
        //   Floor     : r=14 rift_stone, 20% purple crack scatter
        //               From blueprint: purple crack clusters, ring of void bricks at r=12
        //   Obelisk   : 3×3 base Y+1-7, 2×2 mid Y+8-14, 1×1 top Y+15-20, void_bricks
        //               sea_lantern every 4Y at core (Y+4, Y+8, Y+12, Y+16)
        //   SEA LANTERN HALO: floating ring of sea_lanterns at r=2.5, Y+21
        //               KEY detail from close-up concept art — torus/halo shape
        //   Crystal apex: crystal_cluster at Y+22, sea_lantern crown at Y+23
        //   6 satellite pillars: r=9, every 60°, 7 high, crystal top + sea_lantern cap
        //   Purple crack floor: void_bricks patches scattered (30% of floor)
        //   Loot chest: hidden in south section at (0, 1, 10)
        // ==================================================================
        private void buildRiftShrine(WorldGenLevel level, RandomSource rng) {
            final BlockState riftStone  = ModBlocks.RIFT_STONE.get().defaultBlockState();
            final BlockState voidBricks = ModBlocks.VOID_BRICKS.get().defaultBlockState();
            final BlockState crystal    = ModBlocks.CRYSTAL_CLUSTER.get().defaultBlockState();
            final BlockState seaLan     = Blocks.SEA_LANTERN.defaultBlockState();
            final BlockState purpCon    = Blocks.PURPLE_CONCRETE.defaultBlockState();
            final int R = 14;

            // ── RIFT STONE FLOOR with purple crack scatter ──
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist <= R) {
                        // Purple crack patches: scatter pattern from blueprint
                        double noiseVal = Math.sin(x * 0.7) * Math.cos(z * 0.7);
                        BlockState floor = (noiseVal > 0.55f) ? purpCon
                            : (dist > R - 2) ? voidBricks : riftStone;
                        setBlock(level, origin.offset(x, 0, z), floor);
                        setBlock(level, origin.offset(x, -1, z), riftStone);
                    }
                }
            }

            // ── CENTRAL OBELISK: 3×3 → 2×2 → 1×1 taper, 20 blocks ──
            for (int y = 1; y <= 20; y++) {
                int hw = (y <= 7) ? 1 : (y <= 14) ? 0 : -1;  // 3×3, 2×2 (hw=0→offset 0-1), 1×1
                if (hw >= 0) {
                    // 3×3 base (y=1-7): x,z in [-1..1]
                    // 2×2 mid (y=8-14): x,z in [0..1]
                    int lo = (y <= 7) ? -1 : 0;
                    int hi = (y <= 14) ? 1 : 0;  // 1×1 for y=15+
                    for (int x = lo; x <= hi; x++) {
                        for (int z = lo; z <= hi; z++) {
                            boolean isCore = (x == 0 && z == 0);
                            setBlock(level, origin.offset(x, y, z),
                                (isCore && y % 4 == 0) ? seaLan : voidBricks);
                        }
                    }
                } else {
                    // 1×1 top shaft
                    setBlock(level, origin.above(y),
                        (y % 4 == 0) ? seaLan : voidBricks);
                }
            }

            // ── SEA LANTERN HALO at Y+21 (KEY concept art detail) ──
            // Floating ring of 16 sea lanterns at r≈3 around apex
            for (int deg = 0; deg < 360; deg += 22) {
                double angle = Math.toRadians(deg);
                int rx = (int) Math.round(Math.cos(angle) * 3);
                int rz = (int) Math.round(Math.sin(angle) * 3);
                setBlock(level, origin.offset(rx, 21, rz), seaLan);
            }

            // ── CRYSTAL APEX ──
            setBlock(level, origin.above(22), crystal);
            setBlock(level, origin.above(23), seaLan);
            setBlock(level, origin.above(24), crystal);

            // ── 6 SATELLITE PILLARS at r=9, every 60° ──
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * 2 * i / 6;
                int px = (int) Math.round(Math.cos(angle) * 9);
                int pz = (int) Math.round(Math.sin(angle) * 9);
                for (int y = 1; y <= 7; y++) {
                    setBlock(level, origin.offset(px, y, pz),
                        (y == 7) ? crystal : (y == 5) ? seaLan : voidBricks);
                }
                // Crystal cluster top
                setBlock(level, origin.offset(px, 8, pz), crystal);
            }

            placeChest(level, origin.offset(0, 1, 10), "quantumfalcon:chests/rift_shrine", rng);
        }

        // ==================================================================
        // TEMPORAL NEXUS — radius 26, surface
        // Quantum tech hub: raised central platform with 4 wing arms, temporal
        // pillars at corners, quantum forge + incubator in center.
        // Inspired by BoMD gauntlet arena scale (48×48 footprint).
        // Loot: temporal_nexus. Story: CHAPTER_3 zone.
        // ==================================================================
        private void buildTemporalNexus(WorldGenLevel level, RandomSource rng) {
            BlockState qtech    = ModBlocks.QUANTUM_TECH_CYAN.get().defaultBlockState();
            BlockState qtechP   = ModBlocks.QUANTUM_TECH_PURPLE.get().defaultBlockState();
            BlockState qstone   = ModBlocks.QUANTUM_STONE.get().defaultBlockState();
            BlockState qblock   = ModBlocks.QUANTUM_BLOCK.get().defaultBlockState();
            BlockState qglass   = ModBlocks.QUANTUM_GLASS_CYAN.get().defaultBlockState();
            BlockState tpillar  = ModBlocks.TEMPORAL_PILLAR.get().defaultBlockState();
            BlockState seaLan   = Blocks.SEA_LANTERN.defaultBlockState();
            BlockState eLattice = ModBlocks.ECHO_LATTICE.get().defaultBlockState();
            BlockState qTiles   = ModBlocks.QUANTUM_TILES.get().defaultBlockState();
            int r = 26;

            // === MAIN CIRCULAR PLATFORM ===
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist <= r) {
                        // Circuit board pattern: cyan tech grid every 4 blocks
                        boolean isCircuit = (Math.abs(x) % 4 == 0 || Math.abs(z) % 4 == 0);
                        boolean isEdge    = dist > r - 2;
                        BlockState floor  = isEdge ? qblock : (isCircuit ? qtech : qstone);
                        setBlock(level, origin.offset(x, 0, z), floor);
                        setBlock(level, origin.offset(x, -1, z), qstone);
                        setBlock(level, origin.offset(x, -2, z), qstone);
                    }
                }
            }

            // === OUTER RIM WALL (3 high) ===
            for (int deg = 0; deg < 360; deg += 4) {
                double angle = Math.toRadians(deg);
                int wx = (int) (Math.cos(angle) * (r - 1));
                int wz = (int) (Math.sin(angle) * (r - 1));
                for (int y = 1; y <= 3; y++)
                    setBlock(level, origin.offset(wx, y, wz), (y == 2 && deg % 20 == 0) ? seaLan : qblock);
            }

            // === 4 WING ARMS (cardinal directions, 20 blocks long, 5 wide) ===
            int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] dir : dirs) {
                for (int dist = 6; dist <= 24; dist++) {
                    for (int side = -2; side <= 2; side++) {
                        int ax = dir[0] * dist + dir[1] * side;
                        int az = dir[1] * dist + dir[0] * side;
                        double armDist = Math.sqrt(ax * ax + az * az);
                        if (armDist > r) {  // Only outside the main circle
                            BlockState armBlock = (Math.abs(side) <= 1 && dist % 4 == 0) ? qtech : qstone;
                            setBlock(level, origin.offset(ax, 0, az), armBlock);
                            setBlock(level, origin.offset(ax, -1, az), qstone);
                        }
                    }
                    // Arm railings (side ±2 walls)
                    for (int side : new int[]{-2, 2}) {
                        int ax = dir[0] * dist + dir[1] * side;
                        int az = dir[1] * dist + dir[0] * side;
                        double armDist = Math.sqrt(ax * ax + az * az);
                        if (armDist > r) {
                            for (int y = 1; y <= 2; y++)
                                setBlock(level, origin.offset(ax, y, az), qblock);
                        }
                    }
                }
            }

            // === 4 TEMPORAL PILLAR CORNERS ===
            int pillarDist = (int) (r * 0.65);
            for (int i = 0; i < 4; i++) {
                double angle = Math.PI * 2 * i / 4 + Math.PI / 4;
                int px = (int) (Math.cos(angle) * pillarDist);
                int pz = (int) (Math.sin(angle) * pillarDist);
                for (int y = 1; y <= 16; y++) {
                    BlockState pBlock = (y % 4 == 0) ? seaLan : tpillar;
                    setBlock(level, origin.offset(px, y, pz), pBlock);
                    // 2x2 base for first 4 blocks
                    if (y <= 4) {
                        setBlock(level, origin.offset(px+1, y, pz), tpillar);
                        setBlock(level, origin.offset(px, y, pz+1), tpillar);
                    }
                }
                // Echo lattice crown
                setBlock(level, origin.offset(px, 17, pz), eLattice);
                setBlock(level, origin.offset(px, 18, pz), seaLan);
            }

            // === CENTRAL RAISED PLATFORM (quantum tech center) ===
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    if (Math.sqrt(x * x + z * z) <= 5) {
                        setBlock(level, origin.offset(x, 1, z), qTiles);
                        setBlock(level, origin.offset(x, 2, z), qTiles);
                    }
                }
            }
            // Cyan glass cover above center
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    if (Math.sqrt(x * x + z * z) <= 4) {
                        setBlock(level, origin.offset(x, 8, z), qglass);
                    }
                }
            }
            // Quantum forge
            setBlock(level, origin.offset(0, 3, 2), ModBlocks.QUANTUM_FORGE.get().defaultBlockState());
            // Quantum incubator
            setBlock(level, origin.offset(0, 3, -2), ModBlocks.QUANTUM_INCUBATOR.get().defaultBlockState());
            // Central echo lattice pillar
            for (int y = 3; y <= 7; y++) setBlock(level, origin.above(y), y % 2 == 0 ? seaLan : eLattice);

            // Purple tech accent cross on floor
            for (int x = -r; x <= r; x++) {
                if (Math.abs(x) <= r - 2) {
                    setBlock(level, origin.offset(x, 0, 0), qtechP);
                    setBlock(level, origin.offset(0, 0, x), qtechP);
                }
            }

            // Loot chest hidden under wing tip (east arm)
            placeChest(level, origin.offset(24, 1, 0), "quantumfalcon:chests/temporal_nexus", rng);
            // Second chest in north wing
            placeChest(level, origin.offset(0, 1, 24), "quantumfalcon:chests/temporal_nexus", rng);
        }

        // ==================================================================
        // ECHO RIFT GATE — radius 16, surface
        // Rift portal frame in a 5×8 doorway shape, framed by 4 cyan glow pillars,
        // quantum stone courtyard, rift crystal deposits at base.
        // Instability spike: +10 on first visit. Loot: echo_rift_gate.
        // ==================================================================
        private void buildEchoRiftGate(WorldGenLevel level, RandomSource rng) {
            BlockState riftFrame  = ModBlocks.RIFT_FRAME.get().defaultBlockState();
            BlockState riftStone  = ModBlocks.RIFT_STONE.get().defaultBlockState();
            BlockState riftBricks = ModBlocks.RIFT_STONE_BRICKS.get().defaultBlockState();
            BlockState crystal    = ModBlocks.CRYSTAL_CLUSTER.get().defaultBlockState();
            BlockState qglass     = ModBlocks.QUANTUM_GLASS_CYAN.get().defaultBlockState();
            BlockState voidObs    = ModBlocks.VOID_OBSIDIAN.get().defaultBlockState();
            BlockState seaLan     = Blocks.SEA_LANTERN.defaultBlockState();
            BlockState tpillar    = ModBlocks.TEMPORAL_PILLAR.get().defaultBlockState();
            int r = 16;

            // === CIRCULAR COURTYARD ===
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist <= r) {
                        BlockState floor = (dist > r - 2) ? riftBricks :
                            (((Math.abs(x) + Math.abs(z)) % 4 == 0) ? crystal : riftStone);
                        setBlock(level, origin.offset(x, 0, z), floor);
                        setBlock(level, origin.offset(x, -1, z), riftBricks);
                    }
                }
            }

            // === RIFT PORTAL FRAME (5 wide × 8 tall doorway on north face) ===
            // Bottom row
            for (int x = -2; x <= 2; x++) setBlock(level, origin.offset(x, 0, -r + 2), riftFrame);
            // Side columns
            for (int y = 1; y <= 7; y++) {
                setBlock(level, origin.offset(-2, y, -r + 2), riftFrame);
                setBlock(level, origin.offset( 2, y, -r + 2), riftFrame);
            }
            // Top arch
            for (int x = -2; x <= 2; x++) setBlock(level, origin.offset(x, 8, -r + 2), riftFrame);
            // Inner frame glow (rift stone + sea lanterns in frame perimeter)
            for (int x = -1; x <= 1; x++) setBlock(level, origin.offset(x, 0, -r + 3), seaLan);
            setBlock(level, origin.offset(-1, 8, -r + 3), seaLan);
            setBlock(level, origin.offset( 1, 8, -r + 3), seaLan);

            // === 4 CYAN GLOW PILLARS (one per quadrant) ===
            for (int i = 0; i < 4; i++) {
                double angle = Math.PI * 2 * i / 4 + Math.PI / 4;
                int px = (int) (Math.cos(angle) * (r - 3));
                int pz = (int) (Math.sin(angle) * (r - 3));
                for (int y = 1; y <= 12; y++) {
                    setBlock(level, origin.offset(px, y, pz), (y % 3 == 0) ? seaLan : tpillar);
                }
                setBlock(level, origin.offset(px, 13, pz), qglass);
            }

            // === VOID OBSIDIAN BASE RING ===
            for (int deg = 0; deg < 360; deg += 6) {
                double angle = Math.toRadians(deg);
                int wx = (int) (Math.cos(angle) * (r - 1));
                int wz = (int) (Math.sin(angle) * (r - 1));
                setBlock(level, origin.offset(wx, 1, wz), voidObs);
                setBlock(level, origin.offset(wx, 2, wz), voidObs);
            }

            // === RIFT CRYSTAL DEPOSITS near frame base ===
            for (int x = -3; x <= 3; x++) {
                setBlock(level, origin.offset(x, 1, -r + 4), crystal);
            }

            // Loot chest at the base of the frame (hidden inside)
            placeChest(level, origin.offset(0, 1, -r + 5), "quantumfalcon:chests/echo_rift_gate", rng);
        }

        // ==================================================================
        // BREEDING SANCTUM — radius 32, surface
        // Large roofed hall: quantum stone walls, quantum glass ceiling,
        // 8 nesting alcoves with quantum nest blocks, central incubation ring,
        // genetics research chest with special NBT tags.
        // Ties to FalconGeneticsSystem. Loot: breeding_sanctum.
        // ==================================================================
        private void buildBreedingSanctum(WorldGenLevel level, RandomSource rng) {
            BlockState qstone   = ModBlocks.QUANTUM_STONE.get().defaultBlockState();
            BlockState qblock   = ModBlocks.QUANTUM_BLOCK.get().defaultBlockState();
            BlockState qglass   = ModBlocks.QUANTUM_GLASS.get().defaultBlockState();
            BlockState qglassCy = ModBlocks.QUANTUM_GLASS_CYAN.get().defaultBlockState();
            BlockState qNest    = ModBlocks.QUANTUM_NEST.get().defaultBlockState();
            BlockState incubator= ModBlocks.QUANTUM_INCUBATOR.get().defaultBlockState();
            BlockState seaLan   = Blocks.SEA_LANTERN.defaultBlockState();
            BlockState tpillar  = ModBlocks.TEMPORAL_PILLAR.get().defaultBlockState();
            BlockState eLattice = ModBlocks.ECHO_LATTICE.get().defaultBlockState();
            int r = 32;
            int wallH = 12;

            // === OVAL FLOOR ===
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double normDist = Math.sqrt((double)(x*x)/(r*r) + (double)(z*z)/(r*r));
                    if (normDist <= 1.0) {
                        boolean isCircuit = (Math.abs(x) % 5 == 0 || Math.abs(z) % 5 == 0);
                        setBlock(level, origin.offset(x, 0, z), isCircuit ? qblock : qstone);
                        setBlock(level, origin.offset(x, -1, z), qstone);
                    }
                }
            }

            // === WALL RING ===
            for (int deg = 0; deg < 360; deg += 3) {
                double angle = Math.toRadians(deg);
                double rx = r * Math.abs(Math.cos(angle));
                double rz = r * Math.abs(Math.sin(angle));
                int wx = (int) (Math.cos(angle) * (r - 1));
                int wz = (int) (Math.sin(angle) * (r - 1));
                for (int y = 1; y <= wallH; y++) {
                    BlockState w = (y == wallH || y == 1) ? qblock : qstone;
                    setBlock(level, origin.offset(wx, y, wz), (deg % 15 == 0 && y == wallH / 2) ? seaLan : w);
                }
            }

            // === GLASS CEILING ===
            for (int x = -(r-1); x <= (r-1); x++) {
                for (int z = -(r-1); z <= (r-1); z++) {
                    double normDist = Math.sqrt((double)(x*x)/((r-1)*(r-1)) + (double)(z*z)/((r-1)*(r-1)));
                    if (normDist < 1.0) {
                        boolean isCyan = ((Math.abs(x) + Math.abs(z)) % 8 < 2);
                        setBlock(level, origin.offset(x, wallH + 1, z), isCyan ? qglassCy : qglass);
                    }
                }
            }

            // === 8 NESTING ALCOVES ===
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8;
                int nx = (int) (Math.cos(angle) * (r - 6));
                int nz = (int) (Math.sin(angle) * (r - 6));
                // Alcove floor (3×3)
                for (int ax = -1; ax <= 1; ax++) for (int az = -1; az <= 1; az++) {
                    setBlock(level, origin.offset(nx + ax, 0, nz + az), qblock);
                    setBlock(level, origin.offset(nx + ax, 1, nz + az), qstone);
                }
                // Nest block at center of alcove
                setBlock(level, origin.offset(nx, 2, nz), qNest);
                // Alcove glow
                setBlock(level, origin.offset(nx, 3, nz), seaLan);
            }

            // === CENTRAL INCUBATION RING ===
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2 * i / 12;
                int ix = (int) (Math.cos(angle) * 8);
                int iz = (int) (Math.sin(angle) * 8);
                setBlock(level, origin.offset(ix, 1, iz), tpillar);
                setBlock(level, origin.offset(ix, 2, iz), seaLan);
            }

            // === 4 INTERIOR PILLARS ===
            int[] pillarOffsets = {12, -12};
            for (int po : pillarOffsets) {
                for (int qo : pillarOffsets) {
                    for (int y = 1; y <= wallH; y++)
                        setBlock(level, origin.offset(po, y, qo), y % 3 == 0 ? seaLan : tpillar);
                    setBlock(level, origin.offset(po, wallH + 1, qo), eLattice);
                }
            }

            // === 3 INCUBATORS IN CENTRAL RING ===
            setBlock(level, origin.offset(0, 1, 0), incubator);
            setBlock(level, origin.offset(3, 1, 0), incubator);
            setBlock(level, origin.offset(-3, 1, 0), incubator);

            // Loot chests (genetics-tagged)
            placeChest(level, origin.offset(0, 1, 14), "quantumfalcon:chests/breeding_sanctum", rng);
            placeChest(level, origin.offset(14, 1, 0), "quantumfalcon:chests/breeding_sanctum", rng);
        }

        // ==================================================================
        // ECHO CHAMBER — 56×56×56 hollow sphere (r=28), underground Y=40
        //
        // CONSTRUCTION LOG (concept art blueprint match):
        //   Sphere    : void_bricks shell r=26..28, inner r<26 cleared to AIR
        //   Pillar    : 2×2 crying_obsidian central pillar, full height Y=-26..+26
        //               sea_lantern every 6Y on all 4 column corners
        //   4 alcoves : N/S/E/W cardinal walls — 3-wide × 5-tall arch opening
        //               End Portal Frame lines the arch: bottom + 2 sides + top arch
        //               crying_obsidian arch frame surrounding the End Portal Frames
        //               sea_lantern glow in each alcove at mid-height
        //   Sky windows: quantum_glass panels at Y+14..+22 of the sphere (top quarter)
        //               replacing void_brick shell with glass in upper polar zone
        //   Loot chest: at base of central pillar Y=-24
        // ==================================================================
        private void buildEchoChamber(WorldGenLevel level, RandomSource rng) {
            final BlockState voidBricks  = ModBlocks.VOID_BRICKS.get().defaultBlockState();
            final BlockState qGlass      = ModBlocks.QUANTUM_GLASS.get().defaultBlockState();
            final BlockState qGlCy       = ModBlocks.QUANTUM_GLASS_CYAN.get().defaultBlockState();
            final BlockState cryObs      = Blocks.CRYING_OBSIDIAN.defaultBlockState();
            final BlockState seaLan      = Blocks.SEA_LANTERN.defaultBlockState();
            final BlockState epFrame     = Blocks.END_PORTAL_FRAME.defaultBlockState();
            final int R = 28;

            // ── HOLLOW SPHERE SHELL (void_bricks r=26..28) ──
            // Upper polar cap (Y>14): quantum_glass sky windows
            for (int x = -R; x <= R; x++) {
                for (int y = -R; y <= R; y++) {
                    for (int z = -R; z <= R; z++) {
                        double dist = Math.sqrt((double)(x*x) + (double)(y*y) + (double)(z*z));
                        if (dist <= R && dist >= R - 2) {
                            // Sky windows in upper polar cap (top 35% of sphere)
                            boolean isSkyWindow = (y > 14);
                            setBlock(level, origin.offset(x, y, z),
                                isSkyWindow ? (((x + y) % 3 == 0) ? qGlCy : qGlass) : voidBricks);
                        } else if (dist < R - 2) {
                            put(level, origin.offset(x, y, z),
                                Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }

            // ── 2×2 CENTRAL CRYING OBSIDIAN PILLAR (full height) ──
            for (int y = -R + 2; y <= R - 2; y++) {
                for (int px = 0; px <= 1; px++) {
                    for (int pz = 0; pz <= 1; pz++) {
                        setBlock(level, origin.offset(px, y, pz),
                            (y % 6 == 0) ? seaLan : cryObs);
                    }
                }
            }

            // ── 4 CARDINAL PORTAL ALCOVES (N/S/E/W) ──
            // Each alcove: 3-wide × 5-tall arch opening in sphere wall
            // with End Portal Frames + crying_obsidian arch
            int[][] cardinalDirs = {{0,-1},{0,1},{-1,0},{1,0}};  // N,S,W,E (Z,Z,X,X)
            boolean[] isZ = {true, true, false, false};
            for (int c = 0; c < 4; c++) {
                int dx = cardinalDirs[c][0];
                int dz = cardinalDirs[c][1];
                // Alcove center on sphere wall
                int wallX = dx * (R - 1);
                int wallZ = dz * (R - 1);

                // Clear alcove opening (3 wide × 5 tall)
                int perpX = isZ[c] ? 1 : 0;
                int perpZ = isZ[c] ? 0 : 1;
                for (int side = -1; side <= 1; side++) {
                    for (int h = 0; h <= 4; h++) {
                        int ax = wallX + perpX * side;
                        int az = wallZ + perpZ * side;
                        put(level, origin.offset(ax, h - 2, az),
                            Blocks.AIR.defaultBlockState(), 2);
                    }
                }

                // End Portal Frame arch: bottom row + 2 side columns + top
                for (int side = -1; side <= 1; side++) {
                    int ax = wallX + perpX * side;
                    int az = wallZ + perpZ * side;
                    setBlock(level, origin.offset(ax, -2, az), epFrame);   // bottom
                    setBlock(level, origin.offset(ax,  2, az), epFrame);   // top
                }
                setBlock(level, origin.offset(wallX - perpX, -1, wallZ - perpZ), epFrame);
                setBlock(level, origin.offset(wallX - perpX,  0, wallZ - perpZ), epFrame);
                setBlock(level, origin.offset(wallX - perpX,  1, wallZ - perpZ), epFrame);
                setBlock(level, origin.offset(wallX + perpX, -1, wallZ + perpZ), epFrame);
                setBlock(level, origin.offset(wallX + perpX,  0, wallZ + perpZ), epFrame);
                setBlock(level, origin.offset(wallX + perpX,  1, wallZ + perpZ), epFrame);

                // Crying obsidian arch frame around the alcove
                for (int side = -2; side <= 2; side++) {
                    setBlock(level, origin.offset(wallX + perpX * side, 3, wallZ + perpZ * side), cryObs);
                    setBlock(level, origin.offset(wallX + perpX * side, -3, wallZ + perpZ * side), cryObs);
                }
                // Sea lantern glow inside alcove
                setBlock(level, origin.offset(wallX, 0, wallZ), seaLan);
            }

            // Loot chest at center floor
            placeChest(level, origin.offset(0, -(R - 4), 0),
                "quantumfalcon:chests/echo_chamber", rng);
        }

        // ==================================================================
        // CRYSTAL CAVERN — 40×40×40 hollow sphere (r=20), underground Y=20
        //
        // CONSTRUCTION LOG (concept art blueprint match):
        //   Sphere    : crystallized_stone shell r=18..20, inner r<18 cleared
        //               2-block solid floor at bottom (Y=-19..-18) for standing
        //   Pillar    : 2×2 sea_lantern glow pillar, full height Y=-17..+17
        //               (blueprint labels it as central "glow pillar")
        //   Crystal nodes: 60 random wall nodes (spherical distribution)
        //               25% sea_lantern, 75% crystal_cluster
        //               Some nodes (33%) are 2-3 blocks tall spikes
        //   Ore veins : 20 quantum_ore veins embedded in sphere walls
        //               Each vein: 4×4 patch of ore at wall positions (r≈18)
        //   Loot chest: at floor center Y=-17
        // ==================================================================
        private void buildCrystalCavern(WorldGenLevel level, RandomSource rng) {
            final BlockState crystStone = ModBlocks.CRYSTALLIZED_STONE.get().defaultBlockState();
            final BlockState cluster    = ModBlocks.CRYSTAL_CLUSTER.get().defaultBlockState();
            final BlockState seaLan     = Blocks.SEA_LANTERN.defaultBlockState();
            final BlockState quantOre   = ModBlocks.QUANTUM_ORE.get().defaultBlockState();
            final int R = 20;

            // ── HOLLOW CRYSTALLIZED STONE SPHERE (shell r=18..20) ──
            for (int x = -R; x <= R; x++) {
                for (int y = -R; y <= R; y++) {
                    for (int z = -R; z <= R; z++) {
                        double dist = Math.sqrt((double)(x*x) + (double)(y*y) + (double)(z*z));
                        if (dist <= R && dist >= R - 2) {
                            setBlock(level, origin.offset(x, y, z), crystStone);
                        } else if (dist < R - 2) {
                            put(level, origin.offset(x, y, z),
                                Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }

            // ── SOLID FLOOR at bottom (Y=-19..-18) — landing platform ──
            for (int x = -8; x <= 8; x++) {
                for (int z = -8; z <= 8; z++) {
                    if (Math.sqrt(x * x + z * z) <= 8) {
                        setBlock(level, origin.offset(x, -19, z), crystStone);
                        setBlock(level, origin.offset(x, -18, z), crystStone);
                    }
                }
            }

            // ── 2×2 CENTRAL GLOW PILLAR (sea_lantern, full sphere height) ──
            for (int y = -R + 3; y <= R - 3; y++) {
                for (int px = 0; px <= 1; px++) {
                    for (int pz = 0; pz <= 1; pz++) {
                        setBlock(level, origin.offset(px, y, pz), seaLan);
                    }
                }
            }

            // ── 60 CRYSTAL/LANTERN NODES on inner sphere wall ──
            for (int i = 0; i < 60; i++) {
                double theta = rng.nextDouble() * Math.PI * 2;
                double phi   = rng.nextDouble() * Math.PI;
                double wallR = R - 2 + rng.nextDouble() * 1.5;
                int cx = (int) Math.round(Math.sin(phi) * Math.cos(theta) * wallR);
                int cy = (int) Math.round(Math.cos(phi) * wallR);
                int cz = (int) Math.round(Math.sin(phi) * Math.sin(theta) * wallR);

                boolean isLantern = rng.nextFloat() < 0.25f;
                setBlock(level, origin.offset(cx, cy, cz), isLantern ? seaLan : cluster);

                // Tall crystal spike (2-3 blocks) for 33% of nodes — grown inward
                if (!isLantern && rng.nextFloat() < 0.33f) {
                    // Grow 1-2 more blocks toward center
                    double inwardScale = (R - 3.0) / wallR;
                    for (int spike = 1; spike <= 1 + rng.nextInt(2); spike++) {
                        int sx = (int) Math.round(cx * (inwardScale - spike * 0.08));
                        int sy = (int) Math.round(cy * (inwardScale - spike * 0.08));
                        int sz = (int) Math.round(cz * (inwardScale - spike * 0.08));
                        setBlock(level, origin.offset(sx, sy, sz), cluster);
                    }
                }
            }

            // ── 20 QUANTUM ORE VEINS embedded in walls ──
            for (int i = 0; i < 20; i++) {
                double theta = rng.nextDouble() * Math.PI * 2;
                double phi   = Math.PI * 0.2 + rng.nextDouble() * Math.PI * 0.6; // avoid poles
                double wallR = R - 1;
                int vx = (int) Math.round(Math.sin(phi) * Math.cos(theta) * wallR);
                int vy = (int) Math.round(Math.cos(phi) * wallR);
                int vz = (int) Math.round(Math.sin(phi) * Math.sin(theta) * wallR);

                // 2×2 to 3×3 ore vein patch
                int vSize = 1 + rng.nextInt(2);
                for (int ox = -vSize; ox <= vSize; ox++) {
                    for (int oz = -vSize; oz <= vSize; oz++) {
                        if (rng.nextFloat() < 0.65f) {
                            setBlock(level, origin.offset(vx + ox, vy, vz + oz), quantOre);
                        }
                    }
                }
            }

            // Loot chest at floor center
            placeChest(level, origin.offset(0, -17, 2),
                "quantumfalcon:chests/crystal_cavern", rng);
        }

        // ==================================================================
        // DATA ARCHIVE — radius 20, underground Y=30
        // Rectangular vault: quantum stone walls, glass ceiling, bookshelves,
        // central data beacon pillar, records chests on both ends.
        // Inspired by BoMD lich_tower interior scale.
        // Story: CHAPTER_2 zone. Loot: data_archive.
        // ==================================================================
        private void buildDataArchive(WorldGenLevel level, RandomSource rng) {
            BlockState qstone   = ModBlocks.QUANTUM_STONE.get().defaultBlockState();
            BlockState qbricks  = ModBlocks.QUANTUM_BRICKS.get().defaultBlockState();
            BlockState qglass   = ModBlocks.QUANTUM_GLASS_CYAN.get().defaultBlockState();
            BlockState seaLan   = Blocks.SEA_LANTERN.defaultBlockState();
            BlockState bookshelf= Blocks.BOOKSHELF.defaultBlockState();
            BlockState beacon   = Blocks.BEACON.defaultBlockState();
            BlockState eLattice = ModBlocks.ECHO_LATTICE.get().defaultBlockState();
            BlockState tpillar  = ModBlocks.TEMPORAL_PILLAR.get().defaultBlockState();

            // Hall: 40×10 footprint, 10 blocks high
            int halfL = 20; int halfW = 8; int hallH = 10;

            // Floor
            for (int x = -halfL; x <= halfL; x++) {
                for (int z = -halfW; z <= halfW; z++) {
                    boolean isGrid = (Math.abs(x) % 4 == 0 || Math.abs(z) % 4 == 0);
                    setBlock(level, origin.offset(x, 0, z), isGrid ? qbricks : qstone);
                    setBlock(level, origin.offset(x, -1, z), qstone);
                }
            }

            // Walls (N+S, E+W, closing the box)
            for (int x = -halfL; x <= halfL; x++) {
                for (int y = 1; y <= hallH; y++) {
                    setBlock(level, origin.offset(x, y, -halfW), (y == hallH) ? qbricks : qstone);
                    setBlock(level, origin.offset(x, y,  halfW), (y == hallH) ? qbricks : qstone);
                }
            }
            for (int z = -halfW; z <= halfW; z++) {
                for (int y = 1; y <= hallH; y++) {
                    setBlock(level, origin.offset(-halfL, y, z), (y == hallH) ? qbricks : qstone);
                    setBlock(level, origin.offset( halfL, y, z), (y == hallH) ? qbricks : qstone);
                }
            }

            // Glass ceiling
            for (int x = -(halfL - 1); x <= (halfL - 1); x++) {
                for (int z = -(halfW - 1); z <= (halfW - 1); z++) {
                    setBlock(level, origin.offset(x, hallH + 1, z), qglass);
                }
            }

            // Bookshelves on N and S walls (inner side), 3 high
            for (int x = -halfL + 2; x <= halfL - 2; x++) {
                for (int y = 1; y <= 3; y++) {
                    if (x % 3 != 0) {  // Leave doorway gaps every 3 blocks
                        setBlock(level, origin.offset(x, y, -(halfW - 1)), bookshelf);
                        setBlock(level, origin.offset(x, y,  (halfW - 1)), bookshelf);
                    }
                }
                // Sea lanterns in the gaps
                if (x % 3 == 0) {
                    setBlock(level, origin.offset(x, 2, -(halfW - 1)), seaLan);
                    setBlock(level, origin.offset(x, 2,  (halfW - 1)), seaLan);
                }
            }

            // 4 temporal pillars at quarter-points
            for (int qx : new int[]{-halfL/2, halfL/2}) {
                for (int y = 1; y <= hallH; y++)
                    setBlock(level, origin.offset(qx, y, 0), (y % 3 == 0) ? seaLan : tpillar);
            }

            // Central data beacon + echo lattice pillar
            setBlock(level, origin.above(0), beacon);
            for (int y = 1; y <= hallH - 1; y++)
                setBlock(level, origin.above(y), (y % 2 == 0) ? seaLan : eLattice);

            // Two loot chests at east and west ends
            placeChest(level, origin.offset(halfL - 1, 1, 0), "quantumfalcon:chests/data_archive", rng);
            placeChest(level, origin.offset(-(halfL - 1), 1, 0), "quantumfalcon:chests/data_archive", rng);
        }

        // ==================================================================
        // QUANTUM OBSERVATORY — radius 8, surface, height 64
        // Tall signal tower: octagonal quantum stone base (r=8), rising 64 blocks,
        // glass dome at top (r=5), rift crystal antenna array, telescope gear.
        // Inspired by BoMD lich_tower scale (100-block spacing, rare).
        // Story: CHAPTER_8 zone. Loot: quantum_observatory.
        // ==================================================================
        private void buildQuantumObservatory(WorldGenLevel level, RandomSource rng) {
            BlockState qstone   = ModBlocks.QUANTUM_STONE.get().defaultBlockState();
            BlockState qbricks  = ModBlocks.QUANTUM_BRICKS.get().defaultBlockState();
            BlockState qglass   = ModBlocks.QUANTUM_GLASS_CYAN.get().defaultBlockState();
            BlockState qblock   = ModBlocks.QUANTUM_BLOCK.get().defaultBlockState();
            BlockState crystal  = ModBlocks.CRYSTAL_CLUSTER.get().defaultBlockState();
            BlockState seaLan   = Blocks.SEA_LANTERN.defaultBlockState();
            BlockState eLattice = ModBlocks.ECHO_LATTICE.get().defaultBlockState();
            BlockState tpillar  = ModBlocks.TEMPORAL_PILLAR.get().defaultBlockState();
            int baseR = 8; int towerH = 48; int domeR = 5;

            // === OCTAGONAL BASE (radius 8, 2 blocks high) ===
            for (int x = -baseR; x <= baseR; x++) {
                for (int z = -baseR; z <= baseR; z++) {
                    if (Math.abs(x) + Math.abs(z) <= baseR + 3 && Math.sqrt(x*x+z*z) <= baseR) {
                        setBlock(level, origin.offset(x, 0, z), qbricks);
                        setBlock(level, origin.offset(x, -1, z), qstone);
                        // Ground floor circuit
                        if (Math.abs(x) <= 1 || Math.abs(z) <= 1)
                            setBlock(level, origin.offset(x, 0, z), ModBlocks.QUANTUM_TECH_CYAN.get().defaultBlockState());
                    }
                }
            }

            // === OCTAGONAL OUTER WALL (first 12 floors) ===
            for (int y = 1; y <= 12; y++) {
                for (int deg = 0; deg < 360; deg += 5) {
                    double angle = Math.toRadians(deg);
                    // Octagonal approximation
                    double wallR = baseR * (1 - 0.3 * Math.abs(Math.sin(4 * angle)));
                    int wx = (int) (Math.cos(angle) * wallR);
                    int wz = (int) (Math.sin(angle) * wallR);
                    setBlock(level, origin.offset(wx, y, wz), (y % 6 == 0) ? seaLan : qbricks);
                }
            }

            // === HOLLOW TOWER SHAFT (square, 3×3 interior, 4×4 exterior) ===
            for (int y = 1; y <= towerH; y++) {
                // Solid walls (outside ring)
                for (int x = -2; x <= 2; x++) {
                    setBlock(level, origin.offset(-2, y, x), (y % 8 == 0) ? seaLan : qstone);
                    setBlock(level, origin.offset( 2, y, x), (y % 8 == 0) ? seaLan : qstone);
                    setBlock(level, origin.offset(x, y, -2), (y % 8 == 0) ? seaLan : qstone);
                    setBlock(level, origin.offset(x, y,  2), (y % 8 == 0) ? seaLan : qstone);
                }
                // Interior (clear + temporal pillar center)
                put(level, origin.offset(0, y, 0), Blocks.AIR.defaultBlockState(), 2);
                if (y % 12 == 0) setBlock(level, origin.above(y), eLattice);
                else if (y % 4 == 0) setBlock(level, origin.above(y), seaLan);
            }

            // === OBSERVATORY DOME AT TOP (sphere cap, r=5) ===
            BlockPos domeBase = origin.above(towerH);
            for (int x = -domeR; x <= domeR; x++) {
                for (int y = 0; y <= domeR; y++) {
                    for (int z = -domeR; z <= domeR; z++) {
                        double dist = Math.sqrt(x * x + y * y + z * z);
                        if (dist <= domeR && dist >= domeR - 1) {
                            setBlock(level, domeBase.offset(x, y, z),
                                (y > 2 && Math.abs(x) < 3 && Math.abs(z) < 3) ? qglass : qblock);
                        }
                    }
                }
            }
            // Crystal antenna array at the very top
            setBlock(level, domeBase.above(domeR + 1), tpillar);
            setBlock(level, domeBase.above(domeR + 2), crystal);
            setBlock(level, domeBase.above(domeR + 3), eLattice);
            setBlock(level, domeBase.above(domeR + 4), seaLan);

            // === 4 LIGHTNING ROD ARMS FROM DOME PERIMETER ===
            for (int i = 0; i < 4; i++) {
                double angle = Math.PI * 2 * i / 4;
                int ax = (int) (Math.cos(angle) * (domeR - 1));
                int az = (int) (Math.sin(angle) * (domeR - 1));
                setBlock(level, domeBase.offset(ax, 2, az), crystal);
                setBlock(level, domeBase.offset(ax, 3, az), seaLan);
            }

            // === TELESCOPE INSTRUMENT ON DOME FLOOR ===
            setBlock(level, domeBase.above(1), qblock);
            setBlock(level, domeBase.offset(0, 2, 1), eLattice);
            setBlock(level, domeBase.offset(0, 3, 2), crystal);

            // === LOOT CHEST at dome level ===
            placeChest(level, domeBase.offset(3, 1, 0), "quantumfalcon:chests/quantum_observatory", rng);

            // === FLOOR PADS at base (platform around tower) ===
            for (int deg = 0; deg < 360; deg += 45) {
                double angle = Math.toRadians(deg);
                int px = (int) (Math.cos(angle) * (baseR + 3));
                int pz = (int) (Math.sin(angle) * (baseR + 3));
                for (int x = -1; x <= 1; x++)
                    for (int z = -1; z <= 1; z++) {
                        setBlock(level, origin.offset(px + x, 0, pz + z), qbricks);
                        setBlock(level, origin.offset(px + x, -1, pz + z), qstone);
                    }
                setBlock(level, origin.offset(px, 1, pz), seaLan);
            }
        }

        // ==================================================================
        // VOID CITADEL — radius 28, underground Y=50
        // Imposing void fortress: outer curtain wall (6 towers), inner keep,
        // obsidian throne room, void boss spawners.
        // Inspired by BoMD obsidilith_arena scale (64-chunk spacing, rare).
        // Loot: void_citadel (endgame drops).
        // ==================================================================
        private void buildVoidCitadel(WorldGenLevel level, RandomSource rng) {
            BlockState voidBricks  = ModBlocks.VOID_BRICKS.get().defaultBlockState();
            BlockState voidObs     = ModBlocks.VOID_OBSIDIAN.get().defaultBlockState();
            BlockState voidStBricks= ModBlocks.VOID_STONE_BRICKS.get().defaultBlockState();
            BlockState cryObsidian = Blocks.CRYING_OBSIDIAN.defaultBlockState();
            BlockState seaLan      = Blocks.SEA_LANTERN.defaultBlockState();
            BlockState tpillar     = ModBlocks.TEMPORAL_PILLAR.get().defaultBlockState();
            BlockState qglass      = ModBlocks.QUANTUM_GLASS_PURPLE.get().defaultBlockState();
            BlockState eLattice    = ModBlocks.ECHO_LATTICE.get().defaultBlockState();
            int r = 28; int wallH = 18;

            // === CLEAR THE SPACE ===
            for (int x = -r - 2; x <= r + 2; x++) {
                for (int y = -2; y <= wallH + 4; y++) {
                    for (int z = -r - 2; z <= r + 2; z++) {
                        if (Math.sqrt(x * x + z * z) <= r + 1) {
                            put(level, origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }

            // === FLOOR ===
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.sqrt(x * x + z * z) <= r) {
                        setBlock(level, origin.offset(x, -1, z), voidBricks);
                        setBlock(level, origin.offset(x, 0, z),
                            ((Math.abs(x) + Math.abs(z)) % 5 == 0) ? cryObsidian : voidStBricks);
                    }
                }
            }

            // === OUTER CURTAIN WALL ===
            for (int deg = 0; deg < 360; deg += 2) {
                double angle = Math.toRadians(deg);
                int wx = (int) (Math.cos(angle) * (r - 2));
                int wz = (int) (Math.sin(angle) * (r - 2));
                for (int y = 1; y <= wallH; y++) {
                    setBlock(level, origin.offset(wx, y, wz), voidBricks);
                }
                // Crenellations at top
                if (deg % 6 == 0) setBlock(level, origin.offset(wx, wallH + 1, wz), voidObs);
            }

            // === 6 OUTER TOWERS ===
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * 2 * i / 6;
                int tx = (int) (Math.cos(angle) * (r - 4));
                int tz = (int) (Math.sin(angle) * (r - 4));
                // Tower footprint 5×5, 24 high
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        for (int y = 1; y <= wallH + 6; y++) {
                            boolean isWall = (Math.abs(x) == 2 || Math.abs(z) == 2);
                            if (isWall)
                                setBlock(level, origin.offset(tx + x, y, tz + z),
                                    (y % 6 == 0) ? seaLan : voidBricks);
                            else if (y > wallH)
                                put(level, origin.offset(tx + x, y, tz + z), Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
                setBlock(level, origin.offset(tx, wallH + 7, tz), seaLan);   // Beacon cap
                setBlock(level, origin.offset(tx, wallH + 6, tz), tpillar);
            }

            // === INNER KEEP (14×14 footprint, 26 high) ===
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {
                    // Inner keep floor
                    setBlock(level, origin.offset(x, 0, z), voidObs);
                    // Keep walls
                    boolean isWall = (Math.abs(x) == 7 || Math.abs(z) == 7);
                    for (int y = 1; y <= wallH + 8; y++) {
                        if (isWall)
                            setBlock(level, origin.offset(x, y, z),
                                (y % 5 == 0) ? cryObsidian : voidStBricks);
                    }
                }
            }

            // Keep gateway (4-wide opening on south face)
            for (int x = -2; x <= 2; x++) {
                for (int y = 1; y <= 6; y++) {
                    put(level, origin.offset(x, y, 7), Blocks.AIR.defaultBlockState(), 2);
                    put(level, origin.offset(x, y, -7), Blocks.AIR.defaultBlockState(), 2);
                }
            }

            // === THRONE ROOM — central raised dais ===
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    setBlock(level, origin.offset(x, 1, z), voidObs);
                    setBlock(level, origin.offset(x, 2, z), voidObs);
                }
            }
            // Throne: echo lattice chair
            setBlock(level, origin.offset(0, 3, 0), eLattice);
            setBlock(level, origin.offset(0, 4, 0), eLattice);
            setBlock(level, origin.offset(0, 5, 0), seaLan);

            // Purple glass windows in keep walls
            for (int y = wallH / 2; y <= wallH / 2 + 4; y++) {
                setBlock(level, origin.offset(7, y, 0), qglass);
                setBlock(level, origin.offset(-7, y, 0), qglass);
                setBlock(level, origin.offset(0, y, 7), qglass);
                setBlock(level, origin.offset(0, y, -7), qglass);
            }

            // Loot chests in throne room + outer courtyard. the throne-room pair stand on the keep floor beside the dais
            // (they used to hang at y=3, two blocks above the floor, with air on all six sides)
            placeChest(level, origin.offset(5, 1, 0), "quantumfalcon:chests/void_citadel", rng);
            placeChest(level, origin.offset(-5, 1, 0), "quantumfalcon:chests/void_citadel", rng);
            placeChest(level, origin.offset(r - 8, 1, 0), "quantumfalcon:chests/void_citadel", rng);
        }

        // ==================================================================
        // Helpers
        // ==================================================================

        private BoundingBox clip;

        private void setBlock(WorldGenLevel level, BlockPos pos, BlockState state) {
            put(level, pos, state, 3);
        }

        private boolean put(WorldGenLevel level, BlockPos pos, BlockState state, int flags) {
            if (clip != null && !clip.isInside(pos)) return false;
            return level.setBlock(pos, state, flags);
        }

        private void placeChest(WorldGenLevel level, BlockPos pos, String lootTable, RandomSource rng) {
            if (!put(level, pos, Blocks.CHEST.defaultBlockState(), 3)) return;
            if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest) {
                ResourceKey<LootTable> key = ResourceKey.create(
                    Registries.LOOT_TABLE, ResourceLocation.parse(lootTable));
                chest.setLootTable(key, pos.asLong());
            }
        }
    }
}
