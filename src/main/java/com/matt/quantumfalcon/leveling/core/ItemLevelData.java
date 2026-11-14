package com.matt.quantumfalcon.leveling.core;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.rarity.Rarity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Complete data storage for a levelable item.
 * Handles serialization to/from NBT for persistence with full validation.
 */
public class ItemLevelData {

    // NBT Keys
    public static final String TAG_ROOT = "QuantumFalconLeveling";
    public static final String TAG_VERSION = "DataVersion";
    public static final String TAG_LEVEL = "Level";
    public static final String TAG_XP = "Xp";
    public static final String TAG_TOTAL_XP = "TotalXp";
    public static final String TAG_RARITY = "Rarity";
    public static final String TAG_TRAITS = "Traits";
    public static final String TAG_UNLOCKED_TRAITS = "UnlockedTraits";
    public static final String TAG_STATS = "BonusStats";
    public static final String TAG_KILLS = "Kills";
    public static final String TAG_DAMAGE_DEALT = "DamageDealt";
    public static final String TAG_DAMAGE_TAKEN = "DamageTaken";
    public static final String TAG_BLOCKS_MINED = "BlocksMined";
    public static final String TAG_INITIALIZED = "Initialized";
    public static final String TAG_SOULBOUND = "Soulbound";
    public static final String TAG_ITEM_UUID = "ItemUUID";

    private static final int CURRENT_DATA_VERSION = 1;
    private static final int MAX_VALID_LEVEL = 1000;
    private static final long MAX_VALID_XP = 1_000_000_000L;

    // Core leveling data
    private int level = 1;
    private int xp = 0;
    private long totalXp = 0;
    private Rarity rarity = Rarity.COMMON;

    // Traits
    private final Set<ResourceLocation> activeTraits = new HashSet<>();
    private final Set<ResourceLocation> unlockedTraits = new HashSet<>();

    // Bonus stats from leveling and rarity
    private final Map<String, Float> bonusStats = new HashMap<>();

    // Combat/usage statistics
    private int kills = 0;
    private float damageDealt = 0;
    private float damageTaken = 0;
    private int blocksMined = 0;

    // Metadata
    private boolean initialized = false;
    private boolean soulbound = false;
    private UUID itemUUID;

    /**
     * Create new empty level data
     */
    public ItemLevelData() {
        this.itemUUID = UUID.randomUUID();
    }

