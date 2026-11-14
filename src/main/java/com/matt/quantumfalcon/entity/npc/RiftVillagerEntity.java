package com.matt.quantumfalcon.entity.npc;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Villager MVP: the Rift-born villagers of the Stitchring (art: villagers v3). One entity, seven professions; the Falconer in
 * vanilla villages stays a vanilla profession (drawn with the same v3 model by FalconerVillagerRenderer).
 * They live where the village put them (no despawn), rank up with trade XP like vanilla (Novice to Master) and their gear
 * grows with the rank (rank props hidden below their tier by the renderer).
 */
public class RiftVillagerEntity extends QFTraderEntity {
    public static final List<String> PROFESSIONS = List.of("falconer", "anchorkeeper", "void_reforger", "pedigree_registrar", "aerie_mender", "wayfinder", "chronicler");
    /** Vanilla villager XP thresholds to reach rank 2..5. */
    static final int[] RANK_XP = {10, 70, 150, 250};

    private static final EntityDataAccessor<String> DATA_PROFESSION = SynchedEntityData.defineId(RiftVillagerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_TIER = SynchedEntityData.defineId(RiftVillagerEntity.class, EntityDataSerializers.INT);
    private int tradeXp;

    public RiftVillagerEntity(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level, "rift_villager");
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DATA_PROFESSION, "falconer");
        b.define(DATA_TIER, 1);
    }

    public String profession() { return entityData.get(DATA_PROFESSION); }
    public int tier() { return entityData.get(DATA_TIER); }
    public void setProfession(String p) { if (PROFESSIONS.contains(p)) { entityData.set(DATA_PROFESSION, p); this.offers = null; } }
    public void setTier(int t) { entityData.set(DATA_TIER, Math.max(1, Math.min(5, t))); }

    @Override
    public net.minecraft.network.chat.Component getTypeName() {
        return net.minecraft.network.chat.Component.translatable("entity.quantumfalcon.rift_villager." + profession());
    }

    @Override
    public void aiStep() {
        despawnDelay = Integer.MAX_VALUE;   // residents stay
        super.aiStep();
        // Reforger idle (v3): thumbs the hammer face and an amber spark pops off. Client-only particle: no light, no fire.
        if (level().isClientSide && "void_reforger".equals(profession()) && !isTrading() && tickCount % 80 == 17 && getDeltaMovement().horizontalDistanceSqr() < 1.0E-4) {
            double a = Math.toRadians(yBodyRot), hx = getX() - Math.cos(a) * 0.36 - Math.sin(a) * 0.08, hz = getZ() - Math.sin(a) * 0.36 + Math.cos(a) * 0.08;
            for (int i = 0; i < 3; i++)
                level().addParticle(new DustParticleOptions(new org.joml.Vector3f(1.0f, 0.59f, 0.16f), 0.5f), hx, getY() + 0.45, hz,
                    (random.nextDouble() - 0.5) * 0.05, 0.06, (random.nextDouble() - 0.5) * 0.05);
        }
    }

    // ------------------------------------------------------------- trades (2 per rank for the MVP three, 1-2 for the rest)
    private static ItemStack it(String id, int n) {
        Item i = BuiltInRegistries.ITEM.get(id.contains(":") ? ResourceLocation.parse(id) : ResourceLocation.fromNamespaceAndPath("quantumfalcon", id));
        return new ItemStack(i == Items.AIR ? Items.EMERALD : i, n);
    }
    private static MerchantOffer buy(int emeralds, String id, int n, int uses, int xp) { return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), it(id, n), uses, xp, 0.05f); }
    private static MerchantOffer sell(String id, int n, int emeralds, int uses, int xp) { return new MerchantOffer(new ItemCost(it(id, 1).getItem(), n), new ItemStack(Items.EMERALD, emeralds), uses, xp, 0.05f); }

    /** Trades a profession unlocks at a rank (1..5). */
    public List<MerchantOffer> offersFor(String prof, int rank) {
        return switch (prof + ":" + rank) {
            case "falconer:1" -> List.of(buy(2, "falcon_treat", 4, 12, 2), sell("falcon_feather", 6, 1, 16, 2));
            case "falconer:2" -> List.of(buy(8, "leather_falcon_barding", 1, 4, 10), buy(6, "falcon_whistle", 1, 4, 10));
            case "falconer:3" -> List.of(buy(14, "iron_falcon_barding", 1, 3, 15), buy(4, "quantum_berry", 2, 8, 10));
            case "falconer:4" -> List.of(new MerchantOffer(new ItemCost(Items.EMERALD, 24), Optional.of(new ItemCost(Items.GOLD_INGOT, 4)), falconEgg(commonRiftVariant(random), random), 1, 25, 0.05f));
            case "falconer:5" -> List.of(buy(28, "diamond_falcon_barding", 1, 2, 30));
            case "anchorkeeper:1" -> List.of(buy(2, "stability_shard", 1, 12, 2), sell("rift_shard", 4, 1, 16, 2));
            case "anchorkeeper:2" -> List.of(buy(10, "quantum_stabilizer", 1, 4, 10), sell("echo_shard", 6, 1, 12, 5));
            case "anchorkeeper:3" -> List.of(buy(12, "echo_compass", 1, 3, 15), buy(8, "stability_shard", 4, 6, 10));
            case "anchorkeeper:4" -> List.of(buy(20, "rift_totem", 1, 2, 25));
            case "anchorkeeper:5" -> List.of(buy(24, "skill_respec_book", 1, 1, 30));
            case "void_reforger:1" -> List.of(sell("void_shard", 8, 1, 16, 2), buy(3, "temporal_dust", 4, 12, 2));
            case "void_reforger:2" -> List.of(buy(6, "void_crystal", 1, 8, 10), sell("void_essence", 4, 2, 12, 5));
            case "void_reforger:3" -> List.of(buy(10, "temporal_alloy", 1, 6, 15), buy(8, "purple_ingot", 1, 6, 10));
            case "void_reforger:4" -> List.of(buy(18, "quantum_capacitor", 1, 3, 25));
            case "void_reforger:5" -> List.of(buy(30, "void_blade", 1, 1, 30));
            case "pedigree_registrar:1" -> List.of(buy(4, "quantum_codex", 1, 4, 2));
            case "pedigree_registrar:3" -> List.of(sell("golden_falcon_feather", 1, 6, 8, 15));
            case "pedigree_registrar:5" -> List.of(buy(16, "shiny_charm", 1, 1, 30));
            case "aerie_mender:1" -> List.of(buy(3, "quantum_berry", 2, 12, 2), buy(2, "falcon_treat", 3, 12, 2));
            case "aerie_mender:3" -> List.of(buy(6, "resonance_flower", 1, 6, 15));
            case "wayfinder:1" -> List.of(buy(6, "quantum_compass", 1, 4, 2));
            case "wayfinder:3" -> List.of(buy(12, "echo_compass", 1, 3, 15));
            case "wayfinder:5" -> List.of(buy(20, "treasure_compass", 1, 2, 30));
            case "chronicler:1" -> List.of(sell("temporal_dust", 8, 1, 16, 2));
            case "chronicler:3" -> List.of(buy(10, "time_bottle", 1, 3, 15));
            case "chronicler:5" -> List.of(buy(24, "skill_respec_book", 1, 1, 30));
            default -> List.of();
        };
    }

    @Override
    protected void updateTrades() {
        for (int r = 1; r <= tier(); r++) getOffers().addAll(offersFor(profession(), r));
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        super.rewardTradeXp(offer);
        tradeXp += offer.getXp();
        int t = tier();
        if (t < 5 && tradeXp >= RANK_XP[t - 1]) {
            setTier(t + 1);
            getOffers().addAll(offersFor(profession(), t + 1));
            level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.VILLAGER_CELEBRATE, getSoundSource(), 1f, 1f);
        }
    }

    @Override protected String soundId() { return "merchant"; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Profession", profession()); tag.putInt("Tier", tier()); tag.putInt("TradeXp", tradeXp);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Profession")) entityData.set(DATA_PROFESSION, tag.getString("Profession"));
        if (tag.contains("Tier")) setTier(tag.getInt("Tier"));
        tradeXp = tag.getInt("TradeXp");
        super.readAdditionalSaveData(tag);
    }

    /** Tier-1 offers are generated on first open; keep the map for docs/tests. */
    public static final Map<String, String> JOB_SITE = Map.of("falconer", "falcon_perch", "anchorkeeper", "echo_altar", "void_reforger", "void_anvil");
}
