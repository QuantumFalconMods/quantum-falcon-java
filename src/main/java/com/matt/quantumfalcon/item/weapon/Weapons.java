package com.matt.quantumfalcon.item.weapon;

import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.entity.FalconCommand;
import com.matt.quantumfalcon.entity.ability.FalconAbilities;
import com.matt.quantumfalcon.registry.ModFamilyItems.FamilyTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Quantum Falcon weapons. Quantum Blade, Reality Splitter and Void Reaper follow Matthew's recovered designs;
 * the staffs, spear, hammer, daggers and Falconer's Gauntlet specials were designed for this pass (art concepts were his).
 */
public final class Weapons {
    private Weapons() {}

    static Item.Properties props(Rarity r) { return new Item.Properties().rarity(r); }

    // ------------------------------------------------------------ Matthew's designs
    /** Right-click: Quantum Blink (8 blocks). Hits: 20% chance to bypass armour (+4 true damage) and Weakness. */
    public static class QuantumBlade extends AbilitySwordItem {
        public QuantumBlade() { super("quantum_blade", FamilyTier.QUANTUM, props(Rarity.RARE).attributes(SwordItem.createAttributes(FamilyTier.QUANTUM, 4, -2.2f)), 60, 3); }
        @Override public boolean special(Level level, Player p, ItemStack s) {
            boolean ok = WeaponFx.blink(p, 8);
            if (ok) { WeaponFx.sound(p, com.matt.quantumfalcon.registry.ModSounds.WEAPON_BLINK, 0.9f, 1.0f); WeaponFx.glints(p, p.position().add(0, 1, 0), 0x50F0FF, 24, 0.5); WeaponFx.shock(p, p.position(), 0x50F0FF, 1.2); }
            return ok;
        }
        @Override public boolean hurtEnemy(ItemStack s, LivingEntity t, LivingEntity a) {
            if (!a.level().isClientSide && a.getRandom().nextFloat() < 0.2f) {
                WeaponFx.trueHit(a, t, 4f); WeaponFx.effect(t, MobEffects.WEAKNESS, 60, 0);
                WeaponFx.burst(a, t.position().add(0, 1, 0), ParticleTypes.END_ROD, 12, 0.3);
            }
            return super.hurtEnemy(s, t, a);
        }
    }

