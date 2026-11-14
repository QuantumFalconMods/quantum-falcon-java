package com.matt.quantumfalcon.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Stage E skill tree: five branches (Bond, Hunt, Rift, Lineage, Wayfinder), tiers 1-4 matching danger tiers I-IV.
 * Nodes are data ({@code data/quantumfalcon/skill_tree/nodes.json}); effects and tooltip numbers are in {@link SkillEffects}.
 * Lineage nodes are also gated on the highest falcon tier the player has hatched (B6).
 */
public class SkillTreeSystem {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String NODES_PATH = "/data/quantumfalcon/skill_tree/nodes.json";
    /** Cost per tier (index = tier - 1). */
    public static final int[] TIER_COST = {1, 1, 2, 3};

    private static final Map<String, Skill> SKILLS = new LinkedHashMap<>();
    private static final Map<String, List<Skill>> SKILLS_BY_CATEGORY = new HashMap<>();

    /** W18: ids from the pre-Stage-E tree. Mapped ids keep their unlock; the rest are refunded. */
    public static final Map<String, String> MIGRATION = Map.ofEntries(
        Map.entry("bond_1", "bond_kinship"), Map.entry("falcon_health", "bond_vigor"), Map.entry("falcon_speed", "bond_tailwind"),
        Map.entry("flight_stamina", "bond_endurance"), Map.entry("falcon_resurrection", "bond_soul"),
        Map.entry("attack_1", "hunt_talons"), Map.entry("dive_damage", "hunt_dive"), Map.entry("team_attack", "hunt_pack"),
        Map.entry("bleed_effect", "hunt_razor"), Map.entry("execute", "hunt_execute"),
        Map.entry("blink_cooldown", "rift_focus"), Map.entry("quantum_sight", "rift_sight"),
        Map.entry("breeding_master", "lin_warmth"), Map.entry("treasure_hunter", "way_magnet"));
    /** Old costs, for refunds of removed nodes. */
    public static final Map<String, Integer> OLD_COST = Map.ofEntries(
        Map.entry("bond_1", 1), Map.entry("falcon_health", 1), Map.entry("falcon_speed", 1), Map.entry("multi_falcon", 2), Map.entry("breeding_master", 2),
        Map.entry("falcon_resurrection", 3), Map.entry("attack_1", 1), Map.entry("dive_damage", 1), Map.entry("team_attack", 1), Map.entry("bleed_effect", 2),
        Map.entry("critical_strike", 2), Map.entry("execute", 3), Map.entry("blink_cooldown", 1), Map.entry("blink_range", 1), Map.entry("blink_chain", 2),
        Map.entry("entanglement_range", 1), Map.entry("damage_share_reduction", 2), Map.entry("quantum_sight", 3), Map.entry("flight_stamina", 1),
        Map.entry("vision_share", 1), Map.entry("treasure_hunter", 2), Map.entry("fast_travel", 2), Map.entry("efficient_crafting", 1),
        Map.entry("bulk_crafting", 1), Map.entry("auto_smelt", 2), Map.entry("recycler", 2));

    public enum Category {
        BOND("Bond", "§b", 0, 0xFF28ECFF),
        HUNT("Hunt", "§c", 1, 0xFFFF3A4F),
        RIFT("Rift", "§d", 2, 0xFFAA50FF),
        LINEAGE("Lineage", "§6", 3, 0xFFFFB43A),
        WAYFINDER("Wayfinder", "§a", 4, 0xFF5AFFAA);

        public final String displayName;
        public final String colorCode;
        public final int tabIndex;
        public final int argb;

        Category(String displayName, String colorCode, int tabIndex, int argb) {
            this.displayName = displayName; this.colorCode = colorCode; this.tabIndex = tabIndex; this.argb = argb;
        }
    }

    static { load(); }

    private static void load() {
        try (var in = SkillTreeSystem.class.getResourceAsStream(NODES_PATH)) {
            if (in == null) { LOGGER.error("Skill tree data missing: {}", NODES_PATH); return; }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement el : root.getAsJsonArray("nodes")) {
                JsonObject o = el.getAsJsonObject();
                int tier = o.get("tier").getAsInt();
                Skill s = new Skill(o.get("id").getAsString(), Category.valueOf(o.get("branch").getAsString()), o.get("name").getAsString(),
                    o.get("desc").getAsString(), TIER_COST[tier - 1], tier, o.get("col").getAsInt(), strings(o, "requires"), strings(o, "any"),
                    o.has("hatch") ? o.get("hatch").getAsInt() : 0);
                SKILLS.put(s.id, s);
                SKILLS_BY_CATEGORY.computeIfAbsent(s.category.name(), k -> new ArrayList<>()).add(s);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load the skill tree from {}", NODES_PATH, e);
        }
        LOGGER.info("Registered {} skills across {} branches ({} points to buy everything)", SKILLS.size(), Category.values().length, totalCost());
    }

