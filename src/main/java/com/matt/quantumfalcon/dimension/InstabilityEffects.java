package com.matt.quantumfalcon.dimension;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.capability.PlayerInstabilityCapability;
import com.matt.quantumfalcon.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * THE instability system. There used to be two stacked ones: a wall-clock tier in InstabilityManager and this
 * positional meter; now this is the only one that hurts, and InstabilityManager is a thin compatibility layer over it.
 *
 * <ul>
 *   <li>Zone tier 0-3 comes from the distance to the player's arrival point in the Rift (their portal), not 0,0.
 *       Near the portal (under {@link #T1_DIST} blocks) is tier 0: no gain at all.</li>
 *   <li>The meter (0-100 %) rises only in tier 1-3 zones, per game tick (so it never runs while the game is paused),
 *       and drains in tier 0 zones, near an active Rift Stabilizer block, and outside the Rift.</li>
 *   <li>Below {@link #WARNING_THRESHOLD} % nothing happens to the player: that is the visible safe-time budget the HUD
 *       counts down. 50-75 %: Weakness. 75 %+: Slowness and light magic damage. 100 %: heavier damage.</li>
 *   <li>Respawning (e.g. at a bed in the Rift) clears the meter and gives a grace period, so there is no
 * respawn-into-damage loop. A Quantum Stabilizer clears it and holds it for 5 minutes.</li>
 * </ul>
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class InstabilityEffects {

    private static final int EFFECT_CHECK_INTERVAL = 20; // every second
    private static final int PARTICLE_INTERVAL = 10;
    private static final int WARNING_INTERVAL = 200;

    public static final float MAX_INSTABILITY = 100.0f;
    public static final float CRITICAL_THRESHOLD = 75.0f;
    public static final float WARNING_THRESHOLD = 50.0f;

    /** Zone bands, in blocks from the arrival point. */
    public static final int T1_DIST = 400, T2_DIST = 1200, T3_DIST = 2500;
    /** Gain per second by zone tier (%). T1 = 200 s safe, T2 = ~83 s, T3 = ~42 s from 0 to the 50 % line. */
    static final float[] GAIN_PER_SECOND = {0.0f, 0.25f, 0.6f, 1.2f};
    static final float DRAIN_SAFE = 2.0f, DRAIN_OUTSIDE = 5.0f, DRAIN_ARENA = 1.0f;
    public static final int RESPAWN_GRACE_TICKS = 20 * 30;
    public static final int STABILIZER_TICKS = 20 * 60 * 5;
    public static final int STABILIZER_BLOCK_RADIUS = 8;

    private static final Map<UUID, Long> lastWarningTime = new HashMap<>();
    private static final Map<UUID, Boolean> nearStabilizer = new HashMap<>();

    // ============ Zone tier ============

    /** Zone tier for a player: distance from their arrival anchor (their own portal). */
    public static int zoneTier(ServerPlayer player) {
        if (!EchoRiftDimension.isEchoRift(player.level())) return 0;
        PlayerInstabilityCapability cap = PlayerInstabilityCapability.get(player);
        if (!cap.hasAnchor()) cap.setAnchor(player.getBlockX(), player.getBlockZ());
        double dx = player.getX() - cap.getAnchorX(), dz = player.getZ() - cap.getAnchorZ();
        return tierForDistance(Math.sqrt(dx * dx + dz * dz));
    }

    /**
     * the zone tier of a position, for systems with no player of their own (nests, eggs, wild spawns). It uses
     * the anchor of the nearest player within 256 blocks; with nobody near (world generation) it falls back to the
     * distance from 0,0. Outside the Rift it is 0.
     */
    public static int zoneTierAt(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (!EchoRiftDimension.isEchoRift(level)) return 0;
        boolean mainThread = level.getServer() != null && level.getServer().isSameThread(); // worldgen threads: no player lookup
        Player near = mainThread ? level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 256, false) : null;
        if (near instanceof ServerPlayer sp) {
            PlayerInstabilityCapability cap = PlayerInstabilityCapability.get(sp);
            if (cap.hasAnchor()) {
                double dx = pos.getX() - cap.getAnchorX(), dz = pos.getZ() - cap.getAnchorZ();
                return tierForDistance(Math.sqrt(dx * dx + dz * dz));
            }
        }
        return tierForDistance(Math.sqrt((double) pos.getX() * pos.getX() + (double) pos.getZ() * pos.getZ()));
    }

    /** Distance from the player's arrival anchor in the Rift (0 outside it). */
    public static double anchorDistance(ServerPlayer player) {
        if (!EchoRiftDimension.isEchoRift(player.level())) return 0;
        PlayerInstabilityCapability cap = PlayerInstabilityCapability.get(player);
        if (!cap.hasAnchor()) return 0;
        double dx = player.getX() - cap.getAnchorX(), dz = player.getZ() - cap.getAnchorZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static int tierForDistance(double d) {
        return d < T1_DIST ? 0 : d < T2_DIST ? 1 : d < T3_DIST ? 2 : 3;
    }

    /**
     * Called when a player arrives in the Rift through a portal. only the FIRST arrival sets the anchor (it is
     * saved and copied on death); a portal built later 3,000 blocks out no longer turns that spot into zone 0.
     * Returns true if this arrival set the anchor.
     */
    public static boolean onArrive(ServerPlayer player) {
        PlayerInstabilityCapability cap = PlayerInstabilityCapability.get(player);
        if (cap.hasAnchor()) return false;
        cap.setAnchor(player.getBlockX(), player.getBlockZ());
        return true;
    }

    /** Seconds until the meter reaches the 50 % line at the current rate; -1 = not rising (safe). */
    public static int safeSeconds(ServerPlayer player, int tier) {
        PlayerInstabilityCapability cap = PlayerInstabilityCapability.get(player);
        float rate = inArena(player) ? 0f : gainPerTick(player, tier, cap) * 20f;
        if (rate <= 0f) return -1;
        float left = WARNING_THRESHOLD - cap.getInstability();
        if (left <= 0f) return 0;
        return (int) Math.ceil(left / rate) + cap.getGraceTicks() / 20;
    }

    static float gainPerTick(ServerPlayer player, int tier, PlayerInstabilityCapability cap) {
        if (tier <= 0 || isStabilized(player, cap)) return 0f;
        return GAIN_PER_SECOND[Math.min(tier, 3)] / 20.0f
            * com.matt.quantumfalcon.breeding.AbilityEngine.instabilityMultiplier(player)   // Rift Step falcon ability
            * (float) (1.0 - Math.min(0.6, com.matt.quantumfalcon.loot.sig.SignatureLoot.bonus(player, "instability_resist")))   // A8 Rift gear
            * com.matt.quantumfalcon.skills.UseSkills.instabilityMultiplier(player)   // A9 Arcana
            * com.matt.quantumfalcon.WorldConfig.instabilityRate()   // config
            * ("LIGHT".equals(com.matt.quantumfalcon.story.PlayerStoryData.get(player).getChosenEnding()) ? 0.75f : 1f);   // story: after Light the Rift holds you less
    }

    /** Inside an active boss arena the zone does not build passively: only the boss's own spikes do, and they drain. */
    static boolean inArena(ServerPlayer player) {
        return com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.getArenaAt(player.level(), player.blockPosition()) != null;
    }

    static boolean isStabilized(ServerPlayer player, PlayerInstabilityCapability cap) {
        return player.level().getGameTime() < cap.getStabilizedUntil() && cap.getStabilizedUntil() > 0 || nearStabilizer.getOrDefault(player.getUUID(), false);
    }

    // ============ Tick ============

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) return;
        tick(sp, level.getGameTime());
    }

    /** One game tick of the meter for a player (tests drive this directly with FakePlayers, which never tick). */
    public static void tick(ServerPlayer sp, long gameTime) {
        Level level = sp.level();
        PlayerInstabilityCapability cap = PlayerInstabilityCapability.get(sp);

        if (!EchoRiftDimension.isEchoRift(level)) {
            if (cap.getInstability() > 0) cap.reduceInstability(DRAIN_OUTSIDE / 20f);
            return;
        }
        if (sp.isCreative() || sp.isSpectator()) { cap.setCurrentTier(zoneTier(sp)); return; }

        if (gameTime % 20 == 0)
            nearStabilizer.put(sp.getUUID(), com.matt.quantumfalcon.block.RiftStabilizerBlock.isStabilized(level, sp.blockPosition(), STABILIZER_BLOCK_RADIUS));

        int tier = zoneTier(sp);
        cap.setCurrentTier(tier);
        boolean inArena = inArena(sp);
        float gain = inArena ? 0f : gainPerTick(sp, tier, cap);
        if (cap.getGraceTicks() > 0) { cap.setGraceTicks(cap.getGraceTicks() - 1); gain = 0f; }
        com.matt.quantumfalcon.skills.UseSkills.riftTick(sp, gain, gameTime);   // A9 Arcana XP (hourly cap)
        if (gain > 0f) cap.addInstability(gain);
        else if (cap.getInstability() > 0) cap.reduceInstability((tier == 0 || isStabilized(sp, cap) ? DRAIN_SAFE : inArena ? DRAIN_ARENA : 0f) / 20f);

        float instability = cap.getInstability();
        if (gameTime % EFFECT_CHECK_INTERVAL == 0) applyEffects(sp, instability);
        if (gameTime % PARTICLE_INTERVAL == 0) spawnInstabilityParticles(sp, tier, instability);
        if (instability >= WARNING_THRESHOLD && gameTime % WARNING_INTERVAL == 0) sendInstabilityWarning(sp, instability);
    }

    /** Respawn (bed or world spawn) clears the meter and grants a grace period: no respawn-into-damage loop. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerInstabilityCapability cap = PlayerInstabilityCapability.get(sp);
        cap.setInstability(0f);
        cap.setGraceTicks(RESPAWN_GRACE_TICKS);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastWarningTime.remove(event.getEntity().getUUID());
        nearStabilizer.remove(event.getEntity().getUUID());
    }

    public static float getPlayerInstability(Player player) {
        return PlayerInstabilityCapability.get(player).getInstability();
    }

    // ============ Effects (by meter, not by zone) ============

    private static void applyEffects(ServerPlayer player, float instability) {
        if (instability < WARNING_THRESHOLD) return;   // the safe budget: nothing happens below 50 %
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
        if (instability >= CRITICAL_THRESHOLD) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false));
            float dmg = instability >= MAX_INSTABILITY ? 1.5f : 0.5f;
            player.hurt(player.damageSources().magic(), dmg);
            if (instability >= MAX_INSTABILITY)
                player.displayClientMessage(Component.literal("§4§l⚠ INSTABILITY 100% - head back toward your portal or use a stabilizer"), true);
        }
    }

    // ============ Visual Effects ============

    private static void spawnInstabilityParticles(ServerPlayer player, int tier, float instability) {
        if (tier == 0 && instability < 10) return;
        ServerLevel level = player.serverLevel();
        Vec3 pos = player.position();
        int count = (int) (1 + instability / 25.0f);
        var type = switch (tier) {
            case 1 -> ParticleTypes.PORTAL;
            case 2 -> ParticleTypes.REVERSE_PORTAL;
            case 3 -> ParticleTypes.SOUL_FIRE_FLAME;
            default -> ParticleTypes.ENCHANT;
        };
        for (int i = 0; i < count; i++)
            level.sendParticles(type, pos.x + (level.random.nextDouble() - 0.5) * 2, pos.y + level.random.nextDouble() * 2,
                pos.z + (level.random.nextDouble() - 0.5) * 2, 1, 0, 0, 0, 0.05);
        if (instability >= CRITICAL_THRESHOLD)
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 1, pos.z, 3, 0.5, 0.5, 0.5, 0.1);
    }

    // ============ Warning ============

    private static void sendInstabilityWarning(ServerPlayer player, float instability) {
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        Long last = lastWarningTime.get(id);
        if (last != null && now - last < WARNING_INTERVAL) return;
        lastWarningTime.put(id, now);
        String warning, color;
        if (instability >= CRITICAL_THRESHOLD) {
            warning = "⚠ CRITICAL: reality is tearing. Head back toward your portal or use a stabilizer!";
            color = "§c";
            player.level().playSound(null, player.blockPosition(), ModSounds.INSTABILITY_CRITICAL.get(), SoundSource.AMBIENT, 1.0f, 1.0f);
        } else {
            warning = "⚡ Instability rising: you are far from your portal.";
            color = "§e";
            player.level().playSound(null, player.blockPosition(), ModSounds.INSTABILITY_WARNING.get(), SoundSource.AMBIENT, 0.7f, 1.0f);
        }
        player.displayClientMessage(Component.literal(color + warning + " §7[" + String.format("%.0f", instability) + "%]"), true);
    }

    // ============ Stabilizers ============

    /** Quantum Stabilizer item: clear the meter and hold it for 5 minutes. */
    public static void useStabilizerItem(ServerPlayer player) {
        PlayerInstabilityCapability cap = PlayerInstabilityCapability.get(player);
        cap.setInstability(0f);
        cap.setStabilizedUntil(player.level().getGameTime() + STABILIZER_TICKS);
    }

    public static void applyStabilizerEffect(ServerPlayer player, float reduction) {
        PlayerInstabilityCapability cap = PlayerInstabilityCapability.get(player);
        cap.setInstability(Math.max(0, cap.getInstability() - reduction));
        player.level().playSound(null, player.blockPosition(), ModSounds.STABILIZER_ACTIVATE.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
    }

    public static boolean hasStabilizerEquipped(Player player) {
        return player instanceof ServerPlayer sp && isStabilized(sp, PlayerInstabilityCapability.get(sp));
    }
}
