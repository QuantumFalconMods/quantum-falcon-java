package com.matt.quantumfalcon.quest;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Quest System for Quantum Falcon.
 * Defines all quests with objectives and rewards.
 */
public class QuestSystem {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Map<String, Quest> QUESTS = new LinkedHashMap<>();

    // ============ Quest Categories ============
    
    public enum QuestCategory {
        TUTORIAL("Tutorial", "§a", 0),
        MAIN("Main Story", "§6", 1),
        SIDE("Side Quest", "§b", 2),
        CHALLENGE("Challenge", "§c", 3);

        public final String displayName;
        public final String colorCode;
        public final int sortOrder;

        QuestCategory(String displayName, String colorCode, int sortOrder) {
            this.displayName = displayName;
            this.colorCode = colorCode;
            this.sortOrder = sortOrder;
        }
    }

    // ============ Quest Initialization ============
    
    static {
        initializeQuests();
    }

    private static void initializeQuests() {
        // ===== TUTORIAL QUESTS =====
        registerQuest(new Quest("tutorial_basics", QuestCategory.TUTORIAL,
            "First Steps", 
            "Learn the basics of quantum energy and crafting.",
            List.of(
                "Collect 5 Quantum Shards",
                "Craft a Quantum Core"
            ),
            List.of(
                new ItemStack(Items.DIAMOND, 2),
                new ItemStack(Items.EXPERIENCE_BOTTLE, 5)
            ),
            50, // XP reward
            null // No prerequisite
        ));
        
        registerQuest(new Quest("tutorial_falcon", QuestCategory.TUTORIAL,
            "A Faithful Companion",
            "Find and tame your first Quantum Falcon.",
            List.of(
                "Tame a Quantum Falcon",
                "Reach Bond Level 10"
            ),
            List.of(
                new ItemStack(Items.GOLDEN_APPLE, 3)
            ),
            100,
            "tutorial_basics"
        ));
        
        registerQuest(new Quest("tutorial_breeding", QuestCategory.TUTORIAL,
            "The Circle of Life",
            "Learn to breed falcons and understand genetics.",
            List.of(
                "Collect 2 Quantum Crystals",
                "Breed Two Falcons"
            ),
            List.of(
                new ItemStack(Items.DIAMOND, 5)
            ),
            150,
            "tutorial_falcon"
        ));
        registerQuest(new Quest("first_breed", QuestCategory.TUTORIAL,
            "First Breed",
            "Breed two falcons for the first time.",
            List.of("Breed two tamed falcons"),
            List.of(
                new ItemStack(ModItems.QUANTUM_BERRY.get(), 8),
                new ItemStack(ModItems.GOLDEN_FALCON_EGG.get(), 1)
            ),
            0,
            "tutorial_breeding"
        ));
        registerQuest(new Quest("genetic_mastery", QuestCategory.SIDE,
            "Genetic Mastery",
            "Hatch a cosmic or singularity variant falcon.",
            List.of("Hatch an egg that yields cosmic or singularity variant"),
            List.of(
                new ItemStack(Items.NETHERITE_INGOT, 1),
                new ItemStack(ModItems.QUANTUM_CORE.get(), 1)
            ),
            0,
            "first_breed"
        ));

        // ===== MAIN STORY QUESTS =====
        registerQuest(new Quest("main_1", QuestCategory.MAIN,
            "The Quantum Awakening",
            "Something is holding back the dawn. Follow the shards, and find the bird that can see what holds it.",
            List.of(
                "Gather 10 Quantum Shards",
                "Find and Tame a Falcon",
                "Craft a Falcon Whistle"
            ),
            List.of(
                new ItemStack(Items.DIAMOND, 8),
                new ItemStack(Items.EMERALD, 16)
            ),
            500,
            "tutorial_breeding"
        ));
        
        registerQuest(new Quest("main_2", QuestCategory.MAIN,
            "Echoes of the Rift",
            "The rift dimension calls. Rift Shards hide in Overworld riftslate dungeons and Echo Rift Gate ruins (a Quantum Compass points the way). Build a 6x7 ring of Rift Frames and light it with a Rift Igniter. See the Codex: The Rift.",
            List.of(
                "Collect 8 Rift Shards (riftslate dungeons, Echo Rift Gates)",
                "Craft 22 Rift Frame Blocks",
                "Enter the Rift Dimension"
            ),
            List.of(
                new ItemStack(Items.NETHERITE_INGOT, 2)
            ),
            750,
            "main_1"
        ));
        
        registerQuest(new Quest("main_3", QuestCategory.MAIN,
            "The Overseer's Gaze",
            "Something on a throne in the Rift is still watching. Find the Overseer's Altar and end his long night.",
            List.of(
                "Find the Overseer's Altar",
                "Defeat the Quantum Overseer",
                "Obtain the Overseer's Eye"
            ),
            List.of(
                new ItemStack(Items.NETHER_STAR, 1)
            ),
            1000,
            "main_2"
        ));
        
        registerQuest(new Quest("main_4", QuestCategory.MAIN,
            "Guardians of Reality",
            "Three more guard the held hour. Beat them and take back its pieces: the relics.",
            List.of(
                "Defeat the Rift Warden",
                "Defeat the Entangled Twins",
                "Collect All 4 Artifact Fragments"
            ),
            List.of(
                new ItemStack(Items.NETHERITE_INGOT, 4)
            ),
            1500,
            "main_3"
        ));
        
        registerQuest(new Quest("main_5", QuestCategory.MAIN,
            "The Singularity",
            "At the vault's heart, the will that held the hour is waiting for you. Then choose what morning means.",
            List.of(
                "Find the Sovereign's Throne",
                "Defeat the Singularity Sovereign",
                "Craft the Heart of Quantum (Nether Star + Reality Gem + Quantum Core)"
            ),
            List.of(
                new ItemStack(Items.DRAGON_EGG, 1)
            ),
            3000,
            "main_4"
        ));

        // ===== SIDE QUESTS =====
        registerQuest(new Quest("side_breeder", QuestCategory.SIDE,
            "Master Breeder",
            "Become a master of falcon genetics.",
            List.of(
                "Breed 10 Falcons",
                "Breed an Anomaly Falcon",
                "Max Bond with Any Falcon"
            ),
            List.of(
                new ItemStack(Items.DIAMOND, 16)
            ),
            500,
            "tutorial_breeding"
        ));
        
        registerQuest(new Quest("side_treasure", QuestCategory.SIDE,
            "Treasure Hunter",
            "Collect vast amounts of quantum resources.",
            List.of(
                "Collect 100 Quantum Shards",
                "Collect 50 Rift Shards",
                "Find 3 Quantum Wells"
            ),
            List.of(
                new ItemStack(Items.EMERALD_BLOCK, 8)
            ),
            400,
            "main_1"
        ));
        
        registerQuest(new Quest("side_explorer", QuestCategory.SIDE,
            "Rift Explorer",
            "Explore the many biomes of the rift dimension.",
            List.of(
                "Visit 10 Quantum Biomes",
                "Enter the Rift",
                "Discover an Ancient Nest"
            ),
            List.of(
                new ItemStack(Items.ELYTRA, 1)
            ),
            600,
            "main_2"
        ));
        
        registerQuest(new Quest("side_boss_slayer", QuestCategory.SIDE,
            "Boss Slayer",
            "Prove your combat prowess against the bosses.",
            List.of(
                "Defeat 10 Bosses Total",
                "Defeat Sovereign 3 Times"
            ),
            List.of(
                new ItemStack(Items.NETHERITE_BLOCK, 2)
            ),
            2000,
            "main_5"
        ));
        
        registerQuest(new Quest("side_collector", QuestCategory.SIDE,
            "Variant Collector",
            "Collect all falcon variants.",
            List.of(
                "Tame 10 Different Variants",
                "Obtain the Falcon Journal"
            ),
            List.of(
                new ItemStack(Items.DIAMOND_BLOCK, 4)
            ),
            800,
            "main_1"
        ));

        LOGGER.info("Registered {} quests", QUESTS.size());
    }

