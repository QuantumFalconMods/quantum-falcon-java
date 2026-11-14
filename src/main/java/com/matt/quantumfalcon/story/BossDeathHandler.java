package com.matt.quantumfalcon.story;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.dimension.InstabilityManager;
import com.matt.quantumfalcon.entity.boss.*;
import com.matt.quantumfalcon.registry.ModEntities;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.*;

/**
 * Handles boss death events, story progression, and ending triggers.
 * 
 * Boss Progression:
 * 1. Quantum Overseer (Echo Depths) → Unlocks Phase 2, reduces instability
 * 2. Rift Warden (Dimensional Rift) → Unlocks Phase 3, reveals Twins
 * 3. Entangled Twins (Superposition) → Unlocks Final Phase, reveals Sovereign
 * 4. Singularity Sovereign (Quantum Realm) → ENDING CHOICE TRIGGER
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class BossDeathHandler {
    
    // Track active boss fights (prevent despawn exploits) - runtime only, no need to persist
    private static final Map<UUID, BossType> ACTIVE_BOSS_FIGHTS = new HashMap<>();
    
    // Boss progress stored in player.getPersistentData() for persistence across restarts
    private static final String NBT_KEY = "QuantumFalconBossProgress";
    
    /**
     * Boss types for story progression.
     */
    public enum BossType {
        QUANTUM_OVERSEER("Quantum Overseer", 1, 3, ChatFormatting.AQUA),
        RIFT_WARDEN("Rift Warden", 2, 4, ChatFormatting.BLUE),
        ENTANGLED_TWINS("Entangled Twins", 3, 5, ChatFormatting.GREEN),
        SINGULARITY_SOVEREIGN("Singularity Sovereign", 4, 10, ChatFormatting.RED);
        
        public final String displayName;
        public final int storyPhase;
        public final int instabilityReduction; // How much instability is reduced on kill
        public final ChatFormatting color;
        
        BossType(String displayName, int storyPhase, int instabilityReduction, ChatFormatting color) {
            this.displayName = displayName;
            this.storyPhase = storyPhase;
            this.instabilityReduction = instabilityReduction;
            this.color = color;
        }
    }
    
    /**
     * Listen for boss deaths.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        
        // Check if this is a boss entity
        BossType bossType = getBossType(entity);
        if (bossType == null) return;

        // Entangled Twins: only reward when BOTH twins are dead (second death)
        if (bossType == BossType.ENTANGLED_TWINS && entity instanceof EntangledTwinsEntity twin) {
            EntangledTwinsEntity partner = twin.getTwin();
            if (partner != null && partner.isAlive())
                return; // First twin died; partner enrages, no rewards yet
        }
        
        // credit every contributor (players, and owners of falcons/pets that fought), the killer's owner and everyone in
        // the arena; the arena-wide effects run once.
        List<java.util.UUID> away = BossCredit.offlineCredited(entity, event.getSource(), serverLevel);
        List<ServerPlayer> credited = BossCredit.creditedPlayers(entity, event.getSource(), serverLevel);
        // a contributor who logged out before the kill is credited on their next login
        for (java.util.UUID id : away) PendingBossCredit.get(serverLevel.getServer()).add(id, bossType, serverLevel.dimension(), entity.blockPosition());
        // when a veteran in the party made the shared floor drop the reduced table, each first-timer still gets the
        // first-kill extras personally: the 30% Nether Star roll and the XP difference (500 vs 100)
        List<ServerPlayer> rookies = new java.util.ArrayList<>();
        if (entity instanceof QuantumOverseerEntity ov && ov.reducedFloor())
            for (ServerPlayer sp : credited) if (!PlayerStoryData.get(sp).hasDefeatedBoss(bossType.name())) rookies.add(sp);
        boolean first = true;
        for (ServerPlayer sp : credited) { onBossDefeated(sp, serverLevel, bossType, entity.blockPosition(), first); first = false; }
        for (ServerPlayer sp : rookies) {
            sp.giveExperiencePoints(400);
            sp.displayClientMessage(Component.literal("First Overseer kill: +400 XP").withStyle(ChatFormatting.GREEN), false);
            if (sp.getRandom().nextFloat() < FIRST_KILL_STAR_CHANCE) {
                var star = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR);
                if (!sp.getInventory().add(star)) sp.drop(star, false);
                sp.displayClientMessage(Component.literal("+1 ").append(star.getHoverName()).append(" (first kill)").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
        }
        if (credited.isEmpty()) QuantumFalcon.LOGGER.info("{} died with nobody to credit (no player damage, no player in the arena)", bossType.displayName);
    }

    /**
     * Get boss type from entity.
     */
    static String bossEntityId(BossType t) {
        return switch (t) {
            case QUANTUM_OVERSEER -> "quantumfalcon:quantum_overseer";
            case RIFT_WARDEN -> "quantumfalcon:rift_warden";
            case ENTANGLED_TWINS -> "quantumfalcon:entangled_twins";
            case SINGULARITY_SOVEREIGN -> "quantumfalcon:singularity_sovereign";
        };
    }

    private static BossType getBossType(LivingEntity entity) {
        if (entity instanceof QuantumOverseerEntity) return BossType.QUANTUM_OVERSEER;
        if (entity instanceof RiftWardenEntity) return BossType.RIFT_WARDEN;
        if (entity instanceof EntangledTwinsEntity) return BossType.ENTANGLED_TWINS;
        if (entity instanceof SingularitySovereignEntity) return BossType.SINGULARITY_SOVEREIGN;
        return null;
    }
    
    /**
     * Called when a boss is defeated by a player.
     */
    public static void onBossDefeated(ServerPlayer player, ServerLevel level, BossType bossType, BlockPos deathPos) { onBossDefeated(player, level, bossType, deathPos, true); }

    public static void onBossDefeated(ServerPlayer player, ServerLevel level, BossType bossType, BlockPos deathPos, boolean worldEffects) {
        UUID playerId = player.getUUID();
        
        // Record progress in persistent NBT data (survives server restarts)
        boolean firstKill = !PlayerStoryData.get(player).hasDefeatedBoss(bossType.name());
        recordBossDefeat(player, bossType);
        PlayerStoryData.get(player).markBossDefeated(bossType.name());   // persisted, gates the next arena
        com.matt.quantumfalcon.quest.QuestTracker.creditBossKill(player, bossEntityId(bossType));
        // W6 / / B5: skill points go to every credited player, on that player's first kill of this boss only
        if (firstKill) com.matt.quantumfalcon.skill.SkillPointSystem.awardBossFirstKill(player, bossType.ordinal(), bossType.displayName);
        // on every credited kill the boss's relic (Overseer's Eye, Warden's Fragment, Twin Essence,
        // Sovereign's Crown) and main_4's Artifact Fragments (2 from the Warden, 2 from the Twins) go straight into the
        // inventory, so nothing story-critical can be lost down an arena's void pit or to a party member's pickup, and a lost
        // relic or fragment can be earned again by beating that boss at another arena. Gifts count as received for COLLECT.
        {
            java.util.List<net.minecraft.world.item.ItemStack> gifts = new java.util.ArrayList<>();
            switch (bossType) {
                // every boss now re-gifts a lost relic on a repeat kill, but never stacks more of them
                case QUANTUM_OVERSEER -> giftRelic(gifts, player, firstKill, com.matt.quantumfalcon.registry.ModItems.OVERSEERS_EYE.get(), null);
                case RIFT_WARDEN -> giftRelic(gifts, player, firstKill, com.matt.quantumfalcon.registry.ModItems.WARDENS_FRAGMENT.get(), com.matt.quantumfalcon.registry.ModItems.ARTIFACT_FRAGMENT.get());
                case ENTANGLED_TWINS -> giftRelic(gifts, player, firstKill, com.matt.quantumfalcon.registry.ModItems.TWIN_ESSENCE.get(), com.matt.quantumfalcon.registry.ModItems.ARTIFACT_FRAGMENT.get());
                case SINGULARITY_SOVEREIGN -> giftRelic(gifts, player, firstKill, com.matt.quantumfalcon.registry.ModItems.SOVEREIGNS_CROWN.get(), null);
            }
            for (var st : gifts) {
                Component name = st.getHoverName();
                int n = st.getCount();
                com.matt.quantumfalcon.quest.QuestTracker.creditReceived(player, st);
                if (!player.getInventory().add(st)) player.drop(st, false);
                player.displayClientMessage(Component.literal("+" + n + " ").append(name).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
            com.matt.quantumfalcon.quest.QuestTracker.syncCollectObjectives(player);
        }
        
        QuantumFalcon.LOGGER.info("Player {} defeated {}!", player.getName().getString(), bossType.displayName);
        
        // === VICTORY MESSAGE ===
        sendVictoryMessage(player, bossType);
        
        // === REDUCE INSTABILITY ===
        reduceInstability(player, bossType);
        
        // === UNREGISTER ARENA ===
        if (worldEffects) {
            var arenaHere = ArenaMechanics.getArenaAt(level, deathPos);
            if (arenaHere != null) ArenaMechanics.unregisterArena(arenaHere.center);
        }
        
        // === SPAWN VICTORY EFFECTS ===
        if (worldEffects) spawnVictoryEffects(level, deathPos, bossType);
        
        // === STORY PROGRESSION ===
        advanceStory(player, bossType);
        
        // === CHECK FOR FINAL BOSS ===
        if (bossType == BossType.SINGULARITY_SOVEREIGN) {
            triggerEndingChoice(player, level);
        }
    }
    
    /** Relic (+2 Artifact Fragments for main_4) on a first kill; on a repeat only what the player has lost. */
    /** Same odds as the full Overseer table's star pool (weight 3 of 10). Tests may set it to 0 or 1. */
    public static float FIRST_KILL_STAR_CHANCE = 0.3f;

    private static void giftRelic(java.util.List<net.minecraft.world.item.ItemStack> gifts, ServerPlayer player, boolean firstKill,
                                  net.minecraft.world.item.Item relic, @javax.annotation.Nullable net.minecraft.world.item.Item fragments) {
        // gifted relics are bound to the player, so a stashed copy plus a re-gift is a duplicate nobody else can use
        // (the True path and the gem recipe only take your own or unbound relics; the gem/Heart stay bound to you)
        if (firstKill || player.getInventory().countItem(relic) == 0) {
            var st = new net.minecraft.world.item.ItemStack(relic);
            StoryManager.bind(st, player);
            StoryManager.stampRelic(st, player);   // the newest copy is the only live one; an older stashed copy crumbles
            gifts.add(st);
        }
        if (fragments != null && firstKill) gifts.add(new net.minecraft.world.item.ItemStack(fragments, 2));
    }

    /**
     * Send victory message to player.
     */
    private static void sendVictoryMessage(ServerPlayer player, BossType bossType) {
        player.displayClientMessage(Component.empty(), false);
        player.displayClientMessage(
            Component.literal("═══════════════════════════════════").withStyle(bossType.color), false);
        player.displayClientMessage(
            Component.literal("  ★ VICTORY ★").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        player.displayClientMessage(
            Component.literal("  " + bossType.displayName + " has been defeated!")
                .withStyle(bossType.color), false);
        player.displayClientMessage(
            Component.literal("  Piece " + bossType.storyPhase + " of 4 recovered")
                .withStyle(ChatFormatting.GRAY), false);
        player.displayClientMessage(
            Component.literal("═══════════════════════════════════").withStyle(bossType.color), false);
        player.displayClientMessage(Component.empty(), false);
        
        // Play victory sound
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
    }
    
    /**
     * Reduce dimension instability as reward for defeating boss.
     */
    private static void reduceInstability(ServerPlayer player, BossType bossType) {
        InstabilityManager.PlayerEchoData data = InstabilityManager.getPlayerData(player);
        if (data != null) {
            // a cleared arena calms the Rift around the winner: meter to 0 plus a short grace period
            var cap = com.matt.quantumfalcon.capability.PlayerInstabilityCapability.get(player);
            cap.setInstability(0f);
            cap.setGraceTicks(com.matt.quantumfalcon.dimension.InstabilityEffects.RESPAWN_GRACE_TICKS * 2);
            data.echoPhantomCount = 0;
            
            player.displayClientMessage(
                Component.literal("✓ The night loosens its grip. Instability reset.")
                    .withStyle(ChatFormatting.GREEN), false);
        }
        
        // Clear negative effects only: removeAllEffects() also stripped the player's own buffs (Resistance, potions) at the
        // moment the arena's leftovers were still live, so a winner could die on the Final Choice screen
        new java.util.ArrayList<>(player.getActiveEffects()).stream()
            .filter(e -> e.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)
            .forEach(e -> player.removeEffect(e.getEffect()));
    }
    
    /**
     * Spawn victory particle effects.
     */
    private static void spawnVictoryEffects(ServerLevel level, BlockPos pos, BossType bossType) {
        // Explosion of particles
        for (int i = 0; i < 200; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = level.random.nextDouble() * 10;
            double height = level.random.nextDouble() * 5;
            
            level.sendParticles(
                ParticleTypes.END_ROD,
                pos.getX() + Math.cos(angle) * radius,
                pos.getY() + height,
                pos.getZ() + Math.sin(angle) * radius,
                1, 0, 0.1, 0, 0.02);
        }
        
        // Ring expansion
        for (int ring = 0; ring < 5; ring++) {
            final int r = ring;
            for (int i = 0; i < 36; i++) {
                double angle = Math.toRadians(i * 10);
                level.sendParticles(
                    ParticleTypes.FIREWORK,
                    pos.getX() + Math.cos(angle) * (r * 3 + 3),
                    pos.getY() + 2,
                    pos.getZ() + Math.sin(angle) * (r * 3 + 3),
                    1, 0, 0, 0, 0);
            }
        }
        
        // Sound
        level.playSound(null, pos, com.matt.quantumfalcon.registry.ModSounds.BOSS_DEATH.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
    }
    
    /**
     * Advance story based on boss defeated.
     */
    private static void advanceStory(ServerPlayer player, BossType bossType) {
        Component storyMessage = switch (bossType) {
            case QUANTUM_OVERSEER -> Component.literal(
                "He only wanted one more night. " +
                "Deeper in, something has been keeping that night locked: the Rift Warden. (Pieces 1/4)")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
                
            case RIFT_WARDEN -> Component.literal(
                "The last lock is open. " +
                "Past it, two voices are still finishing a promise: the Entangled Twins. (Pieces 2/4)")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
                
            case ENTANGLED_TWINS -> Component.literal(
                "The rite is finished. " +
                "At the heart of the vault, the will that held the hour wakes up: the Singularity Sovereign. (Pieces 3/4)")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
                
            case SINGULARITY_SOVEREIGN -> Component.literal(
                "The hour is loose, and it's in your hands. " +
                "Choose what morning means. (Pieces 4/4)")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        };
        
        // Delayed story message (dramatic effect)
        player.displayClientMessage(Component.empty(), false);
        player.displayClientMessage(storyMessage, false);
        // chapter beats fire when the boss falls (they used to fire on arena entry, before the fight)
        StorylineManager.triggerStoryEvent(player, switch (bossType) {
            case QUANTUM_OVERSEER -> StorylineManager.StoryChapter.CHAPTER_3;
            case RIFT_WARDEN -> StorylineManager.StoryChapter.CHAPTER_5;
            case ENTANGLED_TWINS -> StorylineManager.StoryChapter.CHAPTER_7;
            case SINGULARITY_SOVEREIGN -> StorylineManager.StoryChapter.CHAPTER_9;
        });
    }
    
    /**
     * Trigger the ending choice GUI after final boss defeat.
     */
    private static void triggerEndingChoice(ServerPlayer player, ServerLevel level) {
        QuantumFalcon.LOGGER.info("Triggering ending choice for player {}", player.getName().getString());
        
        // Mark player as ready for ending
        StoryManager.setPlayerReadyForEnding(player, true);
        
        // Send packet to open GUI on client
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            com.matt.quantumfalcon.network.OpenStoryChoiceScreenPacket.OpenStoryChoiceScreenPayload.of(player));
        
        // Dramatic sound
        player.playNotifySound(SoundEvents.END_PORTAL_SPAWN, SoundSource.MASTER, 1.0f, 0.5f);
        
        // Dramatic particles around player
        if (level != null) {
            for (int i = 0; i < 100; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double radius = 2 + level.random.nextDouble() * 3;
                level.sendParticles(
                    ParticleTypes.END_ROD,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + level.random.nextDouble() * 3,
                    player.getZ() + Math.sin(angle) * radius,
                    1, 0, 0.05, 0, 0.01);
            }
        }
    }
    
    // === PERSISTENT STORAGE (survives server restarts) ===
    
    /**
     * Record boss defeat in player's persistent NBT data.
     * Pattern from player.getPersistentData() - same as Twilight Forest progression tracking.
     */
    private static void recordBossDefeat(ServerPlayer player, BossType bossType) {
        CompoundTag data = player.getPersistentData();
        CompoundTag bossData = data.contains(NBT_KEY) ? data.getCompound(NBT_KEY) : new CompoundTag();
        bossData.putBoolean(bossType.name(), true);
        data.put(NBT_KEY, bossData);
    }
    
    /**
     * Get defeated bosses from persistent data.
     */
    private static Set<BossType> getDefeatedBossesFromNBT(Player player) {
        Set<BossType> defeated = new HashSet<>();
        CompoundTag data = player.getPersistentData();
        if (data.contains(NBT_KEY)) {
            CompoundTag bossData = data.getCompound(NBT_KEY);
            for (BossType type : BossType.values()) {
                if (bossData.getBoolean(type.name())) {
                    defeated.add(type);
                }
            }
        }
        return defeated;
    }
    
    // === QUERY METHODS ===
    
    /**
     * Check if player has defeated a specific boss.
     */
    public static boolean hasDefeatedBoss(Player player, BossType bossType) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(NBT_KEY)) {
            return data.getCompound(NBT_KEY).getBoolean(bossType.name());
        }
        return false;
    }
    
    /**
     * Overload for UUID-based lookup (for offline checks, returns false if player not online).
     */
    public static boolean hasDefeatedBoss(UUID playerId, BossType bossType) {
        // For runtime checks, use ACTIVE_BOSS_FIGHTS or require player object
        return false; // Cannot check offline players without server reference
    }
    
    /**
     * Get player's current story phase (0-4).
     */
    public static int getStoryPhase(Player player) {
        Set<BossType> defeated = getDefeatedBossesFromNBT(player);
        if (defeated.isEmpty()) return 0;
        return defeated.stream()
            .mapToInt(b -> b.storyPhase)
            .max()
            .orElse(0);
    }
    
    public static int getStoryPhase(UUID playerId) {
        return 0; // Cannot check offline players
    }
    
    /**
     * Check if player can access a boss based on progression.
     */
    public static boolean canAccessBoss(Player player, BossType bossType) {
        int currentPhase = getStoryPhase(player);
        return currentPhase >= bossType.storyPhase - 1;
    }
    
    public static boolean canAccessBoss(UUID playerId, BossType bossType) {
        return true; // Allow by default for offline checks
    }
    
    /**
     * Get defeated bosses for a player.
     */
    public static Set<BossType> getDefeatedBosses(Player player) {
        return getDefeatedBossesFromNBT(player);
    }
    
    public static Set<BossType> getDefeatedBosses(UUID playerId) {
        return Collections.emptySet(); // Cannot check offline players
    }
}
