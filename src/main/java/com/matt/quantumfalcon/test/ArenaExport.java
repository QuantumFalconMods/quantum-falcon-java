package com.matt.quantumfalcon.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.matt.quantumfalcon.worldgen.structure.ArenaDesign;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Dev tool: renders each boss arena from {@link ArenaDesign} into memory over flat stone ground and writes the
 * blocks it placed as {@code data/quantumfalcon/structure/<arena>.json} (format qf_arena_v1). Those snapshots are
 * the fallback {@code ArenaStructurePlacer} uses if the live design build ever throws.
 */
public final class ArenaExport {
    private ArenaExport() {}

    public static String name(BossArenaStructure.ArenaType t) { return t.name().toLowerCase(Locale.ROOT) + "_arena"; }

    public static int exportAll(Path dir) throws Exception {
        Files.createDirectories(dir);
        int n = 0;
        for (BossArenaStructure.ArenaType t : BossArenaStructure.ArenaType.values()) { write(t, dir.resolve(name(t) + ".json")); n++; }
        return n;
    }

    static void write(BossArenaStructure.ArenaType type, Path out) throws Exception {
        final int floorY = 0;
        BlockPos centre = new BlockPos(0, floorY, 0);
        int reach = ArenaDesign.reach(type);
        BoundingBox clip = new BoundingBox(-reach, floorY - ArenaDesign.MAX_FOUNDATION - 16, -reach, reach, floorY + ArenaDesign.CLEAR_UP + 4, reach);
        Map<BlockPos, BlockState> placed = new HashMap<>();
        ArenaDesign.build(new ArenaDesign.Sink() {
            @Override public BlockState get(BlockPos q) {
                BlockState s = placed.get(q);
                return s != null ? s : (q.getY() < floorY ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            @Override public void set(BlockPos q, BlockState s) { placed.put(q.immutable(), s); }
        }, centre, type, clip);
        // keep only what differs from the ground it was built on
        List<Map.Entry<BlockPos, BlockState>> keep = new ArrayList<>();
        for (var e : placed.entrySet()) {
            BlockState base = e.getKey().getY() < floorY ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
            if (!e.getValue().equals(base) && !e.getValue().isAir()) keep.add(e);
        }
        keep.sort(Comparator.comparingInt((Map.Entry<BlockPos, BlockState> e) -> e.getKey().getY())
            .thenComparingInt(e -> e.getKey().getZ()).thenComparingInt(e -> e.getKey().getX()));
        int minY = keep.stream().mapToInt(e -> e.getKey().getY()).min().orElse(0);
        int maxY = keep.stream().mapToInt(e -> e.getKey().getY()).max().orElse(0);
        Map<String, Integer> pal = new LinkedHashMap<>();
        JsonArray blocks = new JsonArray();
        for (var e : keep) {
            String id = stateString(e.getValue());
            int i = pal.computeIfAbsent(id, k -> pal.size());
            JsonArray b = new JsonArray();
            b.add(e.getKey().getX() + reach); b.add(e.getKey().getY() - minY); b.add(e.getKey().getZ() + reach); b.add(i);
            blocks.add(b);
        }
        JsonObject root = new JsonObject();
        root.addProperty("format", "qf_arena_v1");
        root.addProperty("source", "Quantum Falcon ArenaDesign (" + type.name() + "), exported by ArenaExport");
        JsonArray size = new JsonArray(); size.add(2 * reach + 1); size.add(maxY - minY + 1); size.add(2 * reach + 1);
        root.add("size", size);
        JsonArray origin = new JsonArray(); origin.add(reach); origin.add(floorY - minY); origin.add(reach);
        root.add("origin", origin);
        JsonArray palette = new JsonArray(); pal.keySet().forEach(palette::add);
        root.add("palette", palette);
        root.add("blocks", blocks);
        Files.writeString(out, root.toString(), StandardCharsets.UTF_8);
    }

    static String stateString(BlockState s) {
        StringBuilder sb = new StringBuilder(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString());
        if (!s.getValues().isEmpty()) {
            sb.append('[');
            boolean first = true;
            for (var p : s.getValues().entrySet()) {
                if (!first) sb.append(','); first = false;
                sb.append(p.getKey().getName()).append('=').append(propName(p.getKey(), p.getValue()));
            }
            sb.append(']');
        }
        return sb.toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String propName(net.minecraft.world.level.block.state.properties.Property p, Comparable v) { return p.getName(v); }
}
