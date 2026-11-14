package com.matt.quantumfalcon.dungeon;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Server -> client: show a lore fragment in the vanilla book view. */
public record LoreBookPayload(String title, List<String> pages) implements CustomPacketPayload {
    public static final Type<LoreBookPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "lore_book"));
    public static final StreamCodec<FriendlyByteBuf, LoreBookPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, LoreBookPayload::title,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(32)), LoreBookPayload::pages, LoreBookPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LoreBookPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.matt.quantumfalcon.client.gui.LoreBookScreenOpener.open(p.title(), p.pages()));
    }
}
