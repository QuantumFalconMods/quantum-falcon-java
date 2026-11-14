package com.matt.quantumfalcon.client.renderer.villager;

import com.matt.quantumfalcon.client.renderer.PulsingGlowLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The vanilla-village Falconer (a vanilla profession) drawn with the v3 Falconer model: rank from the villager's level.
 * Installed by intercepting RenderLivingEvent.Pre for falconer villagers; every other villager renders as vanilla.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = com.matt.quantumfalcon.QuantumFalcon.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class FalconerVillagerRenderer extends GeoReplacedEntityRenderer<Villager, FalconerVillagerRenderer.Anim> {
    static FalconerVillagerRenderer INSTANCE;
    private static Villager current;

    public FalconerVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VillagerV3Model<>(a -> "falconer", a -> current == null ? 1 : current.getVillagerData().getLevel()), new Anim());
        this.shadowRadius = 0.5f;
        addRenderLayer(new PulsingGlowLayer<>(this));
    }

    @SubscribeEvent
    public static void onAddLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers e) { INSTANCE = new FalconerVillagerRenderer(e.getContext()); }

    @SubscribeEvent
    public static void onRender(RenderLivingEvent.Pre<?, ?> e) {
        if (INSTANCE == null || !(e.getEntity() instanceof Villager v)) return;
        if (v.getVillagerData().getProfession() != com.matt.quantumfalcon.villager.ModVillagers.FALCONER.get()) return;
        e.setCanceled(true);
        current = v;
        INSTANCE.render(v, v.getYRot(), e.getPartialTick(), e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight());
    }

    public static class Anim implements GeoReplacedEntity {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        @Override public net.minecraft.world.entity.EntityType<?> getReplacingEntityType() { return net.minecraft.world.entity.EntityType.VILLAGER; }
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
            RawAnimation idle = RawAnimation.begin().thenLoop("animation.rift_villager.idle");
            RawAnimation walk = RawAnimation.begin().thenLoop("animation.rift_villager.walk");
            c.add(new AnimationController<>(this, "move", 4, s -> s.setAndContinue(s.isMoving() ? walk : idle)));
        }
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    }
}
