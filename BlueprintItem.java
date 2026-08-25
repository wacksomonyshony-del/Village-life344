package com.villageevolution.mod.ai;

import com.villageevolution.mod.village.VillageInstance;
import com.villageevolution.mod.village.VillageManager;
import com.villageevolution.mod.village.VillageSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * "Villagers repair damaged iron golems" / "Blacksmiths specialize in golem
 * repairs". Any villager can patch up a wounded golem, but blacksmith-type
 * villagers (armorer, weaponsmith, toolsmith - the ones who actually work
 * iron) do it faster, in bigger chunks, and from further away, reflecting
 * their trade specialty.
 */
public class RepairIronGolemGoal extends Goal {

    private static final double BASE_SEARCH_RADIUS = 16.0;
    private static final double SPECIALIST_SEARCH_RADIUS = 24.0;
    private static final double HEAL_RANGE = 2.5;
    private static final int BASE_HEAL_INTERVAL = 20;
    private static final int SPECIALIST_HEAL_INTERVAL = 12;
    private static final float BASE_HEAL_AMOUNT = 4.0F;
    private static final float SPECIALIST_HEAL_AMOUNT = 8.0F;
    private static final int MAX_PURSUIT_TICKS = 600;

    private final Villager villager;
    private final boolean specialist;
    private IronGolem target;
    private int healCooldown;
    private int pursuitTicks;

    public RepairIronGolemGoal(Villager villager) {
        this.villager = villager;
        this.specialist = isBlacksmithProfession(villager);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static boolean isBlacksmithProfession(Villager villager) {
        VillagerProfession profession = villager.getVillagerData().getProfession();
        return profession == VillagerProfession.ARMORER
                || profession == VillagerProfession.WEAPONSMITH
                || profession == VillagerProfession.TOOLSMITH;
    }

    @Override
    public boolean canUse() {
        if (villager.isBaby() || villager.isSleeping()) return false;
        if (villager.getRandom().nextInt(specialist ? 50 : 80) != 0) return false;

        double radius = specialist ? SPECIALIST_SEARCH_RADIUS : BASE_SEARCH_RADIUS;
        List<IronGolem> nearby = villager.level().getEntitiesOfClass(
                IronGolem.class,
                villager.getBoundingBox().inflate(radius),
                golem -> golem.isAlive() && golem.getHealth() < golem.getMaxHealth()
        );
        if (nearby.isEmpty()) return false;

        nearby.sort(Comparator.comparingDouble(g -> g.distanceToSqr(villager)));
        this.target = nearby.get(0);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive()
                && target.getHealth() < target.getMaxHealth()
                && pursuitTicks < MAX_PURSUIT_TICKS;
    }

    @Override
    public void start() {
        pursuitTicks = 0;
        healCooldown = 0;
    }

    @Override
    public void stop() {
        target = null;
        villager.getNavigation().stop();
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) return;
        pursuitTicks++;
        villager.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (villager.distanceToSqr(target) > HEAL_RANGE * HEAL_RANGE) {
            villager.getNavigation().moveTo(target, 0.5D);
            return;
        }

        villager.getNavigation().stop();
        if (--healCooldown <= 0) {
            healCooldown = specialist ? SPECIALIST_HEAL_INTERVAL : BASE_HEAL_INTERVAL;
            float amount = specialist ? SPECIALIST_HEAL_AMOUNT : BASE_HEAL_AMOUNT;
            boolean wasDamaged = target.getHealth() < target.getMaxHealth();
            target.heal(amount);

            if (villager.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(specialist ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.HEART,
                        target.getX(), target.getY() + target.getBbHeight() + 0.2, target.getZ(),
                        specialist ? 6 : 3, 0.3, 0.2, 0.3, 0.0);

                if (wasDamaged && target.getHealth() >= target.getMaxHealth()) {
                    VillageInstance village = VillageSavedData.get(serverLevel)
                            .findNear(villager.blockPosition(), VillageManager.SEARCH_RADIUS);
                    if (village != null) village.getStatistics().incrementGolemsRepaired();
                }
            }
            villager.level().playSound(null, villager.blockPosition(),
                    specialist ? SoundEvents.ANVIL_USE : SoundEvents.VILLAGER_WORK_MASON,
                    SoundSource.NEUTRAL, 0.4F, specialist ? 1.0F : 1.2F);
        }
    }
}
