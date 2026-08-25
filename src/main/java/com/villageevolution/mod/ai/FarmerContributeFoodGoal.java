package com.villageevolution.mod.ai;

import com.villageevolution.mod.util.VillagerTaskData;
import com.villageevolution.mod.village.ResourceType;
import com.villageevolution.mod.village.VillageInstance;
import com.villageevolution.mod.village.VillageManager;
import com.villageevolution.mod.village.VillageSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.List;

/**
 * Profession-specific responsibility: farmer villagers periodically walk
 * their harvested crops back to the village's food stockpile instead of
 * just hoarding them in their trading inventory.
 */
public class FarmerContributeFoodGoal extends Goal {

    private static final List<Item> FOOD_ITEMS = List.of(
            Items.WHEAT, Items.CARROT, Items.POTATO, Items.BEETROOT, Items.BREAD);
    private static final int CONTRIBUTE_THRESHOLD = 6;

    private final Villager farmer;
    private BlockPos targetAnchor;
    private boolean delivering;

    public FarmerContributeFoodGoal(Villager farmer) {
        this.farmer = farmer;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (farmer.isBaby()) return false;
        if (VillagerTaskData.getTask(farmer) != VillagerTaskData.Task.IDLE) return false;
        if (farmer.getRandom().nextInt(100) != 0) return false;
        return countCarriedFood() >= CONTRIBUTE_THRESHOLD && findNearestVillageAnchor() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return delivering && targetAnchor != null;
    }

    @Override
    public void start() {
        delivering = true;
        targetAnchor = findNearestVillageAnchor();
    }

    @Override
    public void stop() {
        delivering = false;
        farmer.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetAnchor == null || !(farmer.level() instanceof ServerLevel level)) {
            delivering = false;
            return;
        }

        double distSqr = farmer.blockPosition().distSqr(targetAnchor);
        if (distSqr > 3.0 * 3.0) {
            farmer.getNavigation().moveTo(targetAnchor.getX() + 0.5, targetAnchor.getY(), targetAnchor.getZ() + 0.5, 0.5D);
            return;
        }

        farmer.getNavigation().stop();
        int deposited = depositFood();
        if (deposited > 0) {
            VillageInstance village = VillageSavedData.get(level).findNear(targetAnchor, VillageManager.SEARCH_RADIUS);
            if (village != null) {
                village.addResource(ResourceType.FOOD, deposited);
                village.getStatistics().addFoodProduced(deposited);
            }
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    farmer.getX(), farmer.getY() + 1, farmer.getZ(), 8, 0.4, 0.3, 0.4, 0.02);
            level.playSound(null, farmer.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.6F, 1.0F);
        }
        delivering = false;
    }

    private int countCarriedFood() {
        int total = 0;
        for (int i = 0; i < farmer.getInventory().getContainerSize(); i++) {
            ItemStack stack = farmer.getInventory().getItem(i);
            if (FOOD_ITEMS.contains(stack.getItem())) total += stack.getCount();
        }
        return total;
    }

    private int depositFood() {
        int deposited = 0;
        for (int i = 0; i < farmer.getInventory().getContainerSize(); i++) {
            ItemStack stack = farmer.getInventory().getItem(i);
            if (FOOD_ITEMS.contains(stack.getItem()) && !stack.isEmpty()) {
                deposited += stack.getCount();
                farmer.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        return deposited;
    }

    private BlockPos findNearestVillageAnchor() {
        if (!(farmer.level() instanceof ServerLevel level)) return null;
        VillageInstance village = VillageSavedData.get(level).findNear(farmer.blockPosition(), VillageManager.SEARCH_RADIUS);
        return village != null ? village.getAnchor() : null;
    }
}
