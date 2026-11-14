package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.QuantumFalcon;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Op-only helper for screenshots and manual testing: /qfdungeon place <structure>, /qfdungeon goto <piece> [n], /qfdungeon list. */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class DungeonDebugCommand {
    private DungeonDebugCommand() {}

    private static DungeonTests.Scan last;
    private static ServerLevel lastLevel;

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("qfdungeon").requires(s -> s.hasPermission(2))
            .then(Commands.literal("place").then(Commands.argument("structure", StringArgumentType.word())
                .executes(c -> place(c.getSource(), StringArgumentType.getString(c, "structure")))))
            .then(Commands.literal("list").executes(c -> list(c.getSource())))
            .then(Commands.literal("visit").then(Commands.argument("structure", StringArgumentType.word())
                .executes(c -> visit(c.getSource(), StringArgumentType.getString(c, "structure")))))
            .then(Commands.literal("view").then(Commands.argument("up", IntegerArgumentType.integer(-64, 128)).then(Commands.argument("back", IntegerArgumentType.integer(0, 128))
                .executes(c -> view(c.getSource(), IntegerArgumentType.getInteger(c, "up"), IntegerArgumentType.getInteger(c, "back"), false)))))
            .then(Commands.literal("viewsouth").then(Commands.argument("up", IntegerArgumentType.integer(-64, 128)).then(Commands.argument("back", IntegerArgumentType.integer(0, 128))
                .executes(c -> view(c.getSource(), IntegerArgumentType.getInteger(c, "up"), IntegerArgumentType.getInteger(c, "back"), true)))))
            .then(Commands.literal("goto").then(Commands.argument("piece", StringArgumentType.word())
                .executes(c -> go(c.getSource(), StringArgumentType.getString(c, "piece"), 0))
                .then(Commands.argument("n", IntegerArgumentType.integer(0))
                    .executes(c -> go(c.getSource(), StringArgumentType.getString(c, "piece"), IntegerArgumentType.getInteger(c, "n")))
                    .then(Commands.argument("at", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                        .then(Commands.argument("look", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                            .executes(c -> goLocal(c.getSource(), StringArgumentType.getString(c, "piece"), IntegerArgumentType.getInteger(c, "n"),
                                c.getArgument("at", net.minecraft.commands.arguments.coordinates.Coordinates.class).getBlockPos(c.getSource().withPosition(net.minecraft.world.phys.Vec3.ZERO)),
                                c.getArgument("look", net.minecraft.commands.arguments.coordinates.Coordinates.class).getBlockPos(c.getSource().withPosition(net.minecraft.world.phys.Vec3.ZERO))))))))));
    }

    /** Overview camera for the last placed structure: {@code up} blocks above its floor centre and {@code back} blocks north of it, looking at the centre. */
    private static int view(CommandSourceStack src, int up, int back, boolean fromSouth) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (last == null) { src.sendFailure(Component.literal("place a structure first")); return 0; }
        BoundingBox bb = last.start.getBoundingBox();
        BlockPos c = new BlockPos(bb.getCenter().getX(), bb.minY() + 10, bb.getCenter().getZ());
        double x = c.getX() + 0.5, y = c.getY() + up, z = c.getZ() + 0.5 + (fromSouth ? back : -back);
        float pitch = (float) Math.toDegrees(Math.atan2(up, Math.max(1, back)));
        p.teleportTo(lastLevel, x, y, z, fromSouth ? 180f : 0f, pitch);
        return 1;
    }

    private static int place(CommandSourceStack src, String id) {
        ServerLevel level = src.getLevel();
        DungeonTests.Scan s = DungeonTests.place(level, id, BlockPos.containing(src.getPosition()));
        if (s == null) { src.sendFailure(Component.literal("no valid start")); return 0; }
        last = s;
        lastLevel = level;
        src.sendSuccess(() -> Component.literal("placed " + id + ": " + s.pieces.size() + " pieces, bbox " + s.start.getBoundingBox()), false);
        return 1;
    }

    /** Natural worldgen: find the nearest naturally generated instance of a structure (like /locate), generate its chunks,
     *  make it the "last" structure for view/goto, and fly the player above it. */
    private static int visit(CommandSourceStack src, String id) {
        ServerLevel level = src.getLevel();
        var reg = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.matt.quantumfalcon.QuantumFalcon.MOD_ID, id));
        var holder = reg.getHolder(key).orElse(null);
        if (holder == null) { src.sendFailure(Component.literal("unknown structure " + id)); return 0; }
        var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holder), BlockPos.containing(src.getPosition()), 100, false);
        if (found == null) { src.sendFailure(Component.literal("none within 100 chunks")); return 0; }
        BlockPos at = found.getFirst();
        var chunk = level.getChunk(at.getX() >> 4, at.getZ() >> 4);
        var start = chunk.getStartForStructure(holder.value());
        if (start == null || !start.isValid()) { src.sendFailure(Component.literal("start not found at " + at.toShortString())); return 0; }
        BoundingBox bb = start.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        last = DungeonTests.scan(level, id, start);
        lastLevel = level;
        src.sendSuccess(() -> Component.literal("visit " + id + " natural at " + bb.getCenter().toShortString() + ": " + last.pieces.size() + " pieces, bbox " + bb), false);
        if (src.getEntity() instanceof ServerPlayer p) p.teleportTo(level, bb.getCenter().getX() + 0.5, bb.maxY() + 12, bb.getCenter().getZ() + 0.5, 0f, 60f);
        com.matt.quantumfalcon.QuantumFalcon.LOGGER.info("[visit] {} natural at {} bbox {}", id, bb.getCenter().toShortString(), bb);
        return 1;
    }

    private static int list(CommandSourceStack src) {
        if (last == null) return 0;
        StringBuilder b = new StringBuilder();
        for (StructurePiece p : last.start.getPieces()) b.append(name(p)).append(' ');
        src.sendSuccess(() -> Component.literal(b.toString()), false);
        return 1;
    }

    static String name(StructurePiece p) {
        String n = p instanceof PoolElementStructurePiece pe ? pe.getElement().toString() : p.getClass().getSimpleName();
        return n.substring(n.lastIndexOf('/') + 1).replace("]", "");
    }

    /** Teleports to template-local {@code at} inside the n-th matching piece, looking at template-local {@code look}
     *  (both transformed by the piece's rotation). Use absolute numbers, e.g. "goto sanctum 0 7 2 4 7 2 9". */
    private static int goLocal(CommandSourceStack src, String piece, int n, BlockPos at, BlockPos look) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (last == null) { src.sendFailure(Component.literal("place a dungeon first")); return 0; }
        int seen = 0;
        for (StructurePiece sp : last.start.getPieces()) {
            if (!(sp instanceof PoolElementStructurePiece pe) || !name(sp).contains(piece) || seen++ != n) continue;
            BlockPos a = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(at, net.minecraft.world.level.block.Mirror.NONE, pe.getRotation(), BlockPos.ZERO).offset(pe.getPosition());
            BlockPos l = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(look, net.minecraft.world.level.block.Mirror.NONE, pe.getRotation(), BlockPos.ZERO).offset(pe.getPosition());
            double dx = l.getX() - a.getX(), dy = l.getY() - a.getY(), dz = l.getZ() - a.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            p.teleportTo(lastLevel, a.getX() + 0.5, a.getY(), a.getZ() + 0.5, yaw, pitch);
            return 1;
        }
        src.sendFailure(Component.literal("no piece " + piece + " #" + n));
        return 0;
    }

    /** Teleports to the floor at the centre of the n-th matching piece, looking toward the following piece. */
    private static int go(CommandSourceStack src, String piece, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (last == null) { src.sendFailure(Component.literal("place a dungeon first")); return 0; }
        var pieces = last.start.getPieces();
        int seen = 0;
        for (int i = 0; i < pieces.size(); i++) {
            if (!name(pieces.get(i)).contains(piece) || seen++ != n) continue;
            BoundingBox bb = pieces.get(i).getBoundingBox();
            BlockPos at = name(pieces.get(i)).equals("entrance") && com.matt.quantumfalcon.dungeon.ReturnRiftBlock.entranceOf(lastLevel, bb.getCenter()) != null
                ? com.matt.quantumfalcon.dungeon.ReturnRiftBlock.entranceOf(lastLevel, bb.getCenter())
                : new BlockPos(bb.getCenter().getX(), bb.minY() + 1, bb.getCenter().getZ());
            BlockPos look = i + 1 < pieces.size() ? pieces.get(i + 1).getBoundingBox().getCenter() : bb.getCenter().north(4);
            double dx = look.getX() - at.getX(), dz = look.getZ() - at.getZ();
            float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
            p.teleportTo(lastLevel, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 10f);
            String nm = name(pieces.get(i));
            src.sendSuccess(() -> Component.literal("at " + nm + " " + at.toShortString()), false);
            return 1;
        }
        src.sendFailure(Component.literal("no piece " + piece + " #" + n));
        return 0;
    }
}
