package com.villageevolution.mod.registry;

import com.villageevolution.mod.VillagerEvolutionMod;
import com.villageevolution.mod.village.BuildingType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** A dedicated creative-inventory tab holding every item this mod adds. */
public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VillagerEvolutionMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> VILLAGE_EVOLUTION_TAB = TABS.register("village_evolution",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.villageevolution"))
                    .icon(() -> new ItemStack(ModItems.blueprint(BuildingType.TOWN_HALL).get()))
                    .displayItems((parameters, output) -> {
                        for (BuildingType type : BuildingType.values()) {
                            output.accept(ModItems.blueprint(type).get());
                        }
                        output.accept(ModItems.CHUNK_LOADER.get());
                    })
                    .build());

    private ModCreativeTab() {}
}
