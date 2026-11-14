package com.matt.quantumfalcon.loot.sig;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.dungeon.DungeonGuardBlockEntity;
import com.matt.quantumfalcon.entity.boss.EntangledTwinsEntity;
import com.matt.quantumfalcon.entity.boss.QFBoss;
import com.matt.quantumfalcon.registry.ModItems;
import com.matt.quantumfalcon.story.BossCredit;
import com.matt.quantumfalcon.story.PlayerStoryData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * A8: WoW-style signature loot. Every boss and miniboss has its own table of unique named items in five rarity tiers.
 * Each credited player rolls personally (personal loot): a drop chance, then a tier, then an item of that tier biased toward
 * the archetype (skill branch) the player has invested in. Bad-luck protection per player per source: every kill without a
 * drop adds {@link #PITY_STEP} to the chance, and {@link #EPIC_PITY} drops in a row below Epic force an Epic-or-better tier.
 * A boss's first kill always drops. This sits beside the story rewards (relics, first-kill points/star, treasures): nothing
 * here touches the relic chain, and no signature item is a story item.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class SignatureLoot {
    private SignatureLoot() {}

    public static final float PITY_STEP = 0.15f;
    public static final int EPIC_PITY = 8;
    /** Tier weights (Common..Legendary). */
    public static final int[] BOSS_TIERS = {30, 32, 22, 12, 4};
    public static final int[] MINI_TIERS = {45, 32, 16, 6, 1};

    // ------------------------------------------------------------------ catalogue
    public enum Kind { ATTR, DATA }
    public record Bonus(Kind kind, @Nullable Holder<Attribute> attr, AttributeModifier.Operation op, EquipmentSlotGroup slot, String key, double perPower, String text) {
        static Bonus attr(Holder<Attribute> a, AttributeModifier.Operation op, EquipmentSlotGroup slot, double perPower, String text) { return new Bonus(Kind.ATTR, a, op, slot, "", perPower, text); }
        static Bonus data(String key, double perPower, String text) { return new Bonus(Kind.DATA, null, AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.ANY, key, perPower, text); }
        public double amount(LootTier t) { return Math.round(perPower * t.power * 100.0) / 100.0; }
        public String describe(LootTier t) {
            double v = amount(t);
            String n = text.contains("%") ? String.valueOf(Math.round(v * 100)) : (v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v));
            return text.replace("{v}", n);
        }
    }
    /** One archetype item shape: nouns per tier (C/U/R/E), a base item per source gear tier (0..3), its bonuses. */
    public record Template(Archetype arch, String[] nouns, IntFunction<Item> base, List<Bonus> bonuses, boolean bow) {}
    public record Source(String id, String name, String theme, boolean boss, int gear, float chance, String storyBoss, Legend legend) {}
    public record Legend(String name, Archetype arch, int template, String flavour) {}
    public record SigItem(String id, String name, LootTier tier, Archetype arch, Template tpl, Source src, String flavour) {}

    private static final EquipmentSlotGroup MAIN = EquipmentSlotGroup.MAINHAND, HEAD = EquipmentSlotGroup.HEAD, CHEST = EquipmentSlotGroup.CHEST,
            LEGS = EquipmentSlotGroup.LEGS, FEET = EquipmentSlotGroup.FEET, HAND = EquipmentSlotGroup.HAND;
    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE, MUL = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;

    private static Item gear(int t, String piece) {
        String set = switch (t) { case 0 -> "aqua"; case 1 -> "purple"; case 2 -> "magenta"; default -> "gold"; };
        String path = set.equals("gold") && !piece.equals("x") ? "gold_" + piece + "_custom" : set + "_" + piece;
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, path));
        return it == Items.AIR ? Items.IRON_SWORD : it;
    }
    private static Item barding(int t) {
        return switch (t) { case 0 -> ModItems.IRON_FALCON_BARDING.get(); case 1 -> ModItems.DIAMOND_FALCON_BARDING.get(); case 2 -> ModItems.NETHERITE_FALCON_BARDING.get(); default -> ModItems.QUANTUM_FALCON_BARDING.get(); };
    }

    public static final Map<Archetype, List<Template>> TEMPLATES = new EnumMap<>(Archetype.class);
    static {
        TEMPLATES.put(Archetype.HUNT, List.of(
            new Template(Archetype.HUNT, new String[]{"Shiv", "Fang", "Edge", "Reaver"}, t -> gear(t, "sword"),
                List.of(Bonus.attr(Attributes.ATTACK_DAMAGE, ADD, MAIN, 1.0, "+{v} attack damage")), false),
            new Template(Archetype.HUNT, new String[]{"Shortbow", "Longbow", "Warbow", "Skypiercer"}, t -> Items.BOW,
                List.of(Bonus.attr(Attributes.MOVEMENT_SPEED, MUL, MAIN, 0.02, "+{v}% speed while drawn")), true)));
        TEMPLATES.put(Archetype.BOND, List.of(
            new Template(Archetype.BOND, new String[]{"Jesses", "Talon Jesses", "Strikemail", "Raptor Harness"}, SignatureLoot::barding,
                List.of(Bonus.data("falcon_attack", 1.0, "Falcon: +{v} attack damage")), false),
            new Template(Archetype.BOND, new String[]{"Mantle", "Aerie Mantle", "Skyward Mantle", "Stormwing Mantle"}, SignatureLoot::barding,
                List.of(Bonus.data("falcon_health", 4.0, "Falcon: +{v} max health")), false)));
        TEMPLATES.put(Archetype.LINEAGE, List.of(
            new Template(Archetype.LINEAGE, new String[]{"Plume", "Brood Plume", "Bloodline Plume", "Progenitor Plume"}, t -> t >= 2 ? ModItems.GOLDEN_FALCON_FEATHER.get() : ModItems.FALCON_FEATHER.get(),
                List.of(Bonus.data("mutation", 0.02, "Carried: +{v}% rare-trait chance on eggs your falcons lay"),
                        Bonus.attr(Attributes.LUCK, ADD, HAND, 0.5, "+{v} luck in hand")), false),
            new Template(Archetype.LINEAGE, new String[]{"Gloves", "Nestkeeper Gloves", "Hatchwarden Gloves", "Matriarch Gloves"}, t -> Items.LEATHER_CHESTPLATE,
                List.of(Bonus.data("mutation", 0.025, "Worn: +{v}% rare-trait chance on eggs your falcons lay"),
                        Bonus.attr(Attributes.ARMOR, ADD, CHEST, 1.0, "+{v} armour")), false)));
        TEMPLATES.put(Archetype.RIFT, List.of(
            new Template(Archetype.RIFT, new String[]{"Circlet", "Rift Circlet", "Void Diadem", "Singularity Crown"}, t -> gear(t, "helmet"),
                List.of(Bonus.data("instability_resist", 0.06, "Instability builds {v}% slower"),
                        Bonus.attr(Attributes.ARMOR_TOUGHNESS, ADD, HEAD, 0.5, "+{v} armour toughness")), false),
            new Template(Archetype.RIFT, new String[]{"Robe", "Riftweave Robe", "Phase Mantle", "Event-Horizon Mantle"}, t -> gear(t, "chestplate"),
                List.of(Bonus.data("instability_resist", 0.05, "Instability builds {v}% slower"),
                        Bonus.attr(Attributes.MAX_HEALTH, ADD, CHEST, 1.0, "+{v} max health")), false)));
        TEMPLATES.put(Archetype.WAYFINDER, List.of(
            new Template(Archetype.WAYFINDER, new String[]{"Treads", "Striders", "Riftwalkers", "Horizon Striders"}, t -> gear(t, "boots"),
                List.of(Bonus.attr(Attributes.MOVEMENT_SPEED, MUL, FEET, 0.03, "+{v}% movement speed"),
                        Bonus.attr(Attributes.SAFE_FALL_DISTANCE, ADD, FEET, 1.0, "+{v} safe fall distance")), false),
            new Template(Archetype.WAYFINDER, new String[]{"Greaves", "Pathfinder Greaves", "Ridgerunner Greaves", "Skyline Greaves"}, t -> gear(t, "leggings"),
                List.of(Bonus.attr(Attributes.STEP_HEIGHT, ADD, LEGS, 0.15, "+{v} step height"),
                        Bonus.attr(Attributes.MOVEMENT_SPEED, MUL, LEGS, 0.015, "+{v}% movement speed")), false)));
    }

    public static final List<Source> SOURCES = List.of(
        new Source("quantum_overseer", "Quantum Overseer", "Overseer's", true, 0, 0.65f, "QUANTUM_OVERSEER",
            new Legend("Eye of the Unblinking", Archetype.RIFT, 0, "It still watches the Rift for you.")),
        new Source("rift_warden", "Rift Warden", "Wardenbound", true, 1, 0.65f, "RIFT_WARDEN",
            new Legend("Halo of the Fallen Warden", Archetype.BOND, 0, "Your falcon wears the light that once caged it.")),
        new Source("entangled_twins", "Entangled Twins", "Entangled", true, 2, 0.65f, "ENTANGLED_TWINS",
            new Legend("Twinbond Reaver", Archetype.HUNT, 0, "Two blades in one, never more than a thought apart.")),
        new Source("singularity_sovereign", "Singularity Sovereign", "Sovereign", true, 3, 0.65f, "SINGULARITY_SOVEREIGN",
            new Legend("Crownfall Striders", Archetype.WAYFINDER, 0, "Every road bends toward the one who wears them.")),
        new Source("void_lord", "Void Lord", "Voidlord's", true, 2, 0.6f, null,
            new Legend("Hollow-Star Plume", Archetype.LINEAGE, 0, "Eggs laid near it dream of other skies.")),
        new Source("eclipse_phantom", "Eclipse Phantom", "Eclipsed", true, 2, 0.6f, null,
            new Legend("Umbral Skypiercer", Archetype.HUNT, 1, "Its arrows leave no shadow.")),
        new Source("sha_sha", "Sha Sha", "Sha Sha's", true, 1, 0.6f, null,
            new Legend("Sha Sha's Grin", Archetype.RIFT, 1, "Nobody knows whose smile it was.")),
        new Source("skywarden_irel", "Skywarden Irel", "Skywarden's", true, 1, 0.6f, null,   // Irel's loot credit (and the Frozen Hours Codex quest)
            new Legend("First-Light Jesses", Archetype.BOND, 1, "She never let a bird fly before dawn.")),
        new Source("dungeon_warden", "Dungeon Warden", "Vaultkeeper's", false, 1, 0.30f, null,
            new Legend("Vaultbreaker Greaves", Archetype.WAYFINDER, 1, "No lock has kept their wearer out for long.")),
        new Source("dungeon_keybearer", "Dungeon Keybearer", "Keybearer's", false, 0, 0.25f, null,
            new Legend("The Last Key's Jesses", Archetype.BOND, 0, "Forged from the key nobody used.")));

    private static final Map<String, List<SigItem>> ITEMS = new HashMap<>();

    public static Source source(String id) { for (Source s : SOURCES) if (s.id().equals(id)) return s; return null; }

    /** The table: per archetype one item for each of Common, Uncommon, Rare, Epic (two shapes alternating), plus the source's Legendary. */
    public static List<SigItem> items(Source s) {
        return ITEMS.computeIfAbsent(s.id(), k -> {
            List<SigItem> out = new ArrayList<>();
            for (Archetype a : Archetype.values()) {
                List<Template> ts = TEMPLATES.get(a);
                for (LootTier t : new LootTier[]{LootTier.COMMON, LootTier.UNCOMMON, LootTier.RARE, LootTier.EPIC}) {
                    Template tpl = ts.get(t.ordinal() % ts.size());
                    out.add(new SigItem(s.id() + "/" + a.name().toLowerCase(Locale.ROOT) + "/" + t.name().toLowerCase(Locale.ROOT),
                        s.theme() + " " + tpl.nouns()[t.ordinal()], t, a, tpl, s, ""));
                }
            }
            Legend l = s.legend();
            out.add(new SigItem(s.id() + "/legendary", l.name(), LootTier.LEGENDARY, l.arch(), TEMPLATES.get(l.arch()).get(l.template()), s, l.flavour()));
            return List.copyOf(out);
        });
    }

    // ------------------------------------------------------------------ stacks
    /** The item as it drops: base item, tier-coloured name, lore, stat bonuses, signature data, glint for Epic+. */
    public static ItemStack stack(SigItem it, @Nullable RegistryAccess reg) {
        ItemStack st = new ItemStack(it.tpl().base().apply(it.src().gear()));
        st.set(DataComponents.CUSTOM_NAME, Component.literal(it.name()).withStyle(s -> s.withColor(it.tier().format).withItalic(false)));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal(it.tier().label + " " + it.arch().label).withStyle(s -> s.withColor(it.tier().format).withItalic(false)));
        ItemAttributeModifiers mods = st.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        CompoundTag sig = new CompoundTag();
        sig.putString("id", it.id()); sig.putString("tier", it.tier().name()); sig.putString("arch", it.arch().name()); sig.putString("source", it.src().id());
        int n = 0;
        for (Bonus b : it.tpl().bonuses()) {
            double v = b.amount(it.tier());
            if (b.kind() == Kind.ATTR) {
                mods = mods.withModifierAdded(b.attr(), new AttributeModifier(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID,
                    "sig_" + it.id().replace('/', '_') + "_" + n++), v, b.op()), b.slot());
            } else sig.putDouble(b.key(), v);
            lore.add(Component.literal(b.describe(it.tier())).withStyle(s -> s.withColor(ChatFormatting.GRAY).withItalic(false)));
        }
        st.set(DataComponents.ATTRIBUTE_MODIFIERS, mods);
        if (it.tpl().bow() && reg != null) {
            int power = Math.max(1, Math.min(5, Math.round(it.tier().power)));
            reg.lookup(Registries.ENCHANTMENT).flatMap(l -> l.get(Enchantments.POWER)).ifPresent(h -> st.enchant(h, power));
            lore.add(Component.literal("Power " + power).withStyle(s -> s.withColor(ChatFormatting.GRAY).withItalic(false)));
        }
        if (!it.flavour().isEmpty()) lore.add(Component.literal("\"" + it.flavour() + "\"").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        lore.add(Component.literal("Signature drop: " + it.src().name()).withStyle(s -> s.withColor(ChatFormatting.DARK_AQUA).withItalic(false)));
        st.set(DataComponents.LORE, new ItemLore(lore));
        CompoundTag root = new CompoundTag(); root.put("qf_sig", sig);
        st.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        if (it.tier().epicOrBetter()) st.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return st;
    }

    /** Signature data on a stack, or null. */
    @Nullable
    public static CompoundTag sig(ItemStack st) {
        CustomData d = st.get(DataComponents.CUSTOM_DATA);
        if (d == null) return null;
        CompoundTag t = d.copyTag();
        return t.contains("qf_sig") ? t.getCompound("qf_sig") : null;
    }

    /** Sum of a DATA bonus over worn armour (and, for "mutation", anything carried). */
    public static double bonus(Player p, String key) {
        double v = 0;
        for (ItemStack st : p.getArmorSlots()) { CompoundTag s = sig(st); if (s != null) v += s.getDouble(key); }
        if (key.equals("mutation")) {
            for (ItemStack st : p.getInventory().items) { CompoundTag s = sig(st); if (s != null && !isArmour(st)) v += s.getDouble(key); }
            for (ItemStack st : p.getInventory().offhand) { CompoundTag s = sig(st); if (s != null) v += s.getDouble(key); }
        }
        return v;
    }
    private static boolean isArmour(ItemStack st) { return st.getItem() instanceof net.minecraft.world.item.ArmorItem; }

    // ------------------------------------------------------------------ ledger (per player, survives death)
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, QuantumFalcon.MOD_ID);
    public static final Supplier<AttachmentType<CompoundTag>> LEDGER = ATTACHMENTS.register("signature_loot_ledger",
        () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC).copyOnDeath().build());
    public static void register(IEventBus bus) { ATTACHMENTS.register(bus); }

    public static final class Ledger {
        final CompoundTag tag;
        public Ledger(CompoundTag tag) { this.tag = tag; }
        public int misses(String src) { return tag.getInt("miss." + src); }
        public int dry(String src) { return tag.getInt("dry." + src); }
        public int kills(String src) { return tag.getInt("kills." + src); }
        public int drops(String src) { return tag.getInt("drops." + src); }
        void set(String k, int v) { tag.putInt(k, v); }
    }
    public static Ledger ledger(Player p) {
        CompoundTag t = p.getData(LEDGER);
        return new Ledger(t);
    }
    private static void save(Player p, Ledger l) { p.setData(LEDGER, l.tag); }

    // ------------------------------------------------------------------ rolls (pure; tests drive them with a seeded RandomSource)
    public static float dropChance(Source s, int misses) { return Math.min(1f, s.chance() + PITY_STEP * misses); }

    public static LootTier rollTier(Source s, RandomSource r, int dry) {
        int[] w = s.boss() ? BOSS_TIERS : MINI_TIERS;
        int from = dry >= EPIC_PITY ? LootTier.EPIC.ordinal() : 0;
        int total = 0; for (int i = from; i < w.length; i++) total += w[i];
        int x = r.nextInt(total);
        for (int i = from; i < w.length; i++) { x -= w[i]; if (x < 0) return LootTier.values()[i]; }
        return LootTier.COMMON;
    }

    public static SigItem pick(Source s, LootTier t, Map<Archetype, Float> weights, RandomSource r) {
        List<SigItem> pool = new ArrayList<>();
        for (int ti = t.ordinal(); ti >= 0 && pool.isEmpty(); ti--)
            for (SigItem it : items(s)) if (it.tier().ordinal() == ti) pool.add(it);
        float total = 0; for (SigItem it : pool) total += weights.getOrDefault(it.arch(), 1f);
        float x = r.nextFloat() * total;
        for (SigItem it : pool) { x -= weights.getOrDefault(it.arch(), 1f); if (x <= 0) return it; }
        return pool.get(pool.size() - 1);
    }

    /** One player's roll for one kill; updates the ledger. Empty = no drop this time. */
    public static Optional<SigItem> roll(Source s, Ledger l, boolean guaranteed, Map<Archetype, Float> weights, RandomSource r) {
        String id = s.id();
        l.set("kills." + id, l.kills(id) + 1);
        boolean hit = guaranteed || r.nextFloat() < dropChance(s, l.misses(id));
        if (!hit) { l.set("miss." + id, l.misses(id) + 1); return Optional.empty(); }
        l.set("miss." + id, 0);
        LootTier t = rollTier(s, r, l.dry(id));
        l.set("dry." + id, t.epicOrBetter() ? 0 : l.dry(id) + 1);
        l.set("drops." + id, l.drops(id) + 1);
        return Optional.of(pick(s, t, weights, r));
    }

    // ------------------------------------------------------------------ kills
    /** Test hook: a fixed RandomSource for kill rolls (null = the level's). */
    public static RandomSource TEST_RANDOM = null;

    @Nullable
    public static Source sourceFor(LivingEntity e) {
        if (e instanceof QFBoss) return source(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath());
        if (e.getTags().contains(DungeonGuardBlockEntity.TAG_BOSS)) return source("dungeon_warden");
        if (e.getTags().contains(DungeonGuardBlockEntity.TAG_KEY)) return source("dungeon_keybearer");
        return null;
    }

    /** HIGH: runs before BossDeathHandler consumes the credit ledger and records first kills. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity mob = e.getEntity();
        if (!(mob.level() instanceof ServerLevel sl)) return;
        Source s = sourceFor(mob);
        if (s == null) return;
        if (mob instanceof EntangledTwinsEntity tw && tw.getTwin() != null && tw.getTwin().isAlive()) return;   // the pair drops once, on the second death
        List<ServerPlayer> credited;
        if (mob instanceof QFBoss) credited = BossCredit.peekCredited(mob, e.getSource(), sl);
        else {
            credited = new ArrayList<>();
            UUID k = BossCredit.creditFor(e.getSource());
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(24))) if (!p.isSpectator()) credited.add(p);
            if (k != null) {
                ServerPlayer kp = sl.getServer().getPlayerList().getPlayer(k);
                if (kp == null) kp = BossCredit.TEST_PLAYERS.get(k);
                if (kp != null && !credited.contains(kp)) credited.add(kp);
            }
        }
        for (ServerPlayer p : credited) award(p, s, mob);
    }

    /** One credited player's personal roll and delivery. Public for tests. */
    public static Optional<SigItem> award(ServerPlayer p, Source s, @Nullable LivingEntity mob) {
        com.matt.quantumfalcon.codex.RiftCodex.onObjective(p, "SIG_KILL", s.id(), 1, false);   // A1 Codex: a credited kill of any boss/miniboss
        Ledger l = ledger(p);
        boolean first = s.storyBoss() != null ? !PlayerStoryData.get(p).hasDefeatedBoss(s.storyBoss()) : l.kills(s.id()) == 0;
        boolean guaranteed = first && s.boss();
        RandomSource r = TEST_RANDOM != null ? TEST_RANDOM : p.getRandom();
        Optional<SigItem> got = roll(s, l, guaranteed, Archetype.weights(Archetype.invested(p)), r);
        save(p, l);
        got.ifPresent(it -> {
            ItemStack st = stack(it, p.level().registryAccess());
            Component name = st.getDisplayName();
            if (!p.getInventory().add(st)) p.drop(st, false);
            p.displayClientMessage(Component.literal(s.name() + " dropped for you: ").withStyle(ChatFormatting.GRAY).append(name), false);
            com.matt.quantumfalcon.codex.RiftCodex.onObjective(p, "SIG_LOOT", it.tier().name().toLowerCase(java.util.Locale.ROOT), 1, false);   // A1 Codex
            com.matt.quantumfalcon.codex.RiftCodex.onObjective(p, "SIG_LOOT", "any", 1, false);
        });
        if (got.isEmpty()) p.displayClientMessage(Component.literal("No signature drop from " + s.name() + " this time (next kill: "
            + Math.round(dropChance(s, l.misses(s.id())) * 100) + "%).").withStyle(ChatFormatting.DARK_GRAY), false);
        return got;
    }
}
