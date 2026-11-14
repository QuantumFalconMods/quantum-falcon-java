package com.matt.quantumfalcon.world;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.blockentity.FalconEggBlockEntity;
import com.matt.quantumfalcon.breeding.BreedingEngine;
import com.matt.quantumfalcon.breeding.BreedingService;
import com.matt.quantumfalcon.breeding.NestQuality;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Wild nest site (BREEDING.md "Exploration", WORLD.md): a nest whose quality matches the area's danger tier, often with
 * a wild egg of a variant native to the biome, ringed by a few themed stones. Higher-tier areas roll higher-tier eggs.
 */
public class WildNestFeature extends Feature<NoneFeatureConfiguration> {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, QuantumFalcon.MOD_ID);
    public static final DeferredHolder<Feature<?>, WildNestFeature> WILD_NEST = FEATURES.register("wild_nest", WildNestFeature::new);
    public static void register(IEventBus bus) { FEATURES.register(bus); }

    public WildNestFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level(); RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, o);
        if (level.getLevel().dimensionType().hasCeiling()) {   // Nether-like: find a ledge below the roof
            top = null;
            for (int y = 100; y > 34; y--) {
                BlockPos p = new BlockPos(o.getX(), y, o.getZ());
                if (level.isEmptyBlock(p) && level.isEmptyBlock(p.above()) && level.getBlockState(p.below()).isSolidRender(level, p.below())) { top = p; break; }
            }
            if (top == null) return false;
        }
        if (top.getY() <= level.getMinBuildHeight() + 2 || top.getY() >= level.getMaxBuildHeight() - 3) return false;
        BlockState ground = level.getBlockState(top.below());
        if (!ground.isFaceSturdy(level, top.below(), net.minecraft.core.Direction.UP) || !level.getFluidState(top).isEmpty()) return false;
        if (!level.getBlockState(top).canBeReplaced() || !level.getBlockState(top.above()).canBeReplaced()) return false;

        var biome = level.getBiome(top);
        String variant = QuantumFalconEntity.riftVariantFor(biome.unwrapKey().map(k -> k.location()).orElse(null));
        if (variant == null) {
            if (biome.is(BiomeTags.IS_NETHER)) variant = r.nextBoolean() ? "crimson" : r.nextBoolean() ? "soulfire" : "warped";
            else if (biome.is(BiomeTags.IS_END)) variant = r.nextInt(3) == 0 ? "aetherwing" : "voidstalker";
            else if (biome.value().getBaseTemperature() < 0.2f) variant = r.nextBoolean() ? "tundra" : "stormrider";
            else if (biome.value().getBaseTemperature() > 1.0f) variant = r.nextBoolean() ? "prairie" : "crimson";
            else if (biome.is(BiomeTags.IS_MOUNTAIN)) variant = r.nextBoolean() ? "tempest" : "stormrider";
            else if (biome.is(BiomeTags.IS_JUNGLE)) variant = "coral";
            else variant = r.nextInt(3) == 0 ? "mushroom" : "peregrine";
        }
        int tier = BreedingService.wildTier(variant, level.getLevel(), o, r);
        NestQuality q = NestQuality.forTier(tier);
        Block nest = switch (q) {
            case AERIE -> ModBlocks.QUANTUM_AERIE.get();
            case VOIDFORGED -> ModBlocks.VOIDFORGED_NEST.get();
            case CRYSTAL -> ModBlocks.CRYSTAL_NEST.get();
            default -> ModBlocks.TWIG_NEST.get();
        };
        level.setBlock(top, nest.defaultBlockState(), 2);
        // a few loose stones / twigs around the site so it reads from a distance
        for (int i = 0; i < 4 + r.nextInt(3); i++) {
            BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, top.offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2));
            if (p.equals(top) || p.equals(top.above()) || !level.getBlockState(p).canBeReplaced() || !level.getFluidState(p).isEmpty()) continue;
            if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) continue;
            BlockState deco = q == NestQuality.TWIG ? (r.nextBoolean() ? Blocks.DEAD_BUSH.defaultBlockState() : Blocks.MOSS_CARPET.defaultBlockState())
                    : q == NestQuality.CRYSTAL ? Blocks.AMETHYST_CLUSTER.defaultBlockState()
                    : Blocks.OBSIDIAN.defaultBlockState();
            if (deco.canSurvive(level, p)) level.setBlock(p, deco, 2);
        }
        if (r.nextFloat() < 0.65f) {
            level.setBlock(top.above(), ModBlocks.FALCON_EGG_BLOCK.get().defaultBlockState(), 2);
            if (level.getBlockEntity(top.above()) instanceof FalconEggBlockEntity be) be.setWildChildWorldgen(BreedingEngine.wild(variant, tier, r));
        }
        return true;
    }

    @SuppressWarnings("unused") private static boolean leaves(BlockState s) { return s.is(BlockTags.LEAVES); }
}
