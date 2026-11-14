/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.Rarity
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.ChestBlockEntity
 */
package com.matt.quantumfalcon.item.special;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public class LockpickItem
extends Item {
    public LockpickItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    }

    public InteractionResult useOn(UseOnContext context) {
        ChestBlockEntity chest;
        CompoundTag nbt;
        BlockEntity blockEntity;
        Level level = context.getLevel();
        if (!level.isClientSide() && (blockEntity = level.getBlockEntity(context.getClickedPos())) instanceof ChestBlockEntity && (nbt = (chest = (ChestBlockEntity)blockEntity).getPersistentData()).contains("Lock")) {
            nbt.remove("Lock");
            context.getPlayer().displayClientMessage((Component)Component.literal((String)"*Click* Chest unlocked!").withStyle(ChatFormatting.GREEN), true);
            context.getItemInHand().shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
