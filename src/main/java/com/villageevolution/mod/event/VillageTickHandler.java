package com.villageevolution.mod.event;

import com.villageevolution.mod.util.VillagerInjuryHelper;
import com.villageevolution.mod.util.VillagerLabelHelper;
import com.villageevolution.mod.village.VillageInstance;
import com.villageevolution.mod.village.VillageManager;
import com.villageevolution.mod.village.VillageSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public class VillageTickHandler {

    /** Base cadence; growth/project checks are further gated by their own intervals inside VillageManager. */
    private static final int CHECK_INTERVAL = 100;
    private static final int INJURY_PARTICLE_INTERVAL = 40;
    /** How often the floating task labels are refreshed (1 second). */
    private static final int LABEL_INTERVAL = 20;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // ServerLifecycleHooks is the long-stable way to get the running server from any
        // event context, avoiding any uncertainty about which TickEvent fields/getters
        // exist on a given Forge build.
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        int tick = server.getTickCount();
        boolean doVillageWork = tick % CHECK_INTERVAL == 0;
        boolean doLabels = tick % LABEL_INTERVAL == 0;
        if (!doVillageWork && !doLabels) return;

        for (ServerLevel level : server.getAllLevels()) {
            VillageSavedData data = VillageSavedData.get(level);
            long gameTime = level.getGameTime();
            for (VillageInstance village : data.getVillages()) {
                AABB area = new AABB(village.getAnchor()).inflate(VillageManager.SEARCH_RADIUS);

                if (doVillageWork) {
                    VillageManager.tickGrowth(level, village, gameTime);
                    VillageManager.tickProjects(level, village, gameTime);

                    if (tick % INJURY_PARTICLE_INTERVAL == 0) {
                        for (Villager villager : level.getEntitiesOfClass(Villager.class, area)) {
                            VillagerInjuryHelper.showInjuryParticles(villager);
                        }
                    }
                }

                if (doLabels) {
                    for (Villager villager : level.getEntitiesOfClass(Villager.class, area)) {
                        VillagerLabelHelper.update(level, villager);
                    }
                }
            }
        }
    }
}
