package com.villageevolution.mod.ai;

import com.villageevolution.mod.util.VillagerTaskData;
import com.villageevolution.mod.village.ConstructionProject;
import com.villageevolution.mod.village.ResourceType;
import com.villageevolution.mod.village.VillageInstance;
import com.villageevolution.mod.village.VillageManager;
import com.villageevolution.mod.village.VillageSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;

import java.util.EnumSet;
import java.util.UUID;

/**
 * "Villagers can deliver materials". Carries whatever GatherMaterialsGoal
 * picked up back to the construction site and hands it over to the project.
 * If the project still needs more of that resource, the villager loops back
 * into gathering; once materials are complete they free up to become a
 * builder instead.
 */
public class DeliverMaterialsGoal extends Goal {

    private final Villager villager;
    private BlockPos destination;

    public DeliverMaterialsGoal(Villager villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return VillagerTaskData.getTask(villager) == VillagerTaskData.Task.DELIVER
                && VillagerTaskData.getCarryingAmount(villager) > 0;
    }

    @Override
    public boolean canContinueToUse() {
        return VillagerTaskData.getTask(villager) == VillagerTaskData.Task.DELIVER;
    }

    @Override
    public void start() {
        destination = VillagerTaskData.getVillageAnchor(villager);
    }

    @Override
    public void stop() {
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(villager.level() instanceof ServerLevel level) || destination == null) {
            VillagerTaskData.setIdle(villager);
            return;
        }

        UUID projectId = VillagerTaskData.getProjectId(villager);
        VillageInstance village = VillageSavedData.get(level).findNear(destination, VillageManager.SEARCH_RADIUS);
        ConstructionProject project = village != null && projectId != null
                ? village.findProject(projectId).orElse(null) : null;

        BlockPos target = project != null ? project.getOrigin() : destination;
        double distSqr = villager.blockPosition().distSqr(target);
        if (distSqr > 2.5 * 2.5) {
            villager.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5D);
            return;
        }

        villager.getNavigation().stop();

        String resourceName = VillagerTaskData.getCarryingResource(villager);
        int amount = VillagerTaskData.getCarryingAmount(villager);
        if (project != null && resourceName != null && !resourceName.isEmpty()) {
            try {
                ResourceType type = ResourceType.valueOf(resourceName);
                project.deliver(type, amount);
                village.getStatistics().addMaterialsGathered(amount);
            } catch (IllegalArgumentException ignored) {
                // stale/unknown resource tag - just drop the carry
            }
        }
        VillagerTaskData.clearCarrying(villager);

        level.sendParticles(ParticleTypes.CRIT, villager.getX(), villager.getY() + 1, villager.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, villager.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.NEUTRAL, 0.5F, 1.1F);

        if (project != null && project.needsMaterials() && project.getAssignedGatherers().contains(villager.getUUID())) {
            VillagerTaskData.assignGather(villager, destination, project.getId());
        } else {
            if (project != null) project.getAssignedGatherers().remove(villager.getUUID());
            VillagerTaskData.setIdle(villager);
        }
    }
}
