package com.matt.quantumfalcon.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LoreLecternBlockEntity extends BlockEntity {
    private String lore = "riftslate_0";

    public LoreLecternBlockEntity(BlockPos pos, BlockState state) {
        super(ModDungeon.LORE_LECTERN_BE.get(), pos, state);
    }

    public String lore() { return lore; }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) { super.saveAdditional(tag, regs); tag.putString("Lore", lore); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) { super.loadAdditional(tag, regs); if (tag.contains("Lore")) lore = tag.getString("Lore"); }
}