    private static List<String> strings(JsonObject o, String key) {
        if (!o.has(key)) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray(key)) out.add(e.getAsString());
        return List.copyOf(out);
    }

    // ============ Public API ============
    public static Skill getSkill(String skillId) { return SKILLS.get(skillId); }
    public static Collection<Skill> getAllSkills() { return SKILLS.values(); }
    public static List<Skill> getSkillsByCategory(Category category) { return SKILLS_BY_CATEGORY.getOrDefault(category.name(), List.of()); }
    public static List<Skill> getSkillsByCategory(String categoryName) { return SKILLS_BY_CATEGORY.getOrDefault(categoryName, List.of()); }
    public static int getTotalSkillCount() { return SKILLS.size(); }
    public static int totalCost() { int c = 0; for (Skill s : SKILLS.values()) c += s.cost; return c; }

    public static class Skill {
        public final String id;
        public final Category category;
        public final String name;
        public final String descTemplate;
        public final String description;
        public final int cost;
        public final int tier;
        public final int gridX;
        public final int gridY;
        public final List<String> prerequisites;
        public final List<String> anyOf;
        /** Lineage: highest falcon tier the player must have hatched (0 = no gate). */
        public final int hatchGate;

        public Skill(String id, Category category, String name, String descTemplate, int cost, int tier, int col,
                     List<String> prerequisites, List<String> anyOf, int hatchGate) {
            this.id = id; this.category = category; this.name = name; this.descTemplate = descTemplate;
            this.description = SkillEffects.describe(descTemplate);
            this.cost = cost; this.tier = tier; this.gridX = col; this.gridY = tier - 1;
            this.prerequisites = prerequisites; this.anyOf = anyOf; this.hatchGate = hatchGate;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Category getCategory() { return category; }
        public int getCost() { return cost; }
        public int getTreeX() { return gridX; }
        public int getTreeY() { return gridY; }
        public int getMaxLevel() { return 1; }
        public int getSkillPointCost() { return cost; }
        public List<String> getPrerequisites() { return prerequisites; }
        /** Every node this one hangs from in the tree (all-of plus any-of), for drawing connectors. */
        public List<String> parents() { List<String> l = new ArrayList<>(prerequisites); l.addAll(anyOf); return l; }

        public ResourceLocation getIconLocation() {
            return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/gui/skills/" + id + ".png");
        }
        public Component getDisplayName() { return Component.literal(category.colorCode + name); }
        public Component getDescriptionComponent() { return Component.literal("§7" + description); }
        public Component getCostComponent() { return Component.literal("§eCost: " + cost + " point" + (cost > 1 ? "s" : "")); }

        /** Prerequisites only (the hatch gate needs the player: {@link #lockReason}). */
        public boolean canUnlock(Set<String> unlocked) {
            for (String p : prerequisites) if (!unlocked.contains(p)) return false;
            if (!anyOf.isEmpty() && anyOf.stream().noneMatch(unlocked::contains)) return false;
            return true;
        }

        /** Why this node can't be bought right now, or null if it can. Shared by the server check and the tooltip. */
        public String lockReason(Set<String> unlocked, int points, int highestHatched) {
            if (unlocked.contains(id)) return "Already unlocked";
            if (hatchGate > 0 && highestHatched < hatchGate) return "Hatch a " + rarityName(hatchGate) + " falcon first (best so far: " + (highestHatched == 0 ? "none" : rarityName(highestHatched)) + ")";   // rarity names, not tier numbers
            for (String p : prerequisites) if (!unlocked.contains(p)) return "Needs " + nameOf(p);
            if (!anyOf.isEmpty() && anyOf.stream().noneMatch(unlocked::contains))
                return "Needs " + String.join(" or ", anyOf.stream().map(SkillTreeSystem::nameOf).toList());
            if (points < cost) return "Needs " + cost + " point" + (cost > 1 ? "s" : "") + " (you have " + points + ")";
            return null;
        }
    }

    static String rarityName(int tier) {
        var t = com.matt.quantumfalcon.breeding.BreedingData.TIERS;
        return tier >= 1 && tier <= t.size() ? t.get(tier - 1).name() : "tier " + tier;
    }
    static String nameOf(String id) { Skill s = SKILLS.get(id); return s == null ? id : s.name; }
}
