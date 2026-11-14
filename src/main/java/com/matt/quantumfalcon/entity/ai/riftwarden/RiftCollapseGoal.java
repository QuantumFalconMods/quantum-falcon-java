package com.matt.quantumfalcon.entity.ai.riftwarden;

import com.matt.quantumfalcon.entity.boss.RiftWardenEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Rift Collapse: Phase 3 ultimate, gravity well + reality tears.
 */
public class RiftCollapseGoal extends Goal {
    private final RiftWardenEntity boss;

    public RiftCollapseGoal(RiftWardenEntity boss) { this.boss = boss; }

    @Override
    public boolean canUse() {
        if (!boss.isAlive() || boss.getTarget() == null) return false;
        if (boss.getPhase() < 3) return false;
        if (boss.getStunTicks() > 0 || boss.getSpecialAttackTicks() > 0 || boss.getPhaseChangeTicks() > 0) return false;
        if (boss.getAttackCooldown() > 0 || !boss.isAggroed()) return false;
        return boss.getRandom().nextFloat() < 0.25f;
    }

    @Override
    public void start() {
        int cooldown = boss.performRiftCollapse();   // telegraphed wind-up included
        boss.setAttackCooldown(cooldown);
    }

    @Override
    public boolean canContinueToUse() { return false; }
}
