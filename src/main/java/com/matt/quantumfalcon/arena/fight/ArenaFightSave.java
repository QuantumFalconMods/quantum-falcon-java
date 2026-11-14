package com.matt.quantumfalcon.arena.fight;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.UUID;

/**
 * boss fights survive a restart. Every running fight (centre, dimension, type, radius, boss, participants, timers)
 * is written with the world; on start the fights come back before any chunk ticks, so the seal keeps standing, the
 * participants may still walk back in, and the empty-arena reset still runs. Seal and pylon blocks are re-adopted from
 * the world the first time the arena is loaded.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class ArenaFightSave extends SavedData {
    private static final String NAME = "quantumfalcon_arena_fights";
    private CompoundTag loaded = new CompoundTag();

    static ArenaFightSave get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ArenaFightSave::new, ArenaFightSave::load, null), NAME);
    }

    /** Always written: the content is the live fight list, so it is never stale. */
    @Override public boolean isDirty() { return true; }

    static ArenaFightSave load(CompoundTag tag, HolderLookup.Provider reg) {
        ArenaFightSave s = new ArenaFightSave();
        s.loaded = tag.copy();
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        ListTag list = new ListTag();
        for (var e : ArenaInteraction.FIGHTS.entrySet()) {
            ServerLevel level = ArenaInteraction.levelOf(e.getKey());
            ArenaInteraction.Fight f = e.getValue();
            if (level == null || f.boss == null) continue;
            CompoundTag t = new CompoundTag();
            t.putString("Dim", level.dimension().location().toString());
            t.put("Center", NbtUtils.writeBlockPos(f.center));
            t.putString("Type", f.type.name());
            t.putInt("Radius", f.radius);
            t.putUUID("Boss", f.boss);
            t.putInt("Ticks", f.ticks); t.putInt("NextSig", f.nextSignature); t.putInt("Sigs", f.signatures); t.putInt("Empty", f.emptyTicks);
            ListTag who = new ListTag();
            for (UUID u : f.participants) who.add(StringTag.valueOf(u.toString()));
            t.put("Participants", who);
            list.add(t);
        }
        tag.put("Fights", list);
        return tag;
    }

    /** Restores the saved fights. Returns how many came back. */
    static int restore(MinecraftServer server) {
        CompoundTag tag = get(server).loaded;
        int n = 0;
        for (Tag raw : tag.getList("Fights", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            try {
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("Dim"))));
                BlockPos c = NbtUtils.readBlockPos(t, "Center").orElse(null);
                if (level == null || c == null || ArenaInteraction.FIGHTS.containsKey(c)) continue;
                ArenaInteraction.Fight f = new ArenaInteraction.Fight(c, ArenaType.valueOf(t.getString("Type")), t.getInt("Radius"));
                f.boss = t.getUUID("Boss");
                f.ticks = t.getInt("Ticks"); f.nextSignature = t.getInt("NextSig"); f.signatures = t.getInt("Sigs"); f.emptyTicks = t.getInt("Empty");
                for (Tag u : t.getList("Participants", Tag.TAG_STRING)) f.participants.add(UUID.fromString(u.getAsString()));
                f.restored = true;
                ArenaInteraction.adoptLater(level, f);
                n++;
            } catch (RuntimeException ex) {
                QuantumFalcon.LOGGER.warn("Skipping a saved arena fight: {}", ex.toString());
            }
        }
        get(server).loaded = new CompoundTag();
        if (n > 0) QuantumFalcon.LOGGER.info("Restored {} arena fight(s) from the save", n);
        return n;
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) { restore(e.getServer()); }

    /** The fight map is static: a new world (or the next integrated server) must not inherit this one's fights. */
    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { ArenaInteraction.clearAll(); }
}
