package com.matt.quantumfalcon.worldgen.structure;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.arena.ArenaStructurePlacer;
import com.matt.quantumfalcon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Programmatic arena builder — all 4 boss arenas matched to concept art.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * CONSTRUCTION LOG — Concept Art Analysis (Sessions 1 & 2)
 * ══════════════════════════════════════════════════════════════════════════
 *
 * ① QUANTUM GUARDIAN ARENA  (concept art: 128×57×128 → fallback r=48, height=57)
 * ─────────────────────────────────────────────────────────────────────────
 * Floor       : quantum_stone base, quantum_tech_cyan circuit grid every 8 blocks
 *               + quantum_block edge ring (r=46–48)
 *               + bright cross (+) accent through center (quantum_block lines)
 * Rim parapet : quantum_block Y+1 (solid ring), quantum_glass_cyan Y+2 (every 3°)
 *               + sea_lantern beacon every 15° at Y+2
 * 4 pillars   : NE/NW/SE/SW at 45°, r=34 (70% of 48), 3×3 shaft, 15 high
 *               + sea_lantern accent every 5Y on center column
 *               + 3×3 sea_lantern cap at top + 8 quantum_glass_cyan crown at Y+16
 * Pedestal    : 5×5 quantum_block Y+1, 3×3 Y+2, sea_lantern 4 cardinals at Y+1
 *               + quantum_lamp boss-spawn marker at Y+3
 * Glass ring  : quantum_glass decorative ring at r=43 (r-5), Y+1
 *
 * ② VOID HORROR ARENA  (concept art: 128×59×121 → fallback r=40, height=59)
 * ─────────────────────────────────────────────────────────────────────────
 * Platform    : OCTAGONAL shape (|x| + |z| ≤ r*1.42, both |x| ≤ r, |z| ≤ r)
 *               void_bricks main, rift_stone edge, crying_obsidian scatter 10%
 * Void center : r=5 cleared to Y-15, surrounded by quantum_glass_cyan safety ring at Y+0
 *               (glowing cyan ring visible in close-up concept art)
 * 6 spires    : void_bricks shaft, crystal_cluster purple apex + sea_lantern glow
 *               Positions (from blueprint): r varies 17–23, irregular angle offsets
 *               Spire 1: NNW r=21 | Spire 2: NNE r=19 | Spire 3: ENE r=22
 *               Spire 4: SSE r=20 | Spire 5: SSW r=23 | Spire 6: WSW r=18
 * 8 platforms : 3×3 void_bricks, r=12–19, Y-2 to Y+3 (alternating), sea_lantern 4 of 8
 * Outer ring  : crystal_cluster posts every 20° at r=r-2, Y+1 to Y+3
 *
 * ③ ENTANGLED TWINS ARENA  (concept art: 112×40×112 → fallback r=26/plat, sep=30)
 * ─────────────────────────────────────────────────────────────────────────
 * 2 platforms : crystallized_stone floor, quantum_tech_purple magenta circuit lines
 *               radiating from center (spoke pattern every 45°) + grid every 6 blocks
 *               quantum_glass_cyan edge ring (r=24–26) + rim parapet 2 high
 * Bridge      : 3 wide, HEIGHT 8 at peak (sin arch), temporal_pillar floor + sides
 *               3 FULL ARCH FRAMES at 25%, 50%, 75% of bridge length:
 *               each frame = 2× temporal_pillar posts + quantum_glass arch cap
 *               + crystal_cluster crown at arch top + sea_lantern glow at bridge
 * 4 pillars   : per platform, 45° NE/NW/SE/SW, r=17, 2×2 crystStone base (3 high)
 *               → temporal_pillar shaft, 18 high total, crystal cluster buds at Y14–18
 *               + sea_lantern cap + crystal_cluster apex at Y+19
 * Boss pedestal: on each platform — 5×5 base, 3×3 second, quantum_lamp at Y+3
 * Crystal ring : r=22 on each platform, crystal_cluster posts every 20°
 *
 * ④ SINGULARITY SOVEREIGN ARENA  (concept art: 128×38×128 → fallback r=48, height=38)
 * ─────────────────────────────────────────────────────────────────────────
 * Platform    : TRUE DONUT RING — solid from r=12 to r=48, void cleared inside r=12
 *               crystallized_stone base, quantum_block spiral arms (4-arm pattern)
 *               quantum_tech_cyan accent ring at r=17 (inner glow ring visible in concept art)
 * Void center : r=12 cleared Y-15 to Y+5 (larger cleared zone than other arenas)
 * Outer rim   : crying_obsidian + quantum_glass_cyan alternating at r=46–47
 *               + small 1×1 temporal_pillar decorative posts (6 high) between main pillars
 * 8 pillars   : at r=36 (75% of 48), 5×5 base → 3×3 mid → 1×1 top, 25 high
 *               sea_lantern accent every 6Y, end_rod apex ×2, crystal cluster crown
 * Inner ring  : r=17, 24 crystal_cluster + sea_lantern alternating (dense ring from blueprint)
 * Outer ring  : quantum_glass at r=43, every 8°
 *
 * ══════════════════════════════════════════════════════════════════════════
 */
public class ArenaTemplateBuilder {

