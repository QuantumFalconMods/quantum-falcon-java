package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.entity.boss.QFBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * ART_STYLE.md section 5: the emissive glowmask (neon cracks, runes, eyes, cores) breathes with a slow pulse and
 * an occasional glitch flicker; bosses pulse faster in later phases. Our own layer on top of GeckoLib's auto-glow.
 */
public class PulsingGlowLayer<T extends GeoAnimatable> extends AutoGlowingGeoLayer<T> {
    public PulsingGlowLayer(GeoRenderer<T> renderer) { super(renderer); }

    public static float brightness(Object animatable, float partialTick) {
        if (!(animatable instanceof Entity e)) return 1f;
        int phase = e instanceof QFBoss b ? Math.max(1, b.qfPhase()) : 1;
        float t = e.tickCount + partialTick;
        float speed = 0.08f + 0.05f * (phase - 1);
        float b = 0.74f + 0.26f * (float) Math.sin(t * speed + e.getId() * 1.7f);
        int slot = (e.tickCount / 3) ^ (e.getId() * 0x9E3779B9);
        if (((slot * 1103515245 + 12345) >>> 16 & 0xFF) < 6) b *= 0.35f;   // glitch flicker (~2% of 3-tick slots)
        return b;
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource,
                       VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        RenderType glow = getRenderType(animatable, bufferSource);
        if (glow == null) return;
        int v = (int) (255 * Math.min(1f, brightness(animatable, partialTick)));
        int colour = 0xFF000000 | (v << 16) | (v << 8) | v;
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow, bufferSource.getBuffer(glow), partialTick,
            0xF000F0, packedOverlay, colour);
    }
}
