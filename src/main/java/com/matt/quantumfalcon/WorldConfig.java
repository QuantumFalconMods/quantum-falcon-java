package com.matt.quantumfalcon;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server (per-world) settings: serverconfig/quantumfalcon-world.toml. Structure placement is read when the server starts
 * (before the levels exist), so a change there needs a restart and only affects chunks generated afterwards.
 */
public final class WorldConfig {
    private WorldConfig() { }

    public static final ModConfigSpec SPEC;

    public record StructureSet(ModConfigSpec.BooleanValue enabled, ModConfigSpec.IntValue spacing, ModConfigSpec.IntValue separation) { }
    /** Per structure set (data/quantumfalcon/worldgen/structure_set). */
    public static final Map<String, StructureSet> STRUCTURES = new LinkedHashMap<>();

    public static final ModConfigSpec.DoubleValue BOSS_HEALTH, BOSS_DAMAGE, DUNGEON_HEALTH, DUNGEON_DAMAGE;
    public static final ModConfigSpec.IntValue TRADER_EGG_MAX_TIER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> TRADER_EGG_POOL;
    public static final ModConfigSpec.BooleanValue RELICS_BIND_ON_PICKUP, BYSTANDER_CREDIT;
    public static final ModConfigSpec.BooleanValue WARD_PROTECT_BLOCKS, WARD_ALLOW_FOREIGN_TELEPORTS, WARD_HARDEN_BLOCKS, WARD_GUARD_MACHINES;
    public static final ModConfigSpec.DoubleValue RIFT_INSTABILITY_RATE, STORY_CALM_PER_PIECE;

    static {
        String[] sets = {
            "ancient_nest", "breeding_sanctum", "crystal_cavern", "data_archive",
            "echo_chamber", "echo_dungeons", "echo_rift_gate", "end_dungeons",
            "entangled_twins_arena", "frozen_hours_eastwatch", "nether_dungeons",
            "overworld_dungeons", "quantum_guardian_arena", "quantum_observatory",
            "quantum_well", "rift_shrine", "singularity_sovereign_arena",
            "temporal_nexus", "void_citadel", "void_horror_arena"};
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Structure placement per structure set. spacing/separation of 0 keep the shipped values. Needs a restart; only new chunks change.").push("structures");
        for (String s : sets) {
            b.push(s);
            STRUCTURES.put(s, new StructureSet(b.define("enabled", true),
                b.comment("Average distance between attempts, in chunks (0 = shipped value)").defineInRange("spacing", 0, 0, 4096),
                b.comment("Minimum distance between two, in chunks; must stay below spacing (0 = shipped value)").defineInRange("separation", 0, 0, 4095)));
            b.pop();
        }
        b.pop();
        b.push("difficulty");
        BOSS_HEALTH = b.comment("Boss max-health multiplier").defineInRange("bossHealth", 1.0, 0.1, 20.0);
        BOSS_DAMAGE = b.comment("Damage dealt by bosses, multiplier").defineInRange("bossDamage", 1.0, 0.1, 20.0);
        DUNGEON_HEALTH = b.comment("Max-health multiplier for dungeon guardians (key bearers, wardens, realm mobs)").defineInRange("dungeonHealth", 1.0, 0.1, 20.0);
        DUNGEON_DAMAGE = b.comment("Damage dealt by dungeon guardians, multiplier").defineInRange("dungeonDamage", 1.0, 0.1, 20.0);
        b.pop();
        b.push("eggs_and_relics");
        TRADER_EGG_MAX_TIER = b.comment("Highest falcon tier any trader sells (5 = Epic; Legendary+ stay boss/cache only)").defineInRange("traderEggMaxTier", 5, 1, 5);
        TRADER_EGG_POOL = b.comment("Variants traders draw common eggs from (empty = the Echo Rift colours below the cap)")
            .defineListAllowEmpty("traderEggPool", List.of(), () -> "", o -> o instanceof String);
        RELICS_BIND_ON_PICKUP = b.comment("Boss relics bind to the first player who picks them up (only the bound player can use them for the True Path)").define("relicsBindOnPickup", true);
        BYSTANDER_CREDIT = b.comment("Players standing inside a boss arena when it dies share the credit and the relic even if they never landed a hit (false: only players who hurt the boss, and the killer)").define("bystanderCredit", true);
        b.pop();
        b.push("ward");
        WARD_PROTECT_BLOCKS = b.comment("Dungeons, arenas and Frozen Hours can't be broken, blown up or flooded").define("protectBlocks", true);
        WARD_HARDEN_BLOCKS = b.comment("Inside a ward, warded blocks read as unbreakable to machines that check hardness (Create drills, quarries, miners)").define("hardenForMachines", true);
        WARD_GUARD_MACHINES = b.comment("Inside a ward, other mods can't remove warded blocks with code that skips the break event").define("guardMachineBreaking", true);
        WARD_ALLOW_FOREIGN_TELEPORTS = b.comment("Allow teleports from other mods and commands into or out of a ward (ender pearls and chorus fruit are always blocked)").define("allowForeignTeleports", false);
        b.pop();
        b.push("rift");
        RIFT_INSTABILITY_RATE = b.comment("How fast Rift instability builds, multiplier (0 = never)").defineInRange("instabilityRate", 1.0, 0.0, 10.0);
        STORY_CALM_PER_PIECE = b.comment("Quantum Storm and Solar Eclipse chance drops by this much per piece of the held hour the players in a dimension have taken back (0 = no effect; 0.2 = a fifth per piece)").defineInRange("storyCalmPerPiece", 0.2, 0.0, 0.25);
        b.pop();
        SPEC = b.build();
    }

    private static boolean loaded() { return SPEC.isLoaded(); }
    public static double bossHealth() { return loaded() ? BOSS_HEALTH.get() : 1.0; }
    public static double bossDamage() { return loaded() ? BOSS_DAMAGE.get() : 1.0; }
    public static double dungeonHealth() { return loaded() ? DUNGEON_HEALTH.get() : 1.0; }
    public static double dungeonDamage() { return loaded() ? DUNGEON_DAMAGE.get() : 1.0; }
    public static int traderEggMaxTier() { return loaded() ? TRADER_EGG_MAX_TIER.get() : 5; }
    public static List<? extends String> traderEggPool() { return loaded() ? TRADER_EGG_POOL.get() : List.of(); }
    public static boolean relicsBind() { return !loaded() || RELICS_BIND_ON_PICKUP.get(); }
    public static boolean wardProtectsBlocks() { return !loaded() || WARD_PROTECT_BLOCKS.get(); }
    public static boolean bystanderCredit() { return !loaded() || BYSTANDER_CREDIT.get(); }
    public static boolean wardHardensBlocks() { return !loaded() || WARD_HARDEN_BLOCKS.get(); }
    public static boolean wardGuardsMachines() { return !loaded() || WARD_GUARD_MACHINES.get(); }
    public static boolean wardAllowsForeignTeleports() { return loaded() && WARD_ALLOW_FOREIGN_TELEPORTS.get(); }
    public static double storyCalmPerPiece() { return loaded() ? STORY_CALM_PER_PIECE.get() : 0.2; }
    public static float instabilityRate() { return loaded() ? RIFT_INSTABILITY_RATE.get().floatValue() : 1f; }
}
