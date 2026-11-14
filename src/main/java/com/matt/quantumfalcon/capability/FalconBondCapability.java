package com.matt.quantumfalcon.capability;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
// Capabilities API changed in NeoForge 1.21.1 - using data components instead
// import net.neoforged.neoforge.capabilities.Capabilities;
// import net.neoforged.neoforge.common.capabilities.Capability;
// import net.neoforged.neoforge.common.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.INBTSerializable;
// import net.neoforged.neoforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Falcon Bond Capability - Tracks bond level (0-10) with each tamed falcon.
 * 
 * Bond progression:
 * - Level 0-3: Basic commands (follow, stay)
 * - Level 4-6: Advanced commands (attack, defend)
 * - Level 7-9: Special abilities unlocked
 * - Level 10: Auto-defend enabled, maximum bond
 * 
 * Bond increases from:
 * - Shared combat XP (when falcon participates in combat)
 * - Feeding falcon treats
 * - Time spent together
 * - Successful breeding
 */
public class FalconBondCapability implements INBTSerializable<CompoundTag> {
    
    // Bond level per falcon UUID
    private final Map<UUID, Integer> bondLevels = new HashMap<>();
    private final Map<UUID, Integer> bondXP = new HashMap<>();
    
    // Constants
    private static final int MAX_BOND_LEVEL = 10;
    private static final int[] XP_PER_LEVEL = {0, 100, 250, 500, 1000, 2000, 3500, 5500, 8000, 12000, 20000};
    
    /**
     * Get bond level for a specific falcon
     */
    public int getBondLevel(UUID falconUUID) {
        return bondLevels.getOrDefault(falconUUID, 0);
    }
    
    /**
     * Get bond XP for a specific falcon
     */
    public int getBondXP(UUID falconUUID) {
        return bondXP.getOrDefault(falconUUID, 0);
    }
    
    /**
     * Add XP to bond (from combat, feeding, etc.)
     */
    public void addBondXP(UUID falconUUID, int xp) {
        int currentLevel = getBondLevel(falconUUID);
        if (currentLevel >= MAX_BOND_LEVEL) return; // Maxed out
        
        int currentXP = getBondXP(falconUUID);
        int newXP = currentXP + xp;
        int requiredXP = XP_PER_LEVEL[currentLevel + 1];
        
        if (newXP >= requiredXP) {
            // Level up!
            bondLevels.put(falconUUID, currentLevel + 1);
            bondXP.put(falconUUID, newXP - requiredXP);
            
            // Notify player of level up
            // This would be called from the entity or event handler
        } else {
            bondXP.put(falconUUID, newXP);
        }
    }
    
    /**
     * Check if auto-defend is unlocked (bond level 10)
     */
    public boolean hasAutoDefend(UUID falconUUID) {
        return getBondLevel(falconUUID) >= 10;
    }
    
    /**
     * Get all bonded falcons
     */
    public Map<UUID, Integer> getAllBonds() {
        return new HashMap<>(bondLevels);
    }
    
    @Override
    public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        CompoundTag bondsTag = new CompoundTag();
        CompoundTag xpTag = new CompoundTag();
        
        for (Map.Entry<UUID, Integer> entry : bondLevels.entrySet()) {
            bondsTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        
        for (Map.Entry<UUID, Integer> entry : bondXP.entrySet()) {
            xpTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        
        tag.put("bonds", bondsTag);
        tag.put("xp", xpTag);
        return tag;
    }
    
    @Override
    public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag tag) {
        bondLevels.clear();
        bondXP.clear();
        
        if (tag.contains("bonds")) {
            CompoundTag bondsTag = tag.getCompound("bonds");
            for (String key : bondsTag.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                bondLevels.put(uuid, bondsTag.getInt(key));
            }
        }
        
        if (tag.contains("xp")) {
            CompoundTag xpTag = tag.getCompound("xp");
            for (String key : xpTag.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                bondXP.put(uuid, xpTag.getInt(key));
            }
        }
    }
    
    /**
     * Get capability instance for a player
     */
    public static FalconBondCapability get(Player player) {
        // This would be attached to player via capability system
        // For now, return a placeholder - actual implementation would use NeoForge capabilities
        return new FalconBondCapability();
    }
}
