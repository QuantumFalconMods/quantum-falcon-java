package com.matt.quantumfalcon.entity.ai.riftwarden;

import com.matt.quantumfalcon.entity.boss.RiftWardenEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** Crystal Storm: Phase 2+ special. */
public class RiftCrystalStormGoal extends Goal {
    private final RiftWardenEntity boss;

    public RiftCrystalStormGoal(RiftWardenEntity boss) { this.boss = boss; }

    @Override
    public boolean canUse() {
        if (!boss.isAlive() || boss.getTarget() == null) return false;
        if (boss.getPhase() < 2) return false;
        if (boss.getStunTicks() > 0 || boss.getSpecialAttackTicks() > 0 || boss.getPhaseChangeTicks() > 0) return false;
        if (boss.getAttackCooldown() > 0 || !boss.isAggroed()) return false;
        return boss.getRandom().nextFloat() < 0.35f;
    }

    @Override
    public void start() {
        int cooldown = boss.performCrystalStorm();   // telegraphed wind-up included
        boss.setAttackCooldown(cooldown);
    }

    @Override
    public boolean canContinueToUse() { return false; }
}
