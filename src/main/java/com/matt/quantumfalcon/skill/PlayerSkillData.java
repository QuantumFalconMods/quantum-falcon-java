package com.matt.quantumfalcon.skill;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Player Skill Data for Quantum Falcon.
 * Tracks skill points and unlocked skills.
 */
public class PlayerSkillData implements INBTSerializable<CompoundTag> {

    // ============ Attachment Registration ============
    
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, QuantumFalcon.MOD_ID);

    public static final Supplier<AttachmentType<PlayerSkillData>> SKILL_DATA_ATTACHMENT =
        ATTACHMENT_TYPES.register("player_skill_data", () ->
            AttachmentType.builder(PlayerSkillData::new)
                .serialize(new IAttachmentSerializer<CompoundTag, PlayerSkillData>() {
                    @Override
                    public PlayerSkillData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                        PlayerSkillData data = new PlayerSkillData();
                        data.deserializeNBT(provider, tag);
                        return data;
                    }

                    @Override
                    public @Nullable CompoundTag write(PlayerSkillData attachment, HolderLookup.Provider provider) {
                        return attachment.serializeNBT(provider);
                    }
                })
                .copyOnDeath()
                .build()
        );

    // ============ Data Fields ============
    
    private int availableSkillPoints = 0;
    private int totalSpentPoints = 0;
    private Set<String> unlockedSkills = new HashSet<>();
    private Map<String, Integer> skillLevels = new HashMap<>();
    private boolean receivedStarterPoints = false;
    private boolean tamedFirstFalcon = false;
    
    // XP and leveling
    private int currentXP = 0;
    private int playerLevel = 1;

    // ============ Static Access ============
    
    public static PlayerSkillData get(Player player) {
        return player.getData(SKILL_DATA_ATTACHMENT.get());
    }

    // ============ Skill Point Management ============
    
    public int getAvailableSkillPoints() {
        return availableSkillPoints;
    }

    public void setAvailableSkillPoints(int points) {
        this.availableSkillPoints = Math.max(0, points);
    }

    public void addSkillPoints(int amount) {
        this.availableSkillPoints += amount;
    }

    public void spendSkillPoints(int amount) {
        this.availableSkillPoints = Math.max(0, this.availableSkillPoints - amount);
        this.totalSpentPoints += amount;
    }

    public int getTotalSpentPoints() {
        return totalSpentPoints;
    }

    // ============ Skill Unlocking ============
    
    public Set<String> getUnlockedSkills() {
        return new HashSet<>(unlockedSkills);
    }

    public boolean hasSkill(String skillId) {
        return unlockedSkills.contains(skillId);
    }

    /** Server-side purchase: prerequisites, the Lineage hatch gate and points (same rules as the tooltip's lock reason). */
    public boolean tryUnlockSkill(Player player, String skillId) {
        return unlockReason(player, skillId) == null && doUnlock(skillId);
    }

    /** Null when the node can be bought now, else the reason (shown to the player). */
    public String unlockReason(Player player, String skillId) {
        SkillTreeSystem.Skill skill = SkillTreeSystem.getSkill(skillId);
        if (skill == null) return "Unknown skill " + skillId;
        int hatched = player == null ? 0 : SkillPointSystem.highestTierHatched(player);
        return skill.lockReason(unlockedSkills, availableSkillPoints, hatched);
    }

    private boolean doUnlock(String skillId) {
        SkillTreeSystem.Skill skill = SkillTreeSystem.getSkill(skillId);
        unlockedSkills.add(skillId);
        spendSkillPoints(skill.getCost());
        return true;
    }

    public void unlockSkillAdmin(String skillId) {
        unlockedSkills.add(skillId);
    }

    // ============ Reset ============
    
    public void resetSkills() {
        unlockedSkills.clear();
        totalSpentPoints = 0;
    }

    // ============ Flags ============
    
    public boolean hasReceivedStarterPoints() {
        return receivedStarterPoints;
    }

    public void setReceivedStarterPoints(boolean value) {
        this.receivedStarterPoints = value;
    }

    public boolean hasTamedFirstFalcon() {
        return tamedFirstFalcon;
    }

    public void setTamedFirstFalcon(boolean value) {
        this.tamedFirstFalcon = value;
    }
    
    // ============ XP and Leveling System ============
    
    public void addXP(Player player, int xp) {
        this.currentXP += xp;
        
        // Check for level up
        while (this.currentXP >= getXpForNextLevel() && this.playerLevel < 100) {
            this.currentXP -= getXpForNextLevel();
            this.playerLevel++;   // B5: levels are cosmetic now; skill points come only from the finite sources in SkillPointSystem
        }
    }
    
    public int getPlayerLevel() {
        return playerLevel;
    }
    
    public int getCurrentXP() {
        return currentXP;
    }
    
    public int getXpForNextLevel() {
        // Progressive XP curve: 100 * level ^ 1.5
        return (int) (100 * Math.pow(playerLevel, 1.5));
    }
    
    public int getSkillLevel(String skillId) {
        return skillLevels.getOrDefault(skillId, unlockedSkills.contains(skillId) ? 1 : 0);
    }
    
    // ============ Mastery Marks (B5 point sink) ============
    // Effects live in SkillEffects; the old getters (and their bonus numbers that matched no tooltip) are gone.
    public static final int MASTERY_MARK_COST = 1;
    private int masteryMarks = 0;

    public int getMasteryMarks() { return masteryMarks; }

    /** Cosmetic sink for points nothing else can use: only once every node is owned. */
    public boolean buyMasteryMark() {
        if (unlockedSkills.size() < SkillTreeSystem.getTotalSkillCount() || availableSkillPoints < MASTERY_MARK_COST) return false;
        spendSkillPoints(MASTERY_MARK_COST);
        masteryMarks++;
        return true;
    }

    // ============ Save migration (W18) ============
    public static final int TREE_VERSION = 2;
    private int treeVersion = TREE_VERSION;

    /** Pre-Stage-E saves: mapped ids keep their unlock, removed ids are refunded at their old cost. Returns the refund. */
    int migrate() {
        if (treeVersion >= TREE_VERSION) return 0;
        int refund = 0;
        Set<String> next = new HashSet<>();
        for (String id : unlockedSkills) {
            if (SkillTreeSystem.getSkill(id) != null) { next.add(id); continue; }
            String mapped = SkillTreeSystem.MIGRATION.get(id);
            SkillTreeSystem.Skill ns = mapped == null ? null : SkillTreeSystem.getSkill(mapped);
            int old = SkillTreeSystem.OLD_COST.getOrDefault(id, 1);
            if (ns != null && next.add(mapped)) refund += Math.max(0, old - ns.cost);   // cheaper now: refund the difference
            else refund += old;
        }
        // a migrated node whose prerequisites didn't migrate is refunded too, so the tree stays consistent
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String id : new HashSet<>(next)) {
                SkillTreeSystem.Skill s = SkillTreeSystem.getSkill(id);
                if (!s.canUnlock(next)) { next.remove(id); refund += s.cost; changed = true; }
            }
        }
        unlockedSkills = next;
        skillLevels.clear();
        availableSkillPoints += refund;
        totalSpentPoints = Math.max(0, totalSpentPoints - refund);
        treeVersion = TREE_VERSION;
        return refund;
    }

    /** Test hook: load an old-format skill set. */
    public int migrateForTest(Set<String> oldIds, int points) {
        unlockedSkills = new HashSet<>(oldIds); availableSkillPoints = points; treeVersion = 1;
        return migrate();
    }

    // ============ NBT Serialization ============
    
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("availablePoints", availableSkillPoints);
        tag.putInt("spentPoints", totalSpentPoints);
        tag.putBoolean("receivedStarter", receivedStarterPoints);
        tag.putBoolean("tamedFirst", tamedFirstFalcon);
        tag.putInt("currentXP", currentXP);
        tag.putInt("playerLevel", playerLevel);
        
        // Unlocked skills
        ListTag skillList = new ListTag();
        for (String skill : unlockedSkills) {
            skillList.add(StringTag.valueOf(skill));
        }
        tag.put("unlockedSkills", skillList);
        
        // Skill levels
        CompoundTag levelsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : skillLevels.entrySet()) {
            levelsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("skillLevels", levelsTag);
        tag.putInt("masteryMarks", masteryMarks);
        tag.putInt("treeVersion", treeVersion);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.availableSkillPoints = tag.getInt("availablePoints");
        this.totalSpentPoints = tag.getInt("spentPoints");
        this.receivedStarterPoints = tag.getBoolean("receivedStarter");
        this.tamedFirstFalcon = tag.getBoolean("tamedFirst");
        this.currentXP = tag.getInt("currentXP");
        this.playerLevel = tag.contains("playerLevel") ? tag.getInt("playerLevel") : 1;
        
        // Unlocked skills
        this.unlockedSkills.clear();
        ListTag skillList = tag.getList("unlockedSkills", Tag.TAG_STRING);
        for (int i = 0; i < skillList.size(); i++) {
            this.unlockedSkills.add(skillList.getString(i));
        }
        
        // Skill levels
        this.skillLevels.clear();
        if (tag.contains("skillLevels")) {
            CompoundTag levelsTag = tag.getCompound("skillLevels");
            for (String key : levelsTag.getAllKeys()) {
                this.skillLevels.put(key, levelsTag.getInt(key));
            }
        }
        this.masteryMarks = tag.getInt("masteryMarks");
        this.treeVersion = tag.contains("treeVersion") ? tag.getInt("treeVersion") : 1;
        migrate();
    }
}
