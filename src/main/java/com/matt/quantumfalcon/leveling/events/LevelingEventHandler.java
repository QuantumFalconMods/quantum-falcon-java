package com.matt.quantumfalcon.leveling.events;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.config.LevelingConfig;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.core.ItemLevelHelper;
import com.matt.quantumfalcon.leveling.core.LevelingSystem;
import com.matt.quantumfalcon.leveling.traits.Trait;
import com.matt.quantumfalcon.leveling.traits.TraitManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Event handlers for the item leveling system.
 * Handles XP gain from combat, mining, and trait effects.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID)
public class LevelingEventHandler {

    /**
     * Handle damage dealt by players - weapon XP and trait effects
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!LevelingConfig.isEnabled()) return;

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        LivingEntity target = event.getEntity();
        ItemStack weapon = player.getMainHandItem();

        if (!ItemLevelHelper.canLevel(weapon)) return;

        ItemLevelData data = ItemLevelHelper.getLevelData(weapon);
        if (data == null) return;

        float damage = event.getNewDamage();

        // Apply trait damage modifications
        for (ResourceLocation traitId : data.getActiveTraits()) {
            Trait trait = TraitManager.getTrait(traitId);
            if (trait != null) {
                damage = trait.onDamageCalculation(player, target, weapon, damage, data);
            }
        }

        // Update damage if modified
        if (damage != event.getNewDamage()) {
            event.setNewDamage(damage);
        }
    }

    /**
     * Handle post-damage effects (lifesteal, fire, etc.)
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!LevelingConfig.isEnabled()) return;

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        LivingEntity target = event.getEntity();
        ItemStack weapon = player.getMainHandItem();

        if (!ItemLevelHelper.canLevel(weapon)) return;

        ItemLevelData data = ItemLevelHelper.getLevelData(weapon);
        if (data == null) return;

        float damage = event.getNewDamage();

        // Apply trait post-damage effects
        for (ResourceLocation traitId : data.getActiveTraits()) {
            Trait trait = TraitManager.getTrait(traitId);
            if (trait != null) {
                trait.onDamageDealt(player, target, weapon, damage, data);
            }
        }

        // Add XP for hitting (chance-based)
        if (player.getRandom().nextFloat() < LevelingConfig.HIT_XP_CHANCE.get()) {
            LevelingSystem.addXpFromDamage(weapon, damage, player);
        }
    }

    /**
     * Handle damage taken by players - armor XP and defensive traits
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerDamaged(LivingDamageEvent.Pre event) {
        if (!LevelingConfig.isEnabled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        DamageSource source = event.getSource();
        float damage = event.getNewDamage();

        // living attackers only, and a per-attacker budget (one award per hit, shared by all pieces)
        boolean armorXp = earnsArmorXp(player, source) && player.getRandom().nextFloat() < LevelingConfig.ARMOR_XP_CHANCE.get()
            && hasLevelableArmor(player) && takeArmorBudget(player, source);

        // Process each armor piece
        for (ItemStack armor : player.getArmorSlots()) {
            if (!ItemLevelHelper.canLevel(armor)) continue;

            ItemLevelData data = ItemLevelHelper.getLevelData(armor);
            if (data == null) continue;

            // Apply defensive trait effects
            for (ResourceLocation traitId : data.getActiveTraits()) {
                Trait trait = TraitManager.getTrait(traitId);
                if (trait != null) {
                    damage = trait.onDamageTaken(player, source, armor, damage, data);
                }
            }

            // Add XP for taking damage (chance-based). only hits from a living attacker (no cactus / fall / lava loops)
            if (armorXp) {
                int xp = (int) Math.ceil(event.getOriginalDamage() * LevelingConfig.ARMOR_XP_MULTIPLIER.get());
                LevelingSystem.addXp(armor, Math.max(1, xp), player);
            }
        }

        // Update damage if modified
        if (damage != event.getNewDamage()) {
            event.setNewDamage(Math.max(0, damage));
        }
    }

    /**
     * Handle entity kills - bonus XP based on mob type
     */
    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        if (!LevelingConfig.isEnabled()) return;

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        LivingEntity killed = event.getEntity();
        ItemStack weapon = player.getMainHandItem();

        if (!ItemLevelHelper.canLevel(weapon)) return;

        ItemLevelData data = ItemLevelHelper.getLevelData(weapon);
        if (data == null) return;

        // Calculate XP based on mob type
        int xp = getXpForMob(killed);

        // Apply kill trait effects
        for (ResourceLocation traitId : data.getActiveTraits()) {
            Trait trait = TraitManager.getTrait(traitId);
            if (trait != null) {
                trait.onKill(player, killed, weapon, data);
            }
        }

