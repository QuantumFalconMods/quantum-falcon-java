package com.matt.quantumfalcon.integration.artifacts;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
// Curios API - Optional dependency
// import top.theillusivec4.curios.api.SlotContext;
// import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Complete Artifacts/Curios integration for Quantum Falcon boss drops.
 * Each boss drops a unique artifact with powerful abilities.
 */
public class ArtifactsIntegration {

    // the boss relics and Heart of Quantum are native items (ModItems). This class used to register the same five ids
    // again when Artifacts was installed, which duplicated registry keys. With Artifacts present it now adds nothing new;
    // the item classes below are kept only as reference behaviour for a future Curios-slot hook on the native items.

    /**
     * Initialize Artifacts integration
     */
    public static void init() {
        // Register boss death loot modification
    }

    /**
     * Register items to the mod event bus
     */
    public static void registerItems(IEventBus modBus) {
        // nothing to register: see the note above
    }

    // ============ Artifact Item Implementations ============

    /**
     * Overseer's Eye (Head Slot)
     * Dropped by: Quantum Overseer
     */
    public static class OverseersEyeItem extends Item implements ICurioItemStub {
        private static final float DODGE_CHANCE = 0.10f;

        public OverseersEyeItem(Properties props) {
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

            // Constant night vision
            if (level.getGameTime() % 60 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, false));
            }

