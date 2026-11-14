package com.matt.quantumfalcon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Pedestal Block - A decorative display block for items.
 * Right-click to place/remove items on top.
 */
public class PedestalBlock extends Block {
    
    public static final MapCodec<PedestalBlock> CODEC = simpleCodec(PedestalBlock::new);
    
    public PedestalBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    // a real display. The shown item lives in a vanilla item_display entity tagged qf_pedestal above the block.
    private static java.util.List<net.minecraft.world.entity.Display.ItemDisplay> shown(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class, new net.minecraft.world.phys.AABB(pos.above()).inflate(0.1), e -> e.getTags().contains("qf_pedestal"));
    }
    private static net.minecraft.world.item.ItemStack itemOf(net.minecraft.world.entity.Display.ItemDisplay d) {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag(); d.saveWithoutId(t);
        return net.minecraft.world.item.ItemStack.parseOptional(d.registryAccess(), t.getCompound("item"));
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                                  net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return net.minecraft.world.ItemInteractionResult.SUCCESS;
        if (!shown(level, pos).isEmpty()) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;   // full: take it first
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putString("id", "minecraft:item_display");
        net.minecraft.nbt.ListTag posTag = new net.minecraft.nbt.ListTag();
        for (double v : new double[]{pos.getX() + 0.5, pos.getY() + 1.35, pos.getZ() + 0.5}) posTag.add(net.minecraft.nbt.DoubleTag.valueOf(v));
        t.put("Pos", posTag);
        t.put("item", stack.copyWithCount(1).save(level.registryAccess()));
        t.putString("billboard", "vertical");
        net.minecraft.nbt.ListTag tags = new net.minecraft.nbt.ListTag(); tags.add(net.minecraft.nbt.StringTag.valueOf("qf_pedestal")); t.put("Tags", tags);
        net.minecraft.world.entity.EntityType.create(t, level).ifPresent(level::addFreshEntity);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1f);
        return net.minecraft.world.ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        for (var d : shown(level, pos)) {
            net.minecraft.world.item.ItemStack it = itemOf(d);
            d.discard();
            if (!player.getInventory().add(it)) player.drop(it, false);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1f);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide)
            for (var d : shown(level, pos)) { popResource(level, pos, itemOf(d)); d.discard(); }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** Test hook: what the pedestal shows (empty if nothing). */
    public static net.minecraft.world.item.ItemStack showing(Level level, BlockPos pos) {
        var l = shown(level, pos); return l.isEmpty() ? net.minecraft.world.item.ItemStack.EMPTY : itemOf(l.get(0));
    }
}
