package com.matt.quantumfalcon.client.renderer.layer;

import com.matt.quantumfalcon.QuantumFalcon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Emissive overlay for Quantum Falcon armour: for every worn piece whose layer texture has a sibling
 * {@code <layer>_glow.png}, re-renders the armour model with that texture through the full-bright "eyes"
 * render type, gently pulsing. GeckoLib armour pieces are skipped (they use AutoGlowingGeoLayer instead).
 */
public class ArmorGlowLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final Map<ResourceLocation, ResourceLocation> GLOW = new HashMap<>();
    /** Animated glows: {@code <layer>_glow_f0.png, _f1, ...} (split from the art's frame strip), cycled every 2 ticks. */
    private static final Map<ResourceLocation, ResourceLocation[]> FRAMES = new HashMap<>();
    private static final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("none");
    private final A inner, outer;

    public ArmorGlowLayer(RenderLayerParent<T, M> parent, A inner, A outer) {
        super(parent);
        this.inner = inner;
        this.outer = outer;
    }

    /** Clears the texture-existence cache (resource reload). */
    public static void clearCache() { GLOW.clear(); FRAMES.clear(); }

    static ResourceLocation glowFor(ResourceLocation tex) {
        return GLOW.computeIfAbsent(tex, t -> {
            if (!t.getNamespace().equals(QuantumFalcon.MOD_ID)) return NONE;
            ResourceLocation g = t.withPath(t.getPath().replace(".png", "_glow.png"));
            return Minecraft.getInstance().getResourceManager().getResource(g).isPresent() ? g : NONE;
        });
    }

    static ResourceLocation[] framesFor(ResourceLocation glow) {
        return FRAMES.computeIfAbsent(glow, g -> {
            var rm = Minecraft.getInstance().getResourceManager();
            java.util.List<ResourceLocation> out = new java.util.ArrayList<>();
            for (int i = 0; i < 64; i++) {
                ResourceLocation f = g.withPath(g.getPath().replace(".png", "_f" + i + ".png"));
                if (rm.getResource(f).isEmpty()) break;
                out.add(f);
            }
            return out.toArray(ResourceLocation[]::new);
        });
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) continue;
            if (armor instanceof software.bernie.geckolib.animatable.GeoItem) continue;
            boolean useInner = slot == EquipmentSlot.LEGS;
            A model = useInner ? inner : outer;
            ArmorMaterial material = armor.getMaterial().value();
            boolean any = false;
            for (ArmorMaterial.Layer layer : material.layers()) if (glowFor(layer.texture(useInner)) != NONE) { any = true; break; }
            if (!any) continue;
            getParentModel().copyPropertiesTo(model);
            setVisibility(model, slot);
            float pulse = 0.78f + 0.22f * Mth.sin((ageInTicks + entity.getId() * 7) * 0.12f);
            int v = (int) (255 * pulse);
            int color = 0xFF000000 | v << 16 | v << 8 | v;
            for (ArmorMaterial.Layer layer : material.layers()) {
                ResourceLocation g = glowFor(layer.texture(useInner));
                if (g == NONE) continue;
                ResourceLocation[] frames = framesFor(g);
                if (frames.length > 0) {   // the art carries its own pulse: draw the current frame at full strength
                    g = frames[(int) (ageInTicks / 2) % frames.length];
                    model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(g)), 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                    continue;
                }
                model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(g)), 0xF000F0, OverlayTexture.NO_OVERLAY, color);
            }
        }
    }

    private static void setVisibility(HumanoidModel<?> m, EquipmentSlot slot) {
        m.setAllVisible(false);
        switch (slot) {
            case HEAD -> { m.head.visible = true; m.hat.visible = true; }
            case CHEST -> { m.body.visible = true; m.rightArm.visible = true; m.leftArm.visible = true; }
            case LEGS -> { m.body.visible = true; m.rightLeg.visible = true; m.leftLeg.visible = true; }
            case FEET -> { m.rightLeg.visible = true; m.leftLeg.visible = true; }
            default -> { }
        }
    }
}
