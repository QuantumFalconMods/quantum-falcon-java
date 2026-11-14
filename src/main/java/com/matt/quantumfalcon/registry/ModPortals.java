package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.dimension.EchoDimensionRegistry;
import com.matt.quantumfalcon.dimension.InstabilityManager;
import com.matt.quantumfalcon.story.StorylineManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.kyrptonaught.customportalapi.CustomPortalRegistrationEvent;
import net.kyrptonaught.customportalapi.api.CustomPortalBuilder;

/**
 * Registers the Quantum Falcon Rift portal with CustomPortalAPI Reforged.
 * Uses RiftFrameBlock as frame, RiftIgniter as igniter, RiftPortalBlock as portal block.
 * Destination: quantumfalcon:echo_rift (Echo Rift dimension).
 * Hooks: InstabilityManager, StorylineManager, and quantum particles on teleport.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class ModPortals {

    public static final ResourceLocation ECHO_RIFT_DIMENSION = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "echo_rift");

    @SubscribeEvent
    public static void onPortalRegistration(CustomPortalRegistrationEvent event) {
        CustomPortalBuilder builder = CustomPortalBuilder.beginPortal()
            .frameBlock(ModBlocks.RIFT_FRAME.get())
            .destDimID(ECHO_RIFT_DIMENSION)
            .returnDim(ResourceLocation.withDefaultNamespace("overworld"), false)
            .lightWithItem(ModBlocks.RIFT_IGNITER.get().asItem())
            .forcedSize(4, 5)
            .customPortalBlock(() -> (net.kyrptonaught.customportalapi.CustomPortalBlock) ModBlocks.RIFT_PORTAL.get())
            .tintColor(0x88, 0x44, 0xCC) // cyberpunk purple
            .registerPreIgniteEvent(ModPortals::onPreIgnite)
            .registerIgniteEvent(ModPortals::onIgnite)
            .registerPostTPEvent(ModPortals::onPostTeleport);

        event.register(builder);
        QuantumFalcon.LOGGER.info("⚡ Quantum Rift portal registered with CustomPortalAPI (frame=rift_frame, igniter=rift_igniter, dest=echo_rift)");
    }

    /** Optional: gate portal lighting behind story progress (e.g. after main_2). Return true to allow. */
    private static boolean onPreIgnite(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level world,
            net.minecraft.core.BlockPos portalPos, net.minecraft.core.BlockPos framePos,
            net.kyrptonaught.customportalapi.portal.PortalIgnitionSource source) {
        // Allow lighting in overworld; optionally check StorylineManager progress
        return true;
    }

    private static void onIgnite(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level world,
            net.minecraft.core.BlockPos portalPos, net.minecraft.core.BlockPos framePos,
            net.kyrptonaught.customportalapi.portal.PortalIgnitionSource source) {
        QuantumFalcon.LOGGER.info("Quantum Rift portal lit at {} by {}", portalPos, player == null ? "<no player>" : player.getName().getString());
    }

    /** After entity teleports: instability, story, and quantum particle burst. */
    private static void onPostTeleport(Entity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        if (entity instanceof ServerPlayer player) {
            boolean enteringEcho = EchoDimensionRegistry.isInEchoDimension(serverLevel);

            if (enteringEcho) {
                int instability = EchoDimensionRegistry.getInstabilityLevel(serverLevel.dimension());
                InstabilityManager.onPlayerEnterEcho(player, instability);
                // Chapter 2 is about your falcon turning east, so it waits until you have one (StoryEvents fires it later)
                if (com.matt.quantumfalcon.story.PlayerStoryData.get(player).hasSeenChapter(StorylineManager.StoryChapter.CHAPTER_1))
                    StorylineManager.triggerStoryEvent(player, StorylineManager.StoryChapter.CHAPTER_2);
                spawnQuantumBurst(serverLevel, player.getX(), player.getY(), player.getZ());
            } else {
                InstabilityManager.onPlayerExitEcho(player);
            }
        }
    }

    private static void spawnQuantumBurst(ServerLevel level, double x, double y, double z) {
        if (level.getServer() == null) return;
        ParticleOptions quantumSpark = ModParticles.QUANTUM_SPARK.get();
        for (int i = 0; i < 40; i++) {
            level.sendParticles(quantumSpark, x, y + 1, z, 1,
                (level.random.nextDouble() - 0.5) * 0.5,
                level.random.nextDouble() * 0.5,
                (level.random.nextDouble() - 0.5) * 0.5, 0.1);
        }
        for (int i = 0; i < 20; i++) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                x, y + 1, z, 1,
                (level.random.nextDouble() - 0.5) * 0.3,
                level.random.nextDouble() * 0.3,
                (level.random.nextDouble() - 0.5) * 0.3, 0.05);
        }
    }
}
