package com.matt.quantumfalcon.entity.ability;

import com.matt.quantumfalcon.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * One themed passive + one active (ABILITY command / sneak-whistle / Falconer's Gauntlet) per falcon variant.
 * Numbers are the base values; skills, perches and berries modify them in {@link FalconAbilities}.
 */
public enum FalconAbility {
    PEREGRINE("peregrine", 20, 0x8FA3B8, () -> ModSounds.FALCON_ABILITY_STRIKE.get()),
    PRAIRIE("prairie", 30, 0xD9B45A, () -> ModSounds.FALCON_ABILITY_NATURE.get()),
    TUNDRA("tundra", 25, 0xBFEFFF, () -> ModSounds.FALCON_ABILITY_FROST.get()),
    STORMRIDER("stormrider", 30, 0x8C9EFF, () -> ModSounds.FALCON_ABILITY_STORM.get()),
    CRIMSON("crimson", 20, 0xC0283A, () -> ModSounds.FALCON_ABILITY_STRIKE.get()),
    WARPED("warped", 15, 0x1FB5A0, () -> ModSounds.FALCON_ABILITY_RIFT.get()),
    DEEPSLATE("deepslate", 30, 0x5A5A66, () -> ModSounds.FALCON_ABILITY_NATURE.get()),
    VOIDSTALKER("voidstalker", 25, 0x5A1E8C, () -> ModSounds.FALCON_ABILITY_VOID.get()),
    AETHERWING("aetherwing", 20, 0xF2F7FF, () -> ModSounds.FALCON_ABILITY_WIND.get()),
    CORAL("coral", 30, 0xFF7F6A, () -> ModSounds.FALCON_ABILITY_HEAL.get()),
    MUSHROOM("mushroom", 35, 0xB5543C, () -> ModSounds.FALCON_ABILITY_NATURE.get()),
    ENTANGLED_PRISM("entangled_prism", 20, 0xE07CFF, () -> ModSounds.FALCON_ABILITY_RIFT.get()),
    SOULFIRE("soulfire", 25, 0x3FD7E8, () -> ModSounds.FALCON_ABILITY_FLAME.get()),
    TEMPEST("tempest", 20, 0x9FD3E6, () -> ModSounds.FALCON_ABILITY_WIND.get()),
    ABYSSAL("abyssal", 30, 0x123A6B, () -> ModSounds.FALCON_ABILITY_VOID.get()),
    SINGULARITY("singularity", 40, 0x2A0A3D, () -> ModSounds.FALCON_ABILITY_VOID.get()),
    COSMIC("cosmic", 40, 0x6A5CFF, () -> ModSounds.FALCON_ABILITY_COSMIC.get()),
    QUANTUM_PHOENIX("quantum_phoenix", 45, 0xFF9A2E, () -> ModSounds.FALCON_ABILITY_FLAME.get());

    public final String variant;
    /** Base active cooldown in seconds. */
    public final int cooldownSeconds;
    public final int color;
    private final Supplier<SoundEvent> sound;

    FalconAbility(String variant, int cooldownSeconds, int color, Supplier<SoundEvent> sound) {
        this.variant = variant; this.cooldownSeconds = cooldownSeconds; this.color = color; this.sound = sound;
    }

    public SoundEvent sound() { return sound.get(); }

    public static FalconAbility of(String variant) {
        if (variant != null) for (FalconAbility a : values()) if (a.variant.equals(variant.toLowerCase(Locale.ROOT))) return a;
        return PEREGRINE;
    }

    public Component variantName() { return Component.translatable("falcon_variant.quantumfalcon." + variant); }
    public Component passiveName() { return Component.translatable("falcon_ability.quantumfalcon." + variant + ".passive"); }
    public Component passiveDesc() { return Component.translatable("falcon_ability.quantumfalcon." + variant + ".passive.desc"); }
    public Component activeName() { return Component.translatable("falcon_ability.quantumfalcon." + variant + ".active"); }
    public Component activeDesc() { return Component.translatable("falcon_ability.quantumfalcon." + variant + ".active.desc"); }
}
