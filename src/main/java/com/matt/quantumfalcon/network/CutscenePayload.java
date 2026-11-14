package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> client: play a boss cutscene. {@code kind} 0 intro, 1 phase shift, 2 death. The focus point and boss height
 * travel with the packet so the death shot still works after the boss entity is gone.
 */
public record CutscenePayload(int entityId, byte kind, int ticks, double x, double y, double z, float height, int rgb) implements CustomPacketPayload {
    public static final byte INTRO = 0, PHASE = 1, DEATH = 2;
    public static final Type<CutscenePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "cutscene"));
    public static final StreamCodec<FriendlyByteBuf, CutscenePayload> CODEC = StreamCodec.of((buf, p) -> {
        buf.writeVarInt(p.entityId); buf.writeByte(p.kind); buf.writeVarInt(p.ticks);
        buf.writeDouble(p.x); buf.writeDouble(p.y); buf.writeDouble(p.z); buf.writeFloat(p.height); buf.writeInt(p.rgb);
    }, buf -> new CutscenePayload(buf.readVarInt(), buf.readByte(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(CutscenePayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.matt.quantumfalcon.client.cutscene.CutsceneClient.play(p));
    }
}
