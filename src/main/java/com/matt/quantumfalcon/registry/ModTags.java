package com.matt.quantumfalcon.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Custom tags for Quantum Falcon mod content.
 */
public class ModTags {

    public static class Blocks {
        public static final TagKey<Block> QUANTUM_ORES = tag("quantum_ores");
        public static final TagKey<Block> QUANTUM_LOGS = tag("quantum_logs");
        public static final TagKey<Block> RIFT_PORTAL_FRAME = tag("rift_portal_frame");
        public static final TagKey<Block> FALCON_NEST_BASE = tag("falcon_nest_base");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("quantumfalcon", name));
        }
    }

    public static class Items {
        public static final TagKey<Item> QUANTUM_CORES = tag("quantum_cores");
        public static final TagKey<Item> FALCON_EGGS = tag("falcon_eggs");
        public static final TagKey<Item> FALCON_FOOD = tag("falcon_food");
        public static final TagKey<Item> QUANTUM_TOOLS = tag("quantum_tools");
        public static final TagKey<Item> RIFT_CRYSTALS = tag("rift_crystals");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("quantumfalcon", name));
        }
    }
}
