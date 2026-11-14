package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.capability.PlayerInstabilityCapability;
import com.matt.quantumfalcon.dimension.EchoRiftDimension;
import com.matt.quantumfalcon.dimension.InstabilityEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

/** One qftest per item from the 2026-11-13 world drops audit review. */
final class WorldDropsTests {
    private WorldDropsTests() { }

    static void register(LinkedHashMap<String, Consumer<ServerLevel>> tests) {
        tests.put("wd1_one_instability_system", WorldDropsTests::wd1);
        tests.put("wd2_overseer_one_legendary_egg", WorldDropsTests::wd2);
        tests.put("wd3_arena_chests_real_eggs", WorldDropsTests::wd3);
        tests.put("wd6_construct_loot_once", WorldDropsTests::wd6);
        tests.put("wd14_drops_kept_out_of_pits", WorldDropsTests::wd14);
        tests.put("wd7_echo_compass_next_boss", WorldDropsTests::wd7);
        tests.put("wd8_rift_route_in_game", WorldDropsTests::wd8);
        tests.put("wd9_wd12_one_tier_scale", WorldDropsTests::wd9);
        tests.put("wd4_no_dead_drops", WorldDropsTests::wd4);
    }

    /**
     * one meter; zone tier from the arrival anchor, not 0,0; T0 near the portal builds nothing; safe budget
     * matches the rate; game-tick driven; respawn clears it with grace; the stabilizer item really holds it.
     */
    private static void wd1(ServerLevel overworld) {
        ServerLevel rift = overworld.getServer().getLevel(EchoRiftDimension.ECHO_RIFT_LEVEL_KEY);
        if (rift == null) { QFTestCommands.result("wd1_one_instability_system", false, "no echo_rift level"); return; }
        StringBuilder d = new StringBuilder();
        FakePlayer fp = AuditTests.freshPlayer(rift, "QFWanderer");
        PlayerInstabilityCapability cap = PlayerInstabilityCapability.get(fp);
        cap.reset(); cap.setGraceTicks(0); cap.setStabilizedUntil(0);
        // arrive far from 0,0 (old system: distance 5000 from 0,0 = tier 3 on arrival), through a real lit Rift portal:
        // the portal's own destination and post-teleport hook (ModPortals) set the anchor
        cap.clearAnchor();
        net.minecraft.world.phys.Vec3 a1 = throughPortal(overworld, rift, fp, 5000, 5000);
        if (a1 == null) { QFTestCommands.result("wd1_one_instability_system", false, "portal 1 did not light / link"); return; }
        int arrivalZone = InstabilityEffects.zoneTier(fp);
        int ax = cap.getAnchorX(), az = cap.getAnchorZ();
        // a second, home-made portal 3,000 blocks out does not move the anchor (that spot stays a far zone)
        net.minecraft.world.phys.Vec3 a2 = throughPortal(overworld, rift, fp, 8000, 5000);
        int secondZone = a2 == null ? -1 : InstabilityEffects.zoneTier(fp);
        boolean anchorKept = cap.getAnchorX() == ax && cap.getAnchorZ() == az;
        d.append("portal1 -> ").append((int) a1.x).append(',').append((int) a1.z).append(" zone=").append(arrivalZone)
         .append(" | portal2 -> ").append(a2 == null ? "none" : (int) a2.x + "," + (int) a2.z).append(" zone=").append(secondZone)
         .append(" anchor kept=").append(anchorKept).append(" | ");
        fp.moveTo(ax + 0.5, 120, az + 0.5);
        long t = rift.getGameTime();
        for (int i = 0; i < 20 * 60; i++) InstabilityEffects.tick(fp, t + i);   // one game minute at the portal
        float atPortal = cap.getInstability();
        d.append("after 60s at portal=").append(atPortal).append('%');
        // walk out to a tier-2 band and measure the budget against the real rate
        fp.moveTo(ax + 0.5 + 1500, 120, az + 0.5);
        int farZone = InstabilityEffects.zoneTier(fp);
        int budget = InstabilityEffects.safeSeconds(fp, farZone);
        for (int i = 0; i < 20 * 10; i++) InstabilityEffects.tick(fp, t + 2000 + i);
        float after10 = cap.getInstability();
        boolean budgetMatches = budget > 60 && Math.abs(after10 - 10 * (50f / budget)) < 1.0f;
        float hpBefore = fp.getHealth();
        d.append(" | 1500 out zone=").append(farZone).append(" budget=").append(budget).append("s, +").append(after10).append("% in 10s (matches=").append(budgetMatches).append(')');
        // nothing hurts below 50 %
        boolean noHarmBelow50 = fp.getHealth() >= hpBefore && !fp.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
        // respawn: meter to 0 + grace
        cap.setInstability(90f);
        InstabilityEffects.onRespawn(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent(fp, false));
        float afterRespawn = cap.getInstability();
        for (int i = 0; i < 20 * 20; i++) InstabilityEffects.tick(fp, t + 4000 + i);
        float afterGrace = cap.getInstability();
        d.append(" | respawn 90%->").append(afterRespawn).append("%, +20s in grace=").append(afterGrace).append('%');
        // stabilizer item: 0 and held
        cap.setGraceTicks(0); cap.setInstability(60f);
        InstabilityEffects.useStabilizerItem(fp);
        for (int i = 0; i < 20 * 30; i++) InstabilityEffects.tick(fp, rift.getGameTime());
        float afterStab = cap.getInstability();
        d.append(" | stabilizer 60%->").append(afterStab).append("% after 30s far out");
        boolean ok = arrivalZone == 0 && secondZone >= 2 && anchorKept && Math.abs(a1.x - 5000) < 16 && atPortal == 0f && farZone == 2 && budgetMatches && noHarmBelow50
            && afterRespawn == 0f && afterGrace == 0f && afterStab == 0f;
        cap.reset(); cap.setStabilizedUntil(0);
        QFTestCommands.result("wd1_one_instability_system", ok, d.toString());
    }

