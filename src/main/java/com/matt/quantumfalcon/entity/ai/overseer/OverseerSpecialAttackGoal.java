package com.matt.quantumfalcon.entity.ai.overseer;

import com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Picks and performs Swirl or Summon (special_attack animation, core weak point).
 */
public class OverseerSpecialAttackGoal extends Goal {

    private final QuantumOverseerEntity boss;

    public OverseerSpecialAttackGoal(QuantumOverseerEntity boss) {
        this.boss = boss;
    }

    @Override
    public boolean canUse() {
        if (!boss.isAlive() || boss.getTarget() == null) return false;
        if (boss.getAttackCooldown() > 0 || !boss.isAggroed()) return false;
        return boss.getPhase() >= 2 && boss.getRandom().nextFloat() < 0.4f;
    }

    @Override
    public void start() {
        byte attack = boss.chooseAttack();
        if (attack == QuantumOverseerEntity.ATTACK_SWIRL || attack == QuantumOverseerEntity.ATTACK_SUMMON) {
            int cooldown = boss.performAttack(attack);
            boss.setAttackCooldown(cooldown);
        } else {
            boss.setAttackCooldown(15);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
