package com.matt.quantumfalcon.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** End-of-dungeon vault: sealed until the dungeon's warden (or the arena boss) dies, then pays out its loot table once. */
public class TreasuryVaultBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TreasuryVaultBlock> CODEC = simpleCodec(TreasuryVaultBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public TreasuryVaultBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(OPEN, false));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, OPEN); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TreasuryVaultBlockEntity(pos, state); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!state.getValue(OPEN) && level instanceof ServerLevel sv
                && DungeonSites.get(sv).isWardenDead(DungeonSites.keyAt(sv, pos))) {
            // this ruin's warden is already dead (the vault was unloaded or rebuilt when it fell)
            DungeonSites.openVault(sv, pos, state);
            state = level.getBlockState(pos);
        }
        if (!state.getValue(OPEN)) {
            player.displayClientMessage(Component.literal("The treasury is sealed. Defeat this place's warden.").withStyle(ChatFormatting.RED), true);
            level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof TreasuryVaultBlockEntity be && level instanceof ServerLevel sl) {
            if (be.payOut(sl, player)) {
                player.displayClientMessage(Component.literal("The treasury yields its hoard.").withStyle(ChatFormatting.GOLD), true);
            } else {
                player.displayClientMessage(Component.literal("Empty. Someone got here first.").withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** Opens every sealed vault within {@code radius} (cubic scan, ±radius/2 vertically). Returns how many opened. */
    public static int unlockAround(ServerLevel level, BlockPos center, int radius) {
        int opened = 0, dy = Math.max(8, radius / 2);
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -dy, -radius), center.offset(radius, dy, radius))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof TreasuryVaultBlock && !s.getValue(OPEN)) {
                level.setBlock(p, s.setValue(OPEN, true), 3);
                level.playSound(null, p, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 1.2f, 1.0f);
                level.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.05);
                opened++;
            }
        }
        return opened;
    }
}
