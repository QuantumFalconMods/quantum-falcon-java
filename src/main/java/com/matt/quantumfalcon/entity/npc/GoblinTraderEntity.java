package com.matt.quantumfalcon.entity.npc;

import com.matt.quantumfalcon.genetics.FalconGenetics;
import com.matt.quantumfalcon.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Goblin Trader (Matthew's design: a wandering goblin who sells "stolen" falcon eggs).
 * Sells two random Overworld-variant eggs (10% chance one is an Echo Rift variant), falcon treats and quantum berries;
 * buys falcon feathers. Leaves after two in-game days.
 */
public class GoblinTraderEntity extends QFTraderEntity {
    public GoblinTraderEntity(EntityType<? extends AbstractVillager> type, Level level) { super(type, level, "goblin_trader"); }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        for (int i = 0; i < 2; i++) {
            boolean rift = random.nextFloat() < 0.10f;
            String[] pool = rift ? FalconGenetics.RIFT_COLORS : FalconGenetics.COMMON_COLORS;
            String v = pool[random.nextInt(pool.length)];
            offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, rift ? 32 : 16), Optional.of(new ItemCost(Items.GOLD_INGOT, rift ? 8 : 4)),
                    falconEgg(v, random), 1, 20, 0.05f));
        }
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 2), new ItemStack(ModItems.FALCON_TREAT.get(), 4), 12, 2, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 6), new ItemStack(ModItems.QUANTUM_BERRY.get(), 2), 6, 5, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(ModItems.FALCON_FEATHER.get(), 6), new ItemStack(Items.EMERALD, 1), 16, 2, 0.05f));
    }
}
