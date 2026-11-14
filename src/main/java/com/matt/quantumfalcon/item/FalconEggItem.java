package com.matt.quantumfalcon.item;

import com.matt.quantumfalcon.blockentity.FalconEggBlockEntity;
import com.matt.quantumfalcon.breeding.BreedingData;
import com.matt.quantumfalcon.breeding.BreedingEngine;
import com.matt.quantumfalcon.breeding.FalconGenome;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/** Falcon egg item. Carries the whole egg (parents, rolled child, catalyst, progress) under "QFEgg" so breaking and
 *  re-placing an egg loses nothing. Wild eggs from nests and loot carry only a child genome. */
public class FalconEggItem extends BlockItem {
    public static final String TAG = "QFEgg";

    public FalconEggItem(Block block, Properties properties) { super(block, properties); }

    @Override
    protected boolean placeBlock(net.minecraft.world.item.context.BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean placed = super.placeBlock(context, state);
        if (placed && !level.isClientSide() && level.getBlockEntity(pos) instanceof FalconEggBlockEntity be) {
            CompoundTag egg = eggData(context.getItemInHand());
            boolean legendary = Boolean.TRUE.equals(context.getItemInHand().get(com.matt.quantumfalcon.registry.ModDataComponents.LEGENDARY_GUARANTEED.get()));
            if (egg != null) be.loadEggData(egg);
            else {   // eggs bought before the fix carry only the trade "Variant"; hatch that, at its tier
                String v = com.matt.quantumfalcon.entity.npc.QFTraderEntity.eggVariant(context.getItemInHand());
                be.setWildChild(v != null ? BreedingEngine.wild(v, com.matt.quantumfalcon.entity.npc.QFTraderEntity.variantTier(v), level.getRandom())
                    : BreedingEngine.wild("peregrine", 1, level.getRandom()));
            }
            if (legendary) {   // the flag is honoured: the child hatches at Legendary tier at least
                FalconGenome c = be.getChild();
                FalconGenome up = com.matt.quantumfalcon.entity.boss.BlessedEggs.ensureLegendary(c, level.getRandom());
                if (up != c) be.setWildChild(up);
            }
            be.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        }
        return placed;
    }

    @Nullable public static CompoundTag eggData(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag t = cd.copyTag();
        return t.contains(TAG) ? t.getCompound(TAG) : null;
    }
    @Nullable public static FalconGenome child(ItemStack stack) {
        CompoundTag e = eggData(stack);
        return e != null && e.contains("Child") ? FalconGenome.load(e.getCompound("Child")) : null;
    }

    public static ItemStack of(ItemStack stack, CompoundTag eggData) {
        CompoundTag t = new CompoundTag(); t.put(TAG, eggData);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return stack;
    }
    /** A wild egg with a fixed genome (nests, caches, loot, boss drops). */
    public static ItemStack wild(ItemStack stack, FalconGenome g) {
        CompoundTag e = new CompoundTag(); e.put("Child", g.save()); e.putInt("BaseTier", g.tier);
        return of(stack, e);
    }
    /** Item for a placed egg (pick block / breaking). */
    public static ItemStack fromBlock(ItemStack stack, FalconEggBlockEntity be) {
        CompoundTag e = new CompoundTag(); be.saveEggData(e); return of(stack, e);
    }

    @Override
    public Component getName(ItemStack stack) {
        FalconGenome g = child(stack);
        if (g == null) {   // a trader egg bought before the fix: name it by what it will hatch
            String v = com.matt.quantumfalcon.entity.npc.QFTraderEntity.eggVariant(stack);
            return v == null ? super.getName(stack) : Component.translatable("falcon_variant.quantumfalcon." + v).append(" Egg");
        }
        return Component.literal(g.tierDef().name() + " ").append(Component.translatable("falcon_variant.quantumfalcon." + g.variant))
                .append(" Egg").withColor(g.tierDef().colour());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tip, TooltipFlag flag) {
        CompoundTag e = eggData(stack);
        FalconGenome g = child(stack);
        if (g == null) { tip.add(Component.translatable("tooltip.quantumfalcon.falcon_egg.blank")); return; }
        tip.add(Component.literal("Tier " + g.tier + " " + g.tierDef().name() + " | crack: " + g.crackDef().name()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Incubation: " + g.tierDef().incubation().replace('_', ' ') + ", " + g.tierDef().hatchMinutes() + " min").withStyle(ChatFormatting.DARK_GRAY));
        if (e.contains("Progress")) tip.add(Component.literal("Progress: " + (int) (e.getFloat("Progress") * 100) + "%").withStyle(ChatFormatting.DARK_GRAY));
        if (e.contains("Catalyst")) tip.add(Component.literal("Catalyst: " + e.getString("Catalyst").replace('_', ' ')).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (e.contains("ParentA")) tip.add(Component.literal("Bred egg (use the Falconer's Codex on it for the full genome)").withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** Item tint: layer 0 shell = variant colour, layer 1 cracks = crack colour. */
    public static int tint(ItemStack stack, int layer) {
        FalconGenome g = child(stack);
        if (g == null) return 0xFFFFFFFF;
        if (layer == 1) return 0xFF000000 | g.crackDef().colour();
        if (layer >= 2) return 0xFFFFFFFF;   // tier pip keeps its own colour
        return 0xFF000000 | shellColour(g.variant);
    }
    public static int shellColour(String variant) {
        return switch (variant) {
            case "peregrine" -> 0x5A6472; case "prairie" -> 0x9C7A4A; case "tundra" -> 0x3C4A58; case "coral" -> 0xD97A6A;
            case "mushroom" -> 0x9A4434; case "deepslate" -> 0x4A4A52; case "tempest" -> 0x7FA4B8; case "stormrider" -> 0x3A4470;
            case "crimson" -> 0x8A1E2A; case "soulfire" -> 0x2A5A64; case "warped" -> 0x1A6A60; case "voidstalker" -> 0x2A1440;
            case "aetherwing" -> 0x444C60; case "abyssal" -> 0x10284A; case "entangled_prism" -> 0x6A3A8A; case "singularity" -> 0x1A0A26;
            case "cosmic" -> 0x2A2460; case "quantum_phoenix" -> 0xC86A1E; case "tidecaller" -> 0x2A6A8A; case "emberfrost" -> 0x34485A;
            case "sporewraith" -> 0x3A2448; case "aurora_seraph" -> 0x2E4A44; case "rift_reaver" -> 0x5A1A3A; case "chronowing" -> 0x6A6A8A;
            case "nebula_prism" -> 0x4A2A7A; case "paradox" -> 0x101018; default -> 0x808080;
        };
    }
    public static int tierOf(ItemStack stack) { FalconGenome g = child(stack); return g == null ? 1 : g.tier; }
    static { if (BreedingData.TIERS.size() != 7) throw new IllegalStateException("tiers"); }
}
