package com.matt.quantumfalcon.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Dormant sigil that wakes an elite (key bearer or warden) when a player comes close. */
public class DungeonGuardBlock extends Block implements EntityBlock {
    public DungeonGuardBlock(Properties props) { super(props); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DungeonGuardBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (l, p, s, be) -> { if (be instanceof DungeonGuardBlockEntity g) g.serverTick(); };
    }
}
