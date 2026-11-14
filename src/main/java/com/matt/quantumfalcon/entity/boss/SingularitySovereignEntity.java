package com.matt.quantumfalcon.entity.boss;

import com.matt.quantumfalcon.entity.ai.BossNavigation;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.skill.PlayerSkillData;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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

import java.util.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Singularity Sovereign - Final boss (Quantum Realm).
 * Hybrid of Obsidilith (shield pillars, wave/spike) + VoidBlossom (HP-milestone
 * blossom heals, spore/blade) + unique gravity/reality distortion.
 *
 * Attacks: Gravity Pull, Reality Distort, Singularity Wave, Dimensional Spike,
 *          Void Burst, Crystal Shield (pillar defense), Event Horizon (ultimate),
 *          Quantum Blossom (milestone heal)
 * 4 phases with escalating mechanics.
 */
public class SingularitySovereignEntity extends Monster implements GeoEntity, QFBoss {
    // bosses never ride (minecarts hauled them out of their arenas) and never take portals
    @Override protected boolean canRide(net.minecraft.world.entity.Entity vehicle) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public int qfPhase() { return getPhase(); }
    @Override public net.minecraft.server.level.ServerBossEvent qfBossBar() { return bossEvent; }

    private static final EntityDataAccessor<Integer> DATA_PHASE =
        SynchedEntityData.defineId(SingularitySovereignEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_INVULNERABLE =
        SynchedEntityData.defineId(SingularitySovereignEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GRAVITY_PULL =
        SynchedEntityData.defineId(SingularitySovereignEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SHIELDED =
        SynchedEntityData.defineId(SingularitySovereignEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_ATTACK_ANIM =
        SynchedEntityData.defineId(SingularitySovereignEntity.class, EntityDataSerializers.BYTE);

    public static final byte ANIM_NONE = 0;
    public static final byte ANIM_MELEE = 1;
    public static final byte ANIM_GRAVITY = 2;
    public static final byte ANIM_EVENT_HORIZON = 3;
    public static final byte ANIM_REALITY_DISTORT = 4;
    public static final byte ANIM_CRYSTAL_SHIELD = 5;
    public static final byte ANIM_DEAD = 6;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
        Component.translatable("entity.quantumfalcon.singularity_sovereign"),
        BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20
    );

    // Attack IDs
    public static final byte ATTACK_GRAVITY_PULL = 4;
    public static final byte ATTACK_REALITY_DISTORT = 5;
    public static final byte ATTACK_SINGULARITY_WAVE = 6;
    public static final byte ATTACK_DIMENSIONAL_SPIKE = 7;
    public static final byte ATTACK_VOID_BURST = 8;
    public static final byte ATTACK_CRYSTAL_SHIELD = 9;
    public static final byte ATTACK_EVENT_HORIZON = 10;
    public static final byte ATTACK_BLOSSOM = 11;

    // HP milestones (combined Obsidilith + VoidBlossom)
    private static final List<Float> HP_MILESTONES = List.of(0.0f, 0.15f, 0.35f, 0.55f, 0.75f, 1.0f);

    private int attackCooldown = 0;
    private int invulnerableTime = 0;
    private boolean shouldDoCrystalShield = false;
    private boolean shouldDoBlossom = false;
    private int lastMilestoneIndex = -1;
    private int eventHorizonCooldown = 0;
    private int attackAnimTimer = 0;

    private final Deque<Byte> moveHistory = new ArrayDeque<>(5);
    private final Map<UUID, Float> damageMemory = new LinkedHashMap<>();
    private final List<BlockPos> singularityCrystals = new ArrayList<>();
    private int shieldTicks;
    public static final int SHIELD_MAX_TICKS = 240;   // 12 s
    /** Test hook: the live shield crystal positions. */
    public List<BlockPos> shieldCrystals() { return singularityCrystals; }
    /** Crystals within 2 blocks (4 vertically) of any of these players shatter. */
    public void shatterTouched(ServerLevel sl, List<? extends Player> players) {
        singularityCrystals.removeIf(pos -> {
            boolean touched = players.stream().anyMatch(pl -> pl.isAlive() && !pl.isSpectator()
                    && Math.abs(pl.getX() - (pos.getX() + 0.5)) <= 2.5 && Math.abs(pl.getZ() - (pos.getZ() + 0.5)) <= 2.5 && Math.abs(pl.getY() - pos.getY()) <= 4);
            if (touched) {
                sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 40, 0.4, 2, 0.4, 0.08);
                sl.playSound(null, pos, com.matt.quantumfalcon.entity.util.QFMobSounds.get("arena_pylon_break"), SoundSource.HOSTILE, 1.5f, 1.2f);
            }
            return touched;
        });
    }
    /** Test hook: raise the Crystal Shield now. */
    public void raiseShieldForTest() { performCrystalShield(); }

    public SingularitySovereignEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 1000;
        this.setPersistenceRequired();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new BossNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SovereignAttackGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 20.0f));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1000.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 25.0)
            .add(Attributes.ARMOR, 20.0)
            .add(Attributes.ARMOR_TOUGHNESS, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_INVULNERABLE, false);
        builder.define(DATA_GRAVITY_PULL, false);
        builder.define(DATA_SHIELDED, false);
        builder.define(DATA_ATTACK_ANIM, ANIM_NONE);
    }

    public byte getAttackAnim() { return entityData.get(DATA_ATTACK_ANIM); }

    private void setAttackAnim(byte anim, int durationTicks) {
        entityData.set(DATA_ATTACK_ANIM, anim);
        attackAnimTimer = durationTicks;
    }

    public int getPhase() { return entityData.get(DATA_PHASE); }
    public void setPhase(int p) { entityData.set(DATA_PHASE, p); }
    public boolean isInvulnerablePhase() { return entityData.get(DATA_INVULNERABLE); }
    public void setInvulnerablePhase(boolean v) { entityData.set(DATA_INVULNERABLE, v); }
    public boolean isGravityPulling() { return entityData.get(DATA_GRAVITY_PULL); }
    public void setGravityPulling(boolean v) { entityData.set(DATA_GRAVITY_PULL, v); }
    public boolean isShielded() { return entityData.get(DATA_SHIELDED); }

    // ========================================
    // DAMAGE (Obsidilith shield + VoidBlossom milestones + damage cap)
    // ========================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerablePhase()) {
            if (this.level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 1, getZ(), 10, 0.5, 0.5, 0.5, 0);
            return false;
        }
        if (isShielded()) {
            if (this.level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 2, getZ(), 15, 0.5, 0.5, 0.5, 0);
            this.level().playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0f, 1.5f);
            return false;
        }

        float cappedDmg = Math.min(amount, getMaxHealth() * 0.05f);
        float hpBefore = getHealth() / getMaxHealth();
        boolean result = super.hurt(source, cappedDmg);
        if (result) {
            float hpAfter = getHealth() / getMaxHealth();
            checkMilestone(hpBefore, hpAfter);
            checkPhaseTransition();
            if (source.getEntity() instanceof Player p) {
                damageMemory.merge(p.getUUID(), amount, Float::sum);
                trySwitchTarget();
            }
        }
        return result;
    }

    private void checkMilestone(float before, float after) {
        for (int i = 0; i < HP_MILESTONES.size(); i++) {
            float m = HP_MILESTONES.get(i);
            if (before > m && after <= m && i > lastMilestoneIndex) {
                lastMilestoneIndex = i;
                if (i % 2 == 0) shouldDoCrystalShield = true;
                else shouldDoBlossom = true;
                break;
            }
        }
    }

    private void trySwitchTarget() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        UUID top = damageMemory.entrySet().stream().max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse(null);
        if (top != null) {
            Player p = sl.getPlayerByUUID(top);
            if (p != null && p.isAlive() && p.distanceToSqr(this) < 4096) setTarget(p);
        }
    }

    // ========================================
    // PHASE MANAGEMENT
    // ========================================

    private void checkPhaseTransition() {
        if (isDeadOrDying()) return;   // a killing blow crosses every threshold; don't announce a phase after the defeat line
        float hp = getHealth() / getMaxHealth();
        int newPhase = getPhase();
        if (hp <= 0.25f && getPhase() < 4) newPhase = 4;
        else if (hp <= 0.50f && getPhase() < 3) newPhase = 3;
        else if (hp <= 0.75f && getPhase() < 2) newPhase = 2;
        if (newPhase != getPhase()) triggerPhaseTransition(newPhase);
    }

    private void triggerPhaseTransition(int newPhase) {
        setPhase(newPhase);
        setInvulnerablePhase(true);
        invulnerableTime = 60;
        setAttackAnim(ANIM_CRYSTAL_SHIELD, 50);
        ArenaMechanics.updateBossPhase(this.blockPosition(), newPhase);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2, getZ(), 5, 2, 2, 2, 0);
            for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(20))) {
                Vec3 kb = p.position().subtract(position()).normalize().scale(3);
                p.push(kb.x, 1.0, kb.z);
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            }
        }

        this.heal(getMaxHealth() * 0.1f);

        switch (newPhase) {
            case 2 -> addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 0));
            case 3 -> {
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 1));
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12000, 0));
            }
            case 4 -> {
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 2));
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12000, 1));
                addEffect(new MobEffectInstance(MobEffects.REGENERATION, 12000, 0));
            }
        }

        String msg = switch (newPhase) {
            case 2 -> "\u26A1 \"You came to end it. Everyone does, at first.\"";
            case 3 -> "\u2620 \"He wanted one more night. You want one more fight. Same wish.\"";
            case 4 -> "\u2622 \"Look closer. The crown fits you.\"";
            default -> "Phase " + newPhase;
        };
        for (ServerPlayer p : bossEvent.getPlayers()) {
            p.displayClientMessage(Component.literal(msg).withStyle(
                newPhase >= 3 ? net.minecraft.ChatFormatting.DARK_RED : net.minecraft.ChatFormatting.DARK_PURPLE,
                net.minecraft.ChatFormatting.BOLD), false);
        }

        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_roar_sovereign"), SoundSource.HOSTILE, 3.0f, 0.5f);
    }

    // ========================================
    // WEIGHTED ATTACK SELECTION (Hybrid Obsidilith + VoidBlossom)
    // ========================================

    private byte chooseAttack() {
        if (shouldDoCrystalShield) { shouldDoCrystalShield = false; return ATTACK_CRYSTAL_SHIELD; }
        if (shouldDoBlossom) { shouldDoBlossom = false; return ATTACK_BLOSSOM; }

        LivingEntity target = getTarget();
        if (target == null) return ATTACK_GRAVITY_PULL;
        double distSq = target.distanceToSqr(this);
        float hp = getHealth() / getMaxHealth();
        int phase = getPhase();

        Map<Byte, Double> w = new LinkedHashMap<>();
        w.put(ATTACK_GRAVITY_PULL, 1.0);
        w.put(ATTACK_REALITY_DISTORT, phase >= 2 ? 0.8 : 0.0);
        w.put(ATTACK_SINGULARITY_WAVE, distSq > 16 ? 0.7 : 0.3);
        w.put(ATTACK_DIMENSIONAL_SPIKE, distSq > 36 ? 0.8 : 0.0);
        w.put(ATTACK_VOID_BURST, distSq < 49 ? 1.0 : 0.0);

        boolean lastWasEH = !moveHistory.isEmpty() && moveHistory.peekLast() == ATTACK_EVENT_HORIZON;
        w.put(ATTACK_EVENT_HORIZON, (phase >= 3 && eventHorizonCooldown <= 0 && !lastWasEH) ? 0.5 : 0.0);

        double total = w.values().stream().mapToDouble(d -> d).sum();
        if (total <= 0) return ATTACK_GRAVITY_PULL;

        double roll = random.nextDouble() * total;
        double cum = 0;
        for (var e : w.entrySet()) {
            cum += e.getValue();
            if (roll <= cum) {
                moveHistory.addLast(e.getKey());
                if (moveHistory.size() > 5) moveHistory.pollFirst();
                return e.getKey();
            }
        }
        return ATTACK_GRAVITY_PULL;
    }

    /** Stage 4: every Sovereign move marks its ground zone first (BossDirector.windup); the hit lands only inside it. */
    public int performAttack(byte attack) {
        return switch (attack) {
            case ATTACK_GRAVITY_PULL -> { setAttackAnim(ANIM_GRAVITY, 40); yield (100 - getPhase() * 15) + tele("gravity_pull", 30, Telegraph.ring(floorHere(), 4, 15), this::performGravityPull); }
            case ATTACK_REALITY_DISTORT -> { setAttackAnim(ANIM_REALITY_DISTORT, 40); yield 80 + tele("reality_distort", 24, Telegraph.circle(aimFloor(), 5), this::performRealityDistort); }
            case ATTACK_SINGULARITY_WAVE -> { setAttackAnim(ANIM_MELEE, 20); yield 80 + tele("singularity_wave", 22, Telegraph.line(floorHere(), aimPoint(), 25, 2), this::performSingularityWave); }
            case ATTACK_DIMENSIONAL_SPIKE -> { setAttackAnim(ANIM_MELEE, 20); yield 80 + tele("dimensional_spike", 18, Telegraph.circle(aimFloor(), 4), this::performDimensionalSpike); }
            case ATTACK_VOID_BURST -> { setAttackAnim(ANIM_MELEE, 20); yield 60 + tele("void_burst", 20, Telegraph.circle(floorHere(), 7), this::performVoidBurst); }
            case ATTACK_CRYSTAL_SHIELD -> { setAttackAnim(ANIM_CRYSTAL_SHIELD, 40); yield 120 + tele("crystal_shield", 30, Telegraph.ring(floorHere(), 9.5, 10.5), this::performCrystalShield); }
            case ATTACK_EVENT_HORIZON -> { setAttackAnim(ANIM_EVENT_HORIZON, 60); eventHorizonCooldown = 600; yield 200 + tele("event_horizon", 40, Telegraph.circle(floorHere(), 20), this::performEventHorizon); }
            case ATTACK_BLOSSOM -> { setAttackAnim(ANIM_EVENT_HORIZON, 60); yield 100 + tele("void_blossom", 30, Telegraph.circle(floorHere(), 3), this::performBlossom); }
            default -> 80;
        };
    }

    /** The zone of the move being resolved right now (the attack bodies read it to pick their victims). */
    private Telegraph zone;

    private int tele(String id, int ticks, Telegraph t, Runnable body) {
        return BossDirector.windup(this, id, ticks, t, () -> { zone = t; try { body.run(); } finally { zone = null; } });
    }

    private Vec3 floorHere() { return Telegraph.floorBelow(level(), position()); }
    private Vec3 aimPoint() { LivingEntity t = getTarget(); return t != null ? t.position() : position().add(getLookAngle().scale(8)); }
    private Vec3 aimFloor() { return Telegraph.floorBelow(level(), aimPoint().add(0, 1, 0)); }

    /** Living things inside the current zone (not the Sovereign, other bosses, or creative/spectator players). */
    private java.util.List<LivingEntity> victims() {
        Telegraph t = zone;
        if (t == null) return java.util.List.of();
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(30, 14, 30),
            e -> e != this && e.isAlive() && !(e instanceof com.matt.quantumfalcon.entity.boss.QFBoss)
                && !(e instanceof Player p && (p.isCreative() || p.isSpectator())) && t.contains(e.position()));
    }

    // ========================================
    // ATTACK IMPLEMENTATIONS
    // ========================================

    private void performGravityPull() {
        setGravityPulling(true);
        if (!(this.level() instanceof ServerLevel sl)) return;
        for (LivingEntity p : victims()) {
            Vec3 pull = position().subtract(p.position()).normalize().scale(0.5);
            p.push(pull.x, pull.y * 0.3, pull.z);
            p.hurtMarked = true;
            p.hurt(damageSources().mobAttack(this), 4.0f + getPhase());
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
        for (int i = 0; i < 360; i += 30) {
            double angle = Math.toRadians(i + tickCount * 5);
            for (int r = 1; r <= 15; r += 2)
                sl.sendParticles(ParticleTypes.PORTAL, getX() + Math.cos(angle) * r, getY() + 0.5, getZ() + Math.sin(angle) * r, 1, 0, 0, 0, 0);
        }
        int phase = getPhase();
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            getX(), getY() + 1.0, getZ(),
            1, 0.0, 0.0, 0.0, 0.0);
        sl.sendParticles(ParticleTypes.FIREWORK,
            getX(), getY() + 1.5, getZ(),
            6 + phase * 3, 0.5, 0.5, 0.5, 0.1);
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_windup_sovereign"), SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    private void performRealityDistort() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 o = zone != null ? zone.origin() : position();
        for (LivingEntity p : victims()) p.hurt(damageSources().mobAttack(this), 8.0f + getPhase() * 2);
        for (int i = 0; i < 4 + getPhase(); i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * 4.5;
            double x = o.x + Math.cos(angle) * dist;
            double z = o.z + Math.sin(angle) * dist;
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, x, o.y, z, 30, 1.5, 0.5, 1.5, 0);
            sl.sendParticles(ParticleTypes.EXPLOSION,
                x, getY() + 0.5, z,
                2 + getPhase(), 0.8, 0.5, 0.8, 0.05);
            sl.sendParticles(ParticleTypes.SCRAPE,
                x, getY() + 0.5, z,
                4 + getPhase() * 2, 0.5, 0.5, 0.5, 0.05);
        }
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("gravity_well"), SoundSource.HOSTILE, 2.0f, 0.8f);
    }

    private void performSingularityWave() {
        if (!(this.level() instanceof ServerLevel sl) || getTarget() == null) return;
        Vec3 dir = zone != null ? zone.dir() : getTarget().position().subtract(position()).normalize();
        Vec3 o = zone != null ? zone.origin() : position();
        for (int i = 1; i <= 25; i++)
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, o.x + dir.x * i, o.y + 0.5, o.z + dir.z * i, 8, 0.5, 1, 0.5, 0);
        for (LivingEntity p : victims()) {
            p.hurt(damageSources().mobAttack(this), 10.0f + getPhase());
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
        }
        Vec3 end = o.add(dir.scale(25));
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            end.x, getY() + 0.5, end.z,
            1, 0.0, 0.0, 0.0, 0.0);
        sl.sendParticles(ParticleTypes.DRAGON_BREATH,
            end.x, getY() + 0.5, end.z,
            20 + getPhase() * 5, 1.0, 0.5, 1.0, 0.02);
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("gravity_well"), SoundSource.HOSTILE, 2.0f, 0.6f);
    }

    private void performDimensionalSpike() {
        if (!(this.level() instanceof ServerLevel sl) || getTarget() == null) return;
        Vec3 tp = zone != null ? zone.origin() : getTarget().position();
        for (LivingEntity p : victims()) p.hurt(damageSources().mobAttack(this), 10.0f + getPhase() * 2);
        for (int i = 0; i < 6 + getPhase(); i++) {
            double ox = (random.nextDouble() - 0.5) * 6;
            double oz = (random.nextDouble() - 0.5) * 6;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, tp.x + ox, tp.y, tp.z + oz, 25, 0.3, 3, 0.3, 0.1);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                tp.x + ox, tp.y + 2.0, tp.z + oz,
                10 + getPhase() * 3, 0.5, 2.0, 0.5, 0.05);
            sl.sendParticles(ParticleTypes.SCRAPE,
                tp.x + ox, tp.y + 0.5, tp.z + oz,
                6, 0.5, 0.5, 0.5, 0.05);
        }
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("entity_sovereign_attack"), SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    private void performVoidBurst() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        for (LivingEntity p : victims()) {
            Vec3 kb = p.position().subtract(position()).normalize().scale(3);
            p.hurt(damageSources().mobAttack(this), 16.0f);
            p.push(kb.x, 1.0, kb.z);
            p.hurtMarked = true;
        }
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            for (int r = 1; r <= 7; r++)
                sl.sendParticles(ParticleTypes.SONIC_BOOM, getX() + Math.cos(angle) * r, getY() + 1, getZ() + Math.sin(angle) * r, 1, 0, 0, 0, 0);
        }
        int phase = getPhase();
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            getX(), getY() + 1.0, getZ(),
            1, 0.0, 0.0, 0.0, 0.0);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
            getX(), getY() + 1.0, getZ(),
            30 + phase * 10, 1.5, 1.5, 1.5, 0.05);
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("gravity_well"), SoundSource.HOSTILE, 3.0f, 0.6f);
    }

    private void performCrystalShield() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        entityData.set(DATA_SHIELDED, true);
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60);
            double x = getX() + Math.cos(angle) * 10;
            double z = getZ() + Math.sin(angle) * 10;
            singularityCrystals.add(BlockPos.containing(x, getY(), z));
            sl.sendParticles(ParticleTypes.END_ROD, x, getY(), z, 60, 0.5, 4, 0.5, 0.05);
        }
        int phase = getPhase();
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            double r = 5.0 + phase * 0.8;
            double px = getX() + Math.cos(angle) * r;
            double pz = getZ() + Math.sin(angle) * r;
            sl.sendParticles(ParticleTypes.END_ROD,
                px, getY() + 1.5, pz,
                1, 0.1, 0.3, 0.1, 0.01);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                px, getY() + 1.5, pz,
                1, 0.1, 0.3, 0.1, 0.01);
        }
        this.heal(getMaxHealth() * 0.03f);
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("singularity_pull"), SoundSource.HOSTILE, 2.0f, 0.3f);
    }

    private void performEventHorizon() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        for (ServerPlayer p : bossEvent.getPlayers())
            p.displayClientMessage(Component.literal("\u2622 EVENT HORIZON — GET OUT NOW!")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.OBFUSCATED), true);

        for (int wave = 1; wave <= 5; wave++) {
            int radius = wave * 4;
            for (int i = 0; i < 36; i++) {
                double angle = Math.toRadians(i * 10);
                sl.sendParticles(ParticleTypes.DRAGON_BREATH,
                    getX() + Math.cos(angle) * radius, getY(), getZ() + Math.sin(angle) * radius, 5, 0.5, 1, 0.5, 0);
            }
        }

        for (LivingEntity p : victims()) {
            double dist = p.distanceTo(this);
            float dmg = (float) Math.max(5, 25 - dist);
            p.hurt(damageSources().mobAttack(this), dmg);
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            Vec3 pull = position().subtract(p.position()).normalize().scale(1.5);
            p.push(pull.x, 0.5, pull.z);
        }
        int phase = getPhase();
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            getX(), getY() + 2.0, getZ(),
            1, 0.0, 0.0, 0.0, 0.0);
        sl.sendParticles(ParticleTypes.FIREWORK,
            getX(), getY() + 3.0, getZ(),
            20 + phase * 10, 2.0, 2.0, 2.0, 0.1);
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_roar_sovereign"), SoundSource.HOSTILE, 3.0f, 0.3f);
    }

    private void performBlossom() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        float healAmt = getMaxHealth() * 0.05f;
        heal(healAmt);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 3, getZ(), 120, 4, 4, 4, 0.1);
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 60, 2, 3, 2, 0.05);

        for (ServerPlayer p : bossEvent.getPlayers())
            p.displayClientMessage(Component.literal("\u2728 The Sovereign absorbs void energy to regenerate!")
                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), false);
        sl.sendParticles(ParticleTypes.ENCHANT,
            getX(), getY() + 2.5, getZ(),
            30 + getPhase() * 10, 1.5, 1.5, 1.5, 0.1);
        this.level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    // ========================================
    // TICK
    // ========================================

    /** Phase 4 (design notes): gravity reverses in 6 s pulses, then returns for 4 s with slow falling. */
    public static final int GRAVITY_CYCLE = 200, GRAVITY_REVERSED = 120;
    /** Test hook: clear phase-transition invulnerability. */
    public void clearInvulnerability() { this.invulnerableTime = 0; ((net.minecraft.world.entity.LivingEntity) this).invulnerableTime = 0; setInvulnerablePhase(false); }

    public boolean isGravityReversed() { return getPhase() >= 4 && (tickCount % GRAVITY_CYCLE) < GRAVITY_REVERSED; }

    private void tickReversedGravity() {
        if (!(level() instanceof ServerLevel sl)) return;
        int t = tickCount % GRAVITY_CYCLE;
        var players = sl.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, getBoundingBox().inflate(40),
                pl -> !pl.isSpectator() && !pl.isCreative());
        if (t == 0) {
            for (var pl : players) pl.displayClientMessage(Component.literal("\u00a75\u00a7lGravity inverts!"), true);
            sl.playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_roar_sovereign"), SoundSource.HOSTILE, 2f, 1.6f);
        }
        if (t < GRAVITY_REVERSED) {
            for (var pl : players) {
                pl.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LEVITATION, 10, 1, false, false));
                if (tickCount % 5 == 0) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pl.getX(), pl.getY(), pl.getZ(), 4, 0.3, 0.1, 0.3, 0.02);
            }
        } else if (t == GRAVITY_REVERSED) {
            for (var pl : players) {
                pl.removeEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
                pl.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 80, 0, false, true));
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        checkPhaseTransition();
        if (!level().isClientSide && getPhase() >= 4) tickReversedGravity();
        if (getNavigation() instanceof BossNavigation bossNav) {
            int p = getPhase();
            bossNav.setSpeedModifier(p >= 4 ? 1.6 : p >= 3 ? 1.35 : p >= 2 ? 1.15 : 1.0);
        }
        if (attackCooldown > 0) attackCooldown--;
        if (eventHorizonCooldown > 0) eventHorizonCooldown--;
        if (invulnerableTime > 0) { invulnerableTime--; if (invulnerableTime <= 0) setInvulnerablePhase(false); }
        if (isGravityPulling() && tickCount % 60 == 0) setGravityPulling(false);
        if (attackAnimTimer > 0) {
            attackAnimTimer--;
            if (attackAnimTimer <= 0) entityData.set(DATA_ATTACK_ANIM, ANIM_NONE);
        }

        singularityCrystals.removeIf(pos -> !pos.closerThan(blockPosition(), 64));
        // Crystal Shield breaks: a player touching a crystal (within 2 blocks) shatters it, and the whole shield lapses after
        // SHIELD_MAX_TICKS. Before this the crystals were particle-only and never cleared, so the first shield milestone left
        // the Sovereign immune for the rest of the fight.
        if (!singularityCrystals.isEmpty() && this.level() instanceof ServerLevel shieldLevel) {
            shatterTouched(shieldLevel, shieldLevel.players());
            if (++shieldTicks >= SHIELD_MAX_TICKS) singularityCrystals.clear();
            else if (tickCount % 10 == 0) for (BlockPos pos : singularityCrystals)
                shieldLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 2, 0.2, 0.01);
        } else shieldTicks = 0;
        entityData.set(DATA_SHIELDED, !singularityCrystals.isEmpty());

        if (getTarget() == null) heal(0.5f);

        if (this.level().isClientSide) {
            int count = getPhase();
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = 1 + random.nextDouble() * 2;
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    getX() + Math.cos(angle) * r, getY() + random.nextDouble() * 4,
                    getZ() + Math.sin(angle) * r, 0, 0.1, 0);
            }
            if (getPhase() >= 3 && tickCount % 2 == 0) {
                double angle = Math.toRadians(tickCount * 3);
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    getX() + Math.cos(angle) * 3, getY() + 2, getZ() + Math.sin(angle) * 3, 0, 0.05, 0);
            }
        }
    }

    // ========================================
    // SOVEREIGN ATTACK GOAL
    // ========================================

    private static class SovereignAttackGoal extends Goal {
        private final SingularitySovereignEntity boss;
        public SovereignAttackGoal(SingularitySovereignEntity boss) { this.boss = boss; }

        @Override
        public boolean canUse() {
            return boss.isAlive() && boss.getTarget() != null && boss.attackCooldown <= 0 && !boss.isInvulnerablePhase();
        }

        @Override
        public void start() {
            byte attack = boss.chooseAttack();
            int cooldown = boss.performAttack(attack);
            boss.attackCooldown = cooldown;
        }

        @Override
        public boolean canContinueToUse() { return false; }
    }

    // ========================================
    // BOSS EVENT + SAVE/LOAD
    // ========================================

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override public void remove(RemovalReason r) { bossEvent.removeAllPlayers(); super.remove(r); }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase());
        tag.putInt("LastMilestone", lastMilestoneIndex);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Phase")) setPhase(tag.getInt("Phase"));
        if (tag.contains("LastMilestone")) lastMilestoneIndex = tag.getInt("LastMilestone");
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {
            handleBossLootOnDeath(sl);
        }
        entityData.set(DATA_ATTACK_ANIM, ANIM_DEAD);
        ArenaMechanics.onBossDeath(this);
        super.die(source);
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("quantumfalcon", "entities/singularity_sovereign"));
    }

    @Override
    protected int getBaseExperienceReward() { return 1000; }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("sovereign"); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("sovereign"); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("sovereign"); }
    @Override public boolean canChangeDimensions(Level from, Level to) { return false; }
    @Override public int getArmorValue() { return getTarget() != null ? super.getArmorValue() : 30; }

    /**
     * Phase-scaled, progression-aware loot logic for Singularity Sovereign.
     */
    private void handleBossLootOnDeath(ServerLevel level) {
        AABB box = new AABB(blockPosition()).inflate(32.0);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, box);
        if (players.isEmpty()) {
            // Nobody in range (e.g. killed remotely): still drop the progression relics at the boss
            spawnAtLocation(new ItemStack(ModItems.SOVEREIGNS_CROWN.get()));
            spawnAtLocation(new ItemStack(ModItems.SINGULARITY_CORE.get()));
            return;
        }

        // Determine top damage dealer
        UUID topId = null;
        float topDamage = 0.0f;
        for (Map.Entry<UUID, Float> entry : damageMemory.entrySet()) {
            if (entry.getValue() > topDamage) {
                topDamage = entry.getValue();
                topId = entry.getKey();
            }
        }

        ServerPlayer topPlayer = null;
        if (topId != null) {
            for (ServerPlayer p : players) {
                if (p.getUUID().equals(topId)) {
                    topPlayer = p;
                    break;
                }
            }
        }
        if (topPlayer == null) {
            topPlayer = players.get(0);
        }

        int phase = getPhase();
        int baseDrops = 8 + (phase - 1) * 4;
        if (baseDrops < 8) baseDrops = 8;
        if (baseDrops > 20) baseDrops = 20;

        RandomSource rng = level.random;

        // Global loot pile at boss position
        for (int i = 0; i < baseDrops; i++) {
            float roll = rng.nextFloat();
            ItemStack stack;
            if (roll < 0.55f) {
                stack = getCommonDrop(rng);
            } else if (roll < 0.9f) {
                stack = getRareDrop(rng, phase);
            } else {
                stack = getLegendaryDrop(rng, phase);
            }
            if (!stack.isEmpty()) {
                spawnAtLocation(stack);
            }
        }

        // Per-player bonus rewards
        for (ServerPlayer player : players) {
            // M5: this read a "quantum_instability" skill that never existed; the bonus now follows the player's real
            // instability (50%+), the same meter the HUD shows.
            int instabilityLevel = com.matt.quantumfalcon.capability.PlayerInstabilityCapability.get(player).getInstabilityPercent() / 10;
            int bonusRare = instabilityLevel >= 5 ? 2 : 0;
            int personalDrops = 0;

            for (int i = 0; i < bonusRare; i++) {
                ItemStack rare = getRareDrop(rng, phase);
                if (!rare.isEmpty()) {
                    player.spawnAtLocation(rare);
                    personalDrops++;
                }
            }

            boolean inEchoRift = level.dimension().location().getPath().toLowerCase().contains("echo");
            if (inEchoRift) {
                ItemStack core = new ItemStack(ModItems.SINGULARITY_CORE.get());
                player.spawnAtLocation(core);
                personalDrops++;
            }

            float dmg = damageMemory.getOrDefault(player.getUUID(), 0.0f);
            player.displayClientMessage(
                    Component.literal("You dealt ")
                            .append(String.format("%.1f", dmg))
                            .append(" damage and earned ")
                            .append(String.valueOf(personalDrops))
                            .append(" bonus rewards."),
                    false
            );
        }

        // Top damage dealer guaranteed legendary rewards
        if (topPlayer != null) {
            topPlayer.spawnAtLocation(new ItemStack(ModItems.SINGULARITY_CORE.get()));
            topPlayer.spawnAtLocation(new ItemStack(ModItems.SOVEREIGNS_CROWN.get()));
            topPlayer.displayClientMessage(
                    Component.literal("You were the top damage dealer and claimed the Singularity Core!"),
                    false
            );
        }
    }

    private ItemStack getCommonDrop(RandomSource rng) {
        int pick = rng.nextInt(6);
        return switch (pick) {
            case 0 -> new ItemStack(ModItems.QUANTUM_BERRY.get(), 2 + rng.nextInt(3));
            case 1 -> new ItemStack(ModItems.FALCON_TREAT.get(), 1 + rng.nextInt(2));
            case 2 -> new ItemStack(ModItems.RIFT_SHARD.get(), 1 + rng.nextInt(3));
            case 3 -> new ItemStack(ModItems.ECHO_SHARD.get(), 1 + rng.nextInt(2));
            case 4 -> new ItemStack(ModItems.VOID_SHARD.get(), 1 + rng.nextInt(2));
            default -> new ItemStack(ModItems.QUANTUM_SHARD.get(), 1 + rng.nextInt(3));
        };
    }

    private ItemStack getRareDrop(RandomSource rng, int phase) {
        int pick = rng.nextInt(8);
        int count = 1 + rng.nextInt(1 + Math.max(0, phase - 1));
        return switch (pick) {
            case 0 -> new ItemStack(ModItems.QUANTUM_CRYSTAL.get(), count);
            case 1 -> new ItemStack(ModItems.QUANTUM_SHARD.get(), count);
            case 2 -> new ItemStack(ModItems.RIFT_SHARD.get(), count);
            case 3 -> new ItemStack(ModItems.ECHO_SHARD.get(), count);
            case 4 -> new ItemStack(ModItems.VOID_SHARD.get(), count);
            case 5 -> new ItemStack(ModItems.AURORA_CRYSTAL.get(), count);
            case 6 -> new ItemStack(ModItems.MAGENTA_GEM.get(), count);
            default -> new ItemStack(ModItems.QUANTUM_SHARD.get(), count);
        };
    }

    private ItemStack getLegendaryDrop(RandomSource rng, int phase) {
        int pick = rng.nextInt(8);
        return switch (pick) {
            case 0 -> new ItemStack(ModItems.OVERSEERS_EYE.get());
            case 1 -> new ItemStack(ModItems.WARDENS_FRAGMENT.get());
            case 2 -> new ItemStack(ModItems.TWIN_ESSENCE.get());
            case 3 -> new ItemStack(ModItems.SOVEREIGNS_CROWN.get());
            case 4 -> new ItemStack(ModItems.SINGULARITY_CORE.get());
            case 5 -> new ItemStack(ModItems.QUANTUM_GRAPPLE.get());
            case 6 -> new ItemStack(ModItems.QUANTUM_STABILIZER.get());
            default -> new ItemStack(ModItems.QUANTUM_CORE.get());
        };
    }

    // ========================================
    // GECKOLIB
    // ========================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { controllers.add(BossDirector.presentation(this, "singularity_sovereign"));
        controllers.add(new AnimationController<>(this, "main_controller", 5, event -> {
            byte anim = this.getAttackAnim();

            if (anim == ANIM_DEAD)
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.singularity_sovereign.death"));

            if (anim != ANIM_NONE) {
                String animName = switch (anim) {
                    case ANIM_GRAVITY -> "animation.singularity_sovereign.gravity_pull";
                    case ANIM_EVENT_HORIZON -> "animation.singularity_sovereign.event_horizon";
                    case ANIM_REALITY_DISTORT -> "animation.singularity_sovereign.reality_distort";
                    case ANIM_CRYSTAL_SHIELD -> "animation.singularity_sovereign.crystal_shield";
                    default -> "animation.singularity_sovereign.attack";
                };
                return event.setAndContinue(RawAnimation.begin().thenPlay(animName));
            }

            if (event.isMoving())
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.singularity_sovereign.walk"));

            int phase = this.getPhase();
            return switch (phase) {
                case 1 -> event.setAndContinue(RawAnimation.begin().thenLoop("animation.singularity_sovereign.phase1"));
                case 2 -> event.setAndContinue(RawAnimation.begin().thenLoop("animation.singularity_sovereign.phase2"));
                case 3 -> event.setAndContinue(RawAnimation.begin().thenLoop("animation.singularity_sovereign.phase3"));
                case 4 -> event.setAndContinue(RawAnimation.begin().thenLoop("animation.singularity_sovereign.phase4"));
                default -> event.setAndContinue(RawAnimation.begin().thenLoop("animation.singularity_sovereign.idle"));
            };
        }));

        controllers.add(new AnimationController<>(this, "wing_controller", 0, event ->
            event.setAndContinue(RawAnimation.begin().thenLoop("animation.singularity_sovereign.wing_flap"))
        ));

        controllers.add(new AnimationController<>(this, "core_controller", 0, event ->
            event.setAndContinue(RawAnimation.begin().thenLoop("animation.singularity_sovereign.core_pulse"))
        ));

        controllers.add(new AnimationController<>(this, "shield_controller", 0, event ->
            event.setAndContinue(RawAnimation.begin().thenLoop("animation.singularity_sovereign.shield_orbit"))
        ));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.step("sovereign"), 0.6f, 1f); }
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity target) { boolean landed = super.doHurtTarget(target); if (landed) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "sovereign", 1f); return landed; }
}
