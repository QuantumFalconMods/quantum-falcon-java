package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.client.model.QuantumFalconModel;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib Renderer for Quantum Falcon entity.
 * Handles model, texture, and animation rendering.
 */
public class QuantumFalconRenderer extends GeoEntityRenderer<QuantumFalconEntity> {

    public QuantumFalconRenderer(EntityRendererProvider.Context context) {
        super(context, new QuantumFalconModel());
        
        // Add glow layer for quantum effect
        addRenderLayer(new QuantumGlowLayer(this));
        
        // Add anomaly rainbow layer
        addRenderLayer(new AnomalyRainbowLayer(this));

        // Falcon barding (body slot)
        addRenderLayer(new BardingLayer(this));

        // Story ending sheen (Dark: a violet sheen over the plumage; True: a faint gold one)
        addRenderLayer(new EndingSheenLayer(this));
    }

    /** A translucent emissive pass of the falcon's own texture, tinted by its owner's ending. */
    public static class EndingSheenLayer extends GeoRenderLayer<QuantumFalconEntity> {
        public EndingSheenLayer(GeoEntityRenderer<QuantumFalconEntity> renderer) { super(renderer); }

        @Override
        public void render(PoseStack poseStack, QuantumFalconEntity entity, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {
            int end = entity.endingLook();
            if (end < 2 || entity.isInvisible()) return;
            float t = entity.tickCount + partialTick;
            int a = (int) ((end == 2 ? 70 : 45) + 25 * Math.sin(t * 0.07f));
            int rgb = end == 2 ? 0x9A48FF : 0xFFC63A;
            RenderType sheen = RenderType.entityTranslucentEmissive(getRenderer().getTextureLocation(entity));
            getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, sheen, bufferSource.getBuffer(sheen),
                    partialTick, packedLight, packedOverlay, (a << 24) | rgb);
        }
    }

    /** Draws the equipped falcon barding over the body, neck and head cap. */
    public static class BardingLayer extends GeoRenderLayer<QuantumFalconEntity> {
        public BardingLayer(GeoEntityRenderer<QuantumFalconEntity> renderer) { super(renderer); }

