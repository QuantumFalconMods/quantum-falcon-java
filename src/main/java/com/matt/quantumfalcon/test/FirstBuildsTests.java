package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.entity.npc.RiftVillagerEntity;
import com.matt.quantumfalcon.registry.ModBlocks;
import com.matt.quantumfalcon.registry.ModFamilyBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** First structure builds (Mews x5, Anchor Chapel, Voidsmithy, Stitchring) + villagers v3 + the two decor blocks. */
final class FirstBuildsTests {
    private FirstBuildsTests() { }

    static void register(Map<String, Consumer<ServerLevel>> t) {
        t.put("first_builds_templates", FirstBuildsTests::templates);
        t.put("first_builds_decor_blocks", FirstBuildsTests::decor);
        t.put("first_builds_mews_natural", FirstBuildsTests::mewsNatural);
        t.put("first_builds_stitchring_natural", FirstBuildsTests::stitchringNatural);
        t.put("villager_rift_trades", FirstBuildsTests::riftTrades);
    }

    private static StructureTemplate tpl(ServerLevel l, String path) {
        return l.getStructureManager().get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", path)).orElse(null);
    }
    static void templates(ServerLevel l) {
        List<String> bad = new ArrayList<>();
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        for (String b : new String[]{"plains", "taiga", "snowy", "savanna", "desert"}) {
            var t = tpl(l, "village/" + b + "/falconer_mews");
            if (t == null) { bad.add(b + " mews missing"); continue; }
            var stands = t.filterBlocks(BlockPos.ZERO, settings, ModBlocks.PERCH_STAND.get(), false);
            long hooded = stands.stream().filter(s -> s.state().getValue(com.matt.quantumfalcon.block.PerchStandBlock.HOODED)).count();
            int perches = t.filterBlocks(BlockPos.ZERO, settings, ModBlocks.FALCON_PERCH.get(), false).size();
            if (stands.size() != 3 || hooded != 2 || perches != 1 || t.getSize().getX() != 15) bad.add(b + " stands=" + stands.size() + " hooded=" + hooded + " perches=" + perches + " size=" + t.getSize());
        }
        var plaza = tpl(l, "village/stitchring/center_plaza"); var chapel = tpl(l, "village/stitchring/anchor_chapel"); var smithy = tpl(l, "village/stitchring/voidsmithy");
        if (plaza == null || chapel == null || smithy == null) { result("first_builds_templates", false, "stitchring templates missing " + bad); return; }
        int bells = plaza.filterBlocks(BlockPos.ZERO, settings, ModBlocks.RESONANCE_BELL.get(), false).size() + chapel.filterBlocks(BlockPos.ZERO, settings, ModBlocks.RESONANCE_BELL.get(), false).size();
        int vanillaBells = plaza.filterBlocks(BlockPos.ZERO, settings, Blocks.BELL, false).size() + chapel.filterBlocks(BlockPos.ZERO, settings, Blocks.BELL, false).size();
        var stab = ModFamilyBlocks.RIFT_STABILIZER.get();
        boolean plazaLive = plaza.filterBlocks(BlockPos.ZERO, settings, stab, false).stream().allMatch(s -> s.state().getValue(com.matt.quantumfalcon.block.RiftStabilizerBlock.ACTIVE))
            && plaza.filterBlocks(BlockPos.ZERO, settings, Blocks.REDSTONE_BLOCK, false).size() == 1;
        boolean chapelCold = chapel.filterBlocks(BlockPos.ZERO, settings, stab, false).stream().noneMatch(s -> s.state().getValue(com.matt.quantumfalcon.block.RiftStabilizerBlock.ACTIVE))
            && chapel.filterBlocks(BlockPos.ZERO, settings, Blocks.REDSTONE_BLOCK, false).isEmpty();
        String chapelRes = residents(chapel), smithyRes = residents(smithy);
        int residentTpls = 0;
        for (String p : new String[]{"falconer", "pedigree_registrar", "aerie_mender", "wayfinder", "chronicler"}) if (residents(tpl(l, "village/stitchring/residents/" + p)).contains(p)) residentTpls++;
        boolean ok = bad.isEmpty() && bells == 2 && vanillaBells == 0 && plazaLive && chapelCold && chapelRes.equals("[anchorkeeper]") && smithyRes.equals("[void_reforger]") && residentTpls == 5;
        result("first_builds_templates", ok, "mews problems=" + bad + " | resonance bells=" + bells + " vanilla bells=" + vanillaBells + " | plaza stabilizer powered=" + plazaLive
            + " chapel unpowered=" + chapelCold + " | chapel residents=" + chapelRes + " smithy=" + smithyRes + " resident templates=" + residentTpls + "/5");
    }

