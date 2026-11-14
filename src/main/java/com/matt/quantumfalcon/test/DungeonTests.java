package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.dungeon.*;
import com.matt.quantumfalcon.registry.ModDimensions;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** Stage 3: jigsaw dungeons (layout, key, door, treasury, shortcut, traps, puzzle, natural spawning) and arena templates. */
public final class DungeonTests {
    private DungeonTests() {}

    static final String[] STRUCTURES = {"dungeon_riftslate", "dungeon_aurorite", "dungeon_prismite", "dungeon_cinderite", "dungeon_chronoslate",
        "dungeon_abyssite", "dungeon_voidshale", "dungeon_singularite", "dungeon_overworld_riftslate", "dungeon_overworld_aurorite",
        "dungeon_nether_cinderite", "dungeon_end_voidshale"};

    /** Scan of one placed dungeon. */
    public static final class Scan {
        final String id; final StructureStart start; final List<String> pieces = new ArrayList<>();
        final Map<Block, List<BlockPos>> blocks = new HashMap<>();
        final List<DungeonGuardBlockEntity> guards = new ArrayList<>();
        final List<BlockPos> lecterns = new ArrayList<>();
        int jigsaws;
        Scan(String id, StructureStart start) { this.id = id; this.start = start; }
        List<BlockPos> of(Block b) { return blocks.getOrDefault(b, List.of()); }
        boolean hasPiece(String s) { return pieces.stream().anyMatch(p -> p.contains(s)); }
    }

