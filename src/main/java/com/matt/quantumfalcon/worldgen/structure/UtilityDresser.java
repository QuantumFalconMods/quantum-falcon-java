package com.matt.quantumfalcon.worldgen.structure;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.dungeon.ModDungeon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Depth post-pass for the Java-built utility structures (QuantumUtilityStructure). Runs after a builder, inside the same
 * chunk clip, and only touches cells next to our own blocks, so natural terrain is left alone. Every decision is a hash
 * of the world position, so the chunks that share a structure agree with each other.
 *
 *  - foundations: floors on uneven ground get a brick skirt down to the terrain instead of floating;
 *  - cornices and merlons: upside-down stairs under overhanging wall tops, walls as crenels on high wall tops;
 *  - rubble and cobwebs on our floors, hanging chains with crystal clusters under our ceilings;
 *  - void tendrils creeping over wall faces;
 *  - surface sites: a lamp-post path leading in from the south edge with a gate of two pillars.
 */
final class UtilityDresser {
    private UtilityDresser() {}

    static String realmFor(QuantumUtilityStructure.UtilityType t) {
        return switch (t) {
            case ANCIENT_NEST, ECHO_RIFT_GATE -> "riftslate";
            case QUANTUM_WELL, BREEDING_SANCTUM -> "aurorite";
            case RIFT_SHRINE, VOID_CITADEL -> "voidshale";
            case TEMPORAL_NEXUS, DATA_ARCHIVE -> "chronoslate";
            case ECHO_CHAMBER -> "abyssite";
            case CRYSTAL_CAVERN -> "prismite";
            case QUANTUM_OBSERVATORY -> "singularite";
        };
    }

