package com.matt.quantumfalcon.entity.boss;

import com.matt.quantumfalcon.entity.util.QFMobSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Skywarden Irel, the flightmaster of Eastwatch Aerie (lore/SIDE_DUNGEONS.md 1): an echo frozen mid-command. Her intro
 * (on top of the shared boss intro: title, cutscene camera, frozen boss, protected players) is her own: she raises her arm,
 * every echo-falcon in the dome wheels east with her, then she lowers it and notices you, a late stitch.
 * <p>Moves (all telegraphed through {@link BossDirector#windup}, so stepping out of the zone dodges them):
 * <ul>
 *   <li><b>Stoop</b>: marks the ground under you; an echo-falcon dives onto the mark.</li>
 *   <li><b>Hood</b>: a cone in front of her; the falconer's hood drops over your eyes (Blindness, Slowness).</li>
 *   <li><b>East, always east</b> (below half health): a lane running east from her; the wind of a hundred wings throws you east.</li>
 * </ul>
 * Tier-2 numbers (the Dungeon Guard scales HP x3.5 and damage x1.3 for a tier-2 boss): 80 base HP, 5 base damage.
 */
public class SkywardenIrelEntity extends Monster implements QFBoss {
    public static final int AMBER = 0xFFB43A;
    private static final EntityDataAccessor<Boolean> COMMANDING = SynchedEntityData.defineId(SkywardenIrelEntity.class, EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.quantumfalcon.skywarden_irel"), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_6);
    private int stoopTimer = 80, hoodTimer = 140, eastTimer = 120;
    /** Intro script clock: -1 not started, 0..INTRO_TICKS running, then done (saved, so a reload never replays it). */
    private int introClock = -1;
    /** Recorded moves for /qftest: "stoop", "hood", "east", "intro:<step>". */
    public final List<String> log = new ArrayList<>();

    public SkywardenIrelEntity(EntityType<? extends Monster> type, Level level) { super(type, level); xpReward = 120; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.ATTACK_DAMAGE, 5).add(Attributes.MOVEMENT_SPEED, 0.31)
            .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ARMOR, 6).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(COMMANDING, true); }
    /** Arm raised: frozen mid-command before the fight, and while she calls a move. Read by the renderer. */
    public boolean isCommanding() { return entityData.get(COMMANDING); }
    void setCommanding(boolean v) { entityData.set(COMMANDING, v); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public int qfPhase() { return getHealth() < getMaxHealth() / 2 ? 2 : 1; }
    @Override public ServerBossEvent qfBossBar() { return bossEvent; }
    // bosses never ride and never take portals
    @Override protected boolean canRide(Entity vehicle) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount % 3 == 0) level().addParticle(new DustParticleOptions(new Vector3f(1f, 0.71f, 0.23f), 0.8f), getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0, 0.02, 0);
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        // the shared intro (BossDirector) sets qf_intro; her own script runs alongside its cutscene
        if (introClock < 0 && getPersistentData().getBoolean("qf_intro")) introClock = 0;
        if (introClock >= 0 && introClock <= BossCutscenes.INTRO_TICKS) { introStep(sl, introClock++); return; }
        if (introClock < 0) return;   // still frozen mid-command, waiting for someone to walk in
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive() || BossCutscenes.isFrozen(this)) return;
        if (--stoopTimer <= 0) { stoop(t); stoopTimer = qfPhase() == 2 ? 90 : 130; }
        if (--hoodTimer <= 0 && distanceTo(t) < 9) { hood(t); hoodTimer = 200; }
        if (qfPhase() == 2 && --eastTimer <= 0) { eastWind(); eastTimer = 170; }
    }

    /** The intro script: arm up, the echoes turn east, arm down, she sees you. */
    void introStep(ServerLevel sl, int t) {
        if (t == 0) {
            setCommanding(true);
            setYRot(-90); setYHeadRot(-90); yBodyRot = -90;   // east
            say(sl, "Skywarden Irel raises her arm.");
            log.add("intro:arm");
            sl.playSound(null, blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.2f, 1.4f);
        }
        if (t < 60 && t % 2 == 0) echoFalcons(sl, t);
        if (t == 25) { say(sl, "Every echo in the dome turns east with her."); log.add("intro:east"); sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 2f, 1.3f); }
        if (t == 60) {
            setCommanding(false);
            Player p = sl.getNearestPlayer(this, 48);
            if (p != null) getLookControl().setLookAt(p, 90, 90);
            say(sl, "She lowers it, and looks at you: a late stitch.");
            log.add("intro:noticed");
        }
        if (t == BossCutscenes.INTRO_TICKS) log.add("intro:fight");
    }

    /** A wheel of amber echo-falcons (motes in a falcon's flight line) around the dome, all banking to fly east. */
    private void echoFalcons(ServerLevel sl, int t) {
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(1f, 0.71f, 0.23f), 1.2f);
        for (int k = 0; k < 8; k++) {
            double a = k / 8.0 * Math.PI * 2 + t * 0.05;
            double r = 7 - t * 0.04, turn = Math.min(1, t / 40.0);
            // before the turn they circle her; after it, every bird heads east
            double x = getX() + Math.cos(a) * r * (1 - turn) + turn * (t - 40) * 0.25, z = getZ() + Math.sin(a) * r * (1 - turn) + turn * Math.sin(a) * 6;
            double y = getY() + 4 + Math.sin(a * 2 + t * 0.2) * 0.6;
            sl.sendParticles(dust, x, y, z, 3, 0.25, 0.05, 0.25, 0);
            sl.sendParticles(dust, x - 0.4, y + 0.15, z + 0.3, 1, 0.05, 0.02, 0.05, 0);   // wingtips
            sl.sendParticles(dust, x - 0.4, y + 0.15, z - 0.3, 1, 0.05, 0.02, 0.05, 0);
        }
    }

    /** her defeat line, on the same action bar as her lesson lines. */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource src) {
        super.die(src);
        if (this.level() instanceof ServerLevel sl) say(sl, "Irel lowers her arm. For the first time in a long time, the echoes land.");
    }

    private void say(ServerLevel sl, String line) {
        for (ServerPlayer p : sl.getPlayers(p -> p.distanceToSqr(this) < 48 * 48)) p.displayClientMessage(Component.literal(line).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
    }

    /** Stoop: a circle on the target's feet; an echo-falcon dives onto it. */
    public void stoop(LivingEntity t) {
        Vec3 c = Telegraph.floorBelow(level(), t.position());
        Telegraph tele = Telegraph.circle(c, 2.5);
        log.add("stoop");
        setCommanding(true);
        BossDirector.windup(this, "stoop", 26, tele, () -> {
            setCommanding(false);
            if (!(level() instanceof ServerLevel sl)) return;
            for (int i = 0; i < 12; i++) sl.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.71f, 0.23f), 1.5f), c.x, c.y + 6 - i * 0.5, c.z, 4, 0.2, 0.1, 0.2, 0);
            sl.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.2, c.z, 20, 1.2, 0.1, 1.2, 0.05);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 2f, 1.4f);
            for (Player p : sl.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(3, 3, 3)))
                if (tele.contains(p.position()) && !p.isCreative() && !p.isSpectator()) {
                    p.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2f);
                    p.push(0, 0.5, 0); p.hurtMarked = true;
                }
        });
    }

    /** Hood: a 35-degree cone in front of her; the hood drops over your eyes. */
    public void hood(LivingEntity t) {
        Telegraph tele = Telegraph.cone(position(), t.position(), 35, 9);
        log.add("hood");
        BossDirector.windup(this, "hood", 18, tele, () -> {
            if (!(level() instanceof ServerLevel sl)) return;
            sl.playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.HOSTILE, 2f, 0.8f);
            for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(10)))
                if (tele.contains(p.position()) && !p.isCreative() && !p.isSpectator()) {
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
        });
    }

    /** East, always east: a lane from her running east; the wind of the echoes throws you along it. */
    public void eastWind() {
        Telegraph tele = Telegraph.line(position(), position().add(1, 0, 0), 16, 1.5);
        log.add("east");
        setCommanding(true);
        if (level() instanceof ServerLevel sl) say(sl, "\"East. Always east.\"");
        BossDirector.windup(this, "east", 22, tele, () -> {
            setCommanding(false);
            if (!(level() instanceof ServerLevel sl)) return;
            sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 3f, 0.8f);
            for (int i = 0; i < 16; i++) sl.sendParticles(ParticleTypes.CLOUD, getX() + i, getY() + 1, getZ(), 4, 0.3, 0.5, 1.2, 0.2);
            for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(17, 4, 17)))
                if (tele.contains(p.position()) && !p.isCreative() && !p.isSpectator()) {
                    p.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
                    p.push(1.6, 0.4, 0); p.hurtMarked = true;
                }
        });
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) playSound(SoundEvents.PHANTOM_BITE, 1f, 1.3f);
        return hit;
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canChangeDimensions(Level from, Level to) { return false; }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return SoundEvents.PHANTOM_AMBIENT; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) { return SoundEvents.PLAYER_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.BELL_RESONATE; }
    @Override public float getVoicePitch() { return 1.3f; }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override public void remove(RemovalReason r) { bossEvent.removeAllPlayers(); super.remove(r); }
    @Override public void setCustomName(Component name) { super.setCustomName(name); bossEvent.setName(getDisplayName()); }

    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("IrelIntro", introClock); }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("IrelIntro")) introClock = tag.getInt("IrelIntro") > BossCutscenes.INTRO_TICKS || tag.getInt("IrelIntro") < 0 ? tag.getInt("IrelIntro") : BossCutscenes.INTRO_TICKS + 1;
        if (introClock > BossCutscenes.INTRO_TICKS) setCommanding(false);
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }
}
