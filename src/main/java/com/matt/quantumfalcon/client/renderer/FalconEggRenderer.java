package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.blockentity.FalconEggBlockEntity;
import com.matt.quantumfalcon.breeding.BreedingData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Falcon egg: model per rarity tier (tools/gen_eggs.py), shell texture per variant + tier, crack bones revealed by
 * hatch stage, and a neon glow pass tinted with the egg's crack colour that pulses faster as hatching nears. */
public class FalconEggRenderer extends GeoBlockRenderer<FalconEggBlockEntity> {
    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, p); }
    private static final ResourceLocation ANIM = rl("animations/falcon_egg.animation.json");
    private static final ResourceLocation[] GEO = new ResourceLocation[8], GLOW = new ResourceLocation[8];
    static { for (int t = 1; t <= 7; t++) { GEO[t] = rl("geo/falcon_egg_t" + t + ".geo.json"); GLOW[t] = rl("textures/block/falcon_egg/glow_t" + t + ".png"); } }

    /** An egg on a nest rests on the nest's seat (NestQuality.seatPx, near the rim) so it reads clearly on top. */
    @Override
    public void render(FalconEggBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        var nq = be.getLevel() == null ? null : com.matt.quantumfalcon.block.FalconNestBlock.qualityAt(be.getLevel(), be.getBlockPos().below());
        if (nq == null) { super.render(be, partialTick, pose, buf, light, overlay); return; }
        pose.pushPose();
        pose.translate(0.5, nq.seatPx / 16.0 - 1.0, 0.5);
        float sc = com.matt.quantumfalcon.block.FalconEggBlock.EGG_SCALE_ON_NEST;
        pose.scale(sc, sc, sc);
        pose.translate(-0.5, 0, -0.5);
        super.render(be, partialTick, pose, buf, light, overlay);
        pose.popPose();
    }

    static int tier(FalconEggBlockEntity be) { return Math.max(1, Math.min(7, be.clientTier)); }

    public FalconEggRenderer() {
        super(new GeoModel<>() {
            @Override public ResourceLocation getModelResource(FalconEggBlockEntity be) { return GEO[tier(be)]; }
            @Override public ResourceLocation getTextureResource(FalconEggBlockEntity be) {
                return rl("textures/block/falcon_egg/" + be.clientVariant + "_t" + tier(be) + ".png");
            }
            @Override public ResourceLocation getAnimationResource(FalconEggBlockEntity be) { return ANIM; }
            @Override public void setCustomAnimations(FalconEggBlockEntity be, long id, AnimationState<FalconEggBlockEntity> state) {
                for (int k = 1; k <= 4; k++) {
                    GeoBone b = getAnimationProcessor().getBone("crack_" + k);
                    if (b != null) b.setHidden(be.stage() < k);
                }
            }
        });
        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, FalconEggBlockEntity be, BakedGeoModel model, RenderType rt, MultiBufferSource buf,
                               VertexConsumer vc, float partialTick, int light, int overlay) {
                RenderType glow = RenderType.eyes(GLOW[tier(be)]);
                int c = BreedingData.CRACKS.get(Math.floorMod(be.clientCrack, BreedingData.CRACKS.size())).colour();
                long gt = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
                float speed = 0.06f + 0.05f * be.stage();
                // always brighter than the nest glow (nest peaks at 0.6 of its accent)
                float k = 0.80f + 0.20f * (float) Math.sin((gt + partialTick) * speed + be.getBlockPos().asLong() * 0.37);
                int r = (int) (((c >> 16) & 255) * k), g = (int) (((c >> 8) & 255) * k), b = (int) ((c & 255) * k);
                getRenderer().reRender(model, pose, buf, be, glow, buf.getBuffer(glow), partialTick, 0xF000F0, overlay,
                    0xFF000000 | (r << 16) | (g << 8) | b);
            }
        });
    }
}
