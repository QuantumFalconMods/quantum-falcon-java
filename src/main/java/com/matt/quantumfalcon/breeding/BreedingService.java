package com.matt.quantumfalcon.breeding;

import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.HashSet;
import java.util.Set;

/** World-facing breeding helpers: egg context from a location, per-player pity counters and discoveries. */
public final class BreedingService {
    private BreedingService() {}
    private static final String KEY = "qf_breeding";

    public static BreedingEngine.Context contextAt(ServerLevel level, BlockPos pos) {
        BreedingEngine.Context c = new BreedingEngine.Context();
        var dim = level.dimension().location();
        c.nether = level.dimension() == Level.NETHER;
        c.end = level.dimension() == Level.END;
        c.rift = "quantumfalcon".equals(dim.getNamespace()) && dim.getPath().startsWith("echo_");
        c.riftLayer = riftLayer(level, pos);
        var biome = level.getBiome(pos);
        c.snowy = biome.value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW;
        String path = biome.unwrapKey().map(k -> k.location().getPath()).orElse("");
        c.voidBiome = path.contains("void");
        c.aurora = path.contains("aurora");
        var nq = com.matt.quantumfalcon.block.FalconNestBlock.qualityAt(level, pos.below());
        if (nq != null) c.nestMutation = nq.mutation;
        return c;
    }

    /** Echo Rift layer (echo_rift = 0, echo_rift_N = N, echo_depths = 1); -1 outside the rift. */
    public static int riftLayer(Level level) {
        var dim = level.dimension().location();
        if (!"quantumfalcon".equals(dim.getNamespace())) return -1;
        String p = dim.getPath();
        if (p.equals("echo_rift")) return 0;
        if (p.equals("echo_depths")) return 1;
        if (p.startsWith("echo_rift_")) try { return Integer.parseInt(p.substring(10)); } catch (NumberFormatException e) { return 0; }
        return -1;
    }

    /**
     * the "deep rift" layer at a position. The numbered layer dimensions never shipped, so in the Echo Rift the
     * layer follows the zone tier (the same tier the instability meter and the banner use): zone 2+ counts as layer 3.
     */
    public static int riftLayer(Level level, BlockPos pos) {
        int l = riftLayer(level);
        if (l == 0 && pos != null) {
            int z = com.matt.quantumfalcon.dimension.InstabilityEffects.zoneTierAt(level, pos);
            return z >= 2 ? 3 : z;
        }
        return l;
    }

    static CompoundTag data(Player p) {
        CompoundTag root = com.matt.quantumfalcon.util.PersistedTag.of(p, KEY);   // pity and Codex discoveries survive death
        if (!root.contains(KEY)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }
    public static int pity(Player p, int tier) { return data(p).getInt("pity" + tier); }
    public static void recordTierRoll(Player p, int baseTier, boolean success) {
        CompoundTag d = data(p);
        d.putInt("pity" + baseTier, success ? 0 : Math.min(10, d.getInt("pity" + baseTier) + 1));
    }
    /** Marks a variant, hybrid, fusion or ability as discovered (the Codex shows ??? for undiscovered secrets). Returns true if new. */
    public static boolean discover(Player p, String id) {
        CompoundTag d = data(p);
        ListTag l = d.getList("found", Tag.TAG_STRING);
        for (Tag t : l) if (t.getAsString().equals(id)) return false;
        l.add(StringTag.valueOf(id)); d.put("found", l);
        return true;
    }
    public static Set<String> discovered(Player p) {
        Set<String> s = new HashSet<>();
        for (Tag t : data(p).getList("found", Tag.TAG_STRING)) s.add(t.getAsString());
        return s;
    }

    /** Wild tier for a natural spawn: the variant's base tier, +1 at 15% (25% in the deep Rift, zone 2+). */
    public static int wildTier(String variant, Level level, BlockPos pos, net.minecraft.util.RandomSource r) {
        int t = BreedingEngine.variantTier(variant);
        float up = riftLayer(level, pos) >= 3 ? 0.25f : 0.15f;
        if (r.nextFloat() < up) t++;
        return Math.min(7, t);
    }

    /** Best free nest (egg seat empty) within r blocks of pos, or null. Higher-quality nests win, then the closest. */
    @javax.annotation.Nullable
    public static BlockPos freeNestNear(ServerLevel level, BlockPos pos, int r) {
        BlockPos best = null; int bestQ = -1; double bestD = 1e9;
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-r, -3, -r), pos.offset(r, 3, r))) {
            var nq = com.matt.quantumfalcon.block.FalconNestBlock.qualityAt(level, q);
            if (nq == null || !level.getBlockState(q.above()).canBeReplaced()) continue;
            double d = q.distSqr(pos);
            if (nq.tier > bestQ || (nq.tier == bestQ && d < bestD)) { best = q.immutable(); bestQ = nq.tier; bestD = d; }
        }
        return best;
    }

    public static boolean bothBonded(QuantumFalconEntity a, QuantumFalconEntity b) { return a.getBondLevel() >= 10 && b.getBondLevel() >= 10; }
}
