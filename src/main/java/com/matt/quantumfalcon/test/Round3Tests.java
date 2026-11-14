package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.arena.BossSummoner;
import com.matt.quantumfalcon.arena.fight.ArenaInteraction;
import com.matt.quantumfalcon.arena.fight.ModArenaBlocks;
import com.matt.quantumfalcon.story.BossCredit;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** Tests for the 2026-11-13 round3 recheck review. */
final class Round3Tests {
    private Round3Tests() {}

    static void register(LinkedHashMap<String, Consumer<ServerLevel>> tests) {
        tests.put("r3_1_arena_lockout", Round3Tests::r31);
        tests.put("r3_6_relic_stash_crumbles", Round3Tests::relicStash);
        tests.put("r3_3_overseer_volley", Round3Tests::r33);
        tests.put("r3_2_first_kill_per_player", Round3Tests::r32);
        tests.put("r3_4_relic_binding_crafter", Round3Tests::r34);
        tests.put("r3_7_gear_xp_sources", Round3Tests::r37);
        tests.put("r3_5_entangled_reload", Round3Tests::r35);
        tests.put("r3_8_seal_spread", Round3Tests::r38);
        // two-phase tests across a REAL server restart (tools/run_restart.sh); skipped by "qftest run all"
        tests.put("restart_a", Round3Tests::restartA);
        tests.put("restart_b", Round3Tests::restartB);
    }

    // ------------------------------------------------------------------
    /** The floor under the seal can't be dug, a participant who walks back to the wall is let in, a stranger isn't,
     *  and with nobody left inside the fight resets: seal down, boss at full health, intro re-armed. */
    private static void r31(ServerLevel level) {
        BlockPos c = BossFightTests.site(level, 13200, 12000 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, ArenaType.QUANTUM_GUARDIAN);
        if (boss == null) { result("r3_1_arena_lockout", false, "summon failed"); return; }
        FakePlayer fp = CheeseTests.fighter(level, "QFLockout");
        FakePlayer stranger = CheeseTests.fighter(level, "QFStranger");
        later(40, () -> {
            boss.setNoAi(true);
            var f = ArenaInteraction.startFight(level, boss); ArenaInteraction.finishSeal(level, f);   // seals go up over ~9 ticks; this test inspects them at once
            if (f == null) { result("r3_1_arena_lockout", false, "no fight"); return; }
            BlockPos in = ArenaInteraction.floorAt(level, c.getX() + 5, c.getY(), c.getZ());
            fp.moveTo(in.getX() + 0.5, in.getY() + 1, in.getZ() + 0.5);
            stranger.moveTo(c.getX() + 400.5, c.getY(), c.getZ() + 0.5);
            boss.invulnerableTime = 0; boss.hurt(level.damageSources().playerAttack(fp), 40f);
            float hurtHp = boss.getHealth();
            later(30, () -> {
                boolean participant = f.participants.contains(fp.getUUID());
                // dig the ground under the wall on the -x side
                int wx = c.getX() - f.radius - 1;
                BlockPos under = null;   // the first real block under the seal column
                for (int y = c.getY() + 8; y > c.getY() - 16 && under == null; y--) {
                    BlockPos q = new BlockPos(wx, y, c.getZ());
                    var st = level.getBlockState(q);
                    if (!st.isAir() && !st.is(ModArenaBlocks.SEAL.get())) under = q;
                }
                boolean dugBlocked = false;
                String underName = under == null ? "none" : level.getBlockState(under).getBlock().toString();
                if (under != null) {
                    var before = level.getBlockState(under);
                    fp.gameMode.destroyBlock(under);
                    dugBlocked = level.getBlockState(under).equals(before);
                }
                // fp "dies" and respawns outside, then walks back to the wall (+x side, 2 blocks out)
                BlockPos out = ArenaInteraction.floorAt(level, c.getX() + f.radius + 3, c.getY(), c.getZ());
                BlockPos outPos = out != null ? out.above() : new BlockPos(c.getX() + f.radius + 3, c.getY(), c.getZ());
                fp.moveTo(outPos.getX() + 0.5, outPos.getY(), outPos.getZ() + 0.5);
                stranger.moveTo(outPos.getX() + 0.5, outPos.getY(), outPos.getZ() + 2.5);
                final boolean dug = dugBlocked, part = participant; final String un = underName; final int sealN = f.seals.size();
                later(30, () -> {
                    double d = Math.hypot(fp.getX() - c.getX() - 0.5, fp.getZ() - c.getZ() - 0.5);
                    boolean letIn = d < f.radius;
                    double ds = Math.hypot(stranger.getX() - c.getX() - 0.5, stranger.getZ() - c.getZ() - 0.5);
                    boolean strangerOut = ds > f.radius;
                    BlockPos aSeal = f.seals.isEmpty() ? null : f.seals.get(0);
                    // everyone leaves
                    fp.moveTo(c.getX() + 400.5, c.getY(), c.getZ() + 0.5);
                    stranger.moveTo(c.getX() + 400.5, c.getY(), c.getZ() + 4.5);
                    later(ArenaInteraction.EMPTY_RESET_TICKS + 60, () -> {
                        boolean reset = !ArenaInteraction.FIGHTS.containsKey(f.center);
                        boolean healed = boss.isAlive() && boss.getHealth() >= boss.getMaxHealth() - 0.01f;
                        boolean sealDown = aSeal != null && !level.getBlockState(aSeal).is(ModArenaBlocks.SEAL.get());
                        boolean introRearmed = !boss.getPersistentData().getBoolean("qf_intro") && !boss.getPersistentData().getBoolean("qf_fight_started");
                        boss.discard();
                        ArenaInteraction.endFight(level, c);
                        BossCredit.TEST_PLAYERS.remove(fp.getUUID()); BossCredit.TEST_PLAYERS.remove(stranger.getUUID());
                        CheeseTests.unforceSite(level, c);
                        boolean ok = part && dug && letIn && strangerOut && reset && healed && sealDown && introRearmed;
                        result("r3_1_arena_lockout", ok, "seals=" + sealN + " participant=" + part + " | dig under the wall (" + un + ") blocked=" + dug
                            + " | participant back at the wall let in=" + letIn + " (d=" + String.format("%.1f", d) + ") stranger kept out=" + strangerOut
                            + " | empty " + ArenaInteraction.EMPTY_RESET_TICKS + "t -> reset=" + reset + " boss hp " + hurtHp + "->" + boss.getHealth() + " healed=" + healed
                            + " seal down=" + sealDown + " intro re-armed=" + introRearmed);
                    });
                });
            });
        });
    }

