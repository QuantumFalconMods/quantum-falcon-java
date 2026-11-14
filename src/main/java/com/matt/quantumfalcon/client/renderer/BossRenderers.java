package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.boss.*;
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
 * GeckoLib Renderers for all Quantum Falcon boss entities.
 */
public class BossRenderers {

    // ============ QUANTUM OVERSEER ============
    
    public static class QuantumOverseerRenderer extends GeoEntityRenderer<QuantumOverseerEntity> {

        public QuantumOverseerRenderer(EntityRendererProvider.Context context) {
            super(context, new QuantumOverseerModel());
            addRenderLayer(new OverseerEyeGlowLayer(this));
        }

        @Override
        public ResourceLocation getTextureLocation(QuantumOverseerEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID,
                "textures/entity/boss/quantum_overseer.png");
        }

        @Override
        public void preRender(PoseStack poseStack, QuantumOverseerEntity entity, BakedGeoModel model,
                              MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                              float partialTick, int packedLight, int packedOverlay, int colour) {
            // Scale up for boss presence
            poseStack.scale(1.7f, 1.7f, 1.7f);
            super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
        }

        public static class QuantumOverseerModel extends GeoModel<QuantumOverseerEntity> {
            private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "geo/entity/quantum_overseer.geo.json");
            private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/boss/quantum_overseer.png");
            private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "animations/entity/quantum_overseer.animation.json");
            @Override public ResourceLocation getModelResource(QuantumOverseerEntity e) { return MODEL; }
            @Override public ResourceLocation getTextureResource(QuantumOverseerEntity e) { return TEXTURE; }
            @Override public ResourceLocation getAnimationResource(QuantumOverseerEntity e) { return ANIMATION; }
        }

        public static class OverseerEyeGlowLayer extends GeoRenderLayer<QuantumOverseerEntity> {
            private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
                QuantumFalcon.MOD_ID, "textures/entity/boss/quantum_overseer_glow.png");

            public OverseerEyeGlowLayer(GeoEntityRenderer<QuantumOverseerEntity> renderer) {
                super(renderer);
            }

            @Override
            public void render(PoseStack poseStack, QuantumOverseerEntity entity, BakedGeoModel bakedModel,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
                float pulse = (float) Math.sin(entity.tickCount * 0.15f) * 0.3f + 0.7f;
                int alpha = (int) (pulse * 200);
                RenderType glowType = RenderType.eyes(GLOW);
                getRenderer().reRender(bakedModel, poseStack, bufferSource, entity,
                    glowType, bufferSource.getBuffer(glowType), partialTick,
                    packedLight, packedOverlay, 0x9966FF | (alpha << 24));
            }
        }
    }

    // ============ RIFT WARDEN ============
    
    public static class RiftWardenRenderer extends GeoEntityRenderer<RiftWardenEntity> {

        public RiftWardenRenderer(EntityRendererProvider.Context context) {
            super(context, new RiftWardenModel());
            addRenderLayer(new RiftCoreGlowLayer(this));
        }

        private static final ResourceLocation WARDEN_TEX = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/boss/rift_warden.png");
        private static final ResourceLocation WARDEN_ANGER_TEX = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/boss/rift_warden_anger.png");

        /** March 2026 remodel: Matthew's "anger" skin is used once the Warden enrages (phase 3). */
        @Override
        public ResourceLocation getTextureLocation(RiftWardenEntity entity) {
            return entity.getPhase() >= 3 ? WARDEN_ANGER_TEX : WARDEN_TEX;
        }

        @Override
        public void preRender(PoseStack poseStack, RiftWardenEntity entity, BakedGeoModel model,
                              MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                              float partialTick, int packedLight, int packedOverlay, int colour) {
            poseStack.scale(0.72f, 0.72f, 0.72f); // ~6 blocks tall natively; 4.3 blocks rendered so it towers over its minions (hitbox 3.5)
            super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
        }

        public static class RiftWardenModel extends GeoModel<RiftWardenEntity> {
            private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "geo/entity/rift_warden.geo.json");
            private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/boss/rift_warden.png");
            private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "animations/entity/rift_warden.animation.json");
            @Override public ResourceLocation getModelResource(RiftWardenEntity e) { return MODEL; }
            @Override public ResourceLocation getTextureResource(RiftWardenEntity e) { return e.getPhase() >= 3 ? WARDEN_ANGER_TEX : TEXTURE; }
            @Override public ResourceLocation getAnimationResource(RiftWardenEntity e) { return ANIMATION; }
        }

        public static class RiftCoreGlowLayer extends GeoRenderLayer<RiftWardenEntity> {
            private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
                QuantumFalcon.MOD_ID, "textures/entity/boss/rift_warden_glow.png");

            public RiftCoreGlowLayer(GeoEntityRenderer<RiftWardenEntity> renderer) {
                super(renderer);
            }

            @Override
            public void render(PoseStack poseStack, RiftWardenEntity entity, BakedGeoModel bakedModel,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
                float pulse = (float) Math.sin(entity.tickCount * 0.1f) * 0.2f + 0.8f;
                int alpha = (int) (pulse * 180);
                RenderType glowType = RenderType.eyes(GLOW);
                getRenderer().reRender(bakedModel, poseStack, bufferSource, entity,
                    glowType, bufferSource.getBuffer(glowType), partialTick,
                    packedLight, packedOverlay, 0x00FFFF | (alpha << 24));
            }
        }
    }

    // ============ ENTANGLED TWINS ============
    
    public static class EntangledTwinsRenderer extends GeoEntityRenderer<EntangledTwinsEntity> {

        public EntangledTwinsRenderer(EntityRendererProvider.Context context) {
            super(context, new EntangledTwinsModel());
            this.shadowRadius = 1.5f;
            addRenderLayer(new EntangledTwinsGlowLayer(this));
        }

        @Override
        public ResourceLocation getTextureLocation(EntangledTwinsEntity entity) {
            return entity.isCyanTwin()
                ? ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/boss/entangled_twins_cyan.png")
                : ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/boss/entangled_twins_magenta.png");
        }

        @Override
        public void preRender(PoseStack poseStack, EntangledTwinsEntity entity, BakedGeoModel model,
                              MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                              float partialTick, int packedLight, int packedOverlay, int colour) {
            poseStack.scale(1.7f, 1.7f, 1.7f);
            super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
        }

        public static class EntangledTwinsModel extends GeoModel<EntangledTwinsEntity> {
            private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "geo/entity/entangled_twins.geo.json");
            private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "animations/entity/entangled_twins.animation.json");
            @Override public ResourceLocation getModelResource(EntangledTwinsEntity e) { return MODEL; }
            @Override public ResourceLocation getTextureResource(EntangledTwinsEntity e) {
                return e.isCyanTwin()
                    ? ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/boss/entangled_twins_cyan.png")
                    : ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/boss/entangled_twins_magenta.png");
            }
            @Override public ResourceLocation getAnimationResource(EntangledTwinsEntity e) { return ANIMATION; }
        }

        public static class EntangledTwinsGlowLayer extends GeoRenderLayer<EntangledTwinsEntity> {
            private static final ResourceLocation CYAN_GLOW = ResourceLocation.fromNamespaceAndPath(
                QuantumFalcon.MOD_ID, "textures/entity/boss/entangled_twins_cyan_glow.png");
            private static final ResourceLocation MAGENTA_GLOW = ResourceLocation.fromNamespaceAndPath(
                QuantumFalcon.MOD_ID, "textures/entity/boss/entangled_twins_magenta_glow.png");

            public EntangledTwinsGlowLayer(GeoEntityRenderer<EntangledTwinsEntity> renderer) {
                super(renderer);
            }

            @Override
            public void render(PoseStack poseStack, EntangledTwinsEntity entity, BakedGeoModel bakedModel,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
                ResourceLocation glow = entity.isCyanTwin() ? CYAN_GLOW : MAGENTA_GLOW;
                float pulse = (float) Math.sin(entity.tickCount * 0.12f) * 0.25f + 0.75f;
                int alpha = (int) (pulse * 220);
                int color = entity.isCyanTwin() ? (0x00FFFF | (alpha << 24)) : (0xFF00FF | (alpha << 24));
                RenderType glowType = RenderType.eyes(glow);
                getRenderer().reRender(bakedModel, poseStack, bufferSource, entity,
                    glowType, bufferSource.getBuffer(glowType), partialTick,
                    packedLight, packedOverlay, color);
            }
        }
    }

    // ============ SINGULARITY SOVEREIGN ============
    
    public static class SingularitySovereignRenderer extends GeoEntityRenderer<SingularitySovereignEntity> {

        public SingularitySovereignRenderer(EntityRendererProvider.Context context) {
            super(context, new SingularitySovereignModel());
            this.shadowRadius = 2.5f;
            addRenderLayer(new SingularityGlowLayer(this));
            addRenderLayer(new SingularityCoreLayer(this));
            addRenderLayer(new EventHorizonLayer(this));
        }

        @Override
        public ResourceLocation getTextureLocation(SingularitySovereignEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID,
                "textures/entity/boss/singularity_sovereign.png");
        }

        @Override
        public void preRender(PoseStack poseStack, SingularitySovereignEntity entity, BakedGeoModel model,
                              MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                              float partialTick, int packedLight, int packedOverlay, int colour) {
            // Massive scale for final boss
            poseStack.scale(2.5f, 2.5f, 2.5f);
            super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
        }

        public static class SingularitySovereignModel extends GeoModel<SingularitySovereignEntity> {
            private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "geo/entity/singularity_sovereign.geo.json");
            private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/boss/singularity_sovereign.png");
            private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "animations/entity/singularity_sovereign.animation.json");
            @Override public ResourceLocation getModelResource(SingularitySovereignEntity e) { return MODEL; }
            @Override public ResourceLocation getTextureResource(SingularitySovereignEntity e) { return TEXTURE; }
            @Override public ResourceLocation getAnimationResource(SingularitySovereignEntity e) { return ANIMATION; }
        }

        public static class SingularityCoreLayer extends GeoRenderLayer<SingularitySovereignEntity> {
            private static final ResourceLocation CORE = ResourceLocation.fromNamespaceAndPath(
                QuantumFalcon.MOD_ID, "textures/entity/boss/singularity_core_glow.png");

            public SingularityCoreLayer(GeoEntityRenderer<SingularitySovereignEntity> renderer) {
                super(renderer);
            }

            @Override
            public void render(PoseStack poseStack, SingularitySovereignEntity entity, BakedGeoModel bakedModel,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
                // Intense pulsing for the singularity core
                float pulse = (float) Math.sin(entity.tickCount * 0.2f) * 0.4f + 0.6f;
                int alpha = (int) (pulse * 255);
                RenderType coreType = RenderType.eyes(CORE);
                getRenderer().reRender(bakedModel, poseStack, bufferSource, entity,
                    coreType, bufferSource.getBuffer(coreType), partialTick,
                    packedLight, packedOverlay, 0x000000 | (alpha << 24));
            }
        }

        public static class SingularityGlowLayer extends GeoRenderLayer<SingularitySovereignEntity> {
            private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
                QuantumFalcon.MOD_ID, "textures/entity/boss/singularity_sovereign_glow.png");

            public SingularityGlowLayer(GeoEntityRenderer<SingularitySovereignEntity> renderer) {
                super(renderer);
            }

            @Override
            public void render(PoseStack poseStack, SingularitySovereignEntity entity, BakedGeoModel bakedModel,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
                RenderType glowType = RenderType.eyes(GLOW);
                getRenderer().reRender(bakedModel, poseStack, bufferSource, entity,
                    glowType, bufferSource.getBuffer(glowType), partialTick,
                    packedLight, packedOverlay, 0xFFFFFFFF);
            }
        }

        public static class EventHorizonLayer extends GeoRenderLayer<SingularitySovereignEntity> {
            private static final ResourceLocation HORIZON = ResourceLocation.fromNamespaceAndPath(
                QuantumFalcon.MOD_ID, "textures/entity/boss/event_horizon.png");

            public EventHorizonLayer(GeoEntityRenderer<SingularitySovereignEntity> renderer) {
                super(renderer);
            }

            @Override
            public void render(PoseStack poseStack, SingularitySovereignEntity entity, BakedGeoModel bakedModel,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
                float rotation = entity.tickCount * 0.05f;
                poseStack.pushPose();
                poseStack.mulPose(com.mojang.math.Axis.YP.rotation(rotation));
                
                RenderType horizonType = RenderType.entityTranslucentEmissive(HORIZON);
                getRenderer().reRender(bakedModel, poseStack, bufferSource, entity,
                    horizonType, bufferSource.getBuffer(horizonType), partialTick,
                    packedLight, packedOverlay, 0xFF6600 | 0x80000000);
                    
                poseStack.popPose();
            }
        }
    }
}
