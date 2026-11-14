package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: open the pedigree page for one falcon or egg. {@code genome} is FalconGenome.save(); {@code subject} is "falcon" or "egg". */
public record PedigreePayload(CompoundTag genome, String subject, String displayName, int pity) implements CustomPacketPayload {
    public static final Type<PedigreePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "pedigree"));
    public static final StreamCodec<FriendlyByteBuf, PedigreePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.COMPOUND_TAG, PedigreePayload::genome, ByteBufCodecs.STRING_UTF8, PedigreePayload::subject,
        ByteBufCodecs.STRING_UTF8, PedigreePayload::displayName, ByteBufCodecs.VAR_INT, PedigreePayload::pity, PedigreePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PedigreePayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.matt.quantumfalcon.client.gui.PedigreeScreen.open(p.genome(), p.subject(), p.displayName(), p.pity()));
    }
}
