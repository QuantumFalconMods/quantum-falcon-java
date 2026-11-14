package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.function.Function;

/**
 * Renderer for the v3 bosses rebuilt from code (ART_STYLE.md): explicit geo/animation paths, a texture chooser
 * (e.g. the Rift Warden's enraged skin), uniform scale, and the pulsing neon glowmask layer. No vanilla death flip:
 * every boss has its own death animation.
 */
public class QFBossGeoRenderer<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T> {
    public QFBossGeoRenderer(EntityRendererProvider.Context ctx, String geo, Function<T, String> texture, float scale, float shadow) {
        super(ctx, new GeoModel<T>() {
            final ResourceLocation g = rl("geo/entity/" + geo + ".geo.json"), a = rl("animations/entity/" + geo + ".animation.json");
            @Override public ResourceLocation getModelResource(T e) { return g; }
            @Override public ResourceLocation getAnimationResource(T e) { return a; }
            @Override public ResourceLocation getTextureResource(T e) { return rl("textures/entity/" + texture.apply(e) + ".png"); }
        });
        this.scaleWidth = scale; this.scaleHeight = scale; this.shadowRadius = shadow;
        addRenderLayer(new PulsingGlowLayer<>(this));
    }

    static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, p); }

    @Override protected float getDeathMaxRotation(T e) { return 0f; }
}
