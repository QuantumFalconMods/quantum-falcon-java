package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.boss.BossCutscenes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent when a player skips a boss cutscene: only that player's cutscene protection ends; the boss stays frozen for the rest. */
public record CutsceneSkipPayload(int bossId) implements CustomPacketPayload {
    public static final Type<CutsceneSkipPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "cutscene_skip"));
    public static final StreamCodec<FriendlyByteBuf, CutsceneSkipPayload> CODEC =
        StreamCodec.of((buf, p) -> buf.writeVarInt(p.bossId), buf -> new CutsceneSkipPayload(buf.readVarInt()));

    public static void handleServer(CutsceneSkipPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) BossCutscenes.skipped(sp); });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
