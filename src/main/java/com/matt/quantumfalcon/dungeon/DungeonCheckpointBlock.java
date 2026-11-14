package com.matt.quantumfalcon.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Antechamber anchor: right-click to set your respawn point here before the warden fight. */
public class DungeonCheckpointBlock extends Block {
    public DungeonCheckpointBlock(Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            sp.setRespawnPosition(sl.dimension(), pos.above(), sp.getYRot(), true, false);
            sp.displayClientMessage(Component.literal("Checkpoint bound. You will return here if you fall.").withStyle(ChatFormatting.AQUA), true);
            sl.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0f, 1.2f);
            sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.3, 0.4, 0.3, 0.02);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
