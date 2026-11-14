package com.matt.quantumfalcon.client.screen;

import com.matt.quantumfalcon.QuantumFalcon;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Custom title screen for Quantum Falcon mod.
 * Replaces the vanilla Minecraft title screen with cyberpunk quantum-themed branding.
 * Pattern: Same approach as ATM10, RLCraft, and other major modpacks.
 */
@OnlyIn(Dist.CLIENT)
public class QuantumFalconTitleScreen extends Screen {

    private static final ResourceLocation LOGO_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        QuantumFalcon.MOD_ID, "textures/gui/title_logo.png");
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        QuantumFalcon.MOD_ID, "textures/gui/title_background.png");
    
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;
    
    private int tickCount = 0;

    public QuantumFalconTitleScreen() {
        super(Component.literal("Quantum Falcon"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonStartY = this.height / 2 + 10;
        
        // Row 1: Singleplayer / Multiplayer
        this.addRenderableWidget(Button.builder(
            Component.translatable("menu.singleplayer"),
            btn -> this.minecraft.setScreen(new SelectWorldScreen(this))
        ).bounds(centerX - BUTTON_WIDTH - 4, buttonStartY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        this.addRenderableWidget(Button.builder(
            Component.translatable("menu.multiplayer"),
            btn -> this.minecraft.setScreen(new JoinMultiplayerScreen(this))
        ).bounds(centerX + 4, buttonStartY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        // Row 2: Mods / Options
        this.addRenderableWidget(Button.builder(
            Component.literal("Mods"),
            btn -> this.minecraft.setScreen(new net.neoforged.neoforge.client.gui.ModListScreen(this))
        ).bounds(centerX - BUTTON_WIDTH - 4, buttonStartY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        this.addRenderableWidget(Button.builder(
            Component.translatable("menu.options"),
            btn -> this.minecraft.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(this, this.minecraft.options))
        ).bounds(centerX + 4, buttonStartY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        // Row 3: Language / Quit
        this.addRenderableWidget(Button.builder(
            Component.translatable("options.language"),
            btn -> this.minecraft.setScreen(new net.minecraft.client.gui.screens.options.LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager()))
        ).bounds(centerX - BUTTON_WIDTH - 4, buttonStartY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        this.addRenderableWidget(Button.builder(
            Component.translatable("menu.quit"),
            btn -> this.minecraft.stop()
        ).bounds(centerX + 4, buttonStartY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void tick() {
        tickCount++;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float time = (tickCount + partialTick) * 0.02f;
        
        // === BACKGROUND ===
        renderBackground(graphics, time);
        
        // === LOGO / TITLE ===
        renderTitle(graphics, time);
        
        // === VERSION INFO (bottom-left, like ATM) ===
        renderVersionInfo(graphics);
        
        // === BUTTONS ===
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // === SUBTITLE ===
        String subtitle = "\u00a77The Rift has been midnight for an age.";
        int subtitleWidth = this.font.width(subtitle);
        graphics.drawString(this.font, subtitle, 
            (this.width - subtitleWidth) / 2, this.height / 2 - 5, 0xAAAAAAFF, true);
    }

    private void renderBackground(GuiGraphics graphics, float time) {
        // Dark gradient background with subtle animated color
        int w = this.width;
        int h = this.height;
        
        // Base dark background
        graphics.fill(0, 0, w, h, 0xFF0A0A14);
        
        // Animated gradient overlay (cyberpunk purple -> dark blue)
        float pulse = (float)(Math.sin(time * 0.5) * 0.5 + 0.5);
        int topColor = blendColor(0xFF1A0A2E, 0xFF0A1A3E, pulse);
        int bottomColor = blendColor(0xFF050510, 0xFF0A0520, pulse);
        graphics.fillGradient(0, 0, w, h, topColor, bottomColor);
        
        // Horizontal scan lines (cyberpunk effect)
        for (int y = 0; y < h; y += 3) {
            int alpha = (y % 6 == 0) ? 0x08 : 0x04;
            graphics.fill(0, y, w, y + 1, (alpha << 24) | 0x00FFFF);
        }
        
        // Floating particles (quantum effect)
        for (int i = 0; i < 30; i++) {
            float px = (float)(Math.sin(time * 0.3 + i * 1.7) * 0.5 + 0.5) * w;
            float py = (float)(Math.cos(time * 0.2 + i * 2.3) * 0.5 + 0.5) * h;
            int size = (i % 3 == 0) ? 2 : 1;
            int particleAlpha = (int)(80 + 40 * Math.sin(time * 2 + i));
            int color = (i % 2 == 0) ? 0x00FFFF : 0xBB00FF;
            graphics.fill((int)px, (int)py, (int)px + size, (int)py + size, 
                (particleAlpha << 24) | (color & 0x00FFFFFF));
        }
        
        // Center glow (behind logo)
        int glowCenterX = w / 2;
        int glowCenterY = h / 2 - 50;
        int glowRadius = 120;
        int glowAlpha = (int)(20 + 10 * Math.sin(time));
        for (int r = glowRadius; r > 0; r -= 4) {
            int a = (int)(glowAlpha * ((float)r / glowRadius));
            graphics.fill(glowCenterX - r, glowCenterY - r/2, glowCenterX + r, glowCenterY + r/2,
                (a << 24) | 0x4400AA);
        }
    }

    private void renderTitle(GuiGraphics graphics, float time) {
        int centerX = this.width / 2;
        int titleY = this.height / 2 - 80;
        
        // "QUANTUM" in large cyan text
        String quantum = "QUANTUM";
        int qWidth = this.font.width(quantum) * 3; // Scaled up
        
        // Shadow
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - qWidth / 2.0f + 2, titleY + 2, 0);
        graphics.pose().scale(3f, 3f, 1f);
        graphics.drawString(this.font, quantum, 0, 0, 0x33000000, false);
        graphics.pose().popPose();
        
        // Main text with animated glow
        float glowPulse = (float)(Math.sin(time * 1.5) * 0.3 + 0.7);
        int cyanR = (int)(0 * glowPulse);
        int cyanG = (int)(255 * glowPulse);
        int cyanB = 255;
        int cyanColor = 0xFF000000 | (cyanR << 16) | (cyanG << 8) | cyanB;
        
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - qWidth / 2.0f, titleY, 0);
        graphics.pose().scale(3f, 3f, 1f);
        graphics.drawString(this.font, quantum, 0, 0, cyanColor, true);
        graphics.pose().popPose();
        
        // "FALCON" below in hot pink/magenta
        String falcon = "FALCON";
        int fWidth = this.font.width(falcon) * 3;
        
        // Shadow
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - fWidth / 2.0f + 2, titleY + 30, 0);
        graphics.pose().scale(3f, 3f, 1f);
        graphics.drawString(this.font, falcon, 0, 2, 0x33000000, false);
        graphics.pose().popPose();
        
        // Main text
        int pinkColor = 0xFFFF1493; // Hot pink
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - fWidth / 2.0f, titleY + 28, 0);
        graphics.pose().scale(3f, 3f, 1f);
        graphics.drawString(this.font, falcon, 0, 0, pinkColor, true);
        graphics.pose().popPose();
        
        // Decorative line under title
        int lineY = titleY + 58;
        float lineAlpha = (float)(0.4 + 0.2 * Math.sin(time * 2));
        int lineColor = (int)(lineAlpha * 255) << 24 | 0x00CCFF;
        graphics.fill(centerX - 100, lineY, centerX + 100, lineY + 1, lineColor);
        graphics.fill(centerX - 60, lineY + 2, centerX + 60, lineY + 3, lineColor & 0x80FFFFFF);
    }

    private void renderVersionInfo(GuiGraphics graphics) {
        int y = this.height - 10;
        
        graphics.drawString(this.font, "Quantum Falcon v1.0.0", 4, y - 30, 0xFFAAAAAA, true);
        graphics.drawString(this.font, "Minecraft 1.21.1 | NeoForge", 4, y - 20, 0xFF888888, true);
        
        int modCount = net.neoforged.fml.ModList.get().size();
        graphics.drawString(this.font, modCount + " Mods Loaded", 4, y - 10, 0xFF888888, true);
        
        // Copyright on bottom-right
        String copyright = "Copyright Matt. All rights reserved.";
        int copyrightWidth = this.font.width(copyright);
        graphics.drawString(this.font, copyright, this.width - copyrightWidth - 4, y - 10, 0xFF666666, true);
    }

    private static int blendColor(int color1, int color2, float factor) {
        int a1 = (color1 >> 24) & 0xFF, r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF, r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        int a = (int)(a1 + (a2 - a1) * factor);
        int r = (int)(r1 + (r2 - r1) * factor);
        int g = (int)(g1 + (g2 - g1) * factor);
        int b = (int)(b1 + (b2 - b1) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Don't close title screen with ESC
    }
}
