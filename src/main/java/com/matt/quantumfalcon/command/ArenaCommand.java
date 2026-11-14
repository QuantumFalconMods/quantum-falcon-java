package com.matt.quantumfalcon.command;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.worldgen.structure.ArenaTemplateBuilder;
import com.matt.quantumfalcon.worldgen.structure.BossArenaStructure;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Commands for testing boss arenas.
 * /qfarena build <type> - Build an arena at player position
 * /qfarena clear - Clear nearby arena
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class ArenaCommand {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("qfarena")
            .requires(source -> source.hasPermission(2)) // OP level 2
            .then(Commands.literal("build")
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest("guardian");
                        builder.suggest("horror");
                        builder.suggest("twins");
                        builder.suggest("sovereign");
                        return builder.buildFuture();
                    })
                    .executes(ArenaCommand::buildArena)))
            .then(Commands.literal("fight")
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((c, b) -> { for (String t : new String[]{"guardian", "horror", "twins", "sovereign"}) b.suggest(t); return b.buildFuture(); })
                    .executes(ArenaCommand::fight)))
            .then(Commands.literal("signature")
                .executes(ArenaCommand::signature))
            .then(Commands.literal("clear")
                .executes(ArenaCommand::clearArena))
            .then(Commands.literal("info")
                .executes(ArenaCommand::arenaInfo)));
    }
    
    private static int buildArena(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String typeArg = StringArgumentType.getString(context, "type");
        
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        
        BossArenaStructure.ArenaType type = switch (typeArg.toLowerCase()) {
            case "guardian" -> BossArenaStructure.ArenaType.QUANTUM_GUARDIAN;
            case "horror" -> BossArenaStructure.ArenaType.VOID_HORROR;
            case "twins" -> BossArenaStructure.ArenaType.ENTANGLED_TWINS;
            case "sovereign" -> BossArenaStructure.ArenaType.SINGULARITY_SOVEREIGN;
            default -> null;
        };
        
        if (type == null) {
            source.sendFailure(Component.literal("Invalid arena type. Use: guardian, horror, twins, sovereign"));
            return 0;
        }
        
        // Build the arena
        source.sendSuccess(() -> Component.literal("Building " + type.name() + " arena..."), true);
        ArenaTemplateBuilder.buildArena(level, center, type);
        source.sendSuccess(() -> Component.literal("Arena built at " + center + "!"), true);
        
        return 1;
    }
    
    private static BossArenaStructure.ArenaType parse(String t) {
        return switch (t.toLowerCase()) {
            case "guardian" -> BossArenaStructure.ArenaType.QUANTUM_GUARDIAN;
            case "horror" -> BossArenaStructure.ArenaType.VOID_HORROR;
            case "twins" -> BossArenaStructure.ArenaType.ENTANGLED_TWINS;
            case "sovereign" -> BossArenaStructure.ArenaType.SINGULARITY_SOVEREIGN;
            default -> null;
        };
    }

    /** Build an arena here and summon its boss (the intro, seal and pylons start when a player is seen). */
    private static int fight(CommandContext<CommandSourceStack> ctx) {
        var type = parse(StringArgumentType.getString(ctx, "type"));
        if (type == null) { ctx.getSource().sendFailure(Component.literal("Use: guardian, horror, twins, sovereign")); return 0; }
        BlockPos c = BlockPos.containing(ctx.getSource().getPosition());
        var boss = com.matt.quantumfalcon.arena.BossSummoner.buildAndSummon(ctx.getSource().getLevel(), c, type);
        ctx.getSource().sendSuccess(() -> Component.literal(boss != null ? "Summoned " + type.name() + " at " + c.toShortString() : "Summon failed"), true);
        return boss != null ? 1 : 0;
    }

    /** Fire the nearest engaged boss's arena signature now (phase checks skipped). */
    private static int signature(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p)) { src.sendFailure(Component.literal("Must be run by a player")); return 0; }
        for (var f : com.matt.quantumfalcon.arena.fight.ArenaInteraction.FIGHTS.values()) {
            if (src.getLevel().getEntity(f.boss) instanceof net.minecraft.world.entity.Mob boss && boss.distanceTo(p) < f.radius + 20) {
                boolean ok = com.matt.quantumfalcon.arena.fight.ArenaInteraction.signature(src.getLevel(), f, boss, p, 4);
                src.sendSuccess(() -> Component.literal(ok ? "Signature: " + f.type.name() : "Signature could not fire"), false);
                return ok ? 1 : 0;
            }
        }
        src.sendFailure(Component.literal("No engaged boss nearby"));
        return 0;
    }

    private static int clearArena(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }
        
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        
        // Clear a 100 block radius
        int[] cleared = {0};
        for (int x = -50; x <= 50; x++) {
            for (int y = -10; y <= 30; y++) {
                for (int z = -50; z <= 50; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.removeBlock(pos, false);
                        cleared[0]++;
                    }
                }
            }
        }
        
        final int clearedCount = cleared[0];
        source.sendSuccess(() -> Component.literal("Cleared " + clearedCount + " blocks"), true);
        return 1;
    }
    
    private static int arenaInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Boss Arena Types ==="), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("QUANTUM GUARDIAN (Tier 1)")
            .withStyle(net.minecraft.ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("  - 64 block diameter, 4 energy pillars"), false);
        source.sendSuccess(() -> Component.literal("  - No void center (learning arena)"), false);
        source.sendSuccess(() -> Component.literal("  - Energy bridges between pillars"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("VOID HORROR (Tier 2)")
            .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), false);
        source.sendSuccess(() -> Component.literal("  - 48 block irregular, 6 twisted spires"), false);
        source.sendSuccess(() -> Component.literal("  - 4-block void center (active phase 2+)"), false);
        source.sendSuccess(() -> Component.literal("  - Scattered platforms for grappling"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("SINGULARITY SOVEREIGN (Tier 3)")
            .withStyle(net.minecraft.ChatFormatting.DARK_RED), false);
        source.sendSuccess(() -> Component.literal("  - 80 block spiral disk, 8 massive pillars"), false);
        source.sendSuccess(() -> Component.literal("  - 12-block void center (INSTANT DEATH phase 2+)"), false);
        source.sendSuccess(() -> Component.literal("  - Accretion disk visual effect"), false);
        
        return 1;
    }
}
