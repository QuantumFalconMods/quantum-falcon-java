package com.matt.quantumfalcon.arena.fight;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Fight-time arena blocks: the energy seal ring, the shield pylons and the collapsing rift tiles. None has an item: the fight places and removes them. */
public final class ModArenaBlocks {
    private ModArenaBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuantumFalcon.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuantumFalcon.MOD_ID);

    public static final DeferredBlock<ArenaSealBlock> SEAL = BLOCKS.register("arena_seal",
        () -> new ArenaSealBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).sound(SoundType.AMETHYST)
            .strength(-1.0f, 3600000.0f).noLootTable().noOcclusion().lightLevel(s -> s.getValue(ArenaSealBlock.LIT) ? 9 : 0).pushReaction(PushReaction.BLOCK)
            .emissiveRendering((s, l, p) -> true)
            .isValidSpawn((s, l, p, t) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
    public static final DeferredBlock<ArenaPylonBlock> PYLON = BLOCKS.register("arena_pylon",
        () -> new ArenaPylonBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).sound(SoundType.AMETHYST_CLUSTER)
            .strength(4.0f, 6.0f).noLootTable().noOcclusion().lightLevel(s -> s.getValue(ArenaPylonBlock.CHARGED) ? 13 : 4).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<ArenaRiftTileBlock> RIFT_TILE = BLOCKS.register("arena_rift_tile",
        () -> new ArenaRiftTileBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.DEEPSLATE_TILES)
            .strength(-1.0f, 3600000.0f).noLootTable().noOcclusion().lightLevel(s -> 7).pushReaction(PushReaction.BLOCK)
            .isValidSpawn((s, l, p, t) -> false)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RiftTileBlockEntity>> RIFT_TILE_BE = BLOCK_ENTITIES.register("arena_rift_tile",
        () -> BlockEntityType.Builder.of(RiftTileBlockEntity::new, RIFT_TILE.get()).build(null));

    public static void register(IEventBus bus) { BLOCKS.register(bus); BLOCK_ENTITIES.register(bus); }
}
