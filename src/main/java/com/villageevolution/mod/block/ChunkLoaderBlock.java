package com.villageevolution.mod.block;

import com.villageevolution.mod.ModConfigs;
import com.villageevolution.mod.VillagerEvolutionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.world.ForgeChunkManager;

/**
 * Forces a centred square of chunks to stay loaded and ticking for as long as
 * the block exists. Coverage comes from the block's tier, clamped by config.
 *
 * Every ticket in the square is owned by this block's position, so breaking
 * the block releases all of them in one call, and the orphan-cleanup callback
 * in VillagerEvolutionMod can drop the whole set by position.
 */
public class ChunkLoaderBlock extends Block {

    private final ChunkLoaderTier tier;

    public ChunkLoaderBlock(Properties properties, ChunkLoaderTier tier) {
        super(properties);
        this.tier = tier;
    }

    public ChunkLoaderTier getTier() {
        return tier;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            setForced(serverLevel, pos, tier, true);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (level instanceof ServerLevel serverLevel && !newState.is(this)) {
            setForced(serverLevel, pos, tier, false);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** Adds or removes the whole square of forced-chunk tickets owned by this block. */
    public static void setForced(ServerLevel level, BlockPos pos, ChunkLoaderTier tier, boolean add) {
        int radius = ModConfigs.radiusFor(tier);
        ChunkPos centre = new ChunkPos(pos);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ForgeChunkManager.forceChunk(level, VillagerEvolutionMod.MOD_ID, pos,
                        centre.x + dx, centre.z + dz, add, /* ticking = */ true);
            }
        }
    }
}
