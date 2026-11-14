package com.matt.quantumfalcon;

import com.matt.quantumfalcon.achievement.AchievementSystem;
import com.matt.quantumfalcon.biome.ModBiomes;
import com.matt.quantumfalcon.capability.PlayerInstabilityCapability;
import com.matt.quantumfalcon.material.ModArmorMaterials;
import com.matt.quantumfalcon.event.DynamicEventManager;
import com.matt.quantumfalcon.leveling.config.LevelingConfig;
import com.matt.quantumfalcon.quest.QuestSystem;
import com.matt.quantumfalcon.registry.ModBlockEntities;
import com.matt.quantumfalcon.registry.ModBlocks;
import com.matt.quantumfalcon.registry.ModCreativeTabs;
import com.matt.quantumfalcon.registry.ModDataComponents;
import com.matt.quantumfalcon.registry.ModEntities;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.registry.ModParticles;
import com.matt.quantumfalcon.registry.ModSounds;
import com.matt.quantumfalcon.skill.SkillTreeSystem;
import com.matt.quantumfalcon.story.StorylineManager;
import com.matt.quantumfalcon.systems.FalconSystems;
import com.matt.quantumfalcon.worldgen.structure.ModStructures;
import com.matt.quantumfalcon.worldgen.structure.pieces.BossArenaPieces;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

/**
 * Quantum Falcon Mod - Main class
 * A cyberpunk falcon companion mod with item leveling, traits, and breeding.
 * 
 * NeoForge 1.21.x compatible
 */
