package com.matt.quantumfalcon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Echo Stone (mid game): its ripples absorb impacts, halving fall damage for anything landing on it. */
public class EchoStoneBlock extends Block {
    public EchoStoneBlock(Properties properties) { super(properties); }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance * 0.5f);
    }
}
