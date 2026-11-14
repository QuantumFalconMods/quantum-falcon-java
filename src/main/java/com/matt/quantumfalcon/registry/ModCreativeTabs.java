package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Creative tab registry for Quantum Falcon
 */
public class ModCreativeTabs {
    /** Blocks with no survival source or no working function yet, kept out of the tab until Matthew decides. */
    public static final java.util.Set<String> CREATIVE_HIDDEN = java.util.Set.of("boulder_trap", "superposition_block", "end_quantum_ore", "rift_ore", "aurora_ore",
        "deepslate_chaos_ore", "deepslate_crystal_ore", "deepslate_echo_ore", "deepslate_phase_ore", "deepslate_probability_ore", "deepslate_quantum_ore",
        "deepslate_resonance_ore", "deepslate_singularity_ore", "deepslate_temporal_ore", "deepslate_void_ore");

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, QuantumFalcon.MOD_ID);

    /**
     * Main creative tab for all Quantum Falcon items
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> QUANTUM_FALCON_TAB = 
        CREATIVE_TABS.register("quantum_falcon_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.quantumfalcon.main"))
            .icon(() -> new ItemStack(ModItems.QUANTUM_CRYSTAL.get()))
            .displayItems((parameters, output) -> {
                // ========================================
                // QUANTUM MATERIALS
                // ========================================
                output.accept(ModItems.QUANTUM_CRYSTAL.get());
                output.accept(ModItems.QUANTUM_SHARD.get());
                output.accept(ModItems.VOID_ESSENCE.get());
                output.accept(ModItems.REALITY_FRAGMENT.get());
                output.accept(ModItems.QUANTUM_CORE.get());
                output.accept(ModItems.TEMPORAL_FRAGMENT.get());
                // survival items that were in no tab
                output.accept(ModItems.HEART_OF_QUANTUM.get());
                output.accept(ModItems.ARTIFACT_FRAGMENT.get());
                output.accept(ModItems.QUANTUM_COMPASS.get());
                output.accept(ModItems.QUANTUM_BERRY.get());
                output.accept(ModItems.FALCON_JOURNAL.get());
                output.accept(ModItems.GOLDEN_FEATHER_TREAT.get());
                output.accept(ModItems.GOLDEN_FALCON_EGG.get());
                output.accept(ModItems.PHASE_CRYSTAL.get());
                output.accept(ModItems.RIFT_TOTEM.get());
                output.accept(ModItems.SINGULARITY_CORE.get());
                output.accept(ModItems.SINGULARITY_SHARD.get());
                output.accept(ModItems.SINGULARITY_FRAGMENT.get());
                
                // ========================================
                // ECHO MATERIALS
                // ========================================
                output.accept(ModItems.ECHO_SHARD.get());
                output.accept(ModItems.ECHO_CRYSTAL.get());
                output.accept(ModItems.ECHO_KEY.get());
                
                // ========================================
                // VOID MATERIALS
                // ========================================
                output.accept(ModItems.VOID_SHARD.get());
                output.accept(ModItems.VOID_CRYSTAL.get());
                output.accept(ModItems.VOID_FRAGMENT.get());
                
                // ========================================
                // TEMPORAL MATERIALS
                // ========================================
                output.accept(ModItems.TEMPORAL_DUST.get());
                output.accept(ModItems.TEMPORAL_CRYSTAL.get());
                output.accept(ModItems.TEMPORAL_ALLOY.get());
                output.accept(ModItems.TIME_BOTTLE.get());
                
                // ========================================
                // AURORA MATERIALS
                // ========================================
                output.accept(ModItems.AURORA_ESSENCE.get());
                output.accept(ModItems.AURORA_CRYSTAL.get());
                
                // ========================================
                // PROBABILITY & RIFT MATERIALS
                // ========================================
                output.accept(ModItems.PROBABILITY_SHARD.get());
                output.accept(ModItems.PROBABILITY_CORE.get());
                output.accept(ModItems.PROBABILITY_DICE.get());
                output.accept(ModItems.RIFT_FRAGMENT.get());
                output.accept(ModItems.RIFT_CRYSTAL.get());
                output.accept(ModItems.HORIZON_ESSENCE.get());
                
                // ========================================
                // FALCON ITEMS
                // ========================================
                output.accept(ModItems.QUANTUM_KNOT.get());
                output.accept(ModItems.QUANTUM_CATALYST.get());
                output.accept(ModItems.FALCON_WHISTLE.get());
                output.accept(ModItems.LEATHER_FALCON_BARDING.get());
                output.accept(ModItems.IRON_FALCON_BARDING.get());
                output.accept(ModItems.DIAMOND_FALCON_BARDING.get());
                output.accept(ModItems.NETHERITE_FALCON_BARDING.get());
                output.accept(ModItems.QUANTUM_FALCON_BARDING.get());
                output.accept(ModItems.FALCON_TREAT.get());
                output.accept(ModItems.FALCON_FEATHER.get());
                output.accept(ModItems.GOLDEN_FALCON_FEATHER.get());
                output.accept(ModItems.LEGENDARY_FALCON_FEATHER.get());
                output.accept(ModItems.FALCON_SPAWN_EGG.get());
                output.accept(ModItems.CRYSTAL_GUARDIAN_SPAWN_EGG.get());
                output.accept(ModItems.DATA_CONSTRUCT_SPAWN_EGG.get());
                output.accept(ModItems.VOID_ECHO_SPAWN_EGG.get());
                output.accept(ModItems.QUANTUM_PHANTOM_SPAWN_EGG.get());
                output.accept(ModItems.QUANTUM_WISP_SPAWN_EGG.get());
                output.accept(ModItems.ENTANGLED_TWINS_SPAWN_EGG.get());
                output.accept(ModItems.SKYWARDEN_IREL_SPAWN_EGG.get());
                output.accept(ModItems.RIFT_WARDEN_MINION_SPAWN_EGG.get());
                output.accept(ModItems.QUANTUM_OVERSEER_SPAWN_EGG.get());
                output.accept(ModItems.RIFT_WARDEN_SPAWN_EGG.get());
                output.accept(ModItems.SINGULARITY_SOVEREIGN_SPAWN_EGG.get());
                output.accept(ModItems.SHA_SHA_SPAWN_EGG.get());
                output.accept(ModItems.GOBLIN_TRADER_SPAWN_EGG.get());
                output.accept(ModItems.QUANTUM_MERCHANT_SPAWN_EGG.get());
                output.accept(ModItems.QUANTUM_STALKER_SPAWN_EGG.get());
                output.accept(ModItems.ECLIPSE_PHANTOM_SPAWN_EGG.get());
                output.accept(ModItems.VOID_LORD_SPAWN_EGG.get());
                // the fake spawn egg is no longer offered (kept registered for old saves)
                output.accept(ModItems.LEGENDARY_EGG.get());
                
                // ========================================
                // BREEDING & GENETICS
                // ========================================
                output.accept(ModItems.SHINY_CHARM.get());
                output.accept(ModItems.RESONANCE_FLOWER.get());
                output.accept(ModItems.QUANTUM_FERTILIZER.get());
                output.accept(ModItems.QUANTUM_REROLL_TOKEN.get());
                
                // ========================================
                // BOSS DROPS
                // ========================================
                output.accept(ModItems.OVERSEERS_EYE.get());
                output.accept(ModItems.WARDENS_FRAGMENT.get());
                output.accept(ModItems.TWIN_ESSENCE.get());
                output.accept(ModItems.SOVEREIGNS_CROWN.get());
                output.accept(ModItems.BOSS_TROPHY.get());
                
                // ========================================
                // SPECIAL UTILITY ITEMS
                // ========================================
                output.accept(ModItems.REALITY_GEM.get());
                output.accept(ModItems.SINGULARITY_CROWN.get());
                output.accept(ModItems.TREASURE_COMPASS.get());
                output.accept(ModItems.DIMENSIONAL_ANCHOR.get());
                output.accept(ModItems.QUANTUM_CAPACITOR.get());
                output.accept(ModItems.BIOME_TELEPORTER.get());
                output.accept(ModItems.SKILL_RESPEC_BOOK.get());
                output.accept(ModItems.TRANSCENDENCE_SHARD.get());
                
                // ========================================
                // DIMENSION ITEMS
                // ========================================
                output.accept(ModBlocks.RIFT_IGNITER.asItem());
                output.accept(ModItems.ECHO_COMPASS.get());
                output.accept(ModItems.STABILITY_SHARD.get());
                output.accept(ModItems.GRAVITY_ANCHOR.get());
                output.accept(ModItems.SINGULARITY_COMPASS.get());
                output.accept(ModItems.TEMPORAL_SHIELD.get());
                output.accept(ModItems.ENTROPY_COLLECTOR.get());
                output.accept(ModItems.CONVERGENCE_KEY.get());
                output.accept(ModItems.SOVEREIGN_SIGIL.get());
                
                // ========================================
                // MOBILITY ITEMS
                // ========================================
                output.accept(ModItems.GRAPPLING_HOOK.get());
                output.accept(ModItems.QUANTUM_GRAPPLE.get());
                
                // ========================================
                // MATERIALS - Color-coded gem sets
                // ========================================
                // Aqua (Singularity theme)
                output.accept(ModItems.AQUA_GEM.get());
                output.accept(ModItems.AQUA_INGOT.get());
                output.accept(ModItems.AQUA_NUGGET.get());
                
                // Purple (Void Forged theme)
                output.accept(ModItems.PURPLE_GEM.get());
                output.accept(ModItems.PURPLE_INGOT.get());
                output.accept(ModItems.PURPLE_NUGGET.get());
                
                // Magenta (Quantum Crystal theme)
                output.accept(ModItems.MAGENTA_GEM.get());
                output.accept(ModItems.MAGENTA_INGOT.get());
                output.accept(ModItems.MAGENTA_NUGGET.get());
                
                // Gold (Temporal Guardian theme)
                output.accept(ModItems.GOLD_GEM.get());
                output.accept(ModItems.GOLD_INGOT_CUSTOM.get());
                output.accept(ModItems.GOLD_NUGGET_CUSTOM.get());
                
                // ========================================
                // AQUA TOOLS & ARMOR
                // ========================================
                output.accept(ModItems.AQUA_SWORD.get());
                output.accept(ModItems.AQUA_PICKAXE.get());
                output.accept(ModItems.AQUA_AXE.get());
                output.accept(ModItems.AQUA_SHOVEL.get());
                output.accept(ModItems.AQUA_HOE.get());
                output.accept(ModItems.AQUA_HELMET.get());
                output.accept(ModItems.AQUA_CHESTPLATE.get());
                output.accept(ModItems.AQUA_LEGGINGS.get());
                output.accept(ModItems.AQUA_BOOTS.get());
                
                // ========================================
                // PURPLE TOOLS & ARMOR
                // ========================================
                output.accept(ModItems.PURPLE_SWORD.get());
                output.accept(ModItems.PURPLE_PICKAXE.get());
                output.accept(ModItems.PURPLE_AXE.get());
                output.accept(ModItems.PURPLE_SHOVEL.get());
                output.accept(ModItems.PURPLE_HOE.get());
                output.accept(ModItems.PURPLE_HELMET.get());
                output.accept(ModItems.PURPLE_CHESTPLATE.get());
                output.accept(ModItems.PURPLE_LEGGINGS.get());
                output.accept(ModItems.PURPLE_BOOTS.get());
                
                // ========================================
                // MAGENTA TOOLS & ARMOR
                // ========================================
                output.accept(ModItems.MAGENTA_SWORD.get());
                output.accept(ModItems.MAGENTA_PICKAXE.get());
                output.accept(ModItems.MAGENTA_AXE.get());
                output.accept(ModItems.MAGENTA_SHOVEL.get());
                output.accept(ModItems.MAGENTA_HOE.get());
                output.accept(ModItems.MAGENTA_HELMET.get());
                output.accept(ModItems.MAGENTA_CHESTPLATE.get());
                output.accept(ModItems.MAGENTA_LEGGINGS.get());
                output.accept(ModItems.MAGENTA_BOOTS.get());
                
                // ========================================
                // GOLD TOOLS & ARMOR
                // ========================================
                output.accept(ModItems.GOLD_SWORD_CUSTOM.get());
                output.accept(ModItems.GOLD_PICKAXE_CUSTOM.get());
                output.accept(ModItems.GOLD_AXE_CUSTOM.get());
                output.accept(ModItems.GOLD_SHOVEL_CUSTOM.get());
                output.accept(ModItems.GOLD_HOE_CUSTOM.get());
                output.accept(ModItems.GOLD_HELMET_CUSTOM.get());
                output.accept(ModItems.GOLD_CHESTPLATE_CUSTOM.get());
                output.accept(ModItems.GOLD_LEGGINGS_CUSTOM.get());
                output.accept(ModItems.GOLD_BOOTS_CUSTOM.get());
                
                // ========================================
                // VOID BLADE
                // ========================================
                output.accept(ModItems.VOID_BLADE.get());
                
                // ========================================
                // BLOCKS - Ores and decorative
                // ========================================
                output.accept(ModBlocks.QUANTUM_ORE.get());
                output.accept(ModBlocks.QUANTUM_BLOCK.get());
                output.accept(ModBlocks.QUANTUM_GLASS.get());
                output.accept(ModBlocks.RIFT_STONE.get());
                output.accept(ModBlocks.RIFT_STONE_BRICKS.get());
                output.accept(ModBlocks.VOID_BRICKS.get());
                output.accept(ModBlocks.QUANTUM_LAMP.get());
                output.accept(ModBlocks.QUANTUM_STONE.get());
                output.accept(ModBlocks.QUANTUM_SOIL.get());
                output.accept(ModBlocks.CRYSTALLIZED_STONE.get());
                output.accept(ModBlocks.CRYSTAL_CLUSTER.get());
                
                // ========================================
                // FUNCTIONAL BLOCKS
                // ========================================
                // Entangled Ore has no source or function yet; hidden until Matthew decides (output.accept(ModBlocks.ENTANGLED_ORE.get))
                output.accept(ModBlocks.RIFT_FRAME.get());
                output.accept(ModBlocks.CRYSTAL_VINE.get());
                output.accept(ModBlocks.FALCON_PERCH.get());
                output.accept(ModBlocks.PERCH_STAND.get());
                output.accept(ModBlocks.RESONANCE_BELL.get());
                for (var fb : ModFamilyBlocks.ALL) if (!CREATIVE_HIDDEN.contains(fb.getId().getPath())) output.accept(fb.get());
                for (var fi : ModFamilyItems.ALL) output.accept(fi.get());
                for (var gi : ModGearItems.ALL) output.accept(gi.get());
                for (var db : ModDerivedBlocks.ALL) output.accept(db.get());
                for (var di : ModDerivedBlocks.ITEMS) output.accept(di.get());
                output.accept(ModBlocks.QUANTUM_NEST.get());
                output.accept(ModBlocks.TWIG_NEST.get());
                output.accept(ModBlocks.CRYSTAL_NEST.get());
                output.accept(ModBlocks.VOIDFORGED_NEST.get());
                output.accept(ModBlocks.QUANTUM_AERIE.get());
                output.accept(ModBlocks.QUANTUM_INCUBATOR.get());
                output.accept(ModBlocks.QUANTUM_FORGE.get());
                output.accept(ModBlocks.PEDESTAL.get());
                output.accept(ModBlocks.AQUA_AEGIS_RELIQUARY.get());
                
                // ========================================
                // DECORATIVE BLOCKS (variant art textures)
                // ========================================
                output.accept(ModBlocks.QUANTUM_BRICKS.get());
                output.accept(ModBlocks.QUANTUM_TILES.get());
                output.accept(ModBlocks.QUANTUM_GLASS_CYAN.get());
                output.accept(ModBlocks.QUANTUM_GLASS_PURPLE.get());
                output.accept(ModBlocks.VOID_OBSIDIAN.get());
                output.accept(ModBlocks.QUANTUM_TECH_CYAN.get());
                output.accept(ModBlocks.QUANTUM_TECH_PURPLE.get());
                output.accept(ModBlocks.ECHO_LATTICE.get());
                output.accept(ModBlocks.TEMPORAL_TILES.get());
                output.accept(ModBlocks.TEMPORAL_PILLAR.get());
                output.accept(ModBlocks.SINGULARITY_CORE_BLOCK.get());
                output.accept(ModBlocks.VOID_STONE_BRICKS.get());
                output.accept(ModBlocks.CHAOS_CONDUIT.get());
                
                // ========================================
                // MISSING ITEMS
                // ========================================
                output.accept(ModItems.RIFT_SHARD.get());
                output.accept(ModItems.QUANTUM_CODEX.get());
                ModItems.CATALYSTS.forEach(c -> output.accept(c.get()));
                output.accept(ModItems.FALCON_EGG.get());
                output.accept(ModItems.QUANTUM_STABILIZER.get());
                output.accept(ModItems.ENTANGLEMENT_BAND.get());
            })
            .build());

    /** Realm block families (tools/gen_realm_blocks.py) and nests. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> REALM_BLOCKS_TAB =
        CREATIVE_TABS.register("realm_blocks_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.quantumfalcon.realm_blocks"))
            .icon(() -> new ItemStack(ModRealmBlocks.CHISELED_VOIDSHALE.get()))
            .withTabsBefore(QUANTUM_FALCON_TAB.getKey())
            .displayItems((parameters, output) -> { for (var b : ModRealmBlocks.ALL) output.accept(b.get()); for (var b : ModFrozenHoursBlocks.ALL) output.accept(b.get()); })
            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
        eventBus.addListener(ModCreativeTabs::hideFromTabs);
        QuantumFalcon.LOGGER.info("⚡ Registered creative tabs");
    }

    /** dev, unused and legacy items carry the c:hidden_from_recipe_viewers tag (JEI and EMI honour it too); no tab shows them. */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> HIDDEN_TAG = net.minecraft.tags.TagKey.create(Registries.ITEM,
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "hidden_from_recipe_viewers"));

    private static void hideFromTabs(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) {
        if (!e.getTabKey().location().getNamespace().equals(QuantumFalcon.MOD_ID)) return;
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(QuantumFalcon.MOD_ID)) continue;
            if (!item.builtInRegistryHolder().is(HIDDEN_TAG) && !CREATIVE_HIDDEN.contains(id.getPath())) continue;
            try { e.remove(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS); } catch (RuntimeException ignored) { }
        }
    }
}
