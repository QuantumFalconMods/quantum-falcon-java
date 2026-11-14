package com.matt.quantumfalcon.entity;

import com.matt.quantumfalcon.genetics.FalconGenetics;
import com.matt.quantumfalcon.genetics.FalconGeneticsSystem;
import com.matt.quantumfalcon.genetics.Genetics;
import com.matt.quantumfalcon.registry.ModBlocks;
import com.matt.quantumfalcon.registry.ModEntities;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.registry.ModParticles;
import com.matt.quantumfalcon.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Quantum Falcon - Main rideable companion entity.
 * Features:
 * - Tameable with salmon or quantum fish
 * - Rideable when tamed (flight capable)
 * - Mendelian genetics breeding system
 * - 18 color/pattern variants
 * - Combat AI protecting owner
 * - Anomaly glow for legendary variants
 */
public class QuantumFalconEntity extends TamableAnimal implements FlyingAnimal, GeoEntity {
    
    // Synched data accessors
    private static final EntityDataAccessor<String> DATA_VARIANT = 
        SynchedEntityData.defineId(QuantumFalconEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_FLYING = 
        SynchedEntityData.defineId(QuantumFalconEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SITTING = 
        SynchedEntityData.defineId(QuantumFalconEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOND_LEVEL = 
        SynchedEntityData.defineId(QuantumFalconEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_STAMINA =
            SynchedEntityData.defineId(QuantumFalconEntity.class, EntityDataSerializers.FLOAT);
    public static final float MAX_STAMINA = 100f;
    /** Ticks left on the Quantum Berry flight boost (no stamina drain, +50% flying speed). */
    private int berryBoostTicks = 0;
    private static final EntityDataAccessor<Integer> DATA_ABILITY_COOLDOWN =
            SynchedEntityData.defineId(QuantumFalconEntity.class, EntityDataSerializers.INT);
    private int passiveTimer = 0, rebirthCooldown = 0, staticChargeCooldown = 0, abilityAnimTicks = 0;
    /** Packed genome visuals for the client: tier(3) | crack(4)<<3 | pattern(4)<<7 | size(3)<<11 | glow(2)<<14. */
    /** Anomaly skin (rare alternate texture set, textures/entity/quantum_falcon/anomaly_*.png); independent of variant. */
    /** Story ending look (0 none, 1 Light, 2 Dark, 3 True), from the owner's chosen ending; drawn by the renderer. */
    private static final EntityDataAccessor<Byte> DATA_ENDING = SynchedEntityData.defineId(QuantumFalconEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_RESTING = SynchedEntityData.defineId(QuantumFalconEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ANOMALY = SynchedEntityData.defineId(QuantumFalconEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_GENOME_VIS =
        SynchedEntityData.defineId(QuantumFalconEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_GENDER = 
        SynchedEntityData.defineId(QuantumFalconEntity.class, EntityDataSerializers.BOOLEAN);
    
    // Genetics for breeding
    @Nullable
    private Genetics genetics;
    @Nullable
    private FalconGenetics falconGenetics;
    private int breedingCooldownTicks = 0;
    
    // Command system
    private FalconCommand currentCommand = FalconCommand.FOLLOW;
    /** Last falcon perch this falcon landed on; WANDER mode returns here. */
    @Nullable
    private net.minecraft.core.BlockPos homePerch;
    
    // GeckoLib animation cache
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // Animation state
    private float flapProgress = 0;
    private float prevFlapProgress = 0;
    private int attackTicks = 0;
    private int hurtTicks = 0;
    
    // Variants — must match .png filenames in textures/entity/quantum_falcon/ (no _glow, no anomaly_)
    public static final String[] VARIANTS = {
        "peregrine", "voidstalker", "aetherwing", "coral", "crimson", "deepslate",
        "entangled_prism", "mushroom", "prairie", "soulfire", "tempest", "tundra",
        "warped", "abyssal", "singularity", "cosmic", "stormrider", "quantum_phoenix"
    };

    public QuantumFalconEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setTame(false, false);
        if (!level.isClientSide()) {
            this.entityData.set(DATA_GENDER, this.random.nextBoolean());   // never level.getRandom in a constructor (worldgen threads)
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.5, true));
        this.goalSelector.addGoal(4, new com.matt.quantumfalcon.entity.ai.FalconPerchGoal(this));
        // WANDER and STAY must not be pulled back to the owner (only FOLLOW/other commands follow)
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.2, 10.0f, 3.0f) {
            @Override public boolean canUse() { return followsOwner() && super.canUse(); }
            @Override public boolean canContinueToUse() { return followsOwner() && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(5, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(5, new com.matt.quantumfalcon.entity.ai.RestInNestGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.FLYING_SPEED, 0.6)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, "peregrine");
        builder.define(DATA_FLYING, false);
        builder.define(DATA_SITTING, false);
        builder.define(DATA_BOND_LEVEL, 0);
        builder.define(DATA_GENDER, true);
        builder.define(DATA_GENOME_VIS, 1 | (2 << 11));
        builder.define(DATA_ANOMALY, false);
        builder.define(DATA_RESTING, false);
        builder.define(DATA_STAMINA, MAX_STAMINA);
        builder.define(DATA_ABILITY_COOLDOWN, 0);
        builder.define(DATA_ENDING, (byte) 0);
    }

    public int endingLook() { return this.entityData.get(DATA_ENDING); }
    public void setEndingLook(int v) { this.entityData.set(DATA_ENDING, (byte) v); }
    public static int endingCode(String ending) {
        return ending == null ? 0 : switch (ending) { case "LIGHT" -> 1; case "DARK" -> 2; case "TRUE" -> 3; default -> 0; };
    }

    /** True on the server tick this falcon was turned east for dusk (qftests). */
    public boolean duskEast;
    private long lastDuskCall = -1;

    /** Overworld dusk (sunset into early night) under open sky. */
    public static boolean isDusk(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (level.dimensionType().hasFixedTime() || !level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) return false;
        long t = level.getDayTime() % 24000L;
        return t >= 11800L && t <= 13300L && level.canSeeSky(pos.above());
    }

    /**
     * Story (approved): idle and perched falcons turn to face east at dusk and call once, waiting for the dawn that was held
     * back. Cosmetic: only the look direction (and a sitting bird's body) turns; goals, targets and paths are untouched.
     * After a Light or True ending they stop: the morning came.
     */
    private void duskTick() {
        duskEast = false;
        int end = endingLook();
        if (end == 1 || end == 3) return;
        if (!isDusk(this.level(), this.blockPosition())) return;
        boolean perched = this.isInSittingPose() || this.isOrderedToSit() || isResting();
        boolean idle = !isFlying() && this.getTarget() == null && this.getNavigation().isDone() && this.getDeltaMovement().horizontalDistanceSqr() < 1.0E-3;
        if (!(perched || idle) || this.isLeashed() || this.isPassenger()) return;
        this.getLookControl().setLookAt(this.getX() + 16, this.getEyeY() + 1.5, this.getZ(), 10f, 40f);
        if (perched) { this.setYRot(net.minecraft.util.Mth.approachDegrees(this.getYRot(), -90f, 6f)); this.yBodyRot = this.getYRot(); this.yHeadRot = net.minecraft.util.Mth.approachDegrees(this.yHeadRot, -90f, 10f); }
        duskEast = true;
        long day = this.level().getDayTime() / 24000L;
        if (day != lastDuskCall && this.random.nextInt(200) == 0) { lastDuskCall = day; this.playAmbientSound(); }
    }

    /** Client: Light motes, Dark violet glints at the eyes, True gold glints whenever the bird faces east. */
    private void endingParticles() {
        int end = endingLook();
        if (end == 0 || this.isInvisible()) return;
        var r = this.random;
        double hx = this.getX() + net.minecraft.util.Mth.cos((this.yHeadRot + 90f) * net.minecraft.util.Mth.DEG_TO_RAD) * 0.25, hz = this.getZ() + net.minecraft.util.Mth.sin((this.yHeadRot + 90f) * net.minecraft.util.Mth.DEG_TO_RAD) * 0.25;
        double hy = this.getEyeY();
        if (end == 1 && r.nextInt(30) == 0)
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, 0.01, 0);
        else if (end == 2 && r.nextInt(12) == 0)
            this.level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.7f, 0.25f, 1.0f), 0.6f), hx, hy, hz, 0, 0, 0);
        else if (end == 3 && Math.abs(net.minecraft.util.Mth.wrapDegrees(this.yHeadRot + 90f)) < 25f && r.nextInt(6) == 0)
            this.level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0f, 0.8f, 0.25f), 0.7f), hx + 0.3, hy + 0.05, hz, 0.02, 0.01, 0);
    }

