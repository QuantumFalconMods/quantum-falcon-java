package com.matt.quantumfalcon.leveling.network;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Handles registration of network packets for the leveling system.
 * NeoForge 1.21.3 uses payload-based networking.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class LevelingPacketHandler {

    public static final ResourceLocation SYNC_LEVEL_DATA_ID = 
        ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "sync_level_data");
    
    public static final ResourceLocation LEVEL_UP_EFFECT_ID = 
        ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "level_up_effect");

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(QuantumFalcon.MOD_ID);

        // Register sync packet (server -> client)
        registrar.playToClient(
            SyncLevelDataPayload.TYPE,
            SyncLevelDataPayload.CODEC,
            SyncLevelDataPayload::handle
        );

        // Register level up effect packet (server -> client)
        registrar.playToClient(
            LevelUpEffectPayload.TYPE,
            LevelUpEffectPayload.CODEC,
            LevelUpEffectPayload::handle
        );

        QuantumFalcon.LOGGER.info("Registered leveling system network packets");
    }
}
