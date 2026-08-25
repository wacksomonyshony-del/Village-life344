package com.villageevolution.mod.village;

import com.villageevolution.mod.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
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

    /** Game tick the project was queued, used to extrapolate an ETA for the floating label. */
    private final long startGameTime;

    /**
     * Blocks standing where the building will go. Builders clear these one at
     * a time before any labor is credited. Filled lazily on first use because
     * it needs world access. Null means "not computed yet".
     */
    private Deque<BlockPos> clearQueue;

    public ConstructionProject(BuildingType type, int targetLevel, BlockPos origin, Direction facing,
                               boolean upgrade, long startGameTime) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.targetLevel = targetLevel;
        this.origin = origin;
        this.facing = facing;
        this.upgrade = upgrade;
        this.startGameTime = startGameTime;
        // With creative materials on, nothing needs gathering - the project
        // opens straight into its BUILDING phase.
        this.requiredMaterials = ModSettings.CREATIVE_MATERIALS
                ? new EnumMap<>(ResourceType.class)
                : type.getCost(targetLevel);
    }

    public long getStartGameTime() { return startGameTime; }

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
        return !needsMaterials() && !hasClearingWork() && laborPoints >= LABOR_REQUIRED;
    }

    // ---- site clearing -------------------------------------------------------

    /**
     * Works out which blocks are in the way of this building and queues them
     * for demolition. Only solid, non-fluid blocks that the blueprint would
     * overwrite are queued - villagers never break anything outside this
     * footprint.
     */
    public void ensureClearQueue(ServerLevel level) {
        if (clearQueue != null) return;
        clearQueue = new ArrayDeque<>();
        List<BlockPlacement> placements = BlueprintLibrary.generate(type, targetLevel, origin, facing);
        for (BlockPlacement placement : placements) {
            BlockState existing = level.getBlockState(placement.pos());
            if (existing.isAir()) continue;
            if (!existing.getFluidState().isEmpty()) continue;
            if (existing.equals(placement.state())) continue;
            clearQueue.add(placement.pos());
        }
    }

    public boolean hasClearingWork() {
        return clearQueue != null && !clearQueue.isEmpty();
    }

    /** The next block to demolish, or null if the site is clear (or not yet surveyed). */
    public BlockPos peekClearTarget() {
        return clearQueue == null ? null : clearQueue.peek();
    }

    public void popClearTarget() {
        if (clearQueue != null) clearQueue.poll();
    }

    /**
     * Rough time-to-finish in ticks, extrapolated from how long the project has
     * taken to reach its current progress. Returns -1 while progress is still
     * too small to extrapolate from.
     */
    public int estimateTicksRemaining(long now) {
        float progress = getProgress();
        if (progress <= 0.02F) return -1;
        long elapsed = Math.max(1L, now - startGameTime);
        return (int) (elapsed * (1.0F - progress) / progress);
    }

    public float getProgress() {
        int totalRequired = requiredMaterials.values().stream().mapToInt(Integer::intValue).sum();
        int totalDelivered = deliveredMaterials.values().stream().mapToInt(Integer::intValue).sum();
        float materialFraction = totalRequired == 0 ? 1.0F : Math.min(1.0F, totalDelivered / (float) totalRequired);
        float laborFraction = Math.min(1.0F, laborPoints / (float) LABOR_REQUIRED);
        if (totalRequired == 0) {
            // Creative-materials mode: the whole job is clearing + labor.
            return laborFraction;
        }
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
