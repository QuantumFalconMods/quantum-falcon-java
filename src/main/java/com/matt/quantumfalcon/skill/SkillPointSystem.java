package com.matt.quantumfalcon.skill;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.capability.FalconBondCapability;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity;
import com.matt.quantumfalcon.entity.boss.RiftWardenEntity;
import com.matt.quantumfalcon.entity.boss.EntangledTwinsEntity;
import com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity;
import com.matt.quantumfalcon.registry.ModSounds;
import com.matt.quantumfalcon.network.SkillPointSyncPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Skill Point Acquisition System for Quantum Falcon.
 * Handles how players earn skill points through various activities.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class SkillPointSystem {

    private static final Logger LOGGER = LogManager.getLogger();

    // ============ Economy (Stage E, B5) ============
    // Every source is finite and once per player, so the supply is a fixed number: 24 points against a tree that costs
    // {@link SkillTreeSystem#totalCost()} (36), about two thirds. Points left over once a player has everything they want go
    // into Mastery Marks (cosmetic sink, {@link PlayerSkillData#buyMasteryMark}). Removed: the 3-point login gift, the
    // 500-XP drip, the 5% kill roll, advancement points (most mapped to advancements that don't exist), boss re-kills
    // and per-falcon max-bond points.
    public static final int FIRST_FALCON_TAME_POINTS = 2;
    /** First credited kill of each boss, paid to every credited player (W6/): Overseer, Warden, Twins, Sovereign. */
    public static final int[] BOSS_FIRST_KILL_POINTS = {2, 2, 2, 3};
    /** First hatch of each falcon tier 1-7 (also what gates the Lineage branch). */
    public static final int HATCH_TIER_POINTS = 1;
    public static final int HATCH_TIERS = 7;
    public static final int MAIN_QUEST_COMPLETE_POINTS = 1;
    public static final int MAIN_QUESTS = 5;
    public static final int QUEST_COMPLETE_POINTS = 0;
    /** First falcon raised to max bond (once per player, not per falcon: NS2). */
    public static final int MAX_BOND_POINTS = 1;

    /** The whole finite supply; the economy qftest checks it against the tree cost. */
    public static int finiteSupply() {
        int boss = 0; for (int b : BOSS_FIRST_KILL_POINTS) boss += b;
        return FIRST_FALCON_TAME_POINTS + boss + HATCH_TIER_POINTS * HATCH_TIERS + MAIN_QUEST_COMPLETE_POINTS * MAIN_QUESTS + MAX_BOND_POINTS;
    }

    /** First credited kill of a boss (index 0-3 = Overseer, Warden, Twins, Sovereign); BossDeathHandler calls it once per credited player. */
    public static void awardBossFirstKill(ServerPlayer player, int bossIndex, String bossName) {
        if (bossIndex < 0 || bossIndex >= BOSS_FIRST_KILL_POINTS.length) return;
        awardSkillPoint(player, BOSS_FIRST_KILL_POINTS[bossIndex], bossName + " defeated (first time)");
    }

    /**
     * Award skill points on first login (tutorial points)
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        syncSkillPoints(player);
    }

    /** The client copy of points and skills is refreshed whenever the player entity is replaced. */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncSkillPoints(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncSkillPoints(player);
    }

    // ============ Public API ============

    /**
     * Award skill points to a player
     * @param player The player to award points to
     * @param amount Number of points to award
     * @param reason Reason for the award (shown in chat)
     */
    public static void awardSkillPoint(ServerPlayer player, int amount, String reason) {
        if (amount <= 0) return;
        
        PlayerSkillData skillData = PlayerSkillData.get(player);
        if (skillData == null) {
            LOGGER.warn("Could not get skill data for player {}", player.getName().getString());
            return;
        }
        
        skillData.addSkillPoints(amount);
        
        // Play sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);   // W3: the unlock sound is for unlocks
        
        // Send chat message
        player.displayClientMessage(
            Component.literal("§d✦ §b+" + amount + " Skill Point" + (amount > 1 ? "s" : "") + " §7(" + reason + ")§r  §e[")
                .append(Component.keybind("key.quantumfalcon.skill_tree").withStyle(net.minecraft.ChatFormatting.YELLOW))
                .append(Component.literal("§e] to spend")),   // say how to spend it
            false
        );
        
        // Sync to client
        syncSkillPoints(player);
        
        LOGGER.debug("Awarded {} skill point(s) to {} for: {}", amount, player.getName().getString(), reason);
    }

    /**
     * Breeding milestone (B6): the first hatch of each falcon tier pays {@link #HATCH_TIER_POINTS} once per player, so egg
     * farms can't print points. The mask is also what gates the Lineage branch ({@link #highestTierHatched}).
     */
    public static void awardHatchMilestone(ServerPlayer player, int tier) {
        net.minecraft.nbt.CompoundTag root = player.getPersistentData();
        net.minecraft.nbt.CompoundTag keep = root.getCompound(Player.PERSISTED_NBT_TAG);
        int mask = keep.getInt("qf_hatched_tiers");
        int bit = 1 << Math.max(0, Math.min(7, tier));
        if ((mask & bit) != 0) return;
        keep.putInt("qf_hatched_tiers", mask | bit);
        root.put(Player.PERSISTED_NBT_TAG, keep);
        awardSkillPoint(player, HATCH_TIER_POINTS, "First tier " + tier + " hatch");
        syncSkillPoints(player);
    }

    /** Highest falcon tier this player has hatched (0 = none); gates the Lineage branch. */
    public static int highestTierHatched(Player player) {
        int mask = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getInt("qf_hatched_tiers");
        for (int t = 7; t >= 1; t--) if ((mask & (1 << t)) != 0) return t;
        return 0;
    }

    /**
     * Award skill points for taming first falcon
     */
    public static void awardFirstTamePoints(ServerPlayer player) {
        PlayerSkillData skillData = PlayerSkillData.get(player);
        if (skillData != null && !skillData.hasTamedFirstFalcon()) {
            awardSkillPoint(player, FIRST_FALCON_TAME_POINTS, "First Falcon Tamed!");
            skillData.setTamedFirstFalcon(true);
        }
    }

    /**
     * Award skill points for reaching max bond with a falcon
     */
    public static void awardMaxBondPoints(ServerPlayer player, QuantumFalconEntity falcon) {
        net.minecraft.nbt.CompoundTag root = player.getPersistentData();
        net.minecraft.nbt.CompoundTag keep = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (keep.getBoolean("qf_max_bond_paid")) return;   // NS2: once per player
        keep.putBoolean("qf_max_bond_paid", true);
        root.put(Player.PERSISTED_NBT_TAG, keep);
        awardSkillPoint(player, MAX_BOND_POINTS, "First max bond (" + falcon.getName().getString() + ")");
    }

    /**
     * Award skill points for quest completion
     * @param player The player who completed the quest
     * @param isMainQuest Whether this is a main story quest
     */
    public static void awardQuestCompletePoints(ServerPlayer player, boolean isMainQuest) {
        // quests complete once each; only the main chain pays (side quests pay items/XP)
        awardSkillPoint(player, isMainQuest ? MAIN_QUEST_COMPLETE_POINTS : QUEST_COMPLETE_POINTS, "Main quest completed");
    }

    /**
     * Get available skill points for a player
     */
    public static int getAvailablePoints(Player player) {
        PlayerSkillData skillData = PlayerSkillData.get(player);
        return skillData != null ? skillData.getAvailableSkillPoints() : 0;
    }

    /**
     * Spend skill points to unlock a skill
     * @return true if successful, false if not enough points
     */
    public static boolean spendPoints(Player player, int amount) {
        PlayerSkillData skillData = PlayerSkillData.get(player);
        if (skillData == null || skillData.getAvailableSkillPoints() < amount) {
            return false;
        }
        
        skillData.spendSkillPoints(amount);
        
        if (player instanceof ServerPlayer serverPlayer) {
            syncSkillPoints(serverPlayer);
        }
        
        return true;
    }

    /**
     * Reset all skill points for a player (refunds all spent points)
     */
    public static void resetSkillPoints(ServerPlayer player) {
        PlayerSkillData skillData = PlayerSkillData.get(player);
        if (skillData != null) {
            int totalSpent = skillData.getTotalSpentPoints();
            skillData.resetSkills();
            skillData.addSkillPoints(totalSpent);
            
            player.displayClientMessage(
                Component.literal("§d✦ §bSkill points reset! §7(" + totalSpent + " points refunded)§r"),
                false
            );
            
            syncSkillPoints(player);
        }
    }

    /**
     * Sync skill points to client
     */
    private static void syncSkillPoints(ServerPlayer player) {
        // B1 (skill tree audit): this used to be a no-op, so the client always saw 0 points and could never spend
        com.matt.quantumfalcon.network.ModNetworking.syncSkillPoints(player);
    }

    // ============ Admin Commands ============

    /**
     * Give skill points to a player (admin command)
     */
    public static void givePoints(ServerPlayer player, int amount) {
        PlayerSkillData skillData = PlayerSkillData.get(player);
        if (skillData != null) {
            skillData.addSkillPoints(amount);
            player.displayClientMessage(
                Component.literal("§a[Admin] §bGiven " + amount + " skill points§r"),
                false
            );
            syncSkillPoints(player);
        }
    }

    /**
     * Set skill points for a player (admin command)
     */
    public static void setPoints(ServerPlayer player, int amount) {
        PlayerSkillData skillData = PlayerSkillData.get(player);
        if (skillData != null) {
            skillData.setAvailableSkillPoints(amount);
            player.displayClientMessage(
                Component.literal("§a[Admin] §bSkill points set to " + amount + "§r"),
                false
            );
            syncSkillPoints(player);
        }
    }
}
