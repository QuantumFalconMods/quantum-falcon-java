package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.codex.RiftCodex;
import com.matt.quantumfalcon.story.PlayerStoryData;
import com.matt.quantumfalcon.story.StoryManager;
import com.matt.quantumfalcon.story.StorylineManager.StoryChapter;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** Story review: the Held Hour tab lines, the story advancement tree, chapter text, the True Path readout. */
final class StoryTests {
    private StoryTests() { }

    static void register(Map<String, Consumer<ServerLevel>> t) {
        t.put("story_held_hour", StoryTests::heldHour);
        t.put("story_advancements", StoryTests::advancements);
        t.put("story_text", StoryTests::text);
        t.put("story_dusk_east", StoryTests::duskEast);
    }

    private static FakePlayer fresh(ServerLevel level, String name) {
        return FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(("qf-story-" + name + System.nanoTime()).getBytes()), name));
    }

    private static Map<String, String> hh(FakePlayer p) {
        Map<String, String> m = new HashMap<>();
        for (String l : RiftCodex.heldHourLines(p)) { String[] a = l.split("\t", 3); m.put(a[1], a.length > 2 ? a[2] : ""); }
        return m;
    }

    static void heldHour(ServerLevel level) {
        FakePlayer p = fresh(level, "HeldHour");
        Map<String, String> a = hh(p);
        PlayerStoryData d = PlayerStoryData.get(p);
        d.markBossDefeated("QUANTUM_OVERSEER"); d.markBossDefeated("RIFT_WARDEN");
        d.raiseFalconBond("VOID", 100); d.raiseFalconBond("ECHO", 100);
        Map<String, String> b = hh(p);
        d.markBossDefeated("ENTANGLED_TWINS"); d.markBossDefeated("SINGULARITY_SOVEREIGN"); d.setChosenEnding("TRUE");
        Map<String, String> c = hh(p);
        boolean ok = a.get("pieces").startsWith("0") && a.get("next").equals("the Quantum Overseer") && a.get("ending").isEmpty()
            && b.get("pieces").startsWith("2") && b.get("next").equals("the Entangled Twins")
            && c.get("pieces").startsWith("4") && c.get("next").isEmpty() && c.get("ending").equals("TRUE");
        boolean inCodex = RiftCodex.codexLines(p).stream().anyMatch(l -> l.startsWith("hh\tpieces"));
        result("story_held_hour", ok && inCodex, "fresh: " + a + " | 2 down: " + b + " | all down + True: " + c + " | sent with the Codex=" + inCodex);
    }

    static void advancements(ServerLevel level) {
        var adv = level.getServer().getAdvancements();
        java.util.function.Function<String, String> parent = path -> {
            var h = adv.get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", path));
            return h == null ? "MISSING" : h.value().parent().map(ResourceLocation::getPath).orElse("-");
        };
        String tame = parent.apply("story/tame_first_falcon"), core = parent.apply("story/craft_quantum_core"), rift = parent.apply("story/enter_rift"),
            gem = parent.apply("story/forge_reality_gem"), choose = parent.apply("story/choose_ending");
        boolean oneRoot = adv.get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", "story/root")) == null;
        // FakePlayers keep no advancements (NeoForge no-ops award), so check the code-granted shape: one impossible criterion
        var ch = adv.get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", "story/choose_ending"));
        boolean granted = ch != null && ch.value().criteria().size() == 1 && ch.value().criteria().containsKey("chosen");
        boolean ok = tame.equals("root") && core.equals("story/tame_first_falcon") && rift.equals("story/craft_quantum_core")
            && gem.equals("story/defeat_sovereign") && choose.equals("story/defeat_sovereign") && oneRoot && granted;
        result("story_advancements", ok, "tame<-" + tame + " core<-" + core + " rift<-" + rift + " gem<-" + gem + " choose<-" + choose
            + " | second root gone=" + oneRoot + " | choose_ending code-granted (1 impossible criterion)=" + granted);
    }

    static void text(ServerLevel level) {
        List<String> bad = new ArrayList<>();
        if (StoryChapter.CHAPTER_5.getTitle().equals("Sacrifice")) bad.add("ch5 title");
        if (StoryChapter.CHAPTER_1.getSubtitle().contains("wounded")) bad.add("ch1 wounded");
        // every deep_rift quest targets something reachable (no numbered layers, no echo_depths dimension)
        for (var q : RiftCodex.QUESTS.values())
            if (q.type().equals("ENTER_DIMENSION") && (q.target().matches(".*echo_rift_\\d") || q.target().endsWith("echo_depths"))) bad.add("unreachable " + q.id());
        boolean bands = RiftCodex.QUESTS.values().stream().filter(q -> q.type().equals("RIFT_BAND")).count() == 3;
        var fh = RiftCodex.QUESTS.get("fh_02");
        boolean irel = fh != null && fh.type().equals("SIG_KILL") && fh.target().equals("skywarden_irel") && com.matt.quantumfalcon.loot.sig.SignatureLoot.source("skywarden_irel") != null;
        boolean noEastEgg = !RiftCodex.QUESTS.containsKey("fh_03");
        String goal = net.minecraft.locale.Language.getInstance().getOrDefault("message.quantumfalcon.goal.title");
        result("story_text", bad.isEmpty() && bands && irel && noEastEgg, "problems=" + bad + " band quests=" + bands + " Irel quest+source=" + irel + " East Egg quest dropped=" + noEastEgg + " goal key(server lang)=" + goal);
    }

    /** Approved mechanic: perched falcons face east at dusk (cosmetic); after a Light ending they stop and wear the Light look. */
    static void duskEast(ServerLevel level) {
        var pad = new net.minecraft.core.BlockPos(5200, 0, -5200);
        WorldDropsTests.force(level, pad, true);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pad.getX(), pad.getZ());
        var at = pad.atY(y);
        WorldDropsTests.whenTicking(level, at, 0, () -> {
            long oldTime = level.getDayTime();
            FakePlayer owner = fresh(level, "Dusk");
            var f = com.matt.quantumfalcon.registry.ModEntities.QUANTUM_FALCON.get().create(level);
            f.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 90f, 0f);
            f.setYHeadRot(90f); f.yBodyRot = 90f;
            level.addFreshEntity(f);
            f.tame(owner); f.setOrderedToSit(true); f.setInSittingPose(true);
            level.setDayTime(oldTime - oldTime % 24000 + 12400);
            QFTestCommands.later(60, () -> {
                float head = net.minecraft.util.Mth.wrapDegrees(f.getYHeadRot() + 90f);
                boolean east = f.duskEast && Math.abs(head) < 25f;
                PlayerStoryData.get(owner).setChosenEnding("LIGHT");
                f.syncEnding(owner);   // FakePlayers are not in the level's player list, so getOwner() can't find them
                QFTestCommands.later(60, () -> {
                    boolean stopped = !f.duskEast && f.endingLook() == 1;
                    level.setDayTime(oldTime - oldTime % 24000 + 6000);
                    QFTestCommands.later(10, () -> {
                        PlayerStoryData.get(owner).setChosenEnding(null);
                        f.syncEnding(owner);
                        QFTestCommands.later(50, () -> {
                            boolean noonIdle = !f.duskEast && f.endingLook() == 0;
                            level.setDayTime(oldTime);
                            f.discard(); WorldDropsTests.force(level, pad, false);
                            result("story_dusk_east", east && stopped && noonIdle, "dusk: headOffEast=" + head + " east=" + east
                                + " | after Light: stopped+look=" + stopped + " | noon, no ending: idle+plain=" + noonIdle);
                        });
                    });
                });
            });
        });
    }
}
