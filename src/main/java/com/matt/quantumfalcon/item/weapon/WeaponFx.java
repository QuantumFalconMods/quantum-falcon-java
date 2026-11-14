package com.matt.quantumfalcon.item.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/** Shared helpers for Quantum Falcon weapon specials. */
public final class WeaponFx {
    private WeaponFx() {}

    /** Living things a weapon special may hit: not the user, not their own pets, not other players unless PvP damage applies normally. */
    public static List<LivingEntity> targets(LivingEntity user, Vec3 c, double r) {
        return user.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), e -> e != user && e.isAlive()
                && !(e instanceof TamableAnimal t && t.isTame() && user.getUUID().equals(t.getOwnerUUID()))
                && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand)
                && e.position().distanceTo(c) <= r);
    }

    public static boolean hit(LivingEntity user, LivingEntity t, float dmg) {
        t.invulnerableTime = 0;
        return t.hurt(user instanceof Player p ? user.damageSources().playerAttack(p) : user.damageSources().mobAttack(user), dmg);
    }

    /** Damage that ignores armour (used for "bypass armour" / "reality cut"). */
    public static boolean trueHit(LivingEntity user, LivingEntity t, float dmg) {
        t.invulnerableTime = 0;
        return t.hurt(user.damageSources().indirectMagic(user, user), dmg);
    }

    public static void effect(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> eff, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(eff, ticks, amp));
    }

    public static void burst(LivingEntity user, Vec3 p, ParticleOptions type, int n, double spread) {
        if (user.level() instanceof ServerLevel sl) sl.sendParticles(type, p.x, p.y, p.z, n, spread, spread * 0.6, spread, 0.05);
    }

    public static void line(LivingEntity user, Vec3 a, Vec3 b, ParticleOptions type) {
        if (!(user.level() instanceof ServerLevel sl)) return;
        int n = (int) Math.max(2, a.distanceTo(b) * 3);
        for (int i = 0; i <= n; i++) { Vec3 p = a.lerp(b, i / (double) n); sl.sendParticles(type, p.x, p.y, p.z, 1, 0, 0, 0, 0); }
    }

    public static void ring(LivingEntity user, Vec3 c, double r, ParticleOptions type) {
        if (!(user.level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < 32; i++) { double a = i * Math.PI / 16; sl.sendParticles(type, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0, 0.02, 0, 0.01); }
    }

    public static void sound(LivingEntity user, SoundEvent s, float vol, float pitch) {
        user.level().playSound(null, user.getX(), user.getY(), user.getZ(), s, SoundSource.PLAYERS, vol, pitch);
    }

    /** First living entity along the user's look within range (blocks stop it). */
    public static EntityHitResult lookTarget(LivingEntity user, double range) {
        Vec3 eye = user.getEyePosition(), look = user.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        HitResult block = user.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        return ProjectileUtil.getEntityHitResult(user.level(), user, eye, end, new AABB(eye, end).inflate(1.0),
                e -> e instanceof LivingEntity le && le.isAlive() && e != user && !e.isSpectator(), 0f);
    }

    public static Vec3 lookEnd(LivingEntity user, double range) {
        Vec3 eye = user.getEyePosition(), end = eye.add(user.getLookAngle().scale(range));
        HitResult block = user.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        return block.getType() == HitResult.Type.MISS ? end : block.getLocation();
    }

    /** Teleport to the furthest free spot along the look direction, up to range. */
    public static boolean blink(LivingEntity user, double range) {
        Vec3 look = user.getLookAngle();
        for (double d = range; d >= 1.5; d -= 0.5) {
            Vec3 p = user.position().add(look.scale(d));
            for (int dy = 0; dy <= 1; dy++) {
                Vec3 q = p.add(0, dy, 0);
                if (user.level().noCollision(user, user.getDimensions(user.getPose()).makeBoundingBox(q))) {
                    Vec3 from = user.position();
                    user.teleportTo(q.x, q.y, q.z);
                    user.resetFallDistance();
                    burst(user, from.add(0, 1, 0), net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 25, 0.4);
                    burst(user, q.add(0, 1, 0), net.minecraft.core.particles.ParticleTypes.PORTAL, 25, 0.4);
                    return true;
                }
            }
        }
        return false;
    }

    public static void tooltip(String id, int lines, List<Component> tip) {
        for (int i = 1; i <= lines; i++)
            tip.add(Component.translatable("item.quantumfalcon." + id + ".tooltip." + i).withStyle(i == 1 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
    }

    public static boolean onCooldown(Player p, ItemStack s) { return p.getCooldowns().isOnCooldown(s.getItem()); }

    public static boolean behind(LivingEntity target, Entity attacker) {
        Vec3 look = target.getViewVector(1f).multiply(1, 0, 1);
        Vec3 to = attacker.position().subtract(target.position()).multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-4 || to.lengthSqr() < 1e-4) return false;
        return look.normalize().dot(to.normalize()) < -0.3;
    }

    // ------------------------------------------------------------------ polish stage 3: tinted FX particles
    public static net.minecraft.core.particles.ColorParticleOption col(net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.ColorParticleOption> type, int rgb) {
        return net.minecraft.core.particles.ColorParticleOption.create(type, 0xFF000000 | rgb);
    }

    /** Crescent slash in front of the user's chest plus a spray of glints along the arc. */
    public static void slash(LivingEntity user, int rgb, int glints) {
        if (!(user.level() instanceof ServerLevel sl)) return;
        Vec3 look = user.getLookAngle();
        Vec3 c = user.getEyePosition().add(look.scale(1.6)).add(0, -0.3, 0);
        sl.sendParticles(col(com.matt.quantumfalcon.registry.ModParticles.SLASH.get(), rgb), c.x, c.y, c.z, 1, 0, 0, 0, 0);
        Vec3 flat = look.multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        for (int i = 0; i < glints; i++) {
            double a = Math.toRadians(-60 + 120.0 * i / Math.max(1, glints - 1));
            Vec3 d = flat.yRot((float) a);
            Vec3 q = user.getEyePosition().add(d.scale(1.1)).add(0, -0.3 - 0.25 * Math.sin(a), 0);
            sl.sendParticles(col(com.matt.quantumfalcon.registry.ModParticles.GLINT.get(), rgb), q.x, q.y, q.z, 0, d.x * 0.15, 0.02, d.z * 0.15, 1);
        }
    }

    public static void glints(LivingEntity user, Vec3 p, int rgb, int n, double spread) {
        if (user.level() instanceof ServerLevel sl) sl.sendParticles(col(com.matt.quantumfalcon.registry.ModParticles.GLINT.get(), rgb), p.x, p.y, p.z, n, spread, spread * 0.6, spread, 0.08);
    }

    /** Expanding shock rings on the ground plus embers thrown outwards. */
    public static void shock(LivingEntity user, Vec3 c, int rgb, double r) {
        if (!(user.level() instanceof ServerLevel sl)) return;
        sl.sendParticles(col(com.matt.quantumfalcon.registry.ModParticles.SHOCK_RING.get(), rgb), c.x, c.y + 0.15, c.z, 1, 0, 0, 0, 0);
        int n = (int) (12 * r);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n, rr = r * (0.4 + 0.6 * user.getRandom().nextDouble());
            sl.sendParticles(col(com.matt.quantumfalcon.registry.ModParticles.EMBER_FX.get(), rgb), c.x + Math.cos(a) * rr, c.y + 0.2, c.z + Math.sin(a) * rr, 0,
                Math.cos(a) * 0.08, 0.12, Math.sin(a) * 0.08, 1);
        }
    }

    /** Floating rune circle (time / void magic). */
    public static void runeCircle(LivingEntity user, Vec3 c, int rgb, double r) {
        if (!(user.level() instanceof ServerLevel sl)) return;
        int n = (int) Math.max(8, r * 5);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            sl.sendParticles(col(com.matt.quantumfalcon.registry.ModParticles.RUNE_FX.get(), rgb), c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    /** Line of glints (beams, lunges, rifts). */
    public static void trail(LivingEntity user, Vec3 a, Vec3 b, int rgb) {
        if (!(user.level() instanceof ServerLevel sl)) return;
        int n = (int) Math.max(2, a.distanceTo(b) * 4);
        for (int i = 0; i <= n; i++) { Vec3 q = a.lerp(b, i / (double) n); sl.sendParticles(col(com.matt.quantumfalcon.registry.ModParticles.GLINT.get(), rgb), q.x, q.y, q.z, 1, 0.04, 0.04, 0.04, 0.01); }
    }

    public static void sound(LivingEntity user, net.minecraft.core.Holder<SoundEvent> s, float vol, float pitch) { sound(user, s.value(), vol, pitch); }
}
