package com.matt.quantumfalcon.test;

import com.matt.quantumfalcon.dungeon.DungeonGuardBlockEntity;
import com.matt.quantumfalcon.loot.sig.Archetype;
import com.matt.quantumfalcon.loot.sig.LootTier;
import com.matt.quantumfalcon.loot.sig.SignatureLoot;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.story.BossCredit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.*;
import java.util.function.Consumer;

import static com.matt.quantumfalcon.test.QFTestCommands.*;

/** A8: roll fairness, archetype bias, per-player rolls on a real kill, pity, miniboss tables, item stats and the story chain. */
public final class SignatureLootTests {
    private SignatureLootTests() {}

    static void register(Map<String, Consumer<ServerLevel>> tests) {
        tests.put("a8_roll_fairness", SignatureLootTests::fairness);
        tests.put("a8_archetype_bias", SignatureLootTests::bias);
        tests.put("a8_pity", SignatureLootTests::pity);
        tests.put("a8_per_player_real_kill", SignatureLootTests::perPlayer);
        tests.put("a8_miniboss_drop", SignatureLootTests::miniboss);
        tests.put("a8_tables_and_story_chain", SignatureLootTests::tables);
        tests.put("a8_item_bonuses_work", SignatureLootTests::bonuses);
    }

    /** nextFloat pinned (0.999 = always miss a chance roll, 0.0 = always hit); ints still random. */
    static RandomSource pinned(float f) {
        return new LegacyRandomSource(7) { @Override public float nextFloat() { return f; } };
    }

    private static void fairness(ServerLevel level) {
        RandomSource r = RandomSource.create(42);
        StringBuilder d = new StringBuilder(); boolean ok = true;
        for (boolean boss : new boolean[]{true, false}) {
            var src = SignatureLoot.source(boss ? "quantum_overseer" : "dungeon_warden");
            int[] w = boss ? SignatureLoot.BOSS_TIERS : SignatureLoot.MINI_TIERS;
            int tot = Arrays.stream(w).sum(), n = 50000;
            int[] got = new int[5];
            for (int i = 0; i < n; i++) got[SignatureLoot.rollTier(src, r, 0).ordinal()]++;
            d.append(boss ? "boss" : "miniboss").append(':');
            for (int i = 0; i < 5; i++) {
                double exp = w[i] / (double) tot, obs = got[i] / (double) n;
                ok &= Math.abs(exp - obs) < 0.01;
                d.append(' ').append(LootTier.values()[i].label).append(' ').append(String.format(Locale.ROOT, "%.3f/%.3f", obs, exp));
            }
            // drop chance itself: 20000 kills at the base chance with misses never counted (fresh ledger each time)
            int hits = 0;
            for (int i = 0; i < 20000; i++) if (SignatureLoot.roll(src, new SignatureLoot.Ledger(new CompoundTag()), false, Archetype.weights(Map.of()), r).isPresent()) hits++;
            double obs = hits / 20000.0;
            ok &= Math.abs(obs - src.chance()) < 0.015;
            d.append(String.format(Locale.ROOT, " | drop %.3f/%.2f; ", obs, src.chance()));
        }
        result("a8_roll_fairness", ok, d.toString());
    }

    private static void bias(ServerLevel level) {
        RandomSource r = RandomSource.create(9);
        var src = SignatureLoot.source("rift_warden");
        Map<Archetype, Integer> inv = new EnumMap<>(Archetype.class);
        inv.put(Archetype.HUNT, 12);
        var w = Archetype.weights(inv);
        Map<Archetype, Integer> got = new EnumMap<>(Archetype.class);
        int n = 40000;
        for (int i = 0; i < n; i++) got.merge(SignatureLoot.pick(src, LootTier.RARE, w, r).arch(), 1, Integer::sum);
        double hunt = got.getOrDefault(Archetype.HUNT, 0) / (double) n;
        double minOther = 1;
        for (Archetype a : Archetype.values()) if (a != Archetype.HUNT) minOther = Math.min(minOther, got.getOrDefault(a, 0) / (double) n);
        // nothing invested: uniform
        Map<Archetype, Integer> flat = new EnumMap<>(Archetype.class);
        for (int i = 0; i < n; i++) flat.merge(SignatureLoot.pick(src, LootTier.RARE, Archetype.weights(Map.of()), r).arch(), 1, Integer::sum);
        double flatMax = 0, flatMin = 1;
        for (Archetype a : Archetype.values()) { double f = flat.getOrDefault(a, 0) / (double) n; flatMax = Math.max(flatMax, f); flatMin = Math.min(flatMin, f); }
        boolean ok = Math.abs(hunt - 0.5) < 0.02 && minOther > 0.10 && flatMax - flatMin < 0.02;
        result("a8_archetype_bias", ok, String.format(Locale.ROOT, "all points in Hunt: Hunt share %.3f (expect 0.500), lowest other %.3f (expect 0.125) | no points: shares %.3f-%.3f (expect 0.200) counts=%s", hunt, minOther, flatMin, flatMax, got));
    }

