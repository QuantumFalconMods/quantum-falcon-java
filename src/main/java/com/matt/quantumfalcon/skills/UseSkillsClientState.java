package com.matt.quantumfalcon.skills;

/** Client copy of the local player's use-skill XP (set by UseSkillsPayload). Plain data, safe on both sides. */
public final class UseSkillsClientState {
    private UseSkillsClientState() {}
    private static final long[] XP = new long[UseSkill.values().length];
    public static long xp(UseSkill s) { return XP[s.ordinal()]; }
    public static void set(java.util.List<Long> xs) { for (int i = 0; i < XP.length && i < xs.size(); i++) XP[i] = xs.get(i); }
}
