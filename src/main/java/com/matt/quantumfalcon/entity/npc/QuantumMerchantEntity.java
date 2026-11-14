package com.matt.quantumfalcon.entity.npc;

import com.matt.quantumfalcon.genetics.FalconGenetics;
import com.matt.quantumfalcon.registry.ModGearItems;
import com.matt.quantumfalcon.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Quantum Merchant (Matthew's design: sells rare falcon eggs and progression items and refreshes its stock).
 * Found in Echo Rift biomes. Restocks every in-game day and rerolls its rare egg.
 */
public class QuantumMerchantEntity extends QFTraderEntity {
    private long lastRestockDay = -1;

    public QuantumMerchantEntity(EntityType<? extends AbstractVillager> type, Level level) { super(type, level, "quantum_merchant"); despawnDelay = 72000; }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        offers.add(rareEggOffer());
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 12), new ItemStack(ModItems.QUANTUM_CRYSTAL.get(), 2), 8, 8, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 20), new ItemStack(ModItems.RIFT_CRYSTAL.get(), 1), 6, 10, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 24), Optional.of(new ItemCost(Items.DIAMOND, 2)), new ItemStack(ModGearItems.VOID_STEEL_INGOT.get(), 1), 4, 15, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 16), new ItemStack(ModItems.GOLDEN_FALCON_FEATHER.get(), 1), 3, 12, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 30), new ItemStack(ModGearItems.FALCONER_GAUNTLET.get(), 1), 1, 20, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(ModItems.RIFT_SHARD.get(), 8), new ItemStack(Items.EMERALD, 3), 12, 4, 0.05f));
    }

    /**
     * the rare egg is the best wild egg a trader may sell, tier 5 at most (Entangled Prism, 5%); otherwise an
     * Echo Rift tier-4 variant. Legendary and Mythic eggs (cosmic, singularity, Quantum Phoenix) are never for sale: they
     * come only from bosses and caches under the limits.
     */
    public MerchantOffer rareEggOffer() {
        float r = random.nextFloat();
        java.util.List<String> top = wildVariants(maxEggTier(), maxEggTier());
        String v = r < 0.05f && !top.isEmpty() ? top.get(random.nextInt(top.size()))
            : commonRiftVariant(random);   // tier-4 only, so the tier-5 egg really is 5%
        boolean best = variantTier(v) >= maxEggTier();
        return new MerchantOffer(new ItemCost(Items.EMERALD, best ? 56 : 40), Optional.of(new ItemCost(Items.DIAMOND, best ? 10 : 6)),
                falconEgg(v, random), 1, 30, 0.05f);
    }

    /** Daily restock: every offer's uses are reset and the rare egg is rerolled. Returns true when a restock happened. */
    public boolean dailyRestock(long day) {
        if (day == lastRestockDay) return false;
        lastRestockDay = day;
        MerchantOffers offers = getOffers();
        offers.forEach(MerchantOffer::resetUses);
        if (!offers.isEmpty()) offers.set(0, rareEggOffer());
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && tickCount % 100 == 0 && !isTrading()) dailyRestock(level().getDayTime() / 24000L);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putLong("LastRestockDay", lastRestockDay); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); lastRestockDay = tag.getLong("LastRestockDay"); }
    @Override protected String soundId() { return "merchant"; }
}
