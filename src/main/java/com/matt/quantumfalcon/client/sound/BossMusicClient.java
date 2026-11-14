package com.matt.quantumfalcon.client.sound;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.network.BossMusicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client boss-music player: intro (one-shot) -> loop A (seamless, client-looped) -> crossfade to loop B on the
 * harder phase -> fade out on stop. Vanilla music is held off while a boss theme plays. Our own implementation.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public final class BossMusicClient {
    private BossMusicClient() {}
    static final int FADE_IN = 30, CROSSFADE = 50, FADE_OUT = 60;

    static final class Track extends AbstractTickableSoundInstance {
        private float target, step;
        Track(ResourceLocation id, boolean loop, float startVol) {
            super(SoundEvent.createVariableRangeEvent(id), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.looping = loop; this.delay = 0; this.volume = startVol; this.target = startVol;
            this.relative = true; this.attenuation = Attenuation.NONE; this.x = this.y = this.z = 0;
        }
        void fadeTo(float t, int ticks) { target = t; step = Math.max(0.001f, Math.abs(t - volume) / Math.max(1, ticks)); }
        @Override public void tick() {
            if (volume < target) volume = Math.min(target, volume + step);
            else if (volume > target) volume = Math.max(target, volume - step);
            if (target <= 0f && volume <= 0f) stop();
        }
        @Override public boolean canStartSilent() { return true; }
    }

    private static String base;
    private static Track current, intro;
    private static int introAge;
    private static boolean wantB;

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, path); }

    public static void handle(String b, byte action) {
        switch (action) {
            case BossMusicPayload.INTRO -> { stopAll(10); base = b; wantB = false; intro = play(rl(b + "_intro"), false, 1f); introAge = 0; }
            case BossMusicPayload.LOOP_A, BossMusicPayload.LOOP_B -> {
                if (b.equals(base) && current != null) return;
                stopAll(FADE_OUT / 2); base = b; wantB = action == BossMusicPayload.LOOP_B;
                current = play(rl(wantB ? b + "_p2" : b), true, 0f); current.fadeTo(1f, FADE_IN);
            }
            case BossMusicPayload.PHASE2 -> {
                if (!b.equals(base) || wantB) return;
                wantB = true;
                if (intro != null) { intro.fadeTo(0f, 20); intro = null; }
                if (current != null) current.fadeTo(0f, CROSSFADE);
                current = play(rl(b + "_p2"), true, 0f); current.fadeTo(1f, CROSSFADE);
            }
            case BossMusicPayload.STOP -> { if (b.equals(base)) { stopAll(FADE_OUT); base = null; } }
            default -> {}
        }
    }

    private static Track play(ResourceLocation id, boolean loop, float vol) {
        Track t = new Track(id, loop, vol);
        Minecraft.getInstance().getSoundManager().play(t);
        return t;
    }

    private static void stopAll(int fade) {
        if (intro != null) { intro.fadeTo(0f, fade); intro = null; }
        if (current != null) { current.fadeTo(0f, fade); current = null; }
    }

    /** True while a boss theme is the active music (used by the client smoke test / HUD). */
    public static String active() { return base; }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (base == null) return;
        Minecraft mc = Minecraft.getInstance();
        mc.getMusicManager().stopPlaying();
        if (intro != null && ++introAge > 20 && !mc.getSoundManager().isActive(intro)) {
            intro = null;
            current = play(rl(wantB ? base + "_p2" : base), true, 1f);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { intro = null; current = null; base = null; wantB = false; }
}
