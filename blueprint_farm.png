package com.villageevolution.mod.village;

/**
 * Development levels 0-5. A village's stage is driven by its population,
 * building count, and construction projects completed (see VillageManager).
 * Town (3) -> City (4) is the headline progression the mod is built around;
 * Metropolis (5) is the capstone level once a city is fully built out.
 */
public enum VillageStage {
    CAMP(0, "Camp", 0),
    HAMLET(1, "Hamlet", 300),
    VILLAGE(2, "Village", 900),
    TOWN(3, "Town", 2200),
    CITY(4, "City", 5000),
    METROPOLIS(5, "Metropolis", 9000);

    private final int level;
    private final String displayName;
    private final int growthRequired;

    VillageStage(int level, String displayName, int growthRequired) {
        this.level = level;
        this.displayName = displayName;
        this.growthRequired = growthRequired;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getGrowthRequired() {
        return growthRequired;
    }

    public VillageStage next() {
        int n = level + 1;
        for (VillageStage stage : values()) {
            if (stage.level == n) return stage;
        }
        return this;
    }

    public boolean isMax() {
        return this == METROPOLIS;
    }

    public static VillageStage byLevel(int level) {
        int clamped = Math.max(0, Math.min(level, values().length - 1));
        for (VillageStage stage : values()) {
            if (stage.level == clamped) return stage;
        }
        return CAMP;
    }
}
