package com.villageevolution.mod.util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;

/**
 * Tracks whether a villager counts as "critically injured" (below 40% max
 * health). This doesn't add a new game-mechanical health system - it's a
 * classification layer on top of vanilla health used to (a) prioritize
 * which patient a cleric goes to first, and (b) show a periodic visual cue
 * so players can spot who needs help.
 */
public final class VillagerInjuryHelper {

    private static final float INJURED_THRESHOLD = 0.4F;

    private VillagerInjuryHelper() {}

    public static boolean isInjured(Villager villager) {
        return villager.getHealth() < villager.getMaxHealth() * INJURED_THRESHOLD;
    }

    public static boolean isWounded(Villager villager) {
        return villager.getHealth() < villager.getMaxHealth();
    }

    /** Severity score used to pick which patient a cleric treats first: lower health % = higher priority. */
    public static double severity(Villager villager) {
        return 1.0 - (villager.getHealth() / (double) villager.getMaxHealth());
    }

    public static void showInjuryParticles(Villager villager) {
        if (villager.level() instanceof ServerLevel serverLevel && isInjured(villager)) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    villager.getX(), villager.getY() + villager.getBbHeight() + 0.3, villager.getZ(),
                    1, 0.1, 0.1, 0.1, 0.0);
        }
    }
}
