package com.matt.quantumfalcon.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Procedural boss arena used by natural worldgen (stage 4 arena pass).
 *
 * Layout, from the ground up:
 *  - a stepped plinth of the realm's bricks from the terrain up to the floor, its skirt terracing down into the
 *    surrounding ground (hills are cut back to the terrace instead of poking through);
 *  - a cleared cylinder above the floor and its apron, plus a band around it where every log, leaf, vine and
 *    snow layer is removed so no cut tree is left hanging;
 *  - a themed floor pattern readable from above (compass rose, void spiral, entangled halves, accretion disk);
 *  - a low parapet with four cardinal entrances;
 *  - pylons set symmetrically on a ring, each joined to the centre by a lit floor spoke;
 *  - for the Warden and the Sovereign, a walled pit at the centre.
 * Every top surface is a realm block tagged minecraft:snow_layer_cannot_survive_on, so neither worldgen frost nor
 * snowfall can settle on the floor, the rim, the terraces or the pylons.
 */
public final class ArenaDesign {
    private ArenaDesign() { }

    /** Where blocks go; implementations clip to the chunk being generated. */
    public interface Sink {
        BlockState get(BlockPos p);
        void set(BlockPos p, BlockState s);
    }

    public static final int APRON = 6;        // terraced skirt width outside the floor
    public static final int TREE_BAND = 7;    // band beyond the apron cleared of tree blocks
    public static final int CLEAR_UP = 56;    // air cleared above the floor (opens the sky under echo overhangs)
    public static final int MAX_FOUNDATION = 30;

    public static int floorRadius(BossArenaStructure.ArenaType t) { return t.templateRadius(); }
    public static int reach(BossArenaStructure.ArenaType t) { return floorRadius(t) + APRON + TREE_BAND; }

    /** The realm palette each arena is built from. */
    record Palette(String realm, String accentRealm) {
        BlockState b(String suffix) { return block(realm + suffix); }
        BlockState accent(String suffix) { return block(accentRealm + suffix); }
        BlockState pre(String prefix) { return block(prefix + realm); }
    }

    static Palette palette(BossArenaStructure.ArenaType t) {
        return switch (t) {
            case QUANTUM_GUARDIAN -> new Palette("riftslate", "aurorite");
            case VOID_HORROR -> new Palette("voidshale", "abyssite");
            case ENTANGLED_TWINS -> new Palette("prismite", "riftslate");
            case SINGULARITY_SOVEREIGN -> new Palette("singularite", "voidshale");
        };
    }

    static BlockState block(String id) {
        Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", id));
        return (b == Blocks.AIR ? Blocks.STONE_BRICKS : b).defaultBlockState();
    }

