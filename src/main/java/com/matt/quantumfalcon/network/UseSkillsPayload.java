package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Server -> client (A9): the player's use-skill XP; optionally a level-up toast and/or open the Skills page. */
public record UseSkillsPayload(List<Long> xp, int toastSkill, int toastLevel, boolean milestone, boolean open) implements CustomPacketPayload {
    public static final Type<UseSkillsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "use_skills"));
    public static final StreamCodec<FriendlyByteBuf, UseSkillsPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(16)), UseSkillsPayload::xp,
        ByteBufCodecs.VAR_INT, UseSkillsPayload::toastSkill, ByteBufCodecs.VAR_INT, UseSkillsPayload::toastLevel,
        ByteBufCodecs.BOOL, UseSkillsPayload::milestone, ByteBufCodecs.BOOL, UseSkillsPayload::open, UseSkillsPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(UseSkillsPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            com.matt.quantumfalcon.skills.UseSkillsClientState.set(p.xp());
            com.matt.quantumfalcon.client.gui.UseSkillsScreen.onSync(p.toastSkill(), p.toastLevel(), p.milestone(), p.open());
        });
    }
}
