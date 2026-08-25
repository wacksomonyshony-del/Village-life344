package com.villageevolution.mod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;

import java.util.UUID;

/**
 * Gives each villager an individual task, persisted in the entity's
 * vanilla "persistent data" tag (the same mechanism scoreboard tags use),
 * so it survives chunk unload/reload and server restarts without needing
 * a custom capability.
 *
 * A task links back to a specific village (by anchor position) and, for
 * gather/build tasks, a specific ConstructionProject (by UUID) so
 * VillageManager can look it back up.
 */
public final class VillagerTaskData {

    public enum Task { IDLE, GATHER, DELIVER, BUILD }

    private static final String ROOT = "VillageEvolution";
    private static final String TASK = "Task";
    private static final String VILLAGE_ANCHOR = "VillageAnchor";
    private static final String PROJECT_MOST = "ProjectMost";
    private static final String PROJECT_LEAST = "ProjectLeast";
    private static final String CARRY_RESOURCE = "CarryResource";
    private static final String CARRY_AMOUNT = "CarryAmount";

    private VillagerTaskData() {}

    private static CompoundTag root(Villager villager) {
        CompoundTag persistent = villager.getPersistentData();
        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }

    private static void save(Villager villager, CompoundTag data) {
        villager.getPersistentData().put(ROOT, data);
    }

    public static Task getTask(Villager villager) {
        CompoundTag data = root(villager);
        if (!data.contains(TASK)) return Task.IDLE;
        try {
            return Task.valueOf(data.getString(TASK));
        } catch (IllegalArgumentException e) {
            return Task.IDLE;
        }
    }

    public static void setIdle(Villager villager) {
        CompoundTag data = root(villager);
        data.putString(TASK, Task.IDLE.name());
        data.remove(PROJECT_MOST);
        data.remove(PROJECT_LEAST);
        save(villager, data);
    }

    public static void assignGather(Villager villager, BlockPos villageAnchor, UUID projectId) {
        setProjectTask(villager, Task.GATHER, villageAnchor, projectId);
    }

    public static void assignBuild(Villager villager, BlockPos villageAnchor, UUID projectId) {
        setProjectTask(villager, Task.BUILD, villageAnchor, projectId);
    }

    public static void setDelivering(Villager villager) {
        CompoundTag data = root(villager);
        data.putString(TASK, Task.DELIVER.name());
        save(villager, data);
    }

    private static void setProjectTask(Villager villager, Task task, BlockPos villageAnchor, UUID projectId) {
        CompoundTag data = root(villager);
        data.putString(TASK, task.name());
        data.putLong(VILLAGE_ANCHOR, villageAnchor.asLong());
        data.putLong(PROJECT_MOST, projectId.getMostSignificantBits());
        data.putLong(PROJECT_LEAST, projectId.getLeastSignificantBits());
        save(villager, data);
    }

    public static BlockPos getVillageAnchor(Villager villager) {
        CompoundTag data = root(villager);
        return data.contains(VILLAGE_ANCHOR) ? BlockPos.of(data.getLong(VILLAGE_ANCHOR)) : null;
    }

    public static UUID getProjectId(Villager villager) {
        CompoundTag data = root(villager);
        if (!data.contains(PROJECT_MOST)) return null;
        return new UUID(data.getLong(PROJECT_MOST), data.getLong(PROJECT_LEAST));
    }

    // ---- carried materials -------------------------------------------------------

    public static void setCarrying(Villager villager, String resourceTypeName, int amount) {
        CompoundTag data = root(villager);
        data.putString(CARRY_RESOURCE, resourceTypeName);
        data.putInt(CARRY_AMOUNT, amount);
        save(villager, data);
    }

    public static String getCarryingResource(Villager villager) {
        return root(villager).getString(CARRY_RESOURCE);
    }

    public static int getCarryingAmount(Villager villager) {
        return root(villager).getInt(CARRY_AMOUNT);
    }

    public static void clearCarrying(Villager villager) {
        CompoundTag data = root(villager);
        data.remove(CARRY_RESOURCE);
        data.remove(CARRY_AMOUNT);
        save(villager, data);
    }
}
