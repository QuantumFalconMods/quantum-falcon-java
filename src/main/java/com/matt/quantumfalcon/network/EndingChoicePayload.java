package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.story.StoryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** the ending pick from StoryChoiceScreen. StoryManager.executeEnding validates (Sovereign defeated, no ending yet, True gate). */
public record EndingChoicePayload(String ending) implements CustomPacketPayload {
    public static final Type<EndingChoicePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "ending_choice"));
    public static final StreamCodec<FriendlyByteBuf, EndingChoicePayload> CODEC =
        StreamCodec.of((buf, p) -> buf.writeUtf(p.ending, 16), buf -> new EndingChoicePayload(buf.readUtf(16)));

    public static void handleServer(EndingChoicePayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            StoryManager.Ending e;
            try { e = StoryManager.Ending.valueOf(p.ending); } catch (IllegalArgumentException ex) { return; }
            StoryManager.executeEnding(sp, e);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
