package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.FalconCommand;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network packet for sending falcon commands from client to server.
 * Updates falcon AI behavior based on player command selection.
 * Registration is done in ModNetworking.registerPackets() to avoid duplicate payload id.
 */
public class FalconCommandPacket {
    
    // ========================================
    // FALCON COMMAND PAYLOAD
    // ========================================
    
    public record FalconCommandPayload(int falconEntityId, String commandName) implements CustomPacketPayload {
        public static final Type<FalconCommandPayload> TYPE = 
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "falcon_command"));
        
        public static final StreamCodec<FriendlyByteBuf, FalconCommandPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, FalconCommandPayload::falconEntityId,
            ByteBufCodecs.STRING_UTF8, FalconCommandPayload::commandName,
            FalconCommandPayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        
        public static void handle(FalconCommandPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                    return;
                }
                
                // Find the falcon entity
                if (serverPlayer.level().getEntity(payload.falconEntityId()) instanceof QuantumFalconEntity falcon) {
                    // Verify ownership
                    if (!falcon.isTame() || !falcon.isOwnedBy(serverPlayer)) {
                        QuantumFalcon.LOGGER.warn("Player {} tried to command non-owned falcon {}", 
                            serverPlayer.getName().getString(), payload.falconEntityId());
                        return;
                    }
                    
                    // Parse command
                    FalconCommand command;
                    try {
                        command = FalconCommand.valueOf(payload.commandName());
                    } catch (IllegalArgumentException e) {
                        QuantumFalcon.LOGGER.warn("Invalid falcon command: {}", payload.commandName());
                        return;
                    }
                    
                    if (command == FalconCommand.ABILITY) {
                        // Variant active ability; FalconAbilities sends its own actionbar feedback
                        com.matt.quantumfalcon.entity.ability.FalconAbilities.activate(falcon, serverPlayer);
                        return;
                    }
                    // Apply command to falcon
                    falcon.setCommand(command);
                    
                    // Send feedback to player
                    serverPlayer.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("✓ Falcon command: " + command.getName())
                            .withStyle(command.getColor()), true);
                    
                    QuantumFalcon.LOGGER.debug("Player {} set falcon {} command to {}", 
                        serverPlayer.getName().getString(), payload.falconEntityId(), command.getName());
                } else {
                    QuantumFalcon.LOGGER.warn("Falcon entity {} not found for command", payload.falconEntityId());
                }
            });
        }
    }
}
