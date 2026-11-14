package com.matt.quantumfalcon.item.gear;

import com.matt.quantumfalcon.item.weapon.Weapons;
import com.matt.quantumfalcon.item.weapon.WeaponFx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Set;

/**
 * 2-piece / 4-piece bonuses for the themed armour families and Matthew's Void Steel set.
 * Void Steel and the Singularity "black hole aura" follow Matthew's recovered designs; the others were designed for this pass.
 */
@EventBusSubscriber(modid = "quantumfalcon")
public final class FamilySetBonuses {
    private FamilySetBonuses() {}
    public static final Set<String> SETS = Set.of("chaos", "crystal", "echo", "phase", "probability", "quantum", "resonance", "singularity", "temporal", "void", "void_steel");
    private static final ResourceLocation SING_KB = ResourceLocation.fromNamespaceAndPath("quantumfalcon", "singularity_set_knockback");

    /** Armour material name ("chaos", "void_steel", ...) of a quantumfalcon armour item, or null. */
    public static String setOf(ItemStack s) {
        if (!(s.getItem() instanceof ArmorItem ai)) return null;
        return ai.getMaterial().unwrapKey().filter(k -> k.location().getNamespace().equals("quantumfalcon"))
                .map(k -> k.location().getPath()).filter(SETS::contains).orElse(null);
    }

    public static int pieces(LivingEntity e, String set) {
        if (e == null) return 0;
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
            if (set.equals(setOf(e.getItemBySlot(slot)))) n++;
        return n;
    }

    public static void setTooltip(String set, List<Component> tip) {
        tip.add(Component.translatable("set_bonus.quantumfalcon.header.2").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("set_bonus.quantumfalcon." + set + ".2").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("set_bonus.quantumfalcon.header.4").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("set_bonus.quantumfalcon." + set + ".4").withStyle(ChatFormatting.GRAY));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        String set = setOf(e.getItemStack());
        if (set != null) setTooltip(set, e.getToolTip());
    }

    static void fx(LivingEntity p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> eff, int amp) {
        p.addEffect(new MobEffectInstance(eff, 50, amp, true, false, true));
    }

