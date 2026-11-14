package com.matt.quantumfalcon.breeding;

import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Pure breeding rules (no world access), so every rule is unit-testable. See BREEDING.md for the design.
 */
public final class BreedingEngine {
    private BreedingEngine() {}

    // ── lookups ───────────────────────────────────────────────────────────────────────────────────────────────
    private static final Map<String, BreedingData.VariantDef> VARIANTS = new HashMap<>();
    private static final Map<String, BreedingData.AbilityDef> ABILITIES = new HashMap<>();
    private static final Map<String, String> NATIVE_CRACK = new HashMap<>();
    public static final Set<String> RIFT_VARIANTS = Set.of("voidstalker", "aetherwing", "warped", "entangled_prism", "abyssal");
    static {
        for (var v : BreedingData.VARIANTS) VARIANTS.put(v.id(), v);
        for (var a : BreedingData.ABILITIES) ABILITIES.put(a.id(), a);
        String[][] nat = {{"peregrine", "CYAN"}, {"prairie", "AMBER"}, {"tundra", "CYAN"}, {"coral", "MAGENTA"}, {"mushroom", "CRIMSON"},
            {"deepslate", "CYAN"}, {"tempest", "CYAN"}, {"stormrider", "VIOLET"}, {"crimson", "CRIMSON"}, {"soulfire", "CYAN"},
            {"warped", "GREEN"}, {"voidstalker", "VIOLET"}, {"aetherwing", "WHITE"}, {"abyssal", "CYAN"}, {"entangled_prism", "MAGENTA"},
            {"singularity", "VIOLET"}, {"cosmic", "VIOLET"}, {"quantum_phoenix", "AMBER"}, {"tidecaller", "CYAN"}, {"emberfrost", "AMBER"},
            {"sporewraith", "GREEN"}, {"aurora_seraph", "AURORA"}, {"rift_reaver", "CRIMSON"}, {"chronowing", "WHITE"},
            {"nebula_prism", "MAGENTA"}, {"paradox", "PRISMATIC"}};
        for (String[] n : nat) NATIVE_CRACK.put(n[0], n[1]);
    }
    @Nullable public static BreedingData.VariantDef variantDef(String id) { return VARIANTS.get(id); }
    @Nullable public static BreedingData.AbilityDef abilityDef(String id) { return ABILITIES.get(id); }
    public static int variantTier(String v) { var d = VARIANTS.get(v); return d == null ? 1 : d.tier(); }
    public static int crackIndex(String id) {
        for (int i = 0; i < BreedingData.CRACKS.size(); i++) if (BreedingData.CRACKS.get(i).id().equals(id)) return i;
        return 0;
    }
    public static int patternIndex(String id) {
        for (int i = 0; i < BreedingData.PATTERNS.size(); i++) if (BreedingData.PATTERNS.get(i).id().equals(id)) return i;
        return 0;
    }
    /** The crack colour a variant's own glow texture is painted in (falcons with this crack render the original texture). */
    public static int nativeCrack(String variant) { return crackIndex(NATIVE_CRACK.getOrDefault(variant, "CYAN")); }

    // ── context ───────────────────────────────────────────────────────────────────────────────────────────────
    /** Where the egg is and what helps it. All fields optional. */
    public static final class Context {
        public @Nullable String catalyst;
        public boolean snowy, nether, end, rift, voidBiome, aurora, highInstability, bothBonded;
        public int riftLayer = -1;
        public int pity;           // earlier failed tier-ups at this tier for this owner
        public float nestMutation; // extra rare-mutation chance from the nest the egg is laid in (NestQuality)
        public Context catalyst(String c) { catalyst = c; return this; }
    }

    public static final class Result {
        public FalconGenome child;
        public boolean tierUpRolled, tierUp;
        public float tierUpChance;
        public @Nullable String hybrid, fusion;
        public final List<String> mutations = new ArrayList<>();
    }

    // ── wild genomes ──────────────────────────────────────────────────────────────────────────────────────────
    public static FalconGenome wild(String variant, int tier, RandomSource r) {
        FalconGenome g = new FalconGenome();
        g.variant = VARIANTS.containsKey(variant) ? variant : "peregrine";
        g.tier = FalconGenome.clamp(Math.max(tier, variantTier(g.variant)), 1, 7);
        int lo = 5 + (g.tier - 1) * 12;
        for (int i = 0; i < FalconGenome.STATS; i++) for (int k = 0; k < 2; k++) g.stats[i][k] = FalconGenome.clamp(lo + r.nextInt(26), 1, 100);
        int nat = nativeCrack(g.variant);
        g.crack[0] = nat; g.crack[1] = r.nextFloat() < 0.6f ? nat : randomCommonCrack(r);
        g.pattern[0] = randomCommonPattern(r); g.pattern[1] = r.nextFloat() < 0.15f ? patternIndex(r.nextBoolean() ? "SCALED" : "MARBLED") : randomCommonPattern(r);
        for (int k = 0; k < 2; k++) { float s = r.nextFloat(); g.size[k] = s < 0.25f ? -1 : s < 0.8f ? 0 : 1; }
        for (int k = 0; k < 2; k++) g.glow[k] = FalconGenome.clamp(r.nextInt(3) + (g.tier >= 5 ? 1 : 0), 0, 3);
        fillAbilities(g, new ArrayList<>(), r);
        return g;
    }

