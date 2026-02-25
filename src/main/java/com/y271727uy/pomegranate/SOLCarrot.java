package com.y271727uy.pomegranate;

import com.y271727uy.pomegranate.communication.FoodListMessage;
import com.y271727uy.pomegranate.communication.PomegranateDataMessage;
import com.y271727uy.pomegranate.integration.appleskin.AppleSkinIntegration;
import com.y271727uy.pomegranate.item.SOLCarrotItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD;

@Mod(SOLCarrot.MOD_ID)
@Mod.EventBusSubscriber(modid = SOLCarrot.MOD_ID, bus = MOD)
public final class SOLCarrot {
	public static final String MOD_ID = "spice_of_life_pomegranate_edition";

	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	
	private static final String PROTOCOL_VERSION = "1.0";
	public static SimpleChannel channel = NetworkRegistry.ChannelBuilder
		.named(resourceLocation("main"))
		.clientAcceptedVersions(PROTOCOL_VERSION::equals)
		.serverAcceptedVersions(PROTOCOL_VERSION::equals)
		.networkProtocolVersion(() -> PROTOCOL_VERSION)
		.simpleChannel();
	
	public static ResourceLocation resourceLocation(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
	
	@SubscribeEvent
	public static void setUp(FMLCommonSetupEvent event) {
		channel.messageBuilder(FoodListMessage.class, 0)
			.encoder(FoodListMessage::write)
			.decoder(FoodListMessage::new)
			.consumerMainThread(FoodListMessage::handle)
			.add();
		channel.messageBuilder(PomegranateDataMessage.class, 1)
			.encoder(PomegranateDataMessage::write)
			.decoder(PomegranateDataMessage::new)
			.consumerMainThread(PomegranateDataMessage::handle)
			.add();
	}
	
	public SOLCarrot() {
		SOLCarrotConfig.setUp();
		PomegranateConfig.setUp();
		SOLCarrotItems.setUp();

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			if (ModList.get().isLoaded("appleskin")) {
				AppleSkinIntegration.init();
			}
		});
	}
}
