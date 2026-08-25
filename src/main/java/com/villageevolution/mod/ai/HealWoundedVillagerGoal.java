package com.villageevolution.mod.ai;

import com.villageevolution.mod.util.VillagerInjuryHelper;
import com.villageevolution.mod.village.VillageInstance;
import com.villageevolution.mod.village.VillageManager;
import com.villageevolution.mod.village.VillageSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * "Clerics heal injured villagers". Cleric villagers act as the village
 * medic: they prioritize whichever wounded villager is most critically
 * injured (see VillagerInjuryHelper), not just the nearest one, then walk
 * over, heal them, and apply Regeneration.
 */
public class HealWoundedVillagerGoal extends Goal {

    private static final double SEARCH_RADIUS = 16.0;
    private static final double HEAL_RANGE = 2.5;
    private static final int HEAL_INTERVAL = 20;
    private static final float HEAL_AMOUNT = 3.0F;
    private static final int MAX_PURSUIT_TICKS = 600;

    private final Villager cleric;
    private Villager patient;
    private int healCooldown;
    private int pursuitTicks;

    public HealWoundedVillagerGoal(Villager cleric) {
        this.cleric = cleric;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cleric.isBaby() || cleric.isSleeping()) return false;
        if (cleric.getRandom().nextInt(60) != 0) return false;

        List<Villager> wounded = cleric.level().getEntitiesOfClass(
                Villager.class,
                cleric.getBoundingBox().inflate(SEARCH_RADIUS),
                v -> v != cleric && v.isAlive() && VillagerInjuryHelper.isWounded(v)
        );
        if (wounded.isEmpty()) return false;

        // Most critically injured first; ties broken by distance.
        wounded.sort(Comparator
                .comparingDouble(VillagerInjuryHelper::severity).reversed()
                .thenComparingDouble(v -> v.distanceToSqr(cleric)));
        this.patient = wounded.get(0);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return patient != null
                && patient.isAlive()
                && VillagerInjuryHelper.isWounded(patient)
                && pursuitTicks < MAX_PURSUIT_TICKS;
    }

    @Override
    public void start() {
        pursuitTicks = 0;
        healCooldown = 0;
    }

    @Override
    public void stop() {
        patient = null;
        cleric.getNavigation().stop();
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public void tick() {
        if (patient == null) return;
        pursuitTicks++;
        cleric.getLookControl().setLookAt(patient, 30.0F, 30.0F);

        if (cleric.distanceToSqr(patient) > HEAL_RANGE * HEAL_RANGE) {
            cleric.getNavigation().moveTo(patient, 0.5D);
            return;
        }

        cleric.getNavigation().stop();
        if (--healCooldown <= 0) {
            healCooldown = HEAL_INTERVAL;
            boolean wasWounded = VillagerInjuryHelper.isWounded(patient);
            patient.heal(HEAL_AMOUNT);
            patient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));

            if (cleric.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EFFECT,
                        patient.getX(), patient.getY() + patient.getBbHeight() + 0.2, patient.getZ(),
                        6, 0.3, 0.2, 0.3, 0.02);

                if (wasWounded && !VillagerInjuryHelper.isWounded(patient)) {
                    VillageInstance village = VillageSavedData.get(serverLevel)
                            .findNear(cleric.blockPosition(), VillageManager.SEARCH_RADIUS);
                    if (village != null) village.getStatistics().incrementVillagersHealed();
                }
            }
            cleric.level().playSound(null, cleric.blockPosition(),
                    SoundEvents.HONEY_DRINK, SoundSource.NEUTRAL, 0.5F, 1.4F);
        }
    }
}
