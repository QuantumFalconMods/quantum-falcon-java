package com.matt.quantumfalcon.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persists registered boss arenas (dimension, centre, type) in the overworld's data storage. */
public class ArenaSavedData extends SavedData {
    private static final String NAME = "quantumfalcon_arenas";
    private final Map<String, CompoundTag> arenas = new LinkedHashMap<>();

    public static ArenaSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ArenaSavedData::new, ArenaSavedData::load, null), NAME);
    }

    private static String key(String dim, BlockPos p) { return dim + "@" + p.asLong(); }

    public synchronized void put(ArenaMechanics.ActiveArena a) {
        CompoundTag t = new CompoundTag();
        String dim = a.dimension == null ? "" : a.dimension.location().toString();
        t.putString("dim", dim);
        t.putLong("pos", a.center.asLong());
        t.putString("type", a.type.name());
        if (a.radius > 0) { t.putInt("r", a.radius); t.putInt("m", a.margin); }
        if (a.bossId != null) t.putUUID("boss", a.bossId);
        arenas.put(key(dim, a.center), t);
        setDirty();
    }

    public synchronized void remove(BlockPos center) {
        if (arenas.values().removeIf(t -> t.getLong("pos") == center.asLong())) setDirty();
    }

    public synchronized List<ArenaMechanics.ActiveArena> all() {
        List<ArenaMechanics.ActiveArena> out = new ArrayList<>();
        for (CompoundTag t : arenas.values()) {
            try {
                var a = new ArenaMechanics.ActiveArena(BlockPos.of(t.getLong("pos")), BossArenaStructure.ArenaType.valueOf(t.getString("type")));
                if (t.contains("r")) { a.radius = t.getInt("r"); a.margin = t.getInt("m"); }
                if (t.hasUUID("boss")) { a.bossId = t.getUUID("boss"); a.bossSpawned = true; }
                if (!t.getString("dim").isEmpty())
                    a.dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("dim")));
                out.add(a);
            } catch (IllegalArgumentException ignored) { }
        }
        return out;
    }

    public static ArenaSavedData load(CompoundTag tag, HolderLookup.Provider reg) {
        ArenaSavedData d = new ArenaSavedData();
        ListTag list = tag.getList("arenas", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            d.arenas.put(key(t.getString("dim"), BlockPos.of(t.getLong("pos"))), t);
        }
        return d;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        ListTag list = new ListTag();
        list.addAll(arenas.values());
        tag.put("arenas", list);
        return tag;
    }
}
