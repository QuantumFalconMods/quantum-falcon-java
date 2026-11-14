package com.matt.quantumfalcon.entity.boss;

import com.matt.quantumfalcon.dimension.InstabilityManager;
import com.matt.quantumfalcon.entity.ai.BossNavigation;
import com.matt.quantumfalcon.entity.ai.riftwarden.*;
import com.matt.quantumfalcon.registry.ModDataComponents;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.registry.ModParticles;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

/**
 * Rift Warden - Boss #2 (Dimensional Rifts). Extends Monster and GeoEntity.
 *
 * <p>Animation names (exact match to rift_warden.animation.json): idle, walk, attack,
 * rift_burst, special_attack, phase_change, death.
 *
 * <p>Phase system:
 * <ul>
 *   <li>Phase 1: 100–66% HP</li>
 *   <li>Phase 2: 65–36% HP (onPhaseChangeTo(2), phase_change, instability, REVERSE_PORTAL + QUANTUM_GLOW, heal, boss bar message)</li>
 *   <li>Phase 3: 35–0% HP (onPhaseChangeTo(3), same)</li>
 * </ul>
 *
 * <p>Halo Spin Nova (special_attack): DATA_SPECIAL_ATTACK_TICKS starts at 60 (3s). Weak point
 * active when ticks between 16 and 44 (0.8s–2.2s window). Only MAGENTA_SWORD or MAGENTA_PICKAXE
 * do 4× damage in window; non-crystal weapons do 0 damage + shield block sound + RIFT_BURST + END_ROD.
 * On successful crystal hit: DATA_STUN_TICKS = 40 (2s stun), drop FALCON_EGG with LEGENDARY_GUARANTEED
 * and FALCON_NAME "Rift Attuned", RIFT_BURST + QUANTUM_GLOW + END_ROD particles, sonic boom sound,
 * +5 instability to hitter if in Echo (InstabilityManager.addInstability).
 *
 * <p>Particles: only REVERSE_PORTAL, QUANTUM_GLOW, END_ROD, RIFT_BURST (no quantum_sparkle/rift_vortex).
 *
 * <p>AI goals (entity.ai.riftwarden): RiftPhaseChangeGoal, RiftHaloSpinGoal, RiftShardVolleyGoal (8 QuantumShardEntity),
 * RiftCrystalStormGoal (Phase 2+, END_ROD + QUANTUM_GLOW, levitation + damage 6-block AABB),
 * RiftCollapseGoal (Phase 3, REVERSE_PORTAL ring, pull + damage 10-block AABB), RiftWardenAttackGoal (fallback).
 *
 * <p>Entanglement: applyEntanglementDamage(float) applies 40% shared damage to players and QuantumFalconEntity
 * in 12-block AABB; called every 20 ticks during special_attack.
 *
 * <p>Arena lock: BossNavigation uses ArenaMechanics to clamp to arena bounds.
 * Instability: InstabilityManager.addInstability for every player in arena on phase change and every boss ability.
 *
 * <p>Synced data: DATA_PHASE, DATA_SPECIAL_ATTACK_TICKS, DATA_PHASE_CHANGE_TICKS, DATA_ATTACK, DATA_STUN_TICKS.
 *
 * <p>Death: die() sets deathAnimationTicks = 84 (4.2s), does not call super.die(). aiStep() countdown then
 * calls super.die(pendingDeathSource). isAlive() returns false during the 3.8s so entity is treated as dead.
 */
public class RiftWardenEntity extends Monster implements GeoEntity, QFBoss {
    // bosses never ride (minecarts hauled them out of their arenas) and never take portals
    @Override protected boolean canRide(net.minecraft.world.entity.Entity vehicle) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public int qfPhase() { return getPhase(); }
    @Override public net.minecraft.server.level.ServerBossEvent qfBossBar() { return bossEvent; }

