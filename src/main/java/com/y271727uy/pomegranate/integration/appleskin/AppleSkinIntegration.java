package com.y271727uy.pomegranate.integration.appleskin;

import com.y271727uy.pomegranate.PomegranateFoodLogic;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.food.FoodValues;

public class AppleSkinIntegration {
    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(AppleSkinIntegration::onFoodValuesEvent);
    }

    public static void onFoodValuesEvent(FoodValuesEvent event) {
        Player player = event.player;
        PomegranateFoodLogic.CustomFoodData foodData = PomegranateFoodLogic.getCustomFoodDataForDisplay(player, event.itemStack);
        if (foodData == null) return;
        event.modifiedFoodValues = new FoodValues(foodData.getNutrition(), foodData.getSaturation());
    }
}
