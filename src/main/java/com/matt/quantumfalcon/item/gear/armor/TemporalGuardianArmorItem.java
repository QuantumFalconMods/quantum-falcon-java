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
 * Temporal Guardian Armor (Tier 4: Gold)
 * End-game divine armor with temporal protection.
 * 
 * Stats:
 * - Defense: 4/6/5/1
 * - Durability: 14-16
 * - Toughness: 1.0
 * - Knockback Resistance: 0.05
 * - Enchantability: 22
 * 
 * Set Bonus (2+ pieces): +15% all damage resistance, regeneration
 * Set Bonus (4 pieces): Temporal Shield (negates fatal damage once per minute)
 */
public class TemporalGuardianArmorItem extends ArmorItem {
    
    private final Type armorType;
    
    public TemporalGuardianArmorItem(Type type) {
        super(ModArmorMaterials.GOLD_ARMOR_MATERIAL, type, new Item.Properties()
            .durability(type.getDurability(17)));
        this.armorType = type;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 4: Temporal Guardian").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("The ultimate quantum protection").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Set Bonus (2+ pieces):").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  +15% All Damage Resistance").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Regeneration I").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Set Bonus (4 pieces):").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Temporal Shield").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Negates fatal damage (1/min)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Balanced divine protection").withStyle(ChatFormatting.DARK_GRAY));
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
                case HELMET -> com.matt.quantumfalcon.registry.ModItems.GOLD_HELMET_CUSTOM.get();
                case CHESTPLATE -> com.matt.quantumfalcon.registry.ModItems.GOLD_CHESTPLATE_CUSTOM.get();
                case LEGGINGS -> com.matt.quantumfalcon.registry.ModItems.GOLD_LEGGINGS_CUSTOM.get();
                case BOOTS -> com.matt.quantumfalcon.registry.ModItems.GOLD_BOOTS_CUSTOM.get();
                default -> null;
            }
        );
    }
}
