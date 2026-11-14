package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: the player crossed into an area with a different danger tier (1-7; 0 = no banner). */
public record DangerTierPayload(String area, int tier) implements CustomPacketPayload {
    public static final Type<DangerTierPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "danger_tier"));
    public static final StreamCodec<FriendlyByteBuf, DangerTierPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, DangerTierPayload::area, ByteBufCodecs.VAR_INT, DangerTierPayload::tier, DangerTierPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(DangerTierPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.matt.quantumfalcon.client.hud.DangerBannerOverlay.show(p.area(), p.tier()));
    }
}
