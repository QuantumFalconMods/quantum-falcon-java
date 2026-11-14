package com.matt.quantumfalcon.entity.ai;

import com.matt.quantumfalcon.block.FalconNestBlock;
import com.matt.quantumfalcon.blockentity.FalconEggBlockEntity;
import com.matt.quantumfalcon.entity.FalconCommand;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;

/** Wild falcons, and tame falcons left to wander or stay, now and then fly to a nearby nest and rest in it. A nest with
 * an egg is preferred: the falcon settles on the egg and broods it (+20% hatch speed, FalconEggBlockEntity). Rest ends
 * after a while, when hurt, when given a target, or when its owner calls it. */
public class RestInNestGoal extends Goal {
    private static final int RANGE = 10;
    private final QuantumFalconEntity falcon;
    @Nullable private BlockPos nest;
    private int restTicks, recalc;

    public RestInNestGoal(QuantumFalconEntity falcon) { this.falcon = falcon; setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK)); }

    private boolean free() {
        if (falcon.isBaby() || falcon.isVehicle() || falcon.isPassenger() || falcon.getTarget() != null || falcon.isOrderedToSit()) return false;
        return !falcon.isTame() || falcon.getCommand() == FalconCommand.WANDER || falcon.getCommand() == FalconCommand.STAY;
    }

    @Override public boolean canUse() {
        if (!free() || falcon.getRandom().nextInt(falcon.level().isNight() ? 60 : 400) != 0) return false;
        nest = findNest();
        return nest != null;
    }

    @Override public boolean canContinueToUse() {
        return nest != null && free() && falcon.getLastHurtByMob() == null && restTicks > 0 && FalconNestBlock.qualityAt(falcon.level(), nest) != null;
    }

    @Override public void start() { restTicks = 600 + falcon.getRandom().nextInt(1200); recalc = 0; }

    @Override public void stop() { falcon.setResting(false); nest = null; }

    @Override public void tick() {
        if (nest == null) return;
        double tx = nest.getX() + 0.5, tz = nest.getZ() + 0.5;
        boolean egg = falcon.level().getBlockEntity(nest.above()) instanceof FalconEggBlockEntity;
        var nq = com.matt.quantumfalcon.block.FalconNestBlock.qualityAt(falcon.level(), nest);
        if (nq == null) { nest = null; return; }
        // stand on the rim (the nest's collision top), or settle over the egg when brooding
        double ty = nest.getY() + (egg ? nq.seatPx / 16.0 + com.matt.quantumfalcon.block.FalconEggBlock.EGG_TOP_ON_NEST : Math.max(4, nq.seatPx + 2) / 16.0);
        double d2 = falcon.distanceToSqr(tx, ty, tz);
        if (d2 < 1.6 * 1.6) {
            if (!falcon.isResting()) { falcon.setFlying(false); falcon.getNavigation().stop(); falcon.moveTo(tx, ty, tz, falcon.getYRot(), 0); falcon.setResting(true); }
            falcon.setDeltaMovement(falcon.getDeltaMovement().multiply(0, 1, 0));
            restTicks--;
            return;
        }
        if (--recalc <= 0) { recalc = 10; falcon.getNavigation().moveTo(tx, ty, tz, 1.1); }
    }

    @Nullable private BlockPos findNest() {
        BlockPos at = falcon.blockPosition(), best = null; int score = -1;
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-RANGE, -4, -RANGE), at.offset(RANGE, 4, RANGE))) {
            var nq = FalconNestBlock.qualityAt(falcon.level(), q);
            if (nq == null) continue;
            boolean egg = falcon.level().getBlockEntity(q.above()) instanceof FalconEggBlockEntity;
            if (!egg && !falcon.level().getBlockState(q.above()).isAir()) continue;
            if (!falcon.level().getEntitiesOfClass(QuantumFalconEntity.class, new net.minecraft.world.phys.AABB(q).inflate(0.5, 2, 0.5), f -> f != falcon && f.isResting()).isEmpty()) continue;
            int s = (egg ? 100 : 0) + nq.tier * 10 - (int) Math.sqrt(q.distSqr(at));
            if (s > score) { score = s; best = q.immutable(); }
        }
        return best;
    }
}
