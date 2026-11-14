package com.matt.quantumfalcon.entity.boss;

import com.matt.quantumfalcon.entity.ai.BossNavigation;
import com.matt.quantumfalcon.registry.ModEntities;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
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
import net.minecraft.world.DifficultyInstance;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Entangled Twins - Two entities (cyan + magenta) sharing one geo, bound by quantum tether.
 * 3 phases, 7 attacks with synced animations. Partner spawns automatically; when one dies the other enrages (sacrifice_link).
 */
public class EntangledTwinsEntity extends Monster implements GeoEntity, QFBoss {
    // bosses never ride (minecarts hauled them out of their arenas) and never take portals
    @Override protected boolean canRide(net.minecraft.world.entity.Entity vehicle) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public int qfPhase() { return getPhase(); }
    @Override public net.minecraft.server.level.ServerBossEvent qfBossBar() { return (isCyanTwin() ? bossEvent : null); }

    private static final EntityDataAccessor<Boolean> DATA_CYAN_TWIN =
        SynchedEntityData.defineId(EntangledTwinsEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TWIN_ID =
        SynchedEntityData.defineId(EntangledTwinsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_ATTACK_ANIM =
        SynchedEntityData.defineId(EntangledTwinsEntity.class, EntityDataSerializers.BYTE);

    public static final byte ANIM_NONE = 0;
    public static final byte ANIM_MIRROR_SLASH = 1;
    public static final byte ANIM_TETHER_LASH = 2;
    public static final byte ANIM_RESONANCE_PULSE = 3;
    public static final byte ANIM_SWAP = 4;
    public static final byte ANIM_PETAL_BARRAGE = 5;
    public static final byte ANIM_SACRIFICE = 6;
    public static final byte ANIM_PHASE_TRANSITION = 7;
    public static final byte ANIM_DEAD = 8;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(
        Component.translatable("entity.quantumfalcon.entangled_twins"),
        BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_6
    );

    @Nullable private UUID twinUUID;
    @Nullable private EntangledTwinsEntity cachedTwin;
    private int attackAnimTimer = 0;
    private int attackCooldown = 0;
    private int phase = 1;
    private boolean shouldDoBlossom = false;
    private int lastMilestoneIndex = -1;
    private boolean hasTriggeredSacrificeLink = false;
    private static final List<Float> HP_MILESTONES = List.of(0.0f, 0.25f, 0.50f, 0.75f, 1.0f);
    private final Deque<Byte> moveHistory = new ArrayDeque<>(4);
    private final Map<UUID, Float> damageMemory = new LinkedHashMap<>();
    private int syncDamageCooldown = 0;
    private int swapCooldown = 0;

    public EntangledTwinsEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 350;
        this.setPersistenceRequired();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new BossNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TwinsAttackGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(3, new StayNearTwinGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 350.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CYAN_TWIN, true);
        builder.define(DATA_ATTACK_ANIM, ANIM_NONE);
        builder.define(DATA_TWIN_ID, -1);
    }

    public boolean isCyanTwin() { return entityData.get(DATA_CYAN_TWIN); }
    public void setCyanTwin(boolean cyan) { entityData.set(DATA_CYAN_TWIN, cyan); }
    public byte getAttackAnim() { return entityData.get(DATA_ATTACK_ANIM); }
    public int getPhase() { return phase; }

    /** Called when first twin is added to world: spawn partner 4 blocks away and link. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
        if (level instanceof ServerLevel sl && twinUUID == null && reason != MobSpawnType.MOB_SUMMONED) {
            EntangledTwinsEntity partner = ModEntities.ENTANGLED_TWINS.get().create(sl);
            if (partner != null) {
                Vec3 pos = position();
                partner.moveTo(pos.x + 4, pos.y, pos.z, random.nextFloat() * 360f, 0f);
                // pick a free spot so the partner doesn't spawn inside an arena pillar
                double[][] offs = {{4, 0}, {-4, 0}, {0, 4}, {0, -4}, {6, 0}, {-6, 0}, {0, 6}, {0, -6}};
                for (double[] o : offs) {
                    partner.moveTo(pos.x + o[0], pos.y, pos.z + o[1], partner.getYRot(), 0f);
                    if (sl.noCollision(partner)) break;
                }
                partner.setCyanTwin(false);
                this.setCyanTwin(true);
                this.setTwin(partner);
                partner.setTwin(this);
                sl.addFreshEntity(partner);
            }
        }
        return d;
    }

    public void setTwin(EntangledTwinsEntity twin) {
        this.twinUUID = twin.getUUID();
        this.cachedTwin = twin;
        this.entityData.set(DATA_TWIN_ID, twin.getId());
    }

    @Nullable
    public EntangledTwinsEntity getTwin() {
        if (cachedTwin != null && cachedTwin.isAlive()) return cachedTwin;
        if (twinUUID != null && this.level() instanceof ServerLevel sl) {
            Entity e = sl.getEntity(twinUUID);
            if (e instanceof EntangledTwinsEntity t) { cachedTwin = t; return t; }
        }
        return null;
    }

    public boolean hasTwin() { return getTwin() != null; }

    /** client: partner resolved from the synced entity id (for the tether beam) */
    @Nullable
    public EntangledTwinsEntity clientTwin() {
        int id = entityData.get(DATA_TWIN_ID);
        return id >= 0 && level().getEntity(id) instanceof EntangledTwinsEntity t ? t : null;
    }

    public void triggerAttackAnim(String name, int ticks) {
        this.attackAnimTimer = ticks;
        byte anim = nameToByte(name);
        entityData.set(DATA_ATTACK_ANIM, anim);
    }

    private static byte nameToByte(String name) {
        return switch (name) {
            case "MIRROR_SLASH" -> ANIM_MIRROR_SLASH;
            case "TETHER_LASH" -> ANIM_TETHER_LASH;
            case "RESONANCE_PULSE" -> ANIM_RESONANCE_PULSE;
            case "SWAP" -> ANIM_SWAP;
            case "PETAL_BARRAGE" -> ANIM_PETAL_BARRAGE;
            case "SACRIFICE" -> ANIM_SACRIFICE;
            case "PHASE_TRANSITION" -> ANIM_PHASE_TRANSITION;
            default -> ANIM_NONE;
        };
    }

    public void triggerMirrorSlash() { triggerAttackAnim("MIRROR_SLASH", 20); }
    public void triggerTetherLash() { triggerAttackAnim("TETHER_LASH", 24); }
    public void triggerResonancePulse() { triggerAttackAnim("RESONANCE_PULSE", 30); }
    public void triggerSwap() { triggerAttackAnim("SWAP", 16); }
    public void triggerPetalBarrage() { triggerAttackAnim("PETAL_BARRAGE", 30); }
    public void triggerSacrificeLink() { triggerAttackAnim("SACRIFICE", 60); }
    public void triggerPhaseTransition() { triggerAttackAnim("PHASE_TRANSITION", 40); }

    public void syncAttackWithPartner(String attackName, int ticks) {
        EntangledTwinsEntity partner = getTwin();
        if (partner != null && partner.attackAnimTimer <= 0)
            partner.triggerAttackAnim(attackName, ticks);
    }

    private float getTotalHealthPercent() {
        float total = getHealth(), max = getMaxHealth();
        EntangledTwinsEntity twin = getTwin();
        if (twin != null) { total += twin.getHealth(); max += twin.getMaxHealth(); }
        return total / max;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && syncDamageCooldown <= 0) {
            EntangledTwinsEntity twin = getTwin();
            if (twin != null && twin.isAlive()) {
                syncDamageCooldown = 10;
                twin.syncDamageCooldown = 10;
                twin.hurt(source, amount * 0.5f);
                if (this.level() instanceof ServerLevel sl) {
                    Vec3 s = position().add(0, 1, 0), e = twin.position().add(0, 1, 0);
                    Vec3 dir = e.subtract(s).normalize();
                    double dist = s.distanceTo(e);
                    for (int i = 0; i < 10; i++) {
                        double t = i / 10.0;
                        sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                            s.x + dir.x * t * dist, s.y + dir.y * t * dist, s.z + dir.z * t * dist, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
        if (result && source.getEntity() instanceof Player p) {
            damageMemory.merge(p.getUUID(), amount, Float::sum);
            trySwitchTarget();
        }
        checkPhaseTransition();
        checkMilestone();
        return result;
    }

    private void checkMilestone() {
        float hp = getTotalHealthPercent();
        for (int i = 0; i < HP_MILESTONES.size(); i++) {
            if (hp <= HP_MILESTONES.get(i) && i > lastMilestoneIndex) {
                lastMilestoneIndex = i;
                shouldDoBlossom = true;
                break;
            }
        }
    }

    private void trySwitchTarget() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        UUID top = damageMemory.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        if (top != null) {
            Player p = sl.getPlayerByUUID(top);
            if (p != null && p.isAlive() && p.distanceToSqr(this) < 1600) setTarget(p);
        }
    }

    private void checkPhaseTransition() {
        if (isDeadOrDying()) return;   // a killing blow crosses every threshold; don't announce a phase after the defeat line
        float hp = getTotalHealthPercent();
        if (hp <= 0.4f && phase < 3) enterPhase(3);
        else if (hp <= 0.7f && phase < 2) enterPhase(2);
    }

    private void enterPhase(int newPhase) {
        this.phase = newPhase;
        ArenaMechanics.updateBossPhase(this.blockPosition(), newPhase);
        triggerPhaseTransition();
        syncAttackWithPartner("PHASE_TRANSITION", 40);
        broadcastMessage(Component.literal("\u26A1 The Twins enter Phase " + newPhase + "!").withStyle(net.minecraft.ChatFormatting.GREEN, net.minecraft.ChatFormatting.BOLD));
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 50, 2, 2, 2, 0.1);
            if (newPhase == 2) swapPositionsWithTwin();
            if (newPhase == 3) {
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 1));
                EntangledTwinsEntity twin = getTwin();
                if (twin != null) twin.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 1));
                broadcastMessage(Component.literal("\u2620 The Twins reach for each other. The beam burns brighter.").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
            }
        }
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_roar_twins"), SoundSource.HOSTILE, 1.5f, 1.2f);
    }

    private void broadcastMessage(Component msg) {
        for (ServerPlayer p : bossEvent.getPlayers()) p.displayClientMessage(msg, false);
    }

    // ----- Attack choice (map to new design: mirror_slash, tether_lash, resonance_pulse, swap, petal_barrage, blossom) -----
    private static final byte ATTACK_MIRROR_SLASH = 1;
    private static final byte ATTACK_TETHER_LASH = 2;
    private static final byte ATTACK_RESONANCE_PULSE = 3;
    private static final byte ATTACK_SWAP = 4;
    private static final byte ATTACK_PETAL_BARRAGE = 5;
    private static final byte ATTACK_BLOSSOM = 6;

    private byte chooseAttack() {
        if (shouldDoBlossom) { shouldDoBlossom = false; return ATTACK_BLOSSOM; }
        LivingEntity target = getTarget();
        if (target == null) return ATTACK_MIRROR_SLASH;
        double distSq = target.distanceToSqr(this);
        float hp = getTotalHealthPercent();
        Map<Byte, Double> w = new LinkedHashMap<>();
        w.put(ATTACK_MIRROR_SLASH, 1.0);
        w.put(ATTACK_TETHER_LASH, 0.9);
        w.put(ATTACK_RESONANCE_PULSE, phase >= 2 ? 0.8 : 0.3);
        w.put(ATTACK_SWAP, (phase >= 2 && swapCooldown <= 0 && hasTwin()) ? 0.7 : 0.0);
        w.put(ATTACK_PETAL_BARRAGE, hp < 0.6f ? 0.8 : 0.2);
        double total = w.values().stream().mapToDouble(d -> d).sum();
        if (total <= 0) return ATTACK_MIRROR_SLASH;
        double roll = random.nextDouble() * total;
        double cum = 0;
        for (var e : w.entrySet()) {
            cum += e.getValue();
            if (roll <= cum) {
                moveHistory.addLast(e.getKey());
                if (moveHistory.size() > 4) moveHistory.pollFirst();
                return e.getKey();
            }
        }
        return ATTACK_MIRROR_SLASH;
    }

    /** Stage 4: every Twins move marks its ground zone first (BossDirector.windup) and then hits only what is inside it. */
    public int performAttack(byte attack) {
        switch (attack) {
            case ATTACK_MIRROR_SLASH -> { triggerMirrorSlash(); syncAttackWithPartner("MIRROR_SLASH", 20); return 40 + mirrorSlash(); }
            case ATTACK_TETHER_LASH -> { triggerTetherLash(); return 50 + tetherLash(); }
            case ATTACK_RESONANCE_PULSE -> { triggerResonancePulse(); syncAttackWithPartner("RESONANCE_PULSE", 30); return 60 + resonancePulse(); }
            case ATTACK_SWAP -> {
                triggerSwap();
                syncAttackWithPartner("SWAP", 16);
                swapCooldown = 300;
                EntangledTwinsEntity t = getTwin();
                if (t != null) t.swapCooldown = 300;
                return 80 + swap();
            }
            case ATTACK_PETAL_BARRAGE -> { triggerPetalBarrage(); syncAttackWithPartner("PETAL_BARRAGE", 30); return 55 + petalBarrage(); }
            case ATTACK_BLOSSOM -> { triggerResonancePulse(); syncAttackWithPartner("RESONANCE_PULSE", 30); return 120 + blossom(); }
            default -> { return 50; }
        }
    }

    public static final byte MOVE_MIRROR_SLASH = ATTACK_MIRROR_SLASH, MOVE_TETHER_LASH = ATTACK_TETHER_LASH, MOVE_RESONANCE_PULSE = ATTACK_RESONANCE_PULSE,
            MOVE_SWAP = ATTACK_SWAP, MOVE_PETAL_BARRAGE = ATTACK_PETAL_BARRAGE, MOVE_BLOSSOM = ATTACK_BLOSSOM;

    private Vec3 floorHere() { return Telegraph.floorBelow(level(), position()); }

    private Vec3 aimPoint() {
        LivingEntity t = getTarget();
        return t != null ? t.position() : position().add(getLookAngle().scale(6));
    }

    private java.util.List<LivingEntity> victims(Telegraph t) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(30, 12, 30),
            e -> e != this && e.isAlive() && !(e instanceof EntangledTwinsEntity)
                && !(e instanceof Player p && (p.isCreative() || p.isSpectator())) && t.contains(e.position()));
    }

    /** Mirror Slash: a 70-degree, 7-block cone toward where the target stood when the wind-up began. */
    private int mirrorSlash() {
        Telegraph t = Telegraph.cone(floorHere(), aimPoint(), 35, 7);
        return BossDirector.windup(this, "mirror_slash", 16, t, () -> {
            if (!(level() instanceof ServerLevel sl)) return;
            for (LivingEntity e : victims(t)) {
                e.hurt(damageSources().mobAttack(this), 12f + phase * 2);
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, e.getX(), e.getY() + 1, e.getZ(), 15, 0.3, 0.5, 0.3, 0);
            }
            Vec3 d = t.dir();
            for (int i = 1; i <= 6; i++) sl.sendParticles(ParticleTypes.SWEEP_ATTACK, t.origin().x + d.x * i, t.origin().y + 1, t.origin().z + d.z * i, 1, 0, 0, 0, 0);
            level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("entity_twins_attack"), SoundSource.HOSTILE, 1.2f, 0.8f);
        });
    }

    /** Tether Lash: a 15-block, 3-wide line toward the locked aim. */
    private int tetherLash() {
        Telegraph t = Telegraph.line(floorHere(), aimPoint(), 15, 1.5);
        return BossDirector.windup(this, "tether_lash", 20, t, () -> {
            if (!(level() instanceof ServerLevel sl)) return;
            Vec3 d = t.dir();
            for (int i = 1; i <= 10; i++)
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, t.origin().x + d.x * i * 1.5, t.origin().y + 1, t.origin().z + d.z * i * 1.5, 3, 0.1, 0.2, 0.1, 0);
            for (LivingEntity e : victims(t)) e.hurt(damageSources().mobAttack(this), 10f);
            level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_windup_twins"), SoundSource.HOSTILE, 1.0f, 1.5f);
        });
    }

    /** Resonance Pulse: everything within 6 blocks of the twin is hit and slowed. */
    private int resonancePulse() {
        Telegraph t = Telegraph.circle(floorHere(), 6);
        return BossDirector.windup(this, "resonance_pulse", 24, t, () -> {
            if (!(level() instanceof ServerLevel sl)) return;
            for (LivingEntity e : victims(t)) {
                e.hurt(damageSources().mobAttack(this), 8f + phase * 2);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 40, 2, 2, 2, 0.05);
            level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("ability_entanglement"), SoundSource.HOSTILE, 1.2f, 1.0f);
        });
    }

    /** Petal Barrage: petals rain on a 4-block circle around where the target stood. */
    private int petalBarrage() {
        Vec3 aim = aimPoint();
        Telegraph t = Telegraph.circle(Telegraph.floorBelow(level(), aim.add(0, 1, 0)), 4);
        return BossDirector.windup(this, "petal_barrage", 22, t, () -> {
            if (!(level() instanceof ServerLevel sl)) return;
            for (int i = 0; i < 10; i++) {
                double ang = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * 3.6;
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, t.origin().x + Math.cos(ang) * r, t.origin().y + 3, t.origin().z + Math.sin(ang) * r, 8, 0.2, 1.5, 0.2, 0.02);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, t.origin().x + Math.cos(ang) * r, t.origin().y + 0.2, t.origin().z + Math.sin(ang) * r, 3, 0.1, 0.1, 0.1, 0.02);
            }
            for (LivingEntity e : victims(t)) e.hurt(damageSources().mobAttack(this), 7f + phase);
            level().playSound(null, net.minecraft.core.BlockPos.containing(t.origin()), com.matt.quantumfalcon.entity.util.QFMobSounds.get("boss_windup_twins"), SoundSource.HOSTILE, 1.2f, 1.0f);
        });
    }

    /** Swap: the twins trade places; the landing spot (the partner's position) is marked, and the arrival shockwave hits it. */
    private int swap() {
        EntangledTwinsEntity twin = getTwin();
        Vec3 land = twin != null ? Telegraph.floorBelow(level(), twin.position()) : floorHere();
        Telegraph t = Telegraph.circle(land, 2.5);
        return BossDirector.windup(this, "swap", 16, t, () -> {
            swapPositionsWithTwin();
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
                for (LivingEntity e : victims(t)) { e.hurt(damageSources().mobAttack(this), 5f); e.push((e.getX() - getX()) * 0.3, 0.3, (e.getZ() - getZ()) * 0.3); }
            }
        });
    }

    /** Quantum Blossom: both twins heal 8% after a 30-tick channel; the 3-block circle marks the channelling twin. */
    private int blossom() {
        Telegraph t = Telegraph.circle(floorHere(), 3);
        return BossDirector.windup(this, "quantum_blossom", 30, t, this::performQuantumBlossom);
    }

    private void performQuantumBlossom() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        float healAmt = getMaxHealth() * 0.08f;
        heal(healAmt);
        EntangledTwinsEntity twin = getTwin();
        if (twin != null) twin.heal(healAmt);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 2, getZ(), 80, 3, 3, 3, 0.1);
        if (twin != null) sl.sendParticles(ParticleTypes.CHERRY_LEAVES, twin.getX(), twin.getY() + 2, twin.getZ(), 80, 3, 3, 3, 0.1);
        broadcastMessage(Component.literal("\u2728 The Twins channel quantum energy to heal!").withStyle(net.minecraft.ChatFormatting.GREEN));
        this.level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2.0f, 0.8f);
    }


    public void swapPositionsWithTwin() {
        EntangledTwinsEntity twin = getTwin();
        if (twin == null) return;
        Vec3 myPos = position(), twinPos = twin.position();
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, myPos.x, myPos.y + 1, myPos.z, 30, 0.5, 1, 0.5, 0.2);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, twinPos.x, twinPos.y + 1, twinPos.z, 30, 0.5, 1, 0.5, 0.2);
        }
        teleportTo(twinPos.x, twinPos.y, twinPos.z);
        twin.teleportTo(myPos.x, myPos.y, myPos.z);
        this.level().playSound(null, blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("ability_entanglement"), SoundSource.HOSTILE, 1.0f, 0.8f);
        twin.level().playSound(null, twin.blockPosition(), com.matt.quantumfalcon.entity.util.QFMobSounds.get("ability_entanglement"), SoundSource.HOSTILE, 1.0f, 1.2f);
    }

    @Override
    public void die(DamageSource source) {
        entityData.set(DATA_ATTACK_ANIM, ANIM_DEAD);
        EntangledTwinsEntity partner = getTwin();
        if (partner == null || !partner.isAlive()) ArenaMechanics.onBossDeath(this); // victory only when both twins fall
        super.die(source);
    }

    /** Quantum tether (design notes): purple particle beam between the twins that burns players who cross it. */
    public static final double TETHER_HIT_RADIUS = 1.1;
    private void tickTether() {
        EntangledTwinsEntity twin = getTwin();
        if (twin == null || !twin.isAlive() || !(level() instanceof ServerLevel sl)) return;
        Vec3 a = position().add(0, getBbHeight() * 0.6, 0), b = twin.position().add(0, twin.getBbHeight() * 0.6, 0);
        double len = a.distanceTo(b);
        if (len < 0.5 || len > 48) return;
        // the beam itself is drawn by EntangledTwinsRenderer (no particle chain: ART_STYLE.md, keep the models readable)
        if (tickCount % 10 != 0) return;
        for (Player pl : sl.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(a, b).inflate(2), pl -> !pl.isSpectator() && !pl.isCreative())) {
            if (distanceToSegment(pl.position().add(0, pl.getBbHeight() * 0.5, 0), a, b) <= TETHER_HIT_RADIUS) {
                pl.hurt(damageSources().indirectMagic(this, twin), 4.0f);
                Vec3 push = pl.position().subtract(a.lerp(b, 0.5)).multiply(1, 0, 1).normalize().scale(0.8);
                pl.push(push.x, 0.35, push.z);
                pl.hurtMarked = true;
            }
        }
    }

    public static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = net.minecraft.util.Mth.clamp(p.subtract(a).dot(ab) / ab.lengthSqr(), 0, 1);
        return p.distanceTo(a.add(ab.scale(t)));
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide && tickCount % 20 == 0) {
            EntangledTwinsEntity t = getTwin();
            entityData.set(DATA_TWIN_ID, t == null ? -1 : t.getId());
        }
        if (!level().isClientSide && isCyanTwin()) tickTether();
        super.aiStep();
        if (getNavigation() instanceof BossNavigation bossNav)
            bossNav.setSpeedModifier(phase >= 3 ? 1.5 : phase >= 2 ? 1.2 : 1.0);
        if (isCyanTwin()) {
            float total = getHealth(), max = getMaxHealth();
            EntangledTwinsEntity twin = getTwin();
            if (twin != null) { total += twin.getHealth(); max += twin.getMaxHealth(); }
            bossEvent.setProgress(total / max);
        }
        if (attackAnimTimer > 0) {
            attackAnimTimer--;
            if (attackAnimTimer <= 0) entityData.set(DATA_ATTACK_ANIM, ANIM_NONE);
        }
        if (attackCooldown > 0) attackCooldown--;
        if (syncDamageCooldown > 0) syncDamageCooldown--;
        if (swapCooldown > 0) swapCooldown--;

        EntangledTwinsEntity twin = getTwin();
        if (twin != null && !twin.isAlive() && !hasTriggeredSacrificeLink && !isDeadOrDying()) {
            hasTriggeredSacrificeLink = true;
            triggerSacrificeLink();
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 1));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12000, 0));
            broadcastMessage(Component.literal("\u2620 One twin falls. The other screams with both their voices.").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
        }

        if (this.level().isClientSide && tickCount % 5 == 0) {
            this.level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 1.5, getY() + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5) * 1.5, 0, 0.05, 0);
        }
    }

    private static class TwinsAttackGoal extends Goal {
        private final EntangledTwinsEntity boss;
        TwinsAttackGoal(EntangledTwinsEntity boss) { this.boss = boss; }

        @Override
        public boolean canUse() {
            return boss.isAlive() && boss.getTarget() != null && boss.attackCooldown <= 0;
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

    private static class StayNearTwinGoal extends Goal {
        private final EntangledTwinsEntity entity;
        StayNearTwinGoal(EntangledTwinsEntity e) { this.entity = e; }

        private static final double TETHER_RANGE_SQR = 12.0 * 12.0; // stay within 12 blocks

        @Override
        public boolean canUse() {
            EntangledTwinsEntity twin = entity.getTwin();
            return twin != null && twin.isAlive() && entity.distanceToSqr(twin) > TETHER_RANGE_SQR;
        }

        @Override
        public void tick() {
            EntangledTwinsEntity twin = entity.getTwin();
            if (twin != null) entity.getNavigation().moveTo(twin, 1.2);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); if (isCyanTwin()) bossEvent.addPlayer(p); }
    @Override
    public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); if (isCyanTwin()) bossEvent.removePlayer(p); }
    @Override
    public void remove(RemovalReason r) { bossEvent.removeAllPlayers(); super.remove(r); }
    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("CyanTwin", isCyanTwin());
        tag.putInt("Phase", phase);
        tag.putInt("LastMilestone", lastMilestoneIndex);
        tag.putBoolean("SacrificeTriggered", hasTriggeredSacrificeLink);
        if (twinUUID != null) tag.putUUID("TwinUUID", twinUUID);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCyanTwin(tag.getBoolean("CyanTwin"));
        if (tag.contains("Phase")) phase = tag.getInt("Phase");
        if (tag.contains("LastMilestone")) lastMilestoneIndex = tag.getInt("LastMilestone");
        if (tag.contains("SacrificeTriggered")) hasTriggeredSacrificeLink = tag.getBoolean("SacrificeTriggered");
        if (tag.hasUUID("TwinUUID")) twinUUID = tag.getUUID("TwinUUID");
        if (isCyanTwin() && hasCustomName()) bossEvent.setName(getDisplayName());
    }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient("twins"); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt("twins"); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death("twins"); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { controllers.add(BossDirector.presentation(this, "entangled_twins"));
        controllers.add(new AnimationController<>(this, "main_controller", 5, event -> {
            byte anim = this.getAttackAnim();
            if (anim == ANIM_DEAD)
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.entangled_twins.death"));
            if (anim != ANIM_NONE) {
                String name = switch (anim) {
                    case ANIM_MIRROR_SLASH -> "animation.entangled_twins.mirror_slash";
                    case ANIM_TETHER_LASH -> "animation.entangled_twins.tether_lash";
                    case ANIM_RESONANCE_PULSE -> "animation.entangled_twins.resonance_pulse";
                    case ANIM_SWAP -> "animation.entangled_twins.entanglement_swap";
                    case ANIM_PETAL_BARRAGE -> "animation.entangled_twins.petal_barrage";
                    case ANIM_SACRIFICE -> "animation.entangled_twins.sacrifice_link";
                    case ANIM_PHASE_TRANSITION -> "animation.entangled_twins.phase_transition";
                    default -> "animation.entangled_twins.idle";
                };
                return event.setAndContinue(RawAnimation.begin().thenPlay(name));
            }
            if (event.isMoving()) return event.setAndContinue(RawAnimation.begin().thenLoop("animation.entangled_twins.walk"));
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.entangled_twins.idle"));
        }));
        controllers.add(new AnimationController<>(this, "shards", 0, e -> e.setAndContinue(RawAnimation.begin().thenLoop("animation.entangled_twins.shard_orbit"))));
        controllers.add(new AnimationController<>(this, "core", 0, e -> e.setAndContinue(RawAnimation.begin().thenLoop("animation.entangled_twins.core_spin"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.step("twins"), 0.6f, 1f); }
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity target) { boolean landed = super.doHurtTarget(target); if (landed) com.matt.quantumfalcon.entity.util.QFMobSounds.attackLanded(this, "twins", 1f); return landed; }
}
