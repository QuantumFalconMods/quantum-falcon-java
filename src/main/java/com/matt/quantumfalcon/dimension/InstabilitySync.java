package com.matt.quantumfalcon.dimension;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.capability.PlayerInstabilityCapability;
import com.matt.quantumfalcon.network.InstabilityPackets;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps each client's Instability meter current. The server owns the value (PlayerInstabilityCapability, 0-100 %); once a
 * second, when the whole-percent value has changed, it sends the value and its tier (1-10, one per 10 %, 0 when calm).
 * Before this the HUD read a client holder that nothing ever filled, so the meter never appeared.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class InstabilitySync {
    private static final Map<UUID, Integer> LAST = new ConcurrentHashMap<>();

    private InstabilitySync() {}

    public static int tierOf(float percent) {
        if (percent <= 0f) return 0;
        return Math.min(10, (int) Math.ceil(percent / 10f));
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.tickCount % 20 != 0) return;
        float v = PlayerInstabilityCapability.get(sp).getInstability();
        boolean rift = EchoRiftDimension.isEchoRift(sp.level());
        int zone = rift ? InstabilityEffects.zoneTier(sp) : -1;
        int safe = rift ? InstabilityEffects.safeSeconds(sp, zone) : -1;
        int key = Math.round(v * 10f) * 31 * 31 + (zone + 1) * 31 + (safe < 0 ? 0 : 1 + safe / 5);
        Integer last = LAST.put(sp.getUUID(), key);
        if (last != null && last == key && safe < 0) return;
        QuantumFalcon.LOGGER.debug("[instability] sync {} {}% tier {} zone {} safe {}s", sp.getGameProfile().getName(), v, tierOf(v), zone, safe);
        InstabilityPackets.syncToClient(sp, v, tierOf(v), zone, safe);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LAST.remove(e.getEntity().getUUID()); }
}
