package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.item.gear.armor.QFGeoArmorItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** GeckoLib armour renderer for {@link QFGeoArmorItem}: model + emissive glowmask layer. */
public class QFGeoArmorRenderer extends GeoArmorRenderer<QFGeoArmorItem> {
    public QFGeoArmorRenderer(String model) {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "armor/" + model)));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    public static GeoRenderProvider provider(String model) {
        return new GeoRenderProvider() {
            private QFGeoArmorRenderer renderer;
            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(T entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<T> original) {
                if (renderer == null) renderer = new QFGeoArmorRenderer(model);
                return renderer;
            }
        };
    }
}
