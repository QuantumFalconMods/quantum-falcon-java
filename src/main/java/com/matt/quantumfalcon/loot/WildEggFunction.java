package com.matt.quantumfalcon.loot;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.breeding.BreedingData;
import com.matt.quantumfalcon.breeding.BreedingEngine;
import com.matt.quantumfalcon.item.FalconEggItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Loot function {@code quantumfalcon:wild_egg}: turns a falcon egg stack into a wild egg with a real genome. Picks a
 * non-hybrid variant whose base tier is within [min_tier, max_tier] (or one of {@code variants} if given), so egg
 * caches in dangerous places hold better eggs. Used by chest loot tables (dungeon egg caches).
 */
public class WildEggFunction extends LootItemConditionalFunction {
    public static final MapCodec<WildEggFunction> CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).and(i.group(
            Codec.INT.optionalFieldOf("min_tier", 1).forGetter(f -> f.minTier),
            Codec.INT.optionalFieldOf("max_tier", 7).forGetter(f -> f.maxTier),
            Codec.STRING.listOf().optionalFieldOf("variants", List.of()).forGetter(f -> f.variants)
    )).apply(i, WildEggFunction::new));

    public static final DeferredRegister<LootItemFunctionType<?>> TYPES = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, QuantumFalcon.MOD_ID);
    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<WildEggFunction>> WILD_EGG = TYPES.register("wild_egg", () -> new LootItemFunctionType<>(CODEC));
    public static void register(IEventBus bus) { TYPES.register(bus); }

    private final int minTier, maxTier;
    private final List<String> variants;

    protected WildEggFunction(List<LootItemCondition> conditions, int minTier, int maxTier, List<String> variants) {
        super(conditions);
        this.minTier = Math.max(1, Math.min(7, minTier));
        this.maxTier = Math.max(this.minTier, Math.min(7, maxTier));
        this.variants = variants;
    }

    @Override public LootItemFunctionType<WildEggFunction> getType() { return WILD_EGG.get(); }

    @Override
    protected ItemStack run(ItemStack stack, LootContext ctx) {
        RandomSource r = ctx.getRandom();
        List<String> pool = new ArrayList<>(variants);
        if (pool.isEmpty())
            for (var v : BreedingData.VARIANTS) if (v.parentA() == null && v.tier() >= minTier && v.tier() <= maxTier && !v.id().equals("quantum_phoenix")) pool.add(v.id());
        if (pool.isEmpty()) pool.add("peregrine");
        String v = pool.get(r.nextInt(pool.size()));
        int tier = Math.max(minTier, Math.min(maxTier, BreedingEngine.variantTier(v) + (r.nextFloat() < 0.2f ? 1 : 0)));
        return FalconEggItem.wild(stack, BreedingEngine.wild(v, tier, r));
    }
}
