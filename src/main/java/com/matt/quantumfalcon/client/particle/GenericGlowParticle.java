package com.matt.quantumfalcon.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Generic Glow Particle - Base class for simple glowing particles.
 * Used for: quantum_spark, quantum_pulse, quantum_trail, phase_shift, etc.
 */
public class GenericGlowParticle extends TextureSheetParticle {
    
    private final float baseScale;
    private final float r, g, b;
    
    protected GenericGlowParticle(ClientLevel level, double x, double y, double z,
                                  double xd, double yd, double zd,
                                  float r, float g, float b, float scale) {
        super(level, x, y, z, xd, yd, zd);
        this.baseScale = scale;
        this.r = r;
        this.g = g;
        this.b = b;
        this.scale(this.baseScale);
        this.lifetime = 20 + random.nextInt(20);
        this.gravity = 0.0f;
        this.hasPhysics = false;
        
        float v = 0.9f + random.nextFloat() * 0.2f;   // slight per-particle colour variance
        this.rCol = Math.min(1f, r * v + 0.05f);
        this.gCol = Math.min(1f, g * v + 0.05f);
        this.bCol = Math.min(1f, b * v + 0.05f);
        this.alpha = 0.8f;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Gentle pulsing
        float pulse = (float)Math.sin(age * 0.2f) * 0.1f + 1.0f;
        this.scale(baseScale * pulse);
        
        // Fade out
        this.alpha = 0.8f * (1.0f - (float)age / lifetime);
        if (animated && sprites != null && !removed) setSpriteFromAge(sprites);
    }
    
    private SpriteSet sprites;
    private boolean animated;

    GenericGlowParticle withSprites(SpriteSet set, boolean animated) {
        this.sprites = set; this.animated = animated;
        if (animated) setSpriteFromAge(set); else pickSprite(set);
        return this;
    }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }   // emissive

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;
        private final float r, g, b, scale;
        private boolean animated;

        /** Frames advance with age (rings, tears) instead of a random frame. */
        public Provider animated() { this.animated = true; return this; }
        
        public Provider(SpriteSet spriteSet, float r, float g, float b, float scale) {
            this.sprite = spriteSet;
            this.r = r;
            this.g = g;
            this.b = b;
            this.scale = scale;
        }
        
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                      double x, double y, double z,
                                      double xd, double yd, double zd) {
            GenericGlowParticle particle = new GenericGlowParticle(level, x, y, z, xd, yd, zd, r, g, b, scale);
            return particle.withSprites(sprite, animated);
        }
    }
}
