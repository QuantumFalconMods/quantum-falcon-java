package com.matt.quantumfalcon.client.gui;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.network.ClientQuestData;
import com.matt.quantumfalcon.quest.QuestSystem;
import com.matt.quantumfalcon.quest.QuestSystem.Quest;
import com.matt.quantumfalcon.quest.QuestSystem.QuestCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quest Log GUI Screen for Quantum Falcon.
 * Displays active, completed, and available quests.
 */
public class QuestLogScreen extends Screen {

    private static final int DESIRED_WIDTH = 280;
    private static final int DESIRED_HEIGHT = 320;

    private int panelWidth;
    private int panelHeight;
    private int guiLeft;
    private int guiTop;
    private int scrollOffset = 0;
    private Quest selectedQuest = null;
    private QuestTab currentTab = QuestTab.ACTIVE;
    private Button acceptButton;

    public enum QuestTab {
        ACTIVE("Active"),
        AVAILABLE("Available"),
        COMPLETED("Completed");

        public final String displayName;
        QuestTab(String displayName) {
            this.displayName = displayName;
        }
    }

    public QuestLogScreen() {
        super(Component.translatable("gui.quantumfalcon.quest_log"));
    }

    @Override
    protected void init() {
        super.init();
        this.panelWidth = Math.min(DESIRED_WIDTH, this.width - 40);
        this.panelHeight = Math.min(DESIRED_HEIGHT, this.height - 40);
        this.guiLeft = (this.width - panelWidth) / 2;
        this.guiTop = (this.height - panelHeight) / 2;

        // Add tab buttons (within panel)
        int tabX = guiLeft + 10;
        int tabY = guiTop + 20;
        int tabWidth = Math.min(80, (panelWidth - 40) / 3);
        
        for (QuestTab tab : QuestTab.values()) {
            // the label is drawn by render (gold with a marker on the selected tab), so the button carries none
            addRenderableWidget(Button.builder(Component.empty(), button -> {
                if (currentTab != tab) QfUi.uiSound("ui_tab");
                currentTab = tab;
                selectedQuest = null;
                scrollOffset = 0;
            }).bounds(tabX, tabY, tabWidth, 20).createNarration(n -> Component.literal(tab.displayName + " tab")).build());
            tabX += tabWidth + 5;
        }
        // accept a side quest from the Available tab (server validates via QuestAcceptPacket)
        acceptButton = addRenderableWidget(Button.builder(Component.literal("Accept"), b -> {
            if (selectedQuest != null) net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.matt.quantumfalcon.network.QuestAcceptPacket(selectedQuest.id));
        }).bounds(guiLeft + panelWidth - 70, guiTop + panelHeight - 28, 60, 20).build());
        acceptButton.visible = false;
    }

    /** Dev smoke helper: select a tab and quest as if clicked. */
    public void devSelect(QuestTab tab, String questId) {
        currentTab = tab;
        selectedQuest = questId == null ? null : QuestSystem.getQuest(questId);
    }

    /** Dev smoke helper: GUI-space centre of a quest's list entry on the current tab (null if not listed or scrolled off). */
    public int[] devEntryCenter(String questId) {
        List<Quest> quests = getQuestsForTab();
        int listX = guiLeft + 10, listY = guiTop + 48, listWidth = Math.min(120, panelWidth / 3), entryHeight = 18;
        int maxVisible = Math.max(1, (panelHeight - 62) / entryHeight);
        for (int i = scrollOffset; i < Math.min(scrollOffset + maxVisible, quests.size()); i++)
            if (quests.get(i).id.equals(questId)) return new int[]{listX + listWidth / 2, listY + (i - scrollOffset) * entryHeight + entryHeight / 2};
        return null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (acceptButton != null) {
            acceptButton.visible = currentTab == QuestTab.AVAILABLE && selectedQuest != null
                && !ClientQuestData.getActiveQuests().contains(selectedQuest.id) && !ClientQuestData.getCompletedQuests().contains(selectedQuest.id);
        }
        super.render(graphics, mouseX, mouseY, partialTick);   // background + panel (renderBackground) and buttons first, text on top
        
        // Draw title (cyan)
        graphics.drawCenteredString(font, title, guiLeft + panelWidth / 2, guiTop + 6, 0x00FFFF);
        
        // Tab labels. the selected tab is unmistakable - a filled accent bar under it, an accent outline and a gold label
        int tabX = guiLeft + 10;
        int tabY = guiTop + 20;
        int tabWidth = Math.min(80, (panelWidth - 40) / 3);
        for (QuestTab tab : QuestTab.values()) {
            int cx = tabX + tabWidth / 2;
            boolean sel = tab == currentTab;
            if (sel) {
                graphics.renderOutline(tabX - 1, tabY - 1, tabWidth + 2, 22, 0xFF00FFCC);
                graphics.fill(tabX, tabY + 21, tabX + tabWidth, tabY + 24, 0xFF00FFCC);
            }
            graphics.drawCenteredString(font, sel ? "\u25B6 " + tab.displayName : tab.displayName, cx, tabY + 6, sel ? 0xFFDD33 : 0xC8C8C8);
            tabX += tabWidth + 5;
        }
        
        // Get quests for current tab
        List<Quest> quests = getQuestsForTab();
        
        // Draw quest list on left side (within panel)
        int listX = guiLeft + 10;
        int listY = guiTop + 48;
        int listWidth = Math.min(120, panelWidth / 3);
        int entryHeight = 18;
        int listHeight = panelHeight - 62;
        int maxVisible = Math.max(1, listHeight / entryHeight);
        
        Quest hoverTip = null;
        for (int i = scrollOffset; i < Math.min(scrollOffset + maxVisible, quests.size()); i++) {
            Quest quest = quests.get(i);
            int entryY = listY + (i - scrollOffset) * entryHeight;
            
            boolean isHovered = mouseX >= listX && mouseX < listX + listWidth &&
                               mouseY >= entryY && mouseY < entryY + entryHeight;
            boolean isSelected = quest == selectedQuest;
            
            // Background
            int bgColor = isSelected ? 0x80FFFFFF : (isHovered ? 0x40FFFFFF : 0x20000000);
            graphics.fill(listX, entryY, listX + listWidth, entryY + entryHeight - 1, bgColor);
            
            // Category color indicator
            int categoryColor = getCategoryColor(quest.category);
            graphics.fill(listX, entryY, listX + 3, entryY + entryHeight - 1, categoryColor);
            
            // a text category tag (not colour alone) on the right, the name truncated before it, full name on hover
            String tag = shortCategory(quest.category);
            int tagW = font.width(tag);
            graphics.drawString(font, tag, listX + listWidth - tagW - 3, entryY + 5, 0xB0B0B0);
            String displayName = quest.name;
            int nameRoom = listWidth - tagW - 14;
            boolean cut = font.width(displayName) > nameRoom;
            if (cut) displayName = font.plainSubstrByWidth(displayName, nameRoom - font.width("...")) + "...";
            graphics.drawString(font, displayName, listX + 6, entryY + 5, 0x00FFCC);
            if (isHovered) hoverTip = quest;
        }
        
        // Draw scroll indicators
        if (scrollOffset > 0) {
            graphics.drawCenteredString(font, "▲", listX + listWidth / 2, listY - 10, 0xFFFFFF);
        }
        if (scrollOffset + maxVisible < quests.size()) {
            graphics.drawCenteredString(font, "▼", listX + listWidth / 2, listY + maxVisible * entryHeight + 2, 0xFFFFFF);
        }
        
        // Draw selected quest details on right side (within panel)
        int detailsX = guiLeft + listWidth + 16;
        int detailsY = guiTop + 48;
        int detailsMaxWidth = panelWidth - listWidth - 26;
        if (selectedQuest != null) {
            renderQuestDetails(graphics, selectedQuest, detailsX, detailsY, detailsMaxWidth);
        } else {
            graphics.drawCenteredString(font, quests.isEmpty() ? emptyText() : "Select a quest", guiLeft + panelWidth / 2, guiTop + panelHeight / 2 - 10, 0xCCCCCC);
        }
        if (hoverTip != null) {
            graphics.renderComponentTooltip(font, List.of(Component.literal(hoverTip.name).withStyle(net.minecraft.ChatFormatting.WHITE),
                Component.literal(hoverTip.category.displayName).withStyle(net.minecraft.ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    private String emptyText() {
        return switch (currentTab) {
            case ACTIVE -> "No active quests";
            case AVAILABLE -> "No side quests or challenges to accept yet";
            case COMPLETED -> "Nothing completed yet";
        };
    }

    private static String shortCategory(QuestCategory c) {
        return switch (c) { case TUTORIAL -> "TUT"; case MAIN -> "MAIN"; case SIDE -> "SIDE"; case CHALLENGE -> "CHAL"; };
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(guiLeft, guiTop, guiLeft + panelWidth, guiTop + panelHeight, 0xDD000000);
        graphics.renderOutline(guiLeft, guiTop, panelWidth, panelHeight, 0xFF6600CC);
    }

    private void renderQuestDetails(GuiGraphics graphics, Quest quest, int x, int y, int maxWidth) {
        if (maxWidth <= 0) maxWidth = 150;
        
        // Quest name with category color
        int categoryColor = getCategoryColor(quest.category);
        graphics.drawString(font, quest.name, x, y, categoryColor);
        y += 12;
        
        // Category (body text)
        graphics.drawString(font, quest.category.displayName, x, y, 0xCCCCCC);
        y += 14;
        
        // Description (word wrapped)
        List<String> descLines = wrapText(quest.description, maxWidth);
        for (String line : descLines) {
            graphics.drawString(font, line, x, y, 0xCCCCCC);
            y += 10;
        }
        y += 6;
        
        // Objectives (label)
        graphics.drawString(font, "Objectives:", x, y, 0xFFFFFF);
        y += 12;
        
        Map<String, Map<Integer, Integer>> progress = ClientQuestData.getObjectiveProgress();
        Map<Integer, Integer> questProgress = progress.get(quest.id);
        
        for (int i = 0; i < quest.objectiveDescriptions.size(); i++) {
            String objDesc = quest.objectiveDescriptions.get(i);
            int objProgress = questProgress != null ? questProgress.getOrDefault(i, 0) : 0;
            
            var defs = com.matt.quantumfalcon.quest.QuestTracker.objectives(quest.id);
            int required = i < defs.size() ? defs.get(i).required : 1;
            boolean complete = objProgress >= required || ClientQuestData.getCompletedQuests().contains(quest.id);
            String status = complete ? "§a✓ " : "§7○ " + (required > 1 ? Math.min(objProgress, required) + "/" + required + " " : "");
            
            List<String> objLines = wrapText(objDesc, maxWidth - 10);
            for (int j = 0; j < objLines.size(); j++) {
                String prefix = j == 0 ? status : "  ";
                graphics.drawString(font, prefix + objLines.get(j), x, y, 0xCCCCCC);
                y += 10;
            }
        }
        y += 8;
        
        // Rewards (label + highlight)
        if (!quest.rewards.isEmpty() || quest.xpReward > 0) {
            graphics.drawString(font, "Rewards:", x, y, 0xFFDD00);
            y += 12;
            
            if (quest.xpReward > 0) {
                graphics.drawString(font, "  +" + quest.xpReward + " XP", x, y, 0xCCCCCC);
                y += 10;
            }
            
            for (var reward : quest.rewards) {
                graphics.drawString(font, "  " + reward.getCount() + "x " + 
                    reward.getHoverName().getString(), x, y, 0xCCCCCC);
                y += 10;
            }
        }
    }

    private List<Quest> getQuestsForTab() {
        List<Quest> result = new ArrayList<>();
        List<String> activeQuests = ClientQuestData.getActiveQuests();
        List<String> completedQuests = ClientQuestData.getCompletedQuests();
        
        switch (currentTab) {
            case ACTIVE:
                for (String questId : activeQuests) {
                    Quest quest = QuestSystem.getQuest(questId);
                    if (quest != null) result.add(quest);
                }
                break;
            case AVAILABLE:
                for (Quest quest : QuestSystem.QUESTS.values()) {
                    // Available lists only what the player can accept - side quests and challenges (main/tutorial auto-start)
                    if (quest.category != QuestCategory.SIDE && quest.category != QuestCategory.CHALLENGE) continue;
                    if (!activeQuests.contains(quest.id) && !completedQuests.contains(quest.id)) {
                        // Check prerequisite
                        if (quest.prerequisite == null || completedQuests.contains(quest.prerequisite)) {
                            result.add(quest);
                        }
                    }
                }
                break;
            case COMPLETED:
                for (String questId : completedQuests) {
                    Quest quest = QuestSystem.getQuest(questId);
                    if (quest != null) result.add(quest);
                }
                break;
        }
        
        return result;
    }

    private int getCategoryColor(QuestCategory category) {
        return switch (category) {
            case TUTORIAL -> 0xFF00FF00;
            case MAIN -> 0xFFFFAA00;
            case SIDE -> 0xFF00AAFF;
            case CHALLENGE -> 0xFFFF0000;
        };
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.width(testLine) > maxWidth) {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<Quest> quests = getQuestsForTab();
            int listX = guiLeft + 10;
            int listY = guiTop + 48;
            int listWidth = Math.min(120, panelWidth / 3);
            int entryHeight = 18;
            int listHeight = panelHeight - 62;
            int maxVisible = Math.max(1, listHeight / entryHeight);
            
            for (int i = scrollOffset; i < Math.min(scrollOffset + maxVisible, quests.size()); i++) {
                int entryY = listY + (i - scrollOffset) * entryHeight;
                
                if (mouseX >= listX && mouseX < listX + listWidth &&
                    mouseY >= entryY && mouseY < entryY + entryHeight) {
                    selectedQuest = quests.get(i);
                    QfUi.uiSound("ui_click");
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<Quest> quests = getQuestsForTab();
        int listHeight = panelHeight - 62;
        int maxVisible = Math.max(1, listHeight / 18);
        
        if (scrollY > 0 && scrollOffset > 0) {
            scrollOffset--;
            return true;
        } else if (scrollY < 0 && scrollOffset + maxVisible < quests.size()) {
            scrollOffset++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
