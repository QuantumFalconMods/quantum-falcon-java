package com.matt.quantumfalcon.arena.fight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Remembers the floor block a rift tile replaced and how long the hole stays open. */
public class RiftTileBlockEntity extends BlockEntity {
    public BlockState original = Blocks.STONE.defaultBlockState();
    public int openTicks = 100;

    public RiftTileBlockEntity(BlockPos pos, BlockState state) { super(ModArenaBlocks.RIFT_TILE_BE.get(), pos, state); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.put("Original", NbtUtils.writeBlockState(original));
        tag.putInt("OpenTicks", openTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        if (tag.contains("Original")) original = NbtUtils.readBlockState(reg.lookupOrThrow(Registries.BLOCK), tag.getCompound("Original"));
        openTicks = tag.getInt("OpenTicks");
    }
}
