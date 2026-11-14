package com.matt.quantumfalcon.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Quantum Glow Particle - Cyan sparkle that pulses and glows.
 * Used for quantum energy effects, item glows, and ambient quantum fields.
 */
public class QuantumGlowParticle extends TextureSheetParticle {
    
    private final float baseScale;
    
    protected QuantumGlowParticle(ClientLevel level, double x, double y, double z, 
                                   double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.baseScale = 0.1f + (float)Math.random() * 0.1f;
        this.scale(this.baseScale);
        this.lifetime = 20 + random.nextInt(20);
        this.gravity = 0.0f;
        this.hasPhysics = false;
        
        // Cyan color with slight variation
        float hue = 0.5f + (float)Math.random() * 0.1f; // Cyan range
        this.rCol = Mth.lerp(hue, 0.0f, 0.5f);
        this.gCol = Mth.lerp(hue, 1.0f, 1.0f);
        this.bCol = Mth.lerp(hue, 1.0f, 1.0f);
        this.alpha = 0.8f;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Pulsing scale effect
        float pulse = (float)Math.sin(age * 0.3f) * 0.1f + 1.0f;
        this.scale(baseScale * pulse);
        
        // Fade out
        this.alpha = 0.8f * (1.0f - (float)age / lifetime);
        
        // Slight upward drift
        this.yd += 0.01;
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;
        
        public Provider(SpriteSet spriteSet) {
            this.sprite = spriteSet;
        }
        
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                      double x, double y, double z,
                                      double xd, double yd, double zd) {
            QuantumGlowParticle particle = new QuantumGlowParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(sprite);
            return particle;
        }
    }
}
