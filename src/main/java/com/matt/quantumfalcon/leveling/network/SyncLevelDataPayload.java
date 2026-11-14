package com.matt.quantumfalcon.leveling.network;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.leveling.core.ItemLevelData;
import com.matt.quantumfalcon.leveling.core.ItemLevelHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Syncs item level data from server to client.
 * Used when item data changes and client needs to update display.
 */
public record SyncLevelDataPayload(
    int slotId,
    CompoundTag levelData
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncLevelDataPayload> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "sync_level_data"));

    public static final StreamCodec<FriendlyByteBuf, SyncLevelDataPayload> CODEC = StreamCodec.of(
        SyncLevelDataPayload::write,
        SyncLevelDataPayload::read
    );

    private static SyncLevelDataPayload read(FriendlyByteBuf buf) {
        int slotId = buf.readVarInt();
        CompoundTag data = buf.readNbt();
        return new SyncLevelDataPayload(slotId, data != null ? data : new CompoundTag());
    }

    private static void write(FriendlyByteBuf buf, SyncLevelDataPayload payload) {
        buf.writeVarInt(payload.slotId);
        buf.writeNbt(payload.levelData);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handle packet on client side
     */
    public static void handle(SyncLevelDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            // Get the item in the specified slot
            ItemStack stack = player.getInventory().getItem(payload.slotId);
            if (stack.isEmpty()) return;

            // Update the level data using DataComponents API (1.21.x)
            if (!payload.levelData.isEmpty()) {
                CustomData existingData = stack.get(DataComponents.CUSTOM_DATA);
                CompoundTag tag = existingData != null ? existingData.copyTag() : new CompoundTag();
                tag.put(ItemLevelData.TAG_ROOT, payload.levelData);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        });
    }

    /**
     * Create a sync payload for an item in a specific slot
     */
    public static SyncLevelDataPayload create(int slotId, ItemStack stack) {
        ItemLevelData data = ItemLevelHelper.getLevelData(stack);
        CompoundTag tag = data != null ? data.save() : new CompoundTag();
        return new SyncLevelDataPayload(slotId, tag);
    }
}
