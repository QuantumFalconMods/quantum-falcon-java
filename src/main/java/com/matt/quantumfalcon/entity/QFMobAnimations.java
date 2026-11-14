package com.matt.quantumfalcon.entity;

import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.*;

/** Shared idle/walk/death + triggerable attack controllers for the GeckoLib hostile mob bodies. */
public final class QFMobAnimations {
    private QFMobAnimations() {}

    public static <T extends LivingEntity & GeoEntity> void register(T mob, String id, AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation." + id + ".idle");
        RawAnimation walk = RawAnimation.begin().thenLoop("animation." + id + ".walk");
        RawAnimation attack = RawAnimation.begin().thenPlay("animation." + id + ".attack");
        RawAnimation death = RawAnimation.begin().thenPlayAndHold("animation." + id + ".death");
        controllers.add(new AnimationController<>(mob, "move", 4, state -> {
            if (mob.isDeadOrDying()) return state.setAndContinue(death);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        controllers.add(new AnimationController<>(mob, "action", 2, state -> PlayState.STOP)
                .triggerableAnim("attack", attack));
    }
}
