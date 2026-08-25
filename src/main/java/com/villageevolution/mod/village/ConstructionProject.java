package com.villageevolution.mod.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A queued or in-progress build/upgrade. Projects go through two phases:
 * 1. GATHERING - villagers deliver materials until deliveredMaterials meets requiredMaterials
 * 2. BUILDING  - assigned workers accumulate labor points until laborRequired is met
 * Once both are satisfied, VillageManager finalizes it: places the blueprint,
 * registers/updates the VillageBuilding, and removes the project.
 */
public class ConstructionProject {

    public enum Phase { GATHERING, BUILDING }

    private final UUID id;
    private final BuildingType type;
    private final int targetLevel;
    private final BlockPos origin;
    private final Direction facing;
    private final boolean upgrade;

    private final Map<ResourceType, Integer> requiredMaterials;
    private final Map<ResourceType, Integer> deliveredMaterials = new EnumMap<>(ResourceType.class);

    private int laborPoints;
    private static final int LABOR_REQUIRED = 200;

    private final Set<UUID> assignedGatherers = new HashSet<>();
    private final Set<UUID> assignedBuilders = new HashSet<>();

    public ConstructionProject(BuildingType type, int targetLevel, BlockPos origin, Direction facing, boolean upgrade) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.targetLevel = targetLevel;
        this.origin = origin;
        this.facing = facing;
        this.upgrade = upgrade;
        this.requiredMaterials = type.getCost(targetLevel);
    }

    public UUID getId() { return id; }
    public BuildingType getType() { return type; }
    public int getTargetLevel() { return targetLevel; }
    public BlockPos getOrigin() { return origin; }
    public Direction getFacing() { return facing; }
    public boolean isUpgrade() { return upgrade; }

    public Map<ResourceType, Integer> getRequiredMaterials() { return requiredMaterials; }

    public int getRemaining(ResourceType resourceType) {
        int required = requiredMaterials.getOrDefault(resourceType, 0);
        int delivered = deliveredMaterials.getOrDefault(resourceType, 0);
        return Math.max(0, required - delivered);
    }

    public boolean needsMaterials() {
        for (ResourceType type : requiredMaterials.keySet()) {
            if (getRemaining(type) > 0) return true;
        }
        return false;
    }

    public void deliver(ResourceType resourceType, int amount) {
        deliveredMaterials.merge(resourceType, amount, Integer::sum);
    }

    public Phase getPhase() {
        return needsMaterials() ? Phase.GATHERING : Phase.BUILDING;
    }

    public void addLabor(int amount) {
        this.laborPoints += amount;
    }

    public boolean isReadyToComplete() {
        return !needsMaterials() && laborPoints >= LABOR_REQUIRED;
    }

    public float getProgress() {
        int totalRequired = requiredMaterials.values().stream().mapToInt(Integer::intValue).sum();
        int totalDelivered = deliveredMaterials.values().stream().mapToInt(Integer::intValue).sum();
        float materialFraction = totalRequired == 0 ? 1.0F : Math.min(1.0F, totalDelivered / (float) totalRequired);
        float laborFraction = Math.min(1.0F, laborPoints / (float) LABOR_REQUIRED);
        // Materials must be gathered before labor can start, so weight both phases evenly.
        return (materialFraction * 0.5F) + (laborFraction * 0.5F);
    }

    public Set<UUID> getAssignedGatherers() { return assignedGatherers; }
    public Set<UUID> getAssignedBuilders() { return assignedBuilders; }

    public boolean needsGatherer() {
        return needsMaterials() && assignedGatherers.size() < type.getIdealGatherers();
    }

    public boolean needsBuilder() {
        return !needsMaterials() && assignedBuilders.size() < type.getIdealBuilders();
    }

    public void releaseWorker(UUID villagerId) {
        assignedGatherers.remove(villagerId);
        assignedBuilders.remove(villagerId);
    }
}
