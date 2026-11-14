package com.matt.quantumfalcon.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.result;

/** Tools and mining audit (critique/2026-11-14_tools_mining_audit.md): loot predicates, tool tags, tool obtainability. */
final class TMTests {
    private TMTests() {}

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("tm1_loot_predicates", TMTests::lootPredicates);
        tests.put("tm2_tool_tags", TMTests::toolTags);
        tests.put("tm3_tools_obtainable", TMTests::toolsObtainable);
        tests.put("ms4_falcon_advancements", TMTests::ms4);
        tests.put("ms2_hybrid_names", TMTests::ms2);
        tests.put("ms3_boss_cutscene_audio", TMTests::ms3);
        tests.put("ms5_unreachable_items", TMTests::ms5);
    }

    /** The 98 tool, weapon and gadget items the audit probed. */
    static final List<String> TOOLS = List.of("aqua_axe","aqua_hoe","aqua_pickaxe","aqua_shovel","aqua_sword","biome_teleporter","chaos_axe","chaos_hammer","chaos_hoe","chaos_pickaxe","chaos_shovel","chaos_sword","crystal_axe","crystal_hoe","crystal_pickaxe","crystal_shovel","crystal_sword","echo_axe","echo_dagger","echo_hoe","echo_pickaxe","echo_shovel","echo_sword","entropy_collector","falconer_gauntlet","gold_axe_custom","gold_hoe_custom","gold_pickaxe_custom","gold_shovel_custom","gold_sword_custom","grappling_hook","invisibility_cloak","magenta_axe","magenta_hoe","magenta_pickaxe","magenta_shovel","magenta_sword","phase_axe","phase_hoe","phase_pickaxe","phase_shovel","phase_sword","probability_axe","probability_hoe","probability_pickaxe","probability_shovel","probability_sword","purple_axe","purple_hoe","purple_pickaxe","purple_shovel","purple_sword","quantum_axe","quantum_blade","quantum_bow","quantum_bronze_axe","quantum_bronze_hoe","quantum_bronze_pickaxe","quantum_bronze_shovel","quantum_dagger","quantum_falcon_blade","quantum_grappling_hook","quantum_hoe","quantum_pickaxe","quantum_shovel","quantum_spear","quantum_staff","quantum_sword","reality_splitter","resonance_axe","resonance_hoe","resonance_pickaxe","resonance_shovel","resonance_sword","rift_spear","singularity_axe","singularity_bow","singularity_hoe","singularity_pickaxe","singularity_shovel","singularity_sword","temporal_axe","temporal_hoe","temporal_pickaxe","temporal_scepter","temporal_shield","temporal_shovel","temporal_staff","temporal_sword","void_axe","void_blade","void_cannon","void_hoe","void_pickaxe","void_reaper","void_scythe","void_shovel","void_sword");

    static ItemStack enchanted(ServerLevel level, Item tool, ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ench, int lvl) {
        ItemStack s = new ItemStack(tool);
        if (ench != null) s.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(ench), lvl);
        return s;
    }

    /** Total count of each item id dropped over n rolls of the block's loot table with the given tool. */
    static Map<String, Integer> roll(ServerLevel level, Block b, ItemStack tool, int n) {
        Map<String, Integer> out = new TreeMap<>();
        BlockState st = b.defaultBlockState();
        for (int i = 0; i < n; i++) {
            var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                .withParameter(LootContextParams.TOOL, tool.copy()).withParameter(LootContextParams.BLOCK_STATE, st);
            for (ItemStack d : st.getDrops(params)) out.merge(BuiltInRegistries.ITEM.getKey(d.getItem()).getPath(), d.getCount(), Integer::sum);
        }
        return out;
    }

    static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", path)); }

    /**
     * Silk Touch gives the ore block, a plain pick gives the material, Fortune III raises the yield;
     * quantum_glass drops only with Silk Touch; echo/void stone and quantum logs drop themselves.
     */
    private static void lootPredicates(ServerLevel level) {
        ItemStack plain = new ItemStack(Items.DIAMOND_PICKAXE), silk = enchanted(level, Items.DIAMOND_PICKAXE, Enchantments.SILK_TOUCH, 1),
            fortune = enchanted(level, Items.DIAMOND_PICKAXE, Enchantments.FORTUNE, 3);
        String[][] ores = {{"quantum_ore", "quantum_shard"}, {"deepslate_quantum_ore", "quantum_shard"}, {"end_quantum_ore", "quantum_shard"},
            {"rift_ore", "rift_shard"}, {"echo_ore", "echo_crystal"}, {"temporal_ore", "temporal_crystal"}, {"phase_ore", "phase_crystal"}, {"void_ore", "void_crystal"}};
        StringBuilder d = new StringBuilder(); boolean ok = true;
        for (String[] o : ores) {
            Block b = block(o[0]);
            var p = roll(level, b, plain, 100); var s = roll(level, b, silk, 20); var f = roll(level, b, fortune, 100);
            boolean good = p.getOrDefault(o[1], 0) > 0 && !p.containsKey(o[0]) && s.getOrDefault(o[0], 0) == 20 && s.size() == 1
                && f.getOrDefault(o[1], 0) > p.getOrDefault(o[1], 0) * 1.3;
            ok &= good;
            d.append(o[0]).append(good ? " ok" : " BAD plain=" + p + " silk=" + s + " fortune=" + f).append("; ");
        }
        var gPlain = roll(level, block("quantum_glass"), plain, 10); var gSilk = roll(level, block("quantum_glass"), silk, 10);
        boolean glass = gPlain.isEmpty() && gSilk.getOrDefault("quantum_glass", 0) == 10;
        ok &= glass; d.append("quantum_glass plain=").append(gPlain).append(" silk=").append(gSilk).append("; ");
        for (String self : new String[]{"echo_stone", "void_stone", "quantum_log", "quantum_planks", "eastwatch_feather_pile_then", "eastwatch_feather_pile_now"}) {
            ItemStack tool = self.contains("log") || self.contains("planks") ? new ItemStack(Items.DIAMOND_AXE) : plain;
            var r = roll(level, block(self), tool, 20);
            boolean good = r.getOrDefault(self, 0) == 20;
            ok &= good; d.append(self).append(good ? " ok" : " BAD " + r).append("; ");
        }
        result("tm1_loot_predicates", ok, d.toString());
    }

    /**
     * every QF block that needs a tool has one that harvests it; the 22 blocks the audit found
     * untagged are pickaxe-mineable; probability/resonance ore need diamond; stone brick gates are not axe-mineable.
     */
    private static void toolTags(ServerLevel level) {
        Item[] best = {Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE};
        List<String> noTool = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            if (!id.getNamespace().equals("quantumfalcon")) continue;
            BlockState st = b.defaultBlockState();
            if (!st.requiresCorrectToolForDrops() || st.getDestroySpeed(level, BlockPos.ZERO) < 0) continue;
            boolean any = false;
            for (Item t : best) any |= new ItemStack(t).isCorrectToolForDrops(st);
            if (!any) noTool.add(id.getPath());
        }
        String[] audited = {"quantum_bricks", "quantum_tiles", "quantum_tech_cyan", "quantum_tech_purple", "void_obsidian", "temporal_tiles", "temporal_pillar",
            "singularity_core_block", "chaos_conduit", "entangled_ore", "deepslate_quantum_ore", "end_quantum_ore", "crystallized_stone", "quantum_incubator",
            "rift_igniter", "void_bricks", "rift_stone", "rift_stone_bricks", "echo_lattice", "crystal_cluster", "aqua_aegis_reliquary"};
        List<String> untagged = new ArrayList<>();
        for (String a : audited) if (!block(a).defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)) untagged.add(a);
        boolean diamondOres = !new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(block("probability_ore").defaultBlockState())
            && !new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(block("resonance_ore").defaultBlockState())
            && new ItemStack(Items.DIAMOND_PICKAXE).isCorrectToolForDrops(block("resonance_ore").defaultBlockState());
        boolean gates = !block("riftslate_brick_fence_gate").defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE);
        boolean stoneNeedsPick = block("quantum_stone").defaultBlockState().requiresCorrectToolForDrops()
            && new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(block("quantum_stone").defaultBlockState());
        boolean ores = block("quantum_ore").defaultBlockState().is(net.minecraft.tags.TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores")));
        result("tm2_tool_tags", noTool.isEmpty() && untagged.isEmpty() && diamondOres && gates && stoneNeedsPick && ores,
            "blocks needing a tool with none that harvests them: " + noTool + " | audited untagged still untagged: " + untagged
                + " | probability/resonance ore diamond-gated=" + diamondOres + " | stone brick gates off mineable/axe=" + gates
                + " | quantum_stone needs a (wooden) pick=" + stoneNeedsPick + " | quantum_ore in c:ores=" + ores);
    }

    /**
     * taming a real quantum_falcon earns tame_falcon; breed_legendary is granted by the hatch hook; and no QF
     * advancement names an entity type that isn't registered.
     */
    private static void ms4(ServerLevel level) {
        var server = level.getServer();
        var fp = AuditTests.freshPlayer(level, "QFAdvTamer");
        var adv = server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", "tame_falcon"));
        var leg = server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", "breed_legendary"));
        boolean tamed = false, legendary = false;
        if (adv != null && leg != null) {
            // FakePlayer advancement progress is a no-op in NeoForge, so check the criterion itself against a real taming context
            var f = com.matt.quantumfalcon.registry.ModEntities.QUANTUM_FALCON.get().create(level);
            f.moveTo(fp.getX(), fp.getY(), fp.getZ());
            level.addFreshEntity(f);
            f.tame(fp);
            var crit = adv.value().criteria().get("tame_falcon");
            if (crit != null && crit.trigger() == net.minecraft.advancements.CriteriaTriggers.TAME_ANIMAL
                    && crit.triggerInstance() instanceof net.minecraft.advancements.critereon.TameAnimalTrigger.TriggerInstance ti)
                tamed = f.isTame() && ti.matches(net.minecraft.advancements.critereon.EntityPredicate.createContext(fp, f));
            f.discard();
            var lcrit = leg.value().criteria().get("breed_legendary");
            legendary = lcrit != null && lcrit.trigger() == net.minecraft.advancements.CriteriaTriggers.IMPOSSIBLE
                    && com.matt.quantumfalcon.blockentity.FalconEggBlockEntity.awardLegendary(fp);
        }
        List<String> bad = new ArrayList<>();
        var typeRef = java.util.regex.Pattern.compile("\"type\"\\s*:\\s*\"(quantumfalcon:[a-z0-9_]+)\"");
        for (var e : server.getResourceManager().listResources("advancement", p -> p.getNamespace().equals("quantumfalcon") && p.getPath().endsWith(".json")).entrySet()) {
            try (InputStream in = e.getValue().open()) {
                var m = typeRef.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                while (m.find()) if (!BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(m.group(1)))) bad.add(e.getKey().getPath() + "->" + m.group(1));
            } catch (Exception ignored) { }
        }
        result("ms4_falcon_advancements", adv != null && leg != null && tamed && legendary && bad.isEmpty(),
            "tame_falcon loaded=" + (adv != null) + " earned by taming a quantum_falcon=" + tamed + " | breed_legendary granted by hatch hook=" + legendary + " | unregistered entity types in advancements: " + bad);
    }

    /** every falcon variant (base and hybrid) has a name in en_us.json. */
    private static void ms2(ServerLevel level) {
        List<String> missing = new ArrayList<>();
        try (InputStream in = TMTests.class.getResourceAsStream("/assets/quantumfalcon/lang/en_us.json")) {
            var lang = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            for (var v : com.matt.quantumfalcon.breeding.BreedingData.VARIANTS)
                if (!lang.has("falcon_variant.quantumfalcon." + v.id())) missing.add(v.id());
        } catch (Exception ex) { missing.add(ex.toString()); }
        result("ms2_hybrid_names", missing.isEmpty(), "variants without a name: " + missing + " of " + com.matt.quantumfalcon.breeding.BreedingData.VARIANTS.size());
    }

    /** each of the 42 boss cutscene/voice events exists, and a boss intro, phase and death cutscene plays its beat. */
    private static void ms3(ServerLevel level) {
        List<String> missing = new ArrayList<>();
        for (String id : new String[]{"warden", "overseer", "twins", "sovereign", "eclipse", "void_lord", "sha_sha"})
            for (String beat : new String[]{"intro", "phase", "death"})
                for (String layer : new String[]{"boss_cutscene_", "boss_voice_"})
                    if (com.matt.quantumfalcon.registry.ModSounds.byId(layer + id + "_" + beat) == null) missing.add(layer + id + "_" + beat);
        var boss = com.matt.quantumfalcon.registry.ModEntities.RIFT_WARDEN.get().create(level);
        boss.moveTo(0.5, 200, 0.5); boss.setNoAi(true);
        level.addFreshEntity(boss);
        com.matt.quantumfalcon.entity.boss.BossCutscenes.AUDIO_LOG.clear();
        for (byte k : new byte[]{com.matt.quantumfalcon.network.CutscenePayload.INTRO, com.matt.quantumfalcon.network.CutscenePayload.PHASE, com.matt.quantumfalcon.network.CutscenePayload.DEATH})
            com.matt.quantumfalcon.entity.boss.BossCutscenes.play(boss, k, 0xFFFFFF);
        var log = new ArrayList<>(com.matt.quantumfalcon.entity.boss.BossCutscenes.AUDIO_LOG);
        boss.discard();
        result("ms3_boss_cutscene_audio", missing.isEmpty() && log.equals(List.of("warden_intro", "warden_phase", "warden_death")),
            "missing events " + missing + " (of 42) | warden cutscenes played " + log);
    }

    /**
     * every QF item a survival player can't reach is either hidden (c:hidden_from_recipe_viewers or the block hide
     * list) or a spawn egg / dungeon fixture. Lists the rest.
     */
    private static void ms5(ServerLevel level) {
        Set<Item> have = closure(level.getServer());
        List<String> shown = new ArrayList<>(), hidden = new ArrayList<>();
        for (Item it : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(it);
            if (!id.getNamespace().equals("quantumfalcon") || have.contains(it)) continue;
            String p = id.getPath();
            if (it instanceof net.minecraft.world.item.SpawnEggItem || p.endsWith("_spawn_egg") || FIXTURES.contains(p)) continue;
            if (it.builtInRegistryHolder().is(com.matt.quantumfalcon.registry.ModCreativeTabs.HIDDEN_TAG)
                || com.matt.quantumfalcon.registry.ModCreativeTabs.CREATIVE_HIDDEN.contains(p)) hidden.add(p); else shown.add(p);
        }
        Collections.sort(shown); Collections.sort(hidden);
        List<String> audited = shown.stream().filter(MS5_AUDITED::contains).toList();
        result("ms5_unreachable_items", audited.isEmpty(), "of the audit's 90 sourceless items, still shown: " + audited + " | closure (lower bound: structure templates and Java placements aren't seeded) unreachable and shown " + shown.size() + ": " + shown + " | unreachable and hidden " + hidden.size() + ": " + hidden);
    }

    /** the 90 non-block items the missing-content sweep found with no survival source. */
    static final Set<String> MS5_AUDITED = Set.of("aqua_axe", "aqua_gem", "aqua_hoe", "aqua_ingot", "aqua_nugget", "aqua_pickaxe", "aqua_shovel", "aqua_sword", "aurora_essence", "biome_teleporter", "boss_trophy", "breeding_catalyst", "chaos_dust", "chaos_essence", "convergence_key", "cosmetic_crate", "crystal_core", "crystal_dust", "crystal_essence", "dimensional_anchor", "echo_core", "echo_dust", "echo_essence", "echo_key", "egg_fragment_legendary", "egg_fragment_rare", "entropy_collector", "fake_death_potion", "gold_gem", "gold_nugget_custom", "gravity_anchor", "horizon_essence", "invisibility_cloak", "legendary_egg", "legendary_falcon_egg", "legendary_falcon_feather", "lockpick", "magenta_nugget", "mutation_serum", "name_tag_quantum", "phase_core", "phase_crystal", "phase_dust", "phase_essence", "prismatic_fish", "prismatic_shard", "probability_core", "probability_dice", "probability_dust", "probability_essence", "probability_shard", "purple_gem", "purple_nugget", "quantum_bronze_axe", "quantum_bronze_hoe", "quantum_bronze_pickaxe", "quantum_bronze_shovel", "quantum_capacitor", "quantum_catalyst", "quantum_dust", "quantum_egg", "quantum_essence", "quantum_falcon_egg", "quantum_fertilizer", "quantum_fish", "quantum_grappling_hook", "quantum_knot", "quantum_reroll_token", "resonance_core", "resonance_crystal", "resonance_dust", "resonance_essence", "resonance_flower", "rift_fragment", "shiny_charm", "singularity_crown", "singularity_dust", "singularity_egg", "singularity_essence", "sovereign_sigil", "stability_shard", "temporal_alloy", "temporal_essence", "temporal_shield", "time_essence", "trait_reroll_stone", "treasure_compass", "void_cannon", "void_fragment", "void_heart");

    /** Dungeon and arena fixtures: placed by structures, never meant to be held. */
    static final Set<String> FIXTURES = Set.of("dungeon_checkpoint", "dungeon_key_door", "dungeon_guard", "treasury_vault", "return_rift", "hour_socket",
        "arena_seal", "arena_rift_tile", "rift_portal", "secret_wall");

    // ------------------------------------------------------------------ survival closure
    /** Items a survival player can reach: vanilla items, non-block loot, loot modifiers, code grants, Codex rewards, worldgen blocks, then block drops and recipes to a fixpoint. */
    static Set<Item> closure(MinecraftServer server) {
        Set<Item> have = new HashSet<>();
        for (Item i : BuiltInRegistries.ITEM) if (BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("minecraft")) have.add(i);
        have.remove(Items.AIR);
        var rm = server.getResourceManager();
        var name = java.util.regex.Pattern.compile("\"(?:name|item|id|Name)\"\\s*:\\s*\"(quantumfalcon:[a-z0-9_/.-]+)\"");
        Map<String, List<Item>> blockDrops = new HashMap<>();
        for (var e : rm.listResources("loot_table", p -> p.getPath().endsWith(".json")).entrySet()) {
            String path = e.getKey().getPath();
            List<Item> items = new ArrayList<>();
            try (InputStream in = e.getValue().open()) {
                var m = name.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                while (m.find()) items.add(BuiltInRegistries.ITEM.get(ResourceLocation.parse(m.group(1))));
            } catch (Exception ignored) { }
            if (path.startsWith("loot_table/blocks/")) blockDrops.put(path.substring("loot_table/blocks/".length(), path.length() - 5), items);
            else have.addAll(items);
        }
        for (String dir : new String[]{"loot_modifiers", "worldgen/configured_feature", "worldgen/noise_settings", "worldgen/placed_feature", "frozen_hours"})
            for (var e : rm.listResources(dir, p -> p.getPath().endsWith(".json")).entrySet()) {
                if (!e.getKey().getNamespace().equals("quantumfalcon") && dir.startsWith("worldgen")) continue;
                try (InputStream in = e.getValue().open()) {
                    var m = name.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    while (m.find()) {
                        String id = m.group(1);
                        if (dir.equals("loot_modifiers")) { have.add(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))); continue; }
                        // a worldgen block: its drops are reachable
                        String b = ResourceLocation.parse(id).getPath();
                        for (Item it : blockDrops.getOrDefault(b, List.of())) have.add(it);
                    }
                } catch (Exception ignored) { }
            }
        for (String id : Obtainable.CODE_GRANTED.keySet()) have.add(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
        for (var q : com.matt.quantumfalcon.codex.RiftCodex.QUESTS.values()) for (ResourceLocation r : q.reward().items()) have.add(BuiltInRegistries.ITEM.get(r));
        have.remove(Items.AIR);
        var recipes = server.getRecipeManager().getRecipes();
        boolean grew = true;
        while (grew) {
            grew = false;
            for (var e : blockDrops.entrySet()) {
                Item self = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", e.getKey()));
                if (self != Items.AIR && have.contains(self)) for (Item it : e.getValue()) grew |= it != Items.AIR && have.add(it);
            }
            for (var h : recipes) {
                var r = h.value();
                ItemStack out = r.getResultItem(server.registryAccess());
                if (out.isEmpty() || have.contains(out.getItem())) continue;
                boolean ok;
                if (r instanceof SmithingTransformRecipe s) {
                    ok = have.stream().anyMatch(i -> s.isTemplateIngredient(new ItemStack(i))) && have.stream().anyMatch(i -> s.isBaseIngredient(new ItemStack(i)))
                        && have.stream().anyMatch(i -> s.isAdditionIngredient(new ItemStack(i)));
                } else {
                    ok = true;
                    for (Ingredient ing : r.getIngredients()) {
                        if (ing.isEmpty()) continue;
                        boolean any = false;
                        for (ItemStack opt : ing.getItems()) if (have.contains(opt.getItem())) { any = true; break; }
                        if (!any) { ok = false; break; }
                    }
                }
                if (ok) { have.add(out.getItem()); grew = true; }
            }
        }
        return have;
    }

    /** how many of the audit's 98 tools and weapons a survival player can reach (closure above). */
    private static void toolsObtainable(ServerLevel level) {
        Set<Item> have = closure(level.getServer());
        List<String> missing = new ArrayList<>();
        int n = 0;
        for (String id : TOOLS) {
            Item it = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", id));
            if (have.contains(it)) n++; else missing.add(id);
        }
        long qf = have.stream().filter(i -> BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("quantumfalcon")).count();
        result("tm3_tools_obtainable", missing.stream().allMatch(HIDDEN::contains),
            "tools/weapons obtainable " + n + "/" + TOOLS.size() + " | not obtainable " + missing + " (hidden on purpose: " + HIDDEN + ") | QF items reachable in the closure " + qf);
    }

    /** Gadgets with no behaviour (plain Items): hidden from the creative tabs and recipe viewers rather than made craftable. */
    static final Set<String> HIDDEN = Set.of("biome_teleporter", "entropy_collector", "temporal_shield");
}
