package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.quest.QuestTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** the J quest log's Accept button. The server validates (known quest, not done, prerequisite met) and syncs back. */
public record QuestAcceptPacket(String questId) implements CustomPacketPayload {
    public static final Type<QuestAcceptPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "quest_accept"));
    public static final StreamCodec<FriendlyByteBuf, QuestAcceptPacket> CODEC =
        StreamCodec.of((buf, p) -> buf.writeUtf(p.questId, 64), buf -> new QuestAcceptPacket(buf.readUtf(64)));

    public static void handleServer(QuestAcceptPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            String err = QuestTracker.tryAccept(player, packet.questId);
            if (err != null) player.displayClientMessage(Component.literal(err).withStyle(ChatFormatting.RED), true);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
