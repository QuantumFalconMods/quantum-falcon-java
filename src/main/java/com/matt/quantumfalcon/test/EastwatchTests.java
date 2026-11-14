package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.dungeon.*;
import com.matt.quantumfalcon.registry.ModFrozenHoursBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Map;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/**
 * A10 Frozen Hours #1, Eastwatch Aerie: placed for real (structure start + references, so the ward, keys and vault see
 * it), its volumes registered from data/quantumfalcon/frozen_hours/eastwatch_aerie.json and found in their Now hour.
 * Checks the layout kit, the Then-only nest ledges (two sockets, one each side), the three bells and their gate (open in
 * both hours, counted once per bell), and Skywarden Irel opening the treasury.
 */
public final class EastwatchTests {
    private EastwatchTests() {}
    static DungeonTests.Scan placed;
    static BlockPos origin;

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("eastwatch_layout", EastwatchTests::layout);
        tests.put("eastwatch_ledges", EastwatchTests::ledges);
        tests.put("eastwatch_bells", EastwatchTests::bells);
        tests.put("eastwatch_irel_treasury", EastwatchTests::irel);
        tests.put("eastwatch_ward_surface", EastwatchTests::wardSurface);
        tests.put("eastwatch_natural", EastwatchTests::natural);
        tests.put("eastwatch_irel_intro", EastwatchTests::irelIntro);
        tests.put("eastwatch_dust_supply", EastwatchTests::dustSupply);
        tests.put("eastwatch_flip_safety", EastwatchTests::flipSafety);
        tests.put("pr6_offline_ledge", EastwatchTests::offlineLedge);
        tests.put("eastwatch_chasm_crossing", EastwatchTests::chasmCrossing);
    }

    static DungeonTests.Scan site(ServerLevel ow, String test) {
        ServerLevel level = DungeonTests.echo(ow);
        if (placed == null) {
            placed = DungeonTests.place(level, "eastwatch_aerie", new BlockPos(26000, 0, 26000));
            if (placed != null) {
                origin = FrozenHours.origin(placed.start);
                ThenNow.get(level).forgetSite(DungeonSites.key(level, placed.start));
                FrozenHours.initSite(level, placed.start);
            }
        }
        if (placed == null || origin == null) { result(test, false, "eastwatch_aerie could not be placed in the Echo Rift"); return null; }
        return placed;
    }
    static BlockPos at(int x, int y, int z) { return origin.offset(x, y, z); }
    static String site(ServerLevel level) { return DungeonSites.key(level, placed.start); }
    static ThenNow.Volume vol(ServerLevel level, String id) { return ThenNow.get(level).volume(site(level) + "#" + id); }
    static void settle(ServerLevel level, String id) { ThenNow.Volume v = vol(level, id); if (v != null) v.lastFlip = -100000; }

    private static void layout(ServerLevel ow) {
        DungeonTests.Scan s = site(ow, "eastwatch_layout"); if (s == null) return;
        ServerLevel level = DungeonTests.echo(ow);
        int keyDoors = s.of(ModDungeon.KEY_DOOR.get()).size(), vaults = s.of(ModDungeon.TREASURY_VAULT.get()).size(), lecterns = s.of(ModDungeon.LORE_LECTERN.get()).size();
        int checkpoints = s.of(ModDungeon.CHECKPOINT.get()).size(), rifts = s.of(ModDungeon.RETURN_RIFT.get()).size();
        DungeonGuardBlockEntity boss = s.guards.stream().filter(g -> "boss".equals(g.role())).findFirst().orElse(null);
        boolean keyGuard = s.guards.stream().anyMatch(g -> "key".equals(g.role()));
        boolean irel = boss != null && "Skywarden Irel".equals(boss.name());
        int sockets = 0;
        for (BlockPos p : BlockPos.betweenClosed(s.start.getBoundingBox().minX(), s.start.getBoundingBox().minY(), s.start.getBoundingBox().minZ(),
            s.start.getBoundingBox().maxX(), s.start.getBoundingBox().maxY(), s.start.getBoundingBox().maxZ()))
            if (level.getBlockState(p).is(ModDungeon.HOUR_SOCKET.get())) sockets++;
        // the four volumes are registered and start in Now
        int nowVols = 0;
        for (String id : new String[]{"mews", "mural", "ledges", "bells"}) { ThenNow.Volume v = vol(level, id); if (v != null && v.hour == ThenNow.Hour.NOW) nowVols++; }
        // warded: a survival player can't mine the gallery wall, and the gallery is inside the dungeon
        BlockPos wall = at(6, 20, 9);
        boolean inside = DungeonWard.inDungeon(level, at(12, 19, 9));
        FakePlayer fp = DungeonWardTests.survivor(level, "QFEastwatchMiner");
        fp.moveTo(wall.getX() + 1.5, wall.getY(), wall.getZ() + 0.5);
        BlockState keep = level.getBlockState(wall);
        boolean mined = fp.gameMode.destroyBlock(wall);
        if (mined) level.setBlock(wall, keep, 3);
        // the hut is on the surface: open sky above its roof, the ladder runs down to the gallery
        boolean sky = level.canSeeSky(at(15, 37, 9));
        boolean ladder = level.getBlockState(at(15, 24, 9)).is(Blocks.LADDER) && level.getBlockState(at(15, 19, 9)).is(Blocks.LADDER);
        boolean east = level.getBlockState(at(22, 32, 9)).is(Blocks.DARK_OAK_FENCE);
        result("eastwatch_layout", keyDoors == 2 && vaults == 2 && lecterns >= 3 && checkpoints == 1 && rifts == 1 && keyGuard && irel && sockets == 5 && nowVols == 4 && inside && !mined && sky && ladder && east,
            "key door halves=" + keyDoors + " vaults=" + vaults + " lecterns=" + lecterns + " checkpoint=" + checkpoints + " return rift=" + rifts + " keybearer=" + keyGuard
                + " Irel=" + irel + " hour sockets=" + sockets + " volumes in Now=" + nowVols + "/4 | warded: inside=" + inside + " mined=" + mined
                + " | hut sky=" + sky + " ladder=" + ladder + " east perch=" + east);
    }

    static boolean ledgesSolid(ServerLevel level) {
        for (int x : new int[]{29, 33, 36, 39, 41}) if (!level.getBlockState(at(x, 18, 17)).is(ModFrozenHoursBlocks.EASTWATCH_TIMBER_THEN.get())) return false;
        return true;
    }
    static boolean ledgesGone(ServerLevel level) {
        for (int x : new int[]{29, 33, 36, 39, 41}) if (!level.getBlockState(at(x, 18, 17)).isAir()) return false;
        return true;
    }

    private static void ledges(ServerLevel ow) {
        DungeonTests.Scan s = site(ow, "eastwatch_ledges"); if (s == null) return;
        ServerLevel level = DungeonTests.echo(ow);
        boolean nowGone = ledgesGone(level);
        settle(level, "ledges");
        String a = HourSocketBlock.shift(level, at(27, 19, 21), null);       // west socket (the volume's own)
        boolean thenSolid = ledgesSolid(level);
        BlockState sock = level.getBlockState(at(27, 19, 21));
        boolean socketShows = sock.getValue(HourSocketBlock.HOUR) == HourSocketBlock.HourShown.THEN && sock.getValue(HourSocketBlock.CHARGED);
        settle(level, "ledges");
        String b = HourSocketBlock.shift(level, at(43, 19, 21), null);       // far-side socket, by reach
        boolean backNow = ledgesGone(level);
        // the drop: the cache ledge and chest sit under the ledges. no ladder runs from the far platform down to it
        // (that made the drop optional); the way out is down into the pit and up the pit ladder on the west side
        boolean cache = level.getBlockState(at(36, 12, 17)).is(Blocks.CHEST) && level.getBlockState(at(37, 11, 17)).isCollisionShapeFullBlock(level, at(37, 11, 17));
        boolean noFarLadder = true;
        for (int y = 12; y <= 18; y++) for (int x = 40; x <= 42; x++) for (int z = 13; z <= 19; z++) if (level.getBlockState(at(x, y, z)).is(Blocks.LADDER)) noFarLadder = false;
        boolean ladder = noFarLadder && level.getBlockState(at(28, 12, 14)).is(Blocks.LADDER) && level.getBlockState(at(28, 7, 14)).is(Blocks.LADDER);
        // Now gap can't be bridged by hand (the ward owns both hours)
        FakePlayer fp = DungeonWardTests.survivor(level, "QFLedgeBridger");
        fp.moveTo(at(27, 19, 17).getX() + 0.5, at(27, 19, 17).getY(), at(27, 19, 17).getZ() + 0.5);
        boolean bridged = Round3Tests.placeAsPlayer(fp, at(29, 18, 17), Items.COBBLESTONE);
        if (bridged) level.setBlock(at(29, 18, 17), Blocks.AIR.defaultBlockState(), 3);
        BlockState far = level.getBlockState(at(43, 19, 21));
        boolean farShows = far.getValue(HourSocketBlock.HOUR) == HourSocketBlock.HourShown.NOW;
        result("eastwatch_ledges", socketShows && farShows && nowGone && a == null && thenSolid && b == null && backNow && cache && ladder && !bridged,
            "Now: ledges gone=" + nowGone + " | west socket -> Then ledges=" + thenSolid + " (" + a + ") | far socket -> Now=" + backNow + " (" + b + ")"
                + " | socket shows Then+charged=" + socketShows + " far socket shows Now=" + farShows + " | cache under ledges=" + cache + " no ladder from the far platform=" + noFarLadder + " pit ladder=" + ladder + " | bridged Now gap=" + bridged);
    }

    private static void bells(ServerLevel ow) {
        DungeonTests.Scan s = site(ow, "eastwatch_bells"); if (s == null) return;
        ServerLevel level = DungeonTests.echo(ow);
        ThenNow tn = ThenNow.get(level);
        tn.bells.remove(site(level)); tn.gatesOpen.remove(site(level));
        for (int x = 36; x <= 38; x++) for (int y = 19; y <= 21; y++) level.setBlock(at(x, y, 30), Blocks.IRON_BARS.defaultBlockState(), 3);
        BlockPos[] bells = {at(33, 23, 26), at(37, 23, 26), at(41, 23, 26)};
        boolean nowNoBells = true;
        for (BlockPos b : bells) nowNoBells &= level.getBlockState(b).is(Blocks.CHAIN);
        settle(level, "bells");
        HourSocketBlock.shift(level, at(31, 19, 23), null);
        boolean thenBells = true;
        for (BlockPos b : bells) thenBells &= level.getBlockState(b).is(Blocks.BELL);
        int r1 = FrozenHours.ring(level, bells[0], null), r1again = FrozenHours.ring(level, bells[0], null), r2 = FrozenHours.ring(level, bells[1], null);
        boolean shutAfterTwo = level.getBlockState(at(37, 20, 30)).is(Blocks.IRON_BARS);
        int r3 = FrozenHours.ring(level, bells[2], null);
        boolean open = level.getBlockState(at(37, 20, 30)).isAir() && level.getBlockState(at(36, 19, 30)).isAir() && level.getBlockState(at(38, 21, 30)).isAir();
        int notBell = FrozenHours.ring(level, at(35, 20, 26), null);
        settle(level, "bells");
        HourSocketBlock.shift(level, at(31, 19, 23), null);     // back to Now: the bells go, the gate stays open
        boolean nowOpen = level.getBlockState(at(37, 20, 30)).isAir() && level.getBlockState(bells[1]).is(Blocks.CHAIN);
        boolean saved = tn.gatesOpen.contains(site(level)) && tn.bells.get(site(level)).size() == 3;
        result("eastwatch_bells", nowNoBells && thenBells && r1 == 1 && r1again == 1 && r2 == 2 && shutAfterTwo && r3 == 3 && open && notBell == -1 && nowOpen && saved,
            "Now has no bells=" + nowNoBells + " Then bells=" + thenBells + " rung " + r1 + "," + r1again + "(same bell)," + r2 + " gate shut after two=" + shutAfterTwo
                + " third=" + r3 + " gate open=" + open + " not-a-bell=" + notBell + " | back in Now gate still open=" + nowOpen + " saved=" + saved);
    }

    private static void irel(ServerLevel ow) {
        DungeonTests.Scan s = site(ow, "eastwatch_irel_treasury"); if (s == null) return;
        ServerLevel level = DungeonTests.echo(ow);
        DungeonSites.get(level).forget(site(level));
        java.util.List<BlockPos> vaults = s.of(ModDungeon.TREASURY_VAULT.get());
        for (BlockPos v : vaults) {
            level.setBlock(v, level.getBlockState(v).setValue(TreasuryVaultBlock.OPEN, false), 3);
            if (level.getBlockEntity(v) instanceof TreasuryVaultBlockEntity tv) tv.resetForTest();
        }
        BlockPos vault = at(4, 19, 33), eggVault = at(4, 19, 36);
        // run past Irel to the East Egg before she falls: it is sealed, nothing is taken
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(eggVault).inflate(3)).forEach(Entity::discard);   // leftovers of earlier runs
        FakePlayer thief = DungeonWardTests.survivor(level, "QFEggThief");
        thief.moveTo(eggVault.getX() + 1.5, eggVault.getY(), eggVault.getZ() + 0.5);
        level.getBlockState(eggVault).useWithoutItem(level, thief, new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(eggVault), net.minecraft.core.Direction.EAST, eggVault, false));
        String eggState = level.getBlockState(eggVault).toString();
        int eggItems = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(eggVault).inflate(2)).size();
        boolean eggSealed = level.getBlockState(eggVault).is(ModDungeon.TREASURY_VAULT.get()) && !level.getBlockState(eggVault).getValue(TreasuryVaultBlock.OPEN) && eggItems == 0;
        boolean eggMined = thief.gameMode.destroyBlock(eggVault);
        DungeonGuardBlockEntity boss = s.guards.stream().filter(g -> "boss".equals(g.role())).findFirst().orElse(null);
        if (boss == null || level.getBlockEntity(boss.getBlockPos()) != boss) { result("eastwatch_irel_treasury", false, "Irel's guard already spent or missing"); return; }
        Entity irel = boss.wake(level);
        String name = irel == null || irel.getCustomName() == null ? "none" : irel.getCustomName().getString();
        boolean tagged = irel != null && irel.getTags().contains(DungeonGuardBlockEntity.TAG_BOSS)
            && irel instanceof com.matt.quantumfalcon.entity.boss.SkywardenIrelEntity;   // her own entity, not a placeholder
        DungeonTests.killTagged(level, irel);
        later(3, () -> {
            boolean opened = level.getBlockState(vault).getValue(TreasuryVaultBlock.OPEN);
            boolean eggOpen = level.getBlockState(eggVault).is(ModDungeon.TREASURY_VAULT.get()) && level.getBlockState(eggVault).getValue(TreasuryVaultBlock.OPEN);
            level.getBlockState(eggVault).useWithoutItem(level, thief, new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(eggVault), net.minecraft.core.Direction.EAST, eggVault, false));
            boolean gotEgg = !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(eggVault).inflate(2),
                ie -> ie.getItem().is(com.matt.quantumfalcon.registry.ModItems.FALCON_EGG.get())).isEmpty();
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(eggVault).inflate(3)).forEach(Entity::discard);
            result("eastwatch_irel_treasury", "Skywarden Irel".equals(name) && tagged && opened && vaults.size() == 2 && eggSealed && !eggMined && eggOpen && gotEgg,
                "boss named=" + name + " tagged=" + tagged + " treasury opened on her death=" + opened + " | vaults=" + vaults.size()
                    + " East Egg before Irel: sealed=" + eggSealed + " (" + eggState + ", items " + eggItems + ") mined=" + eggMined + " | after: open=" + eggOpen + " egg taken=" + gotEgg);
        });
    }

    /**
     * a lantern and torch set on a Then ledge drop off when the ledge goes (no floating stepping stones); a flip is
     * refused while someone else stands on a ledge it would take away; a ledge returning around a mob lifts it out;
     * an arrow and a redstone pulse ring a nest-bell like a right-click does.
     */
    /** a player who logged out on a Then ledge keeps it: the flip that would remove it waits until they log back in. */
    private static void offlineLedge(ServerLevel ow) {
        DungeonTests.Scan s = site(ow, "pr6_offline_ledge"); if (s == null) return;
        ServerLevel level = DungeonTests.echo(ow);
        BlockPos sock = at(27, 19, 21);
        if (!ledgesSolid(level)) { settle(level, "ledges"); HourSocketBlock.shift(level, sock, null); }
        FakePlayer flipper = DungeonWardTests.survivor(level, "QFFlipper2"), away = DungeonWardTests.survivor(level, "QFLoggedOut");
        flipper.moveTo(at(27, 19, 20).getX() + 0.5, at(27, 19, 20).getY(), at(27, 19, 20).getZ() + 0.5);
        away.moveTo(at(36, 19, 17).getX() + 0.5, at(36, 19, 17).getY(), at(36, 19, 17).getZ() + 0.5);
        boolean parked = ThenNow.park(away);   // what the logout event does; the player is in no player list now
        settle(level, "ledges");
        String refused = HourSocketBlock.shift(level, sock, flipper);
        boolean stillThen = ledgesSolid(level);
        ThenNow.unpark(away);                  // logs back in
        settle(level, "ledges");
        String allowed = HourSocketBlock.shift(level, sock, flipper);
        boolean gone = ledgesGone(level);
        boolean saved = ThenNow.get(level).save(new net.minecraft.nbt.CompoundTag(), level.registryAccess()).contains("Parked");
        result("pr6_offline_ledge", parked && refused != null && stillThen && allowed == null && gone && saved,
            "parked on logout=" + parked + " | flip while away refused=" + (refused != null) + " (" + refused + ") ledges kept=" + stillThen
                + " | after login flip allowed=" + (allowed == null) + " ledges gone=" + gone + " | parked list saved=" + saved);
    }

    private static void flipSafety(ServerLevel ow) {
        DungeonTests.Scan s = site(ow, "eastwatch_flip_safety"); if (s == null) return;
        ServerLevel level = DungeonTests.echo(ow);
        // must not knock off anything authored: flip every volume both ways and count drops
        int authoredPopped = 0;
        for (String vid : new String[]{"mews", "mural", "bells", "ledges"}) for (int k = 0; k < 2; k++) {
            ThenNow.Volume vv = vol(level, vid);
            if (vv == null) continue;
            settle(level, vid); ThenNow.get(level).flip(level, vv); authoredPopped += ThenNow.lastPopped;
        }
        final int authoredPop = authoredPopped;
        BlockPos sock = at(27, 19, 21);
        if (!ledgesSolid(level)) { settle(level, "ledges"); HourSocketBlock.shift(level, sock, null); }
        var box = new net.minecraft.world.phys.AABB(at(28, 10, 13)).expandTowards(16, 14, 9);
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).forEach(Entity::discard);
        level.setBlock(at(33, 19, 18), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(at(32, 19, 18), Blocks.TORCH.defaultBlockState(), 3);
        FakePlayer flipper = DungeonWardTests.survivor(level, "QFFlipper"), crosser = DungeonWardTests.survivor(level, "QFCrosser");
        flipper.moveTo(at(27, 19, 20).getX() + 0.5, at(27, 19, 20).getY(), at(27, 19, 20).getZ() + 0.5);
        crosser.moveTo(at(36, 19, 17).getX() + 0.5, at(36, 19, 17).getY(), at(36, 19, 17).getZ() + 0.5);
        ThenNow.TEST_PLAYERS.add(crosser);
        settle(level, "ledges");
        String refused = HourSocketBlock.shift(level, sock, flipper);
        boolean stillThen = ledgesSolid(level);
        crosser.moveTo(at(27, 19, 17).getX() + 0.5, at(27, 19, 17).getY(), at(27, 19, 17).getZ() + 0.5);   // back on the west lip
        settle(level, "ledges");
        String allowed = HourSocketBlock.shift(level, sock, flipper);
        boolean now = ledgesGone(level);
        int popped = ThenNow.lastPopped;
        boolean lanternGone = level.getBlockState(at(33, 19, 18)).isAir() && level.getBlockState(at(32, 19, 18)).isAir();
        boolean lanternDropped = !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box, ie -> ie.getItem().is(Items.LANTERN)).isEmpty();
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).forEach(Entity::discard);
        // a mob hanging in the gap where a ledge returns
        var z = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        z.moveTo(at(36, 18, 17).getX() + 0.5, at(36, 18, 17).getY(), at(36, 18, 17).getZ() + 0.5);
        z.setNoAi(true); z.setNoGravity(true); level.addFreshEntity(z);
        settle(level, "ledges");
        HourSocketBlock.shift(level, sock, flipper);
        boolean lifted = z.getY() >= at(36, 19, 17).getY() && level.getBlockState(z.blockPosition()).getCollisionShape(level, z.blockPosition()).isEmpty();
        int pushed = ThenNow.lastPushed;
        z.discard(); ThenNow.TEST_PLAYERS.clear();
        settle(level, "ledges"); HourSocketBlock.shift(level, sock, null);   // leave it in Now
        // bells, Then hour
        ThenNow tn = ThenNow.get(level);
        tn.bells.remove(site(level)); tn.gatesOpen.remove(site(level));
        if (!level.getBlockState(at(33, 23, 26)).is(Blocks.BELL)) { settle(level, "bells"); HourSocketBlock.shift(level, at(31, 19, 23), null); }
        BlockPos b0 = at(33, 23, 26), b1 = at(37, 23, 26);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW, level);
        arrow.setPos(b0.getX() + 0.5, b0.getY() - 2.5, b0.getZ() + 0.5);
        arrow.shoot(0, 1, 0, 1.6f, 0f);
        level.addFreshEntity(arrow);
        BlockPos rs = null;
        for (var d : new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.DOWN})
            if (level.getBlockState(b1.relative(d)).isAir()) { rs = b1.relative(d); break; }
        if (rs != null) level.setBlock(rs, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        final BlockPos rsF = rs;
        later(20, () -> {
            java.util.Set<Long> rung = tn.bells.getOrDefault(site(level), java.util.Set.of());
            boolean byArrow = rung.contains(b0.asLong()), byRedstone = rung.contains(b1.asLong());
            if (rsF != null) level.setBlock(rsF, Blocks.AIR.defaultBlockState(), 3);
            arrow.discard();
            tn.bells.remove(site(level));
            settle(level, "bells"); HourSocketBlock.shift(level, at(31, 19, 23), null);
            boolean ok = authoredPop == 0 && refused != null && stillThen && allowed == null && now && lanternGone && lanternDropped && popped >= 2 && lifted && pushed >= 1 && byArrow && byRedstone;
            result("eastwatch_flip_safety", ok, "authored blocks knocked off by 8 flips=" + authoredPop + " | co-op: flip with someone on a ledge refused=" + (refused != null) + " (" + refused + ") ledges kept=" + stillThen
                + " | after they step off: flipped=" + (allowed == null) + " Now=" + now + " | lantern+torch fell=" + lanternGone + " lantern dropped=" + lanternDropped + " popped=" + popped
                + " | mob in the gap lifted onto the returning ledge=" + lifted + " pushed=" + pushed + " | bell rung by arrow=" + byArrow + " by redstone=" + byRedstone);
        });
    }

    /**
     * Crossing the Now chasm without the hour (review "untested"): elytra are grounded, riptide has no water or rain to
     * work with, a player's wind charge doesn't launch them, a real ender pearl doesn't carry them over (with a control
     * outside), and hoppers / rails / hopper minecarts can't reach the loot.
     */
    private static void chasmCrossing(ServerLevel ow) {
        DungeonTests.Scan s = site(ow, "eastwatch_chasm_crossing"); if (s == null) return;
        ServerLevel level = DungeonTests.echo(ow);
        if (ledgesSolid(level)) { settle(level, "ledges"); HourSocketBlock.shift(level, at(27, 19, 21), null); }
        BlockPos lip = at(27, 19, 17);
        FakePlayer fp = DungeonWardTests.survivor(level, "QFChasmRunner");
        fp.moveTo(lip.getX() + 0.5, lip.getY(), lip.getZ() + 0.5);
        // elytra
        fp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(Items.ELYTRA));
        fp.startFallFlying();
        boolean wasGliding = fp.isFallFlying();
        boolean grounded = DungeonWard.groundGlider(fp) && !fp.isFallFlying();
        fp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
        // riptide: no water anywhere in the flight-run, rain can't reach the chasm, and a riptide trident does nothing
        int water = 0; boolean roofed = true;
        String waterAt = "";
        for (BlockPos p : BlockPos.betweenClosed(at(27, 7, 13), at(43, 25, 21)))
            if (!level.getFluidState(p).isEmpty() && level.getBlockState(p).getCollisionShape(level, p).isEmpty() && DungeonWard.inDungeon(level, p)) { water++; if (waterAt.isEmpty()) waterAt = p.subtract(origin).toShortString(); }
        for (int x = 28; x <= 41; x++) for (int z = 14; z <= 20; z++)
            if (level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, at(x, 0, z).getX(), at(x, 0, z).getZ()) <= at(0, 19, 0).getY()) roofed = false;
        // any fluid anywhere in the warded site (waterlogged blocks included)
        StringBuilder siteWater = new StringBuilder(); int siteWaterN = 0;
        for (var piece : s.start.getPieces()) { var bb = piece.getBoundingBox();
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ()))
                if (!level.getFluidState(p).isEmpty() && DungeonWard.inDungeon(level, p)) { siteWaterN++; if (siteWater.length() < 200) siteWater.append(p.subtract(origin).toShortString()).append(' ').append(level.getBlockState(p).getBlock().getName().getString()).append("; "); } }
        final String fSiteWater = siteWaterN + (siteWaterN > 0 ? " [" + siteWater + "]" : "");
        var trident = new net.minecraft.world.item.ItemStack(Items.TRIDENT);
        var ench = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.RIPTIDE);
        trident.enchant(ench, 3);
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, trident);
        fp.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        trident.releaseUsing(level, fp, trident.getUseDuration(fp) - 20);
        boolean riptideDead = fp.getDeltaMovement().lengthSqr() < 1e-6 && !fp.isAutoSpinAttack();
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        // wind charge: the player's own gust at the lip
        var wc = new net.minecraft.world.entity.projectile.windcharge.WindCharge(net.minecraft.world.entity.EntityType.WIND_CHARGE, level);
        wc.setPos(lip.getX() + 0.5, lip.getY(), lip.getZ() + 0.5); wc.setOwner(fp);
        boolean windShield = DungeonWard.windShielded(level, fp, wc);
        BlockPos o = DungeonWardTests.control(ow, 30440);
        FakePlayer outside = DungeonWardTests.survivor(ow, "QFOutsideWind");
        outside.moveTo(o.getX() + 0.5, o.getY(), o.getZ() + 0.5);
        var wc2 = new net.minecraft.world.entity.projectile.windcharge.WindCharge(net.minecraft.world.entity.EntityType.WIND_CHARGE, ow);
        wc2.setPos(o.getX() + 0.5, o.getY(), o.getZ() + 0.5); wc2.setOwner(outside);
        boolean windOutside = !DungeonWard.windShielded(ow, outside, wc2);
        // hoppers, rails, hopper minecart
        boolean hopper = Round3Tests.placeAsPlayer(fp, at(27, 19, 15), Items.HOPPER), rail = Round3Tests.placeAsPlayer(fp, at(27, 19, 19), Items.RAIL);
        int exposed = 0, containers = 0;
        for (var piece : s.start.getPieces()) {
            var bb = piece.getBoundingBox();
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ()))
                if (level.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity) {
                    containers++;
                    if (!DungeonWard.inDungeon(level, p.below())) exposed++;   // a hopper could sit under it outside the ward
                }
        }
        BlockPos chest = at(2, 19, 9);
        boolean chestReset = false;
        if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity ch) {
            ch.clearContent();
            ch.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "chests/frozen_hours/eastwatch_room")), 1L);
            chestReset = true;
        }
        var cart = new net.minecraft.world.entity.vehicle.MinecartHopper(net.minecraft.world.entity.EntityType.HOPPER_MINECART, level);
        cart.setPos(chest.getX() + 1.5, chest.getY(), chest.getZ() + 0.5);
        level.addFreshEntity(cart);
        // a real pearl over the chasm, and a control throw outside
        var pearl = new net.minecraft.world.entity.projectile.ThrownEnderpearl(level, fp);
        pearl.setPos(lip.getX() + 0.5, lip.getY() + 1.5, lip.getZ() + 0.5);
        pearl.shoot(1, 0.15, 0, 1.5f, 0f);
        level.addFreshEntity(pearl);
        var pearl2 = new net.minecraft.world.entity.projectile.ThrownEnderpearl(ow, outside);
        pearl2.setPos(o.getX() + 0.5, o.getY() + 1.5, o.getZ() + 0.5);
        pearl2.shoot(1, 0.1, 0, 1.2f, 0f);
        ow.addFreshEntity(pearl2);
        WorldDropsTests.force(level, lip, true); WorldDropsTests.force(ow, o, true);
        final net.minecraft.world.phys.Vec3 startPos = fp.position(), outStart = outside.position();
        final boolean fWasGliding = wasGliding, fGrounded = grounded, fRiptide = riptideDead, fWind = windShield, fWindOut = windOutside, fHopper = hopper, fRail = rail, fReset = chestReset;
        final String fWaterAt = waterAt;
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(chest).inflate(4)).forEach(Entity::discard);
        final int fWater = water, fExposed = exposed, fContainers = containers; final boolean fRoofed = roofed;
        later(60, () -> {
            boolean pearlHeld = fp.position().distanceToSqr(startPos) < 1;
            boolean controlMoved = outside.position().distanceToSqr(outStart) > 4;
            boolean cartTook = level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity ch && ch.getLootTable() == null;
            int cartItems = 0; StringBuilder cartWhat = new StringBuilder();
            for (int i = 0; i < cart.getContainerSize(); i++) if (!cart.getItem(i).isEmpty()) { cartItems += cart.getItem(i).getCount(); cartWhat.append(cart.getItem(i)).append(' '); }
            String chestWas = level.getBlockState(chest).toString() + " cart at " + cart.blockPosition().subtract(origin).toShortString();
            cart.discard(); pearl.discard(); pearl2.discard();
            WorldDropsTests.force(ow, o, false);   // keep the shared Eastwatch site forced (later tests need it ticking)
            boolean ok = fWasGliding && fGrounded && fWater == 0 && fRoofed && fRiptide && fWind && fWindOut && !fHopper && !fRail && fExposed == 0 && fReset && !cartTook && cartItems == 0 && pearlHeld;
            result("eastwatch_chasm_crossing", ok, "elytra: gliding=" + fWasGliding + " grounded by the ward=" + fGrounded
                + " | riptide: open water in the flight-run=" + fWater + (fWater > 0 ? " first at " + fWaterAt : "") + " site fluids=" + fSiteWater + " chasm roofed from rain=" + fRoofed + " trident did nothing=" + fRiptide
                + " | own wind charge shielded=" + fWind + " (outside not=" + fWindOut + ")"
                + " | real pearl over the chasm: player stayed=" + pearlHeld + " (control outside teleported=" + controlMoved + (controlMoved ? "" : ": a FakePlayer isn't pearl-teleported even outside, so this half is inconclusive; the ward's pearl event is covered by a7_pearl_chorus") + ")"
                + " | hopper placed=" + fHopper + " rail placed=" + fRail + " containers=" + fContainers + " with an unwarded cell below=" + fExposed
                + " | hopper minecart beside the room chest: unpacked it=" + cartTook + " items=" + cartItems + (cartItems > 0 ? " (" + cartWhat.toString().trim() + "; " + chestWas + ")" : ""));
        });
    }

    /** the one chest before the chasm always gives the 2 Temporal Dust a clear needs (ledges, then bells). */
    private static void dustSupply(ServerLevel level) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "chests/frozen_hours/eastwatch_room"));
        var table = level.getServer().reloadableRegistries().getLootTable(key);
        int min = Integer.MAX_VALUE, under = 0; long sum = 0; int n = 2000;
        for (int i = 0; i < n; i++) {
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            int dust = 0;
            for (var st : table.getRandomItems(params)) if (st.is(com.matt.quantumfalcon.registry.ModItems.TEMPORAL_DUST.get())) dust += st.getCount();
            min = Math.min(min, dust); sum += dust; if (dust < 2) under++;
        }
        result("eastwatch_dust_supply", min >= 2, n + " rolls of eastwatch_room: min dust=" + min + " mean=" + String.format("%.2f", sum / (double) n) + " runs under 2=" + under);
    }

    /** The ward covers the rooms, shafts and hut only: a tree on the ground beside the hut can be felled, the hut wall can't. */
    private static void wardSurface(ServerLevel ow) {
        DungeonTests.Scan s = site(ow, "eastwatch_ward_surface"); if (s == null) return;
        ServerLevel level = DungeonTests.echo(ow);
        BlockPos log = at(3, 33, 30), hutWall = at(11, 33, 7), deep = at(20, 21, 26);
        level.setBlock(log, Blocks.OAK_LOG.defaultBlockState(), 3);
        boolean outside = !DungeonWard.inDungeon(level, log), hutIn = DungeonWard.inDungeon(level, hutWall), deepIn = DungeonWard.inDungeon(level, deep);
        FakePlayer fp = DungeonWardTests.survivor(level, "QFEastwatchWoodcutter");
        fp.moveTo(log.getX() + 1.5, log.getY(), log.getZ() + 0.5);
        boolean felled = fp.gameMode.destroyBlock(log);
        fp.moveTo(hutWall.getX() - 1.5, hutWall.getY(), hutWall.getZ() + 0.5);
        BlockState keep = level.getBlockState(hutWall);
        boolean hutMined = fp.gameMode.destroyBlock(hutWall);
        if (hutMined) level.setBlock(hutWall, keep, 3);
        // the boxes the client is sent match the server ward (no ghost cracks on the dome, none refused on the ground)
        var boxes = WardSync.boxesNear(level, deep, WardSync.RADIUS_CHUNKS);
        java.util.List<Integer> flat = new java.util.ArrayList<>();
        for (var b : boxes) java.util.Collections.addAll(flat, b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ());
        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        WardSync.Payload.CODEC.encode(buf, new WardSync.Payload(level.dimension().location().toString(), flat));
        WardSync.Payload back = WardSync.Payload.CODEC.decode(buf);
        WardSync.Client.set(back.dim(), back.boxes());
        String dim = level.dimension().location().toString();
        boolean clientMatches = WardSync.Client.inside(dim, deep) && WardSync.Client.inside(dim, hutWall) && !WardSync.Client.inside(dim, log)
            && !WardSync.Client.inside("minecraft:overworld", deep);
        WardSync.Client.set("", java.util.List.of());
        result("eastwatch_ward_surface", outside && felled && hutIn && !hutMined && deepIn && clientMatches,
            "ground beside the hut outside the ward=" + outside + " tree felled=" + felled + " | hut warded=" + hutIn + " hut wall mined=" + hutMined + " | dome warded=" + deepIn
                + " | client ward boxes=" + boxes.size() + " agree with the server=" + clientMatches);
    }

    /**
     * Natural overworld generation (run on a fresh normal world): /locate's search finds an Eastwatch, its hut sits on the
     * terrain with sky over it, the shaft reaches the gallery, the site starts in Now, the gallery is warded and the open
     * ground beside it is not (a survival player can dig it).
     */
    private static void natural(ServerLevel ow) {
        var reg = ow.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var holder = reg.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "eastwatch_aerie"))).orElse(null);
        if (holder == null) { result("eastwatch_natural", false, "structure not registered"); return; }
        BlockPos spawn = ow.getSharedSpawnPos();
        var found = ow.getChunkSource().getGenerator().findNearestMapStructure(ow, net.minecraft.core.HolderSet.direct(holder), spawn, 100, false);
        if (found == null) { result("eastwatch_natural", false, "no eastwatch_aerie within 100 chunks of spawn " + spawn.toShortString()); return; }
        BlockPos p = found.getFirst();
        var start = ow.getChunk(p.getX() >> 4, p.getZ() >> 4).getStartForStructure(holder.value());
        if (start == null || !start.isValid()) { result("eastwatch_natural", false, "located " + p.toShortString() + " but no valid start"); return; }
        var bb = start.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) ow.getChunk(cx, cz);
        BlockPos o = FrozenHours.origin(start);
        String biome = ow.getBiome(o.offset(15, 31, 9)).unwrapKey().map(k -> k.location().toString()).orElse("?");
        boolean floor = ow.getBlockState(o.offset(12, 31, 7)).is(ModFrozenHoursBlocks.EASTWATCH_TIMBER.get());
        boolean sky = ow.canSeeSky(o.offset(15, 37, 9));
        boolean ladder = ow.getBlockState(o.offset(15, 24, 9)).is(Blocks.LADDER) && ow.getBlockState(o.offset(15, 30, 9)).is(Blocks.LADDER);
        // the terrain beside the hut is near the hut floor (it was placed on the surface, not buried or floating)
        int hutY = o.getY() + 31, worst = 0;
        int[][] around = {{5, 9}, {25, 9}, {15, 0}, {15, 18}};
        for (int[] a : around) worst = Math.max(worst, Math.abs(ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, o.getX() + a[0], o.getZ() + a[1]) - 1 - hutY));
        String site = DungeonSites.key(ow, start);
        FrozenHours.initSite(ow, start);
        int nowVols = 0;
        for (String id : new String[]{"mews", "mural", "ledges", "bells"}) { ThenNow.Volume v = ThenNow.get(ow).volume(site + "#" + id); if (v != null && v.hour == ThenNow.Hour.NOW) nowVols++; }
        boolean wallWarded = DungeonWard.inDungeon(ow, o.offset(6, 20, 9));
        // open ground: the top block at template (40, 2), well away from the hut and both light-wells
        int gx = o.getX() + 40, gz = o.getZ() + 2;
        BlockPos ground = new BlockPos(gx, ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz) - 1, gz);
        BlockState was = ow.getBlockState(ground);
        boolean groundFree = !DungeonWard.inDungeon(ow, ground) && bb.isInside(ground);
        FakePlayer fp = DungeonWardTests.survivor(ow, "QFEastwatchDigger");
        fp.moveTo(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 1.5);
        boolean dug = was.isAir() || fp.gameMode.destroyBlock(ground);
        if (!was.isAir()) ow.setBlock(ground, was, 3);
        boolean wellBars = ow.getBlockState(o.offset(34, 30, 17)).is(Blocks.IRON_BARS);
        result("eastwatch_natural", floor && sky && ladder && worst <= 14 && nowVols == 4 && wallWarded && groundFree && dug && wellBars,
            "found at " + o.toShortString() + " (" + biome + ", " + (int) Math.sqrt(spawn.distSqr(o)) + " blocks from spawn) | hut floor=" + floor + " sky=" + sky
                + " shaft ladder=" + ladder + " terrain vs hut floor max dy=" + worst + " | volumes in Now=" + nowVols + "/4 | gallery wall warded=" + wallWarded
                + " | ground " + ground.toShortString() + " (" + was.getBlock().getName().getString() + ") unwarded=" + groundFree + " dug=" + dug + " | light-well barred=" + wellBars);
    }

    /** Irel's own intro: frozen mid-command, arm up, the echoes turn east, she notices you, then the fight and her moves. */
    private static void irelIntro(ServerLevel ow) {
        DungeonTests.Scan s = site(ow, "eastwatch_irel_intro"); if (s == null) return;
        ServerLevel level = DungeonTests.echo(ow);
        var irel = com.matt.quantumfalcon.registry.ModEntities.SKYWARDEN_IREL.get().create(level);
        if (irel == null) { result("eastwatch_irel_intro", false, "no entity"); return; }
        BlockPos p = at(19, 20, 36);
        irel.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        level.addFreshEntity(irel);
        boolean frozenPose = irel.isCommanding();
        com.matt.quantumfalcon.entity.boss.BossDirector.intro(irel, com.matt.quantumfalcon.entity.boss.BossDirector.STYLES.get("skywarden_irel"), level);
        later(30, () -> {
            boolean armUp = irel.isCommanding() && irel.log.contains("intro:arm") && irel.log.contains("intro:east") && Math.abs(net.minecraft.util.Mth.wrapDegrees(irel.getYRot() + 90)) < 5;
            later(80, () -> {
                boolean fight = irel.log.contains("intro:noticed") && irel.log.contains("intro:fight") && !irel.isCommanding();
                FakePlayer fp = DungeonWardTests.survivor(level, "QFIrelTarget");
                fp.moveTo(irel.getX() + 3, irel.getY(), irel.getZ());
                irel.stoop(fp); irel.hood(fp); irel.eastWind();
                boolean moves = irel.log.containsAll(java.util.List.of("stoop", "hood", "east"));
                var tele = com.matt.quantumfalcon.entity.boss.BossDirector.lastTelegraph(irel);
                irel.discard();
                result("eastwatch_irel_intro", frozenPose && armUp && fight && moves && tele != null,
                    "frozen mid-command=" + frozenPose + " | intro: arm up, echoes east, facing east=" + armUp + " | noticed + fight=" + fight
                        + " | moves stoop/hood/east=" + moves + " last telegraph=" + (tele == null ? "none" : tele.move() + " " + tele.shape()) + " | log=" + irel.log);
            });
        });
    }
}
