package com.matt.quantumfalcon.story;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.skill.PlayerSkillData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Server-side story bookkeeping that isn't tied to one entity or block. */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class StoryEvents {
    private StoryEvents() { }

    /** Story §4.4: after the True ending the player list shows the title the Overseer gave you. */
    @SubscribeEvent
    public static void onTabName(net.neoforged.neoforge.event.entity.player.PlayerEvent.TabListNameFormat e) {
        if (!"TRUE".equals(PlayerStoryData.get(e.getEntity()).getChosenEnding())) return;
        net.minecraft.network.chat.Component base = e.getDisplayName() != null ? e.getDisplayName() : e.getEntity().getDisplayName();
        e.setDisplayName(base.copy().append(net.minecraft.network.chat.Component.translatable("title.quantumfalcon.late_stitch").withStyle(net.minecraft.ChatFormatting.GOLD)));
    }

    /** backfill: players who tamed a falcon before taming marked Chapter 1 get it silently, so breeding opens. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PlayerStoryData data = PlayerStoryData.get(p);
        PlayerSkillData skills = PlayerSkillData.get(p);
        if (skills != null && skills.hasTamedFirstFalcon() && !data.hasSeenChapter(StorylineManager.StoryChapter.CHAPTER_1)) {
            data.markChapterSeen(StorylineManager.StoryChapter.CHAPTER_1);
            data.save(p);
        }
        // a choice left pending (logged out on the choice screen) is offered again
        if (StoryManager.canMakeEndingChoice(p))
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, com.matt.quantumfalcon.network.OpenStoryChoiceScreenPacket.OpenStoryChoiceScreenPayload.of(p));
    }

    /** the Prologue fires on the first Quantum Shard picked up. */
    @SubscribeEvent
    public static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Post e) {
        if (e.getPlayer() instanceof ServerPlayer p && e.getOriginalStack().is(com.matt.quantumfalcon.registry.ModItems.QUANTUM_SHARD.get()))
            StorylineManager.triggerStoryEvent(p, StorylineManager.StoryChapter.PROLOGUE);
    }

    /** chapters with no single event of their own, checked every 2 seconds. */
    @SubscribeEvent
    public static void onTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 13) return;
        PlayerStoryData d = PlayerStoryData.get(p);
        boolean ch1 = d.hasSeenChapter(StorylineManager.StoryChapter.CHAPTER_1);
        // Chapter 2: in the Rift with a falcon of your own
        if (ch1 && !d.hasSeenChapter(StorylineManager.StoryChapter.CHAPTER_2) && com.matt.quantumfalcon.dimension.EchoRiftDimension.isEchoRift(p.level()))
            StorylineManager.triggerStoryEvent(p, StorylineManager.StoryChapter.CHAPTER_2);
        // Chapter 6: the first falcon kind at max bond
        if (ch1 && !d.hasSeenChapter(StorylineManager.StoryChapter.CHAPTER_6) && d.maxBondTypeCount() > 0)
            StorylineManager.triggerStoryEvent(p, StorylineManager.StoryChapter.CHAPTER_6);
    }

    /** Chapter 4 on first entering the Warden's arena, Chapter 8 on first entering the Sovereign's (PlayerAreaWatcher). */
    public static void onStructure(ServerPlayer p, String path) {
        if ("void_horror_arena".equals(path)) StorylineManager.triggerStoryEvent(p, StorylineManager.StoryChapter.CHAPTER_4);
        else if ("singularity_sovereign_arena".equals(path)) StorylineManager.triggerStoryEvent(p, StorylineManager.StoryChapter.CHAPTER_8);
    }
}
