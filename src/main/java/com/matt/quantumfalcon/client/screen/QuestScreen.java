package com.matt.quantumfalcon.client.screen;

import com.matt.quantumfalcon.quest.QuestSystem;
import com.matt.quantumfalcon.quest.QuestSystem.Quest;
import com.matt.quantumfalcon.quest.QuestSystem.QuestCategory;
import com.matt.quantumfalcon.quest.PlayerQuestData;
import com.matt.quantumfalcon.skill.PlayerSkillData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Quantum Quest Log - Cyberpunk journal with tabs and quest cards.
 * Press J to open. Tracks main story, side quests, and tutorials.
 */
public class QuestScreen extends Screen {
    // Colors - Cyberpunk theme
    private static final int COLOR_BACKGROUND = 0xDD0A0A14;
    private static final int COLOR_CYAN = 0xFF00FFFF;
    private static final int COLOR_MAGENTA = 0xFFFF00FF;
    private static final int COLOR_GOLD = 0xFFFFAA00;
    private static final int COLOR_GREEN = 0xFF00FF00;
    private static final int COLOR_RED = 0xFFFF5555;
    private static final int COLOR_PURPLE = 0xFFAA00FF;
    
    // Layout
    private static final int TAB_HEIGHT = 25;
    private static final int TAB_WIDTH = 90;
    private static final int CARD_HEIGHT = 100;
    private static final int CARD_MARGIN = 10;
    
    // State
    private final Player player;
    private QuestCategory selectedTab = QuestCategory.MAIN;
    private int scrollOffset = 0;
    private Quest hoveredQuest = null;
    
    public QuestScreen() {
        super(Component.literal("Quantum Quest Log").withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE));
        this.player = Minecraft.getInstance().player;
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dark background
        renderBackground(graphics, mouseX, mouseY, partialTick);
        
        // Main panel
        int panelX = 30;
        int panelY = 50;
        int panelWidth = this.width - 60;
        int panelHeight = this.height - 100;
        
        drawGlassmorphismPanel(graphics, panelX, panelY, panelWidth, panelHeight);
        
        // Title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, COLOR_CYAN);
        
        // Draw tabs
        drawTabs(graphics, panelX, panelY - TAB_HEIGHT, mouseX, mouseY);
        
        // Draw quest list
        drawQuestList(graphics, panelX + 10, panelY + 10, panelWidth - 20, panelHeight - 20, mouseX, mouseY);
        
        // Draw tooltip for hovered quest
        if (hoveredQuest != null) {
            drawQuestTooltip(graphics, hoveredQuest, mouseX, mouseY);
        }
        
