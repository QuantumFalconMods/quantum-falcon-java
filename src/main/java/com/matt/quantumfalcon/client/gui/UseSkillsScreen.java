package com.matt.quantumfalcon.client.gui;

import com.matt.quantumfalcon.skills.UseSkill;
import com.matt.quantumfalcon.skills.UseSkills;
import com.matt.quantumfalcon.skills.UseSkillsClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

/**
 * A9 Skills page: one row per use-skill with an icon, the name, "Lv 17", a thin progress bar with "1,240 / 1,600 XP"
 * and the current bonus in plain words. No sub-menus; hovering a row shows the next level and how to earn XP.
 */
public class UseSkillsScreen extends Screen {
    private static final int W = 360, ROW = 25, HEAD = 30, NEON = 0xFF3FF2FF;
    private final Screen parent;
    private int left, top, h;

    public UseSkillsScreen(Screen parent) { super(Component.literal("Skills")); this.parent = parent; }

    /** Skill tree v6 use-skill badge (HD twin on even GUI scales, like the tree's icons). */
    static net.minecraft.resources.ResourceLocation badge(UseSkill s) {
        boolean hd = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale() % 2 == 0;
        return SkillTreeScreen.st((hd ? "use_skill_hd/" : "use_skill/") + s.key());
    }

    static ItemStack icon(UseSkill s) {
        return new ItemStack(switch (s) {
            case MINING -> Items.IRON_PICKAXE; case WOODCUTTING -> Items.IRON_AXE; case COMBAT -> Items.IRON_SWORD; case ARCHERY -> Items.BOW;
            case FALCONRY -> Items.FEATHER; case BREEDING -> Items.EGG; case EXPLORATION -> Items.COMPASS; case ARCANA -> Items.AMETHYST_SHARD;
        });
    }

    static String n(long v) { return String.format(Locale.ROOT, "%,d", v); }

    @Override
    protected void init() {
        h = HEAD + ROW * UseSkill.values().length + 22;
        left = (width - W) / 2;
        top = Math.max(4, (height - h) / 2);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        QfUi.panel(g, left, top, W, h, 0x3FF2FF, pt);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawString(font, "Skills", left + 12, top + 10, NEON, false);
        String sub = "Level up by doing. Nothing is ever locked.";
        g.drawString(font, sub, left + W - 12 - font.width(sub), top + 10, 0xFF8890A8, false);
        UseSkill hover = null;
        int milestones = 0;
        for (UseSkill s : UseSkill.values()) {
            int y = top + HEAD + s.ordinal() * ROW;
            long xp = UseSkillsClientState.xp(s);
            int lv = UseSkill.levelOf(xp);
            for (int m : UseSkills.MILESTONES) if (lv >= m) milestones++;
            if (mx >= left + 8 && mx < left + W - 8 && my >= y - 2 && my < y + ROW - 2) { hover = s; g.fill(left + 8, y - 2, left + W - 8, y + ROW - 3, 0x22FFFFFF); }
            g.blitSprite(badge(s), left + 12, y + 2, 16, 16);
            g.drawString(font, s.label, left + 34, y, 0xFF000000 | s.colour, false);
            String lvs = "Lv " + lv;
            g.drawString(font, lvs, left + 134 - font.width(lvs), y, 0xFFFFFFFF, false);
            float fill; String prog;
            if (lv >= UseSkill.MAX_LEVEL) { fill = 1f; prog = "MAX"; }
            else {
                long base = UseSkill.totalFor(lv), need = UseSkill.xpForNext(lv);
                fill = (xp - base) / (float) need;
                prog = n(xp - base) + " / " + n(need) + " XP";
            }
            QfUi.bar(g, left + 140, y + 1, 104, 6, fill, s.colour, -1f, pt);
            g.drawString(font, prog, left + 250, y, 0xFFB8C0D8, false);
            g.drawString(font, s.describe(lv), left + 34, y + 11, 0xFFD8DCE8, false);
        }
        int given = Math.min(UseSkills.BONUS_POINTS_CAP, milestones);
        String foot = "Bonus skill points from level 25 and 50 milestones: " + given + " / " + UseSkills.BONUS_POINTS_CAP;
        g.drawString(font, foot, left + 12, top + h - 16, 0xFF8890A8, false);
        if (hover != null) {
            int lv = UseSkill.levelOf(UseSkillsClientState.xp(hover));
            List<Component> tip = new java.util.ArrayList<>();
            tip.add(Component.literal(hover.label + " " + lv).withColor(hover.colour));
            tip.add(Component.literal("How to earn: " + hover.howToEarn()).withColor(0xB8C0D8));
            if (lv < UseSkill.MAX_LEVEL) tip.add(Component.literal("At level " + (lv + 1) + ": " + hover.describe(lv + 1)).withColor(0xFFFFFF));
            g.renderComponentTooltip(font, tip, mx, my);
        }
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(parent); }

    /** Called from UseSkillsPayload on the client. */
    public static void onSync(int toastSkill, int toastLevel, boolean milestone, boolean open) {
        Minecraft mc = Minecraft.getInstance();
        if (toastSkill >= 0 && toastSkill < UseSkill.values().length) mc.getToasts().addToast(new LevelToast(UseSkill.values()[toastSkill], toastLevel, milestone));
        if (open) mc.setScreen(new UseSkillsScreen(mc.screen));
    }

    /** Small level-up toast: "Mining 18" + what changed. */
    static final class LevelToast implements Toast {
        final UseSkill s; final int lv; final boolean milestone;
        LevelToast(UseSkill s, int lv, boolean milestone) { this.s = s; this.lv = lv; this.milestone = milestone; }
        @Override public int width() { return 250; }
        @Override public int height() { return 32; }
        @Override
        public Visibility render(GuiGraphics g, ToastComponent tc, long time) {
            var font = tc.getMinecraft().font;
            g.fill(0, 0, width(), height(), 0xF00A0A10);
            g.renderOutline(0, 0, width(), height(), 0xFF000000 | s.colour);
            g.blitSprite(badge(s), 6, 8, 16, 16);
            g.drawString(font, s.label + " " + lv + (milestone ? "  +1 skill point" : ""), 28, 6, 0xFF000000 | s.colour, false);
            String d = s.describe(lv);
            if (font.width(d) > width() - 32) d = font.plainSubstrByWidth(d, width() - 38) + "...";
            g.drawString(font, d, 28, 18, 0xFFD8DCE8, false);
            return time >= 5000L ? Visibility.HIDE : Visibility.SHOW;
        }
    }
}
