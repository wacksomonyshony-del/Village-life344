package com.villageevolution.mod;

import com.villageevolution.mod.block.ChunkLoaderTier;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.Map;

/**
 * Server-side configuration, written to config/villageevolution-common.toml on
 * first run. Chunk loader coverage is exposed here so server owners can shrink
 * or disable the larger tiers - a tier 8 loader forces 625 ticking chunks,
 * which is a serious performance commitment.
 */
public final class ModConfigs {

    public static final ForgeConfigSpec SPEC;

    /** Hard ceiling applied to every tier, regardless of its own setting. */
    public static final ForgeConfigSpec.IntValue MAX_RADIUS;

    private static final Map<ChunkLoaderTier, ForgeConfigSpec.IntValue> TIER_RADIUS =
            new EnumMap<>(ChunkLoaderTier.class);

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Chunk loader coverage.",
                        "Radius is in chunks out from the loader. A radius of R loads a",
                        "centred square of (2R+1)^2 chunks - radius 0 is 1 chunk (16x16 blocks),",
                        "radius 1 is 9 chunks (48x48 blocks), radius 12 is 625 chunks (400x400 blocks).",
                        "Every loaded chunk is a FULLY TICKING chunk, so large values are expensive.")
               .push("chunk_loaders");

        MAX_RADIUS = builder
                .comment("Maximum radius any tier may use, whatever its own setting says.",
                         "Lower this to cap chunk loading server-wide. Set to 0 to restrict",
                         "every tier to a single chunk.")
                .defineInRange("max_radius", 12, 0, 32);

        for (ChunkLoaderTier tier : ChunkLoaderTier.values()) {
            TIER_RADIUS.put(tier, builder
                    .comment("Radius for the " + tier.getDisplayName() + " Chunk Loader."
                            + " Default " + tier.getDefaultRadius()
                            + " (" + tier.getDefaultSide() + "x" + tier.getDefaultSide()
                            + " = " + tier.getDefaultChunkCount() + " chunks).")
                    .defineInRange("radius_" + tier.getId(), tier.getDefaultRadius(), 0, 32));
        }

        builder.pop();
        SPEC = builder.build();
    }

    /** Configured radius for a tier, clamped by max_radius. */
    public static int radiusFor(ChunkLoaderTier tier) {
        ForgeConfigSpec.IntValue value = TIER_RADIUS.get(tier);
        int configured = value == null ? tier.getDefaultRadius() : value.get();
        return Math.min(configured, MAX_RADIUS.get());
    }

    private ModConfigs() {}
}
