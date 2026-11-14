package com.matt.quantumfalcon.entity.boss;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.network.CutscenePayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * Server side of boss cutscenes. Sends a {@link CutscenePayload} to every non-spectator within 48 blocks; during an intro or
 * phase cutscene the boss holds still and the watching players cannot be hurt, so skipping (sneak) or watching costs nothing.
 * Multiplayer rules (stage 4): a frozen boss takes no damage, and a player still in the cutscene deals none, so nobody gets
 * free hits while others watch. Skipping ({@link com.matt.quantumfalcon.network.CutsceneSkipPayload}) ends only the skipper's
 * protection. Players who arrive later are not pulled into a cutscene. A server stop releases every frozen boss, so none is
 * saved with its AI off.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class BossCutscenes {
    private BossCutscenes() {}

    public static final int INTRO_TICKS = 90, PHASE_TICKS = 45, DEATH_TICKS = 70;
    /** player UUID -> server tick when protection ends. */
    private static final Map<UUID, Long> PROTECTED = new HashMap<>();
    /** boss UUID -> tick when its AI resumes. */
    private static final Map<UUID, Long> FROZEN = new HashMap<>();
    private static final Map<UUID, ServerLevel> FROZEN_LEVEL = new HashMap<>();
    private static long clock;
    /** Recorded sends for /qftest: "kind:playerName". */
    public static final List<String> SENT = new ArrayList<>();

    /** boss id in the boss_cutscene_* / boss_voice_* sound events, per entity type path. */
    static final java.util.Map<String, String> AUDIO_ID = java.util.Map.of("rift_warden", "warden", "quantum_overseer", "overseer",
        "entangled_twins", "twins", "singularity_sovereign", "sovereign", "eclipse_phantom", "eclipse", "void_lord", "void_lord", "sha_sha", "sha_sha");
    /** Last cutscene audio beats played (boss_id_beat), newest last; for qftests. */
    public static final java.util.List<String> AUDIO_LOG = new java.util.ArrayList<>();

    /** the cutscene SFX layer and the spoken line for this beat (intro, phase, death). */
    static void audio(Mob boss, byte kind) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
        String id = key == null ? null : AUDIO_ID.get(key.getPath());
        if (id == null) return;
        String beat = kind == CutscenePayload.INTRO ? "intro" : kind == CutscenePayload.PHASE ? "phase" : "death";
        com.matt.quantumfalcon.entity.util.QFMobSounds.cutscene(boss, id, beat);
        AUDIO_LOG.add(id + "_" + beat);
        if (AUDIO_LOG.size() > 64) AUDIO_LOG.remove(0);
    }

    public static int play(Mob boss, byte kind, int rgb) {
        if (!(boss.level() instanceof ServerLevel sl)) return 0;
        audio(boss, kind);
        int ticks = kind == CutscenePayload.INTRO ? INTRO_TICKS : kind == CutscenePayload.PHASE ? PHASE_TICKS : DEATH_TICKS;
        CutscenePayload p = new CutscenePayload(boss.getId(), kind, ticks, boss.getX(), boss.getY(), boss.getZ(), boss.getBbHeight(), rgb);
        int n = 0;
        for (ServerPlayer pl : sl.getPlayers(x -> x.distanceToSqr(boss) < 48 * 48 && !x.isSpectator())) {
            PacketDistributor.sendToPlayer(pl, p);
            SENT.add(kind + ":" + pl.getGameProfile().getName());
            if (SENT.size() > 64) SENT.remove(0);
            if (kind != CutscenePayload.DEATH) PROTECTED.put(pl.getUUID(), clock + ticks);
            n++;
        }
        if (kind != CutscenePayload.DEATH && !boss.isNoAi()) {
            boss.setNoAi(true);
            boss.setDeltaMovement(0, Math.min(0, boss.getDeltaMovement().y), 0);
            FROZEN.put(boss.getUUID(), clock + ticks);
            FROZEN_LEVEL.put(boss.getUUID(), sl);
        }
        return n;
    }

    /** Test hook: treat a (fake) player as watching a cutscene for the given ticks. */
    public static void testProtect(ServerPlayer p, int ticks) { PROTECTED.put(p.getUUID(), clock + ticks); }

    /** The player skipped: their protection ends now (the boss stays frozen for everyone else). */
    public static void skipped(ServerPlayer p) {
        if (PROTECTED.remove(p.getUUID()) != null) SENT.add("skip:" + p.getGameProfile().getName());
    }

    /** Unfreeze every boss frozen by a cutscene (server stop, tests). */
    public static void releaseAll() {
        for (var en : FROZEN.keySet()) {
            ServerLevel sl = FROZEN_LEVEL.get(en);
            if (sl != null && sl.getEntity(en) instanceof Mob m) m.setNoAi(false);
        }
        FROZEN.clear(); FROZEN_LEVEL.clear(); PROTECTED.clear();
    }

    @SubscribeEvent
    public static void onStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent e) { releaseAll(); }

    public static boolean isFrozen(Entity boss) { return FROZEN.containsKey(boss.getUUID()); }
    public static boolean isProtected(Entity player) { return PROTECTED.containsKey(player.getUUID()); }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        clock++;
        PROTECTED.values().removeIf(t -> t <= clock);
        if (FROZEN.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, Long>> it = FROZEN.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            if (en.getValue() > clock) continue;
            ServerLevel sl = FROZEN_LEVEL.remove(en.getKey());
            if (sl != null && sl.getEntity(en.getKey()) instanceof Mob m) m.setNoAi(false);
            it.remove();
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && PROTECTED.containsKey(p.getUUID())) { e.setCanceled(true); return; }
        if (FROZEN.containsKey(e.getEntity().getUUID())) { e.setCanceled(true); return; }   // no free hits on a frozen boss
        Entity src = e.getSource().getEntity();
        if (src instanceof ServerPlayer p && PROTECTED.containsKey(p.getUUID())) e.setCanceled(true);   // nor from a watcher
    }
}