    static int randomCommonCrack(RandomSource r) {
        List<Integer> c = new ArrayList<>();
        for (int i = 0; i < BreedingData.CRACKS.size(); i++) if (!BreedingData.CRACKS.get(i).mutationOnly() && !BreedingData.CRACKS.get(i).id().equals("VOID")) c.add(i);
        return c.get(r.nextInt(c.size()));
    }
    static int randomCommonPattern(RandomSource r) { return r.nextInt(4); }   // SOLID..STRIPED

    // ── breeding ──────────────────────────────────────────────────────────────────────────────────────────────
    public static Result breed(FalconGenome a, FalconGenome b, Context ctx, RandomSource r) {
        Result res = new Result();
        FalconGenome c = new FalconGenome();
        String cat = ctx.catalyst;
        Set<String> blA = a.bloodlines(), blB = b.bloodlines();

        // variant (hybrids first, then the original special pairings, then 50/50)
        c.variant = null;
        for (var v : BreedingData.VARIANTS) {
            if (v.parentA() == null || !pair(a.variant, b.variant, v.parentA(), v.parentB())) continue;
            String need = BreedingData.HYBRID_CATALYST.get(v.id());
            if (need != null && !need.equals(cat)) continue;
            if (v.id().equals("paradox") && (a.tier < 6 || b.tier < 6)) continue;
            if (r.nextFloat() < BreedingData.HYBRID_CHANCE.getOrDefault(v.id(), 0f)) { c.variant = v.id(); res.hybrid = v.id(); }
            break;
        }
        if (c.variant == null) {
            if (pair(a.variant, b.variant, "stormrider", "voidstalker") && r.nextFloat() < 0.15f) c.variant = "entangled_prism";
            else if (pair(a.variant, b.variant, "entangled_prism", "aetherwing") && r.nextFloat() < 0.01f) c.variant = "singularity";
            else if ((isLegendary(a.variant) && RIFT_VARIANTS.contains(b.variant) || isLegendary(b.variant) && RIFT_VARIANTS.contains(a.variant)) && r.nextFloat() < 0.10f) c.variant = "quantum_phoenix";
            else if (RIFT_VARIANTS.contains(a.variant) && RIFT_VARIANTS.contains(b.variant) && r.nextFloat() < 0.10f) c.variant = "cosmic";
            else if (ctx.highInstability && r.nextFloat() < 0.15f) { c.variant = "singularity"; res.mutations.add("instability:singularity"); }
            else c.variant = r.nextBoolean() ? a.variant : b.variant;
        }

        // tier
        int base = Math.max(Math.min(a.tier, b.tier), (a.tier + b.tier) / 2);
        base = Math.max(base, variantTier(c.variant));
        c.tier = FalconGenome.clamp(base, 1, 7);
        if (c.tier < 7) {
            float ch = BreedingData.TIER_UP[c.tier - 1];
            if (a.tier == b.tier) ch += 0.05f;
            if (ctx.bothBonded) ch += 0.03f;
            if ("rift_ember".equals(cat)) ch += 0.10f;
            if ("singularity_fragment".equals(cat)) ch += 0.15f;
            if (blA.contains("STARBORN") || blB.contains("STARBORN")) ch += 0.05f;
            ch += Math.min(0.20f, 0.02f * Math.max(0, ctx.pity));
            ch = Math.min(0.60f, ch);
            res.tierUpRolled = true; res.tierUpChance = ch;
            if (r.nextFloat() < ch) { c.tier++; res.tierUp = true; }
        }

        // stats
        boolean hybrid = res.hybrid != null;
        for (int i = 0; i < FalconGenome.STATS; i++) {
            for (int k = 0; k < 2; k++) {
                FalconGenome p = k == 0 ? a : b;
                int v = p.stats[i][r.nextInt(2)] + r.nextInt(7) - 3;
                if (hybrid) v += 5;
                if ("ancient_eggshell".equals(cat)) v += 5;
                if (i == FalconGenome.RIFT && "void_pearl".equals(cat)) v += 10;
                c.stats[i][k] = FalconGenome.clamp(v, 1, 100);
            }
        }

        // visible traits
        c.crack[0] = a.crack[r.nextInt(2)]; c.crack[1] = b.crack[r.nextInt(2)];
        c.pattern[0] = a.pattern[r.nextInt(2)]; c.pattern[1] = b.pattern[r.nextInt(2)];
        c.size[0] = a.size[r.nextInt(2)]; c.size[1] = b.size[r.nextInt(2)];
        c.glow[0] = a.glow[r.nextInt(2)]; c.glow[1] = b.glow[r.nextInt(2)];
        mutateCrack(c, ctx.snowy, 0.15f, "WHITE", r, res);
        if (ctx.nether) mutateCrack(c, true, 0.15f, r.nextBoolean() ? "CRIMSON" : "AMBER", r, res);
        mutateCrack(c, ctx.end, 0.15f, "VIOLET", r, res);
        mutateCrack(c, ctx.voidBiome, 0.12f, "VOID", r, res);
        mutateCrack(c, ctx.aurora, 0.15f, "AURORA", r, res);
        mutateCrack(c, "aurora_plume".equals(cat), 0.30f, "AURORA", r, res);
        mutateCrack(c, "void_pearl".equals(cat), 0.35f, "VOID", r, res);
        mutateCrack(c, "prism_shard".equals(cat), 0.25f, "PRISMATIC", r, res);
        mutateCrack(c, "ember_heart".equals(cat), 0.30f, "AMBER", r, res);
        if (ctx.snowy && r.nextFloat() < 0.10f) { c.size[r.nextInt(2)] = 1; res.mutations.add("size:L"); }
        if ("ancient_eggshell".equals(cat) && r.nextFloat() < 0.20f) { c.size[r.nextInt(2)] = 2; res.mutations.add("size:X"); }
        mutatePattern(c, ctx.rift, 0.08f, "FRACTAL", r, res);
        mutatePattern(c, ctx.rift && ctx.riftLayer >= 3, 0.06f, "STARFIELD", r, res);
        mutatePattern(c, "prism_shard".equals(cat), 0.15f, "FRACTAL", r, res);
        if (blA.contains("STARBORN") || blB.contains("STARBORN")) mutatePattern(c, true, 0.05f, "STARFIELD", r, res);
        if (r.nextFloat() < 0.05f) { c.glow[r.nextInt(2)] = Math.min(3, Math.max(c.glow[0], c.glow[1]) + 1); res.mutations.add("glow+"); }
        if (ctx.nestMutation > 0 && r.nextFloat() < ctx.nestMutation) {   // nest-quality bonus: one extra rare visible mutation
            switch (r.nextInt(3)) {
                case 0 -> { c.glow[r.nextInt(2)] = Math.min(3, Math.max(c.glow[0], c.glow[1]) + 1); res.mutations.add("nest:glow+"); }
                case 1 -> mutatePattern(c, true, 1f, r.nextBoolean() ? "FRACTAL" : "STARFIELD", r, res);
                default -> mutateCrack(c, true, 1f, r.nextBoolean() ? "PRISMATIC" : "VOID", r, res);
            }
        }

        // pedigree (needed before abilities so bloodline rules can apply to the child's line)
        c.parents.add(a.asAncestor()); c.parents.add(b.asAncestor());
        c.grandparents.addAll(a.parents); c.grandparents.addAll(b.parents);
        while (c.grandparents.size() > 4) c.grandparents.remove(c.grandparents.size() - 1);
        c.generation = Math.max(a.generation, b.generation) + 1;

        // abilities
        Set<String> pool = new LinkedHashSet<>();
        pool.addAll(a.abilities); pool.addAll(a.dormant); pool.addAll(b.abilities); pool.addAll(b.dormant);
        Set<String> inA = new HashSet<>(a.abilities); inA.addAll(a.dormant);
        Set<String> inB = new HashSet<>(b.abilities); inB.addAll(b.dormant);
        Set<String> forced = new HashSet<>();
        Set<String> bl = new HashSet<>(blA); bl.addAll(blB);
        if (bl.contains("STORMCROWN")) forced.addAll(List.of("storm_call", "gale_burst", "thunder_dive", "tempest_eye"));
        if (bl.contains("ASHBORN")) forced.addAll(List.of("ember_strike", "heat_shield", "thermal_shock"));
        if (bl.contains("FROSTWING")) forced.addAll(List.of("frost_screech", "thermal_shock"));
        List<String> passed = new ArrayList<>();
        for (String s : pool) {
            float ch = inA.contains(s) && inB.contains(s) ? 0.60f : 0.35f;
            if (forced.contains(s) || r.nextFloat() < ch) passed.add(s);
        }
        float fusionBonus = "entanglement_thread".equals(cat) ? 0.20f : 0f;
        for (var f : BreedingData.FUSIONS) {
            if (!pool.contains(f.a()) || !pool.contains(f.b())) continue;
            if (c.tier < abilityDef(f.result()).minTier()) continue;
            if (r.nextFloat() < 0.25f + fusionBonus) {
                passed.remove(f.a()); passed.remove(f.b());
                if (!passed.contains(f.result())) passed.add(0, f.result());
                res.fusion = f.result();
                break;
            }
        }
        if ("storm_feather".equals(cat)) offer(passed, r.nextBoolean() ? "storm_call" : "gale_burst", c.tier, r);
        if ("ember_heart".equals(cat)) offer(passed, r.nextBoolean() ? "ember_strike" : "heat_shield", c.tier, r);
        if (bl.contains("VERDANT") && !passed.contains("regen_down")) passed.add("regen_down");
        fillAbilities(c, passed, r);
        float mut = 0.03f + Math.min(0.05f, (a.stat(FalconGenome.RIFT) + b.stat(FalconGenome.RIFT)) / 4000f);
        if (!c.abilities.isEmpty() && r.nextFloat() < mut) {
            List<String> eligible = eligible(c.tier, c.abilities);
            if (!eligible.isEmpty()) {
                int slot = r.nextInt(c.abilities.size());
                String n = eligible.get(r.nextInt(eligible.size()));
                res.mutations.add("ability:" + c.abilities.get(slot) + "->" + n);
                c.abilities.set(slot, n);
            }
        }
        res.child = c;
        return res;
    }

