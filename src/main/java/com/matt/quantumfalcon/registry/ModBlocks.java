package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.block.FalconEggBlock;
import com.matt.quantumfalcon.blockentity.EntangledOreBlock;
import com.matt.quantumfalcon.dimension.RiftFrameBlock;
import com.matt.quantumfalcon.dimension.RiftPortalBlock;
import com.matt.quantumfalcon.hytale.ClimbableCrystalVineBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Block Registry for Quantum Falcon Mod
 * 
 * NOTE: This is a minimal implementation for NeoForge 1.21.4.
 * Custom block classes (QuantumNestBlock, QuantumForgeBlock, etc.) need to be
 * implemented before they can be registered here.
 */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuantumFalcon.MOD_ID);
    
    // ========================================
    // ORE BLOCKS
    // ========================================
    public static final DeferredBlock<Block> QUANTUM_ORE = registerBlockWithItem("quantum_ore", 
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .strength(4.5f, 3.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.DEEPSLATE)
            .lightLevel(state -> 3)), 
        new Item.Properties().rarity(Rarity.UNCOMMON));
    
    public static final DeferredBlock<Block> DEEPSLATE_QUANTUM_ORE = registerBlockWithItem("deepslate_quantum_ore", 
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .strength(6.0f, 3.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.DEEPSLATE)
            .lightLevel(state -> 5)), 
        new Item.Properties().rarity(Rarity.UNCOMMON));
    
    public static final DeferredBlock<Block> END_QUANTUM_ORE = registerBlockWithItem("end_quantum_ore", 
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .strength(5.0f, 9.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.STONE)
            .lightLevel(state -> 7)), 
        new Item.Properties().rarity(Rarity.RARE));
    
    public static final DeferredBlock<EntangledOreBlock> ENTANGLED_ORE = registerBlockWithItem("entangled_ore",
        () -> new EntangledOreBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(4.0f, 3.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.AMETHYST)
            .lightLevel(state -> 6)),
        new Item.Properties().rarity(Rarity.RARE));
    
    // ========================================
    // STORAGE BLOCKS
    // ========================================
    public static final DeferredBlock<Block> QUANTUM_BLOCK = registerBlockWithItem("quantum_block", 
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(50.0f, 1200.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.METAL)
            .lightLevel(state -> 7)), 
        new Item.Properties().rarity(Rarity.RARE));
    
    // ========================================
    // DECORATIVE BLOCKS
    // ========================================
    
    public static final DeferredBlock<Block> QUANTUM_GLASS = registerBlockWithItem("quantum_glass", 
        () -> new net.minecraft.world.level.block.TransparentBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(0.3f)
            .sound(SoundType.GLASS)
            .lightLevel(state -> 5)
            .noOcclusion()), 
        new Item.Properties().rarity(Rarity.UNCOMMON));
    
    public static final DeferredBlock<Block> RIFT_STONE = registerBlockWithItem("rift_stone", 
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(1.5f, 6.0f)
            .sound(SoundType.STONE)
            .lightLevel(state -> 2)), 
        new Item.Properties());
    
    public static final DeferredBlock<Block> RIFT_STONE_BRICKS = registerBlockWithItem("rift_stone_bricks", 
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)), 
        new Item.Properties());
    
    public static final DeferredBlock<Block> VOID_BRICKS = registerBlockWithItem("void_bricks", 
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(3.0f, 6.0f)
            .sound(SoundType.NETHER_BRICKS)
            .lightLevel(state -> 1)), 
        new Item.Properties().rarity(Rarity.RARE));
    
    public static final DeferredBlock<Block> QUANTUM_LAMP = registerBlockWithItem("quantum_lamp", 
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(0.3f)
            .sound(SoundType.GLASS)
            .lightLevel(state -> 15)), 
        new Item.Properties().rarity(Rarity.UNCOMMON));
    
    // ========================================
    // WORLD GEN BLOCKS (placeholder - simple blocks for now)
    // ========================================
    public static final DeferredBlock<Block> QUANTUM_STONE = registerBlockWithItem("quantum_stone", 
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(1.5f, 6.0f)
            .requiresCorrectToolForDrops()   // Rift bulk stone needs a pickaxe (any tier), like vanilla stone
            .sound(SoundType.STONE)
            .lightLevel(state -> 3)), 
        new Item.Properties().rarity(Rarity.UNCOMMON));
    
    public static final DeferredBlock<Block> QUANTUM_SOIL = registerBlockWithItem("quantum_soil", 
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(0.6f)
            .sound(SoundType.GRAVEL)), 
        new Item.Properties());
    
    public static final DeferredBlock<Block> CRYSTALLIZED_STONE = registerBlockWithItem("crystallized_stone", 
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN)
            .strength(3.0f, 9.0f)
            .sound(SoundType.AMETHYST)
            .lightLevel(state -> 8)), 
        new Item.Properties());
    
    public static final DeferredBlock<Block> CRYSTAL_CLUSTER = registerBlockWithItem("crystal_cluster", 
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN)
            .strength(1.5f)
            .sound(SoundType.AMETHYST_CLUSTER)
            .lightLevel(state -> 7)
            .noOcclusion()), 
        new Item.Properties().rarity(Rarity.RARE));
    
    // ========================================
    // DIMENSION BLOCKS
    // ========================================
    public static final DeferredBlock<RiftFrameBlock> RIFT_FRAME = registerBlockWithItem("rift_frame",
        () -> new RiftFrameBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(50.0f, 1200.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.NETHERITE_BLOCK)
            .lightLevel(state -> state.getValue(RiftFrameBlock.ACTIVATED) ? 12 : 3)),
        new Item.Properties().rarity(Rarity.EPIC));
    
    public static final DeferredBlock<RiftPortalBlock> RIFT_PORTAL = BLOCKS.register("rift_portal",
        () -> new RiftPortalBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .noCollission()
            .strength(-1.0f)
            .sound(SoundType.GLASS)
            .lightLevel(state -> 15)
            .noLootTable()
            .pushReaction(PushReaction.BLOCK)));
    // No item for portal block - only created via rift frame
    
    // ========================================
    // HYTALE-INSPIRED BLOCKS
    // ========================================
    public static final DeferredBlock<ClimbableCrystalVineBlock> CRYSTAL_VINE = registerBlockWithItem("crystal_vine",
        () -> new ClimbableCrystalVineBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN)
            .noCollission()
            .strength(0.2f)
            .sound(SoundType.VINE)
            .noOcclusion()
            .randomTicks()),
        new Item.Properties().rarity(Rarity.UNCOMMON));
    
    // ========================================
    // FUNCTIONAL BLOCKS
    // ========================================
    public static final DeferredBlock<com.matt.quantumfalcon.block.FalconPerchBlock> FALCON_PERCH =
        registerBlockWithItem("falcon_perch",
            () -> new com.matt.quantumfalcon.block.FalconPerchBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(1.5f, 3.0f)
                .sound(SoundType.WOOD)
                .noOcclusion()),
            new Item.Properties());

    /** Decor perch (no POI): Mews loft, Aerie Sentinel towers. hooded is a worldgen-only state in v1. */
    public static final DeferredBlock<com.matt.quantumfalcon.block.PerchStandBlock> PERCH_STAND =
        registerBlockWithItem("perch_stand",
            () -> new com.matt.quantumfalcon.block.PerchStandBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD).strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion()),
            new Item.Properties());

    /** The Stitchring bell (static model, no block entity): rings on use or redstone. */
    public static final DeferredBlock<com.matt.quantumfalcon.block.ResonanceBellBlock> RESONANCE_BELL =
        registerBlockWithItem("resonance_bell",
            () -> new com.matt.quantumfalcon.block.ResonanceBellBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(SoundType.ANVIL).noOcclusion().requiresCorrectToolForDrops()),
            new Item.Properties());

    public static final DeferredBlock<com.matt.quantumfalcon.block.QuantumNestBlock> QUANTUM_NEST = 
        registerBlockWithItem("quantum_nest",
            () -> new com.matt.quantumfalcon.block.QuantumNestBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOL)
                .lightLevel(state -> state.getValue(com.matt.quantumfalcon.block.QuantumNestBlock.ACTIVE) ? 8 : 0)),
            new Item.Properties().rarity(Rarity.UNCOMMON));
    
    public static final DeferredBlock<com.matt.quantumfalcon.block.FalconNestBlock> TWIG_NEST =
        registerBlockWithItem("twig_nest", () -> new com.matt.quantumfalcon.block.FalconNestBlock(com.matt.quantumfalcon.breeding.NestQuality.TWIG,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(1.0f, 6.0f).sound(SoundType.GRASS).noOcclusion()),
            new Item.Properties().rarity(Rarity.COMMON));
    public static final DeferredBlock<com.matt.quantumfalcon.block.FalconNestBlock> CRYSTAL_NEST =
        registerBlockWithItem("crystal_nest", () -> new com.matt.quantumfalcon.block.FalconNestBlock(com.matt.quantumfalcon.breeding.NestQuality.CRYSTAL,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(2.5f, 6.0f).sound(SoundType.AMETHYST).noOcclusion().lightLevel(s -> 6)),
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredBlock<com.matt.quantumfalcon.block.FalconNestBlock> VOIDFORGED_NEST =
        registerBlockWithItem("voidforged_nest", () -> new com.matt.quantumfalcon.block.FalconNestBlock(com.matt.quantumfalcon.breeding.NestQuality.VOIDFORGED,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5f, 6.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion().lightLevel(s -> 8)),
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredBlock<com.matt.quantumfalcon.block.FalconNestBlock> QUANTUM_AERIE =
        registerBlockWithItem("quantum_aerie", () -> new com.matt.quantumfalcon.block.FalconNestBlock(com.matt.quantumfalcon.breeding.NestQuality.AERIE,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(2.5f, 6.0f).sound(SoundType.AMETHYST).noOcclusion().lightLevel(s -> 11)),
            new Item.Properties().rarity(Rarity.EPIC));

    public static final DeferredBlock<com.matt.quantumfalcon.block.QuantumIncubatorBlock> QUANTUM_INCUBATOR = 
        registerBlockWithItem("quantum_incubator",
            () -> new com.matt.quantumfalcon.block.QuantumIncubatorBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(3.0f, 6.0f)
                .sound(SoundType.METAL)
                .lightLevel(state -> 10)),
            new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredBlock<FalconEggBlock> FALCON_EGG_BLOCK = 
        BLOCKS.register("falcon_egg_block",
            () -> new FalconEggBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(0.5f)
                .sound(SoundType.CORAL_BLOCK)
                .noOcclusion()
                .lightLevel(state -> 2)));
    
    public static final DeferredBlock<com.matt.quantumfalcon.block.QuantumForgeBlock> QUANTUM_FORGE = 
        registerBlockWithItem("quantum_forge",
            () -> new com.matt.quantumfalcon.block.QuantumForgeBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(5.0f, 6.0f)
                .sound(SoundType.METAL)
                .lightLevel(state -> state.getValue(com.matt.quantumfalcon.block.QuantumForgeBlock.ACTIVE) ? 12 : 0)),
            new Item.Properties().rarity(Rarity.RARE));
    
    public static final DeferredBlock<com.matt.quantumfalcon.block.RiftIgniterBlock> RIFT_IGNITER = 
        registerBlockWithItem("rift_igniter",
            () -> new com.matt.quantumfalcon.block.RiftIgniterBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(3.0f, 6.0f)
                .sound(SoundType.METAL)
                .lightLevel(state -> 7)),
            new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<com.matt.quantumfalcon.block.AquaAegisReliquaryBlock> AQUA_AEGIS_RELIQUARY = 
        registerBlockWithItem("aqua_aegis_reliquary",
            () -> new com.matt.quantumfalcon.block.AquaAegisReliquaryBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.GOLD)
                .strength(2.0f, 6.0f)
                .sound(SoundType.METAL)
                .lightLevel(state -> 10)),
            new Item.Properties().rarity(Rarity.EPIC));
    
    public static final DeferredBlock<com.matt.quantumfalcon.block.PedestalBlock> PEDESTAL = 
        registerBlockWithItem("pedestal",
            () -> new com.matt.quantumfalcon.block.PedestalBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0f, 6.0f)
                .sound(SoundType.STONE)),
            new Item.Properties());
    
    // ========================================
    // DECORATIVE BLOCKS (Using variant texture art)
    // Pattern: Same as Create mod's decorative blocks and Chisel mod's variants
    // ========================================
    
    // Quantum Bricks (6 variants of cyan-themed brick textures)
    public static final DeferredBlock<Block> QUANTUM_BRICKS = registerBlockWithItem("quantum_bricks",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
            .strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()),
        new Item.Properties());
    
    // Quantum Tiles (decorative tiled surface)
    public static final DeferredBlock<Block> QUANTUM_TILES = registerBlockWithItem("quantum_tiles",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
            .strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()),
        new Item.Properties());
    
    // Quantum Glass Cyan (translucent cyan glass)
    public static final DeferredBlock<Block> QUANTUM_GLASS_CYAN = registerBlockWithItem("quantum_glass_cyan",
        () -> new net.minecraft.world.level.block.HalfTransparentBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN).strength(0.3f).sound(SoundType.GLASS)
            .noOcclusion().isViewBlocking((s,g,p) -> false)),
        new Item.Properties());
    
    // Quantum Glass Purple (translucent purple glass)
    public static final DeferredBlock<Block> QUANTUM_GLASS_PURPLE = registerBlockWithItem("quantum_glass_purple",
        () -> new net.minecraft.world.level.block.HalfTransparentBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE).strength(0.3f).sound(SoundType.GLASS)
            .noOcclusion().isViewBlocking((s,g,p) -> false)),
        new Item.Properties());
    
    // Void Obsidian (ultra-hard dark block, 18 variants)
    public static final DeferredBlock<Block> VOID_OBSIDIAN = registerBlockWithItem("void_obsidian",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
            .strength(50.0f, 1200.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()),
        new Item.Properties());
    
    // Quantum Tech Cyan (tech/machine-looking decorative block)
    public static final DeferredBlock<Block> QUANTUM_TECH_CYAN = registerBlockWithItem("quantum_tech_cyan",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
            .strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()
            .lightLevel(state -> 5)),
        new Item.Properties());
    
    // Quantum Tech Purple
    public static final DeferredBlock<Block> QUANTUM_TECH_PURPLE = registerBlockWithItem("quantum_tech_purple",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
            .strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()
            .lightLevel(state -> 5)),
        new Item.Properties());
    
    // Echo Lattice (transparent decorative)
    public static final DeferredBlock<Block> ECHO_LATTICE = registerBlockWithItem("echo_lattice",
        () -> new net.minecraft.world.level.block.HalfTransparentBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLUE).strength(1.5f).sound(SoundType.GLASS)
            .noOcclusion().lightLevel(state -> 8)),
        new Item.Properties());
    
    // Temporal Tiles
    public static final DeferredBlock<Block> TEMPORAL_TILES = registerBlockWithItem("temporal_tiles",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
            .strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()),
        new Item.Properties());
    
    // Temporal Pillar
    public static final DeferredBlock<Block> TEMPORAL_PILLAR = registerBlockWithItem("temporal_pillar",
        () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.GOLD).strength(2.5f, 6.0f).sound(SoundType.STONE)
            .requiresCorrectToolForDrops()),
        new Item.Properties());
    
    // Singularity Core Block (decorative, emits light)
    public static final DeferredBlock<Block> SINGULARITY_CORE_BLOCK = registerBlockWithItem("singularity_core_block",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
            .strength(4.0f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()
            .lightLevel(state -> 12)),
        new Item.Properties().rarity(Rarity.RARE));
    
    // Void Stone Bricks
    public static final DeferredBlock<Block> VOID_STONE_BRICKS = registerBlockWithItem("void_stone_bricks",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
            .strength(3.0f, 8.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()),
        new Item.Properties());
    
    // Chaos Conduit (redstone-like decorative)
    public static final DeferredBlock<Block> CHAOS_CONDUIT = registerBlockWithItem("chaos_conduit",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
            .strength(2.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()
            .lightLevel(state -> 7)),
        new Item.Properties());

    // ========================================
    // HELPER METHODS
    // ========================================
    private static <T extends Block> DeferredBlock<T> registerBlockWithItem(String name, Supplier<T> blockSupplier, Item.Properties itemProperties) {
        DeferredBlock<T> block = BLOCKS.register(name, blockSupplier);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), itemProperties));
        return block;
    }
    
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        QuantumFalcon.LOGGER.info("⚡ Registered {} blocks", BLOCKS.getEntries().size());
    }
}
