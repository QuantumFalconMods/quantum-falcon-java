package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Particle Type Registry for Quantum Falcon Mod
 * 
 * All custom particles are registered here. Particle rendering is handled
 * in the client-side particle classes.
 */
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = 
        DeferredRegister.create(Registries.PARTICLE_TYPE, QuantumFalcon.MOD_ID);

    // ========================================
    // QUANTUM EFFECTS
    // ========================================
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> QUANTUM_SPARK = 
        PARTICLES.register("quantum_spark", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> QUANTUM_PULSE = 
        PARTICLES.register("quantum_pulse", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> QUANTUM_TRAIL = 
        PARTICLES.register("quantum_trail", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHASE_SHIFT = 
        PARTICLES.register("phase_shift", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ENTANGLEMENT_LINK = 
        PARTICLES.register("entanglement_link", () -> new SimpleParticleType(false));

    // ========================================
    // BREEDING EFFECTS
    // ========================================
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> QUANTUM_HEART = 
        PARTICLES.register("quantum_heart", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BREEDING_AURA = 
        PARTICLES.register("breeding_aura", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUTATION_SPARK = 
        PARTICLES.register("mutation_spark", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LEGENDARY_BURST = 
        PARTICLES.register("legendary_burst", () -> new SimpleParticleType(false));

    // ========================================
    // FALCON ABILITY EFFECTS
    // ========================================
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLINK_START = 
        PARTICLES.register("blink_start", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLINK_END = 
        PARTICLES.register("blink_end", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DIVE_TRAIL = 
        PARTICLES.register("dive_trail", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BACKLASH = 
        PARTICLES.register("backlash", () -> new SimpleParticleType(false));

    // ========================================
    // AMBIENT/WEATHER EFFECTS
    // ========================================
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STORM_CRACKLE = 
        PARTICLES.register("storm_crackle", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOID_WISP = 
        PARTICLES.register("void_wisp", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EMBER_FLOAT = 
        PARTICLES.register("ember_float", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FROST_CRYSTAL = 
        PARTICLES.register("frost_crystal", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PRISM_SHIMMER = 
        PARTICLES.register("prism_shimmer", () -> new SimpleParticleType(false));

    // ========================================
    // LEGENDARY/SPECIAL EFFECTS
    // ========================================
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SINGULARITY_DISTORT = 
        PARTICLES.register("singularity_distort", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COSMIC_STAR = 
        PARTICLES.register("cosmic_star", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHOENIX_FLAME = 
        PARTICLES.register("phoenix_flame", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RIFT_AMBIENT = 
        PARTICLES.register("rift_ambient", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEST_GLOW = 
        PARTICLES.register("nest_glow", () -> new SimpleParticleType(false));

    // ========================================
    // OVERRIDE-ALWAYS PARTICLES (alwaysShow=true)
    // ========================================
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOID_FLAME = 
        PARTICLES.register("void_flame", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CRYSTAL_SHIMMER = 
        PARTICLES.register("crystal_shimmer", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CRYSTAL_SHARD_PARTICLE = 
        PARTICLES.register("crystal_shard", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ENERGY_BOLT = 
        PARTICLES.register("energy_bolt", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAINBOW_SPARK = 
        PARTICLES.register("rainbow_spark", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RIFT_DISTORTION = 
        PARTICLES.register("rift_distortion", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GRAVITY_PARTICLE = 
        PARTICLES.register("gravity_particle", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BOSS_DEATH_BURST = 
        PARTICLES.register("boss_death_burst", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> REALITY_TEAR = 
        PARTICLES.register("reality_tear", () -> new SimpleParticleType(true));

    // ========================================
    // NEW PROFESSIONAL PARTICLES (33 Total)
    // ========================================
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> QUANTUM_GLOW = 
        PARTICLES.register("quantum_glow", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SINGULARITY_PULL = 
        PARTICLES.register("singularity_pull", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RIFT_BURST = 
        PARTICLES.register("rift_burst", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ANOMALY_RAINBOW = 
        PARTICLES.register("anomaly_rainbow", () -> new SimpleParticleType(true));

    // ========================================
    // COLOUR-CARRYING FX (weapons, abilities, set auras) - colour travels in the packet
    // ========================================
    public static final DeferredHolder<ParticleType<?>, ParticleType<net.minecraft.core.particles.ColorParticleOption>> SLASH = PARTICLES.register("slash", () -> colour(true));
    public static final DeferredHolder<ParticleType<?>, ParticleType<net.minecraft.core.particles.ColorParticleOption>> GLINT = PARTICLES.register("glint", () -> colour(false));
    public static final DeferredHolder<ParticleType<?>, ParticleType<net.minecraft.core.particles.ColorParticleOption>> AURA = PARTICLES.register("aura", () -> colour(false));
    public static final DeferredHolder<ParticleType<?>, ParticleType<net.minecraft.core.particles.ColorParticleOption>> RUNE_FX = PARTICLES.register("rune_fx", () -> colour(false));
    public static final DeferredHolder<ParticleType<?>, ParticleType<net.minecraft.core.particles.ColorParticleOption>> SHOCK_RING = PARTICLES.register("shock_ring", () -> colour(true));
    public static final DeferredHolder<ParticleType<?>, ParticleType<net.minecraft.core.particles.ColorParticleOption>> EMBER_FX = PARTICLES.register("ember_fx", () -> colour(false));

    private static ParticleType<net.minecraft.core.particles.ColorParticleOption> colour(boolean alwaysShow) {
        return new ParticleType<>(alwaysShow) {
            @Override public com.mojang.serialization.MapCodec<net.minecraft.core.particles.ColorParticleOption> codec() {
                return net.minecraft.core.particles.ColorParticleOption.codec(this);
            }
            @Override public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, net.minecraft.core.particles.ColorParticleOption> streamCodec() {
                return net.minecraft.core.particles.ColorParticleOption.streamCodec(this);
            }
        };
    }

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
        QuantumFalcon.LOGGER.info("⚡ Registered {} particle types", PARTICLES.getEntries().size());
    }
}
