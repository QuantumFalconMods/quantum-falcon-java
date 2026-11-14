package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.blockentity.*;
import com.matt.quantumfalcon.breeding.BreedingEngine;
import com.matt.quantumfalcon.item.FalconEggItem;
import com.matt.quantumfalcon.registry.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** Blocks/workstations review probes (2026-11-14 review), folded into the suite. */
final class BKTests {
    private BKTests() {}
    static void register(Map<String, Consumer<ServerLevel>> t) {
        t.put("bk_dump", l -> ready(l, 21000, BKTests::dump));
        t.put("bk_entanglement_chest_break", l -> ready(l, 21100, BKTests::chestBreak));
        t.put("bk_entangled_ore_mine", l -> ready(l, 21200, BKTests::oreMine));
        t.put("bk_incubator_owner", l -> ready(l, 21300, BKTests::incubatorOwner));
        t.put("bk_egg_explosion", l -> ready(l, 21400, BKTests::eggExplosion));
        t.put("bk_superposition_cycle", l -> ready(l, 21500, BKTests::superposition));
        t.put("bk_incubator_quantum_scan", l -> ready(l, 21600, BKTests::quantumScan));
        t.put("bk_vine_growth", l -> ready(l, 21700, BKTests::vine));
        t.put("bk_quantum_nest_tick", l -> ready(l, 21800, BKTests::nestTick));
        t.put("bk_pedestal", l -> ready(l, 21900, BKTests::pedestal));
        t.put("bk_self_drops", l -> ready(l, 22000, BKTests::selfDrops));
    }

    /** Entities and block entities only tick, and are only visible to queries, once the pad's chunks are entity-ticking. */
    static void ready(ServerLevel l, int x, java.util.function.Consumer<ServerLevel> body) {
        BlockPos q = new BlockPos(x, 64, 21000);
        WorldDropsTests.force(l, q, true);
        WorldDropsTests.whenTicking(l, q, 0, () -> body.accept(l));
    }

