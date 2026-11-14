/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.matt.quantumfalcon.block;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BoulderTrapBlock
extends Block {
    public BoulderTrapBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!level.isClientSide() && entity instanceof Player) {
            Player player = (Player)entity;
            // no chat spam (it re-sent every tick a player stood on it, and the trap itself does nothing yet); one click
            // when a player first steps on
            if (player.getPersistentData().getLong("qf_boulder_click") + 20 < level.getGameTime()) {
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0f, 0.5f);
            }
            player.getPersistentData().putLong("qf_boulder_click", level.getGameTime());
        }
    }

    private void triggerBoulder(Level level, BlockPos triggerPos) {
        BlockPos spawnPos = triggerPos.above(10);
        Direction rollDir = Direction.NORTH;
    }
}
