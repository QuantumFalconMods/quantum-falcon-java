package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.arena.BossSummoner;
import com.matt.quantumfalcon.arena.fight.ArenaInteraction;
import com.matt.quantumfalcon.arena.fight.ModArenaBlocks;
import com.matt.quantumfalcon.registry.ModEntities;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.story.BossCredit;
import com.matt.quantumfalcon.story.PlayerStoryData;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** Tests for the 2026-11-13 cheese fairness sweep review. */
final class CheeseTests {
    private CheeseTests() {}

    static void register(LinkedHashMap<String, Consumer<ServerLevel>> tests) {
        tests.put("cx1_arena_kill_outside", CheeseTests::cx1);
        tests.put("cx2_arena_restart_shield", CheeseTests::cx2);
        tests.put("cx7_cx18_overseer_shared_loot", CheeseTests::cx7);
        tests.put("cx4_cx14_cx15_boss_flags", CheeseTests::flags);
        tests.put("cx6_death_persistence", CheeseTests::cx6);
        tests.put("cx10_cx11_stars", CheeseTests::stars);
        tests.put("cx12_no_afk_leveling", CheeseTests::cx12);
        tests.put("cx13_falcon_feathers", CheeseTests::cx13);
        tests.put("cx17_cx19_relics", CheeseTests::relics);
    }

