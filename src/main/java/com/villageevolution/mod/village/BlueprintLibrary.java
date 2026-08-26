package com.villageevolution.mod.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.ArrayList;
import java.util.List;

/**
 * Procedurally generates the block layout for each building type/level.
 *
 * Style: one unified medieval palette - stone-brick footings, timber-framed
 * plaster walls (oak logs as posts and beams with plaster infill), pitched
 * stair roofs, trimmed glass-pane windows, and lantern lighting. Every
 * structure is genuinely hollow, has a real door, and contains at least one
 * villager job-site block so the settlement develops a spread of professions
 * instead of a village of unemployed villagers.
 *
 * Coordinates are local to the building: x = sideways ("width"), y = up,
 * z = forward (the direction the building faces, i.e. its front/door side).
 * transform() rotates local coordinates into world space.
 *
 * Note on AIR: interiors, doorways, and the clearance ring are placed as
 * explicit AIR states. That matters - ConstructionProject#ensureClearQueue
 * treats any non-air world block sitting in an AIR placement as something to
 * demolish, so writing air here is what makes builders hollow out a hillside
 * rather than leaving the building half-buried in it.
 */
public final class BlueprintLibrary {

    private BlueprintLibrary() {}

    // ---- palette --------------------------------------------------------------

    private static final BlockState FOOTING     = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState FOOTING_ALT = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    private static final BlockState POST        = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState BEAM        = Blocks.STRIPPED_OAK_LOG.defaultBlockState();
    private static final BlockState PLASTER     = Blocks.WHITE_TERRACOTTA.defaultBlockState();
    private static final BlockState PLANKS      = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState FLOOR       = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState ROOF        = Blocks.DARK_OAK_STAIRS.defaultBlockState();
    private static final BlockState ROOF_SLAB   = Blocks.DARK_OAK_SLAB.defaultBlockState();
    private static final BlockState PANE        = Blocks.GLASS_PANE.defaultBlockState();
    private static final BlockState LANTERN     = Blocks.LANTERN.defaultBlockState();
    private static final BlockState AIR         = Blocks.AIR.defaultBlockState();
    private static final BlockState PATH        = Blocks.DIRT_PATH.defaultBlockState();

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

    // ---- coordinate helpers ---------------------------------------------------

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

