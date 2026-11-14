package com.matt.quantumfalcon.leveling;

import com.matt.quantumfalcon.QuantumFalcon;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.function.Supplier;

/**
 * persistent "not natural" marker per block position, stored as a chunk attachment so it survives restarts.
 * Fed by player placement, fluid-generated blocks (cobble/stone/obsidian/basalt generators) and piston moves; read by tool XP.
 */
public final class PlacedBlocks {
    private PlacedBlocks() {}

    /** Per-chunk cap; a farm reuses a handful of spots, so evicting the oldest entries is harmless. */
    public static final int MAX_PER_CHUNK = 4096;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, QuantumFalcon.MOD_ID);

    private static final Codec<LinkedHashSet<Long>> CODEC = Codec.LONG.listOf().xmap(LinkedHashSet::new, ArrayList::new);

    public static final Supplier<AttachmentType<LinkedHashSet<Long>>> PLACED =
        ATTACHMENT_TYPES.register("placed_blocks", () -> AttachmentType.builder(() -> new LinkedHashSet<Long>()).serialize(CODEC).build());

    public static void register(IEventBus bus) { ATTACHMENT_TYPES.register(bus); }

    private static LevelChunk chunk(LevelAccessor level, BlockPos pos) {
        if (level == null || level.isClientSide() || !level.hasChunkAt(pos)) return null;
        return level.getChunk(pos) instanceof LevelChunk lc ? lc : null;
    }

    public static void mark(LevelAccessor level, BlockPos pos) {
        LevelChunk lc = chunk(level, pos);
        if (lc == null) return;
        LinkedHashSet<Long> set = lc.getData(PLACED.get());
        if (set.add(pos.asLong())) {
            if (set.size() > MAX_PER_CHUNK) { Iterator<Long> it = set.iterator(); it.next(); it.remove(); }
            lc.setUnsaved(true);
        }
    }

    /** True if the position was marked; the marker is consumed (the block is gone after this break). */
    public static boolean consume(LevelAccessor level, BlockPos pos) {
        LevelChunk lc = chunk(level, pos);
        if (lc == null || !lc.hasData(PLACED.get())) return false;
        boolean had = lc.getData(PLACED.get()).remove(pos.asLong());
        if (had) lc.setUnsaved(true);
        return had;
    }

    public static boolean isMarked(LevelAccessor level, BlockPos pos) {
        LevelChunk lc = chunk(level, pos);
        return lc != null && lc.hasData(PLACED.get()) && lc.getData(PLACED.get()).contains(pos.asLong());
    }
}