        @Override
        public void render(PoseStack poseStack, QuantumFalconEntity entity, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {
            if (entity.isInvisible()) return;   // emissive/overlay layers must not reveal an invisible falcon
            var tier = entity.bardingTier();
            if (tier == null) return;
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/falcon_barding/" + tier.id + ".png");
            RenderType armor = RenderType.armorCutoutNoCull(tex);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, armor, bufferSource.getBuffer(armor),
                    partialTick, packedLight, packedOverlay, 0xFFFFFFFF);
            if (tier == com.matt.quantumfalcon.item.FalconBardingItem.Tier.QUANTUM || tier == com.matt.quantumfalcon.item.FalconBardingItem.Tier.NETHERITE) {
                RenderType glow = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/falcon_barding/" + tier.id + "_glowmask.png"));
                getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, glow, bufferSource.getBuffer(glow),
                        partialTick, packedLight, packedOverlay, 0xFFFFFFFF);
            }
        }
    }

    /** Delegates to model's getTextureResource so variant (and anomaly) logic lives in one place. */
    @Override
    public ResourceLocation getTextureLocation(QuantumFalconEntity entity) {
        return ((QuantumFalconModel) getGeoModel()).getTextureResource(entity);
    }

    @Override
    public void preRender(PoseStack poseStack, QuantumFalconEntity entity, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        // v2 rig: feet rest at y=0, ~20px tall standing; scale to ~0.9 blocks.
        // Layers re-render with the already-transformed pose stack, so only transform on the main pass
        if (!isReRender) {
            float arch = com.matt.quantumfalcon.entity.FalconArchetype.of(entity).size;
            poseStack.scale(0.7f * arch, 0.7f * arch, 0.7f * arch);
            // Male: slightly larger crest (approximated as whole-body 1.02x; per-bone crest scale would need model animation)
            if (entity.isMale() && !entity.isBaby()) {
                poseStack.scale(1.02f, 1.02f, 1.02f);
            }
            // Baby scale on top of base scale
            if (entity.isBaby()) {
                poseStack.scale(0.5f, 0.5f, 0.5f);
            }
        }
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
            partialTick, packedLight, packedOverlay, colour);
    }

    /** Bird-style banking: after the yaw is applied, pitch the whole falcon by its smoothed climb/descent angle
     *  (skipped for held qfpose animations, which already pose the body). */
    @Override
    protected void applyRotations(QuantumFalconEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        float pitch = net.minecraft.util.Mth.lerp(partialTick, entity.flightPitchO, entity.flightPitch);
        if (Math.abs(pitch) > 0.05f && !(entity.hasCustomName() && entity.getCustomName().getString().startsWith("qfpose:"))) {
            float pivot = 0.35f * nativeScale;
            poseStack.translate(0, pivot, 0);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
            poseStack.translate(0, -pivot, 0);
        }
        // banking turn: roll into the turn by the smoothed yaw rate ("qfroll:<deg>" names force a roll for screenshots)
        float roll = net.minecraft.util.Mth.lerp(partialTick, entity.bankRollO, entity.bankRoll);
        if (entity.hasCustomName() && entity.getCustomName().getString().startsWith("qfroll:")) {
            try { roll = Float.parseFloat(entity.getCustomName().getString().substring(7)); } catch (NumberFormatException ignored) { }
        }
        if (Math.abs(roll) > 0.05f) {
            float pivot = 0.35f * nativeScale;
            poseStack.translate(0, pivot, 0);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(BANK_SIGN * roll));
            poseStack.translate(0, -pivot, 0);
        }
    }
    /** +1/-1: which way a positive yaw rate (turning right) rolls the model; checked with qfroll screenshots. */
    private static final float BANK_SIGN = 1f;

    @Override
    public void render(QuantumFalconEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Adjust rendering for sitting
        if (entity.isInSittingPose()) {
            poseStack.translate(0, -0.1, 0);
        }
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /**
     * HSB to RGB conversion without java.awt.Color (which crashes dedicated servers).
     */
    private static int hsbToRgb(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0 -> { r = (int) (brightness * 255 + 0.5f); g = (int) (t * 255 + 0.5f); b = (int) (p * 255 + 0.5f); }
                case 1 -> { r = (int) (q * 255 + 0.5f); g = (int) (brightness * 255 + 0.5f); b = (int) (p * 255 + 0.5f); }
                case 2 -> { r = (int) (p * 255 + 0.5f); g = (int) (brightness * 255 + 0.5f); b = (int) (t * 255 + 0.5f); }
                case 3 -> { r = (int) (p * 255 + 0.5f); g = (int) (q * 255 + 0.5f); b = (int) (brightness * 255 + 0.5f); }
                case 4 -> { r = (int) (t * 255 + 0.5f); g = (int) (p * 255 + 0.5f); b = (int) (brightness * 255 + 0.5f); }
                case 5 -> { r = (int) (brightness * 255 + 0.5f); g = (int) (p * 255 + 0.5f); b = (int) (q * 255 + 0.5f); }
            }
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ============ Inner Classes ============

    /**
     * Legacy renderer-specific model wrapper (no longer used directly).
     */
    public static class QuantumFalconRendererModel extends QuantumFalconModel {
        public QuantumFalconRendererModel() {
            super();
        }
    }
    
    /**
     * Quantum glow render layer
     */
    public static class QuantumGlowLayer extends GeoRenderLayer<QuantumFalconEntity> {

        private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            QuantumFalcon.MOD_ID, "textures/entity/quantum_falcon/glow_overlay.png");

        public QuantumGlowLayer(GeoEntityRenderer<QuantumFalconEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, QuantumFalconEntity entity, BakedGeoModel bakedModel,
                          RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                          float partialTick, int packedLight, int packedOverlay) {
            // Neon accents (variant glow mask): every falcon breathes softly; rare variants get a sharper "crack pulse"
            // that surges through the fractures every few seconds and peaks during their flourish; bonded falcons glow brighter.
            if (entity.isInvisible()) return;
            float t = entity.tickCount + partialTick + entity.getId() * 13f;
            float b;
            if (entity.isRareVariant()) {
                float surge = (float) Math.pow(Math.max(0, Math.sin(t * (entity.isLegendaryVariant() ? 0.11f : 0.08f))), 6);
                b = 0.5f + 0.15f * (float) Math.sin(t * 0.21f) + 0.5f * surge;
                float fl = entity.flourishProgress(partialTick);
                if (fl >= 0f) b = Math.max(b, 0.6f + 0.4f * (float) Math.sin(Math.PI * fl) + 0.15f * (float) Math.sin(t * 1.7f));
            } else {
                b = 0.62f + 0.18f * (float) Math.sin(t * 0.09f);
            }
            if (entity.getBondLevel() >= 10) b += 0.15f;
            int v = (int) (255 * Math.max(0f, Math.min(1f, b)));
            int colour = 0xFF000000 | (v << 16) | (v << 8) | v;
            // Story ending looks: the neon accents take the ending's colour (Light warm white, Dark violet, True gold)
            int tint = switch (entity.endingLook()) { case 1 -> 0xFFF2D8; case 2 -> 0xB45CFF; case 3 -> 0xFFC63A; default -> 0xFFFFFF; };
            if (tint != 0xFFFFFF) colour = 0xFF000000 | ((((tint >> 16) & 255) * v / 255) << 16) | ((((tint >> 8) & 255) * v / 255) << 8) | ((tint & 255) * v / 255);
            ResourceLocation tex = ((QuantumFalconModel) getRenderer().getGeoModel()).getGlowTexture(entity);
            RenderType glowType = RenderType.eyes(tex);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, entity,
                glowType, bufferSource.getBuffer(glowType), partialTick,
                0xF000F0, packedOverlay, colour);
        }
    }

    /**
     * Anomaly rainbow shimmer layer
     */
    public static class AnomalyRainbowLayer extends GeoRenderLayer<QuantumFalconEntity> {

        private static final ResourceLocation RAINBOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            QuantumFalcon.MOD_ID, "textures/entity/quantum_falcon/anomaly_shimmer.png");

        public AnomalyRainbowLayer(GeoEntityRenderer<QuantumFalconEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, QuantumFalconEntity entity, BakedGeoModel bakedModel,
                          RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                          float partialTick, int packedLight, int packedOverlay) {
            // Only render for anomaly falcons
            if (!entity.isAnomaly() || entity.isInvisible()) return;
            
            // Rainbow hue shift (manual HSB→RGB - no java.awt.Color which crashes dedicated servers)
            float hue = (entity.tickCount % 100) / 100.0f;
            int color = hsbToRgb(hue, 0.7f, 1.0f);
            
            RenderType shimmerType = RenderType.entityTranslucentEmissive(RAINBOW_TEXTURE);
            
            getRenderer().reRender(bakedModel, poseStack, bufferSource, entity,
                shimmerType, bufferSource.getBuffer(shimmerType), partialTick,
                packedLight, packedOverlay, color | 0x80000000);
        }
    }
}
