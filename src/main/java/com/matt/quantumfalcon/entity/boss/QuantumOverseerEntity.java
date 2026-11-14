package com.matt.quantumfalcon.entity.boss;

import com.matt.quantumfalcon.entity.ai.BossNavigation;
import com.matt.quantumfalcon.entity.ai.overseer.*;
import com.matt.quantumfalcon.dimension.InstabilityManager;
import com.matt.quantumfalcon.registry.ModDataComponents;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Quantum Overseer - First major boss (Echo Depths).
 * Adapted from BoMD Gauntlet patterns: weighted attack selection, HP-threshold
 * ability unlocks, move history tracking, damage memory target switching.
 *
 * Attacks: Quantum Beam, Swirl Burst, Laser Sweep, Minion Summon, Blindness Pulse
 * Phases: 3 (66%, 33% HP)
 */
public class QuantumOverseerEntity extends Monster implements GeoEntity, QFBoss {
    // bosses never ride (minecarts hauled them out of their arenas) and never take portals
    @Override protected boolean canRide(net.minecraft.world.entity.Entity vehicle) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public int qfPhase() { return getPhase(); }
    @Override public net.minecraft.server.level.ServerBossEvent qfBossBar() { return bossEvent; }

    private static final EntityDataAccessor<Integer> DATA_PHASE =
        SynchedEntityData.defineId(QuantumOverseerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING =
        SynchedEntityData.defineId(QuantumOverseerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_ATTACK =
        SynchedEntityData.defineId(QuantumOverseerEntity.class, EntityDataSerializers.BYTE);
    /** Core weak point: 4 seconds after special/phase change; 4× damage with quantum crystal weapon + genetics egg drop. */
    private static final EntityDataAccessor<Boolean> DATA_CORE_WEAK_POINT =
        SynchedEntityData.defineId(QuantumOverseerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_WEAK_POINT_TICKS =
        SynchedEntityData.defineId(QuantumOverseerEntity.class, EntityDataSerializers.INT);
    /** Ticks remaining to play phase_change animation (synced for client). */
    private static final EntityDataAccessor<Integer> DATA_PHASE_CHANGE_TICKS =
        SynchedEntityData.defineId(QuantumOverseerEntity.class, EntityDataSerializers.INT);
    /** Ticks remaining to play special_attack animation (synced for client). */
    private static final EntityDataAccessor<Integer> DATA_SPECIAL_ATTACK_TICKS =
        SynchedEntityData.defineId(QuantumOverseerEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
        Component.translatable("entity.quantumfalcon.quantum_overseer"),
        BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10
    );

    // Attack IDs (BoMD-style byte status for animation sync)
    public static final byte ATTACK_NONE = 0;
    public static final byte ATTACK_BEAM = 4;
    public static final byte ATTACK_SWIRL = 5;
    public static final byte ATTACK_LASER_SWEEP = 6;
    public static final byte ATTACK_SUMMON = 7;
    public static final byte ATTACK_BLINDNESS = 8;
    public static final byte ATTACK_SHARD_VOLLEY = 9;

    // HP thresholds for unlocking attacks (Gauntlet pattern)
    private static final float LASER_SWEEP_THRESHOLD = 0.85f;
    private static final float SWIRL_THRESHOLD = 0.70f;
    private static final float BLINDNESS_THRESHOLD = 0.50f;

    private int attackCooldown = 0;
    private int attackAnimTicks = 0; // ticks to show attack animation before clearing DATA_ATTACK
    private int chargeTime = 0;
    private byte currentAttack = ATTACK_NONE;
    private boolean isAggroed = false;

    // BoMD-style move history to prevent repetition
    private final Deque<Byte> moveHistory = new ArrayDeque<>(4);

    // Damage memory for target switching (BoMD pattern)
    private final Map<UUID, Float> damageMemory = new LinkedHashMap<>();
    private static final int DAMAGE_MEMORY_SIZE = 5;

    public QuantumOverseerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.setPersistenceRequired();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        BossNavigation nav = new BossNavigation(this, level);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PhaseChangeGoal(this));
        this.goalSelector.addGoal(2, new BlinkGoal(this));
        this.goalSelector.addGoal(3, new ShardVolleyGoal(this));
        this.goalSelector.addGoal(4, new OverseerSpecialAttackGoal(this));
        this.goalSelector.addGoal(5, new OverseerAttackGoal(this));
        this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 600.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 18.0)
            .add(Attributes.ARMOR, 12.0)
            .add(Attributes.ARMOR_TOUGHNESS, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_CHARGING, false);
        builder.define(DATA_ATTACK, ATTACK_NONE);
        builder.define(DATA_CORE_WEAK_POINT, false);
        builder.define(DATA_WEAK_POINT_TICKS, 0);
        builder.define(DATA_PHASE_CHANGE_TICKS, 0);
        builder.define(DATA_SPECIAL_ATTACK_TICKS, 0);
    }

