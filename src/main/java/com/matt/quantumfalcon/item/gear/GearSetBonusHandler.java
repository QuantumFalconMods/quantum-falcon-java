package com.matt.quantumfalcon.item.gear;

import com.matt.quantumfalcon.item.gear.armor.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Handles all set bonuses for quantum armor sets.
 * Pattern: Same approach as Botania's Terrasteel/Manaweave and Mekanism's MekaSuit.
 * All attribute modifiers are cleaned up when armor is removed to prevent permanent buffs.
 */
public class GearSetBonusHandler {
    
    // Modifier IDs - must be unique and stable across sessions
    private static final ResourceLocation SINGULARITY_SPEED = ResourceLocation.fromNamespaceAndPath("quantumfalcon", "singularity_speed");
    private static final ResourceLocation VOID_FORGED_KNOCKBACK = ResourceLocation.fromNamespaceAndPath("quantumfalcon", "void_forged_knockback");
    private static final ResourceLocation TEMPORAL_RESIST = ResourceLocation.fromNamespaceAndPath("quantumfalcon", "temporal_resist");
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        
        // Apply set bonuses every 20 ticks (1 second) for performance
        if (player.tickCount % 20 != 0) return;
        
        // ================================================
        // TIER 1: SINGULARITY ARMOR (Aqua)
        // Full: +10% speed + Quantum Dash (shift-sprint)
        // Partial (2+): +5% speed
        // ================================================
        if (SingularityArmorItem.hasFullSet(player)) {
            applyModifier(player, Attributes.MOVEMENT_SPEED, SINGULARITY_SPEED, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else if (SingularityArmorItem.hasPartialSet(player)) {
            applyModifier(player, Attributes.MOVEMENT_SPEED, SINGULARITY_SPEED, 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else {
            removeModifier(player, Attributes.MOVEMENT_SPEED, SINGULARITY_SPEED);
        }
        
        // ================================================
        // TIER 2: VOID FORGED ARMOR (Purple)
        // Full: Rift damage immunity + knockback resistance
        // Partial (2+): +25% rift resistance + minor knockback resistance
        // ================================================
        if (VoidForgedArmorItem.hasFullSet(player)) {
            applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, VOID_FORGED_KNOCKBACK, 0.5, AttributeModifier.Operation.ADD_VALUE);
        } else if (VoidForgedArmorItem.hasPartialSet(player)) {
            applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, VOID_FORGED_KNOCKBACK, 0.25, AttributeModifier.Operation.ADD_VALUE);
        } else {
            removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, VOID_FORGED_KNOCKBACK);
        }
        
        // ================================================
        // TIER 3: QUANTUM CRYSTAL ARMOR (Magenta)
        // Full: Phase Shift (10% dodge in damage event) + speed + haste
        // Partial (2+): Speed buff
        // ================================================
        if (QuantumCrystalArmorItem.hasFullSet(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, false, false));
        } else if (QuantumCrystalArmorItem.hasPartialSet(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
        }
        
        // ================================================
        // TIER 4: TEMPORAL GUARDIAN ARMOR (Gold)
        // Full: Temporal Shield (death save in damage event) + regen + resistance
        // Partial (2+): Regeneration + minor resistance
        // ================================================
        if (TemporalGuardianArmorItem.hasFullSet(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
        } else if (TemporalGuardianArmorItem.hasPartialSet(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
        }
    }
    
    @SubscribeEvent
    public static void onLivingHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        
        // Tier 3: Quantum Crystal Full Set - Phase Shift (10% dodge)
        if (QuantumCrystalArmorItem.hasFullSet(player)) {
            if (player.getRandom().nextFloat() < 0.10f) {
                event.setNewDamage(0);
                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1, player.getZ(),
                        20, 0.5, 0.5, 0.5, 0.1);
                }
                return; // Fully dodged
            }
        }
        
        // Tier 4: Temporal Guardian Full Set - Temporal Shield (death save, 60s cooldown)
        if (TemporalGuardianArmorItem.hasFullSet(player)) {
            if (event.getNewDamage() >= player.getHealth()) {
                long lastShield = player.getPersistentData().getLong("LastTemporalShield");
                long currentTime = player.level().getGameTime();
                
                if (currentTime - lastShield >= 1200) { // 60 second cooldown
                    event.setNewDamage(0);
                    player.setHealth(1.0f);
                    player.getPersistentData().putLong("LastTemporalShield", currentTime);
                    
                    if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                            player.getX(), player.getY() + 1, player.getZ(),
                            50, 0.5, 0.5, 0.5, 0.2);
                    }
                    
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("\u00a76\u2726 Temporal Shield activated! \u00a77(60s cooldown)"),
                        true
                    );
                    return; // Saved from death
                }
            }
        }
        
        // Tier 2: Void Forged - Rift/quantum damage resistance
        String damageId = event.getSource().getMsgId();
        boolean isRiftDamage = damageId.contains("rift") || damageId.contains("quantum") || damageId.contains("instability");
        
        if (isRiftDamage) {
            if (VoidForgedArmorItem.hasFullSet(player)) {
                event.setNewDamage(0); // Full immunity
            } else if (VoidForgedArmorItem.hasPartialSet(player)) {
                event.setNewDamage(event.getNewDamage() * 0.75f); // 25% reduction
            }
        }
    }
    
    // ================================================
    // UTILITY: Safe modifier application/removal
    // Pattern from Botania: always remove before adding to prevent stacking
    // ================================================
    
    private static void applyModifier(Player player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                       ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        
        if (instance.hasModifier(id)) {
            AttributeModifier existing = instance.getModifier(id);
            if (existing != null && existing.amount() != amount) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
            }
        } else {
            instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }
    
    private static void removeModifier(Player player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                        ResourceLocation id) {
        var instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
