package com.matt.quantumfalcon.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import javax.annotation.Nullable;

/**
 * Explorer maps to Quantum Falcon sites, like a cartographer's: a filled map centred on the nearest site in a structure tag,
 * with a red X on it. Used by the Rift Codex (the Eastwatch pointer) and by the Wayfinder's trades.
 */
public final class ExplorerMaps {
    private ExplorerMaps() {}

    /** How far to look, in chunks (vanilla cartographers use 100). */
    public static final int SEARCH_CHUNKS = 100;

    public static TagKey<Structure> tag(String id) { return TagKey.create(Registries.STRUCTURE, ResourceLocation.parse(id)); }

    /** The map, or null when no such site generates within reach of {@code from} in this level. */
    @Nullable
    public static ItemStack create(ServerLevel level, TagKey<Structure> where, BlockPos from, String nameKey, boolean skipKnown) {
        BlockPos at = level.findNearestMapStructure(where, from, SEARCH_CHUNKS, skipKnown);
        if (at == null) return null;
        ItemStack map = MapItem.create(level, at.getX(), at.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, at, "+", MapDecorationTypes.RED_X);
        map.set(net.minecraft.core.component.DataComponents.ITEM_NAME, Component.translatable(nameKey));
        return map;
    }
}
