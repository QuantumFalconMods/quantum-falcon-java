package com.matt.quantumfalcon.item;

import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Falcon Whistle - right-click to summon owned falcons in range.
 * Teleports tamed falcons to the player and makes them follow.
 */
public class FalconWhistleItem extends Item {

    private static final int SUMMON_RANGE = 32;
    private static final int COOLDOWN_TICKS = 100;

    public FalconWhistleItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            AABB searchBox = player.getBoundingBox().inflate(SUMMON_RANGE);
            List<QuantumFalconEntity> falcons = level.getEntitiesOfClass(
                QuantumFalconEntity.class,
                searchBox,
                falcon -> falcon.isTame() && falcon.isOwnedBy(player)
            );

            if (!falcons.isEmpty() && player.isShiftKeyDown()) {
                // Sneak-use: every owned falcon in range uses its variant ability
                level.playSound(null, player.blockPosition(), ModSounds.FALCON_WHISTLE.get(), SoundSource.PLAYERS, 1.0f, 1.5f);
                int used = 0;
                for (QuantumFalconEntity falcon : falcons) {
                    if (com.matt.quantumfalcon.entity.ability.FalconAbilities.activate(falcon, player).ok()) used++;
                }
                player.displayClientMessage(Component.translatable("item.quantumfalcon.falcon_whistle.abilities", used, falcons.size()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                player.getCooldowns().addCooldown(this, 20);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            } else if (!falcons.isEmpty()) {
                level.playSound(null, player.blockPosition(), ModSounds.FALCON_WHISTLE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

                for (QuantumFalconEntity falcon : falcons) {
                    falcon.setOrderedToSit(false);
                    double angle = level.random.nextDouble() * Math.PI * 2.0;
                    double radius = 2.0 + level.random.nextDouble() * 2.0;
                    double x = player.getX() + Math.cos(angle) * radius;
                    double z = player.getZ() + Math.sin(angle) * radius;
                    falcon.teleportTo(x, player.getY() + 1.0, z);
                    falcon.getNavigation().stop();

                    for (int i = 0; i < 10; i++) {
                        level.addParticle(
                            ParticleTypes.END_ROD,
                            falcon.getX() + (level.random.nextDouble() - 0.5),
                            falcon.getY() + level.random.nextDouble(),
                            falcon.getZ() + (level.random.nextDouble() - 0.5),
                            0.0, 0.1, 0.0
                        );
                    }
                }

                player.displayClientMessage(
                    Component.literal("Called " + falcons.size() + " falcon(s)!").withStyle(ChatFormatting.GREEN),
                    true
                );
            } else {
                player.displayClientMessage(
                    Component.literal("No falcons in range").withStyle(ChatFormatting.GRAY),
                    true
                );
            }
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.quantumfalcon.falcon_whistle.tooltip.abilities").withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.literal("Right-click to summon your falcons").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Range: 32 blocks").withStyle(ChatFormatting.AQUA));
    }
}
