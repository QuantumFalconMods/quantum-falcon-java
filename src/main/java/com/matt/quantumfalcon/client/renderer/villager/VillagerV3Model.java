package com.matt.quantumfalcon.client.renderer.villager;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Villagers v3 (GeckoLib): geo per profession, skin + glowmask per rank. Rank props (bones named "<cube>__t<n>") are hidden below
 * their rank; the hidden vanilla body box and hat_rim are left out of the geo (v3 reuses their texels). The head follows the look.
 */
public class VillagerV3Model<T extends GeoAnimatable> extends GeoModel<T> {
    private static final ResourceLocation ANIM = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "animations/entity/villager/rift_villager.animation.json");
    private final Function<T, String> profession;
    private final ToIntFunction<T> tier;

    public VillagerV3Model(Function<T, String> profession, ToIntFunction<T> tier) { this.profession = profession; this.tier = tier; }

    @Override public ResourceLocation getModelResource(T a) { return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "geo/entity/villager/" + profession.apply(a) + ".geo.json"); }
    @Override public ResourceLocation getTextureResource(T a) { return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/entity/villager/v3/" + profession.apply(a) + "_" + Math.max(1, Math.min(5, tier.applyAsInt(a))) + ".png"); }
    @Override public ResourceLocation getAnimationResource(T a) { return ANIM; }

    @Override
    public void setCustomAnimations(T a, long id, AnimationState<T> state) {
        super.setCustomAnimations(a, id, state);
        int t = tier.applyAsInt(a);
        for (GeoBone b : getAnimationProcessor().getRegisteredBones()) {
            int i = b.getName().lastIndexOf("__t");
            if (i > 0) b.setHidden(Integer.parseInt(b.getName().substring(i + 3)) > t);
        }
        GeoBone head = getAnimationProcessor().getBone("head");
        EntityModelData d = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head != null && d != null) {
            head.setRotX(head.getRotX() + d.headPitch() * net.minecraft.util.Mth.DEG_TO_RAD);
            head.setRotY(head.getRotY() + d.netHeadYaw() * net.minecraft.util.Mth.DEG_TO_RAD);
        }
    }
}
