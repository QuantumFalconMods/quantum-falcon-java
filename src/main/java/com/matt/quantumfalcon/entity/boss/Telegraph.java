package com.matt.quantumfalcon.entity.boss;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A ground danger zone drawn while a boss winds up an attack (stage 4 movesets). The same shape is used by the attack
 * itself to decide who gets hit, so what the player sees is exactly what lands: step out of the marked area during the
 * wind-up and the attack misses.
 * <ul>
 * <li>CIRCLE: disc of radius {@code a} around {@code origin};</li>
 * <li>RING: annulus between {@code a} (inner, safe) and {@code b} (outer) around {@code origin};</li>
 * <li>LINE: a lane of half-width {@code a} from {@code origin} along {@code dir} (dir length = lane length);</li>
 * <li>CONE: a wedge of half-angle {@code a} degrees and range {@code b} from {@code origin} along {@code dir}.</li>
 * </ul>
 * Containment is horizontal, with a vertical band of {@link #V_BAND} blocks above and below the shape's floor.
 */
public record Telegraph(Kind kind, Vec3 origin, Vec3 dir, double a, double b) {
    public enum Kind { CIRCLE, RING, LINE, CONE }

    public static final double V_BAND = 4.0;
    /** Which audio cue each live telegraph has already played (weak: dropped with the telegraph). */
    private static final java.util.Map<Telegraph, Integer> CUED = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static final Vector3f DANGER = new Vector3f(1f, 0.23f, 0.31f);   // #FF3A4F

    /** The floor under a (possibly hovering) boss: first solid block within 12 below, else the position itself. */
    public static Vec3 floorBelow(net.minecraft.world.level.Level level, Vec3 at) {
        net.minecraft.core.BlockPos.MutableBlockPos m = net.minecraft.core.BlockPos.containing(at).mutable();
        for (int i = 0; i < 12; i++, m.move(0, -1, 0))
            if (level.getBlockState(m.below()).isSolid()) return new Vec3(at.x, m.getY(), at.z);
        return at;
    }

    public static Telegraph circle(Vec3 c, double r) { return new Telegraph(Kind.CIRCLE, c, Vec3.ZERO, r, 0); }
    public static Telegraph ring(Vec3 c, double inner, double outer) { return new Telegraph(Kind.RING, c, Vec3.ZERO, inner, outer); }
    /** Lane from {@code from} through {@code toward}, {@code length} long. */
    public static Telegraph line(Vec3 from, Vec3 toward, double length, double halfWidth) {
        Vec3 d = new Vec3(toward.x - from.x, 0, toward.z - from.z);
        if (d.lengthSqr() < 1e-6) d = new Vec3(1, 0, 0);
        return new Telegraph(Kind.LINE, from, d.normalize().scale(length), halfWidth, 0);
    }
    public static Telegraph cone(Vec3 origin, Vec3 toward, double halfAngleDeg, double range) {
        Vec3 d = new Vec3(toward.x - origin.x, 0, toward.z - origin.z);
        if (d.lengthSqr() < 1e-6) d = new Vec3(1, 0, 0);
        return new Telegraph(Kind.CONE, origin, d.normalize(), halfAngleDeg, range);
    }

    public String describe() {
        return switch (kind) {
            case CIRCLE -> String.format("circle r%.1f", a);
            case RING -> String.format("ring %.1f-%.1f", a, b);
            case LINE -> String.format("line %.1fx%.1f", dir.length(), a * 2);
            case CONE -> String.format("cone %.0fdeg r%.1f", a * 2, b);
        };
    }

    /** True if {@code p} (feet position) is inside the danger zone. */
    public boolean contains(Vec3 p) {
        if (Math.abs(p.y - origin.y) > V_BAND) return false;
        double dx = p.x - origin.x, dz = p.z - origin.z, d2 = dx * dx + dz * dz;
        return switch (kind) {
            case CIRCLE -> d2 <= a * a;
            case RING -> d2 >= a * a && d2 <= b * b;
            case LINE -> {
                double len = dir.length();
                double ux = dir.x / len, uz = dir.z / len;
                double along = dx * ux + dz * uz, across = Math.abs(-dx * uz + dz * ux);
                yield along >= -a && along <= len && across <= a;
            }
            case CONE -> {
                if (d2 > b * b) yield false;
                if (d2 < 1.0) yield true;
                double cos = (dx * dir.x + dz * dir.z) / Math.sqrt(d2);
                yield cos >= Math.cos(Math.toRadians(a));
            }
        };
    }

    /**
     * Draw the zone at wind-up progress {@code t} (0..1): the outline in danger red and the boss colour, a fill that grows
     * from the origin with progress, and a white flash on the outline in the last quarter (the "now" cue).
     */
    public void draw(ServerLevel sl, double t, int rgb) {
        // audio: arena_telegraph when the zone first appears, arena_telegraph_lock as it crosses into the white "now" flash
        int cue = t >= 0.75 ? 2 : 1, had = CUED.getOrDefault(this, 0);
        if (cue > had) {
            CUED.put(this, cue);
            sl.playSound(null, origin.x, origin.y, origin.z, com.matt.quantumfalcon.entity.util.QFMobSounds.get(cue == 1 ? "arena_telegraph" : "arena_telegraph_lock"),
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f);
        }
        double y = origin.y + 0.15;
        DustParticleOptions edge = new DustParticleOptions(DANGER, 1.4f);
        DustParticleOptions tint = new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 1.1f);
        boolean flash = t >= 0.75;
        switch (kind) {
            case CIRCLE -> { arc(sl, origin.x, y, origin.z, a, 0, 360, edge, flash); fillDisc(sl, y, 0, a * t, tint); }
            case RING -> {
                arc(sl, origin.x, y, origin.z, a, 0, 360, tint, false);
                arc(sl, origin.x, y, origin.z, b, 0, 360, edge, flash);
                fillDisc(sl, y, a, a + (b - a) * t, tint);
            }
            case LINE -> {
                double len = dir.length();
                double ux = dir.x / len, uz = dir.z / len, px = -uz, pz = ux;
                for (double s = 0; s <= len; s += 0.6) {
                    for (int side = -1; side <= 1; side += 2) {
                        double x = origin.x + ux * s + px * a * side, z = origin.z + uz * s + pz * a * side;
                        sl.sendParticles(edge, x, y, z, 1, 0, 0, 0, 0);
                        if (flash && ((int) (s / 0.6)) % 3 == 0) sl.sendParticles(ParticleTypes.END_ROD, x, y + 0.1, z, 1, 0, 0.02, 0, 0);
                    }
                    if (s <= len * t) sl.sendParticles(tint, origin.x + ux * s, y, origin.z + uz * s, 1, a * 0.35, 0, a * 0.35, 0);
                }
            }
            case CONE -> {
                double base = Math.toDegrees(Math.atan2(dir.z, dir.x));
                for (int side = -1; side <= 1; side += 2) {
                    double ang = Math.toRadians(base + a * side);
                    for (double s = 0.5; s <= b; s += 0.6) sl.sendParticles(edge, origin.x + Math.cos(ang) * s, y, origin.z + Math.sin(ang) * s, 1, 0, 0, 0, 0);
                }
                arc(sl, origin.x, y, origin.z, b, base - a, base + a, edge, flash);
                double r = b * t;
                if (r > 0.5) arc(sl, origin.x, y, origin.z, r, base - a, base + a, tint, false);
            }
        }
    }

    private static void arc(ServerLevel sl, double cx, double y, double cz, double r, double fromDeg, double toDeg, DustParticleOptions p, boolean flash) {
        double step = Math.toDegrees(0.6 / Math.max(0.6, r));
        int i = 0;
        for (double d = fromDeg; d <= toDeg + 1e-6; d += step, i++) {
            double x = cx + Math.cos(Math.toRadians(d)) * r, z = cz + Math.sin(Math.toRadians(d)) * r;
            sl.sendParticles(p, x, y, z, 1, 0, 0, 0, 0);
            if (flash && i % 3 == 0) sl.sendParticles(ParticleTypes.END_ROD, x, y + 0.1, z, 1, 0, 0.02, 0, 0);
        }
    }

    private void fillDisc(ServerLevel sl, double y, double r0, double r1, DustParticleOptions p) {
        if (r1 <= r0 + 0.2) return;
        int n = (int) Math.min(80, (r1 * r1 - r0 * r0) * 0.8 + 4);
        var rnd = sl.random;
        for (int i = 0; i < n; i++) {
            double r = Math.sqrt(r0 * r0 + rnd.nextDouble() * (r1 * r1 - r0 * r0)), ang = rnd.nextDouble() * Math.PI * 2;
            sl.sendParticles(p, origin.x + Math.cos(ang) * r, y, origin.z + Math.sin(ang) * r, 1, 0, 0, 0, 0);
        }
    }
}
