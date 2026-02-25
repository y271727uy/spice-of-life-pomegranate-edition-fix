package com.y271727uy.pomegranate.client;

import com.y271727uy.pomegranate.SOLCarrot;
import com.y271727uy.pomegranate.SOLCarrotConfig;
import com.y271727uy.pomegranate.tracking.FoodList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.y271727uy.pomegranate.lib.Localization.localizedComponent;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = SOLCarrot.MOD_ID)
public final class TooltipHandler {
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onItemTooltip(ItemTooltipEvent event) {
		if (!SOLCarrotConfig.isFoodTooltipEnabled()) return;
		
		Player player = event.getEntity();
		if (player == null) return;
		
		Item food = event.getItemStack().getItem();
		if (!food.isEdible()) return;
		
		FoodList foodList = FoodList.get(player);
		boolean hasBeenEaten = foodList.hasEaten(food);
		boolean isAllowed = SOLCarrotConfig.isAllowed(food);
		boolean isHearty = SOLCarrotConfig.isHearty(food);
		
		var tooltip = event.getToolTip();
		if (!isAllowed) {
			if (hasBeenEaten) {
				tooltip.add(localizedTooltip("disabled.eaten", ChatFormatting.DARK_RED));
			}
			String key = SOLCarrotConfig.hasWhitelist() ? "whitelist" : "blacklist";
			tooltip.add(localizedTooltip("disabled." + key, ChatFormatting.DARK_GRAY));
		} else if (isHearty) {
			if (hasBeenEaten) {
				tooltip.add(localizedTooltip("hearty.eaten", ChatFormatting.DARK_GREEN));
			} else {
				tooltip.add(localizedTooltip("hearty.not_eaten", ChatFormatting.DARK_AQUA));
			}
		} else {
			if (hasBeenEaten) {
				tooltip.add(localizedTooltip("cheap.eaten", ChatFormatting.DARK_RED));
			}
			tooltip.add(localizedTooltip("cheap", ChatFormatting.DARK_GRAY));
		}

		int timesEaten = PomegranateClientData.getFoodCount(food);
		String pickyKey = pickyTooltipKey(timesEaten);
		tooltip.add(localizedTooltip(pickyKey, ChatFormatting.GOLD));
	}

	private static String pickyTooltipKey(int timesEaten) {
		// README specifies: 0-5, 6-9, 10-12, >12
		if (timesEaten <= 5) {
			return "pomegranate.picky.0_5";
		}
		if (timesEaten <= 9) {
			return "pomegranate.picky.6_9";
		}
		if (timesEaten <= 12) {
			return "pomegranate.picky.10_12";
		}
		return "pomegranate.picky.13_plus";
	}
	
	private static MutableComponent localizedTooltip(String path, ChatFormatting color) {
		return localizedComponent("tooltip", path).withStyle(color);
	}
	
	private TooltipHandler() {}
}
