package com.matt.quantumfalcon.block;

import com.matt.quantumfalcon.blockentity.FalconEggBlockEntity;
import com.matt.quantumfalcon.blockentity.FalconNestBlockEntity;
import com.matt.quantumfalcon.breeding.NestQuality;
import com.matt.quantumfalcon.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/** Falcon nest (twig, crystal, void-forged, Quantum Aerie). An egg placed on top sits in the bowl and incubates faster;
 * bred eggs are laid into a free nest nearby; falcons rest and brood here. Quality: {@link NestQuality}. */
public class FalconNestBlock extends BaseEntityBlock {
    public static final MapCodec<FalconNestBlock> CODEC = simpleCodec(p -> new FalconNestBlock(NestQuality.TWIG, p));
    public final NestQuality quality;
    private final VoxelShape shape;

    public FalconNestBlock(NestQuality quality, Properties p) {
        super(p);
        this.quality = quality;
        double h = Math.max(4, quality.seatPx + 2);
        this.shape = Block.box(1, 0, 1, 15, h, 15);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return shape; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FalconNestBlockEntity(pos, state); }

    /** Nest quality under an egg position (null if there is no nest). The legacy Quantum Nest counts as {@link NestQuality#QUANTUM}. */
    @Nullable public static NestQuality qualityAt(BlockGetter level, BlockPos nestPos) {
        BlockState s = level.getBlockState(nestPos);
        if (s.getBlock() instanceof FalconNestBlock n) return n.quality;
        if (s.is(ModBlocks.QUANTUM_NEST.get())) return NestQuality.QUANTUM;
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Component egg = level.getBlockEntity(pos.above()) instanceof FalconEggBlockEntity be ? be.status()
                : Component.literal("Empty: place an egg on it, or let a bred pair lay here.").withStyle(ChatFormatting.GRAY);
        player.displayClientMessage(Component.literal(quality.displayName + " ").withStyle(ChatFormatting.AQUA).append(egg), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Nest tier " + quality.tier).withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal(String.format("Hatch speed x%.2f, +%d%% mutation chance", quality.speed, Math.round(quality.mutation * 100))).withStyle(ChatFormatting.GRAY));
        StringBuilder s = new StringBuilder("Counts as: warmth");
        if (quality.heat) s.append(", heat");
        if (quality.riftEnergy) s.append(", rift energy");
        if (quality.quantum) s.append(", quantum incubation");
        tip.add(Component.literal(s.toString()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Place an egg on top; falcons rest and brood here.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
