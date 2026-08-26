package com.villageevolution.mod.block;

import net.minecraft.util.StringRepresentable;

/**
 * The eight chunk loader tiers.
 *
 * Coverage is always a centred square of (2*radius+1)^2 chunks, so the loaded
 * area is symmetrical around the block. A centred square can only have an odd
 * side length, which means the chunk count jumps 1 -> 9 -> 25 -> 49 -> 81 ->
 * 169 -> 289 -> 625. From tier 3 upward each step is close to 2x the previous
 * area (1.96x, 1.65x, 2.09x, 1.71x, 2.16x); the tier 1 -> 2 step is
 * unavoidably 9x, because the smallest centred square above 1x1 is 3x3.
 *
 * Recipes follow a fixed shape - four corner items, four edge items, and the
 * previous tier's loader in the centre - so upgrading is always a chain
 * rather than eight unrelated recipes.
 */
public enum ChunkLoaderTier implements StringRepresentable {

    //     id                display name           radius  corner item              edge item
    BASIC   ("basic",        "Basic",                   0, "minecraft:redstone",        "minecraft:copper_ingot"),
    IMPROVED("improved",     "Improved",                1, "minecraft:redstone",        "minecraft:gold_ingot"),
    ADVANCED("advanced",     "Advanced",                2, "minecraft:lapis_lazuli",    "minecraft:gold_block"),
    GREATER ("greater",      "Greater",                 3, "minecraft:emerald",         "minecraft:obsidian"),
    SUPERIOR("superior",     "Superior",                4, "minecraft:diamond",         "minecraft:emerald_block"),
    ELITE   ("elite",        "Elite",                   6, "minecraft:ender_eye",       "minecraft:obsidian"),
    ULTIMATE("ultimate",     "Ultimate",                8, "minecraft:diamond_block",   "minecraft:echo_shard"),
    NETHERITE("netherite",   "Netherite",              12, "minecraft:netherite_ingot", "minecraft:nether_star");

    private final String id;
    private final String displayName;
    private final int defaultRadius;
    private final String cornerItem;
    private final String edgeItem;

    ChunkLoaderTier(String id, String displayName, int defaultRadius, String cornerItem, String edgeItem) {
        this.id = id;
        this.displayName = displayName;
        this.defaultRadius = defaultRadius;
        this.cornerItem = cornerItem;
        this.edgeItem = edgeItem;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    /** Chunks out from the centre. Side length is 2*radius+1; area is that squared. */
    public int getDefaultRadius() { return defaultRadius; }

    public int getDefaultSide() { return defaultRadius * 2 + 1; }
    public int getDefaultChunkCount() { return getDefaultSide() * getDefaultSide(); }

    public String getCornerItem() { return cornerItem; }
    public String getEdgeItem() { return edgeItem; }

    /** The tier this one is crafted from, or null for the first tier. */
    public ChunkLoaderTier previous() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }

    public String registryName() {
        return "chunk_loader_" + id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
