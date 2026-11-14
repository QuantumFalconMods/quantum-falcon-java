package com.matt.quantumfalcon.capability;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Player Instability Capability for Quantum Falcon.
 * Tracks instability level for dimension effects.
 */
public class PlayerInstabilityCapability implements INBTSerializable<CompoundTag> {

    // ============ Attachment Registration ============
    
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, QuantumFalcon.MOD_ID);

    public static final Supplier<AttachmentType<PlayerInstabilityCapability>> INSTABILITY_ATTACHMENT =
        ATTACHMENT_TYPES.register("player_instability", () ->
            AttachmentType.builder(PlayerInstabilityCapability::new)
                .serialize(new IAttachmentSerializer<CompoundTag, PlayerInstabilityCapability>() {
                    @Override
                    public PlayerInstabilityCapability read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                        PlayerInstabilityCapability cap = new PlayerInstabilityCapability();
                        cap.deserializeNBT(provider, tag);
                        return cap;
                    }

                    @Override
                    public @Nullable CompoundTag write(PlayerInstabilityCapability attachment, HolderLookup.Provider provider) {
                        return attachment.serializeNBT(provider);
                    }
                })
                .copyOnDeath()
                .build()
        );

    // ============ Data Fields ============
    
    private float instability = 0.0f;
    private int currentTier = 0;
    private long lastUpdateTick = 0;
    private boolean hasStabilizer = false;
    // the zone tier is measured from where the player arrived in the Rift (their portal), not from 0,0
    private boolean hasAnchor = false;
    private int anchorX, anchorZ;
    /** Game ticks of no gain left (after a respawn or arriving); counts game ticks so it never runs while paused. */
    private int graceTicks = 0;
    /** Game time until which a Quantum Stabilizer holds instability at zero gain. */
    private long stabilizedUntil = 0L;

    // ============ Constructors ============
    
    public PlayerInstabilityCapability() {
    }

    // ============ Static Access ============
    
    /**
     * Get instability capability for a player
     */
    public static PlayerInstabilityCapability get(Player player) {
        return player.getData(INSTABILITY_ATTACHMENT.get());
    }

    // ============ Getters/Setters ============
    
    public float getInstability() {
        return instability;
    }

    public void setInstability(float value) {
        this.instability = Math.max(0, Math.min(100, value));
    }

    public void addInstability(float amount) {
        setInstability(this.instability + amount);
    }

    public void reduceInstability(float amount) {
        setInstability(this.instability - amount);
    }

    public int getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(int tier) {
        this.currentTier = tier;
    }

    public long getLastUpdateTick() {
        return lastUpdateTick;
    }

    public void setLastUpdateTick(long tick) {
        this.lastUpdateTick = tick;
    }

    public boolean hasStabilizer() {
        return hasStabilizer;
    }

    public void setHasStabilizer(boolean value) {
        this.hasStabilizer = value;
    }

    public boolean hasAnchor() { return hasAnchor; }
    public int getAnchorX() { return anchorX; }
    public int getAnchorZ() { return anchorZ; }
    public void setAnchor(int x, int z) { this.anchorX = x; this.anchorZ = z; this.hasAnchor = true; }
    public void clearAnchor() { this.hasAnchor = false; }
    public int getGraceTicks() { return graceTicks; }
    public void setGraceTicks(int t) { this.graceTicks = Math.max(0, t); }
    public long getStabilizedUntil() { return stabilizedUntil; }
    public void setStabilizedUntil(long t) { this.stabilizedUntil = t; }

    // ============ Utility Methods ============
    
    /**
     * Get instability percentage (0-100)
     */
    public int getInstabilityPercent() {
        return Math.round(instability);
    }

    /**
     * Check if instability is critical
     */
    public boolean isCritical() {
        return instability >= 75.0f;
    }

    /**
     * Check if instability is dangerous
     */
    public boolean isWarning() {
        return instability >= 50.0f;
    }

    /**
     * Reset instability to zero
     */
    public void reset() {
        this.instability = 0.0f;
        this.currentTier = 0;
    }

    // ============ NBT Serialization ============
    
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("instability", instability);
        tag.putInt("tier", currentTier);
        tag.putLong("lastUpdate", lastUpdateTick);
        tag.putBoolean("hasStabilizer", hasStabilizer);
        tag.putBoolean("hasAnchor", hasAnchor);
        tag.putInt("anchorX", anchorX);
        tag.putInt("anchorZ", anchorZ);
        tag.putInt("grace", graceTicks);
        tag.putLong("stabilizedUntil", stabilizedUntil);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.instability = tag.getFloat("instability");
        this.currentTier = tag.getInt("tier");
        this.lastUpdateTick = tag.getLong("lastUpdate");
        this.hasStabilizer = tag.getBoolean("hasStabilizer");
        this.hasAnchor = tag.getBoolean("hasAnchor");
        this.anchorX = tag.getInt("anchorX");
        this.anchorZ = tag.getInt("anchorZ");
        this.graceTicks = tag.getInt("grace");
        this.stabilizedUntil = tag.getLong("stabilizedUntil");
    }
}