    static final Map<String, Scan> PLACED = new HashMap<>();

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("dungeon_layouts", DungeonTests::testLayouts);
        tests.put("dungeon_key_door", DungeonTests::testKeyDoor);
        tests.put("dungeon_treasury", DungeonTests::testTreasury);
        tests.put("dungeon_shortcut_checkpoint", DungeonTests::testShortcut);
        tests.put("dungeon_trap", DungeonTests::testTrap);
        tests.put("dungeon_puzzle", DungeonTests::testPuzzle);
        tests.put("dungeon_natural", DungeonTests::testNatural);
        tests.put("arena_templates", DungeonTests::testArenaTemplates);
        tests.put("dungeon_locate_command", DungeonTests::testLocateCommand);
    }

    // ------------------------------------------------------------------ placement helpers
    static Holder.Reference<Structure> structure(ServerLevel level, String path) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE)
            .getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, path)));
    }

    /** Like /place structure, but also records the start and references so structure lookups (return rift) work. */
    /** Fresh boss-fight sites every server run (stale rift tiles from an interrupted run would hide the signature). */
    static final int RUN_OFF = (int) ((System.currentTimeMillis() / 1000) % 4000) * 4000;   // period ~67 min (was 200 s: reruns landed on earlier runs' leftover arenas)

    static Scan place(ServerLevel level, String id, BlockPos at) {
        var h = structure(level, id);
        var gen = level.getChunkSource().getGenerator();
        // echo_dungeon skips void columns and build-limit peaks, so walk east until the surface suits it
        ChunkPos cp = new ChunkPos(at);
        StructureStart start = StructureStart.INVALID_START;
        // Seed-independent: walk a 12x12 grid of chunks, and on each pass also vary the jigsaw seed, so a world whose terrain
        // or layout rolls reject the first grid still finds a start (the old search gave INVALID START on some seeds).
        for (int pass = 0; pass < 4 && !start.isValid(); pass++) {
            long seed = level.getSeed() ^ (pass * 0x9E3779B97F4A7C15L);
            int ox = pass * 97, oz = pass * 53;   // a different patch of terrain each pass
            for (int k = 0; k < 144 && !start.isValid(); k++) {
                cp = new ChunkPos((at.getX() >> 4) + ox + (k % 12) * 7, (at.getZ() >> 4) + oz + (k / 12) * 7);
                try {
                    start = h.value().generate(level.registryAccess(), gen, gen.getBiomeSource(), level.getChunkSource().randomState(),
                        level.getStructureManager(), seed, cp, 0, level, b -> true);
                } catch (RuntimeException ex) { start = StructureStart.INVALID_START; }
            }
        }
        if (!start.isValid()) return null;
        BoundingBox bb = start.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
            level.setChunkForced(cx, cz, true);
            ChunkAccess c = level.getChunk(cx, cz);
            // drop references to starts saved by earlier runs (older templates) so lookups find this placement
            var refs = new java.util.HashMap<>(c.getAllReferences()); refs.remove(h.value()); c.setAllReferences(refs);
            ChunkPos p = new ChunkPos(cx, cz);
            start.placeInChunk(level, level.structureManager(), gen, level.getRandom(),
                new BoundingBox(p.getMinBlockX(), level.getMinBuildHeight(), p.getMinBlockZ(), p.getMaxBlockX(), level.getMaxBuildHeight(), p.getMaxBlockZ()), p);
            c.addReferenceForStructure(h.value(), cp.toLong());
            c.setUnsaved(true);
        }
        level.getChunk(cp.x, cp.z).setStartForStructure(h.value(), start);
        return scan(level, id, start);
    }

    static Scan scan(ServerLevel level, String id, StructureStart start) {
        Scan s = new Scan(id, start);
        Set<Block> want = Set.of(ModDungeon.KEY_DOOR.get(), ModDungeon.TREASURY_VAULT.get(), ModDungeon.TRAP_PLATE.get(), ModDungeon.LORE_LECTERN.get(),
            ModDungeon.DANGER_SIGN.get(), ModDungeon.CHECKPOINT.get(), ModDungeon.RETURN_RIFT.get(), ModDungeon.GUARD.get(), Blocks.IRON_DOOR, Blocks.LEVER,
            Blocks.SPAWNER, Blocks.CHEST, Blocks.DISPENSER);
        Set<BlockPos> seen = new HashSet<>();
        for (StructurePiece piece : start.getPieces()) {
            s.pieces.add(piece instanceof PoolElementStructurePiece pe ? pe.getElement().toString() : piece.getClass().getSimpleName());
            for (BlockPos p : BlockPos.betweenClosed(piece.getBoundingBox().minX(), piece.getBoundingBox().minY(), piece.getBoundingBox().minZ(),
                piece.getBoundingBox().maxX(), piece.getBoundingBox().maxY(), piece.getBoundingBox().maxZ())) {
                if (!seen.add(p.immutable())) continue;
                BlockState st = level.getBlockState(p);
                if (st.is(Blocks.JIGSAW)) s.jigsaws++;
                if (want.contains(st.getBlock())) s.blocks.computeIfAbsent(st.getBlock(), k -> new ArrayList<>()).add(p.immutable());
                if (st.is(ModDungeon.GUARD.get()) && level.getBlockEntity(p) instanceof DungeonGuardBlockEntity g) s.guards.add(g);
            }
        }
        return s;
    }

    static Scan riftslate(ServerLevel level) {
        Scan s = PLACED.get("dungeon_riftslate");
        if (s == null) { s = place(level, "dungeon_riftslate", new BlockPos(20000, 0, 20000)); if (s != null) PLACED.put("dungeon_riftslate", s); }
        return s;
    }

    /** riftslate() or a clean FAIL (no NPE) when it can't be placed; also requires the blocks a test needs. */
    static Scan riftslateOrFail(ServerLevel level, String test, Block... need) {
        Scan s = riftslate(level);
        if (s == null) { result(test, false, "dungeon_riftslate could not be placed in the Echo Rift (4 passes x 144 chunks)"); return null; }
        for (Block b : need) if (s.of(b).isEmpty()) { result(test, false, "placed dungeon_riftslate has no " + b.getName().getString() + " (pieces=" + s.pieces.size() + ")"); return null; }
        return s;
    }

    /** Dungeons are placed in the Echo Rift: the test server's overworld is superflat (surface y=-60), where the projected
     *  start would sit below the world. */
    static ServerLevel echo(ServerLevel any) {
        ServerLevel e = any.getServer().getLevel(ModDimensions.ECHO_RIFT);
        return e != null ? e : any;
    }

    static BlockHitResult hit(BlockPos p) { return new BlockHitResult(Vec3.atCenterOf(p), Direction.NORTH, p, false); }

    static void killTagged(ServerLevel level, Entity e) {
        if (e instanceof LivingEntity le) { le.invulnerableTime = 0; le.kill(); } else if (e != null) e.discard();
    }

    // ------------------------------------------------------------------ tests
    private static void testLayouts(ServerLevel ow) {
        ServerLevel level = echo(ow);
        StringBuilder d = new StringBuilder();
        boolean[] ok = {true};
        for (int i = 0; i < STRUCTURES.length; i++) {
            final int idx = i;
            later(5 + i * 10, () -> {
                String id = STRUCTURES[idx];
                Scan s = place(level, id, new BlockPos(20000 + idx * 1600, 0, 20000));
                if (s == null) { ok[0] = false; d.append(id).append(": INVALID START; "); return; }
                PLACED.put(id, s);
                Set<String> roles = new TreeSet<>();
                s.guards.forEach(g -> roles.add(g.role()));
                String tier = s.of(ModDungeon.DANGER_SIGN.get()).isEmpty() ? "none"
                    : level.getBlockState(s.of(ModDungeon.DANGER_SIGN.get()).get(0)).getValue(DangerSignBlock.TIER).getSerializedName();
                boolean trial = s.hasPiece("trap_room") || s.hasPiece("puzzle_room");
                boolean good = s.pieces.size() >= 10 && s.hasPiece("entrance") && s.hasPiece("sanctum") && s.hasPiece("guard_room") && trial
                    && s.of(ModDungeon.KEY_DOOR.get()).size() == 2 && !s.of(ModDungeon.TREASURY_VAULT.get()).isEmpty()
                    && roles.equals(Set.of("boss", "key")) && !"none".equals(tier) && s.jigsaws == 0
                    && s.of(ModDungeon.RETURN_RIFT.get()).size() == 1 && s.of(ModDungeon.CHECKPOINT.get()).size() == 1;
                ok[0] &= good;
                BoundingBox bb = s.start.getBoundingBox();
                d.append(id).append(good ? " ok" : " BAD").append(" pieces=").append(s.pieces.size())
                    .append(" span=").append(bb.getXSpan()).append('x').append(bb.getYSpan()).append('x').append(bb.getZSpan())
                    .append(" trial=").append(s.hasPiece("trap_room") ? "trap" : s.hasPiece("puzzle_room") ? "puzzle" : "-")
                    .append(" doors=").append(s.of(ModDungeon.KEY_DOOR.get()).size()).append(" vaults=").append(s.of(ModDungeon.TREASURY_VAULT.get()).size())
                    .append(" guards=").append(roles).append(" tier=").append(tier).append(" lore=").append(s.of(ModDungeon.LORE_LECTERN.get()).size())
                    .append(" spawners=").append(s.of(Blocks.SPAWNER).size()).append(" chests=").append(s.of(Blocks.CHEST).size())
                    .append(" jigsawsLeft=").append(s.jigsaws);
                if (!good) d.append(" list=").append(s.pieces.stream().map(n -> n.replaceAll(".*/", "").replaceAll("\\].*", "")).toList());
                d.append("; ");
            });
        }
        later(10 + STRUCTURES.length * 10, () -> result("dungeon_layouts", ok[0], d.toString()));
    }

    /**
     * Key guard -> key -> door. The door ignores hands and redstone; the dropped key is cut for this ruin, so an
     * uncut key of another realm and a key cut for another ruin are both refused; the right key opens both halves and is
     * used up. the player who earned the key gets through bare-handed later (lost key), a stranger doesn't.
     */
    private static void testKeyDoor(ServerLevel ow) {
        ServerLevel level = echo(ow);
        Scan s = riftslateOrFail(level, "dungeon_key_door");
        if (s == null) return;
        String site = DungeonSites.key(level, s.start);
        DungeonSites.get(level).forget(site);
        FakePlayer earner = DungeonWardTests.survivor(level, "QFKeyEarner");
        FakePlayer stranger = DungeonWardTests.survivor(level, "QFKeyStranger");
        DungeonGuardBlockEntity keyGuard = s.guards.stream().filter(g -> "key".equals(g.role())).findFirst().orElse(null);
        if (keyGuard == null) { result("dungeon_key_door", false, "no key guard"); return; }
        BlockPos gp = keyGuard.getBlockPos();
        Entity elite = keyGuard.wake(level);
        boolean spent = !level.getBlockState(gp).is(ModDungeon.GUARD.get());
        boolean tagged = elite != null && elite.getTags().contains(DungeonGuardBlockEntity.TAG_KEY);
        String eliteName = elite == null ? "none" : elite.getType().getDescriptionId() + " hp=" + ((LivingEntity) elite).getMaxHealth();
        if (elite instanceof LivingEntity le) { le.invulnerableTime = 0; le.hurt(level.damageSources().playerAttack(earner), 100000f); if (le.isAlive()) le.kill(); }
        else killTagged(level, elite);
        later(3, () -> {
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(gp).inflate(8), ie -> ie.getItem().getItem() instanceof DungeonKeyItem);
            ItemStack dropped = drops.isEmpty() ? ItemStack.EMPTY : drops.get(0).getItem().copy();
            boolean keyDropped = !dropped.isEmpty() && ((DungeonKeyItem) dropped.getItem()).realm().equals("riftslate");
            boolean keyCut = site.equals(DungeonKeyItem.siteOf(dropped));
            drops.forEach(Entity::discard);
            BlockPos door = s.of(ModDungeon.KEY_DOOR.get()).stream().filter(p -> level.getBlockState(p).getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER).findFirst().orElseThrow();
            ((DoorBlock) level.getBlockState(door).getBlock()).setOpen(null, level, level.getBlockState(door), door, false);
            // bare hand (a stranger who never earned the key): stays shut
            stranger.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            level.getBlockState(door).useWithoutItem(level, stranger, hit(door));
            boolean handShut = !level.getBlockState(door).getValue(DoorBlock.OPEN);
            // redstone next to it: stays shut
            BlockPos front = door.relative(level.getBlockState(door).getValue(DoorBlock.FACING).getOpposite());
            BlockState frontWas = level.getBlockState(front);
            level.setBlock(front, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            boolean redstoneShut = !level.getBlockState(door).getValue(DoorBlock.OPEN);
            level.setBlock(front, frontWas, 3);
            // an uncut aurorite key, and a riftslate key cut for some other ruin, are refused (and kept)
            ItemStack wrongRealm = new ItemStack(ModDungeon.KEYS.get("aurorite").get());
            stranger.setItemInHand(InteractionHand.MAIN_HAND, wrongRealm);
            level.getBlockState(door).useItemOn(wrongRealm, level, stranger, InteractionHand.MAIN_HAND, hit(door));
            boolean realmRefused = !level.getBlockState(door).getValue(DoorBlock.OPEN) && !stranger.getMainHandItem().isEmpty();
            ItemStack otherRuin = DungeonKeyItem.forSite(new ItemStack(ModDungeon.KEYS.get("riftslate").get()), site + "0");
            stranger.setItemInHand(InteractionHand.MAIN_HAND, otherRuin);
            level.getBlockState(door).useItemOn(otherRuin, level, stranger, InteractionHand.MAIN_HAND, hit(door));
            boolean ruinRefused = !level.getBlockState(door).getValue(DoorBlock.OPEN) && !stranger.getMainHandItem().isEmpty();
            // the dropped key: opens both halves, key consumed
            ItemStack key = dropped.isEmpty() ? new ItemStack(ModDungeon.KEYS.get("riftslate").get()) : dropped;
            earner.setItemInHand(InteractionHand.MAIN_HAND, key);
            level.getBlockState(door).useItemOn(key, level, earner, InteractionHand.MAIN_HAND, hit(door));
            boolean open = level.getBlockState(door).getValue(DoorBlock.OPEN) && level.getBlockState(door.above()).getValue(DoorBlock.OPEN);
            boolean consumed = earner.getMainHandItem().isEmpty();
            // shut it again; the earner (key lost) gets through bare-handed
            ((DoorBlock) level.getBlockState(door).getBlock()).setOpen(null, level, level.getBlockState(door), door, false);
            level.getBlockState(door).useWithoutItem(level, stranger, hit(door));
            boolean strangerStillShut = !level.getBlockState(door).getValue(DoorBlock.OPEN);
            level.getBlockState(door).useWithoutItem(level, earner, hit(door));
            boolean earnerReopens = level.getBlockState(door).getValue(DoorBlock.OPEN);
            boolean ok = spent && tagged && keyDropped && keyCut && handShut && redstoneShut && realmRefused && ruinRefused && open && consumed
                && strangerStillShut && earnerReopens;
            result("dungeon_key_door", ok,
                "elite=" + eliteName + " guardSpent=" + spent + " tagged=" + tagged + " keyDropped=" + keyDropped + " keyCutForThisRuin=" + keyCut
                    + " bareHandShut=" + handShut + " redstoneShut=" + redstoneShut + " otherRealmKeyRefused=" + realmRefused
                    + " otherRuinKeyRefused=" + ruinRefused + " keyOpensBothHalves=" + open + " keyConsumed=" + consumed
                    + " lostKey:strangerShut=" + strangerStillShut + " earnerReopens=" + earnerReopens + " door=" + door.toShortString());
        });
    }

    /**
     * The vault stays sealed until its own warden dies, then pays once. the warden is killed far from the vault
     * (40 blocks out, past the old 24-block search) and the vault must still open; a re-sealed vault reopens on use
     * because the site remembers the kill.
     */
    private static void testTreasury(ServerLevel ow) {
        ServerLevel level = echo(ow);
        Scan s = riftslateOrFail(level, "dungeon_treasury", ModDungeon.TREASURY_VAULT.get());
        if (s == null) return;
        String site = DungeonSites.key(level, s.start);
        DungeonSites.get(level).forget(site);
        FakePlayer fp = tester(level);
        BlockPos vault = s.of(ModDungeon.TREASURY_VAULT.get()).get(0);
        level.setBlock(vault, level.getBlockState(vault).setValue(TreasuryVaultBlock.OPEN, false), 3);
        AABB around = new AABB(vault).inflate(3);
        level.getEntitiesOfClass(ItemEntity.class, around).forEach(Entity::discard);
        level.getBlockState(vault).useWithoutItem(level, fp, hit(vault));
        boolean sealedNoLoot = !level.getBlockState(vault).getValue(TreasuryVaultBlock.OPEN) && level.getEntitiesOfClass(ItemEntity.class, around).isEmpty();
        DungeonGuardBlockEntity boss = s.guards.stream().filter(g -> "boss".equals(g.role())).findFirst().orElse(null);
        if (boss == null) { result("dungeon_treasury", false, "no warden guard"); return; }
        Entity warden = boss.wake(level);
        boolean tagged = warden != null && warden.getTags().contains(DungeonGuardBlockEntity.TAG_BOSS);
        String wname = warden == null ? "none" : warden.getType().getDescriptionId() + " hp=" + ((LivingEntity) warden).getMaxHealth();
        // lure it far away before it dies
        if (warden != null) warden.teleportTo(vault.getX() + 40.5, vault.getY() + 1, vault.getZ() + 0.5);
        double far = warden == null ? 0 : Math.sqrt(warden.blockPosition().distSqr(vault));
        killTagged(level, warden);
        later(3, () -> {
            boolean opened = level.getBlockState(vault).getValue(TreasuryVaultBlock.OPEN);
            level.getBlockState(vault).useWithoutItem(level, fp, hit(vault));
            List<ItemEntity> loot = level.getEntitiesOfClass(ItemEntity.class, around);
            List<String> names = loot.stream().map(ie -> ie.getItem().getCount() + "x" + ie.getItem().getItem().toString()).toList();
            loot.forEach(Entity::discard);
            level.getBlockState(vault).useWithoutItem(level, fp, hit(vault));
            boolean once = level.getEntitiesOfClass(ItemEntity.class, around).isEmpty();
            // re-seal (as if the vault had been unloaded or rebuilt when the warden fell): use reopens it
            level.setBlock(vault, level.getBlockState(vault).setValue(TreasuryVaultBlock.OPEN, false), 3);
            level.getBlockState(vault).useWithoutItem(level, fp, hit(vault));
            boolean remembers = level.getBlockState(vault).getValue(TreasuryVaultBlock.OPEN);
            level.getEntitiesOfClass(ItemEntity.class, new AABB(vault).inflate(48)).forEach(Entity::discard);
            result("dungeon_treasury", sealedNoLoot && tagged && opened && !names.isEmpty() && once && remembers && far > 30,
                "sealedBeforeBoss=" + sealedNoLoot + " warden=" + wname + " tagged=" + tagged + " killedBlocksFromVault=" + (int) far
                    + " openedOnWardenDeath=" + opened + " loot=" + names + " paysOnce=" + once + " resealedVaultRemembersKill=" + remembers);
        });
    }

    private static void testShortcut(ServerLevel ow) {
        ServerLevel level = echo(ow);
        Scan s = riftslateOrFail(level, "dungeon_shortcut_checkpoint", ModDungeon.RETURN_RIFT.get(), Blocks.IRON_DOOR);
        if (s == null) return;
        FakePlayer fp = tester(level);
        BlockPos rift = s.of(ModDungeon.RETURN_RIFT.get()).get(0);
        // the antechamber iron door nearest the rift, and the lever that touches its lower half
        BlockPos door = s.of(Blocks.IRON_DOOR).stream().filter(p -> level.getBlockState(p).getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER)
            .min(Comparator.comparingDouble(p -> p.distSqr(rift))).orElse(null);
        BlockPos lever = door == null ? null : s.of(Blocks.LEVER).stream().filter(p -> p.distManhattan(door) <= 2).findFirst().orElse(null);
        if (door == null || lever == null) { result("dungeon_shortcut_checkpoint", false, "door=" + door + " lever=" + lever); return; }
        boolean shutBefore = !level.getBlockState(door).getValue(DoorBlock.OPEN);
        ((LeverBlock) Blocks.LEVER).pull(level.getBlockState(lever), level, lever, fp);
        later(4, () -> {
            boolean opened = level.getBlockState(door).getValue(DoorBlock.OPEN);
            BlockPos dest = ReturnRiftBlock.entranceOf(level, rift);
            StructurePiece entrance = s.start.getPieces().get(0);
            boolean inEntrance = dest != null && entrance.getBoundingBox().isInside(dest);
            boolean standable = dest != null && level.getBlockState(dest).isAir() && level.getBlockState(dest.above()).isAir()
                && level.getBlockState(dest.below()).isFaceSturdy(level, dest.below(), net.minecraft.core.Direction.UP);
            BlockPos cp = s.of(ModDungeon.CHECKPOINT.get()).get(0);
            level.getBlockState(cp).useWithoutItem(level, fp, hit(cp));
            boolean bound = cp.above().equals(fp.getRespawnPosition()) && fp.isRespawnForced();
            result("dungeon_shortcut_checkpoint", shutBefore && opened && inEntrance && standable && bound,
                "shortcutDoorShutBefore=" + shutBefore + " leverOpens=" + opened + " riftDest=" + (dest == null ? "none" : dest.toShortString())
                    + " insideEntrancePiece=" + inEntrance + " entranceBox=" + entrance.getBoundingBox() + " rot=" + entrance.getRotation() + " first=" + s.pieces.get(0) + " standable=" + standable + " checkpointBindsRespawn=" + bound);
        });
    }

    static BlockPos placeTemplate(ServerLevel level, String path, BlockPos at) {
        StructureTemplate t = level.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, path));
        for (int cx = (at.getX() - 16) >> 4; cx <= (at.getX() + 32) >> 4; cx++) for (int cz = (at.getZ() - 16) >> 4; cz <= (at.getZ() + 32) >> 4; cz++) level.setChunkForced(cx, cz, true);
        t.placeInWorld(level, at, at, new StructurePlaceSettings(), level.getRandom(), 2);
        return at;
    }

    private static void testTrap(ServerLevel level) {
        BlockPos o = placeTemplate(level, "dungeon/cinderite/trap_room", new BlockPos(26000, 80, 26000));
        cleanup(level, o, 30);
        BlockPos plate = o.offset(3, 1, 7);                     // a middle plate (not touching a dispenser)
        boolean armed = level.getBlockState(plate).is(ModDungeon.TRAP_PLATE.get()) && !level.getBlockState(plate).getValue(TrapPlateBlock.POWERED);
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        stand.moveTo(plate.getX() + 0.5, plate.getY() + 0.05, plate.getZ() + 0.5, 0, 0);
        level.addFreshEntity(stand);
        later(8, () -> {
            boolean triggered = level.getBlockState(plate).getValue(TrapPlateBlock.POWERED);
            int arrows = level.getEntitiesOfClass(AbstractArrow.class, new AABB(o).expandTowards(7, 6, 17).inflate(2)).size();
            stand.discard();
            cleanup(level, o.offset(3, 2, 8), 14);
            result("dungeon_trap", armed && triggered && arrows >= 1, "armedBefore=" + armed + " plateTriggered=" + triggered + " arrowsFired=" + arrows);
        });
    }

    private static void testPuzzle(ServerLevel level) {
        BlockPos o = placeTemplate(level, "dungeon/prismite/puzzle_room", new BlockPos(26100, 80, 26000));
        cleanup(level, o, 20);
        FakePlayer fp = tester(level);
        BlockPos door = o.offset(5, 1, 9);
        BlockPos trueLever = level.getBlockState(o.offset(4, 1, 8)).is(Blocks.LEVER) ? o.offset(4, 1, 8) : o.offset(6, 1, 8);
        List<BlockPos> decoys = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(o, o.offset(10, 6, 10)))
            if (level.getBlockState(p).is(Blocks.LEVER) && !p.equals(trueLever)) decoys.add(p.immutable());
        for (BlockPos p : decoys) ((LeverBlock) Blocks.LEVER).pull(level.getBlockState(p), level, p, fp);
        later(6, () -> {
            boolean shutAfterDecoys = !level.getBlockState(door).getValue(DoorBlock.OPEN);
            int arrows = level.getEntitiesOfClass(AbstractArrow.class, new AABB(o).expandTowards(11, 7, 11).inflate(2)).size();
            ((LeverBlock) Blocks.LEVER).pull(level.getBlockState(trueLever), level, trueLever, fp);
            later(4, () -> {
                boolean opened = level.getBlockState(door).getValue(DoorBlock.OPEN);
                boolean sigil = !level.getBlockState(trueLever.above().south()).is(level.getBlockState(o.offset(3, 2, 9)).getBlock());
                cleanup(level, o.offset(5, 3, 5), 12);
                result("dungeon_puzzle", decoys.size() == 4 && shutAfterDecoys && arrows >= 1 && opened && sigil,
                    "decoys=" + decoys.size() + " doorShutAfterDecoys=" + shutAfterDecoys + " decoyArrows=" + arrows + " trueLeverOpens=" + opened
                        + " sigilMarksTrueLever=" + sigil);
            });
        });
    }

    /** /locate-style search in fresh chunks of each dimension, then generate one overworld dungeon's chunks and check it is real. */
    private static void testNatural(ServerLevel level) {
        var server = level.getServer();
        StringBuilder d = new StringBuilder();
        Map<String, BlockPos> found = new LinkedHashMap<>();
        record Probe(ResourceKey<Level> dim, String id) {}
        List<Probe> probes = new ArrayList<>();
        for (int i = 0; i < 8; i++) probes.add(new Probe(ModDimensions.ECHO_RIFT, STRUCTURES[i]));
        probes.add(new Probe(Level.OVERWORLD, "dungeon_overworld_riftslate"));
        probes.add(new Probe(Level.OVERWORLD, "dungeon_overworld_aurorite"));
        probes.add(new Probe(Level.NETHER, "dungeon_nether_cinderite"));
        probes.add(new Probe(Level.END, "dungeon_end_voidshale"));
        BlockPos origin = new BlockPos(100000 + (int) ((System.currentTimeMillis() / 1000) % 1000) * 400, 64, -100000);   // fresh area every run
        // one probe every 10 ticks: each search generates structure starts and can take seconds
        for (int i = 0; i < probes.size(); i++) {
            Probe p = probes.get(i);
            later(1 + i * 10, () -> {
                ServerLevel l = server.getLevel(p.dim());
                if (l == null) { d.append(p.id()).append(": no dimension; "); return; }
                BlockPos from = p.dim() == Level.END ? new BlockPos(3000, 64, 3000) : origin;
                var res = l.getChunkSource().getGenerator().findNearestMapStructure(l, HolderSet.direct(structure(l, p.id())), from, 48, false);
                if (res != null) {
                    found.put(p.id(), res.getFirst());
                    d.append(p.id()).append(" @").append(res.getFirst().toShortString()).append(" (").append((int) Math.sqrt(res.getFirst().distSqr(from))).append(" blocks); ");
                } else d.append(p.id()).append(" not within 48 chunks; ");
            });
        }
        later(10 + probes.size() * 10, () -> naturalGenerate(server, d, found));
    }

    private static void naturalGenerate(net.minecraft.server.MinecraftServer server, StringBuilder d, Map<String, BlockPos> found) {
        // spacing: nearest two overworld riftslate dungeons
        // the test server's overworld is superflat, whose generator places no mod structures; overworld variants are
        // checked with /locate in the real client world instead (see WORLD.md)
        boolean vanillaAll = found.keySet().containsAll(List.of("dungeon_nether_cinderite", "dungeon_end_voidshale"));
        long echo = found.keySet().stream().filter(k -> Arrays.asList(STRUCTURES).subList(0, 8).contains(k)).count();
        // real generation check for the overworld find: load its chunks over a few ticks and count kit blocks
        String genId = found.containsKey("dungeon_chronoslate") ? "dungeon_chronoslate" : found.keySet().stream().filter(k -> Arrays.asList(STRUCTURES).subList(0, 8).contains(k)).findFirst().orElse("dungeon_riftslate");
        BlockPos ow = found.get(genId);
        ServerLevel lvl = server.getLevel(ModDimensions.ECHO_RIFT);
        if (ow == null || lvl == null) { result("dungeon_natural", false, d.toString()); return; }
        ChunkAccess startChunk = lvl.getChunk(ow.getX() >> 4, ow.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS);
        StructureStart st = startChunk.getStartForStructure(structure(lvl, genId).value());
        if (st == null || !st.isValid()) { result("dungeon_natural", false, d + " | no start in located chunk"); return; }
        BoundingBox bb = st.getBoundingBox();
        List<ChunkPos> chunks = new ArrayList<>();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) chunks.add(new ChunkPos(cx, cz));
        for (int i = 0; i < chunks.size(); i++) {
            ChunkPos c = chunks.get(i);
            later(1 + i / 2, () -> { lvl.setChunkForced(c.x, c.z, true); lvl.getChunk(c.x, c.z); });
        }
        later(6 + chunks.size() / 2, () -> {
            Scan s = scan(lvl, "natural", st);
            BlockPos rift = s.of(ModDungeon.RETURN_RIFT.get()).isEmpty() ? null : s.of(ModDungeon.RETURN_RIFT.get()).get(0);
            boolean riftWorks = rift != null && ReturnRiftBlock.entranceOf(lvl, rift) != null;
            boolean real = s.of(ModDungeon.KEY_DOOR.get()).size() == 2 && s.guards.size() == 2 && !s.of(ModDungeon.TREASURY_VAULT.get()).isEmpty() && s.jigsaws == 0;
            for (ChunkPos c : chunks) lvl.setChunkForced(c.x, c.z, false);
            result("dungeon_natural", vanillaAll && echo >= 6 && real && riftWorks,
                "echoTypesFound=" + echo + "/8 vanillaVariantsFound=" + vanillaAll + " | " + d + "| generated " + genId + " find: pieces=" + s.pieces.size()
                    + " chunks=" + chunks.size() + " doors=" + s.of(ModDungeon.KEY_DOOR.get()).size() + " guards=" + s.guards.size()
                    + " vaults=" + s.of(ModDungeon.TREASURY_VAULT.get()).size() + " jigsawsLeft=" + s.jigsaws + " returnRiftResolves=" + riftWorks + " bbox=" + bb
                    + (real ? "" : " list=" + s.pieces.stream().map(n -> n.replaceAll(".*/", "").replaceAll("\\].*", "")).toList()));
        });
    }

    private static void testArenaTemplates(ServerLevel level) {
        StringBuilder d = new StringBuilder();
        boolean ok = true;
        for (ArenaType t : ArenaType.values()) {
            var tpl = level.getStructureManager().get(t.getTemplateId());
            int r = t.templateRadius();
            boolean present = tpl.isPresent();
            boolean size = present && tpl.get().getSize().getX() == 2 * r + 1 && tpl.get().getSize().getZ() == 2 * r + 1;
            boolean marker = present && !tpl.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.STRUCTURE_BLOCK).isEmpty()
                && tpl.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.STRUCTURE_BLOCK).stream()
                    .anyMatch(b -> b.pos().equals(new BlockPos(r, 9, r)) && b.nbt() != null && "boss_spawn".equals(b.nbt().getString("metadata")));
            boolean vault = present && !tpl.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), ModDungeon.TREASURY_VAULT.get()).isEmpty();
            ok &= present && size && marker && vault;
            d.append(t.name()).append(present ? " size=" + tpl.get().getSize().toShortString() : " MISSING").append(" marker=").append(marker).append(" vault=").append(vault).append("; ");
        }
        // natural: locate the nearest Quantum Guardian arena in the Echo Rift, generate it, check it registered without spawning the boss
        ServerLevel echo = level.getServer().getLevel(ModDimensions.ECHO_RIFT);
        if (echo == null) { result("arena_templates", false, d + " no echo dimension"); return; }
        var res = echo.getChunkSource().getGenerator().findNearestMapStructure(echo,
            HolderSet.direct(echo.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE,
                ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "quantum_guardian_arena")))), new BlockPos(-40000, 64, 40000), 100, false);
        if (res == null) { result("arena_templates", false, d + " no natural quantum_guardian_arena within 100 chunks"); return; }
        BlockPos c = res.getFirst();
        int y = 120;
        final boolean okT = ok;
        List<ChunkPos> chunks = new ArrayList<>();
        for (int cx = -2; cx <= 2; cx++) for (int cz = -2; cz <= 2; cz++) chunks.add(new ChunkPos((c.getX() >> 4) + cx, (c.getZ() >> 4) + cz));
        for (int i = 0; i < chunks.size(); i++) { ChunkPos cp = chunks.get(i); later(1 + i / 5, () -> { echo.setChunkForced(cp.x, cp.z, true); echo.getChunk(cp.x, cp.z); }); }
        later(10, () -> {
            // arenas sit on the terrain now, so take the registered arena at this x/z, whatever its height
            var arena = ArenaMechanics.getActiveArenas().stream().filter(a -> Math.abs(a.center.getX() - c.getX()) <= 16 && Math.abs(a.center.getZ() - c.getZ()) <= 16)
                .findFirst().orElse(null);
            BlockPos center = arena != null ? arena.center : new BlockPos(c.getX(), y, c.getZ());
            BlockState floor = echo.getBlockState(center.below());
            long bosses = echo.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(center).inflate(40),
                m -> m instanceof com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity).size();
            for (ChunkPos cp : chunks) echo.setChunkForced(cp.x, cp.z, false);
            boolean natural = !floor.isAir() && arena != null && bosses == 0;
            result("arena_templates", okT && natural, d + "| natural quantum_guardian_arena @" + center.toShortString() + " floor=" + floor.getBlock().getDescriptionId()
                + " registered=" + (arena != null) + (arena != null ? " radius=" + arena.radius() : "") + " bossSpawnedAtWorldgen=" + (bosses > 0));
        });
    }

    /** Runs the real vanilla /locate command (as an op at a fresh spot) for an Echo Rift dungeon, the nether variant and
     *  the end variant, and checks it reports a position. Spread over ticks: each locate is a heavy search. */
    static void testLocateCommand(ServerLevel level) {
        var server = level.getServer();
        String[][] probes = {{"quantumfalcon:echo_rift", "quantumfalcon:dungeon_riftslate"}, {"minecraft:the_nether", "quantumfalcon:dungeon_nether_cinderite"},
            {"minecraft:the_end", "quantumfalcon:dungeon_end_voidshale"}, {"quantumfalcon:echo_rift", "#quantumfalcon:dungeons"}};
        java.util.List<String> out = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        int[] found = {0};
        int base = 30000 + (int) (server.overworld().getGameTime() % 50) * 4000;
        for (int i = 0; i < probes.length; i++) {
            final String[] pr = probes[i];
            QFTestCommands.later(10 + i * 15, () -> {
                StringBuilder msg = new StringBuilder();
                net.minecraft.commands.CommandSource sink = new net.minecraft.commands.CommandSource() {
                    @Override public void sendSystemMessage(net.minecraft.network.chat.Component c) { msg.append(c.getString()).append(' '); }
                    @Override public boolean acceptsSuccess() { return true; }
                    @Override public boolean acceptsFailure() { return true; }
                    @Override public boolean shouldInformAdmins() { return false; }
                };
                var src = new net.minecraft.commands.CommandSourceStack(sink, net.minecraft.world.phys.Vec3.ZERO, net.minecraft.world.phys.Vec2.ZERO, server.overworld(), 4,
                        "qftest", net.minecraft.network.chat.Component.literal("qftest"), server, null);
                String cmd = "execute in " + pr[0] + " positioned " + base + " 64 " + base + " run locate structure " + pr[1];
                long t0 = System.nanoTime();
                server.getCommands().performPrefixedCommand(src, cmd);
                msg.append("(").append((System.nanoTime() - t0) / 1_000_000).append(" ms)");
                com.matt.quantumfalcon.QuantumFalcon.LOGGER.info("[locate] {} -> {}", cmd, msg);
                boolean ok = msg.toString().contains("is at");
                if (ok) found[0]++;
                out.add(pr[1] + " in " + pr[0] + ": " + msg.toString().trim());
            });
        }
        QFTestCommands.later(10 + probes.length * 15 + 5, () ->
            QFTestCommands.result("dungeon_locate_command", found[0] == probes.length, found[0] + "/" + probes.length + " | " + String.join(" | ", out)));
    }
}
