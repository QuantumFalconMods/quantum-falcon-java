package com.matt.quantumfalcon.menu;

import com.matt.quantumfalcon.blockentity.QuantumIncubatorBlockEntity;
import com.matt.quantumfalcon.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Incubator menu: egg slot (56,35), fuel slot (56,57), player inventory at the vanilla positions. Data: progress, fuel, status, tier, seconds left. */
public class QuantumIncubatorMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public QuantumIncubatorMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) { this(id, inv, new SimpleContainer(2), new SimpleContainerData(5)); }

    public QuantumIncubatorMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenuTypes.INCUBATOR.get(), id);
        checkContainerSize(container, 2);
        this.container = container; this.data = data;
        addSlot(new Slot(container, 0, 56, 17) {
            @Override public boolean mayPlace(ItemStack s) { return QuantumIncubatorBlockEntity.isEgg(s); }
            @Override public int getMaxStackSize() { return 1; }
            @Override public void setByPlayer(ItemStack s, ItemStack old) {   // the player who puts the egg in owns the hatchling
                if (container instanceof QuantumIncubatorBlockEntity be && !s.isEmpty()) be.loadingBy(inv.player);
                super.setByPlayer(s, old);
            }
        });
        addSlot(new Slot(container, 1, 56, 53) { @Override public boolean mayPlace(ItemStack s) { return QuantumIncubatorBlockEntity.isFuel(s); } });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, 9 + r * 9 + c, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public float progress() { return data.get(0) / 1000f; }
    public float fuel() { return data.get(1) / 1000f; }
    public int status() { return data.get(2); }
    public int tier() { return data.get(3); }
    public int secondsLeft() { return data.get(4); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem(), copy = s.copy();
        if (index < 2) { if (!moveItemStackTo(s, 2, 38, true)) return ItemStack.EMPTY; }
        else if (QuantumIncubatorBlockEntity.isEgg(s)) { if (!moveItemStackTo(s, 0, 1, false)) return ItemStack.EMPTY; }
        else if (QuantumIncubatorBlockEntity.isFuel(s)) { if (!moveItemStackTo(s, 1, 2, false)) return ItemStack.EMPTY; }
        else if (index < 29) { if (!moveItemStackTo(s, 29, 38, false)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(s, 2, 29, false)) return ItemStack.EMPTY;
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
