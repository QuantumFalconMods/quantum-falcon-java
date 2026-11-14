package com.matt.quantumfalcon.client.gui;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.breeding.BreedingData;
import com.matt.quantumfalcon.breeding.NestQuality;
import com.matt.quantumfalcon.client.ModKeyBindings;
import com.matt.quantumfalcon.registry.ModBlocks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Falconer's Codex. A guide book drawn in the mod's dark-void/neon style: chapter tabs on the left, a paged
 * text panel on the right, item icon rows for blocks and nests. Content is built from the same data the game uses
 * (BreedingData, NestQuality, the realm block registry, the live key bindings), so it cannot drift from the rules.
 * Undiscovered hybrids and secret abilities show as ??? until the player has hatched one.
 */
public class CodexScreen extends Screen {
    private static final int W = 360, H = 210, TAB_W = 100, PAD = 10;
    private static final int NEON = 0xFF3FF2FF;
    private static final ResourceLocation SHEET = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/gui/codex.png");

    /** One row: either wrapped text, or a row of item icons. */
    private record Row(Component text, List<ItemStack> icons) {
        static Row t(Component c) { return new Row(c, List.of()); }
        static Row i(List<ItemStack> s) { return new Row(Component.empty(), s); }
    }
    private record Chapter(String key, List<Row> rows) {}

    private final Set<String> found;
    private final List<Chapter> chapters = new ArrayList<>();
    private List<List<FormattedRow>> pages = List.of();
    private int chapter = 0, page = 0, left, top;
    private int tick;

    private record FormattedRow(FormattedCharSequence line, List<ItemStack> icons, int height) {}

    private final List<String> sOrder = new ArrayList<>();
    public static void open(List<String> found, int chapter, int page) {
        CodexScreen s = new CodexScreen(new HashSet<>(found));
        s.sOrder.addAll(found);
        s.chapter = Math.max(0, chapter); s.page = Math.max(0, page);
        Minecraft.getInstance().setScreen(s);
    }

    public CodexScreen(Set<String> found) {
        super(Component.translatable("codex.quantumfalcon.title"));
        this.found = found;
    }

