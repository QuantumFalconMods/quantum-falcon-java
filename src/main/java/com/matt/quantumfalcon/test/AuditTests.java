package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.story.PlayerStoryData;
import com.matt.quantumfalcon.story.StorylineManager.StoryChapter;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** One qftest per blocker from the 2026-11-12 full mod audit review. */
final class AuditTests {
    private AuditTests() { }

    static void register(LinkedHashMap<String, Consumer<ServerLevel>> tests) {
        tests.put("audit_br1_survival_breeding", AuditTests::br1);
        tests.put("audit_qu1_quests_earnable", AuditTests::qu1);
        tests.put("audit_qu1_main_chain", AuditTests::qu1Chain);
        tests.put("audit_r22_fragments_toss", AuditTests::r22FragmentsToss);
        tests.put("audit_st1_ending_persisted", AuditTests::st1);
        tests.put("audit_lo1_native_heart", AuditTests::lo1);
        tests.put("audit_dt1_structure_banner", AuditTests::dt1);
        tests.put("audit_ev1_events_safe", AuditTests::ev1);
        tests.put("audit_bo1_boss_gating", AuditTests::bo1);
        tests.put("audit_bo4_postgame_spawns", AuditTests::bo4);
        tests.put("audit_arena_chain_entry", AuditTests::arenaChainEntry);
        tests.put("audit_sovereign_shield_breaks", AuditTests::sovereignShieldBreaks);
        tests.put("audit_bo1_fresh_locate", AuditTests::bo1Fresh);
        tests.put("audit_n4_choice_screen", AuditTests::n4);
        tests.put("audit_rel1_published_portal_lib", AuditTests::rel1);
        tests.put("arena_design_natural", AuditTests::arenaDesign);
    }

