package com.matt.quantumfalcon.entity.boss;

import com.matt.quantumfalcon.breeding.BreedingData;
import com.matt.quantumfalcon.breeding.BreedingEngine;
import com.matt.quantumfalcon.breeding.FalconGenome;
import com.matt.quantumfalcon.item.FalconEggItem;
import com.matt.quantumfalcon.registry.ModDataComponents;
import com.matt.quantumfalcon.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * the weak-point "blessed" egg. One per fight (the boss remembers it), only to a player who has never had
 * one from that boss (a farm limit), handed straight to the hitter (not dropped into a void pit), and it really is
 * Legendary: the genome is rolled at Legendary tier when the egg is made, and FalconEggItem re-checks the flag on placing.
 */
public final class BlessedEggs {
    public static final int LEGENDARY_TIER = 6;
    private BlessedEggs() {}

    /** @return true if an egg was given. */
    public static boolean give(Player player, String bossKey, String eggName) {
        String flag = "qf_blessed_egg_" + bossKey;
        var pers = com.matt.quantumfalcon.util.PersistedTag.of(player, flag);   // survives death
        if (pers.getBoolean(flag)) return false;
        pers.putBoolean(flag, true);
        ItemStack egg = legendaryEgg(player.getRandom(), eggName);
        if (!player.getInventory().add(egg)) player.drop(egg, false);
        player.displayClientMessage(Component.literal("✦ " + eggName + " egg: a Legendary falcon egg is yours").withStyle(ChatFormatting.GOLD), false);
        return true;
    }

    public static ItemStack legendaryEgg(RandomSource r, String eggName) {
        var v = BreedingData.VARIANTS.get(r.nextInt(BreedingData.VARIANTS.size()));
        FalconGenome g = BreedingEngine.wild(v.id(), LEGENDARY_TIER, r);
        ItemStack egg = FalconEggItem.wild(new ItemStack(ModItems.FALCON_EGG.get()), g);
        egg.set(ModDataComponents.LEGENDARY_GUARANTEED.get(), true);
        egg.set(ModDataComponents.FALCON_NAME.get(), eggName);
        return egg;
    }

    /** Placing a flagged egg: make sure the child is at least Legendary (old stacks carried only the flag). */
    public static FalconGenome ensureLegendary(FalconGenome g, RandomSource r) {
        if (g == null) return BreedingEngine.wild("peregrine", LEGENDARY_TIER, r);
        if (g.tier < LEGENDARY_TIER) {
            FalconGenome up = BreedingEngine.wild(g.variant, LEGENDARY_TIER, r);
            return up;
        }
        return g;
    }
}
