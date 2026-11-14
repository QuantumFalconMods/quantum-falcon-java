package com.matt.quantumfalcon.entity.ai;

import com.matt.quantumfalcon.entity.FalconCommand;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * WANDER mode: if the falcon has a home perch and has strayed more than {@link #LEASH} blocks,
 * fly back and land on it. Once there it idles on the perch until something else takes over.
 */
public class FalconPerchGoal extends Goal {
    public static final double LEASH = 12.0;
    private final QuantumFalconEntity falcon;
    private int recalc;

    public FalconPerchGoal(QuantumFalconEntity falcon) {
        this.falcon = falcon;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean wandering() {
        return falcon.isTame() && !falcon.isOrderedToSit() && falcon.getCommand() == FalconCommand.WANDER
                && !falcon.isVehicle() && !falcon.isPassenger();
    }

    @Override
    public boolean canUse() {
        if (!wandering()) return false;
        BlockPos home = falcon.getHomePerch();
        if (home == null || !falcon.isValidPerch(home)) return false;
        return falcon.distanceToSqr(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5) > LEASH * LEASH;
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos home = falcon.getHomePerch();
        return wandering() && home != null && falcon.isValidPerch(home) && !falcon.isOnPerch(home);
    }

    @Override
    public void start() { recalc = 0; }

    @Override
    public void tick() {
        BlockPos home = falcon.getHomePerch();
        if (home == null) return;
        double d2 = falcon.distanceToSqr(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5);
        if (d2 < 2.5 * 2.5) {
            falcon.landOnPerch(home);
            return;
        }
        if (--recalc <= 0) {
            recalc = 10;
            if (!falcon.getNavigation().moveTo(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5, 1.2) && d2 > 32 * 32) {
                falcon.landOnPerch(home); // unreachable and very far: return directly
            }
        }
    }
}
