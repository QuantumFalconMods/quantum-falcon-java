package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.*;
import com.matt.quantumfalcon.entity.boss.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Entity Attribute Registration for Quantum Falcon Mod.
 * Registers default attributes for all custom entities including bosses.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class ModEntityAttributes {
    
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // Main companion
        event.put(ModEntities.QUANTUM_FALCON.get(), QuantumFalconEntity.createAttributes().build());
        
        // Ambient
        event.put(ModEntities.QUANTUM_WISP.get(), QuantumWispEntity.createAttributes().build());
        
        // Hostile mobs
        event.put(ModEntities.QUANTUM_PHANTOM.get(), QuantumPhantomEntity.createAttributes().build());
        event.put(ModEntities.CRYSTAL_GUARDIAN.get(), CrystalGuardianEntity.createAttributes().build());
        event.put(ModEntities.SKYWARDEN_IREL.get(), com.matt.quantumfalcon.entity.boss.SkywardenIrelEntity.createAttributes().build());
        event.put(ModEntities.VOID_ECHO.get(), VoidEchoEntity.createAttributes().build());
        event.put(ModEntities.DATA_CONSTRUCT.get(), DataConstructEntity.createAttributes().build());
        
        // ========================================
        // BOSSES
        // ========================================
        
        // Quantum Overseer - First boss (600 HP, 18 damage)
        event.put(ModEntities.QUANTUM_OVERSEER.get(), QuantumOverseerEntity.createAttributes().build());
        
        // Rift Warden - Second boss (500 HP, teleportation)
        event.put(ModEntities.RIFT_WARDEN.get(), RiftWardenEntity.createAttributes().build());
        event.put(ModEntities.GOBLIN_TRADER.get(), com.matt.quantumfalcon.entity.npc.QFTraderEntity.createAttributes(20, 0.5).build());
        event.put(ModEntities.QUANTUM_MERCHANT.get(), com.matt.quantumfalcon.entity.npc.QFTraderEntity.createAttributes(30, 0.5).build());
        event.put(ModEntities.RIFT_VILLAGER.get(), com.matt.quantumfalcon.entity.npc.QFTraderEntity.createAttributes(20, 0.5).build());
        event.put(ModEntities.QUANTUM_STALKER.get(), com.matt.quantumfalcon.entity.QuantumStalkerEntity.createAttributes().build());
        event.put(ModEntities.ECLIPSE_PHANTOM.get(), com.matt.quantumfalcon.entity.boss.EclipsePhantomEntity.createAttributes().build());
        event.put(ModEntities.VOID_LORD.get(), com.matt.quantumfalcon.entity.boss.VoidLordEntity.createAttributes().build());
        event.put(ModEntities.SHA_SHA.get(), com.matt.quantumfalcon.entity.boss.ShaShaEntity.createAttributes().build());
        event.put(ModEntities.RIFT_WARDEN_MINION.get(), com.matt.quantumfalcon.entity.boss.RiftWardenMinionEntity.createAttributes().build());
        
        // Entangled Twins - Third boss (350 HP each, damage sharing)
        event.put(ModEntities.ENTANGLED_TWINS.get(), EntangledTwinsEntity.createAttributes().build());
        
        // Singularity Sovereign - Final boss (1000 HP, gravity pull)
        event.put(ModEntities.SINGULARITY_SOVEREIGN.get(), SingularitySovereignEntity.createAttributes().build());
        
        QuantumFalcon.LOGGER.info("⚡ Registered attributes for {} entities (4 bosses)", 10);
    }
    
    public static void register() {
        // Attributes registered via event subscriber
        QuantumFalcon.LOGGER.info("⚡ Entity attribute registration initialized");
    }

    /** Natural spawning rules (the biome list lives in data/quantumfalcon/neoforge/biome_modifier). */
    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.QUANTUM_FALCON.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, QuantumFalconEntity::checkFalconSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        for (var t : java.util.List.of(ModEntities.CRYSTAL_GUARDIAN.get(), ModEntities.DATA_CONSTRUCT.get(),
                ModEntities.VOID_ECHO.get(), ModEntities.QUANTUM_PHANTOM.get(), ModEntities.QUANTUM_STALKER.get())) {
            event.register(t, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
        event.register(ModEntities.SHA_SHA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.matt.quantumfalcon.entity.boss.ShaShaEntity::checkShaShaSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Traders: only one of each kind within 96 blocks.
        event.register(ModEntities.GOBLIN_TRADER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> net.minecraft.world.entity.Mob.checkMobSpawnRules(t, l, r, p, rnd) && unique(t, l, p, 96), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.QUANTUM_MERCHANT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> net.minecraft.world.entity.Mob.checkMobSpawnRules(t, l, r, p, rnd) && unique(t, l, p, 96), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Bosses: post-game only. They join the spawn cycle near a player who has beaten the Singularity Sovereign
        // (the last arena gate), with monster rules, a 2% roll on top of the tiny biome weight, never two within 128 blocks.
        event.register(ModEntities.ECLIPSE_PHANTOM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> rnd.nextFloat() < 0.02f && postGameBossGate(l, p) && net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(t, l, r, p, rnd) && unique(t, l, p, 128), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.VOID_LORD.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> rnd.nextFloat() < 0.02f && postGameBossGate(l, p) && net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(t, l, r, p, rnd) && unique(t, l, p, 128), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.QUANTUM_WISP.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static boolean unique(net.minecraft.world.entity.EntityType<?> type, net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.core.BlockPos pos, int radius) {
        return level.getEntities(type, new net.minecraft.world.phys.AABB(pos).inflate(radius), e -> true).isEmpty();
    }

    /** Void Lord / Eclipse Phantom may only spawn naturally within 64 blocks of a player who has beaten the Sovereign. */
    public static boolean postGameBossGate(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.core.BlockPos pos) {
        return postGameBossGate(level.getLevel().players(), pos);
    }

    public static boolean postGameBossGate(Iterable<? extends net.minecraft.world.entity.player.Player> players, net.minecraft.core.BlockPos pos) {
        for (var pl : players) {
            if (pl.isSpectator() || pl.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 64 * 64) continue;
            if (com.matt.quantumfalcon.story.PlayerStoryData.get(pl).hasDefeatedBoss("SINGULARITY_SOVEREIGN")) return true;
        }
        return false;
    }
}
