package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.QuantumFalcon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Monster;

/**
 * Renderer for boss entities with glow effect.
 * Scales model and adds custom effects.
 * NeoForge 1.21.1 API - Uses HumanoidModel for flexibility
 */
public class BossRenderer<T extends Monster> extends MobRenderer<T, HumanoidModel<T>> {
    
    private final String textureName;
    private final float scale;

    @SuppressWarnings("unchecked")
    public BossRenderer(EntityRendererProvider.Context context, String textureName, float scale) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), scale * 0.5f);
        this.textureName = textureName;
        this.scale = scale;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, 
            "textures/entity/boss/" + textureName + ".png");
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
