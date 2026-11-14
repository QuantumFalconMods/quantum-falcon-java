package com.matt.quantumfalcon.client.particle;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Client-side particle provider registration.
 * Registers custom particle renderers for all 33 particle types.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public class ModParticleProviders {
    
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // Use event.register() instead of engine.register() for NeoForge 1.21.1
        
        // ========================================
        // EXISTING PARTICLE CLASSES (5 total)
        // ========================================
        event.registerSpriteSet(ModParticles.QUANTUM_GLOW.get(), QuantumGlowParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SINGULARITY_PULL.get(), SingularityPullParticle.Provider::new);
        event.registerSpriteSet(ModParticles.RIFT_BURST.get(), RiftBurstParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ANOMALY_RAINBOW.get(), AnomalyRainbowParticle.Provider::new);
        
        // Use GenericGlowParticle for all other particles (fallback)
        // TODO: Create specific particle classes for better visuals
        event.registerSpriteSet(ModParticles.QUANTUM_SPARK.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.2f, 0.8f, 1.0f, 0.3f));
        event.registerSpriteSet(ModParticles.QUANTUM_PULSE.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.0f, 1.0f, 1.0f, 0.4f).animated());
        event.registerSpriteSet(ModParticles.QUANTUM_TRAIL.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.5f, 0.9f, 1.0f, 0.2f));
        event.registerSpriteSet(ModParticles.PHASE_SHIFT.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 0.0f, 1.0f, 0.3f));
        event.registerSpriteSet(ModParticles.ENTANGLEMENT_LINK.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.8f, 0.2f, 1.0f, 0.3f));
        event.registerSpriteSet(ModParticles.QUANTUM_HEART.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 0.2f, 0.5f, 0.4f));
        event.registerSpriteSet(ModParticles.BREEDING_AURA.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 0.8f, 0.0f, 0.5f));
        event.registerSpriteSet(ModParticles.MUTATION_SPARK.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 0.0f, 1.0f, 0.4f));
        event.registerSpriteSet(ModParticles.LEGENDARY_BURST.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 0.84f, 0.0f, 0.6f));
        event.registerSpriteSet(ModParticles.BLINK_START.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.0f, 1.0f, 1.0f, 0.3f).animated());
        event.registerSpriteSet(ModParticles.BLINK_END.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.0f, 1.0f, 1.0f, 0.3f).animated());
        event.registerSpriteSet(ModParticles.DIVE_TRAIL.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.2f, 0.6f, 1.0f, 0.2f));
        event.registerSpriteSet(ModParticles.BACKLASH.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 0.3f, 0.3f, 0.4f));
        event.registerSpriteSet(ModParticles.STORM_CRACKLE.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.5f, 0.5f, 1.0f, 0.3f));
        event.registerSpriteSet(ModParticles.VOID_WISP.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.2f, 0.0f, 0.3f, 0.3f));
        event.registerSpriteSet(ModParticles.EMBER_FLOAT.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 0.3f, 0.0f, 0.3f));
        event.registerSpriteSet(ModParticles.FROST_CRYSTAL.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.5f, 0.8f, 1.0f, 0.3f));
        event.registerSpriteSet(ModParticles.PRISM_SHIMMER.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 0.5f, 1.0f, 0.3f));
        event.registerSpriteSet(ModParticles.SINGULARITY_DISTORT.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.0f, 0.0f, 0.0f, 0.5f).animated());
        event.registerSpriteSet(ModParticles.COSMIC_STAR.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 1.0f, 1.0f, 0.4f));
        event.registerSpriteSet(ModParticles.PHOENIX_FLAME.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 0.5f, 0.0f, 0.4f));
        event.registerSpriteSet(ModParticles.RIFT_AMBIENT.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.5f, 0.0f, 1.0f, 0.2f));
        event.registerSpriteSet(ModParticles.NEST_GLOW.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.8f, 0.6f, 0.2f, 0.3f));
        event.registerSpriteSet(ModParticles.VOID_FLAME.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.5f, 0.0f, 0.5f, 0.4f));
        event.registerSpriteSet(ModParticles.CRYSTAL_SHIMMER.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.8f, 0.5f, 1.0f, 0.3f));
        event.registerSpriteSet(ModParticles.CRYSTAL_SHARD_PARTICLE.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.9f, 0.7f, 1.0f, 0.2f));
        event.registerSpriteSet(ModParticles.ENERGY_BOLT.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.0f, 1.0f, 1.0f, 0.4f));
        event.registerSpriteSet(ModParticles.RAINBOW_SPARK.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 0.5f, 0.0f, 0.3f));
        event.registerSpriteSet(ModParticles.RIFT_DISTORTION.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.5f, 0.0f, 0.8f, 0.3f));
        event.registerSpriteSet(ModParticles.GRAVITY_PARTICLE.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 0.3f, 0.3f, 0.3f, 0.3f));
        event.registerSpriteSet(ModParticles.BOSS_DEATH_BURST.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 0.84f, 0.0f, 0.6f));
        event.registerSpriteSet(ModParticles.REALITY_TEAR.get(), spriteSet -> 
            new GenericGlowParticle.Provider(spriteSet, 1.0f, 0.0f, 1.0f, 0.5f).animated());
        
        // colour-carrying FX
        event.registerSpriteSet(ModParticles.SLASH.get(), set -> new QFFxParticle.Provider(set, QFFxParticle.Kind.SLASH));
        event.registerSpriteSet(ModParticles.GLINT.get(), set -> new QFFxParticle.Provider(set, QFFxParticle.Kind.GLINT));
        event.registerSpriteSet(ModParticles.AURA.get(), set -> new QFFxParticle.Provider(set, QFFxParticle.Kind.AURA));
        event.registerSpriteSet(ModParticles.RUNE_FX.get(), set -> new QFFxParticle.Provider(set, QFFxParticle.Kind.RUNE));
        event.registerSpriteSet(ModParticles.SHOCK_RING.get(), set -> new QFFxParticle.Provider(set, QFFxParticle.Kind.RING));
        event.registerSpriteSet(ModParticles.EMBER_FX.get(), set -> new QFFxParticle.Provider(set, QFFxParticle.Kind.EMBER));

        QuantumFalcon.LOGGER.info("⚡ Registered {} particle providers (4 custom, 29 generic, 6 colour FX)", 39);
    }
}
