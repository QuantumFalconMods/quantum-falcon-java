package com.matt.quantumfalcon.client.gui;

import com.matt.quantumfalcon.network.ClientSkillData;
import com.matt.quantumfalcon.network.SkillUnlockPacket;
import com.matt.quantumfalcon.skill.SkillTreeSystem;
import com.matt.quantumfalcon.skill.SkillTreeSystem.Category;
import com.matt.quantumfalcon.skill.SkillTreeSystem.Skill;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Set;

/**
 * Stage E skill tree. Five branch tabs with full names across the top, a 3-column x 4-tier tree, and a fixed info strip
 * under the tree instead of a floating tooltip (at GUI scale 4 the tooltip covered the tabs and half the nodes).
 * Node state is shown by frame and a badge, not colour alone (W1): owned = check badge, buyable = plus, locked = padlock
 * (or the hatch gate). All chrome is GUI-atlas sprite art (skill_tree/**, animated via .mcmeta, 1x at odd GUI scales, 2x at even). Connectors are routed as elbows through the gap between tier rows, so a line never crosses an
 * unrelated node (NS1); "one of" links are dotted.
 */
public class SkillTreeScreen extends Screen {

    static final int NODE = 24;
    static final int TAB_H = 16;
    static final int INFO_H = 44;

    private Category selectedCategory = Category.BOND;
    private Skill hoveredSkill = null;
    private Skill focusedSkill = null;
    private int panelW, panelH, left, top;
    private int treeTop, rowH, colW, treeLeft, infoTop;
    private SpriteButton masteryButton, unlockButton;
    private String boughtId; private long boughtAt;

    public SkillTreeScreen() {
        super(Component.translatable("gui.quantumfalcon.skill_tree"));
    }

    /** Sprite id in the GUI atlas: textures/gui/sprites/skill_tree/<p>.png (animated by its .mcmeta, nine-sliced / tiled by gui.scaling). */
    static ResourceLocation st(String p) { return ResourceLocation.fromNamespaceAndPath(com.matt.quantumfalcon.QuantumFalcon.MOD_ID, "skill_tree/" + p); }
    /** Even GUI scales sample the 2x art; odd scales the 1x art (2x at an odd scale lands on 1.5 px per texel). */
    static boolean hd() { return Minecraft.getInstance().getWindow().getGuiScale() % 2 == 0; }
    static String key(Category c) { return c.name().toLowerCase(java.util.Locale.ROOT); }

