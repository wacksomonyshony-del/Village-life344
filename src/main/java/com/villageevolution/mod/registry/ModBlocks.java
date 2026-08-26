package com.villageevolution.mod.registry;

import com.villageevolution.mod.VillagerEvolutionMod;
import com.villageevolution.mod.block.ChunkLoaderBlock;
import com.villageevolution.mod.block.ChunkLoaderTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/** Blocks this mod adds. */
public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, VillagerEvolutionMod.MOD_ID);

    private static final Map<ChunkLoaderTier, RegistryObject<Block>> CHUNK_LOADERS =
            new EnumMap<>(ChunkLoaderTier.class);

    static {
        for (ChunkLoaderTier tier : ChunkLoaderTier.values()) {
            // Higher tiers are progressively tougher, matching their cost.
            float hardness = 3.0F + tier.ordinal();
            CHUNK_LOADERS.put(tier, BLOCKS.register(tier.registryName(),
                    () -> new ChunkLoaderBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(hardness, hardness * 2.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL), tier)));
        }
    }

    public static RegistryObject<Block> chunkLoader(ChunkLoaderTier tier) {
        return CHUNK_LOADERS.get(tier);
    }

    private ModBlocks() {}
}
