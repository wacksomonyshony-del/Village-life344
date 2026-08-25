package com.villageevolution.mod.ai;

import com.villageevolution.mod.util.VillagerTaskData;
import com.villageevolution.mod.village.ConstructionProject;
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
 * "Villagers participate in construction". Once a project has all its
 * materials, assigned builders walk to the site and "work" - accumulating
 * labor points with visible hammering effects - until the project is
 * finished, at which point VillageManager places the finished building.
 */
public class ConstructionWorkGoal extends Goal {

    private static final int WORK_INTERVAL = 20;
    private static final int LABOR_PER_TICK = 6;

    private final Villager villager;
    private int workCooldown;

    public ConstructionWorkGoal(Villager villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return VillagerTaskData.getTask(villager) == VillagerTaskData.Task.BUILD;
    }

    @Override
    public boolean canContinueToUse() {
        return VillagerTaskData.getTask(villager) == VillagerTaskData.Task.BUILD;
    }

    @Override
    public void start() {
        workCooldown = 0;
    }

    @Override
    public void stop() {
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(villager.level() instanceof ServerLevel level)) return;

        BlockPos anchor = VillagerTaskData.getVillageAnchor(villager);
        UUID projectId = VillagerTaskData.getProjectId(villager);
        if (anchor == null || projectId == null) {
            VillagerTaskData.setIdle(villager);
            return;
        }

        VillageInstance village = VillageSavedData.get(level).findNear(anchor, VillageManager.SEARCH_RADIUS);
        ConstructionProject project = village != null ? village.findProject(projectId).orElse(null) : null;
        if (village == null || project == null) {
            VillagerTaskData.setIdle(villager);
            return;
        }

        BlockPos site = project.getOrigin();
        villager.getLookControl().setLookAt(site.getX() + 0.5, site.getY() + 1, site.getZ() + 0.5);
        double distSqr = villager.blockPosition().distSqr(site);
        if (distSqr > 4.0 * 4.0) {
            villager.getNavigation().moveTo(site.getX() + 0.5, site.getY(), site.getZ() + 0.5, 0.5D);
            return;
        }

        villager.getNavigation().stop();
        if (--workCooldown <= 0) {
            workCooldown = WORK_INTERVAL;
            project.addLabor(LABOR_PER_TICK);
            level.sendParticles(ParticleTypes.CRIT, site.getX() + 0.5, site.getY() + 1, site.getZ() + 0.5, 4, 1.0, 0.5, 1.0, 0.0);
            level.playSound(null, site, SoundEvents.STONE_HIT, SoundSource.NEUTRAL, 0.5F, 1.0F);
        }

        if (project.isReadyToComplete()) {
            VillageManager.completeProject(level, village, project);
            VillagerTaskData.setIdle(villager);
        }
    }
}
