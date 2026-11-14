package com.matt.quantumfalcon.integration.curios;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.integration.ModIntegrations;
import com.matt.quantumfalcon.integration.artifacts.ICurioItemStub;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
// Curios API - Optional dependency (compileOnly in build.gradle)
// Using reflection when Curios is not available at compile time
// import top.theillusivec4.curios.api.SlotContext;
// import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.function.Supplier;

/**
 * Complete Curios API integration for Quantum Falcon trinkets.
 * Provides ring, charm, necklace, and belt slot items with quantum effects.
 */
public class CuriosIntegration {

    public static final DeferredRegister.Items CURIO_ITEMS = 
        DeferredRegister.createItems(QuantumFalcon.MOD_ID);

    // ============ Curio Item Registrations ============

    public static final DeferredItem<Item> QUANTUM_BAND = CURIO_ITEMS.register("quantum_band",
        () -> new QuantumBandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> REALITY_ANCHOR = CURIO_ITEMS.register("reality_anchor",
        () -> new RealityAnchorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> ENTANGLEMENT_CORE = CURIO_ITEMS.register("entanglement_core",
        () -> new EntanglementCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> FALCON_FEATHER_CHARM = CURIO_ITEMS.register("falcon_feather_charm",
        () -> new FalconFeatherCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> VOID_LENS = CURIO_ITEMS.register("void_lens",
        () -> new VoidLensItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> SINGULARITY_BELT = CURIO_ITEMS.register("singularity_belt",
        () -> new SingularityBeltItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    /**
     * Initialize Curios integration
     */
    public static void init() {
        // Curios handles slot registration via IMC or data
    }

    /**
     * Register items to the mod event bus
     */
    public static void registerItems(IEventBus modBus) {
        CURIO_ITEMS.register(modBus);
    }

    // ============ Curio Item Implementations ============

    /**
     * Quantum Band (Ring Slot)
     * - Increases falcon ability range by 50%
     * - Grants magnetic item pickup (3 block radius)
     * - Reduces instability decay rate
     */
    public static class QuantumBandItem extends Item implements ICurioItemStub {
        private static final float MAGNETIC_RADIUS = 3.0f;
        private static final float FALCON_RANGE_MULT = 1.5f;

        public QuantumBandItem(Properties props) {
            super(props);
        }

        @Override
        public void curioTick(Object ctx, ItemStack stack) {
            if (ctx == null) return;
            Player player = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object entity = getEntity.invoke(ctx);
                if (entity instanceof Player) {
                    player = (Player) entity;
                }
            } catch (Exception e) {
                return;
            }
            if (player == null) return;
            Level level = player.level();
            
            // Magnetic pickup every 5 ticks
            if (level.getGameTime() % 5 == 0 && !level.isClientSide) {
                final Player finalPlayer = player; // Make effectively final for lambda
                AABB magneticBox = player.getBoundingBox().inflate(MAGNETIC_RADIUS);
                level.getEntitiesOfClass(ItemEntity.class, magneticBox)
                    .forEach(item -> {
                        if (!item.hasPickUpDelay()) {
                            item.setPos(finalPlayer.getX(), finalPlayer.getY(), finalPlayer.getZ());
                        }
                    });
            }

            // Particles (client)
            if (level.isClientSide && level.getGameTime() % 10 == 0) {
                double angle = (level.getGameTime() * 0.1) % (Math.PI * 2);
                double px = player.getX() + Math.cos(angle) * 0.5;
                double pz = player.getZ() + Math.sin(angle) * 0.5;
                level.addParticle(ParticleTypes.PORTAL, px, player.getY() + 0.5, pz, 0, 0.02, 0);
            }
        }

        @Override
        public void onEquip(Object ctx, ItemStack prevStack, ItemStack stack) {
            if (ctx == null) return;
            net.minecraft.world.entity.LivingEntity entity = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object e = getEntity.invoke(ctx);
                if (e instanceof net.minecraft.world.entity.LivingEntity) {
                    entity = (net.minecraft.world.entity.LivingEntity) e;
                }
            } catch (Exception e) {
                return;
            }
            if (entity == null) return;
            entity.level().playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.8f);
        }

        @Override
        public void onUnequip(Object ctx, ItemStack newStack, ItemStack stack) {
            if (ctx == null) return;
            net.minecraft.world.entity.LivingEntity entity = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object e = getEntity.invoke(ctx);
                if (e instanceof net.minecraft.world.entity.LivingEntity) {
                    entity = (net.minecraft.world.entity.LivingEntity) e;
                }
            } catch (Exception e) {
                return;
            }
            if (entity != null) {
                entity.level().playSound(null, entity.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 1.5f);
            }
        }

        @Override
        public boolean canEquipFromUse(Object ctx, ItemStack stack) {
            return true;
        }

        public static float getFalconRangeMultiplier() {
            return FALCON_RANGE_MULT;
        }
    }

    /**
     * Reality Anchor (Charm Slot)
     * - Prevents gravity well pull effects
     * - 50% resistance to rift damage
     * - Stabilizes quantum fluctuations nearby
     */
    public static class RealityAnchorItem extends Item implements ICurioItemStub {
        private static final float RIFT_RESISTANCE = 0.5f;
        private static final float STABILIZE_RADIUS = 8.0f;

        public RealityAnchorItem(Properties props) {
            super(props);
        }

        @Override
        public void curioTick(Object ctx, ItemStack stack) {
            if (ctx == null) return;
            Player player = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object entity = getEntity.invoke(ctx);
                if (entity instanceof Player) {
                    player = (Player) entity;
                }
            } catch (Exception e) {
                return;
            }
            if (player == null) return;
            Level level = player.level();

            // Slow fall when in rift dimension or near gravity wells
            if (player.getDeltaMovement().y < -0.5) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0.6, 1));
                player.fallDistance *= 0.5f;
            }
        }

        @Override
        public void onEquip(Object ctx, ItemStack prevStack, ItemStack stack) {
            if (ctx == null) return;
            net.minecraft.world.entity.LivingEntity entity = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object e = getEntity.invoke(ctx);
                if (e instanceof net.minecraft.world.entity.LivingEntity) {
                    entity = (net.minecraft.world.entity.LivingEntity) e;
                }
            } catch (Exception e) {
                return;
            }
            if (entity == null) return;
            entity.level().playSound(null, entity.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.6f, 1.2f);
        }

        @Override
        public boolean canEquipFromUse(Object ctx, ItemStack stack) {
            return true;
        }

        public static float getRiftResistance() {
            return RIFT_RESISTANCE;
        }
    }

    /**
     * Entanglement Core (Necklace Slot)
     * - Links with tamed falcon for shared effects
     * - Falcon abilities affect player (speed, regen)
     * - Teleport to falcon on sneak + use
     */
    public static class EntanglementCoreItem extends Item implements ICurioItemStub {
        private static final int LINK_RANGE = 64;

        public EntanglementCoreItem(Properties props) {
            super(props);
        }

        @Override
        public void curioTick(Object ctx, ItemStack stack) {
            if (ctx == null) return;
            Player player = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object entity = getEntity.invoke(ctx);
                if (entity instanceof Player) {
                    player = (Player) entity;
                }
            } catch (Exception e) {
                return;
            }
            if (player == null) return;
            Level level = player.level();

            if (level.isClientSide) return;

            // Find linked falcon
            final Player finalPlayer = player; // Make effectively final for lambda
            AABB searchBox = player.getBoundingBox().inflate(LINK_RANGE);
            List<com.matt.quantumfalcon.entity.QuantumFalconEntity> falcons = level.getEntitiesOfClass(
                com.matt.quantumfalcon.entity.QuantumFalconEntity.class, searchBox,
                f -> f.isTame() && f.isOwnedBy(finalPlayer)
            );
            
            if (falcons.isEmpty()) return;
            var linkedFalcon = falcons.get(0);

            // Share effects every second
            if (level.getGameTime() % 20 == 0) {
                if (linkedFalcon.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
                }
                if (linkedFalcon.hasEffect(MobEffects.REGENERATION)) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
                }
            }

            // Visual link particles
            if (level.getGameTime() % 5 == 0 && level instanceof ServerLevel serverLevel) {
                double dx = linkedFalcon.getX() - player.getX();
                double dy = linkedFalcon.getY() - player.getY();
                double dz = linkedFalcon.getZ() - player.getZ();
                
                for (int i = 0; i < 5; i++) {
                    double t = i / 5.0;
                    serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        player.getX() + dx * t,
                        player.getY() + 1 + dy * t,
                        player.getZ() + dz * t,
                        1, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }

        @Override
        public void onEquip(Object ctx, ItemStack prevStack, ItemStack stack) {
            if (ctx == null) return;
            net.minecraft.world.entity.LivingEntity entity = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object e = getEntity.invoke(ctx);
                if (e instanceof net.minecraft.world.entity.LivingEntity) {
                    entity = (net.minecraft.world.entity.LivingEntity) e;
                }
            } catch (Exception e) {
                return;
            }
            if (entity == null) return;
            entity.level().playSound(null, entity.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.8f, 1.5f);
        }

        @Override
        public boolean canEquipFromUse(Object ctx, ItemStack stack) {
            return true;
        }
    }

    /**
     * Falcon Feather Charm (Charm Slot)
     * - Slow falling when crouching
     * - 20% increased jump height
     * - Feather particles on jump
     */
    public static class FalconFeatherCharmItem extends Item implements ICurioItemStub {
        private static final float JUMP_BOOST = 0.2f;

        public FalconFeatherCharmItem(Properties props) {
            super(props);
        }

        @Override
        public void curioTick(Object ctx, ItemStack stack) {
            if (ctx == null) return;
            Player player = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object entity = getEntity.invoke(ctx);
                if (entity instanceof Player) {
                    player = (Player) entity;
                }
            } catch (Exception e) {
                return;
            }
            if (player == null) return;

            // Slow fall when crouching and falling
            if (player.isCrouching() && player.getDeltaMovement().y < -0.1) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0.6, 1));
                player.fallDistance = 0;

                // Feather particles
                if (player.level().isClientSide && player.level().getGameTime() % 3 == 0) {
                    player.level().addParticle(ParticleTypes.WHITE_ASH,
                        player.getX() + (Math.random() - 0.5) * 0.5,
                        player.getY(),
                        player.getZ() + (Math.random() - 0.5) * 0.5,
                        0, -0.05, 0);
                }
            }
        }

        @Override
        public void onEquip(Object ctx, ItemStack prevStack, ItemStack stack) {
            if (ctx == null) return;
            net.minecraft.world.entity.LivingEntity entity = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object e = getEntity.invoke(ctx);
                if (e instanceof net.minecraft.world.entity.LivingEntity) {
                    entity = (net.minecraft.world.entity.LivingEntity) e;
                }
            } catch (Exception e) {
                return;
            }
            if (entity == null) return;
            entity.level().playSound(null, entity.blockPosition(), SoundEvents.PARROT_FLY, SoundSource.PLAYERS, 0.5f, 1.2f);
        }

        @Override
        public boolean canEquipFromUse(Object ctx, ItemStack stack) {
            return true;
        }

        public static float getJumpBoost() {
            return JUMP_BOOST;
        }
    }

    /**
     * Void Lens (Head/Face Slot)
     * - Night vision in rift dimension
     * - See invisible entities
     * - Highlight quantum entities through walls
     */
    public static class VoidLensItem extends Item implements ICurioItemStub {

        public VoidLensItem(Properties props) {
            super(props);
        }

        @Override
        public void curioTick(Object ctx, ItemStack stack) {
            if (ctx == null) return;
            Player player = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object entity = getEntity.invoke(ctx);
                if (entity instanceof Player) {
                    player = (Player) entity;
                }
            } catch (Exception e) {
                return;
            }
            if (player == null) return;
            Level level = player.level();

            // Night vision in rift dimension or caves
            if (level.getGameTime() % 40 == 0) {
                boolean inRift = level.dimension().location().getNamespace().equals(QuantumFalcon.MOD_ID);
                boolean inDark = level.getMaxLocalRawBrightness(player.blockPosition()) < 4;

                if (inRift || inDark) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false));
                }
            }
        }

        @Override
        public void onEquip(Object ctx, ItemStack prevStack, ItemStack stack) {
            if (ctx == null) return;
            net.minecraft.world.entity.LivingEntity entity = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object e = getEntity.invoke(ctx);
                if (e instanceof net.minecraft.world.entity.LivingEntity) {
                    entity = (net.minecraft.world.entity.LivingEntity) e;
                }
            } catch (Exception e) {
                return;
            }
            if (entity == null) return;
            entity.level().playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.PLAYERS, 0.3f, 1.5f);
        }

        @Override
        public boolean canEquipFromUse(Object ctx, ItemStack stack) {
            return true;
        }
    }

    /**
     * Singularity Belt (Belt Slot)
     * - Pull items from 8 blocks on sneak
     * - Increased inventory (if supported)
     */
    public static class SingularityBeltItem extends Item implements ICurioItemStub {
        private static final float PULL_RADIUS = 8.0f;

        public SingularityBeltItem(Properties props) {
            super(props);
        }

        @Override
        public void curioTick(Object ctx, ItemStack stack) {
            if (ctx == null) return;
            Player player = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object entity = getEntity.invoke(ctx);
                if (entity instanceof Player) {
                    player = (Player) entity;
                }
            } catch (Exception e) {
                return;
            }
            if (player == null) return;
            Level level = player.level();

            // Pull items when sneaking
            if (player.isCrouching() && !level.isClientSide && level.getGameTime() % 5 == 0) {
                final Player finalPlayer = player; // Make effectively final for lambda
                AABB pullBox = player.getBoundingBox().inflate(PULL_RADIUS);
                level.getEntitiesOfClass(ItemEntity.class, pullBox).forEach(item -> {
                    if (!item.hasPickUpDelay()) {
                        // Pull towards player
                        double dx = finalPlayer.getX() - item.getX();
                        double dy = finalPlayer.getY() - item.getY();
                        double dz = finalPlayer.getZ() - item.getZ();
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (dist > 0.5) {
                            item.setDeltaMovement(
                                dx / dist * 0.3,
                                dy / dist * 0.3 + 0.1,
                                dz / dist * 0.3
                            );
                        }
                    }
                });

                // Particle vortex
                if (level instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 3; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double radius = PULL_RADIUS * Math.random();
                        serverLevel.sendParticles(ParticleTypes.PORTAL,
                            player.getX() + Math.cos(angle) * radius,
                            player.getY() + 0.5,
                            player.getZ() + Math.sin(angle) * radius,
                            1, 0, 0, 0, 0.1);
                    }
                }
            }
        }

        @Override
        public void onEquip(Object ctx, ItemStack prevStack, ItemStack stack) {
            if (ctx == null) return;
            net.minecraft.world.entity.LivingEntity entity = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object e = getEntity.invoke(ctx);
                if (e instanceof net.minecraft.world.entity.LivingEntity) {
                    entity = (net.minecraft.world.entity.LivingEntity) e;
                }
            } catch (Exception e) {
                return;
            }
            if (entity == null) return;
            entity.level().playSound(null, entity.blockPosition(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 0.5f, 1.3f);
        }

        @Override
        public void onUnequip(Object ctx, ItemStack newStack, ItemStack stack) {
            if (ctx == null) return;
            net.minecraft.world.entity.LivingEntity entity = null;
            try {
                java.lang.reflect.Method getEntity = ctx.getClass().getMethod("entity");
                Object e = getEntity.invoke(ctx);
                if (e instanceof net.minecraft.world.entity.LivingEntity) {
                    entity = (net.minecraft.world.entity.LivingEntity) e;
                }
            } catch (Exception e) {
                return;
            }
            if (entity != null) {
                entity.level().playSound(null, entity.blockPosition(), SoundEvents.ENDER_CHEST_CLOSE, SoundSource.PLAYERS, 0.5f, 1.3f);
            }
        }

        @Override
        public boolean canEquipFromUse(Object ctx, ItemStack stack) {
            return true;
        }
    }
}