    // ---------------------------------------------------------------- periodic bonuses (once a second) + Shadow Step
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (p.level().isClientSide) return;
        shadowStepTick(p);
        if (p.tickCount % 20 == 0) applyPeriodic(p);
        if (p.tickCount % 6 == 0) ambient(p);
    }

    /** Accent colour of each set (matches the worn-layer glow colour in tools/gen_polish_armor.py). */
    public static final java.util.Map<String, Integer> AURA = java.util.Map.ofEntries(
        java.util.Map.entry("chaos", 0xFF7A2A), java.util.Map.entry("crystal", 0xA8F0FF), java.util.Map.entry("echo", 0x3FF0E0),
        java.util.Map.entry("phase", 0x7DFFF0), java.util.Map.entry("probability", 0xC8FF6A), java.util.Map.entry("quantum", 0x43E6FF),
        java.util.Map.entry("resonance", 0xFF9AD0), java.util.Map.entry("singularity", 0x9FFCFF), java.util.Map.entry("temporal", 0xFFD36A),
        java.util.Map.entry("void", 0xB45CFF), java.util.Map.entry("void_steel", 0x9C6BFF));

    /** Full-set ambient particles: soft motes drifting up around the wearer, plus set-specific accents. Returns the set shown or null. */
    public static String ambient(Player p) {
        if (!(p.level() instanceof net.minecraft.server.level.ServerLevel sl) || p.isInvisible() || p.isSpectator()) return null;
        for (String set : SETS) {
            if (pieces(p, set) < 4) continue;
            int c = AURA.getOrDefault(set, 0xFFFFFF);
            var rnd = p.getRandom();
            for (int i = 0; i < 2; i++) {
                double a = rnd.nextDouble() * Math.PI * 2, r = 0.45 + rnd.nextDouble() * 0.25;
                sl.sendParticles(WeaponFx.col(com.matt.quantumfalcon.registry.ModParticles.AURA.get(), c), p.getX() + Math.cos(a) * r, p.getY() + 0.1 + rnd.nextDouble() * 1.6, p.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            if (p.tickCount % 30 == 0) switch (set) {
                case "temporal" -> WeaponFx.runeCircle(p, p.position(), c, 0.9);
                case "chaos" -> sl.sendParticles(WeaponFx.col(com.matt.quantumfalcon.registry.ModParticles.EMBER_FX.get(), c), p.getX(), p.getY() + 0.2, p.getZ(), 4, 0.35, 0.05, 0.35, 0.01);
                case "singularity", "void", "void_steel" -> sl.sendParticles(WeaponFx.col(com.matt.quantumfalcon.registry.ModParticles.GLINT.get(), c), p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.5, 0.6, 0.5, 0.02);
                default -> { }
            }
            return set;
        }
        return null;
    }

    /** Completing a set (4th piece equipped) plays a resonance chime and a ring of the set colour. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onEquip(net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent e) {
        if (!(e.getEntity() instanceof Player p) || p.level().isClientSide || e.getSlot().getType() != EquipmentSlot.Type.HUMANOID_ARMOR) return;
        String set = setOf(e.getTo());
        if (set == null || set.equals(setOf(e.getFrom())) || pieces(p, set) < 4) return;
        WeaponFx.sound(p, com.matt.quantumfalcon.registry.ModSounds.ARMOR_SET_COMPLETE, 0.8f, 1.0f);
        WeaponFx.shock(p, p.position(), AURA.getOrDefault(set, 0xFFFFFF), 1.5);
    }

    /** Applies all once-a-second bonuses; public so tests can call it directly. */
    public static void applyPeriodic(Player p) {
        if (pieces(p, "chaos") >= 2) fx(p, MobEffects.DAMAGE_BOOST, 0);
        if (pieces(p, "crystal") >= 2) fx(p, MobEffects.DIG_SPEED, 0);
        if (pieces(p, "temporal") >= 2) fx(p, MobEffects.DIG_SPEED, 0);
        if (pieces(p, "phase") >= 2) fx(p, MobEffects.MOVEMENT_SPEED, 0);
        if (pieces(p, "probability") >= 2) fx(p, MobEffects.LUCK, 0);
        if (pieces(p, "quantum") >= 2) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        if (pieces(p, "resonance") >= 2 && p.getHealth() < p.getMaxHealth() * 0.5f) fx(p, MobEffects.REGENERATION, 0);
        if (pieces(p, "void") >= 2 && p.hasEffect(MobEffects.WITHER)) p.removeEffect(MobEffects.WITHER);
        if (pieces(p, "void_steel") >= 2) fx(p, MobEffects.FIRE_RESISTANCE, 0);
        if (pieces(p, "echo") >= 4 && p.tickCount % 60 == 0)
            for (LivingEntity h : WeaponFx.targets(p, p.position(), 16)) if (h instanceof Enemy) h.addEffect(new MobEffectInstance(MobEffects.GLOWING, 70, 0, true, false));
        // Singularity: 2pc knockback resistance, 4pc black hole aura (Matthew's design)
        var kb = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            if (pieces(p, "singularity") >= 2) { if (!kb.hasModifier(SING_KB)) kb.addTransientModifier(new AttributeModifier(SING_KB, 0.2, AttributeModifier.Operation.ADD_VALUE)); }
            else kb.removeModifier(SING_KB);
        }
        if (pieces(p, "singularity") >= 4) {
            for (LivingEntity h : WeaponFx.targets(p, p.position(), 6)) {
                if (!(h instanceof Enemy)) continue;
                Vec3 to = p.position().subtract(h.position());
                if (to.length() > 1.5) { h.setDeltaMovement(h.getDeltaMovement().add(to.normalize().scale(0.25))); h.hurtMarked = true; }
                h.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            }
            WeaponFx.ring(p, p.position(), 3, ParticleTypes.REVERSE_PORTAL);
        }
        // Void: rescue from the void (120 s cooldown)
        if (pieces(p, "void") >= 4 && p.getY() < p.level().getMinBuildHeight() - 8) voidRescue(p);
    }

    public static boolean voidRescue(Player p) {
        long now = p.level().getGameTime();
        if (now - p.getPersistentData().getLong("qf_void_rescue") < 2400) return false;
        int y = p.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getBlockX(), p.getBlockZ());
        if (y <= p.level().getMinBuildHeight()) y = p.level().getSharedSpawnPos().getY();
        p.teleportTo(p.getX(), y + 1, p.getZ()); p.resetFallDistance(); p.setDeltaMovement(Vec3.ZERO);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
        p.getPersistentData().putLong("qf_void_rescue", now);
        p.displayClientMessage(Component.translatable("set_bonus.quantumfalcon.void.rescue").withStyle(ChatFormatting.DARK_PURPLE), true);
        return true;
    }

    /** Void Steel 4pc: Shadow Step when you start sneaking (teleport 6 blocks forward, 5 s cooldown). */
    static void shadowStepTick(Player p) {
        boolean sneak = p.isShiftKeyDown();
        boolean was = p.getPersistentData().getBoolean("qf_was_sneaking");
        if (sneak != was) p.getPersistentData().putBoolean("qf_was_sneaking", sneak);
        if (sneak && !was && pieces(p, "void_steel") >= 4) shadowStep(p);
    }

    public static boolean shadowStep(Player p) {
        long now = p.level().getGameTime();
        if (now - p.getPersistentData().getLong("qf_shadow_step") < 100) return false;
        if (!WeaponFx.blink(p, 6)) return false;
        p.getPersistentData().putLong("qf_shadow_step", now);
        p.displayClientMessage(Component.translatable("set_bonus.quantumfalcon.void_steel.step").withStyle(ChatFormatting.DARK_PURPLE), true);
        return true;
    }

    // ---------------------------------------------------------------- damage-time bonuses
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        LivingEntity attacker = e.getSource().getEntity() instanceof LivingEntity le ? le : null;
        boolean melee = attacker != null && e.getSource().getDirectEntity() == attacker;
        // attacker-side bonuses (player hitting something)
        if (attacker instanceof Player ap && melee) {
            if (pieces(ap, "probability") >= 4 && ap.getRandom().nextFloat() < 0.10f) {
                e.setAmount(e.getAmount() * 2f);
                WeaponFx.burst(ap, victim.position().add(0, 1, 0), ParticleTypes.ENCHANTED_HIT, 15, 0.4);
            }
            if (pieces(ap, "chaos") >= 4 && ap.getRandom().nextFloat() < 0.20f) Weapons.ChaosHammer.chaos(ap, victim);
        }
        if (!(victim instanceof Player p)) return;
        // defender-side bonuses
        if (attacker != null && attacker != p && !(attacker instanceof Player)) {
            if (pieces(p, "phase") >= 4 && p.getRandom().nextFloat() < 0.10f) { dodge(e, p); return; }
            if (melee && pieces(p, "void_steel") >= 2 && p.getRandom().nextFloat() < 0.25f) { dodge(e, p); return; }   // Void Walking
        }
        if (melee && attacker != p) {
            if (pieces(p, "crystal") >= 4 && p.getRandom().nextFloat() < 0.20f) {
                attacker.invulnerableTime = 0;
                attacker.hurt(p.damageSources().thorns(p), e.getAmount() * 0.5f);
                WeaponFx.burst(p, attacker.position().add(0, 1, 0), ParticleTypes.END_ROD, 10, 0.3);
            }
            if (pieces(p, "void_steel") >= 4) attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            if (pieces(p, "resonance") >= 4 && p.getRandom().nextFloat() < 0.25f) harmonicPulse(p);
        }
    }

    static void dodge(LivingIncomingDamageEvent e, Player p) {
        e.setCanceled(true);
        WeaponFx.burst(p, p.position().add(0, 1, 0), ParticleTypes.PORTAL, 20, 0.4);
    }

    public static int harmonicPulse(Player p) {
        int n = 0;
        for (LivingEntity h : WeaponFx.targets(p, p.position(), 4)) {
            if (!(h instanceof Enemy)) continue;
            Vec3 away = h.position().subtract(p.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-3) continue;
            away = away.normalize(); h.push(away.x * 1.2, 0.4, away.z * 1.2); h.hurtMarked = true; n++;
        }
        WeaponFx.ring(p, p.position(), 2, ParticleTypes.NOTE); WeaponFx.ring(p, p.position(), 4, ParticleTypes.NOTE);
        return n;
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof Player p) || p.level().isClientSide) return;
        if (pieces(p, "temporal") >= 4 && p.isAlive() && p.getHealth() < p.getMaxHealth() * 0.3f) timeDilation(p);
    }

    /** Temporal 4pc: below 30% health, slow hostiles within 8 and hasten yourself (30 s cooldown). */
    public static boolean timeDilation(Player p) {
        long now = p.level().getGameTime();
        if (now - p.getPersistentData().getLong("qf_time_dilation") < 600) return false;
        p.getPersistentData().putLong("qf_time_dilation", now);
        for (LivingEntity h : WeaponFx.targets(p, p.position(), 8)) if (h instanceof Enemy) h.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));
        WeaponFx.ring(p, p.position(), 8, ParticleTypes.END_ROD);
        p.displayClientMessage(Component.translatable("set_bonus.quantumfalcon.temporal.dilation").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (pieces(e.getEntity(), "echo") >= 2) e.setDamageMultiplier(e.getDamageMultiplier() * 0.5f);
    }
}
