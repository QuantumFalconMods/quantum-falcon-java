package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.registry.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/**
 * Natural generation of the 12 utility structures (READINESS gap 2), on a normal world (own-world test, like
 * eastwatch_natural): each one is located from two far-apart points in every dimension its biomes reach, the find has a
 * real start with pieces, sits in one of its own biomes, and the nearest-site distances are reported as a density check
 * (how many of the dimension's biomes the tag list lets in, the broad #c:is_overworld ones especially). Also the Codex
 * chart (ri_04 reward) really points at Eastwatch.
 */
public final class UtilityStructureTests {
    private UtilityStructureTests() {}

    static final String[] UTILITY = { "quantum_observatory", "void_citadel", "breeding_sanctum", "echo_rift_gate", "temporal_nexus",
        "echo_chamber", "rift_shrine", "data_archive", "ancient_nest", "crystal_cavern", "quantum_well", "eastwatch_aerie" };

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("utility_natural", UtilityStructureTests::natural);
    }

    private static void natural(ServerLevel ow) {
        var server = ow.getServer();
        StringBuilder d = new StringBuilder();
        List<String> problems = new ArrayList<>();
        Map<String, Integer> found = new TreeMap<>();
        List<ResourceKey<Level>> dims = List.of(Level.OVERWORLD, ModDimensions.ECHO_RIFT);
        int[] step = {0};
        for (String id : UTILITY) for (ResourceKey<Level> dim : dims) {
            later(1 + 6 * step[0]++, () -> {
                ServerLevel l = server.getLevel(dim);
                if (l == null) return;
                var reg = l.registryAccess().registryOrThrow(Registries.STRUCTURE);
                Holder<Structure> h = reg.getHolder(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("quantumfalcon", id))).orElse(null);
                if (h == null) { problems.add(id + " not registered"); return; }
                Set<Holder<Biome>> possible = l.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
                long eligible = possible.stream().filter(b -> h.value().biomes().contains(b)).count();
                if (eligible == 0) return;   // not meant for this dimension
                String dn = dim == Level.OVERWORLD ? "ow" : "rift";
                List<Integer> dist = new ArrayList<>();
                BlockPos[] from = { l.getSharedSpawnPos(), new BlockPos(40000, 64, -40000) };
                for (BlockPos f : from) {
                    var res = l.getChunkSource().getGenerator().findNearestMapStructure(l, HolderSet.direct(h), f, 64, false);
                    if (res == null) { dist.add(-1); continue; }
                    BlockPos at = res.getFirst();
                    dist.add((int) Math.sqrt(at.distSqr(new BlockPos(f.getX(), at.getY(), f.getZ()))));
                    if (dist.size() == 1) {
                        var start = l.getChunk(at.getX() >> 4, at.getZ() >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS).getStartForStructure(h.value());
                        if (start == null || !start.isValid() || start.getPieces().isEmpty()) problems.add(id + "/" + dn + " located but no valid start");
                        else {
                            var c = start.getBoundingBox().getCenter();
                            Holder<Biome> b = l.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(c.getX() >> 2, c.getY() >> 2, c.getZ() >> 2, l.getChunkSource().randomState().sampler());
                            if (!h.value().biomes().contains(b)) problems.add(id + "/" + dn + " start in " + b.unwrapKey().map(k -> k.location().getPath()).orElse("?"));
                        }
                    }
                }
                long ok = dist.stream().filter(x -> x >= 0).count();
                if (ok == 0) problems.add(id + "/" + dn + " none within 64 rings");
                found.merge(id, (int) ok, Integer::sum);
                d.append(id).append('/').append(dn).append(" biomes ").append(eligible).append('/').append(possible.size()).append(" nearest ").append(dist).append("; ");
            });
        }
        later(10 + 6 * step[0], () -> {
            // the Codex pointer: completing ri_04 hands over a chart with a red X on the nearest Eastwatch
            var fp = AuditTests.freshPlayer(ow, "QFChartReader");
            fp.getInventory().clearContent();
            com.matt.quantumfalcon.codex.RiftCodex.grant(fp, com.matt.quantumfalcon.codex.RiftCodex.QUESTS.get("ri_04"));
            net.minecraft.world.item.ItemStack map = net.minecraft.world.item.ItemStack.EMPTY;
            for (int i = 0; i < fp.getInventory().getContainerSize(); i++) if (fp.getInventory().getItem(i).is(net.minecraft.world.item.Items.FILLED_MAP)) map = fp.getInventory().getItem(i);
            var deco = map.get(net.minecraft.core.component.DataComponents.MAP_DECORATIONS);
            boolean chart = !map.isEmpty() && deco != null && !deco.decorations().isEmpty();
            String where = chart ? deco.decorations().values().iterator().next().x() + "," + deco.decorations().values().iterator().next().z() : "none";
            boolean atEastwatch = false;
            if (chart) {
                var e = deco.decorations().values().iterator().next();
                var reg = ow.registryAccess().registryOrThrow(Registries.STRUCTURE);
                var h = reg.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("quantumfalcon", "eastwatch_aerie")));
                var st = ow.getChunk(((int) e.x()) >> 4, ((int) e.z()) >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS).getStartForStructure(h.value());
                atEastwatch = st != null && st.isValid();
            }
            fp.getInventory().clearContent();
            boolean all = Arrays.stream(UTILITY).allMatch(found::containsKey);
            result("utility_natural", all && problems.isEmpty() && chart && atEastwatch,
                "types found " + found.size() + "/" + UTILITY.length + " | problems " + problems + " | codex chart '" + map.getHoverName().getString() + "' X at " + where
                    + " on an Eastwatch start=" + atEastwatch + " | " + d);
        });
    }
}
