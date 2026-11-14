package com.matt.quantumfalcon.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;

/**
 * Rest-pose connectivity audit for a GeckoLib/Bedrock geo (Java port of tools/check_connectivity.py).
 * Every cube must touch (within EPS px) an already-attached cube of its own bone, an ancestor bone, or an anchor
 * bone ("torso") hanging off an ancestor, flood-filled from the body/torso cubes. Every bone's pivot must also lie
 * within 1px of both its own geometry and its parent chain, so rotating the joint can't open a gap.
 */
public final class GeoConnectivity {
    static final double EPS = 0.35, JOINT = 1.0;
    static final Set<String> ANCHORS = Set.of("torso"), SEEDS = Set.of("body", "torso"), JOINT_EXEMPT = Set.of("acc_halo");

    private record Xf(double[][] a, double[] t) {}
    private record Cube(String bone, int i, double[] lo, double[] hi, double[][] m, double[] t, double[][] inv) {}

    private GeoConnectivity() {}

    /** @return human-readable problems (empty = all connected) */
    public static List<String> audit(JsonObject geo) {
        Map<String, JsonObject> bones = new LinkedHashMap<>();
        for (var e : geo.getAsJsonArray("bones")) bones.put(e.getAsJsonObject().get("name").getAsString(), e.getAsJsonObject());
        Map<String, Xf> mats = new HashMap<>();
        List<Cube> cubes = new ArrayList<>();
        for (var en : bones.entrySet()) {
            Xf x = mat(en.getKey(), bones, mats);
            if (!en.getValue().has("cubes")) continue;
            int i = 0;
            for (var ce : en.getValue().getAsJsonArray("cubes")) {
                JsonObject c = ce.getAsJsonObject();
                double[] o = vec(c.getAsJsonArray("origin")), s = vec(c.getAsJsonArray("size"));
                double inf = c.has("inflate") ? c.get("inflate").getAsDouble() : 0;
                double[] lo = {-(o[0] + s[0]) - inf, o[1] - inf, o[2] - inf}, hi = {-o[0] + inf, o[1] + s[1] + inf, o[2] + s[2] + inf};
                double[][] ca = id(); double[] ct = new double[3];
                if (c.has("rotation")) {
                    double[] cp = bb(c.has("pivot") ? vec(c.getAsJsonArray("pivot")) : o);
                    double[] r = vec(c.getAsJsonArray("rotation"));
                    ca = rot(-r[0], -r[1], r[2]); ct = sub(cp, mul(ca, cp));
                }
                double[][] m = mm(x.a, ca); double[] t = add(mul(x.a, ct), x.t);
                cubes.add(new Cube(en.getKey(), i++, lo, hi, m, t, inv(m)));
            }
        }
        Set<Cube> attached = new HashSet<>();
        for (Cube c : cubes) if (SEEDS.contains(c.bone)) attached.add(c);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Cube c : cubes) {
                if (attached.contains(c)) continue;
                Set<String> chain = chain(bones, c.bone);
                for (Cube d : List.copyOf(attached)) {
                    if ((chain.contains(d.bone) || chain(bones, d.bone).contains(c.bone)) && touches(c, d)) { attached.add(c); changed = true; break; }
                }
            }
        }
        List<String> bad = new ArrayList<>();
        for (Cube c : cubes) if (!attached.contains(c)) bad.add("floating " + c.bone + "#" + c.i);
        for (var en : bones.entrySet()) {
            String n = en.getKey(); JsonObject b = en.getValue();
            if (!b.has("parent") || ANCHORS.contains(n) || JOINT_EXEMPT.contains(n) || !b.has("cubes")) continue;
            Xf px = mats.get(b.get("parent").getAsString());
            double[] piv = add(mul(px.a, bb(vec(b.getAsJsonArray("pivot")))), px.t);
            double own = 99, par = 99;
            Set<String> parChain = chain(bones, n); parChain.remove(n);
            for (Cube c : cubes) {
                double d = dist(c, piv);
                if (c.bone.equals(n) || ancestors(bones, c.bone).contains(n)) own = Math.min(own, d);
                if (parChain.contains(c.bone)) par = Math.min(par, d);
            }
            if (own > JOINT || par > JOINT) bad.add(String.format("joint %s own=%.2f parent=%.2f", n, own, par));
        }
        return bad;
    }

    private static Set<String> chain(Map<String, JsonObject> bones, String n) {
        List<String> anc = ancestors(bones, n);
        Set<String> out = new HashSet<>(anc); out.add(n);
        for (var en : bones.entrySet())
            if (ANCHORS.contains(en.getKey()) && en.getValue().has("parent") && anc.contains(en.getValue().get("parent").getAsString())) out.add(en.getKey());
        return out;
    }
    private static List<String> ancestors(Map<String, JsonObject> bones, String n) {
        List<String> out = new ArrayList<>();
        while (bones.containsKey(n) && bones.get(n).has("parent")) { n = bones.get(n).get("parent").getAsString(); out.add(n); }
        return out;
    }
    private static Xf mat(String n, Map<String, JsonObject> bones, Map<String, Xf> cache) {
        if (cache.containsKey(n)) return cache.get(n);
        JsonObject b = bones.get(n);
        double[] piv = bb(vec(b.getAsJsonArray("pivot")));
        double[] r = b.has("rotation") ? vec(b.getAsJsonArray("rotation")) : new double[3];
        double[][] a = rot(-r[0], -r[1], r[2]); double[] t = sub(piv, mul(a, piv));
        if (b.has("parent") && bones.containsKey(b.get("parent").getAsString())) {
            Xf p = mat(b.get("parent").getAsString(), bones, cache);
            t = add(mul(p.a, t), p.t); a = mm(p.a, a);
        }
        Xf x = new Xf(a, t); cache.put(n, x); return x;
    }
    private static boolean touches(Cube a, Cube b) { return minDist(a, b) <= EPS || minDist(b, a) <= EPS; }
    /** min distance from surface samples of a to cube b */
    private static double minDist(Cube a, Cube b) {
        double best = 99; int n = 5;
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) for (int k = 0; k < n; k++) {
            if (!(i == 0 || i == n - 1 || j == 0 || j == n - 1 || k == 0 || k == n - 1)) continue;
            double[] l = {a.lo[0] + (a.hi[0] - a.lo[0]) * i / (n - 1), a.lo[1] + (a.hi[1] - a.lo[1]) * j / (n - 1), a.lo[2] + (a.hi[2] - a.lo[2]) * k / (n - 1)};
            best = Math.min(best, dist(b, add(mul(a.m, l), a.t)));
            if (best <= EPS) return best;
        }
        return best;
    }
    private static double dist(Cube c, double[] p) {
        double[] l = mul(c.inv, sub(p, c.t)); double s = 0;
        for (int k = 0; k < 3; k++) {
            double d = Math.max(Math.max(c.lo[k] - l[k], l[k] - c.hi[k]), 0);
            double norm = Math.sqrt(c.m[0][k] * c.m[0][k] + c.m[1][k] * c.m[1][k] + c.m[2][k] * c.m[2][k]);
            s += d * d * norm * norm;
        }
        return Math.sqrt(s);
    }
    // ---- tiny linear algebra ----
    private static double[] vec(JsonArray a) { return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()}; }
    private static double[] bb(double[] v) { return new double[]{-v[0], v[1], v[2]}; }
    private static double[][] id() { return new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}; }
    private static double[][] rot(double rx, double ry, double rz) {
        rx = Math.toRadians(rx); ry = Math.toRadians(ry); rz = Math.toRadians(rz);
        double[][] x = {{1, 0, 0}, {0, Math.cos(rx), -Math.sin(rx)}, {0, Math.sin(rx), Math.cos(rx)}};
        double[][] y = {{Math.cos(ry), 0, Math.sin(ry)}, {0, 1, 0}, {-Math.sin(ry), 0, Math.cos(ry)}};
        double[][] z = {{Math.cos(rz), -Math.sin(rz), 0}, {Math.sin(rz), Math.cos(rz), 0}, {0, 0, 1}};
        return mm(z, mm(y, x));
    }
    private static double[][] mm(double[][] a, double[][] b) {
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++) r[i][j] += a[i][k] * b[k][j];
        return r;
    }
    private static double[] mul(double[][] a, double[] v) { return new double[]{a[0][0] * v[0] + a[0][1] * v[1] + a[0][2] * v[2], a[1][0] * v[0] + a[1][1] * v[1] + a[1][2] * v[2], a[2][0] * v[0] + a[2][1] * v[1] + a[2][2] * v[2]}; }
    private static double[] add(double[] a, double[] b) { return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]}; }
    private static double[] sub(double[] a, double[] b) { return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }
    private static double[][] inv(double[][] m) {
        double a = m[0][0], b = m[0][1], c = m[0][2], d = m[1][0], e = m[1][1], f = m[1][2], g = m[2][0], h = m[2][1], i = m[2][2];
        double A = e * i - f * h, B = -(d * i - f * g), C = d * h - e * g, det = a * A + b * B + c * C;
        return new double[][]{{A / det, -(b * i - c * h) / det, (b * f - c * e) / det},
                {B / det, (a * i - c * g) / det, -(a * f - c * d) / det},
                {C / det, -(a * h - b * g) / det, (a * e - b * d) / det}};
    }
}
