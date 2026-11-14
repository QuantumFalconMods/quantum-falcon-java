package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.skill.PlayerSkillData;
import com.matt.quantumfalcon.skills.UseSkill;
import com.matt.quantumfalcon.skills.UseSkills;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** A9: use-based skills - XP from real actions, the anti-farm gate, persistence, milestone points, and never restricting gear. */
public final class UseSkillsTests {
    private UseSkillsTests() {}

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("useskill_xp_gain", UseSkillsTests::xpGain);
        tests.put("useskill_no_farm", UseSkillsTests::noFarm);
        tests.put("useskill_persistence", UseSkillsTests::persistence);
        tests.put("useskill_bonus_points", UseSkillsTests::bonusPoints);
        tests.put("useskill_never_restricts", UseSkillsTests::neverRestricts);
        tests.put("useskill_exploration_bonus", UseSkillsTests::explorationBonus);
        tests.put("useskill_firsts_no_rollover", UseSkillsTests::firstsNoRollover);
        tests.put("useskill_grinder_cap", UseSkillsTests::grinderCap);
    }

    static long xp(FakePlayer fp, UseSkill s) { return UseSkills.xp(fp, s); }

    static Zombie zombie(ServerLevel level, BlockPos at) {
        Zombie z = EntityType.ZOMBIE.create(level);
        z.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        z.setNoAi(true); z.setPersistenceRequired();
        level.addFreshEntity(z);
        return z;
    }

    static void hit(Zombie z, FakePlayer fp, float dmg) { z.invulnerableTime = 0; z.setHealth(z.getMaxHealth()); z.hurt(fp.damageSources().playerAttack(fp), dmg); }

    /** a stream of 300 fresh mobs (a grinder) feeds Combat no more than the hourly cap. */
    private static void grinderCap(ServerLevel level) {
        BlockPos p = pad(level, 33200, 32000 + DungeonTests.RUN_OFF, Blocks.STONE);
        WorldDropsTests.force(level, p, true);
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            UseSkills.testResetBudgets();
            FakePlayer fp = Round3Tests.miner(level, "QFGrinder");
            fp.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 2.5);
            long x0 = xp(fp, UseSkill.COMBAT);
            for (int i = 0; i < 300; i++) { UseSkills.testSetIdle(fp, 0); Zombie z = zombie(level, p); hit(z, fp, 10f); z.discard(); }
            long gained = xp(fp, UseSkill.COMBAT) - x0;
            result("useskill_grinder_cap", gained > 0 && gained <= UseSkills.COMBAT_XP_PER_HOUR,
                "300 fresh zombies x 30 xp: Combat gained " + gained + " (hourly cap " + UseSkills.COMBAT_XP_PER_HOUR + ", uncapped would be 9000)");
        });
    }

    /** 5000 different "first time" sources, then the very first again: still a repeat (the old list rolled over at
     *  4096 and paid again). An old save's string list is folded in. */
    private static void firstsNoRollover(ServerLevel level) {
        FakePlayer fp = AuditTests.freshPlayer(level, "QFWanderer");
        UseSkills.testSetIdle(fp, 0);
        int ok = 0;
        for (int i = 0; i < 5000; i++) { UseSkills.testSetIdle(fp, 0); if (UseSkills.award(fp, UseSkill.EXPLORATION, 1, "first:chest.test@" + i) == UseSkills.Gate.OK) ok++; }
        UseSkills.testSetIdle(fp, 0);
        var again = UseSkills.award(fp, UseSkill.EXPLORATION, 1, "first:chest.test@0");
        var mid = UseSkills.award(fp, UseSkill.EXPLORATION, 1, "first:chest.test@2500");
        var fresh = UseSkills.award(fp, UseSkill.EXPLORATION, 1, "first:chest.test@new");
        FakePlayer old = AuditTests.freshPlayer(level, "QFOldSave");
        UseSkills.testSetIdle(old, 0);
        var t = new net.minecraft.nbt.CompoundTag(); var l = new net.minecraft.nbt.ListTag(); l.add(net.minecraft.nbt.StringTag.valueOf("biome.minecraft:plains")); t.put("firsts", l);
        UseSkills.testLoad(old, t);
        var legacy = UseSkills.award(old, UseSkill.EXPLORATION, 1, "first:biome.minecraft:plains");
        boolean pass = ok == 5000 && again == UseSkills.Gate.REPEAT && mid == UseSkills.Gate.REPEAT && fresh == UseSkills.Gate.OK && legacy == UseSkills.Gate.REPEAT;
        result("useskill_firsts_no_rollover", pass, "5000 firsts paid=" + ok + " | first one again=" + again + " | #2500 again=" + mid + " | a new one=" + fresh + " | old save's list kept=" + legacy);
    }

    /** at top Exploration a fresh loot chest opened by a survival player sometimes holds one extra copy of plain loot;
     *  a chest whose only loot is a falcon egg never gets a second egg. */
    private static void explorationBonus(ServerLevel level) {
        BlockPos p = pad(level, 33000, 32000 + DungeonTests.RUN_OFF, Blocks.STONE);
        WorldDropsTests.force(level, p, true);
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            FakePlayer fp = Round3Tests.miner(level, "QFExplorer");
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            UseSkills.setXp(fp, UseSkill.EXPLORATION, 50_000_000L);
            int lv = UseSkills.level(fp, UseSkill.EXPLORATION);
            var plainTable = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, net.minecraft.resources.ResourceLocation.withDefaultNamespace("chests/simple_dungeon"));
            var eggTable = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "chests/frozen_hours/eastwatch_egg"));
            int bonus0 = UseSkills.testBonuses;
            java.util.Map<String, Integer> why = new java.util.TreeMap<>();
            int plainExtra = 0, eggChests = 0, eggsMax = 0, plainChests = 0, unopened = 0;
            for (int i = 0; i < 80; i++) {
                UseSkills.testResetBudgets();
                boolean egg = i % 2 == 1;
                BlockPos c = p.offset(-6 + (i % 13), 1, -6 + (i / 13) * 2);
                level.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(c, Blocks.CHEST.defaultBlockState(), 3);
                if (!(level.getBlockEntity(c) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity ch)) continue;
                ch.setLootTable(egg ? eggTable : plainTable, level.random.nextLong());
                // what the table alone gives with this seed, for the "extra" count
                fp.moveTo(c.getX() + 0.5, c.getY() + 1, c.getZ() + 1.5);
                var res = fp.gameMode.useItemOn(fp, level, ItemStack.EMPTY, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(c), net.minecraft.core.Direction.UP, c, false));
                fp.closeContainer();
                // NeoForge's FakePlayer opens no menu, so vanilla's fill-on-open (createMenu -> unpackLootTable) is done here by hand
                if (ch.getLootTable() != null && res.consumesAction()) ch.unpackLootTable(fp);
                if (ch.getLootTable() != null) { unopened++; why.merge(res.toString(), 1, Integer::sum); continue; }
                java.util.Map<net.minecraft.world.item.Item, Integer> stacks = new java.util.HashMap<>();
                int eggs = 0;
                for (int k = 0; k < ch.getContainerSize(); k++) {
                    ItemStack st = ch.getItem(k);
                    if (st.isEmpty()) continue;
                    if (st.is(com.matt.quantumfalcon.registry.ModItems.FALCON_EGG.get())) eggs++;
                    stacks.merge(st.getItem(), 1, Integer::sum);
                }
                if (egg) { eggChests++; eggsMax = Math.max(eggsMax, eggs); }
                else { plainChests++; if (stacks.values().stream().anyMatch(n -> n > 1)) plainExtra++; }
                ch.clearContent();
                level.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
            }
            // the rule itself: never copies an egg, a bound relic or a one-off
            ItemStack relic = new ItemStack(com.matt.quantumfalcon.registry.ModItems.OVERSEERS_EYE.get());
            com.matt.quantumfalcon.story.StoryManager.bind(relic, fp);
            boolean rule = !UseSkills.copyable(com.matt.quantumfalcon.entity.npc.QFTraderEntity.falconEgg("warped", level.random)) && !UseSkills.copyable(relic)
                && !UseSkills.copyable(new ItemStack(Items.DIAMOND_SWORD)) && UseSkills.copyable(new ItemStack(Items.BREAD, 3));
            int bonuses = UseSkills.testBonuses - bonus0;
            boolean ok = lv >= 50 && unopened == 0 && bonuses > 0 && plainExtra > 0 && eggsMax == 1 && rule;
            result("useskill_exploration_bonus", ok, "Exploration Lv" + lv + " | bonus copies=" + bonuses + "/80 opened | plain chests " + plainChests + " with a repeated stack=" + plainExtra
                + " (chests with a natural repeat count too) | egg chests " + eggChests + " max eggs in one=" + eggsMax + " | unopened=" + unopened + " " + why + " | copy rule ok=" + rule);
        });
    }

    private static void xpGain(ServerLevel level) {
        BlockPos p = pad(level, 32400, 32000 + DungeonTests.RUN_OFF, Blocks.STONE);
        WorldDropsTests.force(level, p, true);
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            UseSkills.testResetBudgets();
            FakePlayer fp = Round3Tests.miner(level, "QFSkiller");
            StringBuilder d = new StringBuilder();
            // Mining: 3 natural stone (5 each) + 2 iron ore (20 each) = 55 -> level 2 (needs 40)
            for (int i = 0; i < 3; i++) { BlockPos q = p.offset(-2, -1, i - 1); fp.moveTo(q.getX() + 1.5, q.getY() + 1, q.getZ() + 0.5); fp.gameMode.destroyBlock(q); }
            for (int i = 0; i < 2; i++) { BlockPos q = p.offset(2, 1, i); level.setBlock(q, Blocks.IRON_ORE.defaultBlockState(), 3); fp.moveTo(q.getX() + 1.5, q.getY(), q.getZ() + 0.5); fp.gameMode.destroyBlock(q); }
            long mining = xp(fp, UseSkill.MINING); int mLv = UseSkills.level(fp, UseSkill.MINING);
            // Woodcutting: one natural log
            BlockPos lg = p.offset(0, 1, 3); level.setBlock(lg, Blocks.OAK_LOG.defaultBlockState(), 3);
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            fp.moveTo(lg.getX() + 1.5, lg.getY(), lg.getZ() + 0.5); fp.gameMode.destroyBlock(lg);
            long wood = xp(fp, UseSkill.WOODCUTTING);
            // Combat: a melee hit for 4 on a zombie -> 12
            Zombie z = zombie(level, p.offset(0, 1, -3));
            hit(z, fp, 4f);
            long combat = xp(fp, UseSkill.COMBAT);
            // Archery: an arrow from 16 blocks -> 4 + 16/4 = 8
            Zombie z2 = zombie(level, p.offset(4, 1, -3));
            fp.moveTo(z2.getX() + 16, z2.getY(), z2.getZ());
            Arrow arrow = new Arrow(EntityType.ARROW, level); arrow.setOwner(fp);
            z2.invulnerableTime = 0; z2.hurt(fp.damageSources().arrow(arrow, fp), 3f);
            long archery = xp(fp, UseSkill.ARCHERY);
            // Breeding: a real cow pair bred by the player -> 20
            Cow a = EntityType.COW.create(level), b = EntityType.COW.create(level);
            a.moveTo(p.getX() - 3.5, p.getY() + 1, p.getZ() + 3.5); b.moveTo(p.getX() - 2.5, p.getY() + 1, p.getZ() + 3.5);
            level.addFreshEntity(a); level.addFreshEntity(b);
            UseSkills.creditBreeding(fp, a, b);   // the BabyEntitySpawnEvent path (a FakePlayer can't be a love cause: getLoveCause looks it up in the level's player list)
            long breeding = xp(fp, UseSkill.BREEDING);
            // Exploration: first visit to this biome -> 80
            fp.moveTo(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
            UseSkills.exploreTick(fp);
            long explore = xp(fp, UseSkill.EXPLORATION);
            // Arcana: 10 s in the Rift with instability rising -> 10
            UseSkills.riftTick(fp, 0.01f, 200);
            long arcana = xp(fp, UseSkill.ARCANA);
            // Falconry (hits credited to the owner): 6
            UseSkills.award(fp, UseSkill.FALCONRY, 6, "falcon:" + UUID.randomUUID());
            long falconry = xp(fp, UseSkill.FALCONRY);
            d.append("mining ").append(mining).append(" (Lv ").append(mLv).append(") wood ").append(wood).append(" combat ").append(combat)
             .append(" archery ").append(archery).append(" breeding ").append(breeding).append(" explore ").append(explore)
             .append(" arcana ").append(arcana).append(" falconry ").append(falconry);
            boolean ok = mining == 55 && mLv == 2 && wood == 10 && combat == 12 && archery == 8 && breeding == 20 && explore >= 80 && arcana == 10 && falconry == 6;
            z.discard(); z2.discard(); a.discard(); b.discard();
            level.getEntitiesOfClass(Cow.class, new net.minecraft.world.phys.AABB(p).inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);
            result("useskill_xp_gain", ok, d.toString());
            WorldDropsTests.force(level, p, false);
        });
    }

    private static void noFarm(ServerLevel level) {
        BlockPos p = pad(level, 32600, 32000 + DungeonTests.RUN_OFF, Blocks.STONE);
        WorldDropsTests.force(level, p, true);
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            UseSkills.testResetBudgets();
            FakePlayer fp = Round3Tests.miner(level, "QFFarmer");
            StringBuilder d = new StringBuilder();
            // placed stone (real right-click) -> 0
            int placedOk = 0;
            for (int i = 0; i < 4; i++) { BlockPos q = p.offset(-3, 0, i - 2); if (Round3Tests.placeAsPlayer(fp, q, Items.STONE)) { placedOk++; fp.moveTo(q.getX() + 1.5, q.getY(), q.getZ() + 0.5); fp.gameMode.destroyBlock(q); } }
            long placedXp = xp(fp, UseSkill.MINING);
            // instant-break block (natural short grass on grass) -> 0
            BlockPos gq = p.offset(3, 0, 0); level.setBlock(gq, Blocks.GRASS_BLOCK.defaultBlockState(), 3); level.setBlock(gq.above(), Blocks.SHORT_GRASS.defaultBlockState(), 3);
            fp.gameMode.destroyBlock(gq.above());
            long grassXp = xp(fp, UseSkill.MINING);
            // caged zombie hit 15 times -> only the per-target budget (10 awards)
            Zombie z = zombie(level, p.offset(0, 1, -3));
            for (int i = 0; i < 15; i++) hit(z, fp, 1f);
            long caged = xp(fp, UseSkill.COMBAT);
            // AFK player hitting a fresh zombie -> 0
            Zombie z2 = zombie(level, p.offset(3, 1, -3));
            UseSkills.testSetIdle(fp, UseSkills.AFK_TICKS + 100);
            hit(z2, fp, 4f);
            long afk = xp(fp, UseSkill.COMBAT) - caged;
            UseSkills.testClearIdle(fp);
            // repeat biome visit -> 0
            UseSkills.exploreTick(fp); long e1 = xp(fp, UseSkill.EXPLORATION); UseSkills.exploreTick(fp); long e2 = xp(fp, UseSkill.EXPLORATION);
            // the same cow pair bred again -> 0
            Cow a = EntityType.COW.create(level), b = EntityType.COW.create(level);
            a.moveTo(p.getX() - 3.5, p.getY() + 1, p.getZ() + 3.5); b.moveTo(p.getX() - 2.5, p.getY() + 1, p.getZ() + 3.5);
            level.addFreshEntity(a); level.addFreshEntity(b);
            UseSkills.creditBreeding(fp, a, b);
            long br1 = xp(fp, UseSkill.BREEDING);
            UseSkills.creditBreeding(fp, b, a);
            long br2 = xp(fp, UseSkill.BREEDING);
            // one falcon fighting all hour -> capped; Rift time -> capped
            String falcon = "falcon:" + UUID.randomUUID();
            for (int i = 0; i < 100; i++) UseSkills.award(fp, UseSkill.FALCONRY, 6, falcon);
            long fal = xp(fp, UseSkill.FALCONRY);
            for (int i = 0; i < 100; i++) UseSkills.riftTick(fp, 0.01f, 200);
            long arc = xp(fp, UseSkill.ARCANA);
            d.append("placed stone x").append(placedOk).append(" -> ").append(placedXp).append(" | short grass -> ").append(grassXp)
             .append(" | caged zombie 15 hits -> ").append(caged).append(" (cap ").append(UseSkills.COMBAT_AWARDS_PER_TARGET).append(" awards x3)")
             .append(" | AFK hit -> ").append(afk).append(" | biome visit ").append(e1).append(" then ").append(e2)
             .append(" | same pair bred twice ").append(br1).append(" -> ").append(br2)
             .append(" | one falcon 100 hits -> ").append(fal).append(" (cap ").append(UseSkills.FALCON_XP_PER_HOUR).append(") | rift 100 ticks -> ").append(arc)
             .append(" (cap ").append(UseSkills.ARCANA_XP_PER_HOUR).append(')');
            boolean ok = placedOk == 4 && placedXp == 0 && grassXp == 0 && caged == 3L * UseSkills.COMBAT_AWARDS_PER_TARGET && afk == 0 && e1 >= 80 && e2 == e1
                && br1 == 20 && br2 == 20 && fal == UseSkills.FALCON_XP_PER_HOUR && arc == UseSkills.ARCANA_XP_PER_HOUR;
            z.discard(); z2.discard();
            level.getEntitiesOfClass(Cow.class, new net.minecraft.world.phys.AABB(p).inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);
            result("useskill_no_farm", ok, d.toString());
            WorldDropsTests.force(level, p, false);
        });
    }

    private static void persistence(ServerLevel level) {
        FakePlayer fp = AuditTests.freshPlayer(level, "QFMortal");
        UseSkills.setXp(fp, UseSkill.MINING, UseSkill.totalFor(17) + 5);
        UseSkills.setXp(fp, UseSkill.ARCANA, UseSkill.totalFor(30));
        UseSkills.award(fp, UseSkill.EXPLORATION, 10, "first:test.persist");
        FakePlayer reborn = CheeseTests.respawned(fp);   // death without keepInventory: copyOnDeath restores the attachment
        int m1 = UseSkills.level(reborn, UseSkill.MINING), a1 = UseSkills.level(reborn, UseSkill.ARCANA);
        boolean repeatBlocked = UseSkills.award(reborn, UseSkill.EXPLORATION, 10, "first:test.persist") == UseSkills.Gate.REPEAT;
        // relog / restart: the player file round-trip (saveWithoutId -> load into a new player object)
        CompoundTag file = new CompoundTag(); reborn.saveWithoutId(file);
        FakePlayer loaded = new FakePlayer(level, reborn.getGameProfile());
        loaded.load(file);
        int m2 = UseSkills.level(loaded, UseSkill.MINING), a2 = UseSkills.level(loaded, UseSkill.ARCANA);
        long e2 = UseSkills.xp(loaded, UseSkill.EXPLORATION);
        boolean copyOnDeath;
        try { var f = net.neoforged.neoforge.attachment.AttachmentType.class.getDeclaredField("copyOnDeath"); f.setAccessible(true); copyOnDeath = f.getBoolean(UseSkills.DATA.get()); }
        catch (ReflectiveOperationException ex) { copyOnDeath = false; }
        boolean ok = m1 == 17 && a1 == 30 && repeatBlocked && m2 == 17 && a2 == 30 && e2 == 10 && copyOnDeath;
        result("useskill_persistence", ok, "after death: Mining " + m1 + " Arcana " + a1 + " (first-time list kept=" + repeatBlocked + ") | after save/load: Mining " + m2
            + " Arcana " + a2 + " Exploration xp " + e2 + " | copyOnDeath=" + copyOnDeath);
    }

    private static void bonusPoints(ServerLevel level) {
        FakePlayer fp = AuditTests.freshPlayer(level, "QFGrinderPro");
        int p0 = PlayerSkillData.get(fp).getAvailableSkillPoints();
        StringBuilder d = new StringBuilder();
        for (UseSkill s : new UseSkill[]{UseSkill.MINING, UseSkill.COMBAT, UseSkill.WOODCUTTING}) {
            UseSkills.award(fp, s, UseSkill.totalFor(25), null);
            int after25 = PlayerSkillData.get(fp).getAvailableSkillPoints() - p0;
            UseSkills.award(fp, s, UseSkill.totalFor(50), null);
            int after50 = PlayerSkillData.get(fp).getAvailableSkillPoints() - p0;
            d.append(s.label).append(": Lv ").append(UseSkills.level(fp, s)).append(" points +").append(after25).append("/+").append(after50).append(" | ");
        }
        int gained = PlayerSkillData.get(fp).getAvailableSkillPoints() - p0;
        int maxTree = 24 + UseSkills.BONUS_POINTS_CAP;
        boolean ok = gained == UseSkills.BONUS_POINTS_CAP && UseSkills.bonusPointsGiven(fp) == UseSkills.BONUS_POINTS_CAP;
        result("useskill_bonus_points", ok, d + "total +" + gained + " (cap " + UseSkills.BONUS_POINTS_CAP + "; tree economy max " + maxTree + ")");
    }

    private static void neverRestricts(ServerLevel level) {
        BlockPos p = pad(level, 32800, 32000 + DungeonTests.RUN_OFF, Blocks.STONE);
        WorldDropsTests.force(level, p, true);
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            UseSkills.testResetBudgets();
            FakePlayer fp = AuditTests.freshPlayer(level, "QFNewbie");
            fp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            boolean allLevel1 = Arrays.stream(UseSkill.values()).allMatch(s -> UseSkills.level(fp, s) == 1);
            // every armour slot takes netherite; netherite sword and pickaxe work
            fp.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET)); fp.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
            fp.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS)); fp.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
            boolean worn = fp.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET) && fp.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE)
                && fp.getItemBySlot(EquipmentSlot.LEGS).is(Items.NETHERITE_LEGGINGS) && fp.getItemBySlot(EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS);
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
            float s1 = fp.getDestroySpeed(Blocks.STONE.defaultBlockState());
            BlockPos q = p.offset(1, -1, 0); fp.moveTo(q.getX() + 1.5, q.getY() + 1, q.getZ() + 0.5);
            boolean mined = fp.gameMode.destroyBlock(q);
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));
            Zombie z = zombie(level, p.offset(0, 1, -3));
            hit(z, fp, 6f); float d1 = z.getMaxHealth() - z.getHealth();
            // the same player at level 50: the bonus applies, nothing else changes
            UseSkills.setXp(fp, UseSkill.MINING, UseSkill.totalFor(50)); UseSkills.setXp(fp, UseSkill.COMBAT, UseSkill.totalFor(50));
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
            float s50 = fp.getDestroySpeed(Blocks.STONE.defaultBlockState());
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));
            hit(z, fp, 6f); float d50 = z.getMaxHealth() - z.getHealth();
            z.discard();
            boolean ok = allLevel1 && worn && mined && d1 > 0 && Math.abs(s50 / s1 - 1.25f) < 0.01f && d50 > d1;
            result("useskill_never_restricts", ok, "level-1 player: all skills Lv1=" + allLevel1 + " netherite armour worn=" + worn + " mined stone=" + mined
                + " sword hit=" + String.format(Locale.ROOT, "%.2f", d1) + " | at Lv50: break speed x" + String.format(Locale.ROOT, "%.3f", s50 / s1) + " hit " + String.format(Locale.ROOT, "%.2f", d50)
                + " | Breeding Lv50 egg bonus +" + UseSkill.BREEDING.primary(50) + " (an input to the pity math, capped)");
            WorldDropsTests.force(level, p, false);
        });
    }
}
