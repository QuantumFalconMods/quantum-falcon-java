package com.matt.quantumfalcon.event;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Dynamic Event Manager for Quantum Falcon mod.
 * Manages world events like meteor showers, aurora borealis, quantum storms, and solar eclipses.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class DynamicEventManager {
    private static final Map<String, DynamicEvent> activeEvents = new HashMap<>();
    private static final Map<String, Long> eventCooldowns = new HashMap<>();
    
    // Event chances per 1000-tick check live in EventsConfig
    
    private static final long EVENT_COOLDOWN = 24000L;     // 1 Minecraft day

    @SubscribeEvent
    public static void onServerTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        long gameTime = serverLevel.getGameTime();
        String dimensionKey = serverLevel.dimension().location().toString();
        
        // Check for new events every 1000 ticks (master switch + dimension allowlist from EventsConfig)
        if (gameTime % 1000L == 0L && EventsConfig.ENABLED.get() && EventsConfig.DIMENSIONS.get().contains(dimensionKey)) {
            checkForNewEvents(serverLevel, gameTime);
        }
        
        // Tick active event
        DynamicEvent activeEvent = activeEvents.get(dimensionKey);
        if (activeEvent != null) {
            activeEvent.tick(serverLevel);
            if (activeEvent.isFinished()) {
                activeEvent.onEnd(serverLevel);
                activeEvents.remove(dimensionKey);
                eventCooldowns.put(dimensionKey, gameTime);
            }
        }
    }

    private static void checkForNewEvents(ServerLevel level, long gameTime) {
        String dimensionKey = level.dimension().location().toString();
        
        // Don't start new event if one is active
        if (activeEvents.containsKey(dimensionKey)) {
            return;
        }
        
        // Check cooldown
        Long lastEventTime = eventCooldowns.get(dimensionKey);
        if (lastEventTime != null && gameTime - lastEventTime < EVENT_COOLDOWN) {
            return;
        }
        
        // Check time of day
        long dayTime = level.getDayTime() % 24000L;
        boolean isNight = dayTime >= 13000L && dayTime <= 23000L;
        boolean isDay = !isNight;
        
        // Roll for events based on time
        if (isNight && level.random.nextFloat() < EventsConfig.AURORA_CHANCE.get()) {
            startEvent(level, new AuroraBorealisEvent());
            return;
        }
        
        if (level.random.nextFloat() < EventsConfig.STORM_CHANCE.get() * calm(level)) {   // story: the held hour leaks less as pieces come back
            startEvent(level, new QuantumStormEvent());
            return;
        }
        
        if (isNight && level.random.nextFloat() < EventsConfig.METEOR_CHANCE.get()) {
            startEvent(level, new MeteorShowerEvent());
            return;
        }
        
        if (isDay && level.random.nextFloat() < EventsConfig.ECLIPSE_CHANCE.get() * calm(level)) {
            startEvent(level, new SolarEclipseEvent());
        }
    }

    /**
     * Story (approved): storms and eclipses are the held night leaking into the day, so they thin out as the players in this
     * dimension take back pieces of the hour: x(1 - calmPerPiece * average pieces), never below 0.
     */
    public static double calm(ServerLevel level) {
        var players = level.players();
        if (players.isEmpty()) return 1.0;
        double sum = 0;
        for (var p : players) sum += com.matt.quantumfalcon.story.StoryManager.pieces(p);
        return Math.max(0.0, 1.0 - com.matt.quantumfalcon.WorldConfig.storyCalmPerPiece() * (sum / players.size()));
    }

    private static void startEvent(ServerLevel level, DynamicEvent event) {
        String dimensionKey = level.dimension().location().toString();
        activeEvents.put(dimensionKey, event);
        event.onStart(level);
        QuantumFalcon.LOGGER.info("Dynamic event started: {} in {}", 
            event.getClass().getSimpleName(), dimensionKey);
    }
    
    public static boolean isEventActive(Level level) {
        String dimensionKey = level.dimension().location().toString();
        return activeEvents.containsKey(dimensionKey);
    }
    
    public static DynamicEvent getActiveEvent(Level level) {
        String dimensionKey = level.dimension().location().toString();
        return activeEvents.get(dimensionKey);
    }

    public static void register(IEventBus modEventBus) {
        QuantumFalcon.LOGGER.info("⚡ Dynamic Event System initialized (4 event types)");
    }

    // ========================================
    // BASE EVENT CLASS
    // ========================================

    public static abstract class DynamicEvent {
        protected final int duration;
        protected int elapsed = 0;
        protected final String name;

        public DynamicEvent(String name, int durationTicks) {
            this.name = name;
            this.duration = durationTicks;
        }

        public void tick(ServerLevel level) {
            elapsed++;
            onTick(level);
        }

        public boolean isFinished() {
            return elapsed >= duration;
        }
        
        public String getName() {
            return name;
        }
        
        public float getProgress() {
            return (float) elapsed / duration;
        }

        public abstract void onStart(ServerLevel level);
        public abstract void onTick(ServerLevel level);
        public abstract void onEnd(ServerLevel level);
    }
}
