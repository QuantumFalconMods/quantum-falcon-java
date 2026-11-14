package com.matt.quantumfalcon.item;

import com.matt.quantumfalcon.events.QuantumFlightHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Quantum Berry: fruit that ripened inside a held hour. It keeps the hour it was eaten in, so what it gives
 * depends on the time of day (or on the realm, inside the Echo dimensions). Every berry also gives a short
 * burst of quantum flight. Written for Quantum Falcon; eating, hunger and stack use are vanilla food handling.
 */
public class QuantumBerryItem extends Item {

    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(4).saturationModifier(0.6f).alwaysEdible().build();

    /** Ticks of quantum flight per berry. */
    public static final int FLIGHT_TICKS = 200;

    public QuantumBerryItem(Properties properties) {
        super(properties.food(FOOD));
    }

    /** The hour a berry is eaten in, read from the day clock. */
    public enum Hour { MORNING, NOON, DUSK, NIGHT, ECHO }

    public static Hour hourOf(Level level) {
        if (level.dimension().location().getNamespace().equals("quantumfalcon")) return Hour.ECHO;
        long t = level.getDayTime() % 24000L;
        if (t < 5000) return Hour.MORNING;
        if (t < 11500) return Hour.NOON;
        if (t < 13500) return Hour.DUSK;
        return Hour.NIGHT;
    }

    static MobEffectInstance gift(Hour h) {
        Holder<MobEffect> e; int ticks;
        switch (h) {
            case MORNING -> { e = MobEffects.DIG_SPEED; ticks = 600; }
            case NOON -> { e = MobEffects.MOVEMENT_SPEED; ticks = 600; }
            case DUSK -> { e = MobEffects.SLOW_FALLING; ticks = 400; }
            case NIGHT -> { e = MobEffects.NIGHT_VISION; ticks = 800; }
            default -> { e = MobEffects.DAMAGE_RESISTANCE; ticks = 300; }
        }
        return new MobEffectInstance(e, ticks, 0);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        if (!level.isClientSide && eater instanceof Player player) {
            player.addEffect(gift(hourOf(level)));
            QuantumFlightHandler.grantFlight(player, FLIGHT_TICKS);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.7f, 1.5f);
        }
        return super.finishUsingItem(stack, level, eater);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Ripened in a held hour; it gives what that hour gives").withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.literal("Morning: haste · Noon: speed · Dusk: slow fall · Night: night vision · Echo realms: resistance").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Always: 10s of quantum flight").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Feed to your falcon: 30s tireless, faster flight").withStyle(ChatFormatting.DARK_AQUA));
        super.appendHoverText(stack, ctx, lines, flag);
    }
}
