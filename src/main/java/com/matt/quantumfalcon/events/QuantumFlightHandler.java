package com.matt.quantumfalcon.events;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Temporary "quantum flight" granted by the Quantum Berry (design notes: berry = temporary flight boost).
 * Stores the expiry game time in the player's persistent data so it survives relogs, and lands the
 * player safely (Slow Falling) when it runs out.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class QuantumFlightHandler {
    public static final String KEY = "qf_flight_until";

    private QuantumFlightHandler() {}

    public static void grantFlight(Player player, int ticks) {
        CompoundTag pd = player.getPersistentData();
        long until = Math.max(pd.getLong(KEY), player.level().getGameTime() + ticks);
        pd.putLong(KEY, until);
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        player.displayClientMessage(Component.literal("\u00a7b\u2726 Quantum flight: " + (ticks / 20) + "s \u2014 double-tap jump to fly"), true);
    }

    public static boolean hasQuantumFlight(Player player) {
        return player.getPersistentData().getLong(KEY) > player.level().getGameTime();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag pd = player.getPersistentData();
        if (!pd.contains(KEY)) return;
        long until = pd.getLong(KEY);
        long now = player.level().getGameTime();
        boolean survival = !player.isCreative() && !player.isSpectator();
        if (now < until) {
            if (!player.getAbilities().mayfly) { player.getAbilities().mayfly = true; player.onUpdateAbilities(); }
            if (player.getAbilities().flying && now % 4 == 0 && player.level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(), player.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
            if (until - now == 60) player.displayClientMessage(Component.literal("\u00a7eQuantum flight fading..."), true);
            return;
        }
        pd.remove(KEY);
        if (survival) {
            boolean wasFlying = player.getAbilities().flying;
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            if (wasFlying || !player.onGround()) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
        }
    }
}
