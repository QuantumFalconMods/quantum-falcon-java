package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network packet to open the story choice screen on the client.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class OpenStoryChoiceScreenPacket {
    
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(QuantumFalcon.MOD_ID);
        
        registrar.playToClient(
            OpenStoryChoiceScreenPayload.TYPE,
            OpenStoryChoiceScreenPayload.CODEC,
            OpenStoryChoiceScreenPayload::handle
        );
        
        QuantumFalcon.LOGGER.info("Registered story choice screen packet");
    }
    
    // ========================================
    // OPEN STORY CHOICE SCREEN PAYLOAD
    // ========================================
    
    /** trueUnlocked is decided on the server; the client never reads story data itself. */
    /** relics and bonds (0-4 each) let the screen show why the True path is locked without hovering. */
    public record OpenStoryChoiceScreenPayload(boolean trueUnlocked, int relics, int bonds) implements CustomPacketPayload {
        public static OpenStoryChoiceScreenPayload of(net.minecraft.server.level.ServerPlayer p) {
            return new OpenStoryChoiceScreenPayload(com.matt.quantumfalcon.story.StoryManager.canChooseTrue(p),
                com.matt.quantumfalcon.story.StoryManager.relicCount(p), com.matt.quantumfalcon.story.PlayerStoryData.get(p).maxBondTypeCount());
        }

        public static final Type<OpenStoryChoiceScreenPayload> TYPE = 
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "open_story_choice_screen"));
        
        public static final StreamCodec<FriendlyByteBuf, OpenStoryChoiceScreenPayload> CODEC = 
            StreamCodec.of((buf, p) -> { buf.writeBoolean(p.trueUnlocked()); buf.writeVarInt(p.relics()); buf.writeVarInt(p.bonds()); },
                buf -> new OpenStoryChoiceScreenPayload(buf.readBoolean(), buf.readVarInt(), buf.readVarInt()));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        
        public static void handle(OpenStoryChoiceScreenPayload payload, IPayloadContext context) {
            // Client-only code lives in ClientPayloadHandlers (dedicated-server safe)
            context.enqueueWork(() -> com.matt.quantumfalcon.client.network.ClientPayloadHandlers.openStoryChoiceScreen(payload.trueUnlocked(), payload.relics(), payload.bonds()));
        }
    }
}
