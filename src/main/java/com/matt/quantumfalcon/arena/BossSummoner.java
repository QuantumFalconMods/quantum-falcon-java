package com.matt.quantumfalcon.arena;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.boss.EntangledTwinsEntity;
import com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity;
import com.matt.quantumfalcon.entity.boss.RiftWardenEntity;
import com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity;
import com.matt.quantumfalcon.registry.ModEntities;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import com.matt.quantumfalcon.worldgen.structure.ArenaTemplateBuilder;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

import javax.annotation.Nullable;

/** Builds boss arenas and summons the matching boss (used by /qf boss, arena entry, and tests). */
public final class BossSummoner {
    private BossSummoner() {}

    @Nullable
    public static ArenaType arenaFor(String boss) {
        return switch (boss) {
            case "overseer", "quantum_overseer", "guardian" -> ArenaType.QUANTUM_GUARDIAN;
            case "warden", "rift_warden", "horror" -> ArenaType.VOID_HORROR;
            case "twins", "entangled_twins" -> ArenaType.ENTANGLED_TWINS;
            case "sovereign", "singularity_sovereign" -> ArenaType.SINGULARITY_SOVEREIGN;
            default -> null;
        };
    }

    public static EntityType<? extends Mob> bossType(ArenaType t) {
        return switch (t) {
            case QUANTUM_GUARDIAN -> ModEntities.QUANTUM_OVERSEER.get();
            case VOID_HORROR -> ModEntities.RIFT_WARDEN.get();
            case ENTANGLED_TWINS -> ModEntities.ENTANGLED_TWINS.get();
            case SINGULARITY_SOVEREIGN -> ModEntities.SINGULARITY_SOVEREIGN.get();
        };
    }

    public static boolean isBoss(net.minecraft.world.entity.Entity e) {
        return e instanceof QuantumOverseerEntity || e instanceof RiftWardenEntity
                || e instanceof EntangledTwinsEntity || e instanceof SingularitySovereignEntity;
    }

    public static boolean bossPresent(ServerLevel level, ArenaMechanics.ActiveArena arena) { return owner(level, arena) != null || ownerPending(level, arena); }

    /** the arena's own boss if it is loaded and alive, else any live boss of the arena's type nearby (legacy saves). */
    @Nullable
    public static Mob owner(ServerLevel level, ArenaMechanics.ActiveArena arena) {
        if (arena.bossId != null && level.getEntity(arena.bossId) instanceof Mob m && m.isAlive()) return m;
        var type = bossType(arena.type);
        // generous box: flying bosses (the Overseer) roam well above the floor and past the rim
        BlockPos c = arena.center; int r = arena.radius() + 48;
        var box = new net.minecraft.world.phys.AABB(c.getX() - r, c.getY() - 40, c.getZ() - r, c.getX() + r + 1, c.getY() + 128, c.getZ() + r + 1);
        var found = level.getEntities(type, box, e -> e.isAlive());
        if (found.isEmpty()) return null;
        if (arena.bossId == null || found.stream().noneMatch(e -> e.getUUID().equals(arena.bossId))) {
            ArenaMechanics.markBossSpawned(arena.center, found.get(0).getUUID());   // adopt it, so the arena owns exactly one
        }
        return found.get(0) instanceof Mob m ? m : null;
    }

    /** The arena has a boss on record whose chunk entities have not loaded yet (a fast teleport in): treat it as present. */
    static boolean ownerPending(ServerLevel level, ArenaMechanics.ActiveArena arena) {
        return arena.bossId != null && !level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(arena.center));
    }

    /** Build the arena at {@code center} (registers + persists it) and summon its boss. */
    @Nullable
    public static Mob buildAndSummon(ServerLevel level, BlockPos center, ArenaType type) {
        ArenaTemplateBuilder.buildArena(level, center, type);
        return spawnBoss(level, center, type);
    }

    @Nullable
    public static Mob spawnBoss(ServerLevel level, BlockPos center, ArenaType type) {
        var arena = ArenaMechanics.arenaAt(center);
        if (arena != null) {   // one boss per arena, whoever asks
            Mob existing = owner(level, arena);
            if (existing != null || ownerPending(level, arena)) { QuantumFalcon.LOGGER.info("{} arena at {} already has its boss; not summoning another", type, center); return existing; }
        }
        Mob boss = bossType(type).create(level);
        if (boss == null) return null;
        BlockPos at = center.above(2);
        if (type.hasVoidCenter()) {
            // keep the boss out of the arena's void singularity
            at = center.offset(type.getVoidCenterRadius() + 6, 0, 0);
            for (int dy = 12; dy > -12; dy--) {
                BlockPos q = at.offset(0, dy, 0);
                if (!level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir() && level.getBlockState(q.above(2)).isAir()) { at = q.above(); break; }
            }
        }
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(boss, level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        boss.setPersistenceRequired();
        level.addFreshEntity(boss);
        ArenaMechanics.markBossSpawned(center, boss.getUUID());
        QuantumFalcon.LOGGER.info("Summoned {} at {} ({})", type, at, level.dimension().location());
        return boss;
    }
}
