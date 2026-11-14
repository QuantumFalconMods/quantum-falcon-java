package com.matt.quantumfalcon.hytale;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ItemLike;

/**
 * Hytale-inspired prefab building system specification.
 * 18 pre-built structures across 6 categories.
 */
public class PrefabSystemSpec {
    public static final Map<String, PrefabSpec> PREFABS = new HashMap<>();

    static {
        // HOUSING (3 structures)
        PREFABS.put("small_house", new PrefabSpec("small_house", "Small House", 
            "A cozy starter home (7×5×7)", 7, 5, 7, PrefabCategory.HOUSING,
            List.of(
                new MaterialRequirement(Blocks.OAK_PLANKS, 50),
                new MaterialRequirement(Blocks.OAK_LOG, 30),
                new MaterialRequirement(Blocks.GLASS_PANE, 4),
                new MaterialRequirement(Blocks.OAK_DOOR, 1),
                new MaterialRequirement(Blocks.OAK_STAIRS, 20)
            ), 0, true));

        PREFABS.put("medieval_house", new PrefabSpec("medieval_house", "Medieval House", 
            "A larger medieval-style home (11×8×11)", 11, 8, 11, PrefabCategory.HOUSING,
            List.of(
                new MaterialRequirement(Blocks.COBBLESTONE, 100),
                new MaterialRequirement(Blocks.OAK_PLANKS, 80),
                new MaterialRequirement(Blocks.SPRUCE_LOG, 40),
                new MaterialRequirement(Blocks.GLASS_PANE, 12),
                new MaterialRequirement(Blocks.SPRUCE_DOOR, 2)
            ), 20, true));

        PREFABS.put("mansion", new PrefabSpec("mansion", "Mansion", 
            "A luxurious three-story mansion (21×15×21)", 21, 15, 21, PrefabCategory.HOUSING,
            List.of(
                new MaterialRequirement(Blocks.QUARTZ_BLOCK, 300),
                new MaterialRequirement(Blocks.DARK_OAK_PLANKS, 200),
                new MaterialRequirement(Blocks.GLASS, 50),
                new MaterialRequirement(Blocks.GOLD_BLOCK, 20)
            ), 60, true));

        // FARMING (3 structures)
        PREFABS.put("farm_plot", new PrefabSpec("farm_plot", "Farm Plot", 
            "9×9 hydrated farmland with fence", 9, 2, 9, PrefabCategory.FARMING,
            List.of(
                new MaterialRequirement(Items.WATER_BUCKET, 1),
                new MaterialRequirement(Blocks.OAK_FENCE, 32)
            ), 0, true));

        PREFABS.put("windmill", new PrefabSpec("windmill", "Windmill", 
            "Decorative wheat farm windmill (9×12×9)", 9, 12, 9, PrefabCategory.FARMING,
            List.of(
                new MaterialRequirement(Blocks.COBBLESTONE, 80),
                new MaterialRequirement(Blocks.OAK_PLANKS, 60),
                new MaterialRequirement(Blocks.WHITE_WOOL, 20)
            ), 40, true));

        PREFABS.put("greenhouse", new PrefabSpec("greenhouse", "Greenhouse", 
            "Glass greenhouse for protected farming (13×6×13)", 13, 6, 13, PrefabCategory.FARMING,
            List.of(
                new MaterialRequirement(Blocks.GLASS, 150),
                new MaterialRequirement(Blocks.OAK_LOG, 40),
                new MaterialRequirement(Blocks.FARMLAND, 100)
            ), 30, true));

        // DEFENSE (3 structures)
        PREFABS.put("guard_tower", new PrefabSpec("guard_tower", "Guard Tower", 
            "Defensive watchtower (5×15×5)", 5, 15, 5, PrefabCategory.DEFENSE,
            List.of(
                new MaterialRequirement(Blocks.STONE_BRICKS, 200),
                new MaterialRequirement(Blocks.LADDER, 15),
                new MaterialRequirement(Blocks.TORCH, 10)
            ), 20, true));

        PREFABS.put("wall_section", new PrefabSpec("wall_section", "Wall Section", 
            "10 block defensive wall section (10×4×2)", 10, 4, 2, PrefabCategory.DEFENSE,
            List.of(
                new MaterialRequirement(Blocks.STONE_BRICKS, 80),
                new MaterialRequirement(Blocks.STONE_BRICK_STAIRS, 20)
            ), 10, false));

        PREFABS.put("gate_house", new PrefabSpec("gate_house", "Gate House", 
            "Fortified gate entrance (7×8×7)", 7, 8, 7, PrefabCategory.DEFENSE,
            List.of(
                new MaterialRequirement(Blocks.STONE_BRICKS, 150),
                new MaterialRequirement(Blocks.IRON_DOOR, 1),
                new MaterialRequirement(Blocks.IRON_BARS, 20)
            ), 25, true));

        // DECORATION (3 structures)
        PREFABS.put("fountain", new PrefabSpec("fountain", "Fountain", 
            "Decorative fountain (7×4×7)", 7, 4, 7, PrefabCategory.DECORATION,
            List.of(
                new MaterialRequirement(Blocks.QUARTZ_BLOCK, 40),
                new MaterialRequirement(Blocks.QUARTZ_PILLAR, 4),
                new MaterialRequirement(Items.WATER_BUCKET, 1)
            ), 10, true));

        PREFABS.put("gazebo", new PrefabSpec("gazebo", "Gazebo", 
            "Garden gazebo (7×6×7)", 7, 6, 7, PrefabCategory.DECORATION,
            List.of(
                new MaterialRequirement(Blocks.OAK_PLANKS, 50),
                new MaterialRequirement(Blocks.OAK_LOG, 20),
                new MaterialRequirement(Blocks.OAK_FENCE, 40)
            ), 15, true));

        PREFABS.put("statue", new PrefabSpec("statue", "Statue", 
            "Decorative player statue (3×8×3)", 3, 8, 3, PrefabCategory.DECORATION,
            List.of(
                new MaterialRequirement(Blocks.QUARTZ_BLOCK, 30),
                new MaterialRequirement(Blocks.GOLD_BLOCK, 10)
            ), 15, true));

        // UTILITY (3 structures)
        PREFABS.put("crafting_hall", new PrefabSpec("crafting_hall", "Crafting Hall", 
            "Multi-purpose crafting station (9×5×9)", 9, 5, 9, PrefabCategory.UTILITY,
            List.of(
                new MaterialRequirement(Blocks.STONE_BRICKS, 100),
                new MaterialRequirement(Blocks.CRAFTING_TABLE, 4),
                new MaterialRequirement(Blocks.FURNACE, 4),
                new MaterialRequirement(Blocks.CHEST, 8)
            ), 20, true));

        PREFABS.put("storage_warehouse", new PrefabSpec("storage_warehouse", "Storage Warehouse", 
            "Large storage building (15×6×15)", 15, 6, 15, PrefabCategory.UTILITY,
            List.of(
                new MaterialRequirement(Blocks.SPRUCE_PLANKS, 200),
                new MaterialRequirement(Blocks.CHEST, 27),
                new MaterialRequirement(Blocks.BARREL, 20)
            ), 30, true));

        PREFABS.put("enchanting_tower", new PrefabSpec("enchanting_tower", "Enchanting Tower", 
            "Magic tower with enchanting setup (7×10×7)", 7, 10, 7, PrefabCategory.UTILITY,
            List.of(
                new MaterialRequirement(Blocks.OBSIDIAN, 40),
                new MaterialRequirement(Blocks.BOOKSHELF, 15),
                new MaterialRequirement(Blocks.ENCHANTING_TABLE, 1),
                new MaterialRequirement(Blocks.GLOWSTONE, 20)
            ), 35, true));

        // DUNGEON (3 structures)
        PREFABS.put("pvp_arena", new PrefabSpec("pvp_arena", "PvP Arena", 
            "Battle arena with cover (30×10×30)", 30, 10, 30, PrefabCategory.DUNGEON,
            List.of(
                new MaterialRequirement(Blocks.STONE_BRICKS, 500),
                new MaterialRequirement(Blocks.STONE_BRICK_WALL, 200),
                new MaterialRequirement(Blocks.SEA_LANTERN, 40)
            ), 60, true));

        PREFABS.put("obstacle_course", new PrefabSpec("obstacle_course", "Obstacle Course", 
            "Parkour training course (40×15×10)", 40, 15, 10, PrefabCategory.DUNGEON,
            List.of(
                new MaterialRequirement(Blocks.QUARTZ_BLOCK, 200),
                new MaterialRequirement(Blocks.SLIME_BLOCK, 20),
                new MaterialRequirement(Blocks.WATER, 10)
            ), 50, true));

        PREFABS.put("mob_grinder", new PrefabSpec("mob_grinder", "Mob Grinder", 
            "Automated mob farm (13×20×13)", 13, 20, 13, PrefabCategory.DUNGEON,
            List.of(
                new MaterialRequirement(Blocks.COBBLESTONE, 300),
                new MaterialRequirement(Blocks.WATER, 4),
                new MaterialRequirement(Blocks.HOPPER, 10),
                new MaterialRequirement(Blocks.CHEST, 4)
            ), 60, true));
    }

