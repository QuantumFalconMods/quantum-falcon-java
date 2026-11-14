package com.matt.quantumfalcon.story;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;

/**
 * Storyline Manager for Quantum Falcon mod.
 * Manages story progression, chapter triggers, and narrative choices.
 * 
 * The story follows the player's journey from taming a first falcon
 * to discovering the truth about quantum reality and making a final choice.
 */
public class StorylineManager {
    
    /**
     * Trigger a story chapter event for a player.
     * Shows chapter title/subtitle and triggers associated events.
     */
    public static void triggerStoryEvent(Player player, StoryChapter chapter) {
        PlayerStoryData data = PlayerStoryData.get(player);
        if (data.hasSeenChapter(chapter)) {
            return;
        }
        
        data.markChapterSeen(chapter);
        data.save(player);
        
        // Display chapter announcement
        player.displayClientMessage(
            Component.literal(chapter.getTitle())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        player.displayClientMessage(
            Component.literal(chapter.getSubtitle())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        
        // Trigger chapter-specific events
        switch (chapter) {
            case CHAPTER_1 -> triggerFirstFalconEvent(player);
            case CHAPTER_5 -> triggerSacrificeEvent(player);
            case CHAPTER_8 -> triggerRevelationEvent(player);
            case CHAPTER_9 -> triggerFinalChoice(player);
            case EPILOGUE_LIGHT -> triggerLightEnding(player, data);
            case EPILOGUE_DARK -> triggerDarkEnding(player, data);
            case EPILOGUE_TRUE -> triggerTrueEnding(player, data);
            default -> {} // No special event for this chapter
        }
    }

    // ========================================
    // CHAPTER EVENT TRIGGERS
    // ========================================

    private static void triggerFirstFalconEvent(Player player) {
        // the goal as a title card on the first tame or hatch
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(15, 80, 25));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.translatable("message.quantumfalcon.goal.subtitle").withStyle(ChatFormatting.GRAY)));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.translatable("message.quantumfalcon.goal.title").withStyle(ChatFormatting.GOLD)));
        }
        player.displayClientMessage(
            Component.literal("🦅 Your falcon sees the stitches that hold the night. Together, take back the held hour.")
                .withStyle(ChatFormatting.YELLOW), false);
    }

    private static void triggerSacrificeEvent(Player player) {
        player.displayClientMessage(
            Component.literal("A vision: your falcon flies east into the dark and does not come back. Then it lands on your arm, unharmed. The Warden showed you the price.")
                .withStyle(ChatFormatting.DARK_RED), false);
    }

    private static void triggerRevelationEvent(Player player) {
        String[] visions = {
            "You see a throne at the edge of a black hole.",
            "The one on it wears your face.",
            "Not the past. A future.",
            "One you could still choose."
        };
        for (String vision : visions) {
            player.displayClientMessage(
                Component.literal(vision)
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
        }
    }

    private static void triggerFinalChoice(Player player) {
        player.displayClientMessage(
            Component.literal("=== THE FINAL CHOICE ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        player.displayClientMessage(
            Component.literal("The hour is in your hands.")
                .withStyle(ChatFormatting.GRAY), false);
        player.displayClientMessage(
            Component.literal("[Light] Let the morning come.")
                .withStyle(ChatFormatting.GREEN), false);
        player.displayClientMessage(
            Component.literal("[Dark] Keep the hour. Its crown is yours.")
                .withStyle(ChatFormatting.RED), false);
        player.displayClientMessage(
            Component.literal("[True] Braid the hours. Needs the four relics and a max-bond falcon of every kind.")
                .withStyle(ChatFormatting.AQUA), false);
    }

    public static void triggerLightEnding(Player player, PlayerStoryData data) {
        data.setChosenEnding("LIGHT");
        data.save(player);
        player.displayClientMessage(
            Component.literal("✦ The night has an ending now. The falcons stop looking east. ✦")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), false);
    }

    public static void triggerDarkEnding(Player player, PlayerStoryData data) {
        data.setChosenEnding("DARK");
        data.save(player);
        player.displayClientMessage(
            Component.literal("✦ It is always this hour, and it answers to you. ✦")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
    }

    public static void triggerTrueEnding(Player player, PlayerStoryData data) {
        data.setChosenEnding("TRUE");
        data.save(player);
        player.displayClientMessage(
            Component.literal("✦ Morning comes. The doors stay. You are the stitch. ✦")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Check if player can access the true ending.
     * Requires maximum bond level with all falcon types.
     */
    public static boolean canAccessTrueEnding(Player player) {
        PlayerStoryData data = PlayerStoryData.get(player);
        return data.hasMaxBondAllTypes();
    }
    
    /**
     * Get the player's current story progress percentage.
     */
    public static float getStoryProgress(Player player) {
        PlayerStoryData data = PlayerStoryData.get(player);
        int totalChapters = StoryChapter.values().length - 3; // Exclude epilogues
        int seenChapters = data.getSeenChapterCount();
        return (float) seenChapters / totalChapters;
    }
    
    /**
     * Check if player has completed the story (reached any ending).
     */
    public static boolean hasCompletedStory(Player player) {
        PlayerStoryData data = PlayerStoryData.get(player);
        return data.getChosenEnding() != null;
    }

    public static void register(IEventBus modEventBus) {
        QuantumFalcon.LOGGER.info("⚡ Story System initialized with {} chapters", StoryChapter.values().length);
    }

    // ========================================
    // STORY CHAPTERS ENUM
    // ========================================

    public enum StoryChapter {
        PROLOGUE("The First Rift", "Shards are falling where the night leaks through. Something is holding back the dawn."),
        CHAPTER_1("Eyes That See Stitches", "A wild falcon meets your eyes and does not look away."),
        CHAPTER_2("Echoes of the Past", "Your falcon turns east as you step through, as if it has been here before."),
        CHAPTER_3("The Guardian's Burden", "These creatures were never pets. They were protectors."),
        CHAPTER_4("Fracture Point", "The Rift Warden speaks: 'You should not exist here.'"),
        CHAPTER_5("The Price", "The Warden showed you what mending costs. Not yet."),
        CHAPTER_6("Quantum Bonds", "For a moment you feel every falcon you've ever bonded with, all facing the same way."),
        CHAPTER_7("The Twins' Truth", "Two bodies, one consciousness. You finally understand."),
        CHAPTER_8("Sins of Tomorrow", "The Singularity shows you a future. You are on the throne."),
        CHAPTER_9("The Held Hour", "Mend the hour, keep it, or find the third way."),
        EPILOGUE_LIGHT("Morning", "The night has an ending now. The falcons stop looking east."),
        EPILOGUE_DARK("The Last Night", "It is always this hour, and it answers to you."),
        EPILOGUE_TRUE("The Braided Hour", "Morning comes. The doors stay. You are the stitch.");

        private final String title;
        private final String subtitle;

        StoryChapter(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }

        public String getTitle() {
            return this.title;
        }

        public String getSubtitle() {
            return this.subtitle;
        }
    }
}
