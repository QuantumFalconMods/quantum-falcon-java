package com.matt.quantumfalcon.story;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

/**
 * the Reality Gem, Heart of Quantum and Transcendence Shard recipes bind their result to whoever owns the bound
 * story ingredients (relics are bound when gifted; a gem to whoever it was bound to). That happens inside the recipe
 * itself, so a crafting table, a vanilla Crafter or any modded autocrafter all give the same result. Ingredients with
 * mixed owners or none give an unbound result, which qualifies nobody until it is bound (see StoryManager).
 */
public class BoundRelicRecipe extends ShapedRecipe {
    final String group; final CraftingBookCategory cat; final ShapedRecipePattern pattern; final ItemStack result; final boolean notify;

    public BoundRelicRecipe(String group, CraftingBookCategory cat, ShapedRecipePattern pattern, ItemStack result, boolean notify) {
        super(group, cat, pattern, result, notify);
        this.group = group; this.cat = cat; this.pattern = pattern; this.result = result; this.notify = notify;
    }

    /** The common owner of the bound story items in the grid; null if none or mixed. */
    public static UUID owner(CraftingInput in) {
        UUID who = null;
        for (int i = 0; i < in.size(); i++) {
            UUID u = StoryManager.boundTo(in.getItem(i));
            if (u == null) continue;
            if (who != null && !who.equals(u)) return null;
            who = u;
        }
        return who;
    }

    @Override
    public ItemStack assemble(CraftingInput in, HolderLookup.Provider reg) {
        ItemStack out = super.assemble(in, reg);
        UUID who = owner(in);
        if (who != null) StoryManager.bind(out, who);
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }

    public static final MapCodec<BoundRelicRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
        CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(r -> r.cat),
        ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
        Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(r -> r.notify)
    ).apply(i, BoundRelicRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoundRelicRecipe> STREAM_CODEC = StreamCodec.of(
        (buf, r) -> { buf.writeUtf(r.group); buf.writeEnum(r.cat); ShapedRecipePattern.STREAM_CODEC.encode(buf, r.pattern); ItemStack.STREAM_CODEC.encode(buf, r.result); buf.writeBoolean(r.notify); },
        buf -> new BoundRelicRecipe(buf.readUtf(), buf.readEnum(CraftingBookCategory.class), ShapedRecipePattern.STREAM_CODEC.decode(buf), ItemStack.STREAM_CODEC.decode(buf), buf.readBoolean()));

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, com.matt.quantumfalcon.QuantumFalcon.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BoundRelicRecipe>> SERIALIZER = SERIALIZERS.register("bound_relic", () -> new RecipeSerializer<>() {
        @Override public MapCodec<BoundRelicRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, BoundRelicRecipe> streamCodec() { return STREAM_CODEC; }
    });

    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
}
