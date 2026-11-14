package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.registry.ModBlocks;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.villager.ModVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** Villagers (spec the 2026-11-14 villager brainstorm review): the Falconer. */
public final class VillagerTests {
    private VillagerTests() {}

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("villager_falconer", VillagerTests::falconer);
    }

    /**
     * The perch is the Falconer's job site (an unemployed villager next to one takes the job), each level has its spec trades
     * (Codex for 3 emeralds + a book at Novice, never a rift shard; the Falconer's Map at Expert), and the Mews is in all five
     * village house pools with a real template.
     */
    private static void falconer(ServerLevel level) {
        BlockPos p = pad(level, 36000, 36000, Blocks.GRASS_BLOCK);
        WorldDropsTests.force(level, p, true);
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            boolean flat = level.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource;   // no Ancient Nest can generate: the map trade yields no offer
            boolean poi = net.minecraft.world.entity.ai.village.poi.PoiTypes.forState(ModBlocks.FALCON_PERCH.get().defaultBlockState())
                .map(h -> h.is(ModVillagers.FALCONER_POI_KEY)).orElse(false);
            // trades, level by level
            StringBuilder tr = new StringBuilder(); boolean codex = false, map = false, noShard = true; int[] per = new int[6];
            for (int lv = 1; lv <= 5; lv++) {
                Villager v = EntityType.VILLAGER.create(level);
                v.moveTo(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
                v.setVillagerData(new VillagerData(VillagerType.PLAINS, ModVillagers.FALCONER.get(), lv));
                // vanilla adds one level's offers per level-up; ask for all levels up to lv
                var offers = v.getOffers();
                per[lv] = offers.size();
                for (var o : offers) {
                    if (o.getResult().is(ModItems.QUANTUM_CODEX.get()) && o.getCostA().is(Items.EMERALD) && o.getCostB().is(Items.BOOK)) codex = true;
                    if (o.getResult().is(Items.FILLED_MAP)) map = true;
                    if (o.getResult().is(ModItems.RIFT_SHARD.get())) noShard = false;
                }
                v.discard();
            }
            tr.append("offers by level ").append(java.util.Arrays.toString(java.util.Arrays.copyOfRange(per, 1, 6)));
            // job site: an unemployed villager next to a perch takes the Falconer job
            level.setBlock(p.offset(2, 1, 0), ModBlocks.FALCON_PERCH.get().defaultBlockState(), 3);
            Villager u = EntityType.VILLAGER.create(level);
            u.moveTo(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
            u.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
            u.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.COMMAND, null);
            u.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
            level.addFreshEntity(u);
            // the Mews in the pools
            int templates = 0;
            for (String b : ModVillagers.VILLAGE_BIOMES)
                if (level.getStructureManager().get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", "village/" + b + "/falconer_mews")).isPresent()) templates++;
            int injected = ModVillagers.INJECTED.size();
            final boolean fCodex = codex, fMap = map || (flat && per[4] == 1), fNoShard = noShard; final int fTemplates = templates;
            int[] waited = {0};
            Runnable[] poll = new Runnable[1];
            poll[0] = () -> {
                boolean hired = u.getVillagerData().getProfession() == ModVillagers.FALCONER.get();
                if (!hired && waited[0] < 1200) { waited[0] += 40; later(40, poll[0]); return; }
                u.discard();
                level.setBlock(p.offset(2, 1, 0), Blocks.AIR.defaultBlockState(), 3);
                WorldDropsTests.force(level, p, false);
                result("villager_falconer", poi && fCodex && fMap && fNoShard && fTemplates == 5 && injected == 5 && hired,
                    "perch is the falconer job site=" + poi + " | " + tr + " codex (3 emeralds + book)=" + fCodex + " falconer's map=" + fMap + " no rift shard=" + fNoShard
                        + " | unemployed villager took the perch=" + hired + " after " + waited[0] + " ticks | mews templates " + fTemplates + "/5, pools injected " + injected + " " + ModVillagers.INJECTED);
            };
            later(40, poll[0]);
        });
    }
}
