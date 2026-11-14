package com.matt.quantumfalcon.client.gui;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.blockentity.QuantumIncubatorBlockEntity;
import com.matt.quantumfalcon.breeding.BreedingData;
import com.matt.quantumfalcon.item.FalconEggItem;
import com.matt.quantumfalcon.menu.QuantumIncubatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Quantum Incubator (gui_specs.md section 8): 176x166, egg slot (56,17), fuel slot (56,53), lit fuel well (56,36), hatch bar
 * (79,34) 48x17, pedigree button (132,34), condition / time copy at (79,54). Chrome comes from qf_ui_kit (9-slice, slot glows,
 * pulse); decor and widget strips from quantum_incubator.png. States: idle (gunmetal), working (cyan pulse), paused (amber,
 * with the missing condition).
 */
public class QuantumIncubatorScreen extends AbstractContainerScreen<QuantumIncubatorMenu> {
    private static final ResourceLocation SHEET = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "textures/gui/quantum_incubator.png");

    public QuantumIncubatorScreen(QuantumIncubatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        inventoryLabelY = 74;
    }

    private boolean hasGenome() { return FalconEggItem.child(menu.getSlot(0).getItem()) != null; }
    private int state() {
        int st = menu.status();
        if (menu.getSlot(0).getItem().isEmpty()) return 0;
        return st == QuantumIncubatorBlockEntity.ST_WORKING ? 1 : st == QuantumIncubatorBlockEntity.ST_IDLE ? 0 : 2;
    }
    private int stateRgb() { return switch (state()) { case 1 -> QfUi.CYAN; case 2 -> QfUi.AMBER; default -> 0; }; }
    private boolean overButton(double mx, double my) { return mx >= leftPos + 132 && mx < leftPos + 150 && my >= topPos + 34 && my < topPos + 52; }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos, st = state();
        QfUi.panel(g, x, y, imageWidth, imageHeight, stateRgb(), pt);
        // slots: the egg and fuel wells glow with the machine state; inventory wells idle (hover glow under the cursor)
        QfUi.slot(g, x + 56, y + 17, st == 1 ? QfUi.SLOT_WORKING : st == 2 ? QfUi.SLOT_PAUSED : isHovering(56, 17, 16, 16, mx, my) ? QfUi.SLOT_HOVER : QfUi.SLOT_IDLE, pt);
        boolean fuelled = menu.fuel() > 0;
        QfUi.slot(g, x + 56, y + 53, st == 2 && !fuelled ? QfUi.SLOT_PAUSED : isHovering(56, 53, 16, 16, mx, my) ? QfUi.SLOT_HOVER : QfUi.SLOT_IDLE, pt);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) QfUi.slot(g, x + 8 + c * 18, y + 84 + r * 18, QfUi.SLOT_IDLE, pt);
        for (int c = 0; c < 9; c++) QfUi.slot(g, x + 8 + c * 18, y + 142, QfUi.SLOT_IDLE, pt);
        g.blit(SHEET, x, y, 0, 0, imageWidth, imageHeight, 256, 256);            // cradle, coil, dim flame
        int f = Math.round(14 * menu.fuel());
        if (f > 0) g.blit(SHEET, x + 57, y + 36 + 14 - f, 176, 14 - f, 14, f, 256, 256);
        // hatch bar well + fill (pulses and carries the glint while working; stalls amber when paused)
        g.blit(SHEET, x + 79, y + 34, 176, 14, 48, 17, 256, 256);
        if (st > 0) QfUi.bar(g, x + 90, y + 38, 36, 9, menu.progress(), st == 1 ? QfUi.CYAN : QfUi.AMBER, -1f, st == 1 ? pt : 0f);
        // pedigree button: disabled without a genome egg, hover highlights
        int bu = !hasGenome() ? 212 : overButton(mx, my) ? 194 : 176;
        g.blit(SHEET, x + 132, y + 34, bu, 32, 18, 18, 256, 256);
        if (hasGenome()) {
            int c = QfUi.glow(QfUi.CYAN, pt, 1f);
            g.renderOutline(x + 132, y + 34, 18, 18, (c & 0x00FFFFFF) | 0x90000000);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        QfText.draw(g, font, title.getString(), 8, 6, 152, 9, QfText.Size.DEFAULT, 0xFF28ECFF);
        QfText.draw(g, font, playerInventoryTitle.getString(), 8, 74, 80, 9, QfText.Size.DEFAULT, 0xFF8090B0);
        int t = menu.tier(), st = state();
        if (menu.getSlot(0).getItem().isEmpty()) { QfText.draw(g, font, "INSERT AN EGG", 79, 54, 88, 7, QfText.Size.SMALL, 0xFF6A7290); return; }
        if (t > 0) {
            var td = BreedingData.TIERS.get(Mth.clamp(t, 1, 7) - 1);
            QfUi.tierPip(g, 82, 22, t, st == 1);
            QfText.draw(g, font, td.name().toUpperCase() + "  " + td.hatchMinutes() + " MIN", 92, 23, 75, 7, QfText.Size.SMALL, 0xFF000000 | td.colour());
        }
        if (st == 1) {
            QfText.draw(g, font, fmt(menu.secondsLeft()) + " LEFT", 79, 54, 88, 7, QfText.Size.SMALL, 0xFFC8D2E6);
            QfText.draw(g, font, Math.round(menu.progress() * 100) + "% HATCHED", 79, 61, 88, 7, QfText.Size.SMALL, 0xFF28ECFF);
        } else if (st == 2) {
            QfText.draw(g, font, "PAUSED", 79, 54, 88, 7, QfText.Size.SMALL, 0xFFFFB43A);
            QfText.draw(g, font, menu.status() == QuantumIncubatorBlockEntity.ST_NEED_FUEL ? "NEEDS RIFT ENERGY" : "NEEDS RIFT 3+ OR FORGE", 79, 61, 88, 7, QfText.Size.SMALL, 0xFFFFB43A);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && overButton(mx, my) && hasGenome()) {
            ItemStack egg = menu.getSlot(0).getItem();
            var data = FalconEggItem.eggData(egg);
            if (data != null && data.contains("Child")) {
                PedigreeScreen.open(data.getCompound("Child"), "egg", "", -1, this);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private static String fmt(int s) { return (s / 60) + ":" + String.format("%02d", s % 60); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        if (overButton(mx, my))
            g.renderComponentTooltip(font, List.of(Component.literal(hasGenome() ? "Open pedigree" : "Pedigree: insert an egg")), mx, my);
        else renderTooltip(g, mx, my);
    }
}
