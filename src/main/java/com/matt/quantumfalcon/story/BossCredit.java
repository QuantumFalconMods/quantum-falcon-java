package com.matt.quantumfalcon.story;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.boss.EntangledTwinsEntity;
import com.matt.quantumfalcon.entity.boss.QFBoss;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import javax.annotation.Nullable;
import java.util.*;

/**
 * who gets credit for a boss. Every player who damaged it (directly, with a projectile, or through a tamed falcon or
 * other pet they own), the owner of whatever landed the killing blow, and every player standing in the arena when it falls.
 * Twins share one ledger, so damage to either twin counts.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class BossCredit {
    private BossCredit() {}

    private static final Map<UUID, Set<UUID>> CONTRIBUTORS = new HashMap<>();
    /** qftest fake players (not in the player list) that may be credited. */
    public static final Map<UUID, ServerPlayer> TEST_PLAYERS = new HashMap<>();

    /** The player a damage source should be credited to, or null. */
    @Nullable
    public static UUID creditFor(@Nullable DamageSource src) {
        if (src == null) return null;
        Entity e = src.getEntity() != null ? src.getEntity() : src.getDirectEntity();
        if (e instanceof ServerPlayer p) return p.getUUID();
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null) return o.getOwnerUUID();
        return null;
    }

    private static UUID ledger(LivingEntity boss) {
        if (boss instanceof EntangledTwinsEntity tw && tw.getTwin() != null) {   // one ledger per pair: the lower UUID
            UUID a = boss.getUUID(), b = tw.getTwin().getUUID();
            return a.compareTo(b) <= 0 ? a : b;
        }
        return boss.getUUID();
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof QFBoss) || e.getEntity().level().isClientSide() || e.getNewDamage() <= 0) return;
        UUID who = creditFor(e.getSource());
        if (who != null) CONTRIBUTORS.computeIfAbsent(ledger(e.getEntity()), k -> new LinkedHashSet<>()).add(who);
    }

    /** the same list as {@link #creditedPlayers} without consuming the ledger (for decisions made before the death event). */
    public static List<ServerPlayer> peekCredited(LivingEntity boss, DamageSource killing, ServerLevel level) {
        Set<UUID> c = CONTRIBUTORS.get(ledger(boss));
        Set<UUID> keep = c == null ? null : new LinkedHashSet<>(c);
        List<ServerPlayer> out = creditedPlayers(boss, killing, level);
        if (keep != null) CONTRIBUTORS.put(ledger(boss), keep);
        return out;
    }

    private static Set<UUID> creditedIds(LivingEntity boss, DamageSource killing, ServerLevel level, boolean consume) {
        Set<UUID> ids = new LinkedHashSet<>();
        Set<UUID> c = consume ? CONTRIBUTORS.remove(ledger(boss)) : CONTRIBUTORS.get(ledger(boss));
        if (c != null) ids.addAll(c);
        UUID k = creditFor(killing);
        if (k != null) ids.add(k);
        var arena = ArenaMechanics.getArenaAt(level, boss.blockPosition());
        if (arena != null && com.matt.quantumfalcon.WorldConfig.bystanderCredit())   // config eggs_and_relics.bystanderCredit
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, ArenaMechanics.getArenaBounds(arena)))
                if (!p.isSpectator()) ids.add(p.getUUID());
        return ids;
    }

    @Nullable
    private static ServerPlayer resolve(UUID id, ServerLevel level) {
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
        if (p == null && level.getPlayerByUUID(id) instanceof ServerPlayer fp) p = fp;
        if (p == null) p = TEST_PLAYERS.get(id);   // fake players in qftests are in neither list
        return p;
    }

    /** Everyone who earns the kill and is online, and clears the ledger. */
    public static List<ServerPlayer> creditedPlayers(LivingEntity boss, DamageSource killing, ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : creditedIds(boss, killing, level, true)) { ServerPlayer p = resolve(id, level); if (p != null) out.add(p); }
        return out;
    }

    /** contributors who are offline right now (logged out mid-fight). Call before {@link #creditedPlayers}. */
    public static List<UUID> offlineCredited(LivingEntity boss, DamageSource killing, ServerLevel level) {
        List<UUID> out = new ArrayList<>();
        for (UUID id : creditedIds(boss, killing, level, false)) if (resolve(id, level) == null) out.add(id);
        return out;
    }

    /** Test hook: credit {@code player} as if they had damaged {@code boss}. */
    public static void testContribute(LivingEntity boss, UUID player) { CONTRIBUTORS.computeIfAbsent(ledger(boss), k -> new LinkedHashSet<>()).add(player); }
}
