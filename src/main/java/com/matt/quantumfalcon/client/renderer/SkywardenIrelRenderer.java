package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.boss.SkywardenIrelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Skywarden Irel: a falconer in Eastwatch's amber and dark timber colours (texture by tools/gen_irel_skin.py, our own),
 * on the vanilla humanoid body. Frozen mid-command, her right arm is raised to the east; her gloved left arm stays out as a
 * perch. A little taller than a player.
 */
public class SkywardenIrelRenderer extends HumanoidMobRenderer<SkywardenIrelEntity, SkywardenIrelRenderer.Model> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/skywarden_irel.png");

    public SkywardenIrelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override public ResourceLocation getTextureLocation(SkywardenIrelEntity e) { return TEX; }
    @Override protected void scale(SkywardenIrelEntity e, PoseStack pose, float partial) { pose.scale(1.08f, 1.08f, 1.08f); }

    public static class Model extends HumanoidModel<SkywardenIrelEntity> {
        public Model(net.minecraft.client.model.geom.ModelPart root) { super(root); }

        @Override
        public void setupAnim(SkywardenIrelEntity e, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
            super.setupAnim(e, limbSwing, limbAmount, age, headYaw, headPitch);
            // gloved left forearm held out as a perch
            leftArm.xRot = -1.1f + Mth.cos(age * 0.05f) * 0.03f; leftArm.yRot = 0.35f;
            if (e.isCommanding()) {   // the command: right arm straight up and a little forward
                rightArm.xRot = -2.85f + Mth.sin(age * 0.08f) * 0.04f; rightArm.yRot = -0.1f; rightArm.zRot = 0.1f;
            }
        }
    }
}
