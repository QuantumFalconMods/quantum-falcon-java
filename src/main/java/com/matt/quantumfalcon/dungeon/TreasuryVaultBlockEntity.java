package com.matt.quantumfalcon.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class TreasuryVaultBlockEntity extends BlockEntity {
    private String lootTable = "quantumfalcon:chests/dungeon/treasury_t1";
    private boolean looted;

    public TreasuryVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModDungeon.TREASURY_VAULT_BE.get(), pos, state);
    }

    public boolean looted() { return looted; }
    /** Test hook: a fresh, unlooted vault. */
    public void resetForTest() { looted = false; setChanged(); }

    /** Drops the loot table's items on top of the vault. False if already looted. */
    public boolean payOut(ServerLevel level, Player player) {
        if (looted) return false;
        looted = true;
        setChanged();
        ResourceLocation id = ResourceLocation.tryParse(lootTable);
        if (id == null) return true;
        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
        LootParams params = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, worldPosition.getCenter())
            .withParameter(LootContextParams.THIS_ENTITY, player)
            .withLuck(player.getLuck())
            .create(LootContextParamSets.CHEST);
        for (ItemStack stack : table.getRandomItems(params)) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, stack);
        }
        level.playSound(null, worldPosition, SoundEvents.VAULT_EJECT_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putString("LootTable", lootTable);
        tag.putBoolean("Looted", looted);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (tag.contains("LootTable")) lootTable = tag.getString("LootTable");
        looted = tag.getBoolean("Looted");
    }
}
