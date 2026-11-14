package com.matt.quantumfalcon.hytale;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Hytale-inspired Grappling Hook - Pull yourself toward blocks!
 * Right-click to fire, releases on impact pulling you toward target.
 */
public class GrapplingHookItem extends Item {
    // Config
    private static final double MAX_RANGE = 30.0;
    private static final double PULL_STRENGTH = 1.8;
    private static final double UPWARD_BOOST = 0.3;
    private static final int COOLDOWN_TICKS = 30; // 1.5 seconds
    private static final int USE_DURATION = 20; // Max charge time

    public GrapplingHookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Check cooldown
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        
        // Start using
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        
        // Calculate charge (longer hold = stronger pull)
        int chargeTime = this.getUseDuration(stack, entity) - timeLeft;
        float chargePercent = Math.min(1.0f, chargeTime / (float) USE_DURATION);
        
        // Fire grappling hook
        fireGrapple(level, player, chargePercent);
    }

    /**
     * Fire the grappling hook toward looked-at block.
     */
    private void fireGrapple(Level level, Player player, float chargePercent) {
        // Raycast to find target block
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
            // No target found
            player.displayClientMessage(
                Component.literal("No target in range!").withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, 
                SoundSource.PLAYERS, 0.5f, 1.5f);
            return;
        }
        
        // Calculate pull vector
        Vec3 targetPos = hitResult.getLocation();
        Vec3 playerPos = player.position().add(0, 0.5, 0);
        Vec3 pullDirection = targetPos.subtract(playerPos).normalize();
        
        // Apply velocity based on charge
        double strength = PULL_STRENGTH * (0.6 + 0.4 * chargePercent);
        Vec3 velocity = pullDirection.scale(strength).add(0, UPWARD_BOOST, 0);
        
        player.setDeltaMovement(velocity);
        player.hurtMarked = true; // Sync to client
        
        // Reset fall distance to prevent damage from grapple
        player.resetFallDistance();
        
        // Effects
        level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, 
            SoundSource.PLAYERS, 1.0f, 1.2f);
        level.playSound(null, hitResult.getBlockPos(), SoundEvents.CHAIN_PLACE, 
            SoundSource.BLOCKS, 1.0f, 0.8f);
        
        // Spawn rope particles
        if (level instanceof ServerLevel serverLevel) {
            spawnRopeParticles(serverLevel, playerPos, targetPos);
        }
        
        // Apply cooldown
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        
        // Damage item
        player.getItemInHand(player.getUsedItemHand()).hurtAndBreak(1, player, 
            LivingEntity.getSlotForHand(player.getUsedItemHand()));
        
        QuantumFalcon.LOGGER.debug("Grapple fired by {} to {} with strength {}", 
            player.getName().getString(), targetPos, strength);
    }

    /**
     * Spawn particles along the grapple rope path.
     * Quantum theme: Cyan END_ROD trail with REVERSE_PORTAL impact
     */
    private void spawnRopeParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 step = direction.normalize().scale(0.5);
        
        Vec3 current = start;
        for (int i = 0; i < distance * 2; i++) {
            // Quantum cyan trail
            level.sendParticles(ParticleTypes.END_ROD,
                current.x, current.y, current.z,
                1, 0, 0, 0, 0);
            current = current.add(step);
        }
        
        // Quantum impact burst
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
            end.x, end.y, end.z,
            15, 0.3, 0.3, 0.3, 0.1);
        level.sendParticles(ParticleTypes.END_ROD,
            end.x, end.y, end.z,
            8, 0.2, 0.2, 0.2, 0.05);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000; // Can hold indefinitely
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click to grapple!")
            .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Hold longer for stronger pull")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Range: " + (int)MAX_RANGE + " blocks")
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
