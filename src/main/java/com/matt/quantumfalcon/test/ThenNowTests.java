package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.dungeon.HourSocketBlock;
import com.matt.quantumfalcon.dungeon.ModDungeon;
import com.matt.quantumfalcon.dungeon.ThenNow;
import com.matt.quantumfalcon.registry.ModDerivedBlocks;
import com.matt.quantumfalcon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/**
 * A10 "then/now" test box, built in a real Riftslate ruin corridor: in the Then hour the corridor is open with a floor;
 * in the Now hour a rubble wall blocks it and the floor ahead has fallen in. Checks: the flip opens and restores the path
 * exactly, it never touches doors or vaults (refused at registration, skipped if one turns up later), the Now ruin cannot
 * be mined through and the Now gap cannot be bridged (DungeonWard owns both palettes), the socket spends Temporal Dust
 * and keeps a Time Bottle, and the hour survives a real restart (restart_thennow_a/b).
 */
public final class ThenNowTests {
    private ThenNowTests() {}
    static final String VOL = "qf_test_then_now";
    static final Path STATE = Paths.get("qf_restart_thennow_state.txt");

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("then_now_flip", ThenNowTests::flip);
        tests.put("then_now_protected", ThenNowTests::protectedCells);
        tests.put("then_now_ward", ThenNowTests::ward);
        tests.put("then_now_socket", ThenNowTests::socket);
        tests.put("frozen_hours_blocks", ThenNowTests::blocks);
        tests.put("restart_thennow_a", ThenNowTests::restartA);
        tests.put("restart_thennow_b", ThenNowTests::restartB);
    }

    /** The test box: corridor start c (5 air cells east). Socket at c; wall at c+2 (2 high); floor gap under c+4. */
    record Box(BlockPos c, BlockPos socket, BlockPos wall, BlockPos gap, BoundingBox box, Map<BlockPos, BlockState[]> palette) {}

    static Box build(ServerLevel level, DungeonTests.Scan s) {
        BlockPos c = DungeonWardTests.corridor(level, s, 6);
        if (c == null) return null;
        BlockPos wall = c.east(2), gap = c.east(4).below();
        BlockState rubble = ModDerivedBlocks.CRACKED_TEMPORAL_STONE_BRICKS.get().defaultBlockState();
        Map<BlockPos, BlockState[]> pal = new LinkedHashMap<>();
        pal.put(wall, new BlockState[]{ThenNow.air(), rubble});
        pal.put(wall.above(), new BlockState[]{ThenNow.air(), rubble});
        pal.put(gap, new BlockState[]{ModDerivedBlocks.POLISHED_TEMPORAL_STONE.get().defaultBlockState(), ThenNow.air()});
        BlockPos socket = c.below();
        BoundingBox box = BoundingBox.fromCorners(c.below(), c.east(5).above());
        return new Box(c, socket, wall, gap, box, pal);
    }

    /** Places the socket and registers the volume (Then). Returns null or why it failed. */
    static String install(ServerLevel level, Box b) {
        level.setBlock(b.socket, ModDungeon.HOUR_SOCKET.get().defaultBlockState(), 3);
        return ThenNow.get(level).register(level, VOL, b.box, b.socket, b.palette);
    }

    static void uninstall(ServerLevel level, Box b, BlockState floorUnderC, BlockState gapState) {
        ThenNow.get(level).remove(VOL);
        level.setBlock(b.socket, floorUnderC, 3);
        level.setBlock(b.wall, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(b.wall.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(b.gap, gapState, 3);
    }

    static boolean isThen(ServerLevel level, Box b) {
        return level.getBlockState(b.wall).isAir() && level.getBlockState(b.wall.above()).isAir() && level.getBlockState(b.gap).is(ModDerivedBlocks.POLISHED_TEMPORAL_STONE.get());
    }
    static boolean isNow(ServerLevel level, Box b) {
        var r = ModDerivedBlocks.CRACKED_TEMPORAL_STONE_BRICKS.get();
        return level.getBlockState(b.wall).is(r) && level.getBlockState(b.wall.above()).is(r) && level.getBlockState(b.gap).isAir();
    }

    static void settle(ServerLevel level) { ThenNow.Volume v = ThenNow.get(level).volume(VOL); if (v != null) v.lastFlip = -100000; }

    // ------------------------------------------------------------------ tests
    private static void flip(ServerLevel ow) {
        DungeonTests.Scan s = DungeonWardTests.dungeon(ow, "then_now_flip"); if (s == null) return;
        ServerLevel level = DungeonWardTests.echo(ow);
        Box b = build(level, s);
        if (b == null) { result("then_now_flip", false, "no corridor"); return; }
        BlockState floor = level.getBlockState(b.socket), gapWas = level.getBlockState(b.gap);
        String why = install(level, b);
        if (why != null) { result("then_now_flip", false, "register refused: " + why); uninstall(level, b, floor, gapWas); return; }
        boolean then0 = isThen(level, b);
        String f1 = HourSocketBlock.shift(level, b.socket, null);
        boolean now1 = isNow(level, b) && ThenNow.get(level).volume(VOL).hour == ThenNow.Hour.NOW;
        String cooldown = HourSocketBlock.shift(level, b.socket, null);   // straight away: refused, nothing changes
        boolean stillNow = isNow(level, b);
        settle(level);
        String f2 = HourSocketBlock.shift(level, b.socket, null);
        boolean then2 = isThen(level, b) && ThenNow.get(level).volume(VOL).hour == ThenNow.Hour.THEN;
        // cells outside the palette never change
        boolean cUntouched = level.getBlockState(b.c).isAir() && level.getBlockState(b.c.east()).isAir();
        uninstall(level, b, floor, gapWas);
        result("then_now_flip", then0 && f1 == null && now1 && cooldown != null && stillNow && f2 == null && then2 && cUntouched,
            "then at start=" + then0 + " flip->now=" + now1 + " (" + f1 + ") cooldown refused=" + (cooldown != null) + " still now=" + stillNow
                + " flip->then restored=" + then2 + " (" + f2 + ") other cells untouched=" + cUntouched);
    }

    private static void protectedCells(ServerLevel ow) {
        DungeonTests.Scan s = DungeonWardTests.dungeon(ow, "then_now_protected"); if (s == null) return;
        ServerLevel level = DungeonWardTests.echo(ow);
        Box b = build(level, s);
        if (b == null) { result("then_now_protected", false, "no corridor"); return; }
        BlockState floor = level.getBlockState(b.socket), gapWas = level.getBlockState(b.gap);
        ThenNow tn = ThenNow.get(level);
        // 1) a vault already standing in a palette cell: refused
        level.setBlock(b.wall, ModDungeon.TREASURY_VAULT.get().defaultBlockState(), 3);
        String vaultHere = tn.register(level, VOL, b.box, b.socket, b.palette);
        level.setBlock(b.wall, Blocks.AIR.defaultBlockState(), 3);
        // 2) a palette that would put a door or a sealed vault door in: refused
        Map<BlockPos, BlockState[]> doorPal = new LinkedHashMap<>(b.palette);
        doorPal.put(b.wall, new BlockState[]{ThenNow.air(), Blocks.OAK_DOOR.defaultBlockState()});
        String doorIn = tn.register(level, VOL, b.box, b.socket, doorPal);
        Map<BlockPos, BlockState[]> keyPal = new LinkedHashMap<>(b.palette);
        keyPal.put(b.wall, new BlockState[]{ModDungeon.KEY_DOOR.get().defaultBlockState(), ThenNow.air()});
        String keyIn = tn.register(level, VOL, b.box, b.socket, keyPal);
        // 3) a box outside any dungeon: refused
        BlockPos far = DungeonWardTests.control(ow, 30400);
        String outside = ThenNow.get(ow).register(ow, VOL + "_out", BoundingBox.fromCorners(far, far.above(2)), far,
            Map.of(far.above(), new BlockState[]{ThenNow.air(), Blocks.STONE.defaultBlockState()}));
        // 4) a vault that turns up in a cell after registration is never touched by a flip
        String ok = install(level, b);
        level.setBlock(b.wall, ModDungeon.TREASURY_VAULT.get().defaultBlockState(), 3);
        HourSocketBlock.shift(level, b.socket, null);
        boolean vaultKept = level.getBlockState(b.wall).is(ModDungeon.TREASURY_VAULT.get());
        boolean restFlipped = level.getBlockState(b.gap).isAir();
        level.setBlock(b.wall, Blocks.AIR.defaultBlockState(), 3);
        uninstall(level, b, floor, gapWas);
        boolean noneLeaked = tn.volume(VOL) == null && ThenNow.get(ow).volume(VOL + "_out") == null;
        result("then_now_protected", vaultHere != null && doorIn != null && keyIn != null && outside != null && ok == null && vaultKept && restFlipped && noneLeaked,
            "vault in cell refused=" + vaultHere + " | door palette refused=" + doorIn + " | key door palette refused=" + keyIn + " | outside dungeon refused=" + outside
                + " | late vault kept by flip=" + vaultKept + " rest flipped=" + restFlipped);
    }

    private static void ward(ServerLevel ow) {
        DungeonTests.Scan s = DungeonWardTests.dungeon(ow, "then_now_ward"); if (s == null) return;
        ServerLevel level = DungeonWardTests.echo(ow);
        Box b = build(level, s);
        if (b == null) { result("then_now_ward", false, "no corridor"); return; }
        BlockState floor = level.getBlockState(b.socket), gapWas = level.getBlockState(b.gap);
        String why = install(level, b);
        HourSocketBlock.shift(level, b.socket, null);   // Now: wall up, gap open
        FakePlayer fp = DungeonWardTests.survivor(level, "QFTimeTourist");
        fp.moveTo(b.c.getX() + 0.5, b.c.getY(), b.c.getZ() + 0.5);
        // the Now ruin is not a way through: mining the rubble fails and it stays
        boolean mined = fp.gameMode.destroyBlock(b.wall) || !level.getBlockState(b.wall).is(ModDerivedBlocks.CRACKED_TEMPORAL_STONE_BRICKS.get());
        // the Now gap can't be bridged by hand
        fp.moveTo(b.c.east(3).getX() + 0.5, b.c.getY(), b.c.getZ() + 0.5);
        boolean bridged = Round3Tests.placeAsPlayer(fp, b.gap, Items.COBBLESTONE);
        boolean gapStillOpen = level.getBlockState(b.gap).isAir();
        // a creative builder is exempt (as for the rest of the ward)
        FakePlayer cr = DungeonWardTests.survivor(level, "QFTimeBuilder"); cr.setGameMode(GameType.CREATIVE);
        cr.moveTo(fp.getX(), fp.getY(), fp.getZ());
        boolean creativePlaced = Round3Tests.placeAsPlayer(cr, b.gap, Items.COBBLESTONE);
        level.setBlock(b.gap, Blocks.AIR.defaultBlockState(), 3);
        // flipping back still gives the exact Then (the ruin wasn't damaged)
        settle(level);
        HourSocketBlock.shift(level, b.socket, null);
        boolean then = isThen(level, b);
        uninstall(level, b, floor, gapWas);
        result("then_now_ward", why == null && !mined && !bridged && gapStillOpen && creativePlaced && then,
            "register=" + why + " survival mined Now rubble=" + mined + " bridged Now gap=" + bridged + " gap still open=" + gapStillOpen
                + " | creative placed=" + creativePlaced + " | back to Then intact=" + then);
    }

    static boolean use(ServerLevel level, FakePlayer fp, BlockPos at, ItemStack stack) {
        fp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false);
        return level.getBlockState(at).useItemOn(fp.getMainHandItem(), level, fp, InteractionHand.MAIN_HAND, hit).consumesAction();
    }

    private static void socket(ServerLevel ow) {
        DungeonTests.Scan s = DungeonWardTests.dungeon(ow, "then_now_socket"); if (s == null) return;
        ServerLevel level = DungeonWardTests.echo(ow);
        Box b = build(level, s);
        if (b == null) { result("then_now_socket", false, "no corridor"); return; }
        BlockState floor = level.getBlockState(b.socket), gapWas = level.getBlockState(b.gap);
        String why = install(level, b);
        FakePlayer fp = DungeonWardTests.survivor(level, "QFHourKeeper");
        fp.moveTo(b.c.getX() + 0.5, b.c.getY(), b.c.getZ() + 0.5);
        // a stone in hand does nothing
        use(level, fp, b.socket, new ItemStack(Items.STONE));
        boolean stoneNoFlip = isThen(level, b);
        // Temporal Dust: flips and is used up
        ItemStack dust = new ItemStack(ModItems.TEMPORAL_DUST.get(), 3);
        use(level, fp, b.socket, dust);
        boolean dustFlip = isNow(level, b), dustSpent = fp.getMainHandItem().getCount() == 2;
        // during the cooldown the dust is NOT used up
        use(level, fp, b.socket, fp.getMainHandItem());
        boolean cooldownKept = fp.getMainHandItem().getCount() == 2 && isNow(level, b);
        // Time Bottle: flips and is kept
        settle(level);
        use(level, fp, b.socket, new ItemStack(ModItems.TIME_BOTTLE.get()));
        boolean bottleFlip = isThen(level, b), bottleKept = fp.getMainHandItem().is(ModItems.TIME_BOTTLE.get()) && fp.getMainHandItem().getCount() == 1;
        // the socket itself is warded (unbreakable in survival)
        boolean socketBroke = fp.gameMode.destroyBlock(b.socket);
        uninstall(level, b, floor, gapWas);
        result("then_now_socket", why == null && stoneNoFlip && dustFlip && dustSpent && cooldownKept && bottleFlip && bottleKept && !socketBroke,
            "register=" + why + " stone no flip=" + stoneNoFlip + " dust flips=" + dustFlip + " dust spent=" + dustSpent + " cooldown keeps dust=" + cooldownKept
                + " bottle flips=" + bottleFlip + " bottle kept=" + bottleKept + " socket broken=" + socketBroke);
    }

    /** The Then-hour set: 12 blocks, each with an item, a self-drop loot table, a recipe, a blockstate and its textures. */
    private static void blocks(ServerLevel level) {
        var all = com.matt.quantumfalcon.registry.ModFrozenHoursBlocks.ALL;
        var loot = level.getServer().reloadableRegistries();
        java.util.Set<net.minecraft.world.item.Item> crafted = new java.util.HashSet<>();
        for (var h : level.getServer().getRecipeManager().getRecipes()) crafted.add(h.value().getResultItem(level.registryAccess()).getItem());
        java.util.List<String> miss = new java.util.ArrayList<>();
        ClassLoader cl = ThenNowTests.class.getClassLoader();
        for (var b : all) {
            String id = b.getId().getPath();
            var blk = b.get();
            // v2 props are dungeon decor (no recipe); every block, the feather piles included, drops itself
            boolean prop = blk instanceof net.minecraft.world.level.block.FenceBlock || blk.getClass().getEnclosingClass() == com.matt.quantumfalcon.dungeon.EastwatchProps.class;
            boolean feathers = false;
            boolean noLoot = loot.getLootTable(blk.getLootTable()) == net.minecraft.world.level.storage.loot.LootTable.EMPTY;
            if (noLoot != feathers) miss.add((feathers ? "drops:" : "loot:") + id);
            if (blk.asItem() == Items.AIR) miss.add("item:" + id);
            if (!prop && !crafted.contains(blk.asItem())) miss.add("recipe:" + id);
            if (cl.getResource("assets/quantumfalcon/blockstates/" + id + ".json") == null) miss.add("blockstate:" + id);
            if (cl.getResource("assets/quantumfalcon/models/item/" + id + ".json") == null) miss.add("itemmodel:" + id);
        }
        String[] tex = {"then/riftslate_bricks", "then/riftslate_lamp_glow", "then/chiseled_riftslate", "now/eastwatch_timber", "overlays/time_crack_cyan_pulse", "overlays/flip_transition",
            "hour_socket/then/top", "hour_socket/now_charged/front_glow_pulse", "props/perch_rail/then/rail_side", "props/feather_pile/now/plane"};
        for (String t : tex) if (cl.getResource("assets/quantumfalcon/textures/block/frozen_hours/eastwatch/" + t + ".png") == null) miss.add("texture:" + t);
        // the Now hour is the existing riftslate family: not renamed, still registered
        for (String now : new String[]{"riftslate_bricks", "riftslate_neon_trim", "riftslate_rift_lamp", "carved_riftslate_lore_panel"})
            if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", now))) miss.add("now-missing:" + now);
        // house banner patterns (v2 masks) are registered; Elder Line's proposal is not
        var pats = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BANNER_PATTERN);
        for (String h : new String[]{"eastwatch", "compact", "stormcrown", "ashborn", "voidblood", "frostwing", "verdant", "starborn"})
            if (!pats.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", h))) miss.add("banner:" + h);
        if (pats.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "elder_line"))) miss.add("banner:elder_line registered");
        result("frozen_hours_blocks", all.size() == 20 && miss.isEmpty(), "blocks=" + all.size() + " missing=" + miss);
    }

    // ------------------------------------------------------------------ restart pair
    private static void restartA(ServerLevel ow) {
        DungeonTests.Scan s = DungeonWardTests.dungeon(ow, "restart_thennow_a"); if (s == null) return;
        ServerLevel level = DungeonWardTests.echo(ow);
        Box b = build(level, s);
        if (b == null) { result("restart_thennow_a", false, "no corridor"); return; }
        BlockState floor = level.getBlockState(b.socket), gapWas = level.getBlockState(b.gap);
        String why = install(level, b);
        String f = HourSocketBlock.shift(level, b.socket, null);
        boolean now = isNow(level, b);
        try {
            Files.writeString(STATE, String.join(";", String.valueOf(b.c.asLong()), String.valueOf(floor.getBlock().builtInRegistryHolder().key().location()),
                String.valueOf(gapWas.getBlock().builtInRegistryHolder().key().location())));
        } catch (Exception e) { result("restart_thennow_a", false, "state write " + e); return; }
        level.getServer().saveEverything(true, true, true);
        result("restart_thennow_a", why == null && f == null && now, "register=" + why + " flipped to Now=" + now + " (restart, then run restart_thennow_b)");
    }

    private static void restartB(ServerLevel ow) {
        ServerLevel level = DungeonWardTests.echo(ow);
        String[] st;
        try { st = Files.readString(STATE).split(";"); } catch (Exception e) { result("restart_thennow_b", false, "no state from restart_thennow_a: " + e); return; }
        BlockPos c = BlockPos.of(Long.parseLong(st[0]));
        level.getChunkAt(c);
        ThenNow.Volume v = ThenNow.get(level).volume(VOL);
        if (v == null) { result("restart_thennow_b", false, "volume lost over the restart"); return; }
        Box b = new Box(c, c.below(), c.east(2), c.east(4).below(), v.box, v.cells);
        boolean hourNow = v.hour == ThenNow.Hour.NOW, worldNow = isNow(level, b), cells = v.cells.size() == 3;
        settle(level);
        String f = HourSocketBlock.shift(level, b.socket, null);
        boolean then = isThen(level, b);
        var reg = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        uninstall(level, b, reg.get(net.minecraft.resources.ResourceLocation.parse(st[1])).defaultBlockState(), reg.get(net.minecraft.resources.ResourceLocation.parse(st[2])).defaultBlockState());
        try { Files.deleteIfExists(STATE); } catch (Exception ignored) {}
        result("restart_thennow_b", hourNow && worldNow && cells && f == null && then,
            "hour kept=" + hourNow + " world still Now=" + worldNow + " palette cells=" + v.cells.size() + " flip back after restart=" + then);
    }
}
