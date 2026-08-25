package com.villageevolution.mod.util;

import com.villageevolution.mod.village.ConstructionProject;
import com.villageevolution.mod.village.VillageInstance;
import com.villageevolution.mod.village.VillageManager;
import com.villageevolution.mod.village.VillageSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;

import java.util.UUID;

/**
 * Puts a floating label over any villager that is working on a construction
 * project, showing what it is doing, which structure, and roughly how long
 * is left.
 *
 * This uses the vanilla custom-name mechanism (the same thing a name tag
 * uses), which means it renders above the entity with no client-side code
 * at all. The trade-off is that it occupies the villager's name, so we
 * track a flag in persistent data and refuse to touch villagers a player
 * has named themselves.
 */
public final class VillagerLabelHelper {

    /** Marks a custom name as one we set, so we know it is safe to change or clear. */
    private static final String LABEL_FLAG = "VillageEvolutionLabel";

    private VillagerLabelHelper() {}

    public static void update(ServerLevel level, Villager villager) {
        VillagerTaskData.Task task = VillagerTaskData.getTask(villager);
        if (task == VillagerTaskData.Task.IDLE) {
            clear(villager);
            return;
        }

        BlockPos anchor = VillagerTaskData.getVillageAnchor(villager);
        UUID projectId = VillagerTaskData.getProjectId(villager);
        if (anchor == null || projectId == null) {
            clear(villager);
            return;
        }

        VillageInstance village = VillageSavedData.get(level).findNear(anchor, VillageManager.SEARCH_RADIUS);
        ConstructionProject project = village == null ? null : village.findProject(projectId).orElse(null);
        if (project == null) {
            clear(villager);
            return;
        }

        String verb = switch (task) {
            case GATHER -> "Gathering for";
            case DELIVER -> "Hauling to";
            case BUILD -> project.hasClearingWork() ? "Clearing for" : "Building";
            default -> "Working on";
        };

        int remaining = project.estimateTicksRemaining(level.getGameTime());
        String eta = remaining < 0 ? "--:--" : formatTicks(remaining);

        set(villager, Component.literal(verb + " " + project.getType().getDisplayName())
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("  " + eta).withStyle(ChatFormatting.GRAY)));
    }

    /** Ticks to m:ss at the usual 20 ticks per second. */
    private static String formatTicks(int ticks) {
        int totalSeconds = Math.max(0, ticks / 20);
        if (totalSeconds >= 3600) return (totalSeconds / 3600) + "h+";
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private static void set(Villager villager, Component text) {
        CompoundTag data = villager.getPersistentData();
        // Never stomp on a name a player gave this villager.
        if (villager.hasCustomName() && !data.getBoolean(LABEL_FLAG)) return;

        data.putBoolean(LABEL_FLAG, true);
        villager.setCustomName(text);
        villager.setCustomNameVisible(true);
    }

    private static void clear(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        if (!data.getBoolean(LABEL_FLAG)) return;

        data.putBoolean(LABEL_FLAG, false);
        villager.setCustomName(null);
        villager.setCustomNameVisible(false);
    }
}
