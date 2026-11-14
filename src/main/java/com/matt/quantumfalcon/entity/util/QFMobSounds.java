package com.matt.quantumfalcon.entity.util;

import com.matt.quantumfalcon.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * The audio pack's per-creature events ({@code entity_<id>_ambient/_hurt/_death/_attack/_step}, {@code boss_roar_<id>},
 * {@code boss_windup_<id>}, cutscene and voice layers). Ids: overseer, warden, twins, sovereign, eclipse, void_lord, sha_sha,
 * wisp, phantom, guardian, void_echo, construct, stalker, minion, goblin, merchant.
 */
public final class QFMobSounds {
    private QFMobSounds() {}
    private static final Map<String, SoundEvent> CACHE = new HashMap<>();

    /** The event registered as {@code id}; falls back to a quiet vanilla cue only if the id is unknown (a typo shows up in the log). */
    public static SoundEvent get(String id) {
        return CACHE.computeIfAbsent(id, k -> {
            SoundEvent e = ModSounds.byId(k);
            if (e == null) { com.matt.quantumfalcon.QuantumFalcon.LOGGER.warn("[sound] no pack event {}", k); return SoundEvents.EMPTY; }
            return e;
        });
    }

    public static SoundEvent ambient(String mob) { return get("entity_" + mob + "_ambient"); }
    public static SoundEvent hurt(String mob)    { return get("entity_" + mob + "_hurt"); }
    public static SoundEvent death(String mob)   { return get("entity_" + mob + "_death"); }
    public static SoundEvent step(String mob)    { return get("entity_" + mob + "_step"); }
    public static SoundEvent attack(String mob)  { return get("entity_" + mob + "_attack"); }
    public static SoundEvent roar(String boss)   { return get("boss_roar_" + boss); }
    public static SoundEvent windup(String boss) { return get("boss_windup_" + boss); }

    /** Plays {@code id} at the entity for everyone nearby (server side; no-op on the client). */
    public static void play(Entity at, String id, float volume, float pitch) {
        if (at.level().isClientSide) return;
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), get(id), at.getSoundSource(), volume, pitch);
    }
    public static void play(Entity at, SoundEvent e, float volume, float pitch) {
        if (at.level().isClientSide) return;
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), e, at.getSoundSource(), volume, pitch);
    }
    /** The attack lands: call from doHurtTarget only when the hit connected. */
    public static void attackLanded(Entity at, String mob, float volume) {
        play(at, attack(mob), volume, 0.95f + at.level().random.nextFloat() * 0.1f);
    }
    /** Cutscene layers for the shared boss cutscene: the SFX and the spoken line, both at t=0 (mixed as separate layers). */
    public static void cutscene(Entity boss, String bossId, String beat) {
        if (boss.level().isClientSide) return;
        boss.level().playSound(null, boss.getX(), boss.getY(), boss.getZ(), get("boss_cutscene_" + bossId + "_" + beat), SoundSource.HOSTILE, 1.5f, 1f);
        boss.level().playSound(null, boss.getX(), boss.getY(), boss.getZ(), get("boss_voice_" + bossId + "_" + beat), SoundSource.HOSTILE, 1.4f, 1f);
    }
}
