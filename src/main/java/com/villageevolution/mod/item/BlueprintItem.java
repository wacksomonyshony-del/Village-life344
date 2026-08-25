package com.villageevolution.mod.item;

import com.villageevolution.mod.village.BuildingType;
import com.villageevolution.mod.village.ConstructionProject;
import com.villageevolution.mod.village.VillageBuilding;
import com.villageevolution.mod.village.VillageInstance;
import com.villageevolution.mod.village.VillageManager;
import com.villageevolution.mod.village.VillageSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Right-click while standing in/near a village to manually queue
 * construction of this building type (villagers still have to gather
 * materials and build it - this just tells the village what to prioritize
 * next, on top of the automatic-expansion logic in VillageManager).
 */
public class BlueprintItem extends Item {

    private final BuildingType buildingType;

    public BlueprintItem(BuildingType buildingType, Properties properties) {
        super(properties);
        this.buildingType = buildingType;
    }

    public BuildingType getBuildingType() {
        return buildingType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }

        BlockPos playerPos = player.blockPosition();
        VillageInstance village = VillageSavedData.get(serverLevel).findOrCreate(playerPos, VillageManager.SEARCH_RADIUS);

        if (village.hasActiveProjectFor(buildingType)) {
            player.displayClientMessage(Component.literal(
                    "A " + buildingType.getDisplayName() + " project is already underway in this village."), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!buildingType.isUnlocked(village.getPopulation(), village.getStage())) {
            player.displayClientMessage(Component.literal(
                    "This village isn't ready for a " + buildingType.getDisplayName()
                            + " yet (needs " + buildingType.getMinPopulation() + "+ population, "
                            + buildingType.getMinStage().getDisplayName() + "+ stage)."), true);
            return InteractionResultHolder.fail(stack);
        }

        List<VillageBuilding> existing = village.getBuildingsOfType(buildingType);
        boolean upgrade = !existing.isEmpty() && existing.get(0).canUpgrade();

        Direction facing = player.getDirection();
        if (upgrade) {
            VillageBuilding building = existing.get(0);
            village.addProject(new ConstructionProject(buildingType, building.getLevel() + 1,
                    building.getOrigin(), building.getFacing(), true, level.getGameTime()));
            player.displayClientMessage(Component.literal(
                    "Queued an upgrade for the village's " + buildingType.getDisplayName() + "."), true);
        } else {
            BlockPos site = playerPos.relative(facing, 3);
            village.addProject(new ConstructionProject(buildingType, 1, site, facing, false, level.getGameTime()));
            player.displayClientMessage(Component.literal(
                    "Queued construction of a new " + buildingType.getDisplayName() + "."), true);
        }

        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }
}