    /** Ending look follows the owner's chosen ending (kept while the owner is offline). */
    private void endingTick() {
        if (this.getOwner() instanceof net.minecraft.server.level.ServerPlayer sp) syncEnding(sp);
    }

    public void syncEnding(net.minecraft.server.level.ServerPlayer sp) {
        int code = endingCode(com.matt.quantumfalcon.story.PlayerStoryData.get(sp).getChosenEnding());
        if (code != endingLook()) setEndingLook(code);
    }

    // ========================================
    // NBT SAVE/LOAD
    // ========================================
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Variant", getVariant());
        tag.putBoolean("Anomaly", isAnomaly());
        tag.putBoolean("Flying", isFlying());
        tag.putInt("BondLevel", getBondLevel());
        tag.putString("Command", currentCommand.name());
        if (homePerch != null) tag.putLong("HomePerch", homePerch.asLong());
        tag.putByte("EndingLook", (byte) endingLook());
        
        if (genetics != null) {
            CompoundTag geneticsTag = new CompoundTag();
            genetics.writeTo(geneticsTag);
            tag.put("Genetics", geneticsTag);
        }
        if (falconGenetics != null) {
            tag.put("FalconGenetics", falconGenetics.serialize());
        }
        if (genome != null) tag.put("Genome", genome.save());
        tag.putBoolean("Gender", isMale());
        tag.putFloat("Stamina", getStamina());
        tag.putInt("BerryBoost", berryBoostTicks);
        tag.putInt("BreedingCooldown", breedingCooldownTicks);
        tag.putInt("AbilityCooldown", getAbilityCooldown());
        tag.putInt("RebirthCooldown", rebirthCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getString("Variant"));
        setAnomaly(tag.getBoolean("Anomaly"));
        setFlying(tag.getBoolean("Flying"));
        setBondLevel(tag.getInt("BondLevel"));
        
        if (tag.contains("Command")) {
            try {
                currentCommand = FalconCommand.valueOf(tag.getString("Command"));
            } catch (IllegalArgumentException e) {
                currentCommand = FalconCommand.FOLLOW; // Default
            }
        }
        
        if (tag.contains("Stamina")) setStamina(tag.getFloat("Stamina"));
        berryBoostTicks = tag.getInt("BerryBoost");
        homePerch = tag.contains("HomePerch") ? net.minecraft.core.BlockPos.of(tag.getLong("HomePerch")) : null;
        setEndingLook(tag.getByte("EndingLook"));

