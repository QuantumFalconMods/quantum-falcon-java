package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.dimension.EchoRiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/**
 * how far a player arriving in the Echo Rift has to travel to each boss arena. Uses the same search /locate uses
 * (findNearestMapStructure) from the rift origin and four arrival points 2500 blocks out, and checks the distances
 * against limits per boss (the first boss must be close, the later ones may be further).
 */
final class ArenaDistanceTests {
    private ArenaDistanceTests() {}

    static final String[] ARENAS = {"quantum_guardian_arena", "void_horror_arena", "entangled_twins_arena", "singularity_sovereign_arena"};
    /** Worst-case nearest distance allowed from any sampled arrival point, per arena (blocks). */
    static final int[] MAX_NEAREST = {1200, 3000, 3000, 5000};
    static final BlockPos[] ARRIVALS = {BlockPos.ZERO, new BlockPos(2500, 64, 2500), new BlockPos(-2500, 64, 2500), new BlockPos(2500, 64, -2500), new BlockPos(-2500, 64, -2500)};

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("audit_r23_arena_distance", ArenaDistanceTests::distance);
        tests.put("audit_r23_arena_distance_seeds", ArenaDistanceTests::seeds);
    }

    private static void distance(ServerLevel level) {
        ServerLevel rift = level.getServer().getLevel(EchoRiftDimension.ECHO_RIFT_LEVEL_KEY);
        if (rift == null) { result("audit_r23_arena_distance", false, "no Echo Rift dimension"); return; }
        var reg = rift.registryAccess().registryOrThrow(Registries.STRUCTURE);
        boolean ok = true;
        StringBuilder d = new StringBuilder("seed ").append(level.getSeed()).append(": ");
        for (int i = 0; i < ARENAS.length; i++) {
            var holder = reg.getHolder(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("quantumfalcon", ARENAS[i]))).orElse(null);
            if (holder == null) { ok = false; d.append(ARENAS[i]).append("=missing; "); continue; }
            List<Integer> dists = new ArrayList<>();
            for (BlockPos from : ARRIVALS) {
                var res = rift.getChunkSource().getGenerator().findNearestMapStructure(rift, HolderSet.direct(holder), from, 100, false);
                int dist = res == null ? Integer.MAX_VALUE : (int) Math.sqrt(res.getFirst().distSqr(new BlockPos(from.getX(), res.getFirst().getY(), from.getZ())));
                dists.add(dist);
            }
            int worst = Collections.max(dists);
            boolean pass = worst <= MAX_NEAREST[i];
            ok &= pass;
            d.append(ARENAS[i].replace("_arena", "")).append(" nearest ").append(dists.toString().replace(String.valueOf(Integer.MAX_VALUE), "none"))
                .append(" worst ").append(worst == Integer.MAX_VALUE ? "none" : worst).append(" (limit ").append(MAX_NEAREST[i]).append(pass ? ")" : ") TOO FAR").append("; ");
        }
        result("audit_r23_arena_distance", ok, d.toString());
    }

    static final long[] SEEDS = {-3500126496844798594L, 0L, 1L, 42L, 123456789L, -987654321987L, 7355608L, 0x5EEDL, 20261113L, -1L};

    /**
     * on many seeds: the world seed can't change at runtime, so this rebuilds the placement state for each seed
     * (RandomState + ChunkGeneratorStructureState from the Rift's own generator, biome source and structure sets) and runs
     * the same checks worldgen runs: random_spread cell, exclusion zone, and the structure's biome list at the chunk.
     * BossArenaStructure.findGenerationPoint can no longer refuse a site, so a passing chunk is a generated arena.
     */
    private static void seeds(ServerLevel level) {
        ServerLevel rift = level.getServer().getLevel(EchoRiftDimension.ECHO_RIFT_LEVEL_KEY);
        if (rift == null) { result("audit_r23_arena_distance_seeds", false, "no Echo Rift dimension"); return; }
        var ra = rift.registryAccess();
        var gen = rift.getChunkSource().getGenerator();
        if (!(gen instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator ng)) { result("audit_r23_arena_distance_seeds", false, "rift generator is not noise-based"); return; }
        var sets = ra.lookupOrThrow(Registries.STRUCTURE_SET);
        var structs = ra.registryOrThrow(Registries.STRUCTURE);
        boolean ok = true;
        StringBuilder d = new StringBuilder();
        int[] worstAll = new int[ARENAS.length];
        for (long seed : SEEDS) {
            var rs = net.minecraft.world.level.levelgen.RandomState.create(ng.generatorSettings().value(), ra.lookupOrThrow(Registries.NOISE), seed);
            var state = net.minecraft.world.level.chunk.ChunkGeneratorStructureState.createForNormal(rs, seed, gen.getBiomeSource(), sets);
            for (int i = 0; i < ARENAS.length; i++) {
                var set = sets.getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath("quantumfalcon", ARENAS[i]))).value();
                var structure = structs.getOrThrow(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("quantumfalcon", ARENAS[i])));
                var biomes = structure.biomes();
                int worst = 0;
                for (BlockPos from : ARRIVALS) {
                    int best = Integer.MAX_VALUE;
                    int fcx = from.getX() >> 4, fcz = from.getZ() >> 4;
                    for (int r = 0; r <= 400; r++) {
                        if (best != Integer.MAX_VALUE && best <= (r - 1) * 16) break;   // no farther ring can beat it
                        for (int cx = fcx - r; cx <= fcx + r; cx++) for (int cz = fcz - r; cz <= fcz + r; cz++) {
                            if (Math.max(Math.abs(cx - fcx), Math.abs(cz - fcz)) != r) continue;
                            if (!set.placement().isStructureChunk(state, cx, cz)) continue;
                            var biome = gen.getBiomeSource().getNoiseBiome((cx << 2) + 2, 64 >> 2, (cz << 2) + 2, rs.sampler());
                            if (!biomes.contains(biome)) continue;
                            int dx = (cx << 4) + 8 - from.getX(), dz = (cz << 4) + 8 - from.getZ();
                            best = Math.min(best, (int) Math.sqrt((double) dx * dx + (double) dz * dz));
                        }
                    }
                    worst = Math.max(worst, best);
                }
                worstAll[i] = Math.max(worstAll[i], worst);
                if (worst > MAX_NEAREST[i]) { ok = false; d.append("seed ").append(seed).append(' ').append(ARENAS[i]).append(" worst ").append(worst).append(" > ").append(MAX_NEAREST[i]).append("; "); }
            }
        }
        StringBuilder w = new StringBuilder(SEEDS.length + " seeds x " + ARRIVALS.length + " arrival points, worst nearest per arena:");
        for (int i = 0; i < ARENAS.length; i++) w.append(' ').append(ARENAS[i].replace("_arena", "")).append('=').append(worstAll[i]).append("/").append(MAX_NEAREST[i]);
        result("audit_r23_arena_distance_seeds", ok, w + (d.length() > 0 ? " | TOO FAR: " + d : ""));
    }
}