    // ---------------------------------------------------------------- content
    private static Component h(String s) { return Component.literal(s).withColor(0x3FF2FF); }
    private static Component p(String s) { return Component.literal(s).withColor(0xC8D2E6); }
    private static Component c(String s, int rgb) { return Component.literal(s).withColor(rgb); }
    private static String title(String id) {
        StringBuilder b = new StringBuilder();
        for (String w : id.split("_")) if (!w.isEmpty()) b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return b.toString().trim();
    }
    private static ItemStack item(String path) {
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, path));
        return new ItemStack(it == Items.AIR ? Items.BARRIER : it);
    }
    private static String key(KeyMapping k) { return k.getTranslatedKeyMessage().getString(); }

    private void build() {
        chapters.clear();
        // The Held Hour, first tab (lines come from the server, "hh")
        List<Row> r = new ArrayList<>();
        String[] pieces = null, kinds = null; String next = null, ending = null;
        for (String f : found) {
            if (!f.startsWith("hh\t")) continue;
            String[] a = f.split("\t", -1);
            switch (a[1]) {
                case "pieces" -> pieces = a;
                case "next" -> next = a.length > 2 ? a[2] : "";
                case "kinds" -> kinds = a;
                case "ending" -> ending = a.length > 2 ? a[2] : "";
                default -> { }
            }
        }
        r.add(Row.t(h("The Held Hour")));
        if (pieces == null) r.add(Row.t(p("Open the Codex in a world to see how much of the hour is back.")));
        else {
            int n = Integer.parseInt(pieces[2]);
            r.add(Row.t(c("Pieces " + n + " / 4", 0xFFD27A)));
            List<ItemStack> icons = new ArrayList<>();
            String[] got = pieces.length > 3 ? pieces[3].split(",") : new String[0];
            String[] all = {"overseers_eye", "wardens_fragment", "twin_essence", "sovereigns_crown"};
            for (int i = 0; i < 4; i++) icons.add(i < got.length && !got[i].equals("-") ? item(all[i]) : new ItemStack(net.minecraft.world.item.Items.LIGHT_GRAY_STAINED_GLASS_PANE));
            r.add(Row.i(icons));
            r.add(Row.t(p(next == null || next.isEmpty() ? "Every piece is back. Morning is yours to decide." : "Next: " + next + ". Your Echo Compass points the way.")));
            if (kinds != null && kinds.length >= 6) {
                String[] names = {"Void", "Echo", "Singularity", "Quantum"};
                int k = 0; StringBuilder sb = new StringBuilder("  ");
                for (int i = 0; i < 4; i++) { boolean ok = kinds[2 + i].equals("1"); if (ok) k++; sb.append(ok ? "\u2714 " : "\u2717 ").append(names[i]).append("   "); }
                r.add(Row.t(h("The third way: a max-bond falcon of every kind  " + k + " / 4")));
                r.add(Row.t(p(sb.toString().trim())));
            }
            String m = switch (ending == null ? "" : ending) {
                case "LIGHT" -> "Morning: you let it come.";
                case "DARK" -> "Morning: you kept the hour.";
                case "TRUE" -> "Morning: you braided the hours.";
                default -> "Morning: not yet decided.";
            };
            r.add(Row.t(c(m, 0xB8C0FF)));
        }
        chapters.add(new Chapter("held", r));

        // Controls
        r = new ArrayList<>();
        // the goal, first thing in the book
        r.add(Row.t(h("The Held Hour")));
        r.add(Row.t(p("Something is holding back the dawn. The Rift you've seen leaking into the world is one night, held far too long, and its doors are the only places it can be reached.")));
        r.add(Row.t(p("Your falcon can see the stitches that night hangs from. No one else can. Together, take back the four pieces of the held hour, then decide what morning becomes.")));
        r.add(Row.t(p("Start here: earn a falcon's trust. The Quests tab shows your next step.")));
        r.add(Row.t(h("Welcome, falconer")));
        r.add(Row.t(p("This Codex fills in as you hatch new falcons. Tabs on the left pick a chapter; the arrows or your scroll wheel turn pages.")));
        r.add(Row.t(h("Keys (Options > Controls > Quantum Falcon)")));
        r.add(Row.t(p(key(ModKeyBindings.KEY_FALCON_COMMAND) + "  falcon command wheel (follow, stay, wander, attack)")));
        r.add(Row.t(p(key(ModKeyBindings.KEY_FALCON_CALL) + "  call your falcon (needs a Falcon Whistle)")));
        r.add(Row.t(p(key(ModKeyBindings.KEY_SKILL_TREE) + "  skill tree")));
        r.add(Row.t(p(key(ModKeyBindings.KEY_QUEST_LOG) + "  quest log")));
        r.add(Row.t(h("Commands")));
        r.add(Row.t(p("/qf falcon info, /qf quest, /qf skill: player commands. Admin and debug commands need operator rights.")));
        chapters.add(new Chapter("controls", r));

        // Falcons
        r = new ArrayList<>();
        for (int t = 1; t <= 7; t++) {
            var td = BreedingData.TIERS.get(t - 1);
            r.add(Row.t(c("Tier " + t + "  " + td.name(), td.colour())));
            for (var v : BreedingData.VARIANTS) {
                if (v.tier() != t) continue;
                boolean bred = v.parentA() != null;
                boolean known = !bred || found.contains("variant:" + v.id()) || found.contains("hybrid:" + v.id());
                if (!known) { r.add(Row.t(c("???  (a hybrid you have not hatched yet)", 0x6A7290))); continue; }
                String how = bred ? title(v.parentA()) + " x " + title(v.parentB()) + ". " + v.habitat() : v.habitat();
                r.add(Row.t(Component.literal(title(v.id())).withColor(0xFFFFFF).append(p("  " + how))));
            }
        }
        chapters.add(new Chapter("falcons", r));

        // Breeding
        r = new ArrayList<>();
        r.add(Row.t(h("How breeding works")));
        r.add(Row.t(p("Feed two tame, adult falcons their favourite food near each other. The egg inherits a mix of both parents' stats, colours, pattern and abilities, with a chance of mutations and of going up a tier.")));
        r.add(Row.t(h("Tiers")));
        for (int i = 0; i < BreedingData.TIERS.size(); i++) {
            var td = BreedingData.TIERS.get(i);
            String up = i < BreedingData.TIER_UP.length ? "  tier-up " + Math.round(BreedingData.TIER_UP[i] * 100) + "%" : "";
            r.add(Row.t(c(td.name(), td.colour()).copy().append(p("  stat cap " + td.statCap() + ", " + td.slots() + " ability slot" + (td.slots() > 1 ? "s" : "") + ", hatch " + td.hatchMinutes() + " min" + up))));
        }
        r.add(Row.t(h("Catalysts")));
        for (var cd : BreedingData.CATALYSTS) r.add(Row.t(Component.literal(cd.name()).withColor(0xFFFFFF).append(p("  " + cd.effect()))));
        r.add(Row.t(h("Bloodlines")));
        for (var b : BreedingData.BLOODLINES) r.add(Row.t(Component.literal(b.name()).withColor(0xFFFFFF).append(p("  " + b.rule() + ": " + b.bonus()))));
        chapters.add(new Chapter("breeding", r));

        // Abilities
        r = new ArrayList<>();
        String cat = "";
        for (var a : BreedingData.ABILITIES) {
            if (!a.category().equals(cat)) { cat = a.category(); r.add(Row.t(h(title(cat.toLowerCase(Locale.ROOT))))); }
            boolean secret = a.minTier() >= 6 && !found.contains("ability:" + a.id());
            if (secret) r.add(Row.t(c("???  (tier " + a.minTier() + "+)", 0x6A7290)));
            else r.add(Row.t(Component.literal(a.name()).withColor(0xFFFFFF).append(p("  T" + a.minTier() + "+, " + (a.cooldownSeconds() > 0 ? a.cooldownSeconds() + "s. " : "passive. ") + a.effect()))));
        }
        chapters.add(new Chapter("abilities", r));

        // Eggs and nests
        r = new ArrayList<>();
        r.add(Row.t(h("Incubation")));
        for (var td : BreedingData.TIERS) r.add(Row.t(c(td.name(), td.colour()).copy().append(p("  needs " + td.incubation().replace('_', ' ')))));
        r.add(Row.t(p("Look at an egg to see what it is missing. Falcons resting in a nest brood its egg (+20% speed).")));
        r.add(Row.t(h("Nests")));
        r.add(Row.i(List.of(new ItemStack(ModBlocks.TWIG_NEST.get()), new ItemStack(ModBlocks.CRYSTAL_NEST.get()),
                new ItemStack(ModBlocks.VOIDFORGED_NEST.get()), new ItemStack(ModBlocks.QUANTUM_AERIE.get()))));
        for (NestQuality q : NestQuality.values()) {
            List<String> counts = new ArrayList<>(List.of("warmth"));
            if (q.heat) counts.add("heat"); if (q.riftEnergy) counts.add("rift energy"); if (q.quantum) counts.add("quantum");
            r.add(Row.t(Component.literal(q.displayName).withColor(0xFFFFFF).append(p("  hatch x" + q.speed + ", +" + Math.round(q.mutation * 100)
                    + "% rare mutation, counts as " + String.join(", ", counts)))));
        }
        r.add(Row.t(p("Bred eggs are laid into the best free nest within 8 blocks. Wild nests appear in every dimension; their tier follows the danger of the area.")));
        chapters.add(new Chapter("nests", r));

        // the route to the Rift, instability, bosses and compasses, in game (it used to need a wiki)
        r = new ArrayList<>();
        r.add(Row.t(h("Getting there")));
        r.add(Row.i(List.of(item("quantum_compass"), item("quantum_shard"), item("rift_shard"), item("quantum_core"), item("rift_frame"), item("rift_igniter"))));
        r.add(Row.t(p("1. Rift Shards come from Overworld riftslate dungeons and Echo Rift Gate ruins. A Quantum Compass (compass + quantum shards) points at the nearest gate or shrine.")));
        r.add(Row.t(p("2. Quantum Core: 4 quantum shards + 4 diamonds + an ender pearl. Rift Frames: 4 obsidian + 4 rift shards + 1 core make 8.")));
        r.add(Row.t(p("3. Build a ring of 22 Rift Frames, 6 wide and 7 tall (a 4x5 opening), and light it with a Rift Igniter (2 rift shards, a core and a blaze rod).")));
        r.add(Row.t(h("Instability")));
        r.add(Row.t(p("Near the portal you arrived through you are safe. The farther you roam, the faster the meter (top left) climbs; it shows how long you have before it starts to hurt. Nothing happens below 50%.")));
        r.add(Row.t(p("It drains when you head back toward your portal, beat a boss, stand near a powered Rift Stabilizer, or use a Quantum Stabilizer (clears it and holds it for 5 minutes). Respawning clears it too.")));
        r.add(Row.i(List.of(item("quantum_stabilizer"), item("rift_stabilizer"))));
        r.add(Row.t(h("Bosses")));
        r.add(Row.i(List.of(item("echo_compass"))));
        r.add(Row.t(p("Order: Quantum Overseer, Rift Warden, Entangled Twins, Singularity Sovereign. The Echo Compass (compass + rift and echo shards) always points at the next boss you haven't beaten.")));
        chapters.add(new Chapter("rift", r));

        // A8: boss and miniboss signature loot, with the odds
        r = new ArrayList<>();
        r.add(Row.t(h("Signature loot")));
        r.add(Row.t(p("Every boss and miniboss has its own named items. Each player who fought rolls for themselves. Items lean toward the skill branch you have spent the most points in, but any item can drop. A kill without a drop raises your next chance by "
                + Math.round(com.matt.quantumfalcon.loot.sig.SignatureLoot.PITY_STEP * 100) + "%; " + com.matt.quantumfalcon.loot.sig.SignatureLoot.EPIC_PITY
                + " drops in a row below Epic guarantee an Epic or better. A boss always drops on your first kill.")));
        Component tiers = Component.empty();
        for (var t : com.matt.quantumfalcon.loot.sig.LootTier.values()) tiers = tiers.copy().append(c(t.label + "  ", t.rgb));
        r.add(Row.t(tiers));
        for (var src : com.matt.quantumfalcon.loot.sig.SignatureLoot.SOURCES) {
            int[] w = src.boss() ? com.matt.quantumfalcon.loot.sig.SignatureLoot.BOSS_TIERS : com.matt.quantumfalcon.loot.sig.SignatureLoot.MINI_TIERS;
            int tot = 0; for (int x : w) tot += x;
            r.add(Row.t(h(src.name() + (src.boss() ? "" : " (miniboss)"))));
            StringBuilder odds = new StringBuilder("Drop chance " + Math.round(src.chance() * 100) + "%. If it drops: ");
            var lt = com.matt.quantumfalcon.loot.sig.LootTier.values();
            for (int i = 0; i < w.length; i++) odds.append(lt[i].label).append(' ').append(Math.round(w[i] * 1000f / tot) / 10f).append("%").append(i < w.length - 1 ? ", " : ".");
            r.add(Row.t(p(odds.toString())));
            List<ItemStack> icons = new ArrayList<>();
            for (var it : com.matt.quantumfalcon.loot.sig.SignatureLoot.items(src)) icons.add(com.matt.quantumfalcon.loot.sig.SignatureLoot.stack(it, null));
            r.add(Row.i(icons));
            var leg = com.matt.quantumfalcon.loot.sig.SignatureLoot.items(src).get(com.matt.quantumfalcon.loot.sig.SignatureLoot.items(src).size() - 1);
            r.add(Row.t(c(leg.name(), com.matt.quantumfalcon.loot.sig.LootTier.LEGENDARY.rgb).copy().append(p("  (" + leg.arch().label + ") " + leg.flavour()))));
        }
        chapters.add(new Chapter("loot", r));

        // Realm blocks
        r = new ArrayList<>();
        for (var f : com.matt.quantumfalcon.registry.ModRealmBlocks.FAMILIES) {
            r.add(Row.t(c(f.name() + "  (tier " + f.tier() + ")", f.neon())));
            r.add(Row.t(p(f.where() + ". Craft 8 stone around a " + title(f.catalyst()) + ".")));
            List<ItemStack> icons = new ArrayList<>();
            for (String b : f.blocks()) icons.add(item(b));
            r.add(Row.i(icons));
        }
        chapters.add(new Chapter("blocks", r));

        // A1 Rift Codex quests (lines come from the server inside CodexPayload)
        r = new ArrayList<>();
        r.add(Row.t(h("Rift Codex")));
        r.add(Row.t(p("Quests are always on: do the thing and the Codex notices. Finishing one opens the next. Rewards arrive in your pack.")));
        List<String> lines = new ArrayList<>();
        for (String f : found) if (f.startsWith("rq")) lines.add(f);
        lines.sort(Comparator.comparingInt(sOrder::indexOf));
        for (String f : lines) {
            String[] a = f.split("\t", -1);
            if (a[0].equals("rqc") && a.length >= 4) r.add(Row.t(h(a[1] + "  " + a[2] + "/" + a[3])));
            else if (a[0].equals("rqi") && a.length >= 2) r.add(Row.t(Component.literal(a[1]).withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC)));
            else if (a[0].equals("rq") && a.length >= 6) {
                boolean done = a[1].equals("D"), locked = a[1].equals("L");
                String mark = done ? "\u2714 " : locked ? "\u2022 " : "\u25B6 ";
                int col = done ? 0x5CFF8A : locked ? 0x6A7088 : 0xFFFFFF;
                String prog = done || a[5].equals("1") ? "" : "  " + a[4] + "/" + a[5];
                r.add(Row.t(c(mark + a[2] + prog, col)));
                if (!locked && !done && !a[3].isEmpty()) r.add(Row.t(p("   " + a[3])));
            }
        }
        if (lines.isEmpty()) r.add(Row.t(p("Open the Codex in a world to see your quests.")));
        chapters.add(new Chapter("quests", r));
    }

    /** Wraps a chapter into pages that fit the text panel. */
    private void paginate() {
        int w = W - TAB_W - PAD * 2, maxH = H - PAD * 2 - 16;
        List<List<FormattedRow>> out = new ArrayList<>();
        List<FormattedRow> cur = new ArrayList<>(); int used = 0;
        for (Row row : chapters.get(chapter).rows()) {
            List<FormattedRow> parts = new ArrayList<>();
            if (!row.icons().isEmpty()) {
                int per = w / 18;
                for (int i = 0; i < row.icons().size(); i += per) parts.add(new FormattedRow(null, row.icons().subList(i, Math.min(row.icons().size(), i + per)), 18));
            } else for (FormattedCharSequence l : font.split(row.text(), w)) parts.add(new FormattedRow(l, List.of(), 10));
            boolean heading = parts.size() == 1 && row.icons().isEmpty() && row.text().getStyle().getColor() != null;
            int need = parts.stream().mapToInt(FormattedRow::height).sum() + (heading ? 22 : 0);   // keep a heading with what follows
            if (used + Math.min(need, maxH) > maxH && !cur.isEmpty()) { out.add(cur); cur = new ArrayList<>(); used = 0; }
            for (FormattedRow fr : parts) {
                if (used + fr.height() > maxH) { out.add(cur); cur = new ArrayList<>(); used = 0; }
                cur.add(fr); used += fr.height();
            }
            used += 2;
        }
        if (!cur.isEmpty()) out.add(cur);
        pages = out;
        page = Mth.clamp(page, 0, Math.max(0, pages.size() - 1));
    }

    // ---------------------------------------------------------------- screen
    @Override
    protected void init() {
        left = (width - W) / 2; top = (height - H) / 2;
        build();
        for (int i = 0; i < chapters.size(); i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(Component.translatable("codex.quantumfalcon.chapter." + chapters.get(i).key()), b -> { chapter = idx; page = 0; paginate(); })
                    .bounds(left + 6, top + 20 + i * 18, TAB_W - 12, 16).build());   // 10 tabs fit the panel
        }
        addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(left + TAB_W + PAD, top + H - 22, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(left + W - PAD - 20, top + H - 22, 20, 16).build());
        chapter = Mth.clamp(chapter, 0, chapters.size() - 1);
        paginate();
    }

    private void turn(int d) { page = Mth.clamp(page + d, 0, Math.max(0, pages.size() - 1)); }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) { turn(sy > 0 ? -1 : 1); return true; }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 263 || key == 266) { turn(-1); return true; }   // left, page up
        if (key == 262 || key == 267) { turn(1); return true; }    // right, page down
        return super.keyPressed(key, scan, mods);
    }

    @Override public void tick() { tick++; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int x0 = left + TAB_W + PAD, y = top + PAD + 4;
        for (FormattedRow fr : pages.isEmpty() ? List.<FormattedRow>of() : pages.get(page)) {
            if (fr.line() != null) g.drawString(font, fr.line(), x0, y, 0xFFFFFFFF, false);
            else {
                int x = x0;
                for (ItemStack s : fr.icons()) {
                    g.renderItem(s, x, y);
                    if (mx >= x && mx < x + 16 && my >= y && my < y + 16) g.renderTooltip(font, s, mx, my);
                    x += 18;
                }
            }
            y += fr.height();
        }
        String pg = (page + 1) + " / " + Math.max(1, pages.size());
        g.drawCenteredString(font, pg, left + TAB_W + (W - TAB_W) / 2, top + H - 18, 0xFF8090B0);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        float pulse = 0.55f + 0.45f * Mth.sin((tick + pt) * 0.12f);
        int glow = ((int) (0x60 + 0x9F * pulse) << 24) | (NEON & 0xFFFFFF);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, glow);
        g.blit(SHEET, left, top, 0, 0, W, H, 512, 256);   // textures/gui/codex.png, 512x256 sheet (gui_specs.md section 6)
        g.drawString(font, title, left + 8, top + 8, NEON, false);
    }

    @Override public boolean isPauseScreen() { return false; }
}
