package com.matt.quantumfalcon.world;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.network.DangerTierPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Area danger tiers (1-7 on the BreedingData scale) and the on-entry banner trigger. Provisional table by dimension until
 * PROGRESSION.md assigns structures and biomes (stage E); 0 means "no banner". Checked once a second per player; a change of
 * area or tier (including login into a dangerous dimension) sends {@link DangerTierPayload}.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class DangerTiers {
    /** names of the Rift distance bands (zone 0..3); zone 0 is "Safe". */
    public static final String[] RIFT_BANDS = {"Safe", "Frontier", "Deep Rift", "Abyss"};

    public record Area(String name, int tier) {}
    private static final Map<UUID, Area> LAST = new ConcurrentHashMap<>();

    private DangerTiers() {}

    public static Area of(ResourceLocation dim) {
        String ns = dim.getNamespace(), p = dim.getPath();
        if (ns.equals("minecraft")) return switch (p) {
            case "the_nether" -> new Area("The Nether", 3);
            case "the_end" -> new Area("The End", 5);
            default -> new Area("Overworld", 0);
        };
        if (ns.equals(QuantumFalcon.MOD_ID)) {
            if (p.startsWith("echo_rift_")) {
                int layer = Character.isDigit(p.charAt(p.length() - 1)) ? p.charAt(p.length() - 1) - '0' : 0;
                return new Area("Echo Rift · Layer " + (layer + 1), Math.min(7, 2 + layer));
            }
            if (p.equals("echo_rift")) return new Area("Echo Rift", 2);
            if (p.equals("echo_depths")) return new Area("Echo Depths", 4);
        }
        return new Area("", 0);
    }

    /**
     * structures carry their own tier, so the banner fires in the Overworld too (dungeons, arenas, the citadel),
     * not only on a dimension change. Smaller utility structures stay silent (tier 0).
     */
    public static Area ofStructure(String path) {
        if (path == null) return null;
        if (path.startsWith("dungeon_")) {
            String realm = path.substring(path.lastIndexOf('_') + 1);
            int t = switch (realm) {
                // the banner states the tier the dungeon's chests pay (dungeon/*_tN tables, WORLD.md families)
                case "riftslate" -> 1;
                case "aurorite" -> 2;
                case "prismite", "cinderite" -> 3;
                case "chronoslate" -> 4;
                case "abyssite" -> 5;
                case "voidshale" -> 6;
                case "singularite" -> 7;
                default -> 1;
            };
            return new Area("Echo Dungeon \u00b7 " + Character.toUpperCase(realm.charAt(0)) + realm.substring(1), t);
        }
        return switch (path) {
            case "quantum_guardian_arena" -> new Area("Overseer's Altar", 3);
            case "void_horror_arena" -> new Area("Warden's Rift", 4);
            case "entangled_twins_arena" -> new Area("Twins' Crossing", 5);
            case "singularity_sovereign_arena" -> new Area("Singularity Sanctum", 7);
            case "void_citadel" -> new Area("Void Citadel", 5);
            case "quantum_observatory" -> new Area("Quantum Observatory", 2);
            case "echo_rift_gate" -> new Area("Echo Rift Gate", 2);
            default -> null;
        };
    }

    /** Called once a second by PlayerAreaWatcher with the structure the player stands in (or null). */
    public static void update(ServerPlayer sp, String structurePath) {
        Area s = ofStructure(structurePath);
        Area a = s != null ? s : of(sp.level().dimension().location());
        // in the Echo Rift the banner follows the instability zone (one scale): zone 0-3 -> tier 2-5
        if (s == null && sp.level().dimension().location().getPath().equals("echo_rift") && a.tier() > 0) {
            int z = com.matt.quantumfalcon.dimension.InstabilityEffects.zoneTier(sp);
            // one scale (the danger tier); the Rift bands get names, not a second set of numbers
            a = new Area("Echo Rift \u00b7 " + RIFT_BANDS[Math.max(0, Math.min(3, z))], 2 + z);
        }
        if (sp.level().dimension().location().getPath().equals("echo_rift")) codexBands(sp);
        Area last = LAST.put(sp.getUUID(), a);
        if (a.equals(last)) return;
        if (a.tier() > 0) PacketDistributor.sendToPlayer(sp, new DangerTierPayload(a.name(), a.tier()));
    }

    /** Past the Abyss line by this much again, the Codex counts "the edge" (da_07). */
    public static final int EDGE_DIST = com.matt.quantumfalcon.dimension.InstabilityEffects.T3_DIST * 2;

    /** the Deep Rift Codex chapter follows the distance bands the banner names (the numbered layers can't be reached). */
    static void codexBands(ServerPlayer sp) {
        double d = com.matt.quantumfalcon.dimension.InstabilityEffects.anchorDistance(sp);
        int z = com.matt.quantumfalcon.dimension.InstabilityEffects.tierForDistance(d);
        if (z >= 1) com.matt.quantumfalcon.codex.RiftCodex.onObjective(sp, "RIFT_BAND", "frontier", 1, true);
        if (z >= 2) com.matt.quantumfalcon.codex.RiftCodex.onObjective(sp, "RIFT_BAND", "deep_rift", 1, true);
        if (z >= 3) com.matt.quantumfalcon.codex.RiftCodex.onObjective(sp, "RIFT_BAND", "abyss", 1, true);
        if (d >= EDGE_DIST) com.matt.quantumfalcon.codex.RiftCodex.onObjective(sp, "RIFT_BAND", "edge", 1, true);
    }

    /** Last area the server resolved for this player (qftests). */
    public static Area last(UUID id) { return LAST.get(id); }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LAST.remove(e.getEntity().getUUID()); }
}
