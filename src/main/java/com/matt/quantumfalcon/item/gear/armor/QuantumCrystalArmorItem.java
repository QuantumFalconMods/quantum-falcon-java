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
 * Quantum Crystal Armor (Tier 3: Magenta)
 * Late-game crystalline armor with phase abilities.
 * 
 * Stats:
 * - Defense: 3/5/4/1
 * - Durability: 11-13
 * - Enchantability: 25 (highest)
 * 
 * Set Bonus (2+ pieces): +20% movement speed, +15% attack speed
 * Set Bonus (4 pieces): Phase Shift (10% chance to dodge attacks)
 */
public class QuantumCrystalArmorItem extends ArmorItem {
    
    private final Type armorType;
    
    public QuantumCrystalArmorItem(Type type) {
        super(ModArmorMaterials.MAGENTA_ARMOR_MATERIAL, type, new Item.Properties()
            .durability(type.getDurability(12)));
        this.armorType = type;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 3: Quantum Crystal").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Set Bonus (2+ pieces):").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  +20% Movement Speed").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  +15% Attack Speed").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Set Bonus (4 pieces):").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Phase Shift (10% dodge chance)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Highest enchantability (25)").withStyle(ChatFormatting.GOLD));
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
                case HELMET -> com.matt.quantumfalcon.registry.ModItems.MAGENTA_HELMET.get();
                case CHESTPLATE -> com.matt.quantumfalcon.registry.ModItems.MAGENTA_CHESTPLATE.get();
                case LEGGINGS -> com.matt.quantumfalcon.registry.ModItems.MAGENTA_LEGGINGS.get();
                case BOOTS -> com.matt.quantumfalcon.registry.ModItems.MAGENTA_BOOTS.get();
                default -> null;
            }
        );
    }
}
