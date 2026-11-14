package com.matt.quantumfalcon.dungeon;

import com.matt.quantumfalcon.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A10: the Hour Socket flips its then/now volume. Spend a stitch: one Temporal Dust (used up), or a Time Bottle (kept;
 * the volume's cooldown is the limit). Bare hands only explain.
 */
public class HourSocketBlock extends Block {
    /** Which hour its volume is in (the pedestal's colours: gold Then, cyan Now). */
    public enum HourShown implements net.minecraft.util.StringRepresentable {
        THEN, NOW;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<HourShown> HOUR =
        net.minecraft.world.level.block.state.properties.EnumProperty.create("hour", HourShown.class);
    /** The brief charged look while the hour settles after a flip (ThenNow.FLIP_COOLDOWN ticks). */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty CHARGED =
        net.minecraft.world.level.block.state.properties.BooleanProperty.create("charged");
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
        Block.box(2, 0, 2, 14, 5, 14), Block.box(4, 5, 4, 12, 7, 12), Block.box(6, 7, 6, 10, 15, 10));

    public HourSocketBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(HOUR, HourShown.NOW).setValue(CHARGED, false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> b) { b.add(HOUR, CHARGED); }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s, net.minecraft.world.level.BlockGetter l, BlockPos p, net.minecraft.world.phys.shapes.CollisionContext c) { return SHAPE; }

    /** Shows a volume's new hour on a socket, charged until the hour settles. */
    public static void show(ServerLevel level, BlockPos pos, ThenNow.Hour hour) {
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof HourSocketBlock)) return;
        level.setBlock(pos, st.setValue(HOUR, hour == ThenNow.Hour.THEN ? HourShown.THEN : HourShown.NOW).setValue(CHARGED, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, st.getBlock(), ThenNow.FLIP_COOLDOWN);
    }
    @Override protected void tick(BlockState st, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource r) {
        if (st.getValue(CHARGED)) level.setBlock(pos, st.setValue(CHARGED, false), Block.UPDATE_ALL);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean dust = stack.is(ModItems.TEMPORAL_DUST.get()), bottle = stack.is(ModItems.TIME_BOTTLE.get());
        if (!dust && !bottle) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        String why = shift((ServerLevel) level, pos, player);
        if (why == null && dust && !player.getAbilities().instabuild) stack.shrink(1);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) player.displayClientMessage(Component.literal("An hourglass socket. A pinch of Temporal Dust or a Time Bottle shifts the hour.")
            .withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Flips the socket's volume. Returns null on success, else the message shown. */
    public static String shift(ServerLevel level, BlockPos socket, Player who) {
        ThenNow tn = ThenNow.get(level);
        ThenNow.Volume v = tn.at(level, socket);
        String why = v == null ? "Nothing here remembers another hour." : tn.flip(level, v, who);
        if (why == null) show(level, socket, v.hour);   // the far-side socket too (ThenNow.flip shows the volume's own)
        if (who != null) who.displayClientMessage(Component.literal(why != null ? why
            : v.hour == ThenNow.Hour.THEN ? "The hour turns back: Then." : "The hour runs on: Now.").withStyle(why != null ? ChatFormatting.GRAY : ChatFormatting.LIGHT_PURPLE), true);
        return why;
    }
}
