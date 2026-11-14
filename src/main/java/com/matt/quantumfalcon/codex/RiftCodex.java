package com.matt.quantumfalcon.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.*;
import java.util.function.Supplier;

/**
 * A1 Rift Codex: data-driven quests (data/quantumfalcon/rift_codex/*.json, one file per chapter) layered on the existing
 * QuestTracker objective stream. Every objective event QuestTracker sees is forwarded here ({@link #onObjective}), plus
 * A8/A9 events (SIG_LOOT, USE_SKILL). Quests are always on once their prerequisites are done: no accepting, no dupes.
 * Progress and completions live in a copyOnDeath attachment; a reward is granted exactly once, on completion.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class RiftCodex {
    private RiftCodex() {}

    public record Reward(List<ResourceLocation> items, List<Integer> counts, int xp, String mapTag, String mapName) {
        public Reward(List<ResourceLocation> items, List<Integer> counts, int xp) { this(items, counts, xp, "", ""); }
    }
    public record Quest(String id, String chapter, int order, String title, String text, String type, String target, int count,
                        boolean absolute, List<String> requires, Reward reward) {}
    public record Chapter(String id, String title, int order, List<Quest> quests, String intro) {}   // intro = one italic line under the chapter title

    public static final Map<String, Chapter> CHAPTERS = new LinkedHashMap<>();
    public static final Map<String, Quest> QUESTS = new LinkedHashMap<>();
    public static final List<String> LOAD_ERRORS = new ArrayList<>();

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, QuantumFalcon.MOD_ID);
    public static final Supplier<AttachmentType<CompoundTag>> DATA = ATTACHMENTS.register("rift_codex",
        () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC).copyOnDeath().build());
    public static void register(IEventBus bus) { ATTACHMENTS.register(bus); }

    // ------------------------------------------------------------------ loading
    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent e) { e.addListener(new Loader()); }

    static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() { super(new com.google.gson.Gson(), "rift_codex"); }
        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller prof) { load(files); }
    }

    public static synchronized void load(Map<ResourceLocation, JsonElement> files) {
        CHAPTERS.clear(); QUESTS.clear(); LOAD_ERRORS.clear();
        List<Chapter> chs = new ArrayList<>();
        for (var en : files.entrySet()) {
            if (!en.getKey().getNamespace().equals(QuantumFalcon.MOD_ID)) continue;
            try {
                JsonObject o = en.getValue().getAsJsonObject();
                String cid = en.getKey().getPath();
                List<Quest> qs = new ArrayList<>();
                JsonArray arr = GsonHelper.getAsJsonArray(o, "quests");
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject q = arr.get(i).getAsJsonObject();
                    List<String> req = new ArrayList<>();
                    if (q.has("requires")) for (JsonElement r : q.getAsJsonArray("requires")) req.add(r.getAsString());
                    List<ResourceLocation> items = new ArrayList<>(); List<Integer> counts = new ArrayList<>(); int xp = 0; String mapTag = "", mapName = "";
                    if (q.has("reward")) {
                        JsonObject rw = q.getAsJsonObject("reward");
                        xp = GsonHelper.getAsInt(rw, "xp", 0);
                        if (rw.has("map")) { JsonObject m = rw.getAsJsonObject("map"); mapTag = GsonHelper.getAsString(m, "structures"); mapName = GsonHelper.getAsString(m, "name"); }
                        if (rw.has("items")) for (JsonElement it : rw.getAsJsonArray("items")) {
                            JsonObject io = it.getAsJsonObject();
                            items.add(ResourceLocation.parse(GsonHelper.getAsString(io, "id")));
                            counts.add(GsonHelper.getAsInt(io, "count", 1));
                        }
                    }
                    JsonObject task = GsonHelper.getAsJsonObject(q, "task");
                    qs.add(new Quest(GsonHelper.getAsString(q, "id"), cid, i, GsonHelper.getAsString(q, "title"), GsonHelper.getAsString(q, "text", ""),
                        GsonHelper.getAsString(task, "type"), GsonHelper.getAsString(task, "target", "any"), GsonHelper.getAsInt(task, "count", 1),
                        GsonHelper.getAsBoolean(task, "absolute", false), req, new Reward(items, counts, xp, mapTag, mapName)));
                }
                chs.add(new Chapter(cid, GsonHelper.getAsString(o, "title"), GsonHelper.getAsInt(o, "order", 99), qs, GsonHelper.getAsString(o, "intro", "")));
            } catch (Exception ex) {
                LOAD_ERRORS.add(en.getKey() + ": " + ex.getMessage());
                QuantumFalcon.LOGGER.error("[rift_codex] bad chapter {}: {}", en.getKey(), ex.toString());
            }
        }
        chs.sort(Comparator.comparingInt(Chapter::order));
        for (Chapter c : chs) { CHAPTERS.put(c.id(), c); for (Quest q : c.quests()) if (QUESTS.put(q.id(), q) != null) LOAD_ERRORS.add("duplicate quest id " + q.id()); }
        for (Quest q : QUESTS.values()) {
            for (String r : q.requires()) if (!QUESTS.containsKey(r)) LOAD_ERRORS.add(q.id() + " requires unknown " + r);
            for (ResourceLocation it : q.reward().items()) if (!BuiltInRegistries.ITEM.containsKey(it)) LOAD_ERRORS.add(q.id() + " rewards unknown item " + it);
        }
        QuantumFalcon.LOGGER.info("[rift_codex] {} chapters, {} quests, {} problems", CHAPTERS.size(), QUESTS.size(), LOAD_ERRORS.size());
    }

    // ------------------------------------------------------------------ player state
    static CompoundTag tag(Player p) { return p.getData(DATA); }
    public static boolean isDone(Player p, String id) { return listHas(tag(p).getList("done", Tag.TAG_STRING), id); }
    public static int progress(Player p, String id) { return tag(p).getCompound("progress").getInt(id); }
    public static int doneCount(Player p) { return tag(p).getList("done", Tag.TAG_STRING).size(); }
    static boolean listHas(ListTag l, String s) { for (Tag t : l) if (t.getAsString().equals(s)) return true; return false; }

    public static boolean unlocked(Player p, Quest q) {
        for (String r : q.requires()) if (!isDone(p, r)) return false;
        return true;
    }

    static boolean matches(String questTarget, String target) {
        if (questTarget.equals(target)) return true;
        if (questTarget.endsWith("*")) return target.startsWith(questTarget.substring(0, questTarget.length() - 1));
        return false;
    }

    /** Forwarded from QuestTracker.apply (and A8/A9). type = ObjectiveType name or SIG_LOOT / USE_SKILL. */
    public static void onObjective(ServerPlayer p, String type, String target, int amount, boolean absolute) {
        if (p == null || QUESTS.isEmpty() || amount <= 0) return;
        // completing one quest can unlock the next one the same event should also count for? No: each event counts once,
        // toward quests that were unlocked before it arrived (no chain skipping).
        List<Quest> open = new ArrayList<>();
        for (Quest q : QUESTS.values()) if (q.type().equals(type) && matches(q.target(), target) && !isDone(p, q.id()) && unlocked(p, q)) open.add(q);
        if (open.isEmpty()) return;
        CompoundTag t = tag(p).copy();
        CompoundTag prog = t.getCompound("progress");
        List<Quest> finished = new ArrayList<>();
        for (Quest q : open) {
            int v = (absolute || q.absolute()) ? Math.max(prog.getInt(q.id()), amount) : prog.getInt(q.id()) + amount;
            v = Math.min(v, q.count());
            prog.putInt(q.id(), v);
            if (v >= q.count()) finished.add(q);
        }
        t.put("progress", prog);
        ListTag done = t.getList("done", Tag.TAG_STRING);
        for (Quest q : finished) done.add(StringTag.valueOf(q.id()));
        t.put("done", done);
        p.setData(DATA, t);
        for (Quest q : finished) grant(p, q);
        if (!finished.isEmpty()) catchUp(p);
    }

    /** Totals that only arrive when they change (skill levels, unique variants, quantum biomes) are re-applied whenever a
     *  quest unlocks or the player logs in, so a quest unlocked late still sees progress made earlier. */
    public static void catchUp(ServerPlayer p) {
        for (var s : com.matt.quantumfalcon.skills.UseSkill.values()) {
            int lv = com.matt.quantumfalcon.skills.UseSkills.level(p, s);
            if (lv > 1) onObjectiveQuiet(p, "USE_SKILL", s.key(), lv);
        }
        var qd = com.matt.quantumfalcon.quest.PlayerQuestData.get(p);
        if (qd != null) {
            if (qd.getTamedVariantCount() > 0) onObjectiveQuiet(p, "TAME_FALCON", "unique_variants", qd.getTamedVariantCount());
            if (qd.getVisitedBiomeCount() > 0) onObjectiveQuiet(p, "VISIT_BIOME", "quantum_biomes", qd.getVisitedBiomeCount());
        }
        collectCheck(p);
    }
    private static int depth = 0;
    private static void onObjectiveQuiet(ServerPlayer p, String type, String target, int v) {
        if (depth > 8) return;
        depth++;
        try { onObjective(p, type, target, v, true); } finally { depth--; }
    }

    @SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer sp) catchUp(sp); }

    public static void grant(ServerPlayer p, Quest q) {
        StringBuilder got = new StringBuilder();
        for (int i = 0; i < q.reward().items().size(); i++) {
            Item it = BuiltInRegistries.ITEM.get(q.reward().items().get(i));
            ItemStack st = new ItemStack(it, q.reward().counts().get(i));
            if (got.length() > 0) got.append(", ");
            got.append(st.getCount()).append("x ").append(st.getHoverName().getString());
            if (!p.getInventory().add(st)) p.drop(st, false);
        }
        if (q.reward().xp() > 0) p.giveExperiencePoints(q.reward().xp());
        if (!q.reward().mapTag().isEmpty()) {   // a Codex pointer: an explorer map to the nearest such site in the Overworld
            net.minecraft.server.level.ServerLevel ow = p.server.overworld();
            net.minecraft.core.BlockPos from = p.level() == ow ? p.blockPosition() : ow.getSharedSpawnPos();
            ItemStack map = com.matt.quantumfalcon.world.ExplorerMaps.create(ow, com.matt.quantumfalcon.world.ExplorerMaps.tag(q.reward().mapTag()), from, q.reward().mapName(), false);
            if (map != null) {
                if (got.length() > 0) got.append(", ");
                got.append(map.getHoverName().getString());
                if (!p.getInventory().add(map)) p.drop(map, false);
            }
        }
        Chapter c = CHAPTERS.get(q.chapter());
        p.sendSystemMessage(Component.literal("Rift Codex: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(q.title()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" complete" + (c != null ? " (" + c.title() + ")" : "") + (got.length() > 0 ? ". Reward: " + got : "")).withStyle(ChatFormatting.GRAY)));
    }

    /** COLLECT_ITEM quests count what you hold (like QuestTracker): once a second, progress = max(progress, held). */
    @SubscribeEvent
    public static void onTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer sp && sp.tickCount % 20 == 11) collectCheck(sp);
    }

    public static void collectCheck(ServerPlayer sp) {
        Set<String> seen = new HashSet<>();
        for (Quest q : QUESTS.values()) {
            if (!q.type().equals("COLLECT_ITEM") || !seen.add(q.target()) || isDone(sp, q.id()) || !unlocked(sp, q)) continue;
            Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(q.target()));
            // what another player handed you (dropped for you to pick up) doesn't count toward your collection
            int held = sp.getInventory().countItem(it) - tag(sp).getCompound("gifted").getInt(q.target());
            if (held > 0) onObjective(sp, "COLLECT_ITEM", q.target(), held, true);
        }
    }

    /** picking up a stack another player threw is a gift: remember how many of that item, so they're not "collected". */
    @SubscribeEvent
    public static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Post e) {
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        var thrower = e.getItemEntity().getOwner();
        if (!(thrower instanceof Player other) || other.getUUID().equals(sp.getUUID())) return;
        int got = e.getOriginalStack().getCount() - e.getCurrentStack().getCount();
        if (got <= 0) return;
        String id = BuiltInRegistries.ITEM.getKey(e.getOriginalStack().getItem()).toString();
        CompoundTag t = tag(sp).copy(); CompoundTag g = t.getCompound("gifted");
        g.putInt(id, g.getInt(id) + got); t.put("gifted", g); sp.setData(DATA, t);
    }

    /** Test hook: record a gift as if picked up from another player's throw. */
    public static void testGift(ServerPlayer sp, String itemId, int n) {
        CompoundTag t = tag(sp).copy(); CompoundTag g = t.getCompound("gifted");
        g.putInt(itemId, g.getInt(itemId) + n); t.put("gifted", g); sp.setData(DATA, t);
    }

    /** the Held Hour tab (pieces, next boss, the True Path's falcon kinds, the ending), as "hh" lines. */
    public static List<String> heldHourLines(ServerPlayer p) {
        List<String> out = new ArrayList<>();
        var sd = com.matt.quantumfalcon.story.PlayerStoryData.get(p);
        String[][] bosses = {{"QUANTUM_OVERSEER", "the Quantum Overseer", "overseers_eye"}, {"RIFT_WARDEN", "the Rift Warden", "wardens_fragment"},
            {"ENTANGLED_TWINS", "the Entangled Twins", "twin_essence"}, {"SINGULARITY_SOVEREIGN", "the Singularity Sovereign", "sovereigns_crown"}};
        int pieces = 0; String next = ""; StringBuilder got = new StringBuilder();
        for (String[] b : bosses) {
            boolean d = sd.hasDefeatedBoss(b[0]);
            if (d) pieces++; else if (next.isEmpty()) next = b[1];
            got.append(d ? b[2] : "-").append(',');
        }
        pieces = Math.max(pieces, com.matt.quantumfalcon.story.StoryManager.relicCount(p));
        out.add("hh\tpieces\t" + pieces + "\t" + got);
        out.add("hh\tnext\t" + next);
        int[] bonds = {sd.getVoidFalconBond(), sd.getEchoFalconBond(), sd.getSingularityFalconBond(), sd.getQuantumFalconBond()};
        StringBuilder k = new StringBuilder("hh\tkinds");
        for (int b : bonds) k.append('\t').append(b >= 100 ? 1 : 0);
        out.add(k.toString());
        out.add("hh\tending\t" + (sd.getChosenEnding() == null ? "" : sd.getChosenEnding()));
        return out;
    }

    /** Lines for the Falconer's Codex "Rift Codex" tab (sent inside CodexPayload.found; the client has no quest data). */
    public static List<String> codexLines(ServerPlayer p) {
        List<String> out = new ArrayList<>();
        for (Chapter c : CHAPTERS.values()) {
            int done = 0; for (Quest q : c.quests()) if (isDone(p, q.id())) done++;
            out.add("rqc\t" + c.title() + "\t" + done + "\t" + c.quests().size());
            if (!c.intro().isEmpty()) out.add("rqi\t" + c.intro());
            for (Quest q : c.quests()) {
                String state = isDone(p, q.id()) ? "D" : unlocked(p, q) ? "O" : "L";
                out.add("rq\t" + state + "\t" + q.title() + "\t" + q.text() + "\t" + progress(p, q.id()) + "\t" + q.count());
            }
        }
        out.addAll(heldHourLines(p));
        return out;
    }

    /** Test/admin: mark a quest done without its reward. */
    public static void markDone(Player p, String id) {
        CompoundTag t = tag(p).copy(); ListTag done = t.getList("done", Tag.TAG_STRING);
        if (!listHas(done, id)) done.add(StringTag.valueOf(id));
        t.put("done", done); p.setData(DATA, t);
    }

    /** Test/admin: forget a player's Codex state. */
    public static void reset(Player p) { p.setData(DATA, new CompoundTag()); }
}