    // ----- Synced data (phase, weak point, animation state) -----
    private static final EntityDataAccessor<Integer> DATA_PHASE =
        SynchedEntityData.defineId(RiftWardenEntity.class, EntityDataSerializers.INT);
    /** Ticks remaining of special_attack animation (60 = 3s). When 44–16, halo weak point is active (1.4s window). */
    private static final EntityDataAccessor<Integer> DATA_SPECIAL_ATTACK_TICKS =
        SynchedEntityData.defineId(RiftWardenEntity.class, EntityDataSerializers.INT);
    /** Ticks remaining of phase_change animation. */
    private static final EntityDataAccessor<Integer> DATA_PHASE_CHANGE_TICKS =
        SynchedEntityData.defineId(RiftWardenEntity.class, EntityDataSerializers.INT);
    /** Current attack byte for animation (attack, rift_burst, special_attack). */
    private static final EntityDataAccessor<Byte> DATA_ATTACK =
        SynchedEntityData.defineId(RiftWardenEntity.class, EntityDataSerializers.BYTE);
    /** Stun: no movement/attacks when > 0. */
    private static final EntityDataAccessor<Integer> DATA_STUN_TICKS =
        SynchedEntityData.defineId(RiftWardenEntity.class, EntityDataSerializers.INT);

    public static final byte ATTACK_NONE = 0;
    public static final byte ATTACK_SHARD_VOLLEY = 1;
    public static final byte ATTACK_HALO_SPIN = 2;
    public static final byte ATTACK_CRYSTAL_STORM = 3;
    public static final byte ATTACK_RIFT_COLLAPSE = 4;
    public static final byte ATTACK_RIFT_BURST = 5;

    private static final int SPECIAL_ATTACK_LENGTH_TICKS = 60;  // 3.0s
    /** Weak point window: from tick 16 to 44 (0.8s–2.2s into 3s anim) = 28 ticks = 1.4s. */
    private static final int WEAK_POINT_START_TICK = 44;
    private static final int WEAK_POINT_END_TICK = 16;
    private static final int STUN_TICKS_ON_WEAK_POINT_HIT = 40;  // 2 seconds
    private static final int DEATH_ANIMATION_TICKS = 84;  // 4.2s dramatic death
    private static final int INSTABILITY_ON_PHASE_OR_SPECIAL = 10;   // meter percent; drains 1 %/s in the arena

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(
        Component.translatable("entity.quantumfalcon.rift_warden"),
        BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10
    );

    private int attackCooldown = 0;
    private int attackAnimTicks = 0;
    private boolean isAggroed = false;
    private int deathAnimationTicks = -1;
    private DamageSource pendingDeathSource = null;
    private final Deque<Byte> moveHistory = new ArrayDeque<>(4);
    private final Map<UUID, Float> damageMemory = new LinkedHashMap<>();
    private static final int DAMAGE_MEMORY_SIZE = 5;

