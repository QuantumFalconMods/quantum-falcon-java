package com.matt.quantumfalcon.command;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.capability.PlayerInstabilityCapability;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.registry.ModEntities;
import com.matt.quantumfalcon.network.ModNetworking;
import com.matt.quantumfalcon.quest.PlayerQuestData;
import com.matt.quantumfalcon.quest.QuestSystem;
import com.matt.quantumfalcon.skill.PlayerSkillData;
import com.matt.quantumfalcon.skill.SkillTreeSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

/**
 * Admin Commands for Quantum Falcon mod.
 * Provides testing and debugging commands.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class ModCommands {

    // Suggestion providers
    private static final SuggestionProvider<CommandSourceStack> SKILL_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(
            SkillTreeSystem.getAllSkills().stream().map(s -> s.id),
            builder
        );

    private static final SuggestionProvider<CommandSourceStack> QUEST_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(
            QuestSystem.QUESTS.keySet(),
            builder
        );

    private static final SuggestionProvider<CommandSourceStack> VARIANT_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(
            List.of("peregrine", "arctic", "golden", "shadow", "phoenix", "void", "celestial", "storm"),
            builder
        );

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Main command: /qf
        // /qf is a player command; admin/debug branches below require permission 2 each
        dispatcher.register(Commands.literal("qf")
            // A10 dev: /qf frozen place <structure> (hut centred on you) | /qf frozen flip <volume>
            .then(Commands.literal("frozen").requires(ModCommands::admin)
                .then(Commands.literal("place").then(Commands.argument("structure", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
                    var sl = ctx.getSource().getLevel();
                    var pos = net.minecraft.core.BlockPos.containing(ctx.getSource().getPosition());
                    var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.matt.quantumfalcon.QuantumFalcon.MOD_ID, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "structure"));
                    var start = com.matt.quantumfalcon.dungeon.FrozenHours.placeAt(sl, id, pos.offset(-15, -32, -9));
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(start == null ? "Not a Frozen Hour structure: " + id : "Placed " + id + " at " + start.getBoundingBox()), true);
                    return start == null ? 0 : 1;
                })))
                .then(Commands.literal("flip").then(Commands.argument("volume", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
                    String r = com.matt.quantumfalcon.dungeon.FrozenHours.flipAt(ctx.getSource().getLevel(), net.minecraft.core.BlockPos.containing(ctx.getSource().getPosition()),
                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "volume"));
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(r), true);
                    return 1;
                }))))
            
            // /qf skill <action> [args]
            .then(Commands.literal("skill")
                .then(Commands.literal("list").executes(ModCommands::listSkills))
                .then(Commands.literal("give").requires(ModCommands::admin)
                    .then(Commands.argument("points", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> giveSkillPoints(ctx, IntegerArgumentType.getInteger(ctx, "points")))))
                .then(Commands.literal("unlock")   // players spend points; prerequisites apply
                    .then(Commands.argument("skillId", StringArgumentType.word())
                        .suggests(SKILL_SUGGESTIONS)
                        .executes(ctx -> buySkill(ctx, StringArgumentType.getString(ctx, "skillId")))))
                .then(Commands.literal("grant").requires(ModCommands::admin)   // admin: free unlock
                    .then(Commands.argument("skillId", StringArgumentType.word())
                        .suggests(SKILL_SUGGESTIONS)
                        .executes(ctx -> unlockSkill(ctx, StringArgumentType.getString(ctx, "skillId")))))
                .then(Commands.literal("reset").requires(ModCommands::admin)
                    .executes(ModCommands::resetSkills))
                .then(Commands.literal("wipe").requires(ModCommands::admin)   // admin/dev: back to a brand-new player (no skills, 0 points, no marks)
                    .executes(ModCommands::wipeSkills))
                .then(Commands.literal("unlockall").requires(ModCommands::admin)
                    .executes(ModCommands::unlockAllSkills)))
            
            // /qf quest <action> [args]
            .then(Commands.literal("quest")
                .then(Commands.literal("list").executes(ModCommands::listQuests))
                .then(Commands.literal("accept")
                    .then(Commands.argument("questId", StringArgumentType.word())
                        .suggests(QUEST_SUGGESTIONS)
                        .executes(ctx -> acceptQuest(ctx, StringArgumentType.getString(ctx, "questId")))))
                .then(Commands.literal("complete").requires(ModCommands::admin)
                    .then(Commands.argument("questId", StringArgumentType.word())
                        .suggests(QUEST_SUGGESTIONS)
                        .executes(ctx -> completeQuest(ctx, StringArgumentType.getString(ctx, "questId")))))
                .then(Commands.literal("reset").requires(ModCommands::admin)
                    .executes(ModCommands::resetQuests)))
            
            // /qf instability <value>
            .then(Commands.literal("instability").requires(ModCommands::admin)
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                    .executes(ctx -> setInstability(ctx, IntegerArgumentType.getInteger(ctx, "value")))))
            
            // /qf falcon <action> [args]
            .then(Commands.literal("falcon")
                .then(Commands.literal("info").executes(ModCommands::falconInfo))
                .then(Commands.literal("spawn").requires(ModCommands::admin)
                    .executes(ctx -> spawnFalcon(ctx, "peregrine", false))
                    .then(Commands.argument("variant", StringArgumentType.word())
                        .suggests(VARIANT_SUGGESTIONS)
                        .executes(ctx -> spawnFalcon(ctx, StringArgumentType.getString(ctx, "variant"), false))
                        .then(Commands.literal("anomaly")
                            .executes(ctx -> spawnFalcon(ctx, StringArgumentType.getString(ctx, "variant"), true)))))
                .then(Commands.literal("tame").requires(ModCommands::admin)
                    .executes(ModCommands::tameNearbyFalcons))
                .then(Commands.literal("bond").requires(ModCommands::admin)
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> setBondLevel(ctx, IntegerArgumentType.getInteger(ctx, "level")))))
                .then(Commands.literal("heal").requires(ModCommands::admin)
                    .executes(ModCommands::healNearbyFalcons)))
            
            // /qf boss <bossType>
            .then(Commands.literal("boss").requires(ModCommands::admin)
                .then(bossNode("overseer")).then(bossNode("warden")).then(bossNode("twins")).then(bossNode("sovereign")))
            
            // /qf debug
            .then(Commands.literal("debug").requires(ModCommands::admin)
                .then(Commands.literal("info")
                    .executes(ModCommands::showDebugInfo)))
        );
    }

    // ============ Skill Commands ============

    private static int giveSkillPoints(CommandContext<CommandSourceStack> ctx, int points) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        PlayerSkillData data = PlayerSkillData.get(player);
        data.addSkillPoints(points);
        ModNetworking.syncSkillPoints(player);

        ctx.getSource().sendSuccess(() -> 
            Component.literal("Gave " + points + " skill points. Total: " + data.getAvailableSkillPoints()), 
            true
        );
        return 1;
    }

    private static int unlockSkill(CommandContext<CommandSourceStack> ctx, String skillId) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        if (SkillTreeSystem.getSkill(skillId) == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown skill: " + skillId));
            return 0;
        }

        PlayerSkillData data = PlayerSkillData.get(player);
        data.unlockSkillAdmin(skillId);
        ModNetworking.syncSkillPoints(player);

        ctx.getSource().sendSuccess(() -> Component.literal("Unlocked skill: " + skillId), true);
        return 1;
    }

    private static int resetSkills(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        PlayerSkillData data = PlayerSkillData.get(player);
        int refundPoints = data.getTotalSpentPoints();
        data.resetSkills();
        data.addSkillPoints(refundPoints);
        ModNetworking.syncSkillPoints(player);

        ctx.getSource().sendSuccess(() -> Component.literal("Reset all skills. Refunded " + refundPoints + " points."), true);
        return 1;
    }

    private static int wipeSkills(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        PlayerSkillData data = PlayerSkillData.get(player);
        data.resetSkills();
        data.setAvailableSkillPoints(0);
        ModNetworking.syncSkillPoints(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Skills wiped: 0 points, nothing unlocked."), true);
        return 1;
    }

    private static int unlockAllSkills(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        PlayerSkillData data = PlayerSkillData.get(player);
        for (var skill : SkillTreeSystem.getAllSkills()) {
            data.unlockSkillAdmin(skill.id);
        }
        ModNetworking.syncSkillPoints(player);

        ctx.getSource().sendSuccess(() -> Component.literal("Unlocked all " + SkillTreeSystem.getTotalSkillCount() + " skills!"), true);
        return 1;
    }

    // ============ Quest Commands ============

    static boolean admin(CommandSourceStack source) { return source.hasPermission(2); }

    private static int acceptQuest(CommandContext<CommandSourceStack> ctx, String questId) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        String err = com.matt.quantumfalcon.quest.QuestTracker.tryAccept(player, questId);
        if (err != null) { ctx.getSource().sendFailure(Component.literal(err)); return 0; }
        ctx.getSource().sendSuccess(() -> Component.literal("Accepted quest: " + QuestSystem.getQuest(questId).getName()), false);
        return 1;
    }

    private static int listQuests(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        PlayerQuestData data = PlayerQuestData.get(player);
        StringBuilder active = new StringBuilder(), avail = new StringBuilder();
        for (QuestSystem.Quest q : QuestSystem.QUESTS.values()) {
            var st = data.getQuestStatus(q.id);
            if (st == PlayerQuestData.QuestStatus.ACTIVE) active.append(active.isEmpty() ? "" : ", ").append(q.id);
            else if (st == PlayerQuestData.QuestStatus.AVAILABLE) avail.append(avail.isEmpty() ? "" : ", ").append(q.id);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Active: " + (active.isEmpty() ? "none" : active) + "\nAvailable: " + (avail.isEmpty() ? "none" : avail)
                + "\nCompleted: " + data.getCompletedQuests().size() + "/" + QuestSystem.QUESTS.size()), false);
        return 1;
    }

    private static int listSkills(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        PlayerSkillData data = PlayerSkillData.get(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Skill points: " + data.getAvailableSkillPoints() + "  Unlocked: " + String.join(", ", data.getUnlockedSkills())), false);
        return 1;
    }

    private static int buySkill(CommandContext<CommandSourceStack> ctx, String skillId) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        if (SkillTreeSystem.getSkill(skillId) == null) { ctx.getSource().sendFailure(Component.literal("Unknown skill: " + skillId)); return 0; }
        PlayerSkillData data = PlayerSkillData.get(player);
        String why = data.unlockReason(player, skillId);
        if (why != null || !data.tryUnlockSkill(player, skillId)) { ctx.getSource().sendFailure(Component.literal("Can't unlock " + SkillTreeSystem.getSkill(skillId).name + ": " + why)); return 0; }
        ModNetworking.syncSkillPoints(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Unlocked skill: " + SkillTreeSystem.getSkill(skillId).name), false);
        return 1;
    }

    private static int falconInfo(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        var list = player.serverLevel().getEntitiesOfClass(QuantumFalconEntity.class, player.getBoundingBox().inflate(32), f -> f.isOwnedBy(player));
        if (list.isEmpty()) { ctx.getSource().sendFailure(Component.literal("No falcon of yours within 32 blocks")); return 0; }
        for (QuantumFalconEntity f : list)
            ctx.getSource().sendSuccess(() -> Component.literal(f.getName().getString() + ": " + f.getVariant() + ", bond " + f.getBondLevel()
                    + ", health " + Math.round(f.getHealth()) + "/" + Math.round(f.getMaxHealth())), false);
        return list.size();
    }

    private static int completeQuest(CommandContext<CommandSourceStack> ctx, String questId) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        if (QuestSystem.getQuest(questId) == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown quest: " + questId));
            return 0;
        }

        PlayerQuestData data = PlayerQuestData.get(player);
        data.acceptQuest(questId); // Ensure it's active
        data.completeQuest(questId);
        // the admin shortcut must start the next tutorial/main quest like a real completion does (it only happened on relog)
        com.matt.quantumfalcon.quest.QuestTracker.startAvailableChains(player);
        ModNetworking.syncQuestProgress(player);

        ctx.getSource().sendSuccess(() -> Component.literal("Completed quest: " + questId), true);
        return 1;
    }

    private static int resetQuests(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        PlayerQuestData.get(player).resetQuests();
        ModNetworking.syncQuestProgress(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Quest progress reset!"), true);
        return 1;
    }

    // ============ Instability Commands ============

    private static int setInstability(CommandContext<CommandSourceStack> ctx, int value) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        PlayerInstabilityCapability cap = PlayerInstabilityCapability.get(player);
        cap.setInstability(value);

        ctx.getSource().sendSuccess(() -> Component.literal("Set instability to " + value + "%"), true);
        return 1;
    }

    // ============ Falcon Commands ============

    private static int spawnFalcon(CommandContext<CommandSourceStack> ctx, String variant, boolean isAnomaly) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        QuantumFalconEntity falcon = ModEntities.QUANTUM_FALCON.get().create(player.level());
        if (falcon != null) {
            falcon.setPos(player.getX() + 2, player.getY(), player.getZ());
            falcon.setVariantByName(variant);
            falcon.setAnomaly(isAnomaly);
            player.level().addFreshEntity(falcon);

            String type = isAnomaly ? "Anomaly " + variant : variant;
            ctx.getSource().sendSuccess(() -> Component.literal("Spawned " + type + " falcon!"), true);
        }
        return 1;
    }

    private static int tameNearbyFalcons(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        AABB box = player.getBoundingBox().inflate(20);
        List<Entity> falcons = player.level().getEntities(player, box, 
            e -> e instanceof QuantumFalconEntity);

        int tamed = 0;
        for (Entity entity : falcons) {
            QuantumFalconEntity falcon = (QuantumFalconEntity) entity;
            if (!falcon.isTame()) {
                falcon.tame(player);
                tamed++;
            }
        }

        int finalTamed = tamed;
        ctx.getSource().sendSuccess(() -> Component.literal("Tamed " + finalTamed + " falcon(s)!"), true);
        return 1;
    }

    private static int setBondLevel(CommandContext<CommandSourceStack> ctx, int level) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        AABB box = player.getBoundingBox().inflate(20);
        List<Entity> falcons = player.level().getEntities(player, box, 
            e -> e instanceof QuantumFalconEntity falcon && falcon.isOwnedBy(player));

        for (Entity entity : falcons) {
            QuantumFalconEntity falcon = (QuantumFalconEntity) entity;
            falcon.setBondLevel(level);
            ModNetworking.syncFalconState(falcon);
        }

        ctx.getSource().sendSuccess(() -> Component.literal("Set bond level to " + level + " for nearby falcons!"), true);
        return 1;
    }

    private static int healNearbyFalcons(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        AABB box = player.getBoundingBox().inflate(20);
        List<Entity> falcons = player.level().getEntities(player, box, 
            e -> e instanceof QuantumFalconEntity);

        for (Entity entity : falcons) {
            QuantumFalconEntity falcon = (QuantumFalconEntity) entity;
            falcon.heal(falcon.getMaxHealth());
        }

        ctx.getSource().sendSuccess(() -> Component.literal("Healed all nearby falcons!"), true);
        return 1;
    }

    // ============ Boss Commands ============

    /** /qf boss <type> [noarena] — builds the boss arena at the command position and summons the boss. */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> bossNode(String name) {
        return Commands.literal(name)
            .executes(ctx -> spawnBoss(ctx, name, true))
            .then(Commands.literal("noarena").executes(ctx -> spawnBoss(ctx, name, false)));
    }

    private static int spawnBoss(CommandContext<CommandSourceStack> ctx, String bossType, boolean arena) {
        var src = ctx.getSource();
        var level = src.getLevel();
        var center = net.minecraft.core.BlockPos.containing(src.getPosition());
        var type = com.matt.quantumfalcon.arena.BossSummoner.arenaFor(bossType);
        if (type == null) { src.sendFailure(Component.literal("Unknown boss: " + bossType)); return 0; }
        var boss = arena ? com.matt.quantumfalcon.arena.BossSummoner.buildAndSummon(level, center, type)
                         : com.matt.quantumfalcon.arena.BossSummoner.spawnBoss(level, center, type);
        if (boss == null) { src.sendFailure(Component.literal("Failed to summon " + bossType)); return 0; }
        src.sendSuccess(() -> Component.literal("Summoned " + boss.getName().getString() + (arena ? " in a new " + type.name() + " arena" : "") + " at " + center.toShortString()), true);
        return 1;
    }

    // ============ Debug Commands ============

    private static int showDebugInfo(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        PlayerSkillData skillData = PlayerSkillData.get(player);
        PlayerQuestData questData = PlayerQuestData.get(player);
        PlayerInstabilityCapability instability = PlayerInstabilityCapability.get(player);

        ctx.getSource().sendSuccess(() -> Component.literal("=== Quantum Falcon Debug ==="), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Skill Points: " + skillData.getAvailableSkillPoints() + 
            " (spent: " + skillData.getTotalSpentPoints() + ")"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Skills Unlocked: " + skillData.getUnlockedSkills().size()), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Active Quests: " + questData.getActiveQuests().size()), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Completed Quests: " + questData.getCompletedQuests().size()), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Instability: " + String.format("%.1f", instability.getInstability()) + "%"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Tamed Variants: " + questData.getTamedVariantCount()), false);

        return 1;
    }
}
