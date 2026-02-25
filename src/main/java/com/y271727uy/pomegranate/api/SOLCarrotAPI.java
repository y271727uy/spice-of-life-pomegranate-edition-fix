package com.y271727uy.pomegranate.api;

import com.y271727uy.pomegranate.tracking.CapabilityHandler;
import com.y271727uy.pomegranate.tracking.FoodList;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 Provides a stable API for interfacing with Spice of Life: Carrot Edition.
 */
public final class SOLCarrotAPI {
	public static Capability<FoodCapability> foodCapability = CapabilityManager.get(new CapabilityToken<>() {});
	
	private SOLCarrotAPI() {}
	
	/**
	 Retrieves the {@link com.y271727uy.pomegranate.api.FoodCapability} for the given player.
	 */
	public static FoodCapability getFoodCapability(Player player) {
		return FoodList.get(player);
	}
	
	/**
	 Synchronizes the food list for the given player to the client, updating their max health in the process.
	 */
	public static void syncFoodList(Player player) {
		CapabilityHandler.syncFoodList(player);
	}
}

