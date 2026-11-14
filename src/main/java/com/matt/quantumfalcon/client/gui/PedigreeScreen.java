package com.matt.quantumfalcon.client.gui;

import com.matt.quantumfalcon.breeding.BreedingData;
import com.matt.quantumfalcon.breeding.FalconGenome;
import com.matt.quantumfalcon.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedigree viewer (gui_specs.md section 9, 1:1 reference mockups/_1x_gui_breeding_pedigree.png): 256x220, split-hue kit frame
 * with the shared pulse; portrait well with the baked variant portrait and tier badge; identity stack; five stat rows (icon,
 * name, value, CAP line with bar and cap marker); parent and grandparent cards with busts; three ability cards marked ACTIVE,
 * DORMANT or LOCKED. Every string sits in its section 2.8 field rect via {@link QfText}. Traits and bloodline are in the
 * portrait tooltip. Opened from the Codex on a falcon or egg, or from the Incubator's pedigree button.
 */
public class PedigreeScreen extends Screen {
    private static final int W = 256, H = 220;
    private static final int BODY = 0xFFC8D2E6, MUTED = 0xFF8090B0, LOCKED = 0xFF6A7290;
    private static final String[] STAT = {"SPEED", "STRENGTH", "HUNT", "LOYALTY", "RIFT"};
    private static final int[] STAT_COL = {0x28ECFF, 0xFF38D6, 0xE8C84B, 0x5AFFAA, 0xAA50FF};
    private static final int[] STAT_ICON = {QfUi.ICON_SPEED, QfUi.ICON_STRENGTH, QfUi.ICON_HUNT, QfUi.ICON_LOYALTY, QfUi.ICON_RIFT};
    private final FalconGenome g;
    private final String subject, shownName;
    private final int pity;
    @Nullable private Screen parent;
    private int left, top;
    private float pt;

    public static void open(CompoundTag tag, String subject, String name, int pity) { open(tag, subject, name, pity, null); }

    /** Opens over {@code parent} (for example the Incubator); closing returns to it. pity < 0 hides the pity line. */
    public static void open(CompoundTag tag, String subject, String name, int pity, @Nullable Screen parent) {
        FalconGenome g = FalconGenome.load(tag);
        if (g == null) return;
        PedigreeScreen s = new PedigreeScreen(g, subject, name, pity);
        s.parent = parent;
        Minecraft.getInstance().setScreen(s);
    }

    private PedigreeScreen(FalconGenome g, String subject, String name, int pity) {
        super(Component.translatable("codex.quantumfalcon.pedigree"));
        this.g = g; this.subject = subject; this.shownName = name; this.pity = pity;
    }

