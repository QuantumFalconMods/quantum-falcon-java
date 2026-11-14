package com.matt.quantumfalcon.client;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * any of our blocks gets up to two grey hover lines from {@code block.quantumfalcon.<id>.tooltip_1/_2} when
 * those lang keys exist (so the text lives in en_us, not in code, and blocks without keys show nothing).
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public final class BlockTooltips {
    private BlockTooltips() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        if (!(e.getItemStack().getItem() instanceof BlockItem bi)) return;
        var id = BuiltInRegistries.BLOCK.getKey(bi.getBlock());
        if (!QuantumFalcon.MOD_ID.equals(id.getNamespace())) return;
        for (int i = 1; i <= 2; i++) {
            String key = "block.quantumfalcon." + id.getPath() + ".tooltip_" + i;
            if (I18n.exists(key)) e.getToolTip().add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }
}
