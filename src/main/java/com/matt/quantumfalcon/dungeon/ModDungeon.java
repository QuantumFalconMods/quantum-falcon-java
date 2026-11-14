package com.matt.quantumfalcon.dungeon;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Registry for the Echo dungeon kit: key door, treasury vault, trap plate, lore lectern, danger sign, checkpoint, return rift, guard, and the per-realm keys. */
public final class ModDungeon {
    private ModDungeon() {}

    public static final String[] REALMS = {"riftslate", "aurorite", "prismite", "cinderite", "chronoslate", "abyssite", "voidshale", "singularite"};

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuantumFalcon.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuantumFalcon.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuantumFalcon.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, QuantumFalcon.MOD_ID);

    private static BlockBehaviour.Properties vaultMetal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.NETHERITE_BLOCK)
            .strength(-1.0f, 3600000.0f).noLootTable().pushReaction(PushReaction.BLOCK).lightLevel(s -> 6);
    }

    public static final DeferredBlock<DungeonKeyDoorBlock> KEY_DOOR = BLOCKS.register("dungeon_key_door",
        () -> new DungeonKeyDoorBlock(vaultMetal().noOcclusion()));
    public static final DeferredBlock<TreasuryVaultBlock> TREASURY_VAULT = BLOCKS.register("treasury_vault",
        () -> new TreasuryVaultBlock(vaultMetal()));
    public static final DeferredBlock<TrapPlateBlock> TRAP_PLATE = BLOCKS.register("trap_plate",
        () -> new TrapPlateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.STONE)
            .strength(1.5f, 6.0f).noCollission().pushReaction(PushReaction.DESTROY).lightLevel(s -> 3)));
    public static final DeferredBlock<LoreLecternBlock> LORE_LECTERN = BLOCKS.register("lore_lectern",
        () -> new LoreLecternBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.NETHERITE_BLOCK)
            .strength(2.5f, 1200.0f).noOcclusion().lightLevel(s -> 5)));
    public static final DeferredBlock<DangerSignBlock> DANGER_SIGN = BLOCKS.register("danger_sign",
        () -> new DangerSignBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.NETHERITE_BLOCK)
            .strength(2.0f, 1200.0f).noOcclusion().noCollission().lightLevel(s -> 7)));
    public static final DeferredBlock<DungeonCheckpointBlock> CHECKPOINT = BLOCKS.register("dungeon_checkpoint",
        () -> new DungeonCheckpointBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).sound(SoundType.AMETHYST)
            .strength(-1.0f, 3600000.0f).noLootTable().lightLevel(s -> 10)));
    /** A10 Frozen Hours: flips a then/now volume (placeholder art until art notes: frozen_hours lands). */
    public static final DeferredBlock<HourSocketBlock> HOUR_SOCKET = BLOCKS.register("hour_socket",
        () -> new HourSocketBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).sound(SoundType.AMETHYST)
            .strength(-1.0f, 3600000.0f).noLootTable().noOcclusion().lightLevel(s -> s.getValue(HourSocketBlock.CHARGED) ? 13 : 8)));
    public static final DeferredBlock<ReturnRiftBlock> RETURN_RIFT = BLOCKS.register("return_rift",
        () -> new ReturnRiftBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).sound(SoundType.AMETHYST)
            .strength(-1.0f, 3600000.0f).noLootTable().lightLevel(s -> 12)));
    public static final DeferredBlock<DungeonGuardBlock> GUARD = BLOCKS.register("dungeon_guard",
        () -> new DungeonGuardBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.NETHERITE_BLOCK)
            .strength(-1.0f, 3600000.0f).noLootTable().lightLevel(s -> 4)));

    /** A7: the one dungeon wall that gives way in survival (secret passages); everything else in a dungeon is warded. */
    public static final DeferredBlock<Block> SECRET_WALL = BLOCKS.register("secret_wall",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.DEEPSLATE_BRICKS)
            .strength(2.0f, 6.0f).noLootTable()));

    /** Glowing void growth that creeps over dungeon and arena stonework (multiface, like glow lichen). */
    public static final DeferredBlock<net.minecraft.world.level.block.GlowLichenBlock> VOID_TENDRILS = BLOCKS.register("void_tendrils",
        () -> new net.minecraft.world.level.block.GlowLichenBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).replaceable()
            .noCollission().strength(0.2f).sound(SoundType.SCULK_VEIN).lightLevel(net.minecraft.world.level.block.GlowLichenBlock.emission(5))
            .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    static {
        blockItem("void_tendrils", VOID_TENDRILS, Rarity.COMMON);
        blockItem("secret_wall", SECRET_WALL, Rarity.UNCOMMON);
        ITEMS.register("dungeon_key_door", () -> new DoubleHighBlockItem(KEY_DOOR.get(), new Item.Properties().rarity(Rarity.EPIC)));
        blockItem("treasury_vault", TREASURY_VAULT, Rarity.EPIC);
        blockItem("trap_plate", TRAP_PLATE, Rarity.COMMON);
        blockItem("lore_lectern", LORE_LECTERN, Rarity.UNCOMMON);
        blockItem("danger_sign", DANGER_SIGN, Rarity.UNCOMMON);
        blockItem("dungeon_checkpoint", CHECKPOINT, Rarity.RARE);
        blockItem("hour_socket", HOUR_SOCKET, Rarity.EPIC);
        blockItem("return_rift", RETURN_RIFT, Rarity.RARE);
        blockItem("dungeon_guard", GUARD, Rarity.EPIC);
    }

    private static void blockItem(String name, Supplier<? extends Block> block, Rarity rarity) {
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().rarity(rarity)));
    }

    public static final Map<String, DeferredItem<DungeonKeyItem>> KEYS = new LinkedHashMap<>();
    static {
        for (String r : REALMS) {
            KEYS.put(r, ITEMS.register("dungeon_key_" + r, () -> new DungeonKeyItem(r, new Item.Properties().stacksTo(16).rarity(Rarity.RARE))));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TreasuryVaultBlockEntity>> TREASURY_VAULT_BE =
        BLOCK_ENTITIES.register("treasury_vault", () -> BlockEntityType.Builder.of(TreasuryVaultBlockEntity::new, TREASURY_VAULT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LoreLecternBlockEntity>> LORE_LECTERN_BE =
        BLOCK_ENTITIES.register("lore_lectern", () -> BlockEntityType.Builder.of(LoreLecternBlockEntity::new, LORE_LECTERN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DungeonGuardBlockEntity>> GUARD_BE =
        BLOCK_ENTITIES.register("dungeon_guard", () -> BlockEntityType.Builder.of(DungeonGuardBlockEntity::new, GUARD.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("quantumfalcon_dungeons", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.quantumfalcon.dungeons"))
        .icon(() -> new ItemStack(KEYS.get("riftslate").get()))
        .displayItems((params, out) -> ITEMS.getEntries().forEach(e -> out.accept(e.get())))
        .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
    }
}
