package com.matt.quantumfalcon.blockentity;

import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Block Entity for Quantum Nest Block.
 * Scans for breeding pairs of falcons nearby and activates the nest.
 */
public class QuantumNestBlockEntity extends BlockEntity {
    private static final BooleanProperty ACTIVE = com.matt.quantumfalcon.block.QuantumNestBlock.ACTIVE;
    private int scanCooldown = 0;
    private boolean hasBreedingPair = false;
    
    public QuantumNestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public static void tick(Level level, BlockPos pos, BlockState state, QuantumNestBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        
        blockEntity.scanCooldown--;
        if (blockEntity.scanCooldown <= 0) {
            blockEntity.scanCooldown = 20; // Scan every second
            
            // Find falcons in love within 8 blocks
            List<QuantumFalconEntity> falcons = level.getEntitiesOfClass(
                QuantumFalconEntity.class, 
                new AABB(pos).inflate(8.0), 
                Animal::isInLove
            );
            
            boolean wasActive = blockEntity.hasBreedingPair;
            blockEntity.hasBreedingPair = falcons.size() >= 2;
            
            // Update block state if breeding pair status changed
            if (wasActive != blockEntity.hasBreedingPair) {
                level.setBlock(pos, state.setValue(ACTIVE, blockEntity.hasBreedingPair), 3);
                blockEntity.setChanged();
            }
        }
    }
    
    public boolean hasBreedingPair() {
        return this.hasBreedingPair;
    }
}
