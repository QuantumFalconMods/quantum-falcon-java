package com.matt.quantumfalcon.dungeon;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;

/** Shortcut behind the antechamber lever: steps you back to the dungeon's entrance hut. */
public class ReturnRiftBlock extends Block {
    public static final TagKey<Structure> DUNGEONS = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "dungeons"));
    /** Standing spot inside the entrance hut, in entrance-template coordinates (tools/gen_dungeons.py). */
    public static final BlockPos HUT_SPOT = new BlockPos(4, 13, 17);

    public ReturnRiftBlock(Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return InteractionResult.sidedSuccess(level.isClientSide);
        BlockPos dest = entranceOf(sl, pos);
        if (dest == null) {
            sp.displayClientMessage(Component.literal("The rift flickers but leads nowhere.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        sp.teleportTo(sl, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, sp.getYRot(), sp.getXRot());
        sl.playSound(null, dest, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.3f);
        sp.displayClientMessage(Component.literal("The rift returns you to the surface.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return InteractionResult.CONSUME;
    }

    /** World position of the entrance hut floor for the dungeon containing {@code pos}, or null. */
    public static BlockPos entranceOf(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, DUNGEONS);
        if (!start.isValid() || start.getPieces().isEmpty()) return null;
        StructurePiece first = start.getPieces().get(0);
        if (!(first instanceof PoolElementStructurePiece piece)) return null;
        return StructureTemplate.transform(HUT_SPOT, Mirror.NONE, piece.getRotation(), BlockPos.ZERO).offset(piece.getPosition());
    }
}
