package com.matt.quantumfalcon.quest;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.entity.boss.*;
import com.matt.quantumfalcon.registry.ModBlocks;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.registry.ModSounds;
import com.matt.quantumfalcon.skill.SkillPointSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Quest Auto-Tracking System for Quantum Falcon.
 * Automatically tracks and updates quest objectives based on player actions.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class QuestTracker {

    private static final Logger LOGGER = LogManager.getLogger();

    // ============ Objective Types ============
    
    public enum ObjectiveType {
        KILL_ENTITY,
        TAME_FALCON,
        BREED_FALCON,
        COLLECT_ITEM,
        CRAFT_ITEM,
        PLACE_BLOCK,
        BREAK_BLOCK,
        ENTER_DIMENSION,
        VISIT_BIOME,
        REACH_BOND_LEVEL,
        USE_ABILITY,
        DISCOVER_LOCATION,
        INTERACT_BLOCK,
        BOSS_KILL
    }

    // ============ Quest Objective Definitions ============
    
    /**
     * Defines what objectives each quest has and how to track them
     */
    private static final Map<String, List<QuestObjective>> QUEST_OBJECTIVES = new HashMap<>();
    
    static {
        // ===== TUTORIAL QUESTS =====
        
        // Tutorial: Basics
        QUEST_OBJECTIVES.put("tutorial_basics", List.of(
            new QuestObjective(ObjectiveType.COLLECT_ITEM, "quantumfalcon:quantum_shard", 5, "Collect Quantum Shards"),
            new QuestObjective(ObjectiveType.CRAFT_ITEM, "quantumfalcon:quantum_core", 1, "Craft a Quantum Core")
        ));
        
        // Tutorial: Falcon
        QUEST_OBJECTIVES.put("tutorial_falcon", List.of(
            new QuestObjective(ObjectiveType.TAME_FALCON, "any", 1, "Tame a Quantum Falcon"),
            new QuestObjective(ObjectiveType.REACH_BOND_LEVEL, "10", 1, "Reach Bond Level 10")
        ));
        
        // Tutorial: Breeding
        QUEST_OBJECTIVES.put("tutorial_breeding", List.of(
            new QuestObjective(ObjectiveType.COLLECT_ITEM, "quantumfalcon:quantum_crystal", 2, "Collect Quantum Crystals"),
            new QuestObjective(ObjectiveType.BREED_FALCON, "any", 1, "Breed Two Falcons")
        ));
        
        // ===== MAIN STORY QUESTS =====
        
        // Main 1: The Quantum Awakening
        // these two quests had no objectives, so they could never complete
        QUEST_OBJECTIVES.put("first_breed", List.of(
            new QuestObjective(ObjectiveType.BREED_FALCON, "any", 1, "Breed two tamed falcons")
        ));
        QUEST_OBJECTIVES.put("genetic_mastery", List.of(
            new QuestObjective(ObjectiveType.BREED_FALCON, "rare_variant", 1, "Hatch a cosmic or singularity falcon")
        ));

        QUEST_OBJECTIVES.put("main_1", List.of(
            new QuestObjective(ObjectiveType.COLLECT_ITEM, "quantumfalcon:quantum_shard", 10, "Gather Quantum Shards"),
            new QuestObjective(ObjectiveType.TAME_FALCON, "any", 1, "Find and Tame a Falcon"),
            new QuestObjective(ObjectiveType.CRAFT_ITEM, "quantumfalcon:falcon_whistle", 1, "Craft a Falcon Whistle")
        ));
        
        // Main 2: Echoes of the Rift
        QUEST_OBJECTIVES.put("main_2", List.of(
            new QuestObjective(ObjectiveType.COLLECT_ITEM, "quantumfalcon:rift_shard", 8, "Collect Rift Shards"),
            new QuestObjective(ObjectiveType.CRAFT_ITEM, "quantumfalcon:rift_frame", 22, "Craft Rift Frame Blocks (a 6x7 ring)"),
            new QuestObjective(ObjectiveType.ENTER_DIMENSION, "quantumfalcon:echo_rift", 1, "Enter the Rift Dimension")
        ));
        
        // Main 3: The Overseer's Gaze
        QUEST_OBJECTIVES.put("main_3", List.of(
            new QuestObjective(ObjectiveType.DISCOVER_LOCATION, "overseer_altar", 1, "Find the Overseer's Altar"),
            new QuestObjective(ObjectiveType.BOSS_KILL, "quantumfalcon:quantum_overseer", 1, "Defeat the Quantum Overseer"),
            new QuestObjective(ObjectiveType.COLLECT_ITEM, "quantumfalcon:overseers_eye", 1, "Obtain the Overseer's Eye")
        ));
        
        // Main 4: Guardians of Reality
        QUEST_OBJECTIVES.put("main_4", List.of(
            new QuestObjective(ObjectiveType.BOSS_KILL, "quantumfalcon:rift_warden", 1, "Defeat the Rift Warden"),
            new QuestObjective(ObjectiveType.BOSS_KILL, "quantumfalcon:entangled_twins", 1, "Defeat the Entangled Twins"),
            new QuestObjective(ObjectiveType.COLLECT_ITEM, "quantumfalcon:artifact_fragment", 4, "Collect All Artifact Fragments")
        ));
        
        // Main 5: The Singularity
        QUEST_OBJECTIVES.put("main_5", List.of(
            new QuestObjective(ObjectiveType.DISCOVER_LOCATION, "sovereign_throne", 1, "Find the Sovereign's Throne"),
            new QuestObjective(ObjectiveType.BOSS_KILL, "quantumfalcon:singularity_sovereign", 1, "Defeat the Singularity Sovereign"),
            new QuestObjective(ObjectiveType.CRAFT_ITEM, "quantumfalcon:heart_of_quantum", 1, "Craft the Heart of Quantum (Nether Star + Reality Gem + Quantum Core)")
        ));
        
        // ===== SIDE QUESTS =====
        
        // Side: Master Breeder
        QUEST_OBJECTIVES.put("side_breeder", List.of(
            new QuestObjective(ObjectiveType.BREED_FALCON, "any", 10, "Breed 10 Falcons"),
            new QuestObjective(ObjectiveType.BREED_FALCON, "anomaly", 1, "Breed an Anomaly Falcon"),
            new QuestObjective(ObjectiveType.REACH_BOND_LEVEL, "100", 1, "Max Bond with Any Falcon")
        ));
        
        // Side: Treasure Hunter
        QUEST_OBJECTIVES.put("side_treasure", List.of(
            new QuestObjective(ObjectiveType.COLLECT_ITEM, "quantumfalcon:quantum_shard", 100, "Collect 100 Quantum Shards"),
            new QuestObjective(ObjectiveType.COLLECT_ITEM, "quantumfalcon:rift_shard", 50, "Collect 50 Rift Shards"),
            new QuestObjective(ObjectiveType.DISCOVER_LOCATION, "quantum_well", 3, "Find 3 Quantum Wells")
        ));
        
        // Side: Explorer
        QUEST_OBJECTIVES.put("side_explorer", List.of(
            new QuestObjective(ObjectiveType.VISIT_BIOME, "quantum_biomes", 10, "Visit 10 Quantum Biomes"),
            new QuestObjective(ObjectiveType.ENTER_DIMENSION, "quantumfalcon:echo_rift", 1, "Enter the Rift"),
            new QuestObjective(ObjectiveType.DISCOVER_LOCATION, "ancient_nest", 1, "Discover an Ancient Nest")
        ));
        
        // Side: Boss Slayer
        QUEST_OBJECTIVES.put("side_boss_slayer", List.of(
            new QuestObjective(ObjectiveType.BOSS_KILL, "any", 10, "Defeat 10 Bosses Total"),
            new QuestObjective(ObjectiveType.BOSS_KILL, "quantumfalcon:singularity_sovereign", 3, "Defeat Sovereign 3 Times")
        ));
        
        // Side: Collector
        QUEST_OBJECTIVES.put("side_collector", List.of(
            new QuestObjective(ObjectiveType.TAME_FALCON, "unique_variants", 10, "Tame 10 Different Variants"),
            new QuestObjective(ObjectiveType.COLLECT_ITEM, "quantumfalcon:falcon_journal", 1, "Obtain the Falcon Journal")
        ));
    }

    // ============ Event Handlers ============

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            startAvailableChains(player);
            com.matt.quantumfalcon.network.ModNetworking.syncQuestProgress(player);
        }
    }

    /**
     * Track entity kills
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer player)) return;
        if (entity instanceof com.matt.quantumfalcon.entity.boss.QFBoss) return;
        
        String entityType = entity.getType().getDescriptionId();
        
        // Boss kills are credited in BossDeathHandler.onBossDefeated (every contributor, not just the killing blow)

        // Generic entity kills
        updateObjective(player, ObjectiveType.KILL_ENTITY, entityType, 1);
    }

    /**
     * COLLECT objectives count what you actually hold. Once a second, each active COLLECT objective's progress becomes
     * max(progress, items of that kind in the inventory). Dropping and re-picking one item no longer counts twice, and
     * pickup attempts that fail (full inventory) count nothing.
     */
    @SubscribeEvent
    public static void onPlayerTickCollect(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 7) return;
        syncCollectObjectives(player);
    }

    /** quest credit for a boss, called once per credited player. */
    public static void creditBossKill(ServerPlayer player, String bossId) {
        updateObjective(player, ObjectiveType.BOSS_KILL, bossId, 1);
        updateObjective(player, ObjectiveType.BOSS_KILL, "any", 1);
    }

    /** a story gift counts towards COLLECT as received, so tossing or losing an earlier gift can't cap the objective. */
    public static void creditReceived(ServerPlayer player, ItemStack stack) {
        updateObjective(player, ObjectiveType.COLLECT_ITEM, stack.getItem().builtInRegistryHolder().key().location().toString(), stack.getCount());
    }

    public static void syncCollectObjectives(ServerPlayer player) {
        PlayerQuestData questData = PlayerQuestData.get(player);
        if (questData == null) return;
        java.util.Set<String> targets = new java.util.HashSet<>();
        for (String questId : questData.getActiveQuests()) {
            List<QuestObjective> objectives = QUEST_OBJECTIVES.get(questId);
            if (objectives != null) for (QuestObjective o : objectives) if (o.type == ObjectiveType.COLLECT_ITEM) targets.add(o.target);
        }
        for (String target : targets) {
            int held = 0;
            var inv = player.getInventory();
            for (int k = 0; k < inv.getContainerSize(); k++) {
                ItemStack st = inv.getItem(k);
                if (!st.isEmpty() && st.getItem().builtInRegistryHolder().key().location().toString().equals(target)) held += st.getCount();
            }
            if (held > 0) setObjectiveAtLeast(player, ObjectiveType.COLLECT_ITEM, target, held);
        }
    }

    /**
     * Track item crafting
     */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        ItemStack stack = event.getCrafting();
        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
        
        updateObjective(player, ObjectiveType.CRAFT_ITEM, itemId, stack.getCount());
        // (COLLECT objectives are inventory totals now: see onPlayerTickCollect)
    }

    /**
     * Track block placement
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        Block block = event.getPlacedBlock().getBlock();
        String blockId = block.builtInRegistryHolder().key().location().toString();
        
        updateObjective(player, ObjectiveType.PLACE_BLOCK, blockId, 1);
    }

    /**
     * Track block breaking
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        
        Block block = event.getState().getBlock();
        String blockId = block.builtInRegistryHolder().key().location().toString();
        
        updateObjective(player, ObjectiveType.BREAK_BLOCK, blockId, 1);
    }

    /**
     * Track dimension travel
     */
    @SubscribeEvent
    public static void onDimensionChange(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        String dimensionId = event.getDimension().location().toString();
        updateObjective(player, ObjectiveType.ENTER_DIMENSION, dimensionId, 1);
    }

    /**
     * Track block interactions
     */
    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        String blockId = block.builtInRegistryHolder().key().location().toString();
        
        updateObjective(player, ObjectiveType.INTERACT_BLOCK, blockId, 1);
    }

    // ============ Custom Tracking Methods ============

    /**
     * Call when a falcon is tamed
     */
    public static void onFalconTamed(ServerPlayer player, QuantumFalconEntity falcon) {
        String variant = falcon.getVariant().toString().toLowerCase();
        updateObjective(player, ObjectiveType.TAME_FALCON, "any", 1);
        updateObjective(player, ObjectiveType.TAME_FALCON, variant, 1);
        
        // Track unique variants
        PlayerQuestData questData = PlayerQuestData.get(player);
        if (questData != null && questData.addTamedVariant(variant)) {
            setObjectiveAtLeast(player, ObjectiveType.TAME_FALCON, "unique_variants", questData.getTamedVariantCount());
        }
    }

    /**
     * Call when falcons are bred
     */
    public static void onFalconBred(ServerPlayer player, QuantumFalconEntity offspring) {
        updateObjective(player, ObjectiveType.BREED_FALCON, "any", 1);
        
        if (offspring.isAnomaly()) {
            updateObjective(player, ObjectiveType.BREED_FALCON, "anomaly", 1);
        }
        
        if (offspring.isLegendary()) {
            updateObjective(player, ObjectiveType.BREED_FALCON, "legendary", 1);
        }
    }

    /** Called when an egg hatches for its owner (breeding itself is counted when the egg is laid). */
    public static void onEggHatched(ServerPlayer player, QuantumFalconEntity baby, String variant) {
        if (baby != null && baby.isAnomaly()) updateObjective(player, ObjectiveType.BREED_FALCON, "anomaly", 1);
        if (baby != null && baby.isLegendary()) updateObjective(player, ObjectiveType.BREED_FALCON, "legendary", 1);
        if ("cosmic".equals(variant) || "singularity".equals(variant)) updateObjective(player, ObjectiveType.BREED_FALCON, "rare_variant", 1);
    }

    /**
     * Call when bond level changes
     */
    public static void onBondLevelChange(ServerPlayer player, QuantumFalconEntity falcon, int newLevel) {
        // Check bond thresholds
        if (newLevel >= 10) {
            updateObjective(player, ObjectiveType.REACH_BOND_LEVEL, "10", 1);
        }
        if (newLevel >= 50) {
            updateObjective(player, ObjectiveType.REACH_BOND_LEVEL, "50", 1);
        }
        if (newLevel >= 100) {
            updateObjective(player, ObjectiveType.REACH_BOND_LEVEL, "100", 1);
        }
    }

    /**
     * Call when a location is discovered
     */
    public static void onLocationDiscovered(ServerPlayer player, String locationId) {
        updateObjective(player, ObjectiveType.DISCOVER_LOCATION, locationId, 1);
    }

    /**
     * Call when visiting a biome
     */
    public static void onBiomeVisited(ServerPlayer player, String biomeId) {
        updateObjective(player, ObjectiveType.VISIT_BIOME, biomeId, 1);
        
        // Track quantum biome count
        if (biomeId.startsWith("quantumfalcon:")) {
            PlayerQuestData questData = PlayerQuestData.get(player);
            if (questData != null && questData.addVisitedBiome(biomeId)) {
                setObjectiveAtLeast(player, ObjectiveType.VISIT_BIOME, "quantum_biomes", questData.getVisitedBiomeCount());
            }
        }
    }

    /**
     * Call when an ability is used
     */
    public static void onAbilityUsed(ServerPlayer player, String abilityId) {
        updateObjective(player, ObjectiveType.USE_ABILITY, abilityId, 1);
    }

    // ============ Core Update Logic ============

    /** Objectives for a quest (shared with the client quest log for "n / required"). */
    public static List<QuestObjective> objectives(String questId) {
        return QUEST_OBJECTIVES.getOrDefault(questId, List.of());
    }

    /**
     * Update an objective for a player. targets match exactly ("any" objectives are fed by hooks that send "any"
     * explicitly), so one event can't double-count, and completion is checked once per quest.
     */
    public static void updateObjective(ServerPlayer player, ObjectiveType type, String target, int amount) {
        apply(player, type, target, amount, false);
    }

    /** For counters that are already totals (unique variants, biomes visited): progress = max(progress, value). */
    public static void setObjectiveAtLeast(ServerPlayer player, ObjectiveType type, String target, int value) {
        apply(player, type, target, value, true);
    }

    private static void apply(ServerPlayer player, ObjectiveType type, String target, int amount, boolean absolute) {
        if (player == null) return;
        if (type != ObjectiveType.COLLECT_ITEM) com.matt.quantumfalcon.codex.RiftCodex.onObjective(player, type.name(), target, amount, absolute);   // A1 Rift Codex (collect = held check there)
        PlayerQuestData questData = PlayerQuestData.get(player);
        if (questData == null) return;
        boolean changed = false;
        for (String questId : questData.getActiveQuests()) {
            List<QuestObjective> objectives = QUEST_OBJECTIVES.get(questId);
            if (objectives == null) continue;
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective objective = objectives.get(i);
                if (objective.type != type || !objective.target.equals(target)) continue;
                int current = questData.getObjectiveProgress(questId, i);
                int next = Math.min(absolute ? Math.max(current, amount) : current + amount, objective.required);
                if (next == current) continue;
                questData.setObjectiveProgress(questId, i, next);
                changed = true;
                if (current < objective.required && next >= objective.required) onObjectiveComplete(player, questId, i, objective);
            }
            if (isQuestComplete(questData, questId)) onQuestComplete(player, questId);
        }
        if (changed) com.matt.quantumfalcon.network.ModNetworking.syncQuestProgress(player);
    }

    /**
     * the tutorial and main lines run themselves. The first tutorial quest starts on first login and each finished
     * tutorial/main quest starts its successors; side quests become "Available" and are accepted from the J log (server
     * packet) or /qf quest accept (permission 0). Also backfills older saves.
     */
    public static void startAvailableChains(ServerPlayer player) {
        PlayerQuestData data = PlayerQuestData.get(player);
        if (data == null) return;
        boolean changed = false;
        for (QuestSystem.Quest q : QuestSystem.QUESTS.values()) {
            if (q.category == QuestSystem.QuestCategory.SIDE || q.category == QuestSystem.QuestCategory.CHALLENGE) continue;
            if (data.isQuestActive(q.id) || data.isQuestCompleted(q.id)) continue;
            if (q.prerequisite != null && !data.isQuestCompleted(q.prerequisite)) continue;
            data.acceptQuest(q.id);
            changed = true;
            player.displayClientMessage(Component.literal("\u00a7e\u2726 New quest: \u00a7f" + q.getName() + " \u00a77(J to view)"), false);
        }
        if (changed) com.matt.quantumfalcon.network.ModNetworking.syncQuestProgress(player);
    }

    /** Server-side accept used by the J log packet and /qf quest accept. Returns null on success or a reason. */
    public static String tryAccept(ServerPlayer player, String questId) {
        QuestSystem.Quest q = QuestSystem.getQuest(questId);
        if (q == null) return "Unknown quest: " + questId;
        PlayerQuestData data = PlayerQuestData.get(player);
        if (data.isQuestCompleted(questId)) return "Already completed: " + q.getName();
        if (data.isQuestActive(questId)) return "Already active: " + q.getName();
        if (q.prerequisite != null && !data.isQuestCompleted(q.prerequisite)) {
            QuestSystem.Quest pre = QuestSystem.getQuest(q.prerequisite);
            return "Finish " + (pre != null ? pre.getName() : q.prerequisite) + " first";
        }
        data.acceptQuest(questId);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.QUEST_ACCEPT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        com.matt.quantumfalcon.network.ModNetworking.syncQuestProgress(player);
        return null;
    }

    /**
     * Check if all objectives for a quest are complete
     */
    private static boolean isQuestComplete(PlayerQuestData questData, String questId) {
        List<QuestObjective> objectives = QUEST_OBJECTIVES.get(questId);
        if (objectives == null) return false;
        
        for (int i = 0; i < objectives.size(); i++) {
            if (questData.getObjectiveProgress(questId, i) < objectives.get(i).required) {
                return false;
            }
        }
        return true;
    }

    /**
     * Called when a single objective is completed
     */
    private static void onObjectiveComplete(ServerPlayer player, String questId, int objectiveIndex, QuestObjective objective) {
        // Play sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.QUEST_ACCEPT.get(), SoundSource.PLAYERS, 1.0f, 1.5f);
        
        // Send message
        player.displayClientMessage(
            Component.literal("§a✓ §7Objective Complete: §f" + objective.description),
            false
        );
        
        LOGGER.debug("Player {} completed objective {} for quest {}", player.getName().getString(), objectiveIndex, questId);
    }

    /**
     * Called when all objectives are complete and quest can be turned in
     */
    private static void onQuestComplete(ServerPlayer player, String questId) {
        PlayerQuestData questData = PlayerQuestData.get(player);
        if (questData == null || questData.isQuestComplete(questId)) return;
        
        questData.setQuestComplete(questId, true);
        questData.completeQuest(questId);   // actually completes it (prerequisites check the completed list)

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.QUEST_COMPLETE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        QuestSystem.Quest quest = QuestSystem.QUESTS.get(questId);
        String questName = quest != null ? quest.getName() : questId;
        player.displayClientMessage(Component.literal("\u00a76\u2605 \u00a7eQuest Complete: \u00a7f" + questName), false);

        // Rewards are handed over on completion (there is no turn-in NPC)
        if (quest != null) {
            for (ItemStack reward : quest.rewards) {
                ItemStack copy = reward.copy();
                if (!player.getInventory().add(copy)) player.drop(copy, false);
            }
            if (quest.xpReward > 0) player.giveExperiencePoints(quest.xpReward);
        }

        boolean isMainQuest = questId.startsWith("main_");
        SkillPointSystem.awardQuestCompletePoints(player, isMainQuest);

        startAvailableChains(player);
        for (QuestSystem.Quest q : QuestSystem.QUESTS.values())
            if (questId.equals(q.prerequisite) && (q.category == QuestSystem.QuestCategory.SIDE || q.category == QuestSystem.QuestCategory.CHALLENGE))
                player.displayClientMessage(Component.literal("\u00a7b\u2726 Side quest available: \u00a7f" + q.getName() + " \u00a77(J \u2192 Available \u2192 Accept)"), false);
        com.matt.quantumfalcon.network.ModNetworking.syncQuestProgress(player);

        LOGGER.info("Player {} completed quest {}", player.getName().getString(), questId);
    }

    // ============ Quest Objective Data Class ============

    public static class QuestObjective {
        public final ObjectiveType type;
        public final String target;
        public final int required;
        public final String description;

        public QuestObjective(ObjectiveType type, String target, int required, String description) {
            this.type = type;
            this.target = target;
            this.required = required;
            this.description = description;
        }
    }
}