    private static void pity(ServerLevel level) {
        var src = SignatureLoot.source("void_lord");
        var l = new SignatureLoot.Ledger(new CompoundTag());
        var w = Archetype.weights(Map.of());
        RandomSource unlucky = pinned(0.999f);
        StringBuilder d = new StringBuilder("chances:");
        int kill = 0; boolean dropped = false;
        while (!dropped && kill < 20) {
            d.append(' ').append(Math.round(SignatureLoot.dropChance(src, l.misses(src.id())) * 100));
            dropped = SignatureLoot.roll(src, l, false, w, unlucky).isPresent();
            kill++;
        }
        int expectKills = (int) Math.ceil((1f - src.chance()) / SignatureLoot.PITY_STEP - 1e-6) + 1;
        boolean reset = l.misses(src.id()) == 0;
        // Epic pity: over 3000 drops no run of more than EPIC_PITY non-Epic drops, and dry=EPIC_PITY always gives Epic+
        RandomSource r = RandomSource.create(5);
        var l2 = new SignatureLoot.Ledger(new CompoundTag());
        int run = 0, worst = 0;
        for (int i = 0; i < 3000; i++) {
            var it = SignatureLoot.roll(src, l2, true, w, r).orElseThrow();
            run = it.tier().epicOrBetter() ? 0 : run + 1; worst = Math.max(worst, run);
        }
        boolean forced = true;
        for (int i = 0; i < 500; i++) forced &= SignatureLoot.rollTier(src, r, SignatureLoot.EPIC_PITY).epicOrBetter();
        boolean ok = dropped && kill == expectKills && reset && worst <= SignatureLoot.EPIC_PITY && forced;
        result("a8_pity", ok, d + " | always-unlucky player dropped on kill " + kill + " (expect " + expectKills + "), miss counter reset=" + reset
            + " | longest run below Epic in 3000 drops=" + worst + " (cap " + SignatureLoot.EPIC_PITY + "), dry counter at cap forces Epic+=" + forced);
    }

