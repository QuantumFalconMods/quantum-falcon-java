/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectCategory
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package com.matt.quantumfalcon.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create((ResourceKey)Registries.MOB_EFFECT, (String)"quantumfalcon");
    public static final DeferredHolder<MobEffect, MobEffect> QUANTUM_VERTIGO = MOB_EFFECTS.register("quantum_vertigo", () -> new MobEffect(MobEffectCategory.HARMFUL, 9699539){});
    public static final DeferredHolder<MobEffect, MobEffect> ECHO_SICKNESS = MOB_EFFECTS.register("echo_sickness", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x8B008B){});
    public static final DeferredHolder<MobEffect, MobEffect> ECHO_FAMILIARITY = MOB_EFFECTS.register("echo_familiarity", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 9662683){});
    public static final DeferredHolder<MobEffect, MobEffect> QUANTUM_RESONANCE = MOB_EFFECTS.register("quantum_resonance", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 52945){});

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
