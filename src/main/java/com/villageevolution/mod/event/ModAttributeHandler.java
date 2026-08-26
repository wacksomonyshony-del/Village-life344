package com.villageevolution.mod.event;

import com.villageevolution.mod.VillagerEvolutionMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Villagers have no ATTACK_DAMAGE attribute in vanilla - they were never
 * meant to hit anything - so MeleeAttackGoal would fail on them. This adds
 * the attribute with a deliberately low base value: 1.0, the same base a
 * player has. Their iron sword supplies the rest (+6), which puts an armed
 * villager at roughly a player's un-enchanted melee damage, minus crits,
 * sweeping, and any sense of timing.
 */
@Mod.EventBusSubscriber(modid = VillagerEvolutionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModAttributeHandler {

    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.VILLAGER, Attributes.ATTACK_DAMAGE, 1.0D);
    }

    private ModAttributeHandler() {}
}
