package com.villageevolution.mod.village;

import net.minecraft.nbt.CompoundTag;

/**
 * Running totals for a village, surfaced to players via the /village stats
 * style output the mod can print, and used internally to decide things like
 * "has this village ever had a blacksmith".
 */
public class VillageStatistics {

    private int buildingsConstructed;
    private int buildingsUpgraded;
    private int projectsCompleted;
    private int villagersHealed;
    private int golemsRepaired;
    private int materialsGathered;
    private int foodProduced;

    public void incrementBuildingsConstructed() { buildingsConstructed++; }
    public void incrementBuildingsUpgraded() { buildingsUpgraded++; }
    public void incrementProjectsCompleted() { projectsCompleted++; }
    public void incrementVillagersHealed() { villagersHealed++; }
    public void incrementGolemsRepaired() { golemsRepaired++; }
    public void addMaterialsGathered(int amount) { materialsGathered += amount; }
    public void addFoodProduced(int amount) { foodProduced += amount; }

    public int getBuildingsConstructed() { return buildingsConstructed; }
    public int getBuildingsUpgraded() { return buildingsUpgraded; }
    public int getProjectsCompleted() { return projectsCompleted; }
    public int getVillagersHealed() { return villagersHealed; }
    public int getGolemsRepaired() { return golemsRepaired; }
    public int getMaterialsGathered() { return materialsGathered; }
    public int getFoodProduced() { return foodProduced; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("BuildingsConstructed", buildingsConstructed);
        tag.putInt("BuildingsUpgraded", buildingsUpgraded);
        tag.putInt("ProjectsCompleted", projectsCompleted);
        tag.putInt("VillagersHealed", villagersHealed);
        tag.putInt("GolemsRepaired", golemsRepaired);
        tag.putInt("MaterialsGathered", materialsGathered);
        tag.putInt("FoodProduced", foodProduced);
        return tag;
    }

    public static VillageStatistics load(CompoundTag tag) {
        VillageStatistics stats = new VillageStatistics();
        stats.buildingsConstructed = tag.getInt("BuildingsConstructed");
        stats.buildingsUpgraded = tag.getInt("BuildingsUpgraded");
        stats.projectsCompleted = tag.getInt("ProjectsCompleted");
        stats.villagersHealed = tag.getInt("VillagersHealed");
        stats.golemsRepaired = tag.getInt("GolemsRepaired");
        stats.materialsGathered = tag.getInt("MaterialsGathered");
        stats.foodProduced = tag.getInt("FoodProduced");
        return stats;
    }
}
