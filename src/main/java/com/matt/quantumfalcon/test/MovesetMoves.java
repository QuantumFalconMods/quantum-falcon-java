package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity;
import com.matt.quantumfalcon.entity.boss.RiftWardenEntity;
import com.matt.quantumfalcon.entity.boss.EntangledTwinsEntity;
import com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType;

import java.util.List;

import static com.matt.quantumfalcon.test.MovesetTests.Hit.*;
import static com.matt.quantumfalcon.test.MovesetTests.Move;

/** The moves each boss's moveset test fires (one entry per telegraphed move). */
final class MovesetMoves {
    private MovesetMoves() {}

    static void init() {
        MovesetTests.moves(ArenaType.QUANTUM_GUARDIAN, List.of(
            new Move("quantum_beam", DAMAGE, b -> ((QuantumOverseerEntity) b).performAttack(QuantumOverseerEntity.ATTACK_BEAM)),
            new Move("swirl_burst", DAMAGE, b -> ((QuantumOverseerEntity) b).performAttack(QuantumOverseerEntity.ATTACK_SWIRL)),
            new Move("laser_sweep", DAMAGE, b -> ((QuantumOverseerEntity) b).performAttack(QuantumOverseerEntity.ATTACK_LASER_SWEEP)),
            new Move("summon_constructs", OTHER, b -> ((QuantumOverseerEntity) b).performAttack(QuantumOverseerEntity.ATTACK_SUMMON)),
            new Move("blinding_pulse", OTHER, b -> ((QuantumOverseerEntity) b).performAttack(QuantumOverseerEntity.ATTACK_BLINDNESS)),
            new Move("shard_volley", OTHER, b -> ((QuantumOverseerEntity) b).performShardVolley())));
        MovesetTests.moves(ArenaType.VOID_HORROR, List.of(
            new Move("shard_volley", OTHER, b -> ((RiftWardenEntity) b).performShardVolley()),
            new Move("crystal_storm", DAMAGE, b -> ((RiftWardenEntity) b).performCrystalStorm()),
            new Move("rift_collapse", DAMAGE, b -> ((RiftWardenEntity) b).performRiftCollapse())));
        MovesetTests.moves(ArenaType.ENTANGLED_TWINS, List.of(
            new Move("mirror_slash", DAMAGE, b -> ((EntangledTwinsEntity) b).performAttack(EntangledTwinsEntity.MOVE_MIRROR_SLASH)),
            new Move("tether_lash", DAMAGE, b -> ((EntangledTwinsEntity) b).performAttack(EntangledTwinsEntity.MOVE_TETHER_LASH)),
            new Move("resonance_pulse", DAMAGE, b -> ((EntangledTwinsEntity) b).performAttack(EntangledTwinsEntity.MOVE_RESONANCE_PULSE)),
            new Move("petal_barrage", DAMAGE, b -> ((EntangledTwinsEntity) b).performAttack(EntangledTwinsEntity.MOVE_PETAL_BARRAGE)),
            new Move("quantum_blossom", OTHER, b -> ((EntangledTwinsEntity) b).performAttack(EntangledTwinsEntity.MOVE_BLOSSOM)),
            new Move("swap", OTHER, b -> ((EntangledTwinsEntity) b).performAttack(EntangledTwinsEntity.MOVE_SWAP))));
        MovesetTests.moves(ArenaType.SINGULARITY_SOVEREIGN, List.of(
            new Move("gravity_pull", DAMAGE, b -> ((SingularitySovereignEntity) b).performAttack(SingularitySovereignEntity.ATTACK_GRAVITY_PULL)),
            new Move("reality_distort", DAMAGE, b -> ((SingularitySovereignEntity) b).performAttack(SingularitySovereignEntity.ATTACK_REALITY_DISTORT)),
            new Move("singularity_wave", DAMAGE, b -> ((SingularitySovereignEntity) b).performAttack(SingularitySovereignEntity.ATTACK_SINGULARITY_WAVE)),
            new Move("dimensional_spike", DAMAGE, b -> ((SingularitySovereignEntity) b).performAttack(SingularitySovereignEntity.ATTACK_DIMENSIONAL_SPIKE)),
            new Move("void_burst", DAMAGE, b -> ((SingularitySovereignEntity) b).performAttack(SingularitySovereignEntity.ATTACK_VOID_BURST)),
            new Move("crystal_shield", OTHER, b -> ((SingularitySovereignEntity) b).performAttack(SingularitySovereignEntity.ATTACK_CRYSTAL_SHIELD)),
            new Move("void_blossom", OTHER, b -> ((SingularitySovereignEntity) b).performAttack(SingularitySovereignEntity.ATTACK_BLOSSOM)),
            new Move("event_horizon", DAMAGE, b -> ((SingularitySovereignEntity) b).performAttack(SingularitySovereignEntity.ATTACK_EVENT_HORIZON))));
    }
}
