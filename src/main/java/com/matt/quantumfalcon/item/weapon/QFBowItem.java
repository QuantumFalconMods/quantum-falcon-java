package com.matt.quantumfalcon.item.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

/**
 * Quantum Bow: arrows fly straight (no gravity) and hit 50% harder.
 * Singularity Bow: arrows collapse into a small gravity well on impact (pulls creatures within 5, 4 damage, Slowness II).
 */
@EventBusSubscriber(modid = "quantumfalcon")
public class QFBowItem extends BowItem {
    public static final String TAG = "qf_bow";
    private final String kind;

    public QFBowItem(String kind, int durability, Rarity rarity) {
        super(new Item.Properties().durability(durability).rarity(rarity));
        this.kind = kind;
    }

    public String kind() { return kind; }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        arrow.getPersistentData().putString(TAG, kind);
        WeaponHitFx.tagArrow(arrow, weaponStack, arrow.getOwner() instanceof LivingEntity le ? le : null);
        if ("quantum".equals(kind)) { arrow.setNoGravity(true); arrow.setBaseDamage(arrow.getBaseDamage() * 1.5); }
        return arrow;
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext c, List<Component> tip, TooltipFlag f) { WeaponFx.tooltip(kind + "_bow", 2, tip); }

    /** Gravity well at a point; returns how many creatures were pulled. */
    public static int gravityWell(LivingEntity shooter, Vec3 at) {
        int n = 0;
        for (LivingEntity t : WeaponFx.targets(shooter, at, 5)) {
            Vec3 to = at.subtract(t.position());
            t.setDeltaMovement(to.scale(0.35).add(0, 0.15, 0)); t.hurtMarked = true;
            WeaponFx.hit(shooter, t, 4f); WeaponFx.effect(t, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
            n++;
        }
        WeaponFx.burst(shooter, at, ParticleTypes.REVERSE_PORTAL, 60, 1.2);
        WeaponFx.ring(shooter, at, 2.5, ParticleTypes.PORTAL);
        return n;
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent e) {
        if (!(e.getProjectile() instanceof AbstractArrow arrow) || arrow.level().isClientSide) return;
        if (!"singularity".equals(arrow.getPersistentData().getString(TAG))) return;
        if (arrow.getOwner() instanceof LivingEntity shooter) {
            arrow.getPersistentData().remove(TAG);   // once per arrow
            gravityWell(shooter, e.getRayTraceResult().getLocation());
        }
    }

    /** Quantum arrows leave a trail and expire after 5 s so gravity-free arrows don't fly forever. */
    @SubscribeEvent
    public static void onArrowTick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof AbstractArrow arrow) || arrow.level().isClientSide) return;
        String k = arrow.getPersistentData().getString(TAG);
        if (k.isEmpty()) return;
        if ("quantum".equals(k) && arrow.tickCount > 100 && arrow.isNoGravity()) arrow.setNoGravity(false);
        if (arrow.tickCount % 2 == 0 && arrow.getDeltaMovement().lengthSqr() > 0.05 && arrow.level() instanceof net.minecraft.server.level.ServerLevel sl)
            sl.sendParticles("quantum".equals(k) ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL, arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0, 0, 0, 0);
    }
}
