package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> client boss music control. {@code base} is the loop-A sound event path (e.g. "music_boss_overseer");
 * the client derives "_intro" and "_p2". Actions: INTRO (intro then loop A), LOOP_A / LOOP_B (late join, fade in),
 * PHASE2 (crossfade to loop B), STOP (fade out). Playback, fades and looping live in client.sound.BossMusicClient.
 */
public record BossMusicPayload(String base, byte action) implements CustomPacketPayload {
    public static final byte INTRO = 0, LOOP_A = 1, LOOP_B = 2, PHASE2 = 3, STOP = 4;
    public static final Type<BossMusicPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "boss_music"));
    public static final StreamCodec<FriendlyByteBuf, BossMusicPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, BossMusicPayload::base, ByteBufCodecs.BYTE, BossMusicPayload::action, BossMusicPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BossMusicPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.matt.quantumfalcon.client.sound.BossMusicClient.handle(p.base(), p.action()));
    }
}
