package com.matt.quantumfalcon.entity.ai;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Shared AI Goals for Quantum Falcon entities.
 * Reusable custom goals extracted from entity implementations.
 */
public class ModAIGoals {

    // ========================================
    // TELEPORT BEHIND GOAL
    // ========================================
    
    /**
     * Goal that teleports the mob behind its target.
     * Used by VoidEcho and similar teleporting mobs.
     */
    public static class TeleportBehindGoal extends Goal {
        private final Mob mob;
        private int cooldown = 0;
        private static final int COOLDOWN_TICKS = 60; // 3 seconds
        
        public TeleportBehindGoal(Mob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            return target != null && 
                   cooldown <= 0 &&
                   mob.distanceToSqr(target) > 9 && // More than 3 blocks away
                   mob.distanceToSqr(target) < 256; // Less than 16 blocks away
        }
        
        @Override
        public void start() {
            LivingEntity target = mob.getTarget();
            if (target != null) {
                teleportBehind(target);
            }
        }
        
        @Override
        public boolean canContinueToUse() {
            return false;
        }
        
        @Override
        public void tick() {
            if (cooldown > 0) cooldown--;
        }
        
        private void teleportBehind(LivingEntity target) {
            if (cooldown > 0) return;
            
            // Particles at start
            spawnTeleportParticles();
            
            // Calculate position behind target
            Vec3 lookVec = target.getLookAngle().normalize();
            double x = target.getX() - lookVec.x * 2;
            double y = target.getY();
            double z = target.getZ() - lookVec.z * 2;
            
            // Teleport
            mob.teleportTo(x, y, z);
            mob.setYRot(target.getYRot()); // Face same direction as target (behind them)
            
            // Particles at end
            spawnTeleportParticles();
            
            mob.level().playSound(null, mob.blockPosition(), mob instanceof com.matt.quantumfalcon.entity.VoidEchoEntity ? com.matt.quantumfalcon.entity.util.QFMobSounds.get("entity_void_echo_teleport") : SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.HOSTILE, 0.9f, 1.0f);
            cooldown = COOLDOWN_TICKS;
        }
        
