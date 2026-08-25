package com.villageevolution.mod.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Procedurally generates the block layout for each building type/level
 * rather than loading external NBT structure files, so the whole mod stays
 * self-contained in source. Layouts are simple but distinct per building
 * type, and scale up (bigger footprint, better materials) with level -
 * this is what satisfies "larger upgraded versions as the settlement
 * develops".
 *
 * Coordinates are local to the building: x = sideways ("width"), y = up,
 * z = forward (the direction the building faces, e.g. its door/front).
 * transform() rotates local coordinates into world space based on the
 * building's stored facing direction.
 */
public final class BlueprintLibrary {

    private BlueprintLibrary() {}

    public static List<BlockPlacement> generate(BuildingType type, int level, BlockPos origin, Direction facing) {
        List<BlockPlacement> out = new ArrayList<>();
        switch (type) {
            case HOUSE -> house(out, origin, facing, level);
            case TOWN_HALL -> townHall(out, origin, facing, level);
            case FARM -> farm(out, origin, facing, level);
            case STORAGE -> storage(out, origin, facing, level);
            case BLACKSMITH -> blacksmith(out, origin, facing, level);
            case MARKET -> market(out, origin, facing, level);
            case CLINIC -> clinicOrHospital(out, origin, facing, level, false);
            case HOSPITAL -> clinicOrHospital(out, origin, facing, level, true);
            case WATCHTOWER -> watchtower(out, origin, facing, level);
            case WALL_GATE -> wallGate(out, origin, facing, level);
        }
        return out;
    }

    // ---- coordinate helpers -------------------------------------------------

    private static BlockPos transform(BlockPos origin, Direction facing, int lx, int ly, int lz) {
        Direction right = facing.getClockWise();
        int worldX = origin.getX() + facing.getStepX() * lz + right.getStepX() * lx;
        int worldY = origin.getY() + ly;
        int worldZ = origin.getZ() + facing.getStepZ() * lz + right.getStepZ() * lx;
        return new BlockPos(worldX, worldY, worldZ);
    }

    private static void put(List<BlockPlacement> out, BlockPos origin, Direction facing,
                             int lx, int ly, int lz, BlockState state) {
        out.add(new BlockPlacement(transform(origin, facing, lx, ly, lz), state));
    }

