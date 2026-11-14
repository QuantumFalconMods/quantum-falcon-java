package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.*;
import com.matt.quantumfalcon.entity.boss.*;
import com.matt.quantumfalcon.entity.projectile.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Entity Registration for Quantum Falcon Mod.
 * NeoForge 1.21.1 - All entities enabled.
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
        DeferredRegister.create(Registries.ENTITY_TYPE, QuantumFalcon.MOD_ID);

    // ========================================
    // MAIN COMPANION
    // ========================================
    public static final DeferredHolder<EntityType<?>, EntityType<QuantumFalconEntity>> QUANTUM_FALCON = 
        ENTITIES.register("quantum_falcon", () -> EntityType.Builder
            .of(QuantumFalconEntity::new, MobCategory.CREATURE)
            .sized(0.8f, 0.8f)
            .clientTrackingRange(10)
            .build(QuantumFalcon.MOD_ID + ":quantum_falcon"));

    // ========================================
    // AMBIENT
    // ========================================
    public static final DeferredHolder<EntityType<?>, EntityType<QuantumWispEntity>> QUANTUM_WISP = 
        ENTITIES.register("quantum_wisp", () -> EntityType.Builder
            .of(QuantumWispEntity::new, MobCategory.AMBIENT)
            .sized(0.3f, 0.3f)
            .clientTrackingRange(8)
            .build(QuantumFalcon.MOD_ID + ":quantum_wisp"));

    // ========================================
    // HOSTILE MOBS
    // ========================================
    public static final DeferredHolder<EntityType<?>, EntityType<QuantumPhantomEntity>> QUANTUM_PHANTOM = 
        ENTITIES.register("quantum_phantom", () -> EntityType.Builder
            .of(QuantumPhantomEntity::new, MobCategory.MONSTER)
            .sized(0.9f, 1.9f)
            .clientTrackingRange(10)
            .build(QuantumFalcon.MOD_ID + ":quantum_phantom"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrystalGuardianEntity>> CRYSTAL_GUARDIAN = 
        ENTITIES.register("crystal_guardian", () -> EntityType.Builder
            .of(CrystalGuardianEntity::new, MobCategory.MONSTER)
            .sized(1.2f, 2.5f)
            .clientTrackingRange(10)
            .build(QuantumFalcon.MOD_ID + ":crystal_guardian"));

    /** A10 Frozen Hours: the flightmaster of Eastwatch Aerie (a Dungeon Guard boss, never a natural spawn). */
    public static final DeferredHolder<EntityType<?>, EntityType<com.matt.quantumfalcon.entity.boss.SkywardenIrelEntity>> SKYWARDEN_IREL =
        ENTITIES.register("skywarden_irel", () -> EntityType.Builder
            .of(com.matt.quantumfalcon.entity.boss.SkywardenIrelEntity::new, MobCategory.MONSTER)
            .sized(0.65f, 2.1f)
            .clientTrackingRange(10)
            .build(QuantumFalcon.MOD_ID + ":skywarden_irel"));

    public static final DeferredHolder<EntityType<?>, EntityType<VoidEchoEntity>> VOID_ECHO = 
        ENTITIES.register("void_echo", () -> EntityType.Builder
            .of(VoidEchoEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(10)
            .build(QuantumFalcon.MOD_ID + ":void_echo"));

    public static final DeferredHolder<EntityType<?>, EntityType<DataConstructEntity>> DATA_CONSTRUCT = 
        ENTITIES.register("data_construct", () -> EntityType.Builder
            .of(DataConstructEntity::new, MobCategory.MONSTER)
            .sized(0.8f, 1.4f)
            .clientTrackingRange(10)
            .build(QuantumFalcon.MOD_ID + ":data_construct"));

    // ========================================
    // BOSSES
    // ========================================
    public static final DeferredHolder<EntityType<?>, EntityType<QuantumOverseerEntity>> QUANTUM_OVERSEER = 
        ENTITIES.register("quantum_overseer", () -> EntityType.Builder
            .of(QuantumOverseerEntity::new, MobCategory.MONSTER)
            .sized(2.0f, 3.5f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(QuantumFalcon.MOD_ID + ":quantum_overseer"));

    public static final DeferredHolder<EntityType<?>, EntityType<RiftWardenEntity>> RIFT_WARDEN = 
        ENTITIES.register("rift_warden", () -> EntityType.Builder
            .of(RiftWardenEntity::new, MobCategory.MONSTER)
            .sized(1.8f, 3.0f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(QuantumFalcon.MOD_ID + ":rift_warden"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.matt.quantumfalcon.entity.boss.ShaShaEntity>> SHA_SHA =
        ENTITIES.register("sha_sha", () -> EntityType.Builder
            .of(com.matt.quantumfalcon.entity.boss.ShaShaEntity::new, MobCategory.MONSTER)
            .sized(0.8f, 2.4f)
            .clientTrackingRange(12)
            .build(QuantumFalcon.MOD_ID + ":sha_sha"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.matt.quantumfalcon.entity.boss.RiftWardenMinionEntity>> RIFT_WARDEN_MINION =
        ENTITIES.register("rift_warden_minion", () -> EntityType.Builder
            .of(com.matt.quantumfalcon.entity.boss.RiftWardenMinionEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(10)
            .build(QuantumFalcon.MOD_ID + ":rift_warden_minion"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntangledTwinsEntity>> ENTANGLED_TWINS = 
        ENTITIES.register("entangled_twins", () -> EntityType.Builder
            .of(EntangledTwinsEntity::new, MobCategory.MONSTER)
            .sized(2.0f, 6.8f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(QuantumFalcon.MOD_ID + ":entangled_twins"));

    public static final DeferredHolder<EntityType<?>, EntityType<SingularitySovereignEntity>> SINGULARITY_SOVEREIGN = 
        ENTITIES.register("singularity_sovereign", () -> EntityType.Builder
            .of(SingularitySovereignEntity::new, MobCategory.MONSTER)
            .sized(2.5f, 4.0f)
            .clientTrackingRange(20)
            .fireImmune()
            .build(QuantumFalcon.MOD_ID + ":singularity_sovereign"));

    // ========================================
    // PROJECTILES
    // ========================================
    public static final DeferredHolder<EntityType<?>, EntityType<VoidBlastEntity>> VOID_BLAST = 
        ENTITIES.register("void_blast", () -> EntityType.Builder
            .<VoidBlastEntity>of(VoidBlastEntity::new, MobCategory.MISC)
            .sized(0.5f, 0.5f)
            .clientTrackingRange(8)
            .updateInterval(10)
            .build(QuantumFalcon.MOD_ID + ":void_blast"));

    public static final DeferredHolder<EntityType<?>, EntityType<BlackHoleEntity>> BLACK_HOLE = 
        ENTITIES.register("black_hole", () -> EntityType.Builder
            .<BlackHoleEntity>of(BlackHoleEntity::new, MobCategory.MISC)
            .sized(1.0f, 1.0f)
            .clientTrackingRange(12)
            .updateInterval(5)
            .build(QuantumFalcon.MOD_ID + ":black_hole"));

    public static final DeferredHolder<EntityType<?>, EntityType<RealityRiftEntity>> REALITY_RIFT = 
        ENTITIES.register("reality_rift", () -> EntityType.Builder
            .<RealityRiftEntity>of(RealityRiftEntity::new, MobCategory.MISC)
            .sized(0.8f, 0.8f)
            .clientTrackingRange(10)
            .updateInterval(10)
            .build(QuantumFalcon.MOD_ID + ":reality_rift"));

    public static final DeferredHolder<EntityType<?>, EntityType<QuantumShardEntity>> QUANTUM_SHARD =
        ENTITIES.register("quantum_shard", () -> EntityType.Builder
            .<QuantumShardEntity>of(QuantumShardEntity::new, MobCategory.MISC)
            .sized(0.35f, 0.35f)
            .clientTrackingRange(8)
            .updateInterval(2)
            .build(QuantumFalcon.MOD_ID + ":quantum_shard"));

    /**
     * Register all entities to the event bus
     */
    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
        QuantumFalcon.LOGGER.info("⚡ Registered {} entity types", ENTITIES.getEntries().size());
    }

    // ── Stage 3 army: traders, stalker, Eclipse Phantom, Void Lord ──
    public static final DeferredHolder<EntityType<?>, EntityType<com.matt.quantumfalcon.entity.npc.GoblinTraderEntity>> GOBLIN_TRADER =
        ENTITIES.register("goblin_trader", () -> EntityType.Builder
            .of(com.matt.quantumfalcon.entity.npc.GoblinTraderEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.3f).clientTrackingRange(10).build(QuantumFalcon.MOD_ID + ":goblin_trader"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.matt.quantumfalcon.entity.npc.QuantumMerchantEntity>> QUANTUM_MERCHANT =
        ENTITIES.register("quantum_merchant", () -> EntityType.Builder
            .of(com.matt.quantumfalcon.entity.npc.QuantumMerchantEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.95f).clientTrackingRange(10).build(QuantumFalcon.MOD_ID + ":quantum_merchant"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.matt.quantumfalcon.entity.npc.RiftVillagerEntity>> RIFT_VILLAGER =
        ENTITIES.register("rift_villager", () -> EntityType.Builder
            .of(com.matt.quantumfalcon.entity.npc.RiftVillagerEntity::new, MobCategory.MISC)
            .sized(0.6f, 1.95f).clientTrackingRange(10).build(QuantumFalcon.MOD_ID + ":rift_villager"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.matt.quantumfalcon.entity.QuantumStalkerEntity>> QUANTUM_STALKER =
        ENTITIES.register("quantum_stalker", () -> EntityType.Builder
            .of(com.matt.quantumfalcon.entity.QuantumStalkerEntity::new, MobCategory.MONSTER)
            .sized(0.7f, 2.4f).clientTrackingRange(10).build(QuantumFalcon.MOD_ID + ":quantum_stalker"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.matt.quantumfalcon.entity.boss.EclipsePhantomEntity>> ECLIPSE_PHANTOM =
        ENTITIES.register("eclipse_phantom", () -> EntityType.Builder
            .of(com.matt.quantumfalcon.entity.boss.EclipsePhantomEntity::new, MobCategory.MONSTER)
            .sized(2.2f, 2.6f).clientTrackingRange(16).fireImmune().build(QuantumFalcon.MOD_ID + ":eclipse_phantom"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.matt.quantumfalcon.entity.boss.VoidLordEntity>> VOID_LORD =
        ENTITIES.register("void_lord", () -> EntityType.Builder
            .of(com.matt.quantumfalcon.entity.boss.VoidLordEntity::new, MobCategory.MONSTER)
            .sized(1.4f, 3.6f).clientTrackingRange(16).fireImmune().build(QuantumFalcon.MOD_ID + ":void_lord"));
}
