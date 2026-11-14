package com.matt.quantumfalcon.breeding;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A falcon's full breeding genome: variant, rarity tier, five two-allele stats, visible Mendelian traits (crack colour,
 * pattern, size, glow), active and dormant abilities, and a two-generation pedigree. Immutable apart from the builder-style
 * fields used by {@link BreedingEngine}; serialised to NBT on falcons, eggs and egg items.
 */
public final class FalconGenome {
    public static final int STATS = 5, SPEED = 0, STRENGTH = 1, HUNT = 2, LOYALTY = 3, RIFT = 4;

    public record Ancestor(String variant, int tier, String name) {
        CompoundTag save() { CompoundTag t = new CompoundTag(); t.putString("v", variant); t.putInt("t", tier); t.putString("n", name); return t; }
        static Ancestor load(CompoundTag t) { return new Ancestor(t.getString("v"), Math.max(1, t.getInt("t")), t.getString("n")); }
    }

    public String variant = "peregrine";
    public int tier = 1;
    public final int[][] stats = new int[STATS][2];
    public final int[] crack = new int[2];     // index into BreedingData.CRACKS
    public final int[] pattern = new int[2];   // index into BreedingData.PATTERNS
    public final int[] size = new int[2];      // allele values -1..2
    public final int[] glow = new int[2];      // 0..3
    public final List<String> abilities = new ArrayList<>();
    public final List<String> dormant = new ArrayList<>();
    public int generation = 1;
    public final List<Ancestor> parents = new ArrayList<>();       // 0 or 2
    public final List<Ancestor> grandparents = new ArrayList<>();  // 0..4
    public String name = "";

    // ── expressed values ──────────────────────────────────────────────────────────────────────────────────────
    public BreedingData.TierDef tierDef() { return BreedingData.TIERS.get(Math.max(1, Math.min(7, tier)) - 1); }
    public int statCap() { return tierDef().statCap(); }
    public int slots() { return tierDef().slots(); }

    /** Expressed stat 0..100: allele average capped by tier, plus bloodline bonuses (bonuses may exceed the cap by up to 10). */
    public int stat(int i) {
        int base = Math.min(statCap(), (stats[i][0] + stats[i][1]) / 2);
        Set<String> bl = bloodlines();
        int bonus = 0;
        if (bl.contains("PUREBLOOD")) bonus += 5;
        if (i == SPEED && bl.contains("STORMCROWN")) bonus += 10;
        if (i == RIFT && bl.contains("VOIDBLOOD")) bonus += 10;
        if (i == LOYALTY && bl.contains("FROSTWING")) bonus += 10;
        return Math.max(1, Math.min(100, Math.min(statCap() + 10, base + bonus)));
    }
    public float statFraction(int i) { return stat(i) / 100f; }

    public int expressedCrack() {
        boolean voidDominant = bloodlines().contains("VOIDBLOOD");
        int voidIdx = BreedingEngine.crackIndex("VOID");
        if (voidDominant && (crack[0] == voidIdx || crack[1] == voidIdx)) return voidIdx;
        return Math.min(crack[0], crack[1]);
    }
    public BreedingData.CrackDef crackDef() { return BreedingData.CRACKS.get(expressedCrack()); }
    public int expressedPattern() { return Math.min(pattern[0], pattern[1]); }
    public BreedingData.PatternDef patternDef() { return BreedingData.PATTERNS.get(expressedPattern()); }
    public int sizeIndex() { return Math.max(0, Math.min(5, size[0] + size[1] + 2)); }
    public float sizeScale() { return BreedingData.SIZE_SCALES[sizeIndex()]; }
    public String sizeName() { return BreedingData.SIZE_NAMES[sizeIndex()]; }
    public int glowLevel() { return (glow[0] + glow[1] + 1) / 2; }
    public boolean has(String ability) { return abilities.contains(ability); }
    public boolean isHybrid() { BreedingData.VariantDef d = BreedingEngine.variantDef(variant); return d != null && d.parentA() != null; }

