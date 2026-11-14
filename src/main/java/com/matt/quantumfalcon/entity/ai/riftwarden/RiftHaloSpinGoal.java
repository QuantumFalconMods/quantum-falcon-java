package com.matt.quantumfalcon.entity.ai.riftwarden;

import com.matt.quantumfalcon.entity.boss.RiftWardenEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class RiftHaloSpinGoal extends Goal {
    private final RiftWardenEntity boss;
    private int specialCount = 0;

    public RiftHaloSpinGoal(RiftWardenEntity boss) { this.boss = boss; }

    @Override
    public boolean canUse() {
        if (!boss.isAlive() || boss.getTarget() == null) return false;
        if (boss.getStunTicks() > 0 || boss.getSpecialAttackTicks() > 0 || boss.getPhaseChangeTicks() > 0) return false;
        if (boss.getAttackCooldown() > 0 || !boss.isAggroed()) return false;
        if (boss.getPhase() == 1) return boss.getRandom().nextFloat() < 0.25f;
        specialCount++;
        return specialCount % 2 == 0 || specialCount % 4 == 0;
    }

    @Override
    public void start() {
        boss.startHaloSpinNova();
        boss.setAttackCooldown(80);
    }

    @Override
    public boolean canContinueToUse() { return false; }
}