    @SuppressWarnings("unchecked")
    private static String residents(StructureTemplate t) {
        if (t == null) return "[]";
        try {
            var f = StructureTemplate.class.getDeclaredField("entityInfoList"); f.setAccessible(true);
            List<String> out = new ArrayList<>();
            for (StructureTemplate.StructureEntityInfo e : (List<StructureTemplate.StructureEntityInfo>) f.get(t))
                if (e.nbt.getString("id").equals("quantumfalcon:rift_villager")) out.add(e.nbt.getString("Profession"));
            return out.toString();
        } catch (ReflectiveOperationException ex) { return "[reflect " + ex + "]"; }
    }

    /** perch_stand: no POI, empty-stand drop, hooded outline above the block but collision inside; bell rings and traces a stabilizer. */
    static void decor(ServerLevel l) {
        BlockPos pad = new BlockPos(5600, 0, -5600);
        WorldDropsTests.force(l, pad, true);
        WorldDropsTests.whenTicking(l, pad.atY(l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pad.getX(), pad.getZ())), 0, () -> {
            BlockPos p = pad.atY(l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pad.getX(), pad.getZ()) + 2);
            var hooded = ModBlocks.PERCH_STAND.get().defaultBlockState().setValue(com.matt.quantumfalcon.block.PerchStandBlock.HOODED, true);
            l.setBlock(p, hooded, 3);
            double outlineTop = l.getBlockState(p).getShape(l, p).max(net.minecraft.core.Direction.Axis.Y);
            double collTop = l.getBlockState(p).getCollisionShape(l, p).max(net.minecraft.core.Direction.Axis.Y);
            boolean poi = l.getPoiManager().getType(p).isPresent();
            var drops = Block.getDrops(l.getBlockState(p), l, p, null);
            boolean dropsEmpty = drops.size() == 1 && drops.get(0).is(ModBlocks.PERCH_STAND.get().asItem());
            BlockPos perch = p.east(2); l.setBlock(perch, ModBlocks.FALCON_PERCH.get().defaultBlockState(), 3);
            boolean perchPoi = l.getPoiManager().getType(perch).isPresent();
            BlockPos stab = p.south(4), bell = p.above(3);
            l.setBlock(stab, ModFamilyBlocks.RIFT_STABILIZER.get().defaultBlockState(), 3);
            l.setBlock(bell, ModBlocks.RESONANCE_BELL.get().defaultBlockState().setValue(com.matt.quantumfalcon.block.ResonanceBellBlock.ATTACHMENT, net.minecraft.world.level.block.state.properties.BellAttachType.CEILING), 3);
            BlockPos traced = com.matt.quantumfalcon.block.ResonanceBellBlock.ring(l, bell);
            // redstone: a block of redstone beside the bell powers it
            l.setBlock(bell.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            boolean powered = l.getBlockState(bell).getValue(com.matt.quantumfalcon.block.ResonanceBellBlock.POWERED);
            boolean bellBe = l.getBlockEntity(bell) != null;
            for (BlockPos q : new BlockPos[]{p, perch, stab, bell, bell.west()}) l.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
            WorldDropsTests.force(l, pad, false);
            boolean ok = outlineTop > 1.25 && collTop <= 1.0 && !poi && dropsEmpty && perchPoi && stab.equals(traced) && powered && !bellBe;
            result("first_builds_decor_blocks", ok, "perch_stand hooded outline top=" + outlineTop + " collision top=" + collTop + " POI=" + poi + " drops empty stand=" + dropsEmpty
                + " (falcon_perch POI=" + perchPoi + ") | bell traced stabilizer=" + stab.equals(traced) + " powered by redstone=" + powered + " block entity=" + bellBe);
        });
    }

    /** Real jigsaw assembly of plains villages: how many carry a Mews, and one is placed and checked block by block. */
    static void mewsNatural(ServerLevel l) {
        var reg = l.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Holder<Structure> village = reg.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.withDefaultNamespace("village_plains")));
        var gen = l.getChunkSource().getGenerator();
        int withMews = 0, most = 0, tries = 120; StructureStart chosen = null; int[] cal = new int[3];
        for (int i = 0; i < tries; i++) {
            ChunkPos cp = new ChunkPos(400 + i * 20, -400);
            StructureStart s = village.value().generate(l.registryAccess(), gen, gen.getBiomeSource(), l.getChunkSource().randomState(), l.getStructureManager(), l.getSeed() + i * 7919L, cp, 0, l, b -> true);
            if (!s.isValid()) continue;
            int n = 0;
            for (var piece : s.getPieces()) { String ps = piece.toString(); if (ps.contains("falconer_mews")) n++; if (ps.contains("butcher_shop_2")) cal[0]++; if (ps.contains("library_1")) cal[1]++; cal[2]++; }
            if (n > 0) { withMews++; if (chosen == null) chosen = s; }
            most = Math.max(most, n);
        }
        if (chosen == null) {
            // diagnostics: is our element in the houses pool, and what does the game think it looks like?
            var pools = l.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
            var housePool = pools.get(ResourceLocation.withDefaultNamespace("village/plains/houses"));
            StringBuilder diag = new StringBuilder();
            if (housePool != null) {
                int ours = 0, total = 0;
                for (var el : housePool.getShuffledTemplates(net.minecraft.util.RandomSource.create(1))) {
                    total++;
                    if (el.toString().contains("falconer_mews")) {
                        if (ours++ == 0) {
                            var sz = el.getSize(l.getStructureManager(), net.minecraft.world.level.block.Rotation.NONE);
                            var js = el.getShuffledJigsawBlocks(l.getStructureManager(), BlockPos.ZERO, net.minecraft.world.level.block.Rotation.NONE, net.minecraft.util.RandomSource.create(1));
                            diag.append(" size=").append(sz.toShortString()).append(" jigsaws=");
                            for (var j : js) diag.append(j.pos().toShortString()).append(' ').append(j.nbt() == null ? "?" : j.nbt().getString("name")).append(' ').append(j.state().getValue(net.minecraft.world.level.block.JigsawBlock.ORIENTATION)).append("; ");
                        }
                    }
                }
                diag.append(" ours ").append(ours).append('/').append(total).append(" pool entries");
            }
            int maxStreet = 0;
            result("first_builds_mews_natural", false, "no Mews in " + tries + " plains villages |" + diag + " | calibration: butcher_shop_2 (15x12x7)=" + cal[0] + " library_1=" + cal[1] + " pieces=" + cal[2]); return; }
        final StructureStart start = chosen; final int fw = withMews, fm = most, ft = tries;
        BoundingBox bb = start.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) l.setChunkForced(cx, cz, true);
        QFTestCommands.later(40, () -> {
            for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                ChunkPos c = new ChunkPos(cx, cz);
                start.placeInChunk(l, l.structureManager(), l.getChunkSource().getGenerator(), l.getRandom(),
                    new BoundingBox(c.getMinBlockX(), l.getMinBuildHeight(), c.getMinBlockZ(), c.getMaxBlockX(), l.getMaxBuildHeight(), c.getMaxBlockZ()), c);
            }
            int stands = 0, hooded = 0, perches = 0, trapdoorsOnStands = 0;
            for (BlockPos q : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
                var s = l.getBlockState(q);
                if (s.is(ModBlocks.PERCH_STAND.get())) { stands++; if (s.getValue(com.matt.quantumfalcon.block.PerchStandBlock.HOODED)) hooded++; if (l.getBlockState(q.above()).getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock) trapdoorsOnStands++; }
                else if (s.is(ModBlocks.FALCON_PERCH.get())) perches++;
            }
            for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) l.setChunkForced(cx, cz, false);
            boolean ok = stands >= 3 && stands % 3 == 0 && hooded * 3 == stands * 2 && perches == stands / 3 && trapdoorsOnStands == 0;
            result("first_builds_mews_natural", ok, "[calibration over the same villages: butcher_shop_2=" + cal[0] + " library_1=" + cal[1] + "] " + fw + "/" + ft + " plains villages carry a Mews (most in one village=" + fm + ") | placed one at " + bb.getCenter().toShortString()
                + ": perch stands=" + stands + " hooded=" + hooded + " falcon perches (job site)=" + perches + " trapdoors left on stands=" + trapdoorsOnStands);
        });
    }

    /** Locate a Stitchring in the Echo Rift, check its pieces, residents, bells and its distance from every boss arena. */
    static void stitchringNatural(ServerLevel ow) {
        ServerLevel rift = ow.getServer().getLevel(com.matt.quantumfalcon.dimension.EchoRiftDimension.ECHO_RIFT_LEVEL_KEY);
        if (rift == null) { result("first_builds_stitchring_natural", false, "no echo_rift level"); return; }
        var reg = rift.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var holder = reg.getHolder(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("quantumfalcon", "waystation_stitchring"))).orElse(null);
        if (holder == null) { result("first_builds_stitchring_natural", false, "structure not registered"); return; }
        var found = rift.getChunkSource().getGenerator().findNearestMapStructure(rift, net.minecraft.core.HolderSet.direct(holder), BlockPos.ZERO, 120, false);
        if (found == null) { result("first_builds_stitchring_natural", false, "no Stitchring within 120 chunks of the Rift origin"); return; }
        // sample assemblies in memory: how often each piece turns up
        Map<String, Integer> seen = new TreeMap<>(); int valid = 0;
        { var gen = rift.getChunkSource().getGenerator();
          for (int i = 0; i < 16; i++) {
            StructureStart t = holder.value().generate(rift.registryAccess(), gen, gen.getBiomeSource(), rift.getChunkSource().randomState(), rift.getStructureManager(), rift.getSeed() + i * 104729L, new ChunkPos(found.getFirst().getX() >> 4, found.getFirst().getZ() >> 4), 0, rift, b -> true);
            if (!t.isValid()) continue; valid++;
            Set<String> ps = new TreeSet<>();
            for (var piece : t.getPieces()) { String s = piece.toString(); int k = s.indexOf("stitchring/"); if (k >= 0) ps.add(s.substring(k + 11).replaceAll("[^a-z_/].*", "")); }
            ps.forEach(n -> seen.merge(n, 1, Integer::sum));
          } }
        final String sampled = valid + " sampled: " + seen;
        BlockPos p = found.getFirst();
        var located = rift.getChunk(p.getX() >> 4, p.getZ() >> 4).getStartForStructure(holder.value());
        if (located == null || !located.isValid()) { result("first_builds_stitchring_natural", false, "located " + p.toShortString() + " but no valid start"); return; }
        // the located village may be a start saved by an older build: assemble a fresh one on new ground with today's pools
        StructureStart start = null;
        { var gen = rift.getChunkSource().getGenerator();
          for (int i = 0; i < 24 && start == null; i++) {
            StructureStart t = holder.value().generate(rift.registryAccess(), gen, gen.getBiomeSource(), rift.getChunkSource().randomState(), rift.getStructureManager(), rift.getSeed() + i * 7L, new ChunkPos((p.getX() >> 4) + 200 + i * 9, (p.getZ() >> 4) + 200), 0, rift, b -> true);
            if (t.isValid() && t.getPieces().stream().anyMatch(q -> q.toString().contains("stitchring/voidsmithy"))) start = t;
          } }
        if (start == null) { result("first_builds_stitchring_natural", false, "no complete assembly in 24 tries | " + sampled); return; }
        { BoundingBox b0 = start.getBoundingBox();
          for (int cx = b0.minX() >> 4; cx <= b0.maxX() >> 4; cx++) for (int cz = b0.minZ() >> 4; cz <= b0.maxZ() >> 4; cz++) {
            rift.setChunkForced(cx, cz, true);
            ChunkPos c = new ChunkPos(cx, cz);
            start.placeInChunk(rift, rift.structureManager(), rift.getChunkSource().getGenerator(), rift.getRandom(),
                new BoundingBox(c.getMinBlockX(), rift.getMinBuildHeight(), c.getMinBlockZ(), c.getMaxBlockX(), rift.getMaxBuildHeight(), c.getMaxBlockZ()), c);
          } }
        BoundingBox bb = start.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) rift.setChunkForced(cx, cz, true);
        Set<String> pieces = new TreeSet<>();
        for (var piece : start.getPieces()) { String s = piece.toString(); int i = s.indexOf("stitchring/"); if (i >= 0) pieces.add(s.substring(i + 11).replaceAll("[^a-z_/].*", "")); }
        var setReg = rift.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        var state = rift.getChunkSource().getGeneratorState();
        List<String> nearArenas = new ArrayList<>();
        ChunkPos sc = located.getChunkPos();
        for (String a : new String[]{"quantum_guardian_arena", "void_horror_arena", "entangled_twins_arena", "singularity_sovereign_arena"}) {
            var h = setReg.getHolder(ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath("quantumfalcon", a))).orElse(null);
            if (h != null && state.hasStructureChunkInRange(h, sc.x, sc.z, 8)) nearArenas.add(a);
        }
        QFTestCommands.later(100, () -> {
            int bells = 0, altars = 0, anvils = 0, liveStab = 0;
            for (BlockPos q : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
                var s = rift.getBlockState(q);
                if (s.is(ModBlocks.RESONANCE_BELL.get())) bells++;
                else if (s.is(ModFamilyBlocks.ECHO_ALTAR.get())) altars++;
                else if (s.is(ModFamilyBlocks.VOID_ANVIL.get())) anvils++;
                else if (s.is(ModFamilyBlocks.RIFT_STABILIZER.get()) && s.getValue(com.matt.quantumfalcon.block.RiftStabilizerBlock.ACTIVE)) liveStab++;
            }
            var aabb = net.minecraft.world.phys.AABB.of(bb).inflate(4);
            List<String> res = new ArrayList<>();
            for (var v : rift.getEntitiesOfClass(RiftVillagerEntity.class, aabb)) res.add(v.profession());
            Collections.sort(res);
            for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) rift.setChunkForced(cx, cz, false);
            String biome = rift.getBiome(bb.getCenter()).unwrapKey().map(k -> k.location().getPath()).orElse("?");
            boolean ok = pieces.contains("center_plaza") && pieces.contains("anchor_chapel") && pieces.contains("voidsmithy") && bells == 2 && altars >= 1 && anvils >= 1
                && liveStab == 1 && res.contains("anchorkeeper") && res.contains("void_reforger") && nearArenas.isEmpty();
            result("first_builds_stitchring_natural", ok, "nearest at " + p.toShortString() + ", fresh assembly placed at " + bb.getCenter().toShortString() + " (" + biome + ") pieces=" + pieces + " | resonance bells=" + bells + " altars=" + altars
                + " anvils=" + anvils + " powered stabilizers=" + liveStab + " | residents=" + res + " | arenas within 8 chunks=" + nearArenas + " | " + sampled);
        });
    }

    /** Rift villagers: offers per profession at rank 1, ranking up with trade XP adds the next rank's offers. */
    static void riftTrades(ServerLevel l) {
        List<String> empty = new ArrayList<>();
        var v = com.matt.quantumfalcon.registry.ModEntities.RIFT_VILLAGER.get().create(l);
        for (String p : RiftVillagerEntity.PROFESSIONS) if (v.offersFor(p, 1).isEmpty()) empty.add(p);
        v.setProfession("anchorkeeper");
        int before = v.getOffers().size();
        // fake a few trades' XP: rank 1 -> 2 at 10 XP
        for (var o : v.offersFor("anchorkeeper", 1)) for (int i = 0; i < 3; i++) { try { var m = RiftVillagerEntity.class.getDeclaredMethod("rewardTradeXp", net.minecraft.world.item.trading.MerchantOffer.class); m.setAccessible(true); m.invoke(v, o); } catch (ReflectiveOperationException ex) { empty.add("reflect"); } }
        int tier = v.tier(), after = v.getOffers().size();
        v.discard();
        boolean ok = empty.isEmpty() && before == 2 && tier == 2 && after == 4;
        result("villager_rift_trades", ok, "professions without rank-1 offers=" + empty + " | anchorkeeper offers rank1=" + before + " after 6 trades: rank=" + tier + " offers=" + after);
    }
}