    private static List<ItemStack> sigItems(FakePlayer fp, String source) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack st : fp.getInventory().items) { var s = SignatureLoot.sig(st); if (s != null && s.getString("source").equals(source)) out.add(st); }
        return out;
    }

    private static void perPlayer(ServerLevel level) {
        BlockPos p = pad(level, 32000, 32000 + DungeonTests.RUN_OFF, Blocks.STONE);
        WorldDropsTests.force(level, p, true);
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            FakePlayer a = AuditTests.freshPlayer(level, "QFLooterA"), b = AuditTests.freshPlayer(level, "QFLooterB"), c = AuditTests.freshPlayer(level, "QFBystander");
            for (FakePlayer f : List.of(a, b, c)) { BossCredit.TEST_PLAYERS.put(f.getUUID(), f); f.moveTo(p.getX() + 40, p.getY(), p.getZ()); }
            var boss = (LivingEntity) com.matt.quantumfalcon.registry.ModEntities.VOID_LORD.get().create(level);
            boss.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            level.addFreshEntity(boss);
            BossCredit.testContribute(boss, a.getUUID());
            BossCredit.testContribute(boss, b.getUUID());
            boss.invulnerableTime = 0;
            boss.kill();
            later(5, () -> {
                var ga = sigItems(a, "void_lord"); var gb = sigItems(b, "void_lord"); var gc = sigItems(c, "void_lord");
                var la = SignatureLoot.ledger(a); var lb = SignatureLoot.ledger(b);
                // second kill: rolls again per player, independently (kills counted per player)
                boolean ok = boss.isDeadOrDying() && ga.size() == 1 && gb.size() == 1 && gc.isEmpty() && ga.get(0) != gb.get(0)
                    && la.kills("void_lord") == 1 && lb.kills("void_lord") == 1 && SignatureLoot.ledger(c).kills("void_lord") == 0;
                result("a8_per_player_real_kill", ok, "boss dead=" + boss.isDeadOrDying() + " A got " + names(ga) + " B got " + names(gb) + " bystander got " + names(gc)
                    + " kills A/B/bystander=" + la.kills("void_lord") + "/" + lb.kills("void_lord") + "/" + SignatureLoot.ledger(c).kills("void_lord") + " (first kill always drops)");
                for (FakePlayer f : List.of(a, b, c)) BossCredit.TEST_PLAYERS.remove(f.getUUID());
                WorldDropsTests.force(level, p, false);
            });
        });
    }

    static String names(List<ItemStack> l) { List<String> n = new ArrayList<>(); for (ItemStack s : l) n.add(s.getHoverName().getString() + "[" + SignatureLoot.sig(s).getString("tier") + "]"); return n.toString(); }

    private static void miniboss(ServerLevel level) {
        BlockPos p = pad(level, 32100, 32000 + DungeonTests.RUN_OFF, Blocks.STONE);
        WorldDropsTests.force(level, p, true);
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            FakePlayer k = AuditTests.freshPlayer(level, "QFKeyHunter");
            BossCredit.TEST_PLAYERS.put(k.getUUID(), k);
            k.moveTo(p.getX() + 1.5, p.getY(), p.getZ() + 0.5);
            var elite = EntityType.ZOMBIE.create(level);
            elite.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            elite.addTag(DungeonGuardBlockEntity.TAG_KEY); elite.addTag(DungeonGuardBlockEntity.TAG_REALM + "riftslate");
            level.addFreshEntity(elite);
            SignatureLoot.TEST_RANDOM = pinned(0.0f);   // the chance roll hits; tier and item still roll
            elite.hurt(level.damageSources().playerAttack(k), 500f);
            SignatureLoot.TEST_RANDOM = null;
            var got = sigItems(k, "dungeon_keybearer");
            boolean ok = elite.isDeadOrDying() && got.size() == 1;
            result("a8_miniboss_drop", ok, "keybearer dead=" + elite.isDeadOrDying() + " killer got " + names(got) + " (miniboss base chance " + SignatureLoot.source("dungeon_keybearer").chance() + ")");
            BossCredit.TEST_PLAYERS.remove(k.getUUID());
            WorldDropsTests.force(level, p, false);
        });
    }

    private static void tables(ServerLevel level) {
        Set<Item> story = Set.of(ModItems.OVERSEERS_EYE.get(), ModItems.WARDENS_FRAGMENT.get(), ModItems.TWIN_ESSENCE.get(), ModItems.SOVEREIGNS_CROWN.get(),
            ModItems.HEART_OF_QUANTUM.get(), ModItems.REALITY_GEM.get(), ModItems.TRANSCENDENCE_SHARD.get(), ModItems.ARTIFACT_FRAGMENT.get());
        Set<String> names = new HashSet<>();
        StringBuilder d = new StringBuilder(); boolean ok = SignatureLoot.SOURCES.size() == 10;
        int total = 0;
        for (var s : SignatureLoot.SOURCES) {
            var items = SignatureLoot.items(s);
            EnumSet<LootTier> tiers = EnumSet.noneOf(LootTier.class); EnumSet<Archetype> archs = EnumSet.noneOf(Archetype.class);
            for (var it : items) {
                tiers.add(it.tier()); archs.add(it.arch());
                ok &= names.add(it.name());   // every signature item has its own name
                ItemStack st = SignatureLoot.stack(it, level.registryAccess());
                ok &= !story.contains(st.getItem()) && SignatureLoot.sig(st) != null;
                total++;
            }
            ok &= tiers.size() == 5 && archs.size() == 5;
            d.append(s.id()).append('=').append(items.size()).append(' ');
        }
        result("a8_tables_and_story_chain", ok, "sources=" + SignatureLoot.SOURCES.size() + " items=" + total + " unique names=" + names.size() + " [" + d.toString().trim()
            + "] no story relic/bound item in any table; relic gifts stay in BossDeathHandler (r3_2/r3_4/progression)");
    }

    private static void bonuses(ServerLevel level) {
        BlockPos p = pad(level, 32200, 32000 + DungeonTests.RUN_OFF, Blocks.GRASS_BLOCK);
        WorldDropsTests.force(level, p, true);
        WorldDropsTests.whenTicking(level, p, 0, () -> {
            var src = SignatureLoot.source("rift_warden");
            SignatureLoot.SigItem jesses = null, circlet = null, sword = null, plume = null;
            for (var it : SignatureLoot.items(src)) {
                if (it.tier() != LootTier.EPIC) continue;
                if (it.arch() == Archetype.BOND && it.tpl().bonuses().get(0).key().equals("falcon_attack")) jesses = it;
                if (it.arch() == Archetype.RIFT) circlet = it;
                if (it.arch() == Archetype.HUNT && !it.tpl().bow()) sword = it;
                if (it.arch() == Archetype.LINEAGE && !(SignatureLoot.stack(it, level.registryAccess()).getItem() instanceof net.minecraft.world.item.ArmorItem)) plume = it;
            }
            for (var it : SignatureLoot.items(src)) if (jesses == null && it.arch() == Archetype.BOND && it.tpl().bonuses().get(0).key().equals("falcon_attack")) jesses = it;
            for (var it : SignatureLoot.items(src)) if (plume == null && it.arch() == Archetype.LINEAGE && !(SignatureLoot.stack(it, level.registryAccess()).getItem() instanceof net.minecraft.world.item.ArmorItem)) plume = it;
            for (var it : SignatureLoot.items(src)) if (sword == null && it.arch() == Archetype.HUNT && !it.tpl().bow()) sword = it;
            var falcon = com.matt.quantumfalcon.registry.ModEntities.QUANTUM_FALCON.get().create(level);
            falcon.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            level.addFreshEntity(falcon);
            double atk0 = falcon.getAttributeValue(Attributes.ATTACK_DAMAGE);
            falcon.setBodyArmorItem(SignatureLoot.stack(jesses, level.registryAccess()));
            falcon.updateBardingAttributes();
            double atk1 = falcon.getAttributeValue(Attributes.ATTACK_DAMAGE);
            FakePlayer fp = AuditTests.freshPlayer(level, "QFGearTester");
            fp.setItemSlot(EquipmentSlot.HEAD, SignatureLoot.stack(circlet, level.registryAccess()));
            double resist = SignatureLoot.bonus(fp, "instability_resist");
            ItemStack sw = SignatureLoot.stack(sword, level.registryAccess());
            ItemStack plain = new ItemStack(sw.getItem());
            double[] dmg = {0, 0};
            sw.forEachModifier(EquipmentSlot.MAINHAND, (a, m) -> { if (a.equals(Attributes.ATTACK_DAMAGE)) dmg[0] += m.amount(); });
            plain.forEachModifier(EquipmentSlot.MAINHAND, (a, m) -> { if (a.equals(Attributes.ATTACK_DAMAGE)) dmg[1] += m.amount(); });
            fp.getInventory().add(SignatureLoot.stack(plume, level.registryAccess()));
            double mut = SignatureLoot.bonus(fp, "mutation");
            falcon.discard();
            boolean ok = atk1 > atk0 && resist > 0 && dmg[0] > dmg[1] && dmg[1] > 0 && mut > 0;
            result("a8_item_bonuses_work", ok, "falcon attack " + atk0 + " -> " + atk1 + " with " + jesses.name() + " | " + circlet.name() + " instability resist " + resist
                + " | " + sword.name() + " damage modifiers " + dmg[0] + " vs plain " + dmg[1] + " (base kept) | carried " + plume.name() + " rare-trait bonus " + mut);
            WorldDropsTests.force(level, p, false);
        });
    }
}
