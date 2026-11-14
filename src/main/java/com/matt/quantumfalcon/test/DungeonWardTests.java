package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.dungeon.DungeonWard;
import com.matt.quantumfalcon.dungeon.ModDungeon;
import com.matt.quantumfalcon.dungeon.ReturnRiftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

import java.util.Map;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** A7: one test per dungeon bypass, against a real placed dungeon_riftslate (inside) and an overworld pad (control). */
public final class DungeonWardTests {
    private DungeonWardTests() {}

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("a7_mine_wall", DungeonWardTests::mine);
        tests.put("a7_breakables_ok", DungeonWardTests::breakables);
        tests.put("a7_explosion", DungeonWardTests::explosion);
        tests.put("a7_piston", DungeonWardTests::piston);
        tests.put("a7_mob_griefing", DungeonWardTests::mobGrief);
        tests.put("a7_fluids", DungeonWardTests::fluids);
        tests.put("a7_bridge_pillar", DungeonWardTests::bridge);
        tests.put("a7_pearl_chorus", DungeonWardTests::pearlChorus);
        tests.put("pr2_foreign_teleport", DungeonWardTests::foreignTeleport);
        tests.put("pr21_machine_breakers", DungeonWardTests::machineBreakers);
        tests.put("a7_portal", DungeonWardTests::portal);
        tests.put("a7_falling", DungeonWardTests::falling);
        tests.put("a7_firecharge", DungeonWardTests::fireCharge);
        tests.put("a7_exempt_creative_op", DungeonWardTests::exempt);
        tests.put("a7_all_realm_dungeons", DungeonWardTests::allRealms);
    }

    // ------------------------------------------------------------------ helpers
    static DungeonTests.Scan dungeon(ServerLevel any, String test) { return DungeonTests.riftslateOrFail(DungeonTests.echo(any), test); }
    static ServerLevel echo(ServerLevel any) { return DungeonTests.echo(any); }

    /** A solid, non-breakable, non-block-entity dungeon block with air beside it (a real wall/floor/ceiling face). */
    static BlockPos wall(ServerLevel level, DungeonTests.Scan s) {
        for (StructurePiece piece : s.start.getPieces()) {
            var bb = piece.getBoundingBox();
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
                BlockState st = level.getBlockState(p);
                if (st.isAir() || st.is(DungeonWard.BREAKABLE) || st.hasBlockEntity() || !st.isCollisionShapeFullBlock(level, p)) continue;
                if (!level.getBlockState(p.above()).isAir()) continue;
                if (DungeonWard.inDungeon(level, p) && DungeonWard.inDungeon(level, p.above())) return p.immutable();
            }
        }
        return null;
    }

    /** n air blocks in a row (east) inside the dungeon, with a solid floor under the first. */
    static BlockPos corridor(ServerLevel level, DungeonTests.Scan s, int n) {
        for (StructurePiece piece : s.start.getPieces()) {
            var bb = piece.getBoundingBox();
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
                boolean ok = level.getBlockState(p.below()).isCollisionShapeFullBlock(level, p.below());
                for (int i = 0; i < n && ok; i++) ok = level.getBlockState(p.east(i)).isAir() && level.getBlockState(p.east(i).above()).isAir() && DungeonWard.inDungeon(level, p.east(i));
                if (ok) return p.immutable();
            }
        }
        return null;
    }

    static FakePlayer survivor(ServerLevel level, String name) {
        FakePlayer fp = AuditTests.freshPlayer(level, name);
        fp.setGameMode(GameType.SURVIVAL);
        fp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        return fp;
    }

    static BlockPos control(ServerLevel ow, int x) { return pad(ow, x, 31000 + DungeonTests.RUN_OFF, Blocks.STONE); }

    // ------------------------------------------------------------------ tests
    private static void mine(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "a7_mine_wall"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos w = wall(level, s);
        if (w == null) { result("a7_mine_wall", false, "no wall found"); return; }
        FakePlayer fp = survivor(level, "QFDigger");
        fp.moveTo(w.getX() + 0.5, w.getY() + 1, w.getZ() + 0.5);
        BlockState before = level.getBlockState(w);
        boolean broke = fp.gameMode.destroyBlock(w);
        boolean intact = level.getBlockState(w).equals(before);
        float speed = fp.getDigSpeed(before, w);
        // the floor under the player too (digging down)
        BlockPos below = w; boolean down = fp.gameMode.destroyBlock(below) || !level.getBlockState(below).equals(before);
        // control: the same pickaxe mines stone outside any dungeon
        BlockPos c = control(ow, 30000);
        FakePlayer cp = survivor(ow, "QFDiggerCtl");
        boolean ctl = cp.gameMode.destroyBlock(c.below());
        result("a7_mine_wall", !broke && intact && !down && speed == 0f && ctl,
            "wall " + before.getBlock().getName().getString() + " at " + w.toShortString() + " broke=" + broke + " intact=" + intact
                + " digSpeed=" + speed + " digDown=" + down + " op=" + fp.hasPermissions(2) + " | control stone broke=" + ctl);
    }

    private static void breakables(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "a7_breakables_ok"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos c = corridor(level, s, 3);
        if (c == null) { result("a7_breakables_ok", false, "no corridor"); return; }
        FakePlayer fp = survivor(level, "QFSecret");
        StringBuilder d = new StringBuilder(); boolean ok = true;
        for (var b : new net.minecraft.world.level.block.Block[]{ModDungeon.SECRET_WALL.get(), Blocks.COBWEB, ModDungeon.VOID_TENDRILS.get()}) {
            BlockState st = b == ModDungeon.VOID_TENDRILS.get()
                ? b.defaultBlockState().setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(Direction.DOWN), true) : b.defaultBlockState();
            level.setBlock(c, st, 3);
            boolean broke = fp.gameMode.destroyBlock(c) && level.getBlockState(c).isAir();
            ok &= broke;
            d.append(b.getName().getString()).append('=').append(broke).append(' ');
        }
        level.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
        result("a7_breakables_ok", ok, d.toString().trim());
    }

    private static int changed(ServerLevel level, BlockPos c, int r, Map<BlockPos, BlockState> before) {
        int n = 0;
        for (var e : before.entrySet()) if (!level.getBlockState(e.getKey()).equals(e.getValue())) n++;
        return n;
    }

    private static Map<BlockPos, BlockState> snapshot(ServerLevel level, BlockPos c, int r, boolean skipBreakable) {
        Map<BlockPos, BlockState> m = new java.util.HashMap<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
            BlockState st = level.getBlockState(p);
            if (st.isAir() || (skipBreakable && st.is(DungeonWard.BREAKABLE))) continue;
            if (skipBreakable && !DungeonWard.inDungeon(level, p)) continue;   // inside: only the dungeon's own blocks (terrain around it is fair game)
            m.put(p.immutable(), st);
        }
        return m;
    }

    private static void explosion(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "a7_explosion"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos w = wall(level, s);
        if (w == null) { result("a7_explosion", false, "no wall"); return; }
        BlockPos at = w.above();
        var before = snapshot(level, at, 4, true);
        level.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 4f, Level.ExplosionInteraction.TNT);
        int lost = changed(level, at, 4, before);
        // a creeper-strength mob blast too
        var creeper = EntityType.CREEPER.create(level);
        level.explode(creeper, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 3f, Level.ExplosionInteraction.MOB);
        int lostMob = changed(level, at, 4, before);
        BlockPos c = control(ow, 30040);
        var cb = snapshot(ow, c.below(), 3, false);
        ow.explode(null, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 4f, Level.ExplosionInteraction.TNT);
        int ctl = changed(ow, c.below(), 3, cb);
        result("a7_explosion", lost == 0 && lostMob == 0 && ctl > 0,
            "dungeon blocks changed by TNT-strength blast=" + lost + " (of " + before.size() + ") after creeper blast=" + lostMob + " | control blocks destroyed=" + ctl);
    }

    private static void piston(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "a7_piston"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos c = corridor(level, s, 4);
        if (c == null) { result("a7_piston", false, "no corridor"); return; }
        // piston at c facing east, a block at c+1, air at c+2; power it from above. The control runs on a fresh overworld pad
        // once its chunk is ticking (block events wait for a ticking chunk).
        level.setBlock(c, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST), 3);
        level.setBlock(c.east(), Blocks.OAK_PLANKS.defaultBlockState(), 3);
        BlockPos o = control(ow, 30080);
        WorldDropsTests.force(ow, o, true);
        WorldDropsTests.whenTicking(ow, o, 0, () -> {
            ow.setBlock(o, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST), 3);
            ow.setBlock(o.east(), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            later(2, () -> {
                level.setBlock(c.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                ow.setBlock(o.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                later(20, () -> {
                    boolean ticking = level.isPositionEntityTicking(c);
                    boolean stayed = level.getBlockState(c.east()).is(Blocks.OAK_PLANKS) && !level.getBlockState(c.east(2)).is(Blocks.OAK_PLANKS);
                    boolean extended = level.getBlockState(c).is(Blocks.PISTON) && level.getBlockState(c).getValue(PistonBaseBlock.EXTENDED);
                    boolean ctl = ow.getBlockState(o.east(2)).is(Blocks.OAK_PLANKS);
                    for (BlockPos p : new BlockPos[]{c.above(), c, c.east(), c.east(2)}) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    WorldDropsTests.force(ow, o, false);
                    result("a7_piston", ticking && stayed && !extended && ctl, "dungeon chunk ticking=" + ticking + " dungeon piston pushed=" + !stayed + " extended=" + extended + " | control piston pushed=" + ctl);
                });
            });
        });
    }

    private static void mobGrief(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "a7_mob_griefing"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos c = corridor(level, s, 1), w = wall(level, s);
        if (c == null || w == null) { result("a7_mob_griefing", false, "no spot"); return; }
        var ender = EntityType.ENDERMAN.create(level);
        ender.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        boolean grief = EventHooks.canEntityGrief(level, ender);
        var wither = EntityType.WITHER.create(level);
        wither.moveTo(w.getX() + 0.5, w.getY() + 1, w.getZ() + 0.5);
        boolean destroy = net.neoforged.neoforge.common.CommonHooks.canEntityDestroy(level, w, wither);
        BlockPos o = control(ow, 30120);
        var ender2 = EntityType.ENDERMAN.create(ow);
        ender2.moveTo(o.getX() + 0.5, o.getY(), o.getZ() + 0.5);
        boolean griefOut = EventHooks.canEntityGrief(ow, ender2);
        ender.discard(); wither.discard(); ender2.discard();
        result("a7_mob_griefing", !grief && !destroy && griefOut, "enderman may grief inside=" + grief + " wither may destroy wall=" + destroy + " | enderman outside may grief=" + griefOut);
    }

    static boolean madeBlock(BlockState st) {
        return st.is(Blocks.COBBLESTONE) || st.is(Blocks.OBSIDIAN) || st.is(Blocks.STONE) || st.is(Blocks.BASALT);
    }

    /**
     * Fluids can't change a dungeon (deterministic: only dungeon blocks that were not already stone/cobble/obsidian
     * count, so where the corridor ends no longer matters):
     * (1) a survivor's bucket inside; (2) a dispenser aimed into the dungeon with water, lava, powder snow, a shulker box
     * (3) a lava+water generator inside; (4) a water source poured just outside a hole in the wall doesn't flow in.
     * Controls: the same generator and dispenser on an overworld pad work.
     */
    private static void fluids(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "a7_fluids"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos c = corridor(level, s, 3);
        if (c == null) { result("a7_fluids", false, "no corridor"); return; }
        FakePlayer fp = survivor(level, "QFPlumber");
        fp.moveTo(c.getX() + 0.5, c.getY(), c.getZ() - 1.5);
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        fp.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        var hit = new BlockHitResult(Vec3.atCenterOf(c.below()).add(0, 0.5, 0), Direction.UP, c.below(), false);
        fp.gameMode.useItemOn(fp, level, bucket, InteractionHand.MAIN_HAND, hit);
        boolean poured = level.getFluidState(c).isSource();
        if (poured) level.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
        // (2) dispenser in the corridor aimed east into the dungeon
        StringBuilder disp = new StringBuilder();
        boolean dispOk = dispenseInto(level, c, c.east(), disp);
        BlockPos o = control(ow, 30160);
        StringBuilder ctlDisp = new StringBuilder();
        ow.setBlock(o.above(2), Blocks.AIR.defaultBlockState(), 3);
        boolean ctlDispensed = !dispenseInto(ow, o.above(), o.above().east(), ctlDisp);
        // (3) generator: snapshot of the dungeon blocks around it first
        java.util.Set<BlockPos> already = new java.util.HashSet<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-2, -2, -2), c.offset(5, 2, 2)))
            if (madeBlock(level.getBlockState(p))) already.add(p.immutable());
        level.setBlock(c.east(), Blocks.LAVA.defaultBlockState(), 3);
        level.setBlock(c.east(2), Blocks.WATER.defaultBlockState(), 3);
        ow.setBlock(o, Blocks.LAVA.defaultBlockState(), 3);
        ow.setBlock(o.east(), Blocks.WATER.defaultBlockState(), 3);
        // (4) a hole in an outer wall with water poured just outside it
        BlockPos[] hole = outerHole(level, s, c);   // far from the generator, whose water may flow inside
        java.util.Map<BlockPos, BlockState> restore = new java.util.LinkedHashMap<>();
        if (hole != null) {
            for (BlockPos p : new BlockPos[]{hole[1], hole[2], hole[2].below()}) restore.put(p, level.getBlockState(p));
            level.setBlock(hole[1], Blocks.AIR.defaultBlockState(), 2);          // the hole (warded wall cell)
            if (!level.getBlockState(hole[2].below()).isCollisionShapeFullBlock(level, hole[2].below()))
                level.setBlock(hole[2].below(), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(hole[2], Blocks.WATER.defaultBlockState(), 3);         // poured outside
        }
        later(60, () -> {
            int made = 0;
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-2, -2, -2), c.offset(5, 2, 2)))
                if (!already.contains(p) && madeBlock(level.getBlockState(p)) && DungeonWard.inDungeon(level, p)) made++;
            boolean ctl = false;
            for (BlockPos p : BlockPos.betweenClosed(o.offset(-1, -1, -1), o.offset(2, 1, 1)))
                if (ow.getBlockState(p).is(Blocks.COBBLESTONE) || ow.getBlockState(p).is(Blocks.OBSIDIAN) || ow.getBlockState(p).is(Blocks.STONE) && p.getY() >= o.getY()) ctl = true;
            boolean flowedIn = hole != null && (!level.getFluidState(hole[1]).isEmpty() || !level.getFluidState(hole[0]).isEmpty());
            String holeTxt = hole == null ? "no outer wall found" : "hole " + hole[1].toShortString() + " flowed in=" + flowedIn;
            if (hole != null) {
                level.setBlock(hole[2], Blocks.AIR.defaultBlockState(), 2);
                restore.forEach((p, st) -> level.setBlock(p, st, 2));
            }
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(5, 2, 2)))
                if (!level.getFluidState(p).isEmpty()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            for (BlockPos p : BlockPos.betweenClosed(o.offset(-1, 0, -1), o.offset(3, 3, 1)))
                if (!ow.getFluidState(p).isEmpty() || ow.getBlockState(p).is(Blocks.DISPENSER)) ow.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            result("a7_fluids", !poured && dispOk && made == 0 && hole != null && !flowedIn && ctl && ctlDispensed,
                "bucket poured inside=" + poured + " | dispenser into dungeon: " + disp + "| generator blocks made inside=" + made
                    + " | outside pour: " + holeTxt + " | control generator=" + ctl + " control dispenser: " + ctlDisp);
        });
    }

    /**
     * Puts a dispenser at {@code at} facing {@code target} and fires water, lava, powder snow and a shulker box at it.
     * True when nothing landed (the target stayed air with no fluid) for every item.
     */
    static boolean dispenseInto(ServerLevel level, BlockPos at, BlockPos target, StringBuilder log) {
        BlockState was = level.getBlockState(at);
        Direction f = Direction.getNearest(target.getX() - at.getX(), target.getY() - at.getY(), target.getZ() - at.getZ());
        BlockState ds = Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, f);
        level.setBlock(at, ds, 3);
        boolean none = true;
        if (level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity be) {
            for (var item : new net.minecraft.world.item.Item[]{Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.POWDER_SNOW_BUCKET, Items.SHULKER_BOX}) {
                var beh = net.minecraft.world.level.block.DispenserBlock.DISPENSER_REGISTRY.get(item);
                if (beh == null) { log.append(item).append("=no behaviour "); continue; }
                beh.dispense(new net.minecraft.core.dispenser.BlockSource(level, at, ds, be), new ItemStack(item));
                boolean landed = !level.getBlockState(target).isAir() || !level.getFluidState(target).isEmpty();
                log.append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath()).append('=').append(landed ? "LANDED " : "blocked ");
                if (landed) { none = false; level.setBlock(target, Blocks.AIR.defaultBlockState(), 3); }
            }
        } else { log.append("no dispenser block entity "); none = false; }
        level.removeBlockEntity(at);
        level.setBlock(at, was, 3);
        return none;
    }

    /** {inside air cell, warded wall cell next to it, outside cell past the wall} with the outside cell not in any dungeon. */
    static BlockPos[] outerHole(ServerLevel level, DungeonTests.Scan s, BlockPos avoid) {
        for (StructurePiece piece : s.start.getPieces()) {
            var bb = piece.getBoundingBox();
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY() + 1, bb.minZ(), bb.maxX(), bb.maxY() - 1, bb.maxZ())) {
                if (p.distManhattan(avoid) < 16) continue;
                if (!level.getBlockState(p).isAir() || !DungeonWard.inDungeon(level, p)) continue;
                if (!level.getBlockState(p.below()).isCollisionShapeFullBlock(level, p.below())) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos w = p.relative(d), out = w.relative(d);
                    BlockState ws = level.getBlockState(w);
                    if (ws.isAir() || ws.hasBlockEntity() || !DungeonWard.inDungeon(level, w) || DungeonWard.inDungeon(level, out)) continue;
                    if (level.getBlockState(out).hasBlockEntity()) continue;
                    return new BlockPos[]{p.immutable(), w.immutable(), out.immutable()};
                }
            }
        }
        return null;
    }

    /** sand dropped into a corridor drops as an item; the same sand on an overworld pad lands as a block. */
    private static void falling(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "a7_falling"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos c = corridor(level, s, 1);
        if (c == null) { result("a7_falling", false, "no corridor"); return; }
        var aabb = new net.minecraft.world.phys.AABB(c).inflate(3);
        BlockPos o = control(ow, 30280);
        // on the first run of a session the chunks weren't entity-ticking yet, so the sand never fell. Force and wait.
        WorldDropsTests.force(level, c, true); WorldDropsTests.force(ow, o, true);
        WorldDropsTests.whenTicking(level, c, 0, () -> WorldDropsTests.whenTicking(ow, o, 0, () -> {
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, aabb).forEach(net.minecraft.world.entity.Entity::discard);
        net.minecraft.world.entity.item.FallingBlockEntity.fall(level, c.above(), Blocks.SAND.defaultBlockState());
        ow.setBlock(o.above(3), Blocks.AIR.defaultBlockState(), 3);
        net.minecraft.world.entity.item.FallingBlockEntity.fall(ow, o.above(3), Blocks.SAND.defaultBlockState());
        later(60, () -> {
            boolean landed = level.getBlockState(c).is(Blocks.SAND) || level.getBlockState(c.above()).is(Blocks.SAND);
            var items = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, aabb, ie -> ie.getItem().is(Items.SAND));
            boolean dropped = !items.isEmpty();
            items.forEach(net.minecraft.world.entity.Entity::discard);
            if (level.getBlockState(c).is(Blocks.SAND)) level.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
            if (level.getBlockState(c.above()).is(Blocks.SAND)) level.setBlock(c.above(), Blocks.AIR.defaultBlockState(), 3);
            boolean ctl = false;
            for (int y = 0; y <= 3; y++) if (ow.getBlockState(o.above(y)).is(Blocks.SAND)) { ctl = true; ow.setBlock(o.above(y), Blocks.AIR.defaultBlockState(), 3); }
            WorldDropsTests.force(ow, o, false);   // the dungeon chunks stay forced: other tests use the same site
            result("a7_falling", !landed && dropped && ctl, "sand in a dungeon corridor landed as a block=" + landed + " dropped as an item=" + dropped + " | control sand landed=" + ctl);
        });
        }));
    }

    /** an ownerless small fireball (a dispensed fire charge, a blaze) fired at a dungeon floor lights nothing; the control does. */
    private static void fireCharge(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "a7_firecharge"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos c = corridor(level, s, 1);
        if (c == null) { result("a7_firecharge", false, "no corridor"); return; }
        BlockPos o = control(ow, 30320);
        ow.setBlock(o.below(), Blocks.OAK_PLANKS.defaultBlockState(), 3);
        WorldDropsTests.force(level, c, true); WorldDropsTests.force(ow, o, true);
        WorldDropsTests.whenTicking(level, c, 0, () -> WorldDropsTests.whenTicking(ow, o, 0, () -> {
            for (var pair : java.util.List.of(java.util.Map.entry(level, c), java.util.Map.entry(ow, o))) {
                var fb = new net.minecraft.world.entity.projectile.SmallFireball(pair.getKey(), pair.getValue().getX() + 0.5, pair.getValue().getY() + 0.9, pair.getValue().getZ() + 0.5,
                    new net.minecraft.world.phys.Vec3(0, -1, 0));
                pair.getKey().addFreshEntity(fb);
            }
            later(40, () -> {
                int inside = 0, outside = 0;
                for (BlockPos p : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, 2, 2))) if (level.getBlockState(p).is(net.minecraft.tags.BlockTags.FIRE)) { inside++; level.setBlock(p, Blocks.AIR.defaultBlockState(), 3); }
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-2, -1, -2), o.offset(2, 2, 2))) if (ow.getBlockState(p).is(net.minecraft.tags.BlockTags.FIRE)) { outside++; ow.setBlock(p, Blocks.AIR.defaultBlockState(), 3); }
                WorldDropsTests.force(ow, o, false);   // the dungeon chunks stay forced: other tests use the same site
                result("a7_firecharge", inside == 0 && outside > 0, "ownerless small fireball: fire in the dungeon=" + inside + " | control fire=" + outside);
            });
        }));
    }

    private static void bridge(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "a7_bridge_pillar"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos c = corridor(level, s, 2);
        if (c == null) { result("a7_bridge_pillar", false, "no corridor"); return; }
        FakePlayer fp = survivor(level, "QFBridger");
        fp.moveTo(c.getX() + 0.5, c.getY(), c.getZ() - 1.5);
        boolean cobble = Round3Tests.placeAsPlayer(fp, c, Items.COBBLESTONE);
        boolean scaffold = Round3Tests.placeAsPlayer(fp, c, Items.DIRT);
        boolean torch = Round3Tests.placeAsPlayer(fp, c.east(), Items.TORCH);
        level.setBlock(c.east(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
        BlockPos o = control(ow, 30200);
        FakePlayer cp = survivor(ow, "QFBridgerCtl");
        boolean ctl = Round3Tests.placeAsPlayer(cp, o, Items.COBBLESTONE);
        result("a7_bridge_pillar", !cobble && !scaffold && torch && ctl, "placed inside: cobblestone=" + cobble + " dirt=" + scaffold + " torch(allowed)=" + torch + " | control cobblestone=" + ctl);
    }

    private static void pearlChorus(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "a7_pearl_chorus"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos c = corridor(level, s, 1);
        if (c == null) { result("a7_pearl_chorus", false, "no corridor"); return; }
        FakePlayer fp = survivor(level, "QFPearlRunner");
        fp.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        var bb = s.start.getBoundingBox();
        Vec3 out = new Vec3(bb.maxX() + 30, c.getY() + 40, bb.maxZ() + 30);
        var pearl = EntityType.ENDER_PEARL.create(level);
        var hit = new BlockHitResult(out, Direction.UP, BlockPos.containing(out), false);
        var ev = new EntityTeleportEvent.EnderPearl(fp, out.x, out.y, out.z, pearl, 5f, hit);
        NeoForge.EVENT_BUS.post(ev);
        boolean pearlOut = !ev.isCanceled();
        var evIn = new EntityTeleportEvent.EnderPearl(fp, c.getX() + 0.5, c.getY(), c.getZ() + 3.5, pearl, 5f, hit);
        NeoForge.EVENT_BUS.post(evIn);
        boolean pearlInside = !evIn.isCanceled();
        var zombie = EntityType.ZOMBIE.create(level);
        zombie.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        var ch = EventHooks.onChorusFruitTeleport(fp, out.x, out.y, out.z);
        boolean chorus = !ch.isCanceled();
        // a pearl thrown from outside INTO the dungeon (skipping the door) is stopped too
        fp.moveTo(out.x, out.y, out.z);
        var evInto = new EntityTeleportEvent.EnderPearl(fp, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, pearl, 5f, hit);
        NeoForge.EVENT_BUS.post(evInto);
        boolean pearlInto = !evInto.isCanceled();
        // control: outside to outside
        var evCtl = new EntityTeleportEvent.EnderPearl(fp, out.x + 10, out.y, out.z, pearl, 5f, hit);
        NeoForge.EVENT_BUS.post(evCtl);
        boolean ctl = !evCtl.isCanceled();
        zombie.discard();
        result("a7_pearl_chorus", !pearlOut && !pearlInside && !chorus && !pearlInto && ctl,
            "pearl out=" + pearlOut + " pearl within=" + pearlInside + " chorus out=" + chorus + " pearl in from outside=" + pearlInto + " | control pearl outside=" + ctl);
    }

    /** foreign teleports are off by default; a plain teleportTo (no event) into a ward is undone by the jump watch. */
    private static void foreignTeleport(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "pr2_foreign_teleport"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos c = corridor(level, s, 1);
        if (c == null) { result("pr2_foreign_teleport", false, "no corridor"); return; }
        boolean defOff = !com.matt.quantumfalcon.WorldConfig.wardAllowsForeignTeleports();
        FakePlayer fp = survivor(level, "QFWarpRunner");
        var bb = s.start.getBoundingBox();
        Vec3 out = new Vec3(bb.maxX() + 30, c.getY() + 40, bb.maxZ() + 30);
        fp.moveTo(out.x, out.y, out.z);
        // a command-style teleport event into the ward is cancelled
        var cmd = new EntityTeleportEvent.TeleportCommand(fp, c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        NeoForge.EVENT_BUS.post(cmd);
        boolean cmdBlocked = cmd.isCanceled();
        // a plain teleportTo fires nothing: the jump watch sends the player back
        DungeonWard.guardJump(fp);
        fp.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        boolean undone = DungeonWard.guardJump(fp);
        boolean back = fp.position().distanceTo(out) < 1.0;
        // control: a walk-sized step inside, and a long jump between two outside spots, are left alone
        fp.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        DungeonWard.forgetPos(fp.getUUID());   // entered properly (through the door)
        DungeonWard.guardJump(fp);
        fp.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 1.5);
        boolean stepOk = !DungeonWard.guardJump(fp);
        fp.moveTo(out.x, out.y, out.z); DungeonWard.guardJump(fp);
        fp.moveTo(out.x + 40, out.y, out.z);
        boolean outsideOk = !DungeonWard.guardJump(fp);
        result("pr2_foreign_teleport", defOff && cmdBlocked && undone && back && stepOk && outsideOk,
            "default off=" + defOff + " command tp blocked=" + cmdBlocked + " plain teleportTo undone=" + undone + " back outside=" + back
            + " | control step inside ok=" + stepOk + " outside jump ok=" + outsideOk);
    }

    /**
     * (1) a warded wall reads hardness -1 to machines inside the ward and its normal hardness on a control pad;
     * (2) a removal issued from code outside this mod (simulated by a call chain of JDK frames longer than the trusted
     * window) is refused, both setBlock-to-air and destroyBlock; (3) the same chain removes a block on the control pad;
     * (4) a crumbling wall (dungeon_breakable) still goes; (5) our own code (a direct call) can still change the wall.
     */
    private static void machineBreakers(ServerLevel ow) {
        String t = "pr21_machine_breakers";
        DungeonTests.Scan s = dungeon(ow, t); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos w = wall(level, s);
        if (w == null) { result(t, false, "no wall found"); return; }
        BlockState before = level.getBlockState(w);
        float hardIn = before.getDestroySpeed(level, w);
        BlockPos c = control(ow, 30240).below();
        ow.setBlockAndUpdate(c, before);
        float hardOut = before.getDestroySpeed(ow, c);
        java.util.function.BiFunction<BlockPos, BlockState, Boolean> setIn = level::setBlockAndUpdate, setOut = ow::setBlockAndUpdate;
        java.util.function.BiFunction<BlockPos, Boolean, Boolean> destroyIn = level::destroyBlock;
        for (int i = 0; i < 16; i++) {
            setIn = setIn.andThen(java.util.function.Function.identity());
            setOut = setOut.andThen(java.util.function.Function.identity());
            destroyIn = destroyIn.andThen(java.util.function.Function.identity());
        }
        boolean setRefused = !setIn.apply(w, Blocks.AIR.defaultBlockState()) && level.getBlockState(w).equals(before);
        boolean waterRefused = !setIn.apply(w, Blocks.WATER.defaultBlockState()) && level.getBlockState(w).equals(before);
        boolean destroyRefused = !destroyIn.apply(w, true) && level.getBlockState(w).equals(before);
        boolean outsideOk = setOut.apply(c, Blocks.AIR.defaultBlockState()) && ow.getBlockState(c).isAir();
        BlockState crumble = null; BlockPos cw = null;
        for (StructurePiece piece : s.start.getPieces()) {
            var bb = piece.getBoundingBox();
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ()))
                if (level.getBlockState(p).is(DungeonWard.BREAKABLE)) { cw = p.immutable(); crumble = level.getBlockState(p); break; }
            if (cw != null) break;
        }
        boolean crumbleOk = true;
        if (cw != null) { crumbleOk = setIn.apply(cw, Blocks.AIR.defaultBlockState()) && level.getBlockState(cw).isAir(); level.setBlockAndUpdate(cw, crumble); }
        boolean ours = level.setBlockAndUpdate(w, Blocks.AIR.defaultBlockState()) && level.getBlockState(w).isAir();
        level.setBlockAndUpdate(w, before);
        ow.setBlockAndUpdate(c, Blocks.STONE.defaultBlockState());
        result(t, hardIn == -1f && hardOut > 0f && setRefused && waterRefused && destroyRefused && outsideOk && crumbleOk && ours,
            "wall " + before.getBlock().getName().getString() + " at " + w.toShortString() + " hardness in ward=" + hardIn + " on pad=" + hardOut
                + " | foreign setBlock air refused=" + setRefused + " water refused=" + waterRefused + " destroyBlock refused=" + destroyRefused
                + " | pad removal ok=" + outsideOk + " | crumbling wall " + (cw == null ? "none in this dungeon" : "removed=" + crumbleOk) + " | own code ok=" + ours);
    }

    private static void portal(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "a7_portal"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos c = corridor(level, s, 1);
        if (c == null) { result("a7_portal", false, "no corridor"); return; }
        FakePlayer fp = survivor(level, "QFPortalRunner");
        fp.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        var ev = new EntityTravelToDimensionEvent(fp, Level.OVERWORLD);
        NeoForge.EVENT_BUS.post(ev);
        boolean left = !ev.isCanceled();
        var pig = EntityType.PIG.create(level);
        pig.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        var pev = new EntityTravelToDimensionEvent(pig, Level.NETHER); NeoForge.EVENT_BUS.post(pev);
        boolean pigLeft = !pev.isCanceled();
        BlockPos o = control(ow, 30240);
        FakePlayer cp = survivor(ow, "QFPortalCtl");
        cp.moveTo(o.getX() + 0.5, o.getY(), o.getZ() + 0.5);
        var evCtl = new EntityTravelToDimensionEvent(cp, Level.NETHER);
        NeoForge.EVENT_BUS.post(evCtl);
        boolean ctl = !evCtl.isCanceled();
        pig.discard();
        // arrivals. A nether portal (vanilla PortalForcer) and a Rift portal (CustomPortalAPI) asked to arrive in the
        // corridor must leave every dungeon block as it was and stand outside the dungeon.
        java.util.Map<BlockPos, BlockState> before = new java.util.HashMap<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-20, -8, -20), c.offset(20, 10, 20)))
            if (DungeonWard.inDungeon(level, p)) before.put(p.immutable(), level.getBlockState(p));
        var vanilla = level.getPortalForcer().createPortal(c, Direction.Axis.X);
        var rift = net.kyrptonaught.customportalapi.portal.PortalPlacer.createDestinationPortal(level, c,
            com.matt.quantumfalcon.registry.ModBlocks.RIFT_FRAME.get().defaultBlockState(), Direction.Axis.X);
        int changed = 0;
        for (var e : before.entrySet()) if (!level.getBlockState(e.getKey()).equals(e.getValue())) changed++;
        boolean vOut = vanilla.isPresent() && !DungeonWard.inDungeon(level, vanilla.get().minCorner);
        boolean rOut = rift.isPresent() && !DungeonWard.inDungeon(level, rift.get().minCorner);
        String where = (vanilla.map(r -> " nether at " + r.minCorner.toShortString()).orElse(" nether none"))
            + (rift.map(r -> " rift at " + r.minCorner.toShortString()).orElse(" rift none"));
        // control: the same call on an overworld pad far from dungeons is left where it was asked
        BlockPos ask = o.above(1).east(40);
        var ctlPortal = ow.getPortalForcer().createPortal(ask, Direction.Axis.X);
        boolean ctlNear = ctlPortal.isPresent() && ctlPortal.get().minCorner.distManhattan(ask) < 40;
        clearPortal(level, vanilla); clearPortal(level, rift); clearPortal(ow, ctlPortal);
        result("a7_portal", !left && !pigLeft && ctl && changed == 0 && vOut && rOut && ctlNear,
            "player portal out=" + left + " mob portal out=" + pigLeft + " | arrival: dungeon blocks changed=" + changed + "/" + before.size()
                + " nether outside=" + vOut + " rift outside=" + rOut + where + " | control overworld portal=" + ctl + " control arrival near ask=" + ctlNear);
    }

    /** Removes the portal blocks (and frame) of a test arrival portal. */
    static void clearPortal(ServerLevel level, java.util.Optional<net.minecraft.BlockUtil.FoundRectangle> r) {
        if (r.isEmpty()) return;
        BlockPos m = r.get().minCorner;
        for (BlockPos p : BlockPos.betweenClosed(m.offset(-1, -1, -1), m.offset(r.get().axis1Size + 1, r.get().axis2Size + 1, 1))) {
            if (DungeonWard.inDungeon(level, p)) continue;
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.NETHER_PORTAL) || st.is(Blocks.OBSIDIAN) || st.is(com.matt.quantumfalcon.registry.ModBlocks.RIFT_FRAME.get())
                || st.is(com.matt.quantumfalcon.registry.ModBlocks.RIFT_PORTAL.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    private static void exempt(ServerLevel ow) {
        DungeonTests.Scan s = dungeon(ow, "a7_exempt_creative_op"); if (s == null) return;
        ServerLevel level = echo(ow);
        BlockPos w = wall(level, s);
        if (w == null) { result("a7_exempt_creative_op", false, "no wall"); return; }
        BlockState keep = level.getBlockState(w);
        FakePlayer cr = survivor(level, "QFBuilderCreative");
        cr.setGameMode(GameType.CREATIVE);
        boolean creative = cr.gameMode.destroyBlock(w) && level.getBlockState(w).isAir();
        level.setBlock(w, keep, 3);
        FakePlayer op = survivor(level, "QFBuilderOp");
        var list = level.getServer().getPlayerList();
        list.op(op.getGameProfile());
        boolean isOp = op.hasPermissions(2);
        boolean opBroke = op.gameMode.destroyBlock(w) && level.getBlockState(w).isAir();
        level.setBlock(w, keep, 3);
        BlockPos c = w.above();
        boolean opPlaced = Round3Tests.placeAsPlayer(op, c, Items.COBBLESTONE);
        level.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
        list.deop(op.getGameProfile());
        boolean deopped = !op.hasPermissions(2) && !op.gameMode.destroyBlock(w);
        level.setBlock(w, keep, 3);
        result("a7_exempt_creative_op", creative && isOp && opBroke && opPlaced && deopped,
            "creative broke wall=" + creative + " op(level2)=" + isOp + " op broke=" + opBroke + " op placed=" + opPlaced + " after deop warded again=" + deopped);
    }

    private static void allRealms(ServerLevel ow) {
        var reg = ow.registryAccess().registryOrThrow(Registries.STRUCTURE);
        StringBuilder missing = new StringBuilder();
        for (String id : DungeonTests.STRUCTURES) {
            var h = reg.getHolder(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("quantumfalcon", id)));
            if (h.isEmpty() || !h.get().is(ReturnRiftBlock.DUNGEONS)) missing.append(id).append(' ');
        }
        // and each placed realm dungeon (from dungeon_layouts) answers inDungeon for a block of its own
        int checked = 0, warded = 0;
        for (var e : DungeonTests.PLACED.entrySet()) {
            var pieces = e.getValue().start.getPieces();
            if (pieces.isEmpty()) continue;
            var lvl = echo(ow);
            BlockPos mid = pieces.get(pieces.size() / 2).getBoundingBox().getCenter();
            checked++;
            if (DungeonWard.inDungeon(lvl, mid)) warded++;
        }
        result("a7_all_realm_dungeons", missing.isEmpty() && checked > 0 && warded == checked,
            "dungeon structures not in #quantumfalcon:dungeons: [" + missing.toString().trim() + "] placed dungeons warded " + warded + "/" + checked);
    }
}
