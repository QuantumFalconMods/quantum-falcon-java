package com.matt.quantumfalcon.systems;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.skill.SkillEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Falcon defence: hostiles targeting a player who has one of their own falcons close by are slowed (falcon intimidation).
 * Stage E: the skill-driven auras, poison and item pull that used to live here moved to {@link SkillEffects}, and the
 * falcon count is real (W17/B4: the old placeholder always reported one falcon, so the effects fired with none present).
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class FalconSystems {

    public static final double DEFENSE_RANGE = 15.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 40 != 0) return;
        checkFalconDefense(player);
    }

    /** Slows hostiles that target the player while one of the player's falcons is within range. Public for tests. */
    public static int checkFalconDefense(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return 0;
        if (SkillEffects.ownedFalconsNear(player, DEFENSE_RANGE) == 0) return 0;
        int n = 0;
        for (Monster hostile : serverLevel.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(DEFENSE_RANGE),
                mob -> mob.isAlive() && mob.getTarget() == player)) {
            hostile.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false));
            n++;
        }
        return n;
    }

    public static void register(IEventBus modEventBus) {
        QuantumFalcon.LOGGER.info("⚡ Falcon Systems initialized (falcon defence)");
    }
}
