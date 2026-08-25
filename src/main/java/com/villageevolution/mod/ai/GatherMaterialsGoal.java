package com.villageevolution.mod.ai;

import com.villageevolution.mod.ModSettings;
import com.villageevolution.mod.util.VillagerTaskData;
import com.villageevolution.mod.village.ConstructionProject;
import com.villageevolution.mod.village.ResourceType;
import com.villageevolution.mod.village.VillageInstance;
import com.villageevolution.mod.village.VillageManager;
import com.villageevolution.mod.village.VillageSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * "Villagers can gather construction materials". Assigned by VillageManager
 * when a construction project needs more resources. Finds a nearby matching
 * block (logs for wood, stone-family blocks for stone, ore for iron), mines
 * it, and hands off to DeliverMaterialsGoal to bring it back.
 */
public class GatherMaterialsGoal extends Goal {

    private static final int SCAN_RADIUS = 12;
    private static final int GIVE_UP_TICKS = 400;

    /** Small local pairing so we don't depend on vanilla util.Tuple's exact accessor names. */
    private record ProjectLookup(VillageInstance village, ConstructionProject project) {}

    private final Villager villager;
    private BlockPos targetBlock;
    private ResourceType targetResource;
    private int ticksActive;

    public GatherMaterialsGoal(Villager villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // With creative materials on there is nothing to gather, so villagers
        // never go out and mine the landscape.
        if (ModSettings.CREATIVE_MATERIALS) return false;
        if (villager.isBaby()) return false;
        if (VillagerTaskData.getTask(villager) != VillagerTaskData.Task.GATHER) return false;
        return findProject().isPresent();
    }

    @Override
    public boolean canContinueToUse() {
        return VillagerTaskData.getTask(villager) == VillagerTaskData.Task.GATHER && ticksActive < GIVE_UP_TICKS;
    }

    @Override
    public void start() {
        ticksActive = 0;
        targetBlock = null;
        targetResource = pickNeededResource().orElse(null);
    }

    @Override
    public void stop() {
        villager.getNavigation().stop();
        targetBlock = null;
    }

    @Override
    public void tick() {
        ticksActive++;
        if (!(villager.level() instanceof ServerLevel level)) return;

        if (targetResource == null) {
            targetResource = pickNeededResource().orElse(null);
            if (targetResource == null) {
                giveUp();
                return;
            }
        }

        if (targetBlock == null || !matches(level.getBlockState(targetBlock), targetResource)) {
            targetBlock = findNearestMatch(level, targetResource).orElse(null);
            if (targetBlock == null) {
                if (ticksActive > 100) giveUp(); // nothing to gather nearby; free the slot
                return;
            }
        }

        double distSqr = villager.blockPosition().distSqr(targetBlock);
        if (distSqr > 2.5 * 2.5) {
            villager.getNavigation().moveTo(targetBlock.getX() + 0.5, targetBlock.getY(), targetBlock.getZ() + 0.5, 0.5D);
            return;
        }

        villager.getNavigation().stop();
        level.destroyBlock(targetBlock, false);
        level.playSound(null, targetBlock, SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 0.6F, 1.0F);

        VillagerTaskData.setCarrying(villager, targetResource.name(), 1);
        VillagerTaskData.setDelivering(villager);
        targetBlock = null;
    }

    private void giveUp() {
        VillagerTaskData.setIdle(villager);
        findProject().ifPresent(lookup -> lookup.project().getAssignedGatherers().remove(villager.getUUID()));
    }

    private Optional<ResourceType> pickNeededResource() {
        return findProject().map(lookup -> {
            ConstructionProject project = lookup.project();
            for (ResourceType type : new ResourceType[]{ResourceType.WOOD, ResourceType.STONE, ResourceType.IRON}) {
                if (project.getRemaining(type) > 0) return type;
            }
            return null;
        });
    }

    private Optional<ProjectLookup> findProject() {
        if (!(villager.level() instanceof ServerLevel level)) return Optional.empty();
        BlockPos anchor = VillagerTaskData.getVillageAnchor(villager);
        UUID projectId = VillagerTaskData.getProjectId(villager);
        if (anchor == null || projectId == null) return Optional.empty();

        VillageInstance village = VillageSavedData.get(level).findNear(anchor, VillageManager.SEARCH_RADIUS);
        if (village == null) return Optional.empty();
        return village.findProject(projectId).map(p -> new ProjectLookup(village, p));
    }

    private static boolean matches(BlockState state, ResourceType type) {
        return switch (type) {
            case WOOD -> state.is(BlockTags.LOGS);
            case STONE -> state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.ANDESITE)
                    || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE) || state.is(Blocks.DEEPSLATE)
                    || state.is(Blocks.COBBLED_DEEPSLATE);
            case IRON -> state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE);
            case FOOD -> false;
        };
    }

    private Optional<BlockPos> findNearestMatch(ServerLevel level, ResourceType type) {
        BlockPos center = villager.blockPosition();
        BlockPos bestPos = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-SCAN_RADIUS, -4, -SCAN_RADIUS), center.offset(SCAN_RADIUS, 4, SCAN_RADIUS))) {
            if (matches(level.getBlockState(pos), type)) {
                double d = pos.distSqr(center);
                if (d < bestDist) {
                    bestDist = d;
                    bestPos = pos.immutable();
                }
            }
        }
        return Optional.ofNullable(bestPos);
    }
}