    private static final String ARENA_QUANTUM_GUARDIAN       = "quantum_guardian_arena";
    private static final String ARENA_VOID_HORROR             = "void_horror_arena";
    private static final String ARENA_ENTANGLED_TWINS         = "entangled_twins_arena";
    private static final String ARENA_SINGULARITY_SOVEREIGN   = "singularity_sovereign_arena";

    // =====================================================================
    // Public API
    // =====================================================================

    public static void buildArena(ServerLevel level, BlockPos center, BossArenaStructure.ArenaType type) {
        QuantumFalcon.LOGGER.debug("Building {} arena at {}", type.name(), center);
        String structureName = switch (type) {
            case QUANTUM_GUARDIAN      -> ARENA_QUANTUM_GUARDIAN;
            case VOID_HORROR           -> ARENA_VOID_HORROR;
            case ENTANGLED_TWINS       -> ARENA_ENTANGLED_TWINS;
            case SINGULARITY_SOVEREIGN -> ARENA_SINGULARITY_SOVEREIGN;
        };
        // Stage 4 tidy-up: command/test/summon arenas use the same ArenaDesign as natural worldgen (the old JSON and
        // programmatic builders below are kept only as a fallback if the design build throws).
        int placed;
        try { ArenaDesign.buildInWorld(level, center, type); placed = 1; }
        catch (RuntimeException ex) { QuantumFalcon.LOGGER.warn("ArenaDesign build failed, using the arena snapshot: {}", ex.toString()); placed = ArenaStructurePlacer.placeArena(level, center, structureName); }
        if (placed <= 0) {
            QuantumFalcon.LOGGER.info("Structure JSON not loaded — using programmatic builder");
            switch (type) {
                case QUANTUM_GUARDIAN      -> buildGuardianArena(level, center);
                case VOID_HORROR           -> buildVoidHorrorArena(level, center);
                case ENTANGLED_TWINS       -> buildEntangledTwinsArena(level, center);
                case SINGULARITY_SOVEREIGN -> buildSovereignArena(level, center);
            }
        }
        ArenaMechanics.registerArena(level, center, type);
    }

    public static ArenaStructurePlacer.ArenaPlacementTask buildArenaBatch(
            ServerLevel level, BlockPos center, BossArenaStructure.ArenaType type, int blocksPerTick) {
        String structureName = switch (type) {
            case QUANTUM_GUARDIAN      -> ARENA_QUANTUM_GUARDIAN;
            case VOID_HORROR           -> ARENA_VOID_HORROR;
            case ENTANGLED_TWINS       -> ARENA_ENTANGLED_TWINS;
            case SINGULARITY_SOVEREIGN -> ARENA_SINGULARITY_SOVEREIGN;
        };
        return ArenaStructurePlacer.createBatchPlacement(level, center, structureName, blocksPerTick);
    }

    // =====================================================================
    // ① QUANTUM GUARDIAN ARENA
    //
    //   r=48 (programmatic fallback for 128×128 JSON)
    //   Circular platform | quantum_tech_cyan circuit grid | 4 pillars at 45°
    //   Central 5×5 pedestal | quantum glass edge ring
    //
    //   CONSTRUCTION LOG:
    //   Floor     X=-48..48, Y=0, Z=-48..48  (r=48 circle)
    //   Circuit   X%8==0 || Z%8==0  → quantum_tech_cyan
    //   Cross     X==0 || Z==0  → quantum_block (brighter cross accent)
    //   Edge      r=46..48  → quantum_block
    //   Rim Y+1   r≈47 ring  → quantum_block  Y+2 → quantum_glass_cyan / sea_lantern@15°
    //   Pillar1   NE: center+(+24,0,+24) Y+0..15  (cos45°*34=24)
    //   Pillar2   NW: center+(-24,0,+24)
    //   Pillar3   SW: center+(-24,0,-24)
    //   Pillar4   SE: center+(+24,0,-24)
    //   Pedestal  center Y+1 (5×5), Y+2 (3×3), quantum_lamp at Y+3
    //   GlassRing r=43, Y+1
    // =====================================================================

