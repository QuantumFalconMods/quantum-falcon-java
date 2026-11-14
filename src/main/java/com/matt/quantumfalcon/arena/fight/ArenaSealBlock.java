package com.matt.quantumfalcon.arena.fight;

import com.matt.quantumfalcon.entity.boss.QFBoss;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The energy wall that rings an arena while its boss fight runs. Unbreakable; it checks itself every 2 seconds and dissolves
 * once no living boss is within 64 blocks, so a restart or an abandoned fight never leaves a wall behind.
 */
public class ArenaSealBlock extends HalfTransparentBlock {
    /** only a sparse lattice of seal blocks emits light (about 1 in 12 wall blocks), so raising ~25-36k seals
     *  doesn't force the client to relight every one; the texture still reads as glowing. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT = net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;

    public ArenaSealBlock(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(LIT, false)); }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> b) { b.add(LIT); }

    /** The state for a seal at this position: lit on a sparse lattice. */
    public BlockState stateAt(BlockPos q, boolean roof) {
        boolean lit = !roof && Math.floorMod(q.getY(), 4) == 0 && Math.floorMod(q.getX() + q.getZ(), 3) == 0;
        return defaultBlockState().setValue(LIT, lit);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 40 + level.random.nextInt(20));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // cheap path first: the roof made seals ~10x more numerous, so skip the 128-block entity scan while a fight owns them
        if (ArenaInteraction.fightNear(pos)) { level.scheduleTick(pos, this, 40 + random.nextInt(20)); return; }
        boolean boss = !level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(pos).inflate(64),
            m -> m instanceof QFBoss && m.isAlive()).isEmpty();
        if (boss) level.scheduleTick(pos, this, 40);
        else level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction dir) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, dir);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1.0f; }
}
