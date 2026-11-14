package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.QuantumWispEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib renderer for Quantum Wisp.
 * Uses quantum_wisp.geo.json and quantum_wisp.animation.json with neon glow layer (cyan/magenta/purple theme).
 */
public class WispRenderer extends GeoEntityRenderer<QuantumWispEntity> {

    public WispRenderer(EntityRendererProvider.Context context) {
        super(context, new QuantumWispModel());
        shadowRadius = 0.2f;
        addRenderLayer(new WispGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(QuantumWispEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/quantum_wisp.png");
    }

    @Override
    public void preRender(PoseStack poseStack, QuantumWispEntity entity, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        poseStack.scale(0.5f, 0.5f, 0.5f);
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
            partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    protected int getBlockLightLevel(QuantumWispEntity entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    public static class QuantumWispModel extends GeoModel<QuantumWispEntity> {
        private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            QuantumFalcon.MOD_ID, "geo/quantum_wisp.geo.json");
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            QuantumFalcon.MOD_ID, "textures/entity/quantum_wisp.png");
        private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            QuantumFalcon.MOD_ID, "animations/quantum_wisp.animation.json");

        @Override public ResourceLocation getModelResource(QuantumWispEntity e)     { return MODEL; }
        @Override public ResourceLocation getTextureResource(QuantumWispEntity e)   { return TEXTURE; }
        @Override public ResourceLocation getAnimationResource(QuantumWispEntity e) { return ANIMATION; }
    }

    public static class WispGlowLayer extends GeoRenderLayer<QuantumWispEntity> {
        private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
            QuantumFalcon.MOD_ID, "textures/entity/quantum_wisp_glow.png");

        public WispGlowLayer(GeoEntityRenderer<QuantumWispEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, QuantumWispEntity entity, BakedGeoModel bakedModel,
                          RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                          float partialTick, int packedLight, int packedOverlay) {
            float pulse = (float) Math.sin(entity.tickCount * 0.15f) * 0.25f + 0.75f;
            int alpha = (int) (pulse * 220);
            int color = (entity.getWispColor() & 0x00FFFFFF) | (alpha << 24);
            RenderType glowType = RenderType.eyes(GLOW);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, entity,
                glowType, bufferSource.getBuffer(glowType), partialTick,
                packedLight, packedOverlay, color);
        }
    }
}
