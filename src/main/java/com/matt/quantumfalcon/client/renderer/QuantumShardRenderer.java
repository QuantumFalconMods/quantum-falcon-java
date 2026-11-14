package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.entity.projectile.QuantumShardEntity;
import com.matt.quantumfalcon.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The Overseer's shard volley projectile: the Quantum Shard item, full-bright, spinning along its flight path.
 * (Before this existed the entity had no client renderer, and the first volley crashed every client in range.)
 */
public class QuantumShardRenderer extends EntityRenderer<QuantumShardEntity> {
    private final ItemRenderer items;
    private ItemStack stack;

    public QuantumShardRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.items = ctx.getItemRenderer();
        this.shadowRadius = 0.1f;
    }

    @Override
    public void render(QuantumShardEntity shard, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (stack == null) stack = new ItemStack(ModItems.QUANTUM_SHARD.get());
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.ZP.rotationDegrees((shard.tickCount + partialTick) * 30f));
        pose.scale(0.9f, 0.9f, 0.9f);
        items.renderStatic(stack, ItemDisplayContext.GROUND, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffers, shard.level(), shard.getId());
        pose.popPose();
        super.render(shard, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(QuantumShardEntity shard) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
