package com.matt.quantumfalcon.item.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.List;

/** Melee weapon with a right-click special on a cooldown. */
public abstract class AbilitySwordItem extends SwordItem {
    protected final String id;
    private final int cooldown, tooltipLines;

    protected AbilitySwordItem(String id, Tier tier, Item.Properties props, int cooldownTicks, int tooltipLines) {
        super(tier, props);
        this.id = id; this.cooldown = cooldownTicks; this.tooltipLines = tooltipLines;
    }

    /** Server side. Return true if the special happened (starts the cooldown, costs durability). */
    public abstract boolean special(Level level, Player player, ItemStack stack);

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (cooldown <= 0) return super.use(level, player, hand);
        if (!level.isClientSide) {
            if (special(level, player, stack)) {
                player.getCooldowns().addCooldown(this, cooldown);
                stack.hurtAndBreak(2, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        WeaponFx.tooltip(id, tooltipLines, tip);
    }
}
