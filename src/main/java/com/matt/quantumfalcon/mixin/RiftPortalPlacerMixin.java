package com.matt.quantumfalcon.mixin;

import com.matt.quantumfalcon.dungeon.DungeonWard;
import net.kyrptonaught.customportalapi.portal.PortalPlacer;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** the Rift portal's arrival frame (CustomPortalAPI) is built outside any dungeon, like the nether portal. */
@Mixin(value = PortalPlacer.class, remap = false)
public abstract class RiftPortalPlacerMixin {
    @Inject(method = "createDestinationPortal", at = @At("HEAD"), cancellable = true)
    private static void quantumfalcon$avoidDungeons(ServerLevel level, BlockPos pos, BlockState frame, Direction.Axis axis,
                                                    CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        BlockPos safe = DungeonWard.portalSiteOutside(level, pos);
        if (safe != null) cir.setReturnValue(PortalPlacer.createDestinationPortal(level, safe, frame, axis));
    }
}
