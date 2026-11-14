package com.matt.quantumfalcon.material;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Armor materials for all four themes in NeoForge 1.21.1
 * Uses DeferredRegister so registration happens during RegisterEvent (registry not frozen).
 *
 * - AQUA (Singularity): 12-14 armor, 200 durability
 * - PURPLE (Void Forged): 15-17 armor, 280 durability
 * - MAGENTA (Quantum Crystal): 11-13 armor, 150 durability
 * - GOLD (Temporal Guardian): 14-16 armor, 240 durability
 */
public class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, QuantumFalcon.MOD_ID);

    // AQUA (Singularity) - Sleek, crystalline design
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> AQUA_ARMOR_MATERIAL =
        ARMOR_MATERIALS.register("singularity", () -> create(
            "singularity",
            createDefenseMap(3, 6, 5, 2),
            20,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            0.0F,
            0.0F
        ));

    // PURPLE (Void Forged) - Dark, high durability
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PURPLE_ARMOR_MATERIAL =
        ARMOR_MATERIALS.register("void_forged", () -> create(
            "void_forged",
            createDefenseMap(4, 7, 6, 2),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            2.0F,
            0.1F
        ));

    // MAGENTA (Quantum Crystal) - Fast, lower durability
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MAGENTA_ARMOR_MATERIAL =
        ARMOR_MATERIALS.register("quantum_crystal", () -> create(
            "quantum_crystal",
            createDefenseMap(3, 5, 4, 1),
            25,
            SoundEvents.ARMOR_EQUIP_GOLD,
            0.0F,
            0.0F
        ));

    // GOLD (Temporal Guardian) - Tier 4 endgame, divine
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GOLD_ARMOR_MATERIAL =
        ARMOR_MATERIALS.register("temporal_guardian", () -> create(
            "temporal_guardian",
            createDefenseMap(3, 6, 8, 3),
            37,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0F,
            0.1F
        ));

    private static ArmorMaterial create(String name, Map<ArmorItem.Type, Integer> defense,
            int enchantability, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> equipSound,
            float toughness, float knockbackResistance) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, name);
        return new ArmorMaterial(
            defense,
            enchantability,
            equipSound,
            () -> Ingredient.EMPTY,
            List.of(new ArmorMaterial.Layer(id)),
            toughness,
            knockbackResistance
        );
    }

    private static Map<ArmorItem.Type, Integer> createDefenseMap(int boots, int leggings, int chestplate, int helmet) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.LEGGINGS, leggings);
        map.put(ArmorItem.Type.CHESTPLATE, chestplate);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.BODY, chestplate);
        return map;
    }

    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
        QuantumFalcon.LOGGER.info("Registered {} armor materials (deferred)", ARMOR_MATERIALS.getEntries().size());
    }
}
