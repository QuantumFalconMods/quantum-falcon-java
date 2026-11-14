package com.matt.quantumfalcon.client.gui;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.network.FalconCommandPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Falcon Command Radial Menu for Quantum Falcon.
 * Allows quick access to falcon commands via radial menu.
 */
public class FalconCommandScreen extends Screen {

    private static final int RADIAL_RADIUS = 60;
    private static final int RADIAL_TEXTURE_SIZE = 160;
    private static final int ICON_SIZE = 24;

    private final QuantumFalconEntity targetFalcon;
    private int selectedCommand = -1;

    /** Mirrors the server-side entity.FalconCommand names (the packet sends name()). */
    public enum FalconCommand {
        FOLLOW("follow", 0xFF00FF00),
        STAY("stay", 0xFFFFFF00),
        WANDER("wander", 0xFF88DD88),
        ATTACK("attack", 0xFFFF0000),
        GATHER("gather", 0xFFFFAA00),
        SCOUT("scout", 0xFF00AAFF),
        ABILITY("ability", 0xFFCC66FF);

        public final String key;
        public final int color;

        FalconCommand(String key, int color) { this.key = key; this.color = color; }
        public Component label() { return Component.translatable("gui.quantumfalcon.command." + key); }
        public Component description() { return Component.translatable("gui.quantumfalcon.command." + key + ".desc"); }
    }

    public FalconCommandScreen(QuantumFalconEntity falcon) {
        super(Component.translatable("gui.quantumfalcon.falcon_commands"));
        this.targetFalcon = falcon;
    }

    /**
     * Find the nearest owned falcon for the player
     */
    public static QuantumFalconEntity findNearestOwnedFalcon() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;
        
        AABB searchBox = mc.player.getBoundingBox().inflate(50);
        List<Entity> entities = mc.level.getEntities(mc.player, searchBox, 
            e -> e instanceof QuantumFalconEntity falcon && falcon.isOwnedBy(mc.player));
        
        QuantumFalconEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (Entity entity : entities) {
            double dist = entity.distanceToSqr(mc.player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = (QuantumFalconEntity) entity;
            }
        }
        
        return nearest;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent background
        renderBackground(graphics, mouseX, mouseY, partialTick);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        // Scale radial to fit screen; leave room for instructions and description
        int radialSize = Math.min(RADIAL_TEXTURE_SIZE, Math.min(this.width - 40, this.height - 120));
        int halfRadial = radialSize / 2;
        float scale = radialSize / (float) RADIAL_TEXTURE_SIZE;
        int effectiveRadius = (int) (RADIAL_RADIUS * scale);
        
        // Dark semi-transparent radial area (no missing texture)
        graphics.fill(centerX - halfRadial, centerY - halfRadial, centerX + halfRadial, centerY + halfRadial, 0xDD000000);
        graphics.renderOutline(centerX - halfRadial, centerY - halfRadial, radialSize, radialSize, 0xFF6600CC);
        
        // Calculate mouse angle from center
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.atan2(dy, dx);
        
        // Normalize angle to 0-2π
        if (angle < 0) angle += Math.PI * 2;
        
        // Determine selected command based on angle
        FalconCommand[] commands = FalconCommand.values();
        double sectorAngle = Math.PI * 2 / commands.length;
        
        if (distance > 20 && distance < effectiveRadius + (int)(40 * scale)) {
            selectedCommand = (int) ((angle + sectorAngle / 2) / sectorAngle) % commands.length;
        } else {
            selectedCommand = -1;
        }
        
        // Draw command options in radial layout
        for (int i = 0; i < commands.length; i++) {
            FalconCommand cmd = commands[i];
            double cmdAngle = i * sectorAngle;
            
            int iconX = centerX + (int) (Math.cos(cmdAngle) * effectiveRadius) - ICON_SIZE / 2;
            int iconY = centerY + (int) (Math.sin(cmdAngle) * effectiveRadius) - ICON_SIZE / 2;
            
            // Draw selection highlight
            if (i == selectedCommand) {
                graphics.fill(iconX - 2, iconY - 2, iconX + ICON_SIZE + 2, iconY + ICON_SIZE + 2, 
                    cmd.color | 0x80000000);
            }
            
            // Draw icon background and colored icon (no missing texture)
            int bgColor = i == selectedCommand ? 0xFFFFFFFF : 0xAA000000;
            graphics.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, bgColor);
            graphics.fill(iconX + 2, iconY + 2, iconX + ICON_SIZE - 2, iconY + ICON_SIZE - 2, cmd.color | 0xFF000000);
            
