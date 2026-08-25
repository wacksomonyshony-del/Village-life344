package com.villageevolution.mod.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/** A completed building that exists in the world, tracked by the village. */
public class VillageBuilding {

    private final BuildingType type;
    private int level;
    private final BlockPos origin;
    private final Direction facing;

    public VillageBuilding(BuildingType type, int level, BlockPos origin, Direction facing) {
        this.type = type;
        this.level = level;
        this.origin = origin;
        this.facing = facing;
    }

    public BuildingType getType() { return type; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public BlockPos getOrigin() { return origin; }
    public Direction getFacing() { return facing; }

    public boolean canUpgrade() {
        return level < type.getMaxLevel();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        tag.putInt("Level", level);
        tag.putLong("Origin", origin.asLong());
        tag.putString("Facing", facing.getName());
        return tag;
    }

    public static VillageBuilding load(CompoundTag tag) {
        BuildingType type = BuildingType.valueOf(tag.getString("Type"));
        int level = tag.getInt("Level");
        BlockPos origin = BlockPos.of(tag.getLong("Origin"));
        Direction facing = Direction.byName(tag.getString("Facing"));
        if (facing == null) facing = Direction.NORTH;
        return new VillageBuilding(type, level, origin, facing);
    }
}
