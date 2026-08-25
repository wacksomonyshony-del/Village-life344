package com.villageevolution.mod;

import com.villageevolution.mod.event.VillageTickHandler;
import com.villageevolution.mod.event.VillagerJoinHandler;
import com.villageevolution.mod.registry.ModCreativeTab;
import com.villageevolution.mod.registry.ModItems;
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

        ModItems.ITEMS.register(modEventBus);
        ModCreativeTab.TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(new VillageTickHandler());
        MinecraftForge.EVENT_BUS.register(new VillagerJoinHandler());

        LOGGER.info("Village Evolution initialized: villages track civilization state, " +
                "expand automatically, and villagers gather/deliver/build, repair golems, and heal each other.");
    }
}
