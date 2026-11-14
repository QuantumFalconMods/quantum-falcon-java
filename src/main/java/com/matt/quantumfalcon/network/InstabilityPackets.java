package com.matt.quantumfalcon.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Helper class for sending instability sync packets.
 */
public class InstabilityPackets {

    /**
     * Sync instability data to a specific client
     */
    public static void syncToClient(ServerPlayer player, float instability, int tier) { syncToClient(player, instability, tier, -1, -1); }

    public static void syncToClient(ServerPlayer player, float instability, int tier, int zoneTier, int safeSeconds) {
        InstabilitySyncPacket packet = new InstabilitySyncPacket(instability, tier, zoneTier, safeSeconds);
        PacketDistributor.sendToPlayer(player, packet);
    }

    /**
     * Sync instability data to all players in a level
     */
    public static void syncToAll(net.minecraft.server.level.ServerLevel level, float instability, int tier) {
        InstabilitySyncPacket packet = new InstabilitySyncPacket(instability, tier, -1, -1);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }
    
    // ============ Client-Side Data Holders ============
    
    /**
     * Client-side holder for instability data
     */
    public static class InstabilityClientData {
        private static float currentInstability = 0.0f;
        private static int currentTier = 0;
        private static float currentProgress = 0.0f;
        private static String currentBiome = "";
        private static int zoneTier = -1, safeSeconds = -1;
        private static float instability = 0f;

        public static void updateZone(int zone, int safe) { zoneTier = zone; safeSeconds = safe; }
        public static int getZoneTier() { return zoneTier; }
        public static int getSafeSeconds() { return safeSeconds; }
        public static float getInstability() { return instability; }
        
        public static void update(float instability, int tier, float progress, String biome) {
            currentInstability = instability;
            InstabilityClientData.instability = instability;
            currentTier = tier;
            currentProgress = progress;
            currentBiome = biome;
        }
        
        public static int getCurrentTier() {
            return currentTier;
        }
        
        public static float getCurrentProgress() {
            return currentProgress;
        }
        
        public static String getCurrentBiome() {
            return currentBiome;
        }
    }
    
    /**
     * Client-side holder for arena warning data
     */
    public static class ArenaWarningClientData {
        private static boolean hasWarning = false;
        private static String currentWarning = "";
        
        public static void setWarning(String warning) {
            currentWarning = warning;
            hasWarning = warning != null && !warning.isEmpty();
        }
        
        public static void clear() {
            currentWarning = "";
            hasWarning = false;
        }
        
        public static boolean hasActiveWarning() {
            return hasWarning;
        }
        
        public static String getCurrentWarning() {
            return currentWarning;
        }
    }
    
    /**
     * Client-side holder for boss phase data
     */
    public static class BossPhaseClientData {
        private static int lastPhase = 0;
        private static String lastBossType = "";
        
        public static void update(int phase, String bossType) {
            lastPhase = phase;
            lastBossType = bossType;
        }
        
        public static void clear() {
            lastPhase = 0;
            lastBossType = "";
        }
        
        public static int getLastPhase() {
            return lastPhase;
        }
        
        public static String getLastBossType() {
            return lastBossType;
        }
    }
}
