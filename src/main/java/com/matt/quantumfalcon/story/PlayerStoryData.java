package com.matt.quantumfalcon.story;

import com.matt.quantumfalcon.story.StorylineManager.StoryChapter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Stores player story progression data.
 * Tracks which story chapters have been seen and the chosen ending.
 */
public class PlayerStoryData {
    // a player attachment (saved with the player, copied on death, one instance per side) instead of a static
    // UUID map over getPersistentData(), which was shared by the client and server threads in single-player and could lose
    // the ending on respawn/restart.
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, QuantumFalcon.MOD_ID);
    public static final Supplier<AttachmentType<PlayerStoryData>> STORY =
        ATTACHMENT_TYPES.register("player_story_data", () -> AttachmentType.builder(PlayerStoryData::new)
            .serialize(new IAttachmentSerializer<CompoundTag, PlayerStoryData>() {
                @Override public PlayerStoryData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                    PlayerStoryData d = new PlayerStoryData(); d.readTag(tag); return d;
                }
                @Override public CompoundTag write(PlayerStoryData d, HolderLookup.Provider provider) { return d.writeTag(); }
            })
            .copyOnDeath()
            .build());

    private static final String NBT_KEY = "QuantumFalconStory";
    private static final String NBT_CHAPTERS = "SeenChapters";
    private static final String NBT_ENDING = "ChosenEnding";
    private static final String NBT_BONDS = "FalconBonds";

    private boolean migrated = false;
    private final Set<String> defeatedBosses = new HashSet<>();   // BossDeathHandler.BossType names (gating)
    private boolean sovereignDefeated = false;
    private final Set<StoryChapter> seenChapters = new HashSet<>();
    private String chosenEnding = null;
    private int voidFalconBond = 0;
    private int quantumFalconBond = 0;
    private int echoFalconBond = 0;
    private int singularityFalconBond = 0;

    // ========================================
    // STATIC ACCESS
    // ========================================

    public static PlayerStoryData get(Player player) {
        PlayerStoryData data = player.getData(STORY.get());
        if (!data.migrated) {   // one-time import of the old persistent-data copy
            data.migrated = true;
            CompoundTag old = player.getPersistentData();
            if (old.contains(NBT_KEY)) { data.readTag(old.getCompound(NBT_KEY)); data.migrated = true; old.remove(NBT_KEY); }
        }
        return data;
    }

    /** Kept for old callers; the attachment is saved with the player automatically. */
    public static void removePlayer(UUID uuid) { }

    public boolean isSovereignDefeated() { return sovereignDefeated; }
    public void markBossDefeated(String bossType) { defeatedBosses.add(bossType); }
    public boolean hasDefeatedBoss(String bossType) { return defeatedBosses.contains(bossType); }
    public void setSovereignDefeated(boolean v) { sovereignDefeated = v; }

    /** Bond type a falcon variant counts toward for the True ending ("max bond with all falcon types"). */
    public static String bondType(String variant) {
        if (variant == null) return "quantum";
        return switch (variant.toLowerCase()) {
            case "voidstalker", "abyssal", "warped", "sporewraith", "rift_reaver", "soulfire", "crimson" -> "void";
            case "aetherwing", "chronowing", "tempest", "stormrider", "tidecaller", "coral", "tundra", "aurora_seraph" -> "echo";
            case "singularity", "cosmic", "nebula_prism", "paradox", "quantum_phoenix" -> "singularity";
            default -> "quantum";
        };
    }

    /** Story bond for a type = the best bond level reached with any falcon of that type. */
    public void raiseFalconBond(String falconType, int level) {
        int v = Math.min(100, Math.max(0, level));
        switch (falconType.toLowerCase()) {
            case "void" -> voidFalconBond = Math.max(voidFalconBond, v);
            case "quantum" -> quantumFalconBond = Math.max(quantumFalconBond, v);
            case "echo" -> echoFalconBond = Math.max(echoFalconBond, v);
            case "singularity" -> singularityFalconBond = Math.max(singularityFalconBond, v);
        }
    }

    // ========================================
    // CHAPTER TRACKING
    // ========================================

    public boolean hasSeenChapter(StoryChapter chapter) {
        return this.seenChapters.contains(chapter);
    }

    public void markChapterSeen(StoryChapter chapter) {
        this.seenChapters.add(chapter);
    }
    
    public int getSeenChapterCount() {
        return this.seenChapters.size();
    }
    
    public Set<StoryChapter> getSeenChapters() {
        return new HashSet<>(this.seenChapters);
    }

    // ========================================
    // FALCON BONDS
    // ========================================

    public void addFalconBond(String falconType, int amount) {
        switch (falconType.toLowerCase()) {
            case "void" -> voidFalconBond = Math.min(100, voidFalconBond + amount);
            case "quantum" -> quantumFalconBond = Math.min(100, quantumFalconBond + amount);
            case "echo" -> echoFalconBond = Math.min(100, echoFalconBond + amount);
            case "singularity" -> singularityFalconBond = Math.min(100, singularityFalconBond + amount);
        }
    }
    
    public int getFalconBond(String falconType) {
        return switch (falconType.toLowerCase()) {
            case "void" -> voidFalconBond;
            case "quantum" -> quantumFalconBond;
            case "echo" -> echoFalconBond;
            case "singularity" -> singularityFalconBond;
            default -> 0;
        };
    }
    
    public int getVoidFalconBond() {
        return voidFalconBond;
    }
    
    public int getQuantumFalconBond() {
        return quantumFalconBond;
    }
    
    public int getEchoFalconBond() {
        return echoFalconBond;
    }
    
    public int getSingularityFalconBond() {
        return singularityFalconBond;
    }

    /**
     * Check if player has max bond (100) with all falcon types.
     * Required for the True Ending.
     */
    /** How many of the four falcon types (Void, Quantum, Echo, Singularity) are at max bond. */
    public int maxBondTypeCount() {
        return (voidFalconBond >= 100 ? 1 : 0) + (quantumFalconBond >= 100 ? 1 : 0) + (echoFalconBond >= 100 ? 1 : 0) + (singularityFalconBond >= 100 ? 1 : 0);
    }

    public boolean hasMaxBondAllTypes() {
        return voidFalconBond >= 100 && 
               quantumFalconBond >= 100 && 
               echoFalconBond >= 100 && 
               singularityFalconBond >= 100;
    }

    // ========================================
    // ENDING
    // ========================================

    public void setChosenEnding(String ending) {
        this.chosenEnding = ending;
    }

    public String getChosenEnding() {
        return this.chosenEnding;
    }
    
    public boolean hasChosenEnding() {
        return this.chosenEnding != null;
    }

    // ========================================
    // SAVE/LOAD
    // ========================================

    public void readTag(CompoundTag storyData) {
        migrated = true;
        seenChapters.clear();
        if (storyData.contains(NBT_CHAPTERS)) {
            ListTag chapterList = storyData.getList(NBT_CHAPTERS, Tag.TAG_STRING);
            for (int i = 0; i < chapterList.size(); i++) {
                try { seenChapters.add(StoryChapter.valueOf(chapterList.getString(i))); }
                catch (IllegalArgumentException e) { /* unknown chapter from another version */ }
            }
        }
        chosenEnding = storyData.contains(NBT_ENDING) ? storyData.getString(NBT_ENDING) : null;
        sovereignDefeated = storyData.getBoolean("SovereignDefeated");
        defeatedBosses.clear();
        ListTag bl = storyData.getList("DefeatedBosses", Tag.TAG_STRING);
        for (int i = 0; i < bl.size(); i++) defeatedBosses.add(bl.getString(i));
        if (storyData.contains(NBT_BONDS)) {
            CompoundTag bonds = storyData.getCompound(NBT_BONDS);
            voidFalconBond = bonds.getInt("void");
            quantumFalconBond = bonds.getInt("quantum");
            echoFalconBond = bonds.getInt("echo");
            singularityFalconBond = bonds.getInt("singularity");
        }
    }

    public CompoundTag writeTag() {
        CompoundTag storyData = new CompoundTag();
        ListTag chapterList = new ListTag();
        for (StoryChapter chapter : seenChapters) chapterList.add(StringTag.valueOf(chapter.name()));
        storyData.put(NBT_CHAPTERS, chapterList);
        if (chosenEnding != null) storyData.putString(NBT_ENDING, chosenEnding);
        storyData.putBoolean("SovereignDefeated", sovereignDefeated);
        ListTag bl = new ListTag();
        for (String b : defeatedBosses) bl.add(StringTag.valueOf(b));
        storyData.put("DefeatedBosses", bl);
        CompoundTag bonds = new CompoundTag();
        bonds.putInt("void", voidFalconBond);
        bonds.putInt("quantum", quantumFalconBond);
        bonds.putInt("echo", echoFalconBond);
        bonds.putInt("singularity", singularityFalconBond);
        storyData.put(NBT_BONDS, bonds);
        return storyData;
    }

    /** Kept for old callers; the attachment is saved with the player automatically. */
    public void save(Player player) { }
}
