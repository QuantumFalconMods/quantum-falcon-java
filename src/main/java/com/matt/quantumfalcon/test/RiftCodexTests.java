package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.codex.RiftCodex;
import com.matt.quantumfalcon.quest.QuestTracker;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.skills.UseSkill;
import com.matt.quantumfalcon.skills.UseSkills;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** A1 Rift Codex: data loads cleanly, triggers fire once, rewards can't be duplicated, prerequisites gate, totals catch up, state persists. */
public final class RiftCodexTests {
    private RiftCodexTests() {}

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("codex_quests_load", RiftCodexTests::load);
        tests.put("codex_trigger_once_no_dupe", RiftCodexTests::once);
        tests.put("codex_prereq_gate_and_catchup", RiftCodexTests::gate);
        tests.put("codex_persistence", RiftCodexTests::persist);
        tests.put("codex_sig_kill_hook", RiftCodexTests::sigKill);
    }

    static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static void load(ServerLevel level) {
        int n = RiftCodex.QUESTS.size();
        // every chain reaches a root (no cycles)
        int cyc = 0;
        for (var q : RiftCodex.QUESTS.values()) {
            Set<String> seen = new HashSet<>(); Deque<String> st = new ArrayDeque<>(q.requires());
            while (!st.isEmpty()) { String r = st.pop(); if (r.equals(q.id())) { cyc++; break; } if (seen.add(r) && RiftCodex.QUESTS.containsKey(r)) st.addAll(RiftCodex.QUESTS.get(r).requires()); }
        }
        // COLLECT/CRAFT targets are real items
        int badTargets = 0;
        for (var q : RiftCodex.QUESTS.values())
            if ((q.type().equals("COLLECT_ITEM") || q.type().equals("CRAFT_ITEM")) && !BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(q.target()))) badTargets++;
        StringBuilder d = new StringBuilder();
        for (var c : RiftCodex.CHAPTERS.values()) d.append(c.title()).append('=').append(c.quests().size()).append(' ');
        boolean ok = n >= 40 && RiftCodex.LOAD_ERRORS.isEmpty() && cyc == 0 && badTargets == 0;
        result("codex_quests_load", ok, n + " quests in " + RiftCodex.CHAPTERS.size() + " chapters [" + d.toString().trim() + "] errors=" + RiftCodex.LOAD_ERRORS + " cycles=" + cyc + " bad targets=" + badTargets);
    }

    private static void once(ServerLevel level) {
        FakePlayer fp = AuditTests.freshPlayer(level, "QFCodexOnce");
        RiftCodex.reset(fp);
        Item treat = item("quantumfalcon:falcon_treat");
        fp.getInventory().add(new ItemStack(ModItems.QUANTUM_SHARD.get(), 5));
        RiftCodex.collectCheck(fp);
        boolean done1 = RiftCodex.isDone(fp, "fb_01");
        int t1 = fp.getInventory().countItem(treat);
        // the same shards again, more shards, drop and re-pick: no second reward
        RiftCodex.collectCheck(fp);
        fp.getInventory().add(new ItemStack(ModItems.QUANTUM_SHARD.get(), 10));
        RiftCodex.collectCheck(fp);
        RiftCodex.onObjective(fp, "COLLECT_ITEM", "quantumfalcon:quantum_shard", 50, false);
        int t2 = fp.getInventory().countItem(treat);
        // a friend throws 5 shards over; picking them up doesn't complete your collect quest
        FakePlayer friend = AuditTests.freshPlayer(level, "QFCodexFriend"), borrower = AuditTests.freshPlayer(level, "QFCodexBorrower");
        RiftCodex.reset(borrower);
        borrower.moveTo(fp.getX(), fp.getY(), fp.getZ());
        var ie = new net.minecraft.world.entity.item.ItemEntity(level, borrower.getX(), borrower.getY(), borrower.getZ(), new ItemStack(ModItems.QUANTUM_SHARD.get(), 5));
        ie.setThrower(friend); ie.setNoPickUpDelay();
        level.addFreshEntity(ie);
        ie.playerTouch(borrower);
        int borrowed = borrower.getInventory().countItem(ModItems.QUANTUM_SHARD.get());
        RiftCodex.collectCheck(borrower);
        boolean borrowedDone = RiftCodex.isDone(borrower, "fb_01");
        ie.discard();
        boolean ok = done1 && t1 == 4 && t2 == 4 && RiftCodex.doneCount(fp) == 1 && borrowed == 5 && !borrowedDone;
        result("codex_trigger_once_no_dupe", ok, "5 shards held -> fb_01 done=" + done1 + " treats " + t1 + " | after repeats treats " + t2 + " done count " + RiftCodex.doneCount(fp)
            + " | 5 shards thrown by a friend picked up=" + borrowed + " -> fb_01 done=" + borrowedDone);
    }

    private static void gate(ServerLevel level) {
        FakePlayer fp = AuditTests.freshPlayer(level, "QFCodexGate");
        RiftCodex.reset(fp);
        fp.getInventory().add(new ItemStack(ModItems.QUANTUM_SHARD.get(), 5));
        RiftCodex.collectCheck(fp);                                                     // fb_01
        QuestTracker.updateObjective(fp, QuestTracker.ObjectiveType.CRAFT_ITEM, "quantumfalcon:falcon_whistle", 1);   // fb_03 needs fb_02 first
        boolean earlyCraft = RiftCodex.isDone(fp, "fb_03") || RiftCodex.progress(fp, "fb_03") > 0;
        UseSkills.setXp(fp, UseSkill.FALCONRY, UseSkill.totalFor(6));                     // Falconry 6 before fb_06 is unlocked
        QuestTracker.updateObjective(fp, QuestTracker.ObjectiveType.TAME_FALCON, "any", 1);   // fb_02 -> unlocks fb_03..fb_07
        boolean tame = RiftCodex.isDone(fp, "fb_02");
        boolean caughtUp = RiftCodex.isDone(fp, "fb_06");                                // catch-up from the existing level
        boolean chainNotSkipped = !RiftCodex.isDone(fp, "fb_07");                       // the tame that unlocked fb_07 doesn't also count for it
        QuestTracker.updateObjective(fp, QuestTracker.ObjectiveType.CRAFT_ITEM, "quantumfalcon:falcon_whistle", 1);
        boolean craft = RiftCodex.isDone(fp, "fb_03");
        boolean ok = !earlyCraft && tame && caughtUp && chainNotSkipped && craft;
        result("codex_prereq_gate_and_catchup", ok, "craft before unlock counted=" + earlyCraft + " | tame -> fb_02=" + tame + " | Falconry 6 earned earlier -> fb_06=" + caughtUp
            + " | same tame not reused for fb_07=" + chainNotSkipped + " | craft after unlock -> fb_03=" + craft);
    }

    private static void persist(ServerLevel level) {
        FakePlayer fp = AuditTests.freshPlayer(level, "QFCodexKeep");
        RiftCodex.reset(fp);
        fp.getInventory().add(new ItemStack(ModItems.QUANTUM_SHARD.get(), 5));
        RiftCodex.collectCheck(fp);
        RiftCodex.onObjective(fp, "BREED_FALCON", "any", 1, false);   // locked (needs bg_01): no progress
        FakePlayer reborn = CheeseTests.respawned(fp);
        var file = new net.minecraft.nbt.CompoundTag(); reborn.saveWithoutId(file);
        FakePlayer loaded = new FakePlayer(level, reborn.getGameProfile()); loaded.load(file);
        boolean ok = RiftCodex.isDone(reborn, "fb_01") && RiftCodex.isDone(loaded, "fb_01") && RiftCodex.progress(loaded, "bg_02") == 0;
        result("codex_persistence", ok, "fb_01 after death=" + RiftCodex.isDone(reborn, "fb_01") + " after save/load=" + RiftCodex.isDone(loaded, "fb_01") + " locked bg_02 progress=" + RiftCodex.progress(loaded, "bg_02"));
    }

    /** A credited miniboss kill (the A8 award path) advances SIG_KILL quests; a drop advances SIG_LOOT. */
    private static void sigKill(ServerLevel level) {
        FakePlayer fp = AuditTests.freshPlayer(level, "QFCodexSig");
        RiftCodex.reset(fp);
        RiftCodex.markDone(fp, "ef_09"); RiftCodex.markDone(fp, "ef_13"); RiftCodex.markDone(fp, "ef_14"); RiftCodex.markDone(fp, "bo_15");
        com.matt.quantumfalcon.loot.sig.SignatureLoot.TEST_RANDOM = SignatureLootTests.pinned(0f);   // always drops
        try { com.matt.quantumfalcon.loot.sig.SignatureLoot.award(fp, com.matt.quantumfalcon.loot.sig.SignatureLoot.source("dungeon_warden"), null); }
        finally { com.matt.quantumfalcon.loot.sig.SignatureLoot.TEST_RANDOM = null; }
        boolean kill = RiftCodex.isDone(fp, "bo_13");
        int sig = RiftCodex.progress(fp, "ma_13");
        result("codex_sig_kill_hook", kill, "dungeon_warden credited kill -> bo_13 done=" + kill + " | ma_13 (locked) progress=" + sig + " | quests loaded=" + RiftCodex.QUESTS.size());
    }
}