    /**
     * Load level data from NBT with validation
     */
    public static ItemLevelData load(CompoundTag tag) {
        ItemLevelData data = new ItemLevelData();

        if (tag == null || tag.isEmpty()) {
            return data;
        }

        boolean hadInvalid = false;

        // Version for future migrations
        int version = tag.getInt(TAG_VERSION);

        // Core data with validation
        int loadedLevel = tag.getInt(TAG_LEVEL);
        if (loadedLevel < 1 || loadedLevel > MAX_VALID_LEVEL) {
            QuantumFalcon.LOGGER.warn("Invalid level {} in ItemLevelData, clamping to valid range", loadedLevel);
            loadedLevel = Math.max(1, Math.min(MAX_VALID_LEVEL, loadedLevel));
            hadInvalid = true;
        }
        data.level = loadedLevel;

        int loadedXp = tag.getInt(TAG_XP);
        if (loadedXp < 0) {
            QuantumFalcon.LOGGER.warn("Negative XP {} in ItemLevelData, setting to 0", loadedXp);
            loadedXp = 0;
            hadInvalid = true;
        }
        data.xp = loadedXp;

        long loadedTotalXp = tag.getLong(TAG_TOTAL_XP);
        if (loadedTotalXp < 0 || loadedTotalXp > MAX_VALID_XP) {
            loadedTotalXp = Math.max(0, Math.min(MAX_VALID_XP, loadedTotalXp));
            hadInvalid = true;
        }
        data.totalXp = loadedTotalXp;

        // Rarity with validation
        String rarityName = tag.getString(TAG_RARITY);
        data.rarity = Rarity.byName(rarityName);

        // Active traits with validation
        data.activeTraits.clear();
        if (tag.contains(TAG_TRAITS, Tag.TAG_LIST)) {
            ListTag traitList = tag.getList(TAG_TRAITS, Tag.TAG_STRING);
            for (int i = 0; i < traitList.size(); i++) {
                String traitStr = traitList.getString(i);
                try {
                    data.activeTraits.add(ResourceLocation.parse(traitStr));
                } catch (Exception e) {
                    QuantumFalcon.LOGGER.warn("Invalid trait ID ignored: {}", traitStr);
                    hadInvalid = true;
                }
            }
        }

        // Unlocked traits with validation
        data.unlockedTraits.clear();
        if (tag.contains(TAG_UNLOCKED_TRAITS, Tag.TAG_LIST)) {
            ListTag unlockedList = tag.getList(TAG_UNLOCKED_TRAITS, Tag.TAG_STRING);
            for (int i = 0; i < unlockedList.size(); i++) {
                String traitStr = unlockedList.getString(i);
                try {
                    data.unlockedTraits.add(ResourceLocation.parse(traitStr));
                } catch (Exception e) {
                    hadInvalid = true;
                }
            }
        }

        // Bonus stats
        data.bonusStats.clear();
        if (tag.contains(TAG_STATS, Tag.TAG_COMPOUND)) {
            CompoundTag statsTag = tag.getCompound(TAG_STATS);
            for (String key : statsTag.getAllKeys()) {
                float value = statsTag.getFloat(key);
                // Validate stat values
                if (!Float.isNaN(value) && !Float.isInfinite(value)) {
                    data.bonusStats.put(key, value);
                }
            }
        }

        // Combat stats with validation (clamp negatives)
        data.kills = Math.max(0, tag.getInt(TAG_KILLS));
        data.damageDealt = Math.max(0, tag.getFloat(TAG_DAMAGE_DEALT));
        data.damageTaken = Math.max(0, tag.getFloat(TAG_DAMAGE_TAKEN));
        data.blocksMined = Math.max(0, tag.getInt(TAG_BLOCKS_MINED));

        // Metadata
        data.initialized = tag.getBoolean(TAG_INITIALIZED);
        data.soulbound = tag.getBoolean(TAG_SOULBOUND);
        if (tag.hasUUID(TAG_ITEM_UUID)) {
            data.itemUUID = tag.getUUID(TAG_ITEM_UUID);
        }

        if (hadInvalid) {
            QuantumFalcon.LOGGER.info("ItemLevelData validated and corrected during load");
        }

        return data;
    }

