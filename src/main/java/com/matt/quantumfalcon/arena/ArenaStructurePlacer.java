package com.matt.quantumfalcon.arena;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fallback arena placer. Reads an arena snapshot ({@code data/quantumfalcon/structure/<name>.json}, format
 * {@code qf_arena_v1}, written by {@code test.ArenaExport} from {@code ArenaDesign}) and places its blocks around a
 * centre. Used only if the live {@code ArenaDesign} build throws.
 */
public final class ArenaStructurePlacer {
    private ArenaStructurePlacer() {}

    record Snapshot(int ox, int oy, int oz, BlockState[] palette, int[][] blocks) {}

    static Snapshot load(ServerLevel level, String name) throws Exception {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "structure/" + name + ".json");
        var res = level.getServer().getResourceManager().getResource(loc);
        if (res.isEmpty()) return null;
        JsonObject root;
        try (var r = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) { root = JsonParser.parseReader(r).getAsJsonObject(); }
        if (!"qf_arena_v1".equals(root.get("format").getAsString())) return null;
        JsonArray o = root.getAsJsonArray("origin");
        JsonArray pal = root.getAsJsonArray("palette");
        BlockState[] states = new BlockState[pal.size()];
        for (int i = 0; i < states.length; i++)
            states[i] = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), pal.get(i).getAsString(), false).blockState();
        JsonArray bl = root.getAsJsonArray("blocks");
        int[][] blocks = new int[bl.size()][];
        for (int i = 0; i < blocks.length; i++) {
            JsonArray b = bl.get(i).getAsJsonArray();
            blocks[i] = new int[]{b.get(0).getAsInt(), b.get(1).getAsInt(), b.get(2).getAsInt(), b.get(3).getAsInt()};
        }
        return new Snapshot(o.get(0).getAsInt(), o.get(1).getAsInt(), o.get(2).getAsInt(), states, blocks);
    }

    /** Places the snapshot with its floor centre at {@code centre}. Returns blocks placed, or -1 if missing/unreadable. */
    public static int placeArena(ServerLevel level, BlockPos centre, String name) {
        try {
            Snapshot s = load(level, name);
            if (s == null) { QuantumFalcon.LOGGER.warn("Arena snapshot {} not found", name); return -1; }
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int[] b : s.blocks())
                level.setBlock(m.set(centre.getX() + b[0] - s.ox(), centre.getY() + b[1] - s.oy(), centre.getZ() + b[2] - s.oz()), s.palette()[b[3]], 2);
            return s.blocks().length;
        } catch (Exception e) {
            QuantumFalcon.LOGGER.error("Arena snapshot {} failed to place", name, e);
            return -1;
        }
    }

    public static ArenaPlacementTask createBatchPlacement(ServerLevel level, BlockPos centre, String name, int perTick) {
        return new ArenaPlacementTask(level, centre, name, perTick);
    }

    /** Places a snapshot a slice per tick: call {@link #tick()} until {@link #isComplete()}. */
    public static final class ArenaPlacementTask {
        private final ServerLevel level; private final BlockPos centre; private final int perTick;
        private Snapshot snap; private int next; private boolean failed;
        ArenaPlacementTask(ServerLevel level, BlockPos centre, String name, int perTick) {
            this.level = level; this.centre = centre; this.perTick = Math.max(1, perTick);
            try { snap = load(level, name); } catch (Exception e) { snap = null; }
            failed = snap == null;
        }
        public void tick() {
            if (isComplete()) return;
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            int end = Math.min(snap.blocks().length, next + perTick);
            for (; next < end; next++) {
                int[] b = snap.blocks()[next];
                level.setBlock(m.set(centre.getX() + b[0] - snap.ox(), centre.getY() + b[1] - snap.oy(), centre.getZ() + b[2] - snap.oz()), snap.palette()[b[3]], 2);
            }
        }
        public boolean isComplete() { return failed || next >= snap.blocks().length; }
        public boolean failed() { return failed; }
    }
}