        // Add XP
        LevelingSystem.addXpFromKill(weapon, xp, player);
    }

    /**
     * Handle critical hits - apply critical boost trait
     */
    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        if (!LevelingConfig.isEnabled()) return;
        if (!event.isCriticalHit()) return;

        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getMainHandItem();
        if (!ItemLevelHelper.canLevel(weapon)) return;

        ItemLevelData data = ItemLevelHelper.getLevelData(weapon);
        if (data == null) return;

        float critMult = event.getDamageMultiplier();

        // Apply critical hit trait effects
        for (ResourceLocation traitId : data.getActiveTraits()) {
            Trait trait = TraitManager.getTrait(traitId);
            if (trait != null) {
                critMult = trait.onCriticalHit(player, target, weapon, critMult, data);
            }
        }

        // Update multiplier if changed
        if (critMult != event.getDamageMultiplier()) {
            event.setDamageMultiplier(critMult);
        }
    }

    /**
     * Handle block breaking - tool XP and vein miner
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!LevelingConfig.isEnabled()) return;

        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;

        ItemStack tool = player.getMainHandItem();
        if (!ItemLevelHelper.canLevel(tool)) return;

        ItemLevelData data = ItemLevelHelper.getLevelData(tool);
        if (data == null) return;

        // Apply block break trait effects
        for (ResourceLocation traitId : data.getActiveTraits()) {
            Trait trait = TraitManager.getTrait(traitId);
            if (trait != null) {
                trait.onBlockBreak(player, tool, event.getState(), data);
            }
        }

        // Add XP for mining. no XP for instant-break blocks or blocks a player placed (place-and-break loops)
        boolean placed = com.matt.quantumfalcon.leveling.PlacedBlocks.consume(event.getLevel(), event.getPos());
        if (earnsMiningXp(event.getState(), event.getLevel(), event.getPos(), placed)) LevelingSystem.addXpFromMining(tool, player);
    }

    // ------------------------------------------------------------------ anti-AFK rules
    // Non-natural blocks are tracked persistently in a chunk attachment (PlacedBlocks): player placement, fluid generators, pistons.

    @SubscribeEvent
    public static void onPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide()) com.matt.quantumfalcon.leveling.PlacedBlocks.mark(event.getLevel(), event.getPos());
    }

    /** Cobblestone / stone / obsidian / basalt made by lava meeting water. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!event.isCanceled()) com.matt.quantumfalcon.leveling.PlacedBlocks.mark(event.getLevel(), event.getPos());
    }

    /** Blocks a piston pushes or pulls land somewhere new: mark the destinations. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPiston(net.neoforged.neoforge.event.level.PistonEvent.Pre event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) return;
        var resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) return;
        net.minecraft.core.Direction move = event.getPistonMoveType().isExtend ? event.getDirection() : event.getDirection().getOpposite();
        for (net.minecraft.core.BlockPos bp : resolver.getToPush()) com.matt.quantumfalcon.leveling.PlacedBlocks.mark(event.getLevel(), bp.relative(move));
    }

    private static boolean hasLevelableArmor(Player player) {
        for (ItemStack a : player.getArmorSlots()) if (ItemLevelHelper.canLevel(a) && ItemLevelHelper.getLevelData(a) != null) return true;
        return false;
    }

    public static boolean earnsMiningXp(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos, boolean playerPlaced) {
        if (playerPlaced) return false;
        return state.getDestroySpeed(level, pos) >= 0.5f;   // grass, flowers, torches, crops, slime... break instantly
    }

    public static boolean earnsArmorXp(Player player, DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker && attacker != player;
    }

    /** each attacker can feed one player's armour XP at most this many times per window (a caged mob AFK farm stalls). */
    public static int ARMOR_XP_PER_ATTACKER = 6;
    public static final long ARMOR_XP_WINDOW_TICKS = 20L * 60 * 20;   // 20 minutes
    private static final java.util.Map<String, long[]> ARMOR_BUDGET = new java.util.LinkedHashMap<>(256, 0.75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, long[]> e) { return size() > 8192; }
    };

    /** Consumes one award from the (player, attacker) budget; false when that attacker has used it up this window. */
    public static boolean takeArmorBudget(Player player, DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker)) return false;
        long now = player.level().getGameTime();
        long[] b = ARMOR_BUDGET.computeIfAbsent(player.getUUID() + "|" + attacker.getUUID(), k -> new long[]{0, now});
        if (now - b[1] > ARMOR_XP_WINDOW_TICKS) { b[0] = 0; b[1] = now; }
        if (b[0] >= ARMOR_XP_PER_ATTACKER) return false;
        b[0]++;
        return true;
    }

    /** Test hook: mark a position as player-placed. */
    public static void testMarkPlaced(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) { com.matt.quantumfalcon.leveling.PlacedBlocks.mark(level, pos); }

    /**
     * Calculate XP reward for killing a mob
     */
    private static int getXpForMob(LivingEntity entity) {
        // Boss mobs
        if (entity instanceof WitherBoss || entity instanceof EnderDragon) {
            return LevelingConfig.XP_KILL_BOSS.get();
        }

        // Mini-bosses (Warden, Elder Guardian, etc.)
        if (entity.getMaxHealth() >= 100) {
            return LevelingConfig.XP_KILL_MINIBOSS.get();
        }

        // Hostile mobs
        MobCategory category = entity.getType().getCategory();
        if (category == MobCategory.MONSTER) {
            return LevelingConfig.XP_KILL_HOSTILE.get();
        }

        // Passive mobs
        return LevelingConfig.XP_KILL_PASSIVE.get();
    }
}