    static int countItems(ServerLevel l, BlockPos p, Item it) {
        int n = 0;
        for (ItemEntity e : l.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(4))) if (e.getItem().is(it)) n += e.getItem().getCount();
        return n;
    }
    static void clearItems(ServerLevel l, BlockPos p) { l.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(6)).forEach(e -> e.discard()); }

    static ItemStack pick(ServerLevel l, boolean silk) {
        ItemStack s = new ItemStack(Items.NETHERITE_PICKAXE);
        if (silk) s.enchant(l.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        return s;
    }

    // ---------------------------------------------------------------- registry dump
    static void dump(ServerLevel level) {
        BlockPos base = pad(level, 21000, 21000, Blocks.STONE);
        FakePlayer fp = tester(level);
        // recipes: item id -> count of recipes producing it
        Map<String, Integer> recipeOut = new HashMap<>();
        for (var h : level.getRecipeManager().getRecipes()) {
            try { ItemStack r = h.value().getResultItem(level.registryAccess()); if (!r.isEmpty()) recipeOut.merge(BuiltInRegistries.ITEM.getKey(r.getItem()).toString(), 1, Integer::sum); } catch (Exception ignored) {}
        }
        // structure palettes
        Map<String, Integer> inStructures = new HashMap<>();
        int templates = 0;
        var rm = level.getServer().getResourceManager();
        for (var e : rm.listResources("structure", rl -> rl.getPath().endsWith(".nbt")).keySet()) {
            String path = e.getPath().substring("structure/".length(), e.getPath().length() - 4);
            try {
                var opt = level.getStructureManager().get(ResourceLocation.fromNamespaceAndPath(e.getNamespace(), path));
                if (opt.isEmpty()) continue;
                templates++;
                CompoundTag tag = opt.get().save(new CompoundTag());
                List<ListTag> pals = new ArrayList<>();
                if (tag.contains("palette")) pals.add(tag.getList("palette", Tag.TAG_COMPOUND));
                if (tag.contains("palettes")) { ListTag ps = tag.getList("palettes", Tag.TAG_LIST); for (int i = 0; i < ps.size(); i++) pals.add(ps.getList(i)); }
                Set<String> seen = new HashSet<>();
                for (ListTag pl : pals) for (int i = 0; i < pl.size(); i++) seen.add(pl.getCompound(i).getString("Name"));
                for (String s : seen) inStructures.merge(s, 1, Integer::sum);
            } catch (Exception ex) { }
        }
        List<String> out = new ArrayList<>();
        out.add("id\tclass\titem\tbe\tticker\tlight\thardness\tdrops_pick\tdrops_silk\tself_drop\trecipes\tstructures");
        int i = 0, n = 0;
        for (var key : BuiltInRegistries.BLOCK.keySet()) {
            if (!key.getNamespace().equals("quantumfalcon")) continue;
            n++;
            Block b = BuiltInRegistries.BLOCK.get(key);
            BlockState st = b.defaultBlockState();
            Item item = b.asItem();
            boolean hasItem = item != Items.AIR;
            BlockPos p = base.offset((i % 7) - 3, 1 + (i / 7) % 4, ((i / 28) % 7) - 3);
            i++;
            String be = "-", ticker = "-", dp = "?", ds = "?";
            boolean selfDrop = false;
            try {
                level.setBlock(p, st, 2 | 16);
                BlockEntity bent = level.getBlockEntity(p);
                if (bent != null) {
                    be = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(bent.getType()).getPath();
                    if (b instanceof EntityBlock eb) ticker = eb.getTicker(level, st, bent.getType()) != null ? "server" : "none";
                }
                ItemStack tool = pick(level, false);
                fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tool);
                List<ItemStack> d1 = Block.getDrops(level.getBlockState(p), level, p, bent, fp, tool);
                ItemStack silk = pick(level, true);
                List<ItemStack> d2 = Block.getDrops(level.getBlockState(p), level, p, bent, fp, silk);
                dp = fmt(d1); ds = fmt(d2);
                for (ItemStack s : d1) if (hasItem && s.is(item)) selfDrop = true;
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            } catch (Exception ex) { dp = "ERR " + ex.getClass().getSimpleName(); }
            out.add(key.getPath() + "\t" + b.getClass().getSimpleName() + "\t" + (hasItem ? BuiltInRegistries.ITEM.getKey(item).getPath() : "-") + "\t" + be + "\t" + ticker
                + "\t" + st.getLightEmission() + "\t" + st.getDestroySpeed(level, p) + "\t" + dp + "\t" + ds + "\t" + selfDrop
                + "\t" + (hasItem ? recipeOut.getOrDefault(BuiltInRegistries.ITEM.getKey(item).toString(), 0) : 0)
                + "\t" + inStructures.getOrDefault(key.toString(), 0));
        }
        try { Files.write(Paths.get("bk_dump.tsv"), out); } catch (Exception ex) { }
        result("bk_dump", true, "blocks=" + n + " templates=" + templates + " -> run/bk_dump.tsv");
    }
    static String fmt(List<ItemStack> l) {
        if (l.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        for (ItemStack s : l) { if (sb.length() > 0) sb.append(','); sb.append(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath()).append('x').append(s.getCount()); }
        return sb.toString();
    }

    // ---------------------------------------------------------------- entanglement chest
    static void chestBreak(ServerLevel level) {
        BlockPos p = pad(level, 21100, 21000, Blocks.STONE);
        clearItems(level, p);
        level.setBlock(p, ModFamilyBlocks.ENTANGLEMENT_CHEST.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)) { result("bk_entanglement_chest_break", false, "no container BE"); return; }
        c.setItem(0, new ItemStack(Items.DIAMOND, 5));
        c.setItem(5, new ItemStack(Items.GOLD_INGOT, 7));
        FakePlayer fp = tester(level);
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_AXE));
        fp.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 2.5);
        boolean broke = fp.gameMode.destroyBlock(p);
        final String dbg = " [now: dia=" + countItems(level, p, Items.DIAMOND) + " entTicking=" + level.isPositionEntityTicking(p) + " mode=" + fp.gameMode.getGameModeForPlayer() + " block=" + level.getBlockState(p).getBlock() + "]";
        later(3, () -> {
            int dia = countItems(level, p, Items.DIAMOND), gold = countItems(level, p, Items.GOLD_INGOT), chest = countItems(level, p, ModFamilyBlocks.ENTANGLEMENT_CHEST.get().asItem());
            result("bk_entanglement_chest_break", dia == 5 && gold == 7, "broke=" + broke + " in 5 diamond + 7 gold -> dropped diamond=" + dia + " gold=" + gold + " chest item=" + chest + " (FAIL = contents destroyed)" + dbg);
        });
    }

    // ---------------------------------------------------------------- entangled ore
    static void oreMine(ServerLevel level) {
        BlockPos p = pad(level, 21200, 21000, Blocks.STONE);
        clearItems(level, p);
        UUID netId = UUID.randomUUID();
        for (int k = 0; k < 3; k++) {
            BlockPos q = p.offset(k * 2 - 2, 0, 0);
            level.setBlock(q, ModBlocks.ENTANGLED_ORE.get().defaultBlockState(), 3);
            if (level.getBlockEntity(q) instanceof EntangledOreBlockEntity be) be.linkToNetwork(netId);
        }
        BlockPos lone = p.offset(0, 0, 3);
        level.setBlock(lone, ModBlocks.ENTANGLED_ORE.get().defaultBlockState(), 3);
        FakePlayer fp = tester(level);
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        fp.moveTo(p.getX() + 0.5, p.getY(), p.getZ() - 2.5);
        boolean b1 = fp.gameMode.destroyBlock(p.offset(-2, 0, 0));
        boolean b2 = fp.gameMode.destroyBlock(lone);
        later(3, () -> {
            int items = level.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(6)).stream().mapToInt(e -> e.getItem().getCount()).sum();
            int left = 0; for (int k = 0; k < 3; k++) if (level.getBlockState(p.offset(k * 2 - 2, 0, 0)).is(ModBlocks.ENTANGLED_ORE.get())) left++;
            result("bk_entangled_ore_mine", items > 0, "network of 3 + 1 lone ore, mined one networked (" + b1 + ") and the lone one (" + b2 + "): networked ores left=" + left + ", item entities dropped=" + items + " (FAIL = mining yields nothing)");
        });
    }

    // ---------------------------------------------------------------- incubator owner
    static void incubatorOwner(ServerLevel level) {
        BlockPos p = pad(level, 21300, 21000, Blocks.STONE);
        level.setBlock(p, ModBlocks.QUANTUM_INCUBATOR.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(p) instanceof QuantumIncubatorBlockEntity be)) { result("bk_incubator_owner", false, "no BE"); return; }
        FakePlayer a = tester(level);
        FakePlayer b = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("00000000-0000-4bbb-8000-00000000bbbb"), "Stranger"));
        ItemStack egg = FalconEggItem.wild(new ItemStack(ModItems.FALCON_EGG.get()), BreedingEngine.wild("peregrine", 1, level.getRandom()));
        a.getInventory().clearContent(); a.getInventory().setItem(9, egg);
        var menuA = (net.minecraft.world.inventory.AbstractContainerMenu) be.createMenu(1, a.getInventory(), a);   // owner A loads the egg through the GUI
        menuA.quickMoveStack(a, 2);   // inventory slot 9 is menu slot 2 (after the egg and fuel slots)
        boolean loaded = !be.getItem(QuantumIncubatorBlockEntity.SLOT_EGG).isEmpty();
        be.createMenu(2, b.getInventory(), b);   // stranger B only opens the GUI
        // a hopper-style insert next door (no player) hatches untamed
        BlockPos p2 = p.east(2);
        level.setBlock(p2, ModBlocks.QUANTUM_INCUBATOR.get().defaultBlockState(), 3);
        QuantumIncubatorBlockEntity hop = (QuantumIncubatorBlockEntity) level.getBlockEntity(p2);
        hop.createMenu(3, b.getInventory(), b);
        hop.setItem(QuantumIncubatorBlockEntity.SLOT_EGG, FalconEggItem.wild(new ItemStack(ModItems.FALCON_EGG.get()), BreedingEngine.wild("peregrine", 1, level.getRandom())));
        boolean hopperUnowned = hop.eggOwner() == null;
        level.setBlock(p2, Blocks.AIR.defaultBlockState(), 3);
        be.setProgressForTest(0.0f);
        later(2, () -> {
            be.setProgressForTest(0.9995f);
            later(40, () -> {
                var f = be.lastHatched;
                UUID owner = f == null ? null : f.getOwnerUUID();
                result("bk_incubator_owner", loaded && hopperUnowned && f != null && a.getUUID().equals(owner),
                    "loaded through the GUI=" + loaded + " hopper-fed egg unowned=" + hopperUnowned + " | wild egg loaded by A, GUI opened last by B -> hatched=" + (f != null) + " owner=" + (owner == null ? "none" : owner.equals(a.getUUID()) ? "A (loader)" : owner.equals(b.getUUID()) ? "B (stranger who only opened the GUI)" : owner.toString()) + " (FAIL = last GUI opener owns the hatch)");
            });
        });
    }

    // ---------------------------------------------------------------- egg explosion
    static void eggExplosion(ServerLevel level) {
        BlockPos p = pad(level, 21400, 21000, Blocks.OBSIDIAN);
        clearItems(level, p);
        level.setBlock(p, ModBlocks.FALCON_EGG_BLOCK.get().defaultBlockState(), 3);
        if (level.getBlockEntity(p) instanceof FalconEggBlockEntity be) be.setWildChild(BreedingEngine.wild("peregrine", 3, level.getRandom()));
        level.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 1.5, 3.0f, Level.ExplosionInteraction.TNT);
        later(3, () -> {
            boolean gone = !level.getBlockState(p).is(ModBlocks.FALCON_EGG_BLOCK.get());
            int eggs = countItems(level, p, ModItems.FALCON_EGG.get());
            result("bk_egg_explosion", !gone || eggs > 0, "rare egg block hit by a TNT-size blast: block gone=" + gone + ", egg items dropped=" + eggs + " (FAIL = egg destroyed with no drop)");
        });
    }

    // ---------------------------------------------------------------- superposition
    static void superposition(ServerLevel level) {
        BlockPos p = pad(level, 21500, 21000, Blocks.STONE);
        Block sb = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", "superposition_block"));
        level.setBlock(p, sb.defaultBlockState(), 3);
        level.setBlock(p.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        List<String> states = new ArrayList<>();
        states.add(stateName(level.getBlockState(p)));
        for (int k = 0; k < 10; k++) {
            level.setBlock(p.north(), k % 2 == 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            states.add(stateName(level.getBlockState(p)));
        }
        int changes = 0; for (int k = 1; k < states.size(); k++) if (!states.get(k).equals(states.get(k - 1))) changes++;
        result("bk_superposition_cycle", changes <= 1, "redstone block beside it, then 10 unrelated neighbour updates: state changes=" + changes + " " + states + " (FAIL = any neighbour update re-cycles a powered block)");
    }
    static String stateName(BlockState s) { for (var pr : s.getProperties()) if (pr.getName().equals("quantum_state")) return s.getValue(pr).toString(); return s.getBlock().toString(); }

    // ---------------------------------------------------------------- incubator quantum-tier scan cost
    static void quantumScan(ServerLevel level) {
        BlockPos p = pad(level, 21600, 21000, Blocks.STONE);
        level.setBlock(p, ModBlocks.QUANTUM_INCUBATOR.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(p) instanceof QuantumIncubatorBlockEntity be)) { result("bk_incubator_quantum_scan", false, "no BE"); return; }
        ItemStack egg7 = FalconEggItem.wild(new ItemStack(ModItems.FALCON_EGG.get()), BreedingEngine.wild("quantum_phoenix", 7, level.getRandom()));
        ItemStack egg1 = FalconEggItem.wild(new ItemStack(ModItems.FALCON_EGG.get()), BreedingEngine.wild("peregrine", 1, level.getRandom()));
        long t7 = time(level, p, be, egg7), t1 = time(level, p, be, egg1);
        result("bk_incubator_quantum_scan", true, "2000 serverTicks: Quantum-tier egg (no forge, overworld) " + t7 / 1000 + " us total, status=" + be.status() + "; Common egg " + t1 / 1000 + " us total");
    }
    static long time(ServerLevel level, BlockPos p, QuantumIncubatorBlockEntity be, ItemStack egg) {
        be.setItem(0, egg.copy());
        for (int k = 0; k < 200; k++) QuantumIncubatorBlockEntity.serverTick(level, p, level.getBlockState(p), be);
        long t0 = System.nanoTime();
        for (int k = 0; k < 2000; k++) QuantumIncubatorBlockEntity.serverTick(level, p, level.getBlockState(p), be);
        long t = System.nanoTime() - t0;
        be.setItem(0, ItemStack.EMPTY);
        return t;
    }

    // ---------------------------------------------------------------- crystal vine
    static void vine(ServerLevel level) {
        BlockPos p = pad(level, 21700, 21000, Blocks.STONE);
        level.setBlock(p, ModBlocks.CRYSTAL_VINE.get().defaultBlockState(), 3);
        for (int k = 0; k < 4000; k++) {
            BlockPos top = p; while (level.getBlockState(top.above()).is(ModBlocks.CRYSTAL_VINE.get())) top = top.above();
            if (level.getBlockState(top).getBlock() instanceof net.minecraft.world.level.block.Block bb) level.getBlockState(top).randomTick(level, top, level.getRandom());
        }
        int h = 0; BlockPos q = p; while (level.getBlockState(q).is(ModBlocks.CRYSTAL_VINE.get())) { h++; q = q.above(); }
        result("bk_vine_growth", h < 32, "one free-standing vine, 4000 random ticks on its top: height=" + h + " (FAIL = no height cap)");
    }

    // ---------------------------------------------------------------- quantum nest
    static void nestTick(ServerLevel level) {
        BlockPos p = pad(level, 21800, 21000, Blocks.STONE);
        level.setBlock(p, ModBlocks.QUANTUM_NEST.get().defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(p);
        result("bk_quantum_nest_tick", true, "quantum_nest BE=" + (be == null ? "none" : BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType())) + " ticker=" + (be != null && ((EntityBlock) ModBlocks.QUANTUM_NEST.get()).getTicker(level, level.getBlockState(p), be.getType()) != null));
    }

    // ---------------------------------------------------------------- pedestal
    static void pedestal(ServerLevel level) {
        BlockPos p = pad(level, 21900, 21000, Blocks.STONE);
        clearItems(level, p);
        level.setBlock(p.above(), ModBlocks.PEDESTAL.get().defaultBlockState(), 3);
        BlockPos ped = p.above();
        FakePlayer fp = tester(level);
        fp.moveTo(ped.getX() + 0.5, ped.getY(), ped.getZ() + 2.5);
        ItemStack gem = new ItemStack(Items.DIAMOND, 3);
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gem);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(ped), net.minecraft.core.Direction.UP, ped, false);
        fp.gameMode.useItemOn(fp, level, gem, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        boolean shows = com.matt.quantumfalcon.block.PedestalBlock.showing(level, ped).is(Items.DIAMOND);
        int left = fp.getMainHandItem().getCount();
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        fp.getInventory().clearContent();
        fp.gameMode.useItemOn(fp, level, ItemStack.EMPTY, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        boolean back = fp.getInventory().countItem(Items.DIAMOND) == 1 && com.matt.quantumfalcon.block.PedestalBlock.showing(level, ped).isEmpty();
        // breaking a loaded pedestal drops the shown item
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD));
        fp.gameMode.useItemOn(fp, level, fp.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        level.destroyBlock(ped, true);
        later(3, () -> {
            int em = countItems(level, ped, Items.EMERALD);
            fp.getInventory().clearContent();
            result("bk_pedestal", shows && left == 2 && back && em == 1, "placed a diamond: shown=" + shows + " hand left=" + left + " | empty hand takes it back=" + back + " | broken while showing an emerald: dropped=" + em);
        });
    }

    // ---------------------------------------------------------------- self drops
    static void selfDrops(ServerLevel level) {
        BlockPos p = pad(level, 22000, 21000, Blocks.STONE);
        StringBuilder bad = new StringBuilder(); int ok = 0;
        String[] ids = {"rift_igniter", "rift_stabilizer", "quantum_bricks", "quantum_tiles", "quantum_lamp", "void_obsidian", "echo_lattice", "temporal_tiles", "temporal_pillar",
            "rift_stone_bricks", "singularity_core_block", "quantum_soil", "quantum_tech_cyan", "quantum_tech_purple", "chaos_conduit"};
        for (String id : ids) {
            Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", id));
            BlockState st = b.defaultBlockState();
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(p))
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, new ItemStack(Items.NETHERITE_PICKAXE));
            var drops = st.getDrops(params);
            if (drops.stream().anyMatch(d -> d.is(b.asItem()))) ok++; else bad.append(id).append(' ');
        }
        result("bk_self_drops", ok == ids.length, ok + "/" + ids.length + " drop themselves with a pickaxe; missing: " + bad);
    }
}
