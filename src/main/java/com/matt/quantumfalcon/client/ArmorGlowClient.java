package com.matt.quantumfalcon.client;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.client.renderer.layer.ArmorGlowLayer;
import net.minecraft.client.model.ArmorStandArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** Adds the emissive armour overlay to players, armour stands and the common armour-wearing mobs. */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public final class ArmorGlowClient {
    private ArmorGlowClient() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        var models = event.getEntityModels();
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof LivingEntityRenderer r) {
                boolean slim = skin == PlayerSkin.Model.SLIM;
                r.addLayer(new ArmorGlowLayer(r,
                    new HumanoidModel(models.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel(models.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR))));
            }
        }
        if (event.getRenderer(EntityType.ARMOR_STAND) instanceof LivingEntityRenderer r)
            r.addLayer(new ArmorGlowLayer(r, new ArmorStandArmorModel(models.bakeLayer(ModelLayers.ARMOR_STAND_INNER_ARMOR)),
                new ArmorStandArmorModel(models.bakeLayer(ModelLayers.ARMOR_STAND_OUTER_ARMOR))));
        addMob(event, EntityType.ZOMBIE, ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR);
        addMob(event, EntityType.SKELETON, ModelLayers.SKELETON_INNER_ARMOR, ModelLayers.SKELETON_OUTER_ARMOR);
        addMob(event, EntityType.HUSK, ModelLayers.HUSK_INNER_ARMOR, ModelLayers.HUSK_OUTER_ARMOR);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addMob(EntityRenderersEvent.AddLayers event, EntityType<?> type,
                               net.minecraft.client.model.geom.ModelLayerLocation in, net.minecraft.client.model.geom.ModelLayerLocation out) {
        if (event.getRenderer((EntityType) type) instanceof LivingEntityRenderer r && r.getModel() instanceof HumanoidModel)
            r.addLayer(new ArmorGlowLayer(r, new HumanoidModel(event.getEntityModels().bakeLayer(in)), new HumanoidModel(event.getEntityModels().bakeLayer(out))));
    }

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> ArmorGlowLayer.clearCache());
    }
}
