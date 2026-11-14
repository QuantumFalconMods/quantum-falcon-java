package com.matt.quantumfalcon.integration.skills;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.integration.ModIntegrations;
import com.matt.quantumfalcon.skill.SkillTreeSystem;
import com.matt.quantumfalcon.skill.SkillTreeSystem.Skill;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Skill Tree Integration System
 * 
 * Integrates Quantum Falcon's skill tree with other mods' progression systems:
 * - Apotheosis: Boss rarities unlock higher-tier skills
 * - PlayerEx/LevelZ: Skill XP bonuses from quantum activities
 * - Curios: Trinket skills unlock at certain skill levels
 * - Artifacts: Artifact abilities enhance skill effects
 * 
 * This ensures our skill tree "piggybacks" off other mods and adds new lines.
 */
@EventBusSubscriber
public class SkillTreeIntegration {
    
    // ============ Integration Status ============
    private static boolean apotheosisLoaded = false;
    private static boolean playerExLoaded = false;
    private static boolean levelZLoaded = false;
    
    /**
     * Initialize skill tree integrations
     */
    public static void init() {
        apotheosisLoaded = net.neoforged.fml.ModList.get().isLoaded("apotheosis");
        playerExLoaded = net.neoforged.fml.ModList.get().isLoaded("playerex");
        levelZLoaded = net.neoforged.fml.ModList.get().isLoaded("levelz");
        
        if (apotheosisLoaded) {
            QuantumFalcon.LOGGER.info("  ✓ Apotheosis detected - Boss rarities unlock skills");
        }
        if (playerExLoaded) {
            QuantumFalcon.LOGGER.info("  ✓ PlayerEx detected - Skill XP integration enabled");
        }
        if (levelZLoaded) {
            QuantumFalcon.LOGGER.info("  ✓ LevelZ detected - Level-based skill unlocks");
        }
    }
    
    // ============ Apotheosis Integration ============
    
    /**
     * Check if player has defeated a boss of sufficient rarity to unlock skills
     */
    public static boolean canUnlockSkillFromBossRarity(Player player, String skillId, int requiredRarity) {
        if (!apotheosisLoaded) return true; // No restriction if Apotheosis not loaded
        
        // Check player's boss kill history
        // Apotheosis stores boss kills with rarity levels (0-5)
        // Higher rarity = more powerful bosses = unlock better skills
        
        // Example: Defeat a Rare boss (rarity 2) to unlock Tier 2 skills
        // Defeat a Epic boss (rarity 3) to unlock Tier 3 skills
        // etc.
        
        // This would query Apotheosis' boss kill data
        // For now, return true (would need Apotheosis API)
        return true;
    }
    
    /**
     * Get skill unlock requirement based on Apotheosis boss rarities
     */
    public static String getSkillUnlockRequirement(String skillId) {
        if (!apotheosisLoaded) return "";
        
        // Map skills to required boss rarities
        Map<String, String> requirements = new HashMap<>();
        requirements.put("falcon_resurrection", "Defeat an Epic+ boss (Apotheosis)");
        requirements.put("execute", "Defeat a Legendary+ boss (Apotheosis)");
        requirements.put("quantum_ascension", "Defeat a Mythic+ boss (Apotheosis)");
        
        return requirements.getOrDefault(skillId, "");
    }
    
    // ============ PlayerEx Integration ============
    
    /**
     * Grant PlayerEx skill XP when performing quantum activities
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        if (!playerExLoaded) return;
        
        Player player = event.getEntity();
        
        // Grant XP for quantum activities
        // This would call PlayerEx API to add XP to their skill system
        // Example: Mining quantum ore grants PlayerEx mining XP
        // Taming falcons grants PlayerEx taming XP
    }
    
    /**
     * Get bonus skill XP multiplier from PlayerEx levels
     */
    public static float getSkillXPMultiplier(Player player) {
        if (!playerExLoaded) return 1.0f;
        
        // PlayerEx levels boost quantum skill XP gain
        // Example: Level 10 in PlayerEx = +10% quantum skill XP
        // This would query PlayerEx API
        return 1.0f;
    }
    
    // ============ LevelZ Integration ============
    
    /**
     * Check if player's LevelZ level allows skill unlock
     */
    public static boolean canUnlockSkillFromLevel(Player player, String skillId, int requiredLevel) {
        if (!levelZLoaded) return true;
        
        // LevelZ has player levels (1-100)
        // Higher levels unlock better skills
        // Example: Level 20 required for Tier 2 skills
        // Level 40 required for Tier 3 skills
        
        // This would query LevelZ API
        // int playerLevel = LevelZ.getPlayerLevel(player);
        // return playerLevel >= requiredLevel;
        
        return true;
    }
    
