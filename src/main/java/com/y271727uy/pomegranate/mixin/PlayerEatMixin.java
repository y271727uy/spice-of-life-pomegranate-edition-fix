package com.y271727uy.pomegranate.mixin;

import com.y271727uy.pomegranate.PomegranateConfig;
import com.y271727uy.pomegranate.PomegranateFoodLogic;
import com.y271727uy.pomegranate.SOLCarrotConfig;
import com.mojang.datafixers.util.Pair;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEatMixin {
	@Shadow
	public abstract void awardStat(Stat<?> stat);

	@Shadow
	public abstract void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch);

	@Inject(method = "eat", at = @At("HEAD"), cancellable = true)
	private void spiceOfLifePomegranateEdition$modifyFoodData(Level level, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
		Player self = (Player) (Object) this;
		if (!PomegranateConfig.enablePunishments() || !PomegranateFoodLogic.shouldModifyFood(stack)) {
			return;
		}
		if (SOLCarrotConfig.limitProgressionToSurvival() && self.isCreative()) {
			return;
		}

		if (self instanceof ServerPlayer serverPlayer) {
			PomegranateFoodLogic.CustomFoodData data = PomegranateFoodLogic.getCustomFoodData(serverPlayer, stack);
			serverPlayer.getFoodData().eat(data.getNutrition(), data.getSaturation());
			serverPlayer.awardStat(Stats.ITEM_USED.get(stack.getItem()));

			level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
			CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);

			Item item = stack.getItem();
			if (item.isEdible()) {
				for (Pair<MobEffectInstance, Float> pair : stack.getFoodProperties((LivingEntity) (Object) this).getEffects()) {
					if (!level.isClientSide && pair.getFirst() != null && level.random.nextFloat() < pair.getSecond()) {
						serverPlayer.addEffect(new MobEffectInstance(pair.getFirst()));
					}
				}
			}


			if (!serverPlayer.getAbilities().instabuild) {
				stack.shrink(1);
			}

			serverPlayer.gameEvent(GameEvent.EAT);
			cir.setReturnValue(stack);
			return;
		}

		self.awardStat(Stats.ITEM_USED.get(stack.getItem()));
		level.playSound(null, self.getX(), self.getY(), self.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);

		if (stack.isEdible()) {
			level.playSound(null, self.getX(), self.getY(), self.getZ(), self.getEatingSound(stack), SoundSource.NEUTRAL, 1.0F, 1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.4F);
			Item item = stack.getItem();
			if (item.isEdible()) {
				for (Pair<MobEffectInstance, Float> pair : stack.getFoodProperties((LivingEntity) (Object) this).getEffects()) {
					if (!level.isClientSide && pair.getFirst() != null && level.random.nextFloat() < pair.getSecond()) {
						self.addEffect(new MobEffectInstance(pair.getFirst()));
					}
				}
			}

			if (!self.getAbilities().instabuild) {
				stack.shrink(1);
			}

			self.gameEvent(GameEvent.EAT);
			cir.setReturnValue(stack);
		}
	}
}
