package com.villageevolution.mod.registry;

import com.villageevolution.mod.VillagerEvolutionMod;
import com.villageevolution.mod.item.BlueprintItem;
import com.villageevolution.mod.village.BuildingType;
import com.villageevolution.mod.block.ChunkLoaderTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/** Registers one blueprint item per building type. */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, VillagerEvolutionMod.MOD_ID);

    private static final Map<BuildingType, RegistryObject<Item>> BLUEPRINTS = new EnumMap<>(BuildingType.class);

    /** Item forms of the eight chunk loader blocks. */
    private static final Map<ChunkLoaderTier, RegistryObject<Item>> CHUNK_LOADERS =
            new EnumMap<>(ChunkLoaderTier.class);

    static {
        for (ChunkLoaderTier tier : ChunkLoaderTier.values()) {
            CHUNK_LOADERS.put(tier, ITEMS.register(tier.registryName(),
                    () -> new BlockItem(ModBlocks.chunkLoader(tier).get(), new Item.Properties())));
        }
    }

    public static RegistryObject<Item> chunkLoader(ChunkLoaderTier tier) {
        return CHUNK_LOADERS.get(tier);
    }

    static {
        for (BuildingType type : BuildingType.values()) {
            String id = "blueprint_" + type.name().toLowerCase();
            RegistryObject<Item> item = ITEMS.register(id,
                    () -> new BlueprintItem(type, new Item.Properties().stacksTo(16)));
            BLUEPRINTS.put(type, item);
        }
    }

    private ModItems() {}

    public static RegistryObject<Item> blueprint(BuildingType type) {
        return BLUEPRINTS.get(type);
    }

    public static Map<BuildingType, RegistryObject<Item>> allBlueprints() {
        return BLUEPRINTS;
    }
}
