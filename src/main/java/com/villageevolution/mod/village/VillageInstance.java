package com.villageevolution.mod.village;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The full civilization state for one village: its stage, population,
 * housing, resource stockpiles, completed buildings, active construction
 * projects, and lifetime statistics.
 *
 * Note on persistence: buildings, resources, and stats are saved to disk
 * (via VillageSavedData). Active construction projects are kept in-memory
 * only for simplicity - if the server restarts mid-project, in-progress
 * gathering/building work is lost and villagers will simply pick a fresh
 * task next tick. Completed buildings are never lost.
 */
public class VillageInstance {

    private final BlockPos anchor;
    private VillageStage stage;
    private int growthPoints;
    private long lastGrowthTick;
    private long lastProjectTick;
    private int bonusGolemsSpawned;

    private int population;
    private int housingCapacity;

    /** Daily construction budget (see ModSettings.PROJECTS_PER_DAY). */
    private long lastDayIndex = -1;
    private int projectsStartedToday;

    private final Map<ResourceType, Integer> resources = new EnumMap<>(ResourceType.class);
    private final List<VillageBuilding> buildings = new ArrayList<>();
    private final List<ConstructionProject> projects = new ArrayList<>();
    private VillageStatistics statistics = new VillageStatistics();

    public VillageInstance(BlockPos anchor) {
        this.anchor = anchor;
        this.stage = VillageStage.CAMP;
        for (ResourceType type : ResourceType.values()) resources.put(type, 0);
    }

    // ---- identity / stage ----------------------------------------------------

    public BlockPos getAnchor() { return anchor; }
    public VillageStage getStage() { return stage; }
    public void setStage(VillageStage stage) { this.stage = stage; }
    public int getGrowthPoints() { return growthPoints; }
    public void addGrowth(int amount) { this.growthPoints += amount; }
    public long getLastGrowthTick() { return lastGrowthTick; }
    public void setLastGrowthTick(long t) { this.lastGrowthTick = t; }
    public long getLastProjectTick() { return lastProjectTick; }
    public void setLastProjectTick(long t) { this.lastProjectTick = t; }
    public int getBonusGolemsSpawned() { return bonusGolemsSpawned; }
    public void incrementBonusGolems() { this.bonusGolemsSpawned++; }

    // ---- population / housing -------------------------------------------------

    public int getPopulation() { return population; }
    public void setPopulation(int population) { this.population = population; }
    public int getHousingCapacity() { return housingCapacity; }

    public void recalculateHousingCapacity() {
        int total = 0;
        for (VillageBuilding b : buildings) {
            total += b.getType().getHousingCapacity(b.getLevel());
        }
        this.housingCapacity = total;
    }

    public boolean isOverpopulated() {
        return population > housingCapacity;
    }

    // ---- daily construction budget ---------------------------------------------

    /** Resets the per-day project counter when the world rolls over to a new day. */
    public void rolloverDay(long dayIndex) {
        if (dayIndex != lastDayIndex) {
            lastDayIndex = dayIndex;
            projectsStartedToday = 0;
        }
    }

    public int getProjectsStartedToday() { return projectsStartedToday; }
    public void incrementProjectsStartedToday() { this.projectsStartedToday++; }

    // ---- resources -------------------------------------------------------------

    public int getResource(ResourceType type) { return resources.getOrDefault(type, 0); }
    public void addResource(ResourceType type, int amount) { resources.merge(type, amount, Integer::sum); }
    public boolean consumeResource(ResourceType type, int amount) {
        int current = getResource(type);
        if (current < amount) return false;
        resources.put(type, current - amount);
        return true;
    }
    public Map<ResourceType, Integer> getAllResources() { return resources; }

    // ---- buildings ---------------------------------------------------------------

    public List<VillageBuilding> getBuildings() { return buildings; }

    public List<VillageBuilding> getBuildingsOfType(BuildingType type) {
        return buildings.stream().filter(b -> b.getType() == type).toList();
    }

    public boolean hasBuildingOfType(BuildingType type) {
        return buildings.stream().anyMatch(b -> b.getType() == type);
    }

    public void addBuilding(VillageBuilding building) {
        buildings.add(building);
        recalculateHousingCapacity();
    }

    // ---- projects --------------------------------------------------------------

    public List<ConstructionProject> getProjects() { return projects; }

    public void addProject(ConstructionProject project) { projects.add(project); }
    public void removeProject(ConstructionProject project) { projects.remove(project); }

    public Optional<ConstructionProject> findProject(UUID id) {
        return projects.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public boolean hasActiveProjectFor(BuildingType type) {
        return projects.stream().anyMatch(p -> p.getType() == type);
    }

    // ---- statistics --------------------------------------------------------------

    public VillageStatistics getStatistics() { return statistics; }

    // ---- persistence (buildings/resources/stats only, see class note) ------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Anchor", anchor.asLong());
        tag.putInt("Stage", stage.getLevel());
        tag.putInt("Growth", growthPoints);
        tag.putLong("LastTick", lastGrowthTick);
        tag.putInt("BonusGolems", bonusGolemsSpawned);
        tag.putInt("Population", population);
        tag.putLong("LastDay", lastDayIndex);
        tag.putInt("ProjectsToday", projectsStartedToday);

        CompoundTag resourceTag = new CompoundTag();
        for (Map.Entry<ResourceType, Integer> entry : resources.entrySet()) {
            resourceTag.putInt(entry.getKey().name(), entry.getValue());
        }
        tag.put("Resources", resourceTag);

        ListTag buildingList = new ListTag();
        for (VillageBuilding b : buildings) buildingList.add(b.save());
        tag.put("Buildings", buildingList);

        tag.put("Statistics", statistics.save());
        return tag;
    }

    public static VillageInstance load(CompoundTag tag) {
        VillageInstance instance = new VillageInstance(BlockPos.of(tag.getLong("Anchor")));
        instance.stage = VillageStage.byLevel(tag.getInt("Stage"));
        instance.growthPoints = tag.getInt("Growth");
        instance.lastGrowthTick = tag.getLong("LastTick");
        instance.bonusGolemsSpawned = tag.getInt("BonusGolems");
        instance.population = tag.getInt("Population");
        instance.lastDayIndex = tag.contains("LastDay") ? tag.getLong("LastDay") : -1;
        instance.projectsStartedToday = tag.getInt("ProjectsToday");

        CompoundTag resourceTag = tag.getCompound("Resources");
        for (ResourceType type : ResourceType.values()) {
            instance.resources.put(type, resourceTag.getInt(type.name()));
        }

        ListTag buildingList = tag.getList("Buildings", 10);
        for (int i = 0; i < buildingList.size(); i++) {
            instance.buildings.add(VillageBuilding.load(buildingList.getCompound(i)));
        }
        instance.recalculateHousingCapacity();

        if (tag.contains("Statistics")) {
            instance.statistics = VillageStatistics.load(tag.getCompound("Statistics"));
        }
        return instance;
    }
}
