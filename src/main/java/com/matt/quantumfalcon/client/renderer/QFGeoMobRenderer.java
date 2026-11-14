package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.QuantumFalcon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.function.ToDoubleFunction;

/**
 * GeckoLib renderer for Quantum Falcon's original mob bodies: geo/animation/texture resolved from
 * {@code <path>} (e.g. "mob/void_echo"), emissive {@code <texture>_glowmask.png}, optional per-entity scale.
 * The vanilla death flip is disabled because every body has its own death animation.
 */
public class QFGeoMobRenderer<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T> {
    private final ToDoubleFunction<T> scale;

    public QFGeoMobRenderer(EntityRendererProvider.Context ctx, String path, float shadow) {
        this(ctx, path, shadow, e -> 1.0);
    }

    public QFGeoMobRenderer(EntityRendererProvider.Context ctx, String path, float shadow, ToDoubleFunction<T> scale) {
        super(ctx, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, path)));
        this.scale = scale;
        this.shadowRadius = shadow;
        addRenderLayer(new PulsingGlowLayer<>(this));
    }

    @Override
    public void preRender(PoseStack poseStack, T entity, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (!isReRender) {
            float s = (float) scale.applyAsDouble(entity);
            this.scaleWidth = s; this.scaleHeight = s;
        }
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    protected float getDeathMaxRotation(T entity) { return 0f; }
}