    /** Upper bounds exclusive. */
    private static void fill(List<BlockPlacement> out, BlockPos origin, Direction facing,
                             int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x < x1; x++)
            for (int y = y0; y < y1; y++)
                for (int z = z0; z < z1; z++)
                    put(out, origin, facing, x, y, z, state);
    }

    // ---- structural helpers ---------------------------------------------------

    /**
     * A timber-framed wall shell: stone footing course, corner posts, a beam
     * course under the eaves, plaster infill, and a hollow interior.
     */
    private static void shell(List<BlockPlacement> out, BlockPos origin, Direction facing,
                              int w, int h, int d) {
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++)
                put(out, origin, facing, x, -1, z, ((x + z) % 5 == 0) ? FOOTING_ALT : FOOTING);

        fill(out, origin, facing, 0, 0, 0, w, 1, d, FLOOR);
        fill(out, origin, facing, 1, 1, 1, w - 1, h, d - 1, AIR);

        for (int y = 1; y < h; y++) {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                    if (!edge) continue;
                    boolean corner = (x == 0 || x == w - 1) && (z == 0 || z == d - 1);

                    BlockState state;
                    if (corner) state = POST;
                    else if (y == h - 1) state = BEAM;
                    else if (x % 4 == 0 || z % 4 == 0) state = POST;
                    else if (y == 1) state = PLANKS;
                    else state = PLASTER;
                    put(out, origin, facing, x, y, z, state);
                }
            }
        }
    }

    /**
     * Pitched gable roof, ridge running left-to-right so the slopes face front
     * and back. Sits on the eaves and overhangs one block on every side.
     */
    private static void gableRoof(List<BlockPlacement> out, BlockPos origin, Direction facing,
                                  int w, int baseY, int d) {
        Direction front = facing;
        Direction back = facing.getOpposite();
        int layers = (d + 2) / 2;

        for (int i = 0; i < layers; i++) {
            int y = baseY + i;
            int zLow = -1 + i;
            int zHigh = d - i;
            if (zLow > zHigh) break;

            for (int x = -1; x <= w; x++) {
                if (zLow == zHigh) {
                    put(out, origin, facing, x, y, zLow, ROOF_SLAB);
                } else {
                    put(out, origin, facing, x, y, zLow,
                            ROOF.setValue(BlockStateProperties.HORIZONTAL_FACING, back)
                                .setValue(BlockStateProperties.HALF, Half.BOTTOM));
                    put(out, origin, facing, x, y, zHigh,
                            ROOF.setValue(BlockStateProperties.HORIZONTAL_FACING, front)
                                .setValue(BlockStateProperties.HALF, Half.BOTTOM));
                }
            }
            // Gable ends closed in, loft left hollow.
            for (int z = zLow + 1; z < zHigh; z++) {
                put(out, origin, facing, -1, y, z, PLASTER);
                put(out, origin, facing, w, y, z, PLASTER);
                for (int x = 0; x < w; x++) put(out, origin, facing, x, y, z, AIR);
            }
        }
    }

    /** A working two-high door in the front wall, with an approach path. */
    private static void frontDoor(List<BlockPlacement> out, BlockPos origin, Direction facing, int x) {
        BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState upper = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        put(out, origin, facing, x, 1, 0, lower);
        put(out, origin, facing, x, 2, 0, upper);
        put(out, origin, facing, x, 0, -1, PATH);
        put(out, origin, facing, x, 1, -1, AIR);
        put(out, origin, facing, x, 2, -1, AIR);
    }

    private static void windows(List<BlockPlacement> out, BlockPos origin, Direction facing,
                                int w, int d, int y) {
        for (int z = 2; z < d - 1; z += 3) {
            put(out, origin, facing, 0, y, z, PANE);
            put(out, origin, facing, w - 1, y, z, PANE);
        }
        for (int x = 2; x < w - 1; x += 3) {
            put(out, origin, facing, x, y, d - 1, PANE);
        }
    }

    /** A ring of walking space cleared around the outside, paved. */
    private static void clearance(List<BlockPlacement> out, BlockPos origin, Direction facing,
                                  int w, int d) {
        for (int x = -2; x <= w + 1; x++) {
            for (int z = -2; z <= d + 1; z++) {
                if (x >= -1 && x <= w && z >= -1 && z <= d) continue;
                put(out, origin, facing, x, 0, z, PATH);
                put(out, origin, facing, x, 1, z, AIR);
                put(out, origin, facing, x, 2, z, AIR);
            }
        }
    }

    private static void lanterns(List<BlockPlacement> out, BlockPos origin, Direction facing,
                                 int w, int h, int d) {
        put(out, origin, facing, 1, h - 1, 1, LANTERN);
        put(out, origin, facing, w - 2, h - 1, d - 2, LANTERN);
    }

    private static void bed(List<BlockPlacement> out, BlockPos origin, Direction facing,
                            int x, int z, BlockState bedColour) {
        put(out, origin, facing, x, 1, z, bedColour
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
        put(out, origin, facing, x, 1, z + 1, bedColour
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
    }

    /**
     * Houses rotate through the job sites no dedicated building supplies,
     * picked deterministically from the plot position, so a village ends up
     * with a mix of trades rather than a street of identical ones.
     */
    private static BlockState houseWorkstation(BlockPos origin) {
        BlockState[] options = {
                Blocks.SMOKER.defaultBlockState(),             // butcher
                Blocks.STONECUTTER.defaultBlockState(),        // mason
                Blocks.LOOM.defaultBlockState(),               // shepherd
                Blocks.CARTOGRAPHY_TABLE.defaultBlockState(),  // cartographer
                Blocks.FLETCHING_TABLE.defaultBlockState(),    // fletcher
                Blocks.CAULDRON.defaultBlockState(),           // leatherworker
        };
        int hash = Math.abs(origin.getX() * 31 + origin.getZ() * 17);
        return options[hash % options.length];
    }

    // ---- building generators --------------------------------------------------

    private static void house(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int w = 5 + level * 2;
        int d = 6 + level * 2;
        int h = 4 + (level > 2 ? 1 : 0);

        clearance(out, origin, facing, w, d);
        shell(out, origin, facing, w, h, d);
        gableRoof(out, origin, facing, w, h, d);
        frontDoor(out, origin, facing, w / 2);
        windows(out, origin, facing, w, d, 2);
        lanterns(out, origin, facing, w, h, d);

        for (int i = 0; i < level && 1 + i * 2 < w - 1; i++) {
            bed(out, origin, facing, 1 + i * 2, d - 3, Blocks.RED_BED.defaultBlockState());
        }

        put(out, origin, facing, w - 2, 1, 1, Blocks.CRAFTING_TABLE.defaultBlockState());
        put(out, origin, facing, w - 2, 1, 2, Blocks.BARREL.defaultBlockState());
        put(out, origin, facing, 1, 1, d - 2, Blocks.FURNACE.defaultBlockState());
        put(out, origin, facing, 2, 1, 1, houseWorkstation(origin));

        if (level >= 2) {
            put(out, origin, facing, 1, 1, 2, Blocks.OAK_STAIRS.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
        }
    }

    private static void townHall(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int w = 11 + level * 2;
        int d = 11 + level * 2;
        int h = 6 + level;

        clearance(out, origin, facing, w, d);
        shell(out, origin, facing, w, h, d);
        gableRoof(out, origin, facing, w, h, d);
        windows(out, origin, facing, w, d, 3);
        lanterns(out, origin, facing, w, h, d);

        int mid = w / 2;
        for (int x = mid - 1; x <= mid + 1; x++) {
            for (int y = 1; y <= 3; y++) put(out, origin, facing, x, y, 0, AIR);
            put(out, origin, facing, x, 0, -1, PATH);
            put(out, origin, facing, x, 1, -1, AIR);
            put(out, origin, facing, x, 2, -1, AIR);
        }
        put(out, origin, facing, mid - 2, 4, 0, LANTERN);
        put(out, origin, facing, mid + 2, 4, 0, LANTERN);

        put(out, origin, facing, mid, 1, d - 3, Blocks.BELL.defaultBlockState());
        put(out, origin, facing, mid - 2, 1, d - 3, Blocks.LECTERN.defaultBlockState());
        put(out, origin, facing, mid + 2, 1, d - 3, Blocks.BOOKSHELF.defaultBlockState());
        for (int x = 3; x < w - 3; x += 2) {
            put(out, origin, facing, x, 1, d - 5, Blocks.OAK_STAIRS.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing.getOpposite()));
        }
    }

    private static void farm(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int size = 7 + level * 2;

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                boolean edge = x == 0 || z == 0 || x == size - 1 || z == size - 1;
                boolean channel = (x == size / 2) || (z == size / 2);
                put(out, origin, facing, x, 1, z, AIR);
                put(out, origin, facing, x, 2, z, AIR);
                if (edge) {
                    put(out, origin, facing, x, 0, z, Blocks.OAK_FENCE.defaultBlockState());
                } else if (channel) {
                    put(out, origin, facing, x, -1, z, Blocks.WATER.defaultBlockState());
                    put(out, origin, facing, x, 0, z, AIR);
                } else {
                    put(out, origin, facing, x, -1, z, Blocks.FARMLAND.defaultBlockState());
                    put(out, origin, facing, x, 0, z, Blocks.WHEAT.defaultBlockState());
                }
            }
        }
        put(out, origin, facing, size / 2 + 1, 0, 0, Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing));

        // Barn beside the field, holding the farmer's composter.
        int bw = 5, bd = 4, bh = 4;
        for (int x = 0; x < bw; x++)
            for (int z = 0; z < bd; z++) {
                put(out, origin, facing, size + 1 + x, -1, z, FOOTING);
                put(out, origin, facing, size + 1 + x, 0, z, FLOOR);
            }
        for (int y = 1; y < bh; y++)
            for (int x = 0; x < bw; x++)
                for (int z = 0; z < bd; z++) {
                    boolean edge = x == 0 || z == 0 || x == bw - 1 || z == bd - 1;
                    boolean corner = (x == 0 || x == bw - 1) && (z == 0 || z == bd - 1);
                    boolean openFront = z == 0 && x > 0 && x < bw - 1;
                    BlockState state = !edge || openFront ? AIR
                            : corner ? POST
                            : y == bh - 1 ? BEAM : PLANKS;
                    put(out, origin, facing, size + 1 + x, y, z, state);
                }
        for (int x = -1; x <= bw; x++)
            put(out, origin, facing, size + 1 + x, bh, bd / 2, ROOF_SLAB);

        put(out, origin, facing, size + 2, 1, bd - 2, Blocks.COMPOSTER.defaultBlockState()); // farmer
        put(out, origin, facing, size + 3, 1, bd - 2, Blocks.HAY_BLOCK.defaultBlockState());
        put(out, origin, facing, size + 4, 1, bd - 2, Blocks.BARREL.defaultBlockState());
        put(out, origin, facing, size + 2, bh - 1, 1, LANTERN);
    }

    private static void storage(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int w = 7 + level * 2;
        int d = 7 + level;
        int h = 5;

        clearance(out, origin, facing, w, d);
        shell(out, origin, facing, w, h, d);
        gableRoof(out, origin, facing, w, h, d);
        frontDoor(out, origin, facing, w / 2);
        windows(out, origin, facing, w, d, 3);
        lanterns(out, origin, facing, w, h, d);

        for (int z = 2; z < d - 2; z++) {
            put(out, origin, facing, 1, 1, z, Blocks.BARREL.defaultBlockState());
            put(out, origin, facing, w - 2, 1, z, Blocks.CHEST.defaultBlockState());
            if (level >= 2) put(out, origin, facing, 1, 2, z, Blocks.BARREL.defaultBlockState());
        }
        put(out, origin, facing, 2, 1, d - 2, Blocks.BARREL.defaultBlockState());            // fisherman
        put(out, origin, facing, w - 3, 1, d - 2, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
    }

    private static void blacksmith(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int w = 8 + level * 2;
        int d = 8 + level;
        int h = 5;

        clearance(out, origin, facing, w, d);
        shell(out, origin, facing, w, h, d);
        gableRoof(out, origin, facing, w, h, d);
        frontDoor(out, origin, facing, w / 2);
        windows(out, origin, facing, w, d, 3);
        lanterns(out, origin, facing, w, h, d);

        put(out, origin, facing, 1, 1, d - 2, Blocks.BLAST_FURNACE.defaultBlockState());   // armorer
        put(out, origin, facing, 2, 1, d - 2, Blocks.FURNACE.defaultBlockState());
        put(out, origin, facing, 3, 1, d - 2, Blocks.SMITHING_TABLE.defaultBlockState());  // toolsmith
        put(out, origin, facing, 4, 1, d - 2, Blocks.GRINDSTONE.defaultBlockState());      // weaponsmith
        put(out, origin, facing, 1, 1, 1, Blocks.ANVIL.defaultBlockState());
        put(out, origin, facing, 2, 1, 1, Blocks.CAULDRON.defaultBlockState());
        put(out, origin, facing, w - 2, 1, 2, Blocks.COAL_BLOCK.defaultBlockState());

        for (int y = 1; y < h + 4; y++) {
            put(out, origin, facing, 1, y, d - 1, Blocks.BRICKS.defaultBlockState());
        }
        put(out, origin, facing, 1, h + 4, d - 1, Blocks.CAMPFIRE.defaultBlockState());
    }

    private static void market(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int w = 9 + level * 2;
        int d = 7 + level * 2;

        for (int x = -1; x <= w; x++)
            for (int z = -1; z <= d; z++) {
                put(out, origin, facing, x, -1, z, Blocks.STONE_BRICKS.defaultBlockState());
                for (int y = 0; y < 4; y++) put(out, origin, facing, x, y, z, AIR);
            }

        for (int x = 0; x < w; x += w - 1)
            for (int z = 0; z < d; z += Math.max(1, d - 1))
                for (int y = 0; y < 4; y++)
                    put(out, origin, facing, x, y, z, POST);

        for (int x = -1; x <= w; x++)
            for (int z = -1; z <= d; z++)
                put(out, origin, facing, x, 4, z, ROOF_SLAB);

        put(out, origin, facing, 1, 0, 1, Blocks.LOOM.defaultBlockState());               // shepherd
        put(out, origin, facing, 3, 0, 1, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());  // cartographer
        put(out, origin, facing, 5, 0, 1, Blocks.FLETCHING_TABLE.defaultBlockState());    // fletcher
        put(out, origin, facing, w - 2, 0, d - 2, Blocks.SMOKER.defaultBlockState());     // butcher
        for (int x = 1; x < w - 1; x += 2) {
            put(out, origin, facing, x, 0, d - 2, Blocks.HAY_BLOCK.defaultBlockState());
            put(out, origin, facing, x, 3, d - 2, LANTERN);
        }
        put(out, origin, facing, w / 2, 0, d / 2, Blocks.BELL.defaultBlockState());
    }

    private static void clinicOrHospital(List<BlockPlacement> out, BlockPos origin, Direction facing,
                                         int level, boolean hospital) {
        int w = hospital ? 13 : 7 + level * 2;
        int d = hospital ? 11 : 7 + level;
        int h = hospital ? 6 : 5;

        clearance(out, origin, facing, w, d);
        shell(out, origin, facing, w, h, d);
        gableRoof(out, origin, facing, w, h, d);
        frontDoor(out, origin, facing, w / 2);
        windows(out, origin, facing, w, d, 3);
        lanterns(out, origin, facing, w, h, d);

        int beds = hospital ? 6 : 2;
        for (int i = 0; i < beds; i++) {
            int x = 1 + (i % 3) * 2;
            int z = 2 + (i / 3) * 3;
            if (x < w - 1 && z + 1 < d - 1) {
                bed(out, origin, facing, x, z, Blocks.WHITE_BED.defaultBlockState());
            }
        }
        put(out, origin, facing, w - 2, 1, d - 2, Blocks.BREWING_STAND.defaultBlockState()); // cleric
        put(out, origin, facing, w - 3, 1, d - 2, Blocks.CAULDRON.defaultBlockState());      // leatherworker
        put(out, origin, facing, w - 2, 1, 1, Blocks.BOOKSHELF.defaultBlockState());
    }

    private static void watchtower(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int w = 5;
        int h = 8 + level * 3;

        clearance(out, origin, facing, w, w);
        for (int x = 0; x < w; x++)
            for (int z = 0; z < w; z++)
                put(out, origin, facing, x, -1, z, FOOTING);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < w; z++) {
                    boolean edge = x == 0 || z == 0 || x == w - 1 || z == w - 1;
                    boolean corner = (x == 0 || x == w - 1) && (z == 0 || z == w - 1);
                    if (!edge) {
                        put(out, origin, facing, x, y, z, y == 0 ? FLOOR : AIR);
                    } else if (y >= h - 2) {
                        put(out, origin, facing, x, y, z, Blocks.COBBLESTONE_WALL.defaultBlockState());
                    } else if (corner) {
                        put(out, origin, facing, x, y, z, POST);
                    } else if (y % 4 == 3) {
                        put(out, origin, facing, x, y, z, PANE);
                    } else {
                        put(out, origin, facing, x, y, z, Blocks.COBBLESTONE.defaultBlockState());
                    }
                }
            }
        }

        for (int y = 1; y < h - 2; y++) {
            put(out, origin, facing, w / 2, y, w - 2, Blocks.LADDER.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
        }
        frontDoor(out, origin, facing, w / 2);
        put(out, origin, facing, 1, h - 3, 1, LANTERN);
        put(out, origin, facing, w - 2, h - 3, w - 2, LANTERN);
        put(out, origin, facing, 1, 1, 1, Blocks.FLETCHING_TABLE.defaultBlockState()); // fletcher
    }

    private static void wallGate(List<BlockPlacement> out, BlockPos origin, Direction facing, int level) {
        int length = 11 + level * 6;
        int h = 4 + level;
        int mid = length / 2;

        for (int x = 0; x < length; x++) {
            put(out, origin, facing, x, -1, 0, FOOTING);
            if (x >= mid - 1 && x <= mid + 1) {
                for (int y = 0; y < 3; y++) put(out, origin, facing, x, y, 0, AIR);
                put(out, origin, facing, x, 0, -1, PATH);
                put(out, origin, facing, x, 0, 1, PATH);
                continue;
            }
            for (int y = 0; y < h; y++) {
                put(out, origin, facing, x, y, 0,
                        y == h - 1 ? Blocks.COBBLESTONE_WALL.defaultBlockState()
                                   : Blocks.STONE_BRICKS.defaultBlockState());
            }
            if (x % 6 == 0) put(out, origin, facing, x, h, 0, LANTERN);
        }

        for (int y = 0; y < h + 1; y++) {
            put(out, origin, facing, mid - 2, y, 0, POST);
            put(out, origin, facing, mid + 2, y, 0, POST);
        }
        for (int x = mid - 1; x <= mid + 1; x++) {
            put(out, origin, facing, x, 3, 0, BEAM);
            for (int y = 4; y < h; y++) {
                put(out, origin, facing, x, y, 0, Blocks.STONE_BRICKS.defaultBlockState());
            }
            put(out, origin, facing, x, 0, 0, Blocks.OAK_FENCE_GATE.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
        }
        put(out, origin, facing, mid - 3, 1, 1, Blocks.GRINDSTONE.defaultBlockState()); // weaponsmith
        put(out, origin, facing, mid - 2, h, 0, LANTERN);
        put(out, origin, facing, mid + 2, h, 0, LANTERN);
    }
}