            // Eye particles
            if (level.isClientSide && level.getGameTime() % 8 == 0) {
                double yaw = Math.toRadians(player.getYRot());
                double px = player.getX() - Math.sin(yaw) * 0.3;
                double pz = player.getZ() + Math.cos(yaw) * 0.3;
                level.addParticle(ParticleTypes.END_ROD, px, player.getEyeY() + 0.1, pz, 0, 0.01, 0);
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
            entity.level().playSound(null, entity.blockPosition(), SoundEvents.ENDER_EYE_DEATH, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f);
            if (entity instanceof Player player) {
                player.displayClientMessage(
                    Component.literal("The Overseer's gaze guides you...").withStyle(ChatFormatting.DARK_PURPLE),
                    true
                );
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("Boss Drop: Quantum Overseer").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("✦ Quantum Vision").withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.literal("  Reveals invisible entities").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Permanent night vision").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  10% chance to dodge unseen attacks").withStyle(ChatFormatting.GRAY));
        }

        @Override
        public boolean canEquipFromUse(Object ctx, ItemStack stack) {
            return true;
        }

        public static boolean shouldDodge(Player player, net.minecraft.world.damagesource.DamageSource source) {
            if (source.getEntity() != null && !player.hasLineOfSight(source.getEntity())) {
                return player.getRandom().nextFloat() < DODGE_CHANCE;
            }
            return false;
        }
    }

    /**
     * Warden's Fragment (Charm Slot)
     * Dropped by: Rift Warden
     */
    public static class WardensFragmentItem extends Item implements ICurioItemStub {
        private static final UUID ARMOR_UUID = UUID.fromString("8F91D5D2-9A6B-5C4E-AB3F-2D7E9F0B1C2D");
        private static final UUID TOUGH_UUID = UUID.fromString("9A02E6E3-AB7C-6D5F-BC40-3E8FA01C2D3E");
        private static final UUID KNOCKBACK_UUID = UUID.fromString("AB13F7F4-BC8D-7E60-CD51-4F9GB12D3E4F");

        public WardensFragmentItem(Properties props) {
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

            // Pulsing shield particles
            if (level.isClientSide && level.getGameTime() % 15 == 0) {
                for (int i = 0; i < 3; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = 0.6;
                    level.addParticle(ParticleTypes.ENCHANT,
                        player.getX() + Math.cos(angle) * radius,
                        player.getY() + 0.5 + Math.random(),
                        player.getZ() + Math.sin(angle) * radius,
                        0, 0.05, 0);
                }
            }
        }

        // @Override - signature doesn't match Curios API stub
        public com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, AttributeModifier> 
                getAttributeModifiersReal(Object ctx, UUID uuid, ItemStack stack) {
            com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, AttributeModifier> modifiers = 
                com.google.common.collect.LinkedHashMultimap.create();
            
            modifiers.put(Attributes.ARMOR, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "warden_armor"), 4.0, AttributeModifier.Operation.ADD_VALUE));
            modifiers.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "warden_toughness"), 2.0, AttributeModifier.Operation.ADD_VALUE));
            modifiers.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "warden_stability"), 0.5, AttributeModifier.Operation.ADD_VALUE));
            
            return modifiers;
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
            entity.level().playSound(null, entity.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 0.8f);
            if (entity instanceof Player player) {
                player.displayClientMessage(
                    Component.literal("The Warden's power shields you...").withStyle(ChatFormatting.DARK_AQUA),
                    true
                );
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("Boss Drop: Rift Warden").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("✦ Dimensional Anchor").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("  Immune to rift damage").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  +4 Armor, +2 Toughness").withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.literal("  +50% Knockback Resistance").withStyle(ChatFormatting.BLUE));
        }

        @Override
        public boolean canEquipFromUse(Object ctx, ItemStack stack) {
            return true;
        }
    }

    /**
     * Twin Essence (Ring Slot)
     * Dropped by: Quantum Twins
     */
    public static class TwinEssenceItem extends Item implements ICurioItemStub {
        private static final float DUPLICATE_CHANCE = 0.15f;
        private static final float DOUBLE_STRIKE_CHANCE = 0.20f;

        public TwinEssenceItem(Properties props) {
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

            // Entangled particles between nearby allies
            if (level.isClientSide && level.getGameTime() % 10 == 0) {
                final Player finalPlayer = player; // Make effectively final for lambda
                AABB searchBox = player.getBoundingBox().inflate(16);
                level.getEntitiesOfClass(Player.class, searchBox, p -> p != finalPlayer).stream().findFirst().ifPresent(ally -> {
                    Vec3 diff = ally.position().subtract(finalPlayer.position());
                    for (int i = 0; i < 3; i++) {
                        double t = i / 3.0;
                        level.addParticle(ParticleTypes.SOUL,
                            finalPlayer.getX() + diff.x * t,
                            finalPlayer.getY() + 1 + diff.y * t,
                            finalPlayer.getZ() + diff.z * t,
                            0, 0.02, 0);
                    }
                });
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
            entity.level().playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);
            if (entity instanceof Player player) {
                player.displayClientMessage(
                    Component.literal("Two souls bound as one...").withStyle(ChatFormatting.LIGHT_PURPLE),
                    true
                );
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("Boss Drop: Quantum Twins").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("✦ Duality").withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.literal("  15% chance to duplicate drops").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  20% chance for attacks to hit twice").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  -10% damage when ally nearby").withStyle(ChatFormatting.GREEN));
        }

        @Override
        public boolean canEquipFromUse(Object ctx, ItemStack stack) {
            return true;
        }

        public static boolean shouldDoubleStrike(Player player) {
            return player.getRandom().nextFloat() < DOUBLE_STRIKE_CHANCE;
        }

        public static boolean shouldDuplicateDrop(Player player) {
            return player.getRandom().nextFloat() < DUPLICATE_CHANCE;
        }

        public static float getAllyDamageReduction(Player player) {
            AABB searchBox = player.getBoundingBox().inflate(16);
            boolean hasAlly = !player.level().getEntitiesOfClass(Player.class, searchBox, p -> p != player).isEmpty();
            return hasAlly ? 0.10f : 0;
        }
    }

    /**
     * Sovereign's Crown (Head Slot)
     * Dropped by: Singularity Sovereign (Final Boss)
     */
    public static class SovereignsCrownItem extends Item implements ICurioItemStub {
        private static final float PULL_RADIUS = 8.0f;
        private static final float PULL_STRENGTH = 0.15f;
        private static final float FALCON_DAMAGE_BOOST = 0.50f;

        public SovereignsCrownItem(Properties props) {
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

            // Constant slow fall
            if (level.getGameTime() % 60 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false, false));
            }

            // Singularity Pull when sneaking
            if (player.isCrouching() && !level.isClientSide) {
                final Player finalPlayer = player; // Make effectively final for lambda
                AABB pullBox = player.getBoundingBox().inflate(PULL_RADIUS);
                level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, pullBox, e -> e != finalPlayer && !(e instanceof Player)).forEach(entity -> {
                    Vec3 diff = finalPlayer.position().subtract(entity.position());
                    double dist = diff.length();
                    if (dist > 1.0) {
                        Vec3 pull = diff.normalize().scale(PULL_STRENGTH);
                        entity.setDeltaMovement(entity.getDeltaMovement().add(pull));
                    }
                });

                // Vortex particles
                if (level instanceof ServerLevel serverLevel && level.getGameTime() % 2 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double radius = PULL_RADIUS * Math.random();
                        serverLevel.sendParticles(ParticleTypes.PORTAL,
                            player.getX() + Math.cos(angle) * radius,
                            player.getY() + 1,
                            player.getZ() + Math.sin(angle) * radius,
                            1, 0, 0, 0, 0.5);
                    }
                }
            }

            // Crown particles
            if (level.isClientSide && level.getGameTime() % 5 == 0) {
                double angle = (level.getGameTime() * 0.2) % (Math.PI * 2);
                for (int i = 0; i < 3; i++) {
                    double a = angle + (i * Math.PI * 2 / 3);
                    level.addParticle(ParticleTypes.END_ROD,
                        player.getX() + Math.cos(a) * 0.3,
                        player.getEyeY() + 0.4,
                        player.getZ() + Math.sin(a) * 0.3,
                        0, 0.01, 0);
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
            if (entity instanceof Player player) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
                player.displayClientMessage(
                    Component.literal("Reality bends to your will...").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                    true
                );
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("Boss Drop: Singularity Sovereign").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("The ultimate artifact").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("✦ Singularity Pull").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.literal("  Sneak to pull enemies towards you").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("✦ Gravity Mastery").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.literal("  Permanent slow falling").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("✦ Royal Command").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.literal("  Tamed falcons deal +50% damage").withStyle(ChatFormatting.RED));
        }

        @Override
        public boolean canEquipFromUse(Object ctx, ItemStack stack) {
            return true;
        }

        public static float getFalconDamageBoost() {
            return FALCON_DAMAGE_BOOST;
        }
    }

    /**
     * Heart of Quantum (Necklace Slot)
     * Crafted from all 4 boss artifacts
     */
    public static class HeartOfQuantumItem extends Item implements ICurioItemStub {

        public HeartOfQuantumItem(Properties props) {
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

            // Combined effects (weaker versions)
            if (level.getGameTime() % 80 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, false));
            }

            // Quantum heart particles
            if (level.isClientSide && level.getGameTime() % 3 == 0) {
                level.addParticle(ParticleTypes.END_ROD,
                    player.getX() + (Math.random() - 0.5) * 0.5,
                    player.getY() + 1.0 + (Math.random() - 0.5) * 0.3,
                    player.getZ() + (Math.random() - 0.5) * 0.5,
                    0, 0.05, 0);
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
            if (entity instanceof Player player) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);
                player.displayClientMessage(
                    Component.literal("You have mastered the quantum realm!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    true
                );
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("Forged from the essence of all bosses").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("✦ Combined Boss Powers").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("  Night vision, slow falling").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Rift immunity, ally bonuses").withStyle(ChatFormatting.GRAY));
        }

        @Override
        public boolean canEquipFromUse(Object ctx, ItemStack stack) {
            return true;
        }
    }
}
