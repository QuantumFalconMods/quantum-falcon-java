package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Server -> client: open the Falconer's Codex with the player's discovered ids ("variant:x", "hybrid:x", "ability:x"...). */
public record CodexPayload(List<String> found, int chapter, int page) implements CustomPacketPayload {
    public static final Type<CodexPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "codex"));
    public static final StreamCodec<FriendlyByteBuf, CodexPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(1024)), CodexPayload::found,
        ByteBufCodecs.VAR_INT, CodexPayload::chapter, ByteBufCodecs.VAR_INT, CodexPayload::page, CodexPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(CodexPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.matt.quantumfalcon.client.gui.CodexScreen.open(p.found(), p.chapter(), p.page()));
    }
}
