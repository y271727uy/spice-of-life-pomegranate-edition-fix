package com.y271727uy.pomegranate.tracking;

import com.y271727uy.pomegranate.SOLCarrot;
import com.y271727uy.pomegranate.SOLCarrotConfig;
import com.y271727uy.pomegranate.api.FoodCapability;
import com.y271727uy.pomegranate.communication.FoodListMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD;

@Mod.EventBusSubscriber(modid = SOLCarrot.MOD_ID)
public final class CapabilityHandler {
	private static final ResourceLocation FOOD = SOLCarrot.resourceLocation("food");
	
	@Mod.EventBusSubscriber(modid = SOLCarrot.MOD_ID, bus = MOD)
	private static final class Setup {
		@SubscribeEvent
		public static void registerCapabilities(RegisterCapabilitiesEvent event) {
			event.register(FoodCapability.class);
		}
	}
	
	@SubscribeEvent
	public static void attachPlayerCapability(AttachCapabilitiesEvent<Entity> event) {
		if (!(event.getObject() instanceof Player)) return;
		
		event.addCapability(FOOD, new FoodList());
	}
	
	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		// server needs to send any loaded data to the client
		syncFoodList(event.getEntity());
	}
	
	@SubscribeEvent
	public static void onPlayerDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		syncFoodList(event.getEntity());
	}
	
	@SubscribeEvent
	public static void onClone(PlayerEvent.Clone event) {
		if (event.isWasDeath() && SOLCarrotConfig.shouldResetOnDeath()) return;
		
		var originalPlayer = event.getOriginal();
		originalPlayer.reviveCaps(); // so we can access the capabilities; entity will get removed either way
		var original = FoodList.get(originalPlayer);
		var newInstance = FoodList.get(event.getEntity());
		newInstance.deserializeNBT(original.serializeNBT());
		// can't sync yet; client hasn't attached capabilities yet
		
		originalPlayer.invalidateCaps();
	}
	
	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		syncFoodList(event.getEntity());
	}
	
	public static void syncFoodList(Player player) {
        if (player.level().isClientSide) return;

        var target = (ServerPlayer) player;
        SOLCarrot.channel.sendTo(
            new FoodListMessage(FoodList.get(player)),
            target.connection.connection,
            NetworkDirection.PLAY_TO_CLIENT
        );

        // send full pomegranate data (food counts, staple/produce counts, etc.) so client can cache counts
        SOLCarrot.channel.sendTo(
            new com.y271727uy.pomegranate.communication.PomegranateDataMessage(player),
            target.connection.connection,
            NetworkDirection.PLAY_TO_CLIENT
        );

        MaxHealthHandler.updateFoodHPModifier(player);
    }
}
