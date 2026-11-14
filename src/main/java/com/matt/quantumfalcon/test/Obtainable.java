package com.matt.quantumfalcon.test;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * can a survival player actually get this item? An item counts as obtainable if a recipe makes it, a loot table names
 * it (chest, entity, block or gameplay), or a code path grants it ({@link #CODE_GRANTED}, each proven by its own qftest).
 * Returns the reason, or null when nothing produces the item.
 */
public final class Obtainable {
    private Obtainable() {}

    /** Items handed out by Java code rather than data: id -> where (the qftest that proves it). */
    /** Blocks placed in the world by Java worldgen (no template file to read): QuantumUtilityStructure and WildNestFeature. Unwarded, so they can be mined. */
    public static final java.util.Set<String> WORLDGEN_PLACED = java.util.Set.of(
        "crystallized_stone", "crystal_cluster", "echo_lattice", "quantum_block", "quantum_bricks", "quantum_forge", "quantum_glass", "quantum_glass_cyan",
        "quantum_glass_purple", "quantum_incubator", "quantum_nest", "quantum_ore", "quantum_stone", "quantum_tech_cyan", "quantum_tech_purple", "quantum_tiles",
        "rift_frame", "rift_stone", "rift_stone_bricks", "temporal_pillar", "void_bricks", "void_obsidian", "void_stone_bricks",
        "crystal_nest", "falcon_egg_block", "quantum_aerie", "twig_nest", "voidforged_nest");

    public static final Map<String, String> CODE_GRANTED = Map.of(
        "quantumfalcon:artifact_fragment", "BossDeathHandler, every credited Warden/Twins kill (audit_qu1_main_chain, audit_r22_fragments_toss)",
        "quantumfalcon:overseers_eye", "BossDeathHandler, every credited Overseer kill (audit_qu1_main_chain)",
        "quantumfalcon:wardens_fragment", "BossDeathHandler, every credited Warden kill (audit_qu1_main_chain)",
        "quantumfalcon:twin_essence", "BossDeathHandler, every credited Twins kill (audit_qu1_main_chain)",
        "quantumfalcon:sovereigns_crown", "BossDeathHandler, every credited Sovereign kill (audit_qu1_main_chain)");

    private static Map<String, String> lootIndex;

    public static String why(MinecraftServer server, String id) {
        ResourceLocation rl = ResourceLocation.parse(id);
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
        for (var h : server.getRecipeManager().getRecipes()) {
            ItemStack out = h.value().getResultItem(server.registryAccess());
            if (!out.isEmpty() && out.is(item)) return "recipe " + h.id();
        }
        String loot = loot(server).get(id);
        if (loot != null) return "loot " + loot;
        return CODE_GRANTED.containsKey(id) ? "code " + CODE_GRANTED.get(id) : null;
    }

    private static synchronized Map<String, String> loot(MinecraftServer server) {
        if (lootIndex != null) return lootIndex;
        Map<String, String> idx = new HashMap<>();
        var rm = server.getResourceManager();
        for (var e : rm.listResources("loot_table", p -> p.getPath().endsWith(".json")).entrySet()) {
            try (InputStream in = e.getValue().open()) {
                String txt = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                var m = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([a-z0-9_.-]+:[a-z0-9_/.-]+)\"").matcher(txt);
                while (m.find()) idx.putIfAbsent(m.group(1), e.getKey().toString());
            } catch (IOException ignored) { }
        }
        // global loot modifiers that add items (the vanilla-chest injection)
        for (var e : rm.listResources("loot_modifiers", p -> p.getPath().endsWith(".json")).entrySet()) {
            try (InputStream in = e.getValue().open()) {
                String txt = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                var m = java.util.regex.Pattern.compile("\"(?:item|name|id)\"\\s*:\\s*\"([a-z0-9_.-]+:[a-z0-9_/.-]+)\"").matcher(txt);
                while (m.find()) idx.putIfAbsent(m.group(1), e.getKey().toString());
            } catch (IOException ignored) { }
        }
        return lootIndex = idx;
    }
}
