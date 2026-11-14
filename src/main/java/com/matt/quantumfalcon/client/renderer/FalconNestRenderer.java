package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.blockentity.FalconNestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Nest renderer: model + texture per nest quality (tools/gen_nests.py) and a slow-breathing glow pass: the static grayscale glow tinted with the nest's accent. */
public class FalconNestRenderer extends GeoBlockRenderer<FalconNestBlockEntity> {
    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, p); }
    private static final ResourceLocation ANIM = rl("animations/falcon_nest.animation.json");

    public FalconNestRenderer() {
        super(new GeoModel<>() {
            @Override public ResourceLocation getModelResource(FalconNestBlockEntity be) { return rl("geo/" + be.quality().id + ".geo.json"); }
            @Override public ResourceLocation getTextureResource(FalconNestBlockEntity be) { return rl("textures/block/nest/" + be.quality().id + ".png"); }
            @Override public ResourceLocation getAnimationResource(FalconNestBlockEntity be) { return ANIM; }
        });
        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, FalconNestBlockEntity be, BakedGeoModel model, RenderType rt, MultiBufferSource buf,
                               VertexConsumer vc, float partialTick, int light, int overlay) {
                RenderType glow = RenderType.eyes(rl("textures/block/nest/" + be.quality().id + "_glow.png"));
                long gt = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
                // grayscale glow x nest accent (saturated, never washed pale); peaks at 60% so the egg glow (80-100%) always reads brighter
                int c = be.quality().accent();
                float k = 0.35f + 0.25f * (0.5f + 0.5f * (float) Math.sin((gt + partialTick) * 0.07 + be.getBlockPos().asLong() * 0.31));
                int r = (int) (((c >> 16) & 255) * k), g = (int) (((c >> 8) & 255) * k), b = (int) ((c & 255) * k);
                getRenderer().reRender(model, pose, buf, be, glow, buf.getBuffer(glow), partialTick, 0xF000F0, overlay,
                    0xFF000000 | (r << 16) | (g << 8) | b);
            }
        });
    }
}
