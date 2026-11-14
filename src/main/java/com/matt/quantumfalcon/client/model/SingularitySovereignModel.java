package com.matt.quantumfalcon.client.model;

import com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SingularitySovereignModel extends GeoModel<SingularitySovereignEntity> {

    // Geo file lives at assets/quantumfalcon/geo/entity/singularity_sovereign.geo.json
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            "quantumfalcon", "geo/entity/singularity_sovereign.geo.json");

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "quantumfalcon", "textures/entity/boss/singularity_sovereign.png");

    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            "quantumfalcon", "animations/entity/singularity_sovereign.animation.json");

    @Override
    public ResourceLocation getModelResource(SingularitySovereignEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SingularitySovereignEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SingularitySovereignEntity entity) {
        return ANIMATION;
    }
}