        if (tag.contains("Genetics")) {
            genetics = Genetics.fromNbt(tag.getCompound("Genetics"));
        }
        if (tag.contains("FalconGenetics")) {
            falconGenetics = FalconGenetics.deserialize(tag.getCompound("FalconGenetics"));
        }
        if (tag.contains("Genome")) { var g = com.matt.quantumfalcon.breeding.FalconGenome.load(tag.getCompound("Genome")); if (g != null) setGenome(g); }
        if (tag.contains("Gender")) {
            this.entityData.set(DATA_GENDER, tag.getBoolean("Gender"));
        }
        setAbilityCooldown(tag.getInt("AbilityCooldown"));
        updateBardingAttributes();
        rebirthCooldown = tag.getInt("RebirthCooldown");
        if (tag.contains("BreedingCooldown")) {
            breedingCooldownTicks = tag.getInt("BreedingCooldown");
        }
    }

    // ========================================
    // BREEDING GENOME (breeding/FalconGenome)
    // ========================================
    @Nullable private com.matt.quantumfalcon.breeding.FalconGenome genome;
    public final com.matt.quantumfalcon.breeding.AbilityEngine.Cooldowns genomeCooldowns = new com.matt.quantumfalcon.breeding.AbilityEngine.Cooldowns();

    /** The falcon's genome; falcons from before the breeding update get a wild genome for their variant on first access. */
    public com.matt.quantumfalcon.breeding.FalconGenome getGenome() {
        if (genome == null) {
            var g = com.matt.quantumfalcon.breeding.BreedingEngine.wild(getVariant(), com.matt.quantumfalcon.breeding.BreedingEngine.variantTier(getVariant()), getRandom());
            if (level().isClientSide()) return g;   // client never owns the genome; visuals come from DATA_GENOME_VIS
            setGenome(g);
        }
        return genome;
    }
    @Nullable public com.matt.quantumfalcon.breeding.FalconGenome getGenomeOrNull() { return genome; }

    public void setGenome(com.matt.quantumfalcon.breeding.FalconGenome g) {
        this.genome = g;
        setVariant(g.variant);
        int vis = (g.tier & 7) | ((g.expressedCrack() & 15) << 3) | ((g.expressedPattern() & 15) << 7) | ((g.sizeIndex() & 7) << 11) | ((Math.min(3, g.glowLevel()) & 3) << 14);
        this.entityData.set(DATA_GENOME_VIS, vis);
        applyGenomeAttributes();
    }
    public int visTier() { return Math.max(1, this.entityData.get(DATA_GENOME_VIS) & 7); }
    public int visCrack() { return (this.entityData.get(DATA_GENOME_VIS) >> 3) & 15; }
    public int visPattern() { return (this.entityData.get(DATA_GENOME_VIS) >> 7) & 15; }
    public float visSizeScale() { return com.matt.quantumfalcon.breeding.BreedingData.SIZE_SCALES[Math.min(5, (this.entityData.get(DATA_GENOME_VIS) >> 11) & 7)]; }
    public int visGlow() { return (this.entityData.get(DATA_GENOME_VIS) >> 14) & 3; }

    private static final net.minecraft.resources.ResourceLocation GENOME_MOD = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "genome");
    private void applyGenomeAttributes() {
        if (genome == null || level() == null || level().isClientSide()) return;
        setMod(Attributes.FLYING_SPEED, 0.6 * genome.statFraction(0), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setMod(Attributes.ATTACK_DAMAGE, 8.0 * genome.statFraction(1), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
        setMod(Attributes.FOLLOW_RANGE, 16.0 * genome.statFraction(2), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
        setMod(Attributes.MAX_HEALTH, 4.0 * (genome.tier - 1), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
        setMod(Attributes.ARMOR, genome.has("thick_plumage") ? 4.0 : 0.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
    }
    private void setMod(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double v, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation op) {
        var inst = getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(GENOME_MOD);
        if (v != 0) inst.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(GENOME_MOD, v, op));
    }

    // ========================================
    // GENETICS
    // ========================================
    
    public Genetics getOrCreateGenetics() {
        if (genetics == null) {
            genetics = FalconGeneticsSystem.createRandomGenetics(getRandom());
            syncVariantFromGenetics();
        }
        return genetics;
    }

    public void setGenetics(Genetics g) {
        this.genetics = g;
        syncVariantFromGenetics();
    }

    /** Falcons spawn on open, sky-lit solid ground (mountain stone, snow, grass, forest floor). */
    public static boolean checkFalconSpawnRules(net.minecraft.world.entity.EntityType<QuantumFalconEntity> type,
            net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType reason,
            net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        if (reason == net.minecraft.world.entity.MobSpawnType.SPAWNER) return true;
        net.minecraft.world.level.block.state.BlockState below = level.getBlockState(pos.below());
        return level.canSeeSky(pos) && below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                && !below.is(net.minecraft.tags.BlockTags.LEAVES) && level.getRawBrightness(pos, 0) > 8;
    }

    /** Wild spawns pick a random common variant weighted by biome temperature; 1% are anomalies. */
    @Override
    @Nullable
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType reason,
            @Nullable net.minecraft.world.entity.SpawnGroupData data) {
        if (reason == net.minecraft.world.entity.MobSpawnType.NATURAL || reason == net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION
                || reason == net.minecraft.world.entity.MobSpawnType.SPAWN_EGG || reason == net.minecraft.world.entity.MobSpawnType.COMMAND) {
            if (genetics == null && falconGenetics == null) {
                float temp = level.getBiome(this.blockPosition()).value().getBaseTemperature();
                String[] pool = temp < 0.2f ? new String[]{"tundra", "peregrine", "stormrider", "deepslate"}
                        : temp > 1.0f ? new String[]{"prairie", "crimson", "peregrine", "soulfire"}
                        : new String[]{"peregrine", "peregrine", "tempest", "mushroom", "coral", "stormrider", "deepslate"};
                String riftVariant = riftVariantFor(level.getBiome(this.blockPosition()).unwrapKey().map(k -> k.location()).orElse(null));
                if (riftVariant != null && this.random.nextFloat() < 0.6f) setVariant(riftVariant);
                else setVariant(pool[this.random.nextInt(pool.length)]);
            }
        }
        var spawnData = super.finalizeSpawn(level, difficulty, reason, data);
        if (genome == null && !level.isClientSide()) {
            setGenome(com.matt.quantumfalcon.breeding.BreedingEngine.wild(getVariant(),
                    com.matt.quantumfalcon.breeding.BreedingService.wildTier(getVariant(), this.level(), this.blockPosition(), this.random), this.random));
            if (this.random.nextFloat() < 0.015f) setAnomaly(true);   // rare wild anomaly skin
        }
        return spawnData;
    }

    /** Echo Rift wild variants by biome (see FALCONS.md). Null outside the matching Echo Rift biomes. */
    @Nullable
    public static String riftVariantFor(@Nullable net.minecraft.resources.ResourceLocation biome) {
        if (biome == null || !"quantumfalcon".equals(biome.getNamespace())) return null;
        String p = biome.getPath();
        if (p.contains("abyssal") || p.contains("dark_matter")) return "abyssal";
        if (p.contains("void")) return "voidstalker";
        if (p.contains("entangle") || p.contains("prismatic")) return "entangled_prism";
        if (p.contains("distortion") || p.contains("paradox") || p.contains("phase")) return "warped";
        if (p.contains("peaks") || p.contains("apex") || p.contains("mountains")) return "aetherwing";
        return null;
    }

    private void syncVariantFromGenetics() {
        if (falconGenetics != null) {
            setVariant(falconGenetics.getVariantFromGenes());
        } else if (genetics != null) {
            if (genetics.isLegendary()) setVariant("quantum_phoenix");
            else setVariant(genetics.getExpressedColor().getTextureSuffix());
        }
    }
    
    @Nullable
    public Genetics getGenetics() {
        return genetics;
    }

    public FalconGenetics getOrCreateFalconGenetics() {
        if (falconGenetics == null) {
            // keep the falcon's current variant as its colour gene (wild/egg-spawned falcons used to be re-rolled on breeding)
            falconGenetics = FalconGenetics.forVariant(getVariant(), this.random);
            syncVariantFromGenetics();
        }
        return falconGenetics;
    }

    public void setFalconGenetics(FalconGenetics g) {
        this.falconGenetics = g;
        syncVariantFromGenetics();
    }

    @Nullable
    public FalconGenetics getFalconGenetics() {
        return falconGenetics;
    }

    // ========================================
    // VARIANT ACCESSORS
    // ========================================
    
    public String getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public void setVariant(String variant) {
        this.entityData.set(DATA_VARIANT, variant);
    }

    public boolean isMale() {
        return this.entityData.get(DATA_GENDER);
    }

    public boolean isFemale() {
        return !this.entityData.get(DATA_GENDER);
    }

    public void setGender(boolean male) {
        this.entityData.set(DATA_GENDER, male);
    }

    public boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    public void setFlying(boolean flying) {
        this.entityData.set(DATA_FLYING, flying);
    }
    
    public int getBondLevel() {
        return this.entityData.get(DATA_BOND_LEVEL);
    }
    
    public void setBondLevel(int level) {
        this.entityData.set(DATA_BOND_LEVEL, Mth.clamp(level, 0, 100));
    }
    
    public void addBondLevel(int amount) {
        addBondLevel(amount, getOwner() instanceof net.minecraft.world.entity.player.Player p ? p : null);
    }

    /** Bond change with the owner known to the caller (taming/feeding pass the interacting player). */
    public void addBondLevel(int amount, net.minecraft.world.entity.player.Player knownOwner) {
        if (!level().isClientSide && knownOwner != null && isOwnedBy(knownOwner)) amount = com.matt.quantumfalcon.skill.SkillEffects.scaleBondGain(this, knownOwner, amount);   // bond_kinship
        int before = getBondLevel();
        setBondLevel(before + amount);
        if (level().isClientSide || !(knownOwner instanceof net.minecraft.server.level.ServerPlayer owner) || !isOwnedBy(owner)) return;
        if (getBondLevel() != before) {
            com.matt.quantumfalcon.quest.QuestTracker.onBondLevelChange(owner, this, getBondLevel());
            // story bonds (True ending) track the best bond reached with a falcon of each type
            com.matt.quantumfalcon.story.PlayerStoryData.get(owner).raiseFalconBond(com.matt.quantumfalcon.story.PlayerStoryData.bondType(getVariant()), getBondLevel());
        }
        if (before < 100 && getBondLevel() >= 100 && !getPersistentData().getBoolean("qf_max_bond_awarded")) {
            getPersistentData().putBoolean("qf_max_bond_awarded", true);
            com.matt.quantumfalcon.skill.SkillPointSystem.awardMaxBondPoints(owner, this);
        }
    }

    public FalconCommand getCommand() {
        return currentCommand;
    }
    
    public FalconCommand getCommandMode() {
        return currentCommand;
    }
    
    public void setCommand(FalconCommand command) {
        if (command == FalconCommand.ABILITY) {
            if (getOwner() != null) com.matt.quantumfalcon.entity.ability.FalconAbilities.activate(this, getOwner());
            return;   // the active is instant; keep the current mode
        }
        this.currentCommand = command;
        if (command == FalconCommand.WANDER && (homePerch == null || !isValidPerch(homePerch))) {
            net.minecraft.core.BlockPos found = findNearbyPerch(16);
            if (found != null) homePerch = found;
        }
        if (command == FalconCommand.FOLLOW || command == FalconCommand.WANDER) {
            this.setOrderedToSit(false);
        }
    }

    // ========================================
    // FALCON PERCH
    // ========================================

    private boolean followsOwner() {
        return currentCommand != FalconCommand.WANDER && currentCommand != FalconCommand.STAY;
    }

    // ========================================
    // FLIGHT STAMINA (Quantum Stone perches regenerate it 3x faster)
    // ========================================
    public float getStamina() { return this.entityData.get(DATA_STAMINA); }
    public void setStamina(float v) { this.entityData.set(DATA_STAMINA, net.minecraft.util.Mth.clamp(v, 0f, MAX_STAMINA)); }
    public int getBerryBoostTicks() { return berryBoostTicks; }
    public void applyBerryBoost(int ticks) { berryBoostTicks = Math.max(berryBoostTicks, ticks); setStamina(MAX_STAMINA); }

    /** Block tag of stones that empower a perch placed on them (Quantum Stone family). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> STAMINA_STONES =
            net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.matt.quantumfalcon.QuantumFalcon.MOD_ID, "stamina_stones"));

    /** Perch the falcon currently sits on, if any (checks the block below its feet). */
    @Nullable
    public net.minecraft.core.BlockPos currentPerch() {
        net.minecraft.core.BlockPos below = net.minecraft.core.BlockPos.containing(getX(), getY() - 0.2, getZ());
        if (isValidPerch(below) && isOnPerch(below)) return below;
        return null;
    }

    public boolean isOnQuantumPerch() {
        net.minecraft.core.BlockPos p = currentPerch();
        return p != null && this.level().getBlockState(p.below()).is(STAMINA_STONES);
    }

    /** Per-tick stamina change; public for tests. */
    public float staminaDelta() {
        if (currentPerch() != null) return isOnQuantumPerch() ? 3.0f : 1.0f;
        if (isFlying()) {
            if (berryBoostTicks > 0) return 0f;
            float drain = this.isVehicle() ? 0.25f : 0.05f;
            if (getOwner() instanceof Player owner) {
                var sd = com.matt.quantumfalcon.skill.PlayerSkillData.get(owner);
                if (sd != null && sd.hasSkill("bond_endurance")) drain *= com.matt.quantumfalcon.skill.SkillEffects.ENDURANCE_DRAIN_MULT;
            }
            return -drain;
        }
        return this.onGround() ? 0.5f : 0f;
    }

    private void tickStamina() {
        if (berryBoostTicks > 0) berryBoostTicks--;
        float d = staminaDelta();
        if (d != 0f) setStamina(getStamina() + d);
    }

    /** Owner check by UUID so it also works while the owner is offline/in another dimension (and for FakePlayers). */
    @Override
    public boolean isOwnedBy(net.minecraft.world.entity.LivingEntity entity) {
        return entity != null && entity.getUUID().equals(this.getOwnerUUID());
    }

    public boolean isExhausted() { return getStamina() <= 0.5f; }

    // ========================================
    // VARIANT ABILITIES (see entity/ability/FalconAbilities)
    // ========================================
    public com.matt.quantumfalcon.entity.ability.FalconAbility ability() { return com.matt.quantumfalcon.entity.ability.FalconAbility.of(getVariant()); }
    public int getAbilityCooldown() { return this.entityData.get(DATA_ABILITY_COOLDOWN); }
    public void setAbilityCooldown(int ticks) { this.entityData.set(DATA_ABILITY_COOLDOWN, Math.max(0, ticks)); }
    public int bumpPassiveTimer() { return ++passiveTimer; }
    public void resetPassiveTimer() { passiveTimer = 0; }
    public int getRebirthCooldown() { return rebirthCooldown; }
    public void setRebirthCooldown(int t) { rebirthCooldown = Math.max(0, t); }
    public void tickRebirthCooldown(int t) { rebirthCooldown = Math.max(0, rebirthCooldown - t); }
    public boolean tryUseStaticCharge() { if (staticChargeCooldown > 0) return false; staticChargeCooldown = 20; return true; }
    public boolean isCastingAbility() { return abilityAnimTicks > 0; }
    public void triggerAbilityAnimation() { abilityAnimTicks = 12; attackTicks = 12; if (!this.level().isClientSide) this.level().broadcastEntityEvent(this, ATTACK_ANIM_EVENT); }
    private static final byte ATTACK_ANIM_EVENT = 60;
    @Override
    public void handleEntityEvent(byte id) {
        if (id == ATTACK_ANIM_EVENT) { attackTicks = 12; return; }
        super.handleEntityEvent(id);
    }
    private static final net.minecraft.resources.ResourceLocation BARDING_ARMOR = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "barding_armor");
    private static final net.minecraft.resources.ResourceLocation BARDING_TOUGH = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "barding_toughness");
    private static final net.minecraft.resources.ResourceLocation BARDING_SIG_ATK = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "barding_sig_attack");
    private static final net.minecraft.resources.ResourceLocation BARDING_SIG_HP = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "barding_sig_health");
    private static final net.minecraft.resources.ResourceLocation BARDING_KB = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "barding_knockback");

    @Nullable
    public com.matt.quantumfalcon.item.FalconBardingItem.Tier bardingTier() {
        return getBodyArmorItem().getItem() instanceof com.matt.quantumfalcon.item.FalconBardingItem fb ? fb.tier() : null;
    }

    /** Re-applies the barding's armor/toughness/knockback modifiers (called on equip, removal and load). */
    public void updateBardingAttributes() {
        var t = bardingTier();
        var armor = getAttribute(Attributes.ARMOR); var tough = getAttribute(Attributes.ARMOR_TOUGHNESS); var kb = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (armor != null) armor.removeModifier(BARDING_ARMOR);
        if (tough != null) tough.removeModifier(BARDING_TOUGH);
        if (kb != null) kb.removeModifier(BARDING_KB);
        var atk = getAttribute(Attributes.ATTACK_DAMAGE); var hp = getAttribute(Attributes.MAX_HEALTH);
        if (atk != null) atk.removeModifier(BARDING_SIG_ATK);
        if (hp != null) hp.removeModifier(BARDING_SIG_HP);
        if (t == null) return;
        var op = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
        var sig = com.matt.quantumfalcon.loot.sig.SignatureLoot.sig(getBodyArmorItem());   // A8 Bond gear: named bardings add falcon stats
        if (sig != null) {
            if (atk != null && sig.getDouble("falcon_attack") > 0) atk.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(BARDING_SIG_ATK, sig.getDouble("falcon_attack"), op));
            if (hp != null && sig.getDouble("falcon_health") > 0) { hp.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(BARDING_SIG_HP, sig.getDouble("falcon_health"), op)); }
        }
        if (armor != null) armor.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(BARDING_ARMOR, t.armor, op));
        if (tough != null && t.toughness > 0) tough.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(BARDING_TOUGH, t.toughness, op));
        if (kb != null && t.knockbackRes > 0) kb.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(BARDING_KB, t.knockbackRes, op));
    }

    /** Falcon barding: better tiers shorten the active cooldown (quantum: halves it). */
    public float bardingCooldownMultiplier() {
        net.minecraft.world.item.ItemStack b = getBodyArmorItem();
        return b.getItem() instanceof com.matt.quantumfalcon.item.FalconBardingItem fb ? fb.tier().cooldownMultiplier : 1f;
    }

    @Nullable
    public net.minecraft.core.BlockPos getHomePerch() { return homePerch; }

    public void setHomePerch(@Nullable net.minecraft.core.BlockPos pos) { this.homePerch = pos; }

    public boolean isValidPerch(net.minecraft.core.BlockPos pos) {
        return this.level().isLoaded(pos) && this.level().getBlockState(pos).getBlock() instanceof com.matt.quantumfalcon.block.FalconPerchBlock
                && this.level().getBlockState(pos.above()).getCollisionShape(this.level(), pos.above()).isEmpty();
    }

    public boolean isOnPerch(net.minecraft.core.BlockPos pos) {
        return Math.abs(this.getX() - (pos.getX() + 0.5)) < 0.4 && Math.abs(this.getZ() - (pos.getZ() + 0.5)) < 0.4
                && Math.abs(this.getY() - (pos.getY() + com.matt.quantumfalcon.block.FalconPerchBlock.PERCH_TOP)) < 0.3;
    }

    /** Nearest free perch within radius (horizontal) / 4 blocks vertical, or null. */
    @Nullable
    public net.minecraft.core.BlockPos findNearbyPerch(int radius) {
        net.minecraft.core.BlockPos origin = this.blockPosition();
        net.minecraft.core.BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(origin.offset(-radius, -4, -radius), origin.offset(radius, 4, radius))) {
            if (!isValidPerch(p)) continue;
            double d = p.distSqr(origin);
            if (d < bestD && !perchOccupiedByOther(p)) { bestD = d; best = p.immutable(); }
        }
        return best;
    }

    private boolean perchOccupiedByOther(net.minecraft.core.BlockPos p) {
        return !this.level().getEntitiesOfClass(QuantumFalconEntity.class,
                new net.minecraft.world.phys.AABB(p.above()).inflate(0.2), f -> f != this && f.isOnPerch(p)).isEmpty();
    }

    /** Snap onto the perch crossbar, face its axis, and remember it as home. */
    public void landOnPerch(net.minecraft.core.BlockPos pos) {
        this.getNavigation().stop();
        this.setFlying(false);
        this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        this.moveTo(pos.getX() + 0.5, pos.getY() + com.matt.quantumfalcon.block.FalconPerchBlock.PERCH_TOP, pos.getZ() + 0.5, this.getYRot(), this.getXRot());
        this.homePerch = pos.immutable();
    }

    /** Called when ordered to sit: land on a free perch within 8 blocks, if any. */
    public boolean tryPerchNearby() {
        net.minecraft.core.BlockPos p = findNearbyPerch(8);
        if (p == null) return false;
        landOnPerch(p);
        return true;
    }
    
    public void setVariantByName(String variantName) {
        setVariant(variantName);
    }
    
    /** Anomaly skin flag. (Before 2026-11-12 this returned isLegendary(), which forced every Singularity, Cosmic and Quantum
     *  Phoenix onto the anomaly texture and left the alternate skins unreachable for everyone else.) */
    public boolean isAnomaly() { return this.entityData.get(DATA_ANOMALY); }

    /** Resting in a nest (RestInNestGoal); plays the sit/sleep animation and broods an egg underneath. */
    public boolean isResting() { return this.entityData.get(DATA_RESTING); }
    public void setResting(boolean r) { this.entityData.set(DATA_RESTING, r); }

    public void setAnomaly(boolean anomaly) { this.entityData.set(DATA_ANOMALY, anomaly); }

    public boolean isLegendary() {
        String variant = getVariant();
        return variant.equals("quantum_phoenix") || variant.equals("singularity") || 
               variant.equals("cosmic") || variant.equals("prismatic");
    }
    
    // ========================================
    // CLIENT-SIDE SETTERS (for network sync)
    // ========================================
    
    public void setBondLevelClient(int bondLevel) {
        setBondLevel(bondLevel);
    }
    
    public void setHealthClient(float health) {
        setHealth(health);
    }
    
    public void setCommandModeClient(int commandMode) {
        if (commandMode >= 0 && commandMode < FalconCommand.values().length) {
            this.currentCommand = FalconCommand.values()[commandMode];
        }
    }
    
    public void setAnomalyClient(boolean anomaly) {
        setAnomaly(anomaly);
    }
    
    public void setVariantClient(int variantIndex) {
        if (variantIndex >= 0 && variantIndex < VARIANTS.length) {
            setVariant(VARIANTS[variantIndex]);
        }
    }
    
    public void setBondProgressClient(float bondProgress) {
        // Store or use bond progress as needed
        // For now, this is a client-side display value
    }

    // ========================================
    // INTERACTION
    // ========================================
    
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // Falcon barding: equip with the barding, remove with sneak + shears
        if (this.isTame() && this.isOwnedBy(player)) {
            if (itemstack.getItem() instanceof com.matt.quantumfalcon.item.FalconBardingItem) {
                if (!this.level().isClientSide) {
                    ItemStack old = getBodyArmorItem();
                    setBodyArmorItem(itemstack.copyWithCount(1));
                    if (!player.getAbilities().instabuild) itemstack.shrink(1);
                    if (!old.isEmpty() && !player.addItem(old)) spawnAtLocation(old);
                    this.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1f, 1.2f);
                    updateBardingAttributes();
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (player.isShiftKeyDown() && itemstack.is(net.minecraft.world.item.Items.SHEARS) && !getBodyArmorItem().isEmpty()) {
                if (!this.level().isClientSide) {
                    ItemStack old = getBodyArmorItem();
                    setBodyArmorItem(ItemStack.EMPTY);
                    if (!player.addItem(old)) spawnAtLocation(old);
                    this.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1f, 0.8f);
                    updateBardingAttributes();
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        
        // Taming with salmon, cod, or tropical fish
        if (!this.isTame() && isTamingItem(itemstack)) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            
            // roll only on the server; the client just swings the hand. Running it on both sides
            // let the client show its own (often wrong) "tamed" result.
            if (this.level().isClientSide) return InteractionResult.SUCCESS;
            if (this.random.nextInt(3) == 0) {
                this.tame(player);
                // taming a falcon is Chapter 1 "Eyes That See Stitches"; breeding is gated on it
                com.matt.quantumfalcon.story.StorylineManager.triggerStoryEvent(player, com.matt.quantumfalcon.story.StorylineManager.StoryChapter.CHAPTER_1);
                this.navigation.stop();
                var fa = ability();
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("falcon_ability.quantumfalcon.tamed", fa.variantName(), fa.passiveName(), fa.activeName()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), false);
                this.setOrderedToSit(true);
                this.level().broadcastEntityEvent(this, (byte) 7); // Hearts
                addBondLevel(10, player);
                this.playTameSound(); // Play taming success sound
                
                // Track quest progress: falcon tamed
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) com.matt.quantumfalcon.quest.QuestTracker.onFalconTamed(sp, this);
                
                // Track variant discovery
                
                // First-tame skill points (awarded and synced through SkillPointSystem; skill tree audit B6)
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) com.matt.quantumfalcon.skill.SkillPointSystem.awardFirstTamePoints(sp);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6); // Smoke
            }
            return InteractionResult.SUCCESS;
        }
        
        // ── Quantum Berry / food feeding (BEFORE sit/mount so berry is consumed by falcon) ──
        if (this.isTame() && this.isOwnedBy(player) && isFood(itemstack)) {
            if (!this.level().isClientSide() && itemstack.is(ModItems.QUANTUM_BERRY.get())) {
                applyBerryBoost(600); // 30 s of tireless, faster flight
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u00a7d\u2726 Quantum Berry: flight boost for 30s"), true);
            }
            if (!this.level().isClientSide() && itemstack.is(ModItems.FALCON_TREAT.get())) {
                // the treat (trader, Sovereign drop, chests) is the best snack: big heal and three bond levels at any health
                this.heal(10.0f);
                addBondLevel(3, player);
                this.playBondUpSound();
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u00a7d\u2726 Falcon Treat: +3 bond, healed"), true);
            }
            if (!this.level().isClientSide()) {
                // Heal (4 HP = 2 hearts)
                if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(4.0f);
                    addBondLevel(1, player);
                    if (getBondLevel() % 10 == 0) this.playBondUpSound();
                }
                // Adult + can breed → enter love mode (breeding)
                if (this.getAge() == 0 && this.canFallInLove()) {
                    // owning a tamed falcon *is* Chapter 1. Older saves (falcons tamed before the chapter hook existed, or
                    // hatched ones) are backfilled here instead of being refused forever.
                    com.matt.quantumfalcon.story.StorylineManager.triggerStoryEvent(player, com.matt.quantumfalcon.story.StorylineManager.StoryChapter.CHAPTER_1);
                    this.setInLove(player);
                    this.playSound(ModSounds.FALCON_BREED.get(), 0.9F, 1.0F);
                    if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        for (int i = 0; i < 7; i++) {
                            double dx = this.getRandom().nextGaussian() * 0.02;
                            double dy = this.getRandom().nextGaussian() * 0.02;
                            double dz = this.getRandom().nextGaussian() * 0.02;
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                                this.getX() + this.getRandom().nextFloat() * this.getBbWidth() * 2.0 - this.getBbWidth(),
                                this.getY() + 0.5 + this.getRandom().nextFloat() * this.getBbHeight(),
                                this.getZ() + this.getRandom().nextFloat() * this.getBbWidth() * 2.0 - this.getBbWidth(),
                                1, dx, dy, dz, 0.5);
                        }
                    }
                }
                // Baby → accelerate growth (10% of remaining time per feed)
                else if (this.isBaby()) {
                    int remaining = -this.getAge();
                    this.ageUp((int)(remaining * 0.1), true);
                    this.playSound(ModSounds.FALCON_FEED.get(), 0.8F, 1.0F);
                }
                // Play feed + amethyst chime (quantum berry feel)
                this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
                if (itemstack.is(ModItems.QUANTUM_BERRY.get())) {
                    this.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.2F + this.level().getRandom().nextFloat() * 0.4F);
                }
                // Consume the item
                if (!player.getAbilities().instabuild) itemstack.shrink(1);
            }
            this.level().broadcastEntityEvent(this, (byte) 7); // Hearts (client)
            return InteractionResult.SUCCESS;
        }
        
        // Toggle sit/stand for tamed falcon
        if (this.isTame() && this.isOwnedBy(player) && !isFood(itemstack)) {
            if (player.isShiftKeyDown()) {
                // Shift-click to toggle sit
                this.setOrderedToSit(!this.isOrderedToSit());
                this.jumping = false;
                this.navigation.stop();
                if (this.isOrderedToSit() && !this.level().isClientSide()) {
                    tryPerchNearby();
                }
                return InteractionResult.SUCCESS;
            } else if (!this.isBaby()) {
                // Normal click to ride (if not baby)
                player.startRiding(this);
                return InteractionResult.SUCCESS;
            }
        }
        
        return super.mobInteract(player, hand);
    }
    
    private boolean isTamingItem(ItemStack stack) {
        return stack.is(Items.SALMON) || stack.is(Items.COD) || stack.is(ModItems.FALCON_TREAT.get()) || stack.is(Items.TROPICAL_FISH);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SALMON) || stack.is(Items.COD) || 
               stack.is(Items.RABBIT) || stack.is(Items.CHICKEN) || stack.is(ModItems.FALCON_TREAT.get()) ||
               isBreedingFood(stack);
    }

    public static boolean isBreedingFood(ItemStack stack) {
        return stack.is(ModItems.QUANTUM_BERRY.get()) || 
               stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    // ========================================
    // RIDING
    // ========================================
    
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity entity = this.getFirstPassenger();
        if (entity instanceof Player player && this.isOwnedBy(player)) {
            return player;
        }
        return null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        
        // Rotate falcon to match rider
        this.setYRot(player.getYRot());
        this.yRotO = this.getYRot();
        this.setXRot(player.getXRot() * 0.5F);
        this.setRot(this.getYRot(), this.getXRot());
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.yBodyRot;
        
        // Flight control - use player's forward movement to control altitude
        // Jump key detection requires client-side code, so use zza (forward input)
        // positive zza with shift = fly up, negative zza = fly down
        float verticalInput = 0;
        if (player.zza > 0.1f && !player.isShiftKeyDown()) {
            verticalInput = 0.05f; // Gradual climb
        }
        
        if (isExhausted()) {
            verticalInput = 0; // too tired to climb: glide down
            if (isFlying()) this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04, 0));
            if (this.tickCount % 40 == 0) player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u00a7eYour falcon is exhausted \u2014 land to rest (Quantum Stone perches restore stamina fastest)."), true);
        }
        if (verticalInput > 0) {
            this.setFlying(true);
            this.setDeltaMovement(this.getDeltaMovement().add(0, verticalInput, 0));
        }
        
        if (player.isShiftKeyDown() && isFlying()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.1, 0));
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float forward = player.zza;
        float strafe = player.xxa * 0.5F;
        
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }
        
        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        if (isFlying() && berryBoostTicks > 0) return (float) this.getAttributeValue(Attributes.FLYING_SPEED) * 1.5f;
        return isFlying() ? (float) this.getAttributeValue(Attributes.FLYING_SPEED) : 
                           (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    // ========================================
    // BREEDING
    // ========================================
    
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        if (!(otherParent instanceof QuantumFalconEntity other)) return null;
        BlockPos femalePos = (this.isFemale() ? this : other).blockPosition().above();
        BlockPos nest = com.matt.quantumfalcon.breeding.BreedingService.freeNestNear(level, (this.isFemale() ? this : other).blockPosition(), 8);
        if (nest != null) femalePos = nest.above();   // lay into the best free nest nearby
        else if (!level.getBlockState(femalePos).canBeReplaced()) femalePos = femalePos.above();
        if (level.getBlockState(femalePos).canBeReplaced()) {
            level.setBlock(femalePos, ModBlocks.FALCON_EGG_BLOCK.get().defaultBlockState(), 3);
            if (level.getBlockEntity(femalePos) instanceof com.matt.quantumfalcon.blockentity.FalconEggBlockEntity eggBe) {
                var ctx = com.matt.quantumfalcon.breeding.BreedingService.contextAt(level, femalePos);
                ctx.bothBonded = com.matt.quantumfalcon.breeding.BreedingService.bothBonded(this, other);
                if (getOwner() instanceof net.minecraft.world.entity.player.Player op) {
                    var cap = com.matt.quantumfalcon.capability.PlayerInstabilityCapability.get(op);
                    ctx.highInstability = cap != null && cap.getInstability() >= com.matt.quantumfalcon.dimension.InstabilityEffects.WARNING_THRESHOLD;
                    ctx.nestMutation += (float) Math.min(0.25, com.matt.quantumfalcon.loot.sig.SignatureLoot.bonus(op, "mutation"));   // A8 Lineage gear
                    ctx.nestMutation += (float) com.matt.quantumfalcon.skills.UseSkills.mutationBonus(op);   // A9 Breeding skill (input to the pity math, never past its cap)
                }
                eggBe.setParents(this.getGenome(), other.getGenome(), this.getOwnerUUID(), ctx);
            }
        }
        this.breedingCooldownTicks = 6000;
        other.breedingCooldownTicks = 6000;
        if (this.getOwnerUUID() != null && level.getPlayerByUUID(this.getOwnerUUID()) instanceof net.minecraft.world.entity.player.Player owner) {
            if (owner instanceof net.minecraft.server.level.ServerPlayer sp)
                com.matt.quantumfalcon.quest.QuestTracker.updateObjective(sp, com.matt.quantumfalcon.quest.QuestTracker.ObjectiveType.BREED_FALCON, "any", 1);
        }
        this.playSound(ModSounds.FALCON_BREED.get(), 0.9F, 1.0F);
        return null;
    }

    @Override
    public boolean canMate(Animal other) {
        if (other == this) return false;
        if (!(other instanceof QuantumFalconEntity)) return false;
        if (!this.isTame() || !((QuantumFalconEntity) other).isTame()) return false;
        QuantumFalconEntity falcon = (QuantumFalconEntity) other;
        if (!java.util.Objects.equals(this.getOwnerUUID(), falcon.getOwnerUUID())) return false;
        if (this.isMale() == falcon.isMale()) return false;
        if (this.isBaby() || falcon.isBaby()) return false;
        if (this.breedingCooldownTicks > 0 || falcon.breedingCooldownTicks > 0) return false;
        if (this.isOrderedToSit() || falcon.isOrderedToSit()) return false;
        return this.isInLove() && falcon.isInLove();
    }

    // ========================================
    // TICK & AI
    // ========================================
    
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) tickFlightPitch();
        
        // Update animation
        prevFlapProgress = flapProgress;
        if (isFlying()) {
            flapProgress = (flapProgress + 0.4f) % 1.0f;
            
            // Play wing flapping sounds while flying (not on client side)
            if (!this.level().isClientSide) {
                this.playWingFlapSound();
            }
        } else {
            flapProgress = 0;
        }
        
        if (!this.level().isClientSide() && breedingCooldownTicks > 0) {
            breedingCooldownTicks--;
        }
        if (!this.level().isClientSide()) {
            tickStamina();
            int cd = getAbilityCooldown();
            if (cd > 0) setAbilityCooldown(cd - (isOnQuantumPerch() ? 2 : 1));
            if (staticChargeCooldown > 0) staticChargeCooldown--;
            if (abilityAnimTicks > 0) abilityAnimTicks--;
            if (this.tickCount % 20 == 0 && isTame() && getOwner() != null)
                com.matt.quantumfalcon.entity.ability.FalconAbilities.tickPassive(this, getOwner());
        }
        // Flying state management
        if (!this.onGround() && !this.isInWater()) {
            setFlying(true);
        } else if (this.onGround()) {
            setFlying(false);
        }
        
        // Client-side particles for legendary variants
        if (this.level().isClientSide && isLegendary()) {
            spawnAnomalyParticles();
        }
        
        // Flight trail: a faint variant-coloured glint off each wingtip, only while actually moving fast.
        // (Replaces a per-tick END_ROD spawn at the body, which built a white cloud that hid the model.)
        if (this.level().isClientSide && isFlying() && !onGround() && this.tickCount % 3 == 0 && this.getDeltaMovement().lengthSqr() > 0.04) {
            float yaw = this.yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD;
            double span = 0.55 * com.matt.quantumfalcon.entity.FalconArchetype.of(this).size * (this.isBaby() ? 0.5 : 1.0);
            double cx = Math.cos(yaw), cz = Math.sin(yaw);
            var opt = com.matt.quantumfalcon.item.weapon.WeaponFx.col(com.matt.quantumfalcon.registry.ModParticles.GLINT.get(), accentRgb());
            for (int s = -1; s <= 1; s += 2)
                this.level().addParticle(opt, getX() + cx * span * s, getY() + 0.45, getZ() + cz * span * s, 0, 0, 0);
        }
    }
    
    private static final java.util.Map<String, Integer> ACCENT = java.util.Map.ofEntries(
            java.util.Map.entry("peregrine", 0x00F0FF), java.util.Map.entry("voidstalker", 0xA640FF), java.util.Map.entry("aetherwing", 0x7FE4FF), java.util.Map.entry("coral", 0xFF7A66), java.util.Map.entry("crimson", 0xFF3030), java.util.Map.entry("deepslate", 0x9AB4CC), java.util.Map.entry("entangled_prism", 0xFF58E0), java.util.Map.entry("mushroom", 0xFF4040), java.util.Map.entry("prairie", 0xF0C860), java.util.Map.entry("soulfire", 0x40F4FF), java.util.Map.entry("tempest", 0xB8E6FF), java.util.Map.entry("tundra", 0x9AE0FF), java.util.Map.entry("abyssal", 0x20B0FF), java.util.Map.entry("cosmic", 0xC080FF), java.util.Map.entry("quantum_phoenix", 0xFF8A20), java.util.Map.entry("singularity", 0xFFFFFF), java.util.Map.entry("stormrider", 0x80C8FF), java.util.Map.entry("warped", 0x30FFC8));

    /** Emissive accent colour of this falcon's variant (matches the v2 texture palette). */
    public int accentRgb() {
        String v = getVariant() == null ? "peregrine" : getVariant().toLowerCase();
        return ACCENT.getOrDefault(v, 0x00F0FF);
    }

    private void spawnAnomalyParticles() {
        if (this.tickCount % 5 == 0) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 1.5;
            double y = this.getY() + this.random.nextDouble() * 1.5;
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 1.5;
            this.level().addParticle(ParticleTypes.PORTAL, x, y, z, 
                (this.random.nextDouble() - 0.5) * 0.5, 
                this.random.nextDouble() * 0.5, 
                (this.random.nextDouble() - 0.5) * 0.5);
        }
    }

    public static final int MOLT_MIN = 6000;
    /** Ticks to the next molt; starts at -1 so a freshly loaded falcon schedules one instead of dropping at once. */
    public int moltTicks = -1;

    @Override
    public void aiStep() {
        super.aiStep();
        if (attackTicks > 0) attackTicks--;
        if (hurtTicks > 0) hurtTicks--;
        // tamed adults molt a Falcon Feather every 5-10 minutes (the Codex and Twig Nest need them; no killing the trader)
        if (!this.level().isClientSide && this.isTame() && !this.isBaby() && this.isAlive() && --moltTicks <= 0) {
            if (moltTicks == 0) this.spawnAtLocation(com.matt.quantumfalcon.registry.ModItems.FALCON_FEATHER.get());
            moltTicks = MOLT_MIN + this.random.nextInt(MOLT_MIN);
        }
        if (!this.level().isClientSide && this.tickCount % 20 == 0 && this.isTame())
            com.matt.quantumfalcon.breeding.AbilityEngine.tickSecond(this, getGenome(), genomeCooldowns);
        if (!this.level().isClientSide) { duskTick(); if (this.tickCount % 40 == 5 && this.isTame()) endingTick(); }
        else endingParticles();
        // Slow fall when flying
        if (isFlying() && !this.isNoGravity()) {
            Vec3 motion = this.getDeltaMovement();
            if (motion.y < -0.1) {
                this.setDeltaMovement(motion.x, motion.y * 0.6, motion.z);
            }
        }
    }

    // ========================================
    // DAMAGE & COMBAT
    // ========================================
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        var bt = bardingTier();
        if (bt != null && bt.shieldChance > 0 && !this.level().isClientSide && this.random.nextFloat() < bt.shieldChance
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            return false;   // quantum barding absorbs the hit
        }
        hurtTicks = 8;
        this.setOrderedToSit(false);
        if (!this.level().isClientSide && this.isTame()) {
            var g = getGenome();
            amount *= com.matt.quantumfalcon.breeding.AbilityEngine.loyaltyReduction(this, g);
            if (amount >= getHealth() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                    && com.matt.quantumfalcon.breeding.AbilityEngine.preventDeath(this, g, genomeCooldowns)) return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity) {
            addBondLevel(1); // Bond increases from combat
            // Play screech on attack (20% chance to not spam)
            if (this.random.nextInt(5) == 0) {
                this.playScreech();
            }
        }
        if (hit && target instanceof LivingEntity lt && getOwner() != null && com.matt.quantumfalcon.item.weapon.Weapons.FalconerGauntlet.holds(getOwner())) {
            lt.invulnerableTime = 0;
            lt.hurt(this.damageSources().mobAttack(this), 2f);   // Falconer's Gauntlet: +2 damage
        }
        if (hit) { attackTicks = 12; this.level().broadcastEntityEvent(this, ATTACK_ANIM_EVENT); }
        if (hit && target instanceof LivingEntity glt && this.isTame())
            com.matt.quantumfalcon.breeding.AbilityEngine.onHit(this, getGenome(), glt, (float) getAttributeValue(Attributes.ATTACK_DAMAGE));   // client plays attack/strike
        if (hit && target instanceof LivingEntity && ability() == com.matt.quantumfalcon.entity.ability.FalconAbility.SOULFIRE) target.igniteForSeconds(4);
        if (hit && bardingTier() != null && bardingTier().lifesteal > 0) heal((float) getAttributeValue(Attributes.ATTACK_DAMAGE) * bardingTier().lifesteal);
        return hit;
    }

    // ========================================
    // SOUNDS
    // ========================================
    
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.FALCON_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.FALCON_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.FALCON_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Falcons don't make step sounds when flying
        if (!this.isFlying()) {
            this.level().playSound(null, this.blockPosition(), ModSounds.FALCON_FLAP.get(), 
                net.minecraft.sounds.SoundSource.NEUTRAL, 0.15F, 1.0F);
        }
    }
    
    /**
     * Plays falcon screech sound (for attack or commanded behavior)
     */
    public void playScreech() {
        this.playSound(ModSounds.FALCON_SCREECH.get(), 1.5F, 1.0F);
    }
    
    /**
     * Plays wing flapping sound during flight
     */
    public void playWingFlapSound() {
        if (this.isFlying() && this.random.nextInt(10) == 0) {
            this.playSound(ModSounds.FALCON_FLAP.get(), 0.6F, 1.0F);
        }
    }
    
    /**
     * Plays taming success sound
     */
    public void playTameSound() {
        this.playSound(ModSounds.FALCON_TAME.get(), 1.0F, 1.0F);
    }
    
    /**
     * Plays bond level increase sound
     */
    public void playBondUpSound() {
        this.playSound(ModSounds.FALCON_BOND_UP.get(), 1.0F, 1.0F);
    }
    
    /**
     * Plays quantum ability sound (blink/teleport)
     */
    public void playQuantumBlinkSound() {
        this.playSound(ModSounds.QUANTUM_BLINK.get(), 1.0F, 1.0F);
    }

    // ========================================
    // FLYING ANIMAL INTERFACE
    // ========================================
    
    @Override
    public boolean isFlapping() {
        return isFlying();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // No fall damage for flying creature
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false; // Immune to fall damage
    }

    // ========================================
    // ANIMATION HELPERS
    // ========================================
    
    public float getFlapProgress(float partialTicks) {
        return Mth.lerp(partialTicks, prevFlapProgress, flapProgress);
    }
    
    // ========================================
    // GECKOLIB GEOENTITY INTERFACE
    // ========================================
    
    // quantum falcon 2 geo (art/blockbench) — bone names match animations (idle, walk, fly, glide, attack, sit, sleep)
    /** Client-only smoothed body pitch (degrees, + = nose down) driven by vertical speed while flying. */
    public float flightPitch, flightPitchO;

    /** Client-only animation state: airborne edge detection (takeoff / landing), smoothed yaw rate and bank roll. */
    private boolean animPrevAirborne;
    public int takeoffTicks, landTicks;
    public float yawRate, bankRoll, bankRollO;

    /** Called from the client tick: ease the pitch toward the climb/descent angle, track transitions and banking. */
    public void tickFlightPitch() {
        flightPitchO = flightPitch;
        boolean air = isFlying() && !onGround();
        float target = air ? (float) net.minecraft.util.Mth.clamp(-getDeltaMovement().y * 70.0, -25.0, 35.0) : 0f;
        flightPitch += (target - flightPitch) * 0.2f;
        if (air && !animPrevAirborne && this.tickCount > 5) { takeoffTicks = 18; landTicks = 0; }
        if (!air && animPrevAirborne) { landTicks = 18; takeoffTicks = 0; }
        animPrevAirborne = air;
        if (takeoffTicks > 0) takeoffTicks--;
        if (landTicks > 0) landTicks--;
        float dy = net.minecraft.util.Mth.wrapDegrees(getYRot() - yRotO);
        yawRate += (dy - yawRate) * 0.3f;
        bankRollO = bankRoll;
        float rollTarget = air ? net.minecraft.util.Mth.clamp(yawRate * 3.5f, -30f, 30f) : 0f;
        bankRoll += (rollTarget - bankRoll) * 0.15f;
    }

    /** Rare variants (Echo Rift and legendary) get the neon flourish and the stronger crack pulse. */
    public static final java.util.Set<String> RARE_VARIANTS = java.util.Set.of("singularity", "cosmic", "quantum_phoenix", "entangled_prism",
            "voidstalker", "abyssal", "soulfire", "warped");
    public static final java.util.Set<String> LEGENDARY_VARIANTS = java.util.Set.of("singularity", "cosmic", "quantum_phoenix");
    public boolean isRareVariant() { return getVariant() != null && RARE_VARIANTS.contains(getVariant().toLowerCase()); }
    public boolean isLegendaryVariant() { return getVariant() != null && LEGENDARY_VARIANTS.contains(getVariant().toLowerCase()); }
    /** 0..1 progress through the periodic idle flourish (rare variants on the ground, every 30 s, 2.4 s long); -1 when not in one. */
    public float flourishProgress(float partialTick) {
        if (!isRareVariant()) return -1f;
        float t = ((this.tickCount + this.getId() * 37) % 600) + partialTick;
        return t < 48f ? t / 48f : -1f;
    }

    // Animation set v4 (tools/falcon_anim_v4.py, written from scratch): see FALCONS.md "Animations".
    private static RawAnimation loop(String n) { return RawAnimation.begin().thenLoop("animation.quantum_falcon." + n); }
    private static final RawAnimation ANIM_IDLE = loop("idle"), ANIM_WALK = loop("walk"), ANIM_FOLLOW = loop("follow"), ANIM_FLY = loop("fly"),
            ANIM_FLY_FAST = loop("fly_fast"), ANIM_GLIDE = loop("glide"), ANIM_BANK_L = loop("bank_l"), ANIM_BANK_R = loop("bank_r"),
            ANIM_HOVER = loop("hover"), ANIM_DIVE = loop("dive"), ANIM_SIT = loop("sit"), ANIM_SLEEP = loop("sleep"), ANIM_FX = loop("fx");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("animation.quantum_falcon.attack");
    private static final RawAnimation ANIM_STRIKE = RawAnimation.begin().thenPlay("animation.quantum_falcon.strike").thenLoop("animation.quantum_falcon.fly");
    private static final RawAnimation ANIM_TAKEOFF = RawAnimation.begin().thenPlay("animation.quantum_falcon.takeoff").thenLoop("animation.quantum_falcon.fly");
    private static final RawAnimation ANIM_LAND = RawAnimation.begin().thenPlay("animation.quantum_falcon.land").thenLoop("animation.quantum_falcon.idle");
    private static final RawAnimation ANIM_FLOURISH = RawAnimation.begin().thenPlay("animation.quantum_falcon.flourish").thenLoop("animation.quantum_falcon.idle");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, event -> {
            // Screenshot/debug hook: a falcon named "qfpose:<anim>" holds that animation (e.g. qfpose:dive)
            if (this.hasCustomName()) {
                String nm = this.getCustomName().getString();
                if (nm.startsWith("qfpose:")) return event.setAndContinue(loop(nm.substring(7)));
            }
            boolean air = this.isFlying() && !this.onGround();   // a stale Flying flag on a grounded falcon (e.g. NoAI) must not play flight poses
            if (attackTicks > 0) return event.setAndContinue(air ? ANIM_STRIKE : ANIM_ATTACK);
            if (air) {
                if (takeoffTicks > 0) return event.setAndContinue(ANIM_TAKEOFF);
                var v = this.getDeltaMovement();
                double h = v.horizontalDistanceSqr();
                if (v.y < -0.45) return event.setAndContinue(ANIM_DIVE);                                   // stoop
                if (h < 0.0016 && Math.abs(v.y) < 0.1) return event.setAndContinue(ANIM_HOVER);            // holding station
                if (Math.abs(yawRate) > 5f) return event.setAndContinue(yawRate > 0 ? ANIM_BANK_R : ANIM_BANK_L);
                if (v.y < -0.06 && h > 0.01) return event.setAndContinue(ANIM_GLIDE);
                if (h > 0.30) return event.setAndContinue(ANIM_FLY_FAST);
                return event.setAndContinue(ANIM_FLY);
            }
            if (landTicks > 0) return event.setAndContinue(ANIM_LAND);
            if (this.isInSittingPose() || this.isResting()) return event.setAndContinue(this.level().isNight() ? ANIM_SLEEP : ANIM_SIT);
            double g = this.getDeltaMovement().horizontalDistanceSqr();
            if (g > 0.012) return event.setAndContinue(ANIM_FOLLOW);        // hop-run (following the owner)
            if (g > 1.0E-6) return event.setAndContinue(ANIM_WALK);
            if (flourishProgress(0) >= 0f) return event.setAndContinue(ANIM_FLOURISH);
            return event.setAndContinue(ANIM_IDLE);
        }));
        // Accessory motion (halo / event horizon spin, streamers, plumes, flame flicker) runs on its own layer
        controllers.add(new AnimationController<>(this, "fx", 0, event -> event.setAndContinue(ANIM_FX)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
