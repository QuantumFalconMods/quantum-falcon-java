package com.matt.quantumfalcon.quest;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

/**
 * Player Quest Data for Quantum Falcon.
 * Tracks quest progress, completions, and discovered content.
 */
public class PlayerQuestData implements INBTSerializable<CompoundTag> {

    // ============ Attachment Registration ============
    
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, QuantumFalcon.MOD_ID);

    public static final Supplier<AttachmentType<PlayerQuestData>> QUEST_DATA_ATTACHMENT =
        ATTACHMENT_TYPES.register("player_quest_data", () ->
            AttachmentType.builder(PlayerQuestData::new)
                .serialize(new IAttachmentSerializer<CompoundTag, PlayerQuestData>() {
                    @Override
                    public PlayerQuestData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                        PlayerQuestData data = new PlayerQuestData();
                        data.deserializeNBT(provider, tag);
                        return data;
                    }

                    @Override
                    public @Nullable CompoundTag write(PlayerQuestData attachment, HolderLookup.Provider provider) {
                        return attachment.serializeNBT(provider);
                    }
                })
                .copyOnDeath()
                .build()
        );

    // ============ Data Fields ============
    
    private List<String> activeQuests = new ArrayList<>();
    private List<String> completedQuests = new ArrayList<>();
    private Map<String, Map<Integer, Integer>> objectiveProgress = new HashMap<>();
    private Map<String, Boolean> questComplete = new HashMap<>();
    
    // Discovery tracking
    private Set<String> tamedVariants = new HashSet<>();
    private Set<String> visitedBiomes = new HashSet<>();
    private Set<String> discoveredLocations = new HashSet<>();
    private Set<String> defeatedBosses = new HashSet<>();

    // ============ Static Access ============
    
    public static PlayerQuestData get(Player player) {
        return player.getData(QUEST_DATA_ATTACHMENT.get());
    }

    // ============ Quest Management ============
    
    public List<String> getActiveQuests() {
        return new ArrayList<>(activeQuests);
    }

    public List<String> getCompletedQuests() {
        return new ArrayList<>(completedQuests);
    }

    public boolean isQuestActive(String questId) {
        return activeQuests.contains(questId);
    }

    public boolean isQuestCompleted(String questId) {
        return completedQuests.contains(questId);
    }

    public void acceptQuest(String questId) {
        if (!activeQuests.contains(questId) && !completedQuests.contains(questId)) {
            activeQuests.add(questId);
            objectiveProgress.put(questId, new HashMap<>());
            questComplete.put(questId, false);
        }
    }
    
    public void activateQuest(String questId) {
        acceptQuest(questId);
    }
    
    public enum QuestStatus {
        AVAILABLE,
        ACTIVE,
        COMPLETED,
        LOCKED
    }
    
    public QuestStatus getQuestStatus(String questId) {
        if (completedQuests.contains(questId)) {
            return QuestStatus.COMPLETED;
        } else if (activeQuests.contains(questId)) {
            return QuestStatus.ACTIVE;
        } else {
            QuestSystem.Quest quest = QuestSystem.getQuest(questId);
            if (quest != null && quest.prerequisite != null && !completedQuests.contains(quest.prerequisite)) {
                return QuestStatus.LOCKED;
            }
            return QuestStatus.AVAILABLE;
        }
    }
    
    public QuestStatus status(String questId) {
        return getQuestStatus(questId);
    }

    public void abandonQuest(String questId) {
        activeQuests.remove(questId);
        objectiveProgress.remove(questId);
        questComplete.remove(questId);
    }

    /** Clears every active and completed quest and all objective progress (admin /qf quest reset). */
    public void resetQuests() {
        activeQuests.clear();
        completedQuests.clear();
        objectiveProgress.clear();
        questComplete.clear();
    }

    public void completeQuest(String questId) {
        if (activeQuests.contains(questId)) {
            activeQuests.remove(questId);
            completedQuests.add(questId);
            questComplete.put(questId, true);
        }
    }

    // ============ Objective Progress ============
    
    public int getObjectiveProgress(String questId, int objectiveIndex) {
        Map<Integer, Integer> questProgress = objectiveProgress.get(questId);
        if (questProgress == null) return 0;
        return questProgress.getOrDefault(objectiveIndex, 0);
    }

    public void setObjectiveProgress(String questId, int objectiveIndex, int progress) {
        objectiveProgress.computeIfAbsent(questId, k -> new HashMap<>())
            .put(objectiveIndex, progress);
    }

    public void addObjectiveProgress(String questId, int objectiveIndex, int amount) {
        int current = getObjectiveProgress(questId, objectiveIndex);
        setObjectiveProgress(questId, objectiveIndex, current + amount);
    }

    public Map<String, Map<Integer, Integer>> getAllObjectiveProgress() {
        return new HashMap<>(objectiveProgress);
    }

    public boolean isQuestComplete(String questId) {
        return questComplete.getOrDefault(questId, false);
    }

    public void setQuestComplete(String questId, boolean complete) {
        questComplete.put(questId, complete);
    }

    // ============ Variant Tracking ============
    
    public boolean addTamedVariant(String variant) {
        return tamedVariants.add(variant);
    }

    public int getTamedVariantCount() {
        return tamedVariants.size();
    }

    public Set<String> getTamedVariants() {
        return new HashSet<>(tamedVariants);
    }

    // ============ Biome Tracking ============
    
    public boolean addVisitedBiome(String biomeId) {
        return visitedBiomes.add(biomeId);
    }

    public int getVisitedBiomeCount() {
        return visitedBiomes.size();
    }

    public Set<String> getVisitedBiomes() {
        return new HashSet<>(visitedBiomes);
    }

    // ============ Location Tracking ============
    
    public boolean addDiscoveredLocation(String locationId) {
        return discoveredLocations.add(locationId);
    }

    public boolean hasDiscoveredLocation(String locationId) {
        return discoveredLocations.contains(locationId);
    }

    public Set<String> getDiscoveredLocations() {
        return new HashSet<>(discoveredLocations);
    }

    // ============ Boss Tracking ============
    
    public boolean addDefeatedBoss(String bossId) {
        return defeatedBosses.add(bossId);
    }

    public boolean hasDefeatedBoss(String bossId) {
        return defeatedBosses.contains(bossId);
    }

    public Set<String> getDefeatedBosses() {
        return new HashSet<>(defeatedBosses);
    }

    // ============ NBT Serialization ============
    
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        
        // Active quests
        ListTag activeList = new ListTag();
        for (String quest : activeQuests) {
            activeList.add(StringTag.valueOf(quest));
        }
        tag.put("activeQuests", activeList);
        
        // Completed quests
        ListTag completedList = new ListTag();
        for (String quest : completedQuests) {
            completedList.add(StringTag.valueOf(quest));
        }
        tag.put("completedQuests", completedList);
        
        // Objective progress
        CompoundTag progressTag = new CompoundTag();
        for (Map.Entry<String, Map<Integer, Integer>> entry : objectiveProgress.entrySet()) {
            CompoundTag questTag = new CompoundTag();
            for (Map.Entry<Integer, Integer> obj : entry.getValue().entrySet()) {
                questTag.putInt("obj_" + obj.getKey(), obj.getValue());
            }
            progressTag.put(entry.getKey(), questTag);
        }
        tag.put("objectiveProgress", progressTag);
        
        // Quest complete flags
        CompoundTag completeTag = new CompoundTag();
        for (Map.Entry<String, Boolean> entry : questComplete.entrySet()) {
            completeTag.putBoolean(entry.getKey(), entry.getValue());
        }
        tag.put("questComplete", completeTag);
        
        // Discovery tracking
        tag.put("tamedVariants", serializeStringSet(tamedVariants));
        tag.put("visitedBiomes", serializeStringSet(visitedBiomes));
        tag.put("discoveredLocations", serializeStringSet(discoveredLocations));
        tag.put("defeatedBosses", serializeStringSet(defeatedBosses));
        
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        // Active quests
        activeQuests.clear();
        ListTag activeList = tag.getList("activeQuests", Tag.TAG_STRING);
        for (int i = 0; i < activeList.size(); i++) {
            activeQuests.add(activeList.getString(i));
        }
        
        // Completed quests
        completedQuests.clear();
        ListTag completedList = tag.getList("completedQuests", Tag.TAG_STRING);
        for (int i = 0; i < completedList.size(); i++) {
            completedQuests.add(completedList.getString(i));
        }
        
        // Objective progress
        objectiveProgress.clear();
        CompoundTag progressTag = tag.getCompound("objectiveProgress");
        for (String questId : progressTag.getAllKeys()) {
            CompoundTag questTag = progressTag.getCompound(questId);
            Map<Integer, Integer> questProgress = new HashMap<>();
            for (String key : questTag.getAllKeys()) {
                if (key.startsWith("obj_")) {
                    int index = Integer.parseInt(key.substring(4));
                    questProgress.put(index, questTag.getInt(key));
                }
            }
            objectiveProgress.put(questId, questProgress);
        }
        
        // Quest complete flags
        questComplete.clear();
        CompoundTag completeTag = tag.getCompound("questComplete");
        for (String questId : completeTag.getAllKeys()) {
            questComplete.put(questId, completeTag.getBoolean(questId));
        }
        
        // Discovery tracking
        tamedVariants = deserializeStringSet(tag.getList("tamedVariants", Tag.TAG_STRING));
        visitedBiomes = deserializeStringSet(tag.getList("visitedBiomes", Tag.TAG_STRING));
        discoveredLocations = deserializeStringSet(tag.getList("discoveredLocations", Tag.TAG_STRING));
        defeatedBosses = deserializeStringSet(tag.getList("defeatedBosses", Tag.TAG_STRING));
    }

    private ListTag serializeStringSet(Set<String> set) {
        ListTag list = new ListTag();
        for (String s : set) {
            list.add(StringTag.valueOf(s));
        }
        return list;
    }

    private Set<String> deserializeStringSet(ListTag list) {
        Set<String> set = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            set.add(list.getString(i));
        }
        return set;
    }
}
