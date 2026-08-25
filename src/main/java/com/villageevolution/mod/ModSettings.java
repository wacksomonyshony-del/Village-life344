package com.villageevolution.mod;

/**
 * Central switches for the mod's behaviour. Kept as plain constants rather
 * than a config file so there is exactly one place to look.
 */
public final class ModSettings {

    /**
     * When true, villagers are treated as having an unlimited supply of any
     * block ("creative inventory"): construction projects start with their
     * materials already satisfied, so nobody has to go mine for them. This
     * also means villagers never break blocks to harvest resources - the
     * only blocks they break are the ones standing where a building is
     * about to go (see ConstructionProject#ensureClearQueue).
     */
    public static final boolean CREATIVE_MATERIALS = true;

    /** How many construction projects a single village may START per Minecraft day. */
    public static final int PROJECTS_PER_DAY = 2;

    /** How many projects a village may have running at the same time. */
    public static final int MAX_CONCURRENT_PROJECTS = 2;

    private ModSettings() {}
}
