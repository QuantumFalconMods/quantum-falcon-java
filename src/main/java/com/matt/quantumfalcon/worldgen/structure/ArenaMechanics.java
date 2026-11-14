package com.matt.quantumfalcon.worldgen.structure;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.boss.EntangledTwinsEntity;
import com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity;
import com.matt.quantumfalcon.entity.boss.RiftWardenEntity;
import com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity;
import com.matt.quantumfalcon.registry.ModParticles;
import com.matt.quantumfalcon.story.StorylineManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles boss arena mechanics:
 * - Instant-death void centers (singularity pull)
 * - Gravity wells (pillar attraction/repulsion)
 * - Energy bridges (glowing particle lines)
 * - Arena boundaries (damage at edges)
 * - Boss phase triggers
 * 
 * Inspired by Twilight Forest boss mechanics and Bumblezone hazard zones.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class ArenaMechanics {
    
    // Track active arenas (center pos -> arena data)
    private static final Map<BlockPos, ActiveArena> ACTIVE_ARENAS = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Track players in arenas
    private static final Map<UUID, BlockPos> PLAYERS_IN_ARENA = new java.util.concurrent.ConcurrentHashMap<>();
    
    /** Arena types cleared since server start (used by tests / debug info). */
    public static final java.util.List<BossArenaStructure.ArenaType> DEFEATED_LOG = new java.util.concurrent.CopyOnWriteArrayList<>();

    // Constants
    private static final double VOID_PULL_STRENGTH = 0.15;
    private static final double EDGE_DAMAGE_DISTANCE = 5.0;
    private static final int ENERGY_BRIDGE_PARTICLE_INTERVAL = 5;
    
    /**
     * Register an active boss arena.
     */
    public static void registerArena(BlockPos center, BossArenaStructure.ArenaType type) {
        ACTIVE_ARENAS.put(center, new ActiveArena(center, type));
        QuantumFalcon.LOGGER.debug("Registered {} arena at {}", type.name(), center);
    }

    /** Register an arena in a specific dimension and persist it (survives server restarts). */
    public static void registerArena(ServerLevel level, BlockPos center, BossArenaStructure.ArenaType type) {
        registerArena(level, center, type, -1, 20);
    }

    /** Register with an explicit play radius and entry margin (dungeon arenas). */
    public static void registerArena(ServerLevel level, BlockPos center, BossArenaStructure.ArenaType type, int radius, int margin) {
        ActiveArena a = new ActiveArena(center.immutable(), type);
        a.radius = radius; a.margin = margin;
        a.dimension = level.dimension();
        ACTIVE_ARENAS.put(a.center, a);
        var server = level.getServer();
        if (server.isSameThread()) ArenaSavedData.get(server).put(a);
        else server.execute(() -> ArenaSavedData.get(server).put(a)); // worldgen threads
        QuantumFalcon.LOGGER.debug("Registered {} arena at {} in {}", type.name(), center, level.dimension().location());
    }

    /** The registered arena whose centre is nearest to pos within range, or null (screenshot / debug helper). */
    public static ActiveArena arenaNear(BlockPos pos, int range) {
        ActiveArena best = null; double bd = (double) range * range;
        for (ActiveArena a : ACTIVE_ARENAS.values()) { double d = a.center.distSqr(pos); if (d <= bd) { bd = d; best = a; } }
        return best;
    }

    public static java.util.Collection<ActiveArena> getActiveArenas() { return java.util.Collections.unmodifiableCollection(ACTIVE_ARENAS.values()); }

    @SubscribeEvent
    public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        ACTIVE_ARENAS.clear();
        PLAYERS_IN_ARENA.clear();
        for (ActiveArena a : ArenaSavedData.get(event.getServer()).all()) ACTIVE_ARENAS.put(a.center, a);
        QuantumFalcon.LOGGER.info("Loaded {} persisted boss arena(s)", ACTIVE_ARENAS.size());
    }
    
    /**
     * Unregister an arena (boss defeated).
     */
    public static void unregisterArena(BlockPos center) {
        ActiveArena removed = ACTIVE_ARENAS.remove(center);
        // players standing in it are no longer "in an arena": otherwise their stale entry blocked the next arena's
        // entry (and its boss) until the server restarted
        PLAYERS_IN_ARENA.values().removeIf(center::equals);
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) ArenaSavedData.get(server).remove(center);
        if (removed != null) DEFEATED_LOG.add(removed.type);
        QuantumFalcon.LOGGER.info("Unregistered arena at {}", center);
    }

    /**
     * Called when a boss tied to an arena dies.
     * Triggers victory visuals and cleans up the arena state.
     */
    public static void onBossDeath(net.minecraft.world.entity.Entity boss) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        // retire the arena that OWNS this boss, wherever it died (carted, pearled or portalled out); the death
        // position is only a fallback for bosses no arena recorded
        ActiveArena arena = owningArena(level, boss);
        if (arena == null) {
            return;
        }
        com.matt.quantumfalcon.arena.fight.ArenaInteraction.onBossDeath(level, arena.center, boss);
        ArenaDropGuard.watch(level, getArenaBounds(arena), arena.center, 200);   // no loot down the void pit
        // the boss's live attacks die with it (a Sovereign black hole kept imploding on the winner after the kill)
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, getArenaBounds(arena),
                e -> e instanceof com.matt.quantumfalcon.entity.projectile.BlackHoleEntity
                  || e instanceof com.matt.quantumfalcon.entity.projectile.VoidBlastEntity
                  || e instanceof com.matt.quantumfalcon.entity.projectile.RealityRiftEntity
                  || e instanceof com.matt.quantumfalcon.entity.projectile.QuantumShardEntity)) e.discard();

        // Victory particles at arena center
        level.sendParticles((ParticleOptions) ModParticles.COSMIC_STAR.get(),
            arena.center.getX() + 0.5, arena.center.getY() + 2, arena.center.getZ() + 0.5,
            80, 3.0, 2.0, 3.0, 0.05);
        level.sendParticles(ParticleTypes.FIREWORK,
            arena.center.getX() + 0.5, arena.center.getY() + 4, arena.center.getZ() + 0.5,
            40, 2.0, 2.0, 2.0, 0.1);

        // Boss-specific victory message for all 4 bosses
        String victoryMsg = "✨ The boss has fallen!";
        if (boss instanceof SingularitySovereignEntity) {
            victoryMsg = "✨ The Sovereign falls. The crown keeps its shape a moment longer, waiting for a head.";
        } else if (boss instanceof QuantumOverseerEntity) {
            victoryMsg = "✨ The Quantum Overseer falls. His eye stays open, still turned east.";
        } else if (boss instanceof RiftWardenEntity) {
            victoryMsg = "✨ The Rift Warden kneels. \"Then keep the door yourself.\"";
        } else if (boss instanceof EntangledTwinsEntity) {
            victoryMsg = "✨ The beam between the Twins closes. The rite is finished at last.";
        }
        final String msg = victoryMsg;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getArenaBounds(arena))) {
            player.displayClientMessage(
                Component.literal(msg).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                false
            );
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        int opened = com.matt.quantumfalcon.dungeon.TreasuryVaultBlock.unlockAround(level, arena.center, arena.radius() + 8);
        if (opened > 0) {
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getArenaBounds(arena))) {
                player.displayClientMessage(Component.literal("The arena treasury unseals.").withStyle(ChatFormatting.AQUA), false);
            }
        }
        unregisterArena(arena.center);
    }

    /** the arena whose recorded boss is this entity (or its twin), else the arena at its position. */
    @javax.annotation.Nullable
    public static ActiveArena owningArena(ServerLevel level, net.minecraft.world.entity.Entity boss) {
        java.util.Set<java.util.UUID> ids = new java.util.HashSet<>();
        ids.add(boss.getUUID());
        if (boss instanceof EntangledTwinsEntity tw && tw.getTwin() != null) ids.add(tw.getTwin().getUUID());
        for (ActiveArena a : ACTIVE_ARENAS.values())
            if (a.bossId != null && ids.contains(a.bossId) && (a.dimension == null || a.dimension == level.dimension())) return a;
        return getArenaAt(level, boss.blockPosition());
    }

    /**
     * Get the arena that contains the given world position (for boss pathfinding / arena lock).
     * Returns null if position is not inside any registered arena.
     */
    public static ActiveArena getArenaAt(net.minecraft.world.level.Level level, BlockPos worldPos) {
        for (ActiveArena arena : ACTIVE_ARENAS.values()) {
            if (arena.dimension != null && arena.dimension != level.dimension()) continue;
            double dx = worldPos.getX() - arena.center.getX();
            double dz = worldPos.getZ() - arena.center.getZ();
            if (Math.sqrt(dx * dx + dz * dz) < arena.radius() + arena.margin && Math.abs(worldPos.getY() - arena.center.getY()) < 50) return arena;
        }
        return null;
    }

    public static ActiveArena getArenaAt(BlockPos worldPos) {
        for (ActiveArena arena : ACTIVE_ARENAS.values()) {
            double dx = worldPos.getX() - arena.center.getX();
            double dz = worldPos.getZ() - arena.center.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist < arena.radius() + arena.margin
                && Math.abs(worldPos.getY() - arena.center.getY()) < 50) {
                return arena;
            }
        }
        return null;
    }

    /**
     * Get the bounding box for an arena (for clamping boss pathfinding targets).
     */
    public static AABB getArenaBounds(ActiveArena arena) {
        int r = arena.radius();
        BlockPos c = arena.center;
        return new AABB(
            c.getX() - r, c.getY() - 20, c.getZ() - r,
            c.getX() + r + 1, c.getY() + 30, c.getZ() + r + 1
        );
    }
    
    /**
     * Tick handler for arena mechanics.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        dropStaleTracking(serverPlayer);
        
        // Check each active arena
        for (Map.Entry<BlockPos, ActiveArena> entry : ACTIVE_ARENAS.entrySet()) {
            ActiveArena arena = entry.getValue();
            
            if (isPlayerInArena(player, arena)) {
                // Player entered arena
                if (!PLAYERS_IN_ARENA.containsKey(player.getUUID())) {
                    onPlayerEnterArena(serverPlayer, arena);
                }
                
                // Apply arena mechanics
                applyArenaMechanics(serverPlayer, serverLevel, arena);
            } else {
                // Player left arena
                if (PLAYERS_IN_ARENA.containsKey(player.getUUID()) && 
                    PLAYERS_IN_ARENA.get(player.getUUID()).equals(arena.center)) {
                    onPlayerLeaveArena(serverPlayer, arena);
                }
            }
        }
        
        // Spawn energy bridge particles periodically
        if (serverLevel.getGameTime() % ENERGY_BRIDGE_PARTICLE_INTERVAL == 0) {
            for (ActiveArena arena : ACTIVE_ARENAS.values()) {
                spawnEnergyBridgeParticles(serverLevel, arena);
            }
        }
    }
    
    /** A tracked arena that no longer exists (unregistered after its boss fell) must not keep the player "inside". */
    private static void dropStaleTracking(ServerPlayer player) {
        BlockPos at = PLAYERS_IN_ARENA.get(player.getUUID());
        if (at != null && !ACTIVE_ARENAS.containsKey(at)) PLAYERS_IN_ARENA.remove(player.getUUID());
    }

    /** Test hook (/qftest): runs the same per-player arena logic as onPlayerTick for one (fake) player. */
    public static boolean testPlayerTick(ServerPlayer player, ServerLevel level) {
        dropStaleTracking(player);
        boolean in = false;
        for (ActiveArena arena : ACTIVE_ARENAS.values()) {
            if (isPlayerInArena(player, arena)) {
                in = true;
                if (!PLAYERS_IN_ARENA.containsKey(player.getUUID())) onPlayerEnterArena(player, arena);
                applyArenaMechanics(player, level, arena);
            }
        }
        return in;
    }
    public static boolean isPlayerTracked(java.util.UUID id) { return PLAYERS_IN_ARENA.containsKey(id); }
    public static double deathY(BlockPos center) { ActiveArena a = ACTIVE_ARENAS.get(center); return a == null ? Double.NaN : a.type.getDeathY(center.getY()); }

    /**
     * Check if player is within arena bounds.
     */
    private static boolean isPlayerInArena(Player player, ActiveArena arena) {
        if (arena.dimension != null && arena.dimension != player.level().dimension()) return false;
        double dx = player.getX() - arena.center.getX();
        double dy = player.getY() - arena.center.getY();
        double dz = player.getZ() - arena.center.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        return horizontalDist < arena.radius() + arena.margin && Math.abs(dy) < 50;
    }
    
    /**
     * Called when player enters an arena.
     */
    private static void onPlayerEnterArena(ServerPlayer player, ActiveArena arena) {
        PLAYERS_IN_ARENA.put(player.getUUID(), arena.center);
        
        // the bosses answer in story order (Overseer, Warden, Twins, Sovereign); creative players may skip ahead
        String missing = missingPrerequisite(player, arena.type);
        boolean sealed = missing != null && !player.isCreative() && !com.matt.quantumfalcon.arena.BossSummoner.bossPresent(player.serverLevel(), arena);

        // Display arena warning (a sealed arena stays quiet: nothing "awakens" until its seal can break)
        Component warning = sealed
            ? Component.literal("\u26a0 " + bossName(arena.type) + " sleeps behind the seal").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD)
            : switch (arena.type) {
            case QUANTUM_GUARDIAN -> Component.literal("⚡ The Overseer's Altar. Something on the throne is still watching.")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            case VOID_HORROR -> Component.literal("☠ The Rift Warden turns. \"You should not exist here.\"")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
            case ENTANGLED_TWINS -> Component.literal("⚛ Two voices, one breath: \"Don't let go.\" \"I never did.\"")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
            case SINGULARITY_SOVEREIGN -> Component.literal("⚫ A voice from everywhere: \"Stay. It is always this hour here.\"")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        };
        
        player.displayClientMessage(warning, false);
        if (!sealed) player.playNotifySound(com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_summon"), SoundSource.HOSTILE, 0.8f, 1.0f);

        // (story chapters now fire on boss death in BossDeathHandler, not on arena entry)

        if (sealed) {
            player.displayClientMessage(Component.literal("The seal holds. " + bossName(arena.type) + " keeps its piece of the hour until " + missing + " gives up theirs.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            QuantumFalcon.LOGGER.info("Player {} entered sealed {} arena (needs {})", player.getName().getString(), arena.type, missing);
            return;
        }

        // Summon the arena's guardian if it is not already here (arenas built by command or after a restart)
        if (!com.matt.quantumfalcon.arena.BossSummoner.bossPresent(player.serverLevel(), arena)) {
            com.matt.quantumfalcon.arena.BossSummoner.spawnBoss(player.serverLevel(), arena.center, arena.type);
        }

        QuantumFalcon.LOGGER.info("Player {} entered {} arena", player.getName().getString(), arena.type);
    }
    
    /** name of the boss the player must defeat before this arena's boss appears, or null. */
    public static String missingPrerequisite(net.minecraft.world.entity.player.Player player, BossArenaStructure.ArenaType type) {
        var story = com.matt.quantumfalcon.story.PlayerStoryData.get(player);
        return switch (type) {
            case QUANTUM_GUARDIAN -> null;
            case VOID_HORROR -> story.hasDefeatedBoss("QUANTUM_OVERSEER") ? null : "the Quantum Overseer";
            case ENTANGLED_TWINS -> story.hasDefeatedBoss("RIFT_WARDEN") ? null : "the Rift Warden";
            case SINGULARITY_SOVEREIGN -> story.hasDefeatedBoss("ENTANGLED_TWINS") ? null : "the Entangled Twins";
        };
    }

    private static String bossName(BossArenaStructure.ArenaType type) {
        return switch (type) {
            case QUANTUM_GUARDIAN -> "The Quantum Overseer";
            case VOID_HORROR -> "The Rift Warden";
            case ENTANGLED_TWINS -> "The Entangled Twins";
            case SINGULARITY_SOVEREIGN -> "The Singularity Sovereign";
        };
    }

    /**
     * Called when player leaves an arena.
     */
    private static void onPlayerLeaveArena(ServerPlayer player, ActiveArena arena) {
        PLAYERS_IN_ARENA.remove(player.getUUID());
        
        player.displayClientMessage(
            Component.literal("You have left the arena").withStyle(ChatFormatting.GRAY), true);
        
        QuantumFalcon.LOGGER.info("Player {} left {} arena", player.getName().getString(), arena.type);
    }
    
    /**
     * Apply all arena mechanics to a player.
     */
    private static void applyArenaMechanics(ServerPlayer player, ServerLevel level, ActiveArena arena) {
        if (player.isSpectator()) return;   // spectators are cameras: no void pull, fall rescue or edge damage
        // === VOID CENTER DEATH ZONE ===
        if (arena.type.hasVoidCenter()) {
            applyVoidCenterEffect(player, level, arena);
        }
        
        // === FALL DEATH CHECK ===
        if (player.getY() < arena.type.getDeathY(arena.center.getY())) {
            applyFallDeath(player, level, arena);
        }
        
        // === EDGE DAMAGE ===
        applyEdgeDamage(player, level, arena);
        
        // === ARENA-SPECIFIC EFFECTS ===
        switch (arena.type) {
            case QUANTUM_GUARDIAN      -> applyGuardianEffects(player, level, arena);
            case VOID_HORROR           -> applyVoidHorrorEffects(player, level, arena);
            case ENTANGLED_TWINS       -> applyEntangledTwinsEffects(player, level, arena);
            case SINGULARITY_SOVEREIGN -> applySovereignEffects(player, level, arena);
        }
    }
    
    /**
     * Apply void center pull and death.
     * PHASE-LOCKED: Only active in phase 2+
     */
    private static void applyVoidCenterEffect(ServerPlayer player, ServerLevel level, ActiveArena arena) {
        double dx = player.getX() - arena.center.getX();
        double dz = player.getZ() - arena.center.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        int voidRadius = arena.type.getVoidCenterRadius();
        
        // Phase 1: Void center is dormant - only visual warning
        if (!arena.isVoidCenterActive()) {
            // Show dormant void particles but no damage/pull
            if (dist < voidRadius + 5 && level.getGameTime() % 20 == 0) {
                level.sendParticles(ParticleTypes.SMOKE,
                    arena.center.getX(), arena.center.getY() + 1, arena.center.getZ(),
                    10, voidRadius * 0.5, 0.5, voidRadius * 0.5, 0.01);
                
                player.displayClientMessage(
                    Component.literal("⚠ Dormant singularity detected...")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
            return;
        }
        
        // Phase 2+: Void center is ACTIVE
        
        // Inside void = instant death
        if (dist < voidRadius) {
            player.hurt(player.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            player.displayClientMessage(
                Component.literal("⚫ CONSUMED BY THE SINGULARITY")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
            
            // Death burst particles
            level.sendParticles((ParticleOptions) ModParticles.SINGULARITY_DISTORT.get(),
                player.getX(), player.getY() + 1, player.getZ(),
                50, 1, 1, 1, 0.5);
            return;
        }
        
        // Near void = gravitational pull (strength based on phase)
        if (dist < voidRadius + 10) {
            double pullStrength = arena.getVoidPullStrength() * (1.0 - (dist - voidRadius) / 10.0);
            Vec3 pullDir = new Vec3(-dx, 0, -dz).normalize().scale(pullStrength);
            player.setDeltaMovement(player.getDeltaMovement().add(pullDir));
            player.hurtMarked = true;
            
            // Warning particles - more intense in higher phases
            if (level.getGameTime() % 5 == 0) {
                int particleCount = 5 + (arena.bossPhase * 3);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 1, player.getZ(),
                    particleCount, 0.5, 0.5, 0.5, 0.1);
                
                // Add custom gravity particles in phase 3
                if (arena.bossPhase >= 3) {
                    level.sendParticles((ParticleOptions) ModParticles.GRAVITY_PARTICLE.get(),
                        player.getX(), player.getY(), player.getZ(),
                        3, 0.3, 0.3, 0.3, 0.05);
                }
            }
            
            // Phase 3: Additional damage over time near void
            if (arena.bossPhase >= 3 && level.getGameTime() % 20 == 0) {
                player.hurt(player.damageSources().magic(), 2.0f);
            }
        }
        
        // Spawn void center visual effects
        if (level.getGameTime() % 10 == 0) {
            spawnVoidCenterParticles(level, arena);
        }
    }
    
    /**
     * Spawn visual particles at the void center.
     */
    private static void spawnVoidCenterParticles(ServerLevel level, ActiveArena arena) {
        int voidRadius = arena.type.getVoidCenterRadius();
        
        // Spiral effect around void
        double time = level.getGameTime() * 0.1;
        for (int i = 0; i < 8; i++) {
            double angle = time + (Math.PI * 2 * i / 8);
            double radius = voidRadius * 0.8;
            double x = arena.center.getX() + Math.cos(angle) * radius;
            double z = arena.center.getZ() + Math.sin(angle) * radius;
            
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                x, arena.center.getY() + 2, z,
                2, 0.1, 0.5, 0.1, 0.02);
        }
        
        // Center distortion
        level.sendParticles((ParticleOptions) ModParticles.SINGULARITY_DISTORT.get(),
            arena.center.getX(), arena.center.getY() + 1, arena.center.getZ(),
            5, voidRadius * 0.3, 0.3, voidRadius * 0.3, 0.01);
    }
    
    /**
     * Apply fall death when below arena.
     */
    /** Number of void-fall rescues performed (read by /qftest). */
    public static final java.util.concurrent.atomic.AtomicInteger FALL_RESCUES = new java.util.concurrent.atomic.AtomicInteger();
    private static void applyFallDeath(ServerPlayer player, ServerLevel level, ActiveArena arena) {
        FALL_RESCUES.incrementAndGet();
        player.hurt(player.damageSources().fellOutOfWorld(), 10.0f);
        
        // Teleport back to arena edge if not dead
        if (player.isAlive()) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = arena.radius() * 0.8;
            double newX = arena.center.getX() + Math.cos(angle) * radius;
            double newZ = arena.center.getZ() + Math.sin(angle) * radius;
            player.teleportTo(newX, arena.center.getY() + 2, newZ);
            
            player.displayClientMessage(
                Component.literal("⚠ You fell into the void!")
                    .withStyle(ChatFormatting.RED), true);
        }
    }
    
    /**
     * Apply damage near arena edges.
     */
    private static void applyEdgeDamage(ServerPlayer player, ServerLevel level, ActiveArena arena) {
        double dx = player.getX() - arena.center.getX();
        double dz = player.getZ() - arena.center.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        if (dist > arena.radius() - EDGE_DAMAGE_DISTANCE) {
            // Push player back toward center
            Vec3 pushDir = new Vec3(-dx, 0, -dz).normalize().scale(0.1);
            player.setDeltaMovement(player.getDeltaMovement().add(pushDir));
            player.hurtMarked = true;
            
            // Warning every second
            if (level.getGameTime() % 20 == 0) {
                player.hurt(player.damageSources().magic(), 2.0f);
                player.displayClientMessage(
                    Component.literal("⚠ Arena boundary!").withStyle(ChatFormatting.RED), true);
            }
        }
    }
    
    /**
     * Quantum Guardian arena effects.
     *
     * Phase 1 : Periodic glowing to highlight the player
     * Phase 2+: Cyan laser beams shoot from each energy pillar to the pedestal center
     *           (4 beams at 45° NE/NW/SE/SW, r=34 matching the programmatic builder)
     * Phase 3+: Cross-beams between adjacent pillars + mild energy damage pulse
     */
    private static void applyGuardianEffects(ServerPlayer player, ServerLevel level, ActiveArena arena) {
        // All phases: periodic glowing pulse from energy pillars
        if (level.getGameTime() % 100 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
        }

        // Phase 2+: cyan laser beams from 4 pillars to center (matches concept art)
        if (arena.bossPhase >= 2 && level.getGameTime() % 20 == 0) {
            double pillarR = arena.radius() * 0.70;
            for (int i = 0; i < 4; i++) {
                // 4 pillars at 45° offsets: NE=45°, NW=135°, SW=225°, SE=315°
                double angle = Math.PI / 4 + (Math.PI / 2 * i);
                Vec3 pillarTop = new Vec3(
                    arena.center.getX() + Math.cos(angle) * pillarR,
                    arena.center.getY() + 15,
                    arena.center.getZ() + Math.sin(angle) * pillarR
                );
                Vec3 pedestalTop = Vec3.atCenterOf(arena.center).add(0, 3, 0);
                Vec3 dir  = pedestalTop.subtract(pillarTop);
                int steps = (int) (dir.length() / 2);
                Vec3 step = dir.normalize().scale(2);
                Vec3 cur  = pillarTop;
                for (int j = 0; j < steps; j++) {
                    level.sendParticles((ParticleOptions) ModParticles.ENERGY_BOLT.get(),
                        cur.x, cur.y, cur.z, 1, 0, 0, 0, 0);
                    cur = cur.add(step);
                }
            }
        }

        // Phase 3+: inter-pillar cross beams + damage near beam paths
        if (arena.bossPhase >= 3 && level.getGameTime() % 40 == 0) {
            spawnCenterBeams(level, arena, 4, arena.radius() * 0.70);
            // Energy pulse damage on the laser tick
            player.hurt(player.damageSources().magic(), 1.5f);
        }
    }

    /**
     * Void Horror arena effects.
     *
     * Phase 1 : Constant darkness + cave ambience
     * Phase 2+: Crystal shard burst (particles shooting downward near player)
     *           Wither effect for terror
     * Phase 3+: Wither amplified, rapid shard bursts, void wisps intensify
     */
    private static void applyVoidHorrorEffects(ServerPlayer player, ServerLevel level, ActiveArena arena) {
        // All phases: constant darkness + ambient cave sounds
        if (level.getGameTime() % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        }
        if (level.random.nextFloat() < 0.01f) {
            player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.5f, 0.5f);
        }

        // Phase 2+: crystal shard burst — purple particle spray (concept art action shot)
        if (arena.bossPhase >= 2 && level.getGameTime() % 30 == 0) {
            // Shards erupt downward from boss height toward player
            level.sendParticles((ParticleOptions) ModParticles.VOID_FLAME.get(),
                player.getX(), player.getY() + 4, player.getZ(),
                12, 0.8, 1.5, 0.8, 0.15);
            // Wither applied in phase 2
            if (level.getGameTime() % 60 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, false, false));
            }
        }

        // Phase 3+: amplified wither + rapid shards
        if (arena.bossPhase >= 3) {
            if (level.getGameTime() % 40 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 1, false, false));
            }
            if (level.getGameTime() % 15 == 0) {
                level.sendParticles((ParticleOptions) ModParticles.VOID_FLAME.get(),
                    player.getX(), player.getY() + 2, player.getZ(),
                    8, 1.2, 0.5, 1.2, 0.2);
            }
        }
    }
    
    /**
     * Entangled Twins arena effects — quantum entanglement disorientation, mirrored particle pulses.
     * Every 3 seconds, a random entanglement surge disorients the player (nausea).
     * Particles mirror between the two platform centers to emphasise the twin theme.
     */
    private static void applyEntangledTwinsEffects(ServerPlayer player, ServerLevel level, ActiveArena arena) {
        // Quantum entanglement nausea — brief disorientation surge
        if (level.getGameTime() % 60 == 0 && level.random.nextFloat() < 0.4f) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, false, false));
        }

        // Levitation pulse every 5 seconds near the bridge center
        double dx = player.getX() - arena.center.getX();
        double dz = player.getZ() - arena.center.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 8 && level.getGameTime() % 80 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 25, 0, false, false));
        }

        // Mirrored entanglement link particles between twin platform centres
        if (level.getGameTime() % 8 == 0) {
            double halfR = arena.radius() * 0.45;
            for (int side = -1; side <= 1; side += 2) {
                double px = arena.center.getX() + side * halfR;
                double pz = arena.center.getZ();
                level.sendParticles((ParticleOptions) ModParticles.ENTANGLEMENT_LINK.get(),
                    px, arena.center.getY() + 4, pz,
                    4, 1.5, 1.0, 1.5, 0.02);
            }
        }

        // Quantum spark trail when the player moves quickly
        if (player.getDeltaMovement().horizontalDistance() > 0.2 && level.getGameTime() % 4 == 0) {
            level.sendParticles((ParticleOptions) ModParticles.QUANTUM_SPARK.get(),
                player.getX(), player.getY() + 1, player.getZ(),
                3, 0.3, 0.3, 0.3, 0.05);
        }
    }

    /**
     * Singularity Sovereign specific effects - time dilation, reality tears.
     */
    /**
     * Singularity Sovereign arena effects.
     *
     * Phase 1 : Time dilation near center (slowness scales with proximity)
     *           Accretion disk spiral particles orbit the void
     * Phase 2+: Reality tear portals spawn at random positions around the arena
     *           (concept art action shot: purple rift portals opening mid-fight)
     *           Inter-pillar energy beams fire between the 8 massive pillars
     * Phase 3+: Extreme time dilation (blindness flashes), reality tears multiply,
     *           gravity pull from void escalates, pillar-to-center beams activate
     */
    private static void applySovereignEffects(ServerPlayer player, ServerLevel level, ActiveArena arena) {
        double dist = player.position().distanceTo(Vec3.atCenterOf(arena.center));

        // Time dilation (slowness scales with proximity to singularity — concept art)
        if (dist < 30) {
            int amplifier = (int) Math.min(3, (30 - dist) / 8);
            if (level.getGameTime() % 20 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30,
                    amplifier, false, false));
            }
            // Phase 3+: Blindness flashes near the void — time dilation peak
            if (arena.bossPhase >= 3 && dist < 15 && level.getGameTime() % 80 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 15, 0, false, false));
                player.displayClientMessage(
                    Component.literal("⚫ TIME IS FRACTURING")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
            }
        }

        // All phases: accretion disk ambient particles
        if (level.getGameTime() % 10 == 0) {
            spawnAccretionDisk(level, arena);
        }

        // Phase 2+: Reality tear portals — random positions around the arena
        // (concept art: reality tear portals open mid-fight around the pillars)
        if (arena.bossPhase >= 2 && level.getGameTime() % 40 == 0) {
            int tearCount = arena.bossPhase >= 3 ? 5 : 3;
            for (int i = 0; i < tearCount; i++) {
                double angle  = level.random.nextDouble() * Math.PI * 2;
                double radius = 10 + level.random.nextDouble() * 25;
                double tx = arena.center.getX() + Math.cos(angle) * radius;
                double tz = arena.center.getZ() + Math.sin(angle) * radius;
                double ty = arena.center.getY() + 2 + level.random.nextDouble() * 8;
                // Reality tear: burst of reverse portal + singularity distort
                level.sendParticles((ParticleOptions) ModParticles.SINGULARITY_DISTORT.get(),
                    tx, ty, tz, 15, 1.0, 1.5, 1.0, 0.05);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    tx, ty, tz, 10, 0.5, 1.0, 0.5, 0.08);
                level.sendParticles(ParticleTypes.PORTAL,
                    tx, ty + 1, tz, 8, 0.3, 0.8, 0.3, 0.1);
            }
            // Play rift sound near a tear
            if (level.random.nextFloat() < 0.3f) {
                player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.HOSTILE, 0.6f, 0.4f);
            }
        }

        // Phase 2+: Energy beams between the 8 pillars (ring of cyan bolts)
        if (arena.bossPhase >= 2 && level.getGameTime() % 25 == 0) {
            spawnCenterBeams(level, arena, 8, arena.radius() * 0.75);
        }

        // Phase 3+: Pillar-to-center gravity beams
        if (arena.bossPhase >= 3 && level.getGameTime() % 15 == 0) {
            spawnCenterBeams(level, arena, 8, arena.radius() * 0.75);
            if (level.getGameTime() % 60 == 0) {
                player.displayClientMessage(
                    Component.literal("▼ GRAVITATIONAL COLLAPSE IMMINENT")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            }
        }
    }
    
    /**
     * Spawn energy bridge particles between pillars.
     * Uses custom particles for each arena type.
     */
    private static void spawnEnergyBridgeParticles(ServerLevel level, ActiveArena arena) {
        // Create particle lines between pillar positions
        int pillars = arena.type.getPillarCount();
        double radius = arena.radius() * 0.7;
        
        // Select particle type based on arena
        ParticleOptions bridgeParticle = switch (arena.type) {
            case QUANTUM_GUARDIAN      -> (ParticleOptions) ModParticles.ENERGY_BOLT.get();
            case VOID_HORROR           -> (ParticleOptions) ModParticles.VOID_WISP.get();
            case ENTANGLED_TWINS       -> (ParticleOptions) ModParticles.ENTANGLEMENT_LINK.get();
            case SINGULARITY_SOVEREIGN -> (ParticleOptions) ModParticles.ENTANGLEMENT_LINK.get();
        };

        ParticleOptions accentParticle = switch (arena.type) {
            case QUANTUM_GUARDIAN      -> (ParticleOptions) ModParticles.QUANTUM_SPARK.get();
            case VOID_HORROR           -> (ParticleOptions) ModParticles.RIFT_DISTORTION.get();
            case ENTANGLED_TWINS       -> (ParticleOptions) ModParticles.QUANTUM_PULSE.get();
            case SINGULARITY_SOVEREIGN -> (ParticleOptions) ModParticles.GRAVITY_PARTICLE.get();
        };
        
        for (int i = 0; i < pillars; i++) {
            double angle1 = (2 * Math.PI * i) / pillars;
            double angle2 = (2 * Math.PI * ((i + 1) % pillars)) / pillars;
            
            Vec3 pos1 = new Vec3(
                arena.center.getX() + Math.cos(angle1) * radius,
                arena.center.getY() + 10,
                arena.center.getZ() + Math.sin(angle1) * radius
            );
            Vec3 pos2 = new Vec3(
                arena.center.getX() + Math.cos(angle2) * radius,
                arena.center.getY() + 10,
                arena.center.getZ() + Math.sin(angle2) * radius
            );
            
            // Spawn particles along the bridge line
            Vec3 dir = pos2.subtract(pos1);
            int steps = (int) (dir.length() / 2);
            Vec3 step = dir.normalize().scale(2);
            
            Vec3 current = pos1;
            for (int j = 0; j < steps; j++) {
                // Main bridge particle
                level.sendParticles(bridgeParticle,
                    current.x, current.y, current.z,
                    1, 0, 0, 0, 0);
                
                // Accent particles every few steps
                if (j % 3 == 0) {
                    level.sendParticles(accentParticle,
                        current.x, current.y + 0.5, current.z,
                        1, 0.1, 0.1, 0.1, 0.02);
                }
                
                current = current.add(step);
            }
            
            // Pillar top glow effect
            spawnPillarTopEffect(level, arena, pos1, i);
        }
        
        // Center-to-pillar energy beams (Guardian only, phase 2+)
        if (arena.type == BossArenaStructure.ArenaType.QUANTUM_GUARDIAN && arena.bossPhase >= 2) {
            spawnCenterBeams(level, arena, pillars, radius);
        }

        // Void Horror: Floating spire particles
        if (arena.type == BossArenaStructure.ArenaType.VOID_HORROR) {
            spawnVoidHorrorAmbient(level, arena);
        }

        // Entangled Twins: Mirror pulse between the two platforms
        if (arena.type == BossArenaStructure.ArenaType.ENTANGLED_TWINS) {
            spawnTwinsMirrorPulse(level, arena);
        }

        // Sovereign: Accretion disk effect
        if (arena.type == BossArenaStructure.ArenaType.SINGULARITY_SOVEREIGN) {
            spawnAccretionDisk(level, arena);
        }
    }
    
    /**
     * Spawn pillar top glow effect.
     */
    private static void spawnPillarTopEffect(ServerLevel level, ActiveArena arena, Vec3 pillarPos, int index) {
        // Pulsing glow at pillar top
        double pulse = Math.sin(level.getGameTime() * 0.1 + index) * 0.5 + 0.5;
        int particleCount = (int) (2 + pulse * 3);
        
        ParticleOptions glowParticle = switch (arena.type) {
            case QUANTUM_GUARDIAN      -> (ParticleOptions) ModParticles.QUANTUM_PULSE.get();
            case VOID_HORROR           -> (ParticleOptions) ModParticles.VOID_FLAME.get();
            case ENTANGLED_TWINS       -> (ParticleOptions) ModParticles.ENTANGLEMENT_LINK.get();
            case SINGULARITY_SOVEREIGN -> (ParticleOptions) ModParticles.COSMIC_STAR.get();
        };
        
        level.sendParticles(glowParticle,
            pillarPos.x, pillarPos.y + 5, pillarPos.z,
            particleCount, 0.3, 0.5, 0.3, 0.01);
    }
    
    /**
     * Spawn center-to-pillar energy beams (Guardian phase 2+).
     */
    private static void spawnCenterBeams(ServerLevel level, ActiveArena arena, int pillars, double radius) {
        for (int i = 0; i < pillars; i++) {
            double angle = (2 * Math.PI * i) / pillars;
            Vec3 pillarPos = new Vec3(
                arena.center.getX() + Math.cos(angle) * radius,
                arena.center.getY() + 10,
                arena.center.getZ() + Math.sin(angle) * radius
            );
            
            Vec3 centerPos = Vec3.atCenterOf(arena.center).add(0, 5, 0);
            Vec3 dir = pillarPos.subtract(centerPos);
            int steps = (int) (dir.length() / 3);
            Vec3 step = dir.normalize().scale(3);
            
            Vec3 current = centerPos;
            for (int j = 0; j < steps; j++) {
                level.sendParticles((ParticleOptions) ModParticles.ENERGY_BOLT.get(),
                    current.x, current.y, current.z,
                    1, 0, 0, 0, 0);
                current = current.add(step);
            }
        }
    }
    
    /**
     * Spawn Void Horror ambient effects.
     */
    private static void spawnVoidHorrorAmbient(ServerLevel level, ActiveArena arena) {
        // Floating void wisps
        for (int i = 0; i < 5; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = 5 + level.random.nextDouble() * 20;
            double x = arena.center.getX() + Math.cos(angle) * dist;
            double z = arena.center.getZ() + Math.sin(angle) * dist;
            double y = arena.center.getY() + 2 + level.random.nextDouble() * 15;
            
            level.sendParticles((ParticleOptions) ModParticles.VOID_WISP.get(),
                x, y, z, 1, 0.5, 0.5, 0.5, 0.01);
        }
    }
    
    /**
     * Spawn Entangled Twins mirror pulse.
     * Two symmetric beams of entanglement particles arc between the twin platform centres,
     * visually reinforcing the quantum link between the two bosses.
     */
    private static void spawnTwinsMirrorPulse(ServerLevel level, ActiveArena arena) {
        double halfR = arena.radius() * 0.45;
        double time = level.getGameTime() * 0.07;

        // Twin platform centres
        Vec3 leftCenter  = new Vec3(arena.center.getX() - halfR, arena.center.getY() + 3, arena.center.getZ());
        Vec3 rightCenter = new Vec3(arena.center.getX() + halfR, arena.center.getY() + 3, arena.center.getZ());

        // Draw entanglement beam left → right
        Vec3 dir = rightCenter.subtract(leftCenter);
        int steps = (int) (dir.length() / 2);
        Vec3 step = dir.normalize().scale(2);
        Vec3 cur = leftCenter;
        for (int j = 0; j < steps; j++) {
            double wave = Math.sin(time + j * 0.4) * 1.5;
            level.sendParticles((ParticleOptions) ModParticles.ENTANGLEMENT_LINK.get(),
                cur.x, cur.y + wave, cur.z, 1, 0, 0, 0, 0);
            if (j % 4 == 0) {
                level.sendParticles((ParticleOptions) ModParticles.QUANTUM_PULSE.get(),
                    cur.x, cur.y + wave + 0.5, cur.z, 1, 0.2, 0.2, 0.2, 0.01);
            }
            cur = cur.add(step);
        }

        // Pulsing glow at each platform center
        for (Vec3 center : new Vec3[]{leftCenter, rightCenter}) {
            double pulse = Math.sin(time * 1.5) * 0.5 + 0.5;
            int count = (int) (3 + pulse * 4);
            level.sendParticles((ParticleOptions) ModParticles.ENTANGLEMENT_LINK.get(),
                center.x, center.y + 2, center.z, count, 1.0, 0.5, 1.0, 0.03);
        }
    }

    /**
     * Spawn Sovereign accretion disk effect.
     */
    private static void spawnAccretionDisk(ServerLevel level, ActiveArena arena) {
        // Rotating ring of particles
        double time = level.getGameTime() * 0.05;
        int ringCount = 3;
        
        for (int ring = 0; ring < ringCount; ring++) {
            double radius = 8 + ring * 6;
            int particles = 12 + ring * 4;
            
            for (int i = 0; i < particles; i++) {
                double angle = time * (1.0 - ring * 0.2) + (Math.PI * 2 * i / particles);
                double x = arena.center.getX() + Math.cos(angle) * radius;
                double z = arena.center.getZ() + Math.sin(angle) * radius;
                double y = arena.center.getY() + 3 + Math.sin(angle * 2 + time) * 0.5;
                
                level.sendParticles((ParticleOptions) ModParticles.GRAVITY_PARTICLE.get(),
                    x, y, z, 1, 0, 0, 0, 0);
            }
        }
        
        // Central singularity glow
        level.sendParticles((ParticleOptions) ModParticles.SINGULARITY_DISTORT.get(),
            arena.center.getX(), arena.center.getY() + 2, arena.center.getZ(),
            3, 1, 1, 1, 0.02);
    }
    
    /**
     * Update boss phase for an arena (called by boss entities).
     */
    public static void updateBossPhase(BlockPos center, int phase) {
        ActiveArena arena = ACTIVE_ARENAS.get(center);
        if (arena != null) {
            int oldPhase = arena.bossPhase;
            arena.bossPhase = phase;
            
            if (phase > oldPhase) {
                QuantumFalcon.LOGGER.info("Arena at {} entered phase {}", center, phase);
                
                // Notify nearby players of phase change mechanics
                if (arena.bossPhase >= 2) {
                    for (UUID playerId : PLAYERS_IN_ARENA.keySet()) {
                        if (PLAYERS_IN_ARENA.get(playerId).equals(center)) {
                            // Phase 2+ activates void center
                            ServerPlayer player = findPlayer(playerId);
                            if (player != null) {
                                player.displayClientMessage(
                                    Component.literal("⚠ VOID CENTER ACTIVATED!")
                                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
                                player.playNotifySound(com.matt.quantumfalcon.entity.util.QFMobSounds.get("singularity_pull"), SoundSource.HOSTILE, 0.8f, 1.0f);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Mark boss as spawned in arena.
     */
    public static void markBossSpawned(BlockPos center) { markBossSpawned(center, null); }

    public static void markBossSpawned(BlockPos center, java.util.UUID bossId) {
        ActiveArena arena = ACTIVE_ARENAS.get(center);
        if (arena != null) {
            arena.bossSpawned = true;
            arena.bossPhase = 1;
            if (bossId != null) {
                arena.bossId = bossId;
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) ArenaSavedData.get(server).put(arena);
            }
        }
    }

    public static ActiveArena arenaAt(BlockPos center) { return ACTIVE_ARENAS.get(center); }

    /** Test hook: forget that a player is inside an arena, as if they had walked out. */
    public static void testLeave(java.util.UUID player) { PLAYERS_IN_ARENA.remove(player); }
    
    /**
     * Find a player by UUID from the server.
     */
    private static ServerPlayer findPlayer(UUID playerId) {
        for (ActiveArena arena : ACTIVE_ARENAS.values()) {
            // This is a simplified lookup - in production you'd use server.getPlayerList()
            return null; // Placeholder - would need server reference
        }
        return null;
    }
    
    /**
     * Data class for active arena tracking.
     */
    public static class ActiveArena {
        public final BlockPos center;
        public final BossArenaStructure.ArenaType type;
        public boolean bossSpawned = false;
        /** the one boss this arena owns (persisted); a new one is summoned only when this one is gone. */
        public java.util.UUID bossId;
        public int bossPhase = 0;
        /** Dimension the arena lives in (null = legacy, matches any). */
        public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        public long activationTime;
        /** Play radius and entry margin. Legacy floating arenas use the type radius + 20; dungeon arenas (DungeonBuilder)
         * register their real floor radius and a 2-block margin so the dungeon outside the sealed door is not "in the arena". */
        public int radius = -1, margin = 20;
        public int radius() { return radius > 0 ? radius : type.getRadius(); }
        
        // Phase-locked mechanic flags
        public boolean voidCenterActive = false;
        public boolean energyBridgesActive = true;
        public boolean gravityWellActive = false;
        
        public ActiveArena(BlockPos center, BossArenaStructure.ArenaType type) {
            this.center = center;
            this.type = type;
            this.activationTime = System.currentTimeMillis();
        }
        
        /**
         * Check if void center should be active based on boss phase.
         * Phase 1: Void center OFF (learning phase)
         * Phase 2: Void center 50% power
         * Phase 3+: Void center FULL power
         */
        public boolean isVoidCenterActive() {
            return bossPhase >= 2;
        }
        
        /**
         * Get void pull strength based on phase.
         */
        public double getVoidPullStrength() {
            return switch (bossPhase) {
                case 0, 1 -> 0.0;      // No pull
                case 2 -> 0.08;        // Weak pull
                default -> 0.15;       // Full pull
            };
        }
        
        /**
         * Check if gravity wells are active.
         */
        public boolean isGravityWellActive() {
            return bossPhase >= 3 && type == BossArenaStructure.ArenaType.SINGULARITY_SOVEREIGN;
        }
    }
}
