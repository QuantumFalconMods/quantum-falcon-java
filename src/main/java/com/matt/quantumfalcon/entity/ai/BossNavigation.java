package com.matt.quantumfalcon.entity.ai;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.registry.ModParticles;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;

/**
 * Custom ground pathfinding for boss entities.
 * - Arena lock: when the boss is inside a registered arena, path targets are clamped to arena bounds
 *   so the boss cannot path outside the arena.
 * - Phase-specific speed: optional speed modifier (e.g. faster in phase 2).
 * - Quantum particle burst when a new path is started (server-side).
 *
 * Used by Quantum Overseer, Rift Warden, Entangled Twins, and Singularity Sovereign.
 */
public class BossNavigation extends GroundPathNavigation {

    private double speedModifier = 1.0;

    public BossNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    /**
     * Set movement speed multiplier (e.g. 1.8 for phase 2). Applied when moving along path.
     */
    public void setSpeedModifier(double modifier) {
        this.speedModifier = Mth.clamp(modifier, 0.1, 3.0);
    }

    public double getSpeedModifier() {
        return speedModifier;
    }

    /**
     * Clamp a target position to the arena bounds if the mob is currently inside an arena.
     */
    private BlockPos clampToArena(BlockPos target) {
        ArenaMechanics.ActiveArena arena = ArenaMechanics.getArenaAt(mob.blockPosition());
        if (arena == null) return target;
        AABB bounds = ArenaMechanics.getArenaBounds(arena);
        int x = Mth.clamp(target.getX(), (int) bounds.minX, (int) bounds.maxX);
        int y = Mth.clamp(target.getY(), (int) bounds.minY, (int) bounds.maxY);
        int z = Mth.clamp(target.getZ(), (int) bounds.minZ, (int) bounds.maxZ);
        return new BlockPos(x, y, z);
    }

    @Override
    public Path createPath(BlockPos pTarget, int pRange) {
        BlockPos clamped = clampToArena(pTarget);
        Path path = super.createPath(clamped, pRange);
        if (path != null && !path.isDone() && mob.level() instanceof ServerLevel sl) {
            spawnPathParticles(sl);
        }
        return path;
    }

    @Override
    public Path createPath(Entity pEntity, int pRange) {
        BlockPos targetPos = pEntity.blockPosition();
        BlockPos clamped = clampToArena(targetPos);
        Path path = super.createPath(clamped, pRange);
        if (path != null && !path.isDone() && mob.level() instanceof ServerLevel sl) {
            spawnPathParticles(sl);
        }
        return path;
    }

    /**
     * Spawn quantum-themed particles when boss starts a new path (path recalculation).
     */
    private void spawnPathParticles(ServerLevel level) {
        Vec3 pos = mob.position().add(0, mob.getBbHeight() * 0.5, 0);
        try {
            level.sendParticles(ModParticles.QUANTUM_PULSE.get(),
                pos.x, pos.y, pos.z, 4, 0.3, 0.3, 0.3, 0.02);
            level.sendParticles(ModParticles.PHASE_SHIFT.get(),
                pos.x, pos.y, pos.z, 2, 0.2, 0.2, 0.2, 0.01);
        } catch (Exception e) {
            QuantumFalcon.LOGGER.debug("Boss path particles: {}", e.getMessage());
        }
    }

    @Override
    public boolean moveTo(Entity pEntity, double pSpeed) {
        return super.moveTo(pEntity, pSpeed * speedModifier);
    }

    @Override
    public boolean moveTo(Path pPath, double pSpeed) {
        return super.moveTo(pPath, pSpeed * speedModifier);
    }
}
