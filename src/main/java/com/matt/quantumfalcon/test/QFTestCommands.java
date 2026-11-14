package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.arena.BossSummoner;
import com.matt.quantumfalcon.blockentity.FalconEggBlockEntity;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.entity.boss.EntangledTwinsEntity;
import com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity;
import com.matt.quantumfalcon.item.RiftCompassItem;
import com.matt.quantumfalcon.registry.ModBlocks;
import com.matt.quantumfalcon.registry.ModEntities;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.registry.ModGearItems;
import com.matt.quantumfalcon.skill.PlayerSkillData;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import com.matt.quantumfalcon.worldgen.structure.ArenaMechanics;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.function.Consumer;

/**
 * In-game automated test suite (server-side, works headless over RCON):
 *   /qftest run all | <test>      /qftest results      /qftest list
 * Every result is logged as "QFTEST <name> PASS|FAIL <detail>" so CI can grep the server log.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class QFTestCommands {
    private QFTestCommands() {}

    public record Result(String name, boolean pass, String detail) {}
    private static final List<Result> RESULTS = new ArrayList<>();
    private record Task(long due, Runnable run) {}
    private static final List<Task> TASKS = new ArrayList<>();
    private static final Deque<String> QUEUE = new ArrayDeque<>();
    private static boolean busy = false;
    private static long now = 0;
    private static MinecraftServer server;

    private static final UUID TESTER_ID = UUID.fromString("51f0c0de-0000-4a1c-9e57-0000000000qf".replace("qf", "01"));
    private static final ResourceKey<Level> ECHO_RIFT = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "echo_rift"));

    private static final LinkedHashMap<String, Consumer<ServerLevel>> TESTS = new LinkedHashMap<>();
    static {
        TESTS.put("tame", QFTestCommands::testTame);
        TESTS.put("feed", QFTestCommands::testFeed);
        TESTS.put("breed_egg_hatch", l -> warmThen(l, 3000, 3040, 3000, 3040, () -> testBreedEggHatch(l)));
        TESTS.put("perch", QFTestCommands::testPerch);
        TESTS.put("stamina_quantum_perch", QFTestCommands::testStamina);
        TESTS.put("berry_boost", QFTestCommands::testBerry);
        TESTS.put("echo_stone_fall", QFTestCommands::testEchoStone);
        TESTS.put("void_stone_recipes", QFTestCommands::testVoidStone);
        TESTS.put("portal_round_trip", QFTestCommands::testPortal);
        TESTS.put("compass", QFTestCommands::testCompass);
        TESTS.put("boss_overseer", l -> testBoss(l, ArenaType.QUANTUM_GUARDIAN, 0));
        TESTS.put("boss_warden", l -> testBoss(l, ArenaType.VOID_HORROR, 1));
        TESTS.put("boss_twins", l -> testBoss(l, ArenaType.ENTANGLED_TWINS, 2));
        TESTS.put("boss_sovereign", l -> testBoss(l, ArenaType.SINGULARITY_SOVEREIGN, 3));
        TESTS.put("progression", QFTestCommands::testProgression);
        TESTS.put("vanilla_loot_kept", QFTestCommands::testVanillaLoot);
        TESTS.put("arena_overseer", l -> testArena(l, ArenaType.QUANTUM_GUARDIAN, 0));
        TESTS.put("arena_warden", l -> testArena(l, ArenaType.VOID_HORROR, 1));
        TESTS.put("arena_twins", l -> testArena(l, ArenaType.ENTANGLED_TWINS, 2));
        TESTS.put("arena_sovereign", l -> testArena(l, ArenaType.SINGULARITY_SOVEREIGN, 3));
        TESTS.put("sha_sha", QFTestCommands::testShaSha);
        TESTS.put("mob_bodies", QFTestCommands::testMobBodies);
        TESTS.put("falcon_abilities", l -> warmThen(l, 3400, 3400, 3400 + 17 * 24, 3400, () -> testFalconAbilities(l)));
        TESTS.put("falcon_passives", l -> warmThen(l, 3400, 3460, 3400, 3460, () -> testFalconPassives(l)));
        TESTS.put("falcon_ability_hooks", l -> warmThen(l, 3400, 3500, 3400, 3500, () -> testFalconAbilityHooks(l)));
        TESTS.put("falcon_genetics", QFTestCommands::testFalconGenetics);
        TESTS.put("falcon_barding", QFTestCommands::testFalconBarding);
        TESTS.put("weapons", l -> warmThen(l, 3600, 3700, 3600 + 13 * 24, 3700, () -> testWeapons(l)));
        TESTS.put("army_entities", l -> warmThen(l, 3800, 3900, 3800 + 6 * 24, 3900, () -> testArmyEntities(l)));
        TESTS.put("item_icons", QFTestCommands::testItemIcons);
        TESTS.put("visual_assets", QFTestCommands::testVisualAssets);
        TESTS.put("falcon_archetypes", QFTestCommands::testFalconArchetypes);
        TESTS.put("boss_presentation", QFTestCommands::testBossPresentation);
        TESTS.put("weapon_fx", QFTestCommands::testWeaponFx);
        TESTS.put("breeding_engine", QFTestCommands::testBreedingEngine);
        TESTS.put("egg_loot_caches", QFTestCommands::testEggLoot);
        TESTS.put("incubator_menu", QFTestCommands::testIncubator);
        TESTS.put("danger_tiers", QFTestCommands::testDangerTiers);
        TESTS.put("nest_incubation", l -> warmThen(l, 4000, 4000, 4000, 4000, () -> testNestIncubation(l)));
        TESTS.put("nest_laying_mutation", l -> warmThen(l, 4040, 4000, 4040, 4000, () -> testNestLaying(l)));
        TESTS.put("wild_nest_feature", l -> warmThen(l, 4080, 4000, 4080, 4000, () -> testWildNest(l)));
        TESTS.put("realm_blocks", QFTestCommands::testRealmBlocks);
        TESTS.put("armor_sets", l -> warmThen(l, 3600, 3760, 3600, 3760, () -> testArmorSets(l)));
        DungeonTests.register(TESTS);
        BossFightTests.register(TESTS);
        MovesetTests.register(TESTS);
        BossCreditTests.register(TESTS);
        SkillTreeTests.register(TESTS);
        ArenaDistanceTests.register(TESTS);
        AuditTests.register(TESTS);
        Pre10FixTests.register(TESTS);
        TMTests.register(TESTS);
        WorldDropsTests.register(TESTS);
        CheeseTests.register(TESTS);
        Round3Tests.register(TESTS);
        DungeonWardTests.register(TESTS);
        SignatureLootTests.register(TESTS);
        UseSkillsTests.register(TESTS);
        RiftCodexTests.register(TESTS);
        RestartFightTests.register(TESTS);
        ThenNowTests.register(TESTS);
        EastwatchTests.register(TESTS);
        UtilityStructureTests.register(TESTS);
        VillagerTests.register(TESTS);
        BKTests.register(TESTS);
        StoryTests.register(TESTS);
        FirstBuildsTests.register(TESTS);
        EconomyTests.register(TESTS);
    }

    // ------------------------------------------------------------------ plumbing
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("qftest").requires(s -> s.hasPermission(2))
            .then(Commands.literal("list").executes(c -> { c.getSource().sendSuccess(() -> Component.literal(String.join(", ", TESTS.keySet())), false); return 1; }))
            .then(Commands.literal("fx").executes(c -> {   // screenshot helper: every falcon within 24 blocks uses its active ability for the caller
                var pl = c.getSource().getPlayerOrException();
                int n = 0;
                for (QuantumFalconEntity f : pl.serverLevel().getEntitiesOfClass(QuantumFalconEntity.class, pl.getBoundingBox().inflate(24))) {
                    if (!f.isTame()) { f.setTame(true, false); f.setOwnerUUID(pl.getUUID()); }
                    f.setStamina(QuantumFalconEntity.MAX_STAMINA); f.setAbilityCooldown(0);
                    if (com.matt.quantumfalcon.entity.ability.FalconAbilities.activate(f, pl).ok()) n++;
                    f.triggerAbilityAnimation();
                }
                final int fn = n;
                c.getSource().sendSuccess(() -> Component.literal("fx: " + fn + " abilities fired"), false);
                return n; }))
            .then(Commands.literal("fxdemo").executes(c -> {   // screenshot helper: weapon/ability FX in front of the caller
                var pl = c.getSource().getPlayerOrException();
                var W = com.matt.quantumfalcon.item.weapon.WeaponFx.class;
                net.minecraft.world.phys.Vec3 look = pl.getLookAngle().multiply(1, 0, 1).normalize(), side = look.yRot((float) Math.PI / 2);
                net.minecraft.world.phys.Vec3 base = pl.position().add(look.scale(5));
                com.matt.quantumfalcon.item.weapon.WeaponFx.slash(pl, 0xFF60E0, 14);
                com.matt.quantumfalcon.item.weapon.WeaponFx.shock(pl, base.add(side.scale(-2.5)), 0xFF5A2A, 2.5);
                com.matt.quantumfalcon.item.weapon.WeaponFx.runeCircle(pl, base.add(side.scale(2.5)), 0xFFD36A, 1.8);
                com.matt.quantumfalcon.item.weapon.WeaponFx.trail(pl, base.add(side.scale(-4)).add(0, 2.6, 2), base.add(side.scale(4)).add(0, 2.6, 2), 0x43E6FF);
                com.matt.quantumfalcon.item.weapon.WeaponFx.glints(pl, base.add(0, 1.2, 0), 0xB05CFF, 30, 0.6);
                c.getSource().sendSuccess(() -> Component.literal("fxdemo"), false);
                return 1; }))
            .then(Commands.literal("pedigree").executes(c -> {   // screenshot helper: a 3rd-generation hybrid's pedigree page
                    var sp = c.getSource().getPlayerOrException();
                    var r = net.minecraft.util.RandomSource.create(11);
                    var E = new com.matt.quantumfalcon.breeding.BreedingEngine.Context();
                    var a = com.matt.quantumfalcon.breeding.BreedingEngine.breed(com.matt.quantumfalcon.breeding.BreedingEngine.wild("coral", 3, r), com.matt.quantumfalcon.breeding.BreedingEngine.wild("stormrider", 4, r), E, r).child;
                    var b = com.matt.quantumfalcon.breeding.BreedingEngine.breed(com.matt.quantumfalcon.breeding.BreedingEngine.wild("tempest", 4, r), com.matt.quantumfalcon.breeding.BreedingEngine.wild("abyssal", 4, r), E, r).child;
                    a.name = "Squall"; b.name = "Undertow";
                    var child = com.matt.quantumfalcon.breeding.BreedingEngine.breed(a, b, E, r).child;
                    child.variant = "tidecaller"; child.tier = Math.max(child.tier, 5);
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new com.matt.quantumfalcon.network.PedigreePayload(child.save(), "falcon", "Riptide", 3));
                    return 1; }))
            .then(Commands.literal("giveeggs").executes(c -> {   // screenshot helper: one wild egg per tier into the hotbar
                    var sp = c.getSource().getPlayerOrException();
                    var r = net.minecraft.util.RandomSource.create(5);
                    String[] v = {"peregrine", "tundra", "coral", "stormrider", "aetherwing", "abyssal", "aurora_seraph"};
                    for (int t = 1; t <= 7; t++) {
                        var g = com.matt.quantumfalcon.breeding.BreedingEngine.wild(v[t - 1], t, r);
                        sp.getInventory().setItem(t - 1, com.matt.quantumfalcon.item.FalconEggItem.wild(new net.minecraft.world.item.ItemStack(ModItems.FALCON_EGG.get()), g));
                    }
                    return 1; }))
            .then(Commands.literal("danger").then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 7))
                .then(Commands.argument("area", StringArgumentType.greedyString()).executes(c -> {   // screenshot helper: show the danger banner
                    var sp = c.getSource().getPlayerOrException();
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new com.matt.quantumfalcon.network.DangerTierPayload(
                        StringArgumentType.getString(c, "area"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "tier")));
                    return 1; }))))
            .then(Commands.literal("falconkill").executes(c -> {   // screenshot helper: stage a frozen Overseer with the player's falcon beside it
                    var sp = c.getSource().getPlayerOrException();
                    ServerLevel lv = sp.serverLevel();
                    BlockPos site = BossFightTests.site(lv, 21000, 21000);
                    cleanup(lv, site, 120);
                    net.minecraft.world.entity.Mob boss = com.matt.quantumfalcon.arena.BossSummoner.buildAndSummon(lv, site,
                        com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType.QUANTUM_GUARDIAN);
                    if (boss == null) return 0;
                    boss.setNoAi(true);
                    QuantumFalconEntity f = falcon(lv, boss.blockPosition().offset(4, 1, 2), true);
                    f.setYRot(-65f); f.setYHeadRot(-65f); f.yBodyRot = -65f;
                    f.tame(sp);
                    f.setNoGravity(true);
                    STAGED_BOSS = boss; STAGED_FALCON = f;
                    sp.teleportTo(lv, boss.getX() + 9.5, boss.getY() + 2, boss.getZ() + 4.5, 115f, 8f);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal("[qftest] Overseer frozen, your falcon beside it. You have not hit it.").withStyle(net.minecraft.ChatFormatting.AQUA), false);
                    return 1; })
                .then(Commands.literal("strike").executes(c -> {   // ...and the falcon lands the killing blow
                    var sp = c.getSource().getPlayerOrException();
                    var boss = STAGED_BOSS; var f = STAGED_FALCON;
                    if (boss == null || f == null || !boss.isAlive()) return 0;
                    f.swing(InteractionHand.MAIN_HAND);
                    boss.setHealth(1f); boss.invulnerableTime = 0;
                    boolean killed = boss.hurt(f.damageSources().mobAttack(f), 500f) && !boss.isAlive();
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal(killed
                        ? "[qftest] Your falcon landed the killing blow. You never hit the Overseer and were still credited."
                        : "[qftest] falcon kill failed").withStyle(net.minecraft.ChatFormatting.AQUA), false);
                    STAGED_BOSS = null;
                    return killed ? 1 : 0; })))
            .then(Commands.literal("clipstand").then(Commands.argument("dist", com.mojang.brigadier.arguments.IntegerArgumentType.integer(4, 40)).executes(c -> {
                    // clip helper: stand on solid arena floor about <dist> blocks from the nearest boss, facing it
                    var sp = c.getSource().getPlayerOrException();
                    ServerLevel lv = sp.serverLevel();
                    int want = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "dist");
                    // filming only: stay a valid boss target (survival) but take no damage, so the clip never cuts to a death screen
                    sp.getAbilities().invulnerable = true; sp.onUpdateAbilities(); sp.setHealth(sp.getMaxHealth());
                    var bosses = lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, sp.getBoundingBox().inflate(120), m -> m instanceof com.matt.quantumfalcon.entity.boss.QFBoss && m.isAlive());
                    if (bosses.isEmpty()) return 0;
                    var boss = bosses.get(0);
                    var arena = com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.arenaNear(boss.blockPosition(), 120);
                    BlockPos ref = arena != null ? arena.center : boss.blockPosition();
                    for (int r = want; r <= want + 24; r++) for (int k = 0; k < 24; k++) {
                        double ang = k * Math.PI / 12;
                        int x = (int) Math.floor(boss.getX() + Math.cos(ang) * r), z = (int) Math.floor(boss.getZ() + Math.sin(ang) * r);
                        BlockPos fl = com.matt.quantumfalcon.arena.fight.ArenaInteraction.floorAt(lv, x, ref.getY(), z);
                        if (fl == null) continue;
                        double dx = boss.getX() - (x + 0.5), dz = boss.getZ() - (z + 0.5);
                        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                        sp.teleportTo(lv, x + 0.5, fl.getY() + 1, z + 0.5, yaw, 5f);
                        return r;
                    }
                    return 0; })))
            .then(Commands.literal("playerstate").executes(c -> {   // helper: health, deaths, instability, zone and safe time (logged + chat)
                    var sp = c.getSource().getPlayerOrException();
                    var cap = com.matt.quantumfalcon.capability.PlayerInstabilityCapability.get(sp);
                    int deaths = sp.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.DEATHS));
                    String msg = "[qftest] playerstate health=" + String.format("%.1f", sp.getHealth()) + "/" + (int) sp.getMaxHealth() + " deaths=" + deaths
                        + " mode=" + sp.gameMode.getGameModeForPlayer().getName() + " instability=" + String.format("%.0f", cap.getInstability()) + "% zone="
                        + com.matt.quantumfalcon.dimension.InstabilityEffects.zoneTier(sp) + " safeSeconds=" + com.matt.quantumfalcon.dimension.InstabilityEffects.safeSeconds(sp, com.matt.quantumfalcon.dimension.InstabilityEffects.zoneTier(sp))
                        + " overseerDefeated=" + com.matt.quantumfalcon.story.PlayerStoryData.get(sp).hasDefeatedBoss("QUANTUM_OVERSEER");
                    QuantumFalcon.LOGGER.info(msg);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal(msg).withStyle(net.minecraft.ChatFormatting.YELLOW), false);
                    return 1; }))
            .then(Commands.literal("bosscount").executes(c -> {   // screenshot helper: Overseers near the player and the arena's owned boss
                    var sp = c.getSource().getPlayerOrException();
                    ServerLevel lv = sp.serverLevel();
                    int n = lv.getEntities(com.matt.quantumfalcon.registry.ModEntities.QUANTUM_OVERSEER.get(), sp.getBoundingBox().inflate(256), ov -> ov.isAlive()).size();
                    var arena = com.matt.quantumfalcon.worldgen.structure.ArenaMechanics.arenaNear(sp.blockPosition(), 256);
                    String id = arena == null || arena.bossId == null ? "none" : arena.bossId.toString().substring(0, 8);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal("[qftest] Overseers within 256 blocks: " + n + "  (arena boss " + id + ")")
                        .withStyle(net.minecraft.ChatFormatting.YELLOW), false);
                    return n; }))
            .then(Commands.literal("openmenu").then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos()).executes(c -> {   // screenshot helper: open a block's menu as if used
                    var sp = c.getSource().getPlayerOrException();
                    BlockPos at = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(c, "pos");
                    if (!(c.getSource().getLevel().getBlockEntity(at) instanceof net.minecraft.world.MenuProvider mp)) return 0;
                    sp.openMenu(mp, at);
                    return 1; })))
            .then(Commands.literal("useskills")   // A9 screenshot/admin helper: set a skill's level, or open the Skills page
                .then(Commands.literal("open").executes(c -> { com.matt.quantumfalcon.skills.UseSkills.sync(c.getSource().getPlayerOrException(), -1, 0, false, true); return 1; }))
                .then(Commands.literal("toast").then(Commands.argument("skill", StringArgumentType.word()).executes(c -> {
                    var sp = c.getSource().getPlayerOrException();
                    var s = com.matt.quantumfalcon.skills.UseSkill.valueOf(StringArgumentType.getString(c, "skill").toUpperCase(java.util.Locale.ROOT));
                    com.matt.quantumfalcon.skills.UseSkills.sync(sp, s.ordinal(), com.matt.quantumfalcon.skills.UseSkills.level(sp, s), true, false); return 1; })))
                .then(Commands.literal("set").then(Commands.argument("skill", StringArgumentType.word()).then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 50)).executes(c -> {
                    var sp = c.getSource().getPlayerOrException();
                    var s = com.matt.quantumfalcon.skills.UseSkill.valueOf(StringArgumentType.getString(c, "skill").toUpperCase(java.util.Locale.ROOT));
                    int lv = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "level");
                    long xp = com.matt.quantumfalcon.skills.UseSkill.totalFor(lv) + com.matt.quantumfalcon.skills.UseSkill.xpForNext(lv) * (lv * 37 % 90) / 100;
                    com.matt.quantumfalcon.skills.UseSkills.setXp(sp, s, xp);
                    com.matt.quantumfalcon.skills.UseSkills.sync(sp, -1, 0, false, false); return 1; })))))
            .then(Commands.literal("story").then(Commands.argument("pieces", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-1, 4))
                .then(Commands.argument("ending", StringArgumentType.word()).executes(c -> {   // screenshot helper: -1 = brand-new story; else N bosses down + ending (none|LIGHT|DARK|TRUE)
                    var sp = c.getSource().getPlayerOrException();
                    var d = com.matt.quantumfalcon.story.PlayerStoryData.get(sp);
                    int n = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "pieces");
                    d.readTag(new net.minecraft.nbt.CompoundTag());
                    String[] order = {"QUANTUM_OVERSEER", "RIFT_WARDEN", "ENTANGLED_TWINS", "SINGULARITY_SOVEREIGN"};
                    for (int i = 0; i < n; i++) d.markBossDefeated(order[i]);
                    if (n >= 4) d.setSovereignDefeated(true);
                    if (n >= 0) d.markChapterSeen(com.matt.quantumfalcon.story.StorylineManager.StoryChapter.CHAPTER_1);
                    String endName = StringArgumentType.getString(c, "ending");
                    d.setChosenEnding(endName.equals("none") ? null : endName);
                    sp.refreshTabListName();
                    return 1; })))
                .then(Commands.literal("exportarenas").executes(c -> {   // dev: rewrite the arena snapshot JSONs from ArenaDesign
                    try { int n = ArenaExport.exportAll(java.nio.file.Path.of(System.getProperty("user.dir"), "qf_arena_export"));
                        c.getSource().sendSuccess(() -> Component.literal("QFTEST exported " + n + " arenas"), false); return n; }
                    catch (Exception ex) { c.getSource().sendFailure(Component.literal("export failed: " + ex)); return 0; } }))
                .then(Commands.literal("goalcard").executes(c -> {   // screenshot helper: replay the first-tame goal card
                    com.matt.quantumfalcon.story.StorylineManager.triggerStoryEvent(c.getSource().getPlayerOrException(), com.matt.quantumfalcon.story.StorylineManager.StoryChapter.CHAPTER_1);
                    return 1; })))
            .then(Commands.literal("codex").then(Commands.argument("chapter", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 12))
                .then(Commands.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 99)).executes(c -> {   // screenshot helper: open the Codex at a page
                    var sp = c.getSource().getPlayerOrException();
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new com.matt.quantumfalcon.network.CodexPayload(
                        codexFound(sp),
                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "chapter"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "page")));
                    return 1; }))))
            .then(Commands.literal("egg").then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                .then(Commands.argument("variant", StringArgumentType.word()).then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 7))
                .then(Commands.argument("stage", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 4)).executes(c -> {   // screenshot helper: egg of a given variant/tier/stage
                    ServerLevel lvl = c.getSource().getLevel();
                    BlockPos at = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(c, "pos");
                    String v = StringArgumentType.getString(c, "variant");
                    int tier = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "tier"), st = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "stage");
                    lvl.setBlock(at, ModBlocks.FALCON_EGG_BLOCK.get().defaultBlockState(), 3);
                    if (!(lvl.getBlockEntity(at) instanceof FalconEggBlockEntity be)) { c.getSource().sendFailure(Component.literal("no egg block entity")); return 0; }
                    be.setWildChild(com.matt.quantumfalcon.breeding.BreedingEngine.wild(v, tier, lvl.getRandom()));
                    be.setProgressForTest(new float[]{0f, 0.2f, 0.5f, 0.8f, 0.95f}[st]);
                    be.freezeForTest();
                    return 1;
                }))))))
            .then(Commands.literal("results").executes(c -> { c.getSource().sendSuccess(() -> Component.literal(summary()), false); return 1; }))
            .then(Commands.literal("run").then(Commands.argument("name", StringArgumentType.word())
                .suggests((c, b) -> { b.suggest("all"); b.suggest("restart_phase2"); TESTS.keySet().forEach(b::suggest); return b.buildFuture(); })
                .executes(c -> run(c.getSource(), StringArgumentType.getString(c, "name"))))));
    }

    private static int run(CommandSourceStack src, String name) {
        server = src.getServer();
        // "all" runs every test, ending with the first half of each real-restart pair (restart_*_a / restart_a); after the
        // server restarts, "restart_phase2" runs the second halves (tools/run_suite.sh does both).
        if (name.equals("all")) {
            RESULTS.clear();
            TESTS.keySet().stream().filter(n -> !n.startsWith("restart_") && !OWN_WORLD.contains(n)).forEach(QUEUE::add);
            TESTS.keySet().stream().filter(QFTestCommands::restartFirstHalf).forEach(QUEUE::add);
        }
        else if (name.equals("restart_phase2")) { RESULTS.clear(); TESTS.keySet().stream().filter(QFTestCommands::restartSecondHalf).forEach(QUEUE::add); }
        else if (TESTS.containsKey(name)) QUEUE.add(name);
        else { src.sendFailure(Component.literal("Unknown test " + name)); return 0; }
        // say what the whole suite is, so the count matches the results
        long second = TESTS.keySet().stream().filter(QFTestCommands::restartSecondHalf).count();
        String extra = name.equals("all") ? " (incl. " + TESTS.keySet().stream().filter(QFTestCommands::restartFirstHalf).count() + " restart first halves; "
            + second + " second halves run after a real restart via restart_phase2, total " + (QUEUE.size() + second) + "; not queued, they need their own world: " + OWN_WORLD + ")" : "";
        src.sendSuccess(() -> Component.literal("Queued " + QUEUE.size() + " test(s)" + extra + "; poll /qftest results"), false);
        return 1;
    }

    /** Tests that need a fresh normal world (QF_WORLD/QF_SEED/QF_LEVELTYPE), not the suite's superflat one. */
    static final java.util.Set<String> OWN_WORLD = java.util.Set.of("eastwatch_natural", "utility_natural");
    static boolean restartFirstHalf(String n) { return n.startsWith("restart_") && n.endsWith("_a") || n.equals("restart_a"); }
    static boolean restartSecondHalf(String n) { return n.startsWith("restart_") && n.endsWith("_b") || n.equals("restart_b"); }

    public static String summary() {
        long pass = RESULTS.stream().filter(Result::pass).count();
        StringBuilder sb = new StringBuilder("QFTEST SUMMARY " + pass + "/" + RESULTS.size() + " passed" + (busy || !QUEUE.isEmpty() ? " (running)" : "") + "\n");
        for (Result r : RESULTS) sb.append(r.pass ? "PASS " : "FAIL ").append(r.name).append(" - ").append(r.detail).append('\n');
        return sb.toString();
    }

    static void later(int ticks, Runnable r) { TASKS.add(new Task(now + ticks, r)); }

    /** Environment-gated test that can't run on this world (e.g. a real-terrain test on the flat suite world): logged as SKIP, not failed. */
    static void skip(String name, String why) {
        RESULTS.add(new Result(name, true, "SKIP: " + why));
        QuantumFalcon.LOGGER.info("QFTEST {} SKIP {}", name, why);
        busy = false;
    }

    static boolean flat(ServerLevel level) {
        return level.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource;
    }

    static void result(String name, boolean pass, String detail) {
        RESULTS.add(new Result(name, pass, detail));
        QuantumFalcon.LOGGER.info("QFTEST {} {} {}", name, pass ? "PASS" : "FAIL", detail);
        busy = false;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        now++;
        if (!TASKS.isEmpty()) {
            List<Task> due = new ArrayList<>();
            TASKS.removeIf(t -> { if (t.due <= now) { due.add(t); return true; } return false; });
            for (Task t : due) {
                try { t.run.run(); } catch (Throwable ex) { QuantumFalcon.LOGGER.error("QFTEST task crashed", ex); result("crash", false, ex.toString()); }
            }
        }
        if (!busy && !QUEUE.isEmpty() && server != null) {
            String name = QUEUE.poll();
            busy = true;
            try { TESTS.get(name).accept(server.overworld()); }
            catch (Throwable ex) { QuantumFalcon.LOGGER.error("QFTEST " + name + " crashed", ex); result(name, false, "exception: " + ex); }
        }
    }

    // ------------------------------------------------------------------ helpers
    static java.util.List<String> codexFound(net.minecraft.server.level.ServerPlayer sp) {
        var l = new java.util.ArrayList<>(com.matt.quantumfalcon.breeding.BreedingService.discovered(sp));
        l.addAll(com.matt.quantumfalcon.codex.RiftCodex.codexLines(sp));
        return l;
    }
    static FakePlayer tester(ServerLevel level) {
        FakePlayer fp = FakePlayerFactory.get(level, new GameProfile(TESTER_ID, "QFTester"));
        fp.getInventory().clearContent();
        // the harness tester fights any arena in any order (the gate itself is covered by audit_bo1_boss_gating)
        var story = com.matt.quantumfalcon.story.PlayerStoryData.get(fp);
        for (String b : new String[]{"QUANTUM_OVERSEER", "RIFT_WARDEN", "ENTANGLED_TWINS"}) story.markBossDefeated(b);
        return fp;
    }

    /** A flat 9x9 test pad at (x, groundY, z); returns the block position above the pad centre. */
    static BlockPos pad(ServerLevel level, int x, int z, net.minecraft.world.level.block.Block floor) {
        level.setChunkForced(x >> 4, z >> 4, true);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            level.setBlock(new BlockPos(x + dx, y - 1, z + dz), floor.defaultBlockState(), 3);
            for (int dy = 0; dy < 6; dy++) level.setBlock(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState(), 3);
        }
        return new BlockPos(x, y, z);
    }

    private static net.minecraft.world.entity.Mob STAGED_BOSS;
    private static QuantumFalconEntity STAGED_FALCON;

    static QuantumFalconEntity falcon(ServerLevel level, BlockPos at, boolean noAi) {
        QuantumFalconEntity f = ModEntities.QUANTUM_FALCON.get().create(level);
        f.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        f.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.COMMAND, null);
        f.setNoAi(noAi);
        f.setPersistenceRequired();
        level.addFreshEntity(f);
        return f;
    }

    static void interact(FakePlayer fp, Entity target, Item item, int count) {
        fp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, count));
        fp.moveTo(target.getX() + 1, target.getY(), target.getZ());
        fp.interactOn(target, InteractionHand.MAIN_HAND);
    }

    static void cleanup(ServerLevel level, BlockPos c, int r) {
        for (Entity e : level.getEntities((Entity) null, new AABB(c).inflate(r), e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
    }

    // ------------------------------------------------------------------ falcon loop
    private static void testTame(ServerLevel level) {
        BlockPos p = pad(level, 3000, 3000, Blocks.GRASS_BLOCK);
        cleanup(level, p, 8);
        FakePlayer fp = tester(level);
        QuantumFalconEntity f = falcon(level, p, true);
        int tries = 0;
        while (!f.isTame() && tries < 60) { interact(fp, f, Items.SALMON, 1); tries++; }
        boolean ok = f.isTame() && TESTER_ID.equals(f.getOwnerUUID());
        result("tame", ok, ok ? "tamed after " + tries + " salmon" : "not tamed after " + tries + " tries");
        f.discard();
    }

    private static void testFeed(ServerLevel level) {
        BlockPos p = pad(level, 3000, 3020, Blocks.GRASS_BLOCK);
        cleanup(level, p, 8);
        FakePlayer fp = tester(level);
        QuantumFalconEntity f = falcon(level, p, true);
        f.tame(fp);
        f.setHealth(f.getMaxHealth() - 10);
        float before = f.getHealth();
        interact(fp, f, Items.COD, 1);
        float after = f.getHealth();
        result("feed", after > before, "health " + before + " -> " + after);
        f.discard();
    }

    private static void testBreedEggHatch(ServerLevel level) {
        BlockPos p0 = pad(level, 3000, 3040, Blocks.GRASS_BLOCK);
        // entities are only visible to the count below once the pad's chunks are entity-ticking (flaky under suite load)
        WorldDropsTests.force(level, p0, true);
        busy = true;
        WorldDropsTests.whenTicking(level, p0, 0, () -> testBreedEggHatchTicking(level, p0));
    }

    private static void testBreedEggHatchTicking(ServerLevel level, BlockPos p) {
        cleanup(level, p, 8);
        FakePlayer fp = tester(level);
        com.matt.quantumfalcon.story.PlayerStoryData.get(fp).markChapterSeen(com.matt.quantumfalcon.story.StorylineManager.StoryChapter.CHAPTER_1);
        QuantumFalconEntity a = falcon(level, p, true), b = falcon(level, p.east(2), true);
        a.tame(fp); b.tame(fp); a.setAge(0); b.setAge(0); a.setGender(true); b.setGender(false);
        a.setOrderedToSit(false); b.setOrderedToSit(false);
        interact(fp, a, ModItems.QUANTUM_BERRY.get(), 1);
        interact(fp, b, ModItems.QUANTUM_BERRY.get(), 1);
        boolean love = a.isInLove() && b.isInLove();
        boolean canMate = a.canMate(b);
        a.spawnChildFromBreeding(level, b);
        BlockPos egg = null;
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-3, -1, -3), p.offset(5, 3, 3)))
            if (level.getBlockState(q).is(ModBlocks.FALCON_EGG_BLOCK.get())) { egg = q.immutable(); break; }
        result("breed", love && canMate, "inLove=" + love + " canMate=" + canMate);
        busy = true;
        result("egg", egg != null, egg != null ? "egg block at " + egg.toShortString() : "no egg block placed");
        if (egg == null) { busy = true; result("hatch", false, "no egg"); a.discard(); b.discard(); return; }
        busy = true;
        final BlockPos eggPos = egg;
        int babiesBefore = level.getEntitiesOfClass(QuantumFalconEntity.class, new AABB(p).inflate(8)).size();
        if (level.getBlockEntity(eggPos) instanceof FalconEggBlockEntity be) {
            boolean parents = be.hasParents();
            be.forceHatch(level);
            later(5, () -> {
                int babies = level.getEntitiesOfClass(QuantumFalconEntity.class, new AABB(p).inflate(8)).size();
                result("hatch", babies > babiesBefore, "parents=" + parents + " falcons " + babiesBefore + " -> " + babies + ", egg block now " + level.getBlockState(eggPos).getBlock());
                cleanup(level, p, 8);
            });
        } else result("hatch", false, "egg has no block entity");
    }

    private static void testPerch(ServerLevel level) {
        BlockPos p = pad(level, 3000, 3060, Blocks.GRASS_BLOCK);
        cleanup(level, p, 8);
        FakePlayer fp = tester(level);
        BlockPos perch = p.east(2);
        level.setBlock(perch, ModBlocks.FALCON_PERCH.get().defaultBlockState(), 3);
        QuantumFalconEntity f = falcon(level, p, false);
        f.tame(fp);
        boolean landed = f.tryPerchNearby();
        later(20, () -> {
            boolean on = f.isOnPerch(perch);
            result("perch", landed && on, "tryPerchNearby=" + landed + " onPerch=" + on + " home=" + f.getHomePerch());
            f.discard();
        });
    }

    private static void testStamina(ServerLevel level) {
        BlockPos p = pad(level, 3000, 3080, Blocks.GRASS_BLOCK);
        cleanup(level, p, 8);
        FakePlayer fp = tester(level);
        BlockPos plain = p.east(2), quantum = p.west(2);
        level.setBlock(plain, ModBlocks.FALCON_PERCH.get().defaultBlockState(), 3);
        level.setBlock(quantum.below(), ModBlocks.QUANTUM_STONE.get().defaultBlockState(), 3);
        level.setBlock(quantum, ModBlocks.FALCON_PERCH.get().defaultBlockState(), 3);
        // the falcons only tick in an entity-ticking chunk; the 3000,3080 pad was often not loaded, so stamina never moved
        WorldDropsTests.force(level, p, true);
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            QuantumFalconEntity a = falcon(level, p, false), b = falcon(level, p, false);
            a.tame(fp); b.tame(fp); a.setOrderedToSit(true); b.setOrderedToSit(true);
            a.landOnPerch(plain); b.landOnPerch(quantum);
            a.setStamina(10); b.setStamina(10);
            long t0 = level.getGameTime();
            later(20, () -> {
                float sa = a.getStamina(), sb = b.getStamina();
                long ticks = level.getGameTime() - t0;
                boolean ok = sa > 10 && sb > sa + 15 && b.isOnQuantumPerch();
                result("stamina_quantum_perch", ok, "after " + ticks + " ticks: plain perch 10 -> " + sa + ", quantum-stone perch 10 -> " + sb + " (quantumPerch=" + b.isOnQuantumPerch() + ")");
                a.discard(); b.discard();
                WorldDropsTests.force(level, p, false);
            });
        });
    }

    private static void testBerry(ServerLevel level) {
        BlockPos p = pad(level, 3000, 3100, Blocks.GRASS_BLOCK);
        cleanup(level, p, 8);
        FakePlayer fp = tester(level);
        QuantumFalconEntity f = falcon(level, p, true);
        f.tame(fp);
        f.setStamina(5);
        interact(fp, f, ModItems.QUANTUM_BERRY.get(), 1);
        boolean boosted = f.getBerryBoostTicks() > 0 && f.getStamina() >= QuantumFalconEntity.MAX_STAMINA - 0.01f;
        com.matt.quantumfalcon.events.QuantumFlightHandler.grantFlight(fp, 200);
        boolean flight = fp.getAbilities().mayfly && com.matt.quantumfalcon.events.QuantumFlightHandler.hasQuantumFlight(fp);
        result("berry_boost", boosted && flight, "falconBoostTicks=" + f.getBerryBoostTicks() + " stamina=" + f.getStamina() + " playerMayFly=" + flight);
        fp.getAbilities().mayfly = false; fp.getPersistentData().remove(com.matt.quantumfalcon.events.QuantumFlightHandler.KEY);
        f.discard();
    }

    private static void testEchoStone(ServerLevel level) {
        BlockPos a = pad(level, 3000, 3120, Blocks.STONE);
        BlockPos b = pad(level, 3020, 3120, com.matt.quantumfalcon.registry.ModFamilyBlocks.ECHO_STONE.get());
        cleanup(level, a, 8); cleanup(level, b, 8);
        LivingEntity pa = EntityType.PIG.create(level), pb = EntityType.PIG.create(level);
        pa.moveTo(a.getX() + 0.5, a.getY() + 12, a.getZ() + 0.5); pb.moveTo(b.getX() + 0.5, b.getY() + 12, b.getZ() + 0.5);
        level.addFreshEntity(pa); level.addFreshEntity(pb);
        later(60, () -> {
            float da = pa.getMaxHealth() - pa.getHealth(), db = pb.getMaxHealth() - pb.getHealth();
            result("echo_stone_fall", da > 0 && db < da, "12-block fall damage: stone " + da + ", echo stone " + db);
            pa.discard(); pb.discard();
        });
    }

    private static void testVoidStone(ServerLevel level) {
        Item vs = com.matt.quantumfalcon.registry.ModFamilyBlocks.VOID_STONE.get().asItem();
        List<String> uses = new ArrayList<>();
        for (var h : level.getRecipeManager().getRecipes()) {
            for (var ing : h.value().getIngredients())
                if (ing.test(new ItemStack(vs))) { uses.add(h.id().getPath()); break; }
        }
        boolean singularity = uses.stream().anyMatch(s -> s.contains("singularity") || s.contains("void"));
        result("void_stone_recipes", singularity, uses.size() + " recipes use void_stone: " + uses);
    }

    // ------------------------------------------------------------------ portal
    private static void testPortal(ServerLevel level) {
        int px = 3200 + (int) (level.getGameTime() % 40) * 48; // fresh spot each run (CPA remembers portal links)
        BlockPos base = pad(level, px, 3000, Blocks.STONE);
        cleanup(level, base, 12);
        // 6x7 frame (4x5 opening) in the X/Y plane
        var frame = ModBlocks.RIFT_FRAME.get().defaultBlockState();
        for (int dx = 0; dx < 6; dx++) for (int dy = 0; dy < 7; dy++) {
            boolean edge = dx == 0 || dx == 5 || dy == 0 || dy == 6;
            level.setBlock(base.offset(dx - 2, dy, 0), edge ? frame : Blocks.AIR.defaultBlockState(), 3);
        }
        BlockPos inside = base.offset(0, 1, 0);
        boolean lit = net.kyrptonaught.customportalapi.portal.PortalPlacer.attemptPortalLight(level, inside,
                net.kyrptonaught.customportalapi.portal.PortalIgnitionSource.ItemUseSource(ModBlocks.RIFT_IGNITER.get().asItem()));
        boolean portalBlock = level.getBlockState(inside).getBlock() instanceof net.kyrptonaught.customportalapi.CustomPortalBlock;
        if (!lit || !portalBlock) { result("portal_round_trip", false, "light=" + lit + " portalBlockPresent=" + portalBlock + " at " + inside.toShortString()); return; }
        Entity traveller = EntityType.ARMOR_STAND.create(level);
        traveller.moveTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
        // CPA refuses to teleport dead entities; the echo_rift side portal can be mid-air over the void, so keep the stand floating and unkillable
        traveller.setNoGravity(true); traveller.setInvulnerable(true);
        level.addFreshEntity(traveller);
        var there = net.kyrptonaught.customportalapi.util.CustomTeleporter.createTeleportTarget(level, traveller, ModBlocks.RIFT_FRAME.get(), inside);
        if (there == null) { result("portal_round_trip", false, "no teleport target to echo_rift"); return; }
        Entity arrived = traveller.changeDimension(there);
        later(10, () -> {
            if (arrived == null || arrived.level().dimension() != ECHO_RIFT) { result("portal_round_trip", false, "did not arrive in echo_rift: " + (arrived == null ? "null" : arrived.level().dimension().location())); return; }
            ServerLevel echo = (ServerLevel) arrived.level();
            BlockPos ret = null;
            for (BlockPos q : BlockPos.betweenClosed(arrived.blockPosition().offset(-4, -4, -4), arrived.blockPosition().offset(4, 4, 4)))
                if (echo.getBlockState(q).getBlock() instanceof net.kyrptonaught.customportalapi.CustomPortalBlock) { ret = q.immutable(); break; }
            if (ret == null) { result("portal_round_trip", false, "arrived at " + arrived.blockPosition().toShortString() + " in echo_rift but no return portal"); return; }
            final BlockPos retPos = ret;
            String biome = echo.getBiome(retPos).unwrapKey().map(k -> k.location().getPath()).orElse("?");
            var back0 = net.kyrptonaught.customportalapi.util.CustomTeleporter.createTeleportTarget(echo, arrived, ModBlocks.RIFT_FRAME.get(), retPos);
            if (back0 == null) {   // first boot: the destination chunks may still be generating; retry once a second later
                later(20, () -> portalReturn(echo, arrived, retPos, biome, inside, 1));
                return;
            }
            portalReturnWith(arrived, back0, retPos, biome, inside);
        });
    }

    private static void portalReturn(ServerLevel echo, Entity arrived, BlockPos retPos, String biome, BlockPos inside, int attempt) {
        // keep the overworld side loaded: CPA returns null while the destination chunk is still generating
        echo.getServer().overworld().getChunk(inside);
        if (arrived.isAlive()) arrived.moveTo(retPos.getX() + 0.5, retPos.getY(), retPos.getZ() + 0.5);
        var back = net.kyrptonaught.customportalapi.util.CustomTeleporter.createTeleportTarget(echo, arrived, ModBlocks.RIFT_FRAME.get(), retPos);
        if (back == null && attempt < 5) { later(20 * (attempt + 1), () -> portalReturn(echo, arrived, retPos, biome, inside, attempt + 1)); return; }
        if (back == null) { result("portal_round_trip", false, "no return target from echo_rift portal " + retPos.toShortString() + " (after 5 retries; traveller alive=" + arrived.isAlive() + " at " + arrived.blockPosition().toShortString() + ")"); return; }
        portalReturnWith(arrived, back, retPos, biome, inside);
    }

    private static void portalReturnWith(Entity arrived, net.minecraft.world.level.portal.DimensionTransition back, BlockPos retPos, String biome, BlockPos inside) {
        {
            Entity home = arrived.changeDimension(back);
            later(10, () -> {
                boolean ok = home != null && home.level().dimension() == Level.OVERWORLD && home.blockPosition().distSqr(inside) < 32 * 32;
                result("portal_round_trip", ok, "lit 4x5 portal; overworld " + inside.toShortString() + " -> echo_rift " + retPos.toShortString()
                        + " (biome " + biome + ", return portal generated) -> " + (home == null ? "null" : home.level().dimension().location() + " " + home.blockPosition().toShortString()));
                if (home != null) home.discard();
            });
        }
    }

    private static void testCompass(ServerLevel level) {
        ServerLevel echo = level.getServer().getLevel(ECHO_RIFT);
        var q = (RiftCompassItem) ModItems.QUANTUM_COMPASS.get();
        var e = (RiftCompassItem) ModItems.ECHO_COMPASS.get();
        var rift = RiftCompassItem.locate(echo, BlockPos.ZERO, q.targetTag());
        var arena = RiftCompassItem.locate(echo, BlockPos.ZERO, e.targetTag());
        result("compass", rift.isPresent() && arena.isPresent(), "echo_rift from 0,0: nearest rift " + rift.map(BlockPos::toShortString).orElse("none")
                + ", nearest boss arena " + arena.map(BlockPos::toShortString).orElse("none"));
    }

    // ------------------------------------------------------------------ bosses
    private static String bossKey(ArenaType t) {
        return switch (t) { case QUANTUM_GUARDIAN -> "QUANTUM_OVERSEER"; case VOID_HORROR -> "RIFT_WARDEN"; case ENTANGLED_TWINS -> "ENTANGLED_TWINS"; case SINGULARITY_SOVEREIGN -> "SINGULARITY_SOVEREIGN"; };
    }

    private static Item relic(ArenaType t) {
        return switch (t) {
            case QUANTUM_GUARDIAN -> ModItems.OVERSEERS_EYE.get();
            case VOID_HORROR -> ModItems.WARDENS_FRAGMENT.get();
            case ENTANGLED_TWINS -> ModItems.TWIN_ESSENCE.get();
            case SINGULARITY_SOVEREIGN -> ModItems.SOVEREIGNS_CROWN.get();
        };
    }

    private static void testBoss(ServerLevel level, ArenaType type, int idx) {
        String name = "boss_" + switch (type) { case QUANTUM_GUARDIAN -> "overseer"; case VOID_HORROR -> "warden"; case ENTANGLED_TWINS -> "twins"; case SINGULARITY_SOVEREIGN -> "sovereign"; };
        int x = 6000 + idx * 400, z = 6000;
        for (int cx = -6; cx <= 6; cx++) for (int cz = -6; cz <= 6; cz++) level.setChunkForced((x >> 4) + cx, (z >> 4) + cz, true);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 10;
        BlockPos c = new BlockPos(x, y, z);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, type);
        if (boss == null) { result(name, false, "summon returned null"); return; }
        var arena = ArenaMechanics.getArenaAt(level, c);
        StringBuilder d = new StringBuilder("summoned " + boss.getType().getDescriptionId() + " hp=" + boss.getMaxHealth() + ", arena registered=" + (arena != null));
        // a fresh player per boss: a first kill, so the relic has to drop (no repeat-kill gift path), from real melee
        FakePlayer fp = AuditTests.freshPlayer(level, "QFBoss" + idx);
        com.matt.quantumfalcon.story.BossCredit.TEST_PLAYERS.put(fp.getUUID(), fp);   // fake players are in no player list
        fp.moveTo(c.getX() + 3, c.getY() + 2, c.getZ());
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.NETHERITE_SWORD));
        later(type == ArenaType.ENTANGLED_TWINS ? 40 : 20, () -> {  // twins: let spawn-fall sync cooldown expire
            boolean alive = boss.isAlive();
            d.append(", alive after 1s=").append(alive);
            List<Mob> targets = new ArrayList<>(List.of(boss));
            if (boss instanceof EntangledTwinsEntity tw) {
                EntangledTwinsEntity partner = tw.getTwin();
                d.append(", twin linked=").append(partner != null && partner.getTwin() == tw);
                if (partner != null) {
                    later(0, () -> {}); 
                    float ph = partner.getHealth(), bh = boss.getHealth();
                    boss.invulnerableTime = 0; partner.invulnerableTime = 0;
                    boolean hit = boss.hurt(level.damageSources().playerAttack(fp), 20f);
                    d.append(", shared dmg=").append(partner.getHealth() < ph).append(" (hit=").append(hit).append(" boss ").append(bh).append("->").append(boss.getHealth())
                     .append(", twin ").append(ph).append("->").append(partner.getHealth()).append(")");
                    targets.add(partner);
                }
            }
            if (boss instanceof com.matt.quantumfalcon.entity.boss.RiftWardenEntity wd) {
                wd.onPhaseChangeTo(2);
                int minions = level.getEntitiesOfClass(com.matt.quantumfalcon.entity.boss.RiftWardenMinionEntity.class, boss.getBoundingBox().inflate(10)).size();
                d.append(", phase2 minions summoned=").append(minions);
            }
            if (boss instanceof SingularitySovereignEntity sov) {
                StringBuilder phases = new StringBuilder();
                for (float frac : new float[]{0.70f, 0.45f, 0.20f}) {
                    sov.setHealth(sov.getMaxHealth() * frac + 1);
                    sov.clearInvulnerability();
                    sov.hurt(level.damageSources().playerAttack(fp), 1f);
                    sov.aiStep();
                    phases.append(sov.getPhase());
                }
                d.append(", phases at 70/45/20%=").append(phases).append(" gravityReversedFlag=").append(sov.isGravityReversed() || (sov.getPhase() >= 4));
            }
            // only drops from this kill count: clear any item lying around from earlier runs (the test world persists)
            level.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(64)).forEach(ItemEntity::discard);
            // kill
            for (Mob m : targets) {
                for (int i = 0; i < 40 && m.isAlive() && !m.isDeadOrDying(); i++) {
                    m.invulnerableTime = 0;
                    if (m instanceof SingularitySovereignEntity sv) sv.clearInvulnerability();
                    m.setHealth(Math.min(m.getHealth(), 2f));
                    // the killing blow is a real player melee attack (Player.attack: weapon, crit/sweep rules, death credit)
                    fp.moveTo(m.getX() + 1.5, m.getY(), m.getZ());
                    fp.resetAttackStrengthTicker();
                    fp.attack(m);
                    if (m.isAlive() && i > 30) m.hurt(level.damageSources().playerAttack(fp), 100f);   // fallback only if melee can't land
                }
            }
            later(140, () -> {
                List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(48));
                Set<String> names = new TreeSet<>();
                boolean relic = false, relicOnFloor = false;
                for (ItemEntity ie : drops) {
                    names.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ie.getItem().getItem()).getPath());
                    if (ie.getItem().is(relic(type))) {
                        relic = true;
                        // the drop must be lying on something, not falling into a pit
                        BlockPos at = ie.blockPosition();
                        for (int dy = 0; dy <= 3 && !relicOnFloor; dy++) relicOnFloor = !level.getBlockState(at.below(dy + 1)).getCollisionShape(level, at.below(dy + 1)).isEmpty();
                    }
                }
                boolean dead = targets.stream().noneMatch(LivingEntity::isAlive);
                boolean cleared = ArenaMechanics.getArenaAt(level, c) == null;
                d.append(", dead=").append(dead).append(", arena cleared=").append(cleared).append(", relic dropped=").append(relic).append(" on floor=").append(relicOnFloor)
                 .append(", boss defeat credited to the melee player=").append(com.matt.quantumfalcon.story.BossDeathHandler.hasDefeatedBoss(fp, com.matt.quantumfalcon.story.BossDeathHandler.BossType.valueOf(bossKey(type)))).append(", drops=").append(names);
                List<String> deadDrops = names.stream().filter(nm -> java.util.Arrays.asList(WorldDropsTests.DEAD).contains(nm)).toList();
                if (!deadDrops.isEmpty()) d.append(", DEAD items dropped=").append(deadDrops);
                result(name, deadDrops.isEmpty() && alive && arena != null && dead && relic && relicOnFloor && cleared && com.matt.quantumfalcon.story.BossDeathHandler.hasDefeatedBoss(fp, com.matt.quantumfalcon.story.BossDeathHandler.BossType.valueOf(bossKey(type))), d.toString());
                for (int cx = -6; cx <= 6; cx++) for (int cz = -6; cz <= 6; cz++) level.setChunkForced((x >> 4) + cx, (z >> 4) + cz, false);
            });
        });
    }

    /** Arena mechanics per design notes: entry tracking, force field at the edge, fall rescue, phase effects, cleanup on victory. */
    private static void testArena(ServerLevel level, ArenaType type, int idx) {
        String name = "arena_" + switch (type) { case QUANTUM_GUARDIAN -> "overseer"; case VOID_HORROR -> "warden"; case ENTANGLED_TWINS -> "twins"; case SINGULARITY_SOVEREIGN -> "sovereign"; };
        int x = 9000 + idx * 400 + (int) (now % 50) * 16, z = 9000;
        for (int cx = -6; cx <= 6; cx++) for (int cz = -6; cz <= 6; cz++) level.setChunkForced((x >> 4) + cx, (z >> 4) + cz, true);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 10;
        BlockPos c = new BlockPos(x, y, z);
        cleanup(level, c, 120);
        Mob boss = BossSummoner.buildAndSummon(level, c, type);
        if (boss == null) { result(name, false, "summon returned null"); return; }
        List<Mob> bosses = new ArrayList<>(List.of(boss));
        if (boss instanceof EntangledTwinsEntity tw && tw.getTwin() != null) bosses.add(tw.getTwin());
        bosses.forEach(m -> m.setNoAi(true));
        FakePlayer fp = tester(level);
        fp.removeAllEffects(); fp.setHealth(fp.getMaxHealth());
        StringBuilder d = new StringBuilder();
        int r = type.getRadius();
        // 1) force field: standing just inside the edge pushes you toward the centre
        fp.moveTo(c.getX() + r - 1, c.getY() + 1, c.getZ()); fp.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        boolean in = ArenaMechanics.testPlayerTick(fp, level);
        boolean pushed = fp.getDeltaMovement().x < 0;
        boolean tracked = ArenaMechanics.isPlayerTracked(fp.getUUID());
        d.append("radius=").append(r).append(" inArena=").append(in).append(" entryTracked=").append(tracked).append(" edgePushX=").append(String.format("%.2f", fp.getDeltaMovement().x));
        // 2) fall rescue: dropping below the arena's death line puts you back on the arena
        double dy = ArenaMechanics.deathY(c);
        fp.moveTo(c.getX() + r / 2.0, dy - 2, c.getZ());
        int rescuesBefore = ArenaMechanics.FALL_RESCUES.get();
        ArenaMechanics.testPlayerTick(fp, level);
        // FakePlayer ignores damage and teleport packets, so check that the void-fall rescue branch ran
        // (for real ServerPlayers that branch deals 10 void damage and teleports to the arena ring).
        boolean rescued = ArenaMechanics.FALL_RESCUES.get() == rescuesBefore + 1;
        d.append(" deathY=").append((int) dy).append(" voidFallRescueRan=").append(rescued);
        // 3) phase effects over 120 ticks
        int phase = type == ArenaType.VOID_HORROR ? 2 : 1;
        ArenaMechanics.updateBossPhase(c, phase);
        double off = type == ArenaType.ENTANGLED_TWINS ? 5 : type == ArenaType.SINGULARITY_SOVEREIGN ? 10 : r / 2.0;
        if (type.hasVoidCenter() && off < 6) off = 6;
        final double fo = off;
        for (int t = 1; t <= 120; t++) later(t, () -> {
            fp.moveTo(c.getX() + fo, c.getY() + 1, c.getZ()); fp.setHealth(fp.getMaxHealth());
            ArenaMechanics.testPlayerTick(fp, level);
        });
        later(122, () -> {
            var want = switch (type) {
                case QUANTUM_GUARDIAN -> List.of(net.minecraft.world.effect.MobEffects.GLOWING);
                case VOID_HORROR -> List.of(net.minecraft.world.effect.MobEffects.DARKNESS, net.minecraft.world.effect.MobEffects.WITHER);
                case ENTANGLED_TWINS -> List.of(net.minecraft.world.effect.MobEffects.LEVITATION);
                case SINGULARITY_SOVEREIGN -> List.of(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            };
            Set<String> had = new TreeSet<>();
            fp.getActiveEffects().forEach(e -> had.add(e.getEffect().getRegisteredName()));
            boolean effects = want.stream().allMatch(fp::hasEffect);
            d.append(" phase").append(phase).append(" effects=").append(had).append(" expected=").append(want.stream().map(h -> h.getRegisteredName()).toList());
            // 4) victory cleanup
            for (Mob m : bosses) for (int i = 0; i < 40 && m.isAlive(); i++) {
                m.invulnerableTime = 0; if (m instanceof SingularitySovereignEntity sv) sv.clearInvulnerability();
                m.setHealth(Math.min(m.getHealth(), 2f)); m.hurt(level.damageSources().playerAttack(fp), 100f);
            }
            later(140, () -> {
                boolean cleared = ArenaMechanics.getArenaAt(level, c) == null;
                d.append(" clearedAfterVictory=").append(cleared);
                fp.removeAllEffects();
                result(name, in && tracked && pushed && rescued && effects && cleared, d.toString());
                level.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(48)).forEach(Entity::discard);
                for (int cx = -6; cx <= 6; cx++) for (int cz = -6; cz <= 6; cz++) level.setChunkForced((x >> 4) + cx, (z >> 4) + cz, false);
            });
        });
    }

    /** Skill tree, quests, loot chain recipes and the ending (run after boss_sovereign, which the tester kills). */
    private static void testProgression(ServerLevel level) {
        FakePlayer fp = tester(level);
        StringBuilder d = new StringBuilder();
        int skills = com.matt.quantumfalcon.skill.SkillTreeSystem.getTotalSkillCount();
        int before = com.matt.quantumfalcon.skill.SkillPointSystem.getAvailablePoints(fp);
        com.matt.quantumfalcon.skill.SkillPointSystem.givePoints(fp, 3);
        int after = com.matt.quantumfalcon.skill.SkillPointSystem.getAvailablePoints(fp);
        boolean spent = com.matt.quantumfalcon.skill.SkillPointSystem.spendPoints(fp, 1);
        d.append("skills=").append(skills).append(" points ").append(before).append("->").append(after).append(" spent=").append(spent);
        int quests = com.matt.quantumfalcon.quest.QuestSystem.QUESTS.size();
        var q = com.matt.quantumfalcon.quest.QuestSystem.QUESTS.values().iterator().next();
        boolean accepted = com.matt.quantumfalcon.quest.QuestSystem.acceptQuest(fp, q) || com.matt.quantumfalcon.quest.QuestSystem.hasCompletedQuest(fp, q.id);
        d.append(" quests=").append(quests).append(" accept(").append(q.id).append(")=").append(accepted);
        var rm = level.getServer().getRecipeManager();
        boolean gemRecipe = rm.byKey(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "reality_gem")).isPresent();
        boolean shardRecipe = rm.byKey(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "transcendence_shard")).isPresent();
        d.append(" recipes relics->reality_gem=").append(gemRecipe).append(" reality_gem+void_stone->transcendence_shard=").append(shardRecipe);
        // own state. A brand-new player is not ready; the real Sovereign-defeat handler (no world effects) unlocks the ending.
        FakePlayer ep = AuditTests.freshPlayer(level, "QFEndingProbe");
        com.matt.quantumfalcon.story.StoryManager.resetStoryProgress(ep);
        boolean notYet = !com.matt.quantumfalcon.story.StoryManager.canMakeEndingChoice(ep);
        com.matt.quantumfalcon.story.BossDeathHandler.onBossDefeated(ep, level, com.matt.quantumfalcon.story.BossDeathHandler.BossType.SINGULARITY_SOVEREIGN, ep.blockPosition(), false);
        boolean ready = notYet && com.matt.quantumfalcon.story.StoryManager.canMakeEndingChoice(ep);
        d.append(" endingUnlockedBySovereignKill=").append(ready).append(" (fresh player locked before=").append(notYet).append(")");
        com.matt.quantumfalcon.story.StoryManager.resetStoryProgress(fp);
        com.matt.quantumfalcon.story.StoryManager.setPlayerReadyForEnding(fp, true);
        for (String t : new String[]{"void", "quantum", "echo", "singularity"}) com.matt.quantumfalcon.story.PlayerStoryData.get(fp).raiseFalconBond(t, 100);   // True path also needs max bonds
        fp.getInventory().clearContent();
        ItemStack shard = new ItemStack(ModItems.TRANSCENDENCE_SHARD.get());
        com.matt.quantumfalcon.story.StoryManager.bind(shard, fp);
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, shard);
        { ItemStack g = new ItemStack(ModItems.REALITY_GEM.get()); com.matt.quantumfalcon.story.StoryManager.bind(g, fp); fp.getInventory().add(g); }  // after the shard so it lands in another slot; bound to its crafter
        fp.setShiftKeyDown(false);
        // selection starts at LIGHT; each plain use cycles LIGHT -> DARK -> TRUE
        ModItems.TRANSCENDENCE_SHARD.get().use(level, fp, net.minecraft.world.InteractionHand.MAIN_HAND); // DARK
        ModItems.TRANSCENDENCE_SHARD.get().use(level, fp, net.minecraft.world.InteractionHand.MAIN_HAND); // TRUE (needs relics or Reality Gem)
        fp.setShiftKeyDown(true);
        ModItems.TRANSCENDENCE_SHARD.get().use(level, fp, net.minecraft.world.InteractionHand.MAIN_HAND);
        fp.setShiftKeyDown(false);
        var ending = com.matt.quantumfalcon.story.StoryManager.getCompletedEnding(fp);
        d.append(" ending=").append(ending);
        fp.getInventory().clearContent();
        result("progression", skills > 0 && after == before + 3 && spent && quests > 0 && accepted && gemRecipe && shardRecipe && ready
            && ending == com.matt.quantumfalcon.story.StoryManager.Ending.TRUE, d.toString());
    }

    /** Chest loot integration must add to vanilla tables, not replace them. */
    private static void testVanillaLoot(ServerLevel level) {
        var key = net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON;
        var table = level.getServer().reloadableRegistries().getLootTable(key);
        boolean ourPool = table.getPool(QuantumFalcon.MOD_ID + "_extra") != null;
        Set<String> vanilla = new TreeSet<>(), ours = new TreeSet<>();
        for (int i = 0; i < 30; i++) {
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            for (ItemStack st : table.getRandomItems(params)) {
                var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem());
                (id.getNamespace().equals("minecraft") ? vanilla : ours).add(id.getPath());
            }
        }
        result("vanilla_loot_kept", ourPool && !vanilla.isEmpty(), "simple_dungeon: quantumfalcon pool=" + ourPool + ", vanilla items seen=" + vanilla.size() + " " + vanilla + ", mod items seen=" + ours);
    }

    // ------------------------------------------------------------------ Sha'Sha mini-boss + mob bodies
    /** Sha'Sha: spawn + boss stats, falcon immunity, telegraphed slam hits a target in front, Echo Step blink, phase-2 echoes, kill + loot. */
    private static void testShaSha(ServerLevel level) {
        int x = 12000, z = 12000;
        for (int cx = -2; cx <= 3; cx++) for (int cz = -2; cz <= 2; cz++) level.setChunkForced((x >> 4) + cx, (z >> 4) + cz, true);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1;
        for (int dx = -5; dx <= 20; dx++) for (int dz = -5; dz <= 5; dz++) {
            level.setBlock(new BlockPos(x + dx, y - 1, z + dz), Blocks.STONE.defaultBlockState(), 3);
            for (int dy = 0; dy < 6; dy++) level.setBlock(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState(), 3);
        }
        BlockPos c = new BlockPos(x, y, z);
        cleanup(level, c, 40);
        var sha = ModEntities.SHA_SHA.get().create(level);
        if (sha == null) { result("sha_sha", false, "create returned null"); return; }
        sha.moveTo(x + 0.5, y, z + 0.5, 0f, 0f); sha.setYHeadRot(0f); sha.yBodyRot = 0f;
        sha.finalizeSpawn(level, level.getCurrentDifficultyAt(c), MobSpawnType.SPAWN_EGG, null);
        sha.setNoAi(true);
        level.addFreshEntity(sha);
        StringBuilder d = new StringBuilder("hp=" + sha.getMaxHealth());
        FakePlayer fp = tester(level);
        // tamed falcon is never a valid target
        QuantumFalconEntity f = falcon(level, c.offset(-2, 0, 0), true);
        f.tame(fp);
        boolean falconSafe = !sha.canAttack(f);
        f.discard();
        d.append(", ignores tamed falcon=").append(falconSafe);
        // slam: a dummy iron golem 2 blocks in front (+Z at yaw 0)
        var golem = EntityType.IRON_GOLEM.create(level);
        golem.moveTo(x + 0.5, y, z + 2.5, 180f, 0f); golem.setNoAi(true); level.addFreshEntity(golem);
        sha.setTarget(golem);
        float gh = golem.getHealth();
        sha.startSlam();
        boolean telegraph = sha.isSlamming() && golem.getHealth() == gh;   // nothing lands during the wind-up
        later(20, () -> {
            boolean slamHit = golem.getHealth() < gh && golem.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN) && sha.slamsLanded == 1;
            d.append(", slam telegraphed=").append(telegraph).append(" hit=").append(slamHit).append(" (golem ").append(gh).append("->").append(golem.getHealth()).append(")");
            // Echo Step: golem moves 14 blocks away, Sha'Sha blinks behind it after a 1s telegraph
            golem.teleportTo(x + 15.5, y, z + 0.5); golem.setYRot(90f); golem.setYHeadRot(90f);
            sha.setTarget(golem);
            sha.startBlink(golem);
            later(30, () -> {
                double dist = sha.distanceTo(golem);
                boolean blinked = sha.blinks == 1 && dist < 4.5;
                d.append(String.format(", blink=%s (dist %.1f)", blinked, dist));
                // phase 2 at 50%
                sha.setHealth(sha.getMaxHealth() * 0.5f + 1f); sha.invulnerableTime = 0;
                sha.hurt(level.damageSources().playerAttack(fp), 2f);
                later(3, () -> {
                    int echoes = level.getEntitiesOfClass(com.matt.quantumfalcon.entity.VoidEchoEntity.class, sha.getBoundingBox().inflate(8)).size();
                    boolean p2 = sha.isPhaseTwo() && sha.echoesCalled == 2 && echoes >= 2;
                    d.append(", phase2=").append(sha.isPhaseTwo()).append(" echoes=").append(echoes);
                    level.getEntitiesOfClass(com.matt.quantumfalcon.entity.VoidEchoEntity.class, sha.getBoundingBox().inflate(16)).forEach(Entity::discard);
                    golem.discard();
                    for (int i = 0; i < 20 && sha.isAlive() && !sha.isDeadOrDying(); i++) {
                        sha.invulnerableTime = 0; sha.setHealth(Math.min(sha.getHealth(), 2f));
                        sha.hurt(level.damageSources().playerAttack(fp), 100f);
                    }
                    boolean stillAnimating = sha.isDeadOrDying() && !sha.isRemoved();
                    later(com.matt.quantumfalcon.entity.boss.ShaShaEntity.DEATH_TICKS + 20, () -> {
                        Set<String> names = new TreeSet<>();
                        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(30)))
                            names.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ie.getItem().getItem()).getPath());
                        boolean removed = sha.isRemoved();
                        boolean loot = names.contains("echo_shard") && names.contains("echo_crystal");
                        d.append(", death anim held=").append(stillAnimating).append(" removed=").append(removed).append(", drops=").append(names);
                        result("sha_sha", falconSafe && telegraph && slamHit && blinked && p2 && stillAnimating && removed && loot, d.toString());
                        cleanup(level, c, 40);
                        for (int cx = -2; cx <= 3; cx++) for (int cz = -2; cz <= 2; cz++) level.setChunkForced((x >> 4) + cx, (z >> 4) + cz, false);
                    });
                });
            });
        });
    }

    /** The four hostile mobs (and Sha'Sha) are GeckoLib animatables with geo/animation/texture/glowmask assets in the jar. */
    private static void testMobBodies(ServerLevel level) {
        BlockPos p = pad(level, 12100, 12000, Blocks.STONE);
        cleanup(level, p, 10);
        StringBuilder d = new StringBuilder();
        boolean ok = true;
        String[][] mobs = {{"mob", "quantum_phantom"}, {"mob", "crystal_guardian"}, {"mob", "void_echo"}, {"mob", "data_construct"}, {"boss", "sha_sha"},
            {"mob", "quantum_stalker"}, {"boss", "eclipse_phantom"}, {"boss", "void_lord"}, {"npc", "goblin_trader"}, {"npc", "quantum_merchant"}};
        var dummy = EntityType.IRON_GOLEM.create(level);
        dummy.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 1.5); dummy.setNoAi(true); level.addFreshEntity(dummy);
        for (String[] m : mobs) {
            String base = "/assets/quantumfalcon/";
            String path = "entity/" + m[0] + "/" + m[1];
            boolean assets = true; String anims = "";
            for (String r : new String[]{"geo/" + path + ".geo.json", "animations/" + path + ".animation.json", "textures/" + path + ".png", "textures/" + path + "_glowmask.png"})
                assets &= QFTestCommands.class.getResource(base + r) != null;
            try (var in = QFTestCommands.class.getResourceAsStream(base + "animations/" + path + ".animation.json")) {
                if (in != null) anims = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception ex) { assets = false; }
            boolean allAnims = true;
            boolean npc = m[0].equals("npc");
            for (String a : npc ? new String[]{"idle", "walk", "trade", "celebrate"} : new String[]{"idle", "walk", "attack", "death"}) allAnims &= anims.contains("\"animation." + m[1] + "." + a + "\"");
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, m[1]));
            Entity e = type.create(level);
            boolean geo = e instanceof software.bernie.geckolib.animatable.GeoEntity;
            boolean attacked = false;
            if (npc) { attacked = e != null; if (e != null) e.discard(); }
            else if (e instanceof Mob mob) {
                mob.moveTo(p.getX() + 0.5, p.getY(), p.getZ() - 1.5, 0, 0); mob.setNoAi(true); level.addFreshEntity(mob);
                try { mob.doHurtTarget(dummy); attacked = true; } catch (Throwable ex) { d.append("attack threw ").append(ex).append("; "); }
                mob.discard();
            }
            boolean pass = assets && allAnims && geo && attacked;
            ok &= pass;
            d.append(m[1]).append(pass ? " ok" : " FAIL(assets=" + assets + " anims=" + allAnims + " geo=" + geo + " attack=" + attacked + ")").append("; ");
        }
        dummy.discard();
        result("mob_bodies", ok, d.toString());
    }





    private static com.google.gson.JsonObject readJson(String path) throws java.io.IOException {
        try (var in = QFTestCommands.class.getResourceAsStream(path)) {
            if (in == null) return null;
            return com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    private static String texPng(String ref) { var rl = ResourceLocation.parse(ref); return "/assets/" + rl.getNamespace() + "/textures/" + rl.getPath() + ".png"; }

    /** Falcon body archetypes: every variant maps to an archetype geo that keeps all base (animated) bones, adds accessory bones and is fully connected (GeoConnectivity); all falcon-model textures are 384x384 (2x a 192 layout, v4 geometry pass). */
    private static void testFalconArchetypes(ServerLevel level) {
        List<String> bad = new ArrayList<>(); int acc = 0, tex = 0, joints = 0;
        try {
            var base = readJson("/assets/quantumfalcon/geo/quantum_falcon.geo.json");
            if (!GeoConnectivity.audit(base.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()).isEmpty()) bad.add("base geo not connected");
            java.util.Set<String> baseBones = new java.util.HashSet<>();
            for (var b : base.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) baseBones.add(b.getAsJsonObject().get("name").getAsString());
            for (var a : com.matt.quantumfalcon.entity.FalconArchetype.values()) {
                var rl = a.geo();
                var g = readJson("/assets/" + rl.getNamespace() + "/" + rl.getPath());
                if (g == null) { bad.add(a + ":no geo"); continue; }
                var geo = g.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                if (geo.getAsJsonObject("description").get("texture_height").getAsInt() != 192) bad.add(a + ":texture_height");
                var conn = GeoConnectivity.audit(geo); joints += geo.getAsJsonArray("bones").size();
                if (!conn.isEmpty()) bad.add(a + ":" + conn.subList(0, Math.min(4, conn.size())));
                java.util.Set<String> names = new java.util.HashSet<>(); int accBones = 0;
                for (var b : geo.getAsJsonArray("bones")) { String n = b.getAsJsonObject().get("name").getAsString(); names.add(n); if (n.startsWith("acc_")) { accBones++; acc += b.getAsJsonObject().has("cubes") ? b.getAsJsonObject().getAsJsonArray("cubes").size() : 0; } }
                if (!names.containsAll(baseBones)) bad.add(a + ":missing base bones");
                if (accBones < 3) bad.add(a + ":only " + accBones + " accessory bones");
            }
            String[] variants = {"peregrine","crimson","stormrider","tempest","prairie","deepslate","tundra","coral","abyssal","voidstalker","singularity","cosmic","entangled_prism","quantum_phoenix","aetherwing","soulfire","mushroom","warped"};
            java.util.Set<com.matt.quantumfalcon.entity.FalconArchetype> used = java.util.EnumSet.noneOf(com.matt.quantumfalcon.entity.FalconArchetype.class);
            for (String v : variants) {
                if (!com.matt.quantumfalcon.entity.FalconArchetype.BY_VARIANT.containsKey(v)) bad.add(v + ":unmapped");
                used.add(com.matt.quantumfalcon.entity.FalconArchetype.of(v));
                for (String f : new String[]{v, v + "_glow", "anomaly_" + v}) {
                    try (var in = QFTestCommands.class.getResourceAsStream("/assets/quantumfalcon/textures/entity/quantum_falcon/" + f + ".png")) {
                        if (in == null) { if (!f.endsWith("_glow")) bad.add(f + ":missing"); continue; }
                        var img = javax.imageio.ImageIO.read(in); tex++;
                        if (img.getWidth() != 384 || img.getHeight() != 384) bad.add(f + ":" + img.getWidth() + "x" + img.getHeight());
                    }
                }
            }
            for (String f : new String[]{"leather","iron","diamond","netherite","netherite_glowmask","quantum","quantum_glowmask"}) {
                try (var in = QFTestCommands.class.getResourceAsStream("/assets/quantumfalcon/textures/entity/falcon_barding/" + f + ".png")) {
                    if (in == null) { bad.add("barding " + f + ":missing"); continue; }
                    var img = javax.imageio.ImageIO.read(in); tex++;
                    if (img.getWidth() != 384 || img.getHeight() != 384) bad.add("barding " + f + ":" + img.getWidth() + "x" + img.getHeight());
                }
            }
            if (used.size() < com.matt.quantumfalcon.entity.FalconArchetype.values().length) bad.add("only " + used.size() + " archetypes used");
        } catch (Exception ex) { bad.add(ex.toString()); }
        result("falcon_archetypes", bad.isEmpty(), com.matt.quantumfalcon.entity.FalconArchetype.values().length + " archetypes, " + acc + " accessory cubes, " + joints + " bones connectivity-checked (every cube touches its parent chain, every pivot closed), " + tex + " textures 384x384" + (bad.isEmpty() ? "" : "; bad: " + bad));
    }

    /** Boss redesign: every boss is a QFBoss with intro/windup/phase_shift animations whose bones all exist in its geo, textures match the geo
     *  texture size, original music is registered + streamed; live: bar colour follows phase, intro tags the boss, wind-ups delay the attack. */
    private static void testBossPresentation(ServerLevel level) {
        List<String> bad = new ArrayList<>(); int animBones = 0, cubes = 0;
        String[][] bosses = {{"rift_warden", "rift_warden", "boss/rift_warden"}, {"entangled_twins", "entangled_twins", "boss/entangled_twins_cyan"},
            {"quantum_overseer", "quantum_overseer", "boss/quantum_overseer"}, {"singularity_sovereign", "singularity_sovereign", "boss/singularity_sovereign"},
            {"eclipse_phantom", "boss/eclipse_phantom", "boss/eclipse_phantom"}, {"void_lord", "boss/void_lord", "boss/void_lord"}, {"sha_sha", "boss/sha_sha", "boss/sha_sha"}};
        try {
            var sounds = readJson("/assets/quantumfalcon/sounds.json");
            for (String[] b : bosses) {
                var geo = readJson("/assets/quantumfalcon/geo/entity/" + b[1] + ".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                java.util.Set<String> bones = new java.util.HashSet<>();
                for (var e : geo.getAsJsonArray("bones")) { bones.add(e.getAsJsonObject().get("name").getAsString()); if (e.getAsJsonObject().has("cubes")) cubes += e.getAsJsonObject().getAsJsonArray("cubes").size(); }
                var an = readJson("/assets/quantumfalcon/animations/entity/" + b[1] + ".animation.json").getAsJsonObject("animations");
                for (String w : new String[]{"intro", "windup", "phase_shift"}) if (!an.has("animation." + b[0] + "." + w)) bad.add(b[0] + ":no " + w);
                for (var e : an.entrySet()) for (String bn : e.getValue().getAsJsonObject().getAsJsonObject("bones").keySet()) { animBones++; if (!bones.contains(bn)) bad.add(b[0] + ":" + e.getKey().substring(e.getKey().lastIndexOf('.') + 1) + " animates missing bone " + bn); }
                int tw = geo.getAsJsonObject("description").get("texture_width").getAsInt(), th = geo.getAsJsonObject("description").get("texture_height").getAsInt();
                try (var in = QFTestCommands.class.getResourceAsStream("/assets/quantumfalcon/textures/entity/" + b[2] + ".png")) {
                    var img = javax.imageio.ImageIO.read(in);
                    if (img.getWidth() * th != img.getHeight() * tw) bad.add(b[0] + ": texture " + img.getWidth() + "x" + img.getHeight() + " vs geo " + tw + "x" + th);
                }
                var st = com.matt.quantumfalcon.entity.boss.BossDirector.STYLES.get(b[0]);
                if (st == null) { bad.add(b[0] + ":no style"); continue; }
                String ev = st.music().get().value().getLocation().getPath();
                var entry = sounds.getAsJsonObject(ev);
                if (entry == null) { bad.add(ev + ":not in sounds.json"); continue; }
                var snd = entry.getAsJsonArray("sounds").get(0).getAsJsonObject();
                String file = snd.get("name").getAsString();
                if (!file.startsWith("quantumfalcon:") || !snd.has("stream") || QFTestCommands.class.getResource("/assets/quantumfalcon/sounds/" + file.substring(14) + ".ogg") == null) bad.add(ev + ":bad music " + file);
                for (String suf : new String[]{"_intro", "_p2"}) {   // music v2: intro + harder loop, registered and present
                    var e2 = sounds.getAsJsonObject(ev + suf);
                    if (e2 == null) { bad.add(ev + suf + ":not in sounds.json"); continue; }
                    String f2 = e2.getAsJsonArray("sounds").get(0).getAsJsonObject().get("name").getAsString();
                    if (QFTestCommands.class.getResource("/assets/quantumfalcon/sounds/" + f2.substring(14) + ".ogg") == null) bad.add(ev + suf + ": missing " + f2);
                    if (!net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, ev + suf))) bad.add(ev + suf + ": not registered");
                }
            }
            for (String f : new String[]{"boss/roar", "boss/windup", "boss/phase"}) if (QFTestCommands.class.getResource("/assets/quantumfalcon/sounds/" + f + ".ogg") == null) bad.add(f + " missing");
        } catch (Exception ex) { bad.add(ex.toString()); }
        final int cubesF = cubes, animBonesF = animBones;
        // live checks on a Void Lord (2 phases)
        BlockPos p = pad(level, 5200, 3000, Blocks.STONE);
        cleanup(level, p, 10);
        var lord = com.matt.quantumfalcon.registry.ModEntities.VOID_LORD.get().create(level);
        lord.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0); lord.setNoAi(true); level.addFreshEntity(lord);
        var st = com.matt.quantumfalcon.entity.boss.BossDirector.STYLES.get("void_lord");
        com.matt.quantumfalcon.entity.boss.BossDirector.intro(lord, st, level);
        boolean introTag = lord.getPersistentData().getBoolean("qf_intro");
        int[] fired = {0};
        int delay = com.matt.quantumfalcon.entity.boss.BossDirector.windup(lord, 0, () -> fired[0]++);
        int firedAtOnce = fired[0];
        later(3, () -> {
            boolean bar1 = lord.qfBossBar().getColor() == st.bar(1);
            lord.setHealth(lord.getMaxHealth() * 0.4f);
            later(3, () -> {
                later(delay + 2, () -> {
                    boolean bar2 = lord.qfBossBar().getColor() == st.bar(2) && lord.qfPhase() == 2;
                    boolean phaseSeen = lord.getPersistentData().getInt("qf_phase") == 2;
                    boolean windupOk = firedAtOnce == 0 && fired[0] == 1 && delay > 0;
                    boolean musicHard = com.matt.quantumfalcon.entity.boss.BossDirector.musicHard(lord);
                    lord.discard();
                    if (!musicHard) bad.add("music did not switch to loop B at phase 2");
                    if (!introTag) bad.add("intro tag not set");
                    if (!bar1 || !bar2) bad.add("bar colour phase1=" + bar1 + " phase2=" + bar2 + " ticks=" + lord.tickCount + " colour=" + lord.qfBossBar().getColor() + " qfPhase=" + lord.qfPhase() + " tag=" + lord.getPersistentData());
                    if (!phaseSeen) bad.add("phase shift not recorded");
                    if (!windupOk) bad.add("windup firedAtOnce=" + firedAtOnce + " fired=" + fired[0] + " delay=" + delay);
                    result("boss_presentation", bad.isEmpty(), "7 bosses, " + cubesF + " cubes, " + animBonesF + " animated bone tracks checked, 7 original themes x (intro, loop A, loop B); live: intro, phase bar colour, music loop B switch, windup delay " + delay + "t"
                        + (bad.isEmpty() ? "" : "; bad: " + bad));
                });
            });
        });
    }

    /** Polish assets: emissive glow layers, animated shimmer strips, 3D held models (separate_transforms) and bow pull frames are all well-formed. */
    private static void testVisualAssets(ServerLevel level) {
        List<String> bad = new ArrayList<>(); int glow = 0, anim = 0, three = 0;
        Set<String> signature = Set.of("quantum_blade", "reality_splitter", "void_reaper", "temporal_staff", "quantum_staff");
        Set<String> mustGlow = Set.of("quantum_blade", "reality_splitter", "void_reaper", "temporal_staff", "quantum_staff", "rift_spear", "quantum_spear", "chaos_hammer",
            "echo_dagger", "quantum_dagger", "falconer_gauntlet", "quantum_bow", "singularity_bow", "overseers_eye", "sovereigns_crown", "reality_gem", "singularity_core");
        for (var id : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
            if (!id.getNamespace().equals(QuantumFalcon.MOD_ID)) continue;
            String n = id.getPath();
            try {
                var json = readJson("/assets/quantumfalcon/models/item/" + n + ".json");
                if (json == null) continue;
                String loader = json.has("loader") ? json.get("loader").getAsString() : "";
                if (loader.equals("neoforge:separate_transforms")) {
                    three++;
                    var base = json.getAsJsonObject("base");
                    // a builtin/generated parent (item/handheld, item/generated) makes the baker ignore elements: the model would be invisible in hand
                    if (base.has("parent")) bad.add(n + ":3d base has parent");
                    if (!base.has("elements") || base.getAsJsonArray("elements").isEmpty()) bad.add(n + ":3d no elements");
                    if (!base.has("display") || !base.getAsJsonObject("display").has("firstperson_righthand")) bad.add(n + ":3d no fp display");
                    var t = base.getAsJsonObject("textures");
                    if (t == null || !t.has("t") || QFTestCommands.class.getResource(texPng(t.get("t").getAsString())) == null) bad.add(n + ":3d atlas missing");
                    var gui = json.getAsJsonObject("perspectives").getAsJsonObject("gui");
                    var icon = gui == null ? null : ResourceLocation.parse(gui.get("parent").getAsString());
                    json = icon == null ? null : readJson("/assets/" + icon.getNamespace() + "/models/" + icon.getPath() + ".json");
                    if (json == null) { bad.add(n + ":3d gui icon model missing"); continue; }
                    loader = json.has("loader") ? json.get("loader").getAsString() : "";
                } else if (signature.contains(n)) bad.add(n + ":not 3d");
                if (loader.equals("neoforge:item_layers")) {
                    var tx = json.getAsJsonObject("textures");
                    if (!tx.has("layer1")) { bad.add(n + ":glow no layer1"); continue; }
                    String png = texPng(tx.get("layer1").getAsString());
                    try (var pin = QFTestCommands.class.getResourceAsStream(png)) {
                        if (pin == null) { bad.add(n + ":glow png missing"); continue; }
                        var img = javax.imageio.ImageIO.read(pin); glow++;
                        int lit = 0; for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) if ((img.getRGB(x, y) >>> 24) > 0) lit++;
                        if (lit == 0) bad.add(n + ":glow empty");
                        var meta = readJson(png + ".mcmeta");
                        if (meta != null) {
                            anim++;
                            int frames = img.getHeight() / img.getWidth();
                            if (img.getHeight() % img.getWidth() != 0 || frames < 2) bad.add(n + ":strip " + img.getWidth() + "x" + img.getHeight());
                            if (meta.getAsJsonObject("animation").has("frames"))
                                for (var f : meta.getAsJsonObject("animation").getAsJsonArray("frames")) {
                                    int idx = f.isJsonObject() ? f.getAsJsonObject().get("index").getAsInt() : f.getAsInt();
                                    if (idx < 0 || idx >= frames) { bad.add(n + ":frame " + idx + "/" + frames); break; }
                                }
                        }
                    }
                    if (json.has("overrides")) for (var o : json.getAsJsonArray("overrides")) {
                        var m = ResourceLocation.parse(o.getAsJsonObject().get("model").getAsString());
                        if (readJson("/assets/" + m.getNamespace() + "/models/" + m.getPath() + ".json") == null) bad.add(n + ":override missing " + m.getPath());
                    }
                } else if (mustGlow.contains(n)) bad.add(n + ":no glow layer");
            } catch (Exception ex) { bad.add(n + ":" + ex.getClass().getSimpleName()); }
        }
        // armour: every mod armour material has 2:1 worn layers (64x32 vanilla size or HD); rift sets have glow layers; GeckoLib sets have model + glowmask + all 8 armour bones
        int worn = 0, wornGlow = 0, geoSets = 0;
        Set<String> seenMat = new HashSet<>();
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (!(item instanceof net.minecraft.world.item.ArmorItem ai)) continue;
            var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (!key.getNamespace().equals(QuantumFalcon.MOD_ID)) continue;
            if (item instanceof com.matt.quantumfalcon.item.gear.armor.QFGeoArmorItem gi && seenMat.add("geo:" + gi.modelName())) {
                geoSets++;
                try {
                    var geo = readJson("/assets/quantumfalcon/geo/item/armor/" + gi.modelName() + ".geo.json");
                    if (geo == null) { bad.add(gi.modelName() + ":geo missing"); continue; }
                    Set<String> bones = new HashSet<>(); int cubes = 0;
                    for (var b : geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
                        bones.add(b.getAsJsonObject().get("name").getAsString());
                        if (b.getAsJsonObject().has("cubes")) cubes += b.getAsJsonObject().getAsJsonArray("cubes").size();
                    }
                    for (String need : List.of("armorHead", "armorBody", "armorRightArm", "armorLeftArm", "armorRightLeg", "armorLeftLeg", "armorRightBoot", "armorLeftBoot"))
                        if (!bones.contains(need)) bad.add(gi.modelName() + ":no bone " + need);
                    if (cubes < 24) bad.add(gi.modelName() + ":only " + cubes + " cubes");
                    for (String f : List.of("/assets/quantumfalcon/textures/item/armor/" + gi.modelName() + ".png", "/assets/quantumfalcon/textures/item/armor/" + gi.modelName() + "_glowmask.png",
                                            "/assets/quantumfalcon/animations/item/armor/" + gi.modelName() + ".animation.json"))
                        if (QFTestCommands.class.getResource(f) == null) bad.add(gi.modelName() + ":missing " + f.substring(f.lastIndexOf('/') + 1));
                } catch (Exception ex) { bad.add(gi.modelName() + ":" + ex); }
            }
            for (var layer : ai.getMaterial().value().layers()) for (boolean inner : new boolean[]{false, true}) {
                var tex = layer.texture(inner);
                if (!seenMat.add(tex.toString())) continue;
                String png = "/assets/" + tex.getNamespace() + "/" + tex.getPath();
                try (var pin = QFTestCommands.class.getResourceAsStream(png)) {
                    if (pin == null) { bad.add(key.getPath() + ":worn missing " + tex.getPath()); continue; }
                    var img = javax.imageio.ImageIO.read(pin); worn++;
                    if (img.getWidth() != 2 * img.getHeight() || img.getWidth() < 64) bad.add(tex.getPath() + ":" + img.getWidth() + "x" + img.getHeight());
                    if (QFTestCommands.class.getResource(png.replace(".png", "_glow.png")) != null) wornGlow++;
                } catch (Exception ex) { bad.add(tex.getPath() + ":" + ex.getClass().getSimpleName()); }
            }
        }
        boolean pass = bad.isEmpty() && three >= 5 && glow >= 50 && anim >= 10 && worn >= 28 && wornGlow >= 20 && geoSets >= 4;
        result("visual_assets", pass, three + " 3D held models, " + glow + " glow layers, " + anim + " animated, " + worn + " worn layers (" + wornGlow + " glowing), "
            + geoSets + " GeckoLib armour sets" + (bad.isEmpty() ? "" : "; bad: " + bad));
    }

    /** Particles have definitions + sprites; every themed weapon produces hit FX; full sets produce auras; new sounds exist. */
    private static void testWeaponFx(ServerLevel level) {
        List<String> bad = new ArrayList<>(); int parts = 0, weapons = 0, auras = 0, sounds = 0;
        for (var id : net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.keySet()) {
            if (!id.getNamespace().equals(QuantumFalcon.MOD_ID)) continue;
            try {
                var j = readJson("/assets/quantumfalcon/particles/" + id.getPath() + ".json");
                if (j == null) { bad.add("particle " + id.getPath() + ": no definition"); continue; }
                var tex = j.getAsJsonArray("textures");
                if (tex.isEmpty()) bad.add("particle " + id.getPath() + ": no textures");
                for (var t : tex) if (QFTestCommands.class.getResource(texPng(t.getAsString().replace("quantumfalcon:", "quantumfalcon:particle/"))) == null) bad.add("particle sprite missing " + t.getAsString());
                parts++;
            } catch (Exception ex) { bad.add(id.getPath() + ":" + ex); }
        }
        FakePlayer fp = tester(level);
        BlockPos p = new BlockPos(5000, level.getMaxBuildHeight() - 20, 5000);
        fp.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        var dummy = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        dummy.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 2, 0, 0); dummy.setNoAi(true);
        for (var e : com.matt.quantumfalcon.item.weapon.WeaponHitFx.COLOURS.entrySet()) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, e.getKey()));
            if (item == net.minecraft.world.item.Items.AIR) { bad.add("weapon missing " + e.getKey()); continue; }
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(item));
            boolean bow = e.getKey().endsWith("_bow");
            try {
                if (com.matt.quantumfalcon.item.weapon.WeaponHitFx.hitFx(fp, dummy) == bow) bad.add(e.getKey() + ": hit fx " + (bow ? "on bow" : "missing"));
                else weapons++;
            } catch (Exception ex) { bad.add(e.getKey() + ":" + ex); }
        }
        try {
            var w = com.matt.quantumfalcon.item.weapon.WeaponFx.class;
            com.matt.quantumfalcon.item.weapon.WeaponFx.shock(fp, fp.position(), 0xFF00FF, 3);
            com.matt.quantumfalcon.item.weapon.WeaponFx.runeCircle(fp, fp.position(), 0xFF00FF, 3);
            com.matt.quantumfalcon.item.weapon.WeaponFx.trail(fp, fp.position(), fp.position().add(5, 0, 0), 0xFF00FF);
        } catch (Exception ex) { bad.add("fx helpers: " + ex); }
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        for (String set : com.matt.quantumfalcon.item.gear.FamilySetBonuses.SETS) {
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
                String piece = switch (slot) { case HEAD -> "helmet"; case CHEST -> "chestplate"; case LEGS -> "leggings"; default -> "boots"; };
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, set + "_" + piece));
                fp.setItemSlot(slot, new ItemStack(item));
            }
            String shown = com.matt.quantumfalcon.item.gear.FamilySetBonuses.ambient(fp);
            if (set.equals(shown)) auras++; else bad.add("aura " + set + " -> " + shown);
        }
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) fp.setItemSlot(slot, ItemStack.EMPTY);
        dummy.discard();
        try {
            var sj = readJson("/assets/quantumfalcon/sounds.json");
            for (String k : List.of("weapon_swing", "weapon_rift_slash", "weapon_void_reap", "weapon_staff_cast", "weapon_hammer_slam", "weapon_blink", "weapon_temporal_tick", "armor_set_complete")) {
                if (!net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, k))) { bad.add("sound event " + k); continue; }
                if (sj == null || !sj.has(k)) { bad.add("sounds.json " + k); continue; }
                for (var snd : sj.getAsJsonObject(k).getAsJsonArray("sounds")) {
                    var rl = ResourceLocation.parse(snd.getAsJsonObject().get("name").getAsString());
                    if (QFTestCommands.class.getResource("/assets/" + rl.getNamespace() + "/sounds/" + rl.getPath() + ".ogg") == null) bad.add("ogg missing " + rl);
                }
                sounds++;
            }
        } catch (Exception ex) { bad.add("sounds: " + ex); }
        boolean pass = bad.isEmpty() && parts >= 42 && weapons == 12 && auras == com.matt.quantumfalcon.item.gear.FamilySetBonuses.SETS.size() && sounds == 8;
        result("weapon_fx", pass, parts + " particle defs, " + weapons + " weapons with FX, " + auras + " set auras, " + sounds + " new sounds" + (bad.isEmpty() ? "" : "; bad: " + bad));
    }

    /** Every mod item's layer0 texture must be a real inventory icon: <=32px (or animated) with a transparent background. */
    private static void testItemIcons(ServerLevel level) {
        Set<String> fullTile = Set.of("falcon_spawn_egg", "legendary_falcon_feather", "quantum_dust", "quantum_falcon_egg");
        List<String> bad = new ArrayList<>(); int checked = 0;
        for (var id : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
            if (!id.getNamespace().equals(QuantumFalcon.MOD_ID)) continue;
            String model = "/assets/quantumfalcon/models/item/" + id.getPath() + ".json";
            try (var in = QFTestCommands.class.getResourceAsStream(model)) {
                if (in == null) continue;   // block items / template eggs without their own model file
                var json = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                if (json.has("perspectives")) {   // 3D held model: check its flat GUI icon instead
                    var icon = ResourceLocation.parse(json.getAsJsonObject("perspectives").getAsJsonObject("gui").get("parent").getAsString());
                    json = readJson("/assets/" + icon.getNamespace() + "/models/" + icon.getPath() + ".json");
                    if (json == null) { bad.add(id.getPath() + ":icon model missing"); continue; }
                }
                if (!json.has("textures") || !json.getAsJsonObject("textures").has("layer0")) continue;
                var tex = ResourceLocation.parse(json.getAsJsonObject("textures").get("layer0").getAsString());
                if (!tex.getNamespace().equals(QuantumFalcon.MOD_ID)) continue;
                String png = "/assets/quantumfalcon/textures/" + tex.getPath() + ".png";
                try (var pin = QFTestCommands.class.getResourceAsStream(png)) {
                    if (pin == null) { bad.add(id.getPath() + ":missing"); continue; }
                    var img = javax.imageio.ImageIO.read(pin);
                    checked++;
                    boolean animated = QFTestCommands.class.getResource(png + ".mcmeta") != null;
                    String name = tex.getPath().substring(tex.getPath().lastIndexOf('/') + 1);
                    if (fullTile.contains(name) || fullTile.contains(id.getPath()) || animated) continue;
                    boolean corner = (img.getRGB(0, 0) >>> 24) == 0xFF && (img.getRGB(img.getWidth() - 1, img.getHeight() - 1) >>> 24) == 0xFF;
                    if (img.getWidth() > 32 && corner) bad.add(id.getPath() + ":" + img.getWidth() + "px opaque");
                }
            } catch (Exception ex) { bad.add(id.getPath() + ":" + ex.getClass().getSimpleName()); }
        }
        result("item_icons", bad.isEmpty() && checked > 100, checked + " item icons checked" + (bad.isEmpty() ? "" : "; bad: " + bad));
    }

    // ------------------------------------------------------------------ stage 3 army entities
    private static List<ItemStack> mobLoot(ServerLevel level, net.minecraft.world.entity.LivingEntity mob, FakePlayer fp) {
        var table = level.getServer().reloadableRegistries().getLootTable(mob.getLootTable());
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, mob)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, mob.position())
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(fp))
            .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY, fp)
            .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER, fp)
            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
        return table.getRandomItems(params);
    }

    private static Set<String> lootIds(ServerLevel level, net.minecraft.world.entity.LivingEntity mob, FakePlayer fp, int rolls) {
        Set<String> ids = new TreeSet<>();
        for (int i = 0; i < rolls; i++) for (ItemStack st : mobLoot(level, mob, fp)) if (!st.isEmpty()) ids.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).getPath());
        return ids;
    }

    /** Goblin Trader, Quantum Merchant, Quantum Stalker, Eclipse Phantom, Void Lord: spawn, stats, trades, abilities, loot, spawn data. */
    private static void testArmyEntities(ServerLevel level) {
        FakePlayer fp = tester(level);
        List<String> ok = new ArrayList<>(), bad = new ArrayList<>();
        java.util.function.BiConsumer<String, Boolean> check = (n, b) -> (b ? ok : bad).add(n);
        int[] slot = {0};
        java.util.function.Supplier<BlockPos> arena = () -> { BlockPos p = pad(level, 3800 + (slot[0]++) * 24, 3900, Blocks.STONE); cleanup(level, p, 10); return p; };
        // Goblin Trader
        BlockPos p = arena.get();
        var gob = com.matt.quantumfalcon.registry.ModEntities.GOBLIN_TRADER.get().create(level);
        gob.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0); level.addFreshEntity(gob);
        var offers = gob.getOffers();
        long eggs = offers.stream().filter(o -> o.getResult().is(ModItems.FALCON_EGG.get())).count();
        var egg = offers.get(0).getResult().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        boolean eggTag = egg != null && egg.copyTag().contains("Parent1Genetics") && egg.copyTag().contains("Variant");
        check.accept("goblin hp=" + gob.getMaxHealth() + " offers=" + offers.size() + " eggs=" + eggs + " eggTag=" + eggTag, gob.isAlive() && gob.getMaxHealth() == 20 && offers.size() == 5 && eggs == 2 && eggTag);
        Set<String> gl = lootIds(level, gob, fp, 10);
        check.accept("goblin loot " + gl, gl.contains("falcon_feather"));
        // Quantum Merchant
        p = arena.get();
        var mer = com.matt.quantumfalcon.registry.ModEntities.QUANTUM_MERCHANT.get().create(level);
        mer.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0); level.addFreshEntity(mer);
        var mo = mer.getOffers();
        long day = level.getDayTime() / 24000L;
        mer.dailyRestock(day);
        for (int i = 0; i < 10; i++) mo.get(1).increaseUses();
        boolean used = mo.get(1).isOutOfStock();
        boolean restocked = mer.dailyRestock(day + 1) && !mo.get(1).isOutOfStock() && !mer.dailyRestock(day + 1);
        check.accept("merchant hp=" + mer.getMaxHealth() + " offers=" + mo.size() + " soldOut=" + used + " restock=" + restocked,
            mer.getMaxHealth() == 30 && mo.size() == 7 && mo.get(0).getResult().is(ModItems.FALCON_EGG.get()) && used && restocked);
        // Quantum Stalker
        p = arena.get();
        var st = com.matt.quantumfalcon.registry.ModEntities.QUANTUM_STALKER.get().create(level);
        st.moveTo(p.getX() + 0.5, p.getY(), p.getZ() - 5.5, 0, 0); level.addFreshEntity(st);
        var vil = EntityType.VILLAGER.create(level); vil.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 2.5, 0f, 0f); vil.setNoAi(true); level.addFreshEntity(vil);
        boolean phased = st.phaseBehind(vil) && st.distanceTo(vil) < 4 && st.getZ() < vil.getZ() - 1;
        boolean weak = st.doHurtTarget(vil) && vil.hasEffect(MobEffects.WEAKNESS);
        check.accept("stalker hp=" + st.getMaxHealth() + " dmg=" + st.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) + " phase=" + phased + " weakness=" + weak,
            st.getMaxHealth() == 30 && phased && weak);
        Set<String> sl = lootIds(level, st, fp, 30);
        check.accept("stalker loot " + sl, sl.contains("rift_shard"));
        vil.discard(); st.discard();
        // Eclipse Phantom
        BlockPos ep = arena.get();
        var ecl = com.matt.quantumfalcon.registry.ModEntities.ECLIPSE_PHANTOM.get().create(level);
        ecl.moveTo(ep.getX() + 0.5, ep.getY() + 3, ep.getZ() + 0.5, 0, 0); level.addFreshEntity(ecl);
        var z1 = dummy(level, ep, 4, 0); var z2 = dummy(level, ep, 0, 20);
        int hitN0 = ecl.darknessWave();
        boolean wave0 = z1.hasEffect(MobEffects.DARKNESS) && z1.hasEffect(MobEffects.BLINDNESS) && !z2.hasEffect(MobEffects.DARKNESS);
        double dmg1 = ecl.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        ecl.setHealth(ecl.getMaxHealth() * 0.5f);
        Set<String> el = lootIds(level, ecl, fp, 3);
        check.accept("eclipse loot " + el, el.containsAll(List.of("diamond", "emerald", "void_shard")));   // the star is a 10% roll now (cx10_cx11_stars)
        // Void Lord
        BlockPos vp = arena.get();
        var lord = com.matt.quantumfalcon.registry.ModEntities.VOID_LORD.get().create(level);
        lord.moveTo(vp.getX() + 0.5, vp.getY(), vp.getZ() + 0.5, 0, 0); level.addFreshEntity(lord);
        var lv = EntityType.VILLAGER.create(level); lv.moveTo(vp.getX() + 0.5, vp.getY(), vp.getZ() + 9.5, 0f, 0f); lv.setNoAi(true); level.addFreshEntity(lv);
        lord.setTarget(lv);
        int minions = lord.summonMinions();
        boolean immune = !lord.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WITHER, 100)) && !lord.hasEffect(MobEffects.WITHER);
        lord.setHealth(lord.getMaxHealth() * 0.4f);
        boolean tp = lord.teleportNear(lv) && lord.distanceTo(lv) < 5.5;
        check.accept("void_lord hp=" + lord.getMaxHealth() + " minions=" + minions + " witherImmune=" + immune + " teleport=" + tp,
            lord.getMaxHealth() == 800 && minions >= 3 && minions <= 5 && immune && tp);
        Set<String> vl = lootIds(level, lord, fp, 3);
        check.accept("void_lord loot " + vl, vl.containsAll(List.of("quantum_core", "void_steel_ingot", "void_essence")) && !vl.contains("void_heart"));   // star 10%; void_heart is dead
        // spawn data present
        var mods = level.registryAccess().registryOrThrow(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        List<String> missing = new ArrayList<>();
        for (String n : List.of("goblin_trader", "quantum_merchant", "quantum_stalker", "eclipse_phantom", "void_lord")) {
            if (!mods.containsKey(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, n + "_spawns"))) missing.add(n + "_spawns");
            if (net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, n + "_spawn_egg")) == Items.AIR) missing.add(n + "_spawn_egg");
        }
        check.accept("spawn modifiers+eggs missing=" + missing, missing.isEmpty());
        // let the bosses tick: phase change + flight
        later(40, () -> {
            double dmg2 = ecl.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            // on a cold server the entities spawned this same tick are not always in the section index yet: fire the wave again once they have ticked
            int hitN = hitN0 > 0 ? hitN0 : ecl.darknessWave();
            boolean wave = wave0 || (z1.hasEffect(MobEffects.DARKNESS) && z1.hasEffect(MobEffects.BLINDNESS) && !z2.hasEffect(MobEffects.DARKNESS));
            boolean flying = ecl.isNoGravity() && ecl.getY() > ep.getY() + 1;
            check.accept("eclipse hp=" + ecl.getMaxHealth() + " waveHit=" + hitN + " wave=" + wave + " phase=" + ecl.getPhase() + " dmg " + dmg1 + "->" + dmg2 + " flying=" + flying,
                ecl.getMaxHealth() == 500 && wave && hitN >= 1 && ecl.getPhase() == 2 && dmg2 == dmg1 + 5 && flying);
            for (var e : level.getEntitiesOfClass(com.matt.quantumfalcon.entity.VoidEchoEntity.class, lord.getBoundingBox().inflate(30))) e.discard();
            for (Entity e : new Entity[]{gob, mer, ecl, z1, z2, lord, lv}) e.discard();
            result("army_entities", bad.isEmpty(), ok.size() + " army checks OK" + (bad.isEmpty() ? ": " + ok : "; FAILED: " + bad + "; OK: " + ok));
        });
    }

    // ------------------------------------------------------------------ falcon variant abilities
    /** Force-load the chunks covering the test area and wait so freshly added entities are visible and ticking. */
    private static void warmThen(ServerLevel level, int x0, int z0, int x1, int z1, Runnable body) {
        for (int cx = (x0 - 16) >> 4; cx <= (x1 + 16) >> 4; cx++)
            for (int cz = (z0 - 16) >> 4; cz <= (z1 + 16) >> 4; cz++) level.setChunkForced(cx, cz, true);
        // a fixed 40 ticks was not enough on a cold server (run alone, the forced chunks were not entity-ticking yet and every
        // dummy spawned there was invisible to the weapon/AoE queries): wait until both ends of the strip tick entities
        BlockPos a = new BlockPos(x0, 64, z0), b = new BlockPos(x1, 64, z1);
        later(40, () -> WorldDropsTests.whenTicking(level, a, 0, () -> WorldDropsTests.whenTicking(level, b, 0, body)));
    }

    private static net.minecraft.world.entity.monster.Zombie dummy(ServerLevel level, BlockPos p, int dx, int dz) {
        var z = EntityType.ZOMBIE.create(level);
        z.moveTo(p.getX() + 0.5 + dx, p.getY(), p.getZ() + 0.5 + dz, 0, 0);
        z.setNoAi(true); z.setPersistenceRequired();
        z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // no sun burn
        level.addFreshEntity(z);
        return z;
    }

    private static QuantumFalconEntity variantFalcon(ServerLevel level, BlockPos p, String variant, java.util.UUID owner) {
        QuantumFalconEntity f = falcon(level, p, true);
        f.setVariant(variant);
        f.setTame(true, false);
        f.setOwnerUUID(owner);
        f.setStamina(QuantumFalconEntity.MAX_STAMINA);
        f.setAbilityCooldown(0);
        return f;
    }

    private static void testFalconAbilities(ServerLevel level) {
        FakePlayer fp = tester(level);
        PlayerSkillData.get(fp).resetSkills();
        List<String> ok = new ArrayList<>(), bad = new ArrayList<>();
        int i = 0;
        for (var a : com.matt.quantumfalcon.entity.ability.FalconAbility.values()) {
            BlockPos p = pad(level, 3400 + (i++) * 24, 3400, a == com.matt.quantumfalcon.entity.ability.FalconAbility.DEEPSLATE ? Blocks.DIAMOND_ORE : Blocks.STONE);
            cleanup(level, p, 10);
            boolean teleportOwner = a == com.matt.quantumfalcon.entity.ability.FalconAbility.WARPED || a == com.matt.quantumfalcon.entity.ability.FalconAbility.ENTANGLED_PRISM;
            LivingEntity owner;
            if (teleportOwner) {
                var v = EntityType.VILLAGER.create(level);
                v.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5 - 3, 0, 0); v.setNoAi(true); level.addFreshEntity(v);
                owner = v;
            } else {
                fp.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5 - 3, 0, 0);
                fp.setHealth(10f); fp.removeAllEffects(); fp.clearFire();
                owner = fp;
            }
            QuantumFalconEntity f = variantFalcon(level, p.offset(2, 0, 0), a.variant, owner.getUUID());
            var z = dummy(level, p, 0, 2);
            var pig = EntityType.PIG.create(level);
            if (a == com.matt.quantumfalcon.entity.ability.FalconAbility.PRAIRIE) { pig.moveTo(p.getX() - 2.5, p.getY(), p.getZ() + 0.5); pig.setNoAi(true); level.addFreshEntity(pig); }
            if (a == com.matt.quantumfalcon.entity.ability.FalconAbility.ABYSSAL) z.setHealth(10f);
            Vec3 ownerBefore = owner.position(), falconBefore = f.position();
            float zBefore = z.getHealth(), staminaBefore = f.getStamina();
            var r = com.matt.quantumfalcon.entity.ability.FalconAbilities.activate(f, owner);
            boolean effect = switch (a) {
                case PEREGRINE, STORMRIDER, ABYSSAL, COSMIC -> z.getHealth() < zBefore || !z.isAlive();
                case PRAIRIE -> !pig.isAlive();
                case TUNDRA -> z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && z.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 3;
                case CRIMSON -> z.hasEffect(MobEffects.WITHER);
                case WARPED -> owner.position().distanceTo(ownerBefore) >= 2;
                case DEEPSLATE -> r.affected() > 0;
                case VOIDSTALKER -> z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && z.getHealth() < zBefore;
                case AETHERWING -> owner.hasEffect(MobEffects.LEVITATION) && owner.hasEffect(MobEffects.SLOW_FALLING);
                case CORAL -> owner.getHealth() > 10f && owner.hasEffect(MobEffects.REGENERATION);
                case MUSHROOM -> (z.hasEffect(MobEffects.POISON) || z.hasEffect(MobEffects.WEAKNESS)) && owner.hasEffect(MobEffects.REGENERATION);
                case ENTANGLED_PRISM -> owner.position().distanceTo(falconBefore) < 0.5 && f.position().distanceTo(ownerBefore) < 0.5;
                case SOULFIRE, QUANTUM_PHOENIX -> z.isOnFire() && z.getHealth() < zBefore;
                case TEMPEST -> z.getDeltaMovement().horizontalDistance() > 0.3;
                case SINGULARITY -> z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && z.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 2;
            };
            boolean cd = f.getAbilityCooldown() > 0 && Math.abs(staminaBefore - f.getStamina() - 20f) < 0.01f;
            boolean blocked = !com.matt.quantumfalcon.entity.ability.FalconAbilities.activate(f, owner).ok();
            String line = a.variant + "(" + r.detail() + ")";
            if (r.ok() && effect && cd && blocked) ok.add(line); else bad.add(line + " ok=" + r.ok() + " effect=" + effect + " cd/stamina=" + cd + " blocked=" + blocked);
            if (teleportOwner) owner.discard();
            cleanup(level, p, 12);
            if (a == com.matt.quantumfalcon.entity.ability.FalconAbility.DEEPSLATE) pad(level, p.getX(), p.getZ(), Blocks.STONE);
        }
        fp.setHealth(fp.getMaxHealth()); fp.removeAllEffects(); fp.clearFire();
        result("falcon_abilities", bad.isEmpty(), ok.size() + "/18 actives OK" + (bad.isEmpty() ? ": " + ok : "; FAILED: " + bad));
    }

    private static void testFalconPassives(ServerLevel level) {
        FakePlayer fp = tester(level);
        PlayerSkillData.get(fp).resetSkills();
        BlockPos p = pad(level, 3400, 3460, Blocks.STONE);
        cleanup(level, p, 10);
        fp.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0); fp.removeAllEffects();
        List<String> bad = new ArrayList<>(), ok = new ArrayList<>();
        var A = com.matt.quantumfalcon.entity.ability.FalconAbilities.class;
        // Keen Eye
        QuantumFalconEntity f = variantFalcon(level, p.offset(2, 0, 0), "peregrine", fp.getUUID());
        com.matt.quantumfalcon.entity.ability.FalconAbilities.tickPassive(f, fp);
        (fp.hasEffect(MobEffects.MOVEMENT_SPEED) ? ok : bad).add("peregrine speed"); f.discard();
        // Tailwind
        f = variantFalcon(level, p.offset(2, 0, 0), "stormrider", fp.getUUID());
        com.matt.quantumfalcon.entity.ability.FalconAbilities.tickPassive(f, fp);
        (fp.hasEffect(MobEffects.JUMP) && fp.getEffect(MobEffects.JUMP).getAmplifier() == 1 ? ok : bad).add("stormrider jump II"); f.discard();
        // Frostfeather + Warped Spores + Deep Gaze on a dummy
        var z = dummy(level, p, 0, 3);
        for (String v : new String[]{"tundra", "warped", "abyssal"}) {
            f = variantFalcon(level, p.offset(2, 0, 0), v, fp.getUUID());
            com.matt.quantumfalcon.entity.ability.FalconAbilities.tickPassive(f, fp); f.discard();
        }
        (z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) ? ok : bad).add("tundra slow");
        (z.hasEffect(MobEffects.WEAKNESS) ? ok : bad).add("warped weakness");
        (z.hasEffect(MobEffects.GLOWING) ? ok : bad).add("abyssal glow");
        // Bloodlust only below half health
        f = variantFalcon(level, p.offset(2, 0, 0), "crimson", fp.getUUID());
        fp.removeAllEffects(); fp.setHealth(fp.getMaxHealth());
        boolean fullNo = !com.matt.quantumfalcon.entity.ability.FalconAbilities.tickPassive(f, fp);
        fp.setHealth(6f);
        boolean lowYes = com.matt.quantumfalcon.entity.ability.FalconAbilities.tickPassive(f, fp) && fp.hasEffect(MobEffects.DAMAGE_BOOST);
        (fullNo && lowYes ? ok : bad).add("crimson bloodlust"); f.discard(); fp.setHealth(fp.getMaxHealth());
        // Static Charge
        f = variantFalcon(level, p.offset(2, 0, 0), "tempest", fp.getUUID());
        float zh = z.getHealth();
        boolean sc1 = com.matt.quantumfalcon.entity.ability.FalconAbilities.staticCharge(fp, z);
        boolean sc2 = com.matt.quantumfalcon.entity.ability.FalconAbilities.staticCharge(fp, z);
        (sc1 && z.getHealth() < zh && !sc2 ? ok : bad).add("tempest static charge (+1s internal cd)"); f.discard();
        // Entangled Bond
        f = variantFalcon(level, p.offset(2, 0, 0), "entangled_prism", fp.getUUID());
        float fh = f.getHealth();
        float left = com.matt.quantumfalcon.entity.ability.FalconAbilities.entangledBond(fp, 8f);
        (Math.abs(left - 6f) < 0.01f && f.getHealth() < fh ? ok : bad).add("entangled bond 8->" + left); f.discard();
        // Rebirth
        f = variantFalcon(level, p.offset(2, 0, 0), "quantum_phoenix", fp.getUUID());
        boolean rb1 = com.matt.quantumfalcon.entity.ability.FalconAbilities.tryRebirth(fp);
        boolean rb2 = com.matt.quantumfalcon.entity.ability.FalconAbilities.tryRebirth(fp);
        (rb1 && !rb2 && f.getRebirthCooldown() == com.matt.quantumfalcon.entity.ability.FalconAbilities.PHOENIX_REBIRTH_COOLDOWN ? ok : bad).add("phoenix rebirth once per 10 min");
        f.discard(); fp.setHealth(fp.getMaxHealth()); fp.removeAllEffects();
        // Forager / Spore Snack / Event Horizon
        f = variantFalcon(level, p.offset(2, 0, 0), "prairie", fp.getUUID());
        (!com.matt.quantumfalcon.entity.ability.FalconAbilities.forage(f, fp).isEmpty() ? ok : bad).add("prairie forage"); f.discard();
        fp.getFoodData().setFoodLevel(10);
        (com.matt.quantumfalcon.entity.ability.FalconAbilities.sporeSnack(fp) && fp.getFoodData().getFoodLevel() == 11 ? ok : bad).add("mushroom snack");
        fp.getFoodData().setFoodLevel(20);
        cleanup(level, p, 10);
        ItemEntity drop = new ItemEntity(level, p.getX() + 6.5, p.getY(), p.getZ() + 0.5, new ItemStack(Items.DIAMOND));
        level.addFreshEntity(drop);
        double d0 = drop.distanceTo(fp);
        int pulled = com.matt.quantumfalcon.entity.ability.FalconAbilities.eventHorizon(fp);
        (pulled >= 1 && drop.distanceTo(fp) < d0 ? ok : bad).add("singularity event horizon");
        drop.discard();
        // Passive radius: perch doubles, quantum_sight x1.5
        f = variantFalcon(level, p.offset(2, 0, 0), "peregrine", fp.getUUID());
        float r0 = com.matt.quantumfalcon.entity.ability.FalconAbilities.passiveRadius(f, fp);
        PlayerSkillData.get(fp).unlockSkillAdmin("rift_sight");
        float r1 = com.matt.quantumfalcon.entity.ability.FalconAbilities.passiveRadius(f, fp);
        PlayerSkillData.get(fp).resetSkills();
        (r0 == 16f && r1 == 24f ? ok : bad).add("radius " + r0 + "->" + r1 + " with rift_sight");
        f.discard();
        result("falcon_passives", bad.isEmpty(), ok.size() + " passive checks OK" + (bad.isEmpty() ? ": " + ok : "; FAILED: " + bad));
    }

    private static void testFalconAbilityHooks(ServerLevel level) {
        FakePlayer fp = tester(level);
        PlayerSkillData.get(fp).resetSkills();
        BlockPos p = pad(level, 3400, 3500, Blocks.STONE);
        cleanup(level, p, 10);
        QuantumFalconEntity f = variantFalcon(level, p, "tempest", fp.getUUID());   // 20 s base
        var FA = com.matt.quantumfalcon.entity.ability.FalconAbilities.class;
        int base = com.matt.quantumfalcon.entity.ability.FalconAbilities.cooldownTicks(f, fp);
        float costBase = com.matt.quantumfalcon.entity.ability.FalconAbilities.staminaCost(f, fp);
        PlayerSkillData.get(fp).unlockSkillAdmin("rift_focus");
        PlayerSkillData.get(fp).unlockSkillAdmin("bond_endurance");
        int skillCd = com.matt.quantumfalcon.entity.ability.FalconAbilities.cooldownTicks(f, fp);
        float costSkill = com.matt.quantumfalcon.entity.ability.FalconAbilities.staminaCost(f, fp);
        PlayerSkillData.get(fp).resetSkills();
        f.applyBerryBoost(600);
        int berryCd = com.matt.quantumfalcon.entity.ability.FalconAbilities.cooldownTicks(f, fp);
        float costBerry = com.matt.quantumfalcon.entity.ability.FalconAbilities.staminaCost(f, fp);
        f.setBodyArmorItem(new ItemStack(ModItems.QUANTUM_FALCON_BARDING.get()));
        int bardCd = com.matt.quantumfalcon.entity.ability.FalconAbilities.cooldownTicks(f, fp);
        // exhausted falcon refuses
        QuantumFalconEntity g = variantFalcon(level, p.offset(2, 0, 0), "coral", fp.getUUID());
        g.setStamina(5f);
        boolean tired = !com.matt.quantumfalcon.entity.ability.FalconAbilities.activate(g, fp).ok();
        // cooldown ticks down (server tick), faster on a quantum perch is covered by isOnQuantumPerch branch
        g.setStamina(QuantumFalconEntity.MAX_STAMINA); g.setAbilityCooldown(40);
        later(20, () -> {
            boolean ticking = g.getAbilityCooldown() <= 21 && g.getAbilityCooldown() >= 18;
            boolean pass = base == 400 && skillCd == 280 && berryCd == 200 && bardCd == 100 && costBase == 20f && costSkill == 10f && costBerry == 0f && tired && ticking;
            result("falcon_ability_hooks", pass, "cooldown base=" + base + " blink_cooldown=" + skillCd + " berry=" + berryCd + " berry+quantumBarding=" + bardCd
                    + " | stamina cost base=" + costBase + " flight_stamina=" + costSkill + " berry=" + costBerry + " | tiredRefused=" + tired + " | cd after 20t=" + g.getAbilityCooldown());
            f.discard(); g.discard();
        });
    }

    private static void testFalconGenetics(ServerLevel level) {
        BlockPos p = pad(level, 3400, 3540, Blocks.GRASS_BLOCK);
        cleanup(level, p, 10);
        List<String> bad = new ArrayList<>(), ok = new ArrayList<>();
        // variant survives genetics creation (used to be re-rolled)
        QuantumFalconEntity f = falcon(level, p, true);
        f.setVariant("warped");
        var g = f.getOrCreateFalconGenetics();
        ("warped".equals(f.getVariant()) && "warped".equals(g.getColorGene()) ? ok : bad).add("variant kept: " + f.getVariant() + "/" + g.getColorGene());
        f.discard();
        // all 18 variants round-trip through the colour gene
        int rt = 0;
        for (String v : QuantumFalconEntity.VARIANTS) if (v.equals(com.matt.quantumfalcon.genetics.FalconGenetics.getVariantFromColorGene(v))) rt++;
        (rt == QuantumFalconEntity.VARIANTS.length ? ok : bad).add(rt + " variants round-trip");
        // breeding outcomes
        var rnd = net.minecraft.util.RandomSource.create(42);
        var wa = com.matt.quantumfalcon.genetics.FalconGenetics.forVariant("warped", rnd);
        var ab = com.matt.quantumfalcon.genetics.FalconGenetics.forVariant("abyssal", rnd);
        var co = com.matt.quantumfalcon.genetics.FalconGenetics.forVariant("cosmic", rnd);
        var vo = com.matt.quantumfalcon.genetics.FalconGenetics.forVariant("voidstalker", rnd);
        var st = com.matt.quantumfalcon.genetics.FalconGenetics.forVariant("stormrider", rnd);
        java.util.Map<String, Integer> riftKids = new java.util.TreeMap<>(), legKids = new java.util.TreeMap<>(), prismKids = new java.util.TreeMap<>();
        for (int i = 0; i < 2000; i++) prismKids.merge(com.matt.quantumfalcon.genetics.FalconGenetics.breed(st, vo, rnd).getVariantFromGenes(), 1, Integer::sum);
        (prismKids.getOrDefault("entangled_prism", 0) > 200 ? ok : bad).add("stormrider x voidstalker " + prismKids);
        for (int i = 0; i < 2000; i++) {
            riftKids.merge(com.matt.quantumfalcon.genetics.FalconGenetics.breed(wa, ab, rnd).getVariantFromGenes(), 1, Integer::sum);
            legKids.merge(com.matt.quantumfalcon.genetics.FalconGenetics.breed(co, vo, rnd).getVariantFromGenes(), 1, Integer::sum);
        }
        int parents = riftKids.getOrDefault("warped", 0) + riftKids.getOrDefault("abyssal", 0);
        (parents > 1400 && riftKids.getOrDefault("cosmic", 0) > 100 ? ok : bad).add("warped x abyssal " + riftKids);
        (legKids.getOrDefault("quantum_phoenix", 0) > 100 ? ok : bad).add("cosmic x voidstalker " + legKids);
        // Echo Rift spawn mapping
        String m = QuantumFalconEntity.riftVariantFor(ResourceLocation.fromNamespaceAndPath("quantumfalcon", "void_forest")) + ","
                + QuantumFalconEntity.riftVariantFor(ResourceLocation.fromNamespaceAndPath("quantumfalcon", "abyssal_depths")) + ","
                + QuantumFalconEntity.riftVariantFor(ResourceLocation.fromNamespaceAndPath("quantumfalcon", "entanglement_web")) + ","
                + QuantumFalconEntity.riftVariantFor(ResourceLocation.fromNamespaceAndPath("quantumfalcon", "paradox_zone")) + ","
                + QuantumFalconEntity.riftVariantFor(ResourceLocation.fromNamespaceAndPath("quantumfalcon", "dimensional_apex")) + ","
                + QuantumFalconEntity.riftVariantFor(ResourceLocation.fromNamespaceAndPath("minecraft", "plains"));
        ("voidstalker,abyssal,entangled_prism,warped,aetherwing,null".equals(m) ? ok : bad).add("rift spawns " + m);
        result("falcon_genetics", bad.isEmpty(), (bad.isEmpty() ? "" : "FAILED: " + bad + "; ") + "OK: " + ok);
    }

    private static void testFalconBarding(ServerLevel level) {
        FakePlayer fp = tester(level);
        BlockPos p = pad(level, 3400, 3580, Blocks.GRASS_BLOCK);
        cleanup(level, p, 10);
        QuantumFalconEntity f = variantFalcon(level, p, "peregrine", fp.getUUID());
        double armor0 = f.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        interact(fp, f, ModItems.DIAMOND_FALCON_BARDING.get(), 1);
        double armorD = f.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        boolean consumed = fp.getMainHandItem().isEmpty();
        interact(fp, f, ModItems.QUANTUM_FALCON_BARDING.get(), 1);
        double armorQ = f.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        boolean swappedBack = fp.getInventory().countItem(ModItems.DIAMOND_FALCON_BARDING.get()) == 1;
        // quantum barding phases through ~20% of hits
        int absorbed = 0;
        for (int i = 0; i < 200; i++) {
            f.setHealth(f.getMaxHealth()); f.invulnerableTime = 0;
            float h = f.getHealth();
            f.hurt(level.damageSources().generic(), 1f);
            if (f.getHealth() >= h) absorbed++;
        }
        // save/load keeps barding
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        f.saveWithoutId(tag);
        QuantumFalconEntity copy = ModEntities.QUANTUM_FALCON.get().create(level);
        copy.load(tag);
        boolean persisted = copy.bardingTier() == com.matt.quantumfalcon.item.FalconBardingItem.Tier.QUANTUM
                && copy.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) == armorQ;
        // shears remove
        fp.setShiftKeyDown(true);
        interact(fp, f, Items.SHEARS, 1);
        fp.setShiftKeyDown(false);
        boolean removed = f.bardingTier() == null && f.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) == armor0;
        boolean pass = armorD == armor0 + 6 && consumed && armorQ == armor0 + 10 && swappedBack && absorbed > 15 && absorbed < 75 && persisted && removed;
        result("falcon_barding", pass, "armor " + armor0 + " -> diamond " + armorD + " -> quantum " + armorQ + " | consumed=" + consumed + " oldReturned=" + swappedBack
                + " | quantum phase " + absorbed + "/200 hits | nbt=" + persisted + " | shearsRemoved=" + removed);
        f.discard();
        fp.getInventory().clearContent();
    }

    // ------------------------------------------------------------------ weapons + armour sets
    private static void testWeapons(ServerLevel level) {
        FakePlayer fp = tester(level);
        List<String> ok = new ArrayList<>(), bad = new ArrayList<>();
        int[] slot = {0};
        java.util.function.Function<String, BlockPos> arena = name -> {
            BlockPos p = pad(level, 3600 + (slot[0]++) * 24, 3700, Blocks.STONE);
            cleanup(level, p, 10);
            fp.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0f, 0f);   // facing +z
            fp.setHealth(10f); fp.removeAllEffects();
            return p;
        };
        java.util.function.BiConsumer<String, Boolean> check = (n, b) -> (b ? ok : bad).add(n);
        Item blade = com.matt.quantumfalcon.registry.ModFamilyItems.QUANTUM_BLADE.get();
        // Quantum Blade: blink + 20% armour bypass
        BlockPos p = arena.apply("blade");
        var v = EntityType.VILLAGER.create(level); v.moveTo(p.getX() + 0.5, p.getY(), p.getZ() - 3.5, 0f, 0f); v.setNoAi(true); level.addFreshEntity(v);
        Vec3 v0 = v.position();
        boolean blinked = com.matt.quantumfalcon.item.weapon.WeaponFx.blink(v, 8) && v.position().distanceTo(v0) >= 4;
        var z = dummy(level, p, 0, 3);
        int bypass = 0;
        for (int i = 0; i < 200; i++) { z.setHealth(z.getMaxHealth()); z.removeAllEffects(); blade.hurtEnemy(new ItemStack(blade), z, fp); if (z.getHealth() < z.getMaxHealth()) bypass++; }
        check.accept("quantum_blade blink=" + blinked + " bypass " + bypass + "/200", blinked && bypass > 15 && bypass < 70);
        v.discard();
        // Reality Splitter: arc hits front only; banishment moves 16+
        p = arena.apply("splitter");
        var front = dummy(level, p, 0, 4); var back = dummy(level, p, 0, -4);
        var rs = (com.matt.quantumfalcon.item.weapon.Weapons.RealitySplitter) com.matt.quantumfalcon.registry.ModFamilyItems.REALITY_SPLITTER.get();
        rs.special(level, fp, new ItemStack(rs));
        boolean arc = front.getHealth() < front.getMaxHealth() && back.getHealth() == back.getMaxHealth();
        Vec3 f0 = front.position();
        boolean banished = com.matt.quantumfalcon.item.weapon.Weapons.RealitySplitter.banish(fp, front) && front.position().distanceTo(f0) >= 15 && front.hasEffect(MobEffects.CONFUSION);
        check.accept("reality_splitter arc=" + arc + " banish=" + banished, arc && banished);
        // Void Reaper: pulse + lifesteal + wither chance
        p = arena.apply("reaper");
        z = dummy(level, p, 2, 2);
        var vr = (com.matt.quantumfalcon.item.weapon.Weapons.VoidReaper) com.matt.quantumfalcon.registry.ModFamilyItems.VOID_REAPER.get();
        vr.special(level, fp, new ItemStack(vr));
        boolean pulse = z.getHealth() < z.getMaxHealth() && z.hasEffect(MobEffects.WITHER);
        fp.setHealth(10f);
        int withered = 0;
        for (int i = 0; i < 100; i++) { z.removeAllEffects(); vr.hurtEnemy(new ItemStack(vr), z, fp); if (z.hasEffect(MobEffects.WITHER)) withered++; }
        check.accept("void_reaper pulse=" + pulse + " lifesteal hp=" + fp.getHealth() + " wither " + withered + "/100", pulse && fp.getHealth() > 10f && withered > 20 && withered < 60);
        // Temporal Staff
        p = arena.apply("tstaff");
        z = dummy(level, p, 0, 5);
        ((com.matt.quantumfalcon.item.weapon.AbilitySwordItem) ModGearItems.TEMPORAL_STAFF.get()).special(level, fp, new ItemStack(ModGearItems.TEMPORAL_STAFF.get()));
        check.accept("temporal_staff time stop", z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && z.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 4 && fp.hasEffect(MobEffects.DIG_SPEED));
        // Quantum Staff: hitscan
        p = arena.apply("qstaff");
        z = dummy(level, p, 0, 8);
        ((com.matt.quantumfalcon.item.weapon.AbilitySwordItem) ModGearItems.QUANTUM_STAFF.get()).special(level, fp, new ItemStack(ModGearItems.QUANTUM_STAFF.get()));
        check.accept("quantum_staff bolt hp=" + z.getHealth(), z.getHealth() <= z.getMaxHealth() - 7.9f);
        // Rift Spear: reach attribute + lunge path damage
        p = arena.apply("spear");
        z = dummy(level, p, 0, 3);
        var spear = new ItemStack(ModGearItems.RIFT_SPEAR.get());
        boolean reach = spear.getAttributeModifiers().modifiers().stream().anyMatch(m -> m.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE));
        boolean lunge = ((com.matt.quantumfalcon.item.weapon.AbilitySwordItem) spear.getItem()).special(level, fp, spear) && z.getHealth() < z.getMaxHealth();
        check.accept("rift_spear reach=" + reach + " lunge=" + lunge, reach && lunge);
        // Quantum Spear: a step above the Rift Spear (damage, reach), longer lunge with slowness
        p = arena.apply("qspear");
        z = dummy(level, p, 0, 4);
        var qspear = new ItemStack(ModGearItems.QUANTUM_SPEAR.get());
        java.util.function.BiFunction<ItemStack, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, Double> attr = (st, at) ->
            st.getAttributeModifiers().modifiers().stream().filter(m -> m.attribute().equals(at)).mapToDouble(m -> m.modifier().amount()).sum();
        var DMG = net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE; var RCH = net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE;
        boolean stronger = attr.apply(qspear, DMG) > attr.apply(spear, DMG) && attr.apply(qspear, RCH) > attr.apply(spear, RCH)
            && qspear.getMaxDamage() > spear.getMaxDamage();
        boolean qlunge = ((com.matt.quantumfalcon.item.weapon.AbilitySwordItem) qspear.getItem()).special(level, fp, qspear) && z.getHealth() < z.getMaxHealth()
            && z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
        check.accept("quantum_spear stronger=" + stronger + " lunge=" + qlunge, stronger && qlunge);
        // Chaos Hammer: slam + chaos effects
        p = arena.apply("hammer");
        z = dummy(level, p, 2, 1);
        var hammer = (com.matt.quantumfalcon.item.weapon.AbilitySwordItem) ModGearItems.CHAOS_HAMMER.get();
        hammer.special(level, fp, new ItemStack(hammer));
        boolean slam = z.getHealth() < z.getMaxHealth() && z.getDeltaMovement().y > 0.3;
        int chaos = 0;
        // 25% proc, 1 in 6 rolls poison which a zombie ignores -> ~21%; 400 swings keeps a 10/100 bad streak from failing the suite
        for (int i = 0; i < 400; i++) { z.removeAllEffects(); hammer.hurtEnemy(new ItemStack(hammer), z, fp); if (!z.getActiveEffects().isEmpty()) chaos++; }
        check.accept("chaos_hammer slam=" + slam + " chaos " + chaos + "/400", slam && chaos > 45 && chaos < 160);
        // Echo Dagger: backstab only from behind
        p = arena.apply("edagger");
        z = dummy(level, p, 0, 2);
        z.setYRot(0f); z.setYHeadRot(0f); z.yBodyRot = 0f;     // zombie looks +z, away from the tester (behind it)
        Item ed = ModGearItems.ECHO_DAGGER.get();
        ed.hurtEnemy(new ItemStack(ed), z, fp);
        boolean backstab = z.hasEffect(MobEffects.GLOWING);
        z.removeAllEffects(); z.setYRot(180f); z.setYHeadRot(180f); z.yBodyRot = 180f;   // now facing the tester
        ed.hurtEnemy(new ItemStack(ed), z, fp);
        check.accept("echo_dagger backstab=" + backstab + " frontNoMark=" + !z.hasEffect(MobEffects.GLOWING), backstab && !z.hasEffect(MobEffects.GLOWING));
        // Quantum Dagger: blink behind (villager attacker, since fake players can't be teleported)
        p = arena.apply("qdagger");
        z = dummy(level, p, 0, 3); z.setYRot(180f); z.setYHeadRot(180f);
        v = EntityType.VILLAGER.create(level); v.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5); v.setNoAi(true); level.addFreshEntity(v);
        boolean behind = com.matt.quantumfalcon.item.weapon.Weapons.QuantumDagger.blinkBehind(v, z) && v.getZ() > z.getZ();
        check.accept("quantum_dagger blink behind=" + behind, behind);
        v.discard();
        // Bows: arrow tagging + gravity well
        p = arena.apply("bows");
        var arrow = new net.minecraft.world.entity.projectile.Arrow(level, fp, new ItemStack(Items.ARROW), new ItemStack(ModGearItems.QUANTUM_BOW.get()));
        double baseDmg = arrow.getBaseDamage();
        ((com.matt.quantumfalcon.item.weapon.QFBowItem) ModGearItems.QUANTUM_BOW.get()).customArrow(arrow, new ItemStack(Items.ARROW), new ItemStack(ModGearItems.QUANTUM_BOW.get()));
        boolean qb = arrow.isNoGravity() && Math.abs(arrow.getBaseDamage() - baseDmg * 1.5) < 1e-6;
        z = dummy(level, p, 3, 3);
        Vec3 z0 = z.position();
        int pulled = com.matt.quantumfalcon.item.weapon.QFBowItem.gravityWell(fp, new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5));
        boolean well = pulled >= 1 && z.getHealth() < z.getMaxHealth() && z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && z.getDeltaMovement().horizontalDistance() > 0.2;
        check.accept("quantum_bow straight+1.5x=" + qb + " singularity_well=" + well, qb && well);
        // Falconer's Gauntlet
        p = arena.apply("gauntlet");
        QuantumFalconEntity f = variantFalcon(level, p.offset(2, 0, 0), "peregrine", fp.getUUID());
        f.setNoAi(false);
        z = dummy(level, p, 0, 6);
        int sent = com.matt.quantumfalcon.item.weapon.Weapons.FalconerGauntlet.sic(fp, z);
        fp.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModGearItems.FALCONER_GAUNTLET.get()));
        boolean holds = com.matt.quantumfalcon.item.weapon.Weapons.FalconerGauntlet.holds(fp);
        fp.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        check.accept("falconer_gauntlet sent=" + sent + " target=" + (f.getTarget() == z) + " mode=" + f.getCommandMode() + " holds=" + holds, sent == 1 && f.getTarget() == z && holds);
        // recipes exist for every new item
        List<String> missing = new ArrayList<>();
        for (var it : ModGearItems.ALL) {
            Item item = it.get();
            boolean has = level.getRecipeManager().getRecipes().stream().anyMatch(h -> h.value().getResultItem(level.registryAccess()).is(item));
            if (!has) missing.add(it.getId().getPath());
        }
        for (Item item : new Item[]{blade, rs, vr}) if (level.getRecipeManager().getRecipes().stream().noneMatch(h -> h.value().getResultItem(level.registryAccess()).is(item))) missing.add(item.toString());
        check.accept("recipes for all " + (ModGearItems.ALL.size() + 3) + " items" + (missing.isEmpty() ? "" : " missing " + missing), missing.isEmpty());
        for (int i = 0; i < slot[0]; i++) cleanup(level, new BlockPos(3600 + i * 24, p.getY(), 3700), 12);
        fp.setHealth(fp.getMaxHealth()); fp.removeAllEffects(); fp.getInventory().clearContent();
        result("weapons", bad.isEmpty(), ok.size() + " weapon checks OK" + (bad.isEmpty() ? ": " + ok : "; FAILED: " + bad + "; OK: " + ok));
    }

    private static void testArmorSets(ServerLevel level) {
        FakePlayer fp = tester(level);
        BlockPos p = pad(level, 3600, 3760, Blocks.STONE);
        cleanup(level, p, 10);
        fp.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0f, 0f);
        List<String> ok = new ArrayList<>(), bad = new ArrayList<>();
        java.util.function.BiConsumer<String, Boolean> check = (n, b) -> (b ? ok : bad).add(n);
        net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
        String[] suffix = {"helmet", "chestplate", "leggings", "boots"};
        java.util.function.Consumer<String> wear = set -> {
            for (int i = 0; i < 4; i++) fp.setItemSlot(slots[i], new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("quantumfalcon", set + "_" + suffix[i]))));
            fp.removeAllEffects(); fp.setHealth(fp.getMaxHealth());
        };
        var FSB = com.matt.quantumfalcon.item.gear.FamilySetBonuses.class;
        int counted = 0;
        for (String set : com.matt.quantumfalcon.item.gear.FamilySetBonuses.SETS) { wear.accept(set); if (com.matt.quantumfalcon.item.gear.FamilySetBonuses.pieces(fp, set) == 4) counted++; }
        check.accept("11 sets detected (" + counted + ")", counted == 11);
        String[][] simple = {{"chaos", "strength"}, {"crystal", "haste"}, {"temporal", "haste"}, {"phase", "speed"}, {"probability", "luck"}, {"quantum", "night_vision"}, {"void_steel", "fire_resistance"}};
        for (String[] s2 : simple) {
            wear.accept(s2[0]);
            com.matt.quantumfalcon.item.gear.FamilySetBonuses.applyPeriodic(fp);
            boolean has = fp.getActiveEffects().stream().anyMatch(e -> e.getEffect().unwrapKey().get().location().getPath().equals(s2[1]));
            check.accept(s2[0] + "->" + s2[1], has);
        }
        wear.accept("resonance"); fp.setHealth(6f); com.matt.quantumfalcon.item.gear.FamilySetBonuses.applyPeriodic(fp);
        check.accept("resonance low-health regen", fp.hasEffect(MobEffects.REGENERATION));
        wear.accept("void"); fp.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WITHER, 100)); com.matt.quantumfalcon.item.gear.FamilySetBonuses.applyPeriodic(fp);
        check.accept("void wither immunity", !fp.hasEffect(MobEffects.WITHER));
        boolean r1 = com.matt.quantumfalcon.item.gear.FamilySetBonuses.voidRescue(fp), r2 = com.matt.quantumfalcon.item.gear.FamilySetBonuses.voidRescue(fp);
        check.accept("void rescue + cooldown", r1 && !r2);
        fp.getPersistentData().remove("qf_void_rescue");
        // singularity: knockback modifier + black hole aura
        var z = dummy(level, p, 0, 4);
        wear.accept("singularity"); com.matt.quantumfalcon.item.gear.FamilySetBonuses.applyPeriodic(fp);
        double kb = fp.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
        check.accept("singularity kb=" + kb + " aura", kb >= 0.2 && z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && z.getDeltaMovement().horizontalDistance() > 0.1);
        // echo sense (every 3 s)
        z.removeAllEffects(); wear.accept("echo"); fp.tickCount = 60; com.matt.quantumfalcon.item.gear.FamilySetBonuses.applyPeriodic(fp);
        check.accept("echo sense glow", z.hasEffect(MobEffects.GLOWING));
        // temporal time dilation + cooldown
        z.removeAllEffects(); wear.accept("temporal");
        fp.getPersistentData().remove("qf_time_dilation");
        boolean t1 = com.matt.quantumfalcon.item.gear.FamilySetBonuses.timeDilation(fp), t2 = com.matt.quantumfalcon.item.gear.FamilySetBonuses.timeDilation(fp);
        check.accept("temporal dilation + cooldown", t1 && !t2 && z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN));
        fp.getPersistentData().remove("qf_time_dilation");
        // resonance harmonic pulse
        var z2 = dummy(level, p, 2, 0);
        check.accept("resonance harmonic pulse", com.matt.quantumfalcon.item.gear.FamilySetBonuses.harmonicPulse(fp) >= 1);
        // void steel shadow step + cooldown
        wear.accept("void_steel"); fp.getPersistentData().remove("qf_shadow_step");
        boolean s1 = com.matt.quantumfalcon.item.gear.FamilySetBonuses.shadowStep(fp), s2 = com.matt.quantumfalcon.item.gear.FamilySetBonuses.shadowStep(fp);
        check.accept("void_steel shadow step + 5s cooldown", s1 && !s2);
        fp.getPersistentData().remove("qf_shadow_step");
        // attacker-side: probability lucky strike (~10% double) and chaos touch (~20% affliction)
        wear.accept("probability");
        int doubled = 0;
        for (int i = 0; i < 300; i++) { z.setHealth(z.getMaxHealth()); z.invulnerableTime = 0; z.hurt(level.damageSources().playerAttack(fp), 4f); if (z.getMaxHealth() - z.getHealth() > 5f) doubled++; }
        check.accept("probability lucky strike " + doubled + "/300", doubled > 10 && doubled < 60);
        wear.accept("chaos");
        int afflicted = 0;
        for (int i = 0; i < 200; i++) { z.setHealth(z.getMaxHealth()); z.removeAllEffects(); z.invulnerableTime = 0; z.hurt(level.damageSources().playerAttack(fp), 1f); if (!z.getActiveEffects().isEmpty()) afflicted++; }
        check.accept("chaos touch " + afflicted + "/200", afflicted > 15 && afflicted < 70);
        // quantum link: falcon ability cooldown x0.8
        wear.accept("quantum");
        QuantumFalconEntity f = variantFalcon(level, p.offset(-2, 0, 0), "tempest", fp.getUUID());
        int cd = com.matt.quantumfalcon.entity.ability.FalconAbilities.cooldownTicks(f, fp);
        check.accept("quantum link cooldown " + cd, cd == 320);
        for (var sl : slots) fp.setItemSlot(sl, ItemStack.EMPTY);
        com.matt.quantumfalcon.item.gear.FamilySetBonuses.applyPeriodic(fp);   // removes the singularity modifier
        fp.removeAllEffects(); fp.setHealth(fp.getMaxHealth());
        cleanup(level, p, 10);
        result("armor_sets", bad.isEmpty(), ok.size() + " set-bonus checks OK" + (bad.isEmpty() ? ": " + ok : "; FAILED: " + bad + "; OK: " + ok));
    }
    // ------------------------------------------------------------------ breeding / nests / realm blocks (2026-11-12)
    private static void testBreedingEngine(ServerLevel level) {
        var r = net.minecraft.util.RandomSource.create(42);
        int ups = 0, bad = 0, n = 300, hybrids = 0;
        for (int i = 0; i < n; i++) {
            var a = com.matt.quantumfalcon.breeding.BreedingEngine.wild("tempest", 4, r);
            var b = com.matt.quantumfalcon.breeding.BreedingEngine.wild("stormrider", 4, r);
            var res = com.matt.quantumfalcon.breeding.BreedingEngine.breed(a, b, new com.matt.quantumfalcon.breeding.BreedingEngine.Context(), r);
            var c = res.child;
            if (res.tierUp) ups++;
            if (res.hybrid != null) hybrids++;
            for (int s = 0; s < com.matt.quantumfalcon.breeding.FalconGenome.STATS; s++) if (c.stat(s) > c.statCap() || c.stat(s) < 1) bad++;
            if (c.abilities.size() > c.slots() || c.generation != 2 || c.parents.size() != 2) bad++;
            var round = com.matt.quantumfalcon.breeding.FalconGenome.load(c.save());
            if (round == null || round.tier != c.tier || !round.variant.equals(c.variant) || !round.abilities.equals(c.abilities)) bad++;
        }
        final int u = ups, h = hybrids, bd = bad;
        result("breeding_engine", bd == 0 && u > 0 && u < n / 2, n + " T4 pairs: tier-ups=" + u + " hybrids=" + h + " invariant violations=" + bd + " (stats within cap, slots, pedigree, NBT round-trip)");
    }

    private static void testNestIncubation(ServerLevel level) {
        BlockPos p = pad(level, 4000, 4000, Blocks.STONE); cleanup(level, p, 6);
        // a tier-3 egg needs heat: bare ground -> waiting; on a crystal nest -> incubating; tier 7 quantum on an aerie -> incubating
        BlockPos bare = p.offset(-2, 0, 0), onNest = p.offset(0, 1, 0), onAerie = p.offset(2, 1, 0);
        level.setBlock(p, ModBlocks.CRYSTAL_NEST.get().defaultBlockState(), 3);
        level.setBlock(p.offset(2, 0, 0), ModBlocks.QUANTUM_AERIE.get().defaultBlockState(), 3);
        String need3 = com.matt.quantumfalcon.breeding.BreedingData.TIERS.get(2).incubation(), need7 = com.matt.quantumfalcon.breeding.BreedingData.TIERS.get(6).incubation();
        level.setBlock(bare, ModBlocks.FALCON_EGG_BLOCK.get().defaultBlockState(), 3);
        level.setBlock(onNest, ModBlocks.FALCON_EGG_BLOCK.get().defaultBlockState(), 3);
        level.setBlock(onAerie, ModBlocks.FALCON_EGG_BLOCK.get().defaultBlockState(), 3);
        boolean ok = level.getBlockEntity(bare) instanceof FalconEggBlockEntity && level.getBlockEntity(onNest) instanceof FalconEggBlockEntity
                && level.getBlockEntity(onAerie) instanceof FalconEggBlockEntity;
        String d = "";
        if (ok) {
            var e0 = (FalconEggBlockEntity) level.getBlockEntity(bare); var e1 = (FalconEggBlockEntity) level.getBlockEntity(onNest); var e2 = (FalconEggBlockEntity) level.getBlockEntity(onAerie);
            String m0 = e0.missingCondition(level, bare, need3), m1 = e1.missingCondition(level, onNest, need3), m2 = e2.missingCondition(level, onAerie, need7);
            var q = com.matt.quantumfalcon.block.FalconNestBlock.qualityAt(level, p);
            // a twig nest is warmth only: it must not satisfy a heat requirement
            level.setBlock(p.offset(-4, 0, 0), ModBlocks.TWIG_NEST.get().defaultBlockState(), 3);
            level.setBlock(p.offset(-4, 1, 0), ModBlocks.FALCON_EGG_BLOCK.get().defaultBlockState(), 3);
            String mt = level.getBlockEntity(p.offset(-4, 1, 0)) instanceof FalconEggBlockEntity et ? et.missingCondition(level, p.offset(-4, 1, 0), need3) : "";
            ok = !m0.isEmpty() && m1.isEmpty() && m2.isEmpty() && !mt.isEmpty() && q == com.matt.quantumfalcon.breeding.NestQuality.CRYSTAL && q.speed > 1f;
            d = "T3 need=" + need3 + " bare='" + m0 + "' twigNest='" + mt + "' crystalNest='" + m1 + "' | T7 need=" + need7 + " aerie='" + m2 + "' speed=" + (q == null ? "-" : q.speed);
        }
        result("nest_incubation", ok, d);
        cleanup(level, p, 6);
    }

    private static void testNestLaying(ServerLevel level) {
        BlockPos p = pad(level, 4040, 4000, Blocks.GRASS_BLOCK); cleanup(level, p, 8);
        level.setBlock(p.offset(3, 0, 0), ModBlocks.TWIG_NEST.get().defaultBlockState(), 3);
        level.setBlock(p.offset(-3, 0, 2), ModBlocks.VOIDFORGED_NEST.get().defaultBlockState(), 3);
        BlockPos best = com.matt.quantumfalcon.breeding.BreedingService.freeNestNear(level, p, 8);
        boolean pick = p.offset(-3, 0, 2).equals(best);
        // occupy the void-forged nest: the twig nest is next
        level.setBlock(p.offset(-3, 1, 2), ModBlocks.FALCON_EGG_BLOCK.get().defaultBlockState(), 3);
        BlockPos second = com.matt.quantumfalcon.breeding.BreedingService.freeNestNear(level, p, 8);
        var ctx = com.matt.quantumfalcon.breeding.BreedingService.contextAt(level, p.offset(-3, 1, 2));
        // statistical: nest mutation bonus raises rare mutations
        var r = net.minecraft.util.RandomSource.create(7); int with = 0, without = 0;
        for (int i = 0; i < 400; i++) {
            var a = com.matt.quantumfalcon.breeding.BreedingEngine.wild("peregrine", 1, r); var b = com.matt.quantumfalcon.breeding.BreedingEngine.wild("prairie", 1, r);
            var c0 = new com.matt.quantumfalcon.breeding.BreedingEngine.Context(); var c1 = new com.matt.quantumfalcon.breeding.BreedingEngine.Context(); c1.nestMutation = 0.10f;
            if (com.matt.quantumfalcon.breeding.BreedingEngine.breed(a, b, c0, r).mutations.stream().anyMatch(m -> m.startsWith("nest:") || m.contains("FRACTAL") || m.contains("STARFIELD") || m.contains("PRISMATIC") || m.contains("VOID"))) without++;
            if (com.matt.quantumfalcon.breeding.BreedingEngine.breed(a, b, c1, r).mutations.stream().anyMatch(m -> m.startsWith("nest:") || m.contains("FRACTAL") || m.contains("STARFIELD") || m.contains("PRISMATIC") || m.contains("VOID"))) with++;
        }
        boolean ok = pick && p.offset(3, 0, 0).equals(second) && Math.abs(ctx.nestMutation - 0.06f) < 1e-4 && with > without;
        result("nest_laying_mutation", ok, "best=" + best + " (void-forged expected) then " + second + " (twig) ctx.nestMutation=" + ctx.nestMutation + " rare mutations 400 pairs: aerie-level " + with + " vs none " + without);
        cleanup(level, p, 8);
    }

    private static void testWildNest(ServerLevel level) {
        BlockPos p = pad(level, 4080, 4000, Blocks.GRASS_BLOCK); cleanup(level, p, 6);
        boolean placed = com.matt.quantumfalcon.world.WildNestFeature.WILD_NEST.get().place(net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration.NONE,
                level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(3), p.above());
        BlockPos nest = null;
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 3, 1)))
            if (com.matt.quantumfalcon.block.FalconNestBlock.qualityAt(level, q) != null) { nest = q.immutable(); break; }
        String egg = nest != null && level.getBlockEntity(nest.above()) instanceof FalconEggBlockEntity be && be.getChild() != null ? be.getChild().variant + " T" + be.getChild().tier : "none";
        result("wild_nest_feature", placed && nest != null, "placed=" + placed + " nest=" + (nest == null ? "-" : level.getBlockState(nest).getBlock()) + " egg=" + egg);
        cleanup(level, p, 6);
    }

    private static void testRealmBlocks(ServerLevel level) {
        var all = com.matt.quantumfalcon.registry.ModRealmBlocks.ALL;
        int missingLoot = 0, missingRecipe = 0, missingItem = 0;
        var loot = level.getServer().reloadableRegistries();
        var recipes = level.getServer().getRecipeManager();
        java.util.Set<net.minecraft.world.item.Item> crafted = new java.util.HashSet<>();
        for (var h : recipes.getRecipes()) crafted.add(h.value().getResultItem(level.registryAccess()).getItem());
        java.util.List<String> miss = new java.util.ArrayList<>();
        for (var b : all) {
            var blk = b.get();
            var key = blk.getLootTable();
            if (loot.getLootTable(key) == net.minecraft.world.level.storage.loot.LootTable.EMPTY) { missingLoot++; if (miss.size() < 5) miss.add("loot:" + b.getId().getPath()); }
            if (blk.asItem() == net.minecraft.world.item.Items.AIR) missingItem++;
            if (!crafted.contains(blk.asItem())) { missingRecipe++; if (miss.size() < 5) miss.add("recipe:" + b.getId().getPath()); }
        }
        for (var n : new net.minecraft.world.level.block.Block[]{ModBlocks.TWIG_NEST.get(), ModBlocks.CRYSTAL_NEST.get(), ModBlocks.VOIDFORGED_NEST.get(), ModBlocks.QUANTUM_AERIE.get()})
            if (!crafted.contains(n.asItem())) { missingRecipe++; miss.add("recipe:" + n); }
        result("realm_blocks", all.size() == 248 && missingLoot == 0 && missingRecipe == 0 && missingItem == 0,
            "blocks=" + all.size() + " missing loot=" + missingLoot + " recipe=" + missingRecipe + " item=" + missingItem + " " + miss);
    }


    private static void testEggLoot(ServerLevel level) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.matt.quantumfalcon.QuantumFalcon.MOD_ID, "chests/void_citadel"));
        var table = level.getServer().reloadableRegistries().getLootTable(key);
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        int eggs = 0, bad = 0; java.util.Set<String> seen = new java.util.TreeSet<>();
        for (int i = 0; i < 200; i++) for (ItemStack st : table.getRandomItems(params)) {
            if (!st.is(com.matt.quantumfalcon.registry.ModItems.FALCON_EGG.get())) continue;
            eggs++;
            var g = com.matt.quantumfalcon.item.FalconEggItem.child(st);
            if (g == null || g.tier < 4 || g.tier > 6) bad++; else seen.add(g.variant + "T" + g.tier);
        }
        result("egg_loot_caches", table != net.minecraft.world.level.storage.loot.LootTable.EMPTY && eggs > 50 && bad == 0,
            "void_citadel x200: eggs=" + eggs + " bad=" + bad + " kinds=" + seen);
    }

    private static void testDangerTiers(ServerLevel level) {
        var ow = com.matt.quantumfalcon.world.DangerTiers.of(ResourceLocation.withDefaultNamespace("overworld"));
        var ne = com.matt.quantumfalcon.world.DangerTiers.of(ResourceLocation.withDefaultNamespace("the_nether"));
        var r5 = com.matt.quantumfalcon.world.DangerTiers.of(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "echo_rift_5"));
        var r0 = com.matt.quantumfalcon.world.DangerTiers.of(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "echo_rift_0"));
        boolean ok = ow.tier() == 0 && ne.tier() == 3 && r0.tier() == 2 && r5.tier() == 7 && !r5.name().isEmpty();
        result("danger_tiers", ok, "overworld=" + ow.tier() + " nether=" + ne.tier() + " " + r0.name() + "=" + r0.tier() + " " + r5.name() + "=" + r5.tier());
    }

    private static void testIncubator(ServerLevel level) {
        BlockPos p = pad(level, 4120, 4000, Blocks.STONE); cleanup(level, p, 4);
        level.setBlock(p, ModBlocks.QUANTUM_INCUBATOR.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(p) instanceof com.matt.quantumfalcon.blockentity.QuantumIncubatorBlockEntity be)) { result("incubator_menu", false, "no block entity"); return; }
        var egg = com.matt.quantumfalcon.item.FalconEggItem.wild(new ItemStack(com.matt.quantumfalcon.registry.ModItems.FALCON_EGG.get()),
                com.matt.quantumfalcon.breeding.BreedingEngine.wild("voidstalker", 5, level.getRandom()));
        be.setItem(0, egg);
        var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        boolean menuOk = be.createMenu(1, fp.getInventory(), fp) instanceof com.matt.quantumfalcon.menu.QuantumIncubatorMenu;
        var st = level.getBlockState(p);
        com.matt.quantumfalcon.blockentity.QuantumIncubatorBlockEntity.serverTick(level, p, st, be);
        int s1 = be.status(), total = be.totalTicks();
        be.setItem(1, new ItemStack(com.matt.quantumfalcon.registry.ModItems.QUANTUM_SHARD.get(), 2));
        com.matt.quantumfalcon.blockentity.QuantumIncubatorBlockEntity.serverTick(level, p, st, be);
        int s2 = be.status();
        be.setProgressForTest(0.99999f);
        for (int i = 0; i < 40 && !be.getItem(0).isEmpty(); i++) com.matt.quantumfalcon.blockentity.QuantumIncubatorBlockEntity.serverTick(level, p, st, be);
        var baby = be.lastHatched;
        boolean hatched = be.getItem(0).isEmpty() && baby != null && !baby.isRemoved() && baby.isAddedToLevel() && baby.isBaby();
        String v = baby == null ? "-" : baby.getVariant() + " T" + baby.visTier();
        if (baby != null) baby.discard();
        boolean ok = menuOk && s1 == com.matt.quantumfalcon.blockentity.QuantumIncubatorBlockEntity.ST_NEED_FUEL && total == 18 * 1200
                && s2 == com.matt.quantumfalcon.blockentity.QuantumIncubatorBlockEntity.ST_WORKING && hatched;
        result("incubator_menu", ok, "menu=" + menuOk + " T5 no fuel status=" + s1 + " hatchTicks=" + total + " (18 min) with shard status=" + s2 + " hatched=" + hatched + " " + v);
        cleanup(level, p, 4);
    }
}