    /** Puts candidate abilities into active slots (tier permitting) and dormant genes; tops up empty slots. */
    static void fillAbilities(FalconGenome g, List<String> candidates, RandomSource r) {
        g.abilities.clear(); g.dormant.clear();
        List<String> ok = new ArrayList<>(), above = new ArrayList<>();
        for (String s : candidates) {
            var d = abilityDef(s); if (d == null || ok.contains(s) || above.contains(s)) continue;
            (d.minTier() <= g.tier ? ok : above).add(s);
        }
        // fusions and rarer abilities first, otherwise keep inheritance order
        ok.sort(Comparator.comparingInt((String s) -> -abilityDef(s).minTier()));
        for (String s : ok) { if (g.abilities.size() < g.slots()) g.abilities.add(s); else g.dormant.add(s); }
        g.dormant.addAll(above);
        var vd = variantDef(g.variant);
        if (vd != null) for (String s : vd.affinity()) {
            if (g.abilities.size() >= g.slots()) break;
            var d = abilityDef(s);
            if (d != null && d.minTier() <= g.tier && !g.abilities.contains(s)) { g.abilities.add(s); g.dormant.remove(s); }
        }
        while (g.abilities.size() < g.slots()) {
            List<String> e = eligible(g.tier, g.abilities);
            if (e.isEmpty()) break;
            String s = e.get(r.nextInt(e.size())); g.abilities.add(s); g.dormant.remove(s);
        }
        while (g.dormant.size() > 4) g.dormant.remove(g.dormant.size() - 1);
    }

