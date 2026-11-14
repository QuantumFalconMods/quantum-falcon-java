package com.matt.quantumfalcon.entity.ai.riftwarden;

import com.matt.quantumfalcon.entity.boss.RiftWardenEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** Triggers phase transition when HP crosses 65% or 35%. */
public class RiftPhaseChangeGoal extends Goal {
    private final RiftWardenEntity boss;

    public RiftPhaseChangeGoal(RiftWardenEntity boss) { this.boss = boss; }

    @Override
    public boolean canUse() {
        if (!boss.isAlive() || boss.getTarget() == null) return false;
        float hp = boss.getHealth() / boss.getMaxHealth();
        if (hp <= 0.35f && boss.getPhase() < 3) return true;
        if (hp <= 0.65f && boss.getPhase() < 2) return true;
        return false;
    }

    @Override
    public void start() {
        float hp = boss.getHealth() / boss.getMaxHealth();
        if (hp <= 0.35f) boss.onPhaseChangeTo(3);
        else if (hp <= 0.65f) boss.onPhaseChangeTo(2);
    }

    @Override
    public boolean canContinueToUse() { return false; }
}
