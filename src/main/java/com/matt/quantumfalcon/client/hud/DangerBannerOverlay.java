package com.matt.quantumfalcon.client.hud;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.breeding.BreedingData;
import com.matt.quantumfalcon.client.gui.QfText;
import com.matt.quantumfalcon.client.gui.QfUi;
import com.matt.quantumfalcon.network.InstabilityPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * On-entry danger-tier banner (gui_specs.md section 11): 248x64, centred at y = 10, split-hue kit frame in the tier colour with
 * the shared pulse, area name, "DANGER TIER n · RECOMMENDED: {tier} GEAR", seven skull pips (first n filled). Fades in 8 ticks,
 * holds 4 s, fades out 12 ticks, plus the compact "ENTERED ..." line lower-left. Drops below the Instability meter when the
 * screen is too narrow for both on one row, and below any boss bars (it used to cover the boss HP bar for 5 s).
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public final class DangerBannerOverlay {
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "danger_banner");
    private static final int W = 248, H = 64, IN = 8, HOLD = 80, OUT = 12;
    private static String area = "";
    private static int tier;
    private static long start = -1000;

    private DangerBannerOverlay() {}

    public static void show(String areaName, int dangerTier) {
        area = areaName; tier = Mth.clamp(dangerTier, 1, 7);
        Minecraft mc = Minecraft.getInstance();
        start = mc.level != null ? mc.level.getGameTime() : 0;
    }

    /** bottom edge of the boss bars drawn this frame (vanilla stacks them from y = 12), so the banner sits below them. */
    private static int bossBottom = 0;

    @SubscribeEvent
    public static void onGuiPre(net.neoforged.neoforge.client.event.RenderGuiEvent.Pre e) { bossBottom = 0; }

    @SubscribeEvent
    public static void onBossBar(net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.BossEventProgress e) {
        bossBottom = Math.max(bossBottom, e.getY() + e.getIncrement());
    }

    /** Banner top edge for the current frame (public for the dev smoke/qftest). */
    public static int bannerY(int sw) {
        int x = (sw - W) / 2, y = 10;
        if (x < 136 && (InstabilityPackets.InstabilityClientData.getCurrentTier() > 0 || InstabilityPackets.InstabilityClientData.getZoneTier() >= 0)) y = 54;   // keep clear of the meter at (10,10)
        if (bossBottom > 0) y = Math.max(y, bossBottom + 4);
        return y;
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, LAYER, (g, delta) -> render(g, delta.getGameTimeDeltaPartialTick(false)));
    }

    private static void render(GuiGraphics g, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui || tier <= 0) return;
        float t = mc.level.getGameTime() - start + pt;
        if (t < 0 || t > IN + HOLD + OUT) return;
        float a = t < IN ? t / IN : t < IN + HOLD ? 1f : 1f - (t - IN - HOLD) / OUT;
        if (a <= 0.02f) return;
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        int x = (sw - W) / 2, y = bannerY(sw);
        var td = BreedingData.TIERS.get(tier - 1);
        int col = td.colour();
        g.pose().pushPose();
        g.setColor(1f, 1f, 1f, a);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        QfUi.panelSplit(g, x, y, W, H, col, QfUi.MAGENTA, pt);
        int cx = x + W / 2;                                                       // diamond stud on the top bezel
        g.fill(cx - 1, y - 1, cx + 1, y, 0xFFFFFFFF); g.fill(cx - 2, y, cx + 2, y + 2, 0xFFFFFFFF); g.fill(cx - 1, y + 2, cx + 1, y + 3, 0xFFFFFFFF);
        int alpha = Math.max(4, (int) (a * 255)) << 24;
        String name = area.toUpperCase();
        QfText.draw(g, font, name, x + 10, y + 12, 228, 9, QfText.Size.DEFAULT, alpha | col, QfText.CENTRE);
        boolean safe = name.endsWith("SAFE");   // the Rift's safe band says so instead of "DANGER TIER 2"
        QfText.draw(g, font, (safe ? "SAFE  ·  NO INSTABILITY BUILDS HERE" : "DANGER TIER " + tier + "  ·  RECOMMENDED: " + td.name().toUpperCase() + " GEAR"), x + 8, y + 24, 232, 7, QfText.Size.SMALL, alpha | 0x8090B0, QfText.CENTRE);
        int px = x + W / 2 - (7 * 10 - 2) / 2;
        for (int i = 0; i < 7; i++) QfUi.skull(g, px + i * 10, y + 38, !safe && i < tier, col, pt);
        // compact confirmation lower-left
        QfText.draw(g, font, "ENTERED " + name, 8, sh - 66, 192, 7, QfText.Size.SMALL, alpha | 0xE6E6EC);
        QfText.draw(g, font, safe ? "SAFE ZONE" : "DANGER TIER " + tier + " ZONE", 8, sh - 58, 192, 7, QfText.Size.SMALL, alpha | col);
        g.setColor(1f, 1f, 1f, 1f);
        g.pose().popPose();
    }
}
