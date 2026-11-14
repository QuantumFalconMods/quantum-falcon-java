package com.matt.quantumfalcon.client.screen;

import com.matt.quantumfalcon.entity.FalconCommand;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Falcon Command Screen - Radial/List menu for issuing commands to tamed falcons.
 * Press V to open. Cyberpunk themed with neon glow.
 */
public class FalconCommandScreen extends Screen {
    // Colors
    private static final int COLOR_BACKGROUND = 0xEE0A0A14;
    private static final int COLOR_CYAN = 0xFF00FFFF;
    private static final int COLOR_MAGENTA = 0xFFFF00FF;
    
    // Layout
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 35;
    private static final int BUTTON_SPACING = 8;
    
    // State
    private final Player player;
    private FalconCommand hoveredCommand = null;
    private FalconCommand currentCommand = FalconCommand.FOLLOW; // Default
    
    public FalconCommandScreen() {
        super(Component.literal("Falcon Commands").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA));
        this.player = Minecraft.getInstance().player;
        
        // Get current command from nearest owned falcon
        if (player instanceof net.minecraft.client.player.LocalPlayer localPlayer) {
            QuantumFalconEntity nearestFalcon = findNearestOwnedFalcon(localPlayer);
            if (nearestFalcon != null) {
                currentCommand = nearestFalcon.getCommand();
            }
        }
    }
    
    private QuantumFalconEntity findNearestOwnedFalcon(net.minecraft.client.player.LocalPlayer player) {
        if (player.level() == null) return null;
        
        double minDist = Double.MAX_VALUE;
        QuantumFalconEntity nearest = null;
        
        for (net.minecraft.world.entity.Entity entity : player.level().getEntitiesOfClass(
            QuantumFalconEntity.class, 
            player.getBoundingBox().inflate(32.0))) {
            
            if (entity instanceof QuantumFalconEntity falcon && 
                falcon.isTame() && 
                falcon.isOwnedBy(player)) {
                
                double dist = player.distanceToSqr(falcon);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = falcon;
                }
            }
        }
        
        return nearest;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent overlay
        renderBackground(graphics, mouseX, mouseY, partialTick);
        
        // Center panel
        int panelWidth = BUTTON_WIDTH + 40;
        int panelHeight = FalconCommand.values().length * (BUTTON_HEIGHT + BUTTON_SPACING) + 80;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        
        // Draw glassmorphism panel
        drawGlassmorphismPanel(graphics, panelX, panelY, panelWidth, panelHeight);
        
        // Title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 15, COLOR_CYAN);
        
        // Subtitle
        graphics.drawCenteredString(this.font, "Select a command for your falcon",
            this.width / 2, panelY + 30, 0xFF888888);
        
        // Draw command buttons
        int buttonX = panelX + 20;
        int buttonY = panelY + 50;
        
        hoveredCommand = null;
        
        for (FalconCommand command : FalconCommand.values()) {
            boolean isHovered = mouseX >= buttonX && mouseX < buttonX + BUTTON_WIDTH &&
                               mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT;
            boolean isCurrent = command == currentCommand;
            
            if (isHovered) {
                hoveredCommand = command;
            }
            
            drawCommandButton(graphics, command, buttonX, buttonY, isHovered, isCurrent);
            buttonY += BUTTON_HEIGHT + BUTTON_SPACING;
        }
        
        // Draw tooltip for hovered command
        if (hoveredCommand != null) {
            drawCommandTooltip(graphics, hoveredCommand, mouseX, mouseY);
        }
        
        // Instructions
        graphics.drawCenteredString(this.font, "Click to issue | ESC to close",
            this.width / 2, panelY + panelHeight - 20, 0xFF666666);
    }

    private void drawGlassmorphismPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        // Background
        graphics.fill(x, y, x + width, y + height, COLOR_BACKGROUND);
        
        // Border glow
        graphics.fill(x, y, x + width, y + 2, COLOR_CYAN);
        graphics.fill(x, y + height - 2, x + width, y + height, COLOR_CYAN);
        graphics.fill(x, y, x + 2, y + height, COLOR_CYAN);
        graphics.fill(x + width - 2, y, x + width, y + height, COLOR_CYAN);
        
        // Inner glow
        graphics.fill(x + 2, y + 2, x + width - 2, y + 4, 0x4400FFFF);
    }

    private void drawCommandButton(GuiGraphics graphics, FalconCommand command, int x, int y, boolean hovered, boolean current) {
        // Button background
        int bgColor = current ? 0xCC004444 : (hovered ? 0xCC002244 : 0xAA111122);
        graphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, bgColor);
        
        // Border
        int borderColor = current ? COLOR_CYAN : (hovered ? COLOR_MAGENTA : 0xFF333333);
        graphics.fill(x, y, x + BUTTON_WIDTH, y + 2, borderColor);
        graphics.fill(x, y + BUTTON_HEIGHT - 2, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, borderColor);
        graphics.fill(x, y, x + 2, y + BUTTON_HEIGHT, borderColor);
        graphics.fill(x + BUTTON_WIDTH - 2, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, borderColor);
        
        // Icon
        graphics.drawString(this.font, command.getIcon(), x + 10, y + (BUTTON_HEIGHT - 8) / 2, 0xFFFFFFFF);
        
        // Command name
        int textColor = command.getColor().getColor() != null ? command.getColor().getColor() : 0xFFFFFFFF;
        graphics.drawString(this.font, command.getName(), x + 35, y + 8, textColor);
        
        // Short description
        String desc = command.getDescription();
        if (desc.length() > 30) desc = desc.substring(0, 27) + "...";
        graphics.drawString(this.font, desc, x + 35, y + 20, 0xFF888888);
        
        // Current indicator
        if (current) {
            graphics.drawString(this.font, "◄ ACTIVE", x + BUTTON_WIDTH - 55, y + (BUTTON_HEIGHT - 8) / 2, COLOR_CYAN);
        }
    }

    private void drawCommandTooltip(GuiGraphics graphics, FalconCommand command, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        
        lines.add(Component.literal(command.getIcon() + " " + command.getName())
            .withStyle(command.getColor(), ChatFormatting.BOLD));
        lines.add(Component.empty());
        lines.add(Component.literal(command.getDescription()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.empty());
        
        // Command-specific details
        switch (command) {
            case FOLLOW -> {
                lines.add(Component.literal("• Falcon stays within 5 blocks").withStyle(ChatFormatting.WHITE));
                lines.add(Component.literal("• Teleports if too far").withStyle(ChatFormatting.WHITE));
            }
            case STAY -> {
                lines.add(Component.literal("• Guards 16 block radius").withStyle(ChatFormatting.WHITE));
                lines.add(Component.literal("• Attacks hostiles automatically").withStyle(ChatFormatting.WHITE));
            }
            case WANDER -> {
                lines.add(Component.literal("• Explores within 32 blocks").withStyle(ChatFormatting.WHITE));
                lines.add(Component.literal("• Returns if called").withStyle(ChatFormatting.WHITE));
            }
            case ATTACK -> {
                lines.add(Component.literal("• Targets all hostiles in range").withStyle(ChatFormatting.WHITE));
                lines.add(Component.literal("• +50% damage, +30% speed").withStyle(ChatFormatting.RED));
            }
            case GATHER -> {
                lines.add(Component.literal("• Collects items within 20 blocks").withStyle(ChatFormatting.WHITE));
                lines.add(Component.literal("• Brings items to owner").withStyle(ChatFormatting.WHITE));
            }
            case SCOUT -> {
                lines.add(Component.literal("• Flies to Y=200 and surveys").withStyle(ChatFormatting.WHITE));
                lines.add(Component.literal("• Marks structures & mobs").withStyle(ChatFormatting.WHITE));
            }
            case ABILITY -> {
                lines.add(Component.literal("• Quantum Burst: AoE damage").withStyle(ChatFormatting.LIGHT_PURPLE));
                lines.add(Component.literal("• 60 second cooldown").withStyle(ChatFormatting.RED));
            }
        }
        
        lines.add(Component.empty());
        lines.add(Component.literal("Click to activate").withStyle(ChatFormatting.YELLOW));
        
        graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredCommand != null && button == 0) {
            // Execute command
            executeCommand(hoveredCommand);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void executeCommand(FalconCommand command) {
        currentCommand = command;
        
        // Display feedback
        player.displayClientMessage(
            Component.literal("Falcon command: " + command.getName())
                .withStyle(command.getColor()), true);
        
        // Send packet to server to update falcon AI
        if (player instanceof net.minecraft.client.player.LocalPlayer localPlayer) {
            // Find nearest owned falcon
            QuantumFalconEntity nearestFalcon = findNearestOwnedFalcon(localPlayer);
            if (nearestFalcon != null) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.matt.quantumfalcon.network.FalconCommandPacket.FalconCommandPayload(
                        nearestFalcon.getId(),
                        command.name()
                    )
                );
            } else {
                player.displayClientMessage(
                    Component.literal("No owned falcon nearby!")
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
            }
        }
        
        // Play sound effect
        // player.playSound(ModSounds.FALCON_COMMAND.get(), 1.0f, 1.0f);
        
        // Close screen
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            this.onClose();
            return true;
        }
        
        // Number keys for quick select (1-7)
        if (keyCode >= 49 && keyCode <= 55) { // Keys 1-7
            int index = keyCode - 49;
            FalconCommand[] commands = FalconCommand.values();
            if (index < commands.length) {
                executeCommand(commands[index]);
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