    public static String getSystemSummary() {
        int totalPrefabs = PREFABS.size();
        int housingCount = (int) PREFABS.values().stream().filter(p -> p.category == PrefabCategory.HOUSING).count();
        int farmingCount = (int) PREFABS.values().stream().filter(p -> p.category == PrefabCategory.FARMING).count();
        int defenseCount = (int) PREFABS.values().stream().filter(p -> p.category == PrefabCategory.DEFENSE).count();
        int decorationCount = (int) PREFABS.values().stream().filter(p -> p.category == PrefabCategory.DECORATION).count();
        int utilityCount = (int) PREFABS.values().stream().filter(p -> p.category == PrefabCategory.UTILITY).count();
        int dungeonCount = (int) PREFABS.values().stream().filter(p -> p.category == PrefabCategory.DUNGEON).count();
        
        return String.format("""
            PREFAB SYSTEM SUMMARY
            =====================

            TOTAL PREFABS: %d

            BY CATEGORY:
            🏠 Housing: %d
            🌾 Farming: %d
            🛡️ Defense: %d
            ✨ Decoration: %d
            ⚙️ Utility: %d
            ⚔️ Dungeon: %d

            FEATURES:
            ✓ Material requirement checking
            ✓ Preview mode (particle outline)
            ✓ Instant placement (or timed)
            ✓ Auto-rotation support
            ✓ Flat ground checking
            ✓ GUI selector interface

            LARGEST PREFAB: Mansion (21×15×21 = 6,615 blocks!)
            SMALLEST PREFAB: Statue (3×8×3 = 72 blocks)

            IMPLEMENTATION: Hytale-inspired instant building
            """, totalPrefabs, housingCount, farmingCount, defenseCount, decorationCount, utilityCount, dungeonCount);
    }

