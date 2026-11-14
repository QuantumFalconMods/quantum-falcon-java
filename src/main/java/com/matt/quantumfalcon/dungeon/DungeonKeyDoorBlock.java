package com.matt.quantumfalcon.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

/** A vault door that ignores hands and redstone; a dungeon key opens it for good (the key is used up). */
public class DungeonKeyDoorBlock extends DoorBlock {
    public DungeonKeyDoorBlock(Properties props) {
        super(BlockSetType.IRON, props);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof DungeonKeyItem)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (state.getValue(OPEN)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            if (level instanceof ServerLevel sl && !fits(sl, pos, stack)) {
                player.displayClientMessage(Component.literal("This key was cut for another ruin.").withStyle(ChatFormatting.RED), true);
                level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 0.6f);
                return ItemInteractionResult.CONSUME;
            }
            unlock(level, pos, state);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.displayClientMessage(Component.literal("The key dissolves into the lock. The vault door opens.").withStyle(ChatFormatting.AQUA), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(OPEN) && !level.isClientSide) {
            // a player who earned this ruin's key and lost it is let through; the key is never the only copy
            if (level instanceof ServerLevel sl && DungeonSites.get(sl).earnedKey(DungeonSites.keyAt(sl, pos), player.getUUID())) {
                unlock(level, pos, state);
                player.displayClientMessage(Component.literal("The lock remembers your key. The vault door opens.").withStyle(ChatFormatting.AQUA), true);
                return InteractionResult.sidedSuccess(false);
            }
            player.displayClientMessage(Component.literal("Sealed. A dungeon key from this ruin's guard opens it.").withStyle(ChatFormatting.RED), true);
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 0.6f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * a site-cut key fits only its own ruin; an uncut key fits any ruin of its realm. Doors outside any dungeon
     * (placed by hand) take any key.
     */
    public static boolean fits(ServerLevel sl, BlockPos pos, ItemStack stack) {
        String door = DungeonSites.keyAt(sl, pos);
        if (door == null) return true;
        String site = DungeonKeyItem.siteOf(stack);
        if (site != null) return site.equals(door);
        return stack.getItem() instanceof DungeonKeyItem k && k.realm().equals(DungeonSites.realmOf(door));
    }

    /** Opens both halves (no-op if already open). */
    public void unlock(Level level, BlockPos pos, BlockState state) {
        setOpen(null, level, state, pos, true);
        level.playSound(null, pos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 1.0f, 0.8f);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 0.3, 0.8, 0.3, 0.02);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        // deliberately ignores redstone
    }
}
