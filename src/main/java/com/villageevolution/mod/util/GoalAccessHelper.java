package com.villageevolution.mod.util;

import com.villageevolution.mod.VillagerEvolutionMod;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Small reflection helper that lets us add custom AI goals to vanilla mobs
 * (like Villager) without needing an Access Transformer file. Forge has run
 * on Mojang's official mapping names at runtime since 1.17, so the plain
 * field name "goalSelector" resolves correctly both in the dev environment
 * and in a production/obfuscated install.
 *
 * If this ever throws NoSuchFieldException after a Minecraft update, check
 * the current field name for GoalSelector inside net.minecraft.world.entity.Mob
 * and update FIELD_NAME below to match.
 */
public final class GoalAccessHelper {

    private static final String FIELD_NAME = "goalSelector";
    private static final String TARGET_FIELD_NAME = "targetSelector";
    private static Field goalSelectorField;
    private static Field targetSelectorField;

    // Tracks which goal classes we've already added to which mob instances,
    // so re-firing EntityJoinLevelEvent (e.g. on dimension change) doesn't
    // stack duplicate goals onto the same entity.
    private static final Map<Mob, Set<Class<?>>> PATCHED = new WeakHashMap<>();

    private GoalAccessHelper() {
    }

    public static void addGoal(Mob mob, int priority, Goal goal) {
        Set<Class<?>> alreadyAdded = PATCHED.computeIfAbsent(mob, m -> new HashSet<>());
        if (!alreadyAdded.add(goal.getClass())) return;

        try {
            GoalSelector selector = getGoalSelector(mob);
            if (selector != null) {
                selector.addGoal(priority, goal);
            }
        } catch (ReflectiveOperationException e) {
            VillagerEvolutionMod.LOGGER.error("Failed to add custom AI goal {} to {}",
                    goal.getClass().getSimpleName(), mob, e);
        }
    }

    /** Same as addGoal, but for the target selector (what the mob decides to attack). */
    public static void addTargetGoal(Mob mob, int priority, Goal goal) {
        Set<Class<?>> alreadyAdded = PATCHED.computeIfAbsent(mob, m -> new HashSet<>());
        if (!alreadyAdded.add(goal.getClass())) return;

        try {
            if (targetSelectorField == null) {
                Field field = Mob.class.getDeclaredField(TARGET_FIELD_NAME);
                field.setAccessible(true);
                targetSelectorField = field;
            }
            GoalSelector selector = (GoalSelector) targetSelectorField.get(mob);
            if (selector != null) {
                selector.addGoal(priority, goal);
            }
        } catch (ReflectiveOperationException e) {
            VillagerEvolutionMod.LOGGER.error("Failed to add custom target goal {} to {}",
                    goal.getClass().getSimpleName(), mob, e);
        }
    }

    private static GoalSelector getGoalSelector(Mob mob) throws ReflectiveOperationException {
        if (goalSelectorField == null) {
            Field field = Mob.class.getDeclaredField(FIELD_NAME);
            field.setAccessible(true);
            goalSelectorField = field;
        }
        return (GoalSelector) goalSelectorField.get(mob);
    }
}
