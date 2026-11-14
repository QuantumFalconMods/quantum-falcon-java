package com.matt.quantumfalcon.client.renderer.villager;

import com.matt.quantumfalcon.client.renderer.PulsingGlowLayer;
import com.matt.quantumfalcon.entity.npc.RiftVillagerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RiftVillagerRenderer extends GeoEntityRenderer<RiftVillagerEntity> {
    public RiftVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VillagerV3Model<>(RiftVillagerEntity::profession, RiftVillagerEntity::tier));
        this.shadowRadius = 0.5f;
        addRenderLayer(new PulsingGlowLayer<>(this));
    }
    @Override protected float getDeathMaxRotation(RiftVillagerEntity e) { return 0f; }
}
