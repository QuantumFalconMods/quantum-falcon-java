package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.quest.PlayerQuestData;
import com.matt.quantumfalcon.skill.PlayerSkillData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Network Packets for Quantum Falcon multiplayer sync.
 * Handles synchronization of falcon state, quests, skills, and instability.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class ModNetworking {

    private static final Logger LOGGER = LogManager.getLogger();
    
    public static final String PROTOCOL_VERSION = "1.0";

    // ============ Packet Registration ============

    @SubscribeEvent
    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(QuantumFalcon.MOD_ID)
            .versioned(PROTOCOL_VERSION);
        
        // Boss music control (Server -> Client)
        registrar.playToClient(BossMusicPayload.TYPE, BossMusicPayload.CODEC, BossMusicPayload::handle);
        registrar.playToClient(UseSkillsPayload.TYPE, UseSkillsPayload.CODEC, UseSkillsPayload::handle);   // A9
        registrar.playToClient(CodexPayload.TYPE, CodexPayload.CODEC, CodexPayload::handle);
        registrar.playToClient(PedigreePayload.TYPE, PedigreePayload.CODEC, PedigreePayload::handle);
        registrar.playToClient(DangerTierPayload.TYPE, DangerTierPayload.CODEC, DangerTierPayload::handle);
        registrar.playToClient(CutscenePayload.TYPE, CutscenePayload.CODEC, CutscenePayload::handle);
        registrar.playToClient(com.matt.quantumfalcon.dungeon.WardSync.Payload.TYPE, com.matt.quantumfalcon.dungeon.WardSync.Payload.CODEC, com.matt.quantumfalcon.dungeon.WardSync.Payload::handle);
        registrar.playToClient(com.matt.quantumfalcon.dungeon.LoreBookPayload.TYPE, com.matt.quantumfalcon.dungeon.LoreBookPayload.CODEC, com.matt.quantumfalcon.dungeon.LoreBookPayload::handle);

        // Falcon State Sync (Server -> Client)
        registrar.playToClient(
            FalconStateSyncPacket.TYPE,
            FalconStateSyncPacket.CODEC,
            FalconStateSyncPacket::handleClient
        );
        
        // Falcon Command (Client -> Server)
        registrar.playToServer(
            com.matt.quantumfalcon.network.FalconCommandPacket.FalconCommandPayload.TYPE,
            com.matt.quantumfalcon.network.FalconCommandPacket.FalconCommandPayload.CODEC,
            com.matt.quantumfalcon.network.FalconCommandPacket.FalconCommandPayload::handle
        );
        
        // Skill Point Sync (Server -> Client)
        registrar.playToClient(
            SkillPointSyncPacket.TYPE,
            SkillPointSyncPacket.CODEC,
            ModNetworking::handleSkillPointSync
        );
        
        // Skill Unlock Request (Client -> Server)
        registrar.playToServer(
            com.matt.quantumfalcon.network.SkillUnlockPacket.TYPE,
            com.matt.quantumfalcon.network.SkillUnlockPacket.CODEC,
            com.matt.quantumfalcon.network.SkillUnlockPacket::handleServer
        );
        
        // Ending choice (Client -> Server),
        registrar.playToServer(EndingChoicePayload.TYPE, EndingChoicePayload.CODEC, EndingChoicePayload::handleServer);
        registrar.playToServer(CutsceneSkipPayload.TYPE, CutsceneSkipPayload.CODEC, CutsceneSkipPayload::handleServer);

        // Quest Accept (Client -> Server),
        registrar.playToServer(QuestAcceptPacket.TYPE, QuestAcceptPacket.CODEC, QuestAcceptPacket::handleServer);

        // Quest Progress Sync (Server -> Client)
        registrar.playToClient(
            QuestSyncPacket.TYPE,
            QuestSyncPacket.CODEC,
            QuestSyncPacket::handleClient
        );
        
        // Instability Sync (Server -> Client)
        registrar.playToClient(
            InstabilitySyncPacket.TYPE,
            InstabilitySyncPacket.CODEC,
            InstabilitySyncPacket::handleClient
        );
        
        // Bond Level Sync (Server -> Client)
        registrar.playToClient(
            BondLevelSyncPacket.TYPE,
            BondLevelSyncPacket.CODEC,
            BondLevelSyncPacket::handleClient
        );
        
        LOGGER.info("Registered {} network packets", 7);
    }

    // ============ Helper Methods ============

    /**
     * Sync falcon state to all tracking players
     */
    public static void syncFalconState(QuantumFalconEntity falcon) {
        if (falcon.level().isClientSide()) return;
        
        // Find variant index
        int variantIndex = 0;
        String currentVariant = falcon.getVariant();
        for (int i = 0; i < QuantumFalconEntity.VARIANTS.length; i++) {
            if (QuantumFalconEntity.VARIANTS[i].equals(currentVariant)) {
                variantIndex = i;
                break;
            }
        }
        
        FalconStateSyncPacket packet = new FalconStateSyncPacket(
            falcon.getId(),
            falcon.getBondLevel(),
            falcon.getHealth(),
            falcon.getMaxHealth(),
            falcon.getCommandMode().ordinal(),
            falcon.isAnomaly(),
            variantIndex
        );
        
        // Send to all tracking players
        PacketDistributor.sendToPlayersTrackingEntity(falcon, packet);
    }

    /**
     * Sync skill points to specific player
     */
    public static void syncSkillPoints(ServerPlayer player) {
        PlayerSkillData data = PlayerSkillData.get(player);
        if (data == null) return;
        
        SkillPointSyncPacket packet = new SkillPointSyncPacket(
            data.getAvailableSkillPoints(),
            data.getTotalSpentPoints(),
            data.getUnlockedSkills(),
            com.matt.quantumfalcon.skill.SkillPointSystem.highestTierHatched(player),
            data.getMasteryMarks()
        );
        
        PacketDistributor.sendToPlayer(player, packet);
    }

    /**
     * Sync quest progress to specific player
     */
    public static void syncQuestProgress(ServerPlayer player) {
        PlayerQuestData data = PlayerQuestData.get(player);
        if (data == null) return;
        
        QuestSyncPacket packet = new QuestSyncPacket(
            data.getActiveQuests(),
            data.getCompletedQuests(),
            data.getAllObjectiveProgress()
        );
        
        PacketDistributor.sendToPlayer(player, packet);
    }
    
    // ============ Packet Handlers ============
    
    public static void handleSkillPointSync(SkillPointSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Update client-side skill data
            ClientSkillData.setAvailablePoints(packet.availablePoints());
            ClientSkillData.setLineage(packet.highestHatched(), packet.masteryMarks());
            ClientSkillData.setSpentPoints(packet.spentPoints());
            ClientSkillData.setUnlockedSkills(packet.unlockedSkills());
        });
    }
}

