package com.villageevolution.mod.village;

import com.villageevolution.mod.util.VillagerTaskData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * The village "brain": population/housing tracking, resource bookkeeping,
 * automatic expansion (deciding what to build next), and finalizing
 * construction projects once they're fully gathered + built. Villager AI
 * goals (see the ai package) do the legwork; this class makes the
 * town-planning decisions and applies their results to the world.
 */
public class VillageManager {

    public static final int SEARCH_RADIUS = 48;
    public static final int GROWTH_INTERVAL_TICKS = 6000;   // 5 minutes: stage growth + food upkeep
    public static final int PROJECT_INTERVAL_TICKS = 100;   // 5 seconds: worker assignment + expansion checks

    private static final Random RANDOM = new Random();

    // =========================================================================================
    // Growth / stage progression
    // =========================================================================================

    public static void tickGrowth(ServerLevel level, VillageInstance village, long gameTime) {
        if (gameTime - village.getLastGrowthTick() < GROWTH_INTERVAL_TICKS) return;
        village.setLastGrowthTick(gameTime);

        BlockPos anchor = village.getAnchor();
        AABB area = new AABB(anchor).inflate(SEARCH_RADIUS);
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, area);
        List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class, area);

        village.setPopulation(villagers.size());
        if (villagers.isEmpty()) return; // abandoned; don't grow or shrink further

        // Food upkeep: population consumes stockpiled food. A shortage slows growth.
        int foodNeeded = villagers.size();
        boolean fed = village.consumeResource(ResourceType.FOOD, foodNeeded);

        int growth = villagers.size() * 8 + golems.size() * 3;
        if (!fed) growth /= 2;
        village.addGrowth(growth);

        VillageStage current = village.getStage();
        if (!current.isMax()) {
            VillageStage nextStage = current.next();
            boolean hasEnoughBuildings = village.getBuildings().size() >= nextStage.getLevel() * 2;
            if (village.getGrowthPoints() >= nextStage.getGrowthRequired() && hasEnoughBuildings) {
                advanceStage(level, village, nextStage);
            }
        }
    }

    private static void advanceStage(ServerLevel level, VillageInstance village, VillageStage newStage) {
        village.setStage(newStage);
        BlockPos anchor = village.getAnchor();

        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                anchor.getX() + 0.5, anchor.getY() + 1.5, anchor.getZ() + 0.5,
                40, 3.0, 2.0, 3.0, 0.02);
        level.playSound(null, anchor, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);

        Component message = Component.literal(
                "The settlement near " + anchor.toShortString() + " has grown into a " + newStage.getDisplayName() + "!");
        double announceRadiusSqr = Math.pow(SEARCH_RADIUS * 4, 2);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level && player.blockPosition().distSqr(anchor) < announceRadiusSqr) {
                player.displayClientMessage(message, false);
            }
        }

        int targetBonusGolems = newStage.getLevel();
        while (village.getBonusGolemsSpawned() < targetBonusGolems) {
            if (!spawnBonusGolem(level, village)) break;
        }
    }

    private static boolean spawnBonusGolem(ServerLevel level, VillageInstance village) {
        BlockPos anchor = village.getAnchor();
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos candidate = anchor.offset(RANDOM.nextInt(21) - 10, 0, RANDOM.nextInt(21) - 10);
            BlockPos ground = findSurface(level, candidate);
            if (ground == null) continue;

            IronGolem golem = new IronGolem(EntityType.IRON_GOLEM, level);
            golem.moveTo(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 0.5, 0.0F, 0.0F);
            golem.setPlayerCreated(false);
            level.addFreshEntity(golem);
            village.incrementBonusGolems();
            return true;
        }
        return false;
    }

    // =========================================================================================
    // Automatic expansion + worker assignment
    // =========================================================================================

    public static void tickProjects(ServerLevel level, VillageInstance village, long gameTime) {
        if (gameTime - village.getLastProjectTick() < PROJECT_INTERVAL_TICKS) return;
        village.setLastProjectTick(gameTime);

        if (village.getProjects().isEmpty()) {
            considerNewProject(level, village);
        }
        assignWorkers(level, village);
    }

    /** Decides what the village needs most and queues a project for it, if resources/space allow. */
    private static void considerNewProject(ServerLevel level, VillageInstance village) {
        BuildingType next = decideNextBuilding(village);
        if (next == null) return;

        boolean isUpgrade = village.hasBuildingOfType(next)
                && village.getBuildingsOfType(next).stream().anyMatch(VillageBuilding::canUpgrade)
                && (next != BuildingType.HOUSE); // prefer building new houses over upgrading old ones

        BlockPos anchor = village.getAnchor();
        if (isUpgrade) {
            VillageBuilding existing = village.getBuildingsOfType(next).stream()
                    .filter(VillageBuilding::canUpgrade).findFirst().orElse(null);
            if (existing == null) return;
            village.addProject(new ConstructionProject(next, existing.getLevel() + 1, existing.getOrigin(), existing.getFacing(), true));
        } else {
            BlockPos site = findBuildSite(level, anchor);
            if (site == null) return;
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
            village.addProject(new ConstructionProject(next, 1, site, facing, false));
        }
    }

    /** Priority order reflecting a believable settlement growth pattern. */
    private static BuildingType decideNextBuilding(VillageInstance village) {
        int population = village.getPopulation();
        VillageStage stage = village.getStage();

        if (!village.hasBuildingOfType(BuildingType.TOWN_HALL)) return BuildingType.TOWN_HALL;
        if (village.isOverpopulated() || village.getBuildingsOfType(BuildingType.HOUSE).isEmpty()) {
            return BuildingType.HOUSE;
        }
        if (village.getBuildingsOfType(BuildingType.FARM).size() < 1 + population / 8) {
            return BuildingType.FARM;
        }
        for (BuildingType type : new BuildingType[]{
                BuildingType.STORAGE, BuildingType.BLACKSMITH, BuildingType.MARKET,
                BuildingType.CLINIC, BuildingType.WATCHTOWER, BuildingType.WALL_GATE, BuildingType.HOSPITAL
        }) {
            if (!type.isUnlocked(population, stage)) continue;
            if (!village.hasBuildingOfType(type)) return type;
        }
        // Everything exists at least once - build more houses if there's room to grow, else upgrade town hall.
        if (village.getHousingCapacity() < population + 4) return BuildingType.HOUSE;
        return BuildingType.TOWN_HALL;
    }

    private static BlockPos findBuildSite(ServerLevel level, BlockPos anchor) {
        for (int attempt = 0; attempt < 20; attempt++) {
            BlockPos candidate = anchor.offset(RANDOM.nextInt(61) - 30, 0, RANDOM.nextInt(61) - 30);
            BlockPos ground = findSurface(level, candidate);
            if (ground != null) return ground;
        }
        return null;
    }

    private static BlockPos findSurface(ServerLevel level, BlockPos columnPos) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, columnPos);
        if (top.getY() <= level.getMinBuildHeight() + 1) return null;
        return top;
    }

    /** Assigns idle nearby villagers to gather materials for, or build, active projects. */
    private static void assignWorkers(ServerLevel level, VillageInstance village) {
        if (village.getProjects().isEmpty()) return;

        List<Villager> nearby = level.getEntitiesOfClass(Villager.class,
                new AABB(village.getAnchor()).inflate(SEARCH_RADIUS));

        for (ConstructionProject project : village.getProjects()) {
            for (Villager villager : nearby) {
                if (VillagerTaskData.getTask(villager) != VillagerTaskData.Task.IDLE) continue;

                if (project.needsGatherer()) {
                    VillagerTaskData.assignGather(villager, village.getAnchor(), project.getId());
                    project.getAssignedGatherers().add(villager.getUUID());
                } else if (project.needsBuilder()) {
                    VillagerTaskData.assignBuild(villager, village.getAnchor(), project.getId());
                    project.getAssignedBuilders().add(villager.getUUID());
                }
            }
        }
    }

    // =========================================================================================
    // Called by AI goals
    // =========================================================================================

    public static Optional<ConstructionProject> findProject(VillageInstance village, UUID projectId) {
        return village.findProject(projectId);
    }

    /** Finalizes a project once fully gathered and built: places the blueprint and registers the building. */
    public static void completeProject(ServerLevel level, VillageInstance village, ConstructionProject project) {
        List<BlockPlacement> placements = BlueprintLibrary.generate(
                project.getType(), project.getTargetLevel(), project.getOrigin(), project.getFacing());
        for (BlockPlacement placement : placements) {
            level.setBlockAndUpdate(placement.pos(), placement.state());
        }

        if (project.isUpgrade()) {
            village.getBuildingsOfType(project.getType()).stream()
                    .filter(b -> b.getOrigin().equals(project.getOrigin()))
                    .findFirst()
                    .ifPresent(b -> b.setLevel(project.getTargetLevel()));
            village.getStatistics().incrementBuildingsUpgraded();
        } else {
            village.addBuilding(new VillageBuilding(project.getType(), project.getTargetLevel(), project.getOrigin(), project.getFacing()));
            village.getStatistics().incrementBuildingsConstructed();
        }
        village.getStatistics().incrementProjectsCompleted();
        village.removeProject(project);

        level.sendParticles(ParticleTypes.CLOUD,
                project.getOrigin().getX() + 0.5, project.getOrigin().getY() + 2.0, project.getOrigin().getZ() + 0.5,
                30, 2.0, 1.5, 2.0, 0.02);
        level.playSound(null, project.getOrigin(), SoundEvents.STONE_PLACE, SoundSource.NEUTRAL, 1.0F, 0.8F);
    }
}
