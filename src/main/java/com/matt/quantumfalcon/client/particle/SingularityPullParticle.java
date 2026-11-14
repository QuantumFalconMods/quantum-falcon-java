package com.matt.quantumfalcon.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Singularity Pull Particle - Reverse portal swirl that pulls inward.
 * Used for Singularity Sovereign's gravity well ability.
 */
public class SingularityPullParticle extends TextureSheetParticle {
    
    private final double centerX, centerY, centerZ;
    private final float pullStrength;
    
    protected SingularityPullParticle(ClientLevel level, double x, double y, double z,
                                     double centerX, double centerY, double centerZ,
                                     double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.pullStrength = 0.05f + random.nextFloat() * 0.05f;
        this.lifetime = 40 + random.nextInt(20);
        this.gravity = 0.0f;
        this.hasPhysics = false;
        
        // Dark purple/black with white center
        this.rCol = 0.2f;
        this.gCol = 0.0f;
        this.bCol = 0.3f;
        this.alpha = 0.7f;
        this.scale(0.2f);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Pull towards center
        double dx = centerX - x;
        double dy = centerY - y;
        double dz = centerZ - z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (dist > 0.1) {
            this.xd += (dx / dist) * pullStrength;
            this.yd += (dy / dist) * pullStrength;
            this.zd += (dz / dist) * pullStrength;
        }
        
        // Spin effect
        double angle = age * 0.2;
        this.xd += Math.cos(angle) * 0.01;
        this.zd += Math.sin(angle) * 0.01;
        
        // Fade and shrink as it approaches center
        float progress = (float)age / lifetime;
        this.alpha = 0.7f * (1.0f - progress);
        this.scale(0.2f * (1.0f - progress * 0.5f));
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
            // Use xd, yd, zd as center coordinates
            SingularityPullParticle particle = new SingularityPullParticle(
                level, x, y, z, xd, yd, zd, 0, 0, 0);
            particle.pickSprite(sprite);
            return particle;
        }
    }
}