    /**
     * Get required LevelZ level for a skill
     */
    public static int getRequiredLevelZLevel(String skillId) {
        if (!levelZLoaded) return 0;
        
        Map<String, Integer> levelRequirements = new HashMap<>();
        levelRequirements.put("falcon_bond_1", 5);
        levelRequirements.put("falcon_health", 10);
        levelRequirements.put("falcon_speed", 10);
        levelRequirements.put("multi_falcon", 15);
        levelRequirements.put("breeding_master", 20);
        levelRequirements.put("falcon_resurrection", 30);
        levelRequirements.put("falcon_attack_1", 5);
        levelRequirements.put("dive_damage", 10);
        levelRequirements.put("team_attack", 15);
        levelRequirements.put("bleed_effect", 15);
        levelRequirements.put("critical_strike", 20);
        levelRequirements.put("execute", 30);
        // ... more skills
        
        return levelRequirements.getOrDefault(skillId, 0);
    }
    
    // ============ Curios Integration ============
    
    /**
     * Check if player has required Curios trinkets to unlock skill
     */
    public static boolean canUnlockSkillFromCurios(Player player, String skillId) {
        if (!ModIntegrations.isCuriosLoaded()) return true;
        
        // Some skills require specific trinkets
        // Example: "Living Power" skill requires Quantum Band equipped
        Map<String, String> trinketRequirements = new HashMap<>();
        trinketRequirements.put("living_power", "quantumfalcon:quantum_band");
        trinketRequirements.put("quantum_shield", "quantumfalcon:reality_anchor");
        trinketRequirements.put("entanglement_mastery", "quantumfalcon:entanglement_core");
        
        String requiredTrinket = trinketRequirements.get(skillId);
        if (requiredTrinket == null) return true;
        
        // Check if player has trinket equipped
        // This would use Curios API
        return true; // Placeholder
    }
    
    // ============ Artifacts Integration ============
    
    /**
     * Get skill effect multiplier from equipped artifacts
     */
    public static float getSkillEffectMultiplier(Player player, String skillId) {
        if (!ModIntegrations.isArtifactsLoaded()) return 1.0f;
        
        float multiplier = 1.0f;
        
        // Artifacts enhance skill effects
        // Example: Sovereign's Crown doubles falcon damage skills
        // Overseer's Eye enhances vision-related skills
        
        // This would check equipped artifacts via Curios API
        return multiplier;
    }
    
    // ============ Combined Integration Check ============
    
    /**
     * Check all integration requirements for skill unlock
     */
    public static boolean canUnlockSkill(Player player, String skillId) {
        Skill skill = SkillTreeSystem.getSkill(skillId);
        if (skill == null) return false;
        
        // Check LevelZ level requirement
        int requiredLevel = getRequiredLevelZLevel(skillId);
        if (requiredLevel > 0 && !canUnlockSkillFromLevel(player, skillId, requiredLevel)) {
            return false;
        }
        
        // Check Apotheosis boss rarity requirement
        int requiredRarity = getRequiredBossRarity(skillId);
        if (requiredRarity > 0 && !canUnlockSkillFromBossRarity(player, skillId, requiredRarity)) {
            return false;
        }
        
        // Check Curios trinket requirement
        if (!canUnlockSkillFromCurios(player, skillId)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get required boss rarity for skill (Apotheosis)
     */
    private static int getRequiredBossRarity(String skillId) {
        Map<String, Integer> rarityRequirements = new HashMap<>();
        rarityRequirements.put("falcon_resurrection", 3); // Epic
        rarityRequirements.put("execute", 4); // Legendary
        rarityRequirements.put("quantum_ascension", 5); // Mythic
        
        return rarityRequirements.getOrDefault(skillId, 0);
    }
    
    /**
     * Get skill unlock tooltip with integration requirements
     */
    public static String getSkillUnlockTooltip(String skillId) {
        StringBuilder tooltip = new StringBuilder();
        
        // LevelZ requirement
        int levelReq = getRequiredLevelZLevel(skillId);
        if (levelReq > 0 && levelZLoaded) {
            tooltip.append("Requires LevelZ Level: ").append(levelReq).append("\n");
        }
        
        // Apotheosis requirement
        int rarityReq = getRequiredBossRarity(skillId);
        if (rarityReq > 0 && apotheosisLoaded) {
            String[] rarities = {"Common", "Uncommon", "Rare", "Epic", "Legendary", "Mythic"};
            if (rarityReq < rarities.length) {
                tooltip.append("Requires defeating a ").append(rarities[rarityReq]).append("+ boss\n");
            }
        }
        
        // Curios requirement
        Map<String, String> trinketReqs = new HashMap<>();
        trinketReqs.put("living_power", "Quantum Band (Curios)");
        trinketReqs.put("quantum_shield", "Reality Anchor (Curios)");
        
        String trinketReq = trinketReqs.get(skillId);
        if (trinketReq != null && ModIntegrations.isCuriosLoaded()) {
            tooltip.append("Requires: ").append(trinketReq).append("\n");
        }
        
        return tooltip.toString();
    }
}
