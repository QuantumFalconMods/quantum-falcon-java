package com.matt.quantumfalcon.entity.boss;

import net.minecraft.server.level.ServerBossEvent;

/** Implemented by every Quantum Falcon boss so {@link BossDirector} can drive intros, phase visuals, bar colours and music. */
public interface QFBoss {
    /** Current fight phase, starting at 1. */
    int qfPhase();
    /** The boss bar, or null if this entity doesn't own one (the magenta twin). */
    ServerBossEvent qfBossBar();
}
