package com.matt.quantumfalcon.worldgen.structure;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * boss loot spawns where the boss died, and the Warden (r4) and Sovereign (r12) arenas have void pits under the
 * middle. For 10 s after a boss dies, any item in the arena that has no floor under it is moved onto the nearest solid
 * floor to the arena centre, so table drops and the Sovereign's loot pile can't fall out of the world.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class ArenaDropGuard {
    private record Guard(ResourceKey<Level> dim, AABB bounds, BlockPos center, long until, BlockPos[] safe) {}
    private static final List<Guard> GUARDS = new ArrayList<>();
    private ArenaDropGuard() {}

    public static void watch(ServerLevel level, AABB bounds, BlockPos center, int ticks) {
        GUARDS.add(new Guard(level.dimension(), bounds, center, level.getGameTime() + ticks, new BlockPos[1]));
    }

    /** True if there is a solid block somewhere below p, down to 48 blocks. */
    static boolean hasFloor(ServerLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, y, z);
        for (int i = 0; i < 48 && m.getY() > level.getMinBuildHeight(); i++) {
            if (!level.getBlockState(m).getCollisionShape(level, m).isEmpty()) return true;
            m.move(0, -1, 0);
        }
        return false;
    }

    /** Nearest standable spot to the centre, scanning rings outward (the centre itself may be the pit). */
    static BlockPos findSafe(ServerLevel level, BlockPos c, AABB b) {
        int top = (int) Math.min(b.maxY, c.getY() + 12), bottom = (int) Math.max(b.minY, c.getY() - 12);
        for (int r = 0; r <= 40; r++) {
            int steps = Math.max(1, r * 6);
            for (int s = 0; s < steps; s++) {
                double a = s * Math.PI * 2 / steps;
                int x = c.getX() + (int) Math.round(Math.cos(a) * r), z = c.getZ() + (int) Math.round(Math.sin(a) * r);
                for (int y = top; y >= bottom; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                            && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                            && level.getBlockState(p.above(2)).getCollisionShape(level, p.above(2)).isEmpty())
                        return p.above();
                }
            }
        }
        return c.above(2);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (GUARDS.isEmpty()) return;
        var it = GUARDS.iterator();
        while (it.hasNext()) {
            Guard g = it.next();
            ServerLevel level = e.getServer().getLevel(g.dim());
            if (level == null || level.getGameTime() > g.until()) { it.remove(); continue; }
            if (level.getGameTime() % 2 != 0) continue;
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, g.bounds().inflate(0, 64, 0))) {
                if (hasFloor(level, item.getX(), item.getY(), item.getZ())) continue;
                if (g.safe()[0] == null) g.safe()[0] = findSafe(level, g.center(), g.bounds());
                BlockPos s = g.safe()[0];
                item.teleportTo(s.getX() + 0.5, s.getY() + 0.2, s.getZ() + 0.5);
                item.setDeltaMovement(Vec3.ZERO);
            }
        }
    }
}