    public static class MaterialRequirement {
        public final ItemLike material;
        public final int count;

        public MaterialRequirement(ItemLike material, int count) {
            this.material = material;
            this.count = count;
        }

        public String getDisplayString() {
            return count + "× " + material.toString();
        }
    }

    public static enum PrefabCategory {
        HOUSING("Housing", "Homes and shelters", "🏠"),
        FARMING("Farming", "Farms and agriculture", "🌾"),
        DEFENSE("Defense", "Walls and towers", "🛡️"),
        DECORATION("Decoration", "Decorative structures", "✨"),
        UTILITY("Utility", "Functional buildings", "⚙️"),
        DUNGEON("Dungeon", "Combat arenas", "⚔️");

        public final String name;
        public final String description;
        public final String icon;

        PrefabCategory(String name, String desc, String icon) {
            this.name = name;
            this.description = desc;
            this.icon = icon;
        }
    }

    public static class PrefabCrafting {
        public static String getPrefabToolRecipe() {
            return """
                PREFAB TOOL RECIPE:
                ===================

                Shaped Recipe (3×3):

                [ Book ] [ Diamond ] [ Book ]
                [ Iron ] [ Crafting ] [ Iron ]
                [ Iron ] [ Diamond ] [ Iron ]

                Where:
                - Book = Written Book
                - Diamond = Diamond
                - Iron = Iron Ingot
                - Crafting = Crafting Table

                Output: 1× Prefab Tool (Unbreakable)
                """;
        }
    }

    public static class PrefabSpec {
        public final String id;
        public final String name;
        public final String description;
        public final int width;
        public final int height;
        public final int depth;
        public final PrefabCategory category;
        public final List<MaterialRequirement> materials;
        public final int placementTime;
        public final boolean requiresFlat;

        public PrefabSpec(String id, String name, String desc, int w, int h, int d, 
                         PrefabCategory cat, List<MaterialRequirement> materials, 
                         int placeTime, boolean needsFlat) {
            this.id = id;
            this.name = name;
            this.description = desc;
            this.width = w;
            this.height = h;
            this.depth = d;
            this.category = cat;
            this.materials = materials;
            this.placementTime = placeTime;
            this.requiresFlat = needsFlat;
        }

        public int getTotalBlocks() {
            return width * height * depth;
        }

        public String getSizeString() {
            return width + "×" + height + "×" + depth;
        }
    }

    public static class PrefabToolSpec {
        public static final String ITEM_ID = "prefab_tool";
        public static final String NAME = "Prefab Tool";
        public static final String DESCRIPTION = "Place pre-built structures instantly";
        public static final String[] CONTROLS = {
            "Right-Click: Open prefab menu",
            "Shift + Right-Click: Preview mode",
            "Sneak + Use: Rotate structure"
        };
        public static final int DURABILITY = 0;
        public static final String RARITY = "UNCOMMON";

        public static String getTooltip() {
            return """
                PREFAB TOOL
                ===========

                Place pre-designed structures instantly!

                CONTROLS:
                - Right-Click: Open prefab menu
                - Shift + Right-Click: Preview mode
                - Sneak + Use: Rotate structure

                FEATURES:
                - 20+ prefabs
                - 6 categories
                - Material checking
                - Preview particles
                - Instant placement

                CATEGORIES:
                🏠 Housing (3 types)
                🌾 Farming (3 types)
                🛡️ Defense (3 types)
                ✨ Decoration (3 types)
                ⚙️ Utility (3 types)
                ⚔️ Dungeon (3 types)
                """;
        }
    }
}
