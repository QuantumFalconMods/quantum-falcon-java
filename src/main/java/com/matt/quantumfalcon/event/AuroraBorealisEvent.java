package com.matt.quantumfalcon.event;

import com.matt.quantumfalcon.achievement.AchievementSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Aurora Borealis Event - beautiful nighttime event that grants luck buff.
 * Northern lights dance across the sky, granting players temporary luck.
 */
public class AuroraBorealisEvent extends DynamicEventManager.DynamicEvent {
    private static final int DURATION = 6000; // 5 minutes

    public AuroraBorealisEvent() {
        super("Aurora Borealis", DURATION);
    }

    @Override
    public void onStart(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal("✨ The Aurora Borealis illuminates the night sky! ✨")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.5f, 1.0f);
            
            // Grant achievement
            AchievementSystem.unlockAchievement(player, "see_aurora");
        }
    }

    @Override
    public void onTick(ServerLevel level) {
        // Spawn aurora particles above players every 10 ticks
        if (elapsed % 10 == 0) {
            for (ServerPlayer player : level.players()) {
                // Grant luck buff
                if (elapsed % 200 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.LUCK, 220, 0, false, false));
                }
                
                // Spawn colorful particles high in the sky
                double x = player.getX() + (level.random.nextDouble() - 0.5) * 60;
                double y = player.getY() + 80 + level.random.nextDouble() * 40;
                double z = player.getZ() + (level.random.nextDouble() - 0.5) * 60;
                
                // Alternate colors - end rod (white), glow (green-ish effect)
                if (level.random.nextBoolean()) {
                    level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0.1, 0, 0.01);
                } else {
                    level.sendParticles(ParticleTypes.GLOW, x, y, z, 1, 0, 0.1, 0, 0.01);
                }
            }
        }
    }

    @Override
    public void onEnd(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal("The Aurora Borealis fades into the night...")
                .withStyle(ChatFormatting.GRAY));
        }
    }
}
