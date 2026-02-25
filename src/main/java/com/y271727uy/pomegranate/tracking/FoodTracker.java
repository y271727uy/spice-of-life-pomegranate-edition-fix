package com.y271727uy.pomegranate.tracking;

import com.y271727uy.pomegranate.SOLCarrot;
import com.y271727uy.pomegranate.SOLCarrotConfig;
import com.y271727uy.pomegranate.PomegranateConfig;
import com.y271727uy.pomegranate.PomegranateData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ThreadLocalRandom;

import static com.y271727uy.pomegranate.lib.Localization.localizedComponent;
import static com.y271727uy.pomegranate.lib.Localization.localizedQuantityComponent;

@Mod.EventBusSubscriber(modid = SOLCarrot.MOD_ID)
public final class FoodTracker {
	@SubscribeEvent
	public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
		if (!(event.getEntity() instanceof Player player)) return;
		
		var isClientSide = player.level().isClientSide;
		
		if (SOLCarrotConfig.limitProgressionToSurvival() && player.isCreative()) return;
		
		var usedItem = event.getItem().getItem();
		if (!usedItem.isEdible()) return;
		
		FoodList foodList = FoodList.get(player);
		boolean hasTriedNewFood = foodList.addFood(usedItem);
		
		// check this before syncing, because the sync entails an hp update
		boolean newMilestoneReached = MaxHealthHandler.updateFoodHPModifier(player);
		
		CapabilityHandler.syncFoodList(player);
		var progressInfo = foodList.getProgressInfo();
		
		// Rewards based on variety (unique foods eaten) - server-side only
		if (!player.level().isClientSide && PomegranateConfig.enableRewards() && hasTriedNewFood) {
			int uniqueFoods = progressInfo.foodsEaten;
			int stage = PomegranateData.getVarietyRewardStage(player);
			int[] thresholds = new int[] {3, 7, 12, 15};
			while (stage < thresholds.length && uniqueFoods >= thresholds[stage]) {
				int durationSec;
				int amp;
				switch (stage) {
					case 0 -> { durationSec = 5; amp = 0; }
					case 1 -> { durationSec = 10; amp = 1; }
					case 2 -> { durationSec = 20; amp = 2; }
					default -> { durationSec = 60; amp = 2; }
				}
				// choose a random positive effect
				var picker = ThreadLocalRandom.current().nextInt(4);
				switch (picker) {
					case 0 -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationSec * 20, amp));
					case 1 -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, durationSec * 20, amp));
					case 2 -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, durationSec * 20, amp));
					default -> player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, durationSec * 20, amp));
				}
				// notify player
				player.sendSystemMessage(Component.literal("Variety reward: feel the effects!"));
				stage++;
				PomegranateData.setVarietyRewardStage(player, stage);
			}
		}

		// Auto-reset when unique foods reach configured max
		if (!player.level().isClientSide) {
			int maxUnique = PomegranateConfig.getMaxUniqueFoods();
			if (foodList.getEatenFoodCount() >= maxUnique) {
				// clear both capability and persistent storage
				foodList.clearFood();
				PomegranateData.resetAll(player);
				CapabilityHandler.syncFoodList(player);
				player.sendSystemMessage(Component.literal("Food list has reached max unique entries and was reset."));
				// reset variety reward stage as well
				PomegranateData.setVarietyRewardStage(player, 0);
			}
		}

		if (newMilestoneReached) {
			if (isClientSide && SOLCarrotConfig.shouldPlayMilestoneSounds()) {
				// passing the player makes it not play for some reason
				player.level().playSound(
					player,
					player.blockPosition(),
					SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
					1.0F, 1.0F
				);
			}
			
			if (isClientSide && SOLCarrotConfig.shouldSpawnMilestoneParticles()) {
				spawnParticles(player, ParticleTypes.HEART, 12);
				
				if (progressInfo.hasReachedMax()) {
					spawnParticles(player, ParticleTypes.HAPPY_VILLAGER, 16);
				}
			}
			
			var heartsDescription = localizedQuantityComponent("message", "hearts", SOLCarrotConfig.getHeartsPerMilestone());
			
			if (isClientSide && SOLCarrotConfig.shouldShowProgressAboveHotbar()) {
				String messageKey = progressInfo.hasReachedMax() ? "finished.hotbar" : "milestone_achieved";
				player.displayClientMessage(localizedComponent("message", messageKey, heartsDescription), true);
			} else {
				showChatMessage(player, ChatFormatting.DARK_AQUA, localizedComponent("message", "milestone_achieved", heartsDescription));
				if (progressInfo.hasReachedMax()) {
					showChatMessage(player, ChatFormatting.GOLD, localizedComponent("message", "finished.chat"));
				}
			}
		} else if (hasTriedNewFood) {
			if (isClientSide && SOLCarrotConfig.shouldSpawnIntermediateParticles()) {
				spawnParticles(player, ParticleTypes.END_ROD, 12);
			}
		}
	}
	
	private static void spawnParticles(Player player, ParticleOptions type, int count) {
		// hacky way to reuse the existing logic for randomizing particle spawn positions
		var connection = Minecraft.getInstance().getConnection();
		assert connection != null;
		connection.handleParticleEvent(new ClientboundLevelParticlesPacket(
			type, false,
			player.getX(), player.getY() + player.getEyeHeight(), player.getZ(),
			0.5F, 0.5F, 0.5F,
			0.0F, count
		));
	}
	
	private static void showChatMessage(Player player, ChatFormatting color, Component message) {
		var component = localizedComponent("message", "chat_wrapper", message)
			.withStyle(color);
		player.displayClientMessage(component, false);
	}
	
	private FoodTracker() {}
}
