package com.matt.quantumfalcon.gameplay;

import com.matt.quantumfalcon.integration.artifacts.ArtifactsIntegration;
import com.matt.quantumfalcon.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * Artifact Synergies System
 * 
 * When multiple boss artifacts are equipped, they provide synergy bonuses.
 * Also integrates with other mod systems (breeding, falcon abilities, etc.)
 */
@EventBusSubscriber
public class ArtifactSynergies {
    
    /**
     * Check if player has all 4 boss artifacts equipped
     */
    public static boolean hasAllArtifacts(Player player) {
        return hasArtifact(player, ModItems.OVERSEERS_EYE.get()) &&
               hasArtifact(player, ModItems.WARDENS_FRAGMENT.get()) &&
               hasArtifact(player, ModItems.TWIN_ESSENCE.get()) &&
               hasArtifact(player, ModItems.SOVEREIGNS_CROWN.get());
    }
    
    /**
     * Check if player has a specific artifact equipped
     */
    public static boolean hasArtifact(Player player, net.minecraft.world.item.Item item) {
        // Check inventory
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) return true;
        }
        
        // Check Curios slots if available (using reflection for optional integration)
        if (com.matt.quantumfalcon.integration.ModIntegrations.isCuriosLoaded()) {
            try {
                // Use reflection to access Curios API
                Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                java.lang.reflect.Method getCuriosInv = curiosApiClass.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
                Object optionalInv = getCuriosInv.invoke(null, player);
                
                if (optionalInv instanceof java.util.Optional<?>) {
                    java.util.Optional<?> opt = (java.util.Optional<?>) optionalInv;
                    if (opt.isPresent()) {
                        Object inv = opt.get();
                        java.lang.reflect.Method getCurios = inv.getClass().getMethod("getCurios");
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, ?> curios = (java.util.Map<String, ?>) getCurios.invoke(inv);
                        
                        for (Object handler : curios.values()) {
                            java.lang.reflect.Method getSlots = handler.getClass().getMethod("getSlots");
                            java.lang.reflect.Method getStackInSlot = handler.getClass().getMethod("getStackInSlot", int.class);
                            int slots = (int) getSlots.invoke(handler);
                            for (int i = 0; i < slots; i++) {
                                ItemStack stack = (ItemStack) getStackInSlot.invoke(handler, i);
                                if (stack.is(item)) return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Curios integration failed - this is fine, just skip
            }
        }
        
        return false;
    }
    
    /**
     * Get synergy bonus multiplier based on artifacts equipped
     */
    public static float getSynergyMultiplier(Player player) {
        int artifactCount = 0;
        if (hasArtifact(player, ModItems.OVERSEERS_EYE.get())) artifactCount++;
        if (hasArtifact(player, ModItems.WARDENS_FRAGMENT.get())) artifactCount++;
        if (hasArtifact(player, ModItems.TWIN_ESSENCE.get())) artifactCount++;
        if (hasArtifact(player, ModItems.SOVEREIGNS_CROWN.get())) artifactCount++;
        
        // 2 artifacts: +10%, 3 artifacts: +25%, 4 artifacts: +50%
        return switch (artifactCount) {
            case 2 -> 1.10f;
            case 3 -> 1.25f;
            case 4 -> 1.50f;
            default -> 1.0f;
        };
    }
    
    /**
     * Apply artifact synergies on player tick
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        
        Player player = event.getEntity();
        
        // Sovereign's Crown: Increase singularity egg chance in breeding
        if (hasArtifact(player, ModItems.SOVEREIGNS_CROWN.get())) {
            // This would be checked in breeding logic
            // Example: breedingMutationChance *= 1.5f
        }
        
        // Twin Essence: Share falcon traits with nearby players
        if (hasArtifact(player, ModItems.TWIN_ESSENCE.get())) {
            // Check for nearby allies and share effects
            // This is handled in ArtifactsIntegration.TwinEssenceItem
        }
        
        // All 4 artifacts: Ultimate synergy
        if (hasAllArtifacts(player)) {
            // Apply ultimate synergy effects
            // This could be handled by Heart of Quantum item
        }
    }
    
    /**
     * Get breeding mutation bonus from artifacts
     */
    public static float getBreedingMutationBonus(Player player) {
        float bonus = 0.0f;
        
        if (hasArtifact(player, ModItems.SOVEREIGNS_CROWN.get())) {
            bonus += 0.5f; // +50% mutation chance
        }
        
        if (hasArtifact(player, ModItems.TWIN_ESSENCE.get())) {
            bonus += 0.25f; // +25% mutation chance
        }
        
        return bonus;
    }
    
    /**
     * Get falcon damage bonus from artifacts
     */
    public static float getFalconDamageBonus(Player player) {
        float bonus = 0.0f;
        
        if (hasArtifact(player, ModItems.SOVEREIGNS_CROWN.get())) {
            bonus += ArtifactsIntegration.SovereignsCrownItem.getFalconDamageBoost(); // +50%
        }
        
        if (hasArtifact(player, ModItems.OVERSEERS_EYE.get())) {
            bonus += 0.10f; // +10% from Overseer's Eye
        }
        
        return bonus;
    }
}
