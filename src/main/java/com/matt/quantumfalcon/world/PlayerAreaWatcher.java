package com.matt.quantumfalcon.world;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.quest.PlayerQuestData;
import com.matt.quantumfalcon.quest.QuestTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;

/**
 * Once a second per player: which of our structures (if any) the player stands in, and which biome. Feeds quest
 * DISCOVER_LOCATION / VISIT_BIOME objectives (nothing called those hooks) and the danger banner.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class PlayerAreaWatcher {
    private PlayerAreaWatcher() { }

    /** Structure path -> quest location id used by QuestTracker objectives. Others report "structure:<path>". */
    private static final Map<String, String> LOCATION_IDS = Map.of(
        "quantum_guardian_arena", "overseer_altar",
        "singularity_sovereign_arena", "sovereign_throne",
        "quantum_well", "quantum_well",
        "ancient_nest", "ancient_nest");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7) return;
        check(p);
    }

    /** One scan (public for qftests). Returns the structure path the player stands in, or null. */
    public static String check(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos pos = p.blockPosition();
        level.getBiome(pos).unwrapKey().ifPresent(k -> QuestTracker.onBiomeVisited(p, k.location().toString()));
        String inside = structureAt(level, pos);
        if (inside != null) {
            com.matt.quantumfalcon.story.StoryEvents.onStructure(p, inside);
            String loc = LOCATION_IDS.getOrDefault(inside, "structure:" + inside);
            PlayerQuestData data = PlayerQuestData.get(p);
            // count each place once per player so "find 3 wells" needs 3 different wells
            BlockPos origin = structureOrigin(level, pos, inside);
            String key = loc + "@" + (origin == null ? "?" : origin.getX() + "," + origin.getZ());
            if (data != null && data.addDiscoveredLocation(key)) QuestTracker.onLocationDiscovered(p, loc);
        }
        DangerTiers.update(p, inside);
        return inside;
    }

    /** Our structure whose piece contains pos, or null. Only chunks that reference a structure are looked at. */
    public static String structureAt(ServerLevel level, BlockPos pos) {
        var mgr = level.structureManager();
        for (Structure s : mgr.getAllStructuresAt(pos).keySet()) {
            ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(s);
            if (id == null || !id.getNamespace().equals(QuantumFalcon.MOD_ID)) continue;
            if (mgr.getStructureWithPieceAt(pos, s).isValid()) return id.getPath();
        }
        return null;
    }

    private static BlockPos structureOrigin(ServerLevel level, BlockPos pos, String path) {
        var mgr = level.structureManager();
        for (Structure s : mgr.getAllStructuresAt(pos).keySet()) {
            ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(s);
            if (id != null && id.getPath().equals(path)) {
                var start = mgr.getStructureWithPieceAt(pos, s);
                if (start.isValid()) return start.getBoundingBox().getCenter();
            }
        }
        return null;
    }
}