    /**
     * Build a Rift Frame portal in the Overworld at (x, z), light it with the Rift Igniter, ask the lit portal block for its
     * destination (the API links / builds the Rift side) and run the transition's post-teleport hook on {@code riftPlayer}.
     * Returns the arrival position, or null if the portal did not light.
     */
    static net.minecraft.world.phys.Vec3 throughPortal(ServerLevel ow, ServerLevel rift, FakePlayer riftPlayer, int x, int z) {
        net.minecraft.core.BlockPos base = QFTestCommands.pad(ow, x, z, net.minecraft.world.level.block.Blocks.STONE);
        var frame = com.matt.quantumfalcon.registry.ModBlocks.RIFT_FRAME.get().defaultBlockState();
        // the Rift portal has a forced size (ModPortals.forcedSize(4, 5)): a 4x5 opening in a 6x7 frame
        for (int dx = 0; dx <= 5; dx++) for (int dy = 0; dy <= 6; dy++) {
            boolean edge = dx == 0 || dx == 5 || dy == 0 || dy == 6;
            ow.setBlock(base.offset(dx, dy, 0), edge ? frame : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        net.minecraft.core.BlockPos inside = base.offset(1, 1, 0);
        net.kyrptonaught.customportalapi.portal.PortalPlacer.attemptPortalLight(ow, inside,
            net.kyrptonaught.customportalapi.portal.PortalIgnitionSource.ItemUseSource(com.matt.quantumfalcon.registry.ModBlocks.RIFT_IGNITER.get().asItem()));
        if (!(ow.getBlockState(inside).getBlock() instanceof net.kyrptonaught.customportalapi.CustomPortalBlock portal)) return null;
        FakePlayer walker = AuditTests.freshPlayer(ow, "QFPortalWalker");
        walker.moveTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
        var tr = portal.getPortalDestination(ow, walker, inside);
        if (tr == null || tr.newLevel() != rift) return null;
        riftPlayer.moveTo(tr.pos().x, tr.pos().y, tr.pos().z);
        tr.postDimensionTransition().onTransition(riftPlayer);
        return tr.pos();
    }

    /** Run r once the pad's chunk is entity-ticking (forced chunks load asynchronously; under load that takes a while). */
    static void whenTicking(ServerLevel level, net.minecraft.core.BlockPos p, int waited, Runnable r) {
        if (level.isPositionEntityTicking(p) || waited > 400) { r.run(); return; }
        QFTestCommands.later(5, () -> whenTicking(level, p, waited + 5, r));
    }

    /** Entities only tick (and are visible to queries) in loaded chunks: force the 3x3 around a test pad. */
    static void force(ServerLevel level, net.minecraft.core.BlockPos p, boolean on) {
        for (int cx = -1; cx <= 1; cx++) for (int cz = -1; cz <= 1; cz++) level.setChunkForced((p.getX() >> 4) + cx, (p.getZ() >> 4) + cz, on);
    }

    private static int eggs(FakePlayer fp, java.util.List<Integer> tiers) {
        int n = 0;
        for (int i = 0; i < fp.getInventory().getContainerSize(); i++) {
            var st = fp.getInventory().getItem(i);
            if (st.is(com.matt.quantumfalcon.registry.ModItems.FALCON_EGG.get())) {
                n += st.getCount();
                var g = com.matt.quantumfalcon.item.FalconEggItem.child(st);
                tiers.add(g == null ? 0 : g.tier);
            }
        }
        return n;
    }

    /** one blessed egg per fight, no egg for hits eaten by i-frames, really Legendary, and not again from a second Overseer. */
    private static void wd2(ServerLevel level) {
        net.minecraft.core.BlockPos p = QFTestCommands.pad(level, 3000, 3240, net.minecraft.world.level.block.Blocks.STONE);
        QFTestCommands.cleanup(level, p, 10);
        FakePlayer fp = AuditTests.freshPlayer(level, "QFEggTapper");
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.matt.quantumfalcon.registry.ModItems.MAGENTA_SWORD.get()));
        fp.moveTo(p.getX() + 2.5, p.getY() + 1, p.getZ() + 0.5);
        var boss = com.matt.quantumfalcon.registry.ModEntities.QUANTUM_OVERSEER.get().create(level);
        boss.moveTo(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
        boss.setNoAi(true);
        level.addFreshEntity(boss);
        boss.devOpenWeakPoint();
        var src = level.damageSources().playerAttack(fp);
        boss.invulnerableTime = 0;
        boolean hit1 = boss.hurt(src, 4f);
        boolean hit2 = boss.hurt(src, 4f);    // same tick: inside i-frames
        boss.invulnerableTime = 0;
        boolean hit3 = boss.hurt(src, 4f);    // lands, same fight
        java.util.List<Integer> tiers = new java.util.ArrayList<>();
        int afterFight1 = eggs(fp, tiers);
        boss.discard();
        // a second Overseer: same player gets no second egg (farm limit); a new player still gets theirs
        var boss2 = com.matt.quantumfalcon.registry.ModEntities.QUANTUM_OVERSEER.get().create(level);
        boss2.moveTo(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
        boss2.setNoAi(true);
        level.addFreshEntity(boss2);
        boss2.devOpenWeakPoint();
        boss2.invulnerableTime = 0;
        boss2.hurt(src, 4f);
        int afterFight2 = eggs(fp, new java.util.ArrayList<>());
        FakePlayer other = AuditTests.freshPlayer(level, "QFNewcomer");
        other.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.matt.quantumfalcon.registry.ModItems.MAGENTA_SWORD.get()));
        boss2.blessedEggGiven = false; boss2.invulnerableTime = 0;
        boss2.hurt(level.damageSources().playerAttack(other), 4f);
        java.util.List<Integer> otherTiers = new java.util.ArrayList<>();
        int otherEggs = eggs(other, otherTiers);
        boss2.discard();
        // die and respawn, then a third Overseer: the once-per-player cap survives death
        FakePlayer reborn = CheeseTests.respawned(fp);
        var boss3 = com.matt.quantumfalcon.registry.ModEntities.QUANTUM_OVERSEER.get().create(level);
        boss3.moveTo(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
        boss3.setNoAi(true);
        level.addFreshEntity(boss3);
        boss3.devOpenWeakPoint();
        boss3.invulnerableTime = 0;
        boolean hitAfterDeath = boss3.hurt(level.damageSources().playerAttack(reborn), 4f);
        int afterDeath = eggs(reborn, new java.util.ArrayList<>());
        boss3.discard();
        // an old flag-only egg is honoured when placed
        var legacy = com.matt.quantumfalcon.entity.boss.BlessedEggs.ensureLegendary(com.matt.quantumfalcon.breeding.BreedingEngine.wild("peregrine", 1, level.random), level.random);
        boolean ok = afterFight1 == 1 && tiers.size() == 1 && tiers.get(0) >= 6 && afterFight2 == 1 && hitAfterDeath && afterDeath == 0 && otherEggs == 1 && otherTiers.get(0) >= 6 && legacy.tier >= 6;
        QFTestCommands.result("wd2_overseer_one_legendary_egg", ok, "hits landed " + hit1 + "/" + hit2 + "/" + hit3 + " -> eggs " + afterFight1 + " tiers " + tiers
            + " | second Overseer same player -> " + afterFight2 + " | after death, third Overseer (hit " + hitAfterDeath + ") -> " + afterDeath + " | new player -> " + otherEggs + " tiers " + otherTiers + " | legacy flag egg tier 1 -> " + legacy.tier);
    }

    /** every boss-arena chest's headline egg is a real falcon egg with a genome, and no table names the fake spawn egg. */
    private static void wd3(ServerLevel level) {
        StringBuilder d = new StringBuilder(); boolean ok = true;
        for (String t : new String[]{"quantum_guardian_arena", "void_horror_arena", "entangled_twins_arena", "singularity_sovereign_arena"}) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "chests/boss_arenas/" + t));
            var table = level.getServer().reloadableRegistries().getLootTable(key);
            int eggs = 0, fake = 0; java.util.Set<Integer> tiers = new java.util.TreeSet<>();
            for (int i = 0; i < 10; i++) {
                var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
                for (var st : table.getRandomItems(params)) {
                    if (st.is(com.matt.quantumfalcon.registry.ModItems.GENETIC_FALCON_SPAWN_EGG.get())) fake++;
                    if (st.is(com.matt.quantumfalcon.registry.ModItems.FALCON_EGG.get())) {
                        var g = com.matt.quantumfalcon.item.FalconEggItem.child(st);
                        if (g != null) { eggs++; tiers.add(g.tier); }
                    }
                }
            }
            d.append(t).append(": genome eggs=").append(eggs).append("/10 tiers=").append(tiers).append(" fake=").append(fake).append("; ");
            ok &= eggs >= 10 && fake == 0;
        }
        QFTestCommands.result("wd3_arena_chests_real_eggs", ok, d.toString());
    }

    /** a size-3 Data Construct family drops loot only from its four size-1 pieces. */
    private static void wd6(ServerLevel level) {
        net.minecraft.core.BlockPos p = QFTestCommands.pad(level, 3000, 3280, net.minecraft.world.level.block.Blocks.STONE);
        force(level, p, true);
        whenTicking(level, p, 0, () -> afterEntitiesLoad(level, p, 0, () -> wd6Body(level, p)));
    }

    /** Entity sections load after the chunk ticks: wait for them, then sweep leftovers (persistent pieces from earlier runs
     *  loading late used to join the count and make wd6 flaky). */
    static void afterEntitiesLoad(ServerLevel level, net.minecraft.core.BlockPos p, int waited, Runnable r) {
        boolean all = true;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
            all &= level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong((p.getX() >> 4) + dx, (p.getZ() >> 4) + dz));
        if (all || waited > 200) {
            level.getEntitiesOfClass(com.matt.quantumfalcon.entity.DataConstructEntity.class, new net.minecraft.world.phys.AABB(p).inflate(56)).forEach(net.minecraft.world.entity.Entity::discard);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(p).inflate(56)).forEach(net.minecraft.world.entity.Entity::discard);
            r.run();
        } else QFTestCommands.later(5, () -> afterEntitiesLoad(level, p, waited + 5, r));
    }

    private static void wd6Body(ServerLevel level, net.minecraft.core.BlockPos p) {
        QFTestCommands.cleanup(level, p, 12);
        FakePlayer fp = AuditTests.freshPlayer(level, "QFConstructKiller");
        var big = com.matt.quantumfalcon.registry.ModEntities.DATA_CONSTRUCT.get().create(level);
        big.moveTo(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
        big.setConstructSize(3); big.setNoAi(true); big.setPersistenceRequired();   // pieces inherit it: no despawn from a far-off player in the suite
        boolean added = level.addFreshEntity(big);
        boolean hurt = big.hurt(level.damageSources().playerAttack(fp), 1000f);
        int rightAfter = level.getEntitiesOfClass(com.matt.quantumfalcon.entity.DataConstructEntity.class, new net.minecraft.world.phys.AABB(p).inflate(8)).size();
        String diag = "added=" + added + " hurt=" + hurt + " dead=" + big.isDeadOrDying() + " removed=" + big.getRemovalReason() + " byId=" + (level.getEntity(big.getId()) != null)
            + " at " + big.blockPosition().toShortString() + " pad " + p.toShortString() + " size=" + big.getConstructSize() + " piecesNow=" + rightAfter + " | ";
        QFTestCommands.later(5, () -> {
            var box = new net.minecraft.world.phys.AABB(p).inflate(24);   // split pieces hop off with random velocity; 8 lost some
            int bigDrops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).size();
            var pieces = level.getEntitiesOfClass(com.matt.quantumfalcon.entity.DataConstructEntity.class, box);
            int killed = 0;
            for (int round = 0; round < 3; round++)
                for (var c : level.getEntitiesOfClass(com.matt.quantumfalcon.entity.DataConstructEntity.class, box)) {
                    if (c.isAlive() && c.getConstructSize() > 1) { c.invulnerableTime = 0; c.hurt(level.damageSources().playerAttack(fp), 1000f); killed++; }
                }
            final int killedF = killed;
            QFTestCommands.later(5, () -> {
                int midDrops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).size();
                var small = level.getEntitiesOfClass(com.matt.quantumfalcon.entity.DataConstructEntity.class, box, c -> c.isAlive() && c.getConstructSize() == 1);
                for (var c : small) { c.invulnerableTime = 0; c.hurt(level.damageSources().playerAttack(fp), 1000f); }
                QFTestCommands.later(5, () -> {
                    int endDrops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).size();
                    boolean ok = bigDrops == 0 && midDrops == 0 && small.size() == 4 && endDrops > 0;
                    QFTestCommands.result("wd6_construct_loot_once", ok, diag + "size-3 death drops=" + bigDrops + ", 2 size-2 deaths drops=" + midDrops
                        + ", size-1 pieces=" + small.size() + ", drops after they die=" + endDrops + " | pieces after split=" + pieces.size() + " size-2 hits=" + killedF
                        + " sizes now=" + level.getEntitiesOfClass(com.matt.quantumfalcon.entity.DataConstructEntity.class, box.inflate(32)).stream().map(c -> c.getConstructSize() + (c.isAlive() ? "" : "x")).toList());
                    level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).forEach(net.minecraft.world.entity.Entity::discard);
                    level.getEntitiesOfClass(com.matt.quantumfalcon.entity.DataConstructEntity.class, box.inflate(32)).forEach(net.minecraft.world.entity.Entity::discard);
                });
            });
        });
    }

    /** an item that drops over an arena's void pit right after a boss dies lands on the nearest floor instead. */
    private static void wd14(ServerLevel level) {
        net.minecraft.core.BlockPos p = QFTestCommands.pad(level, 3000, 3320, net.minecraft.world.level.block.Blocks.STONE);
        force(level, p, true);
        whenTicking(level, p, 0, () -> wd14Body(level, p));
    }

    private static void wd14Body(ServerLevel level, net.minecraft.core.BlockPos p) {
        QFTestCommands.cleanup(level, p, 12);
        // a floating 11x11 floor 70 up with a 3x3 hole in the middle: nothing under the hole for 70 blocks, like a void pit
        net.minecraft.core.BlockPos ground = p;
        p = p.above(70);
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++)
            level.setBlock(p.offset(dx, 0, dz), Math.abs(dx) <= 1 && Math.abs(dz) <= 1 ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        final net.minecraft.core.BlockPos fp = p;
        var bounds = new net.minecraft.world.phys.AABB(p).inflate(10, 8, 10);
        com.matt.quantumfalcon.worldgen.structure.ArenaDropGuard.watch(level, bounds, p, 100);
        var item = new net.minecraft.world.entity.item.ItemEntity(level, p.getX() + 0.5, p.getY() + 3, p.getZ() + 0.5,
            new net.minecraft.world.item.ItemStack(com.matt.quantumfalcon.registry.ModItems.QUANTUM_SHARD.get()));
        double startY = item.getY();
        level.addFreshEntity(item);
        QFTestCommands.later(40, () -> {
            boolean alive = item.isAlive();
            double y = item.getY();
            boolean ticked = item.tickCount > 20;
            boolean ok = alive && ticked && y >= fp.getY() && Math.abs(item.getX() - (fp.getX() + 0.5)) <= 4;
            QFTestCommands.result("wd14_drops_kept_out_of_pits", ok, "item over the pit after 2 s: ticked=" + item.tickCount + " alive=" + alive + " y=" + String.format("%.1f", y)
                + " (floor " + fp.getY() + ") at " + item.blockPosition().toShortString());
            item.discard();
        });
    }

    /** the Echo Compass is craftable and aims at the next unbeaten boss's arena type. */
    private static void wd7(ServerLevel level) {
        ServerLevel rift = level.getServer().getLevel(EchoRiftDimension.ECHO_RIFT_LEVEL_KEY);
        FakePlayer fp = AuditTests.freshPlayer(level, "QFCompassUser");
        var sd = com.matt.quantumfalcon.story.PlayerStoryData.get(fp);
        String first = com.matt.quantumfalcon.item.RiftCompassItem.nextBoss(fp)[0];
        sd.markBossDefeated("QUANTUM_OVERSEER");
        String[] second = com.matt.quantumfalcon.item.RiftCompassItem.nextBoss(fp);
        var where = com.matt.quantumfalcon.item.RiftCompassItem.locateOne(rift, net.minecraft.core.BlockPos.ZERO, second[1]);
        boolean recipe = level.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "echo_compass")).isPresent();
        for (String b : new String[]{"RIFT_WARDEN", "ENTANGLED_TWINS", "SINGULARITY_SOVEREIGN"}) sd.markBossDefeated(b);
        boolean allDone = com.matt.quantumfalcon.item.RiftCompassItem.nextBoss(fp) == null;
        boolean ok = first.equals("QUANTUM_OVERSEER") && second[0].equals("RIFT_WARDEN") && where.isPresent() && recipe && allDone;
        QFTestCommands.result("wd7_echo_compass_next_boss", ok, "fresh -> " + first + ", after Overseer -> " + second[0] + " (" + second[1] + ") at "
            + where.map(net.minecraft.core.BlockPos::toShortString).orElse("none") + " from 0,0 in the Rift | recipe=" + recipe + " | all beaten -> none=" + allDone);
    }

    /** the portal route is in game: recipe-unlock advancements, an 8-frame craft, the quest names its sources. */
    private static void wd8(ServerLevel level) {
        var server = level.getServer();
        var adv = server.getAdvancements().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "recipes/rift_route_shards"));
        var frame = server.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "rift_frame"));
        int frames = frame.map(r -> r.value().getResultItem(level.registryAccess()).getCount()).orElse(0);
        var q = com.matt.quantumfalcon.quest.QuestSystem.QUESTS.get("main_2");
        boolean questNamesSources = q != null && q.description.contains("riftslate") && q.description.contains("Codex");
        boolean ok = adv != null && frames == 8 && questNamesSources;
        QFTestCommands.result("wd8_rift_route_in_game", ok, "recipe advancement=" + (adv != null) + " frames per craft=" + frames + " main_2 names sources=" + questNamesSources);
    }

    /** banner, chests, instability and breeding read one tier scale. */
    private static void wd9(ServerLevel level) {
        ServerLevel rift = level.getServer().getLevel(EchoRiftDimension.ECHO_RIFT_LEVEL_KEY);
        int riftslate = com.matt.quantumfalcon.world.DangerTiers.ofStructure("dungeon_overworld_riftslate").tier();
        int cinder = com.matt.quantumfalcon.world.DangerTiers.ofStructure("dungeon_overworld_cinderite").tier();
        int prism = com.matt.quantumfalcon.world.DangerTiers.ofStructure("dungeon_overworld_prismite").tier();
        FakePlayer fp = AuditTests.freshPlayer(rift, "QFZoneWalker");
        fp.setPos(100.5, 120, 100.5);
        var cap = com.matt.quantumfalcon.capability.PlayerInstabilityCapability.get(fp);
        cap.setAnchor(100, 100);
        com.matt.quantumfalcon.world.DangerTiers.update(fp, null);
        var near = com.matt.quantumfalcon.world.DangerTiers.last(fp.getUUID());
        fp.setPos(2100.5, 120, 100.5);
        com.matt.quantumfalcon.world.DangerTiers.update(fp, null);
        var far = com.matt.quantumfalcon.world.DangerTiers.last(fp.getUUID());
        int zoneFar = com.matt.quantumfalcon.dimension.InstabilityEffects.zoneTier(fp);
        int layerFar = com.matt.quantumfalcon.breeding.BreedingService.riftLayer(rift, new net.minecraft.core.BlockPos(2100, 80, 100));
        int layerNear = com.matt.quantumfalcon.breeding.BreedingService.riftLayer(rift, new net.minecraft.core.BlockPos(2050, 80, 120));
        fp.setPos(100.5, 120, 100.5);
        int layerHome = com.matt.quantumfalcon.breeding.BreedingService.riftLayer(rift, new net.minecraft.core.BlockPos(110, 80, 110));
        boolean ok = riftslate == 1 && cinder == prism && near.tier() == 2 && far.tier() == 2 + zoneFar && zoneFar == 2 && layerFar == 3 && layerHome == 0
            && near.name().endsWith("Safe") && !far.name().matches(".*\\d.*");   // one number on screen (the tier)
        QFTestCommands.result("wd9_wd12_one_tier_scale", ok, "banner riftslate=" + riftslate + " cinderite=" + cinder + " prismite=" + prism
            + " | at anchor " + near + ", 2000 out " + far + " (zone " + zoneFar + ") | breeding layer far=" + layerFar + " (2nd spot " + layerNear + ") home=" + layerHome);
    }

    /** items with no recipe use and no code use; no loot table or code drop may hand them out. */
    static final String[] DEAD = {"aurora_essence", "dimensional_anchor", "echo_key", "egg_fragment_legendary", "gravity_anchor", "horizon_essence", "legendary_egg",
        "probability_core", "probability_dice", "probability_shard", "purple_gem", "quantum_capacitor", "quantum_catalyst", "quantum_fertilizer", "quantum_knot",
        "resonance_flower", "rift_fragment", "shiny_charm", "sovereign_sigil", "temporal_alloy",
        "temporal_shield", "void_fragment", "void_heart", "golden_falcon_egg", "genetic_falcon_spawn_egg"};

    /** loot pools hold no dead items; the respec book, Singularity Compass, Falcon Treat and rift ores do something. */
    private static void wd4(ServerLevel level) {
        String[] dead = DEAD;
        var server = level.getServer();
        var rm = server.getResourceManager();
        java.util.List<String> found = new java.util.ArrayList<>();
        for (var e : rm.listResources("loot_table", rl -> rl.getNamespace().equals("quantumfalcon") && rl.getPath().endsWith(".json")).entrySet()) {
            try (var in = e.getValue().open()) {
                String txt = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                for (String d : dead) if (txt.contains("\"quantumfalcon:" + d + "\"")) found.add(e.getKey().getPath() + ":" + d);
            } catch (Exception ex) { found.add("read-error " + e.getKey()); }
        }
        // respec book refunds
        FakePlayer fp = AuditTests.freshPlayer(level, "QFRespec");
        var sd = com.matt.quantumfalcon.skill.PlayerSkillData.get(fp);
        sd.addSkillPoints(3);
        int before = sd.getAvailableSkillPoints();
        String node = null; boolean spent = false;
        for (var sk : com.matt.quantumfalcon.skill.SkillTreeSystem.getAllSkills()) {
            if (sk.tier == 1 && sd.tryUnlockSkill(fp, sk.id)) { node = sk.id; spent = true; break; }
        }
        int afterBuy = sd.getAvailableSkillPoints();
        ItemStack book = new ItemStack(com.matt.quantumfalcon.registry.ModItems.SKILL_RESPEC_BOOK.get());
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, book);
        book.use(level, fp, net.minecraft.world.InteractionHand.MAIN_HAND);
        int afterBook = sd.getAvailableSkillPoints();
        boolean bookOk = spent && afterBuy < before && afterBook == before && fp.getMainHandItem().isEmpty();
        boolean sing = com.matt.quantumfalcon.registry.ModItems.SINGULARITY_COMPASS.get() instanceof com.matt.quantumfalcon.item.RiftCompassItem;
        var falcon = new com.matt.quantumfalcon.entity.QuantumFalconEntity(com.matt.quantumfalcon.registry.ModEntities.QUANTUM_FALCON.get(), level);
        boolean treat = falcon.isFood(new ItemStack(com.matt.quantumfalcon.registry.ModItems.FALCON_TREAT.get()));
        boolean ores = true;
        for (String m : new String[]{"chaos", "crystal", "probability", "resonance", "singularity"})
            ores &= server.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", m + "_ingot_from_smelting")).isPresent();
        boolean ok = found.isEmpty() && bookOk && sing && treat && ores;
        QFTestCommands.result("wd4_no_dead_drops", ok, "dead items left in loot=" + found + " | respec: " + before + " -> bought " + node + " -> " + afterBuy + " -> book -> " + afterBook
            + " consumed=" + fp.getMainHandItem().isEmpty() + " | singularity compass works=" + sing + " | treat is falcon food=" + treat + " | rift ores smelt=" + ores);
    }
}
