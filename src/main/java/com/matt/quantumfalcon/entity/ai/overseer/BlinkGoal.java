package com.matt.quantumfalcon.entity.ai.overseer;

import com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Teleports the Overseer to a position near the target (behind or to the side).
 */
public class BlinkGoal extends Goal {

    private final QuantumOverseerEntity boss;
    private static final int COOLDOWN_TICKS = 100;
    private static final double MIN_DIST = 3;
    private static final double MAX_DIST = 7;
    private static final int ATTEMPTS = 12;

    public BlinkGoal(QuantumOverseerEntity boss) {
        this.boss = boss;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = boss.getTarget();
        if (!boss.isAlive() || target == null) return false;
        if (boss.getAttackCooldown() > 0 || !boss.isAggroed()) return false;
        double distSq = boss.distanceToSqr(target);
        return distSq < 36 && distSq > 4 && boss.getRandom().nextFloat() < 0.25f;
    }

    @Override
    public void start() {
        LivingEntity target = boss.getTarget();
        if (target == null || !(boss.level() instanceof ServerLevel sl)) return;

        Vec3 from = boss.position();
        Vec3 targetPos = target.position();
        Vec3 away = from.subtract(targetPos).normalize();

        List<BlockPos> candidates = new ArrayList<>();
        for (int i = 0; i < ATTEMPTS; i++) {
            double angle = (2 * Math.PI * i / ATTEMPTS) + boss.getRandom().nextDouble() * 0.5;
            double dist = MIN_DIST + boss.getRandom().nextDouble() * (MAX_DIST - MIN_DIST);
            double dx = away.x * dist * Math.cos(angle) - away.z * Math.sin(angle);
            double dz = away.z * dist * Math.cos(angle) + away.x * Math.sin(angle);
            int x = (int) (targetPos.x + dx);
            int y = (int) targetPos.y;
            int z = (int) (targetPos.z + dz);
            BlockPos pos = new BlockPos(x, y, z);
            if (sl.getBlockState(pos).isAir() && sl.getBlockState(pos.above()).isAir()) {
                AABB bounds = boss.getBoundingBox().move(pos.getX() - boss.getX(), pos.getY() - boss.getY(), pos.getZ() - boss.getZ());
                if (sl.noCollision(bounds)) {
                    ArenaMechanics.ActiveArena arena = ArenaMechanics.getArenaAt(boss.blockPosition());
                    if (arena != null) {
                        AABB arenaBounds = ArenaMechanics.getArenaBounds(arena);
                        if (arenaBounds.contains(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))
                            candidates.add(pos);
                    } else {
                        candidates.add(pos);
                    }
                }
            }
        }

        if (!candidates.isEmpty()) {
            BlockPos dest = candidates.get(boss.getRandom().nextInt(candidates.size()));
            sl.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 30, 0.5, 0.5, 0.5, 0.1);
            boss.teleportTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5, 25, 0.3, 0.3, 0.3, 0.08);
            sl.playSound(null, dest, com.matt.quantumfalcon.entity.util.QFMobSounds.get("ability_blink_end"), SoundSource.HOSTILE, 1.2f, 1.0f);
        }
        boss.setAttackCooldown(COOLDOWN_TICKS);
        boss.onBossAbilityUsed();
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
