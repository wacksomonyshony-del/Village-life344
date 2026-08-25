package com.villageevolution.mod.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists the set of tracked villages for a dimension to the level's
 * saved-data storage (region files), so progress survives a restart.
 */
public class VillageSavedData extends SavedData {

    private static final String DATA_NAME = "villageevolution_villages";
    private final List<VillageInstance> villages = new ArrayList<>();

    public static VillageSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VillageSavedData::new, VillageSavedData::load),
                DATA_NAME
        );
    }

    public List<VillageInstance> getVillages() {
        return villages;
    }

    public VillageInstance findNear(BlockPos pos, int radius) {
        double radiusSqr = (double) radius * radius;
        for (VillageInstance village : villages) {
            if (village.getAnchor().distSqr(pos) <= radiusSqr) {
                return village;
            }
        }
        return null;
    }

    public VillageInstance findOrCreate(BlockPos pos, int radius) {
        VillageInstance existing = findNear(pos, radius);
        if (existing != null) return existing;

        VillageInstance created = new VillageInstance(pos);
        villages.add(created);
        setDirty();
        return created;
    }

    public static VillageSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        VillageSavedData data = new VillageSavedData();
        ListTag list = tag.getList("Villages", 10); // 10 = CompoundTag id
        for (int i = 0; i < list.size(); i++) {
            data.villages.add(VillageInstance.load(list.getCompound(i)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (VillageInstance village : villages) {
            list.add(village.save());
        }
        tag.put("Villages", list);
        return tag;
    }
}
