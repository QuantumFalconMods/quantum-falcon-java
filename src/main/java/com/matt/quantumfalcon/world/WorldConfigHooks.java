package com.matt.quantumfalcon.world;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.WorldConfig;
import com.matt.quantumfalcon.dungeon.DungeonGuardBlockEntity;
import com.matt.quantumfalcon.entity.boss.QFBoss;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.lang.reflect.Field;

/** Applies {@link WorldConfig}: structure placement at server start, boss and dungeon difficulty on spawn and on hit. */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public final class WorldConfigHooks {
    private WorldConfigHooks() { }

    public static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "config_health");
    /** Last structure placement applied, "set=spacing/separation/frequency" (qftests). */
    public static final java.util.Map<String, String> APPLIED = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onServerAboutToStart(ServerAboutToStartEvent e) {
        var reg = e.getServer().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        APPLIED.clear();
        WorldConfig.STRUCTURES.forEach((id, cfg) -> {
            var set = reg.get(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, id));
            if (set == null) return;
            StructurePlacement pl = set.placement();
            try {
                if (pl instanceof RandomSpreadStructurePlacement rs) {
                    int spacing = cfg.spacing().get() > 0 ? cfg.spacing().get() : rs.spacing();
                    int sep = cfg.separation().get() > 0 ? cfg.separation().get() : rs.separation();
                    if (sep >= spacing) sep = Math.max(0, spacing - 1);   // vanilla requires separation < spacing
                    set(RandomSpreadStructurePlacement.class, rs, "spacing", spacing);
                    set(RandomSpreadStructurePlacement.class, rs, "separation", sep);
                }
                float freq = cfg.enabled().get() ? 1.0f : 0.0f;
                set(StructurePlacement.class, pl, "frequency", freq);
                APPLIED.put(id, (pl instanceof RandomSpreadStructurePlacement rs ? rs.spacing() + "/" + rs.separation() : "-") + "/" + freq);
            } catch (ReflectiveOperationException ex) {
                QuantumFalcon.LOGGER.warn("[config] could not apply structure placement for {}: {}", id, ex.toString());
            }
        });
    }

    private static void set(Class<?> owner, Object o, String name, Object v) throws ReflectiveOperationException {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        if (v instanceof Integer i) f.setInt(o, i); else if (v instanceof Float fl) f.setFloat(o, fl); else f.set(o, v);
    }

    public static boolean dungeonGuard(Entity e) {
        for (String t : e.getTags()) if (t.equals(DungeonGuardBlockEntity.TAG_KEY) || t.equals(DungeonGuardBlockEntity.TAG_BOSS) || t.startsWith(DungeonGuardBlockEntity.TAG_REALM)) return true;
        return false;
    }

    /** Boss / dungeon health: a multiplier modifier on max health, added once (it is saved with the entity). */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof LivingEntity le)) return;
        double m = le instanceof QFBoss ? WorldConfig.bossHealth() : dungeonGuard(le) ? WorldConfig.dungeonHealth() : 1.0;
        var inst = le.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        boolean has = inst.getModifier(HEALTH_ID) != null;
        if (m == 1.0) { if (has) inst.removeModifier(HEALTH_ID); return; }
        if (has && inst.getModifier(HEALTH_ID).amount() == m - 1.0) return;
        boolean full = le.getHealth() >= le.getMaxHealth() - 0.01f;
        inst.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_ID, m - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (full) le.setHealth(le.getMaxHealth());
    }

    /** Boss / dungeon damage multiplier on whatever they hit (melee or their projectiles). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingIncomingDamageEvent e) {
        Entity src = e.getSource().getEntity();
        if (src == null || src.level().isClientSide) return;
        double m = src instanceof QFBoss ? WorldConfig.bossDamage() : dungeonGuard(src) ? WorldConfig.dungeonDamage() : 1.0;
        if (m != 1.0) e.setAmount((float) (e.getAmount() * m));
    }
}