            // Draw command name if selected
            if (i == selectedCommand) {
                int textRadius = effectiveRadius + (int)(35 * scale);
                int textX = centerX + (int) (Math.cos(cmdAngle) * textRadius);
                int textY = centerY + (int) (Math.sin(cmdAngle) * textRadius);
                graphics.drawCenteredString(font, cmd.label(), textX, textY, 0xFFFFFF);
            }
        }
        
        // Draw center falcon name
        if (targetFalcon != null) {
            String falconName = targetFalcon.hasCustomName() ? 
                targetFalcon.getCustomName().getString() : "Quantum Falcon";
            graphics.drawCenteredString(font, falconName, centerX, centerY - 5, 0xFFFFFF);
            
            // Current command
            String currentCmd = "Mode: " + getCurrentCommandName();
            graphics.drawCenteredString(font, currentCmd, centerX, centerY + 8, 0xCCCCCC);
            // Variant abilities + cooldown (top of screen)
            var ab = targetFalcon.ability();
            graphics.drawCenteredString(font, ab.variantName(), centerX, 8, ab.color);
            graphics.drawCenteredString(font, Component.translatable("gui.quantumfalcon.ability.passive", ab.passiveName()), centerX, 20, 0xDDDDDD);
            int cd = targetFalcon.getAbilityCooldown();
            Component state = cd > 0 ? Component.translatable("gui.quantumfalcon.ability.cooldown", (cd + 19) / 20) : Component.translatable("gui.quantumfalcon.ability.ready");
            graphics.drawCenteredString(font, Component.translatable("gui.quantumfalcon.ability.active", ab.activeName(), state), centerX, 32, cd > 0 ? 0xFF8888 : 0x88FF88);
        }
        
        // Instructions at bottom (body text)
        int instructionY1 = this.height - 40;
        int instructionY2 = this.height - 22;
        graphics.drawCenteredString(font, "Click to select command", centerX, instructionY1, 0xCCCCCC);
        graphics.drawCenteredString(font, "Right-click or ESC to cancel", centerX, instructionY2, 0xCCCCCC);
        
        // Selected command description (above instructions, within bounds)
        if (selectedCommand >= 0 && selectedCommand < commands.length) {
            FalconCommand selectedCmd = commands[selectedCommand];
            int descY = Math.min(centerY + (int)(75 * scale), this.height - 52);
            Component desc = selectedCmd == FalconCommand.ABILITY && targetFalcon != null
                    ? targetFalcon.ability().activeDesc() : selectedCmd.description();
            graphics.drawCenteredString(font, desc, centerX, descY, 0xFFFFFF);
        }
    }

    private String getCurrentCommandName() {
        if (targetFalcon == null) return "Unknown";
        return targetFalcon.getCommandMode().name();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && selectedCommand >= 0) {
            // Send command to server
            sendCommand(FalconCommand.values()[selectedCommand]);
            onClose();
            return true;
        } else if (button == 1) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendCommand(FalconCommand command) {
        if (targetFalcon == null) return;
        
        int targetEntityId = 0;
        
        // For attack command, get the player's looking target
        if (command == FalconCommand.ATTACK) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.crosshairPickEntity != null) {
                targetEntityId = mc.crosshairPickEntity.getId();
            }
        }
        
        // Use new FalconCommandPacket format
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
            new com.matt.quantumfalcon.network.FalconCommandPacket.FalconCommandPayload(
                targetFalcon.getId(),
                command.name()
            )
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Quick number keys for commands
        if (keyCode >= 49 && keyCode <= 55) { // Keys 1-7
            int cmdIndex = keyCode - 49;
            if (cmdIndex < FalconCommand.values().length) {
                sendCommand(FalconCommand.values()[cmdIndex]);
                onClose();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
