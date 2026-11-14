package com.matt.quantumfalcon.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Rift Burst Particle - Explosive purple burst with dimensional energy.
 * Used for rift portal activations, dimension shifts, and rift-related abilities.
 */
public class RiftBurstParticle extends TextureSheetParticle {
    
    private final float baseScale;
    
    protected RiftBurstParticle(ClientLevel level, double x, double y, double z,
                                double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.baseScale = 0.3f + random.nextFloat() * 0.2f;
        this.scale(this.baseScale);
        this.lifetime = 10 + random.nextInt(10);
        this.gravity = 0.0f;
        this.hasPhysics = false;
        
        // Purple/magenta burst
        this.rCol = 0.7f + random.nextFloat() * 0.3f;
        this.gCol = 0.0f;
        this.bCol = 0.8f + random.nextFloat() * 0.2f;
        this.alpha = 1.0f;
        
        // Explosive outward motion
        this.xd = (random.nextDouble() - 0.5) * 0.3;
        this.yd = (random.nextDouble() - 0.5) * 0.3;
        this.zd = (random.nextDouble() - 0.5) * 0.3;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Expand then contract
        float progress = (float)age / lifetime;
        float scaleMultiplier = progress < 0.3f 
            ? 1.0f + progress * 2.0f  // Expand
            : 1.6f - (progress - 0.3f) * 2.0f; // Contract
        this.scale(baseScale * scaleMultiplier);
        
        // Fade out
        this.alpha = 1.0f - progress;
        
        // Slow down
        this.xd *= 0.9;
        this.yd *= 0.9;
        this.zd *= 0.9;
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
            RiftBurstParticle particle = new RiftBurstParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(sprite);
            return particle;
        }
    }
}
