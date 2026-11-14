package com.matt.quantumfalcon.story;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.*;

/**
 * boss kills owed to players who fought and then logged out before the boss fell. Saved with the world, paid out
 * (defeat, relic, first-kill points, quests) through {@link BossDeathHandler#onBossDefeated} on their next login.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class PendingBossCredit extends SavedData {
    record Owed(BossDeathHandler.BossType type, ResourceKey<Level> dim, BlockPos pos) {}
    private final Map<UUID, List<Owed>> owed = new HashMap<>();

    public static PendingBossCredit get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(PendingBossCredit::new, PendingBossCredit::load, null), "quantumfalcon_pending_boss_credit");
    }

    public void add(UUID player, BossDeathHandler.BossType type, ResourceKey<Level> dim, BlockPos pos) {
        owed.computeIfAbsent(player, k -> new ArrayList<>()).add(new Owed(type, dim, pos.immutable()));
        setDirty();
    }

    public int pending(UUID player) { return owed.getOrDefault(player, List.of()).size(); }

    /** Pays out everything owed to this player. Returns how many kills were credited. */
    public int payOut(ServerPlayer sp) {
        List<Owed> list = owed.remove(sp.getUUID());
        if (list == null || list.isEmpty()) return 0;
        setDirty();
        for (Owed o : list) {
            ServerLevel lvl = sp.server.getLevel(o.dim());
            sp.displayClientMessage(Component.literal("While you were away, your party brought down the " + o.type().displayName + ". Your share:")
                .withStyle(ChatFormatting.GOLD), false);
            BossDeathHandler.onBossDefeated(sp, lvl != null ? lvl : sp.serverLevel(), o.type(), o.pos(), false);
        }
        return list.size();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) get(sp.server).payOut(sp);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        ListTag all = new ListTag();
        owed.forEach((id, list) -> list.forEach(o -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("player", id);
            t.putString("boss", o.type().name());
            t.putString("dim", o.dim().location().toString());
            t.putLong("pos", o.pos().asLong());
            all.add(t);
        }));
        tag.put("owed", all);
        return tag;
    }

    static PendingBossCredit load(CompoundTag tag, HolderLookup.Provider reg) {
        PendingBossCredit d = new PendingBossCredit();
        for (Tag x : tag.getList("owed", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) x;
            try {
                d.owed.computeIfAbsent(t.getUUID("player"), k -> new ArrayList<>()).add(new Owed(
                    BossDeathHandler.BossType.valueOf(t.getString("boss")),
                    ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("dim"))),
                    BlockPos.of(t.getLong("pos"))));
            } catch (IllegalArgumentException ignored) {}
        }
        return d;
    }
}
