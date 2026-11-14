package com.matt.quantumfalcon.event;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * server config for world events (quantumfalcon-server.toml, per world in serverconfig/). Read at the use sites in
 * DynamicEventManager, QuantumStormEvent and MeteorShowerEvent.
 */
public final class EventsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue GRIEFING;
    public static final ModConfigSpec.IntValue PROTECT_RADIUS;
    public static final ModConfigSpec.DoubleValue AURORA_CHANCE, STORM_CHANCE, METEOR_CHANCE, ECLIPSE_CHANCE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("World events: aurora, quantum storm, meteor shower, solar eclipse").push("events");
        ENABLED = b.comment("Master switch for all world events").define("eventsEnabled", true);
        GRIEFING = b.comment("Allow storms and meteors to change blocks (fire, lava). Even when true, the mobGriefing gamerule,",
                "claim mods (block-break checks) and the protection radius around player-built blocks are respected.")
            .define("eventGriefing", false);
        PROTECT_RADIUS = b.comment("No event block changes within this many blocks of player-built blocks (beds, containers, doors, glass, ...)")
            .defineInRange("protectRadius", 12, 0, 64);
        AURORA_CHANCE = b.comment("Chance per check (every 1000 ticks) at night").defineInRange("auroraChance", 0.05, 0.0, 1.0);
        STORM_CHANCE = b.comment("Chance per check, any time").defineInRange("stormChance", 0.02, 0.0, 1.0);
        METEOR_CHANCE = b.comment("Chance per check at night").defineInRange("meteorChance", 0.001, 0.0, 1.0);
        ECLIPSE_CHANCE = b.comment("Chance per check by day").defineInRange("eclipseChance", 0.01, 0.0, 1.0);
        DIMENSIONS = b.comment("Dimensions where events may run").defineListAllowEmpty("dimensions", List.of("minecraft:overworld"), () -> "minecraft:overworld", o -> o instanceof String);
        b.pop();
        SPEC = b.build();
    }

    private EventsConfig() { }
}