    private static BlockState b(String name) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, name)).defaultBlockState();
    }

    /** Our own position hash: three odd 64-bit multipliers, then a xor-shift finaliser. */
    static long seed(int x, int y, int z) {
        long l = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
        l ^= l >>> 31; l *= 0xBF58476D1CE4E5B9L; l ^= l >>> 29;
        return l;
    }

    private static double h(int x, int y, int z, int salt) {
        long s = seed(x, y + salt * 131, z) * 0x9E3779B97F4A7C15L;
        return ((s >>> 40) & 0xFFFFFF) / (double) 0x1000000;
    }

    private static boolean ours(BlockState s) {
        if (s.isAir()) return false;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
        return QuantumFalcon.MOD_ID.equals(id.getNamespace()) || s.is(Blocks.SEA_LANTERN) || s.is(Blocks.PURPLE_CONCRETE);
    }

    private static boolean plainSolid(WorldGenLevel level, BlockPos p, BlockState s) {
        if (!ours(s) || !s.isCollisionShapeFullBlock(level, p)) return false;
        String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return !path.contains("glass") && !path.contains("lamp") && !path.contains("nest") && !path.contains("incubator") && !path.contains("forge");
    }

    static void dress(WorldGenLevel level, BoundingBox clip, BlockPos o, QuantumUtilityStructure.UtilityType type, boolean surface) {
        String r = realmFor(type);
        BlockState bricks = b(r + "_bricks"), cracked = b("cracked_" + r + "_bricks"), lamp = b(r + "_rift_lamp");
        BlockState stairs = b(r + "_brick_stairs").setValue(StairBlock.HALF, Half.TOP);
        BlockState slab = b("cracked_" + r + "_brick_slab").setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState wall = b(r + "_brick_wall"), fence = b(r + "_brick_fence"), pillar = b(r + "_pillar");
        BlockState polished = b("polished_" + r), tiles = b(r + "_tiles");
        BlockState cluster = b(r + "_crystal_cluster").setValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING, Direction.DOWN);
        BlockState tendrils = ModDungeon.VOID_TENDRILS.get().defaultBlockState();
        BlockState chain = Blocks.CHAIN.defaultBlockState();
        int R = type.getRadius(), H = type.getHeight();
        int x0 = Math.max(clip.minX(), o.getX() - R - 1), x1 = Math.min(clip.maxX(), o.getX() + R + 1);
        int z0 = Math.max(clip.minZ(), o.getZ() - R - 1), z1 = Math.min(clip.maxZ(), o.getZ() + R + 1);
        int y0 = Math.max(clip.minY(), o.getY() - 5), y1 = Math.min(clip.maxY() - 1, o.getY() + H);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(), q = new BlockPos.MutableBlockPos();

        // foundations under our floor
        if (surface) {
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                if (!ours(level.getBlockState(p.set(x, o.getY(), z)))) continue;
                for (int y = o.getY() - 1; y >= o.getY() - 12 && y >= clip.minY(); y--) {
                    BlockState s = level.getBlockState(p.set(x, y, z));
                    if (!(s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty())) break;
                    put(level, clip, p, h(x, y, z, 1) < 0.22 ? cracked : bricks);
                }
            }
        }
        // cell pass
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) {
            p.set(x, y, z);
            BlockState s = level.getBlockState(p);
            if (!s.isAir()) {
                if (!plainSolid(level, p, s) || !level.getBlockState(q.set(x, y + 1, z)).isAir()) continue;
                // wall top: cornice under the overhang side, merlons on high tops
                boolean high = level.getBlockState(q.set(x, y - 3, z)).isSolidRender(level, q) && y - o.getY() >= 4;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    q.set(x + d.getStepX(), y, z + d.getStepZ());
                    if (!clip.isInside(q) || !level.getBlockState(q).isAir()) continue;
                    if (!level.getBlockState(q.below()).isAir() || !level.getBlockState(q.below(2)).isAir()) continue;
                    if (h(x, y, z, 2 + d.ordinal()) < 0.7) put(level, clip, q.immutable(), stairs.setValue(StairBlock.FACING, d.getOpposite()));
                }
                if (high && ((x + z) & 1) == 0 && h(x, y, z, 9) < 0.8) {
                    int open = 0;
                    for (Direction d : Direction.Plane.HORIZONTAL) if (level.getBlockState(q.set(x + d.getStepX(), y, z + d.getStepZ())).isAir()) open++;
                    if (open >= 1 && open <= 2) put(level, clip, p.above(), wall);
                }
                continue;
            }
            BlockState below = level.getBlockState(q.set(x, y - 1, z));
            BlockState above = level.getBlockState(q.set(x, y + 1, z));
            double k = h(x, y, z, 3);
            if (ours(below) && below.isFaceSturdy(level, q.set(x, y - 1, z), Direction.UP)) {
                Direction wallSide = null;
                for (Direction d : Direction.Plane.HORIZONTAL) if (plainSolid(level, q.set(x + d.getStepX(), y, z + d.getStepZ()), level.getBlockState(q))) { wallSide = d; break; }
                if (wallSide != null && k < 0.10) { put(level, clip, p, slab); continue; }
                if (wallSide != null && k < 0.13) { put(level, clip, p, Blocks.COBWEB.defaultBlockState()); continue; }
                if (wallSide != null && k < 0.30) { put(level, clip, p, tendrils.setValue(MultifaceBlock.getFaceProperty(wallSide), true)); continue; }
                if (k > 0.995) { put(level, clip, p.below(), lamp); continue; }
            }
            if (plainSolid(level, q.set(x, y + 1, z), above) && k < 0.02 && y - 3 > y0
                && level.getBlockState(q.set(x, y - 1, z)).isAir() && level.getBlockState(q.set(x, y - 2, z)).isAir() && level.getBlockState(q.set(x, y - 3, z)).isAir()) {
                put(level, clip, p, chain); put(level, clip, p.below(), cluster); continue;
            }
            if (k > 0.94) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    q.set(x + d.getStepX(), y, z + d.getStepZ());
                    if (plainSolid(level, q, level.getBlockState(q))) { put(level, clip, p, tendrils.setValue(MultifaceBlock.getFaceProperty(d), true)); break; }
                }
            }
        }
        // path leading in from the south edge, with a gate of two pillars
        if (surface) {
            int px = o.getX(), zStart = o.getZ() + R + 1;
            for (int i = 0; i < 14; i++) {
                int z = zStart + i;
                for (int dx = -1; dx <= 1; dx++) {
                    int x = px + dx;
                    if (x < clip.minX() || x > clip.maxX() || z < clip.minZ() || z > clip.maxZ()) continue;
                    int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                    put(level, clip, p.set(x, top - 1, z), dx == 0 ? tiles : (h(x, 0, z, 5) < 0.25 ? cracked : polished));
                    for (int y = top; y < top + 3; y++) {
                        BlockState s = level.getBlockState(p.set(x, y, z));
                        if (s.canBeReplaced() && !s.isAir()) put(level, clip, p, Blocks.AIR.defaultBlockState());
                    }
                }
                if (i % 4 == 1 || i == 0) for (int dx : new int[]{-2, 2}) {
                    int x = px + dx;
                    if (x < clip.minX() || x > clip.maxX() || z < clip.minZ() || z > clip.maxZ()) continue;
                    int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                    if (i == 0) {
                        for (int y = 0; y < 4; y++) put(level, clip, p.set(x, top + y, z), pillar);
                        put(level, clip, p.set(x, top + 4, z), lamp);
                    } else {
                        put(level, clip, p.set(x, top, z), wall); put(level, clip, p.set(x, top + 1, z), fence); put(level, clip, p.set(x, top + 2, z), lamp);
                    }
                }
            }
        }
    }

    private static void put(WorldGenLevel level, BoundingBox clip, BlockPos pos, BlockState s) {
        if (clip.isInside(pos)) level.setBlock(pos, s, 2);
    }
}