    @Override
    protected void init() {
        super.init();
        panelW = Math.min(360, width - 16);
        panelH = Math.min(250, height - 12);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        treeTop = top + 14 + TAB_H + 8;
        infoTop = top + panelH - 18 - INFO_H;
        rowH = Math.max(NODE + 8, (infoTop - 4 - treeTop) / 4);
        colW = Math.min(80, (panelW - 24) / 3);
        treeLeft = left + (panelW - colW * 3) / 2;
        masteryButton = addRenderableWidget(new SpriteButton(left + panelW - 100, top + panelH - 16, 92, 12,
                Component.literal("Mastery Mark (" + com.matt.quantumfalcon.skill.PlayerSkillData.MASTERY_MARK_COST + ")"),
                () -> PacketDistributor.sendToServer(new SkillUnlockPacket(SkillUnlockPacket.MASTERY_MARK))));
        masteryButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("A Mastery Mark is a trophy for a finished tree (cosmetic).")));
        unlockButton = addRenderableWidget(new SpriteButton(left, top, 60, 12, Component.literal("Unlock"), this::buyFocused));
        unlockButton.visible = false;
        addRenderableWidget(new SpriteButton(left + panelW / 2 - 26, top + panelH - 16, 52, 12, Component.literal("Skills"),
                () -> minecraft.setScreen(new UseSkillsScreen(this))))   // A9: use-based skills page
            .setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Skills you level by doing: Mining, Combat, Falconry...")));
        onboarding = !java.nio.file.Files.exists(seenFile());
    }

    /** buying is an explicit second step (the Unlock button in the strip). */
    void buyFocused() {
        Skill s = focusedSkill;
        if (s == null || s.lockReason(ClientSkillData.getUnlockedSkills(), ClientSkillData.getAvailablePoints(), ClientSkillData.getHighestHatched()) != null) return;
        PacketDistributor.sendToServer(new SkillUnlockPacket(s.id));
        boughtId = s.id; boughtAt = System.currentTimeMillis();
    }

    private void dismissOnboarding() {
        onboarding = false;
        try { java.nio.file.Files.createDirectories(seenFile().getParent()); java.nio.file.Files.writeString(seenFile(), "seen"); } catch (java.io.IOException ignored) {}
    }

    private int tabX(int i) { return left + 8 + i * ((panelW - 16) / Category.values().length); }
    private int tabW() { return (panelW - 16) / Category.values().length - 2; }

    com.matt.quantumfalcon.skill.SkillTreeLayout layout() { return new com.matt.quantumfalcon.skill.SkillTreeLayout(treeLeft, treeTop, colW, rowH, NODE); }
    int nodeX(Skill s) { return layout().x(s); }
    int nodeY(Skill s) { return layout().y(s); }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        // static panel (the animated background/frame/header made the buy pulse meaningless); no watermark
        g.blitSprite(st("panel/background_still"), left + 2, top + 2, panelW - 4, panelH - 4);
        g.blitSprite(st("panel/frame_still"), left, top, panelW, panelH);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        Set<String> owned = ClientSkillData.getUnlockedSkills();
        int points = ClientSkillData.getAvailablePoints();
        int hatched = ClientSkillData.getHighestHatched();
        masteryButton.active = owned.size() >= SkillTreeSystem.getTotalSkillCount() && points >= com.matt.quantumfalcon.skill.PlayerSkillData.MASTERY_MARK_COST;
        boolean hd = hd();
        String nd = hd ? "node_hd/" : "node/", ic = hd ? "icon_hd/" : "icon/", bd = hd ? "badge_hd/" : "badge/", pip = hd ? "pip_hd/" : "pip/";
        String b = key(selectedCategory);

        // header plaque: void-stalker portrait + title
        int hw = font.width(title) + 40, hx = left + (panelW - hw) / 2;
        g.blitSprite(st("panel/header_still"), hx, top - 5, hw, 16);
        Portraits.draw(g, "voidstalker", Portraits.SMALL, hx + 8, top - 4, 18, 14);
        g.drawString(font, title, hx + 28, top - 1, 0xFF7FE9FF, false);

        // tabs: branch emblem + full name (the owned count moved to the info strip)
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            int x = tabX(i), y = top + 14, w = tabW();
            boolean sel = c == selectedCategory;
            boolean hov = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + TAB_H;
            g.blitSprite(st(sel ? "tab/selected_" + key(c) + "_still" : hov ? "tab/hover" : "tab/idle"), x, y, w, TAB_H);
            // no emblem blobs; the full name carries the meaning
            String label = c.displayName;
            int lx = x + (w - font.width(label)) / 2;
            g.drawString(font, label, lx, y + 4, sel ? 0xFFFFFFFF : hov ? 0xFFD8D8E8 : 0xFFB8B8C8, false);
        }

        // no right-hand banner (it repeated the selected tab). no row numerals (the rows are visible).
        // calm the tree area: the panel's lightning streaks and specks are dimmed behind the nodes
        g.fill(treeLeft - 6, treeTop - 4, treeLeft + colW * 3 + 6, infoTop - 3, 0xB00A0912);

        List<Skill> skills = SkillTreeSystem.getSkillsByCategory(selectedCategory);
        Skill hovPre = null;
        for (Skill s : skills) { int x = nodeX(s), y = nodeY(s); if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) hovPre = s; }

        // + v6 art: one routing (SkillTreeLayout.connector), drawn with the link/* sprites (3 px, tiled):
        // lit "owned" once the parent is owned, a slow comet ("active") when the child can be bought, gunmetal "locked" otherwise;
        // any-of parents use the dotted twins. Joints sit on the elbows.
        String br = selectedCategory.name().toLowerCase(java.util.Locale.ROOT);
        for (Skill s : skills) {
            NodeState cs = state(s, owned, points, hatched);
            for (String pid : s.parents()) {
                Skill p = SkillTreeSystem.getSkill(pid);
                if (p == null || p.category != s.category) continue;
                boolean on = owned.contains(pid);
                String ls = !on ? "locked" : cs == NodeState.BUYABLE ? "active" : "owned";
                String dot = s.anyOf.contains(pid) ? "_dotted" : "";
                List<int[]> segs = layout().connector(p, s);
                boolean rev = segs.get(2)[0] < segs.get(0)[0];
                for (int[] seg : segs) {
                    if (seg[0] == seg[2] && seg[1] == seg[3]) continue;
                    if (seg[1] == seg[3]) {
                        String name = on ? br + "_h_" + ls + (rev ? "_rev" : "") + dot : "h_locked" + dot;
                        int x0 = Math.min(seg[0], seg[2]);
                        g.blitSprite(st("link/" + name), x0, seg[1], Math.max(seg[0], seg[2]) + 2 - x0, 3);
                    } else {
                        String name = on ? br + "_v_" + ls + dot : "v_locked" + dot;
                        int y0 = Math.min(seg[1], seg[3]);
                        g.blitSprite(st("link/" + name), seg[0], y0, 3, Math.max(seg[1], seg[3]) - y0);
                    }
                }
                int[] mid = segs.get(1);
                if (mid[0] != mid[2]) {
                    ResourceLocation joint = st(on ? "link/" + br + "_joint" : "link/joint_locked");
                    g.blitSprite(joint, segs.get(0)[0] - 1, mid[1] - 1, 5, 5);
                    g.blitSprite(joint, segs.get(2)[0] - 1, mid[1] - 1, 5, 5);
                }
            }
        }

        hoveredSkill = null;
        for (Skill s : skills) {
            int x = nodeX(s), y = nodeY(s);
            NodeState ns = state(s, owned, points, hatched);
            boolean hov = s == hovPre;
            if (hov) hoveredSkill = s;
            // owned = solid filled, static frame + green check; buyable = the only animated node, gold outline;
            // next (prerequisites met, points short) = still frame dimmed, with its cost; locked = grey frame + padlock
            switch (ns) {
                case OWNED -> {
                    g.fill(x - 1, y - 1, x + NODE + 1, y + NODE + 1, (selectedCategory.argb & 0x00FFFFFF) | 0x66000000);
                    g.blitSprite(st(nd + b + "_owned_still"), x - 2, y - 2, NODE + 4, NODE + 4);
                }
                case BUYABLE -> {
                    g.blitSprite(st(nd + b + "_available"), x - 2, y - 2, NODE + 4, NODE + 4);
                    g.renderOutline(x - 3, y - 3, NODE + 6, NODE + 6, 0xFFFFD75A);
                }
                case NEXT -> {
                    g.setColor(.6f, .6f, .66f, 1f);
                    g.blitSprite(st(nd + b + "_available_still"), x - 2, y - 2, NODE + 4, NODE + 4);
                    g.setColor(1f, 1f, 1f, 1f);
                }
                default -> g.blitSprite(st(nd + "locked"), x - 2, y - 2, NODE + 4, NODE + 4);
            }
            // locked icons stay readable (about 3:1, not tinted to near-black)
            if (ns == NodeState.LOCKED) g.setColor(.78f, .78f, .84f, 1f);
            else if (ns == NodeState.NEXT) g.setColor(.85f, .85f, .9f, 1f);
            g.blitSprite(st(ic + s.id), x + 4, y + 4, 16, 16);
            g.setColor(1f, 1f, 1f, 1f);
            // fixed badge colours on every branch (green check, gold plus, grey padlock, egg for a hatch gate)
            String badge = switch (ns) {
                case OWNED -> "owned"; case BUYABLE -> "available"; case NEXT -> null;
                default -> s.lockReason(owned, 999, hatched) != null && s.lockReason(owned, 999, hatched).startsWith("Hatch") ? "egg" : "locked";
            };
            if (badge != null) g.blitSprite(st(bd + badge), x + NODE - 7, y + NODE - 7, 9, 9);
            // the cost as a number (gold if affordable, grey otherwise); owned nodes show none
            if (ns != NodeState.OWNED) {
                String c = String.valueOf(s.cost);
                g.drawString(font, c, x + NODE + 3, y + NODE - 8, s.cost <= points ? 0xFFFFD75A : 0xFF8A8A9A, true);
            }
            if (hov || s == focusedSkill) g.blitSprite(st(nd + "reticle"), x - 4, y - 4, NODE + 8, NODE + 8);
        }
        // hovering a locked node outlines the parent(s) it is still waiting for
        if (hoveredSkill != null && state(hoveredSkill, owned, points, hatched) == NodeState.LOCKED) {
            for (String pid : hoveredSkill.parents()) {
                Skill p = SkillTreeSystem.getSkill(pid);
                if (p == null || owned.contains(pid) || p.category != selectedCategory) continue;
                int px = nodeX(p), py = nodeY(p);
                if (state(p, owned, points, hatched) == NodeState.BUYABLE) {
                    // the parent you can buy now is the next step, not a warning: pulse its gold frame
                    float t = (System.currentTimeMillis() % 1000L) / 1000f;
                    int a = 0x60 + (int) (0x9F * (0.5f + 0.5f * (float) Math.sin(t * Math.PI * 2)));
                    g.renderOutline(px - 4, py - 4, NODE + 8, NODE + 8, (a << 24) | 0xFFD75A);
                    g.renderOutline(px - 5, py - 5, NODE + 10, NODE + 10, ((a / 2) << 24) | 0xFFD75A);
                    // on a dark plate clear of the pulse frame, so it never sits on the connector bar
                    int lw = font.width("buy first"), lx = px + NODE / 2 - lw / 2, ly = py - 18;
                    g.fill(lx - 3, ly - 2, lx + lw + 3, ly + 9, 0xE0101018);
                    g.renderOutline(lx - 3, ly - 2, lw + 6, 11, 0x80FFD75A);
                    g.drawString(font, "buy first", lx, ly, 0xFFFFD75A, false);
                } else {   // a parent that is itself locked keeps the warning colour
                    g.renderOutline(px - 4, py - 4, NODE + 8, NODE + 8, 0xFFFF8A7A);
                    g.renderOutline(px - 5, py - 5, NODE + 10, NODE + 10, 0x80FF8A7A);
                }
            }
        }

        renderInfo(g, hoveredSkill != null ? hoveredSkill : focusedSkill, owned, points, hatched);
        // the strip is drawn after the widgets, so the gold Unlock button is drawn again on top of it
        if (unlockButton.visible) unlockButton.render(g, mouseX, mouseY, partialTick);

        int fy = top + panelH - 14;
        g.blitSprite(st((hd ? "footer_hd/" : "footer/") + "point"), left + 8, fy - 1, 9, 9);
        g.drawString(font, "Points: " + points, left + 19, fy, 0xFFFFD75A, false);
        // the Mastery Mark (a cosmetic counter that needs the full tree) stays hidden until every node is owned
        boolean complete = owned.size() >= SkillTreeSystem.getTotalSkillCount();
        masteryButton.visible = complete;
        if (complete) {
            int mx = left + 19 + font.width("Points: " + points) + 10;
            g.blitSprite(st((hd ? "footer_hd/" : "footer/") + "mastery_mark"), mx, fy - 1, 9, 9);
            g.drawString(font, "Marks: " + ClientSkillData.getMasteryMarks(), mx + 11, fy, 0xFFFFD75A, false);
        }
        if (onboarding) renderOnboarding(g);
    }

    enum NodeState { OWNED, BUYABLE, NEXT, LOCKED }

    /** "next" = prerequisites (and hatch gate) met but not enough points; "locked" = anything else still missing. */
    static NodeState state(Skill s, Set<String> owned, int points, int hatched) {
        if (owned.contains(s.id)) return NodeState.OWNED;
        if (s.lockReason(owned, points, hatched) == null) return NodeState.BUYABLE;
        return s.lockReason(owned, 9999, hatched) == null ? NodeState.NEXT : NodeState.LOCKED;
    }

    // ---- one-time first-open card (remembered in config/quantumfalcon-skilltree-seen)
    private boolean onboarding;
    private static java.nio.file.Path seenFile() { return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("quantumfalcon-skilltree-seen"); }
    private void renderOnboarding(GuiGraphics g) {
        int w = 236, h = 62, x = left + (panelW - w) / 2, y = treeTop + 18;
        g.pose().pushPose(); g.pose().translate(0, 0, 400);
        g.fill(x, y, x + w, y + h, 0xF0101018);
        g.renderOutline(x, y, w, h, 0xFFFFD75A);
        g.drawString(font, "How the skill tree works", x + 8, y + 6, 0xFFFFD75A, false);
        g.drawString(font, "Gold outline + = you can buy it", x + 8, y + 19, 0xFFE0E0EC, false);
        g.drawString(font, "Click a node, then press Unlock", x + 8, y + 30, 0xFFE0E0EC, false);
        g.drawString(font, "Padlock = hover it to see why", x + 8, y + 41, 0xFFE0E0EC, false);
        g.drawString(font, "click to close", x + w - 6 - font.width("click to close"), y + h - 10, 0xFF8A8A9A, false);
        g.pose().popPose();
    }

    private void renderInfo(GuiGraphics g, Skill s, Set<String> owned, int points, int hatched) {
        int x = left + 8, y = infoTop, w = panelW - 16;
        g.blitSprite(st("panel/inset_still"), x, y, w, INFO_H);
        List<Skill> branch = SkillTreeSystem.getSkillsByCategory(selectedCategory);
        int have = (int) branch.stream().filter(k -> owned.contains(k.id)).count();
        String count = selectedCategory.displayName + " " + have + "/" + branch.size();
        unlockButton.visible = false;
        if (s == null) {
            // the branch count gets its own line, so the help text never runs into it
            g.drawString(font, count + " owned", x + 4, y + 4, 0xFF8FE88F, false);
            String help = selectedCategory == Category.LINEAGE ? "Hatch falcons to open Lineage: each node needs a hatched falcon of its rarity."
                    : points > 0 ? "Gold outline = you can buy it. Click a node, then press Unlock."
                    : "Earn points by taming, first boss kills, first hatches, main quests and a first max bond.";
            List<FormattedCharSequence> lines = font.split(Component.literal(help), w - 8);
            for (int i = 0; i < Math.min(3, lines.size()); i++) g.drawString(font, lines.get(i), x + 4, y + 15 + i * 10, 0xFF9A9AB0, false);
            return;
        }
        g.drawString(font, count, x + w - 4 - font.width(count), y + 4, 0xFF8FE88F, false);
        boolean hd = hd();
        NodeState ns = state(s, owned, points, hatched);
        String why = s.lockReason(owned, points, hatched);
        String frame = switch (ns) { case OWNED -> key(selectedCategory) + "_owned_still"; case BUYABLE -> key(selectedCategory) + "_available"; case NEXT -> key(selectedCategory) + "_available_still"; default -> "locked"; };
        g.blitSprite(st((hd ? "node_hd/" : "node/") + frame), x + 5, y + 8, 28, 28);
        if (ns == NodeState.LOCKED) g.setColor(.78f, .78f, .84f, 1f);
        g.blitSprite(st((hd ? "icon_hd/" : "icon/") + s.id), x + 11, y + 14, 16, 16);
        g.setColor(1f, 1f, 1f, 1f);
        String state; int stateCol;
        boolean justBought = s.id.equals(boughtId) && System.currentTimeMillis() - boughtAt < 2500 && ns == NodeState.OWNED;
        if (justBought) { state = "Unlocked!"; stateCol = 0xFF8FFF8F; }
        else if (ns == NodeState.OWNED) { state = "Owned"; stateCol = 0xFF8FE88F; }
        else if (ns == NodeState.BUYABLE) { state = s == focusedSkill ? "Ready to unlock" : "Click to select, then Unlock"; stateCol = 0xFFFFD75A; }
        else { state = (ns == NodeState.NEXT ? "Next: " : "Locked: ") + why; stateCol = ns == NodeState.NEXT ? 0xFFD8C080 : 0xFFFF8A7A; }
        int tx = x + 40, tw = w - 46;
        // no "Tier N" (row) in the title; the cost is a plain number
        g.drawString(font, s.name + "  \u00b7  " + s.cost + " pt" + (s.cost > 1 ? "s" : ""), tx, y + 4, selectedCategory.argb, false);
        List<FormattedCharSequence> lines = font.split(Component.literal(s.description), tw);
        for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(font, lines.get(i), tx, y + 15 + i * 10, 0xFFE0E0EC, false);
        // a buyable node that is selected gets an "Unlock: N pts" button in the strip; the first click only selects
        int stateW = tw;
        // a framed gold "Unlock (N pt)" button; greyed out (still shown) when the node is next but unaffordable
        if ((ns == NodeState.BUYABLE || ns == NodeState.NEXT) && s == focusedSkill) {
            unlockButton.setMessage(Component.literal("Unlock (" + s.cost + " pt" + (s.cost > 1 ? "s" : "") + ")"));
            int bw = font.width(unlockButton.getMessage()) + 16;
            unlockButton.setWidth(bw); unlockButton.setHeight(14); unlockButton.setX(x + w - 4 - bw); unlockButton.setY(y + 29);
            unlockButton.visible = true; unlockButton.active = ns == NodeState.BUYABLE;
            unlockButton.gold = true;
            stateW = tw - bw - 6;
        }
        List<FormattedCharSequence> st = font.split(Component.literal(state), stateW);
        if (!st.isEmpty()) g.drawString(font, st.get(0), tx, y + 34, stateCol, false);
    }

    /** The Mastery Mark button on the panel's own nine-slice button sprites. */
    static final class SpriteButton extends net.minecraft.client.gui.components.AbstractButton {
        private static final net.minecraft.client.gui.components.WidgetSprites SPRITES =
                new net.minecraft.client.gui.components.WidgetSprites(st("button/normal_still"), st("button/disabled_still"), st("button/hover_still"));
        private final Runnable onPress;
        /** the screen's main action: gold fill and frame, brighter on hover, grey when disabled. */
        boolean gold;
        SpriteButton(int x, int y, int w, int h, Component label, Runnable onPress) { super(x, y, w, h, label); this.onPress = onPress; }
        @Override public void onPress() { QfUi.uiSound("ui_click"); onPress.run(); }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            if (gold) {
                var font = Minecraft.getInstance().font;
                boolean hov = active && isHoveredOrFocused();
                int x = getX(), y = getY(), w = getWidth(), h = getHeight();
                int fill = !active ? 0xFF2E2E36 : hov ? 0xFF8A6A1C : 0xFF5E4612;
                int edge = !active ? 0xFF5A5A66 : hov ? 0xFFFFF0A8 : 0xFFFFD75A;
                g.fill(x, y, x + w, y + h, edge);
                g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
                if (active) g.fill(x + 1, y + 1, x + w - 1, y + 2, hov ? 0xFFFFE89A : 0xFFB8902E);   // top bevel
                g.drawCenteredString(font, getMessage(), x + w / 2, y + (h - 8) / 2, !active ? 0xFF8A8A9A : hov ? 0xFFFFFFFF : 0xFFFFE6A0);
                return;
            }
            g.blitSprite(SPRITES.get(active, isHoveredOrFocused()), getX(), getY(), getWidth(), getHeight());
            var font = Minecraft.getInstance().font;
            g.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, active ? 0xFFFFFFFF : 0xFF8A8A9A);
        }
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) { defaultButtonNarrationText(out); }
    }

    private Skill skillAt(double mx, double my) {
        for (Skill s : SkillTreeSystem.getSkillsByCategory(selectedCategory)) {
            int x = nodeX(s), y = nodeY(s);
            if (mx >= x && mx < x + NODE && my >= y && my < y + NODE) return s;
        }
        return null;
    }

    /** Selects a tab (dev smoke runner too). */
    public void selectCategory(Category category) { this.selectedCategory = category; }

    /** Pins the info strip to a node (dev smoke runner: SKILLTREE <BRANCH> <id>). */
    public void focusSkill(String id) { this.focusedSkill = SkillTreeSystem.getSkill(id); }

    /** Centre of a skill node in GUI coordinates, or null if unknown. */
    public int[] nodeCenter(String skillId) {
        Skill s = SkillTreeSystem.getSkill(skillId);
        if (s == null) return null;
        return new int[]{nodeX(s) + NODE / 2, nodeY(s) + NODE / 2};
    }

    /** Rectangles of the visible nodes and the tabs, for the NS1 routing check in tests. */
    public java.util.Map<String, int[]> nodeRects() {
        java.util.Map<String, int[]> m = new java.util.LinkedHashMap<>();
        for (Skill s : SkillTreeSystem.getSkillsByCategory(selectedCategory)) m.put(s.id, new int[]{nodeX(s), nodeY(s), NODE, NODE});
        return m;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (onboarding) { dismissOnboarding(); return true; }
        if (button == 0) {
            Category[] cats = Category.values();
            for (int i = 0; i < cats.length; i++) {
                int x = tabX(i), y = top + 14;
                if (mouseX >= x && mouseX < x + tabW() && mouseY >= y && mouseY < y + TAB_H) { if (selectedCategory != cats[i]) QfUi.uiSound("ui_tab"); selectedCategory = cats[i]; focusedSkill = null; return true; }
            }
            // the node under the click itself, not the last frame's hover (a stale hover swallowed Unlock clicks)
            Skill under = skillAt(mouseX, mouseY);
            if (under != null) {   // a click selects (pins the strip); the strip's Unlock button buys
                focusedSkill = under;
                QfUi.uiSound("ui_click");
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // W12: Tab / Shift+Tab cycle branches
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
            Category[] cats = Category.values();
            int i = (selectedCategory.ordinal() + ((mods & org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT) != 0 ? cats.length - 1 : 1)) % cats.length;
            selectedCategory = cats[i]; focusedSkill = null;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
