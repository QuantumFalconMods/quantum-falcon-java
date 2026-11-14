package com.matt.quantumfalcon.mixin;

import com.matt.quantumfalcon.dungeon.DungeonWard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * block breakers from other mods that never fire BlockEvent.BreakEvent (Create's drill and saw call setBlock directly)
 * can't remove warded dungeon blocks. Only removals (to air or a fluid) of warded blocks inside a ward are checked, and the
 * caller check runs last, so the hot path is a couple of comparisons.
 */
@Mixin(Level.class)
public abstract class LevelSetBlockMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void quantumfalcon$wardRemoval(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> cir) {
        if (DungeonWard.wardsRemoval((Level) (Object) this, pos, state)) cir.setReturnValue(false);
    }
}
