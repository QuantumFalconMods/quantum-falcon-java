package com.matt.quantumfalcon.entity.ai.overseer;

import com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Triggers phase transition when HP crosses threshold.
 * Only canUse when phase change is due; start() runs the transition and animation.
 */
public class PhaseChangeGoal extends Goal {

    private final QuantumOverseerEntity boss;

    public PhaseChangeGoal(QuantumOverseerEntity boss) {
        this.boss = boss;
    }

    @Override
    public boolean canUse() {
        if (!boss.isAlive() || boss.getTarget() == null) return false;
        float hp = boss.getHealth() / boss.getMaxHealth();
        if (hp <= 0.33f && boss.getPhase() < 3) return true;
        if (hp <= 0.66f && boss.getPhase() < 2) return true;
        return false;
    }

    @Override
    public void start() {
        float hp = boss.getHealth() / boss.getMaxHealth();
        if (hp <= 0.33f) boss.onPhaseChangeTo(3);
        else if (hp <= 0.66f) boss.onPhaseChangeTo(2);
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
