package com.matt.quantumfalcon.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A realm's dungeon key: dropped by the guard-room elite, consumed by a sealed key door. a key is cut for the ruin
 * whose keybearer dropped it (custom data "QfSite") and only fits that ruin's doors. Keys without a site (old worlds,
 * creative menu) fit any door of their own realm.
 */
public class DungeonKeyItem extends Item {
    private final String realm;

    public DungeonKeyItem(String realm, Properties props) {
        super(props);
        this.realm = realm;
    }

    public String realm() { return realm; }

    public static final String SITE = "QfSite";

    public static ItemStack forSite(ItemStack stack, String site) {
        if (site == null) return stack;
        CompoundTag t = new CompoundTag();
        t.putString(SITE, site);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return stack;
    }

    public static String siteOf(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (d == null) return null;
        CompoundTag t = d.copyTag();
        return t.contains(SITE) ? t.getString(SITE) : null;
    }

    /** "x, z" of the ruin a site key points at (its start chunk centre), for the tooltip. */
    static String where(String site) {
        try {
            long l = Long.parseLong(site.substring(site.indexOf('@') + 1));
            net.minecraft.world.level.ChunkPos c = new net.minecraft.world.level.ChunkPos(l);
            return c.getMiddleBlockX() + ", " + c.getMiddleBlockZ();
        } catch (RuntimeException ex) { return "?"; }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Opens a sealed Echo vault door").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Cut from the " + realm + " ruins").withStyle(ChatFormatting.DARK_AQUA));
        String site = siteOf(stack);
        tooltip.add(Component.literal(site != null ? "Fits only the ruin near " + where(site) : "Fits any " + realm + " ruin").withStyle(ChatFormatting.DARK_GRAY));
    }
}