    public int getPhase() { return this.entityData.get(DATA_PHASE); }
    public void setPhase(int phase) { this.entityData.set(DATA_PHASE, phase); }
    public boolean isCharging() { return this.entityData.get(DATA_CHARGING); }
    public void setCharging(boolean c) { this.entityData.set(DATA_CHARGING, c); }
    public byte getCurrentAttack() { return this.entityData.get(DATA_ATTACK); }
    public int getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(int ticks) { this.attackCooldown = ticks; }
    public void setAttackAnimTicks(int ticks) { this.attackAnimTicks = ticks; }
    public void setDataAttack(byte attack) { this.entityData.set(DATA_ATTACK, attack); }
    public boolean isAggroed() { return isAggroed; }

    // ========================================
    // PHASE MANAGEMENT (BoMD StagedDamageHandler pattern)
    // ========================================

    private void checkPhaseTransition() {
        if (isDeadOrDying()) return;   // a killing blow crosses every threshold; don't announce a phase after the defeat line
        float hp = this.getHealth() / this.getMaxHealth();
        if (hp <= 0.33f && getPhase() < 3) {
            onPhaseChange(3);
        } else if (hp <= 0.66f && getPhase() < 2) {
            onPhaseChange(2);
        }
    }

    /** Called by PhaseChangeGoal when HP threshold is crossed. */
    public void onPhaseChangeTo(int newPhase) {
        onPhaseChange(newPhase);
    }