    public RiftWardenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 600;
        this.setPersistenceRequired();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new BossNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RiftPhaseChangeGoal(this));
        this.goalSelector.addGoal(2, new RiftHaloSpinGoal(this));
        this.goalSelector.addGoal(3, new RiftShardVolleyGoal(this));
        this.goalSelector.addGoal(4, new RiftCrystalStormGoal(this));
        this.goalSelector.addGoal(5, new RiftCollapseGoal(this));
        this.goalSelector.addGoal(6, new RiftWardenAttackGoal(this));
        this.goalSelector.addGoal(7, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 800.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 18.0)
            .add(Attributes.ARMOR, 12.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.95)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_SPECIAL_ATTACK_TICKS, 0);
        builder.define(DATA_PHASE_CHANGE_TICKS, 0);
        builder.define(DATA_ATTACK, ATTACK_NONE);
        builder.define(DATA_STUN_TICKS, 0);
    }

    public int getPhase() { return this.entityData.get(DATA_PHASE); }
    public void setPhase(int p) { this.entityData.set(DATA_PHASE, p); }
    public int getSpecialAttackTicks() { return this.entityData.get(DATA_SPECIAL_ATTACK_TICKS); }
    public void setSpecialAttackTicks(int t) { this.entityData.set(DATA_SPECIAL_ATTACK_TICKS, t); }
    public int getPhaseChangeTicks() { return this.entityData.get(DATA_PHASE_CHANGE_TICKS); }
    public void setPhaseChangeTicks(int t) { this.entityData.set(DATA_PHASE_CHANGE_TICKS, t); }
    public byte getDataAttack() { return this.entityData.get(DATA_ATTACK); }
    public void setDataAttack(byte a) { this.entityData.set(DATA_ATTACK, a); }
    public int getStunTicks() { return this.entityData.get(DATA_STUN_TICKS); }
    public void setStunTicks(int t) { this.entityData.set(DATA_STUN_TICKS, t); }
    public int getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(int t) { this.attackCooldown = t; }
    public void setAttackAnimTicks(int t) { this.attackAnimTicks = t; }
    public boolean isAggroed() { return isAggroed; }

    /** True during the 1.4s window (0.8–2.2s into special_attack) when halo/rift_core is weak point. */
    public boolean isHaloWeakPointActive() {
        int t = this.entityData.get(DATA_SPECIAL_ATTACK_TICKS);
        return t > WEAK_POINT_END_TICK && t <= WEAK_POINT_START_TICK;
    }

    /** Whether we are currently playing the special_attack (Halo Spin Nova) animation. */
    public boolean isInSpecialAttack() {
        return this.entityData.get(DATA_SPECIAL_ATTACK_TICKS) > 0;
    }

    // ----- Phase system (100–66% = 1, 65–36% = 2, 35–0% = 3) -----
    private void checkPhaseTransition() {
        if (isDeadOrDying()) return;   // a killing blow crosses every threshold; don't announce a phase after the defeat line
        float hp = getHealth() / getMaxHealth();
        if (hp <= 0.35f && getPhase() < 3) onPhaseChangeTo(3);
        else if (hp <= 0.65f && getPhase() < 2) onPhaseChangeTo(2);
    }

    public void onPhaseChangeTo(int newPhase) {
        setPhase(newPhase);
        ArenaMechanics.updateBossPhase(this.blockPosition(), newPhase);
        setPhaseChangeTicks(60);  // 3s phase_change
        addArenaInstability(12);  // +12 % instability to all players in arena on phase change
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 80, 2, 2, 2, 0.15);
            sl.sendParticles(ModParticles.QUANTUM_GLOW.get(), getX(), getY() + 1.5, getZ(), 30, 0.5, 0.5, 0.5, 0.02);
        }
        heal(getMaxHealth() * 0.1f);  // 10% heal on phase change
        summonMinions(newPhase); // rift_warden_minion adds: 2 in phase 2, 3 in phase 3
        String msg = switch (newPhase) {
            case 2 -> "\u26A1 The Warden tears at the fabric of reality!";
            case 3 -> "\u2620 THE WARDEN UNLEASHES THE RIFT!";
            default -> "Phase " + newPhase;
        };
        for (ServerPlayer p : bossEvent.getPlayers()) {
            p.displayClientMessage(Component.literal(msg).withStyle(
                newPhase == 3 ? ChatFormatting.DARK_RED : ChatFormatting.BLUE, ChatFormatting.BOLD), false);
        }
        level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_roar_warden"), SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    public java.util.List<RiftWardenMinionEntity> summonMinions(int phase) {
        java.util.List<RiftWardenMinionEntity> out = new java.util.ArrayList<>();
        if (!(level() instanceof ServerLevel sl)) return out;
        int n = phase >= 3 ? 3 : 2;
        for (int i = 0; i < n; i++) {
            RiftWardenMinionEntity m = com.matt.quantumfalcon.registry.ModEntities.RIFT_WARDEN_MINION.get().create(sl);
            if (m == null) continue;
            double a = (Math.PI * 2 / n) * i;
            m.moveTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, (float) Math.toDegrees(a), 0);
            m.setOwnerWarden(this);
            m.setTarget(getTarget());
            sl.addFreshEntity(m);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, m.getX(), m.getY() + 1, m.getZ(), 25, 0.3, 0.6, 0.3, 0.1);
            out.add(m);
        }
        return out;
    }

    private void addArenaInstability(int amount) {
        if (!(level() instanceof ServerLevel sl)) return;
        ArenaMechanics.ActiveArena arena = ArenaMechanics.getArenaAt(blockPosition());
        if (arena == null) return;
        AABB bounds = ArenaMechanics.getArenaBounds(arena);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, bounds)) {
            if (InstabilityManager.isPlayerInEcho(p)) InstabilityManager.addInstability(p, amount);
        }
    }

    public void onBossAbilityUsed() {
        addArenaInstability(INSTABILITY_ON_PHASE_OR_SPECIAL);
    }

    // ----- Halo Spin Nova: start special_attack, weak point window, stun + egg on crystal hit -----
    public void startHaloSpinNova() {
        setSpecialAttackTicks(SPECIAL_ATTACK_LENGTH_TICKS);
        setDataAttack(ATTACK_HALO_SPIN);
        setAttackAnimTicks(SPECIAL_ATTACK_LENGTH_TICKS);
        onBossAbilityUsed();
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 20, 0.5, 0.5, 0.5, 0.02);
            sl.sendParticles(ModParticles.RIFT_BURST.get(), getX(), getY() + 1, getZ(), 15, 0.4, 0.4, 0.4, 0.01);
            sl.sendParticles(ModParticles.QUANTUM_GLOW.get(), getX(), getY() + 1.5, getZ(), 25, 0.4, 0.4, 0.4, 0.02);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 25, 0.4, 0.4, 0.4, 0.05);
        }
        level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_windup_warden"), SoundSource.HOSTILE, 1.0f, 0.6f);
    }

    public boolean blessedEggGiven = false;

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // During Halo Spin weak point window: only Quantum Crystal weapons deal damage (4×) and trigger stun + egg.
        if (isHaloWeakPointActive() && source.getEntity() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();
            boolean isCrystalWeapon = weapon.is(ModItems.MAGENTA_SWORD.get()) || weapon.is(ModItems.MAGENTA_PICKAXE.get());
            if (isCrystalWeapon) {
                amount *= 4.0f;
                setStunTicks(STUN_TICKS_ON_WEAK_POINT_HIT);
                setSpecialAttackTicks(0);
                setDataAttack(ATTACK_NONE);
                if (!level().isClientSide && !blessedEggGiven) {   // one per fight, straight to the hitter, real Legendary
                    blessedEggGiven = true;
                    BlessedEggs.give(player, "warden", "Rift Attuned");
                }
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ModParticles.RIFT_BURST.get(), getX(), getY() + 1.5, getZ(), 40, 0.3, 0.3, 0.3, 0.05);
                    sl.sendParticles(ModParticles.QUANTUM_GLOW.get(), getX(), getY() + 1.5, getZ(), 25, 0.4, 0.4, 0.4, 0.02);
                    sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 30, 0.3, 0.3, 0.3, 0.1);
                }
                level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_roar_warden"), SoundSource.HOSTILE, 1.5f, 1.0f);
                if (InstabilityManager.isPlayerInEcho(player) && player instanceof ServerPlayer sp)
                    InstabilityManager.addInstability(sp, 3f);
            } else {
                level().playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.8f, 1.2f);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ModParticles.RIFT_BURST.get(), getX(), getY() + 1.5, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                    sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                }
                return false;
            }
        }

        boolean result = super.hurt(source, amount);
        if (result && source.getEntity() instanceof Player p) {
            if (!isAggroed) isAggroed = true;
            damageMemory.merge(p.getUUID(), amount, Float::sum);
            while (damageMemory.size() > DAMAGE_MEMORY_SIZE) damageMemory.keySet().iterator().remove();
            trySwitchTarget();
        }
        checkPhaseTransition();
        return result;
    }

    private void trySwitchTarget() {
        if (!(level() instanceof ServerLevel sl)) return;
        UUID top = damageMemory.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        if (top != null) {
            Player p = sl.getPlayerByUUID(top);
            if (p != null && p.isAlive() && p.distanceToSqr(this) < 2500) setTarget(p);
        }
    }

    // ----- Death: play full 3.8s animation then remove -----
    @Override
    public void die(DamageSource source) {
        if (deathAnimationTicks >= 0) return;
        setHealth(0);
        deathAnimationTicks = DEATH_ANIMATION_TICKS;
        pendingDeathSource = source;
        ArenaMechanics.onBossDeath(this);
    }

    @Override
    public boolean isAlive() {
        return deathAnimationTicks < 0 && super.isAlive();
    }

    /** Hold the vanilla 20-tick corpse removal until the death animation has finished and loot was dropped. */
    @Override
    protected void tickDeath() {
        if (deathAnimationTicks > 0) return;
        super.tickDeath();
    }

    // ----- Tick: cooldowns, animation tick-down, death delay, particles -----
    @Override
    public void aiStep() {
        if (deathAnimationTicks > 0) {
            deathAnimationTicks--;
            if (deathAnimationTicks == 0 && pendingDeathSource != null) {
                super.die(pendingDeathSource);
            }
            return;
        }
        if (deathAnimationTicks == 0) return;

        super.aiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (getNavigation() instanceof BossNavigation bn) bn.setSpeedModifier(getPhase() >= 3 ? 1.5 : getPhase() >= 2 ? 1.25 : 1.0);

        if (getStunTicks() > 0) {
            setStunTicks(getStunTicks() - 1);
            setDeltaMovement(Vec3.ZERO);
            getNavigation().stop();
        }

        if (attackCooldown > 0) attackCooldown--;
        if (attackAnimTicks > 0) {
            attackAnimTicks--;
            if (attackAnimTicks <= 0) setDataAttack(ATTACK_NONE);
        }
        int spec = getSpecialAttackTicks();
        if (spec > 0) {
            setSpecialAttackTicks(spec - 1);
            if (spec - 1 <= 0) setDataAttack(ATTACK_NONE);
        }
        int pc = getPhaseChangeTicks();
        if (pc > 0) setPhaseChangeTicks(pc - 1);

        if (getTarget() == null && isAggroed) heal(0.4f);

        if (level().isClientSide && tickCount % 8 == 0) {   // ART_STYLE: sparse dark void wisps
            this.level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * 4, getY() + random.nextDouble() * 4, getZ() + (random.nextDouble() - 0.5) * 4, 0, 0.02, 0);
        }
        if (!level().isClientSide && isInSpecialAttack() && level() instanceof ServerLevel sl) {
            if (tickCount % 2 == 0) {
                sl.sendParticles(ModParticles.QUANTUM_GLOW.get(), getX(), getY() + 1.5, getZ(), 2, 0.2, 0.2, 0.2, 0.01);
            }
            if (spec > 0 && spec % 20 == 0) applyEntanglementDamage(3.0f);
        }
    }

    // ----- Attack performance (called by goals) -----
    // Stage 4 moveset: each move telegraphs its zone on the floor first; the hit uses the same zone (step out to dodge).
    private Vec3 floorHere() { return new Vec3(getX(), getY(), getZ()); }

    private java.util.List<LivingEntity> victims(Telegraph t) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(28, 12, 28),
            e -> e != this && e.isAlive() && !(e instanceof RiftWardenEntity) && !(e instanceof RiftWardenMinionEntity)
                && !(e instanceof Player p && (p.isCreative() || p.isSpectator())) && t.contains(e.position()));
    }

    /** Shard Volley: a 40-degree cone toward where the target stood when the wind-up began; 8 shards fly down it. */
    public int performShardVolley() {
        setDataAttack(ATTACK_RIFT_BURST);
        setAttackAnimTicks(24);
        LivingEntity tgt = getTarget();
        Vec3 aim = tgt != null ? tgt.position() : floorHere().add(getLookAngle());
        Telegraph t = Telegraph.cone(floorHere(), aim, 20, 20);
        return 60 + BossDirector.windup(this, "shard_volley", 18, t, () -> {
            onBossAbilityUsed();
            if (level() instanceof ServerLevel sl) {
                Vec3 eye = getEyePosition(1f);
                Vec3 dir = t.dir();
                // aim the fan down at chest height where the target stood (a flat -0.08 drop from the Warden's 4-block-high
                // eye sailed over every player's head inside ~15 blocks)
                double horiz = Math.max(2.0, Math.hypot(aim.x - eye.x, aim.z - eye.z));
                double pitch = net.minecraft.util.Mth.clamp(((aim.y + 1.0) - eye.y) / horiz, -0.9, 0.3);
                for (int i = 0; i < 8; i++) {
                    com.matt.quantumfalcon.entity.projectile.QuantumShardEntity shard = new com.matt.quantumfalcon.entity.projectile.QuantumShardEntity(
                        com.matt.quantumfalcon.registry.ModEntities.QUANTUM_SHARD.get(), this, eye.x, eye.y, eye.z, level(), 7f + getPhase() * 2, 0.05f);
                    double yaw = Math.toRadians((i - 3.5) * 5);   // fanned evenly inside the cone
                    Vec3 hd = new Vec3(dir.x, 0, dir.z).normalize();
                    Vec3 to = new Vec3(hd.x * Math.cos(yaw) - hd.z * Math.sin(yaw), pitch, hd.x * Math.sin(yaw) + hd.z * Math.cos(yaw)).normalize();
                    shard.setDeltaMovement(to.scale(0.7));
                    sl.addFreshEntity(shard);
                }
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 15, 0.3, 0.3, 0.3, 0.03);
            }
            level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8f, 1.2f);
        });
    }

    /** Crystal Storm: a 6-block disc around the Warden; damage and a short levitation. */
    public int performCrystalStorm() {
        setDataAttack(ATTACK_RIFT_BURST);
        setAttackAnimTicks(40);
        Telegraph t = Telegraph.circle(floorHere(), 6);
        return 80 + BossDirector.windup(this, "crystal_storm", 20, t, () -> {
            onBossAbilityUsed();
            if (level() instanceof ServerLevel sl) {
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI * i / 8) + tickCount * 0.1;
                    double r = 3 + random.nextDouble() * 2;
                    double x = t.origin().x + Math.cos(angle) * r, z = t.origin().z + Math.sin(angle) * r;
                    sl.sendParticles(ParticleTypes.END_ROD, x, t.origin().y + 1, z, 5, 0.2, 0.2, 0.2, 0.02);
                    sl.sendParticles(ModParticles.QUANTUM_GLOW.get(), x, t.origin().y + 1, z, 2, 0.1, 0.1, 0.1, 0.01);
                }
                for (LivingEntity e : victims(t)) {
                    e.hurt(damageSources().mobAttack(this), 6.0f + getPhase() * 2);
                    e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
                }
            }
            level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_roar_warden"), SoundSource.HOSTILE, 1.2f, 0.8f);
        });
    }

    /** Rift Collapse (phase 3): the floor between 2.5 and 10 blocks folds inward; hugging the Warden is the safe spot. */
    public int performRiftCollapse() {
        setDataAttack(ATTACK_RIFT_BURST);
        setAttackAnimTicks(50);
        Telegraph t = Telegraph.ring(floorHere(), 2.5, 10);
        return 120 + BossDirector.windup(this, "rift_collapse", 30, t, () -> {
            onBossAbilityUsed();
            if (level() instanceof ServerLevel sl) {
                for (int r = 3; r <= 10; r++)
                    for (int i = 0; i < 24; i++) {
                        double angle = Math.toRadians(i * 15 + tickCount);
                        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, t.origin().x + Math.cos(angle) * r, t.origin().y + 1, t.origin().z + Math.sin(angle) * r, 1, 0, 0, 0, 0);
                    }
                for (LivingEntity e : victims(t)) {
                    Vec3 pull = t.origin().subtract(e.position()).normalize().scale(0.15);
                    e.push(pull.x, 0.05, pull.z);
                    e.hurt(damageSources().mobAttack(this), 10.0f + getPhase() * 3);
                }
            }
            level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_windup_warden"), SoundSource.HOSTILE, 1.5f, 0.5f);
        });
    }

    /** Entanglement: during special attack, 40% of damage to linked players/falcons is shared. */
    public void applyEntanglementDamage(float amount) {
        if (!(level() instanceof ServerLevel sl)) return;
        AABB box = getBoundingBox().inflate(12);
        float shared = amount * 0.4f;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && (e instanceof Player || e instanceof com.matt.quantumfalcon.entity.QuantumFalconEntity))) {
            e.hurt(damageSources().mobAttack(this), shared);
        }
    }

    // ----- Boss event & save/load -----
    @Override
    public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override
    public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override
    public void remove(RemovalReason r) { bossEvent.removeAllPlayers(); super.remove(r); }
    @Override
    public boolean removeWhenFarAway(double d) { return false; }

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

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("warden"); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("warden"); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("warden"); }
    @Override
    public int getArmorValue() { return getTarget() != null ? super.getArmorValue() : 20; }

    // ----- GeckoLib: exact animation names from .animation.json -----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { controllers.add(BossDirector.presentation(this, "rift_warden"));
        controllers.add(new AnimationController<>(this, "fx", 0, e -> e.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.rift_warden.fx"))));   // v3 FX bones (separate from body)
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (deathAnimationTicks >= 0)
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.rift_warden.death"));
            if (getPhaseChangeTicks() > 0)
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.rift_warden.phase_change"));
            if (getSpecialAttackTicks() > 0)
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.rift_warden.special_attack"));
            byte atk = getDataAttack();
            if (atk == ATTACK_HALO_SPIN)
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.rift_warden.special_attack"));
            if (atk == ATTACK_RIFT_BURST)
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.rift_warden.rift_burst"));
            if (atk != ATTACK_NONE)
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.rift_warden.attack"));
            if (hurtTime > 0)
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.rift_warden.attack"));
            if (state.isMoving())
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.rift_warden.walk"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.rift_warden.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.step("warden"), 0.6f, 1f); }
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity target) { boolean landed = super.doHurtTarget(target); if (landed) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "warden", 1f); return landed; }
}
