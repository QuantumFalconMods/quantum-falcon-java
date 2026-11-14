package com.matt.quantumfalcon.skills;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.leveling.PlacedBlocks;
import com.matt.quantumfalcon.skill.PlayerSkillData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.*;
import java.util.function.Supplier;

/**
 * A9: use-based skills. Every XP award goes through {@link #award} so the anti-farm rules live in one place:
 * natural blocks only (PlacedBlocks + instant-break rule), hostile living targets with a per-target budget,
 * no XP while AFK (60 s without input), per-falcon and per-Rift-hour caps, first-time-only exploration, distinct breeding pairs.
 * Levels live in a copyOnDeath player attachment, so they survive death, relog and dimension change.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class UseSkills {
    private UseSkills() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, QuantumFalcon.MOD_ID);
    public static final Supplier<AttachmentType<CompoundTag>> DATA = ATTACHMENTS.register("use_skills",
        () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC).copyOnDeath().build());
    public static void register(IEventBus bus) { ATTACHMENTS.register(bus); }

    // ------------------------------------------------------------------ tuning
    public static final int[] MILESTONES = {25, 50};
    /** Total bonus tree points use-skills can ever give (keeps the 24-point tree economy: 24 + 4 max). */
    public static int BONUS_POINTS_CAP = 4;
    public static final long AFK_TICKS = 20L * 60;
    public static final int COMBAT_AWARDS_PER_TARGET = 10;
    public static final long TARGET_WINDOW_TICKS = 20L * 60 * 20;
    public static final long COMBAT_XP_PER_HOUR = 2400, ARCHERY_XP_PER_HOUR = 1200;
    public static final long FALCON_XP_PER_HOUR = 240, ARCANA_XP_PER_HOUR = 360, HOUR_TICKS = 20L * 60 * 60;
    public static final int FIRSTS_LIMIT = 16384;
    static int firstHash(String k) { long x = 1125899906842597L; for (int i = 0; i < k.length(); i++) x = 31 * x + k.charAt(i); return (int) (x ^ (x >>> 32)); }

    // ------------------------------------------------------------------ data
    static CompoundTag tag(Player p) { return p.getData(DATA); }
    static void save(Player p, CompoundTag t) { p.setData(DATA, t); }
    /** Test hook: replace a player's skill data (e.g. an old save's layout). */
    public static void testLoad(Player p, CompoundTag t) { save(p, t); }

    public static long xp(Player p, UseSkill s) {
        if (p.level().isClientSide) return UseSkillsClientState.xp(s);
        return tag(p).getLong("xp." + s.key());
    }
    public static int level(Player p, UseSkill s) { return UseSkill.levelOf(xp(p, s)); }
    public static int bonusPointsGiven(Player p) { return tag(p).getInt("bonusPoints"); }

    // ------------------------------------------------------------------ AFK
    private static final Map<UUID, long[]> ACTIVITY = new HashMap<>();   // uuid -> {lastActiveTick, lastPoseHash}
    /** Players that haven't moved or turned for 60 s earn nothing (FakePlayers and fresh logins count as active). */
    public static boolean isAfk(Player p) {
        long[] a = ACTIVITY.get(p.getUUID());
        return a != null && p.level().getGameTime() - a[0] > AFK_TICKS;
    }
    public static void testSetIdle(Player p, long ticks) { ACTIVITY.put(p.getUUID(), new long[]{p.level().getGameTime() - ticks, 0}); }
    public static void testClearIdle(Player p) { ACTIVITY.remove(p.getUUID()); }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (p.level().isClientSide) return;
        long h = Double.hashCode(Math.floor(p.getX() * 4)) * 31L + Double.hashCode(Math.floor(p.getZ() * 4)) * 17L
            + Float.hashCode(Math.round(p.getYRot())) * 7L + Float.hashCode(Math.round(p.getXRot()));   // swinging alone (an auto-clicker) is not activity
        long[] a = ACTIVITY.computeIfAbsent(p.getUUID(), k -> new long[]{p.level().getGameTime(), h});
        if (a[1] != h) { a[1] = h; a[0] = p.level().getGameTime(); }
        if (p instanceof ServerPlayer sp && p.tickCount % 40 == 0) exploreTick(sp);
    }

    // ------------------------------------------------------------------ the one gate
    /** Result codes for tests/diagnostics. */
    public enum Gate { OK, AFK, CAPPED, REPEAT, INVALID }

    /**
     * Adds XP to a skill if the anti-farm rules allow it. {@code source} is an optional rule key:
     * "first:<id>" (once ever), "falcon:<uuid>" (hourly cap per falcon), "rift" (hourly cap).
     */
    public static Gate award(Player p, UseSkill s, long amount, String source) {
        if (!(p instanceof ServerPlayer) && !(p instanceof net.neoforged.neoforge.common.util.FakePlayer)) return Gate.INVALID;
        if (p.level().isClientSide || amount <= 0) return Gate.INVALID;
        if (isAfk(p)) return Gate.AFK;
        CompoundTag t = tag(p).copy();
        long now = p.level().getGameTime();
        if (source != null && source.startsWith("first:")) {
            // firsts are kept as 32-bit hashes and never evicted (the old 4096-string list rolled over and paid again);
            // the old string list is folded in on first use. Past the limit, new firsts simply stop paying.
            String k = source.substring(6);
            int[] hs = t.getIntArray("firstsH");
            if (t.contains("firsts")) {
                ListTag old = t.getList("firsts", Tag.TAG_STRING);
                int[] m = java.util.Arrays.copyOf(hs, hs.length + old.size());
                for (int i = 0; i < old.size(); i++) m[hs.length + i] = firstHash(old.getString(i));
                hs = m; t.remove("firsts");
            }
            int h = firstHash(k);
            for (int x : hs) if (x == h) { t.putIntArray("firstsH", hs); save(p, t); return Gate.REPEAT; }
            if (hs.length >= FIRSTS_LIMIT) return Gate.CAPPED;
            hs = java.util.Arrays.copyOf(hs, hs.length + 1); hs[hs.length - 1] = h;
            t.putIntArray("firstsH", hs);
        } else if (source != null && (source.startsWith("falcon:") || source.equals("rift") || source.equals("combat") || source.equals("archery"))) {
            // Combat and Archery also have a per-player hourly cap (a stream of fresh mobs at a grinder levels no faster)
            long cap = switch (source) { case "rift" -> ARCANA_XP_PER_HOUR; case "combat" -> COMBAT_XP_PER_HOUR; case "archery" -> ARCHERY_XP_PER_HOUR; default -> FALCON_XP_PER_HOUR; };
            CompoundTag caps = t.getCompound("caps");
            long[] w = caps.contains(source) ? caps.getLongArray(source) : new long[]{now, 0};
            if (now - w[0] > HOUR_TICKS) w = new long[]{now, 0};
            if (w[1] >= cap) return Gate.CAPPED;
            amount = Math.min(amount, cap - w[1]);
            w[1] += amount;
            caps.putLongArray(source, w);
            t.put("caps", caps);
        }
        long before = t.getLong("xp." + s.key());
        int l0 = UseSkill.levelOf(before);
        long after = before + amount;
        t.putLong("xp." + s.key(), after);
        int l1 = UseSkill.levelOf(after);
        boolean milestone = false;
        for (int lv = l0 + 1; lv <= l1; lv++) {
            for (int m : MILESTONES) if (lv == m && t.getInt("bonusPoints") < BONUS_POINTS_CAP) {
                t.putInt("bonusPoints", t.getInt("bonusPoints") + 1);
                PlayerSkillData.get(p).addSkillPoints(1);
                milestone = true;
            }
        }
        save(p, t);
        if (p instanceof ServerPlayer sp && !(p instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
            if (milestone) com.matt.quantumfalcon.network.ModNetworking.syncSkillPoints(sp);
            sync(sp, l1 > l0 ? s.ordinal() : -1, l1, milestone, false);
        }
        if (l1 > l0 && p instanceof ServerPlayer sp2) com.matt.quantumfalcon.codex.RiftCodex.onObjective(sp2, "USE_SKILL", s.key(), l1, true);   // A1 Codex
        return Gate.OK;
    }

    /** Admin/test: set a skill's XP directly (no milestone points). */
    public static void setXp(Player p, UseSkill s, long xp) { CompoundTag t = tag(p).copy(); t.putLong("xp." + s.key(), xp); save(p, t); }

    public static void sync(ServerPlayer sp, int toastSkill, int toastLevel, boolean milestone, boolean open) {
        List<Long> xs = new ArrayList<>();
        for (UseSkill s : UseSkill.values()) xs.add(tag(sp).getLong("xp." + s.key()));
        PacketDistributor.sendToPlayer(sp, new com.matt.quantumfalcon.network.UseSkillsPayload(xs, toastSkill, toastLevel, milestone, open));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer sp) sync(sp, -1, 0, false, false); }
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer sp) sync(sp, -1, 0, false, false); }
    @SubscribeEvent
    public static void onDim(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer sp) sync(sp, -1, 0, false, false); }

    // ------------------------------------------------------------------ Mining / Woodcutting
    public static boolean isLog(BlockState s) { return s.is(BlockTags.LOGS); }
    public static boolean isOre(BlockState s) { return s.is(Tags.Blocks.ORES); }
    public static boolean isStoneLike(BlockState s) { return s.is(BlockTags.MINEABLE_WITH_PICKAXE); }

    private static final Map<UUID, BlockPos> NATURAL_BREAK = new HashMap<>();

    /** HIGH: read the marker before the tool-levelling handler consumes it. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent e) {
        Player p = e.getPlayer();
        if (p == null || p.level().isClientSide || e.isCanceled()) return;
        BlockState st = e.getState();
        boolean placed = PlacedBlocks.isMarked(e.getLevel(), e.getPos());
        boolean natural = !placed && st.getDestroySpeed(e.getLevel(), e.getPos()) >= 0.5f;
        NATURAL_BREAK.remove(p.getUUID());
        if (!natural) return;
        if (isLog(st)) { if (award(p, UseSkill.WOODCUTTING, 10, null) == Gate.OK) NATURAL_BREAK.put(p.getUUID(), e.getPos().immutable()); }
        else if (isOre(st)) { if (award(p, UseSkill.MINING, 20, null) == Gate.OK) NATURAL_BREAK.put(p.getUUID(), e.getPos().immutable()); }
        else if (isStoneLike(st)) award(p, UseSkill.MINING, 5, null);
    }

    /** Clean up the marker if the tool-levelling handler didn't (it only runs for levelable tools). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakLast(BlockEvent.BreakEvent e) {
        if (!e.isCanceled() && e.getPlayer() != null && !e.getLevel().isClientSide()) PlacedBlocks.consume(e.getLevel(), e.getPos());
    }

    /** Bonus ore / extra log from a natural break. */
    @SubscribeEvent
    public static void onDrops(BlockDropsEvent e) {
        if (!(e.getBreaker() instanceof Player p)) return;
        BlockPos at = NATURAL_BREAK.remove(p.getUUID());
        if (at == null || !at.equals(e.getPos()) || e.getDrops().isEmpty()) return;
        UseSkill s = isLog(e.getState()) ? UseSkill.WOODCUTTING : UseSkill.MINING;
        if (p.getRandom().nextDouble() >= s.secondary(level(p, s))) return;
        ItemEntity first = e.getDrops().get(0);
        ItemStack extra = first.getItem().copyWithCount(1);
        e.getDrops().add(new ItemEntity(e.getLevel(), first.getX(), first.getY(), first.getZ(), extra));
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        BlockState st = e.getState();
        UseSkill s = isLog(st) ? UseSkill.WOODCUTTING : (isOre(st) || isStoneLike(st)) ? UseSkill.MINING : null;
        if (s == null || e.getNewSpeed() <= 0) return;
        e.setNewSpeed(e.getNewSpeed() * (float) (1 + s.primary(level(e.getEntity(), s))));
    }

    // ------------------------------------------------------------------ Combat / Archery / Falconry
    private static final Map<String, long[]> TARGET_BUDGET = new LinkedHashMap<>(256, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, long[]> e) { return size() > 8192; }
    };
    /** pattern: one target can feed one player's combat XP only so many times per window (caged-mob farms stall). */
    static boolean takeTargetBudget(Player p, Entity target) {
        long now = p.level().getGameTime();
        long[] b = TARGET_BUDGET.computeIfAbsent(p.getUUID() + "|" + target.getUUID(), k -> new long[]{0, now});
        if (now - b[1] > TARGET_WINDOW_TICKS) { b[0] = 0; b[1] = now; }
        if (b[0] >= COMBAT_AWARDS_PER_TARGET) return false;
        b[0]++;
        return true;
    }
    public static void testResetBudgets() { TARGET_BUDGET.clear(); }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        var src = e.getSource();
        if (src.getEntity() instanceof Player p && src.getDirectEntity() == p) e.setAmount(e.getAmount() * (float) (1 + UseSkill.COMBAT.primary(level(p, UseSkill.COMBAT))));
        else if (src.getEntity() instanceof Player p && src.getDirectEntity() instanceof AbstractArrow) e.setAmount(e.getAmount() * (float) (1 + UseSkill.ARCHERY.primary(level(p, UseSkill.ARCHERY))));
        else if (src.getEntity() instanceof QuantumFalconEntity f && f.getOwner() instanceof Player p) e.setAmount(e.getAmount() * (float) (1 + UseSkill.FALCONRY.primary(level(p, UseSkill.FALCONRY))));
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide || e.getNewDamage() <= 0 || !(target instanceof Enemy)) return;
        var src = e.getSource();
        float dmg = Math.min(e.getNewDamage(), 20f);
        if (src.getEntity() instanceof Player p && src.getDirectEntity() == p) {
            if (takeTargetBudget(p, target)) award(p, UseSkill.COMBAT, Math.max(1, Math.round(dmg * 3)), "combat");
        } else if (src.getEntity() instanceof Player p && src.getDirectEntity() instanceof AbstractArrow) {
            if (takeTargetBudget(p, target)) award(p, UseSkill.ARCHERY, 4 + Math.round(p.distanceTo(target) / 4f), "archery");
        } else if (src.getEntity() instanceof QuantumFalconEntity f && f.getOwner() instanceof Player p) {
            if (takeTargetBudget(p, target)) award(p, UseSkill.FALCONRY, 6, "falcon:" + f.getUUID());
        }
    }

    @SubscribeEvent
    public static void onCrit(CriticalHitEvent e) {
        Player p = e.getEntity();
        if (p.level().isClientSide || e.isCriticalHit() || !(e.getTarget() instanceof LivingEntity)) return;
        if (p.getRandom().nextDouble() < UseSkill.COMBAT.secondary(level(p, UseSkill.COMBAT))) { e.setCriticalHit(true); e.setDamageMultiplier(1.5f); }
    }

    // ------------------------------------------------------------------ Breeding
    @SubscribeEvent
    public static void onBaby(BabyEntitySpawnEvent e) {
        Player p = e.getCausedByPlayer();
        if (p == null || p.level().isClientSide || e.getParentA() == null || e.getParentB() == null) return;
        creditBreeding(p, e.getParentA(), e.getParentB());
    }

    /** Breeding XP for a bred pair (new pairs only). */
    public static Gate creditBreeding(Player p, Entity pa, Entity pb) {
        String a = pa.getUUID().toString(), b = pb.getUUID().toString();
        String pair = a.compareTo(b) < 0 ? a + "+" + b : b + "+" + a;
        return award(p, UseSkill.BREEDING, pa instanceof QuantumFalconEntity ? 60 : 20, "first:pair." + pair);
    }

    /** Rare-trait chance to add to a nest's mutation input (applied before the breeding pity; the pity cap still rules). */
    public static double mutationBonus(Player owner) { return UseSkill.BREEDING.primary(level(owner, UseSkill.BREEDING)); }

    // ------------------------------------------------------------------ Exploration
    public static void exploreTick(ServerPlayer sp) {
        ServerLevel lvl = sp.serverLevel();
        BlockPos pos = sp.blockPosition();
        lvl.getBiome(pos).unwrapKey().ifPresent(k -> award(sp, UseSkill.EXPLORATION, 80, "first:biome." + k.location()));
        if (!lvl.isLoaded(pos)) return;
        var reg = lvl.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (var en : lvl.structureManager().getAllStructuresAt(pos).entrySet()) {
            var start = lvl.structureManager().getStructureWithPieceAt(pos, en.getKey());
            if (!start.isValid()) continue;
            var id = reg.getKey(en.getKey());
            if (id == null) continue;
            award(sp, UseSkill.EXPLORATION, 150, "first:structure." + id);
            award(sp, UseSkill.EXPLORATION, 30, "first:site." + id + "@" + start.getChunkPos().toLong());
        }
    }

    /** First opening of a loot chest: XP, and the Exploration bonus may add one extra copy of a stack once it's filled. */
    @SubscribeEvent
    public static void onOpen(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.isSpectator()) return;
        if (!(sp.level().getBlockEntity(e.getPos()) instanceof RandomizableContainerBlockEntity c) || c.getLootTable() == null) return;
        BlockPos at = e.getPos().immutable();
        Gate g = award(sp, UseSkill.EXPLORATION, 40, "first:chest." + sp.level().dimension().location() + "@" + at.asLong());
        if (g != Gate.OK) return;
        double chance = UseSkill.EXPLORATION.primary(level(sp, UseSkill.EXPLORATION));
        if (sp.getRandom().nextDouble() >= chance) return;
        // fill the chest now (vanilla would on open; it finds the table already spent and just shows it), then copy.
        // server.execute ran inline on the server thread, before the unpack, so the bonus never fired.
        c.unpackLootTable(sp);
        explorationBonus(c, sp.getRandom());
    }

    /**
     * one extra copy of a stack in a freshly filled chest. Only plain stackable loot is copied: never a falcon egg,
     * a story relic or fused relic, anything bound or carrying custom data (signature loot, eggs, maps), or a one-off item.
     * Returns the copied stack, or EMPTY.
     */
    public static ItemStack explorationBonus(net.minecraft.world.Container cc, net.minecraft.util.RandomSource r) {
        List<Integer> filled = new ArrayList<>(), empty = new ArrayList<>();
        for (int i = 0; i < cc.getContainerSize(); i++) {
            ItemStack st = cc.getItem(i);
            if (st.isEmpty()) empty.add(i); else if (copyable(st)) filled.add(i);
        }
        if (filled.isEmpty() || empty.isEmpty()) return ItemStack.EMPTY;
        ItemStack pick = cc.getItem(filled.get(r.nextInt(filled.size()))).copy();
        cc.setItem(empty.get(r.nextInt(empty.size())), pick);
        testBonuses++;
        return pick;
    }

    /** Test counter: Exploration bonus copies made. */
    public static int testBonuses;

    public static boolean copyable(ItemStack st) {
        if (st.getMaxStackSize() <= 1) return false;   // tools, armour, relics, eggs that don't stack, books
        if (st.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) return false;   // eggs (QFEgg), bound relics, signature rolls
        if (st.is(com.matt.quantumfalcon.registry.ModItems.FALCON_EGG.get())) return false;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
        return !(id.contains("egg") || id.contains("relic") || id.contains("eye") || id.contains("fragment") || id.contains("essence")
            || id.contains("crown") || id.contains("gem") || id.contains("heart") || id.contains("shard_of") || id.contains("jesses") || id.contains("time_bottle"));
    }

    // ------------------------------------------------------------------ Arcana (called from InstabilityEffects each tick)
    public static void riftTick(ServerPlayer sp, float gain, long gameTime) {
        if (gain > 0f && gameTime % 200 == 0) award(sp, UseSkill.ARCANA, 10, "rift");
    }
    /** Multiplier on instability gain (Arcana: up to -20%). */
    public static float instabilityMultiplier(Player p) { return (float) (1 - UseSkill.ARCANA.primary(level(p, UseSkill.ARCANA))); }
}
