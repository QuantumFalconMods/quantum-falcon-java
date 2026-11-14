package com.matt.quantumfalcon.loot.sig;

import com.matt.quantumfalcon.skill.PlayerSkillData;
import com.matt.quantumfalcon.skill.SkillTreeSystem;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * A8: loot archetypes are the five skill-tree branches (like WoW specs): Hunt (melee/ranged damage), Bond (falcon gear),
 * Lineage (breeding), Rift (magic, instability), Wayfinder (exploration, mobility). A player's archetype weights follow the
 * skill points they have spent in each branch.
 */
public enum Archetype {
    HUNT("Hunt", SkillTreeSystem.Category.HUNT),
    BOND("Bond", SkillTreeSystem.Category.BOND),
    LINEAGE("Lineage", SkillTreeSystem.Category.LINEAGE),
    RIFT("Rift", SkillTreeSystem.Category.RIFT),
    WAYFINDER("Wayfinder", SkillTreeSystem.Category.WAYFINDER);

    public final String label;
    public final SkillTreeSystem.Category branch;
    Archetype(String label, SkillTreeSystem.Category branch) { this.label = label; this.branch = branch; }

    /** How much more likely an archetype the player has fully invested in is than one with no points. */
    public static final float BIAS = 3.0f;

    /** Points spent per branch. */
    public static Map<Archetype, Integer> invested(Player p) {
        Map<Archetype, Integer> m = new EnumMap<>(Archetype.class);
        for (Archetype a : values()) m.put(a, 0);
        for (String id : PlayerSkillData.get(p).getUnlockedSkills()) {
            var s = SkillTreeSystem.getSkill(id);
            if (s == null) continue;
            for (Archetype a : values()) if (a.branch == s.getCategory()) m.merge(a, s.getCost(), Integer::sum);
        }
        return m;
    }

    /** Weight per archetype: 1 + BIAS x (share of spent points). Nothing spent: all equal. Every archetype stays >= 1. */
    public static Map<Archetype, Float> weights(Map<Archetype, Integer> invested) {
        int total = 0;
        for (int v : invested.values()) total += v;
        Map<Archetype, Float> w = new EnumMap<>(Archetype.class);
        for (Archetype a : values()) w.put(a, 1f + (total == 0 ? 0f : BIAS * invested.getOrDefault(a, 0) / (float) total));
        return w;
    }
}