    private static void box(List<BlockPlacement> out, BlockPos origin, Direction facing,
                             int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x < x1; x++)
            for (int y = y0; y < y1; y++)
                for (int z = z0; z < z1; z++)
                    put(out, origin, facing, x, y, z, state);
    }

    private static void hollowBox(List<BlockPlacement> out, BlockPos origin, Direction facing,
                                   int x0, int y0, int z0, int x1, int y1, int z1, BlockState wall, BlockState floor) {
        for (int x = x0; x < x1; x++)
            for (int z = z0; z < z1; z++)
                put(out, origin, facing, x, y0, z, floor);
        for (int x = x0; x < x1; x++)
            for (int y = y0; y < y1; y++) {
                put(out, origin, facing, x, y, z0, wall);
                put(out, origin, facing, x, y, z1 - 1, wall);
            }
        for (int z = z0; z < z1; z++)
            for (int y = y0; y < y1; y++) {
                put(out, origin, facing, x0, y, z, wall);
                put(out, origin, facing, x1 - 1, y, z, wall);
            }
    }

    // ---- building generators --------------------------------------------------

    private static void house(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int size = 4 + level * 2; // 6, 8, 10
        int height = 3 + level;
        BlockState wall = level >= 3 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
        BlockState floor = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState roof = level >= 2 ? Blocks.SPRUCE_SLAB.defaultBlockState() : Blocks.OAK_SLAB.defaultBlockState();

        hollowBox(out, origin, facing, 0, 0, 0, size, height, size, wall, floor);
        box(out, origin, facing, 1, height, 1, size - 1, height + 1, size - 1, roof);

        // Door opening + windows.
        int mid = size / 2;
        put(out, origin, facing, mid, 0, 0, Blocks.AIR.defaultBlockState());
        put(out, origin, facing, mid, 1, 0, Blocks.AIR.defaultBlockState());
        if (size >= 6) {
            put(out, origin, facing, 1, 1, 0, Blocks.GLASS_PANE.defaultBlockState());
            put(out, origin, facing, size - 2, 1, 0, Blocks.GLASS_PANE.defaultBlockState());
        }
        // Bonus beds for higher tiers to reflect more residents.
        int beds = level;
        for (int i = 0; i < beds; i++) {
            put(out, origin, facing, 1, 1, 1 + i * 2, Blocks.RED_BED.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                    .setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
        }
    }

    private static void townHall(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int size = 9 + level * 3;
        int height = 5 + level * 2;
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState roof = Blocks.DARK_OAK_SLAB.defaultBlockState();

        hollowBox(out, origin, facing, 0, 0, 0, size, height, size, wall, floor);
        box(out, origin, facing, 0, height, 0, size, height + 1, size, roof);

        int mid = size / 2;
        for (int y = 0; y < 3; y++) put(out, origin, facing, mid, y, 0, Blocks.AIR.defaultBlockState());
        put(out, origin, facing, mid, 2, 1, Blocks.BELL.defaultBlockState());
        put(out, origin, facing, mid - 1, height, mid, Blocks.OAK_FENCE.defaultBlockState());
        put(out, origin, facing, mid - 1, height + 1, mid, Blocks.TORCH.defaultBlockState());
    }

    private static void farm(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int size = 3 + level * 2; // 5, 7, 9
        BlockState farmland = Blocks.FARMLAND.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState wheat = Blocks.WHEAT.defaultBlockState();
        BlockState fence = Blocks.OAK_FENCE.defaultBlockState();

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                boolean edge = x == 0 || z == 0 || x == size - 1 || z == size - 1;
                boolean waterChannel = (x == size / 2) || (z == size / 2);
                if (edge) {
                    put(out, origin, facing, x, 0, z, fence);
                } else if (waterChannel) {
                    put(out, origin, facing, x, -1, z, water);
                } else {
                    put(out, origin, facing, x, -1, z, farmland);
                    put(out, origin, facing, x, 0, z, wheat);
                }
            }
        }
    }

    private static void storage(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int width = 5 + level * 2;
        int depth = 5 + level;
        int height = 3 + level;
        BlockState wall = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState floor = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState roof = Blocks.SPRUCE_SLAB.defaultBlockState();

        hollowBox(out, origin, facing, 0, 0, 0, width, height, depth, wall, floor);
        box(out, origin, facing, 0, height, 0, width, height + 1, depth, roof);
        put(out, origin, facing, width / 2, 0, 0, Blocks.AIR.defaultBlockState());
        put(out, origin, facing, width / 2, 1, 0, Blocks.AIR.defaultBlockState());

        int chests = 2 + level * 2;
        for (int i = 0; i < chests && i < width - 2; i++) {
            put(out, origin, facing, 1 + i, 1, depth - 2, Blocks.CHEST.defaultBlockState());
        }
        put(out, origin, facing, width - 2, 1, 1, Blocks.BARREL.defaultBlockState());
    }

    private static void blacksmith(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int size = 6 + level * 2;
        int height = 4 + level;
        BlockState wall = Blocks.COBBLESTONE.defaultBlockState();
        BlockState floor = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState roof = Blocks.STONE_BRICK_SLAB.defaultBlockState();

        hollowBox(out, origin, facing, 0, 0, 0, size, height, size, wall, floor);
        box(out, origin, facing, 0, height, 0, size, height + 1, size, roof);
        put(out, origin, facing, size / 2, 0, 0, Blocks.AIR.defaultBlockState());
        put(out, origin, facing, size / 2, 1, 0, Blocks.AIR.defaultBlockState());

        put(out, origin, facing, 1, 1, 1, Blocks.FURNACE.defaultBlockState());
        put(out, origin, facing, 2, 1, 1, Blocks.ANVIL.defaultBlockState());
        if (level >= 2) {
            put(out, origin, facing, 3, 1, 1, Blocks.BLAST_FURNACE.defaultBlockState());
            put(out, origin, facing, 1, 1, 2, Blocks.IRON_BLOCK.defaultBlockState());
        }
    }

    private static void market(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int width = 7 + level * 3;
        int depth = 5 + level * 2;
        BlockState floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState post = Blocks.OAK_FENCE.defaultBlockState();
        BlockState roof = Blocks.OAK_SLAB.defaultBlockState();

        box(out, origin, facing, 0, -1, 0, width, 0, depth, floor);
        for (int x = 0; x < width; x += width - 1) {
            for (int z = 0; z < depth; z += depth - 1) {
                box(out, origin, facing, x, 0, z, x + 1, 3, z + 1, post);
            }
        }
        box(out, origin, facing, 0, 3, 0, width, 4, depth, roof);

        int stalls = 2 + level;
        for (int i = 0; i < stalls; i++) {
            int x = 1 + (i * 2) % (width - 2);
            put(out, origin, facing, x, 0, 1, Blocks.HAY_BLOCK.defaultBlockState());
            put(out, origin, facing, x, 1, 1, Blocks.CANDLE.defaultBlockState());
        }
        put(out, origin, facing, width / 2, 0, depth / 2, Blocks.LECTERN.defaultBlockState());
    }

    private static void clinicOrHospital(List<BlockPlacement> out, BlockPos origin, Direction facing, int level, boolean hospital) {
        int size = hospital ? 12 : (6 + level * 2);
        int height = hospital ? 6 : (4 + level);
        BlockState wall = Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState floor = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState roof = Blocks.QUARTZ_SLAB.defaultBlockState();

        hollowBox(out, origin, facing, 0, 0, 0, size, height, size, wall, floor);
        box(out, origin, facing, 0, height, 0, size, height + 1, size, roof);
        put(out, origin, facing, size / 2, 0, 0, Blocks.AIR.defaultBlockState());
        put(out, origin, facing, size / 2, 1, 0, Blocks.AIR.defaultBlockState());

        int beds = hospital ? 6 : 2;
        for (int i = 0; i < beds; i++) {
            int x = 1 + (i * 2) % (size - 2);
            int z = 1 + ((i * 2) / (size - 2)) * 2;
            put(out, origin, facing, x, 1, Math.min(z, size - 2), Blocks.WHITE_BED.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                    .setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
        }
        put(out, origin, facing, size / 2, 1, size - 2, Blocks.BREWING_STAND.defaultBlockState());
    }

    private static void watchtower(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int width = 5;
        int height = 6 + level * 4;
        BlockState wall = Blocks.COBBLESTONE_WALL.defaultBlockState();
        BlockState solidWall = Blocks.COBBLESTONE.defaultBlockState();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    boolean edge = x == 0 || z == 0 || x == width - 1 || z == width - 1;
                    if (edge) put(out, origin, facing, x, y, z, y == height - 1 ? wall : solidWall);
                }
            }
        }
        put(out, origin, facing, width / 2, 0, 0, Blocks.AIR.defaultBlockState());
        put(out, origin, facing, width / 2, 1, 0, Blocks.AIR.defaultBlockState());
        put(out, origin, facing, 1, height - 1, 1, Blocks.TORCH.defaultBlockState());
        put(out, origin, facing, width - 2, height - 1, width - 2, Blocks.TORCH.defaultBlockState());
    }

    private static void wallGate(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int length = 9 + level * 6;
        int height = 3 + level;
        BlockState wall = Blocks.COBBLESTONE_WALL.defaultBlockState();

        for (int x = 0; x < length; x++) {
            int mid = length / 2;
            if (x == mid || x == mid + 1) continue; // gap for the gate
            for (int y = 0; y < height; y++) {
                put(out, origin, facing, x, y, 0, wall);
            }
        }
        put(out, origin, facing, length / 2, 0, 0, Blocks.OAK_FENCE_GATE.defaultBlockState());
        put(out, origin, facing, length / 2 + 1, 0, 0, Blocks.OAK_FENCE_GATE.defaultBlockState());
    }
}
