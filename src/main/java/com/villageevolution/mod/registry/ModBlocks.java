package com.villageevolution.mod.registry;

import com.villageevolution.mod.VillagerEvolutionMod;
import com.villageevolution.mod.block.ChunkLoaderBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Blocks this mod adds. */
public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, VillagerEvolutionMod.MOD_ID);

    public static final RegistryObject<Block> CHUNK_LOADER = BLOCKS.register("chunk_loader",
            () -> new ChunkLoaderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    private ModBlocks() {}
}