    /** A FakePlayer that has died and respawned: a new player object restored from the old one, as PlayerList.respawn does. */
    static FakePlayer respawned(FakePlayer old) {
        FakePlayer reborn = new FakePlayer((ServerLevel) old.level(), old.getGameProfile());
        reborn.restoreFrom(old, false);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(reborn, old, true));
        return reborn;
    }

    static FakePlayer fighter(ServerLevel level, String name) {
        FakePlayer fp = AuditTests.freshPlayer(level, name);
        BossCredit.TEST_PLAYERS.put(fp.getUUID(), fp);
        fp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return fp;
    }

    static int count(FakePlayer fp, Item it) { return fp.getInventory().countItem(it); }

    static void kill(ServerLevel level, LivingEntity m, net.minecraft.world.damagesource.DamageSource src) {
        for (int i = 0; i < 40 && m.isAlive() && !m.isDeadOrDying(); i++) {
            m.invulnerableTime = 0;
            if (m instanceof com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity sv) sv.clearInvulnerability();
            m.setHealth(Math.min(m.getHealth(), 2f));
            m.hurt(src, 100f);
        }
    }

    static List<ItemEntity> drops(ServerLevel level, Vec3i at, int r) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(new BlockPos(at.getX(), at.getY(), at.getZ())).inflate(r));
    }
    private record Vec3i(int getX, int getY, int getZ) {}
    static Vec3i v(Entity e) { return new Vec3i(e.getBlockX(), e.getBlockY(), e.getBlockZ()); }

    static void unforceSite(ServerLevel level, BlockPos c) {
        for (int cx = -6; cx <= 6; cx++) for (int cz = -6; cz <= 6; cz++) level.setChunkForced((c.getX() >> 4) + cx, (c.getZ() >> 4) + cz, false);
    }

    static void clearItems(ServerLevel level, BlockPos c, int r) {
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(r))) ie.discard();
    }

    // ------------------------------------------------------------------
    /** A Warden killed 200 blocks outside its arena still retires that arena; a repeat kill elsewhere re-gifts only a lost relic. */
    private static void cx1(ServerLevel level) {
        BlockPos c = BossFightTests.site(level, 12000, 12000 + DungeonTests.RUN_OFF);
        BlockPos far = new BlockPos(c.getX() + 200, c.getY(), c.getZ());
        WorldDropsTests.force(level, far, true);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, ArenaType.VOID_HORROR);
        if (boss == null) { result("cx1_arena_kill_outside", false, "summon failed"); return; }
        FakePlayer fp = fighter(level, "QFCarter");
        PlayerStoryData.get(fp).markBossDefeated("QUANTUM_OVERSEER");
        later(40, () -> WorldDropsTests.whenTicking(level, far, 0, () -> {
            var arena = ArenaMechanics.arenaAt(c);
            boolean owned = arena != null && boss.getUUID().equals(arena.bossId);
            fp.setPos(boss.getX() + 2, boss.getY(), boss.getZ());
            boss.invulnerableTime = 0; boss.hurt(level.damageSources().playerAttack(fp), 4f);   // contributes
            boss.setPos(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);                       // hauled out (cart / pearl / portal)
            fp.setPos(far.getX() + 2.5, far.getY(), far.getZ() + 0.5);
            kill(level, boss, level.damageSources().playerAttack(fp));
            later(100, () -> {   // the Warden's death animation runs 84 ticks before loot and credit
                boolean retired = ArenaMechanics.arenaAt(c) == null;
                int frag = count(fp, ModItems.WARDENS_FRAGMENT.get()), art = count(fp, ModItems.ARTIFACT_FRAGMENT.get());
                // a second Warden at another arena, same player still holding the relic: no second relic, no more fragments
                BlockPos c2 = BossFightTests.site(level, 12600, 12000 + DungeonTests.RUN_OFF);
                cleanup(level, c2, 120);
                Mob boss2 = BossSummoner.buildAndSummon(level, c2, ArenaType.VOID_HORROR);
                later(40, () -> { if (boss2 != null) { fp.setPos(boss2.getX() + 2, boss2.getY(), boss2.getZ()); kill(level, boss2, level.damageSources().playerAttack(fp)); } });
                later(140, () -> {
                    int frag2 = -1, art2 = -1;
                    if (boss2 != null) { frag2 = count(fp, ModItems.WARDENS_FRAGMENT.get()); art2 = count(fp, ModItems.ARTIFACT_FRAGMENT.get()); }
                    // and after losing it, a repeat kill gives it back
                    fp.getInventory().clearContent();
                    BlockPos c3 = c2;   // same site, rebuilt (keeps the forced-chunk footprint small)
                    Mob boss3 = BossSummoner.buildAndSummon(level, c3, ArenaType.VOID_HORROR);
                    final int f2 = frag2, a2 = art2;
                    later(40, () -> { if (boss3 != null) { fp.setPos(boss3.getX() + 2, boss3.getY(), boss3.getZ()); kill(level, boss3, level.damageSources().playerAttack(fp)); } });
                    later(140, () -> {
                        int frag3 = boss3 == null ? -1 : count(fp, ModItems.WARDENS_FRAGMENT.get());
                        boolean ok = owned && retired && frag == 1 && art == 2 && f2 == 1 && a2 == 2 && frag3 == 1;
                        WorldDropsTests.force(level, far, false);
                        unforceSite(level, c); unforceSite(level, c2);
                        BossCredit.TEST_PLAYERS.remove(fp.getUUID());
                        result("cx1_arena_kill_outside", ok, "arena owns boss=" + owned + " | killed 200 out -> arena retired=" + retired
                            + " relic=" + frag + " artifact=" + art + " | repeat kill holding relic -> relic=" + f2 + " artifact=" + a2
                            + " | repeat kill after losing it -> relic=" + frag3);
                    });
                });
            });
        }));
    }

    // ------------------------------------------------------------------
    /**
     * A save/reload mid-fight (boss written to NBT and loaded back, the unsaved fight map cleared) rebuilds the fight from
     * the world: pylons stay as they were (broken ones stay broken), the shield works again, no doubled pylons. Also the
     * seal: full wall over grass, a roof, and no pearl out while the fight is on.
     */
    private static void cx2(ServerLevel level) {
        BlockPos c = BossFightTests.site(level, 12000, 12800 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        Mob boss0 = BossSummoner.buildAndSummon(level, c, ArenaType.VOID_HORROR);
        if (boss0 == null) { result("cx2_arena_restart_shield", false, "summon failed"); return; }
        StringBuilder d = new StringBuilder();
        later(40, () -> {
            var arena = ArenaMechanics.arenaAt(c);
            int r = arena.radius() + 1;
            BlockPos ringFloor = ArenaInteraction.sealFloorAt(level, c.getX() + r, c.getY(), c.getZ());
            if (ringFloor == null) {   // the ring is over open air here: give it a floor block to grow grass on
                ringFloor = new BlockPos(c.getX() + r, c.getY() - 1, c.getZ());
                level.setBlock(ringFloor, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
            }
            BlockPos grass = ringFloor.above();
            if (grass != null) level.setBlock(grass, net.minecraft.world.level.block.Blocks.SHORT_GRASS.defaultBlockState(), 2);
            boss0.getPersistentData().putBoolean("qf_intro", true);
            ArenaInteraction.Fight f = ArenaInteraction.startFight(level, boss0); ArenaInteraction.finishSeal(level, f);   // seals go up over ~9 ticks; this test inspects them at once
            int pylons0 = ArenaInteraction.chargedPylons(level, f);
            boolean grassSealed = grass != null && level.getBlockState(grass).is(ModArenaBlocks.SEAL.get());
            boolean tall = level.getBlockState(ringFloor.above(10)).is(ModArenaBlocks.SEAL.get());
            boolean roof = level.getBlockState(c.above(ArenaInteraction.ROOF)).is(ModArenaBlocks.SEAL.get());
            boolean pearlBlocked = com.matt.quantumfalcon.entity.boss.BossRules.leavesActiveFight(level, net.minecraft.world.phys.Vec3.atCenterOf(c.above()),
                net.minecraft.world.phys.Vec3.atCenterOf(c.offset(arena.radius() + 40, 0, 0)));
            boolean pearlInsideOk = !com.matt.quantumfalcon.entity.boss.BossRules.leavesActiveFight(level, net.minecraft.world.phys.Vec3.atCenterOf(c.above()),
                net.minecraft.world.phys.Vec3.atCenterOf(c.offset(3, 1, 3)));
            // the real event: a pearl thrown from the centre to outside is cancelled
            FakePlayer thrower = fighter(level, "QFPearler");
            thrower.setPos(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5);
            var pearl = EntityType.ENDER_PEARL.create(level);
            var ev = new net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl(thrower, c.getX() + arena.radius() + 40, c.getY(), c.getZ(), pearl, 5f,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(c), net.minecraft.core.Direction.UP, c, false));
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(ev);
            boolean pearlEvent = ev.isCanceled();
            d.append("seal over grass=").append(grassSealed).append(" wall at +10=").append(tall).append(" roof=").append(roof)
             .append(" pearl out blocked=").append(pearlBlocked && pearlEvent).append(" pearl inside ok=").append(pearlInsideOk);
            // break 2 pylons, then "restart": reload the boss from NBT and drop the fight map entry
            FakePlayer fp = fighter(level, "QFQuitter");
            int broken = 0;
            for (BlockPos p : new ArrayList<>(f.pylons)) { if (broken == 2) break; fp.moveTo(p.getX() + 1.5, p.getY(), p.getZ() + 0.5); if (fp.gameMode.destroyBlock(p)) broken++; }
            final int brokenF = broken;
            CompoundTag saved = new CompoundTag(); boss0.save(saved);
            boss0.discard();
            ArenaInteraction.FIGHTS.remove(c);
            Entity loaded = EntityType.loadEntityRecursive(saved, level, e -> e);
            level.addFreshEntity(loaded);
            Mob boss = (Mob) loaded;
            later(60, () -> {
                ArenaInteraction.Fight g = ArenaInteraction.fightFor(boss);
                int charged = g == null ? -1 : ArenaInteraction.chargedPylons(level, g);
                long pylonBlocks = BlockPos.betweenClosedStream(c.offset(-r - 2, -16, -r - 2), c.offset(r + 2, 24, r + 2))
                    .filter(p -> level.getBlockState(p).is(ModArenaBlocks.PYLON.get())).count();
                float h0 = boss.getHealth(); boss.invulnerableTime = 0; boss.hurt(level.damageSources().magic(), 10f);
                float shielded = h0 - boss.getHealth();
                int seals = g == null ? 0 : BossFightTests.sealCount(level, g);
                d.append(" | pylons ").append(pylons0).append(", broke ").append(brokenF).append(" | after reload: fight back=").append(g != null)
                 .append(" charged=").append(charged).append(" pylon blocks=").append(pylonBlocks).append(" seal blocks tracked=").append(seals)
                 .append(" dmg10 shielded=").append(String.format("%.1f", shielded));
                // finish it: the seal (wall + roof) must all come down
                kill(level, boss, level.damageSources().playerAttack(fp));
                later(100, () -> {
                    int left = g == null ? -1 : BossFightTests.sealCount(level, g);
                    d.append(" | killed -> seal left=").append(left);
                    BossCredit.TEST_PLAYERS.remove(fp.getUUID()); BossCredit.TEST_PLAYERS.remove(thrower.getUUID());
                    boolean ok = grassSealed && tall && roof && pearlBlocked && pearlEvent && pearlInsideOk && pylons0 == 4 && brokenF == 2
                        && g != null && charged == 2 && pylonBlocks == 2 && shielded > 0 && shielded < 9f && seals > 100 && left == 0;
                    unforceSite(level, c);
                    result("cx2_arena_restart_shield", ok, d.toString());
                });
            });
        });
    }

    // ------------------------------------------------------------------
    /**
     * An Overseer with nobody credited (void / suffocation) drops the reduced table: no Eye, no Nether Star on the floor.
     * A party of a veteran and a newcomer gets the reduced shared table too, while the newcomer still gets the Eye and
     * first-kill credit personally, and an ally who hit once and stood outside the arena is credited.
     */
    private static void cx7(ServerLevel level) {
        BlockPos c = BossFightTests.site(level, 12000, 13600 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, ArenaType.QUANTUM_GUARDIAN);
        if (boss == null) { result("cx7_cx18_overseer_shared_loot", false, "summon failed"); return; }
        Item eye = ModItems.OVERSEERS_EYE.get(), star = net.minecraft.world.item.Items.NETHER_STAR;
        later(40, () -> {
            clearItems(level, c, 140);
            var at = v(boss);
            kill(level, boss, level.damageSources().fellOutOfWorld());
            later(10, () -> {
                long eyes = drops(level, at, 40).stream().filter(ie -> ie.getItem().is(eye) || ie.getItem().is(star)).count();
                long any = drops(level, at, 40).size();
                boolean retired = ArenaMechanics.arenaAt(c) == null;
                // party run
                BlockPos c2 = BossFightTests.site(level, 12600, 13600 + DungeonTests.RUN_OFF);
                cleanup(level, c2, 120);
                Mob b2 = BossSummoner.buildAndSummon(level, c2, ArenaType.QUANTUM_GUARDIAN);
                later(40, () -> {
                    if (b2 == null) { result("cx7_cx18_overseer_shared_loot", false, "second summon failed"); return; }
                    FakePlayer vet = fighter(level, "QFVeteran"), rookie = fighter(level, "QFRookie"), ally = fighter(level, "QFAllyOutside");
                    PlayerStoryData.get(vet).markBossDefeated("QUANTUM_OVERSEER");
                    vet.getInventory().add(new net.minecraft.world.item.ItemStack(eye));
                    for (FakePlayer p : List.of(vet, rookie, ally)) { p.setPos(b2.getX() + 2, b2.getY(), b2.getZ()); b2.invulnerableTime = 0; b2.hurt(level.damageSources().playerAttack(p), 3f); }
                    ally.setPos(c2.getX() + 400, c2.getY(), c2.getZ());   // stepped out before the kill
                    clearItems(level, c2, 140);
                    var at2 = v(b2);
                    kill(level, b2, level.damageSources().playerAttack(rookie));
                    later(10, () -> {
                        long floor2 = drops(level, at2, 40).stream().filter(ie -> ie.getItem().is(eye) || ie.getItem().is(star)).count();
                        int vetEyes = count(vet, eye), rookieEyes = count(rookie, eye), allyEyes = count(ally, eye);
                        boolean rookieCredit = PlayerStoryData.get(rookie).hasDefeatedBoss("QUANTUM_OVERSEER");
                        boolean allyCredit = PlayerStoryData.get(ally).hasDefeatedBoss("QUANTUM_OVERSEER");
                        for (FakePlayer p : List.of(vet, rookie, ally)) BossCredit.TEST_PLAYERS.remove(p.getUUID());
                        unforceSite(level, c); unforceSite(level, c2);
                        boolean ok = eyes == 0 && retired && floor2 == 0 && vetEyes == 1 && rookieEyes == 1 && allyEyes == 1 && rookieCredit && allyCredit;
                        result("cx7_cx18_overseer_shared_loot", ok, "void kill, nobody credited -> eye/star on floor=" + eyes + " (items " + any + "), arena retired=" + retired
                            + " | veteran+rookie party -> eye/star on floor=" + floor2 + ", veteran eyes=" + vetEyes + " rookie eyes=" + rookieEyes + " rookie credited=" + rookieCredit
                            + " | ally who hit once then left -> credited=" + allyCredit + " eyes=" + allyEyes);
                    });
                });
            });
        });
    }

    // ------------------------------------------------------------------
    private static void flags(ServerLevel level) {
        BlockPos p = pad(level, 3000, 3480, net.minecraft.world.level.block.Blocks.STONE);
        cleanup(level, p, 12);
        List<EntityType<? extends Mob>> types = new ArrayList<>(List.of(ModEntities.QUANTUM_OVERSEER.get(), ModEntities.RIFT_WARDEN.get(), ModEntities.ENTANGLED_TWINS.get(),
            ModEntities.SINGULARITY_SOVEREIGN.get(), ModEntities.VOID_LORD.get(), ModEntities.SHA_SHA.get(), ModEntities.ECLIPSE_PHANTOM.get()));
        types.add(EntityType.ZOMBIE);   // control: proves the lane and both cart paths really mount an ordinary mob
        // Real minecart cheese, one rail lane per mob, both vanilla paths: (A) a cart rolling into the mob, (B) the mob walking into a parked cart
        List<BlockPos> lanes = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            BlockPos lane = new BlockPos(p.getX(), p.getY(), p.getZ() + 20 + i * 8);
            lanes.add(lane);
            level.setChunkForced(lane.getX() >> 4, lane.getZ() >> 4, true);
            for (int dx = -8; dx <= 8; dx++) for (int dz = -3; dz <= 3; dz++) {
                level.setBlock(lane.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                for (int dy = 0; dy < 6; dy++) level.setBlock(lane.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
            for (int dx = -8; dx <= 8; dx++) level.setBlock(lane.offset(dx, 0, 0), net.minecraft.world.level.block.Blocks.RAIL.defaultBlockState(), 3);
        }
        WorldDropsTests.whenTicking(level, lanes.get(lanes.size() - 1), 0, () -> WorldDropsTests.whenTicking(level, lanes.get(0), 0, () -> {
            List<Mob> mobs = new ArrayList<>();
            List<net.minecraft.world.entity.vehicle.AbstractMinecart> cartsA = new ArrayList<>(), cartsB = new ArrayList<>();
            for (int i = 0; i < types.size(); i++) {
                BlockPos lane = lanes.get(i);
                Mob b = types.get(i).create(level);
                b.moveTo(lane.getX() + 3.5, lane.getY(), lane.getZ() + 0.5);   // AI on: a NoAI mob isn't pushable, so carts never take it
                level.addFreshEntity(b);
                mobs.add(b);
                var cart = EntityType.MINECART.create(level);   // (A) 0.6 blocks short, rolling at it (empty-cart drag stops pickups below 0.1 b/t)
                cart.moveTo(b.getX() - b.getBbWidth() / 2 - 0.49 - 0.6, lane.getY(), lane.getZ() + 0.5);
                cart.setDeltaMovement(0.4, 0, 0);
                level.addFreshEntity(cart);
                cartsA.add(cart);
            }
            boolean[] rodeA = new boolean[types.size()], rodeB = new boolean[types.size()];
            later(12, () -> {
                for (int i = 0; i < mobs.size(); i++) {
                    Mob b = mobs.get(i);
                    rodeA[i] = b.isPassenger();
                    b.stopRiding(); cartsA.get(i).discard();
                    b.moveTo(lanes.get(i).getX() + 3.5, lanes.get(i).getY(), lanes.get(i).getZ() + 0.5);
                    var cart = EntityType.MINECART.create(level);   // (B) parked right next to the mob, which walks into it
                    cart.moveTo(b.getX() - b.getBbWidth() / 2 - 0.49 - 0.1, lanes.get(i).getY(), lanes.get(i).getZ() + 0.5);
                    level.addFreshEntity(cart);
                    cartsB.add(cart);
                }
                for (int t = 1; t <= 12; t++) later(t, () -> {
                    for (int i = 0; i < mobs.size(); i++) { Mob b = mobs.get(i); if (!b.isPassenger()) b.setDeltaMovement(-0.25, b.getDeltaMovement().y, 0); }
                });
                later(14, () -> {
                    StringBuilder d = new StringBuilder(); boolean ok = true; boolean control = false;
                    for (int i = 0; i < mobs.size(); i++) {
                        Mob b = mobs.get(i); var cart = cartsB.get(i);
                        rodeB[i] = b.isPassenger();
                        String name = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(types.get(i)).getPath();
                        if (types.get(i) == EntityType.ZOMBIE) {
                            control = rodeA[i] || rodeB[i];
                            d.insert(0, "control zombie: rolling cart took it=" + rodeA[i] + ", walked into parked cart=" + rodeB[i] + " | ");
                            b.discard(); cart.discard(); continue;
                        }
                        b.stopRiding();
                        boolean forced = b.startRiding(cart, false) || b.startRiding(cart, true);   // and a forced mount is refused too
                        if (b instanceof com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity sv) sv.clearInvulnerability();
                        float h = b.getHealth();
                        b.invulnerableTime = 0; b.hurt(level.damageSources().inWall(), 5f);
                        b.invulnerableTime = 0; b.hurt(level.damageSources().drown(), 5f);
                        boolean envDmg = b.getHealth() < h;
                        boolean portal = b.canUsePortal(false) || b.canUsePortal(true);
                        if (rodeA[i] || rodeB[i] || forced || envDmg || portal) ok = false;
                        d.append(name).append(": rolling cart=").append(rodeA[i]).append(" parked cart=").append(rodeB[i]).append(" forced=").append(forced)
                         .append(" wall/drown dmg=").append(envDmg).append(" portal=").append(portal).append("; ");
                        b.discard(); cart.discard();
                    }
                    for (BlockPos l : lanes) level.setChunkForced(l.getX() >> 4, l.getZ() >> 4, false);
                    result("cx4_cx14_cx15_boss_flags", ok && control, d.toString());
                });
            });
        }));
    }

    // ------------------------------------------------------------------
    private static void cx6(ServerLevel level) {
        FakePlayer fp = AuditTests.freshPlayer(level, "QFMortal");
        var bs = com.matt.quantumfalcon.breeding.BreedingService.class;
        com.matt.quantumfalcon.breeding.BreedingService.recordTierRoll(fp, 3, false);
        com.matt.quantumfalcon.breeding.BreedingService.recordTierRoll(fp, 3, false);
        boolean newFind = com.matt.quantumfalcon.breeding.BreedingService.discover(fp, "variant:cx6_test");
        int pityBefore = com.matt.quantumfalcon.breeding.BreedingService.pity(fp, 3);
        FakePlayer reborn = respawned(fp);
        int pityAfter = com.matt.quantumfalcon.breeding.BreedingService.pity(reborn, 3);
        boolean stillFound = !com.matt.quantumfalcon.breeding.BreedingService.discover(reborn, "variant:cx6_test");
        boolean ok = newFind && pityBefore == 2 && pityAfter == 2 && stillFound;
        result("cx6_death_persistence", ok, "pity(t3) before death=" + pityBefore + " after=" + pityAfter + " | Codex discovery kept after death=" + stillFound);
    }

    // ------------------------------------------------------------------
    static int rollStars(ServerLevel level, Mob mob, FakePlayer killer, int n) {
        var table = level.getServer().reloadableRegistries().getLootTable(mob.getLootTable());
        var src = level.damageSources().playerAttack(killer);
        int stars = 0;
        for (int i = 0; i < n; i++) {
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, mob)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, mob.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, src)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            for (var st : table.getRandomItems(params)) if (st.is(net.minecraft.world.item.Items.NETHER_STAR)) stars += st.getCount();
        }
        return stars;
    }

    /** Natural Void Lords / Eclipse Phantoms despawn and drop a star only rarely; the Heart needs one star and the quest says so. */
    private static void stars(ServerLevel level) {
        BlockPos p = pad(level, 3000, 3520, net.minecraft.world.level.block.Blocks.STONE);
        FakePlayer fp = AuditTests.freshPlayer(level, "QFStarHunter");
        StringBuilder d = new StringBuilder(); boolean ok = true;
        for (var t : List.<EntityType<? extends Mob>>of(ModEntities.VOID_LORD.get(), ModEntities.ECLIPSE_PHANTOM.get())) {
            Mob m = t.create(level);
            m.moveTo(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(p), net.minecraft.world.entity.MobSpawnType.NATURAL, null);
            boolean despawns = !m.isPersistenceRequired() && m.removeWhenFarAway(200 * 200);
            int stars = rollStars(level, m, fp, 400);
            boolean rare = stars > 0 && stars < 100;
            ok &= despawns && rare;
            d.append(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath()).append(": natural despawns=").append(despawns)
             .append(" stars in 400 player kills=").append(stars).append("; ");
        }
        var heart = level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "heart_of_quantum"));
        long starSlots = heart.map(h -> h.value().getIngredients().stream().filter(ing -> ing.test(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR))).count()).orElse(-1L);
        boolean questSays = com.matt.quantumfalcon.quest.QuestSystem.getQuest("main_5") != null
            && String.join(" ", com.matt.quantumfalcon.quest.QuestSystem.getQuest("main_5").objectiveDescriptions).contains("Nether Star");
        ok &= starSlots == 1 && questSays;
        d.append("Heart of Quantum star slots=").append(starSlots).append(" main_5 names the cost=").append(questSays);
        result("cx10_cx11_stars", ok, d.toString());
    }

    // ------------------------------------------------------------------
    /** Real block breaks and real hits: placed and instant-break blocks and cactus pricks give no gear XP; natural stone and mob hits do. */
    private static void cx12(ServerLevel level) {
        BlockPos p = pad(level, 3000, 3560, net.minecraft.world.level.block.Blocks.STONE);
        cleanup(level, p, 8);
        FakePlayer fp = AuditTests.freshPlayer(level, "QFGrinder");
        fp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var pick = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pick);
        boolean levelable = com.matt.quantumfalcon.leveling.core.ItemLevelHelper.canLevel(fp.getMainHandItem());
        java.util.function.IntSupplier toolXp = () -> com.matt.quantumfalcon.leveling.core.ItemLevelHelper.getXp(fp.getMainHandItem());
        fp.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        int x0 = toolXp.getAsInt();
        for (int i = 0; i < 10; i++) {   // place-and-break loop
            BlockPos q = p.offset(2, 0, 0);
            level.setBlock(q, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            com.matt.quantumfalcon.leveling.events.LevelingEventHandler.testMarkPlaced(level, q);
            fp.gameMode.destroyBlock(q);
        }
        int placedXp = toolXp.getAsInt() - x0;
        int x1 = toolXp.getAsInt();
        for (int i = 0; i < 10; i++) { BlockPos q = p.offset(-2, 0, i - 5); level.setBlock(q, net.minecraft.world.level.block.Blocks.SHORT_GRASS.defaultBlockState(), 2); fp.gameMode.destroyBlock(q); }
        int grassXp = toolXp.getAsInt() - x1;
        int x2 = toolXp.getAsInt();
        for (int i = 0; i < 10; i++) { BlockPos q = p.offset(3, 0, i - 5); level.setBlock(q, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3); fp.gameMode.destroyBlock(q); }
        int naturalXp = toolXp.getAsInt() - x2;
        // armor
        var chest = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
        fp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
        java.util.function.IntSupplier armorXp = () -> com.matt.quantumfalcon.leveling.core.ItemLevelHelper.getXp(fp.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
        int a0 = armorXp.getAsInt();
        // FakePlayers are invulnerable, so post the same damage event a real hit fires
        java.util.function.Consumer<net.minecraft.world.damagesource.DamageSource> hit = src -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
            new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(fp, new net.neoforged.neoforge.common.damagesource.DamageContainer(src, 1f)));
        for (int i = 0; i < 30; i++) { hit.accept(level.damageSources().cactus()); hit.accept(level.damageSources().fall()); }
        int envXp = armorXp.getAsInt() - a0;
        var z = EntityType.ZOMBIE.create(level); z.moveTo(p.getX() + 1.5, p.getY(), p.getZ() + 0.5); level.addFreshEntity(z);
        int a1 = armorXp.getAsInt();
        for (int i = 0; i < 30; i++) hit.accept(level.damageSources().mobAttack(z));
        int mobXp = armorXp.getAsInt() - a1;
        z.discard();
        boolean ok = levelable && placedXp == 0 && grassXp == 0 && naturalXp > 0 && envXp == 0 && mobXp > 0;
        result("cx12_no_afk_leveling", ok, "pickaxe levelable=" + levelable + " | 10 placed-stone breaks +" + placedXp + " xp, 10 grass +" + grassXp
            + ", 10 natural stone +" + naturalXp + " | armor: 30 cactus+fall +" + envXp + ", 30 zombie hits +" + mobXp);
    }

    // ------------------------------------------------------------------
    private static void cx13(ServerLevel level) {
        BlockPos p = pad(level, 3000, 3600, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        cleanup(level, p, 8);
        WorldDropsTests.force(level, p, true);
        FakePlayer fp = AuditTests.freshPlayer(level, "QFMolter");
        var f = ModEntities.QUANTUM_FALCON.get().create(level);
        f.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        level.addFreshEntity(f);
        var table = level.getServer().reloadableRegistries().getLootTable(f.getLootTable());
        int feathers = 0, vanilla = 0;
        for (int i = 0; i < 50; i++) {
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, f)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, f.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(fp))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            for (var st : table.getRandomItems(params)) { if (st.is(ModItems.FALCON_FEATHER.get())) feathers += st.getCount(); if (st.is(net.minecraft.world.item.Items.FEATHER)) vanilla += st.getCount(); }
        }
        f.tame(fp);
        f.moltTicks = 3;
        final int fe = feathers, va = vanilla;
        WorldDropsTests.whenTicking(level, p, 0, () -> later(20, () -> {
            long molted = level.getEntitiesOfClass(ItemEntity.class, f.getBoundingBox().inflate(4), ie -> ie.getItem().is(ModItems.FALCON_FEATHER.get())).size();
            boolean rescheduled = f.moltTicks >= com.matt.quantumfalcon.entity.QuantumFalconEntity.MOLT_MIN - 30;
            f.discard(); clearItems(level, p, 8);
            WorldDropsTests.force(level, p, false);
            result("cx13_falcon_feathers", fe > 0 && va == 0 && molted == 1 && rescheduled, "50 falcon kills -> falcon_feather " + fe + ", vanilla feather " + va
                + " | tamed adult molt -> " + molted + " feather, next molt in " + f.moltTicks + "t");
        }));
    }

    // ------------------------------------------------------------------
    private static void relics(ServerLevel level) {
        boolean noBandRecipe = level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "entanglement_band")).isEmpty();
        FakePlayer a = AuditTests.freshPlayer(level, "QFCrafterA"), b = AuditTests.freshPlayer(level, "QFFriendB");
        var gem = new net.minecraft.world.item.ItemStack(ModItems.REALITY_GEM.get());
        com.matt.quantumfalcon.story.StoryManager.bind(gem, a);
        a.getInventory().add(gem.copy());
        boolean aQualifies = com.matt.quantumfalcon.story.StoryManager.hasAllArtifacts(a);
        b.getInventory().add(gem.copy());   // handed over
        boolean bQualifies = com.matt.quantumfalcon.story.StoryManager.hasAllArtifacts(b);
        // a raw relic set is spent by the True choice
        b.getInventory().clearContent();
        for (Item r : List.of(ModItems.OVERSEERS_EYE.get(), ModItems.WARDENS_FRAGMENT.get(), ModItems.TWIN_ESSENCE.get(), ModItems.SOVEREIGNS_CROWN.get()))
            b.getInventory().add(new net.minecraft.world.item.ItemStack(r));
        boolean bWithRelics = com.matt.quantumfalcon.story.StoryManager.hasAllArtifacts(b);
        String spent = com.matt.quantumfalcon.story.StoryManager.consumeTrueRelics(b);
        boolean bAfter = com.matt.quantumfalcon.story.StoryManager.hasAllArtifacts(b);
        boolean ok = noBandRecipe && aQualifies && !bQualifies && bWithRelics && "relics".equals(spent) && !bAfter;
        result("cx17_cx19_relics", ok, "entanglement_band recipe gone=" + noBandRecipe + " | crafter with own gem qualifies=" + aQualifies
            + ", friend handed the gem qualifies=" + bQualifies + " | raw relic set qualifies=" + bWithRelics + ", True choice spends " + spent + ", still qualifies after=" + bAfter);
    }
}