    /** A brand-new player (no story flags, no test shortcut) tames two falcons with fish, then feeds berries: both must fall in love. */
    static FakePlayer freshPlayer(ServerLevel level, String name) {
        FakePlayer fp = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), name));
        fp.getInventory().clearContent();
        return fp;
    }

    private static void br1(ServerLevel level) {
        BlockPos p = pad(level, 3000, 3080, Blocks.GRASS_BLOCK);
        cleanup(level, p, 8);
        FakePlayer fp = freshPlayer(level, "QFSurvivor");
        boolean chapterBefore = PlayerStoryData.get(fp).hasSeenChapter(StoryChapter.CHAPTER_1);
        QuantumFalconEntity a = falcon(level, p, true), b = falcon(level, p.east(2), true);
        int tries = 0;
        while ((!a.isTame() || !b.isTame()) && tries < 120) { interact(fp, a.isTame() ? b : a, Items.SALMON, 1); tries++; }
        boolean tamed = a.isTame() && b.isTame();
        boolean chapter = PlayerStoryData.get(fp).hasSeenChapter(StoryChapter.CHAPTER_1);
        a.setAge(0); b.setAge(0); a.setGender(true); b.setGender(false);
        a.setOrderedToSit(false); b.setOrderedToSit(false);
        interact(fp, a, ModItems.QUANTUM_BERRY.get(), 1);
        interact(fp, b, ModItems.QUANTUM_BERRY.get(), 1);
        boolean love = a.isInLove() && b.isInLove();
        result("audit_br1_survival_breeding", !chapterBefore && tamed && chapter && love,
                "chapter1 before=" + chapterBefore + " tamed=" + tamed + " (" + tries + " fish) chapter1 after=" + chapter + " inLove=" + love);
        cleanup(level, p, 8);
    }

    // ------------------------------------------------------------------
    private static void qu1(ServerLevel level) {
        StringBuilder d = new StringBuilder();
        boolean ok = true;
        // (a) every quest has objectives whose targets exist, so its points can be earned
        var items = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        var ents = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE;
        java.util.Set<String> locs = java.util.Set.of("overseer_altar", "sovereign_throne", "quantum_well", "ancient_nest");
        java.util.List<String> bad = new java.util.ArrayList<>();
        for (var q : com.matt.quantumfalcon.quest.QuestSystem.QUESTS.values()) {
            var objs = com.matt.quantumfalcon.quest.QuestTracker.objectives(q.id);
            if (objs.isEmpty()) bad.add(q.id + ":no objectives");
            for (var o : objs) {
                boolean fine = switch (o.type) {
                    // registered is not enough; something must produce it
                    case COLLECT_ITEM, CRAFT_ITEM -> items.containsKey(net.minecraft.resources.ResourceLocation.parse(o.target)) && Obtainable.why(level.getServer(), o.target) != null;
                    case BOSS_KILL -> o.target.equals("any") || ents.containsKey(net.minecraft.resources.ResourceLocation.parse(o.target));
                    case DISCOVER_LOCATION -> locs.contains(o.target);
                    case ENTER_DIMENSION -> level.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.ResourceLocation.parse(o.target))) != null;
                    default -> true;
                };
                if (!fine) bad.add(q.id + ":" + o.type + " " + o.target);
            }
        }
        d.append("unreachable=").append(bad);
        ok &= bad.isEmpty();
        // (b) a new player gets the tutorial on login; real tame hook + bond hook finish tutorial_falcon
        FakePlayer fp = freshPlayer(level, "QFQuester");
        var qd = com.matt.quantumfalcon.quest.PlayerQuestData.get(fp);
        com.matt.quantumfalcon.quest.QuestTracker.startAvailableChains(fp);
        boolean started = qd.isQuestActive("tutorial_basics");
        int pts0 = com.matt.quantumfalcon.skill.SkillPointSystem.getAvailablePoints(fp);
        com.matt.quantumfalcon.quest.QuestTracker.updateObjective(fp, com.matt.quantumfalcon.quest.QuestTracker.ObjectiveType.COLLECT_ITEM, "quantumfalcon:quantum_shard", 5);
        com.matt.quantumfalcon.quest.QuestTracker.updateObjective(fp, com.matt.quantumfalcon.quest.QuestTracker.ObjectiveType.CRAFT_ITEM, "quantumfalcon:quantum_core", 1);
        boolean basicsDone = qd.isQuestCompleted("tutorial_basics");
        boolean nextActive = qd.isQuestActive("tutorial_falcon");
        int pts1 = com.matt.quantumfalcon.skill.SkillPointSystem.getAvailablePoints(fp);
        BlockPos p = pad(level, 3000, 3120, Blocks.GRASS_BLOCK);
        cleanup(level, p, 8);
        QuantumFalconEntity f = falcon(level, p, true);
        int tries = 0;
        while (!f.isTame() && tries < 60) { interact(fp, f, Items.SALMON, 1); tries++; }
        boolean falconDone = qd.isQuestCompleted("tutorial_falcon");
        d.append(" started=").append(started).append(" basicsDone=").append(basicsDone).append(" next=").append(nextActive)
         .append(" points ").append(pts0).append("->").append(pts1).append(" tutorial_falcon(real tame+bond hooks)=").append(falconDone);
        ok &= started && basicsDone && nextActive && pts1 == pts0 + com.matt.quantumfalcon.skill.SkillPointSystem.QUEST_COMPLETE_POINTS && falconDone;   // B5: only main quests pay points
        // (c) side quests: refused before their prerequisite, accepted after (same server path as the J-log packet)
        String early = com.matt.quantumfalcon.quest.QuestTracker.tryAccept(fp, "side_treasure");
        qd.acceptQuest("main_1"); qd.completeQuest("main_1");
        String late = com.matt.quantumfalcon.quest.QuestTracker.tryAccept(fp, "side_treasure");
        d.append(" sideBefore=").append(early).append(" sideAfter=").append(late == null ? "accepted" : late);
        ok &= early != null && late == null && qd.isQuestActive("side_treasure");
        // (d) permissions: a level-0 player can use /qf quest, skill list/unlock and falcon info, not give/grant/debug
        var disp = level.getServer().getCommands().getDispatcher();
        var src = fp.createCommandSourceStack().withPermission(0);
        java.util.function.Predicate<String> parses = c -> { var r = disp.parse(c, src); return !r.getReader().canRead() && r.getContext().getCommand() != null; };
        boolean playerOk = parses.test("qf quest list") && parses.test("qf quest accept side_breeder") && parses.test("qf skill list")
            && parses.test("qf skill unlock bond_kinship") && parses.test("qf falcon info");
        boolean adminBlocked = !parses.test("qf skill give 5") && !parses.test("qf skill grant bond_kinship") && !parses.test("qf quest complete main_1") && !parses.test("qf debug info");
        // running it anyway changes nothing and is answered by AdminCommandNotice (not Brigadier's "Incorrect argument")
        int ptsBefore = com.matt.quantumfalcon.skill.SkillPointSystem.getAvailablePoints(fp);
        java.util.List<String> said = new java.util.ArrayList<>();
        var capture = new net.minecraft.commands.CommandSource() {
            public void sendSystemMessage(net.minecraft.network.chat.Component c) { said.add(c.getString()); }
            public boolean acceptsSuccess() { return true; }
            public boolean acceptsFailure() { return true; }
            public boolean shouldInformAdmins() { return false; }
        };
        var capSrc = new net.minecraft.commands.CommandSourceStack(capture, fp.position(), fp.getRotationVector(), level, 0, fp.getName().getString(), fp.getDisplayName(), level.getServer(), fp);
        level.getServer().getCommands().performPrefixedCommand(capSrc, "qf skill give 5");
        boolean unchanged = com.matt.quantumfalcon.skill.SkillPointSystem.getAvailablePoints(fp) == ptsBefore;
        boolean friendly = said.stream().anyMatch(m -> m.contains("server operators"));
        adminBlocked &= unchanged && friendly;
        d.append(" perm0 player cmds=").append(playerOk).append(" admin cmds blocked=").append(adminBlocked).append(" giveReply=").append(said);
        ok &= playerOk && adminBlocked;
        cleanup(level, p, 8);
        result("audit_qu1_quests_earnable", ok, d.toString());
    }

    /** Real crafting for tests: matches a 3x3 grid (row-major, null = empty) in the recipe manager, takes the grid items
     *  from the player's inventory, adds the result and fires ItemCraftedEvent like the crafting table's output slot. */
    static net.minecraft.world.item.ItemStack craftFromInventory(ServerLevel level, FakePlayer fp, net.minecraft.world.item.Item[] grid) {
        java.util.Map<net.minecraft.world.item.Item, Integer> need = new java.util.HashMap<>();
        java.util.List<net.minecraft.world.item.ItemStack> cells = new java.util.ArrayList<>();
        for (var it : grid) { cells.add(it == null ? net.minecraft.world.item.ItemStack.EMPTY : new net.minecraft.world.item.ItemStack(it)); if (it != null) need.merge(it, 1, Integer::sum); }
        for (var e : need.entrySet()) if (fp.getInventory().countItem(e.getKey()) < e.getValue()) return null;
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, cells);
        var match = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level);
        if (match.isEmpty()) return null;
        var out = match.get().value().assemble(input, level.registryAccess());
        var inv = fp.getInventory();
        for (var e : need.entrySet()) {
            int left = e.getValue();
            for (int k = 0; k < inv.getContainerSize() && left > 0; k++) {
                var st = inv.getItem(k);
                if (st.is(e.getKey())) { int take = Math.min(left, st.getCount()); st.shrink(take); left -= take; }
            }
        }
        var matrix = new net.minecraft.world.SimpleContainer(cells.toArray(new net.minecraft.world.item.ItemStack[0]));
        out.onCraftedBy(level, fp, out.getCount());
        net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(fp, out, matrix);
        inv.add(out.copy());
        return out;
    }

    // ------------------------------------------------------------------ losing the Warden's fragments can't cap main_4
    private static void r22FragmentsToss(ServerLevel level) {
        FakePlayer fp = freshPlayer(level, "QFTosser");
        var qd = com.matt.quantumfalcon.quest.PlayerQuestData.get(fp);
        for (String q : new String[]{"tutorial_basics", "tutorial_falcon", "tutorial_breeding", "first_breed", "main_1", "main_2", "main_3"}) { qd.acceptQuest(q); qd.completeQuest(q); }
        com.matt.quantumfalcon.quest.QuestTracker.startAvailableChains(fp);
        boolean m4 = qd.isQuestActive("main_4");
        BlockPos p = new BlockPos(3000, 100, 3400);
        var frag = com.matt.quantumfalcon.registry.ModItems.ARTIFACT_FRAGMENT.get();
        com.matt.quantumfalcon.story.BossDeathHandler.onBossDefeated(fp, level, com.matt.quantumfalcon.story.BossDeathHandler.BossType.RIFT_WARDEN, p, false);
        int afterWarden = fp.getInventory().countItem(frag);
        fp.getInventory().clearOrCountMatchingItems(st -> st.is(frag), -1, fp.inventoryMenu.getCraftSlots());   // tossed / lost in the void
        com.matt.quantumfalcon.quest.QuestTracker.syncCollectObjectives(fp);
        boolean notDoneYet = !qd.isQuestCompleted("main_4");
        com.matt.quantumfalcon.story.BossDeathHandler.onBossDefeated(fp, level, com.matt.quantumfalcon.story.BossDeathHandler.BossType.ENTANGLED_TWINS, p, false);
        com.matt.quantumfalcon.quest.QuestTracker.syncCollectObjectives(fp);
        boolean done = qd.isQuestCompleted("main_4");
        // and a lost fragment is earned again from another Warden
        com.matt.quantumfalcon.story.BossDeathHandler.onBossDefeated(fp, level, com.matt.quantumfalcon.story.BossDeathHandler.BossType.RIFT_WARDEN, p, false);
        int again = fp.getInventory().countItem(frag);
        result("audit_r22_fragments_toss", m4 && afterWarden == 2 && notDoneYet && done && again >= 2,
            "main_4 active=" + m4 + " fragments after Warden=" + afterWarden + " tossed, main_4 not done=" + notDoneYet
            + " after Twins main_4 done=" + done + " fragments after a 2nd Warden=" + again);
    }

    // ------------------------------------------------------------------ Void Lord / Eclipse Phantom are post-game spawns only
    private static void bo4(ServerLevel level) {
        FakePlayer fp = freshPlayer(level, "QFPostGame");
        BlockPos at = fp.blockPosition();
        var players = java.util.List.of(fp);
        boolean freshBlocked = !com.matt.quantumfalcon.registry.ModEntityAttributes.postGameBossGate(players, at);
        com.matt.quantumfalcon.story.PlayerStoryData.get(fp).markBossDefeated("SINGULARITY_SOVEREIGN");
        boolean afterOpen = com.matt.quantumfalcon.registry.ModEntityAttributes.postGameBossGate(players, at);
        boolean farBlocked = !com.matt.quantumfalcon.registry.ModEntityAttributes.postGameBossGate(players, at.offset(100, 0, 0));
        result("audit_bo4_postgame_spawns", freshBlocked && afterOpen && farBlocked,
            "fresh player blocks spawn=" + freshBlocked + " after Sovereign kill allows=" + afterOpen + " 100 blocks away blocks=" + farBlocked);
    }

    // ------------------------------------------------------------------ residual + main_4 -> main_5 -> side_boss_slayer
    private static void qu1Chain(ServerLevel level) {
        StringBuilder d = new StringBuilder();
        FakePlayer fp = freshPlayer(level, "QFChain");
        var qd = com.matt.quantumfalcon.quest.PlayerQuestData.get(fp);
        var QT = com.matt.quantumfalcon.quest.QuestTracker.class;
        for (String q : new String[]{"tutorial_basics", "tutorial_falcon", "tutorial_breeding", "first_breed", "main_1", "main_2", "main_3"}) { qd.acceptQuest(q); qd.completeQuest(q); }
        com.matt.quantumfalcon.quest.QuestTracker.startAvailableChains(fp);
        boolean m4 = qd.isQuestActive("main_4");
        BlockPos p = new BlockPos(3000, 100, 3300);
        var BT = com.matt.quantumfalcon.story.BossDeathHandler.BossType.class;
        com.matt.quantumfalcon.story.BossDeathHandler.onBossDefeated(fp, level, com.matt.quantumfalcon.story.BossDeathHandler.BossType.RIFT_WARDEN, p, false);
        com.matt.quantumfalcon.story.BossDeathHandler.onBossDefeated(fp, level, com.matt.quantumfalcon.story.BossDeathHandler.BossType.ENTANGLED_TWINS, p, false);
        int frags = fp.getInventory().countItem(com.matt.quantumfalcon.registry.ModItems.ARTIFACT_FRAGMENT.get());
        com.matt.quantumfalcon.quest.QuestTracker.syncCollectObjectives(fp);
        boolean m4done = qd.isQuestCompleted("main_4"), m5 = qd.isQuestActive("main_5");
        d.append("main_4 active=").append(m4).append(" fragments from Warden+Twins=").append(frags).append(" main_4 done=").append(m4done).append(" main_5 active=").append(m5);
        // the Heart chain must be craftable from obtainable parts: relics -> Reality Gem -> Heart
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (String id : new String[]{"quantumfalcon:overseers_eye", "quantumfalcon:wardens_fragment", "quantumfalcon:twin_essence", "quantumfalcon:sovereigns_crown",
                "quantumfalcon:reality_fragment", "quantumfalcon:reality_gem", "quantumfalcon:quantum_core", "quantumfalcon:heart_of_quantum"})
            if (Obtainable.why(level.getServer(), id) == null) missing.add(id);
        d.append(" heart chain unobtainable=").append(missing);
        com.matt.quantumfalcon.quest.QuestTracker.setObjectiveAtLeast(fp, com.matt.quantumfalcon.quest.QuestTracker.ObjectiveType.DISCOVER_LOCATION, "sovereign_throne", 1);   // PlayerAreaWatcher does this in play
        com.matt.quantumfalcon.story.BossDeathHandler.onBossDefeated(fp, level, com.matt.quantumfalcon.story.BossDeathHandler.BossType.QUANTUM_OVERSEER, p, false);
        com.matt.quantumfalcon.story.BossDeathHandler.onBossDefeated(fp, level, com.matt.quantumfalcon.story.BossDeathHandler.BossType.SINGULARITY_SOVEREIGN, p, false);
        var I = new Object() { net.minecraft.world.item.Item eye = com.matt.quantumfalcon.registry.ModItems.OVERSEERS_EYE.get(), wf = com.matt.quantumfalcon.registry.ModItems.WARDENS_FRAGMENT.get(),
            te = com.matt.quantumfalcon.registry.ModItems.TWIN_ESSENCE.get(), crown = com.matt.quantumfalcon.registry.ModItems.SOVEREIGNS_CROWN.get(),
            rf = com.matt.quantumfalcon.registry.ModItems.REALITY_FRAGMENT.get(), core = com.matt.quantumfalcon.registry.ModItems.QUANTUM_CORE.get(),
            gem = com.matt.quantumfalcon.registry.ModItems.REALITY_GEM.get(), heart = com.matt.quantumfalcon.registry.ModItems.HEART_OF_QUANTUM.get(), star = Items.NETHER_STAR; };
        int relicsGiven = 0;
        for (var it : new net.minecraft.world.item.Item[]{I.eye, I.wf, I.te, I.crown}) relicsGiven += Math.min(1, fp.getInventory().countItem(it));
        d.append(" relics from the four kills=").append(relicsGiven).append("/4");
        // real crafts through the recipe manager. The relics come only from the kills above; the non-boss parts
        // (Reality Fragment, Quantum Core, 1 Nether Star,) are handed over here and are checked by Obtainable above.
        fp.getInventory().add(new net.minecraft.world.item.ItemStack(I.rf));
        fp.getInventory().add(new net.minecraft.world.item.ItemStack(I.core));
        fp.getInventory().add(new net.minecraft.world.item.ItemStack(I.star, 1));
        var gem = craftFromInventory(level, fp, new net.minecraft.world.item.Item[]{null, I.eye, null, I.wf, I.rf, I.te, null, I.crown, null});
        boolean gemOk = gem != null && gem.is(I.gem);
        var heart = gemOk ? craftFromInventory(level, fp, new net.minecraft.world.item.Item[]{null, I.star, null, null, I.gem, null, null, I.core, null}) : null;
        boolean heartOk = heart != null && heart.is(I.heart) && fp.getInventory().countItem(I.heart) == 1;
        d.append(" crafted gem=").append(gemOk).append(" crafted heart=").append(heartOk);
        boolean m5done = qd.isQuestCompleted("main_5");
        String slayer = com.matt.quantumfalcon.quest.QuestTracker.tryAccept(fp, "side_boss_slayer");
        boolean slayerOk = qd.isQuestActive("side_boss_slayer");
        // having turned the gem into the Heart still counts as carrying the four relics for the True ending
        boolean trueRelics = com.matt.quantumfalcon.story.StoryManager.hasAllArtifacts(fp);
        d.append(" main_5 done=").append(m5done).append(" side_boss_slayer accept=").append(slayer == null ? "ok" : slayer).append(" active=").append(slayerOk)
         .append(" relics still count after the Heart=").append(trueRelics);
        result("audit_qu1_main_chain", relicsGiven == 4 && gemOk && heartOk && m4 && frags == 4 && m4done && m5 && missing.isEmpty() && m5done && slayerOk && trueRelics, d.toString());
    }

    // ------------------------------------------------------------------
    private static void st1(ServerLevel level) {
        StringBuilder d = new StringBuilder();
        FakePlayer fp = freshPlayer(level, "QFEnder");
        var sd = PlayerStoryData.get(fp);
        // bonds rise from real play: taming gives bond 10 to that falcon's type
        BlockPos p = pad(level, 3000, 3160, Blocks.GRASS_BLOCK);
        cleanup(level, p, 8);
        QuantumFalconEntity f = falcon(level, p, true);
        int tries = 0;
        while (!f.isTame() && tries < 60) { interact(fp, f, Items.SALMON, 1); tries++; }
        String type = PlayerStoryData.bondType(f.getVariant());
        int bondAfterTame = sd.getFalconBond(type);
        f.addBondLevel(100, fp);
        int bondMax = sd.getFalconBond(type);
        d.append("type=").append(type).append(" bond tame=").append(bondAfterTame).append(" max=").append(bondMax);
        boolean ok = bondAfterTame >= 10 && bondMax == 100;
        // ending refused before the Sovereign falls
        com.matt.quantumfalcon.story.StoryManager.executeEnding(fp, com.matt.quantumfalcon.story.StoryManager.Ending.LIGHT);
        boolean refused = !sd.hasChosenEnding();
        // the Sovereign's death (server path) marks readiness and Chapter 9
        com.matt.quantumfalcon.story.BossDeathHandler.onBossDefeated(fp, level, com.matt.quantumfalcon.story.BossDeathHandler.BossType.SINGULARITY_SOVEREIGN, p);
        boolean ready = com.matt.quantumfalcon.story.StoryManager.canMakeEndingChoice(fp);
        boolean ch9 = sd.hasSeenChapter(StoryChapter.CHAPTER_9);
        // TRUE refused without all four bonds, then LIGHT accepted
        com.matt.quantumfalcon.story.StoryManager.executeEnding(fp, com.matt.quantumfalcon.story.StoryManager.Ending.TRUE);
        boolean trueRefused = !sd.hasChosenEnding();
        new com.matt.quantumfalcon.network.EndingChoicePayload("LIGHT");   // codec class loads on the server
        com.matt.quantumfalcon.story.StoryManager.executeEnding(fp, com.matt.quantumfalcon.story.StoryManager.Ending.LIGHT);
        String chosen = sd.getChosenEnding();
        // persisted: attachment round trip (what a save/restart does) and it is marked copy-on-death
        var tag = sd.writeTag();
        net.minecraft.nbt.CompoundTag again = tag.copy();
        boolean persisted = "LIGHT".equals(again.getString("ChosenEnding")) && again.getBoolean("SovereignDefeated")
            && again.getCompound("FalconBonds").getInt(type) == 100;
        boolean copyOnDeath;
        try { var fld = net.neoforged.neoforge.attachment.AttachmentType.class.getDeclaredField("copyOnDeath"); fld.setAccessible(true); copyOnDeath = fld.getBoolean(PlayerStoryData.STORY.get()); }
        catch (ReflectiveOperationException ex) { copyOnDeath = false; }
        // a second choice is refused
        com.matt.quantumfalcon.story.StoryManager.executeEnding(fp, com.matt.quantumfalcon.story.StoryManager.Ending.DARK);
        boolean sealed = "LIGHT".equals(sd.getChosenEnding());
        d.append(" refusedEarly=").append(refused).append(" readyAfterSovereign=").append(ready).append(" chapter9=").append(ch9)
         .append(" trueRefused=").append(trueRefused).append(" chosen=").append(chosen).append(" persisted=").append(persisted)
         .append(" copyOnDeath=").append(copyOnDeath).append(" sealed=").append(sealed);
        ok &= refused && ready && ch9 && trueRefused && "LIGHT".equals(chosen) && persisted && copyOnDeath && sealed;
        cleanup(level, p, 8);
        result("audit_st1_ending_persisted", ok, d.toString());
    }

    // ------------------------------------------------------------------
    private static void lo1(ServerLevel level) {
        var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.matt.quantumfalcon.QuantumFalcon.MOD_ID, "heart_of_quantum");
        boolean item = net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id);
        boolean recipe = level.getServer().getRecipeManager().byKey(id).isPresent();
        boolean adv = level.getServer().getAdvancements().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.matt.quantumfalcon.QuantumFalcon.MOD_ID, "story/obtain_heart_of_quantum")) != null;
        boolean artifacts = net.neoforged.fml.ModList.get().isLoaded("artifacts"), create = net.neoforged.fml.ModList.get().isLoaded("create");
        java.util.List<String> dups = com.matt.quantumfalcon.integration.ModIntegrations.duplicateIds();
        result("audit_lo1_native_heart", item && recipe && adv && dups.isEmpty(),
            "item=" + item + " recipe=" + recipe + " advancement=" + adv + " (artifacts loaded=" + artifacts + ", create loaded=" + create + ") integration ids clashing with native=" + dups);
    }

    // ------------------------------------------------------------------
    private static void dt1(ServerLevel level) {
        FakePlayer fp = freshPlayer(level, "QFScout");
        com.matt.quantumfalcon.world.DangerTiers.update(fp, null);
        var plain = com.matt.quantumfalcon.world.DangerTiers.last(fp.getUUID());
        com.matt.quantumfalcon.world.DangerTiers.update(fp, "dungeon_overworld_riftslate");
        var dun = com.matt.quantumfalcon.world.DangerTiers.last(fp.getUUID());
        com.matt.quantumfalcon.world.DangerTiers.update(fp, "singularity_sovereign_arena");
        var sov = com.matt.quantumfalcon.world.DangerTiers.last(fp.getUUID());
        com.matt.quantumfalcon.world.DangerTiers.update(fp, "void_citadel");
        var cit = com.matt.quantumfalcon.world.DangerTiers.last(fp.getUUID());
        // Review: also inside a real generated structure. /place a riftslate dungeon, stand the player inside one of its
        // pieces, and resolve the area exactly as PlayerAreaWatcher does (structureAt -> DangerTiers.update).
        // Review: also inside a real, naturally generated structure. The flat test overworld has no dungeons (and /place
        // fails the dungeon's terrain check), so use the nearest Overseer arena the Echo Rift generated by itself.
        ServerLevel rl = level.getServer().getLevel(com.matt.quantumfalcon.dimension.EchoRiftDimension.ECHO_RIFT_LEVEL_KEY);
        BlockPos inside = null;
        com.matt.quantumfalcon.world.DangerTiers.Area real = null;
        var found = com.matt.quantumfalcon.item.RiftCompassItem.locateOne(rl, new BlockPos(9400, 64, 9400), "quantum_guardian_arena");
        if (found.isPresent()) {
            BlockPos at = found.get();
            for (int cx = -3; cx <= 3; cx++) for (int cz = -3; cz <= 3; cz++) rl.getChunk((at.getX() >> 4) + cx, (at.getZ() >> 4) + cz);   // generate it fully
            search:
            for (int y = rl.getMaxBuildHeight() - 2; y >= rl.getMinBuildHeight() + 2; y -= 3) for (int dx = -24; dx <= 24; dx += 4) for (int dz = -24; dz <= 24; dz += 4) {
                BlockPos q = new BlockPos(at.getX() + dx, y, at.getZ() + dz);
                if ("quantum_guardian_arena".equals(com.matt.quantumfalcon.world.PlayerAreaWatcher.structureAt(rl, q))) { inside = q; break search; }
            }
        }
        if (inside != null) {
            FakePlayer rp = freshPlayer(rl, "QFDungeonWalker");
            rp.setPos(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
            com.matt.quantumfalcon.world.DangerTiers.update(rp, com.matt.quantumfalcon.world.PlayerAreaWatcher.structureAt(rl, rp.blockPosition()));
            real = com.matt.quantumfalcon.world.DangerTiers.last(rp.getUUID());
        }
        boolean realOk = real != null && real.tier() == 3 && real.name().contains("Overseer");
        result("audit_dt1_structure_banner", plain.tier() == 0 && dun.tier() == 1 && sov.tier() == 7 && cit.tier() == 5 && realOk,
            "overworld=" + plain + " dungeon=" + dun + " sovereign=" + sov + " citadel=" + cit + " | standing inside a naturally generated Overseer arena at "
            + (inside == null ? "none found" : inside.toShortString()) + " -> " + real);
    }

    // ------------------------------------------------------------------
    private static void ev1(ServerLevel level) {
        boolean defaults = com.matt.quantumfalcon.event.EventsConfig.ENABLED.get() && !com.matt.quantumfalcon.event.EventsConfig.GRIEFING.get()
            && com.matt.quantumfalcon.event.EventsConfig.DIMENSIONS.get().equals(java.util.List.of("minecraft:overworld"));
        // a small player base: planks floor, chest, glass, planks roof
        // R3 tests: fresh sites every run, away from every other test's pads (leftover protected blocks made the field check fail)
        int ez = 12000 + DungeonTests.RUN_OFF + 2000;
        BlockPos b = pad(level, 17600, ez, Blocks.GRASS_BLOCK);
        cleanup(level, b, 10);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            level.setBlock(b.offset(dx, -1, dz), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            level.setBlock(b.offset(dx, 3, dz), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            if (Math.abs(dx) == 2 || Math.abs(dz) == 2) for (int y = 0; y < 3; y++) level.setBlock(b.offset(dx, y, dz), (y == 1 ? Blocks.GLASS : Blocks.OAK_PLANKS).defaultBlockState(), 3);
        }
        level.setBlock(b, Blocks.CHEST.defaultBlockState(), 3);
        java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> before = new java.util.HashMap<>();
        for (BlockPos q : BlockPos.betweenClosed(b.offset(-4, -2, -4), b.offset(4, 6, 4))) before.put(q.immutable(), level.getBlockState(q));
        // default config: meteor on the roof, 30 storm strikes around the base
        BlockPos roofTop = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, b);
        new com.matt.quantumfalcon.event.MeteorShowerEvent().createMeteorImpact(level, roofTop);
        boolean allVisual = true, onSurface = true;
        for (int i = 0; i < 30; i++) {
            BlockPos sp = com.matt.quantumfalcon.event.QuantumStormEvent.strikeNear(level, b);
            allVisual &= com.matt.quantumfalcon.event.QuantumStormEvent.lastVisualOnly;
            onSurface &= sp.getY() >= level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sp.getX(), sp.getZ());
        }
        int changed = 0;
        for (var e : before.entrySet()) if (!level.getBlockState(e.getKey()).equals(e.getValue())) changed++;
        // griefing opted in: open ground far from builds may change, the base still may not, and mobGriefing=false wins
        com.matt.quantumfalcon.event.EventsConfig.GRIEFING.set(true);
        BlockPos field = pad(level, 17600, ez + 200, Blocks.GRASS_BLOCK);
        boolean fieldOk = com.matt.quantumfalcon.event.EventSafety.mayAlter(level, field);
        boolean baseProtected = !com.matt.quantumfalcon.event.EventSafety.mayAlter(level, roofTop);
        var rule = level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        rule.set(false, level.getServer());
        boolean ruleRespected = !com.matt.quantumfalcon.event.EventSafety.mayAlter(level, field);
        rule.set(true, level.getServer());
        com.matt.quantumfalcon.event.EventsConfig.GRIEFING.set(false);
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.LightningBolt.class, new net.minecraft.world.phys.AABB(b).inflate(40))) e.discard();
        result("audit_ev1_events_safe", defaults && changed == 0 && allVisual && onSurface && fieldOk && baseProtected && ruleRespected,
            "defaults(enabled, griefing off, overworld only)=" + defaults + " base blocks changed by meteor + 30 strikes=" + changed + " strikes visual-only=" + allVisual
            + " strikes on surface=" + onSurface + " | griefing on: open field may change=" + fieldOk + " base protected=" + baseProtected + " mobGriefing=false respected=" + ruleRespected);
        cleanup(level, b, 10);
        level.setChunkForced(b.getX() >> 4, b.getZ() >> 4, false); level.setChunkForced(field.getX() >> 4, field.getZ() >> 4, false);
    }

    /** arenas only generate in their Echo Rift realm biomes (none on the Overworld) and each boss waits for the previous one's defeat. */
    /** on a fresh world: /locate's own search (findNearestMapStructure) finds every arena in the Echo Rift and none in the
     *  overworld, and a scan of the generated chunks around overworld spawn finds no arena structure starts or references. */
    /** Arena chain: beating a boss while standing in its arena must not stop the next arena from waking (stale presence). */
    private static void arenaChainEntry(ServerLevel level) {
        var AM = com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.class;
        BlockPos a = pad(level, 3600, 3400, Blocks.STONE), b = pad(level, 3900, 3400, Blocks.STONE);
        for (BlockPos q : new BlockPos[]{a, b}) { cleanup(level, q, 40); for (int cx = -5; cx <= 5; cx++) for (int cz = -5; cz <= 5; cz++) level.setChunkForced((q.getX() >> 4) + cx, (q.getZ() >> 4) + cz, true); }
        var G = com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType.QUANTUM_GUARDIAN;
        var W = com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType.VOID_HORROR;
        com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.registerArena(level, a, G);
        com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.registerArena(level, b, W);
        FakePlayer pl = freshPlayer(level, "QFChain");
        pl.moveTo(a.getX() + 0.5, a.getY() + 1, a.getZ() + 0.5);
        com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.testPlayerTick(pl, level);
        boolean trackedA = com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.isPlayerTracked(pl.getUUID());
        // the Overseer falls with the player still inside: the arena is retired
        level.getEntities(com.matt.quantumfalcon.registry.ModEntities.QUANTUM_OVERSEER.get(), new net.minecraft.world.phys.AABB(a).inflate(80), e -> true).forEach(net.minecraft.world.entity.Entity::discard);
        PlayerStoryData.get(pl).markBossDefeated("QUANTUM_OVERSEER");
        com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.unregisterArena(a);
        boolean freed = !com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.isPlayerTracked(pl.getUUID());
        // straight on to the Warden's arena (a teleport, no walk out)
        pl.moveTo(b.getX() + 0.5, b.getY() + 1, b.getZ() + 0.5);
        com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.testPlayerTick(pl, level);
        var woke = level.getEntities(com.matt.quantumfalcon.registry.ModEntities.RIFT_WARDEN.get(), new net.minecraft.world.phys.AABB(b).inflate(80), e -> e.isAlive());
        boolean summoned = !woke.isEmpty();
        woke.forEach(net.minecraft.world.entity.Entity::discard);
        com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.unregisterArena(b);
        com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.testLeave(pl.getUUID());
        for (BlockPos q : new BlockPos[]{a, b}) { cleanup(level, q, 40); for (int cx = -5; cx <= 5; cx++) for (int cz = -5; cz <= 5; cz++) level.setChunkForced((q.getX() >> 4) + cx, (q.getZ() >> 4) + cz, false); }
        result("audit_arena_chain_entry", trackedA && freed && summoned,
            "tracked in Overseer arena=" + trackedA + " presence cleared when the arena retired=" + freed + " Warden woke on teleporting into the next arena=" + summoned);
    }

    /** Sovereign Crystal Shield: it blocks damage while up, a player touching a crystal shatters it, and it lapses on its own. */
    private static void sovereignShieldBreaks(ServerLevel level) {
        BlockPos c = pad(level, 4200, 3400, Blocks.STONE);
        cleanup(level, c, 30);
        for (int cx = -3; cx <= 3; cx++) for (int cz = -3; cz <= 3; cz++) level.setChunkForced((c.getX() >> 4) + cx, (c.getZ() >> 4) + cz, true);
        var boss = com.matt.quantumfalcon.registry.ModEntities.SINGULARITY_SOVEREIGN.get().create(level);
        boss.moveTo(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5); boss.setNoAi(true); level.addFreshEntity(boss);
        FakePlayer pl = freshPlayer(level, "QFShield");
        pl.moveTo(c.getX() + 0.5, c.getY() + 1, c.getZ() + 30.5);
        boss.raiseShieldForTest(); boss.tick();
        int raised = boss.shieldCrystals().size();
        float hp0 = boss.getHealth();
        boolean blocked = !boss.hurt(level.damageSources().playerAttack(pl), 20f) && boss.getHealth() == hp0;
        // touch one crystal: it shatters, the rest stay
        BlockPos first = boss.shieldCrystals().get(0);
        pl.moveTo(first.getX() + 0.5, first.getY(), first.getZ() + 0.5); boss.shatterTouched(level, java.util.List.of(pl));
        int afterTouch = boss.shieldCrystals().size();
        pl.moveTo(c.getX() + 0.5, c.getY() + 1, c.getZ() + 30.5);
        for (int i = 0; i < com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity.SHIELD_MAX_TICKS + 2; i++) boss.tick();
        boolean lapsed = boss.shieldCrystals().isEmpty() && !boss.isShielded();
        ((net.minecraft.world.entity.Entity) boss).invulnerableTime = 0;
        boolean hurtsAgain = boss.hurt(level.damageSources().playerAttack(pl), 20f) && boss.getHealth() < hp0;
        boss.discard();
        cleanup(level, c, 30);
        for (int cx = -3; cx <= 3; cx++) for (int cz = -3; cz <= 3; cz++) level.setChunkForced((c.getX() >> 4) + cx, (c.getZ() >> 4) + cz, false);
        result("audit_sovereign_shield_breaks", raised == 6 && blocked && afterTouch == 5 && lapsed && hurtsAgain,
            "crystals raised=" + raised + " damage blocked while up=" + blocked + " after touching one=" + afterTouch + " lapsed after " + com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity.SHIELD_MAX_TICKS + " ticks=" + lapsed + " hurt again=" + hurtsAgain);
    }

    private static void bo1Fresh(ServerLevel level) {
        var server = level.getServer();
        var structures = server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var rift = server.getLevel(com.matt.quantumfalcon.dimension.EchoRiftDimension.ECHO_RIFT_LEVEL_KEY);
        StringBuilder d = new StringBuilder("worldAgeTicks=").append(level.getGameTime()).append(' ');
        boolean ok = rift != null;
        java.util.Set<net.minecraft.world.level.levelgen.structure.Structure> arenas = new java.util.HashSet<>();
        BlockPos spawn = level.getSharedSpawnPos();
        for (String id : new String[]{"quantum_guardian_arena", "void_horror_arena", "entangled_twins_arena", "singularity_sovereign_arena"}) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", id));
            var holder = structures.getHolder(key).orElse(null);
            if (holder == null) { ok = false; d.append(id).append("=missing; "); continue; }
            arenas.add(holder.value());
            var set = net.minecraft.core.HolderSet.direct(holder);
            var ow = level.getChunkSource().getGenerator().findNearestMapStructure(level, set, spawn, 64, false);
            var rf = rift == null ? null : rift.getChunkSource().getGenerator().findNearestMapStructure(rift, set, BlockPos.ZERO, 64, false);
            boolean startThere = false;
            if (rf != null) {
                var ch = rift.getChunk(rf.getFirst().getX() >> 4, rf.getFirst().getZ() >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
                var start = ch.getStartForStructure(holder.value());
                startThere = start != null && start.isValid();
            }
            ok &= ow == null && rf != null && startThere;
            d.append(id).append(" overworld=").append(ow == null ? "none" : ow.getFirst().toShortString())
             .append(" rift=").append(rf == null ? "none" : rf.getFirst().toShortString()).append(" startGenerated=").append(startThere).append("; ");
        }
        int scanned = 0, found = 0;
        for (int cx = -6; cx <= 6; cx++) for (int cz = -6; cz <= 6; cz++) {
            var ch = level.getChunk((spawn.getX() >> 4) + cx, (spawn.getZ() >> 4) + cz);
            scanned++;
            for (var st : arenas) if (ch.getStartForStructure(st) != null || !ch.getReferencesForStructure(st).isEmpty()) found++;
        }
        ok &= found == 0;
        d.append("overworld chunks scanned=").append(scanned).append(" arena starts/refs=").append(found);
        result("audit_bo1_fresh_locate", ok, d.toString());
    }

    /** every ending-screen text colour clears 4.5:1 on the plate's worst case, and the open-screen payload carries the
     *  relic and bond progress the screen prints as the True path's lock reason. */
    private static void n4(ServerLevel level) {
        StringBuilder d = new StringBuilder();
        double worst = 99;
        for (int c : com.matt.quantumfalcon.story.StoryChoicePalette.TEXT) {
            double r = com.matt.quantumfalcon.story.StoryChoicePalette.contrast(c, com.matt.quantumfalcon.story.StoryChoicePalette.WORST_PLATE_RGB);
            worst = Math.min(worst, r);
            if (r < 4.5) d.append(String.format("#%06X=%.2f ", c, r));
        }
        d.append(String.format("lowest text contrast=%.2f:1 ", worst));
        FakePlayer fp = freshPlayer(level, "QFChooser");
        fp.getInventory().clearContent();
        fp.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.OVERSEERS_EYE.get()));
        fp.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.TWIN_ESSENCE.get()));
        var sd = PlayerStoryData.get(fp);
        sd.raiseFalconBond("void", 100);
        sd.raiseFalconBond("echo", 60);
        var pl = com.matt.quantumfalcon.network.OpenStoryChoiceScreenPacket.OpenStoryChoiceScreenPayload.of(fp);
        d.append("payload locked: trueUnlocked=").append(pl.trueUnlocked()).append(" relics=").append(pl.relics()).append("/4 bonds=").append(pl.bonds()).append("/4");
        boolean lockedOk = !pl.trueUnlocked() && pl.relics() == 2 && pl.bonds() == 1;
        { var g = new net.minecraft.world.item.ItemStack(ModItems.REALITY_GEM.get()); com.matt.quantumfalcon.story.StoryManager.bind(g, fp); fp.getInventory().add(g); }   // a held gem is bound to its crafter
        for (String t : new String[]{"void", "quantum", "echo", "singularity"}) sd.raiseFalconBond(t, 100);
        var open = com.matt.quantumfalcon.network.OpenStoryChoiceScreenPacket.OpenStoryChoiceScreenPayload.of(fp);
        d.append(" | with gem+bonds: trueUnlocked=").append(open.trueUnlocked()).append(" relics=").append(open.relics()).append(" bonds=").append(open.bonds());
        boolean openOk = open.trueUnlocked() && open.relics() == 4 && open.bonds() == 4;
        fp.getInventory().clearContent();
        result("audit_n4_choice_screen", worst >= 4.5 && lockedOk && openOk, d.toString());
    }

    private static void bo1(ServerLevel level) {
        var server = level.getServer();
        var structures = server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var overworldBiomes = level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
        var rift = server.getLevel(com.matt.quantumfalcon.dimension.EchoRiftDimension.ECHO_RIFT_LEVEL_KEY);
        var riftBiomes = rift == null ? java.util.Set.<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>of() : rift.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
        StringBuilder d = new StringBuilder();
        boolean worldgenOk = true;
        for (String id : new String[]{"quantum_guardian_arena", "void_horror_arena", "entangled_twins_arena", "singularity_sovereign_arena"}) {
            var st = structures.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", id));
            if (st == null) { worldgenOk = false; d.append(id).append("=missing "); continue; }
            long inOverworld = st.biomes().stream().filter(overworldBiomes::contains).count();
            long inRift = st.biomes().stream().filter(riftBiomes::contains).count();
            worldgenOk &= inOverworld == 0 && inRift > 0;
            d.append(id).append(" overworldBiomes=").append(inOverworld).append(" riftBiomes=").append(inRift).append("; ");
        }
        boolean dupSetGone = server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET)
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "boss_arenas")) == null;
        // gating: a fresh survival player walking into the Warden's arena finds it sealed; one who beat the Overseer wakes the boss
        BlockPos p = pad(level, 3000, 3400, Blocks.STONE);
        cleanup(level, p, 40);
        for (int cx = -5; cx <= 5; cx++) for (int cz = -5; cz <= 5; cz++) level.setChunkForced((p.getX() >> 4) + cx, (p.getZ() >> 4) + cz, true);
        var type = com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType.VOID_HORROR;
        com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.registerArena(level, p, type);
        FakePlayer rookie = freshPlayer(level, "QFRookie");
        String needs = com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.missingPrerequisite(rookie, type);
        rookie.moveTo(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
        com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.testPlayerTick(rookie, level);
        boolean sealed = level.getEntities(com.matt.quantumfalcon.registry.ModEntities.RIFT_WARDEN.get(), new net.minecraft.world.phys.AABB(p).inflate(80), e -> e.isAlive()).isEmpty();
        FakePlayer veteran = freshPlayer(level, "QFVeteran");
        PlayerStoryData.get(veteran).markBossDefeated("QUANTUM_OVERSEER");
        boolean vetClear = com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.missingPrerequisite(veteran, type) == null;
        boolean overseerOpen = com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.missingPrerequisite(rookie,
                com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType.QUANTUM_GUARDIAN) == null;
        veteran.moveTo(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
        com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.testPlayerTick(veteran, level);
        var woke = level.getEntities(com.matt.quantumfalcon.registry.ModEntities.RIFT_WARDEN.get(), new net.minecraft.world.phys.AABB(p).inflate(80), e -> e.isAlive());
        boolean summoned = !woke.isEmpty();
        // the defeat record survives save/load (attachment NBT)
        var tag = PlayerStoryData.get(veteran).writeTag();
        PlayerStoryData copy = new PlayerStoryData();
        copy.readTag(tag);
        boolean persisted = copy.hasDefeatedBoss("QUANTUM_OVERSEER") && !copy.hasDefeatedBoss("RIFT_WARDEN");
        woke.forEach(net.minecraft.world.entity.Entity::discard);
        com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.unregisterArena(p);
        cleanup(level, p, 40);
        for (int cx = -5; cx <= 5; cx++) for (int cz = -5; cz <= 5; cz++) level.setChunkForced((p.getX() >> 4) + cx, (p.getZ() >> 4) + cz, false);
        d.append("dupStructureSetGone=").append(dupSetGone).append(" | rookie needs=").append(needs).append(" sealed=").append(sealed)
         .append(" overseerOpenDay1=").append(overseerOpen).append(" veteranClear=").append(vetClear).append(" wardenSummonedForVeteran=").append(summoned)
         .append(" defeatPersisted=").append(persisted);
        result("audit_bo1_boss_gating", worldgenOk && dupSetGone && needs != null && sealed && overseerOpen && vetClear && summoned && persisted, d.toString());
    }

    /** SHA-512 of cpapireforged-neo-1.21.1-1.2.2.jar as published on Modrinth (version lowfyLfO). */
    static final String PUBLISHED_CPAPI_SHA512 = "c7ec668bb6b67e3855eaf1175caa0eac77101412af10747b8a118134adf86f43d849b7a1523768b5c540debaad2d97b6009db14e280dd35370d420f289b3f9c0"; // gitleaks:allow (public file hash, not a secret)

    /** the portal library loaded at runtime is the published 1.2.2 release, byte for byte, and our portal is registered with it. */
    private static void rel1(ServerLevel level) {
        var info = net.neoforged.fml.ModList.get().getModContainerById("cpapireforged");
        String version = info.map(c -> c.getModInfo().getVersion().toString()).orElse("absent");
        String hash = "?";
        String file = "?";
        try {
            var path = net.neoforged.fml.ModList.get().getModFileById("cpapireforged").getFile().getFilePath();
            file = path.getFileName().toString();
            var md = java.security.MessageDigest.getInstance("SHA-512");
            hash = java.util.HexFormat.of().formatHex(md.digest(java.nio.file.Files.readAllBytes(path)));
        } catch (Exception e) { hash = "error " + e; }
        boolean published = PUBLISHED_CPAPI_SHA512.equals(hash);
        boolean portalLinked = net.kyrptonaught.customportalapi.CustomPortalApiRegistry.getAllPortalLinks().stream()
                .anyMatch(l -> l.dimID != null && l.dimID.getNamespace().equals("quantumfalcon"));
        result("audit_rel1_published_portal_lib", version.equals("1.2.2") && published && portalLinked,
                "cpapireforged version=" + version + " file=" + file + " sha512 matches Modrinth lowfyLfO=" + published + " quantumfalcon portal registered=" + portalLinked);
    }

    /** Stage 4 arena pass: each natural arena clears trees fully, sheds snow, stands on a plinth, has a readable floor and symmetric lit pylons. */
    private static void arenaDesign(ServerLevel level) {
        StringBuilder d = new StringBuilder();
        boolean ok = true;
        var types = com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType.values();
        // truly fresh ground on every run: a persistent site counter (the old gameTime % 40 slot reused sites built by earlier runs)
        java.nio.file.Path siteFile = java.nio.file.Paths.get("qf_arena_site.txt");
        int site = 40;
        try { if (java.nio.file.Files.exists(siteFile)) site = Integer.parseInt(java.nio.file.Files.readString(siteFile).trim()); } catch (Exception ignored) {}
        try { java.nio.file.Files.writeString(siteFile, String.valueOf(site + 1)); } catch (Exception ignored) {}
        for (int t = 0; t < types.length; t++) {
            var type = types[t];
            int cx = 5000 + t * 160, cz = 5000 + site * 160;
            int reach = com.matt.quantumfalcon.worldgen.structure.ArenaDesign.reach(type), r = com.matt.quantumfalcon.worldgen.structure.ArenaDesign.floorRadius(type);
            for (int x = (cx - reach) >> 4; x <= (cx + reach) >> 4; x++) for (int z = (cz - reach) >> 4; z <= (cz + reach) >> 4; z++) level.setChunkForced(x, z, true);
            // generate a ring of chunks past the edge first: a neighbour decorated after the build could drop a natural tree into the arena
            for (int x = ((cx - reach) >> 4) - 2; x <= ((cx + reach) >> 4) + 2; x++) for (int z = ((cz - reach) >> 4) - 2; z <= ((cz + reach) >> 4) + 2; z++) level.getChunk(x, z);
            int ground = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, cx, cz) - 1;
            int f = ground + 1;
            // a small forest and a snow blanket where the arena will go, with some trees straddling the edge
            var rnd = new java.util.Random(77 + t);
            for (int k = 0; k < 40; k++) {
                int tx = cx + rnd.nextInt(2 * reach + 1) - reach, tz = cz + rnd.nextInt(2 * reach + 1) - reach;
                int ty = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, tx, tz);
                for (int y = 0; y < 6; y++) level.setBlock(new BlockPos(tx, ty + y, tz), Blocks.OAK_LOG.defaultBlockState(), 2);
                for (int ox = -2; ox <= 2; ox++) for (int oz = -2; oz <= 2; oz++) for (int oy = 4; oy <= 7; oy++) {
                    BlockPos lp = new BlockPos(tx + ox, ty + oy, tz + oz);
                    if (level.getBlockState(lp).isAir()) level.setBlock(lp, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 2);
                }
            }
            BlockPos centre = new BlockPos(cx, f, cz);
            var box = new net.minecraft.world.level.levelgen.structure.BoundingBox(cx - reach, f - 60, cz - reach, cx + reach, f + 60, cz + reach);
            com.matt.quantumfalcon.worldgen.structure.ArenaDesign.build(new com.matt.quantumfalcon.worldgen.structure.ArenaDesign.Sink() {
                @Override public net.minecraft.world.level.block.state.BlockState get(BlockPos p) { return level.getBlockState(p); }
                @Override public void set(BlockPos p, net.minecraft.world.level.block.state.BlockState s) { level.setBlock(p, s, 2); }
            }, centre, type, box);
            int trees = 0, snowable = 0, holes = 0;
            java.util.Set<net.minecraft.world.level.block.Block> floor = new java.util.HashSet<>();
            for (int dx = -reach; dx <= reach; dx++) for (int dz = -reach; dz <= reach; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > reach) continue;
                for (int y = f - 10; y <= f + 40; y++) {
                    var s = level.getBlockState(new BlockPos(cx + dx, y, cz + dz));
                    if (s.is(net.minecraft.tags.BlockTags.LOGS) || s.is(net.minecraft.tags.BlockTags.LEAVES) || s.is(Blocks.SNOW)) trees++;
                }
                if (dist <= r + com.matt.quantumfalcon.worldgen.structure.ArenaDesign.APRON) {
                    int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, cx + dx, cz + dz);
                    BlockPos above = new BlockPos(cx + dx, top, cz + dz);
                    if (Blocks.SNOW.defaultBlockState().canSurvive(level, above)) snowable++;
                }
                if (dist <= r && dist > type.getVoidCenterRadius() + 1.5) {
                    floor.add(level.getBlockState(new BlockPos(cx + dx, f, cz + dz)).getBlock());
                    for (int y = f - 3; y <= f; y++) if (level.getBlockState(new BlockPos(cx + dx, y, cz + dz)).isAir()) holes++;
                }
            }
            int n = type.getPillarCount(), lamps = 0, h = com.matt.quantumfalcon.worldgen.structure.ArenaDesign.pylonHeightOf(type);
            for (int i = 0; i < n; i++) {
                double a = 2 * Math.PI * (i + 0.5) / n;
                BlockPos lamp = new BlockPos(cx + (int) Math.round(Math.cos(a) * (r - 4)), f + h + 1, cz + (int) Math.round(Math.sin(a) * (r - 4)));
                if (level.getBlockState(lamp).getLightEmission(level, lamp) > 0) lamps++;
            }
            boolean pass = trees == 0 && snowable == 0 && holes == 0 && floor.size() >= 4 && lamps == n;
            ok &= pass;
            d.append(type).append(": treeBlocks=").append(trees).append(" snowCanSettle=").append(snowable).append(" floorHoles=").append(holes)
             .append(" floorBlockKinds=").append(floor.size()).append(" litPylons=").append(lamps).append("/").append(n).append("; ");
            for (int x = (cx - reach) >> 4; x <= (cx + reach) >> 4; x++) for (int z = (cz - reach) >> 4; z <= (cz + reach) >> 4; z++) level.setChunkForced(x, z, false);
        }
        result("arena_design_natural", ok, d.toString());
    }
}
