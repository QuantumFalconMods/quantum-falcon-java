/*
 * Inspired by qCraft's linked-block entanglement; reimplemented for QuantumFalcon.
 * Nodes share a network ID; mining one triggers cascade effects and consolidated drops.
 */
package com.matt.quantumfalcon.blockentity;

import com.matt.quantumfalcon.registry.ModParticles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class EntangledOreBlockEntity
extends BlockEntity {
    private UUID networkId = null;
    private int networkStrength = 0;
    private static final int MAX_LINKS = 16;
    private static final Map<UUID, EntangledNetwork> GLOBAL_NETWORKS = new ConcurrentHashMap<UUID, EntangledNetwork>();

    public EntangledOreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean linkToNetwork(UUID networkId) {
        if (this.networkId != null) {
            return false;
        }
        EntangledNetwork network = GLOBAL_NETWORKS.computeIfAbsent(networkId, EntangledNetwork::new);
        if (network.getNodeCount() >= 16) {
            return false;
        }
        this.networkId = networkId;
        network.addNode(this);
        this.calculateNetworkStrength();
        this.setChanged();
        return true;
    }

    public void unlinkFromNetwork() {
        if (this.networkId != null) {
            EntangledNetwork network = GLOBAL_NETWORKS.get(this.networkId);
            if (network != null) {
                network.removeNode(this);
            }
            this.networkId = null;
            this.setChanged();
        }
    }

    private void calculateNetworkStrength() {
        if (this.networkId == null) {
            this.networkStrength = 0;
            return;
        }
        EntangledNetwork network = GLOBAL_NETWORKS.get(this.networkId);
        if (network == null) {
            this.networkStrength = 0;
            return;
        }
        this.networkStrength = Math.min(100, network.getNodeCount() * 5);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            this.renderNetworkVisualization();
            return;
        }
        if (level.getGameTime() % 100L == 0L) {
            this.calculateNetworkStrength();
        }
    }

    private void renderNetworkVisualization() {
        if (this.networkId == null || this.level == null) {
            return;
        }
        float pulse = (float)Math.sin((double)this.level.getGameTime() * 0.1) * 0.5f + 0.5f;
        this.level.addParticle((ParticleOptions)ModParticles.QUANTUM_SPARK.get(), (double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5, 0.0, 0.02 * (double)pulse, 0.0);
    }

    private static void spawnNetworkBreakEffect(Level level, BlockPos pos) {
        int i = 0;
        while (i < 20) {
            level.addParticle((ParticleOptions)ModParticles.QUANTUM_SPARK.get(), (double)pos.getX() + 0.5 + (level.random.nextDouble() - 0.5), (double)pos.getY() + 0.5 + (level.random.nextDouble() - 0.5), (double)pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5), (level.random.nextDouble() - 0.5) * 0.2, level.random.nextDouble() * 0.2, (level.random.nextDouble() - 0.5) * 0.2);
            ++i;
        }
    }

    private static void spawnNetworkCollapseEffect(Level level, BlockPos pos) {
        int i = 0;
        while (i < 100) {
            level.addParticle((ParticleOptions)ParticleTypes.REVERSE_PORTAL, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, (level.random.nextDouble() - 0.5) * 0.5, level.random.nextDouble() * 0.5, (level.random.nextDouble() - 0.5) * 0.5);
            ++i;
        }
        level.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.BLOCKS, 1.0f, 1.5f);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.networkId != null) {
            tag.putUUID("NetworkId", this.networkId);
        }
        tag.putInt("NetworkStrength", this.networkStrength);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("NetworkId")) {
            this.networkId = tag.getUUID("NetworkId");
        }
        this.networkStrength = tag.getInt("NetworkStrength");
    }

    /** Rejoin global network on chunk load so linked-block state stays in sync across dimensions. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (networkId != null && level != null && !level.isClientSide()) {
            EntangledNetwork net = GLOBAL_NETWORKS.computeIfAbsent(networkId, EntangledNetwork::new);
            if (net.getNodeCount() < MAX_LINKS || net.nodes.containsKey(nodeKey(this))) {
                net.addNode(this);
                calculateNetworkStrength();
            }
        }
    }

    /** Set when the chunk unloads (NeoForge calls this before setRemoved): unloading is not breaking the ore. */
    private boolean chunkUnloading;

    @Override
    public void onChunkUnloaded() {
        chunkUnloading = true;
        // leave the in-memory network by position (no setChanged: the chunk is mid-save) and keep networkId, so the
        // reload re-adds this same position instead of stacking a second object for it
        if (networkId != null && level != null && !level.isClientSide()) {
            EntangledNetwork net = GLOBAL_NETWORKS.get(networkId);
            if (net != null) net.removeAt(nodeKey(this));
        }
        super.onChunkUnloaded();
    }

    static String nodeKey(EntangledOreBlockEntity be) {
        return (be.level == null ? "?" : be.level.dimension().location().toString()) + "|" + be.worldPosition.asLong();
    }

    @Override
    public void setRemoved() {
        // Only a real removal leaves the network. Unlinking on unload marked the chunk dirty while it was being saved
        // (setChanged), which kept ChunkMap rescheduling the unload forever on server stop/relog, and also dropped the
        // saved network id.
        if (!chunkUnloading) unlinkFromNetwork();
        super.setRemoved();
    }

    public UUID getNetworkId() {
        return this.networkId;
    }

    public int getNetworkStrength() {
        return this.networkStrength;
    }

    public static EntangledNetwork getNetwork(UUID networkId) {
        return GLOBAL_NETWORKS.get(networkId);
    }

    public static class EntangledNetwork {
        private final UUID networkId;
        /** keyed by dimension + position, so a reloaded block entity replaces its old object instead of adding a node. */
        private final Map<String, EntangledOreBlockEntity> nodes = new java.util.LinkedHashMap<>();
        private int totalYield = 0;

        public EntangledNetwork(UUID id) {
            this.networkId = id;
        }

        public void addNode(EntangledOreBlockEntity node) {
            this.nodes.put(nodeKey(node), node);
            this.calculateTotalYield();
        }

        public void removeNode(EntangledOreBlockEntity node) {
            removeAt(nodeKey(node));
        }

        public void removeAt(String key) {
            this.nodes.remove(key);
            this.calculateTotalYield();
            if (this.nodes.isEmpty()) {
                GLOBAL_NETWORKS.remove(this.networkId);
            }
        }

        public int getTotalYield() { return totalYield; }

        private void calculateTotalYield() {
            this.totalYield = this.nodes.size();
            HashSet dimensions = new HashSet();
            this.nodes.values().forEach(node -> {
                if (((EntangledOreBlockEntity)node).level != null) {
                    dimensions.add(((EntangledOreBlockEntity)node).level);
                }
            });
            this.totalYield += dimensions.size() * 2;
        }

        public void onNodeMined(Level level, BlockPos pos, Player player) {
            List<ItemStack> allDrops = new ArrayList<>();
            for (EntangledOreBlockEntity node : new ArrayList<>(this.nodes.values())) {
                if (node.isRemoved() || node.level == null) continue;
                BlockState state = node.level.getBlockState(node.worldPosition);
                List<ItemStack> drops = Block.getDrops(state, (ServerLevel) node.level, node.worldPosition, null, player, player.getMainHandItem());
                allDrops.addAll(drops);
                node.level.setBlock(node.worldPosition, Blocks.AIR.defaultBlockState(), 3);
                EntangledOreBlockEntity.spawnNetworkBreakEffect(node.level, node.worldPosition);
                node.level.playSound(null, node.worldPosition, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            float multiplier = 1.0f + (float) this.totalYield / 10.0f;
            for (ItemStack stack : allDrops) {
                int count = (int) ((float) stack.getCount() * multiplier);
                ItemStack multipliedStack = stack.copy();
                multipliedStack.setCount(count);
                Block.popResource(level, pos, multipliedStack);
            }
            EntangledOreBlockEntity.spawnNetworkCollapseEffect(level, pos);
            this.nodes.clear();
            GLOBAL_NETWORKS.remove(this.networkId);
        }

        public int getNodeCount() {
            return this.nodes.size();
        }
    }
}
