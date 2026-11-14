package com.matt.quantumfalcon.dungeon;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LevelEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * {@code BlockEvent.EntityPlaceEvent} does not fire for dispensers, so a dispenser aimed into a dungeon could pour
 * water or lava, place shulker boxes or powder snow, light fires or wax blocks inside it. Every registered dispense
 * behaviour for a block-changing item (buckets, block items, flint and steel, bone meal, shears, honeycomb) is wrapped:
 * aimed at a dungeon block, the dispenser just clicks and keeps the item. Arrows and other projectiles still fire, so
 * dungeon traps work. Installed when the server starts, after every mod has registered its behaviours.
 */
public final class DungeonDispenseGuard implements DispenseItemBehavior {
    private static final Set<Item> TOOLS = Set.of(Items.FLINT_AND_STEEL, Items.BONE_MEAL, Items.SHEARS, Items.HONEYCOMB, Items.GLASS_BOTTLE);
    private final DispenseItemBehavior inner;

    private DungeonDispenseGuard(DispenseItemBehavior inner) { this.inner = inner; }

    public static boolean changesBlocks(Item item) {
        return item instanceof BucketItem || item instanceof SolidBucketItem || item instanceof BlockItem || TOOLS.contains(item);
    }

    /** Wraps every block-changing behaviour once. Returns how many were wrapped this call. */
    public static synchronized int install() {
        int n = 0;
        Map<Item, DispenseItemBehavior> reg = DispenserBlock.DISPENSER_REGISTRY;
        for (var e : new ArrayList<>(reg.entrySet())) {
            if (e.getValue() instanceof DungeonDispenseGuard || !changesBlocks(e.getKey())) continue;
            reg.put(e.getKey(), new DungeonDispenseGuard(e.getValue()));
            n++;
        }
        if (n > 0) QuantumFalcon.LOGGER.info("Dungeon ward: guarded {} dispenser behaviours", n);
        return n;
    }

    @Override
    public ItemStack dispense(BlockSource src, ItemStack stack) {
        BlockPos target = src.pos().relative(src.state().getValue(DispenserBlock.FACING));
        if (DungeonWard.inDungeon(src.level(), target)) {
            src.level().levelEvent(LevelEvent.SOUND_DISPENSER_FAIL, src.pos(), 0);
            return stack;
        }
        return inner.dispense(src, stack);
    }
}
