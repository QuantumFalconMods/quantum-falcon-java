package com.matt.quantumfalcon.mixin;

import com.matt.quantumfalcon.dungeon.DungeonWard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * inside a dungeon ward, warded blocks report bedrock hardness (-1) on the server. Machines that respect hardness
 * (Create drills, Mekanism miners, AE2 annihilation planes, quarries) then refuse them cleanly. The same blocks keep their
 * normal hardness everywhere else, so they stay ordinary building blocks once crafted.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateDestroySpeedMixin {
    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void quantumfalcon$wardHardness(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (DungeonWard.wardsHardness(level, pos, (BlockState) (Object) this)) cir.setReturnValue(-1.0f);
    }
}
