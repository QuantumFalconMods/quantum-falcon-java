package com.matt.quantumfalcon.villager;

import com.google.common.collect.ImmutableSet;
import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.registry.ModBlocks;
import com.matt.quantumfalcon.registry.ModGearItems;
import com.matt.quantumfalcon.registry.ModItems;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Villagers (spec: the 2026-11-14 villager brainstorm review, decisions at the end; hybrid placement).
 * The Falconer is a real vanilla profession: its job site is the falcon_perch, it lives in Overworld villages (a Mews house
 * is added to the five village house pools), and it sells the starter falconry kit, including the Codex.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class ModVillagers {
    private ModVillagers() {}

    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, QuantumFalcon.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, QuantumFalcon.MOD_ID);

    public static final DeferredHolder<PoiType, PoiType> FALCONER_POI = POI_TYPES.register("falconer",
        () -> new PoiType(ImmutableSet.copyOf(ModBlocks.FALCON_PERCH.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final ResourceKey<PoiType> FALCONER_POI_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "falconer"));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> FALCONER = PROFESSIONS.register("falconer",
        () -> new VillagerProfession("falconer", h -> h.is(FALCONER_POI_KEY), h -> h.is(FALCONER_POI_KEY), ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER));

    public static void register(IEventBus bus) { POI_TYPES.register(bus); PROFESSIONS.register(bus); }

    /** The village biomes that get a Mews, and the template each uses (tools/gen_villagers.py). */
    public static final String[] VILLAGE_BIOMES = { "plains", "taiga", "desert", "savanna", "snowy" };
    /** How many entries of the Mews go into each house pool: plains_cartographer_1 has weight 1 in a pool of ~150. */
    public static final int MEWS_WEIGHT = 5;

    // ------------------------------------------------------------------ trades
    private static VillagerTrades.ItemListing sell(int emeralds, ItemLike extra, int extraCount, ItemLike what, int count, int maxUses, int xp) {
        return (e, r) -> new MerchantOffer(new ItemCost(Items.EMERALD, emeralds),
            extra == null ? java.util.Optional.empty() : java.util.Optional.of(new ItemCost(extra, extraCount)), new ItemStack(what, count), maxUses, xp, 0.05f);
    }
    private static VillagerTrades.ItemListing buy(ItemLike what, int count, int maxUses, int xp) {
        return (e, r) -> new MerchantOffer(new ItemCost(what, count), new ItemStack(Items.EMERALD), maxUses, xp, 0.05f);
    }

    @SubscribeEvent
    public static void onTrades(VillagerTradesEvent e) {
        if (e.getType() != FALCONER.get()) return;
        var t = e.getTrades();
        // Novice: leather in; the Codex for emeralds + a book (no rift shard sold, G1)
        t.get(1).add(buy(Items.LEATHER, 6, 16, 2));
        t.get(1).add(sell(3, Items.BOOK, 1, ModItems.QUANTUM_CODEX.get(), 1, 1, 5));
        // Apprentice
        t.get(2).add(sell(4, null, 0, ModBlocks.FALCON_PERCH.get(), 1, 12, 10));
        t.get(2).add(sell(2, null, 0, ModItems.FALCON_TREAT.get(), 4, 16, 5));
        // Journeyman
        t.get(3).add(sell(6, null, 0, ModItems.FALCON_WHISTLE.get(), 1, 12, 10));
        t.get(3).add(sell(4, Items.LEATHER, 2, ModItems.LEATHER_FALCON_BARDING.get(), 1, 12, 10));
        // Expert: iron barding, and the Falconer's Map to the nearest Ancient Nest
        t.get(4).add(sell(8, Items.IRON_INGOT, 2, ModItems.IRON_FALCON_BARDING.get(), 1, 12, 15));
        t.get(4).add(new VillagerTrades.TreasureMapForEmeralds(8, com.matt.quantumfalcon.world.ExplorerMaps.tag("quantumfalcon:on_falconer_maps"),
            "filled_map.quantumfalcon.ancient_nest", MapDecorationTypes.RED_X, 12, 15));
        // Master
        t.get(5).add(sell(24, Items.LEATHER, 1, ModGearItems.FALCONER_GAUNTLET.get(), 1, 3, 30));
        // Eastwatch can sit 10,000 blocks out; a master Falconer charts the way (the Codex's ri_04 reward is the other route)
        t.get(5).add(new VillagerTrades.TreasureMapForEmeralds(16, com.matt.quantumfalcon.world.ExplorerMaps.tag("quantumfalcon:frozen_hours"),
            "filled_map.quantumfalcon.eastwatch", MapDecorationTypes.RED_X, 6, 30));
    }

    // ------------------------------------------------------------------ the Mews in vanilla villages
    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent e) {
        Registry<StructureTemplatePool> pools = e.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> procs = e.getServer().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
        for (String b : VILLAGE_BIOMES)
            addToPool(pools, procs, ResourceLocation.withDefaultNamespace("village/" + b + "/houses"),
                QuantumFalcon.MOD_ID + ":village/" + b + "/falconer_mews", MEWS_WEIGHT);
    }

    /** Elements added to vanilla pools this run (for the tests). */
    public static final List<String> INJECTED = new ArrayList<>();

    @SuppressWarnings("unchecked")
    static void addToPool(Registry<StructureTemplatePool> pools, Registry<StructureProcessorList> procs, ResourceLocation pool, String template, int weight) {
        StructureTemplatePool p = pools.get(pool);
        if (p == null) return;
        Holder<StructureProcessorList> mossy = procs.getHolder(ResourceKey.create(Registries.PROCESSOR_LIST, ResourceLocation.withDefaultNamespace("mossify_10_percent")))
            .map(h -> (Holder<StructureProcessorList>) h).orElse(null);
        StructurePoolElement el = (mossy != null ? StructurePoolElement.legacy(template, mossy) : StructurePoolElement.legacy(template))
            .apply(StructureTemplatePool.Projection.RIGID);
        try {
            var templates = (it.unimi.dsi.fastutil.objects.ObjectArrayList<StructurePoolElement>) field(p, "templates");
            for (int i = 0; i < weight; i++) templates.add(el);
            var raw = (List<Pair<StructurePoolElement, Integer>>) field(p, "rawTemplates");
            List<Pair<StructurePoolElement, Integer>> copy = new ArrayList<>(raw); copy.add(Pair.of(el, weight));
            java.lang.reflect.Field f = StructureTemplatePool.class.getDeclaredField("rawTemplates"); f.setAccessible(true); f.set(p, copy);
            INJECTED.add(pool + " <- " + template);
        } catch (ReflectiveOperationException ex) {
            QuantumFalcon.LOGGER.error("[villagers] could not add {} to {}: {}", template, pool, ex.toString());
        }
    }

    private static Object field(Object o, String name) throws ReflectiveOperationException {
        java.lang.reflect.Field f = StructureTemplatePool.class.getDeclaredField(name); f.setAccessible(true); return f.get(o);
    }
}
