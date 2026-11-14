package com.matt.quantumfalcon.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Telegraphed trap trigger: a mob-sensitive pressure plate whose rune glows while armed. When pressed it fires every
 * dispenser within {@link #REACH} blocks that is aimed along the plate's row, so the whole row is dangerous, not only
 * the plates touching a dispenser.
 */
public class TrapPlateBlock extends PressurePlateBlock {
    public static final int REACH = 6;

    public TrapPlateBlock(Properties props) {
        super(BlockSetType.STONE, props);
    }

    @Override
    protected void updateNeighbours(Level level, BlockPos pos) {
        super.updateNeighbours(level, pos);
        BlockState now = level.getBlockState(pos);
        if (level instanceof ServerLevel sl && now.is(this) && now.getValue(POWERED)) fire(sl, pos);
    }

    /** Fires each aimed dispenser once. Returns how many fired. */
    public static int fire(ServerLevel level, BlockPos plate) {
        int fired = 0;
        for (BlockPos d : BlockPos.betweenClosed(plate.offset(-REACH, -1, -REACH), plate.offset(REACH, 1, REACH))) {
            BlockState ds = level.getBlockState(d);
            if (!(ds.getBlock() instanceof DispenserBlock) || !(level.getBlockEntity(d) instanceof DispenserBlockEntity be)) continue;
            Direction f = ds.getValue(DispenserBlock.FACING);
            boolean aimed = f.getAxis() == Direction.Axis.X ? d.getZ() == plate.getZ() && Integer.signum(plate.getX() - d.getX()) == f.getStepX()
                : f.getAxis() == Direction.Axis.Z && d.getX() == plate.getX() && Integer.signum(plate.getZ() - d.getZ()) == f.getStepZ();
            if (!aimed) continue;
            be.unpackLootTable(null);
            int slot = be.getRandomSlot(level.random);
            if (slot < 0) continue;
            ItemStack stack = be.getItem(slot);
            DispenseItemBehavior behavior = DispenserBlock.DISPENSER_REGISTRY.get(stack.getItem());
            if (behavior == null) continue;
            be.setItem(slot, behavior.dispense(new BlockSource(level, d.immutable(), ds, be), stack));
            fired++;
        }
        return fired;
    }
}
