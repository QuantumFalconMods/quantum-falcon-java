package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.skill.PlayerSkillData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SkillUnlockPacket(
    String skillId
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SkillUnlockPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "skill_unlock"));

    public static final StreamCodec<FriendlyByteBuf, SkillUnlockPacket> CODEC = 
        StreamCodec.of(SkillUnlockPacket::write, SkillUnlockPacket::read);

    public static SkillUnlockPacket read(FriendlyByteBuf buf) {
        return new SkillUnlockPacket(buf.readUtf());
    }

    public static final String MASTERY_MARK = "mastery_mark";

    public static void write(FriendlyByteBuf buf, SkillUnlockPacket packet) {
        buf.writeUtf(packet.skillId);
    }

    public static void handleServer(SkillUnlockPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            
            PlayerSkillData data = PlayerSkillData.get(player);
            if (data == null) return;
            
            // "mastery_mark" buys the cosmetic sink; anything else is a node. Failures say why (W3).
            if (MASTERY_MARK.equals(packet.skillId)) {
                if (data.buyMasteryMark()) player.displayClientMessage(net.minecraft.network.chat.Component.literal("Mastery Mark " + data.getMasteryMarks() + " earned").withStyle(net.minecraft.ChatFormatting.GOLD), true);
                else player.displayClientMessage(net.minecraft.network.chat.Component.literal("Mastery Marks open once every node is owned").withStyle(net.minecraft.ChatFormatting.RED), true);
            } else {
                String why = data.unlockReason(player, packet.skillId);
                if (why == null && data.tryUnlockSkill(player, packet.skillId)) {
                    var sk = com.matt.quantumfalcon.skill.SkillTreeSystem.getSkill(packet.skillId);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("Unlocked: " + sk.name).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                    player.level().playSound(null, player.blockPosition(), com.matt.quantumfalcon.registry.ModSounds.UI_SKILL_UNLOCK.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.2f);
                } else {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(why == null ? "Can't unlock that" : why).withStyle(net.minecraft.ChatFormatting.RED), true);
                }
            }
            com.matt.quantumfalcon.network.ModNetworking.syncSkillPoints(player);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
