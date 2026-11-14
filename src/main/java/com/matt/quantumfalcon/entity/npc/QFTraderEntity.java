package com.matt.quantumfalcon.entity.npc;

import com.matt.quantumfalcon.genetics.FalconGenetics;
import com.matt.quantumfalcon.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Shared base for the travelling Quantum Falcon traders: trading UI, wandering AI, despawn timer, GeckoLib body. */
public abstract class QFTraderEntity extends AbstractVillager implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String animId;
    /** Ticks until the trader leaves (wanders off). Persisted. */
    protected int despawnDelay = 48000;

    protected QFTraderEntity(EntityType<? extends AbstractVillager> type, Level level, String animId) {
        super(type, level);
        this.animId = animId;
    }

    public static AttributeSupplier.Builder createAttributes(double hp, double speed) {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, hp).add(Attributes.MOVEMENT_SPEED, speed).add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 8f, 0.6, 0.7));
        goalSelector.addGoal(1, new PanicGoal(this, 0.6));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.4));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isTrading() || isBaby()) return super.mobInteract(player, hand);
        if (!level().isClientSide) {
            if (getOffers().isEmpty()) return InteractionResult.CONSUME;
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
            triggerAnim("action", "trade");
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp()) {
            int xp = 3 + random.nextInt(4);
            level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(level(), getX(), getY() + 0.5, getZ(), xp));
        }
        triggerAnim("action", "celebrate");
    }

    @Override public boolean showProgressBar() { return false; }
    @Override public boolean canRestock() { return true; }
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected SoundEvent getTradeUpdatedSound(boolean ok) { return ok ? SoundEvents.WANDERING_TRADER_YES : SoundEvents.WANDERING_TRADER_NO; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.WANDERING_TRADER_YES; }
    /** Audio-pack voice set: "goblin" or "merchant" (trade yes/no stay on the trader cues). */
    protected String soundId() { return "goblin"; }
    @Override protected SoundEvent getAmbientSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.ambient(soundId()); }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.matt.quantumfalcon.entity.util.QFMobSounds.hurt(soundId()); }
    @Override protected SoundEvent getDeathSound() { return com.matt.quantumfalcon.entity.util.QFMobSounds.death(soundId()); }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.matt.quantumfalcon.entity.util.QFMobSounds.step(soundId()), 0.5f, 1f); }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && !isTrading() && --despawnDelay <= 0) discard();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("DespawnDelay", despawnDelay); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("DespawnDelay")) despawnDelay = tag.getInt("DespawnDelay");
        capEggOffers();
    }

    /** traders saved before the cap may still hold a Legendary/Mythic egg offer: swap it for a capped egg. Returns how many. */
    public int capEggOffers() {
        int n = 0;
        var offers = getOffers();
        for (int i = 0; i < offers.size(); i++) {
            var o = offers.get(i);
            String v = eggVariant(o.getResult());
            if (v == null || variantTier(v) <= maxEggTier()) continue;
            offers.set(i, new net.minecraft.world.item.trading.MerchantOffer(o.getItemCostA(), o.getItemCostB(), falconEgg(v, random), o.getMaxUses(), o.getXp(), o.getPriceMultiplier()));
            n++;
        }
        return n;
    }

    /** no trader sells an egg above this tier (5 = Epic); Legendary+ eggs are boss/cache only. */
    public static final int MAX_TRADER_EGG_TIER = 5;
    /** The configured cap (WorldConfig eggs_and_relics.traderEggMaxTier), never above {@link #MAX_TRADER_EGG_TIER}. */
    public static int maxEggTier() { return Math.min(MAX_TRADER_EGG_TIER, com.matt.quantumfalcon.WorldConfig.traderEggMaxTier()); }

    public static int variantTier(String variant) {
        for (var d : com.matt.quantumfalcon.breeding.BreedingData.VARIANTS) if (d.id().equals(variant)) return d.tier();
        return 0;
    }

    /** Wild (non-hybrid) variants with tier in [min, max]. */
    public static java.util.List<String> wildVariants(int min, int max) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (var d : com.matt.quantumfalcon.breeding.BreedingData.VARIANTS)
            if (d.parentA() == null && d.tier() >= min && d.tier() <= max && !d.habitat().startsWith("Egg only")) out.add(d.id());
        return out;
    }

    /** Variant an egg stack would hatch into (from its trade data), or null. */
    public static String eggVariant(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (d == null) return null;
        CompoundTag t = d.copyTag();
        return t.contains("Variant") ? t.getString("Variant") : null;
    }

    /** an Echo Rift variant below the trader cap (tier 4); Entangled Prism (tier 5) is only the rare roll. */
    public static String commonRiftVariant(net.minecraft.util.RandomSource rnd) {
        java.util.List<String> pool = new java.util.ArrayList<>();
        for (String c : com.matt.quantumfalcon.WorldConfig.traderEggPool()) if (variantTier(c) > 0 && variantTier(c) <= maxEggTier()) pool.add(c);   // config pool
        if (pool.isEmpty()) for (String c : FalconGenetics.RIFT_COLORS) if (variantTier(c) < Math.max(2, maxEggTier())) pool.add(c);
        if (pool.isEmpty()) pool.addAll(java.util.Arrays.asList(FalconGenetics.RIFT_COLORS));
        return pool.get(rnd.nextInt(pool.size()));
    }

    /**
     * A falcon egg whose two "parents" are the given variant, so it hatches into that variant (barring a breeding mutation).
     * a variant above {@link #MAX_TRADER_EGG_TIER} is replaced by a tier-4 Echo Rift variant.
     */
    public static ItemStack falconEgg(String variant, net.minecraft.util.RandomSource rnd) {
        if (variantTier(variant) > maxEggTier()) variant = commonRiftVariant(rnd);
        ItemStack egg = new ItemStack(ModItems.FALCON_EGG.get());
        // the egg carries a real child genome at the sold variant and tier, so it hatches what the label says
        com.matt.quantumfalcon.item.FalconEggItem.wild(egg, com.matt.quantumfalcon.breeding.BreedingEngine.wild(variant, variantTier(variant), rnd));
        CompoundTag t = egg.get(DataComponents.CUSTOM_DATA).copyTag();
        t.putString("Variant", variant);
        t.put("Parent1Genetics", FalconGenetics.forVariant(variant, rnd).serialize());
        t.put("Parent2Genetics", FalconGenetics.forVariant(variant, rnd).serialize());
        egg.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return egg;
    }

    // ---------------------------------------------------------------- GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation." + animId + ".idle");
        RawAnimation walk = RawAnimation.begin().thenLoop("animation." + animId + ".walk");
        controllers.add(new AnimationController<>(this, "move", 4, s -> s.setAndContinue(s.isMoving() ? walk : idle)));
        controllers.add(new AnimationController<>(this, "action", 2, s -> PlayState.STOP)
                .triggerableAnim("trade", RawAnimation.begin().thenPlay("animation." + animId + ".trade"))
                .triggerableAnim("celebrate", RawAnimation.begin().thenPlay("animation." + animId + ".celebrate")));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
