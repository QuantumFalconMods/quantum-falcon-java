package com.matt.quantumfalcon.mixin;

import com.matt.quantumfalcon.dungeon.DungeonWard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** fluid poured outside a dungeon can't flow into it (water features already inside still flow). */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void quantumfalcon$dungeonWard(LevelAccessor level, BlockPos pos, BlockState state, Direction dir, FluidState fluid, CallbackInfo ci) {
        if (DungeonWard.blocksFluidInto(level, pos, dir)) ci.cancel();
    }
}