    @Override protected void init() { left = (width - W) / 2; top = (height - H) / 2; }
    @Override public boolean isPauseScreen() { return false; }
    @Override
    public void onClose() {
        if (parent != null) Minecraft.getInstance().setScreen(parent);
        else super.onClose();
    }
    private boolean overClose(double mx, double my) { return mx >= left + 240 && mx < left + 252 && my >= top + 4 && my < top + 16; }
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (overClose(mx, my)) { QfUi.uiSound("ui_click"); onClose(); return true; }
        return super.mouseClicked(mx, my, button);
    }

    private static String title(String id) {
        StringBuilder b = new StringBuilder();
        for (String w : id.split("_")) if (!w.isEmpty()) b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(' ');
        return b.toString().trim();
    }
    private static BreedingData.TierDef tierDef(int t) { return BreedingData.TIERS.get(Mth.clamp(t, 1, 7) - 1); }
    private static String abbrev(int t) { String n = tierDef(t).name().toUpperCase(); return n.substring(0, Math.min(3, n.length())); }
    @Nullable private static BreedingData.AbilityDef ability(String id) {
        for (var a : BreedingData.ABILITIES) if (a.id().equals(id)) return a;
        return null;
    }
    private static int categoryIcon(String cat) {
        return switch (cat.toUpperCase()) {
            case "COMBAT" -> QfUi.ICON_CLAW; case "ACTIVE" -> QfUi.ICON_WING; case "PASSIVE" -> QfUi.ICON_LOYALTY;
            case "RIFT" -> QfUi.ICON_RIFT; case "FUSION" -> QfUi.ICON_SHARD; default -> QfUi.ICON_BOOK;
        };
    }
    private int nextSlotTier() {
        for (int t = g.tier + 1; t <= 7; t++) if (tierDef(t).slots() > g.slots()) return t;
        return 0;
    }
    private void text(GuiGraphics gg, String s, int x, int y, int w, int h, QfText.Size size, int col, int align) {
        QfText.draw(gg, font, s, left + x, top + y, w, h, size, col, align);
    }
    private void text(GuiGraphics gg, String s, int x, int y, int w, int h, QfText.Size size, int col) { text(gg, s, x, y, w, h, size, col, QfText.LEFT); }

    @Override
    public void render(GuiGraphics gg, int mx, int my, float pt) {
        super.render(gg, mx, my, pt);
        this.pt = pt;
        int x = left, y = top;
        QfUi.panelSplit(gg, x, y, W, H, QfUi.CYAN, QfUi.MAGENTA, pt);
        text(gg, "PEDIGREE", 16, 8, 220, 9, QfText.Size.DEFAULT, 0xFF28ECFF, QfText.CENTRE);
        // close box (240,4) 12x12 with a pixel X
        int cc = overClose(mx, my) ? 0xFFFFFFFF : 0xFFFF38D6;
        gg.renderOutline(x + 240, y + 4, 12, 12, cc);
        for (int i = 0; i < 6; i++) { gg.fill(x + 243 + i, y + 7 + i, x + 244 + i, y + 8 + i, cc); gg.fill(x + 248 - i, y + 7 + i, x + 249 - i, y + 8 + i, cc); }

        portrait(gg, x, y, mx, my);
        stats(gg);
        lineage(gg, mx, my);
        abilities(gg, mx, my);
    }

    private void portrait(GuiGraphics gg, int x, int y, int mx, int my) {
        int c = tierDef(g.tier).colour();
        gg.fill(x + 8, y + 20, x + 70, y + 88, 0xFF0A1218);
        gg.renderOutline(x + 8, y + 20, 62, 68, QfUi.glow(QfUi.CYAN, pt, 1f));
        if (subject.equals("egg")) {
            ItemStack egg = new ItemStack(ModItems.FALCON_EGG.get());
            com.matt.quantumfalcon.item.FalconEggItem.wild(egg, g);
            gg.pose().pushPose(); gg.pose().translate(x + 39 - 24, y + 24, 0); gg.pose().scale(3f, 3f, 1f);
            gg.renderItem(egg, 0, 0);
            gg.pose().popPose();
        } else Portraits.draw(gg, g.variant, Portraits.LARGE, x + 9, y + 21, 60, 56);
        text(gg, "T" + g.tier, 10, 77, 16, 9, QfText.Size.DEFAULT, 0xFFFFFFFF);
        text(gg, g.tierDef().name().toUpperCase(), 28, 77, 40, 9, QfText.Size.DEFAULT, c);
        String head = subject.equals("egg") ? title(g.variant) + " egg" : (shownName.isEmpty() ? title(g.variant) : shownName);
        text(gg, head.toUpperCase(), 8, 92, 62, 9, QfText.Size.DEFAULT, BODY);
        text(gg, "GEN " + g.generation + (g.isHybrid() ? " HYBRID" : ""), 8, 101, 62, 7, QfText.Size.SMALL, MUTED);
        if (pity >= 0) text(gg, "PITY " + pity + "/10 +" + pity * 2 + "%", 8, 108, 62, 5, QfText.Size.TINY, 0xFF28ECFF);
        if (mx >= x + 8 && mx < x + 70 && my >= y + 20 && my < y + 114) {
            var bl = g.bloodlines();
            gg.renderComponentTooltip(font, List.of(
                Component.literal(title(g.variant) + (shownName.isEmpty() ? "" : " \"" + shownName + "\"")).withColor(c),
                Component.literal("Crack: " + g.crackDef().name()).withColor(g.crackDef().colour()),
                Component.literal("Pattern: " + g.patternDef().name()),
                Component.literal("Size: " + title(g.sizeName()) + "   Glow: " + g.glowLevel()),
                Component.literal("Bloodline: " + (bl.isEmpty() ? "none" : String.join(", ", bl.stream().map(PedigreeScreen::title).toList()))).withColor(0xFFB43A)), mx, my);
        }
    }

    private void stats(GuiGraphics gg) {
        text(gg, "STATS", 74, 20, 90, 9, QfText.Size.DEFAULT, 0xFF28ECFF);
        int cap = g.statCap();
        for (int i = 0; i < FalconGenome.STATS; i++) {
            int v = Mth.clamp(g.stat(i), 0, 100), ry = 31 + i * 18;
            QfUi.icon(gg, left + 74, top + ry, STAT_ICON[i], 0xFF000000 | STAT_COL[i]);
            text(gg, STAT[i], 84, ry, 52, 9, QfText.Size.DEFAULT, BODY);
            text(gg, String.valueOf(v), 146, ry, 18, 9, QfText.Size.DEFAULT, STAT_COL[i], QfText.RIGHT);
            text(gg, "CAP " + cap, 84, ry + 11, 28, 5, QfText.Size.TINY, MUTED);
            QfUi.bar(gg, left + 114, top + ry + 11, 50, 5, v / 100f, STAT_COL[i], cap / 100f, pt);
        }
    }

    private void lineage(GuiGraphics gg, int mx, int my) {
        text(gg, "LINEAGE", 168, 20, 80, 9, QfText.Size.DEFAULT, 0xFFFF38D6);
        text(gg, "PARENTS", 168, 29, 80, 7, QfText.Size.SMALL, MUTED);
        text(gg, "GRANDPARENTS", 168, 86, 80, 7, QfText.Size.SMALL, MUTED);
        for (int i = 0; i < 2; i++) {
            int cx = 168 + i * 41, cy = 37;
            FalconGenome.Ancestor a = i < g.parents.size() ? g.parents.get(i) : null;
            if (a == null) { QfUi.card(gg, left + cx, top + cy, 39, 46, QfUi.CARD_LOCKED, 0, pt); text(gg, "WILD", cx + 2, cy + 18, 35, 7, QfText.Size.SMALL, LOCKED, QfText.CENTRE); continue; }
            QfUi.card(gg, left + cx, top + cy, 39, 46, QfUi.CARD_ACTIVE, tierDef(a.tier()).colour(), pt);
            Portraits.draw(gg, a.variant(), Portraits.MEDIUM, left + cx + 1, top + cy + 2, 37, 24);
            text(gg, (a.name().isEmpty() ? title(a.variant()) : a.name()).toUpperCase(), cx + 2, cy + 28, 35, 7, QfText.Size.SMALL, BODY);
            text(gg, "T" + a.tier() + " " + abbrev(a.tier()), cx + 2, cy + 36, 35, 7, QfText.Size.SMALL, tierDef(a.tier()).colour());
            tip(gg, a, cx, cy, 39, 46, mx, my);
        }
        for (int i = 0; i < 4; i++) {
            int cx = 168 + (i % 2) * 41, cy = 94 + (i / 2) * 23;
            FalconGenome.Ancestor a = i < g.grandparents.size() ? g.grandparents.get(i) : null;
            QfUi.card(gg, left + cx, top + cy, 39, 22, a == null ? QfUi.CARD_LOCKED : QfUi.CARD_DORMANT, 0, pt);
            if (a == null) { text(gg, "?", cx + 2, cy + 7, 35, 9, QfText.Size.DEFAULT, LOCKED, QfText.CENTRE); continue; }
            Portraits.draw(gg, a.variant(), Portraits.SMALL, left + cx + 2, top + cy + 1, 18, 14);
            text(gg, "T" + a.tier(), cx + 21, cy + 2, 16, 7, QfText.Size.TINY, tierDef(a.tier()).colour());
            text(gg, title(a.variant()).toUpperCase(), cx + 2, cy + 15, 35, 5, QfText.Size.TINY, BODY);
            tip(gg, a, cx, cy, 39, 22, mx, my);
        }
    }
    private void tip(GuiGraphics gg, FalconGenome.Ancestor a, int cx, int cy, int w, int h, int mx, int my) {
        if (mx >= left + cx && mx < left + cx + w && my >= top + cy && my < top + cy + h)
            gg.renderComponentTooltip(font, List.of(Component.literal(a.name().isEmpty() ? title(a.variant()) : a.name()),
                Component.literal(title(a.variant()) + " · " + tierDef(a.tier()).name()).withColor(tierDef(a.tier()).colour())), mx, my);
    }

    private void abilities(GuiGraphics gg, int mx, int my) {
        text(gg, "ABILITIES", 8, 142, 70, 9, QfText.Size.DEFAULT, 0xFF28ECFF);
        int[] lx = {82, 136, 196};
        String[] ln = {"ACTIVE", "DORMANT", "LOCKED"};
        int[] lc = {0xFF28ECFF, 0xFFAA50FF, LOCKED};
        for (int i = 0; i < 3; i++) {
            gg.fill(left + lx[i], top + 144, left + lx[i] + 4, top + 148, lc[i]);
            text(gg, ln[i], lx[i] + 6, 143, 48, 7, QfText.Size.SMALL, MUTED);
        }
        record Card(String id, int state) {}
        List<Card> cards = new ArrayList<>();
        for (String a : g.abilities) cards.add(new Card(a, QfUi.CARD_ACTIVE));
        for (String a : g.dormant) cards.add(new Card(a, QfUi.CARD_DORMANT));
        int next = nextSlotTier();
        if (next > 0) cards.add(new Card("", QfUi.CARD_LOCKED));
        for (int i = 0; i < 3; i++) {
            int cx = 8 + i * 80, cy = 152;
            if (i >= cards.size()) { QfUi.card(gg, left + cx, top + cy, 78, 58, QfUi.CARD_LOCKED, 0, pt); text(gg, "EMPTY", cx + 17, cy + 22, 59, 9, QfText.Size.DEFAULT, 0xFF404456); continue; }
            Card c = cards.get(i);
            QfUi.card(gg, left + cx, top + cy, 78, 58, c.state, QfUi.CYAN, pt);
            int tagCol = c.state == QfUi.CARD_ACTIVE ? 0xFF28ECFF : c.state == QfUi.CARD_DORMANT ? 0xFFAA50FF : LOCKED;
            gg.fill(left + cx + 3, top + cy + 4, left + cx + 15, top + cy + 16, 0xFF06080C);
            gg.renderOutline(left + cx + 3, top + cy + 4, 12, 12, (tagCol & 0x00FFFFFF) | 0x80000000);
            if (c.state == QfUi.CARD_LOCKED) {
                QfUi.icon(gg, left + cx + 5, top + cy + 6, QfUi.ICON_LOCK, LOCKED);
                text(gg, "SLOT " + (g.slots() + 1), cx + 17, cy + 4, 59, 9, QfText.Size.DEFAULT, LOCKED);
                text(gg, "Unlocks at " + tierDef(next).name() + " (T" + next + ")", cx + 17, cy + 14, 59, 14, QfText.Size.SMALL, LOCKED);
            } else {
                var def = ability(c.id);
                QfUi.icon(gg, left + cx + 5, top + cy + 6, def == null ? QfUi.ICON_BOOK : categoryIcon(def.category()), tagCol);
                text(gg, (def == null ? title(c.id) : def.name()).toUpperCase(), cx + 17, cy + 4, 59, 9, QfText.Size.DEFAULT, c.state == QfUi.CARD_ACTIVE ? BODY : MUTED);
                if (def != null) {
                    text(gg, def.effect(), cx + 17, cy + 14, 59, 14, QfText.Size.SMALL, MUTED);
                    text(gg, title(def.category()) + (def.cooldownSeconds() > 0 ? " · " + def.cooldownSeconds() + "s" : ""), cx + 17, cy + 30, 59, 7, QfText.Size.SMALL, 0xFF6A7290);
                    if (mx >= left + cx && mx < left + cx + 78 && my >= top + cy && my < top + cy + 58)
                        gg.renderTooltip(font, font.split(Component.literal(def.name() + ": " + def.effect()), 200), mx, my);
                }
            }
            text(gg, c.state == QfUi.CARD_ACTIVE ? "ACTIVE" : c.state == QfUi.CARD_DORMANT ? "DORMANT" : "LOCKED", cx + 17, cy + 46, 48, 9, QfText.Size.DEFAULT, tagCol);
        }
        if (cards.size() > 3) text(gg, "+" + (cards.size() - 3) + " MORE ABILITIES", 8, 133, 62, 7, QfText.Size.SMALL, MUTED);
    }
}