    /**
     * Save level data to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putInt(TAG_VERSION, CURRENT_DATA_VERSION);

        // Core data
        tag.putInt(TAG_LEVEL, level);
        tag.putInt(TAG_XP, xp);
        tag.putLong(TAG_TOTAL_XP, totalXp);
        tag.putString(TAG_RARITY, rarity.getName());

        // Active traits
        ListTag traitList = new ListTag();
        for (ResourceLocation trait : activeTraits) {
            traitList.add(StringTag.valueOf(trait.toString()));
        }
        tag.put(TAG_TRAITS, traitList);

        // Unlocked traits
        ListTag unlockedList = new ListTag();
        for (ResourceLocation trait : unlockedTraits) {
            unlockedList.add(StringTag.valueOf(trait.toString()));
        }
        tag.put(TAG_UNLOCKED_TRAITS, unlockedList);

        // Bonus stats
        CompoundTag statsTag = new CompoundTag();
        for (Map.Entry<String, Float> entry : bonusStats.entrySet()) {
            statsTag.putFloat(entry.getKey(), entry.getValue());
        }
        tag.put(TAG_STATS, statsTag);

        // Combat stats
        tag.putInt(TAG_KILLS, kills);
        tag.putFloat(TAG_DAMAGE_DEALT, damageDealt);
        tag.putFloat(TAG_DAMAGE_TAKEN, damageTaken);
        tag.putInt(TAG_BLOCKS_MINED, blocksMined);

        // Metadata
        tag.putBoolean(TAG_INITIALIZED, initialized);
        tag.putBoolean(TAG_SOULBOUND, soulbound);
        if (itemUUID != null) {
            tag.putUUID(TAG_ITEM_UUID, itemUUID);
        }

        return tag;
    }

    // ==================== GETTERS ====================

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public long getTotalXp() {
        return totalXp;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public Set<ResourceLocation> getActiveTraits() {
        return Collections.unmodifiableSet(activeTraits);
    }

    public Set<ResourceLocation> getUnlockedTraits() {
        return Collections.unmodifiableSet(unlockedTraits);
    }

    public Map<String, Float> getBonusStats() {
        return Collections.unmodifiableMap(bonusStats);
    }

    public int getKills() {
        return kills;
    }

    public float getDamageDealt() {
        return damageDealt;
    }

    public float getDamageTaken() {
        return damageTaken;
    }

    public int getBlocksMined() {
        return blocksMined;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isSoulbound() {
        return soulbound;
    }

    public UUID getItemUUID() {
        return itemUUID;
    }

    // ==================== SETTERS ====================

    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(MAX_VALID_LEVEL, level));
    }

    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
    }

    public void setRarity(Rarity rarity) {
        this.rarity = rarity != null ? rarity : Rarity.COMMON;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public void setSoulbound(boolean soulbound) {
        this.soulbound = soulbound;
    }

    public void addXp(int amount) {
        if (amount > 0) {
            this.xp += amount;
            this.totalXp = Math.min(MAX_VALID_XP, this.totalXp + amount);
        }
    }

    public void addKill() {
        this.kills++;
    }

    public void addDamageDealt(float amount) {
        if (amount > 0) {
            this.damageDealt += amount;
        }
    }

    public void addDamageTaken(float amount) {
        if (amount > 0) {
            this.damageTaken += amount;
        }
    }

    public void addBlockMined() {
        this.blocksMined++;
    }

    // ==================== TRAIT MANAGEMENT ====================

    public void addTrait(ResourceLocation trait) {
        if (trait != null) {
            this.activeTraits.add(trait);
        }
    }

    public void removeTrait(ResourceLocation trait) {
        if (trait != null) {
            this.activeTraits.remove(trait);
        }
    }

    public void unlockTrait(ResourceLocation trait) {
        if (trait != null) {
            this.unlockedTraits.add(trait);
            this.activeTraits.add(trait);
        }
    }

    public boolean hasTrait(ResourceLocation trait) {
        return trait != null && activeTraits.contains(trait);
    }

    public boolean hasUnlockedTrait(ResourceLocation trait) {
        return trait != null && unlockedTraits.contains(trait);
    }

    // ==================== STAT MANAGEMENT ====================

    public void setBonusStat(String stat, float value) {
        if (stat != null && !stat.isEmpty() && !Float.isNaN(value) && !Float.isInfinite(value)) {
            this.bonusStats.put(stat, value);
        }
    }

    public float getBonusStat(String stat) {
        return bonusStats.getOrDefault(stat, 0f);
    }

    public void addBonusStat(String stat, float value) {
        float current = getBonusStat(stat);
        setBonusStat(stat, current + value);
    }

    public void removeBonusStat(String stat) {
        bonusStats.remove(stat);
    }

    // ==================== UTILITY ====================

    /**
     * Copy all data to another ItemLevelData instance
     */
    public void copyTo(ItemLevelData other) {
        other.level = this.level;
        other.xp = this.xp;
        other.totalXp = this.totalXp;
        other.rarity = this.rarity;

        other.activeTraits.clear();
        other.activeTraits.addAll(this.activeTraits);

        other.unlockedTraits.clear();
        other.unlockedTraits.addAll(this.unlockedTraits);

        other.bonusStats.clear();
        other.bonusStats.putAll(this.bonusStats);

        other.kills = this.kills;
        other.damageDealt = this.damageDealt;
        other.damageTaken = this.damageTaken;
        other.blocksMined = this.blocksMined;

        other.initialized = this.initialized;
        other.soulbound = this.soulbound;
        // Note: UUID is NOT copied to maintain uniqueness
    }

    /**
     * Reset all progress (keep rarity)
     */
    public void resetProgress() {
        this.level = 1;
        this.xp = 0;
        this.totalXp = 0;
        this.kills = 0;
        this.damageDealt = 0;
        this.damageTaken = 0;
        this.blocksMined = 0;

        // Clear unlocked traits (keep rarity traits)
        Set<ResourceLocation> rarityTraits = new HashSet<>(activeTraits);
        rarityTraits.removeAll(unlockedTraits);

        this.activeTraits.clear();
        this.activeTraits.addAll(rarityTraits);
        this.unlockedTraits.clear();

        // Reset level-based stats (keep rarity stats)
        bonusStats.entrySet().removeIf(e -> e.getKey().startsWith("level_"));
    }

    @Override
    public String toString() {
        return String.format("ItemLevelData[level=%d, xp=%d, rarity=%s, traits=%d]",
                level, xp, rarity.getName(), activeTraits.size());
    }
}
