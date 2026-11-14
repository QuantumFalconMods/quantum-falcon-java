package com.matt.quantumfalcon.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Falcon barding (Matthew's falcon-armor plan: leather, iron, diamond, netherite, quantum tiers).
 * Use it on your own falcon to equip it (body slot); sneak-use shears on the falcon to take it off.
 */
public class FalconBardingItem extends Item {
    public enum Tier {
        LEATHER("leather", 2, 0, 0f, 1.0f, 0f, 0f, 0x8A5A32),
        IRON("iron", 4, 0, 0f, 1.0f, 0f, 0f, 0xC8C8CC),
        DIAMOND("diamond", 6, 1, 0f, 0.9f, 0f, 0f, 0x5FE3E0),
        NETHERITE("netherite", 8, 2, 0.1f, 0.8f, 0f, 0f, 0x4A4146),
        QUANTUM("quantum", 10, 3, 0.2f, 0.5f, 0.2f, 0.1f, 0x9B5CFF);

        public final String id; public final int armor, toughness; public final float knockbackRes, cooldownMultiplier, shieldChance, lifesteal; public final int color;
        Tier(String id, int armor, int toughness, float kb, float cd, float shield, float lifesteal, int color) {
            this.id = id; this.armor = armor; this.toughness = toughness; this.knockbackRes = kb; this.cooldownMultiplier = cd; this.shieldChance = shield; this.lifesteal = lifesteal; this.color = color;
        }
    }

    private final Tier tier;

    public FalconBardingItem(Tier tier) {
        super(new Item.Properties().stacksTo(1).rarity(tier == Tier.QUANTUM ? Rarity.EPIC : tier == Tier.NETHERITE ? Rarity.RARE : Rarity.COMMON).fireResistant());
        this.tier = tier;
    }

    public Tier tier() { return tier; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.quantumfalcon.falcon_barding.tooltip.use").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.quantumfalcon.falcon_barding.tooltip.armor", tier.armor, tier.toughness).withStyle(ChatFormatting.BLUE));
        if (tier.cooldownMultiplier < 1f)
            tip.add(Component.translatable("item.quantumfalcon.falcon_barding.tooltip.cooldown", Math.round((1 - tier.cooldownMultiplier) * 100)).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (tier.shieldChance > 0) tip.add(Component.translatable("item.quantumfalcon.falcon_barding.tooltip.shield", Math.round(tier.shieldChance * 100)).withStyle(ChatFormatting.AQUA));
        if (tier.lifesteal > 0) tip.add(Component.translatable("item.quantumfalcon.falcon_barding.tooltip.lifesteal", Math.round(tier.lifesteal * 100)).withStyle(ChatFormatting.RED));
    }
}
