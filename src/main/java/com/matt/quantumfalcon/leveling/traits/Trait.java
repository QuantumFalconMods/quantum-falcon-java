package com.matt.quantumfalcon.leveling.traits;

import com.matt.quantumfalcon.leveling.api.ILevelableItem.ItemCategory;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;

/**
 * Base class for all traits/abilities.
 * Traits can modify stats, trigger on events, and provide passive effects.
 */
public abstract class Trait {

    protected final ResourceLocation id;
    protected final String displayName;
    protected final String description;
    protected int unlockLevel;
    protected final Set<ItemCategory> validCategories;
    protected final int maxLevel;
    protected final boolean isRarityTrait;

    /**
     * Create a trait that unlocks at a specific level
     */
    protected Trait(ResourceLocation id, String displayName, String description,
                    int unlockLevel, Set<ItemCategory> validCategories, int maxLevel) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.unlockLevel = unlockLevel;
        this.validCategories = validCategories;
        this.maxLevel = maxLevel;
        this.isRarityTrait = false;
    }

    /**
     * Create a rarity-granted trait (level 0)
     */
    protected Trait(ResourceLocation id, String displayName, String description,
                    Set<ItemCategory> validCategories) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.unlockLevel = 0;
        this.validCategories = validCategories;
        this.maxLevel = 1;
        this.isRarityTrait = true;
    }

    // ==================== GETTERS ====================

    public ResourceLocation getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getUnlockLevel() {
        return unlockLevel;
    }

    public Set<ItemCategory> getValidCategories() {
        return validCategories;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean isRarityTrait() {
        return isRarityTrait;
    }

    /**
     * Check if this trait can be applied to an item category
     */
    public boolean isValidFor(ItemCategory category) {
        return validCategories.contains(category);
    }

    // ==================== EVENT HOOKS ====================
    // Override these in subclasses to implement trait behavior

    /**
     * Called before damage is dealt when player attacks.
     * @return modified damage amount
     */
    public float onAttack(Player player, LivingEntity target, ItemStack weapon,
                          float damage, ItemLevelData data) {
        return damage;
    }

    /**
     * Called after damage calculation, before it's applied.
     * @return modified damage amount
     */
    public float onDamageCalculation(Player player, LivingEntity target, ItemStack weapon,
                                     float damage, ItemLevelData data) {
        return damage;
    }

    /**
     * Called after damage is dealt to the target.
     */
    public void onDamageDealt(Player player, LivingEntity target, ItemStack weapon,
                              float damage, ItemLevelData data) {
        // Override in subclass
    }

    /**
     * Called when player kills an entity.
     */
    public void onKill(Player player, LivingEntity killed, ItemStack weapon,
                       ItemLevelData data) {
        // Override in subclass
    }

    /**
     * Called when player takes damage (armor traits).
     * @return modified damage amount
     */
    public float onDamageTaken(Player player, DamageSource source, ItemStack armor,
                               float damage, ItemLevelData data) {
        return damage;
    }

    /**
     * Called after player takes damage.
     */
    public void onHurt(Player player, DamageSource source, ItemStack armor,
                       float damage, ItemLevelData data) {
        // Override in subclass
    }

    /**
     * Called every tick while item is equipped/held.
     * @param isSelected true if in main hand (weapons) or being worn (armor)
     */
    public void onTick(Player player, ItemStack stack, ItemLevelData data, boolean isSelected) {
        // Override in subclass
    }

    /**
     * Called when item is equipped (armor) or selected (weapon).
     */
    public void onEquip(Player player, ItemStack stack, ItemLevelData data) {
        // Override in subclass
    }

    /**
     * Called when item is unequipped or deselected.
     */
    public void onUnequip(Player player, ItemStack stack, ItemLevelData data) {
        // Override in subclass
    }

    /**
     * Called when a block is broken with this tool.
     */
    public void onBlockBreak(Player player, ItemStack tool, BlockState state,
                             ItemLevelData data) {
        // Override in subclass
    }

    /**
     * Called when the item is used (right-click).
     */
    public void onItemUse(Player player, ItemStack stack, ItemLevelData data) {
        // Override in subclass
    }

    /**
     * Called when player blocks with a shield.
     * @return modified blocked damage
     */
    public float onBlock(Player player, ItemStack shield, float blockedDamage,
                         DamageSource source, ItemLevelData data) {
        return blockedDamage;
    }

    /**
     * Called when a critical hit occurs.
     * @return modified critical damage multiplier
     */
    public float onCriticalHit(Player player, LivingEntity target, ItemStack weapon,
                               float critMultiplier, ItemLevelData data) {
        return critMultiplier;
    }

    /**
     * Check if this trait should prevent item from breaking.
     */
    public boolean preventsBreaking(ItemStack stack, ItemLevelData data) {
        return false;
    }

    /**
     * Get bonus XP multiplier from this trait.
     */
    public float getXpMultiplier(ItemStack stack, ItemLevelData data) {
        return 1.0f;
    }

    /**
     * Optional: Add tooltip lines for this trait
     */
    public void appendTooltip(ItemStack stack, ItemLevelData data, List<Component> tooltip) {
        // Override in subclass if needed
    }

    // ==================== DISPLAY ====================

    /**
     * Get formatted tooltip line for this trait.
     */
    public String getTooltipLine() {
        return "§d✧ " + displayName + " §7(" + description + ")";
    }

    /**
     * Get short description for compact display.
     */
    public String getShortDescription() {
        return description;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Trait other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Trait[" + id + "]";
    }
}
