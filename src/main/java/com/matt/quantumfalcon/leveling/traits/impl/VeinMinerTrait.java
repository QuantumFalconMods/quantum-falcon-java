package com.matt.quantumfalcon.leveling.traits.impl;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.traits.Trait;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Vein Miner trait - breaks connected similar blocks.
 * Unlocks at level 75 for tools.
 */
public class VeinMinerTrait extends Trait {

    private final int maxBlocks;

    public VeinMinerTrait(int unlockLevel, int maxBlocks) {
        super(
            ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "vein_miner"),
            "Vein Miner",
            String.format("Break up to %d connected blocks", maxBlocks),
            unlockLevel,
            Set.of(ItemCategory.TOOL),
            1
        );
        this.maxBlocks = maxBlocks;
    }

    @Override
    public void onBlockBreak(Player player, ItemStack tool, BlockState state, ItemLevelData data) {
        // This will be called by the event handler with the block position
        // Actual vein mining logic is implemented in the event handler
    }

    /**
     * Find connected blocks of the same type
     */
    public List<BlockPos> findConnectedBlocks(Level level, BlockPos startPos, BlockState targetState) {
        List<BlockPos> result = new ArrayList<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        Set<BlockPos> checked = new HashSet<>();

        toCheck.add(startPos);
        checked.add(startPos);

        while (!toCheck.isEmpty() && result.size() < maxBlocks) {
            BlockPos current = toCheck.poll();
            BlockState currentState = level.getBlockState(current);

            if (currentState.is(targetState.getBlock())) {
                result.add(current);

                // Check all adjacent blocks
                for (BlockPos offset : BlockPos.betweenClosed(
                        current.offset(-1, -1, -1),
                        current.offset(1, 1, 1))) {
                    if (!checked.contains(offset)) {
                        checked.add(offset.immutable());
                        toCheck.add(offset.immutable());
                    }
                }
            }
        }

        return result;
    }

    public int getMaxBlocks() {
        return maxBlocks;
    }
}
