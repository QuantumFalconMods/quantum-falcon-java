package com.matt.quantumfalcon.leveling.network;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Random;

/**
 * Triggers level up visual effects on the client.
 * Spawns particles and plays sounds.
 */
public record LevelUpEffectPayload(
    double x,
    double y,
    double z,
    int newLevel,
    boolean isMilestone
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LevelUpEffectPayload> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "level_up_effect"));

    public static final StreamCodec<FriendlyByteBuf, LevelUpEffectPayload> CODEC = StreamCodec.of(
        LevelUpEffectPayload::write,
        LevelUpEffectPayload::read
    );

    private static LevelUpEffectPayload read(FriendlyByteBuf buf) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        int level = buf.readVarInt();
        boolean milestone = buf.readBoolean();
        return new LevelUpEffectPayload(x, y, z, level, milestone);
    }

    private static void write(FriendlyByteBuf buf, LevelUpEffectPayload payload) {
        buf.writeDouble(payload.x);
        buf.writeDouble(payload.y);
        buf.writeDouble(payload.z);
        buf.writeVarInt(payload.newLevel);
        buf.writeBoolean(payload.isMilestone);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handle packet on client side
     */
    public static void handle(LevelUpEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            handleClient(payload);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(LevelUpEffectPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Player player = mc.player;

        if (level == null || player == null) return;

        Random rand = new Random();

        // Base particle count - more for milestones
        int particleCount = payload.isMilestone ? 30 : 10;

        // Spawn cyan/purple particles
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * 1.5;
            double offsetY = rand.nextDouble() * 2.0;
            double offsetZ = (rand.nextDouble() - 0.5) * 1.5;

            double velX = (rand.nextDouble() - 0.5) * 0.3;
            double velY = rand.nextDouble() * 0.5 + 0.2;
            double velZ = (rand.nextDouble() - 0.5) * 0.3;

            // Use enchantment particles for magical effect
            level.addParticle(
                ParticleTypes.ENCHANT,
                payload.x + offsetX,
                payload.y + offsetY,
                payload.z + offsetZ,
                velX, velY, velZ
            );
        }

        // Extra sparkle particles for milestones
        if (payload.isMilestone) {
            for (int i = 0; i < 15; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double radius = rand.nextDouble() * 2;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;

                level.addParticle(
                    ParticleTypes.END_ROD,
                    payload.x + offsetX,
                    payload.y + rand.nextDouble() * 2,
                    payload.z + offsetZ,
                    0, 0.1, 0
                );
            }
        }

        // Play sound (client-side)
        float pitch = payload.isMilestone ? 1.5f : 1.2f;
        level.playLocalSound(
            payload.x, payload.y, payload.z,
            SoundEvents.PLAYER_LEVELUP,
            SoundSource.PLAYERS,
            0.5f, pitch, false
        );

        // Extra sound for milestones
        if (payload.isMilestone) {
            level.playLocalSound(
                payload.x, payload.y, payload.z,
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS,
                0.6f, 1.0f, false
            );
        }
    }

    /**
     * Create a level up effect payload for a player
     */
    public static LevelUpEffectPayload create(Player player, int newLevel) {
        boolean milestone = (newLevel == 10 || newLevel == 25 || newLevel == 50 || 
                            newLevel == 75 || newLevel == 100);
        return new LevelUpEffectPayload(
            player.getX(),
            player.getY(),
            player.getZ(),
            newLevel,
            milestone
        );
    }
}