@Mod(value = QuantumFalcon.MOD_ID)
public class QuantumFalcon {
    public static final String MOD_ID = "quantumfalcon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public QuantumFalcon(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Quantum Falcon Mod...");
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::onConfigLoad);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        
        // Register all mod content (order matters for dependencies)
        ModDataComponents.register(modEventBus);  // Data components first
        ModArmorMaterials.register(modEventBus);
        com.matt.quantumfalcon.registry.ModFamilyArmorMaterials.register(modEventBus);
        com.matt.quantumfalcon.registry.ModFamilyItems.init();
        com.matt.quantumfalcon.registry.ModGearItems.register(modEventBus);   // weapons + void steel armour   // Armor materials before items (DeferredRegister)
        com.matt.quantumfalcon.registry.ModFamilyBlocks.init(); // queue family blocks before BLOCKS is attached
        com.matt.quantumfalcon.registry.ModDerivedBlocks.init();
        com.matt.quantumfalcon.registry.ModRealmBlocks.init();
        com.matt.quantumfalcon.registry.ModFrozenHoursBlocks.init();   // A10 Frozen Hours Then-hour masonry (tools/gen_frozen_hours_blocks.py)   // realm block families (tools/gen_realm_blocks.py)
        com.matt.quantumfalcon.registry.ModRealmShapes.init();   // shape completion (tools/gen_realm_shapes.py)
        ModBlocks.register(modEventBus);
        com.matt.quantumfalcon.villager.ModVillagers.register(modEventBus);   // villagers: POI + professions (Falconer)
        com.matt.quantumfalcon.dungeon.ModDungeon.register(modEventBus);
        com.matt.quantumfalcon.arena.fight.ModArenaBlocks.register(modEventBus);
        com.matt.quantumfalcon.world.WildNestFeature.register(modEventBus);
        com.matt.quantumfalcon.loot.WildEggFunction.register(modEventBus);
        com.matt.quantumfalcon.registry.ModMenuTypes.register(modEventBus);
        ModBlockEntities.register(modEventBus);   // After blocks
        com.matt.quantumfalcon.story.BoundRelicRecipe.register(modEventBus);
        com.matt.quantumfalcon.leveling.PlacedBlocks.register(modEventBus);   // persistent placed-block marker // story recipes bind their result
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModEntities.register(modEventBus);
        ModParticles.register(modEventBus);
        ModSounds.register(modEventBus);
        com.matt.quantumfalcon.biome.ModBiomes.register();  // Custom biomes (no event bus needed)
        PlayerInstabilityCapability.ATTACHMENT_TYPES.register(modEventBus); // Player instability capability
        com.matt.quantumfalcon.quest.PlayerQuestData.ATTACHMENT_TYPES.register(modEventBus); // Quest data persistence
        com.matt.quantumfalcon.story.PlayerStoryData.ATTACHMENT_TYPES.register(modEventBus); // Story data
        com.matt.quantumfalcon.skill.PlayerSkillData.ATTACHMENT_TYPES.register(modEventBus); // Skill data persistence
        com.matt.quantumfalcon.loot.sig.SignatureLoot.register(modEventBus); // A8: signature loot pity ledger
        com.matt.quantumfalcon.skills.UseSkills.register(modEventBus); // A9: use-based skills
        com.matt.quantumfalcon.codex.RiftCodex.register(modEventBus); // A1: Rift Codex quests
        
        // Worldgen registrations
        ModStructures.register(modEventBus);      // Boss arena structures
        BossArenaPieces.register(modEventBus);    // Structure piece types
        
        // Initialize gameplay systems (non-registry) - these are static initializers
        // SkillTreeSystem and QuestSystem initialize via static blocks
        // No explicit register call needed
        StorylineManager.register(modEventBus);   // Story system with 13 chapters
        AchievementSystem.register(modEventBus);  // Achievement system with 30+ achievements
        DynamicEventManager.register(modEventBus); // Dynamic world events
        FalconSystems.register(modEventBus);      // Falcon auras, defense, gathering
        
        // Initialize mod integrations (soft dependencies)
        com.matt.quantumfalcon.integration.ModIntegrations.init();
        com.matt.quantumfalcon.integration.ModIntegrations.registerItems(modEventBus);
        
        // Initialize skill tree and loot table integrations
        com.matt.quantumfalcon.integration.skills.SkillTreeIntegration.init();
        com.matt.quantumfalcon.integration.loot.LootTableIntegration.integrateApotheosisBossLoot();
        com.matt.quantumfalcon.integration.loot.LootTableIntegration.integrateCreateLoot();
        com.matt.quantumfalcon.integration.loot.LootTableIntegration.integrateWaystonesLoot();
        
        // Register gear set bonus handler
        NeoForge.EVENT_BUS.register(com.matt.quantumfalcon.item.gear.GearSetBonusHandler.class);
        
        LOGGER.info("✅ Quantum Falcon systems activated:");
        LOGGER.info("  - Color-coded item sets: 4 tiers (Singularity, Void Forged, Quantum Crystal, Temporal Guardian)");
        LOGGER.info("  - Blocks: Ores, decorative, entangled ore system");
        LOGGER.info("  - Block entities: Quantum-linked blocks");
        LOGGER.info("  - Biomes: 55 custom quantum-themed biomes");
        LOGGER.info("  - Structures: 3 boss arenas (Guardian, Void Horror, Sovereign)");
        LOGGER.info("  - Particles: 30+ custom effects");
        LOGGER.info("  - Genetics: Mendelian breeding system");
        LOGGER.info("  - Skill Tree: 26 skills across 5 categories");
        LOGGER.info("  - Quest System: 13 quests (5 main, 5 side, 3 tutorial)");
        LOGGER.info("  - Story System: 13 chapters with 3 endings");
        LOGGER.info("  - Dimension: Echo Rift with instability system");
        LOGGER.info("  - Networking: Client-server sync for skills, quests, and falcon state");
        LOGGER.info("  - Commands: Admin commands for testing and debugging");
        LOGGER.info("  - Achievement System: 30+ achievements with XP rewards");
        LOGGER.info("  - Dynamic Events: Meteor showers, aurora, storms, eclipses");
        LOGGER.info("  - Falcon Systems: Auras, defense, item gathering");
        LOGGER.info("  - Hytale features: Grappling hooks, climbing vines, physics");
        
        // Register configs
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "quantumfalcon-common.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, LevelingConfig.SPEC, "quantumfalcon-leveling.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, com.matt.quantumfalcon.event.EventsConfig.SPEC, "quantumfalcon-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, WorldConfig.SPEC, "quantumfalcon-world.toml");   // structures, difficulty, eggs, ward, rift
        // Integration config is optional and loaded separately if integrations are present
        
        LOGGER.info("Quantum Falcon Mod initialized!");
    }

    private void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == LevelingConfig.SPEC) {
            LevelingConfig.markLoaded();
            LOGGER.info("Leveling config loaded");
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Quantum Falcon common setup starting...");
        event.enqueueWork(() -> {
            LOGGER.info("Registered {} blocks", ModBlocks.BLOCKS.getEntries().size());
            LOGGER.info("Registered {} items", ModItems.ITEMS.getEntries().size());
            LOGGER.info("Registered {} entities", ModEntities.ENTITIES.getEntries().size());
            LOGGER.info("Registered {} data components", ModDataComponents.DATA_COMPONENTS.getEntries().size());
            LOGGER.info("Registered {} particles", ModParticles.PARTICLES.getEntries().size());
            LOGGER.info("Registered {} sounds", ModSounds.SOUNDS.getEntries().size());
            com.matt.quantumfalcon.registry.ModDimensions.init();
            LOGGER.info("Common setup complete - Core systems active");
        });
    }

    private void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Quantum Falcon client setup starting...");
        event.enqueueWork(() -> {
            // Particle providers are registered via @EventBusSubscriber in ModParticleProviders
            com.matt.quantumfalcon.client.QFItemProperties.register();
            LOGGER.info("Client setup tasks complete - Particles, renderers, and GUIs ready");
        });
    }

    private void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Quantum Falcon: Server starting");
    }
}