    /** Right-click: Reality Slash, a 10-block arc in front. Hits: +3 true damage (reality cut); 10% Dimensional Banishment. */
    public static class RealitySplitter extends AbilitySwordItem {
        public RealitySplitter() { super("reality_splitter", FamilyTier.TEMPORAL, props(Rarity.EPIC).attributes(SwordItem.createAttributes(FamilyTier.TEMPORAL, 6, -2.6f)), 100, 4); }
        @Override public boolean special(Level level, Player p, ItemStack s) {
            Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
            int n = 0;
            for (LivingEntity t : WeaponFx.targets(p, p.position(), 10)) {
                Vec3 to = t.position().subtract(p.position()).multiply(1, 0, 1);
                if (to.lengthSqr() < 1e-3 || to.normalize().dot(look) < 0.5) continue;   // ~120 degree arc
                if (WeaponFx.hit(p, t, 10f)) n++;
            }
            for (int i = -6; i <= 6; i++) {
                Vec3 dir = look.yRot((float) Math.toRadians(i * 10));
                WeaponFx.line(p, p.position().add(0, 1, 0).add(dir.scale(1.5)), p.position().add(0, 1, 0).add(dir.scale(10)), ParticleTypes.WITCH);
            }
            for (int i = -2; i <= 2; i++) WeaponFx.trail(p, p.position().add(0, 1.1, 0).add(look.yRot((float) Math.toRadians(i * 24)).scale(2)), p.position().add(0, 1.1, 0).add(look.yRot((float) Math.toRadians(i * 24)).scale(9)), 0xFF60E0);
            WeaponFx.slash(p, 0xFF60E0, 14);
            WeaponFx.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f); WeaponFx.sound(p, com.matt.quantumfalcon.registry.ModSounds.WEAPON_RIFT_SLASH, 1.0f, 0.9f);
            return true;   // the slash always happens
        }
        @Override public boolean hurtEnemy(ItemStack s, LivingEntity t, LivingEntity a) {
            if (!a.level().isClientSide) {
                WeaponFx.trueHit(a, t, 3f);
                if (a.getRandom().nextFloat() < 0.10f) banish(a, t);
            }
            return super.hurtEnemy(s, t, a);
        }
        public static boolean banish(LivingEntity a, LivingEntity t) {
            for (int tries = 0; tries < 16; tries++) {
                double ang = a.getRandom().nextDouble() * Math.PI * 2, d = 16 + a.getRandom().nextDouble() * 16;
                double x = t.getX() + Math.cos(ang) * d, z = t.getZ() + Math.sin(ang) * d;
                int y = t.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
                if (y <= t.level().getMinBuildHeight()) continue;
                WeaponFx.burst(a, t.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 40, 0.5);
                t.teleportTo(x, y, z);
                WeaponFx.effect(t, MobEffects.CONFUSION, 200, 2); WeaponFx.effect(t, MobEffects.WEAKNESS, 100, 2);
                if (a instanceof Player p) p.displayClientMessage(Component.translatable("item.quantumfalcon.reality_splitter.banish").withStyle(ChatFormatting.DARK_PURPLE), true);
                return true;
            }
            return false;
        }
    }

    /** Hits: 30% lifesteal, 40% Wither II (3s). Right-click: Void Pulse (6 blocks). */
    public static class VoidReaper extends AbilitySwordItem {
        public VoidReaper() { super("void_reaper", FamilyTier.VOID, props(Rarity.EPIC).attributes(SwordItem.createAttributes(FamilyTier.VOID, 7, -3.0f)), 160, 4); }
        @Override public boolean special(Level level, Player p, ItemStack s) {
            for (LivingEntity t : WeaponFx.targets(p, p.position(), 6)) {
                WeaponFx.hit(p, t, 8f); WeaponFx.effect(t, MobEffects.WITHER, 100, 1);
                Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-3) { away = away.normalize(); t.push(away.x * 0.8, 0.3, away.z * 0.8); t.hurtMarked = true; }
            }
            WeaponFx.ring(p, p.position(), 3, ParticleTypes.SQUID_INK); WeaponFx.ring(p, p.position(), 6, ParticleTypes.PORTAL);
            WeaponFx.shock(p, p.position(), 0xB05CFF, 3); WeaponFx.runeCircle(p, p.position(), 0xB05CFF, 2.5);
            WeaponFx.sound(p, SoundEvents.WARDEN_SONIC_BOOM, 0.4f, 1.6f); WeaponFx.sound(p, com.matt.quantumfalcon.registry.ModSounds.WEAPON_VOID_REAP, 1.0f, 1.0f);
            return true;
        }
        @Override public boolean hurtEnemy(ItemStack s, LivingEntity t, LivingEntity a) {
            if (!a.level().isClientSide) {
                a.heal((float) a.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.3f);
                if (a.getRandom().nextFloat() < 0.4f) WeaponFx.effect(t, MobEffects.WITHER, 60, 1);
            }
            return super.hurtEnemy(s, t, a);
        }
    }

    // ------------------------------------------------------------ new weapons (Matthew's art concepts, effects designed here)
    /** Right-click: Time Stop — hostiles within 8 get Slowness V + Weakness II (5s); you get Haste II. */
    public static class TemporalStaff extends AbilitySwordItem {
        public TemporalStaff() { super("temporal_staff", FamilyTier.TEMPORAL, props(Rarity.EPIC).attributes(SwordItem.createAttributes(FamilyTier.TEMPORAL, 1, -2.8f)), 400, 3); }
        @Override public boolean special(Level level, Player p, ItemStack s) {
            int n = 0;
            for (LivingEntity t : WeaponFx.targets(p, p.position(), 8)) {
                if (!(t instanceof net.minecraft.world.entity.monster.Enemy)) continue;
                WeaponFx.effect(t, MobEffects.MOVEMENT_SLOWDOWN, 100, 4); WeaponFx.effect(t, MobEffects.WEAKNESS, 100, 1);
                t.setDeltaMovement(Vec3.ZERO); t.hurtMarked = true; n++;
            }
            WeaponFx.effect(p, MobEffects.DIG_SPEED, 200, 1);
            WeaponFx.ring(p, p.position(), 4, new DustParticleOptions(new Vector3f(1f, 0.75f, 0.3f), 1.5f));
            WeaponFx.ring(p, p.position(), 8, new DustParticleOptions(new Vector3f(1f, 0.75f, 0.3f), 1.5f));
            WeaponFx.runeCircle(p, p.position(), 0xFFD36A, 3); WeaponFx.runeCircle(p, p.position(), 0xFFD36A, 6); WeaponFx.shock(p, p.position(), 0xFFD36A, 2);
            WeaponFx.sound(p, SoundEvents.BELL_RESONATE, 0.7f, 1.5f); WeaponFx.sound(p, com.matt.quantumfalcon.registry.ModSounds.WEAPON_TEMPORAL_TICK, 1.0f, 1.0f);
            return true;
        }
    }

    /** Right-click: Quantum Bolt — instant 24-block beam, 8 damage to the first creature hit. */
    public static class QuantumStaff extends AbilitySwordItem {
        public QuantumStaff() { super("quantum_staff", FamilyTier.QUANTUM, props(Rarity.RARE).attributes(SwordItem.createAttributes(FamilyTier.QUANTUM, 1, -2.8f)), 30, 2); }
        @Override public boolean special(Level level, Player p, ItemStack s) {
            EntityHitResult hit = WeaponFx.lookTarget(p, 24);
            Vec3 end = hit != null ? hit.getLocation() : WeaponFx.lookEnd(p, 24);
            WeaponFx.line(p, p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0, -0.2, 0), end, new DustParticleOptions(new Vector3f(0.35f, 0.95f, 1f), 1.0f));
            if (hit != null && hit.getEntity() instanceof LivingEntity t) {
                WeaponFx.trueHit(p, t, 8f);
                WeaponFx.burst(p, end, ParticleTypes.END_ROD, 15, 0.3);
            }
            WeaponFx.trail(p, p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0, -0.2, 0), end, 0x43E6FF);
            if (hit != null) WeaponFx.shock(p, end.add(0, -0.8, 0), 0x43E6FF, 0.8);
            WeaponFx.sound(p, com.matt.quantumfalcon.registry.ModSounds.WEAPON_STAFF_CAST, 1f, 1.1f);
            return true;
        }
    }

    /** Longer reach (+1.5). Right-click: Rift Lunge — dash 6 blocks forward, 6 damage to everything passed. */
    public static class RiftSpear extends AbilitySwordItem {
        static final ResourceLocation REACH = ResourceLocation.fromNamespaceAndPath("quantumfalcon", "rift_spear_reach");
        public RiftSpear() {
            super("rift_spear", FamilyTier.PHASE, props(Rarity.RARE).attributes(SwordItem.createAttributes(FamilyTier.PHASE, 4, -2.9f)
                    .withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH, 1.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)), 120, 3);
        }
        @Override public boolean special(Level level, Player p, ItemStack s) {
            Vec3 start = p.position(), look = p.getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 end = start;
            for (double d = 0.5; d <= 6; d += 0.5) {
                Vec3 q = start.add(look.scale(d));
                if (!level.noCollision(p, p.getDimensions(p.getPose()).makeBoundingBox(q))) break;
                end = q;
            }
            if (end.distanceTo(start) < 1) return false;
            final Vec3 fEnd = end;
            for (LivingEntity t : WeaponFx.targets(p, start.lerp(end, 0.5), end.distanceTo(start) / 2 + 1.5)) {
                Vec3 rel = t.position().subtract(start);
                double along = rel.dot(look);
                if (along < -0.5 || along > fEnd.distanceTo(start) + 1 || rel.subtract(look.scale(along)).length() > 1.8) continue;
                WeaponFx.hit(p, t, 6f);
            }
            WeaponFx.line(p, start.add(0, 1, 0), end.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL);
            p.teleportTo(end.x, end.y, end.z); p.resetFallDistance();
            WeaponFx.trail(p, start.add(0, 1, 0), end.add(0, 1, 0), 0xC080FF); WeaponFx.glints(p, end.add(0, 1, 0), 0xC080FF, 16, 0.4);
            WeaponFx.sound(p, SoundEvents.TRIDENT_RIPTIDE_1.value(), 0.8f, 1.3f); WeaponFx.sound(p, com.matt.quantumfalcon.registry.ModSounds.WEAPON_BLINK, 0.6f, 0.7f);
            return true;
        }
    }

    /** One tier above the Rift Spear: Chaos-tier stats, +2 reach. Right-click: Quantum Lunge — dash up to 8 blocks,
     *  8 damage to everything passed and a brief Slowness on each, with a cyan spiral wound around the path. */
    public static class QuantumSpear extends AbilitySwordItem {
        static final ResourceLocation REACH = ResourceLocation.fromNamespaceAndPath("quantumfalcon", "quantum_spear_reach");
        public QuantumSpear() {
            super("quantum_spear", FamilyTier.CHAOS, props(Rarity.EPIC).attributes(SwordItem.createAttributes(FamilyTier.CHAOS, 5, -2.8f)
                    .withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH, 2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)), 110, 3);
        }
        @Override public boolean special(Level level, Player p, ItemStack s) {
            Vec3 start = p.position(), look = p.getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 end = start;
            for (double d = 0.5; d <= 8; d += 0.5) {
                Vec3 q = start.add(look.scale(d));
                if (!level.noCollision(p, p.getDimensions(p.getPose()).makeBoundingBox(q))) break;
                end = q;
            }
            if (end.distanceTo(start) < 1) return false;
            double len = end.distanceTo(start);
            for (LivingEntity t : WeaponFx.targets(p, start.lerp(end, 0.5), len / 2 + 1.5)) {
                Vec3 rel = t.position().subtract(start);
                double along = rel.dot(look);
                if (along < -0.5 || along > len + 1 || rel.subtract(look.scale(along)).length() > 2.0) continue;
                WeaponFx.hit(p, t, 8f);
                t.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {      // the spiral: two strands wound around the dash line
                Vec3 side = new Vec3(-look.z, 0, look.x);
                DustParticleOptions cyan = new DustParticleOptions(new Vector3f(0.26f, 0.9f, 1f), 1.1f);
                for (double d = 0; d <= len; d += 0.25) {
                    double a = d * 2.2;
                    for (int k = 0; k < 2; k++) {
                        double ang = a + k * Math.PI;
                        Vec3 c = start.add(look.scale(d)).add(0, 1.0, 0).add(side.scale(Math.cos(ang) * 0.55)).add(0, Math.sin(ang) * 0.55, 0);
                        sl.sendParticles(cyan, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                    }
                }
            }
            p.teleportTo(end.x, end.y, end.z); p.resetFallDistance();
            WeaponFx.trail(p, start.add(0, 1, 0), end.add(0, 1, 0), 0x43E6FF); WeaponFx.glints(p, end.add(0, 1, 0), 0x43E6FF, 20, 0.5);
            WeaponFx.sound(p, SoundEvents.TRIDENT_RIPTIDE_2.value(), 0.8f, 1.5f); WeaponFx.sound(p, com.matt.quantumfalcon.registry.ModSounds.WEAPON_BLINK, 0.6f, 0.9f);
            return true;
        }
    }

    /** Hits: 25% chance of a random chaos effect. Right-click: Ground Slam — 6 damage and knock-up within 4. */
    public static class ChaosHammer extends AbilitySwordItem {
        public ChaosHammer() { super("chaos_hammer", FamilyTier.CHAOS, props(Rarity.EPIC).attributes(SwordItem.createAttributes(FamilyTier.CHAOS, 8, -3.3f)), 160, 3); }
        @Override public boolean special(Level level, Player p, ItemStack s) {
            for (LivingEntity t : WeaponFx.targets(p, p.position(), 4)) { WeaponFx.hit(p, t, 6f); t.push(0, 0.7, 0); t.hurtMarked = true; }
            WeaponFx.ring(p, p.position(), 2, ParticleTypes.LAVA); WeaponFx.ring(p, p.position(), 4, ParticleTypes.EXPLOSION);
            WeaponFx.shock(p, p.position(), 0xFF5A2A, 2); WeaponFx.shock(p, p.position(), 0xFFB040, 4);
            WeaponFx.sound(p, SoundEvents.GENERIC_EXPLODE.value(), 0.4f, 1.4f); WeaponFx.sound(p, com.matt.quantumfalcon.registry.ModSounds.WEAPON_HAMMER_SLAM, 1.1f, 1.0f);
            return true;
        }
        @Override public boolean hurtEnemy(ItemStack s, LivingEntity t, LivingEntity a) {
            if (!a.level().isClientSide && a.getRandom().nextFloat() < 0.25f) chaos(a, t);
            return super.hurtEnemy(s, t, a);
        }
        public static void chaos(LivingEntity a, LivingEntity t) {
            var effs = List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.POISON, MobEffects.WITHER, MobEffects.LEVITATION, MobEffects.BLINDNESS);
            WeaponFx.effect(t, effs.get(a.getRandom().nextInt(effs.size())), 60, 0);
            WeaponFx.burst(a, t.position().add(0, 1, 0), new DustParticleOptions(new Vector3f(0.9f, 0.2f, 0.2f), 1.2f), 15, 0.4);
        }
    }

    /** Echo Dagger: backstabs deal +6 true damage and mark the target (Glowing). */
    public static class EchoDagger extends SwordItem {
        public EchoDagger() { super(FamilyTier.ECHO, props(Rarity.RARE).attributes(SwordItem.createAttributes(FamilyTier.ECHO, 1, -1.6f))); }
        @Override public boolean hurtEnemy(ItemStack s, LivingEntity t, LivingEntity a) {
            if (!a.level().isClientSide && WeaponFx.behind(t, a)) {
                WeaponFx.trueHit(a, t, 6f); WeaponFx.effect(t, MobEffects.GLOWING, 100, 0);
                WeaponFx.burst(a, t.position().add(0, 1, 0), ParticleTypes.SCULK_SOUL, 10, 0.3);
            }
            return super.hurtEnemy(s, t, a);
        }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> tip, TooltipFlag f) { WeaponFx.tooltip("echo_dagger", 2, tip); }
    }

    /** Quantum Dagger: 25% chance on hit to blink behind the target. */
    public static class QuantumDagger extends SwordItem {
        public QuantumDagger() { super(FamilyTier.QUANTUM, props(Rarity.RARE).attributes(SwordItem.createAttributes(FamilyTier.QUANTUM, 1, -1.6f))); }
        @Override public boolean hurtEnemy(ItemStack s, LivingEntity t, LivingEntity a) {
            if (!a.level().isClientSide && a.getRandom().nextFloat() < 0.25f) blinkBehind(a, t);
            return super.hurtEnemy(s, t, a);
        }
        public static boolean blinkBehind(LivingEntity a, LivingEntity t) {
            Vec3 back = t.getViewVector(1f).multiply(1, 0, 1);
            if (back.lengthSqr() < 1e-4) back = new Vec3(0, 0, 1);
            Vec3 q = t.position().subtract(back.normalize().scale(1.5));
            if (!a.level().noCollision(a, a.getDimensions(a.getPose()).makeBoundingBox(q))) return false;
            WeaponFx.burst(a, a.position().add(0, 1, 0), ParticleTypes.PORTAL, 15, 0.3);
            a.teleportTo(q.x, q.y, q.z);
            a.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
            return true;
        }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> tip, TooltipFlag f) { WeaponFx.tooltip("quantum_dagger", 2, tip); }
    }

    /**
     * Falconer's Gauntlet (invented, not in Matthew's notes). Use on/at a creature: your falcons within 32 attack it.
     * Use at nothing: your nearest falcon uses its variant ability. While held, your falcons deal +2 damage.
     */
    public static class FalconerGauntlet extends Item {
        public FalconerGauntlet() { super(new Item.Properties().stacksTo(1).durability(500).rarity(Rarity.UNCOMMON)); }
        public static boolean holds(LivingEntity e) { return e != null && (e.getMainHandItem().getItem() instanceof FalconerGauntlet || e.getOffhandItem().getItem() instanceof FalconerGauntlet); }
        public static List<QuantumFalconEntity> falcons(Player p) {
            return p.level().getEntitiesOfClass(QuantumFalconEntity.class, p.getBoundingBox().inflate(32), f -> f.isTame() && f.isOwnedBy(p) && f.isAlive());
        }
        /** Returns how many falcons were sent. */
        public static int sic(Player p, LivingEntity target) {
            int n = 0;
            for (QuantumFalconEntity f : falcons(p)) {
                if (f == target) continue;
                f.setOrderedToSit(false); f.setCommand(FalconCommand.ATTACK); f.setTarget(target); n++;
            }
            return n;
        }
        @Override public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
            ItemStack s = p.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.consume(s);
            EntityHitResult hit = WeaponFx.lookTarget(p, 32);
            if (hit != null && hit.getEntity() instanceof LivingEntity t && !(t instanceof QuantumFalconEntity qf && qf.isOwnedBy(p))) {
                int n = sic(p, t);
                p.displayClientMessage(Component.translatable("item.quantumfalcon.falconer_gauntlet.sic", n), true);
                WeaponFx.burst(p, t.position().add(0, t.getBbHeight() + 0.3, 0), ParticleTypes.ANGRY_VILLAGER, 3, 0.2);
            } else {
                QuantumFalconEntity f = falcons(p).stream().min(java.util.Comparator.comparingDouble(q -> q.distanceToSqr(p))).orElse(null);
                if (f == null) { p.displayClientMessage(Component.translatable("item.quantumfalcon.falconer_gauntlet.none"), true); return InteractionResultHolder.fail(s); }
                FalconAbilities.activate(f, p);
            }
            p.getCooldowns().addCooldown(this, 20);
            s.hurtAndBreak(1, p, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            return InteractionResultHolder.success(s);
        }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> tip, TooltipFlag f) { WeaponFx.tooltip("falconer_gauntlet", 3, tip); }
    }
}
