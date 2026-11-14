package com.matt.quantumfalcon.client.hud;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.network.InstabilityPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * HUD overlay for displaying instability meter and boss phase indicators.
 * The Instability meter sits top-left at (10, 10); the boss phase plate stays top-right.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public class InstabilityHudOverlay {
    
    // Colors
    private static final int COLOR_EXTREME = 0xFFFF0000;    // Red (tier 9-10)
    
    
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "instability");

    /** Own GUI layer so the meter shows in every game mode (the old hook rode the health layer, which creative skips). */
    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, LAYER, (g, delta) -> render(g, delta.getGameTimeDeltaPartialTick(false)));
    }

    private static void render(GuiGraphics graphics, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        
        Font font = mc.font;
        int tier = InstabilityPackets.InstabilityClientData.getCurrentTier();
        boolean inRift = InstabilityPackets.InstabilityClientData.getZoneTier() >= 0;
        if (tier > 0 || inRift) drawInstabilityMeter(graphics, font, tier, pt);
        
        // Draw arena warning if active
        if (InstabilityPackets.ArenaWarningClientData.hasActiveWarning()) {
            drawArenaWarning(graphics, font, mc.getWindow().getGuiScaledWidth());
        }
        
        // Draw boss phase indicator if in boss fight
        int bossPhase = InstabilityPackets.BossPhaseClientData.getLastPhase();
        if (bossPhase > 0) {
            drawBossPhaseIndicator(graphics, font, bossPhase, 
                InstabilityPackets.BossPhaseClientData.getLastBossType(),
                mc.getWindow().getGuiScaledWidth());
        }
    }
    
    /** Status word under the bar. */
    static String status(int tier) {
        if (tier <= 2) return "STABLE";
        if (tier <= 5) return "FLICKERING";
        if (tier <= 8) return "PULSING";
        return "CRITICAL";
    }

    /**
     * Instability meter, top-left 120x40 at (10, 10) (gui_specs.md section 11: separate from the danger banner, tiers 1-10,
     * green / yellow / orange / red). Chrome is the shared 9-slice with the crack glow in the tier colour; the fill pulses.
     */
    private static void drawInstabilityMeter(GuiGraphics g, Font font, int tier, float pt) {
        float progress = InstabilityPackets.InstabilityClientData.getCurrentProgress();
        int x = 10, y = 10, w = 120, h = 40, col = getTierColor(tier);
        com.matt.quantumfalcon.client.gui.QfUi.panel(g, x, y, w, h, col & 0xFFFFFF, pt);
        com.matt.quantumfalcon.client.gui.QfText.draw(g, font, "INSTABILITY", 16, 14, 70, 9, com.matt.quantumfalcon.client.gui.QfText.Size.DEFAULT, 0xFFE6E6EC);
        com.matt.quantumfalcon.client.gui.QfText.draw(g, font, Math.round(InstabilityPackets.InstabilityClientData.getInstability()) + "%", 88, 14, 36, 9, com.matt.quantumfalcon.client.gui.QfText.Size.DEFAULT, col, com.matt.quantumfalcon.client.gui.QfText.RIGHT);
        float level = Math.min(1f, InstabilityPackets.InstabilityClientData.getInstability() / 100f);
        com.matt.quantumfalcon.client.gui.QfUi.bar(g, x + 8, y + 14, w - 16, 5, level, col & 0xFFFFFF, -1f, pt);
        // the safe-time budget: the zone (0 near your portal) and how long until effects start at this rate
        int zone = InstabilityPackets.InstabilityClientData.getZoneTier(), safe = InstabilityPackets.InstabilityClientData.getSafeSeconds();
        float pct = InstabilityPackets.InstabilityClientData.getInstability();
        String st; int stCol = col;
        if (pct >= 50f) st = status(tier);
        else if (zone >= 0 && safe < 0) { st = zone == 0 ? "SAFE" : "STABLE"; stCol = 0xFF6CFF8C; }   // no second number next to the danger tier
        else if (safe >= 0) { st = "SAFE " + safe / 60 + ":" + String.format("%02d", safe % 60); stCol = safe < 30 ? 0xFFFFC040 : 0xFFB8FFC8; }
        else st = status(tier);
        if (tier < 9 || (System.currentTimeMillis() / 500) % 2 == 0) com.matt.quantumfalcon.client.gui.QfText.draw(g, font, st, 16, 32, 56, 7, com.matt.quantumfalcon.client.gui.QfText.Size.SMALL, stCol);
        Minecraft mc = Minecraft.getInstance();
        String biome = InstabilityPackets.InstabilityClientData.getCurrentBiome();
        if ((biome == null || biome.isEmpty()) && mc.level != null && mc.player != null)
            biome = mc.level.getBiome(mc.player.blockPosition()).unwrapKey().map(k -> {
                StringBuilder b = new StringBuilder();
                for (String word : k.location().getPath().split("_")) if (!word.isEmpty()) b.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
                return b.toString().trim(); }).orElse("");
        if (biome != null && !biome.isEmpty())
            // R3 shots: the 58-px slot next to the status word cut long Echo Rift names off ("Probability De..."); give the
            // biome its own line just under the panel, wide enough for every biome name
            com.matt.quantumfalcon.client.gui.QfText.draw(g, font, biome, x + 2, y + h + 3, 200, 7, com.matt.quantumfalcon.client.gui.QfText.Size.SMALL, 0xFFA8B0C8);
    }


    private static void drawArenaWarning(GuiGraphics graphics, Font font, int screenWidth) {
        String warning = InstabilityPackets.ArenaWarningClientData.getCurrentWarning();
        
        int textWidth = font.width(warning);
        int x = (screenWidth - textWidth) / 2;
        int y = 50;
        
        // Pulsing background
        long time = System.currentTimeMillis();
        float pulse = (float) Math.sin(time * 0.02) * 0.3f + 0.7f;
        int bgAlpha = (int) (pulse * 200);
        
        graphics.fill(x - 10, y - 5, x + textWidth + 10, y + 15, (bgAlpha << 24) | 0x00AA0000);
        graphics.drawCenteredString(font, warning, screenWidth / 2, y, 0xFFFF4444);
    }
    
    private static void drawBossPhaseIndicator(GuiGraphics graphics, Font font, int phase, String bossType, int screenWidth) {
        int x = screenWidth - 130;
        int y = 10;
        int width = 120;
        int height = 30;
        
        // Background
        graphics.fill(x, y, x + width, y + height, 0xCC0A0A14);
        
        // Border (color based on phase)
        int phaseColor = switch (phase) {
            case 1 -> 0xFF00FFFF;   // Cyan
            case 2 -> 0xFFFFFF00;   // Yellow
            case 3 -> 0xFFFF8800;   // Orange
            default -> 0xFFFF0000;  // Red
        };
        graphics.fill(x, y, x + width, y + 1, phaseColor);
        graphics.fill(x, y + height - 1, x + width, y + height, phaseColor);
        
        // Boss name
        String displayName = bossType.replace("_", " ");
        if (displayName.length() > 12) displayName = displayName.substring(0, 10) + "..";
        graphics.drawString(font, displayName, x + 5, y + 3, 0xFFFFFFFF, false);
        
        // Phase indicator
        String phaseText = "PHASE " + phase;
        graphics.drawString(font, phaseText, x + 5, y + 15, phaseColor, false);
        
        // Phase dots
        int dotX = x + width - 40;
        for (int i = 1; i <= 4; i++) {
            int dotColor = i <= phase ? phaseColor : 0xFF333333;
            graphics.fill(dotX, y + 18, dotX + 6, y + 24, dotColor);
            dotX += 10;
        }
    }
    
    private static int getTierColor(int tier) {
        if (tier <= 2) return 0xFF00FF00;
        if (tier <= 5) return 0xFFFFFF00;
        if (tier <= 8) return 0xFFFF8800;
        return COLOR_EXTREME;
    }
}