// ============ Falcon State Sync Packet ============

record FalconStateSyncPacket(
    int entityId,
    int bondLevel,
    float health,
    float maxHealth,
    int commandMode,
    boolean isAnomaly,
    int variant
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FalconStateSyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "falcon_state"));

    public static final StreamCodec<FriendlyByteBuf, FalconStateSyncPacket> CODEC = 
        StreamCodec.of(FalconStateSyncPacket::write, FalconStateSyncPacket::read);

    public static FalconStateSyncPacket read(FriendlyByteBuf buf) {
        return new FalconStateSyncPacket(
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readVarInt()
        );
    }

    public static void write(FriendlyByteBuf buf, FalconStateSyncPacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.bondLevel);
        buf.writeFloat(packet.health);
        buf.writeFloat(packet.maxHealth);
        buf.writeVarInt(packet.commandMode);
        buf.writeBoolean(packet.isAnomaly);
        buf.writeVarInt(packet.variant);
    }

    public static void handleClient(FalconStateSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = context.player().level();
            Entity entity = level.getEntity(packet.entityId);
            if (entity instanceof QuantumFalconEntity falcon) {
                falcon.setBondLevelClient(packet.bondLevel);
                falcon.setHealthClient(packet.health);
                falcon.setCommandModeClient(packet.commandMode);
                falcon.setAnomalyClient(packet.isAnomaly);
                falcon.setVariantClient(packet.variant);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

// ============ Falcon Command Packet ============
// NOTE: FalconCommandPacket is now in its own file (FalconCommandPacket.java)
// This duplicate record definition has been removed to fix compilation errors

// ============ Skill Point Sync Packet ============
// NOTE: SkillPointSyncPacket is now in its own file (SkillPointSyncPacket.java)
// Handler is in ModNetworking class

// ============ Skill Unlock Request Packet ============
// Moved to separate file: SkillUnlockPacket.java

// ============ Quest Sync Packet ============

record QuestSyncPacket(
    java.util.List<String> activeQuests,
    java.util.List<String> completedQuests,
    Map<String, Map<Integer, Integer>> objectiveProgress
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuestSyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "quest_sync"));

    public static final StreamCodec<FriendlyByteBuf, QuestSyncPacket> CODEC = 
        StreamCodec.of(QuestSyncPacket::write, QuestSyncPacket::read);

    public static QuestSyncPacket read(FriendlyByteBuf buf) {
        // Read active quests
        int activeCount = buf.readVarInt();
        java.util.List<String> active = new java.util.ArrayList<>();
        for (int i = 0; i < activeCount; i++) {
            active.add(buf.readUtf());
        }
        
        // Read completed quests
        int completedCount = buf.readVarInt();
        java.util.List<String> completed = new java.util.ArrayList<>();
        for (int i = 0; i < completedCount; i++) {
            completed.add(buf.readUtf());
        }
        
        // Read objective progress
        int questCount = buf.readVarInt();
        Map<String, Map<Integer, Integer>> progress = new HashMap<>();
        for (int i = 0; i < questCount; i++) {
            String questId = buf.readUtf();
            int objectiveCount = buf.readVarInt();
            Map<Integer, Integer> objectives = new HashMap<>();
            for (int j = 0; j < objectiveCount; j++) {
                objectives.put(buf.readVarInt(), buf.readVarInt());
            }
            progress.put(questId, objectives);
        }
        
        return new QuestSyncPacket(active, completed, progress);
    }

    public static void write(FriendlyByteBuf buf, QuestSyncPacket packet) {
        // Write active quests
        buf.writeVarInt(packet.activeQuests.size());
        for (String quest : packet.activeQuests) {
            buf.writeUtf(quest);
        }
        
        // Write completed quests
        buf.writeVarInt(packet.completedQuests.size());
        for (String quest : packet.completedQuests) {
            buf.writeUtf(quest);
        }
        
        // Write objective progress
        buf.writeVarInt(packet.objectiveProgress.size());
        for (var entry : packet.objectiveProgress.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(entry.getValue().size());
            for (var obj : entry.getValue().entrySet()) {
                buf.writeVarInt(obj.getKey());
                buf.writeVarInt(obj.getValue());
            }
        }
    }

    public static void handleClient(QuestSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientQuestData.setActiveQuests(packet.activeQuests);
            ClientQuestData.setCompletedQuests(packet.completedQuests);
            ClientQuestData.setObjectiveProgress(packet.objectiveProgress);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

// ============ Instability Sync Packet ============

record InstabilitySyncPacket(
    float instability,
    int tier,
    int zoneTier,      // zone tier 0-3 from the arrival portal, -1 = not in the Rift
    int safeSeconds    // seconds until the 50 % line at the current rate, -1 = not rising
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InstabilitySyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "instability_sync"));

    public static final StreamCodec<FriendlyByteBuf, InstabilitySyncPacket> CODEC = 
        StreamCodec.of(InstabilitySyncPacket::write, InstabilitySyncPacket::read);

    public static InstabilitySyncPacket read(FriendlyByteBuf buf) {
        return new InstabilitySyncPacket(buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void write(FriendlyByteBuf buf, InstabilitySyncPacket packet) {
        buf.writeFloat(packet.instability);
        buf.writeVarInt(packet.tier);
        buf.writeVarInt(packet.zoneTier);
        buf.writeVarInt(packet.safeSeconds);
    }

    public static void handleClient(InstabilitySyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientInstabilityData.setInstability(packet.instability);
            ClientInstabilityData.setTier(packet.tier);
            float pct = Math.max(0f, Math.min(100f, packet.instability));
            InstabilityPackets.InstabilityClientData.update(pct, packet.tier, packet.tier >= 10 ? 1f : (pct % 10f) / 10f, "");
            InstabilityPackets.InstabilityClientData.updateZone(packet.zoneTier, packet.safeSeconds);
            QuantumFalcon.LOGGER.debug("[instability] client {}% tier {}", pct, packet.tier);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

// ============ Bond Level Sync Packet ============

record BondLevelSyncPacket(
    int entityId,
    int bondLevel,
    float bondProgress
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BondLevelSyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "bond_sync"));

    public static final StreamCodec<FriendlyByteBuf, BondLevelSyncPacket> CODEC = 
        StreamCodec.of(BondLevelSyncPacket::write, BondLevelSyncPacket::read);

    public static BondLevelSyncPacket read(FriendlyByteBuf buf) {
        return new BondLevelSyncPacket(buf.readVarInt(), buf.readVarInt(), buf.readFloat());
    }

    public static void write(FriendlyByteBuf buf, BondLevelSyncPacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.bondLevel);
        buf.writeFloat(packet.bondProgress);
    }

    public static void handleClient(BondLevelSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = context.player().level();
            Entity entity = level.getEntity(packet.entityId);
            if (entity instanceof QuantumFalconEntity falcon) {
                falcon.setBondLevelClient(packet.bondLevel);
                falcon.setBondProgressClient(packet.bondProgress);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

// ============ Client-Side Data Holders ============
// Moved to separate files: ClientSkillData.java and ClientQuestData.java

class ClientInstabilityData {
    private static float instability = 0.0f;
    private static int tier = 0;
    
    public static void setInstability(float value) { instability = value; }
    public static void setTier(int value) { tier = value; }
    public static float getInstability() { return instability; }
    public static int getTier() { return tier; }
}
