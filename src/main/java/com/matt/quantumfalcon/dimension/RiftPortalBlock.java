package com.matt.quantumfalcon.dimension;

import com.matt.quantumfalcon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.kyrptonaught.customportalapi.CustomPortalBlock;
import org.jetbrains.annotations.NotNull;

/**
 * Quantum Rift Portal Block - extends CustomPortalAPI's CustomPortalBlock for stable
 * frame detection, ignition (Rift Igniter), and dimension teleport. Keeps cyberpunk
 * particles and our blockstate (rift_portal.json). Teleport hooks (InstabilityManager,
 * StorylineManager, particles) are in ModPortals.
 */
public class RiftPortalBlock extends CustomPortalBlock {

    public RiftPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        // Quantum / cyberpunk particles
        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double dx = (random.nextDouble() - 0.5) * 0.5;
            double dy = (random.nextDouble() - 0.5) * 0.5;
            double dz = (random.nextDouble() - 0.5) * 0.5;
            level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, dx, dy, dz);
        }
        if (random.nextInt(20) == 0) {
            level.addParticle(ParticleTypes.DRAGON_BREATH,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                0, 0.05, 0);
        }
    }
}
