package com.matt.quantumfalcon.client;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.client.renderer.*;
import com.matt.quantumfalcon.registry.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void itemColours(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> com.matt.quantumfalcon.item.FalconEggItem.tint(stack, layer), com.matt.quantumfalcon.registry.ModItems.FALCON_EGG.get());
    }


    @SubscribeEvent
    public static void menuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(com.matt.quantumfalcon.registry.ModMenuTypes.INCUBATOR.get(), com.matt.quantumfalcon.client.gui.QuantumIncubatorScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.matt.quantumfalcon.registry.ModBlockEntities.FALCON_EGG.get(), ctx -> new FalconEggRenderer());
        event.registerBlockEntityRenderer(com.matt.quantumfalcon.registry.ModBlockEntities.FALCON_NEST.get(), ctx -> new FalconNestRenderer());
        // ── Main companion ──
        event.registerEntityRenderer(ModEntities.QUANTUM_FALCON.get(), QuantumFalconRenderer::new);

        // ── Ambient (GeckoLib) ──
        event.registerEntityRenderer(ModEntities.QUANTUM_WISP.get(), WispRenderer::new);

        // ── Hostile mobs (humanoid placeholder) ──
        event.registerEntityRenderer(ModEntities.QUANTUM_PHANTOM.get(),
            ctx -> new com.matt.quantumfalcon.client.renderer.QFGeoMobRenderer<>(ctx, "mob/quantum_phantom", 0.6f));
        event.registerEntityRenderer(ModEntities.CRYSTAL_GUARDIAN.get(),
            ctx -> new com.matt.quantumfalcon.client.renderer.QFGeoMobRenderer<>(ctx, "mob/crystal_guardian", 0.9f));
        event.registerEntityRenderer(ModEntities.SKYWARDEN_IREL.get(), com.matt.quantumfalcon.client.renderer.SkywardenIrelRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_ECHO.get(),
            ctx -> new com.matt.quantumfalcon.client.renderer.QFGeoMobRenderer<>(ctx, "mob/void_echo", 0.4f));
        event.registerEntityRenderer(ModEntities.DATA_CONSTRUCT.get(),
            ctx -> new com.matt.quantumfalcon.client.renderer.QFGeoMobRenderer<com.matt.quantumfalcon.entity.DataConstructEntity>(ctx, "mob/data_construct", 0.5f,
                e -> 0.4 + 0.3 * e.getConstructSize()));

        event.registerEntityRenderer(ModEntities.QUANTUM_STALKER.get(),
            ctx -> new QFGeoMobRenderer<com.matt.quantumfalcon.entity.QuantumStalkerEntity>(ctx, "mob/quantum_stalker", 0.5f));
        event.registerEntityRenderer(ModEntities.GOBLIN_TRADER.get(),
            ctx -> new QFGeoMobRenderer<com.matt.quantumfalcon.entity.npc.GoblinTraderEntity>(ctx, "npc/goblin_trader", 0.4f));
        event.registerEntityRenderer(ModEntities.RIFT_VILLAGER.get(), com.matt.quantumfalcon.client.renderer.villager.RiftVillagerRenderer::new);
        event.registerEntityRenderer(ModEntities.QUANTUM_MERCHANT.get(),
            ctx -> new QFGeoMobRenderer<com.matt.quantumfalcon.entity.npc.QuantumMerchantEntity>(ctx, "npc/quantum_merchant", 0.5f));
        event.registerEntityRenderer(ModEntities.ECLIPSE_PHANTOM.get(),
            ctx -> new QFGeoMobRenderer<com.matt.quantumfalcon.entity.boss.EclipsePhantomEntity>(ctx, "boss/eclipse_phantom", 1.2f));
        event.registerEntityRenderer(ModEntities.VOID_LORD.get(),
            ctx -> new QFGeoMobRenderer<com.matt.quantumfalcon.entity.boss.VoidLordEntity>(ctx, "boss/void_lord", 1.0f));

        // ── Bosses (GeckoLib) ──
        // v3 rebuilds (tools/gen_overseer.py, gen_warden.py; ART_STYLE.md)
        event.registerEntityRenderer(ModEntities.QUANTUM_OVERSEER.get(),
            ctx -> new com.matt.quantumfalcon.client.renderer.QFBossGeoRenderer<com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity>(ctx, "quantum_overseer", e -> "boss/quantum_overseer", 1.3f, 1.6f));
        event.registerEntityRenderer(ModEntities.RIFT_WARDEN.get(),
            ctx -> new com.matt.quantumfalcon.client.renderer.QFBossGeoRenderer<com.matt.quantumfalcon.entity.boss.RiftWardenEntity>(ctx, "rift_warden", e -> e.getPhase() >= 3 ? "boss/rift_warden_anger" : "boss/rift_warden", 1.15f, 1.8f));
        event.registerEntityRenderer(ModEntities.RIFT_WARDEN_MINION.get(),
            ctx -> new software.bernie.geckolib.renderer.GeoEntityRenderer<>(ctx,
                new software.bernie.geckolib.model.DefaultedEntityGeoModel<com.matt.quantumfalcon.entity.boss.RiftWardenMinionEntity>(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.matt.quantumfalcon.QuantumFalcon.MOD_ID, "boss/rift_warden_minion"))));
        event.registerEntityRenderer(ModEntities.SHA_SHA.get(),
            ctx -> new QFGeoMobRenderer<com.matt.quantumfalcon.entity.boss.ShaShaEntity>(ctx, "boss/sha_sha", 0.7f, e -> 1.25));
        event.registerEntityRenderer(ModEntities.ENTANGLED_TWINS.get(),
            com.matt.quantumfalcon.client.renderer.EntangledTwinsRenderer::new);
        event.registerEntityRenderer(ModEntities.SINGULARITY_SOVEREIGN.get(),
            SingularitySovereignRenderer::new);

        // ── Projectiles (no visible model — effects via particles) ──
        event.registerEntityRenderer(ModEntities.VOID_BLAST.get(),    ctx -> new NoopRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(),    ctx -> new NoopRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.REALITY_RIFT.get(),  ctx -> new NoopRenderer<>(ctx));
        // the Overseer's shard volley: was missing, and a renderer-less entity crashes the client on its first frame
        event.registerEntityRenderer(ModEntities.QUANTUM_SHARD.get(), com.matt.quantumfalcon.client.renderer.QuantumShardRenderer::new);
    }
}

