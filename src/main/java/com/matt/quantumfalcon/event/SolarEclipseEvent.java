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
 * Solar Eclipse Event - daytime event that temporarily darkens the world.
 * Hostile mobs can spawn during the eclipse. Grants night vision to help navigate.
 */
public class SolarEclipseEvent extends DynamicEventManager.DynamicEvent {
    private static final int DURATION = 4000; // ~3.3 minutes

    public SolarEclipseEvent() {
        super("Solar Eclipse", DURATION);
    }

    @Override
    public void onStart(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal("🌑 A Solar Eclipse begins... darkness falls! 🌑")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
            player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0f, 0.5f);
            
            // Grant night vision to help see
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION + 100, 0, false, false));
        }
    }

    @Override
    public void onTick(ServerLevel level) {
        // Dark particles to create eclipse atmosphere
        if (elapsed % 20 == 0) {
            for (ServerPlayer player : level.players()) {
                // Dark smoke particles around the sun position (high up)
                double x = player.getX() + (level.random.nextDouble() - 0.5) * 100;
                double y = player.getY() + 150 + level.random.nextDouble() * 30;
                double z = player.getZ() + (level.random.nextDouble() - 0.5) * 100;
                
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 5, 5, 2, 5, 0.01);
                
                // Corona effect - occasional bright particles around the dark center
                if (level.random.nextFloat() < 0.3f) {
                    level.sendParticles(ParticleTypes.END_ROD, 
                        x + (level.random.nextDouble() - 0.5) * 20, 
                        y, 
                        z + (level.random.nextDouble() - 0.5) * 20, 
                        1, 0, 0, 0, 0.01);
                }
            }
        }
        
        // Refresh night vision during eclipse
        if (elapsed % 200 == 0) {
            for (ServerPlayer player : level.players()) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false));
            }
        }
        
        // Occasional eerie sounds
        if (elapsed % 300 == 0) {
            for (ServerPlayer player : level.players()) {
                player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.3f, 0.7f);
            }
        }
    }

    @Override
    public void onEnd(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal("The Solar Eclipse ends. Light returns to the world.")
                .withStyle(ChatFormatting.YELLOW));
            
            // Clear the night vision early
            player.removeEffect(MobEffects.NIGHT_VISION);
            
            // Grant achievement for all events
            if (AchievementSystem.hasAchievement(player, "see_aurora") &&
                AchievementSystem.hasAchievement(player, "survive_storm") &&
                AchievementSystem.hasAchievement(player, "meteor_loot")) {
                AchievementSystem.unlockAchievement(player, "all_events");
            }
        }
    }
}
