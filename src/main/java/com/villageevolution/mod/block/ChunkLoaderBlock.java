package com.villageevolution.mod.block;

import com.villageevolution.mod.VillagerEvolutionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.world.ForgeChunkManager;

/**
 * Keeps its own chunk loaded (and ticking) for as long as it exists, using
 * Forge's block-owned chunk tickets. The ticket is registered when the block
 * is placed and released when it is broken, and survives a server restart -
 * ForgeChunkManager persists block tickets in the level's forced-chunk data.
 *
 * VillagerEvolutionMod#validateChunkTickets cleans up orphaned tickets on
 * world load, covering the case where the block was removed by something
 * that bypassed onRemove (world edits, chunk deletion, etc).
 */
public class ChunkLoaderBlock extends Block {

    public ChunkLoaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            setForced(serverLevel, pos, true);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (level instanceof ServerLevel serverLevel && !newState.is(this)) {
            setForced(serverLevel, pos, false);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** Adds or removes this block's forced-chunk ticket. */
    public static void setForced(ServerLevel level, BlockPos pos, boolean add) {
        ChunkPos chunkPos = new ChunkPos(pos);
        ForgeChunkManager.forceChunk(level, VillagerEvolutionMod.MOD_ID, pos,
                chunkPos.x, chunkPos.z, add, /* ticking = */ true);
    }
}
