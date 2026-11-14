package com.matt.quantumfalcon.blockentity;

import com.matt.quantumfalcon.breeding.BreedingData;
import com.matt.quantumfalcon.breeding.BreedingService;
import com.matt.quantumfalcon.breeding.FalconGenome;
import com.matt.quantumfalcon.item.FalconEggItem;
import com.matt.quantumfalcon.menu.QuantumIncubatorMenu;
import com.matt.quantumfalcon.registry.ModBlocks;
import com.matt.quantumfalcon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Quantum Incubator (BREEDING.md "Eggs and incubation"). Two slots: an egg and fuel (Quantum Shards or Rift Embers).
 * It always counts as warmth and heat, so Common to Epic eggs hatch with no fuel. Mythic and Legendary eggs need rift
 * energy, which burning fuel provides (one item = 5 minutes); Quantum eggs need fuel plus Echo Rift layer 3+ or a
 * Quantum Forge within 4 blocks. Hatch time is the tier's real minutes (BreedingData.hatchMinutes: 5 to 30).
 * An egg resting on top of the incubator (as a block) still works as before: a fuelled incubator gives it rift energy.
 */
public class QuantumIncubatorBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    public static final int SLOT_EGG = 0, SLOT_FUEL = 1, FUEL_TICKS = 6000;
    public static final int ST_IDLE = 0, ST_WORKING = 1, ST_NEED_FUEL = 2, ST_NEED_QUANTUM = 3;
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int progressTicks, totalTicks, burnTicks, status;
    @Nullable private UUID lastUser;
    /** who put the current egg in (through the GUI). A hopper-fed egg has none and hatches untamed. */
    @Nullable private UUID eggOwner;
    @Nullable private UUID pendingLoader;
    /** Called by the menu's egg slot just before a player puts an egg in. */
    public void loadingBy(Player p) { pendingLoader = p.getUUID(); }
    @Nullable public UUID eggOwner() { return eggOwner; }

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> totalTicks <= 0 ? 0 : (int) (1000L * progressTicks / totalTicks);
                case 1 -> burnTicks * 1000 / FUEL_TICKS;
                case 2 -> status;
                case 3 -> eggTier();
                case 4 -> Math.max(0, (totalTicks - progressTicks) / 20);
                default -> 0;
            };
        }
        @Override public void set(int i, int v) { }
        @Override public int getCount() { return 5; }
    };

    public QuantumIncubatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    /** A fuelled incubator provides rift energy to an egg block resting on top of it. */
    public boolean hasFuel() { return burnTicks > 0 || isFuel(items.get(SLOT_FUEL)); }
    public void consumeFuel() { if (burnTicks <= 0 && isFuel(items.get(SLOT_FUEL))) { items.get(SLOT_FUEL).shrink(1); burnTicks = FUEL_TICKS; setChanged(); } }

    public static boolean isFuel(ItemStack s) {
        if (s.isEmpty()) return false;
        if (s.is(ModItems.QUANTUM_SHARD.get())) return true;
        for (var c : ModItems.CATALYSTS) if (c.getId().getPath().equals("rift_ember") && s.is(c.get())) return true;
        return false;
    }
    public static boolean isEgg(ItemStack s) { return s.is(ModItems.FALCON_EGG.get()); }

    private int eggTier() { FalconGenome g = FalconEggItem.child(items.get(SLOT_EGG)); return g == null ? 0 : g.tier; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, QuantumIncubatorBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        ItemStack egg = be.items.get(SLOT_EGG);
        if (!isEgg(egg)) { if (be.progressTicks != 0 || be.status != ST_IDLE) { be.progressTicks = 0; be.totalTicks = 0; be.status = ST_IDLE; be.setChanged(); } return; }
        FalconGenome g = FalconEggItem.child(egg);
        int tier = g == null ? 1 : g.tier;
        be.totalTicks = BreedingData.TIERS.get(Math.max(1, Math.min(7, tier)) - 1).hatchMinutes() * 1200;
        String need = BreedingData.TIERS.get(Math.max(1, Math.min(7, tier)) - 1).incubation();
        boolean needsFuel = need.startsWith("rift_energy") || need.equals("quantum");
        boolean quantumOk = !need.equals("quantum") || BreedingService.riftLayer(level, pos) >= 3
                || BlockPos.betweenClosedStream(pos.offset(-4, -2, -4), pos.offset(4, 2, 4)).anyMatch(q -> level.getBlockState(q).is(ModBlocks.QUANTUM_FORGE.get()));
        if (needsFuel && be.burnTicks <= 0) be.consumeFuel();
        if (!quantumOk) be.status = ST_NEED_QUANTUM;
        else if (needsFuel && be.burnTicks <= 0) be.status = ST_NEED_FUEL;
        else {
            be.status = ST_WORKING;
            if (needsFuel) be.burnTicks--;
            be.progressTicks++;
            if (be.progressTicks % 50 == 1)   // incubator_hum is a 2.5 s loopable bed: retrigger it every 50 ticks while working
                sl.playSound(null, pos, com.matt.quantumfalcon.entity.util.QFMobSounds.get("incubator_hum"), net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.0f);
            if (be.progressTicks % 40 == 0)
                sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0.01);
            if (be.progressTicks >= be.totalTicks) {
                sl.playSound(null, pos, com.matt.quantumfalcon.entity.util.QFMobSounds.get("incubator_complete"), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                CompoundTag eggData = FalconEggItem.eggData(egg);
                be.lastHatched = FalconEggBlockEntity.hatchDetached(sl, pos.above(), eggData == null ? new CompoundTag() : eggData, be.eggOwner);   // the loader, never the last GUI viewer
                egg.shrink(1);
                be.progressTicks = 0;
            }
        }
        be.setChanged();
    }

    /** The falcon hatched most recently (not saved; used by tests and future hatch toasts). */
    @javax.annotation.Nullable public com.matt.quantumfalcon.entity.QuantumFalconEntity lastHatched;

    /** Test hook: jump progress to the given fraction. */
    public void setProgressForTest(float f) { if (totalTicks <= 0) totalTicks = 1; progressTicks = (int) (totalTicks * f); }
    public int status() { return status; }
    public int totalTicks() { return totalTicks; }

    // ── NBT ──────────────────────────────────────────────────────────────────────────────────────────────────
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider p) {
        super.saveAdditional(tag, p);
        ContainerHelper.saveAllItems(tag, items, p);
        tag.putInt("Progress", progressTicks); tag.putInt("Burn", burnTicks); tag.putInt("DataVersion", 2);
        if (lastUser != null) tag.putUUID("LastUser", lastUser);
        if (eggOwner != null) tag.putUUID("EggOwner", eggOwner);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider p) {
        super.loadAdditional(tag, p);
        items.set(0, ItemStack.EMPTY); items.set(1, ItemStack.EMPTY);
        if (tag.getInt("DataVersion") >= 2) ContainerHelper.loadAllItems(tag, items, p);
        else {   // v1 saves (EggSlot / FuelSlot): keep the player's items
            items.set(SLOT_EGG, ItemStack.parseOptional(p, tag.getCompound("EggSlot")));
            items.set(SLOT_FUEL, ItemStack.parseOptional(p, tag.getCompound("FuelSlot")));
        }
        progressTicks = tag.getInt("Progress"); burnTicks = tag.getInt("Burn");
        lastUser = tag.hasUUID("LastUser") ? tag.getUUID("LastUser") : null;
        eggOwner = tag.hasUUID("EggOwner") ? tag.getUUID("EggOwner") : null;
    }

    // ── menu ─────────────────────────────────────────────────────────────────────────────────────────────────
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        lastUser = player.getUUID();
        return new QuantumIncubatorMenu(id, inv, this, data);
    }
    @Override public Component getDisplayName() { return Component.translatable("block.quantumfalcon.quantum_incubator"); }

    // ── container ────────────────────────────────────────────────────────────────────────────────────────────
    @Override public int getContainerSize() { return 2; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int n) { ItemStack r = ContainerHelper.removeItem(items, slot, n); if (!r.isEmpty()) setChanged(); return r; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot == SLOT_EGG && !ItemStack.isSameItemSameComponents(stack, items.get(SLOT_EGG))) {
            progressTicks = 0;
            eggOwner = stack.isEmpty() ? null : pendingLoader;   // a hopper (no pending loader) leaves it unowned
        }
        if (slot == SLOT_EGG) pendingLoader = null;
        items.set(slot, stack); setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack s) { return slot == SLOT_EGG ? isEgg(s) : isFuel(s); }
    @Override public int getMaxStackSize() { return 64; }
    @Override public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
    }
    @Override public void clearContent() { items.clear(); setChanged(); }
    // hoppers: eggs from the top, fuel from the sides; nothing is pulled out automatically
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.UP ? new int[]{SLOT_EGG} : side == Direction.DOWN ? new int[0] : new int[]{SLOT_FUEL}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack s, @Nullable Direction d) { return canPlaceItem(slot, s); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack s, Direction d) { return false; }
    public NonNullList<ItemStack> items() { return items; }
}
