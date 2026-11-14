package com.matt.quantumfalcon.network;

import com.matt.quantumfalcon.QuantumFalcon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public record SkillPointSyncPacket(
    int availablePoints,
    int spentPoints,
    Set<String> unlockedSkills,
    int highestHatched,
    int masteryMarks
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SkillPointSyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "skill_sync"));

    public static final StreamCodec<FriendlyByteBuf, SkillPointSyncPacket> CODEC = 
        StreamCodec.of(SkillPointSyncPacket::write, SkillPointSyncPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void write(FriendlyByteBuf buf, SkillPointSyncPacket packet) {
        buf.writeInt(packet.availablePoints);
        buf.writeInt(packet.spentPoints);
        buf.writeInt(packet.unlockedSkills.size());
        for (String skill : packet.unlockedSkills) {
            buf.writeUtf(skill);
        }
        buf.writeVarInt(packet.highestHatched);
        buf.writeVarInt(packet.masteryMarks);
    }

    public static SkillPointSyncPacket read(FriendlyByteBuf buf) {
        int available = buf.readInt();
        int spent = buf.readInt();
        int skillCount = buf.readInt();
        Set<String> skills = new HashSet<>();
        for (int i = 0; i < skillCount; i++) {
            skills.add(buf.readUtf());
        }
        return new SkillPointSyncPacket(available, spent, skills, buf.readVarInt(), buf.readVarInt());
    }
}