    /** True for blocks that are part of the natural surface the plinth stands on (not trees, plants or snow). */
    static boolean isGround(BlockState s) {
        if (s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty() && !s.getFluidState().isSource()) return false;
        if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.is(Blocks.SNOW) || s.is(BlockTags.FLOWERS)
                || s.is(Blocks.BEE_NEST) || s.is(Blocks.MUSHROOM_STEM) || s.is(Blocks.RED_MUSHROOM_BLOCK) || s.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || s.is(BlockTags.SAPLINGS) || s.is(Blocks.BAMBOO) || s.is(Blocks.CACTUS) || s.is(Blocks.VINE)) return false;
        return true;
    }

    static boolean isTreeOrCover(BlockState s) {
        return s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.is(Blocks.VINE) || s.is(Blocks.SNOW) || s.is(Blocks.BEE_NEST)
                || s.is(Blocks.MUSHROOM_STEM) || s.is(Blocks.RED_MUSHROOM_BLOCK) || s.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || s.is(Blocks.COCOA) || s.is(Blocks.BAMBOO) || s.is(Blocks.MOSS_CARPET) || s.is(Blocks.GLOW_LICHEN) || s.is(Blocks.HANGING_ROOTS);
    }

    /** Height of the top of the terrace at horizontal distance d from the centre (the floor out to R+1). */
    static int terraceTop(int floorY, double d, int r) {
        if (d <= r + 1) return floorY;
        return floorY - (int) Math.ceil((d - (r + 1)) * 1.5);
    }

    /** Builds the arena straight into a live level (commands, tests, /qf boss), floor at {@code centre}'s height. */
    public static void buildInWorld(net.minecraft.server.level.ServerLevel lvl, BlockPos centre, BossArenaStructure.ArenaType type) {
        int reach = reach(type);
        for (int cx = (centre.getX() - reach) >> 4; cx <= (centre.getX() + reach) >> 4; cx++)
            for (int cz = (centre.getZ() - reach) >> 4; cz <= (centre.getZ() + reach) >> 4; cz++) lvl.getChunk(cx, cz);
        BoundingBox box = new BoundingBox(centre.getX() - reach, centre.getY() - MAX_FOUNDATION - 16, centre.getZ() - reach,
            centre.getX() + reach, centre.getY() + CLEAR_UP + 4, centre.getZ() + reach);
        build(new Sink() {
            @Override public BlockState get(BlockPos q) { return lvl.getBlockState(q); }
            @Override public void set(BlockPos q, BlockState st) { lvl.setBlock(q, st, 2); }
        }, centre, type, box);
    }

    public static void build(Sink w, BlockPos centre, BossArenaStructure.ArenaType type, BoundingBox clip) {
        int r = floorRadius(type), f = centre.getY(), reach = reach(type);
        Palette p = palette(type);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) for (int dz = -reach; dz <= reach; dz++) {
            int x = centre.getX() + dx, z = centre.getZ() + dz;
            if (x < clip.minX() || x > clip.maxX() || z < clip.minZ() || z > clip.maxZ()) continue;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > reach) continue;
            int ground = groundY(w, m, x, z, f);
            if (d > r + APRON) {
                // tree band: strip every tree block, leaf and snow layer so nothing is left floating near the arena
                for (int y = ground + 1; y <= f + CLEAR_UP; y++) if (isTreeOrCover(w.get(m.set(x, y, z)))) w.set(m, Blocks.AIR.defaultBlockState());
                continue;
            }
            int top = terraceTop(f, d, r);
            for (int y = top + 1; y <= f + CLEAR_UP && y < 320; y++) if (!w.get(m.set(x, y, z)).isAir()) w.set(m, Blocks.AIR.defaultBlockState());
            // foundation: down to the ground (capped and tapered underneath, so a plinth over a drop reads as a pedestal)
            int bottom = Math.max(ground + 1, f - MAX_FOUNDATION + (int) (d * 0.6));
            bottom = Math.min(bottom, top - (d <= r ? 3 : 1));
            for (int y = top; y >= bottom; y--) w.set(m.set(x, y, z), foundation(p, d, r, y, top, f));
            if (d > r + 1) {
                // skirt surface: polished terrace treads with brick risers
                w.set(m.set(x, top, z), (int) Math.ceil((d - (r + 1)) * 1.5) % 3 == 0 ? p.pre("polished_") : p.b("_tiles"));
                continue;
            }
            if (d > r) { w.set(m.set(x, f, z), p.b("_bricks")); continue; }
            w.set(m.set(x, f, z), floorPattern(type, p, dx, dz, d, r));
            w.set(m.set(x, f - 1, z), p.b("_bricks"));
        }
        rim(w, centre, type, p, clip);
        pit(w, centre, type, p, clip);
        pylons(w, centre, type, p, clip);
    }

    static int groundY(Sink w, BlockPos.MutableBlockPos m, int x, int z, int f) {
        for (int y = f + CLEAR_UP; y > f - MAX_FOUNDATION - 20; y--) if (isGround(w.get(m.set(x, y, z)))) return y;
        return f - MAX_FOUNDATION - 20;
    }

    static BlockState foundation(Palette p, double d, int r, int y, int top, int f) {
        if (d > r - 1.5) return ((y + (int) d) % 5 == 0) ? p.b("_bricks") : (y % 4 == 0 ? p.pre("polished_") : p.b("_bricks"));
        return p.b("");
    }

    /** The readable floor motif for each boss. */
    static BlockState floorPattern(BossArenaStructure.ArenaType type, Palette p, int dx, int dz, double d, int r) {
        double a = Math.atan2(dz, dx);
        BlockState base = p.b("_tiles"), alt = p.pre("polished_"), line = block(p.realm() + "_rift_lamp"), glow = block("glowing_" + p.realm() + "_bricks");
        switch (type) {
            case QUANTUM_GUARDIAN -> {
                // compass rose: an eight-point star of glowing bricks on polished stone, two trim rings, trim compass lines
                if (d <= 2.5) return d < 1 ? block(p.realm() + "_rift_lamp") : p.pre("chiseled_");
                double star = 0.55 * r * (0.45 + 0.55 * Math.pow(Math.abs(Math.cos(2 * a)), 6)) + 0.18 * r * Math.pow(Math.abs(Math.sin(2 * a)), 6);
                if (Math.abs(d - star) < 0.7) return line;
                if (d < star) return Math.abs(Math.sin(4 * a)) < 0.12 ? glow : alt;
                if (Math.abs(d - 0.8 * r) < 0.6) return line;
                if (onAxis(dx, dz, 0)) return glow;
                return ((int) Math.floor(d) % 2 == 0) ? base : p.accent("_tiles");
            }
            case VOID_HORROR -> {
                // void spiral: three glowing arms winding out of the pit over dark tiles, a trim ring at mid radius
                double arm = (a / (2 * Math.PI) * 3 + d / r * 1.4) % 1.0;
                if (arm < 0) arm += 1;
                if (arm < 0.09) return glow;
                if (arm < 0.14) return line;
                if (Math.abs(d - 0.62 * r) < 0.6) return line;
                return ((dx + dz) & 1) == 0 ? base : p.accent("_tiles");
            }
            case ENTANGLED_TWINS -> {
                // entangled halves: a two-colour S-split disc (prismite magenta / riftslate cyan), each half carrying
                // a dot of the other colour, edged with trim
                double h = r / 2.0;
                double dTop = Math.sqrt(dx * dx + (dz + h) * (dz + h)), dBot = Math.sqrt(dx * dx + (dz - h) * (dz - h));
                boolean magenta = dTop <= h ? true : dBot <= h ? false : dx > 0;
                double edge = Math.min(Math.abs(dTop - h), Math.abs(dBot - h));
                if (dTop <= h * 0.28) return block("riftslate_rift_lamp");
                if (dBot <= h * 0.28) return block("prismite_rift_lamp");
                if (dTop <= h * 0.38 || dBot <= h * 0.38) return line;
                if (edge < 0.6 && ((dTop <= h && dx <= 0) || (dBot <= h && dx >= 0))) return line;
                if (Math.abs(d - (r - 1.5)) < 0.6) return line;
                return magenta ? (((int) d % 4 == 0) ? p.pre("polished_") : p.b("_tiles")) : (((int) d % 4 == 0) ? block("polished_riftslate") : block("riftslate_tiles"));
            }
            case SINGULARITY_SOVEREIGN -> {
                // accretion disk: banded rings that brighten toward the singularity, eight curved infall streaks
                int band = (int) Math.floor(d / 3.0);
                double streak = ((a + d * 0.11) / (2 * Math.PI) * 8) % 1.0;
                if (streak < 0) streak += 1;
                if (streak < 0.07) return d < r * 0.7 ? glow : line;
                if (Math.abs(d % 3.0) < 0.5) return band % 2 == 0 ? line : p.pre("polished_");
                return band % 2 == 0 ? base : p.accent("_tiles");
            }
        }
        return base;
    }

    static boolean onAxis(int dx, int dz, int w) { return Math.abs(dx) <= w || Math.abs(dz) <= w; }

    /** One-block parapet with cardinal entrances, a lamp on every eighth post. */
    static void rim(Sink w, BlockPos c, BossArenaStructure.ArenaType type, Palette p, BoundingBox clip) {
        int r = floorRadius(type);
        int steps = (int) (2 * Math.PI * r * 1.5);
        for (int i = 0; i < steps; i++) {
            double a = 2 * Math.PI * i / steps;
            int x = c.getX() + (int) Math.round(Math.cos(a) * r), z = c.getZ() + (int) Math.round(Math.sin(a) * r);
            if (!clip.isInside(x, c.getY(), z)) continue;
            int dx = x - c.getX(), dz = z - c.getZ();
            if (Math.abs(dx) <= 2 || Math.abs(dz) <= 2) {   // entrance: lamp-flanked opening onto the terraces
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) w.set(new BlockPos(x, c.getY() + 1, z), block(p.realm() + "_rift_lamp"));
                continue;
            }
            int post = Math.floorMod((int) Math.round(a / (2 * Math.PI) * 64), 8);
            w.set(new BlockPos(x, c.getY() + 1, z), post == 0 ? block(p.realm() + "_rift_lamp") : p.b("_bricks"));
        }
    }

    static void pit(Sink w, BlockPos c, BossArenaStructure.ArenaType type, Palette p, BoundingBox clip) {
        int vr = type.getVoidCenterRadius();
        if (vr <= 0) return;
        int f = c.getY();
        for (int dx = -vr - 1; dx <= vr + 1; dx++) for (int dz = -vr - 1; dz <= vr + 1; dz++) {
            int x = c.getX() + dx, z = c.getZ() + dz;
            if (!clip.isInside(x, f, z)) continue;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d <= vr) {
                for (int y = f; y >= f - 9; y--) w.set(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                w.set(new BlockPos(x, f - 10, z), block("void_obsidian"));
                w.set(new BlockPos(x, f - 11, z), p.b("_bricks"));
            } else if (d <= vr + 1.2) {
                w.set(new BlockPos(x, f, z), p.b("_void_glass"));
                for (int y = f - 1; y >= f - 10; y--) w.set(new BlockPos(x, y, z), (y % 3 == 0) ? block("glowing_" + p.realm() + "_bricks") : p.b("_bricks"));
            }
        }
    }

    public static int pylonHeightOf(BossArenaStructure.ArenaType t) { return pylonHeight(t); }

    static int pylonHeight(BossArenaStructure.ArenaType t) {
        return switch (t) { case QUANTUM_GUARDIAN -> 7; case VOID_HORROR -> 9; case ENTANGLED_TWINS -> 8; case SINGULARITY_SOVEREIGN -> 11; };
    }

    /** Pylons on a ring (offset so the cardinal entrances fall between them), each joined to the centre by a lit spoke. */
    static void pylons(Sink w, BlockPos c, BossArenaStructure.ArenaType type, Palette p, BoundingBox clip) {
        int n = type.getPillarCount(), r = floorRadius(type), f = c.getY(), h = pylonHeight(type);
        int ring = r - 4, inner = Math.max(type.getVoidCenterRadius() + 2, 3);
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * (i + 0.5) / n;
            double cos = Math.cos(a), sin = Math.sin(a);
            // spoke: trim line with a floor lamp every fourth block
            for (int s = inner; s < ring - 1; s++) {
                int x = c.getX() + (int) Math.round(cos * s), z = c.getZ() + (int) Math.round(sin * s);
                if (clip.isInside(x, f, z)) w.set(new BlockPos(x, f, z), (s - inner) % 2 == 0 ? block(p.realm() + "_rift_lamp") : p.b("_void_glass"));
            }
            int px = c.getX() + (int) Math.round(cos * ring), pz = c.getZ() + (int) Math.round(sin * ring);
            for (int ox = -2; ox <= 2; ox++) for (int oz = -2; oz <= 2; oz++) {
                int x = px + ox, z = pz + oz;
                if (!clip.isInside(x, f, z)) continue;
                int ax = Math.abs(ox), az = Math.abs(oz);
                boolean ringCell = ax == 2 || az == 2, corner2 = ax == 2 && az == 2;
                if (ringCell) {
                    // plinth apron: chiseled ring, lamps on the four outer corners
                    w.set(new BlockPos(x, f, z), corner2 ? block(p.realm() + "_rift_lamp") : p.pre("chiseled_"));
                    continue;
                }
                boolean core = ax == 0 && az == 0, corner = ax == 1 && az == 1;
                w.set(new BlockPos(x, f, z), p.pre("polished_"));
                w.set(new BlockPos(x, f + 1, z), core ? p.pre("chiseled_") : corner ? p.pre("chiseled_") : stair(p, ox, oz));
                // 3x3 shaft: pillar corners, glowing faces banded with bricks, a lamp ring under the crown
                for (int y = f + 2; y < f + h; y++) {
                    BlockState st = corner ? p.b("_pillar") : core ? p.b("_bricks")
                        : (y - f) % 3 == 0 ? block(p.realm() + "_rift_lamp") : block("glowing_" + p.realm() + "_bricks");
                    w.set(new BlockPos(x, y, z), st);
                }
                w.set(new BlockPos(x, f + h, z), corner ? slabTop(p) : core ? p.pre("chiseled_") : p.pre("polished_"));
                if (core) {
                    w.set(new BlockPos(x, f + h + 1, z), block(p.realm() + "_rift_lamp"));
                    w.set(new BlockPos(x, f + h + 2, z), clusterUp(p));
                } else if (!corner) {
                    w.set(new BlockPos(x, f + h + 1, z), clusterUp(p));
                }
            }
        }
    }

    static BlockState stair(Palette p, int ox, int oz) {
        BlockState s = p.b("_brick_stairs");
        if (!s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return p.b("_bricks");
        Direction facing = ox > 0 ? Direction.WEST : ox < 0 ? Direction.EAST : oz > 0 ? Direction.NORTH : Direction.SOUTH;
        return s.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }

    static BlockState slabTop(Palette p) {
        BlockState s = p.b("_brick_slab");
        return s.hasProperty(BlockStateProperties.SLAB_TYPE) ? s.setValue(BlockStateProperties.SLAB_TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP) : s;
    }

    static BlockState clusterUp(Palette p) {
        BlockState s = p.b("_crystal_cluster");
        return s.hasProperty(BlockStateProperties.FACING) ? s.setValue(BlockStateProperties.FACING, Direction.UP) : s;
    }
}
