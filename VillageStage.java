package com.villageevolution.mod.event;

import com.villageevolution.mod.ai.ConstructionWorkGoal;
import com.villageevolution.mod.ai.DeliverMaterialsGoal;
import com.villageevolution.mod.ai.FarmerContributeFoodGoal;
import com.villageevolution.mod.ai.GatherMaterialsGoal;
import com.villageevolution.mod.ai.HealWoundedVillagerGoal;
import com.villageevolution.mod.ai.RepairIronGolemGoal;
import com.villageevolution.mod.util.GoalAccessHelper;
import com.villageevolution.mod.village.VillageManager;
import com.villageevolution.mod.village.VillageSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Equips every villager with the mod's "advanced API": individual task
 * goals (gather/deliver/build), golem repair, and (profession-gated)
 * cleric healing + farmer food contribution.
 *
 * Goal priorities (lower number = checked first): clerics healing the
 * wounded outranks everything, since a dying villager is more urgent than
 * a construction job; golem repair and construction work come next;
 * gathering/delivering/food are lowest priority so they yield to the above.
 */
public class VillagerJoinHandler {

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        VillageSavedData data = VillageSavedData.get(serverLevel);
        data.findOrCreate(villager.blockPosition(), VillageManager.SEARCH_RADIUS);

        // Universal caretaking + civilization-building behaviors.
        GoalAccessHelper.addGoal(villager, 6, new RepairIronGolemGoal(villager));
        GoalAccessHelper.addGoal(villager, 7, new ConstructionWorkGoal(villager));
        GoalAccessHelper.addGoal(villager, 8, new DeliverMaterialsGoal(villager));
        GoalAccessHelper.addGoal(villager, 9, new GatherMaterialsGoal(villager));

        // Profession-specific responsibilities.
        // NOTE: if getProfession() ends up returning a Holder<VillagerProfession> on your
        // exact build, compare with `.value() == VillagerProfession.X` instead.
        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.CLERIC) {
            GoalAccessHelper.addGoal(villager, 5, new HealWoundedVillagerGoal(villager));
        }
        if (profession == VillagerProfession.FARMER) {
            GoalAccessHelper.addGoal(villager, 10, new FarmerContributeFoodGoal(villager));
        }
    }
}
