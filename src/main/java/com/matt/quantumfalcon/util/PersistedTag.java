package com.matt.quantumfalcon.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * player data that must survive death lives under {@link Player#PERSISTED_NBT_TAG}; NeoForge's
 * {@code ServerPlayer.restoreFrom} copies only that sub-tag. Data saved by older builds at the root is moved in on first read.
 */
public final class PersistedTag {
    private PersistedTag() {}

    public static CompoundTag of(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    /** The persisted value for {@code key}, migrating a legacy root-level copy. */
    public static CompoundTag of(Player p, String key) {
        CompoundTag root = p.getPersistentData(), pers = of(p);
        if (root.contains(key)) { if (!pers.contains(key)) pers.put(key, root.get(key).copy()); root.remove(key); }
        return pers;
    }
}