    private void onPhaseChange(int newPhase) {
        setPhase(newPhase);
        ArenaMechanics.updateBossPhase(this.blockPosition(), newPhase);
        // Play phase_change animation (3s) and activate core weak point for 4 seconds
        this.entityData.set(DATA_PHASE_CHANGE_TICKS, 60);
        activateCoreWeakPoint();

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 20, 1, 1, 1, 0.1);
            for (int i = 0; i < 50; i++) {
                double angle = (2 * Math.PI / 50) * i;
                double radius = 3 + i * 0.1;
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    getX() + Math.cos(angle) * radius, getY() + i * 0.2,
                    getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
            }
        }

        this.heal(20.0f);

        String msg = switch (newPhase) {
            case 2 -> "\u26A1 The Overseer summons quantum reinforcements!";
            case 3 -> "\u2620 THE OVERSEER UNLEASHES FULL POWER!";
            default -> "Phase " + newPhase + " begins!";
        };
        for (ServerPlayer p : bossEvent.getPlayers()) {
            p.displayClientMessage(Component.literal(msg).withStyle(
                newPhase == 3 ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.LIGHT_PURPLE,
                net.minecraft.ChatFormatting.BOLD), false);
        }

        if (newPhase == 2) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
        } else if (newPhase == 3) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0));
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));
        }

        this.level().playSound(null, this.blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_roar_overseer"), SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    /** Called when boss uses an ability (e.g. ShardVolley, Blink). Adds arena instability. */
    public void onBossAbilityUsed() {
        addArenaInstability(1);
    }

    /** When in an arena, add instability to all players in the arena (Echo dimension). */
    private void addArenaInstability(int tiers) {   // tiers -> meter percent (one system, 6 % per step)
        if (!(this.level() instanceof ServerLevel sl)) return;
        ArenaMechanics.ActiveArena arena = ArenaMechanics.getArenaAt(this.blockPosition());
        if (arena == null) return;
        AABB bounds = ArenaMechanics.getArenaBounds(arena);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, bounds)) {
            if (InstabilityManager.isPlayerInEcho(p)) InstabilityManager.addInstability(p, tiers * 6f);
        }
    }

    /** Activates core weak point for 4 seconds (80 ticks). Glow particles; 4× damage with quantum crystal weapon + overseer egg drop. */
    public void devOpenWeakPoint() { activateCoreWeakPoint(); }

    private void activateCoreWeakPoint() {
        this.entityData.set(DATA_CORE_WEAK_POINT, true);
        this.entityData.set(DATA_WEAK_POINT_TICKS, 80);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 40, 0.5, 0.5, 0.5, 0.08);
        }
    }

    // ========================================
    // WEIGHTED ATTACK SELECTION (BoMD Gauntlet pattern)
    // ========================================

    public byte chooseAttack() {
        LivingEntity target = getTarget();
        if (target == null) return ATTACK_BEAM;
        float hp = getHealth() / getMaxHealth();

        Map<Byte, Double> weights = new LinkedHashMap<>();
        weights.put(ATTACK_BEAM, 1.0);
        weights.put(ATTACK_SUMMON, getPhase() >= 2 ? 0.6 : 0.0);

        boolean lastWasLaser = !moveHistory.isEmpty() && moveHistory.peekLast() == ATTACK_LASER_SWEEP;
        weights.put(ATTACK_LASER_SWEEP, (lastWasLaser || hp >= LASER_SWEEP_THRESHOLD) ? 0.0 : 0.7);

        boolean lastWasSwirl = !moveHistory.isEmpty() && moveHistory.peekLast() == ATTACK_SWIRL;
        weights.put(ATTACK_SWIRL, (lastWasSwirl || hp >= SWIRL_THRESHOLD) ? 0.0 : 0.7);

        boolean usedBlindness = moveHistory.contains(ATTACK_BLINDNESS);
        weights.put(ATTACK_BLINDNESS, (usedBlindness || hp >= BLINDNESS_THRESHOLD) ? 0.0 : 1.0);

        double totalWeight = weights.values().stream().mapToDouble(d -> d).sum();
        if (totalWeight <= 0) return ATTACK_BEAM;

        double roll = this.random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (var entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                byte chosen = entry.getKey();
                moveHistory.addLast(chosen);
                if (moveHistory.size() > 4) moveHistory.pollFirst();
                return chosen;
            }
        }
        return ATTACK_BEAM;
    }

    // ========================================
    // ATTACK IMPLEMENTATIONS
    // ========================================

    public int performAttack(byte attack) {
        this.entityData.set(DATA_ATTACK, attack);
        this.attackAnimTicks = 24; // ~1.2s to match attack animation length
        if (attack == ATTACK_SWIRL || attack == ATTACK_SUMMON) {
            this.entityData.set(DATA_SPECIAL_ATTACK_TICKS, 60); // 3s special_attack animation
            activateCoreWeakPoint();
        }
        if (this.level() instanceof ServerLevel sl)
            sl.broadcastEntityEvent(this, attack);

        addArenaInstability(1);

        // Stage 4 moveset: every move telegraphs its exact danger zone on the floor, and the hit uses the same zone.
        LivingEntity tgt = getTarget();
        Vec3 floor = new Vec3(getX(), tgt != null ? tgt.getY() : getY(), getZ());
        return switch (attack) {
            case ATTACK_BEAM -> {   // aim locks when the wind-up starts: a 26-block lane, 2.6 wide
                Telegraph t = Telegraph.line(floor, tgt != null ? tgt.position() : floor.add(getLookAngle()), 26, 1.3);
                yield 80 + BossDirector.windup(this, "quantum_beam", 24, t, () -> performQuantumBeam(t));
            }
            case ATTACK_SWIRL -> {  // shockwave around where the Overseer hovers
                Telegraph t = Telegraph.circle(floor, 6.5);
                yield 60 + BossDirector.windup(this, "swirl_burst", 22, t, () -> performSwirlBurst(t));
            }
            case ATTACK_LASER_SWEEP -> {   // sweeping lasers: safe right under the Overseer or beyond 15 blocks
                Telegraph t = Telegraph.ring(floor, 3, 15);
                yield 100 + BossDirector.windup(this, "laser_sweep", 32, t, () -> performLaserSweep(t));
            }
            case ATTACK_SUMMON -> {
                Telegraph t = Telegraph.ring(floor, 3.5, 4.5);
                yield 120 + BossDirector.windup(this, "summon_constructs", 28, t, () -> performSummonMinions(t));
            }
            case ATTACK_BLINDNESS -> {  // the whole arena: break line of sight or get out
                Telegraph t = Telegraph.circle(floor, 20);
                yield 80 + BossDirector.windup(this, "blinding_pulse", 34, t, () -> performBlindnessPulse(t));
            }
            default -> 80;
        };
    }

    /** Shards per volley and damage per shard by phase (the first boss's volley was 6 x 10-14 with no warning). */
    public static int volleyShards(int phase) { return Math.min(6, 3 + Math.max(1, phase)); }
    public static float volleyDamage(int phase) { return 3.5f + 1.5f * Math.max(1, phase); }

    /** Shard Volley now winds up for 16 ticks (sound, converging glints, a cone on the floor toward where you stood)
     *  before it fires; 4/5/6 slow, weakly homing shards at 5/6.5/8 damage by phase. Returns the cooldown. */
    public int performShardVolley() {
        LivingEntity tgt = getTarget();
        setDataAttack(ATTACK_SHARD_VOLLEY);
        setAttackAnimTicks(24);
        Vec3 floor = new Vec3(getX(), tgt != null ? tgt.getY() : getY(), getZ());
        Vec3 aim = tgt != null ? tgt.position() : floor.add(getLookAngle());
        Telegraph t = Telegraph.cone(floor, aim, 20, 22);
        return 70 + BossDirector.windup(this, "shard_volley", 16, t, () -> {
            LivingEntity target = getTarget();
            if (!(level() instanceof ServerLevel sl)) return;
            Vec3 eye = getEyePosition(1f);
            Vec3 at = target != null ? target.getEyePosition(1f) : eye.add(t.dir());
            int n = volleyShards(getPhase());
            for (int i = 0; i < n; i++) {
                Vec3 dir = at.subtract(eye).normalize();
                double spread = 0.12;
                dir = dir.add(random.nextGaussian() * spread * 0.5, random.nextGaussian() * spread * 0.3, random.nextGaussian() * spread * 0.5).normalize();
                com.matt.quantumfalcon.entity.projectile.QuantumShardEntity shard = new com.matt.quantumfalcon.entity.projectile.QuantumShardEntity(
                    com.matt.quantumfalcon.registry.ModEntities.QUANTUM_SHARD.get(), this, eye.x, eye.y, eye.z, level(), volleyDamage(getPhase()), 0.025f);
                shard.setDeltaMovement(dir.scale(0.55));
                sl.addFreshEntity(shard);
            }
            onBossAbilityUsed();
        });
    }

    private java.util.List<LivingEntity> victims(Telegraph t) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(32, 24, 32),
            e -> e != this && e.isAlive() && !(e instanceof QuantumOverseerEntity) && !(e instanceof com.matt.quantumfalcon.entity.DataConstructEntity)
                && !(e instanceof Player p && (p.isCreative() || p.isSpectator())) && t.contains(e.position()));
    }

    private void performQuantumBeam(Telegraph t) {
        if (this.level() instanceof ServerLevel sl) {
            double len = t.dir().length();
            Vec3 u = t.dir().normalize();
            for (double d = 0; d <= len; d += 0.5)
                sl.sendParticles(ParticleTypes.DRAGON_BREATH, t.origin().x + u.x * d, t.origin().y + 1.0, t.origin().z + u.z * d, 2, 0.15, 0.15, 0.15, 0);
            for (double d = 0; d <= 6; d += 0.5)   // from the eye down onto the lane
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getEyeY() - (getEyeY() - t.origin().y - 1) * d / 6, getZ(), 1, 0.05, 0.05, 0.05, 0);
        }
        for (LivingEntity e : victims(t)) {
            e.hurt(this.damageSources().mobAttack(this), 12.0f);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_windup_overseer"), SoundSource.HOSTILE, 1.5f, 0.6f);
    }

    private void performSwirlBurst(Telegraph t) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        for (LivingEntity e : victims(t)) {
            Vec3 kb = e.position().subtract(t.origin()).multiply(1, 0, 1).normalize().scale(2.5);
            e.push(kb.x, 0.8, kb.z);
            e.hurt(this.damageSources().mobAttack(this), 15.0f);
        }
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            for (int r = 1; r <= 6; r++)
                sl.sendParticles(ParticleTypes.SONIC_BOOM, t.origin().x + Math.cos(angle) * r, t.origin().y + 1, t.origin().z + Math.sin(angle) * r, 1, 0, 0, 0, 0);
        }
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("entity_overseer_attack"), SoundSource.HOSTILE, 2.0f, 1.2f);
    }

    private void performLaserSweep(Telegraph t) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        setCharging(true);
        for (int sweep = 0; sweep < 12; sweep++) {
            double angle = Math.toRadians(sweep * 30 + this.tickCount * 3);
            for (double r = t.a(); r <= t.b(); r += 0.75)
                sl.sendParticles(ParticleTypes.END_ROD, t.origin().x + Math.cos(angle) * r, t.origin().y + 1.2, t.origin().z + Math.sin(angle) * r, 1, 0, 0, 0, 0);
        }
        for (LivingEntity e : victims(t)) e.hurt(damageSources().mobAttack(this), 10.0f + getPhase() * 2);
        setCharging(false);
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_windup_overseer"), SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    /** Spawns 1 + phase Data Constructs on the marked ring (at most 6 alive near the Overseer). */
    private void performSummonMinions(Telegraph t) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        int alive = sl.getEntitiesOfClass(com.matt.quantumfalcon.entity.DataConstructEntity.class, getBoundingBox().inflate(32)).size();
        int count = Math.min(1 + getPhase(), Math.max(0, 6 - alive));
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / Math.max(1, count)) * i;
            double x = t.origin().x + Math.cos(angle) * 4, z = t.origin().z + Math.sin(angle) * 4;
            sl.sendParticles(ParticleTypes.PORTAL, x, t.origin().y, z, 30, 0.5, 1, 0.5, 0);
            sl.sendParticles(ParticleTypes.WITCH, x, t.origin().y + 1, z, 10, 0.3, 0.5, 0.3, 0);
            var m = com.matt.quantumfalcon.registry.ModEntities.DATA_CONSTRUCT.get().create(sl);
            if (m == null) continue;
            m.moveTo(x, t.origin().y, z, random.nextFloat() * 360, 0);
            if (getTarget() != null) m.setTarget(getTarget());
            sl.addFreshEntity(m);
        }
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_windup_overseer"), SoundSource.HOSTILE, 1.5f, 1.0f);
    }

    private void performBlindnessPulse(Telegraph t) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        for (LivingEntity e : victims(t)) {
            if (!(e instanceof Player p)) continue;
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), t.origin().y + 2, getZ(), 100, 5, 3, 5, 0.1);
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_roar_overseer"), SoundSource.HOSTILE, 2.0f, 0.7f);
    }

    // ========================================
    // TARGET SWITCHING (BoMD DamageMemory pattern)
    // ========================================

    /** one blessed egg per fight. */
    public boolean blessedEggGiven = false;

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Core weak point: 4× damage with a quantum crystal weapon. the blessed egg is given once per fight, to the
        // first player whose crystal hit actually lands (hits eaten by invulnerability frames give nothing)
        boolean crystalWeakHit = false;
        if (this.entityData.get(DATA_CORE_WEAK_POINT) && source.getEntity() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();
            if (weapon.is(ModItems.MAGENTA_SWORD.get()) || weapon.is(ModItems.MAGENTA_PICKAXE.get())) {
                amount *= 4.0f;
                crystalWeakHit = true;
            }
        }
        boolean result = super.hurt(source, amount);
        if (result && crystalWeakHit && !blessedEggGiven && !level().isClientSide && source.getEntity() instanceof Player hitter) {
            blessedEggGiven = true;
            BlessedEggs.give(hitter, "overseer", "Overseer Blessed");
        }
        if (result && source.getEntity() instanceof Player player) {
            if (!isAggroed) {
                isAggroed = true;
            }
            damageMemory.merge(player.getUUID(), amount, Float::sum);
            if (damageMemory.size() > DAMAGE_MEMORY_SIZE) {
                Iterator<UUID> it = damageMemory.keySet().iterator();
                it.next(); it.remove();
            }
            trySwitchTarget();
        }
        checkPhaseTransition();
        return result;
    }

    private void trySwitchTarget() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        UUID topDamager = damageMemory.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse(null);
        if (topDamager != null) {
            Player p = sl.getPlayerByUUID(topDamager);
            if (p != null && p.isAlive() && p.distanceToSqr(this) < 2500) {
                this.setTarget(p);
            }
        }
    }

    // ========================================
    // TICK
    // ========================================

    @Override
    public void aiStep() {
        super.aiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        checkPhaseTransition();
        if (getNavigation() instanceof BossNavigation bossNav) {
            int p = getPhase();
            bossNav.setSpeedModifier(p >= 3 ? 1.8 : p >= 2 ? 1.4 : 1.0);
        }
        if (attackCooldown > 0) attackCooldown--;
        if (attackAnimTicks > 0) {
            attackAnimTicks--;
            if (attackAnimTicks <= 0) this.entityData.set(DATA_ATTACK, ATTACK_NONE);
        }
        // Core weak point timer (4 seconds)
        int wp = this.entityData.get(DATA_WEAK_POINT_TICKS);
        if (wp > 0) {
            this.entityData.set(DATA_WEAK_POINT_TICKS, wp - 1);
            if (wp - 1 <= 0) this.entityData.set(DATA_CORE_WEAK_POINT, false);
        }
        // Phase change / special attack animation tick-down (client sync)
        int pc = this.entityData.get(DATA_PHASE_CHANGE_TICKS);
        if (pc > 0) this.entityData.set(DATA_PHASE_CHANGE_TICKS, pc - 1);
        int sc = this.entityData.get(DATA_SPECIAL_ATTACK_TICKS);
        if (sc > 0) this.entityData.set(DATA_SPECIAL_ATTACK_TICKS, sc - 1);

        if (getTarget() == null && isAggroed) {
            this.heal(0.5f);
        }

        if (this.level().isClientSide && this.tickCount % 8 == 0) {   // ART_STYLE: sparse dark void wisps, never hide the model
            double x = getX() + (random.nextDouble() - 0.5) * 4;
            double y = getY() + random.nextDouble() * 4;
            double z = getZ() + (random.nextDouble() - 0.5) * 4;
            this.level().addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
        }
    }

    // ========================================
    // OVERSEER ATTACK GOAL (BoMD ActionGoal + CooldownAction pattern)
    // ========================================

    private static class OverseerAttackGoal extends Goal {
        private final QuantumOverseerEntity boss;

        public OverseerAttackGoal(QuantumOverseerEntity boss) {
            this.boss = boss;
        }

        @Override
        public boolean canUse() {
            return boss.isAlive() && boss.getTarget() != null && boss.attackCooldown <= 0 && boss.isAggroed;
        }

        @Override
        public void start() {
            byte attack = boss.chooseAttack();
            int cooldown = boss.performAttack(attack);
            boss.attackCooldown = cooldown;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    // ========================================
    // BOSS EVENT + SAVE/LOAD
    // ========================================

    @Override
    public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override
    public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override
    public void remove(RemovalReason r) { bossEvent.removeAllPlayers(); super.remove(r); }
    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public void die(DamageSource source) {
        // the Overseer is repeatable (every guardian arena has one), so only a killer's first Overseer pays the full
        // table; repeats pay a reduced table (no Eye, no nether star) and a fifth of the XP. Decided before super.die,
        // because the death event inside it records the kill.
        // peek, don't consume (the death handler needs the same ledger to credit everyone who fought).
        // nobody credited (void pit, suffocation, a cart off the edge) -> the reduced table.
        // the shared drop is the reduced table as soon as any credited player has beaten an Overseer before; each
        // first-timer still gets the Eye (and skill points) personally through BossDeathHandler.
        var killer = level() instanceof net.minecraft.server.level.ServerLevel sl ? com.matt.quantumfalcon.story.BossCredit.peekCredited(this, source, sl) : java.util.List.<net.minecraft.server.level.ServerPlayer>of();
        repeatKill = killer.isEmpty() || killer.stream().anyMatch(p -> com.matt.quantumfalcon.story.PlayerStoryData.get(p).hasDefeatedBoss("QUANTUM_OVERSEER"));
        if (repeatKill) this.xpReward = 100;
        ArenaMechanics.onBossDeath(this);
        super.die(source);
    }

    private boolean repeatKill = false;
    /** true once die picked the reduced shared table (read by BossDeathHandler for per-player first-kill extras). */
    public boolean reducedFloor() { return repeatKill; }

    @Override
    protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> getDefaultLootTable() {
        if (!repeatKill) return super.getDefaultLootTable();
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "entities/quantum_overseer_repeat"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase());
        tag.putBoolean("Aggroed", isAggroed);
        tag.putBoolean("BlessedEggGiven", blessedEggGiven);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Phase")) setPhase(tag.getInt("Phase"));
        if (tag.contains("Aggroed")) isAggroed = tag.getBoolean("Aggroed");
        blessedEggGiven = tag.getBoolean("BlessedEggGiven");
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }

    @Override
    public void setCustomName(Component name) { super.setCustomName(name); bossEvent.setName(getDisplayName()); }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("overseer"); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("overseer"); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("overseer"); }
    @Override
    public boolean canChangeDimensions(Level from, Level to) { return false; }

    @Override
    public int getArmorValue() {
        return getTarget() != null ? super.getArmorValue() : 24;
    }

    // ========================================
    // GECKOLIB — Exact 7 animations from quantum_overseer.animation.json
    // Priority: death > phase_change > special_attack > attack > hurt > walk > idle
    // ========================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { controllers.add(BossDirector.presentation(this, "quantum_overseer"));
        controllers.add(new AnimationController<>(this, "orbit", 0, e -> e.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.quantum_overseer.orbit"))));   // v3 FX bones (separate from body)
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            if (!this.isAlive())
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.quantum_overseer.death"));

            if (this.entityData.get(DATA_PHASE_CHANGE_TICKS) > 0)
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.quantum_overseer.phase_change"));

            if (this.entityData.get(DATA_SPECIAL_ATTACK_TICKS) > 0)
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.quantum_overseer.special_attack"));

            if (this.getCurrentAttack() != ATTACK_NONE)
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.quantum_overseer.attack"));

            if (this.hurtTime > 0)
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.quantum_overseer.hurt"));

            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.001)
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.quantum_overseer.walk"));

            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.quantum_overseer.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.step("overseer"), 0.6f, 1f); }
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity target) { boolean landed = super.doHurtTarget(target); if (landed) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "overseer", 1f); return landed; }
}