        private void spawnTeleportParticles() {
            if (mob.level().isClientSide) {
                for (int i = 0; i < 15; i++) {
                    double x = mob.getX() + (mob.getRandom().nextDouble() - 0.5);
                    double y = mob.getY() + mob.getRandom().nextDouble() * 2;
                    double z = mob.getZ() + (mob.getRandom().nextDouble() - 0.5);
                    mob.level().addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0, 0);
                }
            }
        }
    }

    // ========================================
    // CRYSTALLIZE GOAL
    // ========================================
    
    /**
     * Goal that makes a mob enter a defensive crystallized state.
     * Used by CrystalGuardian and similar defensive mobs.
     */
    public static class CrystallizeGoal extends Goal {
        private final Mob mob;
        private final Crystallizable crystallizable;
        private int cooldown = 0;
        private static final int COOLDOWN_TICKS = 400; // 20 seconds
        
        public interface Crystallizable {
            boolean isCrystallized();
            void setCrystallized(boolean crystallized);
            void setCrystallizeTimer(int ticks);
            void setCrystallizeCooldown(int ticks);
            int getCrystallizeCooldown();
        }
        
        public CrystallizeGoal(Mob mob, Crystallizable crystallizable) {
            this.mob = mob;
            this.crystallizable = crystallizable;
        }
        
        @Override
        public boolean canUse() {
            return mob.getHealth() < mob.getMaxHealth() * 0.5f && 
                   crystallizable.getCrystallizeCooldown() <= 0 &&
                   !crystallizable.isCrystallized();
        }
        
        @Override
        public void start() {
            crystallizable.setCrystallized(true);
            crystallizable.setCrystallizeTimer(100); // 5 seconds
            crystallizable.setCrystallizeCooldown(COOLDOWN_TICKS);
            if (mob instanceof LivingEntity living) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 100, 2));
            }
            mob.level().playSound(null, mob.blockPosition(), mob instanceof com.matt.quantumfalcon.entity.CrystalGuardianEntity ? com.matt.quantumfalcon.entity.util.QFMobSounds.get("entity_guardian_crystallize") : SoundEvents.GLASS_PLACE, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f);
        }
        
        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    // ========================================
    // PHASE THROUGH GOAL
    // ========================================
    
    /**
     * Goal that makes a mob phase through blocks when hurt.
     * Used by QuantumPhantom and similar phasing mobs.
     */
    public static class PhaseThroughGoal extends Goal {
        private final Mob mob;
        private final Phasable phasable;
        
        public interface Phasable {
            boolean isPhasing();
            void setPhasing(boolean phasing);
            void setPhaseTimer(int ticks);
            int getPhaseTimer();
        }
        
        public PhaseThroughGoal(Mob mob, Phasable phasable) {
            this.mob = mob;
            this.phasable = phasable;
        }
        
        @Override
        public boolean canUse() {
            return mob.hurtTime > 0 && !phasable.isPhasing();
        }
        
        @Override
        public void start() {
            phasable.setPhasing(true);
            phasable.setPhaseTimer(40); // 2 seconds
            mob.noPhysics = true;
            
            // Quick teleport
            double x = mob.getX() + (mob.getRandom().nextDouble() - 0.5) * 10;
            double y = mob.getY() + mob.getRandom().nextDouble() * 5;
            double z = mob.getZ() + (mob.getRandom().nextDouble() - 0.5) * 10;
            mob.teleportTo(x, y, z);
        }
        
        @Override
        public void tick() {
            if (phasable.getPhaseTimer() > 0) {
                phasable.setPhaseTimer(phasable.getPhaseTimer() - 1);
                if (phasable.getPhaseTimer() <= 0) {
                    phasable.setPhasing(false);
                    mob.noPhysics = false;
                }
            }
        }
        
        @Override
        public boolean canContinueToUse() {
            return phasable.isPhasing();
        }
    }

    // ========================================
    // LEAD TO LOCATION GOAL
    // ========================================
    
    /**
     * Goal that makes a mob lead players to a specific location.
     * Used by QuantumWisp and similar guide mobs.
     */
    public static class LeadToLocationGoal extends Goal {
        private final Mob mob;
        private final Leadable leadable;
        
        public interface Leadable {
            net.minecraft.core.BlockPos getTargetLocation();
            int getLeadTimer();
            void setLeadTimer(int ticks);
        }
        
        public LeadToLocationGoal(Mob mob, Leadable leadable) {
            this.mob = mob;
            this.leadable = leadable;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }
        
        @Override
        public boolean canUse() {
            return leadable.getLeadTimer() > 0 && leadable.getTargetLocation() != null;
        }
        
        @Override
        public void tick() {
            net.minecraft.core.BlockPos target = leadable.getTargetLocation();
            if (target == null) return;
            
            // Move towards target
            mob.getNavigation().moveTo(
                target.getX(),
                target.getY(),
                target.getZ(),
                1.0
            );
            
            // Stop when close
            if (mob.blockPosition().distSqr(target) < 9) {
                leadable.setLeadTimer(0);
                mob.level().playSound(null, mob.blockPosition(), SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 2.0f);
            }
        }
        
        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
    }

    // ========================================
    // TELEPORT TO TARGET GOAL
    // ========================================
    
    /**
     * Goal that teleports the mob to its target.
     * Used by RiftWarden and similar teleporting bosses.
     */
    public static class TeleportToTargetGoal extends Goal {
        private final Monster mob;
        private int cooldown = 0;
        private static final int COOLDOWN_TICKS = 100; // 5 seconds
        
        public TeleportToTargetGoal(Monster mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            return target != null && 
                   cooldown <= 0 &&
                   mob.distanceToSqr(target) > 16 && // More than 4 blocks away
                   mob.distanceToSqr(target) < 400; // Less than 20 blocks away
        }
        
        @Override
        public void start() {
            LivingEntity target = mob.getTarget();
            if (target != null) {
                teleportToTarget(target);
            }
        }
        
        @Override
        public boolean canContinueToUse() {
            return false;
        }
        
        @Override
        public void tick() {
            if (cooldown > 0) cooldown--;
        }
        
        private void teleportToTarget(LivingEntity target) {
            if (cooldown > 0) return;
            
            // Teleport particles at start position
            if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    mob.getX(), mob.getY() + 1, mob.getZ(), 30, 0.5, 1, 0.5, 0.1);
            }
            
            // Calculate teleport position behind target
            Vec3 lookVec = target.getLookAngle();
            double x = target.getX() - lookVec.x * 3;
            double y = target.getY();
            double z = target.getZ() - lookVec.z * 3;
            
            // Teleport
            mob.teleportTo(x, y, z);
            
            // Teleport particles at end position
            if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    mob.getX(), mob.getY() + 1, mob.getZ(), 30, 0.5, 1, 0.5, 0.1);
            }
            
            mob.level().playSound(null, mob.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f);
            cooldown = COOLDOWN_TICKS;
        }
    }
}