    // ------------------------------------------------------------------
    /** The Overseer's volley telegraphs before any shard flies, phase 1 deals at most 4 x 5, and it no longer levitates. */
    private static void r33(ServerLevel level) {
        BlockPos c = BossFightTests.site(level, 13800, 12000 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        Mob m = BossSummoner.buildAndSummon(level, c, ArenaType.QUANTUM_GUARDIAN);
        if (!(m instanceof com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity boss)) { result("r3_3_overseer_volley", false, "summon failed"); return; }
        later(40, () -> {
            boss.setNoAi(true);
            BlockPos fl = ArenaInteraction.floorAt(level, c.getX() + 12, c.getY(), c.getZ());
            net.minecraft.world.phys.Vec3 at = fl != null ? net.minecraft.world.phys.Vec3.atBottomCenterOf(fl.above()) : net.minecraft.world.phys.Vec3.atBottomCenterOf(c).add(12, 0, 0);
            var dz = MovesetTests.dummy(level, at);
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET})
                dz.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
            boss.setTarget(dz);
            float hp0 = dz.getHealth();
            boss.performShardVolley();
            later(8, () -> {
                int early = level.getEntitiesOfClass(com.matt.quantumfalcon.entity.projectile.QuantumShardEntity.class, boss.getBoundingBox().inflate(30)).size();
                var tr = com.matt.quantumfalcon.entity.boss.BossDirector.lastTelegraph(boss);
                boolean tele = tr != null && "shard_volley".equals(tr.move()) && tr.ticks() >= 12;
                later(80, () -> {
                    float dealt = hp0 - dz.getHealth();
                    float cap = com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity.volleyShards(1) * com.matt.quantumfalcon.entity.boss.QuantumOverseerEntity.volleyDamage(1);
                    boolean lev = dz.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
                    dz.discard(); boss.discard(); ArenaInteraction.endFight(level, c); CheeseTests.unforceSite(level, c);
                    boolean ok = early == 0 && tele && dealt > 0 && dealt <= cap + 0.01f && !lev;
                    result("r3_3_overseer_volley", ok, "shards in flight 8t into the wind-up=" + early + " telegraph=" + tele + (tr != null ? " (" + tr.move() + " " + tr.ticks() + "t)" : "")
                        + " | phase-1 volley at 12 m dealt " + String.format("%.1f", dealt) + " (cap " + cap + ") levitation=" + lev);
                });
            });
        });
    }

    // ------------------------------------------------------------------
    /** Veteran + rookie: the floor stays the reduced table, but the rookie personally gets the first-kill star roll and +400 XP. */
    private static void r32(ServerLevel level) {
        BlockPos c = BossFightTests.site(level, 14400, 12000 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        Mob b = BossSummoner.buildAndSummon(level, c, ArenaType.QUANTUM_GUARDIAN);
        if (b == null) { result("r3_2_first_kill_per_player", false, "summon failed"); return; }
        var star = net.minecraft.world.item.Items.NETHER_STAR;
        later(40, () -> {
            FakePlayer vet = CheeseTests.fighter(level, "QFVet2"), rookie = CheeseTests.fighter(level, "QFRookie2");
            com.matt.quantumfalcon.story.PlayerStoryData.get(vet).markBossDefeated("QUANTUM_OVERSEER");
            for (FakePlayer p : List.of(vet, rookie)) { p.setPos(b.getX() + 2, b.getY(), b.getZ()); b.invulnerableTime = 0; b.hurt(level.damageSources().playerAttack(p), 3f); }
            int rxp = rookie.totalExperience, vxp = vet.totalExperience;
            float old = com.matt.quantumfalcon.story.BossDeathHandler.FIRST_KILL_STAR_CHANCE;
            com.matt.quantumfalcon.story.BossDeathHandler.FIRST_KILL_STAR_CHANCE = 1f;
            CheeseTests.clearItems(level, c, 140);
            var at = b.blockPosition();
            CheeseTests.kill(level, b, level.damageSources().playerAttack(rookie));
            later(10, () -> {
                com.matt.quantumfalcon.story.BossDeathHandler.FIRST_KILL_STAR_CHANCE = old;
                long floor = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(at).inflate(40),
                    ie -> ie.getItem().is(star) || ie.getItem().is(com.matt.quantumfalcon.registry.ModItems.OVERSEERS_EYE.get())).size();
                int rStars = rookie.getInventory().countItem(star), vStars = vet.getInventory().countItem(star);
                int rGain = rookie.totalExperience - rxp, vGain = vet.totalExperience - vxp;
                int rEyes = rookie.getInventory().countItem(com.matt.quantumfalcon.registry.ModItems.OVERSEERS_EYE.get());
                for (FakePlayer p : List.of(vet, rookie)) BossCredit.TEST_PLAYERS.remove(p.getUUID());
                ArenaInteraction.endFight(level, c); CheeseTests.unforceSite(level, c);
                boolean ok = floor == 0 && rStars == 1 && vStars == 0 && rGain >= 400 && vGain < 400 && rEyes == 1;
                result("r3_2_first_kill_per_player", ok, "veteran+rookie: eye/star on floor=" + floor + " | rookie star=" + rStars + " xp+" + rGain + " eye=" + rEyes
                    + " | veteran star=" + vStars + " xp+" + vGain);
            });
        });
    }

    // ------------------------------------------------------------------
    /** A vanilla Crafter fed A's relics makes a gem bound to A; mixed owners give an unbound gem that qualifies nobody until
     *  picked up; B crafting a Heart from A's gem gets a Heart bound to A and doesn't qualify; gifted relics are bound. */
    private static void r34(ServerLevel level) {
        BlockPos p = pad(level, 14800, 12000 + DungeonTests.RUN_OFF, net.minecraft.world.level.block.Blocks.STONE);
        WorldDropsTests.force(level, p, true);
        FakePlayer a = AuditTests.freshPlayer(level, "QFRelicA"), bb = AuditTests.freshPlayer(level, "QFRelicB");
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            BlockPos cp = p.above(2);
            level.setBlock(cp, net.minecraft.world.level.block.Blocks.CRAFTER.defaultBlockState(), 3);
            var be = (net.minecraft.world.level.block.entity.CrafterBlockEntity) level.getBlockEntity(cp);
            java.util.function.BiFunction<java.util.UUID, java.util.UUID, net.minecraft.world.item.ItemStack[]> grid = (eyeOwner, rest) -> {
                net.minecraft.world.item.ItemStack[] g = new net.minecraft.world.item.ItemStack[9];
                Arrays.fill(g, net.minecraft.world.item.ItemStack.EMPTY);
                g[1] = rel(com.matt.quantumfalcon.registry.ModItems.OVERSEERS_EYE.get(), eyeOwner);
                g[3] = rel(com.matt.quantumfalcon.registry.ModItems.WARDENS_FRAGMENT.get(), rest);
                g[4] = new net.minecraft.world.item.ItemStack(com.matt.quantumfalcon.registry.ModItems.REALITY_FRAGMENT.get());
                g[5] = rel(com.matt.quantumfalcon.registry.ModItems.TWIN_ESSENCE.get(), rest);
                g[7] = rel(com.matt.quantumfalcon.registry.ModItems.SOVEREIGNS_CROWN.get(), rest);
                return g;
            };
            var g1 = grid.apply(a.getUUID(), a.getUUID());
            for (int i = 0; i < 9; i++) be.setItem(i, g1[i]);
            level.setBlock(cp.below(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);   // power it
            later(12, () -> {
                var made = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(cp).inflate(4),
                    ie -> ie.getItem().is(com.matt.quantumfalcon.registry.ModItems.REALITY_GEM.get()));
                java.util.UUID owner1 = made.isEmpty() ? null : com.matt.quantumfalcon.story.StoryManager.boundTo(made.get(0).getItem());
                boolean crafterBoundToA = a.getUUID().equals(owner1);
                made.forEach(net.minecraft.world.entity.Entity::discard);
                // mixed owners: A's eye + B's other relics -> unbound gem, qualifies neither until someone binds it
                level.setBlock(cp.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                var g2 = grid.apply(a.getUUID(), bb.getUUID());
                for (int i = 0; i < 9; i++) be.setItem(i, g2[i]);
                later(4, () -> {
                    level.setBlock(cp.below(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                    later(12, () -> {
                        var made2 = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(cp).inflate(4),
                            ie -> ie.getItem().is(com.matt.quantumfalcon.registry.ModItems.REALITY_GEM.get()));
                        net.minecraft.world.item.ItemStack mixed = made2.isEmpty() ? net.minecraft.world.item.ItemStack.EMPTY : made2.get(0).getItem().copy();
                        made2.forEach(net.minecraft.world.entity.Entity::discard);
                        boolean mixedUnbound = !mixed.isEmpty() && com.matt.quantumfalcon.story.StoryManager.boundTo(mixed) == null;
                        bb.getInventory().clearContent(); bb.getInventory().add(mixed.copy());
                        boolean unboundQualifies = com.matt.quantumfalcon.story.StoryManager.hasAllArtifacts(bb);
                        // B crafts a Heart from A's gem (the recipe path every crafting block uses)
                        var aGem = new net.minecraft.world.item.ItemStack(com.matt.quantumfalcon.registry.ModItems.REALITY_GEM.get());
                        com.matt.quantumfalcon.story.StoryManager.bind(aGem, a);
                        List<net.minecraft.world.item.ItemStack> in = new ArrayList<>(Collections.nCopies(9, net.minecraft.world.item.ItemStack.EMPTY));
                        in.set(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR));
                        in.set(4, aGem);
                        in.set(7, new net.minecraft.world.item.ItemStack(com.matt.quantumfalcon.registry.ModItems.QUANTUM_CORE.get()));
                        var ci = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, in);
                        var rec = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, ci, level);
                        net.minecraft.world.item.ItemStack heart = rec.map(h -> h.value().assemble(ci, level.registryAccess())).orElse(net.minecraft.world.item.ItemStack.EMPTY);
                        var cont = new net.minecraft.world.SimpleContainer(9);
                        for (int i = 0; i < 9; i++) cont.setItem(i, in.get(i));
                        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(bb, heart, cont));
                        bb.getInventory().clearContent(); bb.getInventory().add(heart.copy());
                        boolean heartToA = a.getUUID().equals(com.matt.quantumfalcon.story.StoryManager.boundTo(heart));
                        boolean bQualifies = com.matt.quantumfalcon.story.StoryManager.hasAllArtifacts(bb);
                        level.removeBlock(cp, false); level.removeBlock(cp.below(), false);
                        WorldDropsTests.force(level, p, false);
                        boolean ok = crafterBoundToA && mixedUnbound && !unboundQualifies && heartToA && !bQualifies && !heart.isEmpty();
                        result("r3_4_relic_binding_crafter", ok, "vanilla Crafter with A's relics -> gem bound to A=" + crafterBoundToA
                            + " | mixed owners -> unbound=" + mixedUnbound + ", qualifies the holder=" + unboundQualifies
                            + " | B crafts a Heart from A's gem -> " + (heart.isEmpty() ? "no recipe match" : "bound to A=" + heartToA) + ", B qualifies=" + bQualifies);
                    });
                });
            });
        });
    }

    /** stash a gifted relic, get it re-gifted, take the stash back out: only the newest copy survives. */
    private static void relicStash(ServerLevel level) {
        FakePlayer fp = AuditTests.freshPlayer(level, "QFRelicStasher");
        var eye = com.matt.quantumfalcon.registry.ModItems.OVERSEERS_EYE.get();
        var first = new net.minecraft.world.item.ItemStack(eye);
        com.matt.quantumfalcon.story.StoryManager.bind(first, fp); com.matt.quantumfalcon.story.StoryManager.stampRelic(first, fp);
        var chest = new net.minecraft.world.SimpleContainer(1); chest.setItem(0, first);       // stashed
        var second = new net.minecraft.world.item.ItemStack(eye);                                // the repeat kill's re-gift
        com.matt.quantumfalcon.story.StoryManager.bind(second, fp); com.matt.quantumfalcon.story.StoryManager.stampRelic(second, fp);
        fp.getInventory().add(second);
        int purgedNone = com.matt.quantumfalcon.story.StoryManager.purgeStaleRelics(fp);
        fp.getInventory().add(chest.removeItemNoUpdate(0));                                      // take the stash back out
        int purged = com.matt.quantumfalcon.story.StoryManager.purgeStaleRelics(fp);
        int held = fp.getInventory().countItem(eye);
        // someone else's relic and an old unstamped save copy of a never-regifted relic are left alone
        FakePlayer other = AuditTests.freshPlayer(level, "QFRelicOther");
        var theirs = new net.minecraft.world.item.ItemStack(eye); com.matt.quantumfalcon.story.StoryManager.bind(theirs, other);
        var oldSave = new net.minecraft.world.item.ItemStack(com.matt.quantumfalcon.registry.ModItems.TWIN_ESSENCE.get()); com.matt.quantumfalcon.story.StoryManager.bind(oldSave, fp);
        fp.getInventory().add(theirs); fp.getInventory().add(oldSave);
        int purgedOthers = com.matt.quantumfalcon.story.StoryManager.purgeStaleRelics(fp);
        boolean ok = purgedNone == 0 && purged == 1 && held == 1 && purgedOthers == 0;
        result("r3_6_relic_stash_crumbles", ok, "re-gift held, nothing stale=" + (purgedNone == 0) + " | stash taken back: crumbled=" + purged + " eyes held=" + held
            + " | another player's eye and an old unstamped relic kept=" + (purgedOthers == 0));
    }

    static net.minecraft.world.item.ItemStack rel(net.minecraft.world.item.Item it, java.util.UUID owner) {
        var st = new net.minecraft.world.item.ItemStack(it);
        if (owner != null) com.matt.quantumfalcon.story.StoryManager.bind(st, owner);
        return st;
    }

    // ------------------------------------------------------------------
    static final net.minecraft.world.level.block.Block STONE = net.minecraft.world.level.block.Blocks.STONE;

    /** Real placement through ItemStack.useOn (the path a right-click takes; fires EntityPlaceEvent). */
    static boolean placeAsPlayer(FakePlayer fp, BlockPos at, net.minecraft.world.item.Item item) {
        var keep = fp.getMainHandItem().copy();
        var st = new net.minecraft.world.item.ItemStack(item);
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, st);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at.below()).add(0, 0.5, 0), net.minecraft.core.Direction.UP, at.below(), false);
        var res = st.useOn(new net.minecraft.world.item.context.UseOnContext(fp, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, keep);
        return res.consumesAction() && fp.level().getBlockState(at).is(((net.minecraft.world.item.BlockItem) item).getBlock());
    }

    /** Tool XP gained by breaking one block with the held pickaxe. */
    static int mine(FakePlayer fp, BlockPos at) {
        int x0 = com.matt.quantumfalcon.leveling.core.ItemLevelHelper.getXp(fp.getMainHandItem());
        fp.moveTo(at.getX() + 1.5, at.getY(), at.getZ() + 0.5);
        fp.gameMode.destroyBlock(at);
        return com.matt.quantumfalcon.leveling.core.ItemLevelHelper.getXp(fp.getMainHandItem()) - x0;
    }

    static FakePlayer miner(ServerLevel level, String name) {
        FakePlayer fp = AuditTests.freshPlayer(level, name);
        fp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
        return fp;
    }

    /** Real right-click placement, a real lava+water generator and a real piston all give no tool XP; natural stone does.
     *  Armour XP from one caged attacker stops at the per-attacker budget; a new attacker still pays. */
    private static void r37(ServerLevel level) {
        BlockPos p = pad(level, 15200, 12000 + DungeonTests.RUN_OFF, STONE);
        WorldDropsTests.force(level, p, true);
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            FakePlayer fp = miner(level, "QFGrinder3");
            // 1) right-click placement
            int pXp = 0, pOk = 0;
            for (int i = 0; i < 5; i++) { BlockPos q = p.offset(-3, 0, i - 2); if (placeAsPlayer(fp, q, net.minecraft.world.item.Items.STONE)) { pOk++; pXp += mine(fp, q); } }
            final int placedXp = pXp, placedOk = pOk;
            // 2) cobble generator: lava source and water source two apart in a glass channel
            BlockPos g = p.offset(0, 0, -3);
            for (int dx = -2; dx <= 4; dx++) { level.setBlock(g.offset(dx, 0, -1), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(), 3); level.setBlock(g.offset(dx, 0, 1), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(), 3); }
            level.setBlock(g.offset(-2, 0, 0), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(), 3);
            level.setBlock(g.offset(4, 0, 0), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(), 3);
            level.setBlock(g.offset(-1, 0, 0), net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 3);
            level.setBlock(g.offset(3, 0, 0), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
            // 3) piston pushes a natural stone block one east
            BlockPos ps = p.offset(0, 0, 3);
            level.setBlock(ps.offset(-1, 0, 0), net.minecraft.world.level.block.Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING, net.minecraft.core.Direction.EAST), 3);
            level.setBlock(ps, STONE.defaultBlockState(), 3);
            level.setBlock(ps.offset(-2, 0, 0), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            later(80, () -> {
                List<BlockPos> gen = new ArrayList<>();
                for (int dx = -1; dx <= 3; dx++) {
                    BlockPos q = g.offset(dx, 0, 0); var st = level.getBlockState(q);
                    if (st.is(net.minecraft.world.level.block.Blocks.COBBLESTONE) || st.is(net.minecraft.world.level.block.Blocks.OBSIDIAN) || st.is(STONE)) gen.add(q);
                }
                int genMarked = 0, genXp = 0;
                for (BlockPos q : gen) { if (com.matt.quantumfalcon.leveling.PlacedBlocks.isMarked(level, q)) genMarked++; }
                for (int dx = -1; dx <= 3; dx++) { BlockPos q = g.offset(dx, 0, 0); if (!level.getFluidState(q).isEmpty() && level.getFluidState(q).isSource()) level.setBlock(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3); }
                for (BlockPos q : gen) genXp += mine(fp, q);
                BlockPos moved = ps.offset(1, 0, 0);
                boolean pushed = level.getBlockState(moved).is(STONE);
                boolean pushedMarked = com.matt.quantumfalcon.leveling.PlacedBlocks.isMarked(level, moved);
                int pistonXp = pushed ? mine(fp, moved) : -1;
                int naturalXp = 0;
                for (int i = 0; i < 5; i++) { BlockPos q = p.offset(3, 0, i - 2); level.setBlock(q, STONE.defaultBlockState(), 3); naturalXp += mine(fp, q); }
                // armour budget
                fp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE));
                java.util.function.IntSupplier ax = () -> com.matt.quantumfalcon.leveling.core.ItemLevelHelper.getXp(fp.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
                java.util.function.Consumer<net.minecraft.world.damagesource.DamageSource> hit = src -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(fp, new net.neoforged.neoforge.common.damagesource.DamageContainer(src, 1f)));
                var z1 = net.minecraft.world.entity.EntityType.ZOMBIE.create(level); z1.moveTo(p.getX() + 1.5, p.getY(), p.getZ() + 0.5); level.addFreshEntity(z1);
                var z2 = net.minecraft.world.entity.EntityType.ZOMBIE.create(level); z2.moveTo(p.getX() - 1.5, p.getY(), p.getZ() + 0.5); level.addFreshEntity(z2);
                int awards1 = 0, awards2 = 0;
                for (int i = 0; i < 200; i++) { int a = ax.getAsInt(); hit.accept(level.damageSources().mobAttack(z1)); if (ax.getAsInt() > a) awards1++; }
                for (int i = 0; i < 200; i++) { int a = ax.getAsInt(); hit.accept(level.damageSources().mobAttack(z2)); if (ax.getAsInt() > a) awards2++; }
                z1.discard(); z2.discard();
                int cap = com.matt.quantumfalcon.leveling.events.LevelingEventHandler.ARMOR_XP_PER_ATTACKER;
                for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) for (int dy = 0; dy < 3; dy++) level.setBlock(p.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                WorldDropsTests.force(level, p, false);
                boolean ok = placedOk == 5 && placedXp == 0 && !gen.isEmpty() && genMarked == gen.size() && genXp == 0 && pushed && pushedMarked && pistonXp == 0
                    && naturalXp > 0 && awards1 == cap && awards2 > 0 && awards2 <= cap;
                result("r3_7_gear_xp_sources", ok, "right-click placed " + placedOk + "/5 -> +" + placedXp + " xp | generator made " + gen.size() + " block(s), marked " + genMarked + " -> +" + genXp
                    + " | piston pushed=" + pushed + " marked=" + pushedMarked + " -> +" + pistonXp + " | 5 natural stone +" + naturalXp
                    + " | armour: 200 hits from one zombie -> " + awards1 + " awards (cap " + cap + "), a second zombie -> " + awards2);
            });
        });
    }

    // ------------------------------------------------------------------ over a REAL restart
    static final java.nio.file.Path RESTART_STATE = java.nio.file.Paths.get("qf_restart_state.txt");   // server cwd (run/)
    static final int RX = 61000, RZ = 61000;

    /** Phase 1: start a fight, break 2 of 4 pylons, place a block by right-click next to it; save the state. Then the server stops. */
    private static void restartA(ServerLevel level) {
        BlockPos c = BossFightTests.site(level, RX, RZ);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, ArenaType.VOID_HORROR);
        if (boss == null) { result("restart_a", false, "summon failed"); return; }
        later(40, () -> {
            boss.getPersistentData().putBoolean("qf_intro", true);
            ArenaInteraction.Fight f = ArenaInteraction.startFight(level, boss); ArenaInteraction.finishSeal(level, f);   // seals go up over ~9 ticks; this test inspects them at once
            int pyl0 = ArenaInteraction.chargedPylons(level, f);
            FakePlayer fp = CheeseTests.fighter(level, "QFQuitter");
            int broken = 0;
            for (BlockPos q : new ArrayList<>(f.pylons)) { if (broken == 2) break; fp.moveTo(q.getX() + 1.5, q.getY(), q.getZ() + 0.5); if (fp.gameMode.destroyBlock(q)) broken++; }
            float h0 = boss.getHealth(); boss.invulnerableTime = 0; boss.hurt(level.damageSources().magic(), 10f);
            float sh = h0 - boss.getHealth();
            int seals = BossFightTests.sealCount(level, f);
            BossCredit.TEST_PLAYERS.remove(fp.getUUID());
            // a right-click-placed block on a pad far from the arena
            BlockPos pd = pad(level, RX + 300, RZ, STONE);
            FakePlayer m = miner(level, "QFPlacer");
            BlockPos q = pd.offset(2, 0, 0);
            boolean placed = placeAsPlayer(m, q, net.minecraft.world.item.Items.STONE);
            String line = c.getX() + " " + c.getY() + " " + c.getZ() + " " + boss.getUUID() + " " + broken + " " + seals + " " + q.getX() + " " + q.getY() + " " + q.getZ();
            try { java.nio.file.Files.writeString(RESTART_STATE, line); } catch (Exception ex) { result("restart_a", false, "state write failed " + ex); return; }
            result("restart_a", pyl0 == 4 && broken == 2 && sh > 0 && placed && com.matt.quantumfalcon.leveling.PlacedBlocks.isMarked(level, q),
                "fight up: pylons " + pyl0 + ", broke " + broken + ", dmg10 -> " + String.format("%.1f", sh) + ", seals " + seals
                + " | right-click placed stone=" + placed + " | state saved: " + line);
        });
    }

    /** Phase 2 (after a real JVM restart): the fight is rebuilt with 2 charged pylons and the shield, the seal is tracked,
     *  a kill 200 blocks out retires the arena and drops the seal; the pre-restart placed block still gives no tool XP. */
    private static void restartB(ServerLevel level) {
        String[] s;
        try { s = java.nio.file.Files.readString(RESTART_STATE).trim().split(" "); } catch (Exception ex) { result("restart_b", false, "no state (run restart_a first) " + ex); return; }
        BlockPos c = new BlockPos(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]));
        UUID id = UUID.fromString(s[3]);
        BlockPos q = new BlockPos(Integer.parseInt(s[6]), Integer.parseInt(s[7]), Integer.parseInt(s[8]));
        BossFightTests.site(level, c.getX(), c.getZ());
        WorldDropsTests.force(level, q, true);
        BlockPos far = new BlockPos(c.getX() + 200, c.getY(), c.getZ());
        WorldDropsTests.force(level, far, true);
        WorldDropsTests.whenTicking(level, c, 0, () -> WorldDropsTests.whenTicking(level, far, 0, () -> later(100, () -> {
            StringBuilder d = new StringBuilder("REAL restart: ");
            // placed marker survived
            boolean marked = com.matt.quantumfalcon.leveling.PlacedBlocks.isMarked(level, q);
            FakePlayer m = miner(level, "QFPlacer");
            boolean stoneThere = level.getBlockState(q).is(STONE);
            int placedXp = stoneThere ? mine(m, q) : -1;
            d.append("placed stone still there=").append(stoneThere).append(" marked=").append(marked).append(" -> +").append(placedXp).append(" xp | ");
            var e = level.getEntity(id);
            if (!(e instanceof Mob boss)) { cleanupRestart(level, c, far, q); result("restart_b", false, d.append("boss not found (").append(e).append(")").toString()); return; }
            var a = com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.arenaAt(c);
            boolean owns = a != null && id.equals(a.bossId);
            ArenaInteraction.Fight g = ArenaInteraction.fightFor(boss);
            int charged = g == null ? -1 : ArenaInteraction.chargedPylons(level, g);
            int seals = g == null ? -1 : BossFightTests.sealCount(level, g);
            float h0 = boss.getHealth(); boss.invulnerableTime = 0; boss.hurt(level.damageSources().magic(), 10f);
            float sh = h0 - boss.getHealth();
            d.append("arena owns boss=").append(owns).append(" fight rebuilt=").append(g != null).append(" charged pylons=").append(charged)
             .append(" (2 expected) seals=").append(seals).append(" (before ").append(s[5]).append(") dmg10 -> ").append(String.format("%.1f", sh));
            FakePlayer fp = CheeseTests.fighter(level, "QFHauler");
            boss.setPos(far.getX() + 0.5, far.getY() + 1, far.getZ() + 0.5);
            fp.setPos(boss.getX() + 2, boss.getY(), boss.getZ());
            for (int i = 0; i < 6 && boss.isAlive(); i++) { boss.invulnerableTime = 0; boss.setHealth(Math.min(boss.getHealth(), 2f)); boss.hurt(level.damageSources().playerAttack(fp), 100f); }
            boolean dead = !boss.isAlive();
            later(120, () -> {
                boolean retired = com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.arenaAt(c) == null;
                int sealsLeft = g == null ? -1 : BossFightTests.sealCount(level, g);
                d.append(" | killed 200 out: dead=").append(dead).append(" arena retired=").append(retired).append(" seals left=").append(sealsLeft);
                BossCredit.TEST_PLAYERS.remove(fp.getUUID());
                cleanupRestart(level, c, far, q);
                try { java.nio.file.Files.deleteIfExists(RESTART_STATE); } catch (Exception ignored) {}
                result("restart_b", stoneThere && marked && placedXp == 0 && owns && g != null && charged == 2 && sh > 0 && sh < 9f && dead && retired && sealsLeft == 0, d.toString());
            });
        })));
    }

    private static void cleanupRestart(ServerLevel level, BlockPos c, BlockPos far, BlockPos q) {
        WorldDropsTests.force(level, far, false); WorldDropsTests.force(level, q, false); CheeseTests.unforceSite(level, c);
    }

    // ------------------------------------------------------------------
    /** Two linked Entangled Ores; their chunk really unloads and reloads 3 times: the network stays at 2 nodes, the same yield,
     *  and the chunk isn't left dirty-looping (it unloads every time). */
    private static void r35(ServerLevel level) {
        BlockPos p = pad(level, 15600, 12000 + DungeonTests.RUN_OFF, STONE);
        WorldDropsTests.force(level, p, true);
        UUID netId = UUID.randomUUID();
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            for (int k = 0; k < 2; k++) {
                BlockPos q = p.offset(4 + k * 2, 0, 0);
                level.setBlock(q, com.matt.quantumfalcon.registry.ModBlocks.ENTANGLED_ORE.get().defaultBlockState(), 3);
                if (level.getBlockEntity(q) instanceof com.matt.quantumfalcon.blockentity.EntangledOreBlockEntity be) be.linkToNetwork(netId);
            }
            var n0 = com.matt.quantumfalcon.blockentity.EntangledOreBlockEntity.getNetwork(netId);
            int nodes0 = n0 == null ? -1 : n0.getNodeCount(), yield0 = n0 == null ? -1 : n0.getTotalYield();
            List<String> cycles = new ArrayList<>();
            int cx = p.getX() >> 4, cz = p.getZ() >> 4;
            Runnable[] cycle = new Runnable[1];
            int[] left = {3};
            cycle[0] = () -> {
                WorldDropsTests.force(level, p, false);   // drop every ticket around (pad() and force() both add some)
                waitUnload(level, cx, cz, 0, unloaded -> {
                    var mid = com.matt.quantumfalcon.blockentity.EntangledOreBlockEntity.getNetwork(netId);
                    int midNodes = mid == null ? 0 : mid.getNodeCount();
                    WorldDropsTests.force(level, p, true);
                    WorldDropsTests.whenTicking(level, p, 0, () -> later(5, () -> {
                        var n = com.matt.quantumfalcon.blockentity.EntangledOreBlockEntity.getNetwork(netId);
                        cycles.add("unloaded=" + unloaded + " (nodes while out " + midNodes + ") -> nodes " + (n == null ? 0 : n.getNodeCount()) + " yield " + (n == null ? 0 : n.getTotalYield()));
                        if (--left[0] > 0) { cycle[0].run(); return; }
                        boolean ok = nodes0 == 2 && cycles.stream().allMatch(c -> c.startsWith("unloaded=true") && c.endsWith("nodes 2 yield " + yield0));
                        for (int k = 0; k < 2; k++) level.setBlock(p.offset(4 + k * 2, 0, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        WorldDropsTests.force(level, p, false);
                        result("r3_5_entangled_reload", ok, "linked 2 -> nodes " + nodes0 + " yield " + yield0 + " | real unload/reload x3: " + cycles);
                    }));
                });
            };
            cycle[0].run();
        });
    }

    /** Polls until the chunk is no longer loaded (or 600 ticks pass). */
    static void waitUnload(ServerLevel level, int cx, int cz, int waited, java.util.function.Consumer<Boolean> then) {
        if (level.getChunkSource().getChunkNow(cx, cz) == null) { then.accept(true); return; }
        if (waited > 600) { then.accept(false); return; }
        later(10, () -> waitUnload(level, cx, cz, waited + 10, then));
    }

    // ------------------------------------------------------------------
    /** The Guardian's seal (the largest per tick before) goes up spread over ticks, bottom rows first, with a sparse lit
     *  lattice; the call that starts the fight stays short; the drop is spread too and leaves nothing behind. */
    private static void r38(ServerLevel level) {
        BlockPos c = BossFightTests.site(level, 16000, 12000 + DungeonTests.RUN_OFF);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, ArenaType.QUANTUM_GUARDIAN);
        if (boss == null) { result("r3_8_seal_spread", false, "summon failed"); return; }
        later(40, () -> {
            boss.setNoAi(true);
            long t0 = System.nanoTime();
            var f = ArenaInteraction.startFight(level, boss);
            double startMs = (System.nanoTime() - t0) / 1e6;
            int planned = f.seals.size();
            int placed0 = BossFightTests.sealCount(level, f);
            // lowest wall row done first?
            int minY = f.seals.stream().mapToInt(BlockPos::getY).min().orElse(0);
            long lowPlaced = f.seals.stream().filter(q -> q.getY() == minY && level.getBlockState(q).is(ModArenaBlocks.SEAL.get())).count();
            long lowTotal = f.seals.stream().filter(q -> q.getY() == minY).count();
            int[] ticksToFull = {-1};
            for (int t = 1; t <= 30; t++) { final int tt = t; later(t, () -> { if (ticksToFull[0] < 0 && BossFightTests.sealCount(level, f) == planned) ticksToFull[0] = tt; }); }
            later(32, () -> {
                int full = BossFightTests.sealCount(level, f);
                long lit = f.seals.stream().filter(q -> { var st = level.getBlockState(q); return st.is(ModArenaBlocks.SEAL.get()) && st.getValue(com.matt.quantumfalcon.arena.fight.ArenaSealBlock.LIT); }).count();
                long t1 = System.nanoTime();
                ArenaInteraction.endFight(level, c);
                double endMs = (System.nanoTime() - t1) / 1e6;
                int afterEnd = BossFightTests.sealCount(level, f);
                later(30, () -> {
                    int left = BossFightTests.sealCount(level, f);
                    CheeseTests.unforceSite(level, c);
                    int per = ArenaInteraction.SEAL_PER_TICK;
                    boolean ok = planned > 10000 && placed0 <= per && lowPlaced == lowTotal && ticksToFull[0] > 0 && ticksToFull[0] <= 15 && full == planned
                        && lit > 0 && lit < planned / 8 && afterEnd >= planned - per && left == 0;
                    result("r3_8_seal_spread", ok, String.format("seals planned %d | startFight call %.1f ms, placed in that tick %d (budget %d/tick), lowest row %d/%d done first"
                        + " | full after %d ticks | lit %d (%.1f%%) | endFight call %.1f ms, still standing right after %d, after 30 ticks %d",
                        planned, startMs, placed0, per, lowPlaced, lowTotal, ticksToFull[0], lit, 100.0 * lit / Math.max(1, planned), endMs, afterEnd, left));
                });
            });
        });
    }
}
