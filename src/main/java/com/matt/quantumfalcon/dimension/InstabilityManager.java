package com.matt.quantumfalcon.dimension;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player instability in Echo Dimensions.
 * Instability increases over time, causing effects, spawning enemies, and environmental hazards.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class InstabilityManager {
    // Player tracking
    private static final Map<UUID, PlayerEchoData> PLAYER_ECHO_DATA = new ConcurrentHashMap<>();
    
    
    // Biome instability tags (Bumblezone pattern)
    public static final TagKey<Biome> LOW_INSTABILITY = TagKey.create(Registries.BIOME,
        ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "low_instability"));
    public static final TagKey<Biome> MEDIUM_INSTABILITY = TagKey.create(Registries.BIOME,
        ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "medium_instability"));
    public static final TagKey<Biome> HIGH_INSTABILITY = TagKey.create(Registries.BIOME,
        ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "high_instability"));
    public static final TagKey<Biome> EXTREME_INSTABILITY = TagKey.create(Registries.BIOME,
        ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "extreme_instability"));
    
    // Gravity well biomes (pull player toward singularities)
    public static final TagKey<Biome> GRAVITY_WELL = TagKey.create(Registries.BIOME,
        ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "gravity_well"));
    
    // Gravity well constants
    private static final double GRAVITY_PULL_STRENGTH = 0.08;
    private static final int GRAVITY_PARTICLE_INTERVAL = 10;
    
    /**
     * Get instability multiplier for a biome.
     * Low = 1.0x, Medium = 1.5x, High = 2.5x, Extreme = 5.0x
     */
    public static float getBiomeInstabilityMultiplier(Holder<Biome> biomeHolder) {
        if (biomeHolder.is(EXTREME_INSTABILITY)) return 5.0f;
        if (biomeHolder.is(HIGH_INSTABILITY)) return 2.5f;
        if (biomeHolder.is(MEDIUM_INSTABILITY)) return 1.5f;
        if (biomeHolder.is(LOW_INSTABILITY)) return 1.0f;
        return 0.5f; // Non-quantum biomes have reduced instability
    }

    /**
     * Called when a player arrives in an echo dimension. this is no longer a second instability system; it only
     * sets the player's anchor (their arrival point, which is what zone tiers are measured from) and says so.
     */
    public static void onPlayerEnterEcho(Player player, int baseInstability) {
        PlayerEchoData data = new PlayerEchoData();
        data.enterTick = player.level().getGameTime();
        PLAYER_ECHO_DATA.put(player.getUUID(), data);
        boolean first = player instanceof ServerPlayer sp && EchoRiftDimension.isEchoRift(sp.level()) && InstabilityEffects.onArrive(sp);
        player.displayClientMessage(Component.literal(first
                ? "⚠ You have entered the Echo Rift. Near this first portal is safe; instability builds the farther you roam."
                : "⚠ You have entered the Echo Rift. The safe zone stays around your first portal.")
            .withStyle(ChatFormatting.YELLOW), false);
    }

    public static void onPlayerExitEcho(Player player) {
        PLAYER_ECHO_DATA.remove(player.getUUID());
    }

    /** Biome mechanics only (gravity wells) and Echo Phantoms driven by the one meter (InstabilityEffects). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        if (!EchoDimensionRegistry.isInEchoDimension(serverLevel)) {
            if (PLAYER_ECHO_DATA.containsKey(player.getUUID())) onPlayerExitEcho(player);
            return;
        }
        PlayerEchoData data = PLAYER_ECHO_DATA.get(player.getUUID());
        if (data == null) {   // logged in inside the Rift: keep the saved anchor
            data = new PlayerEchoData();
            data.enterTick = serverLevel.getGameTime();
            PLAYER_ECHO_DATA.put(player.getUUID(), data);
        }
        int tier = data.getCurrentTier(serverPlayer);
        if (serverLevel.getBiome(player.blockPosition()).is(GRAVITY_WELL)) applyGravityWellEffect(serverPlayer, serverLevel, tier);
        if (serverLevel.getGameTime() % 100L == 0L && tier >= 8 && !serverPlayer.isCreative()) {
            data.echoPhantomCount = (int) serverLevel.getEntitiesOfClass(net.minecraft.world.entity.monster.Phantom.class, player.getBoundingBox().inflate(40)).size();
            if (data.echoPhantomCount < tier - 7 && serverLevel.random.nextFloat() < 0.3f) spawnEchoPhantom(serverPlayer, serverLevel, data);
        }
    }

    private static void applyGravityWellEffect(ServerPlayer player, ServerLevel level, int tier) {
        double pullStrength = GRAVITY_PULL_STRENGTH * (1.0 + tier * 0.1);
        if (!player.getAbilities().flying && !player.isCreative()) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, -pullStrength, 0));
            player.hurtMarked = true;
        }
        if (level.getGameTime() % GRAVITY_PARTICLE_INTERVAL == 0L) {
            for (int i = 0; i < 5; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2, radius = 2.0 + level.random.nextDouble() * 3.0;
                double px = player.getX() + Math.cos(angle) * radius, pz = player.getZ() + Math.sin(angle) * radius, py = player.getY() + level.random.nextDouble() * 2.0;
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 1, (player.getX() - px) * 0.1, (player.getY() - py) * 0.1, (player.getZ() - pz) * 0.1, 0.05);
            }
        }
    }

    private static void spawnEchoPhantom(ServerPlayer player, ServerLevel level, PlayerEchoData data) {
        double angle = level.random.nextDouble() * Math.PI * 2.0, distance = 15.0 + level.random.nextDouble() * 10.0;
        BlockPos spawnPos = player.blockPosition().offset((int) (Math.cos(angle) * distance), 5 + level.random.nextInt(5), (int) (Math.sin(angle) * distance));
        var phantom = net.minecraft.world.entity.EntityType.PHANTOM.create(level);
        if (phantom == null) return;
        phantom.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        level.addFreshEntity(phantom);
        data.echoPhantomCount++;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.1);
        player.playNotifySound(com.matt.quantumfalcon.entity.util.QFMobSounds.get("rift_open"), SoundSource.HOSTILE, 0.9f, 0.8f);
    }

    // === Data Access ===

    public static PlayerEchoData getPlayerData(Player player) { return PLAYER_ECHO_DATA.get(player.getUUID()); }
    public static boolean isPlayerInEcho(Player player) { return PLAYER_ECHO_DATA.containsKey(player.getUUID()); }
    public static void cleanup(UUID playerUUID) { PLAYER_ECHO_DATA.remove(playerUUID); }

    // === Inner Classes ===

    /**
     * Boss and arena spikes add straight to the one meter, in percent. They drain again once the fight is over.
     */
    public static void addInstability(ServerPlayer player, float percent) {
        com.matt.quantumfalcon.capability.PlayerInstabilityCapability.get(player).addInstability(percent);
    }

    public static class PlayerEchoData {
        public long enterTick = 0L;
        public int echoPhantomCount = 0;

        /** 0-10, derived from the one meter (10 % per step). */
        public int getCurrentTier(ServerPlayer player) {
            return InstabilitySync.tierOf(com.matt.quantumfalcon.capability.PlayerInstabilityCapability.get(player).getInstability());
        }
    }
}
