package com.matt.quantumfalcon.item.gear.armor;

import com.matt.quantumfalcon.material.ModArmorMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Void Forged Armor (Tier 2: Purple)
 * Mid-game void-infused armor with rift protection.
 * 
 * Stats:
 * - Defense: 4/7/6/2
 * - Durability: 15-17
 * - Toughness: 2.0 (like Netherite)
 * - Knockback Resistance: 0.1
 * - Enchantability: 15
 * 
 * Set Bonus (2+ pieces): Rift damage resistance (+25%)
 * Set Bonus (4 pieces): Rift immunity + gravity well protection
 */
public class VoidForgedArmorItem extends ArmorItem {
    
    private final Type armorType;
    
    public VoidForgedArmorItem(Type type) {
        super(ModArmorMaterials.PURPLE_ARMOR_MATERIAL, type, new Item.Properties()
            .durability(type.getDurability(20)));
        this.armorType = type;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 2: Void Forged").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Set Bonus (2+ pieces):").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("  +25% Rift Damage Resistance").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Set Bonus (4 pieces):").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("  Immune to rift damage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Gravity well protection").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  +2.0 Armor Toughness").withStyle(ChatFormatting.BLUE));
    }
    
    public static boolean hasFullSet(Player player) {
        return hasPiece(player, Type.HELMET) &&
               hasPiece(player, Type.CHESTPLATE) &&
               hasPiece(player, Type.LEGGINGS) &&
               hasPiece(player, Type.BOOTS);
    }
    
    public static boolean hasPartialSet(Player player) {
        int count = 0;
        if (hasPiece(player, Type.HELMET)) count++;
        if (hasPiece(player, Type.CHESTPLATE)) count++;
        if (hasPiece(player, Type.LEGGINGS)) count++;
        if (hasPiece(player, Type.BOOTS)) count++;
        return count >= 2;
    }
    
    private static boolean hasPiece(Player player, Type type) {
        return player.getInventory().armor.get(type.getSlot().getIndex()).is(
            switch (type) {
                case HELMET -> com.matt.quantumfalcon.registry.ModItems.PURPLE_HELMET.get();
                case CHESTPLATE -> com.matt.quantumfalcon.registry.ModItems.PURPLE_CHESTPLATE.get();
                case LEGGINGS -> com.matt.quantumfalcon.registry.ModItems.PURPLE_LEGGINGS.get();
                case BOOTS -> com.matt.quantumfalcon.registry.ModItems.PURPLE_BOOTS.get();
                default -> null;
            }
        );
    }
}