    static List<String> eligible(int tier, Collection<String> exclude) {
        List<String> e = new ArrayList<>();
        for (var d : BreedingData.ABILITIES) if (!"FUSION".equals(d.category()) && d.minTier() <= tier && !exclude.contains(d.id())) e.add(d.id());
        return e;
    }

    private static void offer(List<String> passed, String ability, int tier, RandomSource r) {
        if (abilityDef(ability).minTier() <= tier && !passed.contains(ability)) passed.add(0, ability);
    }
    private static void mutateCrack(FalconGenome c, boolean cond, float ch, String id, RandomSource r, Result res) {
        if (cond && r.nextFloat() < ch) { c.crack[r.nextInt(2)] = crackIndex(id); res.mutations.add("crack:" + id); }
    }
    private static void mutatePattern(FalconGenome c, boolean cond, float ch, String id, RandomSource r, Result res) {
        if (cond && r.nextFloat() < ch) { c.pattern[r.nextInt(2)] = patternIndex(id); res.mutations.add("pattern:" + id); }
    }
    static boolean pair(String a, String b, String x, String y) { return a.equals(x) && b.equals(y) || a.equals(y) && b.equals(x); }
    public static boolean isLegendary(String v) { return v.equals("cosmic") || v.equals("singularity"); }

    /** Incubation time in ticks for a tier (before speed-ups). */
    public static int hatchTicks(int tier) { return BreedingData.TIERS.get(FalconGenome.clamp(tier, 1, 7) - 1).hatchMinutes() * 60 * 20; }
}
