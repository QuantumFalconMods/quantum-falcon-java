package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.item.weapon.QFBowItem;
import com.matt.quantumfalcon.item.weapon.Weapons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/** Weapons and the Void Steel armour set added in the "armies, weapons and entities" pass. */
public final class ModGearItems {
    public static final List<DeferredItem<Item>> ALL = new ArrayList<>();
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, QuantumFalcon.MOD_ID);

    /** Matthew's Void Steel design (netherite-class, wither-proofed). */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> VOID_STEEL = MATERIALS.register("void_steel", () -> {
        EnumMap<ArmorItem.Type, Integer> m = new EnumMap<>(ArmorItem.Type.class);
        m.put(ArmorItem.Type.BOOTS, 3); m.put(ArmorItem.Type.LEGGINGS, 6); m.put(ArmorItem.Type.CHESTPLATE, 8); m.put(ArmorItem.Type.HELMET, 3); m.put(ArmorItem.Type.BODY, 8);
        return new ArmorMaterial(m, 16, SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "void_steel_ingot"))),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "void_steel"))), 3.0f, 0.1f);
    });

    public static final DeferredItem<Item> VOID_STEEL_INGOT = reg("void_steel_ingot", () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));
    public static final DeferredItem<Item> VOID_STEEL_HELMET = reg("void_steel_helmet", () -> armor(ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> VOID_STEEL_CHESTPLATE = reg("void_steel_chestplate", () -> armor(ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> VOID_STEEL_LEGGINGS = reg("void_steel_leggings", () -> armor(ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> VOID_STEEL_BOOTS = reg("void_steel_boots", () -> armor(ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> TEMPORAL_STAFF = reg("temporal_staff", Weapons.TemporalStaff::new);
    public static final DeferredItem<Item> QUANTUM_STAFF = reg("quantum_staff", Weapons.QuantumStaff::new);
    public static final DeferredItem<Item> RIFT_SPEAR = reg("rift_spear", Weapons.RiftSpear::new);
    public static final DeferredItem<Item> QUANTUM_SPEAR = reg("quantum_spear", Weapons.QuantumSpear::new);
    public static final DeferredItem<Item> CHAOS_HAMMER = reg("chaos_hammer", Weapons.ChaosHammer::new);
    public static final DeferredItem<Item> ECHO_DAGGER = reg("echo_dagger", Weapons.EchoDagger::new);
    public static final DeferredItem<Item> QUANTUM_DAGGER = reg("quantum_dagger", Weapons.QuantumDagger::new);
    public static final DeferredItem<Item> QUANTUM_BOW = reg("quantum_bow", () -> new QFBowItem("quantum", 600, Rarity.RARE));
    public static final DeferredItem<Item> SINGULARITY_BOW = reg("singularity_bow", () -> new QFBowItem("singularity", 1000, Rarity.EPIC));
    public static final DeferredItem<Item> FALCONER_GAUNTLET = reg("falconer_gauntlet", Weapons.FalconerGauntlet::new);

    private static Item armor(ArmorItem.Type t) {
        return new com.matt.quantumfalcon.item.gear.armor.QFGeoArmorItem(VOID_STEEL, t, new Item.Properties().durability(t.getDurability(35)).rarity(Rarity.EPIC).fireResistant(), "void_steel");
    }

    private static DeferredItem<Item> reg(String name, Supplier<? extends Item> sup) {
        @SuppressWarnings("unchecked")
        DeferredItem<Item> it = (DeferredItem<Item>) (DeferredItem<?>) ModItems.ITEMS.register(name, sup);
        ALL.add(it);
        return it;
    }

    public static void register(IEventBus bus) { MATERIALS.register(bus); QuantumFalcon.LOGGER.info("Quantum Falcon: {} gear items queued", ALL.size()); }
    private ModGearItems() {}
}