    private static void registerQuest(Quest quest) {
        QUESTS.put(quest.id, quest);
    }

    // ============ Public API ============
    
    public static Quest getQuest(String questId) {
        return QUESTS.get(questId);
    }

    public static List<Quest> getQuestsByCategory(QuestCategory category) {
        List<Quest> result = new ArrayList<>();
        for (Quest quest : QUESTS.values()) {
            if (quest.category == category) {
                result.add(quest);
            }
        }
        return result;
    }

    public static List<Quest> getAvailableQuests(PlayerQuestData playerData) {
        List<Quest> available = new ArrayList<>();
        for (Quest quest : QUESTS.values()) {
            if (!playerData.isQuestActive(quest.id) && 
                !playerData.isQuestCompleted(quest.id) &&
                (quest.prerequisite == null || playerData.isQuestCompleted(quest.prerequisite))) {
                available.add(quest);
            }
        }
        return available;
    }

    // ============ Quest Class ============
    
    public static class Quest {
        public final String id;
        public final QuestCategory category;
        public final String name;
        public final String description;
        public final List<String> objectiveDescriptions;
        public final List<ItemStack> rewards;
        public final int xpReward;
        public final String prerequisite;

        public Quest(String id, QuestCategory category, String name, String description,
                     List<String> objectiveDescriptions, List<ItemStack> rewards, 
                     int xpReward, String prerequisite) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.description = description;
            this.objectiveDescriptions = objectiveDescriptions;
            this.rewards = rewards;
            this.xpReward = xpReward;
            this.prerequisite = prerequisite;
        }

        public String getName() {
            return name;
        }
        
        // Convenience properties for compatibility with older code
        public String getTitle() {
            return name;
        }
        
        public String title() {
            return name;
        }
        
        public QuestCategory type() {
            return category;
        }
        
        public String prerequisiteQuestId() {
            return prerequisite;
        }

        public Component getDisplayName() {
            return Component.literal(category.colorCode + name);
        }

        public Component getDescriptionComponent() {
            return Component.literal("§7" + description);
        }

        public int getObjectiveCount() {
            return objectiveDescriptions.size();
        }

        public ResourceLocation getIconLocation() {
            return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID,
                "textures/gui/quests/" + id + ".png");
        }
    }
    
    // ============ Player Quest Methods ============
    
    public static boolean acceptQuest(net.minecraft.world.entity.player.Player player, Quest quest) {
        PlayerQuestData questData = PlayerQuestData.get(player);
        if (questData == null) return false;
        
        // Check prerequisites
        if (quest.prerequisite != null && !questData.isQuestCompleted(quest.prerequisite)) {
            return false;
        }
        
        // Activate quest
        questData.activateQuest(quest.id);
        return true;
    }
    
    public static boolean hasCompletedQuest(net.minecraft.world.entity.player.Player player, String questId) {
        PlayerQuestData questData = PlayerQuestData.get(player);
        return questData != null && questData.isQuestCompleted(questId);
    }
    
    public static Quest getQuestDefinition(String questId) {
        return QUESTS.get(questId);
    }
}