    private static void buildGuardianArena(ServerLevel level, BlockPos center) {
        final BlockState qStone   = ModBlocks.QUANTUM_STONE.get().defaultBlockState();
        final BlockState qBlock   = ModBlocks.QUANTUM_BLOCK.get().defaultBlockState();
        final BlockState qGlass   = ModBlocks.QUANTUM_GLASS.get().defaultBlockState();
        final BlockState qGlassCy = ModBlocks.QUANTUM_GLASS_CYAN.get().defaultBlockState();
        final BlockState qTech    = ModBlocks.QUANTUM_TECH_CYAN.get().defaultBlockState();
        final BlockState qLamp    = ModBlocks.QUANTUM_LAMP.get().defaultBlockState();
        final BlockState tPillar  = ModBlocks.TEMPORAL_PILLAR.get().defaultBlockState();
        final BlockState seaLan   = Blocks.SEA_LANTERN.defaultBlockState();
        final int R = 48;
        final int pillarR = 34;   // 70% of R, at 45° → ~24 each axis

        // ── FLOOR: circuit grid + cross accent ──
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > R) continue;
                BlockPos pos = center.offset(x, 0, z);
                boolean isCross   = (x == 0 || z == 0);
                boolean isCircuit = (x % 8 == 0 || z % 8 == 0);
                boolean isEdge    = dist > R - 3;
                BlockState floor = isEdge ? qBlock
                    : isCross      ? qBlock
                    : isCircuit    ? qTech
                    : qStone;
                level.setBlock(pos, floor, 3);
                level.setBlock(pos.below(),  qStone, 3);
                level.setBlock(pos.below(2), qStone, 3);
            }
        }

        // ── OUTER RIM PARAPET (Y+1 solid, Y+2 glass/lantern) ──
        for (int deg = 0; deg < 360; deg += 3) {
            double angle = Math.toRadians(deg);
            int wx = center.getX() + (int) (Math.cos(angle) * (R - 1));
            int wz = center.getZ() + (int) (Math.sin(angle) * (R - 1));
            level.setBlock(new BlockPos(wx, center.getY() + 1, wz), qBlock, 3);
            boolean isBeacon = deg % 15 == 0;
            level.setBlock(new BlockPos(wx, center.getY() + 2, wz),
                isBeacon ? seaLan : qGlassCy, 3);
        }

        // ── 4 ENERGY PILLARS at 45° NE/NW/SE/SW, r=34 ──
        // Pillar coords: cos(45°)*34 ≈ 24.0 → (±24, ±24)
        int[][] pillarXZ = { {24, 24}, {-24, 24}, {-24, -24}, {24, -24} };
        for (int[] pxz : pillarXZ) {
            BlockPos pBase = center.offset(pxz[0], 0, pxz[1]);
            buildGuardianPillar(level, pBase, 15, qBlock, tPillar, seaLan, qGlassCy);
        }

        // ── DECORATIVE QUANTUM GLASS RING at r=43 ──
        for (int deg = 0; deg < 360; deg += 8) {
            double angle = Math.toRadians(deg);
            int rx = center.getX() + (int) (Math.cos(angle) * 43);
            int rz = center.getZ() + (int) (Math.sin(angle) * 43);
            level.setBlock(new BlockPos(rx, center.getY() + 1, rz), qGlass, 3);
        }

        // ── CENTRAL BOSS PEDESTAL (5×5 base, 3×3 second, quantum_lamp at Y+3) ──
        // 5×5 base at Y+1
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            level.setBlock(center.offset(x, 1, z), qBlock, 3);
        // Sea lantern ring at 4 cardinal points on base
        for (int[] c4 : new int[][]{{2,0},{-2,0},{0,2},{0,-2}})
            level.setBlock(center.offset(c4[0], 1, c4[1]), seaLan, 3);
        // 3×3 raised platform at Y+2
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            level.setBlock(center.offset(x, 2, z), qBlock, 3);
        // Quantum lamp boss-spawn marker at Y+3
        level.setBlock(center.above(3), qLamp, 3);
    }

    /**
     * Guardian energy pillar:
     * 3×3 shaft (outer=qBlock, inner=tPillar core), 15 high.
     * sea_lantern every 5Y on center column, 3×3 sea_lantern cap.
     * 8-point quantum_glass_cyan floating crown ring at Y+16.
     */
    private static void buildGuardianPillar(ServerLevel level, BlockPos base, int height,
                                             BlockState outer, BlockState inner,
                                             BlockState cap, BlockState crown) {
        // 3×3 shaft
        for (int y = 0; y < height; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    boolean isCore = (x == 0 && z == 0);
                    BlockState block = isCore
                        ? ((y % 5 == 0) ? cap : inner)
                        : outer;
                    level.setBlock(base.offset(x, y, z), block, 3);
                }
            }
        }
        // 3×3 sea lantern cap at top
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            level.setBlock(base.offset(x, height, z), cap, 3);
        // 8-point floating crown ring at Y+height+1
        for (int deg = 0; deg < 360; deg += 45) {
            double angle = Math.toRadians(deg);
            int cx = base.getX() + (int) Math.round(Math.cos(angle) * 2);
            int cz = base.getZ() + (int) Math.round(Math.sin(angle) * 2);
            level.setBlock(new BlockPos(cx, base.getY() + height + 1, cz), crown, 3);
        }
    }

    // =====================================================================
    // ② VOID HORROR ARENA
    //
    //   r=40 (programmatic fallback for 128×59×121 JSON)
    //   OCTAGONAL platform | 4-block void center | cyan safety ring
    //   6 twisted spires | 8 grappling platforms | crying obsidian accents
    //
    //   CONSTRUCTION LOG:
    //   Octagon   |x|+|z| ≤ r*1.42  (diamond clamp → octagonal visual)
    //             AND |x| ≤ r AND |z| ≤ r
    //             void center skip: dist ≤ 5
    //   Safety    r=6 ring around void at Y+0: quantum_glass_cyan
    //   Floor     void_bricks main, rift_stone scatter 15%, crying_obsidian 8%
    //   6 spires  angles: 20°, 75°, 130°, 200°, 265°, 320° (irregular from blueprint)
    //             distances: 21, 19, 22, 20, 23, 18 (irregular from blueprint)
    //             Spire: void_bricks 3×3 base→1×1 apex, crystal_cluster top, sea_lantern cap
    //   8 grapples r=14..20, every 45°+offset, Y ∈ {-2,-1,0,1,2,3}
    //             3×3 void_bricks, sea_lantern on 4 of 8
    //   Outer edge crystal_cluster posts every 20° at r-2, Y+1..3
    // =====================================================================

    private static void buildVoidHorrorArena(ServerLevel level, BlockPos center) {
        final BlockState voidBricks = ModBlocks.VOID_BRICKS.get().defaultBlockState();
        final BlockState riftStone  = ModBlocks.RIFT_STONE.get().defaultBlockState();
        final BlockState cryObs     = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        final BlockState crystal    = ModBlocks.CRYSTAL_CLUSTER.get().defaultBlockState();
        final BlockState qGlassCy   = ModBlocks.QUANTUM_GLASS_CYAN.get().defaultBlockState();
        final BlockState seaLan     = Blocks.SEA_LANTERN.defaultBlockState();
        final int R = 40;

        // ── OCTAGONAL PLATFORM FLOOR ──
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                double dist = Math.sqrt(x * x + z * z);
                // Octagonal: diamond-clip approach (|x|+|z| ≤ R*1.42) inside circle
                if (dist > R || Math.abs(x) + Math.abs(z) > R * 1.42) continue;
                if (dist <= 5) continue;  // void center gap

                BlockPos pos = center.offset(x, 0, z);
                // Floor material: scatter with noise for irregular feel
                double noiseVal = Math.sin(x * 0.4) * Math.cos(z * 0.4);
                BlockState floor = (noiseVal > 0.6) ? cryObs
                    : (noiseVal > 0.3) ? riftStone
                    : voidBricks;
                level.setBlock(pos, floor, 3);
                level.setBlock(pos.below(), voidBricks, 3);
                level.setBlock(pos.below(2), voidBricks, 3);
            }
        }

        // ── VOID CENTER CLEARED (r=5) ──
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                if (Math.sqrt(x * x + z * z) <= 5) {
                    for (int dy = -15; dy <= 5; dy++)
                        level.setBlock(center.offset(x, dy, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // ── CYAN SAFETY RING around void at Y+0, r=6 ──
        // (glowing cyan ring visible in close-up concept art images)
        for (int deg = 0; deg < 360; deg += 6) {
            double angle = Math.toRadians(deg);
            int rx = center.getX() + (int) Math.round(Math.cos(angle) * 6);
            int rz = center.getZ() + (int) Math.round(Math.sin(angle) * 6);
            level.setBlock(new BlockPos(rx, center.getY(), rz), qGlassCy, 3);
        }
        // Double ring at Y-1 for depth
        for (int deg = 0; deg < 360; deg += 8) {
            double angle = Math.toRadians(deg);
            int rx = center.getX() + (int) Math.round(Math.cos(angle) * 6);
            int rz = center.getZ() + (int) Math.round(Math.sin(angle) * 6);
            level.setBlock(new BlockPos(rx, center.getY() - 1, rz), qGlassCy, 3);
        }

        // ── 6 TWISTED SPIRES (irregular positions from blueprint) ──
        // Concept art blueprint shows: Spire 1 NNW r=21 | Spire 2 NNE r=19 |
        // Spire 3 ENE r=22 | Spire 4 SSE r=20 | Spire 5 SSW r=23 | Spire 6 WSW r=18
        double[] spireAngles = { 20, 75, 130, 200, 265, 320 };
        double[] spireRadii  = { 21, 19, 22, 20, 23, 18 };
        int[]    spireSizes  = { 16, 14, 18, 15, 17, 13 };

        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(spireAngles[i]);
            int sx = center.getX() + (int) (Math.cos(angle) * spireRadii[i]);
            int sz = center.getZ() + (int) (Math.sin(angle) * spireRadii[i]);
            buildVoidSpire(level, new BlockPos(sx, center.getY(), sz), spireSizes[i],
                voidBricks, cryObs, crystal, seaLan);
        }

        // ── 8 GRAPPLING PLATFORMS (varying r=14..20, Y offset -2 to +3) ──
        // Concept art shows 8 separate floating void-brick platforms for dodging
        double[] grappleAngles = { 0, 45, 90, 135, 180, 225, 270, 315 };
        double[] grappleRadii  = { 14, 18, 16, 20, 15, 19, 17, 13 };
        int[]    grappleY      = { 0, 2, -1, 3, 1, -2, 2, 0 };

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(grappleAngles[i]);
            int px = center.getX() + (int) (Math.cos(angle) * grappleRadii[i]);
            int pz = center.getZ() + (int) (Math.sin(angle) * grappleRadii[i]);
            int py = center.getY() + grappleY[i];
            // 3×3 platform
            for (int gx = -1; gx <= 1; gx++) {
                for (int gz = -1; gz <= 1; gz++) {
                    level.setBlock(new BlockPos(px + gx, py, pz + gz), voidBricks, 3);
                    level.setBlock(new BlockPos(px + gx, py - 1, pz + gz), voidBricks, 3);
                }
            }
            // Sea lantern on every other platform (visible from action concept art)
            if (i % 2 == 0) level.setBlock(new BlockPos(px, py + 1, pz), seaLan, 3);
        }

        // ── OUTER CRYSTAL CLUSTER POSTS every 20° ──
        for (int deg = 0; deg < 360; deg += 20) {
            double angle = Math.toRadians(deg);
            int rx = center.getX() + (int) (Math.cos(angle) * (R - 3));
            int rz = center.getZ() + (int) (Math.sin(angle) * (R - 3));
            for (int y = 1; y <= 3; y++)
                level.setBlock(new BlockPos(rx, center.getY() + y, rz),
                    y == 3 ? crystal : voidBricks, 3);
        }
    }

    /**
     * Void Horror twisted spire:
     * 3×3 base (first third), narrowing to 1×1, twisted shaft.
     * Crying obsidian accents at base. Crystal cluster apex × 2, sea_lantern glow top.
     */
    private static void buildVoidSpire(ServerLevel level, BlockPos base, int height,
                                        BlockState shaft, BlockState accent,
                                        BlockState apex, BlockState glow) {
        for (int y = 0; y < height; y++) {
            double twist = y * 0.25;
            int ox = (int) (Math.sin(twist) * Math.max(0, 2 - y * 2.0 / height));
            int oz = (int) (Math.cos(twist) * Math.max(0, 2 - y * 2.0 / height));

            level.setBlock(base.offset(ox, y, oz), shaft, 3);

            // 3-wide base in first third
            if (y < height / 3) {
                level.setBlock(base.offset(ox + 1, y, oz), shaft, 3);
                level.setBlock(base.offset(ox - 1, y, oz), shaft, 3);
                level.setBlock(base.offset(ox, y, oz + 1), shaft, 3);
                if (y % 2 == 0) {
                    level.setBlock(base.offset(ox + 2, y, oz), accent, 3);
                    level.setBlock(base.offset(ox, y, oz - 2), accent, 3);
                }
            }
        }
        // Crystal cluster apex × 2 + sea lantern glow cap
        level.setBlock(base.above(height),     apex, 3);
        level.setBlock(base.above(height + 1), apex, 3);
        level.setBlock(base.above(height + 2), glow, 3);
    }

    // =====================================================================
    // ③ ENTANGLED TWINS ARENA
    //
    //   Two platforms r=26 offset ±30 | bridge arch H=8 | 3 full arch frames
    //   magenta radiating circuit lines | 8 crystal pillars (4 per platform)
    //
    //   CONSTRUCTION LOG:
    //   Platform L  center+(-30,0,0), r=26 crystallized_stone floor
    //   Platform R  center+(+30,0,0), r=26 crystallized_stone floor
    //   Circuit     X%6==0 || Z%6==0 → quantum_tech_purple (magenta grid)
    //   Spokes      radial lines from platform center every 45° → quantum_tech_purple
    //               (concept art shows radiating magenta lines)
    //   Edge        r=24..26 → quantum_glass_cyan
    //   Rim         Y+1 quantum_glass_cyan parapet ring
    //   Bridge      from leftCenter → rightCenter (60 blocks), 3 wide, arch H=8
    //               3 arch frames at 25%, 50%, 75%: 2× temporal_pillar posts + glass cap
    //               crystal_cluster crown at arch top, sea_lantern glow every 8 steps
    //   Pillars     4 per platform, 45° at r=17, 2×2 base(3h)+1×1 shaft, 18 total
    //               crystal buds at Y14-18, sea_lantern cap, crystal apex Y+19
    //   Pedestal    5×5 Y+1, 3×3 Y+2, quantum_lamp Y+3 (both platforms)
    //   Crystal ring r=22, crystal_cluster posts every 20°
    // =====================================================================

    private static void buildEntangledTwinsArena(ServerLevel level, BlockPos center) {
        final BlockState crystStone = ModBlocks.CRYSTALLIZED_STONE.get().defaultBlockState();
        final BlockState qGlassCy   = ModBlocks.QUANTUM_GLASS_CYAN.get().defaultBlockState();
        final BlockState qTechPurp  = ModBlocks.QUANTUM_TECH_PURPLE.get().defaultBlockState();
        final BlockState tPillar    = ModBlocks.TEMPORAL_PILLAR.get().defaultBlockState();
        final BlockState crystal    = ModBlocks.CRYSTAL_CLUSTER.get().defaultBlockState();
        final BlockState seaLan     = Blocks.SEA_LANTERN.defaultBlockState();
        final BlockState qBlock     = ModBlocks.QUANTUM_BLOCK.get().defaultBlockState();
        final BlockState qLamp      = ModBlocks.QUANTUM_LAMP.get().defaultBlockState();
        final int halfR     = 26;
        final int separation = 30;

        BlockPos leftCenter  = center.offset(-separation, 0, 0);
        BlockPos rightCenter = center.offset( separation, 0, 0);

        // ── BOTH PLATFORMS ──
        for (BlockPos platCenter : new BlockPos[]{ leftCenter, rightCenter }) {
            // Floor: crystallized_stone + magenta grid + radial spokes
            for (int x = -halfR; x <= halfR; x++) {
                for (int z = -halfR; z <= halfR; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist > halfR) continue;
                    BlockPos pos = platCenter.offset(x, 0, z);
                    boolean isEdge   = dist > halfR - 2;
                    boolean isGrid   = (x % 6 == 0 || z % 6 == 0);
                    // Radial spoke pattern: check if (x,z) is close to any of 8 diagonal lines
                    double rawAngle = Math.atan2(z, x) * 4 / Math.PI; // 0-8 range
                    boolean isSpoke  = (Math.abs(rawAngle - Math.round(rawAngle)) < 0.15 && dist > 2);
                    level.setBlock(pos, isEdge ? qGlassCy
                        : (isSpoke || isGrid) ? qTechPurp
                        : crystStone, 3);
                    level.setBlock(pos.below(),  crystStone, 3);
                    level.setBlock(pos.below(2), crystStone, 3);
                }
            }

            // Rim parapet at Y+1
            for (int deg = 0; deg < 360; deg += 4) {
                double angle = Math.toRadians(deg);
                int wx = platCenter.getX() + (int) (Math.cos(angle) * (halfR - 1));
                int wz = platCenter.getZ() + (int) (Math.sin(angle) * (halfR - 1));
                level.setBlock(new BlockPos(wx, center.getY() + 1, wz),
                    deg % 20 == 0 ? seaLan : qGlassCy, 3);
            }

            // 4 crystal pillars at 45° NE/NW/SE/SW, r=17
            int pillarR = 17;
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4 + Math.PI / 4;
                int px = platCenter.getX() + (int) (Math.cos(angle) * pillarR);
                int pz = platCenter.getZ() + (int) (Math.sin(angle) * pillarR);
                buildCrystalPillar(level, new BlockPos(px, center.getY(), pz),
                    18, crystStone, tPillar, crystal, seaLan);
            }

            // Boss pedestal (5×5 → 3×3 → quantum_lamp)
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                level.setBlock(platCenter.offset(x, 1, z), qBlock, 3);
            for (int[] c4 : new int[][]{{2,0},{-2,0},{0,2},{0,-2}})
                level.setBlock(platCenter.offset(c4[0], 1, c4[1]), seaLan, 3);
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
                level.setBlock(platCenter.offset(x, 2, z), qBlock, 3);
            level.setBlock(platCenter.above(3), qLamp, 3);

            // Decorative crystal ring at r=22
            for (int deg = 0; deg < 360; deg += 20) {
                double angle = Math.toRadians(deg);
                int rx = platCenter.getX() + (int) (Math.cos(angle) * 22);
                int rz = platCenter.getZ() + (int) (Math.sin(angle) * 22);
                level.setBlock(new BlockPos(rx, center.getY() + 1, rz), crystal, 3);
                if (deg % 40 == 0)
                    level.setBlock(new BlockPos(rx, center.getY() + 2, rz), crystal, 3);
            }
        }

        // ── CRYSTAL BRIDGE with arch frames (H=8) ──
        buildEntangledBridge(level, leftCenter, rightCenter, center.getY(),
            tPillar, qGlassCy, crystal, seaLan);
    }

    /**
     * Entangled Twins crystal bridge:
     * 3 blocks wide, arch HEIGHT=8 at midpoint.
     * 3 full arch frames at 25%, 50%, 75%: two temporal_pillar posts + quantum_glass arch cap.
     * Crystal cluster crown at arch apex, sea_lantern glow at midpoint.
     */
    private static void buildEntangledBridge(ServerLevel level, BlockPos from, BlockPos to,
                                              int floorY, BlockState pillarBlock,
                                              BlockState glassBlock, BlockState crystalBlock,
                                              BlockState glowBlock) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        int steps = (int) len;

        // Perpendicular unit vector for 3-wide bridge
        double perpX = -dz / len;
        double perpZ =  dx / len;

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            int bx = from.getX() + (int) (dx * t);
            int bz = from.getZ() + (int) (dz * t);
            // Arch height 8 at midpoint (sin curve)
            int archY = (int) (floorY + Math.sin(t * Math.PI) * 8);

            // 3-wide quantum glass walkway: center + 1 each side
            for (int side = -1; side <= 1; side++) {
                int wx = bx + (int) Math.round(perpX * side);
                int wz = bz + (int) Math.round(perpZ * side);
                level.setBlock(new BlockPos(wx, archY, wz), glassBlock, 3);
                level.setBlock(new BlockPos(wx, archY - 1, wz), pillarBlock, 3);
            }
            // Sea lantern glow at every 8th step
            if (i % 8 == 0)
                level.setBlock(new BlockPos(bx, archY + 1, bz), glowBlock, 3);
        }

        // ── 3 FULL ARCH FRAMES at 25%, 50%, 75% of bridge length ──
        // Each frame: 2 temporal_pillar posts rising to arch height, glass arch cap
        for (double frac : new double[]{ 0.25, 0.50, 0.75 }) {
            int fbx = from.getX() + (int) (dx * frac);
            int fbz = from.getZ() + (int) (dz * frac);
            int archH = (int) (floorY + Math.sin(frac * Math.PI) * 8);

            // Post height: from floor to arch height + 4 (framing above walkway)
            int postHeight = archH - floorY + 4;
            for (int side : new int[]{ -3, 3 }) {
                int px = fbx + (int) Math.round(perpX * side);
                int pz = fbz + (int) Math.round(perpZ * side);
                // Vertical post from floor to arch level
                for (int py = 0; py <= postHeight; py++)
                    level.setBlock(new BlockPos(px, floorY + py, pz), pillarBlock, 3);
                // Crystal cluster at post top
                level.setBlock(new BlockPos(px, floorY + postHeight + 1, pz), crystalBlock, 3);
            }
            // Arch cap: quantum glass connecting left post top to right post top
            int capY = floorY + postHeight;
            for (int s = -3; s <= 3; s++) {
                int ax = fbx + (int) Math.round(perpX * s);
                int az = fbz + (int) Math.round(perpZ * s);
                level.setBlock(new BlockPos(ax, capY, az), glassBlock, 3);
            }
            // Crystal cluster crown at arch center top
            level.setBlock(new BlockPos(fbx, capY + 1, fbz), crystalBlock, 3);
            level.setBlock(new BlockPos(fbx, capY + 2, fbz), glowBlock, 3);
        }
    }

    // =====================================================================
    // ④ SINGULARITY SOVEREIGN ARENA
    //
    //   r=48 (programmatic fallback for 128×38×128 JSON)
    //   TRUE DONUT RING: solid r=12..48, void cleared r<12
    //   4-arm spiral | 8 massive 5×5→3×3→1×1 pillars at r=36 | inner crystal ring r=17
    //
    //   CONSTRUCTION LOG:
    //   Void      r=0..12 cleared Y-15..Y+5
    //   Platform  r=12..48 crystallized_stone, 4-arm quantum_block spiral
    //             quantum_tech_cyan accent ring at r=17 (inner glow ring)
    //   Outer rim r=46..47: crying_obsidian + quantum_glass_cyan alternating every 4°
    //             + decorative 1×1 temporal_pillar posts (6h) between main pillars
    //   8 pillars r=36, evenly at 0°/45°/90°.., 5×5 base → 3×3 mid → 1×1 top, 25 high
    //             end_rod apex × 2, crystal cluster crown (4-side × 3 blocks)
    //   Inner ring r=17, 24 crystal_cluster + sea_lantern alternating (every 15°)
    //   Outer ring r=43, quantum_glass every 8°
    // =====================================================================

    private static void buildSovereignArena(ServerLevel level, BlockPos center) {
        final BlockState crystStone = ModBlocks.CRYSTALLIZED_STONE.get().defaultBlockState();
        final BlockState qBlock     = ModBlocks.QUANTUM_BLOCK.get().defaultBlockState();
        final BlockState qGlass     = ModBlocks.QUANTUM_GLASS.get().defaultBlockState();
        final BlockState qGlassCy   = ModBlocks.QUANTUM_GLASS_CYAN.get().defaultBlockState();
        final BlockState qTechCy    = ModBlocks.QUANTUM_TECH_CYAN.get().defaultBlockState();
        final BlockState crystal    = ModBlocks.CRYSTAL_CLUSTER.get().defaultBlockState();
        final BlockState seaLan     = Blocks.SEA_LANTERN.defaultBlockState();
        final BlockState endRod     = Blocks.END_ROD.defaultBlockState();
        final BlockState cryObs     = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        final BlockState tPillar    = ModBlocks.TEMPORAL_PILLAR.get().defaultBlockState();
        final BlockState singCore   = ModBlocks.SINGULARITY_CORE_BLOCK.get().defaultBlockState();
        final int R = 48;

        // ── TRUE DONUT RING FLOOR (r=12 to r=48) ──
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                double dist  = Math.sqrt(x * x + z * z);
                if (dist > R || dist < 12) continue;  // skip outside + void center

                double angle = Math.atan2(z, x);
                // 4-arm spiral pattern (concept art blueprint shows 4 arms)
                double spiralPhase = ((dist * 0.07 + angle) % (Math.PI * 2 / 4) + Math.PI * 2) % (Math.PI * 2 / 4);
                boolean inArm  = spiralPhase < Math.PI / 5;
                boolean isEdge = dist > R - 3;
                boolean isInnerGlow = (dist >= 16 && dist <= 18); // inner quantum tech ring

                BlockState floor = isEdge     ? qBlock
                    : isInnerGlow ? qTechCy
                    : inArm       ? qBlock
                    : crystStone;
                level.setBlock(center.offset(x, 0, z), floor, 3);
                level.setBlock(center.offset(x, -1, z), crystStone, 3);
            }
        }

        // ── VOID CENTER CLEARED (r=12, Y-15..Y+5) ──
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                if (Math.sqrt(x * x + z * z) <= 12) {
                    for (int dy = -15; dy <= 5; dy++)
                        level.setBlock(center.offset(x, dy, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // ── OUTER RIM: crying_obsidian + quantum_glass_cyan alternating every 4° ──
        for (int deg = 0; deg < 360; deg += 4) {
            double angle = Math.toRadians(deg);
            int wx = center.getX() + (int) (Math.cos(angle) * (R - 1));
            int wz = center.getZ() + (int) (Math.sin(angle) * (R - 1));
            level.setBlock(new BlockPos(wx, center.getY() + 1, wz),
                (deg % 16 == 0) ? seaLan
                : (deg % 8 == 0) ? qGlassCy
                : cryObs, 3);
        }

        // ── 8 MASSIVE TAPERED PILLARS at r=36, every 45° ──
        int pillarR = 36;
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 * i) / 8;
            int px = center.getX() + (int) (Math.cos(angle) * pillarR);
            int pz = center.getZ() + (int) (Math.sin(angle) * pillarR);
            buildSovereignPillar(level, new BlockPos(px, center.getY(), pz), 25,
                crystStone, endRod, crystal, seaLan, singCore);
        }

        // ── DECORATIVE TEMPORAL PILLAR POSTS between main pillars ──
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 * i) / 8 + Math.PI / 8;  // midpoint between main pillars
            int px = center.getX() + (int) (Math.cos(angle) * (R - 4));
            int pz = center.getZ() + (int) (Math.sin(angle) * (R - 4));
            for (int y = 1; y <= 6; y++)
                level.setBlock(new BlockPos(px, center.getY() + y, pz),
                    y % 3 == 0 ? seaLan : tPillar, 3);
            level.setBlock(new BlockPos(px, center.getY() + 7, pz), crystal, 3);
        }

        // ── INNER CRYSTAL RING at r=17 (dense, from blueprint) ──
        for (int i = 0; i < 24; i++) {
            double angle  = Math.PI * 2 * i / 24;
            int rx = center.getX() + (int) (Math.cos(angle) * 17);
            int rz = center.getZ() + (int) (Math.sin(angle) * 17);
            level.setBlock(new BlockPos(rx, center.getY() + 1, rz),
                i % 2 == 0 ? crystal : seaLan, 3);
            // Every 3rd: tall crystal spike
            if (i % 3 == 0) {
                level.setBlock(new BlockPos(rx, center.getY() + 2, rz), crystal, 3);
                level.setBlock(new BlockPos(rx, center.getY() + 3, rz), seaLan, 3);
            }
        }

        // ── OUTER DECORATIVE GLASS RING at r=43 ──
        for (int deg = 0; deg < 360; deg += 8) {
            double angle = Math.toRadians(deg);
            int rx = center.getX() + (int) (Math.cos(angle) * 43);
            int rz = center.getZ() + (int) (Math.sin(angle) * 43);
            level.setBlock(new BlockPos(rx, center.getY() + 1, rz), qGlass, 3);
        }
    }

    /**
     * Singularity Sovereign massive tapered pillar:
     * 5×5 base (first 8 blocks) → 3×3 mid (9–18) → 1×1 top (19–25), 25 high.
     * Sea lantern every 6Y at core.
     * End rod × 2 at apex, singularity_core_block above end rods.
     * Crystal cluster crown: 4-side × 3-block cluster arrangement.
     */
    private static void buildSovereignPillar(ServerLevel level, BlockPos base, int height,
                                              BlockState pillarBlock, BlockState rodBlock,
                                              BlockState crystalBlock, BlockState glowBlock,
                                              BlockState coreBlock) {
        for (int y = 0; y < height; y++) {
            int maxOff = (y < 8) ? 2 : (y < 18) ? 1 : 0;
            for (int x = -maxOff; x <= maxOff; x++) {
                for (int z = -maxOff; z <= maxOff; z++) {
                    level.setBlock(base.offset(x, y, z), pillarBlock, 3);
                }
            }
            if (y % 6 == 0) level.setBlock(base.above(y), glowBlock, 3);
        }
        // End rod apex
        level.setBlock(base.above(height),     rodBlock, 3);
        level.setBlock(base.above(height + 1), rodBlock, 3);
        level.setBlock(base.above(height + 2), coreBlock, 3);

        // Crystal cluster crown: 4 cardinals + 4 diagonals, 2 tiers
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 2 * i / 4;
            int cx = (int) Math.round(Math.cos(angle));
            int cz = (int) Math.round(Math.sin(angle));
            level.setBlock(base.offset(cx, height - 3, cz), crystalBlock, 3);
            level.setBlock(base.offset(cx * 2, height - 4, cz * 2), crystalBlock, 3);
            level.setBlock(base.offset(cx, height - 1, cz), crystalBlock, 3);
        }
        level.setBlock(base.above(height + 3), glowBlock, 3);
    }

    // =====================================================================
    // SHARED HELPER — Crystal Pillar (Entangled Twins)
    // 2×2 crystStone base (3 high) + 1×1 temporal_pillar shaft (18 total)
    // Crystal cluster buds at Y14–18, sea_lantern cap, crystal apex Y+19
    // =====================================================================

    private static void buildCrystalPillar(ServerLevel level, BlockPos base, int height,
                                            BlockState baseBlock, BlockState shaftBlock,
                                            BlockState crystalBlock, BlockState capBlock) {
        // 2×2 base for first 3 blocks
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x <= 1; x++) for (int z = 0; z <= 1; z++)
                level.setBlock(base.offset(x, y, z), baseBlock, 3);
        }
        // 1×1 temporal_pillar shaft
        for (int y = 3; y < height; y++) {
            level.setBlock(base.above(y), (y % 4 == 0) ? capBlock : shaftBlock, 3);
        }
        // Crystal cluster buds at 4 cardinal sides, top 5 blocks
        for (int y = height - 4; y <= height; y++) {
            for (int i = 0; i < 4; i++) {
                double angle = Math.PI * 2 * i / 4;
                int bx = (int) Math.round(Math.cos(angle));
                int bz = (int) Math.round(Math.sin(angle));
                if (y >= height - 2)
                    level.setBlock(base.offset(bx, y, bz), crystalBlock, 3);
            }
        }
        // Sea lantern cap + crystal apex
        level.setBlock(base.above(height),     capBlock,     3);
        level.setBlock(base.above(height + 1), crystalBlock, 3);
    }
}
