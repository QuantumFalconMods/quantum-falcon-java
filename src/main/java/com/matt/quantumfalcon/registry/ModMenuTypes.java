package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Menu Type Registry for Quantum Falcon Mod
 * 
 * NOTE: This is a minimal stub implementation for NeoForge 1.21.4.
 * Menu classes need to be implemented before they can be registered here.
 * 
 * PLANNED MENUS:
 * - QUANTUM_FORGE: Crafting interface for quantum items
 * - INCUBATOR: Egg incubation progress and controls
 * - FALCON_JOURNAL: Falcon discovery and stats viewer
 * - SKILL_TREE: Player progression interface
 */
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = 
        DeferredRegister.create(Registries.MENU, QuantumFalcon.MOD_ID);

    // ========================================
    // MENUS - TODO: Implement Menu classes
    // ========================================
    /*
    public static final DeferredHolder<MenuType<?>, MenuType<QuantumForgeMenu>> QUANTUM_FORGE = 
        MENU_TYPES.register("quantum_forge", () -> IMenuTypeExtension.create(QuantumForgeMenu::new));
    */

    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<com.matt.quantumfalcon.menu.QuantumIncubatorMenu>> INCUBATOR =
        MENU_TYPES.register("quantum_incubator", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.matt.quantumfalcon.menu.QuantumIncubatorMenu::new));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
