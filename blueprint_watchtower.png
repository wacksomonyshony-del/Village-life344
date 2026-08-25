package com.villageevolution.mod.village;

import java.util.EnumMap;
import java.util.Map;

/**
 * Every building type the village can construct/upgrade, with its
 * requirements (min population + min village stage before it can be
 * queued), max level, how much housing it grants (HOUSE only), and its
 * per-level resource cost.
 */
public enum BuildingType {
    TOWN_HALL("Town Hall", 3, 0, VillageStage.CAMP, 0, 16, 10, 0),
    HOUSE("House", 3, 0, VillageStage.CAMP, 4, 10, 4, 0),
    FARM("Farm", 3, 0, VillageStage.CAMP, 0, 6, 0, 0),
    STORAGE("Storage", 3, 6, VillageStage.HAMLET, 0, 10, 6, 0),
    BLACKSMITH("Blacksmith", 2, 8, VillageStage.HAMLET, 0, 8, 12, 4),
    MARKET("Market", 2, 10, VillageStage.VILLAGE, 0, 14, 10, 2),
    CLINIC("Clinic", 1, 8, VillageStage.VILLAGE, 0, 10, 14, 2),
    WATCHTOWER("Watchtower", 3, 12, VillageStage.TOWN, 0, 10, 20, 4),
    WALL_GATE("Wall & Gate", 2, 15, VillageStage.TOWN, 0, 6, 24, 2),
    HOSPITAL("Hospital", 1, 20, VillageStage.CITY, 0, 16, 26, 8);

    private final String displayName;
    private final int maxLevel;
    private final int minPopulation;
    private final VillageStage minStage;
    private final int housingPerLevel;
    private final int woodBase;
    private final int stoneBase;
    private final int ironBase;

    BuildingType(String displayName, int maxLevel, int minPopulation, VillageStage minStage,
                 int housingPerLevel, int woodBase, int stoneBase, int ironBase) {
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.minPopulation = minPopulation;
        this.minStage = minStage;
        this.housingPerLevel = housingPerLevel;
        this.woodBase = woodBase;
        this.stoneBase = stoneBase;
        this.ironBase = ironBase;
    }

    public String getDisplayName() { return displayName; }
    public int getMaxLevel() { return maxLevel; }
    public int getMinPopulation() { return minPopulation; }
    public VillageStage getMinStage() { return minStage; }

    /** Housing capacity a single building of this type grants at the given level (HOUSE only; 0 for others). */
    public int getHousingCapacity(int level) {
        return housingPerLevel * level;
    }

    public boolean isUnlocked(int population, VillageStage stage) {
        return population >= minPopulation && stage.getLevel() >= minStage.getLevel();
    }

    /** Resource cost to build/upgrade to the given level (1-indexed). */
    public Map<ResourceType, Integer> getCost(int level) {
        Map<ResourceType, Integer> cost = new EnumMap<>(ResourceType.class);
        cost.put(ResourceType.WOOD, woodBase * level);
        cost.put(ResourceType.STONE, stoneBase * level);
        if (ironBase > 0) cost.put(ResourceType.IRON, ironBase * level);
        return cost;
    }

    /** How many workers should ideally gather materials for this project at once. */
    public int getIdealGatherers() {
        return this == WALL_GATE || this == WATCHTOWER ? 3 : 2;
    }

    public int getIdealBuilders() {
        return this == TOWN_HALL || this == WALL_GATE ? 2 : 1;
    }
}