    /** Bloodlines from the six ancestors (2 parents + 4 grandparents) and the generation counter. */
    public Set<String> bloodlines() {
        Set<String> out = new LinkedHashSet<>();
        List<Ancestor> all = new ArrayList<>(parents); all.addAll(grandparents);
        if (parents.size() == 2 && grandparents.size() == 4 && all.stream().allMatch(a -> a.variant().equals(variant))) out.add("PUREBLOOD");
        if (count(all, "stormrider", "tempest", "tidecaller") >= 3) out.add("STORMCROWN");
        if (count(all, "soulfire", "crimson", "emberfrost", "quantum_phoenix") >= 3) out.add("ASHBORN");
        if (count(all, "voidstalker", "abyssal", "sporewraith", "singularity") >= 3) out.add("VOIDBLOOD");
        if (count(all, "tundra", "aurora_seraph", "emberfrost") >= 3) out.add("FROSTWING");
        if (count(all, "prairie", "mushroom", "coral") >= 3) out.add("VERDANT");
        if (count(all, "cosmic", "nebula_prism", "entangled_prism", "paradox") >= 3) out.add("STARBORN");
        if (generation >= 10) out.add("ELDER");
        return out;
    }
    private static int count(List<Ancestor> all, String... vs) {
        int n = 0; for (Ancestor a : all) for (String v : vs) if (a.variant().equals(v)) { n++; break; } return n;
    }

    public Ancestor asAncestor() { return new Ancestor(variant, tier, name); }

    // ── NBT ───────────────────────────────────────────────────────────────────────────────────────────────────
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("variant", variant); t.putInt("tier", tier); t.putInt("gen", generation); t.putString("name", name);
        int[] flat = new int[STATS * 2];
        for (int i = 0; i < STATS; i++) { flat[2 * i] = stats[i][0]; flat[2 * i + 1] = stats[i][1]; }
        t.putIntArray("stats", flat);
        t.putIntArray("crack", crack.clone()); t.putIntArray("pattern", pattern.clone());
        t.putIntArray("size", size.clone()); t.putIntArray("glow", glow.clone());
        t.put("abilities", strList(abilities)); t.put("dormant", strList(dormant));
        ListTag p = new ListTag(); for (Ancestor a : parents) p.add(a.save()); t.put("parents", p);
        ListTag g = new ListTag(); for (Ancestor a : grandparents) g.add(a.save()); t.put("grandparents", g);
        return t;
    }

    public static FalconGenome load(CompoundTag t) {
        FalconGenome g = new FalconGenome();
        if (t == null || !t.contains("variant")) return null;
        g.variant = t.getString("variant"); g.tier = Math.max(1, Math.min(7, t.getInt("tier")));
        g.generation = Math.max(1, t.getInt("gen")); g.name = t.getString("name");
        int[] flat = t.getIntArray("stats");
        for (int i = 0; i < STATS; i++) for (int k = 0; k < 2; k++) g.stats[i][k] = flat.length > 2 * i + k ? clamp(flat[2 * i + k], 1, 100) : 10;
        copy(t.getIntArray("crack"), g.crack, 0, BreedingData.CRACKS.size() - 1);
        copy(t.getIntArray("pattern"), g.pattern, 0, BreedingData.PATTERNS.size() - 1);
        copy(t.getIntArray("size"), g.size, -1, 2);
        copy(t.getIntArray("glow"), g.glow, 0, 3);
        for (Tag s : t.getList("abilities", Tag.TAG_STRING)) if (BreedingEngine.abilityDef(s.getAsString()) != null) g.abilities.add(s.getAsString());
        for (Tag s : t.getList("dormant", Tag.TAG_STRING)) if (BreedingEngine.abilityDef(s.getAsString()) != null) g.dormant.add(s.getAsString());
        for (Tag a : t.getList("parents", Tag.TAG_COMPOUND)) g.parents.add(Ancestor.load((CompoundTag) a));
        for (Tag a : t.getList("grandparents", Tag.TAG_COMPOUND)) g.grandparents.add(Ancestor.load((CompoundTag) a));
        return g;
    }

    public FalconGenome copy() { return load(save()); }

    private static ListTag strList(List<String> l) { ListTag t = new ListTag(); for (String s : l) t.add(StringTag.valueOf(s)); return t; }
    private static void copy(int[] src, int[] dst, int lo, int hi) { for (int i = 0; i < dst.length; i++) dst[i] = src.length > i ? clamp(src[i], lo, hi) : Math.max(lo, 0); }
    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(variant).append(" T").append(tier).append(" gen").append(generation).append(" stats[");
        for (int i = 0; i < STATS; i++) sb.append(i == 0 ? "" : ",").append(stat(i));
        sb.append("] crack=").append(crackDef().id()).append(" pattern=").append(patternDef().id()).append(" size=").append(sizeName())
          .append(" abilities=").append(abilities).append(" dormant=").append(dormant).append(" bloodlines=").append(bloodlines());
        return sb.toString();
    }
}
