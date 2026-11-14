/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.NonNullList
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.Container
 *  net.minecraft.world.ContainerHelper
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ChestMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package com.matt.quantumfalcon.block.entity;

import com.matt.quantumfalcon.block.EntanglementChestBlock;
import com.matt.quantumfalcon.registry.ModBlockEntities;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.Block;

public class EntanglementChestBlockEntity
extends BlockEntity
implements MenuProvider,
Container {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    @Nullable
    private UUID entanglementId = null;

    public EntanglementChestBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlockEntities.ENTANGLEMENT_CHEST.get(), pos, state);
    }

    public void setEntanglementId(UUID id) {
        this.entanglementId = id;
        if (this.level != null) {
            this.level.setBlock(this.worldPosition, (BlockState)this.getBlockState().setValue((Property)EntanglementChestBlock.LINKED, (Comparable)Boolean.valueOf(true)), 3);
        }
        this.setChanged();
    }

    @Nullable
    public UUID getEntanglementId() {
        return this.entanglementId;
    }

    public void onRemoved() {
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems((CompoundTag)tag, this.items, (HolderLookup.Provider)registries);
        if (this.entanglementId != null) {
            tag.putUUID("EntanglementId", this.entanglementId);
        }
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(27, ItemStack.EMPTY);
        ContainerHelper.loadAllItems((CompoundTag)tag, this.items, (HolderLookup.Provider)registries);
        if (tag.hasUUID("EntanglementId")) {
            this.entanglementId = tag.getUUID("EntanglementId");
        }
    }

    public Component getDisplayName() {
        return Component.translatable((String)"block.quantumfalcon.entanglement_chest");
    }

    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return ChestMenu.threeRows((int)containerId, (Inventory)playerInventory, (Container)this);
    }

    public int getContainerSize() {
        return 27;
    }

    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    public ItemStack getItem(int slot) {
        return (ItemStack)this.items.get(slot);
    }

    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, (int)slot, (int)amount);
        if (!result.isEmpty()) {
            this.setChanged();
            this.syncToLinkedChest();
        }
        return result;
    }

    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, (int)slot);
    }

    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
        this.syncToLinkedChest();
    }

    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this && player.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    public void clearContent() {
        this.items.clear();
        this.syncToLinkedChest();
    }

    private void syncToLinkedChest() {
    }
}
