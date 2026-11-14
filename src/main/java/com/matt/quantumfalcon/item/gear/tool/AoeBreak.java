package com.matt.quantumfalcon.item.gear.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * every extra block an area tool breaks goes through the same checks as a normal break. The BreakEvent fires
 * (so the dungeon ward, claim mods and protection plugins apply), unbreakable blocks are skipped, and the tool loses
 * 1 durability per block; the sweep stops when the tool breaks.
 */
public final class AoeBreak {
    private AoeBreak() {}

    /** Break pos as player with the main-hand tool. False when refused, unbreakable, or the tool is gone. */
    public static boolean breakAs(Level level, BlockPos pos, Player player) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return false;
        ItemStack tool = sp.getMainHandItem();
        if (tool.isEmpty()) return false;
        BlockState st = level.getBlockState(pos);
        if (st.isAir() || st.getDestroySpeed(level, pos) < 0) return false;
        if (CommonHooks.fireBlockBreak(level, sp.gameMode.getGameModeForPlayer(), sp, pos, st).isCanceled()) return false;
        boolean broke = level.destroyBlock(pos, !sp.isCreative(), sp);
        if (broke && !sp.isCreative() && tool.isDamageableItem()) tool.hurtAndBreak(1, sp, EquipmentSlot.MAINHAND);
        return broke;
    }
}
