package com.matt.quantumfalcon.dungeon;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Keybearer deaths drop the realm key; warden deaths unseal nearby treasury vaults. */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class DungeonEvents {
    private DungeonEvents() {}

    static final String FIGHTERS = "qf_key_fighters";

    /** remember every player who landed a hit on a keybearer. */
    @SubscribeEvent
    public static void onHurt(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || !e.getTags().contains(DungeonGuardBlockEntity.TAG_KEY)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getNewDamage() <= 0) return;
        var list = e.getPersistentData().getList(FIGHTERS, net.minecraft.nbt.Tag.TAG_INT_ARRAY);
        var id = net.minecraft.nbt.NbtUtils.createUUID(p.getUUID());
        if (!list.contains(id)) { list.add(id); e.getPersistentData().put(FIGHTERS, list); }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel sl)) return;
        var tags = e.getTags();
        if (tags.contains(DungeonGuardBlockEntity.TAG_KEY)) {
            String realm = realmOf(tags);
            var key = ModDungeon.KEYS.getOrDefault(realm, ModDungeon.KEYS.get("riftslate"));
            String site = siteOf(sl, e, tags);
            e.spawnAtLocation(DungeonKeyItem.forSite(new ItemStack(key.get()), site));
            // the lock remembers who earned this site's key, so losing it never soft-locks the door
            DungeonSites sites = DungeonSites.get(sl);
            // only those who fought the keybearer earn the key, not every bystander within 32 blocks
            for (net.minecraft.nbt.Tag t : e.getPersistentData().getList(FIGHTERS, net.minecraft.nbt.Tag.TAG_INT_ARRAY))
                sites.markKeyEarned(site, net.minecraft.nbt.NbtUtils.loadUUID(t));
            if (event.getSource().getEntity() instanceof ServerPlayer p) {
                sites.markKeyEarned(site, p.getUUID());
                p.displayClientMessage(Component.literal("The keybearer drops a dungeon key.").withStyle(ChatFormatting.AQUA), true);
            }
        }
        if (tags.contains(DungeonGuardBlockEntity.TAG_BOSS)) {
            int opened = unlockForWarden(sl, e, tags);
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, e.getBoundingBox().inflate(32))) {
                p.displayClientMessage(Component.literal(opened > 0 ? "The warden falls. The treasury unseals." : "The warden falls.").withStyle(ChatFormatting.GOLD), false);
            }
        }
    }

    /**
     * the warden's dungeon is the one holding its guard sigil (home tag), or failing that the one it died in.
     * Every vault in that dungeon opens, however far from the vault the warden fell, and the site remembers the kill.
     * Wardens with no dungeon at all (spawned by hand) fall back to the old 24-block search.
     */
    public static int unlockForWarden(ServerLevel sl, LivingEntity e, java.util.Set<String> tags) {
        BlockPos home = DungeonSites.homeOf(tags);
        StructureStart start = home != null ? DungeonSites.startAt(sl, home) : null;
        if (start == null) start = DungeonSites.startAt(sl, e.blockPosition());
        if (start == null) return TreasuryVaultBlock.unlockAround(sl, home != null ? home : e.blockPosition(), 24);
        DungeonSites.get(sl).markWardenDead(DungeonSites.key(sl, start));
        return DungeonSites.unlockVaults(sl, start);
    }

    /** Site key of a guard's elite: its home sigil's dungeon, else where it stands. */
    static String siteOf(ServerLevel sl, LivingEntity e, java.util.Set<String> tags) {
        BlockPos home = DungeonSites.homeOf(tags);
        String site = home != null ? DungeonSites.keyAt(sl, home) : null;
        return site != null ? site : DungeonSites.keyAt(sl, e.blockPosition());
    }

    private static String realmOf(java.util.Set<String> tags) {
        for (String t : tags) if (t.startsWith(DungeonGuardBlockEntity.TAG_REALM)) return t.substring(DungeonGuardBlockEntity.TAG_REALM.length());
        return "riftslate";
    }
}
