package com.matt.quantumfalcon.blockentity;

import com.matt.quantumfalcon.block.FalconNestBlock;
import com.matt.quantumfalcon.breeding.NestQuality;
import com.matt.quantumfalcon.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Animated nest (idle details: feathers sway, crystals pulse, rift rings and halo spin). Stateless: the egg is its own
 * block entity on top, so nests never hold item data that could be lost. */
public class FalconNestBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.falcon_nest.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public FalconNestBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.FALCON_NEST.get(), pos, state); }

    public NestQuality quality() { return getBlockState().getBlock() instanceof FalconNestBlock n ? n.quality : NestQuality.TWIG; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { c.add(new AnimationController<>(this, "idle", 0, e -> e.setAndContinue(IDLE))); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
