package com.matt.quantumfalcon.hytale;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Quantum Grapple - Enhanced version with:
 * - 40 block range (2x normal)
 * - Falcon boost when companion nearby (+50% speed)
 * - No durability loss
 * - Shorter cooldown
 * - Quantum particle trail
 */
public class QuantumGrappleItem extends Item {
    // Enhanced stats
    private static final double MAX_RANGE = 40.0;
    private static final double PULL_STRENGTH = 2.5;
    private static final double UPWARD_BOOST = 0.4;
    private static final double FALCON_BOOST_MULTIPLIER = 1.5;
    private static final double FALCON_DETECTION_RADIUS = 20.0;
    private static final int COOLDOWN_TICKS = 15; // 0.75 seconds (vs 1.5s normal)
    private static final int USE_DURATION = 15; // Faster charge

    public QuantumGrappleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        
        int chargeTime = this.getUseDuration(stack, entity) - timeLeft;
        float chargePercent = Math.min(1.0f, chargeTime / (float) USE_DURATION);
        
        fireQuantumGrapple(level, player, chargePercent);
    }

    private void fireQuantumGrapple(Level level, Player player, float chargePercent) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(MAX_RANGE * (0.5 + 0.5 * chargePercent)));
        
        BlockHitResult hitResult = level.clip(new ClipContext(
            eyePos, endPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));
        
        if (hitResult.getType() == HitResult.Type.MISS) {
            player.displayClientMessage(
                Component.literal("Target out of range!").withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, 
                SoundSource.PLAYERS, 0.3f, 0.5f);
            return;
        }
        
        // Check for falcon companion nearby for boost
        boolean hasFalconBoost = isFalconNearby(level, player);
        double boostMultiplier = hasFalconBoost ? FALCON_BOOST_MULTIPLIER : 1.0;
        
        Vec3 targetPos = hitResult.getLocation();
        Vec3 playerPos = player.position().add(0, 0.5, 0);
        Vec3 pullDirection = targetPos.subtract(playerPos).normalize();
        
        double strength = PULL_STRENGTH * (0.6 + 0.4 * chargePercent) * boostMultiplier;
        Vec3 velocity = pullDirection.scale(strength).add(0, UPWARD_BOOST * boostMultiplier, 0);
        
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.resetFallDistance();
        
        // Quantum sound effect
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, 
            SoundSource.PLAYERS, 1.0f, 1.5f);
        level.playSound(null, hitResult.getBlockPos(), SoundEvents.AMETHYST_BLOCK_CHIME, 
            SoundSource.BLOCKS, 1.0f, 1.2f);
        
        // Spawn quantum particles
        if (level instanceof ServerLevel serverLevel) {
            spawnQuantumTrail(serverLevel, playerPos, targetPos, hasFalconBoost);
        }
        
        // Falcon boost notification
        if (hasFalconBoost) {
            player.displayClientMessage(
                Component.literal("⚡ Falcon Boost Active!").withStyle(ChatFormatting.AQUA), true);
        }
        
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        
        // No durability loss - quantum tech!
        
        QuantumFalcon.LOGGER.debug("Quantum Grapple fired by {} with {}x boost", 
            player.getName().getString(), boostMultiplier);
    }

    /**
     * Check if a quantum falcon is within range for boost.
     */
    private boolean isFalconNearby(Level level, Player player) {
        AABB searchBox = player.getBoundingBox().inflate(FALCON_DETECTION_RADIUS);
        List<Entity> nearbyEntities = level.getEntities(player, searchBox, 
            e -> e.getType().getDescriptionId().contains("falcon"));
        return !nearbyEntities.isEmpty();
    }

    /**
     * Spawn quantum-themed particle trail.
     */
    private void spawnQuantumTrail(ServerLevel level, Vec3 start, Vec3 end, boolean boosted) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 step = direction.normalize().scale(0.3);
        
        Vec3 current = start;
        for (int i = 0; i < distance * 3; i++) {
            // Primary cyan trail
            level.sendParticles(ParticleTypes.END_ROD,
                current.x, current.y, current.z,
                1, 0, 0, 0, 0);
            
            // Extra particles if boosted
            if (boosted && i % 2 == 0) {
                level.sendParticles(ParticleTypes.DRAGON_BREATH,
                    current.x, current.y, current.z,
                    1, 0.1, 0.1, 0.1, 0.02);
            }
            
            current = current.add(step);
        }
        
        // Impact burst
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
            end.x, end.y, end.z,
            25, 0.4, 0.4, 0.4, 0.15);
        level.sendParticles(ParticleTypes.END_ROD,
            end.x, end.y, end.z,
            15, 0.3, 0.3, 0.3, 0.08);
        
        if (boosted) {
            level.sendParticles(ParticleTypes.ENCHANT,
                end.x, end.y, end.z,
                30, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("⚡ Quantum-Enhanced Grapple")
            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Right-click to grapple!")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Hold longer for max range")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("▸ Range: " + (int)MAX_RANGE + " blocks")
            .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("▸ Cooldown: 0.75s")
            .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("▸ Falcon Boost: +50% speed")
            .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("▸ Unbreakable")
            .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Enchanted glow - quantum shimmer
    }
}
