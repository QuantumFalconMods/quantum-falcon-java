package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * Generic mob renderer that uses humanoid model.
 * Used as placeholder for custom mobs until proper models are created.
 * NeoForge 1.21.1 API - Uses HumanoidModel for flexibility
 */
public class GenericMobRenderer<T extends Mob> extends MobRenderer<T, HumanoidModel<T>> {
    
    private final String textureName;

    @SuppressWarnings("unchecked")
    public GenericMobRenderer(EntityRendererProvider.Context context, String textureName, float shadowRadius) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), shadowRadius);
        this.textureName = textureName;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, 
            "textures/entity/" + textureName + ".png");
    }
}
