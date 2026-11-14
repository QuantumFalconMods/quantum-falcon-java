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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Meteor Shower Event - rare nighttime event that drops valuable loot.
 * Meteors rain down from the sky, creating small craters with rare materials.
 */
public class MeteorShowerEvent extends DynamicEventManager.DynamicEvent {
    private static final int DURATION = 3600; // 3 minutes
    private static final int METEOR_INTERVAL = 50; // Every 2.5 seconds
    
    private int meteorCount = 0;

    public MeteorShowerEvent() {
        super("Meteor Shower", DURATION);
    }

    @Override
    public void onStart(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal("☄️ A meteor shower lights up the night sky! ☄️")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            player.playNotifySound(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.AMBIENT, 0.5f, 0.8f);
        }
    }

    @Override
    public void onTick(ServerLevel level) {
        if (elapsed % METEOR_INTERVAL == 0) {
            spawnMeteor(level);
        }
    }

    @Override
    public void onEnd(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal("The meteor shower ends. " + meteorCount + " meteors fell!")
                .withStyle(ChatFormatting.GRAY));
            
            // Grant achievement if they collected meteor loot
            if (meteorCount > 0) {
                AchievementSystem.unlockAchievement(player, "meteor_loot");
            }
        }
    }

    private void spawnMeteor(ServerLevel level) {
        if (level.players().isEmpty()) return;
        
        // Pick random player to target near
        ServerPlayer targetPlayer = level.players().get(level.random.nextInt(level.players().size()));
        
        // Random offset from player
        int offsetX = level.random.nextInt(60) - 30;
        int offsetZ = level.random.nextInt(60) - 30;
        BlockPos impactPos = targetPlayer.blockPosition().offset(offsetX, 0, offsetZ);
        impactPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, impactPos);
        
        // Create visual trail
        spawnMeteorTrail(level, impactPos);
        
        // Create impact
        createMeteorImpact(level, impactPos);
        
        meteorCount++;
    }

    private void spawnMeteorTrail(ServerLevel level, BlockPos impactPos) {
        // Particle trail from sky to ground
        for (int y = 100; y >= impactPos.getY(); y -= 2) {
            level.sendParticles(ParticleTypes.FLAME, 
                impactPos.getX() + 0.5, y, impactPos.getZ() + 0.5, 
                5, 0.3, 0.3, 0.3, 0.05);
            level.sendParticles(ParticleTypes.SMOKE, 
                impactPos.getX() + 0.5, y, impactPos.getZ() + 0.5, 
                3, 0.2, 0.2, 0.2, 0.02);
        }
    }

    public void createMeteorImpact(ServerLevel level, BlockPos impactPos) {
        // Sound
        level.playSound(null, impactPos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.0f, 0.8f);
        
        // Explosion particles
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, 
            impactPos.getX() + 0.5, impactPos.getY() + 0.5, impactPos.getZ() + 0.5, 
            1, 0, 0, 0, 0);
        
        // Small crater. only air/replaceable blocks, and only where EventSafety allows (config, mobGriefing, claims,
        // player builds). Otherwise the impact is sound, particles and loot only.
        if (EventSafety.mayAlter(level, impactPos)) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos craterPos = impactPos.offset(x, 0, z);
                    if (!level.getBlockState(craterPos).canBeReplaced()) continue;
                    if (x == 0 && z == 0 && level.random.nextFloat() < 0.5f) {
                        level.setBlock(craterPos, Blocks.LAVA.defaultBlockState(), 3);
                    } else if (level.random.nextFloat() < 0.3f && Blocks.FIRE.defaultBlockState().canSurvive(level, craterPos)) {
                        level.setBlock(craterPos, Blocks.FIRE.defaultBlockState(), 3);
                    }
                }
            }
        }
        
        // Drop loot
        dropMeteorLoot(level, impactPos);
    }

    private void dropMeteorLoot(ServerLevel level, BlockPos pos) {
        float roll = level.random.nextFloat();
        
        ItemStack loot;
        if (roll < 0.01f) {
            // 1% - Netherite scrap
            loot = new ItemStack(Items.NETHERITE_SCRAP, 1);
        } else if (roll < 0.05f) {
            // 4% - Diamonds
            loot = new ItemStack(Items.DIAMOND, level.random.nextInt(2) + 1);
        } else if (roll < 0.2f) {
            // 15% - Gold
            loot = new ItemStack(Items.GOLD_INGOT, level.random.nextInt(3) + 1);
        } else {
            // 80% - Iron
            loot = new ItemStack(Items.IRON_INGOT, level.random.nextInt(4) + 2);
        }
        
        ItemEntity itemEntity = new ItemEntity(level, 
            pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, loot);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
        
        // Sparkle effect on loot
        level.sendParticles(ParticleTypes.END_ROD, 
            pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 
            10, 0.3, 0.3, 0.3, 0.1);
    }
}
