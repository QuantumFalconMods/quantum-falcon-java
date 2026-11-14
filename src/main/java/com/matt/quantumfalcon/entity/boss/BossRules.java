package com.matt.quantumfalcon.entity.boss;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.arena.fight.ArenaInteraction;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/**
 * Cheese guards shared by all seven bosses (Overseer, Warden, Twins, Sovereign, Void Lord, Eclipse Phantom, Sha Sha):
 * <ul>
 * <li>a boss never mounts anything (a minecart or boat hauled it out of its arena, de-aggroed it and
 *     voided its arena kill).</li>
 * <li>suffocation, drowning and cramming don't hurt a boss (block it into a wall or a pool and wait).</li>
 * <li>a boss never changes dimension (a portal parked next to the arena swallowed it).</li>
 * <li>no ender pearl or chorus fruit out of a sealed arena while its fight is on.</li>
 * </ul>
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class BossRules {
    private BossRules() {}

    public static boolean isBoss(Entity e) { return e instanceof QFBoss; }

    public static boolean environmentImmune(DamageSource s) {
        return s.is(DamageTypes.IN_WALL) || s.is(DamageTypes.DROWN) || s.is(DamageTypes.CRAMMING) || s.is(DamageTypeTags.IS_DROWNING);
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent e) {
        if (e.isMounting() && isBoss(e.getEntityMounting())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInvulnerable(EntityInvulnerabilityCheckEvent e) {
        if (isBoss(e.getEntity()) && environmentImmune(e.getSource())) e.setInvulnerable(true);
    }

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent e) {
        if (isBoss(e.getEntity())) e.setCanceled(true);
    }

    /** True when a teleport from {@code from} to {@code to} would leave a sealed arena with a fight in progress. */
    public static boolean leavesActiveFight(ServerLevel level, Vec3 from, Vec3 to) {
        var a = ArenaMechanics.getArenaAt(level, net.minecraft.core.BlockPos.containing(from));
        if (a == null || !ArenaInteraction.FIGHTS.containsKey(a.center)) return false;
        return !ArenaMechanics.getArenaBounds(a).contains(to);
    }

    @SubscribeEvent
    public static void onPearl(EntityTeleportEvent.EnderPearl e) { guard(e); }

    @SubscribeEvent
    public static void onChorus(EntityTeleportEvent.ChorusFruit e) { guard(e); }

    private static void guard(EntityTeleportEvent e) {
        if (!(e.getEntity().level() instanceof ServerLevel sl)) return;
        if (leavesActiveFight(sl, e.getPrev(), e.getTarget())) {
            e.setCanceled(true);
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("The arena seal holds: no way out while the boss lives.")
                    .withStyle(net.minecraft.ChatFormatting.AQUA), true);
        }
    }
}
