package com.matt.quantumfalcon.event;

import com.matt.quantumfalcon.achievement.AchievementSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

/**
 * Quantum Storm Event - dangerous electrical storm with lightning strikes.
 * Purple lightning and quantum particles fill the sky. Dangerous but survivable.
 */
public class QuantumStormEvent extends DynamicEventManager.DynamicEvent {
    private static final int DURATION = 4800; // 4 minutes
    private static final int LIGHTNING_INTERVAL = 100; // Every 5 seconds

    public QuantumStormEvent() {
        super("Quantum Storm", DURATION);
    }

    @Override
    public void onStart(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal("⚡ A Quantum Storm approaches! Seek shelter! ⚡")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0f, 0.8f);
        }
    }

    @Override
    public void onTick(ServerLevel level) {
        // Spawn quantum particles around players
        if (elapsed % 5 == 0) {
            for (ServerPlayer player : level.players()) {
                // Purple portal particles
                double x = player.getX() + (level.random.nextDouble() - 0.5) * 30;
                double y = player.getY() + level.random.nextDouble() * 20;
                double z = player.getZ() + (level.random.nextDouble() - 0.5) * 30;
                
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 3, 0.5, 0.5, 0.5, 0.02);
            }
        }
        
        // Lightning strikes
        if (elapsed % LIGHTNING_INTERVAL == 0 && !level.players().isEmpty()) {
            ServerPlayer target = level.players().get(level.random.nextInt(level.players().size()));
            
            strikeNear(level, target.blockPosition());
        }
        
        // Apply resistance to players to help survive (make it fair)
        if (elapsed % 100 == 0) {
            for (ServerPlayer player : level.players()) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, false, false));
            }
        }
    }

    /**
     * one strike near a position. Lands on the surface (MOTION_BLOCKING heightmap, never inside a base or cave) and is
     * visual-only (no fire, no damage) unless EventSafety allows block changes there. Returns the strike position.
     */
    public static BlockPos strikeNear(ServerLevel level, BlockPos around) {
        int ox = level.random.nextInt(40) - 20, oz = level.random.nextInt(40) - 20;
        if (level.random.nextFloat() < 0.1f) { ox = level.random.nextInt(6) - 3; oz = level.random.nextInt(6) - 3; }
        BlockPos col = around.offset(ox, 0, oz);
        BlockPos strikePos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, col);
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        boolean visual = !EventSafety.mayAlter(level, strikePos);
        lastVisualOnly = visual;
        if (lightning != null) {
            lightning.moveTo(Vec3.atBottomCenterOf(strikePos));
            lightning.setVisualOnly(visual);
            level.addFreshEntity(lightning);
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, strikePos.getX(), strikePos.getY() + 1, strikePos.getZ(), 50, 1, 2, 1, 0.1);
        return strikePos;
    }

    /** For qftests: whether the last strike was visual-only. */
    public static boolean lastVisualOnly = true;

    @Override
    public void onEnd(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal("The Quantum Storm dissipates...")
                .withStyle(ChatFormatting.GRAY));
            
            // Grant achievement for surviving
            AchievementSystem.unlockAchievement(player, "survive_storm");
        }
    }
}
