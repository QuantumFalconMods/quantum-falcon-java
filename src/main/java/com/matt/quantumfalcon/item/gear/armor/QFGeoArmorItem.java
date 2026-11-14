package com.matt.quantumfalcon.item.gear.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Armour piece rendered with a GeckoLib model (pauldrons, visors, crests, halos...). The model, texture,
 * glowmask and idle animation live under {@code geo|textures|animations/item/armor/<model>}.
 */
public class QFGeoArmorItem extends ArmorItem implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final String model;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public QFGeoArmorItem(Holder<ArmorMaterial> material, Type type, Properties props, String model) {
        super(material, type, props);
        this.model = model;
    }

    public String modelName() { return model; }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(com.matt.quantumfalcon.client.renderer.QFGeoArmorRenderer.provider(model));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
