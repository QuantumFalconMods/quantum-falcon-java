package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.hytale.GrapplingHookItem;
import com.matt.quantumfalcon.hytale.QuantumGrappleItem;
import com.matt.quantumfalcon.item.FalconEggItem;
import com.matt.quantumfalcon.item.FalconWhistleItem;
import com.matt.quantumfalcon.item.MaterialWithTooltipItem;
import com.matt.quantumfalcon.item.QuantumBerryItem;
import com.matt.quantumfalcon.item.TranscendenceShardItem;
import com.matt.quantumfalcon.material.ModArmorMaterials;
import com.matt.quantumfalcon.material.ModToolTiers;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ModItems registry for Quantum Falcon mod.
 * Includes materials and special items.
 * Note: Spawn eggs temporarily disabled until entity system is updated for 1.21.11 API.
 */
public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuantumFalcon.MOD_ID);

    // ==================== AQUA MATERIALS ====================
    public static final DeferredItem<Item> AQUA_GEM = ITEMS.register("aqua_gem", 
        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> AQUA_INGOT = ITEMS.register("aqua_ingot", 
        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> AQUA_NUGGET = ITEMS.register("aqua_nugget", 
        () -> new Item(new Item.Properties()));

    // ==================== PURPLE MATERIALS ====================
    public static final DeferredItem<Item> PURPLE_GEM = ITEMS.register("purple_gem", 
        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PURPLE_INGOT = ITEMS.register("purple_ingot", 
        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PURPLE_NUGGET = ITEMS.register("purple_nugget", 
        () -> new Item(new Item.Properties()));

    // ==================== MAGENTA MATERIALS ====================
    public static final DeferredItem<Item> MAGENTA_GEM = ITEMS.register("magenta_gem", 
        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MAGENTA_INGOT = ITEMS.register("magenta_ingot", 
        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MAGENTA_NUGGET = ITEMS.register("magenta_nugget", 
        () -> new Item(new Item.Properties()));

    // ==================== GOLD MATERIALS ====================
    public static final DeferredItem<Item> GOLD_GEM = ITEMS.register("gold_gem", 
        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_INGOT_CUSTOM = ITEMS.register("gold_ingot_custom", 
        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_NUGGET_CUSTOM = ITEMS.register("gold_nugget_custom", 
        () -> new Item(new Item.Properties()));

    // ==================== QUANTUM MATERIALS ====================
    public static final DeferredItem<Item> QUANTUM_CRYSTAL = ITEMS.register("quantum_crystal", 
        () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> QUANTUM_SHARD = ITEMS.register("quantum_shard", 
        () -> new MaterialWithTooltipItem(new Item.Properties().rarity(Rarity.RARE), "quantum_shard"));
    public static final DeferredItem<Item> RIFT_SHARD = ITEMS.register("rift_shard", 
        () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> VOID_ESSENCE = ITEMS.register("void_essence", 
        () -> new MaterialWithTooltipItem(new Item.Properties().rarity(Rarity.RARE), "void_essence"));
    public static final DeferredItem<Item> REALITY_FRAGMENT = ITEMS.register("reality_fragment", 
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ==================== FALCON ITEMS ====================
    public static final DeferredItem<Item> QUANTUM_KNOT = ITEMS.register("quantum_knot", 
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> QUANTUM_CATALYST = ITEMS.register("quantum_catalyst", 
        () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    // Falcon barding (body slot armor for tamed falcons)
    public static final DeferredItem<Item> LEATHER_FALCON_BARDING = ITEMS.register("leather_falcon_barding", () -> new com.matt.quantumfalcon.item.FalconBardingItem(com.matt.quantumfalcon.item.FalconBardingItem.Tier.LEATHER));
    public static final DeferredItem<Item> IRON_FALCON_BARDING = ITEMS.register("iron_falcon_barding", () -> new com.matt.quantumfalcon.item.FalconBardingItem(com.matt.quantumfalcon.item.FalconBardingItem.Tier.IRON));
    public static final DeferredItem<Item> DIAMOND_FALCON_BARDING = ITEMS.register("diamond_falcon_barding", () -> new com.matt.quantumfalcon.item.FalconBardingItem(com.matt.quantumfalcon.item.FalconBardingItem.Tier.DIAMOND));
    public static final DeferredItem<Item> NETHERITE_FALCON_BARDING = ITEMS.register("netherite_falcon_barding", () -> new com.matt.quantumfalcon.item.FalconBardingItem(com.matt.quantumfalcon.item.FalconBardingItem.Tier.NETHERITE));
    public static final DeferredItem<Item> QUANTUM_FALCON_BARDING = ITEMS.register("quantum_falcon_barding", () -> new com.matt.quantumfalcon.item.FalconBardingItem(com.matt.quantumfalcon.item.FalconBardingItem.Tier.QUANTUM));

    public static final DeferredItem<Item> FALCON_WHISTLE = ITEMS.register("falcon_whistle", 
        () -> new FalconWhistleItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> FALCON_TREAT = ITEMS.register("falcon_treat", 
        () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> QUANTUM_BERRY = ITEMS.register("quantum_berry",
        () -> new QuantumBerryItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> FALCON_EGG = ITEMS.register("falcon_egg", 
        () -> new FalconEggItem(ModBlocks.FALCON_EGG_BLOCK.get(), new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> GOLDEN_FALCON_EGG = ITEMS.register("golden_falcon_egg",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    // ==================== BREEDING CATALYSTS (BREEDING.md) ====================
    public static final java.util.List<DeferredItem<Item>> CATALYSTS = java.util.stream.Stream.of(
            "aurora_plume", "void_pearl", "prism_shard", "storm_feather", "ember_heart", "rift_ember", "chrono_sand", "entanglement_thread", "ancient_eggshell")
        .map(id -> ITEMS.register(id, () -> (Item) new com.matt.quantumfalcon.item.CatalystItem(id, new Item.Properties().stacksTo(16).rarity(Rarity.RARE))))
        .toList();
    public static final DeferredItem<Item> QUANTUM_CODEX = ITEMS.register("quantum_codex", 
        () -> new com.matt.quantumfalcon.item.QuantumCodexItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    
    // ==================== BOSS DROPS ====================
    public static final DeferredItem<Item> OVERSEERS_EYE = ITEMS.register("overseers_eye", 
        () -> new com.matt.quantumfalcon.item.MaterialWithTooltipItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1), "overseers_eye"));
    public static final DeferredItem<Item> WARDENS_FRAGMENT = ITEMS.register("wardens_fragment", 
        () -> new com.matt.quantumfalcon.item.MaterialWithTooltipItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1), "wardens_fragment"));
    public static final DeferredItem<Item> TWIN_ESSENCE = ITEMS.register("twin_essence", 
        () -> new com.matt.quantumfalcon.item.MaterialWithTooltipItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1), "twin_essence"));
    public static final DeferredItem<Item> SOVEREIGNS_CROWN = ITEMS.register("sovereigns_crown", 
        () -> new com.matt.quantumfalcon.item.MaterialWithTooltipItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1), "sovereigns_crown"));
    /** the end of the loot chain is a native item (it used to exist only when Artifacts was installed). */
    public static final DeferredItem<Item> HEART_OF_QUANTUM = ITEMS.register("heart_of_quantum",
        () -> new com.matt.quantumfalcon.item.HeartOfQuantumItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()));
    
    // ==================== DIMENSION ITEMS ====================
    // NOTE: RIFT_IGNITER block+item is registered in ModBlocks.RIFT_IGNITER via registerBlockWithItem.
    // Use ModBlocks.RIFT_IGNITER.asItem() to reference it. Do NOT duplicate here.
    
    public static final DeferredItem<Item> ECHO_COMPASS = ITEMS.register("echo_compass",
        () -> new com.matt.quantumfalcon.item.RiftCompassItem(new Item.Properties().rarity(Rarity.RARE), "boss_arenas", "boss arena"));
    
    public static final DeferredItem<Item> STABILITY_SHARD = ITEMS.register("stability_shard",
        () -> new com.matt.quantumfalcon.item.StabilityShardItem(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON)));
    
    // ==================== QUANTUM STABILIZER (Boss Drop - Instability Reset) ====================
    public static final DeferredItem<Item> QUANTUM_STABILIZER = ITEMS.register("quantum_stabilizer",
        () -> new com.matt.quantumfalcon.item.QuantumStabilizerItem(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant()));
    
    // ==================== HYTALE-INSPIRED ITEMS ====================
    public static final DeferredItem<GrapplingHookItem> GRAPPLING_HOOK = ITEMS.register("grappling_hook",
        () -> new GrapplingHookItem(new Item.Properties()
            .stacksTo(1)
            .durability(250)
            .rarity(Rarity.RARE)));
    
    // ==================== QUANTUM ENDGAME ITEMS ====================
    // Upgraded grapple with infinite durability, 40-block range, falcon boost
    public static final DeferredItem<QuantumGrappleItem> QUANTUM_GRAPPLE = ITEMS.register("quantum_grapple",
        () -> new QuantumGrappleItem(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant()));
    
    // Gravity well protection
    public static final DeferredItem<Item> GRAVITY_ANCHOR = ITEMS.register("gravity_anchor",
        () -> new Item(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.EPIC)));
    
    // Singularity navigation
    public static final DeferredItem<Item> SINGULARITY_COMPASS = ITEMS.register("singularity_compass",
        // points straight at the nearest Singularity Sanctum (observatory loot); it used to be a plain item
        () -> new com.matt.quantumfalcon.item.RiftCompassItem(new Item.Properties().rarity(Rarity.EPIC), "singularity_sanctums", "Singularity Sanctum"));
    
    // Event Horizon survival
    public static final DeferredItem<Item> TEMPORAL_SHIELD = ITEMS.register("temporal_shield",
        () -> new Item(new Item.Properties()
            .stacksTo(1)
            .durability(500)
            .rarity(Rarity.EPIC)));
    
    // Black hole harvesting tool
    public static final DeferredItem<Item> ENTROPY_COLLECTOR = ITEMS.register("entropy_collector",
        () -> new Item(new Item.Properties()
            .stacksTo(1)
            .durability(100)
            .rarity(Rarity.EPIC)));
    
    // Singularity cores for crafting
    public static final DeferredItem<Item> SINGULARITY_CORE = ITEMS.register("singularity_core",
        () -> new Item(new Item.Properties()
            .stacksTo(16)
            .rarity(Rarity.EPIC)
            .fireResistant()));
    
    // Event Horizon essence
    public static final DeferredItem<Item> HORIZON_ESSENCE = ITEMS.register("horizon_essence",
        () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.RARE)));
    
    // Probability manipulation
    public static final DeferredItem<Item> PROBABILITY_DICE = ITEMS.register("probability_dice",
        () -> new Item(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.RARE)));
    
    // Timeline key for boss
    public static final DeferredItem<Item> CONVERGENCE_KEY = ITEMS.register("convergence_key",
        () -> new Item(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.EPIC)));
    
    // Final boss summon
    public static final DeferredItem<Item> SOVEREIGN_SIGIL = ITEMS.register("sovereign_sigil",
        () -> new Item(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant()));
    
    // Transcendence ending item - triggers ending choice
    public static final DeferredItem<TranscendenceShardItem> TRANSCENDENCE_SHARD = ITEMS.register("transcendence_shard",
        () -> new TranscendenceShardItem(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant()));

    // ==================== BREEDING & GENETICS ITEMS ====================
    public static final DeferredItem<Item> SHINY_CHARM = ITEMS.register("shiny_charm",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> RESONANCE_FLOWER = ITEMS.register("resonance_flower",
        () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LEGENDARY_EGG = ITEMS.register("legendary_egg",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> GENETIC_FALCON_SPAWN_EGG = ITEMS.register("genetic_falcon_spawn_egg",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> QUANTUM_FERTILIZER = ITEMS.register("quantum_fertilizer",
        () -> new Item(new Item.Properties().stacksTo(64)));

    // ==================== ECHO MATERIALS ====================
    public static final DeferredItem<Item> ECHO_SHARD = ITEMS.register("echo_shard",
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> ECHO_CRYSTAL = ITEMS.register("echo_crystal",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> ECHO_KEY = ITEMS.register("echo_key",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ==================== VOID MATERIALS ====================
    public static final DeferredItem<Item> VOID_SHARD = ITEMS.register("void_shard",
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> VOID_CRYSTAL = ITEMS.register("void_crystal",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> VOID_FRAGMENT = ITEMS.register("void_fragment",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> VOID_BLADE = ITEMS.register("void_blade",
        () -> new SwordItem(ModToolTiers.PURPLE, new Item.Properties()
            .rarity(Rarity.EPIC)
            .attributes(SwordItem.createAttributes(ModToolTiers.PURPLE, 5, -2.4f))));

    // ==================== TEMPORAL MATERIALS ====================
    public static final DeferredItem<Item> TEMPORAL_DUST = ITEMS.register("temporal_dust",
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> TEMPORAL_CRYSTAL = ITEMS.register("temporal_crystal",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> TEMPORAL_ALLOY = ITEMS.register("temporal_alloy",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> TIME_BOTTLE = ITEMS.register("time_bottle",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ==================== AURORA MATERIALS ====================
    public static final DeferredItem<Item> AURORA_ESSENCE = ITEMS.register("aurora_essence",
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> AURORA_CRYSTAL = ITEMS.register("aurora_crystal",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    // ==================== PROBABILITY & RIFT MATERIALS ====================
    public static final DeferredItem<Item> PROBABILITY_SHARD = ITEMS.register("probability_shard",
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> PROBABILITY_CORE = ITEMS.register("probability_core",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> RIFT_FRAGMENT = ITEMS.register("rift_fragment",
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RIFT_CRYSTAL = ITEMS.register("rift_crystal",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> SINGULARITY_SHARD = ITEMS.register("singularity_shard",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> SINGULARITY_FRAGMENT = ITEMS.register("singularity_fragment",
        () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> QUANTUM_CORE = ITEMS.register("quantum_core",
        () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> ENTANGLEMENT_BAND = ITEMS.register("entanglement_band",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> TEMPORAL_FRAGMENT = ITEMS.register("temporal_fragment",
        () -> new MaterialWithTooltipItem(new Item.Properties().rarity(Rarity.EPIC), "temporal_fragment"));
    public static final DeferredItem<Item> RIFT_TOTEM = ITEMS.register("rift_totem",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    // ==================== SPECIAL UTILITY ITEMS ====================
    public static final DeferredItem<Item> REALITY_GEM = ITEMS.register("reality_gem",
        () -> new com.matt.quantumfalcon.item.MaterialWithTooltipItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "reality_gem"));
    public static final DeferredItem<Item> SINGULARITY_CROWN = ITEMS.register("singularity_crown",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<Item> TREASURE_COMPASS = ITEMS.register("treasure_compass",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> DIMENSIONAL_ANCHOR = ITEMS.register("dimensional_anchor",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> BOSS_TROPHY = ITEMS.register("boss_trophy",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> QUANTUM_CAPACITOR = ITEMS.register("quantum_capacitor",
        () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> QUANTUM_REROLL_TOKEN = ITEMS.register("quantum_reroll_token",
        () -> new com.matt.quantumfalcon.item.RerollTokenItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> BIOME_TELEPORTER = ITEMS.register("biome_teleporter",
        () -> new Item(new Item.Properties().stacksTo(1).durability(16).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> SKILL_RESPEC_BOOK = ITEMS.register("skill_respec_book",
        () -> new com.matt.quantumfalcon.item.SkillRespecBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ==================== FALCON FEATHERS ====================
    public static final DeferredItem<Item> FALCON_FEATHER = ITEMS.register("falcon_feather",
        () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> GOLDEN_FALCON_FEATHER = ITEMS.register("golden_falcon_feather",
        () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> LEGENDARY_FALCON_FEATHER = ITEMS.register("legendary_falcon_feather",
        () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));

    // ==================== AQUA TOOLS & ARMOR (Tier 1: Singularity) ====================
    public static final DeferredItem<SwordItem> AQUA_SWORD = ITEMS.register("aqua_sword",
        () -> new com.matt.quantumfalcon.item.gear.weapon.SingularitySwordItem());
    public static final DeferredItem<PickaxeItem> AQUA_PICKAXE = ITEMS.register("aqua_pickaxe",
        () -> new com.matt.quantumfalcon.item.gear.tool.SingularityPickaxeItem());
    public static final DeferredItem<AxeItem> AQUA_AXE = ITEMS.register("aqua_axe",
        () -> new com.matt.quantumfalcon.item.gear.tool.SingularityAxeItem());
    public static final DeferredItem<ShovelItem> AQUA_SHOVEL = ITEMS.register("aqua_shovel",
        () -> new com.matt.quantumfalcon.item.gear.tool.SingularityShovelItem());
    public static final DeferredItem<HoeItem> AQUA_HOE = ITEMS.register("aqua_hoe",
        () -> new com.matt.quantumfalcon.item.gear.tool.SingularityHoeItem());
    public static final DeferredItem<ArmorItem> AQUA_HELMET = ITEMS.register("aqua_helmet",
        () -> new com.matt.quantumfalcon.item.gear.armor.SingularityArmorItem(ArmorItem.Type.HELMET));
    public static final DeferredItem<ArmorItem> AQUA_CHESTPLATE = ITEMS.register("aqua_chestplate",
        () -> new com.matt.quantumfalcon.item.gear.armor.SingularityArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<ArmorItem> AQUA_LEGGINGS = ITEMS.register("aqua_leggings",
        () -> new com.matt.quantumfalcon.item.gear.armor.SingularityArmorItem(ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<ArmorItem> AQUA_BOOTS = ITEMS.register("aqua_boots",
        () -> new com.matt.quantumfalcon.item.gear.armor.SingularityArmorItem(ArmorItem.Type.BOOTS));

    // ==================== PURPLE TOOLS & ARMOR (Tier 2: Void Forged) ====================
    public static final DeferredItem<SwordItem> PURPLE_SWORD = ITEMS.register("purple_sword",
        () -> new com.matt.quantumfalcon.item.gear.weapon.VoidForgedSwordItem());
    public static final DeferredItem<PickaxeItem> PURPLE_PICKAXE = ITEMS.register("purple_pickaxe",
        () -> new com.matt.quantumfalcon.item.gear.tool.VoidForgedPickaxeItem());
    public static final DeferredItem<AxeItem> PURPLE_AXE = ITEMS.register("purple_axe",
        () -> new com.matt.quantumfalcon.item.gear.tool.VoidForgedAxeItem());
    public static final DeferredItem<ShovelItem> PURPLE_SHOVEL = ITEMS.register("purple_shovel",
        () -> new com.matt.quantumfalcon.item.gear.tool.VoidForgedShovelItem());
    public static final DeferredItem<HoeItem> PURPLE_HOE = ITEMS.register("purple_hoe",
        () -> new com.matt.quantumfalcon.item.gear.tool.VoidForgedHoeItem());
    public static final DeferredItem<ArmorItem> PURPLE_HELMET = ITEMS.register("purple_helmet",
        () -> new com.matt.quantumfalcon.item.gear.armor.VoidForgedArmorItem(ArmorItem.Type.HELMET));
    public static final DeferredItem<ArmorItem> PURPLE_CHESTPLATE = ITEMS.register("purple_chestplate",
        () -> new com.matt.quantumfalcon.item.gear.armor.VoidForgedArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<ArmorItem> PURPLE_LEGGINGS = ITEMS.register("purple_leggings",
        () -> new com.matt.quantumfalcon.item.gear.armor.VoidForgedArmorItem(ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<ArmorItem> PURPLE_BOOTS = ITEMS.register("purple_boots",
        () -> new com.matt.quantumfalcon.item.gear.armor.VoidForgedArmorItem(ArmorItem.Type.BOOTS));

    // ==================== MAGENTA TOOLS & ARMOR (Tier 3: Quantum Crystal) ====================
    public static final DeferredItem<SwordItem> MAGENTA_SWORD = ITEMS.register("magenta_sword",
        () -> new com.matt.quantumfalcon.item.gear.weapon.QuantumCrystalSwordItem());
    public static final DeferredItem<PickaxeItem> MAGENTA_PICKAXE = ITEMS.register("magenta_pickaxe",
        () -> new com.matt.quantumfalcon.item.gear.tool.QuantumCrystalPickaxeItem());
    public static final DeferredItem<AxeItem> MAGENTA_AXE = ITEMS.register("magenta_axe",
        () -> new com.matt.quantumfalcon.item.gear.tool.QuantumCrystalAxeItem());
    public static final DeferredItem<ShovelItem> MAGENTA_SHOVEL = ITEMS.register("magenta_shovel",
        () -> new com.matt.quantumfalcon.item.gear.tool.QuantumCrystalShovelItem());
    public static final DeferredItem<HoeItem> MAGENTA_HOE = ITEMS.register("magenta_hoe",
        () -> new com.matt.quantumfalcon.item.gear.tool.QuantumCrystalHoeItem());
    public static final DeferredItem<ArmorItem> MAGENTA_HELMET = ITEMS.register("magenta_helmet",
        () -> new com.matt.quantumfalcon.item.gear.armor.QuantumCrystalArmorItem(ArmorItem.Type.HELMET));
    public static final DeferredItem<ArmorItem> MAGENTA_CHESTPLATE = ITEMS.register("magenta_chestplate",
        () -> new com.matt.quantumfalcon.item.gear.armor.QuantumCrystalArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<ArmorItem> MAGENTA_LEGGINGS = ITEMS.register("magenta_leggings",
        () -> new com.matt.quantumfalcon.item.gear.armor.QuantumCrystalArmorItem(ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<ArmorItem> MAGENTA_BOOTS = ITEMS.register("magenta_boots",
        () -> new com.matt.quantumfalcon.item.gear.armor.QuantumCrystalArmorItem(ArmorItem.Type.BOOTS));

    // ==================== GOLD TOOLS & ARMOR (Tier 4: Temporal Guardian) ====================
    public static final DeferredItem<SwordItem> GOLD_SWORD_CUSTOM = ITEMS.register("gold_sword_custom",
        () -> new com.matt.quantumfalcon.item.gear.weapon.TemporalGuardianSwordItem());
    public static final DeferredItem<PickaxeItem> GOLD_PICKAXE_CUSTOM = ITEMS.register("gold_pickaxe_custom",
        () -> new com.matt.quantumfalcon.item.gear.tool.TemporalGuardianPickaxeItem());
    public static final DeferredItem<AxeItem> GOLD_AXE_CUSTOM = ITEMS.register("gold_axe_custom",
        () -> new com.matt.quantumfalcon.item.gear.tool.TemporalGuardianAxeItem());
    public static final DeferredItem<ShovelItem> GOLD_SHOVEL_CUSTOM = ITEMS.register("gold_shovel_custom",
        () -> new com.matt.quantumfalcon.item.gear.tool.TemporalGuardianShovelItem());
    public static final DeferredItem<HoeItem> GOLD_HOE_CUSTOM = ITEMS.register("gold_hoe_custom",
        () -> new com.matt.quantumfalcon.item.gear.tool.TemporalGuardianHoeItem());
    public static final DeferredItem<ArmorItem> GOLD_HELMET_CUSTOM = ITEMS.register("gold_helmet_custom",
        () -> new com.matt.quantumfalcon.item.gear.armor.TemporalGuardianArmorItem(ArmorItem.Type.HELMET));
    public static final DeferredItem<ArmorItem> GOLD_CHESTPLATE_CUSTOM = ITEMS.register("gold_chestplate_custom",
        () -> new com.matt.quantumfalcon.item.gear.armor.TemporalGuardianArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<ArmorItem> GOLD_LEGGINGS_CUSTOM = ITEMS.register("gold_leggings_custom",
        () -> new com.matt.quantumfalcon.item.gear.armor.TemporalGuardianArmorItem(ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<ArmorItem> GOLD_BOOTS_CUSTOM = ITEMS.register("gold_boots_custom",
        () -> new com.matt.quantumfalcon.item.gear.armor.TemporalGuardianArmorItem(ArmorItem.Type.BOOTS));

    // ==================== SPAWN EGG PLACEHOLDERS ====================
    // TODO: Add spawn eggs when entity system is updated for 1.21.11 API
    // Spawn eggs temporarily disabled due to API changes
    public static final DeferredItem<Item> SHA_SHA_SPAWN_EGG = ITEMS.register("sha_sha_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.SHA_SHA, 0x2E2060, 0x6EF0FF, new Item.Properties()));
    public static final DeferredItem<Item> GOBLIN_TRADER_SPAWN_EGG = ITEMS.register("goblin_trader_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.GOBLIN_TRADER, 0x4E7A2E, 0xC8A050, new Item.Properties()));
    public static final DeferredItem<Item> QUANTUM_MERCHANT_SPAWN_EGG = ITEMS.register("quantum_merchant_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.QUANTUM_MERCHANT, 0x24184A, 0x3FE0FF, new Item.Properties()));
    public static final DeferredItem<Item> QUANTUM_STALKER_SPAWN_EGG = ITEMS.register("quantum_stalker_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.QUANTUM_STALKER, 0x14101E, 0xB040FF, new Item.Properties()));
    public static final DeferredItem<Item> ECLIPSE_PHANTOM_SPAWN_EGG = ITEMS.register("eclipse_phantom_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.ECLIPSE_PHANTOM, 0x08060E, 0xFFB030, new Item.Properties()));
    public static final DeferredItem<Item> VOID_LORD_SPAWN_EGG = ITEMS.register("void_lord_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.VOID_LORD, 0x100818, 0x8A2BE2, new Item.Properties()));
    // spawn eggs for every living type (creative and map makers)
    public static final DeferredItem<Item> CRYSTAL_GUARDIAN_SPAWN_EGG = ITEMS.register("crystal_guardian_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.CRYSTAL_GUARDIAN, 0x3FA9FF, 0xE8F7FF, new Item.Properties()));
    public static final DeferredItem<Item> DATA_CONSTRUCT_SPAWN_EGG = ITEMS.register("data_construct_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.DATA_CONSTRUCT, 0x1E2A3A, 0x3FE0FF, new Item.Properties()));
    public static final DeferredItem<Item> VOID_ECHO_SPAWN_EGG = ITEMS.register("void_echo_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.VOID_ECHO, 0x140A22, 0x8A2BE2, new Item.Properties()));
    public static final DeferredItem<Item> QUANTUM_PHANTOM_SPAWN_EGG = ITEMS.register("quantum_phantom_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.QUANTUM_PHANTOM, 0x2A1F4A, 0xC86BFF, new Item.Properties()));
    public static final DeferredItem<Item> QUANTUM_WISP_SPAWN_EGG = ITEMS.register("quantum_wisp_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.QUANTUM_WISP, 0x6A5CFF, 0xF2F7FF, new Item.Properties()));
    public static final DeferredItem<Item> ENTANGLED_TWINS_SPAWN_EGG = ITEMS.register("entangled_twins_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.ENTANGLED_TWINS, 0xFF5FD2, 0x4FE3FF, new Item.Properties()));
    public static final DeferredItem<Item> SKYWARDEN_IREL_SPAWN_EGG = ITEMS.register("skywarden_irel_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.SKYWARDEN_IREL, 0x3A2A18, 0xFFB43A, new Item.Properties()));

    public static final DeferredItem<Item> RIFT_WARDEN_MINION_SPAWN_EGG = ITEMS.register("rift_warden_minion_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.RIFT_WARDEN_MINION, 0x1A0B2E, 0x8844CC, new Item.Properties()));
    // painted spawn eggs: white tints so the item colour handler leaves the art untouched
    public static final DeferredItem<Item> QUANTUM_OVERSEER_SPAWN_EGG = ITEMS.register("quantum_overseer_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.QUANTUM_OVERSEER, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<Item> RIFT_WARDEN_SPAWN_EGG = ITEMS.register("rift_warden_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.RIFT_WARDEN, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<Item> SINGULARITY_SOVEREIGN_SPAWN_EGG = ITEMS.register("singularity_sovereign_spawn_egg",
        () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.SINGULARITY_SOVEREIGN, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<Item> FALCON_SPAWN_EGG = ITEMS.register("falcon_spawn_egg",
        () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    // ==================== RECONSTRUCTION MERGE (from Downloads/quantum_falcon_fixes/java/ModItems.java) ====================
    // Only items that already had an item model + texture in assets were registered; the rest of the fix list
    // (power_*, destiny_knot, oval_charm, quantum_feed, quantum_ingot, eclipse_shard) still need art first.
    public static final DeferredItem<Item> ARTIFACT_FRAGMENT = ITEMS.registerSimpleItem("artifact_fragment", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> FALCON_JOURNAL = ITEMS.registerSimpleItem("falcon_journal", new Item.Properties().rarity(Rarity.COMMON));
    public static final DeferredItem<Item> GOLDEN_FEATHER_TREAT = ITEMS.registerSimpleItem("golden_feather_treat", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> PHASE_CRYSTAL = ITEMS.registerSimpleItem("phase_crystal", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> QUANTUM_COMPASS = ITEMS.register("quantum_compass",
        () -> new com.matt.quantumfalcon.item.RiftCompassItem(new Item.Properties().rarity(Rarity.UNCOMMON), "rifts", "quantum rift"));
    public static final DeferredItem<Item> QUANTUM_DUST = ITEMS.registerSimpleItem("quantum_dust", new Item.Properties().rarity(Rarity.COMMON));
    public static final DeferredItem<Item> VOID_HEART = ITEMS.registerSimpleItem("void_heart", new Item.Properties().rarity(Rarity.EPIC));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        QuantumFalcon.LOGGER.info("⚡ Registered {} items", ITEMS.getEntries().size());
    }
}
