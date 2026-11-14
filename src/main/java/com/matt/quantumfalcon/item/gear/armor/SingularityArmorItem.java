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
 * Singularity Armor (Tier 1: Aqua)
 * Entry-level quantum armor with basic set bonus.
 * 
 * Stats:
 * - Defense: 3/6/5/2 (helmet/chest/legs/boots)
 * - Durability: 12-14
 * - Enchantability: 20
 * 
 * Set Bonus (2+ pieces): +10% movement speed
 * Set Bonus (4 pieces): Quantum Dash (sneak + jump to dash)
 */
public class SingularityArmorItem extends ArmorItem {
    
    private final Type armorType;
    
    public SingularityArmorItem(Type type) {
        super(ModArmorMaterials.AQUA_ARMOR_MATERIAL, type, new Item.Properties()
            .durability(type.getDurability(15)));
        this.armorType = type;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier 1: Singularity").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Set Bonus (2+ pieces):").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  +10% Movement Speed").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Set Bonus (4 pieces):").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Quantum Dash (sneak + jump)").withStyle(ChatFormatting.GRAY));
    }
    
    /**
     * Check if player has full set equipped
     */
    public static boolean hasFullSet(Player player) {
        return hasPiece(player, Type.HELMET) &&
               hasPiece(player, Type.CHESTPLATE) &&
               hasPiece(player, Type.LEGGINGS) &&
               hasPiece(player, Type.BOOTS);
    }
    
    /**
     * Check if player has 2+ pieces
     */
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
                case HELMET -> com.matt.quantumfalcon.registry.ModItems.AQUA_HELMET.get();
                case CHESTPLATE -> com.matt.quantumfalcon.registry.ModItems.AQUA_CHESTPLATE.get();
                case LEGGINGS -> com.matt.quantumfalcon.registry.ModItems.AQUA_LEGGINGS.get();
                case BOOTS -> com.matt.quantumfalcon.registry.ModItems.AQUA_BOOTS.get();
                default -> null;
            }
        );
    }
}
