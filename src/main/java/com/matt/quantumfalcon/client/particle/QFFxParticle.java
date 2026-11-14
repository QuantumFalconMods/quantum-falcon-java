package com.matt.quantumfalcon.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;

/**
 * Emissive, tinted FX particle used by weapons, abilities and armour set auras. The colour comes from the
 * {@link ColorParticleOption}; behaviour (size, life, motion, frame animation) comes from {@link Kind}.
 */
public class QFFxParticle extends TextureSheetParticle {
    public enum Kind { SLASH, GLINT, AURA, RUNE, RING, EMBER }

    private final SpriteSet sprites;
    private final Kind kind;
    private final float baseSize;

    protected QFFxParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                           SpriteSet sprites, Kind kind, float r, float g, float b) {
        super(level, x, y, z, 0, 0, 0);
        this.sprites = sprites; this.kind = kind;
        this.hasPhysics = false; this.gravity = 0;
        this.xd = xd; this.yd = yd; this.zd = zd;
        this.rCol = r; this.gCol = g; this.bCol = b;
        switch (kind) {
            case SLASH -> { lifetime = 7; baseSize = 0.5f + random.nextFloat() * 0.12f; roll = oRoll = (random.nextFloat() - 0.5f) * 1.2f; this.xd = this.yd = this.zd = 0; }
            case RING -> { lifetime = 12; baseSize = 1.0f; this.xd = this.yd = this.zd = 0; }
            case GLINT -> { lifetime = 8 + random.nextInt(8); baseSize = 0.06f + random.nextFloat() * 0.06f; friction = 0.86f; }
            case AURA -> { lifetime = 22 + random.nextInt(14); baseSize = 0.07f + random.nextFloat() * 0.06f; this.yd = 0.015 + random.nextFloat() * 0.02; friction = 0.96f; }
            case RUNE -> { lifetime = 26 + random.nextInt(10); baseSize = 0.16f; this.yd = 0.02; friction = 0.95f; roll = oRoll = random.nextFloat() * 6.28f; }
            default -> { lifetime = 14 + random.nextInt(10); baseSize = 0.08f + random.nextFloat() * 0.05f; this.yd = 0.03 + random.nextFloat() * 0.03; friction = 0.94f; }
        }
        quadSize = baseSize;
        if (kind == Kind.SLASH || kind == Kind.RING) setSpriteFromAge(sprites); else pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        float t = (float) age / lifetime;
        switch (kind) {
            case SLASH -> { setSpriteFromAge(sprites); quadSize = baseSize * (0.85f + 0.3f * t); alpha = 1f - t * t; }
            case RING -> { setSpriteFromAge(sprites); quadSize = baseSize * (0.5f + 2.2f * t); alpha = 1f - t; }
            case GLINT -> { quadSize = baseSize * (1f - t) * (0.8f + 0.4f * Mth.sin(age * 1.7f)); alpha = 1f - t * 0.6f; }
            case RUNE -> { oRoll = roll; roll += 0.05f; alpha = t < 0.2f ? t * 5 : 1f - (t - 0.2f) / 0.8f; }
            case AURA -> { alpha = Mth.sin(t * Mth.PI); quadSize = baseSize * (1f - 0.5f * t); }
            default -> { alpha = (1f - t) * (0.7f + 0.3f * Mth.sin(age * 2.3f)); xd += (random.nextFloat() - 0.5f) * 0.006; zd += (random.nextFloat() - 0.5f) * 0.006; }
        }
    }

    /** Shock rings lie flat on the ground instead of facing the camera. */
    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, net.minecraft.client.Camera camera, float partialTicks) {
        if (kind != Kind.RING) { super.render(buffer, camera, partialTicks); return; }
        org.joml.Quaternionf flat = new org.joml.Quaternionf().rotationX(-Mth.HALF_PI);
        renderRotatedQuad(buffer, camera, flat, partialTicks);
        renderRotatedQuad(buffer, camera, new org.joml.Quaternionf().rotationX(Mth.HALF_PI), partialTicks);   // visible from below too
    }

    @Override public int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites; private final Kind kind;
        public Provider(SpriteSet sprites, Kind kind) { this.sprites = sprites; this.kind = kind; }
        @Override
        public Particle createParticle(ColorParticleOption o, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new QFFxParticle(level, x, y, z, xd, yd, zd, sprites, kind, o.getRed(), o.getGreen(), o.getBlue());
        }
    }
}
