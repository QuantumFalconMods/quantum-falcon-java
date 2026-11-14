package com.matt.quantumfalcon.entity.boss;

import com.matt.quantumfalcon.network.BossMusicPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.item.weapon.WeaponFx;
import com.matt.quantumfalcon.registry.ModParticles;
import com.matt.quantumfalcon.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.*;
import java.util.function.Supplier;

/**
 * Boss presentation shared by all seven bosses:
 * <ul>
 * <li>intro: the first time a player comes close, the boss plays its intro/roar animation and a roar, a title card
 *     shows its name and epithet, and its theme starts;</li>
 * <li>phases: when {@link QFBoss#qfPhase()} rises, the boss plays a phase-shift animation, shock rings and a horn,
 *     and the bar changes colour; later phases add a denser ambient aura;</li>
 * <li>wind-ups: {@link #windup} telegraphs an attack (wind-up animation, converging glints, a rune circle under the
 *     target, a rising charge sound) and runs it after a delay if the boss is still alive;</li>
 * <li>music: each boss has an original theme (intro, loop A, harder loop B). Players within 48 blocks get the intro then
 *     loop A; the harder phase crossfades to loop B; it fades out on death, removal, when a player strays beyond 72
 *     blocks, or when nobody has been near for 10 s (client side: client.sound.BossMusicClient).</li>
 * </ul>
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class BossDirector {
    private BossDirector() {}

    public record Style(String id, int rgb, BossEvent.BossBarColor[] bars, BossEvent.BossBarOverlay overlay,
                        Supplier<Holder<SoundEvent>> music, int musicTicks, float roarPitch, int windup) {
        public BossEvent.BossBarColor bar(int phase) { return bars[Math.max(0, Math.min(bars.length - 1, phase - 1))]; }
        public String anim(String what) { return "animation." + id + "." + what; }
    }

    private static final BossEvent.BossBarColor BLUE = BossEvent.BossBarColor.BLUE, WHITE = BossEvent.BossBarColor.WHITE, RED = BossEvent.BossBarColor.RED,
        PURPLE = BossEvent.BossBarColor.PURPLE, PINK = BossEvent.BossBarColor.PINK, YELLOW = BossEvent.BossBarColor.YELLOW, GREEN = BossEvent.BossBarColor.GREEN;

    public static final Map<String, Style> STYLES = Map.of(
        "rift_warden", new Style("rift_warden", 0x4FE3FF, new BossEvent.BossBarColor[]{BLUE, WHITE, RED}, BossEvent.BossBarOverlay.NOTCHED_6, () -> ModSounds.MUSIC_BOSS_WARDEN, 48 * 20, 0.75f, 16),
        "quantum_overseer", new Style("quantum_overseer", 0xC86BFF, new BossEvent.BossBarColor[]{PURPLE, PINK, RED}, BossEvent.BossBarOverlay.NOTCHED_10, () -> ModSounds.MUSIC_BOSS_OVerseer, 40 * 20, 0.9f, 18),
        "entangled_twins", new Style("entangled_twins", 0xFF5FD2, new BossEvent.BossBarColor[]{PINK, BLUE, PURPLE}, BossEvent.BossBarOverlay.NOTCHED_6, () -> ModSounds.MUSIC_BOSS_TWINS, 40 * 20, 1.3f, 14),
        "singularity_sovereign", new Style("singularity_sovereign", 0xFFB347, new BossEvent.BossBarColor[]{YELLOW, PURPLE, RED, WHITE}, BossEvent.BossBarOverlay.NOTCHED_20, () -> ModSounds.MUSIC_BOSS_SOVEREIGN, 56 * 20, 0.55f, 20),
        "eclipse_phantom", new Style("eclipse_phantom", 0xFFA040, new BossEvent.BossBarColor[]{WHITE, YELLOW, RED}, BossEvent.BossBarOverlay.NOTCHED_6, () -> ModSounds.MUSIC_BOSS_ECLIPSE, 36 * 20, 1.1f, 10),
        "void_lord", new Style("void_lord", 0xAA50FF, new BossEvent.BossBarColor[]{PURPLE, RED}, BossEvent.BossBarOverlay.NOTCHED_10, () -> ModSounds.MUSIC_BOSS_VOID_LORD, 44 * 20, 0.6f, 20),
        "sha_sha", new Style("sha_sha", 0x7CFFB0, new BossEvent.BossBarColor[]{GREEN, RED}, BossEvent.BossBarOverlay.PROGRESS, () -> ModSounds.MUSIC_BOSS_SHA_SHA, 32 * 20, 1.2f, 12),
        // A10 Eastwatch Aerie's flightmaster: dawn amber (#FFB43A); borrows the Eclipse loop until she has her own track
        "skywarden_irel", new Style("skywarden_irel", 0xFFB43A, new BossEvent.BossBarColor[]{YELLOW, RED}, BossEvent.BossBarOverlay.NOTCHED_6, () -> ModSounds.MUSIC_BOSS_ECLIPSE, 36 * 20, 1.25f, 14));

    public static Style style(Entity e) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return key != null && key.getNamespace().equals(QuantumFalcon.MOD_ID) ? STYLES.get(key.getPath()) : null;
    }

    /** Extra GeckoLib controller every boss registers; holds the triggerable intro / windup / phase_shift animations. */
    public static <T extends GeoAnimatable> AnimationController<T> presentation(T animatable, String id) {
        Style s = STYLES.get(id);
        return new AnimationController<>(animatable, "qf_present", 3, st -> PlayState.STOP)
            .triggerableAnim("intro", RawAnimation.begin().thenPlay(s.anim("intro")))
            .triggerableAnim("windup", RawAnimation.begin().thenPlay(s.anim("windup")))
            .triggerableAnim("phase_shift", RawAnimation.begin().thenPlay(s.anim("phase_shift")));
    }

    // ---------------------------------------------------------------- scheduler
    private record Task(long at, Runnable r) {}
    private static final List<Task> TASKS = new ArrayList<>();
    private static long now;
    private static void later(int ticks, Runnable r) { TASKS.add(new Task(now + ticks, r)); }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        now++;
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        TASKS.removeIf(t -> { if (t.at <= now) { due.add(t); return true; } return false; });
        for (Task t : due) { try { t.r.run(); } catch (Exception ex) { QuantumFalcon.LOGGER.warn("BossDirector task failed", ex); } }
    }

    // ---------------------------------------------------------------- wind-ups
    /** Telegraph then run {@code attack} after the boss style's wind-up (or {@code ticks} if > 0). Returns the delay used. */
    /** Last telegraph each boss showed: shape description and wind-up ticks (read by the stage 4 moveset test). */
    public record TeleRecord(String move, String shape, int ticks, long at, Telegraph tele) {}
    private static final Map<UUID, TeleRecord> LAST_TELE = new HashMap<>();
    public static TeleRecord lastTelegraph(Entity boss) { return LAST_TELE.get(boss.getUUID()); }
    public static void clearTelegraph(Entity boss) { LAST_TELE.remove(boss.getUUID()); }

    /**
     * Stage 4 movesets: wind up {@code attack} for {@code ticks} while drawing {@code tele} on the ground (a danger zone that
     * fills as the attack charges and flashes white just before it lands). The attack should use the same telegraph to pick
     * its victims, so stepping out of the zone during the wind-up dodges it.
     */
    public static int windup(Mob boss, String move, int ticks, Telegraph tele, Runnable attack) {
        int d = windup(boss, ticks, attack);
        LAST_TELE.put(boss.getUUID(), new TeleRecord(move, tele.describe(), d, now, tele));
        Style s = style(boss);
        if (d > 0 && boss.level() instanceof ServerLevel sl) {
            int rgb = s != null ? s.rgb : 0xFFFFFF;
            for (int i = 0; i < d; i += 2) {
                final double t = i / (double) d;
                later(i, () -> { if (boss.isAlive()) tele.draw(sl, t, rgb); });
            }
        }
        return d;
    }

    public static int windup(Mob boss, int ticks, Runnable attack) {
        Style s = style(boss);
        int d = ticks > 0 ? ticks : s != null ? s.windup : 12;
        if (!(boss.level() instanceof ServerLevel sl) || s == null) { attack.run(); return 0; }
        if (boss instanceof GeoEntity g) g.triggerAnim("qf_present", "windup");
        WeaponFx.sound(boss, ModSounds.BOSS_WINDUP, 1.6f, s.roarPitch + 0.2f);
        LivingEntity target = boss.getTarget();
        Vec3 mark = target != null ? target.position() : boss.position();
        for (int i = 0; i < d; i += 2) {
            final int step = i;
            later(i, () -> {
                if (!boss.isAlive()) return;
                double r = Math.max(1.5, boss.getBbWidth() * 1.4) * (1.0 - step / (double) d) + 0.6;
                Vec3 c = boss.position().add(0, boss.getBbHeight() * 0.6, 0);
                for (int k = 0; k < 6; k++) {
                    double a = (k / 6.0 + step * 0.07) * Math.PI * 2;
                    Vec3 p = c.add(Math.cos(a) * r, (k % 2 == 0 ? 0.6 : -0.6), Math.sin(a) * r);
                    Vec3 v = c.subtract(p).scale(0.12);
                    sl.sendParticles(WeaponFx.col(ModParticles.GLINT.get(), s.rgb), p.x, p.y, p.z, 0, v.x, v.y, v.z, 1);
                }
                if (step % 4 == 0 && target != null) WeaponFx.runeCircle(boss, mark, s.rgb, 1.2 + step / (double) d);
            });
        }
        // Cancel the telegraphed attack if the boss died, or if it had a target that is now gone (dead, removed or out of range).
        later(d, () -> {
            if (!boss.isAlive() || boss.isRemoved()) return;
            if (target != null && (!target.isAlive() || target.isRemoved() || target.level() != boss.level() || target.distanceToSqr(boss) > 64 * 64)) return;
            attack.run();
        });
        return d;
    }

    // ---------------------------------------------------------------- intro / phases / music
    private static final class Fight { final Set<UUID> listeners = new HashSet<>(); long nextMusic, startTick; int emptyTicks; boolean hard; }
    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof Mob boss) || !(ent instanceof QFBoss qb) || !(boss.level() instanceof ServerLevel sl) || !boss.isAlive()) return;
        Style s = style(boss);
        if (s == null) return;
        var tag = boss.getPersistentData();
        int phase = Math.max(1, qb.qfPhase());
        var bar = qb.qfBossBar();
        if (bar != null) {
            if (bar.getColor() != s.bar(phase)) bar.setColor(s.bar(phase));
            if (bar.getOverlay() != s.overlay) bar.setOverlay(s.overlay);
        }
        if (!tag.getBoolean("qf_intro")) {
            if (boss.tickCount % 5 == 0) {
                ServerPlayer p = (ServerPlayer) sl.getNearestPlayer(boss, 20);
                if (p != null && !p.isSpectator() && boss.hasLineOfSight(p)) intro(boss, s, sl);
            }
            tag.putInt("qf_phase", phase);
            return;
        }
        if (boss.tickCount % 20 == 0) com.matt.quantumfalcon.arena.fight.ArenaInteraction.resumeIfDropped(sl, boss);
        int seen = tag.getInt("qf_phase");
        if (phase > seen) { tag.putInt("qf_phase", phase); phaseShift(boss, s, sl, phase); }
        if (boss.tickCount % 10 == 0) {
            int n = phase - 1;   // ART_STYLE: no aura in phase 1, sparse in later phases (never hide the model)
            if (n > 0)
            sl.sendParticles(WeaponFx.col(ModParticles.AURA.get(), s.rgb), boss.getX(), boss.getY() + boss.getBbHeight() * 0.5, boss.getZ(), n,
                boss.getBbWidth() * 0.6, boss.getBbHeight() * 0.4, boss.getBbWidth() * 0.6, 0.02);
        }
        music(boss, s, sl);
    }

    public static void intro(Mob boss, Style s, ServerLevel sl) {
        boss.getPersistentData().putBoolean("qf_intro", true);
        boss.getPersistentData().putInt("qf_phase", Math.max(1, ((QFBoss) boss).qfPhase()));
        if (boss instanceof GeoEntity g) g.triggerAnim("qf_present", "intro");
        WeaponFx.sound(boss, ModSounds.BOSS_ROAR, 3.0f, s.roarPitch);
        Vec3 c = boss.position();
        WeaponFx.shock(boss, c, s.rgb, 3.5);
        later(6, () -> WeaponFx.shock(boss, c, s.rgb, 6.0));
        WeaponFx.runeCircle(boss, c, s.rgb, 3.0);
        Component name = boss.getDisplayName().copy().withColor(s.rgb);
        Component epithet = Component.translatable("boss.quantumfalcon." + s.id + ".epithet");
        for (ServerPlayer p : sl.getPlayers(p -> p.distanceToSqr(boss) < 48 * 48)) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(epithet));
            p.connection.send(new ClientboundSetTitleTextPacket(name));
        }
        FIGHTS.computeIfAbsent(boss.getUUID(), u -> new Fight()).startTick = boss.level().getGameTime();
        BossCutscenes.play(boss, com.matt.quantumfalcon.network.CutscenePayload.INTRO, s.rgb);
        com.matt.quantumfalcon.arena.fight.ArenaInteraction.startFight(sl, boss);
    }

    public static void phaseShift(Mob boss, Style s, ServerLevel sl, int phase) {
        if (boss instanceof GeoEntity g) g.triggerAnim("qf_present", "phase_shift");
        WeaponFx.sound(boss, ModSounds.BOSS_PHASE, 3.0f, s.roarPitch);
        Vec3 c = boss.position();
        for (int i = 0; i < 3; i++) { final double r = 2.5 + i * 2.5; later(i * 5, () -> WeaponFx.shock(boss, c, s.rgb, r)); }
        WeaponFx.glints(boss, c.add(0, boss.getBbHeight() * 0.6, 0), s.rgb, 40, boss.getBbWidth());
        Component msg = Component.translatable("boss.quantumfalcon.phase", boss.getDisplayName(), Component.translatable("boss.quantumfalcon.phase." + Math.min(phase, 4))).withColor(s.rgb);
        for (ServerPlayer p : sl.getPlayers(p -> p.distanceToSqr(boss) < 48 * 48)) p.displayClientMessage(msg, true);
        BossCutscenes.play(boss, com.matt.quantumfalcon.network.CutscenePayload.PHASE, s.rgb);
    }

    /** Path of the loop-A sound event (client derives _intro / _p2). */
    private static String base(Style s) { return s.music.get().value().getLocation().getPath(); }
    /** Loop B (harder theme) starts at phase 2; the four-phase Sovereign holds it for phase 3. */
    private static boolean hard(Mob boss, Style s, int phase) { return phase >= ("singularity_sovereign".equals(s.id) ? 3 : 2); }

    private static void music(Mob boss, Style s, ServerLevel sl) {
        Fight f = FIGHTS.computeIfAbsent(boss.getUUID(), u -> new Fight());
        var near = sl.getPlayers(p -> p.distanceToSqr(boss) < 48 * 48 && !p.isSpectator());
        String b = base(s);
        boolean hard = hard(boss, s, Math.max(1, ((QFBoss) boss).qfPhase()));
        long t = sl.getGameTime();
        if (f.startTick == 0) f.startTick = t;
        if (hard && !f.hard) {
            f.hard = true;
            for (UUID u : f.listeners) send(sl, u, b, BossMusicPayload.PHASE2);
        }
        // players who wandered far away fade out individually
        f.listeners.removeIf(u -> {
            var p = sl.getServer().getPlayerList().getPlayer(u);
            boolean gone = p == null || p.level() != sl || p.distanceToSqr(boss) > 72 * 72;
            if (gone && p != null) PacketDistributor.sendToPlayer(p, new BossMusicPayload(b, BossMusicPayload.STOP));
            return gone;
        });
        if (near.isEmpty()) {
            if (++f.emptyTicks > 200 && !f.listeners.isEmpty()) stopMusic(boss, s, sl, f);
            return;
        }
        f.emptyTicks = 0;
        for (ServerPlayer p : near) {
            if (!f.listeners.add(p.getUUID())) continue;
            byte a = t - f.startTick < 40 ? BossMusicPayload.INTRO : (f.hard ? BossMusicPayload.LOOP_B : BossMusicPayload.LOOP_A);
            PacketDistributor.sendToPlayer(p, new BossMusicPayload(b, a));
        }
    }

    private static void send(ServerLevel sl, UUID u, String b, byte action) {
        var p = sl.getServer().getPlayerList().getPlayer(u);
        if (p != null) PacketDistributor.sendToPlayer(p, new BossMusicPayload(b, action));
    }

    private static void stopMusic(Entity boss, Style s, ServerLevel sl, Fight f) {
        String b = base(s);
        for (UUID u : f.listeners) send(sl, u, b, BossMusicPayload.STOP);
        f.listeners.clear();
    }

    private static void end(Entity e) {
        Fight f = FIGHTS.remove(e.getUUID());
        Style s = style(e);
        if (f != null && s != null && e.level() instanceof ServerLevel sl) stopMusic(e, s, sl, f);
    }

    @SubscribeEvent public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof QFBoss) || !(e.getEntity() instanceof Mob boss)) return;
        end(boss);
        Style s = style(boss);
        // the twins fall together: only the last one standing gets the death cutscene
        boolean last = !(boss instanceof EntangledTwinsEntity tw) || tw.getTwin() == null || !tw.getTwin().isAlive();
        if (s != null && last && !boss.level().isClientSide) BossCutscenes.play(boss, com.matt.quantumfalcon.network.CutscenePayload.DEATH, s.rgb);
    }
    @SubscribeEvent public static void onLeave(EntityLeaveLevelEvent e) { if (e.getEntity() instanceof QFBoss && !e.getLevel().isClientSide()) end(e.getEntity()); }

    /** Test hook: has this fight's music switched to the harder loop B? */
    public static boolean musicHard(Entity boss) { Fight f = FIGHTS.get(boss.getUUID()); return f != null && f.hard; }

    /** Test hook: is a fight (music listeners) being tracked for this boss? */
    public static int listeners(Entity boss) { Fight f = FIGHTS.get(boss.getUUID()); return f == null ? 0 : f.listeners.size(); }
}
