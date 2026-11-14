package com.matt.quantumfalcon.dungeon;

import com.matt.quantumfalcon.QuantumFalcon;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

/**
 * the client learns the warded boxes near the player, so a warded block doesn't show cracks for the
 * server to refuse (the "ghost cracking" seen when mining dungeon stone). The server stays the authority: this
 * only drives {@link DungeonWard#onBreakSpeed} on the client.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class WardSync {
    private WardSync() {}
    public static final int RADIUS_CHUNKS = 5, EVERY = 40;
    private static final Map<UUID, Integer> SENT = new HashMap<>();

    public record Payload(String dim, List<Integer> boxes) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "ward_boxes"));
        public static final StreamCodec<ByteBuf, Payload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Payload::dim, ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), Payload::boxes, Payload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        public static void handle(Payload p, IPayloadContext ctx) { ctx.enqueueWork(() -> Client.set(p.dim(), p.boxes())); }
    }

    /** Client copy (plain data, safe on both sides). */
    public static final class Client {
        private Client() {}
        static volatile String dim = "";
        static volatile int[] boxes = new int[0];
        public static void set(String d, List<Integer> b) { int[] a = new int[b.size()]; for (int i = 0; i < a.length; i++) a[i] = b.get(i); dim = d; boxes = a; }
        public static boolean inside(String levelDim, BlockPos p) {
            if (!levelDim.equals(dim)) return false;
            int[] b = boxes;
            for (int i = 0; i + 5 < b.length; i += 6)
                if (p.getX() >= b[i] && p.getY() >= b[i + 1] && p.getZ() >= b[i + 2] && p.getX() <= b[i + 3] && p.getY() <= b[i + 4] && p.getZ() <= b[i + 5]) return true;
            return false;
        }
    }

    /** Warded boxes (world coords) of every dungeon with a reference within {@code r} chunks of {@code at}. */
    public static List<BoundingBox> boxesNear(ServerLevel sl, BlockPos at, int r) {
        List<BoundingBox> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        var reg = sl.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        ChunkPos c0 = new ChunkPos(at);
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            var chunk = sl.getChunk(c0.x + dx, c0.z + dz, ChunkStatus.STRUCTURE_REFERENCES, false);
            if (chunk == null) continue;
            for (var e : chunk.getAllReferences().entrySet()) {
                if (!reg.wrapAsHolder(e.getKey()).is(ReturnRiftBlock.DUNGEONS)) continue;
                for (long ref : e.getValue()) {
                    if (!seen.add(ref ^ ((long) reg.getId(e.getKey()) << 52))) continue;
                    ChunkPos rp = new ChunkPos(ref);
                    var startChunk = sl.getChunk(rp.x, rp.z, ChunkStatus.STRUCTURE_STARTS, false);
                    if (startChunk == null) continue;
                    StructureStart st = sl.structureManager().getStartForStructure(SectionPos.bottomOf(startChunk), e.getKey(), startChunk);
                    if (st == null || !st.isValid()) continue;
                    if (st.getStructure() instanceof com.matt.quantumfalcon.worldgen.structure.FrozenHourStructure) {
                        List<BoundingBox> w = FrozenHours.wardBoxes(sl, st);
                        if (w != null) { out.addAll(w); continue; }
                    }
                    for (StructurePiece p : st.getPieces()) out.add(p.getBoundingBox());
                }
            }
        }
        return out;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.tickCount % EVERY != 0 || !(sp.level() instanceof ServerLevel sl)) return;
        List<Integer> flat = new ArrayList<>();
        for (BoundingBox b : boxesNear(sl, sp.blockPosition(), RADIUS_CHUNKS))
            Collections.addAll(flat, b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ());
        String dim = sl.dimension().location().toString();
        int hash = Objects.hash(dim, flat);
        if (Objects.equals(SENT.get(sp.getUUID()), hash)) return;
        SENT.put(sp.getUUID(), hash);
        PacketDistributor.sendToPlayer(sp, new Payload(dim, flat));
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) { SENT.remove(e.getEntity().getUUID()); }
}
