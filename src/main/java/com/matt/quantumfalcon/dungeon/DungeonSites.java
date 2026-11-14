package com.matt.quantumfalcon.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Identity and per-site state for generated dungeons. A site is one structure start of {@code #quantumfalcon:dungeons}:
 * its key is {@code <dimension>|<structure id>@<start chunk>}, so it is stable across restarts.
 * <ul>
 * <li>the warden's death opens every vault in its own dungeon, wherever it dies; the vault also remembers.</li>
 * <li>keys are cut for one site; the door checks the site.</li>
 * <li>the lock remembers everyone who earned that site's key, so a lost key never soft-locks the door.</li>
 * </ul>
 */
public final class DungeonSites extends SavedData {
    private static final String NAME = "quantumfalcon_dungeon_sites";
    public static final String TAG_HOME = "qf_dungeon_home_";

    private final Set<String> wardenDead = new HashSet<>();
    private final Map<String, Set<UUID>> keyEarned = new HashMap<>();

    public static DungeonSites get(ServerLevel any) {
        return any.getServer().overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(DungeonSites::new, DungeonSites::load, null), NAME);
    }

    /** The dungeon structure start whose pieces contain {@code pos}, or null. */
    public static StructureStart startAt(ServerLevel sl, BlockPos pos) {
        StructureStart s = sl.structureManager().getStructureWithPieceAt(pos, ReturnRiftBlock.DUNGEONS);
        return s.isValid() ? s : null;
    }

    public static String key(ServerLevel sl, StructureStart s) {
        ResourceLocation id = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(s.getStructure());
        return sl.dimension().location() + "|" + id + "@" + s.getChunkPos().toLong();
    }

    /** Site key at pos, or null outside every dungeon. */
    public static String keyAt(ServerLevel sl, BlockPos pos) {
        StructureStart s = startAt(sl, pos);
        return s == null ? null : key(sl, s);
    }

    /** Realm of a dungeon structure id: the last segment of the path (dungeon_nether_cinderite -> cinderite). */
    public static String realmOf(String siteKey) {
        if (siteKey == null) return null;
        int at = siteKey.indexOf('@');
        String id = at < 0 ? siteKey : siteKey.substring(0, at);
        return id.substring(id.lastIndexOf('_') + 1);
    }

    /** Opens every sealed treasury vault inside the start's pieces. Returns how many opened. */
    public static int unlockVaults(ServerLevel sl, StructureStart s) {
        int opened = 0;
        for (StructurePiece piece : s.getPieces()) {
            BoundingBox bb = piece.getBoundingBox();
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
                BlockState st = sl.getBlockState(p);
                if (st.getBlock() instanceof TreasuryVaultBlock && !st.getValue(TreasuryVaultBlock.OPEN)) {
                    openVault(sl, p.immutable(), st);
                    opened++;
                }
            }
        }
        return opened;
    }

    static void openVault(ServerLevel sl, BlockPos p, BlockState st) {
        sl.setBlock(p, st.setValue(TreasuryVaultBlock.OPEN, true), 3);
        sl.playSound(null, p, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 1.2f, 1.0f);
        sl.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.05);
    }

    /** Home tag written on a guard's elite when it wakes: the guard block position (always inside its dungeon). */
    public static String homeTag(BlockPos pos) { return TAG_HOME + pos.getX() + "_" + pos.getY() + "_" + pos.getZ(); }

    public static BlockPos homeOf(Set<String> tags) {
        for (String t : tags) {
            if (!t.startsWith(TAG_HOME)) continue;
            String[] p = t.substring(TAG_HOME.length()).split("_");
            if (p.length != 3) continue;
            try { return new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])); }
            catch (NumberFormatException ignored) { }
        }
        return null;
    }

    // ------------------------------------------------------------------ state
    public boolean isWardenDead(String site) { return site != null && wardenDead.contains(site); }
    public void markWardenDead(String site) { if (site != null && wardenDead.add(site)) setDirty(); }

    public boolean earnedKey(String site, UUID who) { return site != null && keyEarned.getOrDefault(site, Set.of()).contains(who); }
    public void markKeyEarned(String site, UUID who) {
        if (site != null && keyEarned.computeIfAbsent(site, k -> new HashSet<>()).add(who)) setDirty();
    }

    /** Tests: forget a site's warden kill and earned keys (test worlds are reused between runs). */
    public void forget(String site) {
        if (wardenDead.remove(site) | keyEarned.remove(site) != null) setDirty();
    }

    public static DungeonSites load(CompoundTag tag, HolderLookup.Provider reg) {
        DungeonSites d = new DungeonSites();
        for (Tag t : tag.getList("WardenDead", Tag.TAG_STRING)) d.wardenDead.add(t.getAsString());
        CompoundTag keys = tag.getCompound("KeyEarned");
        for (String site : keys.getAllKeys()) {
            Set<UUID> s = new HashSet<>();
            for (Tag t : keys.getList(site, Tag.TAG_STRING)) {
                try { s.add(UUID.fromString(t.getAsString())); } catch (IllegalArgumentException ignored) { }
            }
            d.keyEarned.put(site, s);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        ListTag dead = new ListTag();
        for (String s : wardenDead) dead.add(StringTag.valueOf(s));
        tag.put("WardenDead", dead);
        CompoundTag keys = new CompoundTag();
        keyEarned.forEach((site, set) -> {
            ListTag l = new ListTag();
            for (UUID u : set) l.add(StringTag.valueOf(u.toString()));
            keys.put(site, l);
        });
        tag.put("KeyEarned", keys);
        return tag;
    }
}
