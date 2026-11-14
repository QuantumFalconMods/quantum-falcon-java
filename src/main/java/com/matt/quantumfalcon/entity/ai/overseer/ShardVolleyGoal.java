package com.matt.quantumfalcon.entity.ai.overseer;

import com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity;
import com.matt.quantumfalcon.entity.projectile.QuantumShardEntity;
import com.matt.quantumfalcon.registry.ModEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Fires a volley of homing Quantum Shards at the target.
 */
public class ShardVolleyGoal extends Goal {

    private final QuantumOverseerEntity boss;

    public ShardVolleyGoal(QuantumOverseerEntity boss) {
        this.boss = boss;
    }

    @Override
    public boolean canUse() {
        if (!boss.isAlive() || boss.getTarget() == null) return false;
        if (boss.getAttackCooldown() > 0 || !boss.isAggroed()) return false;
        return boss.getRandom().nextFloat() < 0.35f;
    }

    @Override
    public void start() {
        if (boss.getTarget() == null) return;
        boss.setAttackCooldown(boss.performShardVolley());   // telegraphed, phase-scaled (see QuantumOverseerEntity)
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
