package com.matt.quantumfalcon.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Anomaly Rainbow Particle - Shifting rainbow colors for anomaly falcons.
 * Used for legendary/anomaly falcon trails and special effects.
 */
public class AnomalyRainbowParticle extends TextureSheetParticle {
    
    private final float baseScale;
    
    protected AnomalyRainbowParticle(ClientLevel level, double x, double y, double z,
                                     double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.baseScale = 0.15f + random.nextFloat() * 0.1f;
        this.scale(this.baseScale);
        this.lifetime = 30 + random.nextInt(20);
        this.gravity = 0.0f;
        this.hasPhysics = false;
        
        // Initial rainbow color (will shift)
        updateRainbowColor(0);
        this.alpha = 0.9f;
    }
    
    private void updateRainbowColor(float hue) {
        // HSV to RGB conversion for rainbow effect
        float h = (hue % 1.0f) * 6.0f;
        int i = (int)h;
        float f = h - i;
        float p = 0.0f;
        float q = 1.0f - f;
        float t = f;
        
        switch (i % 6) {
            case 0 -> { rCol = 1.0f; gCol = t; bCol = p; }
            case 1 -> { rCol = q; gCol = 1.0f; bCol = p; }
            case 2 -> { rCol = p; gCol = 1.0f; bCol = t; }
            case 3 -> { rCol = p; gCol = q; bCol = 1.0f; }
            case 4 -> { rCol = t; gCol = p; bCol = 1.0f; }
            case 5 -> { rCol = 1.0f; gCol = p; bCol = q; }
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Shift through rainbow spectrum
        float hue = (age * 0.05f) % 1.0f;
        updateRainbowColor(hue);
        
        // Gentle pulsing
        float pulse = (float)Math.sin(age * 0.2f) * 0.1f + 1.0f;
        this.scale(baseScale * pulse);
        
        // Fade out
        this.alpha = 0.9f * (1.0f - (float)age / lifetime);
        
        // Slight drift
        this.xd *= 0.98;
        this.yd += 0.005;
        this.zd *= 0.98;
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
            AnomalyRainbowParticle particle = new AnomalyRainbowParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(sprite);
            return particle;
        }
    }
}
