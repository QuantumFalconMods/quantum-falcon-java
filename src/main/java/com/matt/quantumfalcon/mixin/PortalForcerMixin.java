package com.matt.quantumfalcon.mixin;

import com.matt.quantumfalcon.dungeon.DungeonWard;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * a nether portal arriving where a dungeon stands is built (or linked) outside the dungeon instead, so arrival
 * never replaces warded blocks or opens an entrance past the key door.
 */
@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {
    @Shadow @Final private ServerLevel level;

    @Inject(method = "createPortal", at = @At("HEAD"), cancellable = true)
    private void quantumfalcon$avoidDungeons(BlockPos pos, Direction.Axis axis, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        BlockPos safe = DungeonWard.portalSiteOutside(level, pos);
        if (safe == null) return;
        PortalForcer self = (PortalForcer) (Object) this;
        Optional<BlockPos> found = self.findClosestPortalPosition(safe, level.dimension() == Level.NETHER, level.getWorldBorder());
        if (found.isPresent()) {
            BlockPos f = found.get();
            BlockState st = level.getBlockState(f);
            Direction.Axis a = st.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
            cir.setReturnValue(Optional.of(BlockUtil.getLargestRectangleAround(f, a, 21, Direction.Axis.Y, 21, p -> level.getBlockState(p) == st)));
            return;
        }
        cir.setReturnValue(self.createPortal(safe, axis));
    }
}
