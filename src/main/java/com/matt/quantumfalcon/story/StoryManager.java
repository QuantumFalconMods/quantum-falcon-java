package com.matt.quantumfalcon.story;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.*;

/**
 * Manages the overall story progression and multiple endings system.
 * 
 * Three endings after defeating Singularity Sovereign:
 * - LIGHT: Restore reality, sacrifice power
 * - DARK: keep the hour and wear the crown
 * - TRUE: Transcend all limitations (requires all boss artifacts)
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = com.matt.quantumfalcon.QuantumFalcon.MOD_ID)
public class StoryManager {
    
    // readiness (Sovereign defeated) and the chosen ending live in the PlayerStoryData attachment, so they survive
    // restarts and death. (They used to be in-memory sets.)
    
    /**
     * Ending types.
     */
    public enum Ending {
        LIGHT("Path of Light", "You chose to restore reality...", ChatFormatting.WHITE),
        DARK("Path of Darkness", "You embraced the void's power...", ChatFormatting.DARK_PURPLE),
        TRUE("Path of Transcendence", "You transcended all limitations...", ChatFormatting.LIGHT_PURPLE);
        
        public final String title;
        public final String description;
        public final ChatFormatting color;
        
        Ending(String title, String description, ChatFormatting color) {
            this.title = title;
            this.description = description;
            this.color = color;
        }
    }
    
    /** Mark player as ready for ending choice (the Sovereign fell to them). */
    public static void setPlayerReadyForEnding(net.minecraft.world.entity.player.Player player, boolean ready) {
        PlayerStoryData.get(player).setSovereignDefeated(ready);
    }

    /** Ready = Sovereign defeated and no ending chosen yet. */
    public static boolean canMakeEndingChoice(net.minecraft.world.entity.player.Player player) {
        PlayerStoryData d = PlayerStoryData.get(player);
        return d.isSovereignDefeated() && !d.hasChosenEnding();
    }

    /** True path: the four relics (or the Reality Gem made from them) and max bond with every falcon type. */
    public static boolean canChooseTrue(ServerPlayer player) {
        return hasAllArtifacts(player) && PlayerStoryData.get(player).hasMaxBondAllTypes();
    }

    /** How many of the four relics count for the True path (4 once they are fused into a Reality Gem or the Heart). */
    public static int relicCount(ServerPlayer player) {
        if (hasAllArtifacts(player)) return 4;
        int n = 0;
        for (String id : new String[]{"overseers_eye", "wardens_fragment", "twin_essence", "sovereigns_crown"})
            if (player.getInventory().contains(stack -> stack.getItem().toString().contains(id) && boundToOrFree(stack, player))) n++;
        return n;
    }

    /**
     * Check if player has required items for True Ending.
     */
    public static boolean hasAllArtifacts(ServerPlayer player) {
        // Check for all 4 boss artifacts in inventory
        boolean hasOverseerEye = player.getInventory().contains(
            stack -> stack.getItem().toString().contains("overseers_eye") && boundToOrFree(stack, player));
        boolean hasWardenFragment = player.getInventory().contains(
            stack -> stack.getItem().toString().contains("wardens_fragment") && boundToOrFree(stack, player));
        boolean hasTwinEssence = player.getInventory().contains(
            stack -> stack.getItem().toString().contains("twin_essence") && boundToOrFree(stack, player));
        boolean hasSovereignCrown = player.getInventory().contains(
            stack -> stack.getItem().toString().contains("sovereigns_crown") && boundToOrFree(stack, player));
        
        if (hasOverseerEye && hasWardenFragment && hasTwinEssence && hasSovereignCrown) return true;
        // Loot chain: the 4 relics are fused into a Reality Gem, which is fused into the Transcendence Shard.
        // Holding a Reality Gem, or having crafted one, counts as carrying all four artifacts.
        // the Heart of Quantum is now crafted from a Reality Gem, so holding or having crafted either one counts too.
        // a gem or Heart someone else crafted doesn't count (it is bound to its crafter)
        return heldFusedRelic(player) || craftedFusedRelic(player);
    }
    
    /**
     * the True choice spends a raw relic set (one of each) if that is what qualifies the player; a Reality Gem or
     * Heart of Quantum is bound to whoever crafted it ({@link #onCrafted}), so handing one to a friend doesn't qualify
     * them. Returns what was consumed ("" if nothing).
     */
    public static String consumeTrueRelics(ServerPlayer player) {
        var inv = player.getInventory();
        purgeStaleRelics(player);
        if (heldFusedRelic(player) || craftedFusedRelic(player)) return "";
        var relics = new net.minecraft.world.item.Item[]{com.matt.quantumfalcon.registry.ModItems.OVERSEERS_EYE.get(), com.matt.quantumfalcon.registry.ModItems.WARDENS_FRAGMENT.get(),
            com.matt.quantumfalcon.registry.ModItems.TWIN_ESSENCE.get(), com.matt.quantumfalcon.registry.ModItems.SOVEREIGNS_CROWN.get()};
        for (var r : relics) if (inv.countItem(r) == 0) return "";
        for (var r : relics) inv.clearOrCountMatchingItems(st -> st.is(r), 1, player.inventoryMenu.getCraftSlots());
        player.displayClientMessage(Component.literal("The four relics dissolve into the new reality.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return "relics";
    }

    // ------------------------------------------------------------------ one live copy of each gifted relic
    public static final String GEN_KEY = "qf_relic_gen";
    static String genKey(net.minecraft.world.item.Item it) { return GEN_KEY + "." + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it).getPath(); }

    /** Marks a freshly gifted relic as this player's newest copy (and so every older copy of it as stale). */
    public static void stampRelic(net.minecraft.world.item.ItemStack st, net.minecraft.world.entity.player.Player p) {
        var pt = com.matt.quantumfalcon.util.PersistedTag.of(p);
        int g = pt.getInt(genKey(st.getItem())) + 1;
        pt.putInt(genKey(st.getItem()), g);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, st, t -> t.putInt(GEN_KEY, g));
    }

    /** A copy of a gifted relic, bound to p, older than p's newest one (a stash kept through a re-gift). */
    public static boolean staleRelic(net.minecraft.world.item.ItemStack st, net.minecraft.world.entity.player.Player p) {
        if (st.isEmpty() || !p.getUUID().equals(boundTo(st))) return false;
        int live = com.matt.quantumfalcon.util.PersistedTag.of(p).getInt(genKey(st.getItem()));
        if (live == 0) return false;
        var cd = st.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        int g = cd == null ? 0 : cd.copyTag().getInt(GEN_KEY);
        return g < live;
    }

    /** Removes stale relic copies from the inventory. Returns how many crumbled. */
    public static int purgeStaleRelics(net.minecraft.world.entity.player.Player p) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var st = inv.getItem(i);
            if (staleRelic(st, p)) {
                p.displayClientMessage(Component.literal("An older " + st.getHoverName().getString() + " crumbles to dust. The relic answers only to its newest echo.")
                    .withStyle(ChatFormatting.DARK_PURPLE), false);
                n += st.getCount(); inv.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        return n;
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerTickRelics(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer sp && sp.tickCount % 20 == 7) purgeStaleRelics(sp);
    }

    public static final String BOUND_KEY = "qf_bound_to";
    /** persisted on the player once they have made (or been bound) a Reality Gem / Heart / Transcendence Shard of their own. */
    public static final String FUSED_KEY = "qf_fused_relic";

    static boolean isFused(net.minecraft.world.item.ItemStack st) {
        return st.is(com.matt.quantumfalcon.registry.ModItems.REALITY_GEM.get()) || st.is(com.matt.quantumfalcon.registry.ModItems.HEART_OF_QUANTUM.get())
            || st.getItem().toString().contains("transcendence_shard");
    }

    /**
     * BoundRelicRecipe already bound the result to the owner of the bound ingredients (every crafting path,
     * the vanilla Crafter included). At a crafting table, a result made only from unbound ingredients (old saves, floor loot)
     * is bound to the crafter; a result from someone else's gem stays theirs, so it never qualifies the crafter.
     */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onCrafted(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e) {
        var st = e.getCrafting();
        if (!isFused(st)) return;
        if (boundTo(st) == null) {
            boolean anyBound = false;
            var inv = e.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) if (boundTo(inv.getItem(i)) != null) anyBound = true;
            if (!anyBound) bind(st, e.getEntity());
        }
        markFused(e.getEntity(), st);
    }

    /** an unbound gem / Heart (a Crafter fed with unbound relics) binds to the first player who picks it up. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre e) {
        var st = e.getItemEntity().getItem();
        if (!isFused(st) || boundTo(st) != null || e.getPlayer().level().isClientSide) return;
        bind(st, e.getPlayer());
        markFused(e.getPlayer(), st);
    }

    static void markFused(net.minecraft.world.entity.player.Player p, net.minecraft.world.item.ItemStack st) {
        java.util.UUID who = boundTo(st);
        if (who != null && who.equals(p.getUUID())) com.matt.quantumfalcon.util.PersistedTag.of(p).putBoolean(FUSED_KEY, true);
    }

    public static void bind(net.minecraft.world.item.ItemStack st, net.minecraft.world.entity.player.Player p) { bind(st, p.getUUID()); }

    public static void bind(net.minecraft.world.item.ItemStack st, java.util.UUID who) {
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, st, t -> t.putUUID(BOUND_KEY, who));
    }

    /** The player a story item is bound to, or null. */
    public static java.util.UUID boundTo(net.minecraft.world.item.ItemStack st) {
        var cd = st.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        var t = cd.copyTag();
        return t.hasUUID(BOUND_KEY) ? t.getUUID(BOUND_KEY) : null;
    }

    /** True if the stack is unbound (older saves, floor loot) or bound to this player. Used for raw relics. */
    public static boolean boundToOrFree(net.minecraft.world.item.ItemStack st, net.minecraft.world.entity.player.Player p) {
        if (!com.matt.quantumfalcon.WorldConfig.relicsBind()) return true;   // config: relics don't bind
        java.util.UUID u = boundTo(st);
        return u == null || u.equals(p.getUUID());
    }

    /** a fused relic counts only when bound to this player (an unbound one binds on pickup or at the crafting table). */
    static boolean heldFusedRelic(ServerPlayer player) {
        return player.getInventory().contains(st -> isFused(st) && player.getUUID().equals(boundTo(st)));
    }

    /** replaces the ITEM_CRAFTED stat, which counted any craft (someone else's gem, a gem later handed away). */
    static boolean craftedFusedRelic(ServerPlayer player) {
        return com.matt.quantumfalcon.util.PersistedTag.of(player).getBoolean(FUSED_KEY);
    }

    /**
     * Execute the chosen ending.
     */
    public static void executeEnding(ServerPlayer player, Ending ending) {
        if (!canMakeEndingChoice(player)) {
            player.displayClientMessage(
                Component.literal(PlayerStoryData.get(player).hasChosenEnding() ? "Your fate is already sealed." : "You have not yet earned the right to choose your fate.")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        if (ending == Ending.TRUE && !canChooseTrue(player)) {
            player.displayClientMessage(
                Component.literal("The True Path needs the four relics and a max-bond falcon of each hidden kind: Void, Echo, Singularity, Quantum. (The Rift Codex page shows which you have.)")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        if (ending == Ending.TRUE) consumeTrueRelics(player);   // one set of relics can't be passed around a server
        QuantumFalcon.LOGGER.info("Player {} chose ending: {}", player.getName().getString(), ending.name());
        PlayerStoryData.get(player).setChosenEnding(ending.name());
        grantAdvancement(player, "story/choose_ending");
        player.refreshTabListName();   // True: "Late Stitch" (StoryEvents.onTabName)
        StorylineManager.triggerStoryEvent(player, switch (ending) {
            case LIGHT -> StorylineManager.StoryChapter.EPILOGUE_LIGHT;
            case DARK -> StorylineManager.StoryChapter.EPILOGUE_DARK;
            case TRUE -> StorylineManager.StoryChapter.EPILOGUE_TRUE;
        });
        switch (ending) {
            case LIGHT -> executeLightEnding(player);
            case DARK -> executeDarkEnding(player);
            case TRUE -> executeTrueEnding(player);
        }
    }

    /**
     * Light Ending: let the morning come.
     */
    private static void executeLightEnding(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        
        // Display ending text
        displayEndingText(player, Ending.LIGHT, new String[]{
            "",
            "══════════════════════════════════════════",
            "          ☀ LET THE MORNING COME ☀",
            "══════════════════════════════════════════",
            "",
            "The Sovereign is gone, and the hour it held",
            "slips out of your hands like a bird let go.",
            "",
            "Somewhere past the Rift's east edge,",
            "for the first time in an age, it gets lighter.",
            "",
            "The doors will still open. The echoes will still walk.",
            "But the night has an ending now.",
            "",
            "Your falcon stops looking east. It doesn't need to.",
            "",
            "You took back the hour, and you let it go. That was the whole point.",
            "",
            "══════════════════════════════════════════",
            "      ENDING: THE MORNING",
            "══════════════════════════════════════════"
        });
        
        // Visual effects - bright white particles rising
        for (int i = 0; i < 500; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = level.random.nextDouble() * 20;
            level.sendParticles(
                ParticleTypes.END_ROD,
                player.getX() + Math.cos(angle) * radius,
                player.getY() + level.random.nextDouble() * 30,
                player.getZ() + Math.sin(angle) * radius,
                1, 0, 0.5, 0, 0.02);
        }
        
        // Positive effects
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 6000, 2));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 6000, 1));
        player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 12000, 0));
        
        // Sound
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.MASTER, 2.0f, 1.0f);
    }
    
    /**
     * Dark Ending: keep the hour.
     */
    private static void executeDarkEnding(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        
        // Display ending text
        displayEndingText(player, Ending.DARK, new String[]{
            "",
            "══════════════════════════════════════════",
            "          ⚫ KEEP THE HOUR ⚫",
            "══════════════════════════════════════════",
            "",
            "The crown waited for a head. It found yours.",
            "",
            "The Overseer asked for one more night.",
            "You asked for all of them.",
            "",
            "The Rift is yours now, every held hour of it.",
            "Its things will bow. Its doors will open for you.",
            "They will not close again.",
            "",
            "Your falcon's eyes turn violet. It still loves you.",
            "That is the cruel part.",
            "",
            "You took back the hour. You just didn't give it to anyone.",
            "",
            "══════════════════════════════════════════",
            "      ENDING: THE LAST NIGHT",
            "══════════════════════════════════════════"
        });
        
        // Visual effects - dark particles swirling inward
        for (int i = 0; i < 500; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 15 + level.random.nextDouble() * 10;
            level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                player.getX() + Math.cos(angle) * radius,
                player.getY() + level.random.nextDouble() * 10,
                player.getZ() + Math.sin(angle) * radius,
                1, 
                (player.getX() - (player.getX() + Math.cos(angle) * radius)) * 0.05,
                0.1,
                (player.getZ() - (player.getZ() + Math.sin(angle) * radius)) * 0.05,
                0.05);
        }
        
        // Dark power effects
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 2));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12000, 2));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 12000, 0));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 12000, 0));
        
        // Sound
        player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.MASTER, 1.0f, 0.5f);
    }
    
    /**
     * True Ending: braid the hours.
     */
    private static void executeTrueEnding(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        
        // Display ending text
        displayEndingText(player, Ending.TRUE, new String[]{
            "",
            "══════════════════════════════════════════",
            "          ✧ BRAID THE HOURS ✧",
            "══════════════════════════════════════════",
            "",
            "Four relics. A falcon of every kind.",
            "Enough to hold night and morning at once.",
            "",
            "You don't end the night. You don't keep it.",
            "You tie it to the morning like two birds on one jess.",
            "",
            "The Compact waited an age for a stitch in the east.",
            "It is standing here, and it is you.",
            "",
            "Morning comes. The doors stay.",
            "",
            "You took back the hour and tied it to the morning, the way a falconer ties a jess.",
            "",
            "══════════════════════════════════════════",
            "      ENDING: THE BRAIDED HOUR",
            "      ★ SECRET ENDING ★",
            "══════════════════════════════════════════"
        });
        
        // Visual effects - rainbow explosion
        for (int i = 0; i < 1000; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double vertAngle = level.random.nextDouble() * Math.PI;
            double radius = 5 + level.random.nextDouble() * 25;
            
            double x = player.getX() + Math.cos(angle) * Math.sin(vertAngle) * radius;
            double y = player.getY() + Math.cos(vertAngle) * radius + 5;
            double z = player.getZ() + Math.sin(angle) * Math.sin(vertAngle) * radius;
            
            level.sendParticles(
                i % 3 == 0 ? ParticleTypes.END_ROD : 
                i % 3 == 1 ? ParticleTypes.ENCHANT : ParticleTypes.FIREWORK,
                x, y, z,
                1, 0, 0, 0, 0.05);
        }
        
        // Transcendent effects - all positive
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 24000, 3));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 24000, 2));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 24000, 2));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 24000, 2));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 24000, 2));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 24000, 0));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 24000, 0));
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 24000, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 24000, 0));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 6000, 0));
        
        // Sound - combined
        player.playNotifySound(SoundEvents.BEACON_ACTIVATE, SoundSource.MASTER, 2.0f, 1.0f);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.MASTER, 2.0f, 1.5f);
    }
    
    /**
     * Display ending text to player.
     */
    private static void displayEndingText(ServerPlayer player, Ending ending, String[] lines) {
        for (String line : lines) {
            ChatFormatting format = line.contains("═") ? ending.color : 
                                    line.contains("✦") || line.contains("⚫") || line.contains("✧") ? 
                                        ChatFormatting.BOLD : ChatFormatting.GRAY;
            
            if (line.contains("ENDING ACHIEVED") || line.contains("SECRET")) {
                player.displayClientMessage(
                    Component.literal(line).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            } else {
                player.displayClientMessage(
                    Component.literal(line).withStyle(ending.color), false);
            }
        }
    }
    
    // === QUERY METHODS ===
    
    /** Player's chosen ending (persisted), or null. */
    public static Ending getCompletedEnding(net.minecraft.world.entity.player.Player player) {
        String e = PlayerStoryData.get(player).getChosenEnding();
        try { return e == null ? null : Ending.valueOf(e); } catch (IllegalArgumentException ex) { return null; }
    }

    public static boolean hasCompletedEnding(net.minecraft.world.entity.player.Player player) {
        return PlayerStoryData.get(player).hasChosenEnding();
    }

    /** Reset player's ending state (replay / tests). */
    public static void resetStoryProgress(net.minecraft.world.entity.player.Player player) {
        PlayerStoryData d = PlayerStoryData.get(player);
        d.setSovereignDefeated(false);
        d.setChosenEnding(null);
    }

    /** Pieces of the held hour taken back (0..4): story bosses down, or relics held / fused. */
    public static int pieces(ServerPlayer p) {
        var sd = PlayerStoryData.get(p);
        int n = 0;
        for (String b : new String[]{"QUANTUM_OVERSEER", "RIFT_WARDEN", "ENTANGLED_TWINS", "SINGULARITY_SOVEREIGN"}) if (sd.hasDefeatedBoss(b)) n++;
        return Math.max(n, relicCount(p));
    }

    /** Grants every criterion of one of our advancements (for "impossible"-trigger ones awarded from code). */
    public static void grantAdvancement(ServerPlayer player, String path) {
        var adv = player.server.getAdvancements().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, path));
        if (adv == null) return;
        var progress = player.getAdvancements().getOrStartProgress(adv);
        for (String c : progress.getRemainingCriteria()) player.getAdvancements().award(adv, c);
    }
}
