package com.villageevolution.mod;

import com.villageevolution.mod.event.VillageTickHandler;
import com.villageevolution.mod.event.VillagerJoinHandler;
import com.villageevolution.mod.block.ChunkLoaderBlock;
import com.villageevolution.mod.registry.ModBlocks;
import com.villageevolution.mod.registry.ModCreativeTab;
import com.villageevolution.mod.registry.ModItems;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(VillagerEvolutionMod.MOD_ID)
public class VillagerEvolutionMod {

    public static final String MOD_ID = "villageevolution";
    public static final Logger LOGGER = LogManager.getLogger();

    public VillagerEvolutionMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTab.TABS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(new VillageTickHandler());
        MinecraftForge.EVENT_BUS.register(new VillagerJoinHandler());

        LOGGER.info("Village Evolution initialized: villages track civilization state, " +
                "expand automatically, and villagers gather/deliver/build, repair golems, and heal each other.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Drop forced-chunk tickets whose chunk loader block no longer exists.
        event.enqueueWork(() -> ForgeChunkManager.setForcedChunkLoadingCallback(MOD_ID, (level, ticketHelper) ->
                ticketHelper.getBlockTickets().forEach((pos, tickets) -> {
                    if (!(level.getBlockState(pos).getBlock() instanceof ChunkLoaderBlock)) {
                        ticketHelper.removeAllTickets(pos);
                    }
                })));
    }
}