        // Instructions
        graphics.drawString(this.font, "Scroll to navigate | Click to accept/track | ESC to close",
            this.width / 2 - 150, this.height - 25, 0xFF888888);
    }

    private void drawGlassmorphismPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        // Background
        graphics.fill(x, y, x + width, y + height, COLOR_BACKGROUND);
        
        // Border glow
        graphics.fill(x, y, x + width, y + 2, COLOR_CYAN);
        graphics.fill(x, y + height - 2, x + width, y + height, COLOR_CYAN);
        graphics.fill(x, y, x + 2, y + height, COLOR_CYAN);
        graphics.fill(x + width - 2, y, x + width, y + height, COLOR_CYAN);
    }

    private void drawTabs(GuiGraphics graphics, int startX, int startY, int mouseX, int mouseY) {
        int tabX = startX;
        
        QuestCategory[] tabs = {QuestCategory.MAIN, QuestCategory.SIDE, QuestCategory.TUTORIAL};
        String[] tabNames = {"Main Story", "Side Quests", "Tutorials"};
        int[] tabColors = {COLOR_PURPLE, COLOR_GOLD, COLOR_GREEN};
        
        for (int i = 0; i < tabs.length; i++) {
            boolean isSelected = tabs[i] == selectedTab;
            boolean isHovered = mouseX >= tabX && mouseX < tabX + TAB_WIDTH &&
                               mouseY >= startY && mouseY < startY + TAB_HEIGHT;
            
            // Tab background
            int bgColor = isSelected ? 0xCC002244 : (isHovered ? 0xAA001133 : 0x88000011);
            graphics.fill(tabX, startY, tabX + TAB_WIDTH, startY + TAB_HEIGHT, bgColor);
            
            // Tab border
            int borderColor = isSelected ? tabColors[i] : (isHovered ? 0xFF00AAAA : 0xFF333333);
            graphics.fill(tabX, startY, tabX + TAB_WIDTH, startY + 2, borderColor);
            if (isSelected) {
                graphics.fill(tabX, startY, tabX + 2, startY + TAB_HEIGHT, borderColor);
                graphics.fill(tabX + TAB_WIDTH - 2, startY, tabX + TAB_WIDTH, startY + TAB_HEIGHT, borderColor);
            }
            
            // Tab text
            int textColor = isSelected ? tabColors[i] : (isHovered ? 0xFFFFFFFF : 0xFFAAAAAA);
            graphics.drawCenteredString(this.font, tabNames[i], tabX + TAB_WIDTH / 2, startY + 8, textColor);
            
            tabX += TAB_WIDTH + 5;
        }
    }

    private void drawQuestList(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        // Get quests for selected tab
        List<Quest> quests = getQuestsForTab(selectedTab);
        
        // Enable scissor for scrolling
        graphics.enableScissor(x, y, x + width, y + height);
        
        hoveredQuest = null;
        int cardY = y - scrollOffset;
        
        for (Quest quest : quests) {
            if (cardY + CARD_HEIGHT > y && cardY < y + height) {
                // Check hover
                boolean hovered = mouseX >= x && mouseX < x + width &&
                                 mouseY >= cardY && mouseY < cardY + CARD_HEIGHT;
                if (hovered) {
                    hoveredQuest = quest;
                }
                
                drawQuestCard(graphics, quest, x, cardY, width, hovered, mouseX, mouseY);
            }
            cardY += CARD_HEIGHT + CARD_MARGIN;
        }
        
        // Show "No quests" if empty
        if (quests.isEmpty()) {
            graphics.drawCenteredString(this.font, "No quests available in this category",
                x + width / 2, y + height / 2, 0xFF888888);
        }
        
        graphics.disableScissor();
    }

    private void drawQuestCard(GuiGraphics graphics, Quest quest, int x, int y, int width, boolean hovered, int mouseX, int mouseY) {
        // Get player's progress for this quest
        PlayerQuestData playerData = player != null ? PlayerQuestData.get(player) : null;
        boolean isActive = playerData != null && playerData.isQuestActive(quest.id);
        boolean isCompleted = playerData != null && playerData.isQuestCompleted(quest.id);
        boolean canAccept = !isActive && !isCompleted && canAcceptQuest(quest);
        
        // Card background
        int bgColor = hovered ? 0xEE112233 : 0xCC0A0A14;
        if (isCompleted) bgColor = 0xCC002200;
        else if (isActive) bgColor = 0xCC002244;
        graphics.fill(x, y, x + width, y + CARD_HEIGHT, bgColor);
        
        // Border
        int borderColor = isCompleted ? COLOR_GREEN : (isActive ? COLOR_CYAN : (canAccept ? COLOR_GOLD : COLOR_RED));
        graphics.fill(x, y, x + width, y + 2, borderColor);
        graphics.fill(x, y, x + 2, y + CARD_HEIGHT, borderColor);
        graphics.fill(x + width - 2, y, x + width, y + CARD_HEIGHT, borderColor);
        graphics.fill(x, y + CARD_HEIGHT - 2, x + width, y + CARD_HEIGHT, borderColor);
        
        // Status icon
        String statusIcon = isCompleted ? "✓" : (isActive ? "◉" : (canAccept ? "+" : "🔒"));
        int iconColor = isCompleted ? COLOR_GREEN : (isActive ? COLOR_CYAN : (canAccept ? COLOR_GOLD : 0xFF666666));
        graphics.drawString(this.font, statusIcon, x + 8, y + 8, iconColor);
        
        // Quest title
        int titleColor = isCompleted ? COLOR_GREEN : (isActive ? COLOR_CYAN : 0xFFFFFFFF);
        graphics.drawString(this.font, quest.name, x + 25, y + 8, titleColor);
        
        // Quest type badge
        String typeBadge = "[" + getTypeShortName(quest.category) + "]";
        int badgeColor = getTypeColor(quest.category);
        graphics.drawString(this.font, typeBadge, x + width - 50, y + 8, badgeColor);
        
        // Description (truncated)
        String desc = quest.description;
        if (desc.length() > 60) desc = desc.substring(0, 57) + "...";
        graphics.drawString(this.font, desc, x + 10, y + 25, 0xFFAAAAAA);
        
        // Objectives progress
        int objY = y + 40;
        int maxObjectives = Math.min(3, quest.objectiveDescriptions.size());
        for (int i = 0; i < maxObjectives; i++) {
            String objDesc = quest.objectiveDescriptions.get(i);
            int currentProgress = isActive && playerData != null ? playerData.getObjectiveProgress(quest.id, i) : 0;
            boolean complete = currentProgress >= 1; // Simplified check
            
            String objText = (complete ? "✓ " : "○ ") + objDesc;
            if (objText.length() > 45) objText = objText.substring(0, 42) + "...";
            
            int objColor = complete ? COLOR_GREEN : 0xFFCCCCCC;
            graphics.drawString(this.font, objText, x + 15, objY, objColor);
            
            // Progress bar for active quests
            if (isActive && !complete && currentProgress > 0) {
                int barX = x + width - 80;
                int barWidth = 60;
                float progress = Math.min(1.0f, currentProgress / 10.0f); // Assume 10 is max for display
                
                graphics.fill(barX, objY, barX + barWidth, objY + 8, 0xFF333333);
                graphics.fill(barX, objY, barX + (int)(barWidth * progress), objY + 8, COLOR_CYAN);
                
                String progressText = currentProgress + "/" + 10;
                graphics.drawString(this.font, progressText, barX + barWidth + 5, objY, 0xFFFFFFFF);
            }
            
            objY += 12;
        }
        if (quest.objectiveDescriptions.size() > 3) {
            graphics.drawString(this.font, "+" + (quest.objectiveDescriptions.size() - 3) + " more...", x + 15, objY, 0xFF888888);
        }
        
        // Rewards preview
        graphics.drawString(this.font, "Rewards: " + quest.xpReward + " XP", x + 10, y + CARD_HEIGHT - 15, COLOR_GOLD);
        if (!quest.rewards.isEmpty()) {
            graphics.drawString(this.font, " + " + quest.rewards.size() + " items", x + 90, y + CARD_HEIGHT - 15, COLOR_GOLD);
        }
    }

    private void drawQuestTooltip(GuiGraphics graphics, Quest quest, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        
        // Title
        lines.add(Component.literal(quest.title()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        lines.add(Component.literal("[" + quest.type().name() + "]").withStyle(ChatFormatting.GRAY));
        lines.add(Component.empty());
        
        // Description
        lines.add(Component.literal(quest.description).withStyle(ChatFormatting.WHITE));
        lines.add(Component.empty());
        
        // Objectives
        lines.add(Component.literal("Objectives:").withStyle(ChatFormatting.YELLOW));
        for (String objDesc : quest.objectiveDescriptions) {
            lines.add(Component.literal("  • " + objDesc)
                .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.empty());
        
        // Rewards
        lines.add(Component.literal("Rewards:").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("  • " + quest.xpReward + " XP").withStyle(ChatFormatting.AQUA));
        for (ItemStack rewardStack : quest.rewards) {
            String itemName = rewardStack.getDescriptionId();
            lines.add(Component.literal("  • " + rewardStack.getCount() + "x " + itemName).withStyle(ChatFormatting.GREEN));
        }
        
        // Prerequisite
        if (quest.prerequisiteQuestId() != null) {
            Quest prereq = QuestSystem.getQuestDefinition(quest.prerequisiteQuestId());
            boolean hasPrereq = QuestSystem.hasCompletedQuest(player, quest.prerequisiteQuestId());
            lines.add(Component.empty());
            lines.add(Component.literal("Requires: " + (prereq != null ? prereq.title() : quest.prerequisiteQuestId()))
                .withStyle(hasPrereq ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        
        // Action hint
        PlayerQuestData questData = PlayerQuestData.get(player);
            boolean isActive = questData != null && questData.isQuestActive(quest.id);
            boolean isCompleted = questData != null && questData.isQuestCompleted(quest.id);
        
        lines.add(Component.empty());
        if (isCompleted) {
            lines.add(Component.literal("COMPLETED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else if (isActive) {
            lines.add(Component.literal("Click to track").withStyle(ChatFormatting.YELLOW));
        } else if (canAcceptQuest(quest)) {
            lines.add(Component.literal("Click to accept!").withStyle(ChatFormatting.GREEN));
        } else {
            lines.add(Component.literal("Requirements not met").withStyle(ChatFormatting.RED));
        }
        
        graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private List<Quest> getQuestsForTab(QuestCategory category) {
        return QuestSystem.getQuestsByCategory(category);
    }

    private boolean canAcceptQuest(Quest quest) {
        if (quest.prerequisiteQuestId() != null && !QuestSystem.hasCompletedQuest(player, quest.prerequisiteQuestId())) {
            return false;
        }
        return true;
    }

    private String getTypeShortName(QuestCategory category) {
        return switch (category) {
            case MAIN -> "MAIN";
            case SIDE -> "SIDE";
            case TUTORIAL -> "TUT";
            case CHALLENGE -> "CHAL";
        };
    }

    private int getTypeColor(QuestCategory category) {
        return switch (category) {
            case MAIN -> COLOR_PURPLE;
            case SIDE -> COLOR_GOLD;
            case TUTORIAL -> COLOR_GREEN;
            case CHALLENGE -> COLOR_RED;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check tab clicks
        int tabX = 30;
        int tabY = 50 - TAB_HEIGHT;
        QuestCategory[] tabs = {QuestCategory.MAIN, QuestCategory.SIDE, QuestCategory.TUTORIAL};
        
        for (QuestCategory tab : tabs) {
            if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH &&
                mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
                selectedTab = tab;
                scrollOffset = 0;
                return true;
            }
            tabX += TAB_WIDTH + 5;
        }
        
        // Check quest card clicks
        if (hoveredQuest != null && button == 0) {
            PlayerQuestData playerData = PlayerQuestData.get(player);
            boolean isActive = playerData != null && playerData.getQuestStatus(hoveredQuest.id) == PlayerQuestData.QuestStatus.ACTIVE;
            
            if (!isActive && canAcceptQuest(hoveredQuest)) {
                // Accept quest
                if (QuestSystem.acceptQuest(player, hoveredQuest)) {
                    player.displayClientMessage(
                        Component.literal("Quest accepted: " + hoveredQuest.title())
                            .withStyle(ChatFormatting.GREEN), false);
                }
            }
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, getQuestsForTab(selectedTab).size() * (CARD_HEIGHT + CARD_MARGIN) - (this.height - 150));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(scrollY * 30)));
        return true;
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
