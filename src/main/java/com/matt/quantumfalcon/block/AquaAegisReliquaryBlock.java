package com.matt.quantumfalcon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Aqua Aegis Reliquary — single-use healing structure.
 * Right-click to restore full HP and clear negative effects; block is destroyed.
 */
public class AquaAegisReliquaryBlock extends Block {

    public static final MapCodec<AquaAegisReliquaryBlock> CODEC = simpleCodec(AquaAegisReliquaryBlock::new);

    public AquaAegisReliquaryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20f);
        for (MobEffectInstance effect : java.util.List.copyOf(player.getActiveEffects())) {
            if (effect.getEffect().value().isBeneficial() == false) {
                player.removeEffect(effect.getEffect());
            }
        }

        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {   // server-side addParticle never reached clients
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 16, 0.4, 0.4, 0.4, 0.05);
            sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.03);
        }

        level.destroyBlock(pos, false);

        return InteractionResult.SUCCESS;
    }
}
