package com.matt.quantumfalcon.client.cutscene;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** One cutscene audio layer (SFX or voice) at the boss; {@link #fadeOut()} ramps it to silence over 6 ticks so a skip doesn't click. */
final class CutsceneSound extends AbstractTickableSoundInstance {
    private static final int FADE_TICKS = 6;
    private final float base;
    private int fade = -1;

    CutsceneSound(SoundEvent event, double x, double y, double z, float volume) {
        super(event, SoundSource.HOSTILE, RandomSource.create());
        this.x = x; this.y = y; this.z = z;
        this.volume = this.base = volume;
        this.pitch = 1f;
        this.attenuation = SoundInstance.Attenuation.LINEAR;   // the event's range covers the 48-block cutscene radius
        this.relative = false;
    }

    void fadeOut() { if (fade < 0) fade = FADE_TICKS; }

    @Override public void tick() {
        if (fade < 0) return;
        if (fade == 0) { stop(); return; }
        volume = base * fade / (float) FADE_TICKS;
        fade--;
    }
}
