package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.codex.RiftCodex;
import com.matt.quantumfalcon.entity.npc.QFTraderEntity;
import com.matt.quantumfalcon.entity.npc.QuantumMerchantEntity;
import com.matt.quantumfalcon.item.FalconEggItem;
import com.matt.quantumfalcon.item.RerollTokenItem;
import com.matt.quantumfalcon.item.StabilityShardItem;
import com.matt.quantumfalcon.registry.ModEntities;
import com.matt.quantumfalcon.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** trader eggs are capped at tier 5 (no Phoenix); every Rift Codex reward has a use and no reward is an egg. */
public final class EconomyTests {
    private EconomyTests() {}

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("ec1_merchant_egg_cap", EconomyTests::merchant);
        tests.put("ec1_codex_rewards_useful", EconomyTests::codexRewards);
        tests.put("ec1_reward_item_uses", EconomyTests::itemUses);
        tests.put("r5_trader_egg_hatch", EconomyTests::traderEggHatch);
    }

    /** (a) 5000 rare-egg rolls, a stale saved Phoenix offer, and a direct Phoenix request: nothing above tier 5. */
    private static void merchant(ServerLevel level) {
        QuantumMerchantEntity m = ModEntities.QUANTUM_MERCHANT.get().create(level);
        if (m == null) { result("ec1_merchant_egg_cap", false, "no merchant"); return; }
        Map<String, Integer> seen = new TreeMap<>();
        int maxTier = 0;
        for (int i = 0; i < 5000; i++) {
            String v = QFTraderEntity.eggVariant(m.rareEggOffer().getResult());
            seen.merge(v, 1, Integer::sum);
            maxTier = Math.max(maxTier, QFTraderEntity.variantTier(v));
        }
        boolean phoenix = seen.containsKey("quantum_phoenix") || seen.containsKey("cosmic") || seen.containsKey("singularity");
        String forced = QFTraderEntity.eggVariant(QFTraderEntity.falconEgg("quantum_phoenix", level.random));
        boolean forcedCapped = QFTraderEntity.variantTier(forced) <= QFTraderEntity.MAX_TRADER_EGG_TIER;
        // an old merchant saved with a Phoenix egg offer (built by hand, bypassing the cap): the cap swaps it out
        ItemStack old = QFTraderEntity.falconEgg("warped", level.random);
        var tag = old.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
        tag.putString("Variant", "quantum_phoenix");
        old.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        m.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD, 64), Optional.of(new ItemCost(Items.DIAMOND, 16)), old, 1, 30, 0.05f));
        int swapped = m.capEggOffers();
        boolean offersCapped = m.getOffers().stream().allMatch(o -> { String v = QFTraderEntity.eggVariant(o.getResult()); return v == null || QFTraderEntity.variantTier(v) <= 5; });
        m.discard();
        int top = 0; for (var e : seen.entrySet()) if (QFTraderEntity.variantTier(e.getKey()) >= 5) top += e.getValue();
        double topRate = top / 5000.0;   // the tier-5 egg is the 5% roll, not 5% + a fifth of the rest
        boolean ok = maxTier <= 5 && !phoenix && forcedCapped && swapped == 1 && offersCapped && seen.containsKey("entangled_prism")
            && topRate >= 0.03 && topRate <= 0.075;
        result("ec1_merchant_egg_cap", ok, "5000 rolls: max tier " + maxTier + " tier-5 rate=" + String.format("%.1f%%", topRate * 100) + " " + seen + " | legendary/phoenix seen=" + phoenix
            + " | falconEgg(quantum_phoenix) -> " + forced + " | stale Phoenix offer swapped=" + swapped + " all offers <= tier 5=" + offersCapped);
    }

    /** a survival player places a bought egg (useOn) and it hatches the variant and tier it was sold as; an old stack
     *  saved before the fix (trade "Variant" only) hatches its variant too. */
    private static void traderEggHatch(ServerLevel level) {
        net.minecraft.core.BlockPos p = pad(level, 5800, 5600, net.minecraft.world.level.block.Blocks.STONE);
        cleanup(level, p, 8);
        FakePlayer fp = AuditTests.freshPlayer(level, "QFEggBuyer");
        fp.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 2.5);
        StringBuilder sb = new StringBuilder(); boolean ok = true; int i = 0;
        String[][] cases = { {"entangled_prism", "new"}, {"voidstalker", "new"}, {"warped", "legacy"}, {"peregrine", "new"} };
        for (String[] c : cases) {
            ItemStack egg = QFTraderEntity.falconEgg(c[0], level.random);
            if (c[1].equals("legacy")) {   // the pre-fix stack: trade data only, no QFEgg
                var t = egg.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag(); t.remove(FalconEggItem.TAG);
                egg.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(t));
            }
            String label = egg.getHoverName().getString();
            net.minecraft.core.BlockPos at = p.offset(-3 + 2 * i++, 0, 0);
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, egg);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at.below()).add(0, 0.5, 0), net.minecraft.core.Direction.UP, at.below(), false);
            egg.useOn(new net.minecraft.world.item.context.UseOnContext(fp, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
            String got = "not placed"; boolean good = false;
            if (level.getBlockEntity(at) instanceof com.matt.quantumfalcon.blockentity.FalconEggBlockEntity be) {
                be.forceHatch(level);
                var f = be.lastHatched;
                if (f != null) {
                    var g = f.getGenome();
                    int want = QFTraderEntity.variantTier(c[0]);
                    got = f.getVariant() + " tier " + (g == null ? "?" : g.tier);
                    good = c[0].equals(f.getVariant()) && g != null && g.tier == want;
                    f.discard();
                } else got = "no falcon";
            }
            ok &= good;
            sb.append(c[0]).append(" (").append(c[1]).append(", label '").append(label).append("') -> ").append(got).append("; ");
        }
        cleanup(level, p, 8);
        result("r5_trader_egg_hatch", ok, sb.toString());
    }

    /** Items whose only uses are in code (no recipe, plain Item class) but which really do something. */
    static final Set<String> CODE_USED = Set.of("falcon_treat");   // taming and feeding (FalconTreat handling in QuantumFalconEntity)

    /** Does this item do anything? Custom item class, block, food, recipe ingredient, or a known code use. */
    static String uselessWhy(ServerLevel level, Item it) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(it);
        if (Arrays.asList(WorldDropsTests.DEAD).contains(id.getPath())) return "on the dead-item list";
        if (it.getClass() != Item.class && it.getClass() != com.matt.quantumfalcon.item.MaterialWithTooltipItem.class) return null;
        if (it instanceof BlockItem || it.components().has(net.minecraft.core.component.DataComponents.FOOD)) return null;
        if (CODE_USED.contains(id.getPath())) return null;
        ItemStack st = new ItemStack(it);
        for (var h : level.getServer().getRecipeManager().getRecipes())
            for (var ing : h.value().getIngredients()) if (ing.test(st)) return null;
        return "plain item, no recipe uses it";
    }

    /** (b) every reward item of every loaded quest has a use; no reward is an egg. */
    private static void codexRewards(ServerLevel level) {
        List<String> bad = new ArrayList<>();
        int rewards = 0;
        for (var q : RiftCodex.QUESTS.values()) {
            if (q.reward() == null) continue;
            for (ResourceLocation rl : q.reward().items()) {
                rewards++;
                Item it = BuiltInRegistries.ITEM.get(rl);
                String why = it == Items.AIR ? "missing" : uselessWhy(level, it);
                if (it instanceof FalconEggItem || rl.getPath().contains("egg")) why = "an egg (egg limits)";
                if (why != null) bad.add(q.id() + ":" + rl.getPath() + " (" + why + ")");
            }
        }
        result("ec1_codex_rewards_useful", rewards > 0 && bad.isEmpty(), rewards + " reward items in " + RiftCodex.QUESTS.size() + " quests; useless: " + bad);
    }

    /** The two items the Codex hands out most now do something: a shard drains instability, a token re-rolls an egg. */
    private static void itemUses(ServerLevel level) {
        FakePlayer fp = AuditTests.freshPlayer(level, "QFEconomist");
        var cap = com.matt.quantumfalcon.capability.PlayerInstabilityCapability.get(fp);
        cap.setInstability(50f);
        ItemStack shards = new ItemStack(ModItems.STABILITY_SHARD.get(), 3);
        boolean used = StabilityShardItem.crush(fp, shards);
        float after = cap.getInstability();
        cap.setInstability(0f);
        boolean calmRefused = !StabilityShardItem.crush(fp, new ItemStack(ModItems.STABILITY_SHARD.get()));
        ItemStack egg = FalconEggItem.wild(new ItemStack(ModItems.FALCON_EGG.get()), com.matt.quantumfalcon.breeding.BreedingEngine.wild("warped", 4, level.random));
        var g0 = FalconEggItem.child(egg);
        String before = g0.save().toString();
        ItemStack tokens = new ItemStack(ModItems.QUANTUM_REROLL_TOKEN.get(), 2);
        boolean rolled = RerollTokenItem.reroll(fp, tokens, egg);
        var g1 = FalconEggItem.child(egg);
        boolean same = g1 != null && g1.variant.equals(g0.variant) && g1.tier == g0.tier;
        boolean changed = g1 != null && !g1.save().toString().equals(before);
        boolean noEggRefused = !RerollTokenItem.reroll(fp, tokens, new ItemStack(Items.DIRT));
        boolean ok = used && Math.abs(after - 35f) < 0.01f && shards.getCount() == 2 && calmRefused && rolled && same && changed && tokens.getCount() == 1 && noEggRefused;
        result("ec1_reward_item_uses", ok, "shard: used=" + used + " instability 50 -> " + after + " shards left " + shards.getCount() + " refused when calm=" + calmRefused
            + " | token: rolled=" + rolled + " variant/tier kept=" + same + " genome changed=" + changed + " tokens left " + tokens.getCount() + " refused without egg=" + noEggRefused);
    }
}
