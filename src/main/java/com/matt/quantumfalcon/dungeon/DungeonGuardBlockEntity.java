package com.matt.quantumfalcon.dungeon;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Role "key": wakes an elite that drops the realm's dungeon key.
 * Role "boss": wakes the dungeon warden; its death unseals the treasury vault.
 */
public class DungeonGuardBlockEntity extends BlockEntity {
    public static final String TAG_KEY = "qf_dungeon_key", TAG_BOSS = "qf_dungeon_warden", TAG_REALM = "qf_realm_";
    private static final double WAKE_RANGE = 10.0;

    private String mob = "minecraft:zombie", role = "key", realm = "riftslate";
    private int tier = 1;

    public DungeonGuardBlockEntity(BlockPos pos, BlockState state) {
        super(ModDungeon.GUARD_BE.get(), pos, state);
    }

    public String role() { return role; }
    public String realm() { return realm; }
    public String mob() { return mob; }

    void serverTick() {
        if (!(level instanceof ServerLevel sl) || sl.getGameTime() % 10 != 0) return;
        Player near = sl.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, WAKE_RANGE, true);
        if (near == null || near.isSpectator() || near.isCreative()) return;
        wake(sl);
    }

    /** Spawns the elite and turns the sigil into the realm's chiseled stone. */
    public Entity wake(ServerLevel sl) {
        BlockState spent = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "chiseled_" + realm))
            .map(Block::defaultBlockState).orElse(net.minecraft.world.level.block.Blocks.CHISELED_DEEPSLATE.defaultBlockState());
        Entity e = EntityType.byString(mob).map(t -> t.create(sl)).orElse(null);
        sl.setBlock(worldPosition, spent, 3);
        if (e == null) return null;
        boolean boss = "boss".equals(role);
        e.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        if (e instanceof Mob m) {
            EventHooks.finalizeMobSpawn(m, sl, sl.getCurrentDifficultyAt(worldPosition), MobSpawnType.STRUCTURE, null);
            m.setPersistenceRequired();
            scale(m.getAttribute(Attributes.MAX_HEALTH), boss ? 2.5 + 0.5 * tier : 1.4 + 0.2 * tier);
            scale(m.getAttribute(Attributes.ATTACK_DAMAGE), 1.0 + 0.15 * tier);
            m.setHealth(m.getMaxHealth());
        }
        String title = realm.substring(0, 1).toUpperCase() + realm.substring(1);
        e.setCustomName(Component.literal(!name.isEmpty() ? name : boss ? title + " Warden" : title + " Keybearer").withStyle(boss ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        e.setCustomNameVisible(true);
        e.addTag(boss ? TAG_BOSS : TAG_KEY);
        e.addTag(TAG_REALM + realm);
        e.addTag(DungeonSites.homeTag(worldPosition));
        sl.addFreshEntity(e);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 1, e.getZ(), 60, 0.5, 1.0, 0.5, 0.1);
        sl.playSound(null, worldPosition, boss ? SoundEvents.WITHER_SPAWN : SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, boss ? 0.6f : 1.0f, 1.0f);
        return e;
    }

    private static void scale(AttributeInstance a, double k) {
        if (a != null) a.setBaseValue(a.getBaseValue() * k);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putString("Mob", mob); tag.putString("Role", role); tag.putString("Realm", realm); tag.putInt("Tier", tier);
        if (!name.isEmpty()) tag.putString("Name", name);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (tag.contains("Mob")) mob = tag.getString("Mob");
        if (tag.contains("Role")) role = tag.getString("Role");
        if (tag.contains("Realm")) realm = tag.getString("Realm");
        if (tag.contains("Tier")) tier = tag.getInt("Tier");
        if (tag.contains("Name")) name = tag.getString("Name");
    }
    /** A10: an authored guard's own name (Skywarden Irel); empty = "<Realm> Warden/Keybearer". */
    private String name = "";
    public String name() { return name; }
}
