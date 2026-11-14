package com.matt.quantumfalcon.item.weapon;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.registry.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;

/**
 * Melee hit FX for every Quantum Falcon weapon (themed slash arc + glints + swing sound) and coloured trails
 * for arrows fired from Quantum Falcon bows. Purely cosmetic; damage logic lives in the weapon classes.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class WeaponHitFx {
    private WeaponHitFx() {}

    public static final Map<String, Integer> COLOURS = Map.ofEntries(
        Map.entry("quantum_blade", 0x50F0FF), Map.entry("reality_splitter", 0xFF60E0), Map.entry("void_reaper", 0xB05CFF),
        Map.entry("temporal_staff", 0xFFD36A), Map.entry("quantum_staff", 0x43E6FF), Map.entry("rift_spear", 0xC080FF), Map.entry("quantum_spear", 0x43E6FF),
        Map.entry("chaos_hammer", 0xFF5A2A), Map.entry("echo_dagger", 0x3FF0E0), Map.entry("quantum_dagger", 0x9C6BFF),
        Map.entry("quantum_bow", 0x50F0FF), Map.entry("singularity_bow", 0xFF9A30));
    public static final String TRAIL_TAG = "qf_trail";

    public static Integer colourOf(ItemStack s) {
        var id = BuiltInRegistries.ITEM.getKey(s.getItem());
        return id.getNamespace().equals(QuantumFalcon.MOD_ID) ? COLOURS.get(id.getPath()) : null;
    }

    /** Spawns the hit FX; returns true when the held item is a themed weapon (used by tests). */
    public static boolean hitFx(LivingEntity attacker, LivingEntity target) {
        Integer c = colourOf(attacker.getMainHandItem());
        if (c == null || !(attacker.level() instanceof ServerLevel)) return false;
        String path = BuiltInRegistries.ITEM.getKey(attacker.getMainHandItem().getItem()).getPath();
        if (path.endsWith("_bow")) return false;
        WeaponFx.slash(attacker, c, path.contains("dagger") ? 5 : 9);
        WeaponFx.glints(attacker, target.position().add(0, target.getBbHeight() * 0.6, 0), c, path.contains("hammer") ? 16 : 8, 0.25);
        float pitch = path.contains("dagger") ? 1.35f : path.contains("hammer") || path.contains("reaper") ? 0.75f : 1.0f;
        WeaponFx.sound(attacker, ModSounds.WEAPON_SWING, 0.7f, pitch + attacker.getRandom().nextFloat() * 0.1f);
        return true;
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post e) {
        if (e.getSource().getDirectEntity() instanceof LivingEntity a && e.getSource().getEntity() == a && e.getNewDamage() > 0 && !e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE))
            if (!(e.getEntity() == a)) hitFx(a, e.getEntity());
    }

    /** Arrow trails: QFBowItem tags its arrows with the bow colour. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof AbstractArrow arrow) || !(arrow.level() instanceof ServerLevel sl)) return;
        if (!arrow.getPersistentData().contains(TRAIL_TAG)) return;
        if (arrow.onGround() || arrow.tickCount > 120) return;
        int c = arrow.getPersistentData().getInt(TRAIL_TAG);
        sl.sendParticles(WeaponFx.col(com.matt.quantumfalcon.registry.ModParticles.GLINT.get(), c), arrow.getX(), arrow.getY(), arrow.getZ(), 2, 0.03, 0.03, 0.03, 0.005);
    }

    /** Called by QFBowItem when an arrow leaves the bow. */
    public static void tagArrow(AbstractArrow arrow, ItemStack bow, LivingEntity shooter) {
        Integer c = colourOf(bow);
        if (c == null) return;
        arrow.getPersistentData().putInt(TRAIL_TAG, c);
        if (shooter != null) WeaponFx.glints(shooter, shooter.getEyePosition().add(shooter.getLookAngle().scale(1.0)).add(0, -0.2, 0), c, 10, 0.15);
    }
}
