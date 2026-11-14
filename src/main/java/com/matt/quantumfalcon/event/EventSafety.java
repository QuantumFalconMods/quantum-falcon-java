package com.matt.quantumfalcon.event;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.UUID;

/** one place that decides whether a world event may change a block. */
public final class EventSafety {
    /** Blocks that mark an area as player-built (data/quantumfalcon/tags/block/event_protected.json). */
    public static final TagKey<Block> PROTECTED = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.matt.quantumfalcon.QuantumFalcon.MOD_ID, "event_protected"));
    private static final GameProfile EVENTS = new GameProfile(UUID.fromString("5a1f0c9e-3b7d-4e2a-9c41-0e5f6a7b8c9d"), "[QuantumFalcon events]");

    private EventSafety() { }

    /** True if config, gamerule, claims and nearby player builds all allow changing the block at pos. */
    public static boolean mayAlter(ServerLevel level, BlockPos pos) {
        if (!EventsConfig.GRIEFING.get()) return false;
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;
        if (nearPlayerBuild(level, pos, EventsConfig.PROTECT_RADIUS.get())) return false;
        // claim mods cancel block breaks by non-members in claimed chunks; ask them with our own fake player
        BlockState state = level.getBlockState(pos);
        BlockEvent.BreakEvent ev = new BlockEvent.BreakEvent(level, pos, state, FakePlayerFactory.get(level, EVENTS));
        NeoForge.EVENT_BUS.post(ev);
        return !ev.isCanceled();
    }

    /** Any player-built marker block within r (checked on a 2-block lattice plus the column, cheap enough per strike). */
    public static boolean nearPlayerBuild(ServerLevel level, BlockPos c, int r) {
        if (r <= 0) return false;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx += 2) for (int dz = -r; dz <= r; dz += 2) for (int dy = -4; dy <= 6; dy += 2) {
            m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
            if (level.isLoaded(m) && level.getBlockState(m).is(PROTECTED)) return true;
        }
        return false;
    }
}
