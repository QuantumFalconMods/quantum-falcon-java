package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** Fixes from the pre-1.0 test pass (the 2026-11-14 pre10 test pass review). */
final class Pre10FixTests {
    private Pre10FixTests() {}

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("pr5_owner_only", Pre10FixTests::pr5);
        tests.put("pr7_falcon_ctor_threads", Pre10FixTests::pr7);
        tests.put("dev_export_arenas", l -> {   // dev: rewrite the arena snapshots from ArenaDesign into run/qf_arena_export
            try { int n = ArenaExport.exportAll(java.nio.file.Path.of(System.getProperty("user.dir"), "qf_arena_export")); result("dev_export_arenas", n == 4, "exported " + n); }
            catch (Exception ex) { result("dev_export_arenas", false, ex.toString()); } });
        tests.put("pr17_arena_snapshots", Pre10FixTests::pr17);
        tests.put("pr1_frozen_hour_fluids", Pre10FixTests::pr1);
        tests.put("pr8_falconer_chart", Pre10FixTests::pr8);
        tests.put("pr11_citadel_chests", Pre10FixTests::pr11);
    }

    /** another player can't breed or leash your falcon; you can, and their name tag still works. */
    static void pr5(ServerLevel level) {
        BlockPos p = pad(level, 3400, 3480, Blocks.GRASS_BLOCK);
        cleanup(level, p, 8);
        FakePlayer owner = AuditTests.freshPlayer(level, "QFOwnerA"), other = AuditTests.freshPlayer(level, "QFOtherB");
        QuantumFalconEntity f = falcon(level, p, true);
        f.tame(owner); f.setAge(0);
        f.setInLove(null); f.resetLove();
        interact(other, f, Items.SALMON, 4);
        boolean love = f.isInLove();
        interact(other, f, Items.LEAD, 1);
        boolean leashedByOther = f.isLeashed();
        interact(other, f, Items.NAME_TAG, 1);   // a plain name tag has no name: the interaction must at least reach vanilla
        boolean tagReached = true;
        f.setInLove(null); f.resetLove();
        interact(owner, f, Items.LEAD, 1);
        boolean leashedByOwner = f.isLeashed() && f.getLeashHolder() == owner;
        if (f.isLeashed()) f.dropLeash(true, false);
        f.discard();
        result("pr5_owner_only", !love && !leashedByOther && leashedByOwner && tagReached,
            "non-owner salmon -> in love=" + love + ", non-owner lead -> leashed=" + leashedByOther + " | owner lead -> leashed=" + leashedByOwner);
    }

    /** falcons built on several worker threads at once (as worldgen chunk loading does) construct without a threading error. */
    static void pr7(ServerLevel level) {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        AtomicInteger made = new AtomicInteger(), errors = new AtomicInteger();
        StringBuilder first = new StringBuilder();
        CountDownLatch go = new CountDownLatch(1);
        try {
            var futures = new java.util.ArrayList<Future<?>>();
            for (int t = 0; t < 6; t++) futures.add(pool.submit(() -> {
                try { go.await(); } catch (InterruptedException ignored) {}
                for (int i = 0; i < 150; i++) {
                    try { var f = new QuantumFalconEntity(ModEntities.QUANTUM_FALCON.get(), level); if (f != null) made.incrementAndGet(); }
                    catch (Throwable ex) { if (errors.getAndIncrement() == 0) synchronized (first) { first.append(ex); } }
                }
            }));
            go.countDown();
            for (var fu : futures) fu.get(60, TimeUnit.SECONDS);
        } catch (Exception ex) { errors.incrementAndGet(); first.append(ex); }
        finally { pool.shutdownNow(); }
        result("pr7_falcon_ctor_threads", errors.get() == 0 && made.get() == 900,
            "constructed " + made.get() + "/900 on 6 threads, errors=" + errors.get() + (first.length() > 0 ? " first: " + first : ""));
    }

    /** the rebuilt arena snapshots (our own ArenaDesign export) load and place through the clean placer. */
    static void pr17(ServerLevel level) {
        StringBuilder d = new StringBuilder(); boolean ok = true; int k = 0;
        for (var t : com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType.values()) {
            BlockPos c = new BlockPos(52000 + 300 * k++, 120, 52000);
            level.setChunkForced(c.getX() >> 4, c.getZ() >> 4, true);
            int n = com.matt.quantumfalcon.arena.ArenaStructurePlacer.placeArena(level, c, ArenaExport.name(t));
            boolean floor = false;   // the Void Horror and Sovereign arenas have an open pit at the centre: look out along +x
            for (int dx = 0; dx < 40 && !floor; dx++) floor = !level.getBlockState(c.offset(dx, 0, 0)).isAir() || !level.getBlockState(c.offset(dx, -1, 0)).isAir();
            ok &= n > 1000 && floor;
            d.append(ArenaExport.name(t)).append('=').append(n).append(floor ? "" : " (no floor at centre)").append(' ');
            level.setChunkForced(c.getX() >> 4, c.getZ() >> 4, false);
        }
        result("pr17_arena_snapshots", ok, d.toString().trim());
    }

    /**
     * (run on a real-terrain world: QF_WORLD=... QF_LEVELTYPE=minecraft:normal): fluid cells inside the Eastwatch ward
     * (a) at the nearest natural Eastwatch, generated fresh, and (b) at a site forced under the sea with its surface at y=32.
     */
    static void pr1(ServerLevel ow) {
        if (QFTestCommands.flat(ow)) { QFTestCommands.skip("pr1_frozen_hour_fluids", "real-terrain test; run with QF_LEVELTYPE=minecraft:normal"); return; }
        var h = DungeonTests.structure(ow, "eastwatch_aerie");
        var gen = ow.getChunkSource().getGenerator();
        StringBuilder d = new StringBuilder();
        boolean ok = true;
        // (a) natural
        var found = gen.findNearestMapStructure(ow, net.minecraft.core.HolderSet.direct(h), ow.getSharedSpawnPos(), 24, false);
        if (found == null) { d.append("natural: none found; "); ok = false; }
        else {
            BlockPos p = found.getFirst();
            var c0 = ow.getChunk(p.getX() >> 4, p.getZ() >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
            var start = c0.getStartForStructure(h.value());
            if (start == null || !start.isValid()) { d.append("natural: no start at ").append(p.toShortString()).append("; "); ok = false; }
            else {
                var bb = start.getBoundingBox();
                for (int cx = (bb.minX() >> 4) - 1; cx <= (bb.maxX() >> 4) + 1; cx++) for (int cz = (bb.minZ() >> 4) - 1; cz <= (bb.maxZ() >> 4) + 1; cz++) ow.getChunk(cx, cz);
                int[] f = fluids(ow, start);
                ok &= f[0] == 0;
                d.append("natural at ").append(bb.getCenter().toShortString()).append(" surface y=").append(FrozenHoursY(start)).append(": ward fluid cells=").append(f[0]).append(" (ward cells ").append(f[1]).append("); ");
            }
        }
        // (b) forced: an ocean column near spawn, surface floor at y=32
        BlockPos sea = null;
        BlockPos sp = ow.getSharedSpawnPos();
        outer:
        for (int r = 0; r < 4000; r += 64) for (int a = 0; a < 16; a++) {
            int x = sp.getX() + (int) (r * Math.cos(a * Math.PI / 8)), z = sp.getZ() + (int) (r * Math.sin(a * Math.PI / 8));
            int top = gen.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, ow, ow.getChunkSource().randomState());
            int floor = gen.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, ow, ow.getChunkSource().randomState());
            if (top - floor >= 12 && floor > 20 && floor < 50) { sea = new BlockPos(x & ~15, 0, z & ~15); break outer; }
        }
        if (sea == null) { d.append("forced: no ocean found near spawn"); ok = false; }
        else {
            int depth = depthOf(h.value());
            BlockPos origin = new BlockPos(sea.getX(), 32 - depth, sea.getZ());
            var piece = new com.matt.quantumfalcon.worldgen.structure.FrozenHourStructure.Piece(ow.getStructureManager(),
                ((com.matt.quantumfalcon.worldgen.structure.FrozenHourStructure) h.value()).templateId(), origin);
            var start = new net.minecraft.world.level.levelgen.structure.StructureStart(h.value(), new net.minecraft.world.level.ChunkPos(origin),
                0, new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(piece)));
            var bb = start.getBoundingBox();
            for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                ow.setChunkForced(cx, cz, true);
                var cp = new net.minecraft.world.level.ChunkPos(cx, cz);
                start.placeInChunk(ow, ow.structureManager(), gen, ow.getRandom(),
                    new net.minecraft.world.level.levelgen.structure.BoundingBox(cp.getMinBlockX(), ow.getMinBuildHeight(), cp.getMinBlockZ(), cp.getMaxBlockX(), ow.getMaxBuildHeight(), cp.getMaxBlockZ()), cp);
            }
            int[] f = fluids(ow, start);
            ok &= f[0] == 0;
            d.append("forced under the sea at ").append(bb.getCenter().toShortString()).append(" surface y=32: ward fluid cells=").append(f[0]).append(" (ward cells ").append(f[1]).append(")");
            for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) ow.setChunkForced(cx, cz, false);
        }
        result("pr1_frozen_hour_fluids", ok, d.toString());
    }

    /** a master Falconer at spawn offers a chart whose red X sits on an Eastwatch Aerie start. */
    static void pr8(ServerLevel ow) {
        if (QFTestCommands.flat(ow)) { QFTestCommands.skip("pr8_falconer_chart", "real-terrain test; run with QF_LEVELTYPE=minecraft:normal"); return; }
        BlockPos sp = ow.getSharedSpawnPos();
        var v = net.minecraft.world.entity.EntityType.VILLAGER.create(ow);
        v.moveTo(sp.getX() + 0.5, ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sp.getX(), sp.getZ()), sp.getZ() + 0.5);
        v.setVillagerData(new net.minecraft.world.entity.npc.VillagerData(net.minecraft.world.entity.npc.VillagerType.PLAINS, com.matt.quantumfalcon.villager.ModVillagers.FALCONER.get(), 5));
        var offers = v.getOffers();
        net.minecraft.world.item.ItemStack map = net.minecraft.world.item.ItemStack.EMPTY; int cost = 0;
        for (var o : offers) if (o.getResult().is(Items.FILLED_MAP) && o.getResult().getHoverName().getString().contains("Eastwatch")) { map = o.getResult(); cost = o.getCostA().getCount(); }
        var deco = map.get(net.minecraft.core.component.DataComponents.MAP_DECORATIONS);
        boolean atEastwatch = false; String where = "none";
        if (deco != null && !deco.decorations().isEmpty()) {
            var e = deco.decorations().values().iterator().next();
            where = (int) e.x() + "," + (int) e.z();
            var h = DungeonTests.structure(ow, "eastwatch_aerie");
            var st = ow.getChunk(((int) e.x()) >> 4, ((int) e.z()) >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS).getStartForStructure(h.value());
            atEastwatch = st != null && st.isValid();
        }
        v.discard();
        result("pr8_falconer_chart", !map.isEmpty() && atEastwatch,
            "master Falconer offers " + offers.size() + " trades; chart '" + (map.isEmpty() ? "none" : map.getHoverName().getString()) + "' for " + cost + " emeralds, X at " + where + " on an Eastwatch start=" + atEastwatch);
    }

    /** every chest in the nearest natural Void Citadel stands on something (no chest with air below). */
    static void pr11(ServerLevel ow) {
        if (QFTestCommands.flat(ow)) { QFTestCommands.skip("pr11_citadel_chests", "real-terrain test; run with QF_LEVELTYPE=minecraft:normal"); return; }
        var h = DungeonTests.structure(ow, "void_citadel");
        var found = ow.getChunkSource().getGenerator().findNearestMapStructure(ow, net.minecraft.core.HolderSet.direct(h), ow.getSharedSpawnPos(), 100, false);
        if (found == null) { result("pr11_citadel_chests", false, "no void_citadel within 100 regions"); return; }
        BlockPos p = found.getFirst();
        var start = ow.getChunk(p.getX() >> 4, p.getZ() >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS).getStartForStructure(h.value());
        if (start == null || !start.isValid()) { result("pr11_citadel_chests", false, "no start at " + p.toShortString()); return; }
        var bb = start.getBoundingBox();
        int chests = 0, floating = 0; StringBuilder bad = new StringBuilder();
        for (int cx = (bb.minX() >> 4) - 1; cx <= (bb.maxX() >> 4) + 1; cx++) for (int cz = (bb.minZ() >> 4) - 1; cz <= (bb.maxZ() >> 4) + 1; cz++) ow.getChunk(cx, cz);
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++)
            for (var be : ow.getChunk(cx, cz).getBlockEntities().values()) {
                if (!(be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity) || !bb.isInside(be.getBlockPos())) continue;
                chests++;
                if (ow.getBlockState(be.getBlockPos().below()).isAir()) { floating++; bad.append(be.getBlockPos().toShortString()).append(' '); }
            }
        result("pr11_citadel_chests", chests >= 3 && floating == 0,
            "void_citadel at " + bb.getCenter().toShortString() + ": chests " + chests + ", with air below " + floating + (bad.length() > 0 ? " at " + bad : ""));
    }

    static int FrozenHoursY(net.minecraft.world.level.levelgen.structure.StructureStart s) {
        return com.matt.quantumfalcon.dungeon.FrozenHours.origin(s).getY();
    }

    static int depthOf(net.minecraft.world.level.levelgen.structure.Structure s) {
        try { var f = com.matt.quantumfalcon.worldgen.structure.FrozenHourStructure.class.getDeclaredField("depth"); f.setAccessible(true); return f.getInt(s); }
        catch (ReflectiveOperationException e) { return 20; }
    }

    /** {fluid cells inside the ward, ward cells} for a placed Frozen Hour. */
    static int[] fluids(ServerLevel l, net.minecraft.world.level.levelgen.structure.StructureStart start) {
        int wet = 0, ward = 0;
        var bb = start.getBoundingBox();
        for (BlockPos q : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            if (!com.matt.quantumfalcon.dungeon.FrozenHours.inWard(l, start, q)) continue;
            ward++;
            if (!l.getFluidState(q).isEmpty()) wet++;
        }
        return new int[]{wet, ward};
    }
}
