package com.matt.quantumfalcon.entity.ai.riftwarden;

import com.matt.quantumfalcon.entity.boss.RiftWardenEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Fallback attack goal: rift burst (close-range) when no special is chosen.
 */
public class RiftWardenAttackGoal extends Goal {
    private final RiftWardenEntity boss;

    public RiftWardenAttackGoal(RiftWardenEntity boss) { this.boss = boss; }

    @Override
    public boolean canUse() {
        if (!boss.isAlive() || boss.getTarget() == null) return false;
        if (boss.getStunTicks() > 0 || boss.getSpecialAttackTicks() > 0 || boss.getPhaseChangeTicks() > 0) return false;
        return boss.getAttackCooldown() <= 0 && boss.isAggroed();
    }

    private int last = -1, prev = -2;

    @Override
    public void start() {
        // fallback when no special rolled: pick evenly from the moves this phase has open, never the same one three
        // times running (always falling back to the volley made the phase-3 Warden throw volley after volley)
        int open = boss.getPhase() >= 3 ? 3 : boss.getPhase() >= 2 ? 2 : 1;
        int pick = boss.getRandom().nextInt(open);
        if (open > 1 && pick == last && last == prev) pick = (pick + 1 + boss.getRandom().nextInt(open - 1)) % open;
        prev = last; last = pick;
        int cooldown = switch (pick) {
            case 1 -> boss.performCrystalStorm();
            case 2 -> boss.performRiftCollapse();
            default -> boss.performShardVolley();
        };   // telegraphed wind-up included
        boss.setAttackCooldown(cooldown);
    }

    @Override
    public boolean canContinueToUse() { return false; }
}
