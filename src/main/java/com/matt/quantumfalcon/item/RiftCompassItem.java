package com.matt.quantumfalcon.item;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Quantum Compass / Echo Compass: right-click to locate the nearest structure in a structure tag.
 * Quantum Compass -> #quantumfalcon:rifts (Rift Shrines and Echo Rift Gates).
 * Echo Compass    -> #quantumfalcon:boss_arenas (the four boss arenas; works in the Echo Rift too).
 * Stores the target as a lodestone tracker and draws a particle trail toward it.
 */
public class RiftCompassItem extends Item {
    private final TagKey<Structure> target;
    private final String label;

    public RiftCompassItem(Properties props, String structureTag, String label) {
        super(props.stacksTo(1));
        this.target = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, structureTag));
        this.label = label;
    }

    public TagKey<Structure> targetTag() { return target; }

    /** Server-side locate; public so tests can call it without a player. */
    public static Optional<BlockPos> locate(ServerLevel level, BlockPos from, TagKey<Structure> tag) {
        var found = level.findNearestMapStructure(tag, from, 100, false);
        return Optional.ofNullable(found);
    }

    /** boss order; the Echo Compass points at the first one this player hasn't beaten. */
    static final String[][] BOSS_ORDER = {
        {"QUANTUM_OVERSEER", "quantum_guardian_arena", "Quantum Overseer arena"},
        {"RIFT_WARDEN", "void_horror_arena", "Rift Warden arena"},
        {"ENTANGLED_TWINS", "entangled_twins_arena", "Entangled Twins arena"},
        {"SINGULARITY_SOVEREIGN", "singularity_sovereign_arena", "Singularity Sovereign arena"}};

    /** The next unbeaten boss for this player: {bossType, structure path, label}, or null when all four are beaten. */
    public static String[] nextBoss(Player player) {
        var sd = com.matt.quantumfalcon.story.PlayerStoryData.get(player);
        for (String[] b : BOSS_ORDER) if (!sd.hasDefeatedBoss(b[0])) return b;
        return null;
    }

    /** Nearest arena of one type (structure path). */
    public static Optional<BlockPos> locateOne(ServerLevel level, BlockPos from, String structurePath) {
        var reg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var holder = reg.getHolder(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, structurePath)));
        if (holder.isEmpty()) return Optional.empty();
        var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder.get()), from, 100, false);
        return found == null ? Optional.empty() : Optional.of(found.getFirst());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            player.getCooldowns().addCooldown(this, 60);
            String what = label;
            Optional<BlockPos> pos;
            String[] next = target.location().getPath().equals("boss_arenas") ? nextBoss(player) : null;
            if (next != null) { what = next[2]; pos = locateOne(sl, player.blockPosition(), next[1]); }
            else pos = locate(sl, player.blockPosition(), target);
            if (pos.isEmpty()) {
                player.displayClientMessage(Component.literal("The needle spins aimlessly \u2014 no " + what + " in range.").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            BlockPos p = pos.get();
            stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(sl.dimension(), p)), false));
            double dx = p.getX() + 0.5 - player.getX(), dz = p.getZ() + 0.5 - player.getZ();
            int dist = (int) Math.sqrt(dx * dx + dz * dz);
            String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
            int idx = (int) Math.floorMod(Math.round(Math.toDegrees(Math.atan2(-dx, dz)) / 45.0), 8);
            player.displayClientMessage(Component.literal("\u2726 " + what + " detected " + dist + " blocks " + dirs[idx]
                    + " (" + p.getX() + ", " + p.getZ() + ")").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            Vec3 dir = new Vec3(dx, 0, dz).normalize();
            for (int i = 1; i <= 12; i++) {
                sl.sendParticles(ParticleTypes.END_ROD, player.getX() + dir.x * i * 0.6, player.getEyeY() - 0.3, player.getZ() + dir.z * i * 0.6, 1, 0, 0, 0, 0);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1f, 1.2f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) { return stack.has(DataComponents.LODESTONE_TRACKER); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(target.location().getPath().equals("boss_arenas") ? "Right-click to find the next boss you haven't beaten"
            : "Right-click to locate the nearest " + label).withStyle(ChatFormatting.GRAY));
        LodestoneTracker t = stack.get(DataComponents.LODESTONE_TRACKER);
        if (t != null && t.target().isPresent()) {
            BlockPos p = t.target().get().pos();
            tip.add(Component.literal("Locked: " + p.getX() + ", " + p.getZ()).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
