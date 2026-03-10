package com.y271727uy.pomegranate.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.y271727uy.pomegranate.SOLCarrot;
import com.y271727uy.pomegranate.SOLCarrotConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Basic custom health overlay that keeps bonus hearts on a single row.
 * <p>
 * To avoid conflicts, this renderer fully opts out when Mantle is installed.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = SOLCarrot.MOD_ID)
public final class SingleRowHeartOverlayHandler {
	private static final Minecraft MC = Minecraft.getInstance();
	private static final ResourceLocation GUI_ICONS_LOCATION = new ResourceLocation("textures/gui/icons.png");
	private static final ResourceLocation EXTRA_HEARTS_LOCATION = SOLCarrot.resourceLocation("textures/gui/extra_hearts.png");
	private static final boolean HAS_MANTLE = ModList.get().isLoaded("mantle");
	private static final int HEART_VARIANTS = 12;
	private static final int HEART_SIZE = 9;
	private static final int HEART_SPACING = 8;
	private static final int ROW_HEIGHT = 10;
	private static final int HARDCORE_OFFSET = HEART_SIZE;
	private static final int DAMAGE_OFFSET = 2 * HEART_SIZE;
	private static final int MAX_VARIANT = 180;
	private static final int NORMAL_VARIANT = 0;
	private static final int POISON_VARIANT = 36;
	private static final int WITHER_VARIANT = 72;
	private static final int FREEZE_VARIANT = 108;
	private static final int ABSORPTION_VARIANT = 216;
	private static final int NORMAL_CONTAINER = 216;
	private static final int ABSORPTION_CONTAINER = 234;

	private static final int[] OFFSETS = new int[20];
	private static final Random RANDOM = new Random();

	private static int lastHealth;
	private static int displayHealth;
	private static long healthBlinkTime;
	private static long lastHealthTime;

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onRenderHealth(RenderGuiOverlayEvent.Pre event) {
		if (HAS_MANTLE || !SOLCarrotConfig.isSingleRowHeartOverlayEnabled() || event.isCanceled() || event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type()) {
			return;
		}
		if (!(MC.gui instanceof ForgeGui gui) || MC.options.hideGui || !gui.shouldDrawSurvivalElements()) {
			return;
		}

		Entity cameraEntity = MC.getCameraEntity();
		if (!(cameraEntity instanceof Player player)) {
			return;
		}

		gui.setupOverlayRenderState(true, false);
		MC.getProfiler().push("pomegranateHealth");

		int tickCount = MC.gui.getGuiTicks();
		int health = Mth.ceil(player.getHealth());
		int maxHealth = Math.max(Mth.ceil(player.getAttributeValue(Attributes.MAX_HEALTH)), Math.max(displayHealth, health));
		if (maxHealth <= 20) {
			return;
		}
		boolean highlight = healthBlinkTime > tickCount && (healthBlinkTime - tickCount) / 3L % 2L == 1L;

		long systemTime = Util.getMillis();
		if (player.invulnerableTime > 0) {
			if (health < lastHealth) {
				lastHealthTime = systemTime;
				healthBlinkTime = tickCount + 20L;
			} else if (health > lastHealth) {
				lastHealthTime = systemTime;
				healthBlinkTime = tickCount + 10L;
			}
		}
		if (systemTime - lastHealthTime > 1000L) {
			displayHealth = health;
			lastHealthTime = systemTime;
		}
		lastHealth = health;
		int shownHealth = displayHealth;

		int absorb = Mth.ceil(player.getAbsorptionAmount());
		boolean lowHealth = health + absorb <= 4;
		int regen = -1;
		if (player.hasEffect(MobEffects.REGENERATION)) {
			regen = tickCount % 25;
		}
		RANDOM.setSeed(tickCount * 312871L);

		int containerOffset = MAX_VARIANT;
		int heartOffset = NORMAL_VARIANT;
		int absorptionOffset = ABSORPTION_VARIANT;
		if (player.hasEffect(MobEffects.POISON)) {
			heartOffset = POISON_VARIANT;
		} else if (player.hasEffect(MobEffects.WITHER)) {
			heartOffset = WITHER_VARIANT;
			absorptionOffset = WITHER_VARIANT;
		} else if (player.isFullyFrozen()) {
			heartOffset = FREEZE_VARIANT;
		}
		if (MC.level != null && MC.level.getLevelData().isHardcore()) {
			containerOffset += HARDCORE_OFFSET;
			heartOffset += HARDCORE_OFFSET;
			absorptionOffset += HARDCORE_OFFSET;
		}
		if (highlight) {
			containerOffset += DAMAGE_OFFSET;
		}

		GuiGraphics graphics = event.getGuiGraphics();
		int left = MC.getWindow().getGuiScaledWidth() / 2 - 91;
		int top = MC.getWindow().getGuiScaledHeight() - gui.leftHeight;

		int visibleHealth = Math.min(health, 20);
		int showHearts = 10;
		int occupiedHealthHearts = (visibleHealth + 1) / 2;
		boolean compactAbsorption = occupiedHealthHearts < 10 && absorb <= 2 * (10 - occupiedHealthHearts);
		int absorptionRowOffset = ROW_HEIGHT;

		setOffsets(0, showHearts, lowHealth, regen);
		absorptionRowOffset += 1;
		renderHeartRow(graphics, left, top, 0, NORMAL_CONTAINER, containerOffset, 0, 10, false);

		if (absorb > 0) {
			int absorbHeartSlots = Math.min((absorb + 1) / 2, 10);
			setOffsets(10, absorbHeartSlots, lowHealth, -1);
			boolean halfAbsorb = absorb < 20 && (absorb & 1) == 1;
			int absorbHearts = absorb / 2;
			if (compactAbsorption) {
				renderHeartRow(graphics, left + HEART_SPACING * occupiedHealthHearts, top, 10, ABSORPTION_CONTAINER, containerOffset, 0, absorbHearts, halfAbsorb);
			} else {
				renderHeartRow(graphics, left, top - absorptionRowOffset, 10, ABSORPTION_CONTAINER, containerOffset, 0, Math.min(absorbHearts, 10), halfAbsorb);
			}
		}

		if (highlight && shownHealth > health) {
			renderHeartsWithDamage(graphics, left, top, heartOffset, health, shownHealth);
		}
		renderHearts(graphics, left, top, heartOffset, health, 0);
		if (compactAbsorption) {
			int absorbHearts = absorb / 2;
			renderHeartRow(graphics, left + HEART_SPACING * occupiedHealthHearts, top, 10, 0, absorptionOffset, 0, absorbHearts, (absorb & 1) == 1);
		} else {
			renderHearts(graphics, left, top - absorptionRowOffset, absorptionOffset, absorb, 10);
		}

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
		gui.leftHeight += ROW_HEIGHT;
		if (!compactAbsorption && absorb > 0) {
			gui.leftHeight += absorptionRowOffset;
		}
		event.setCanceled(true);
		RenderSystem.disableBlend();
		MC.getProfiler().pop();
		//noinspection UnstableApiUsage
		MinecraftForge.EVENT_BUS.post(new RenderGuiOverlayEvent.Post(event.getWindow(), graphics, event.getPartialTick(), VanillaGuiOverlay.PLAYER_HEALTH.type()));
	}

	private static void renderHearts(GuiGraphics graphics, int left, int top, int heartOffset, int count, int indexOffset) {
		if (count <= 0) {
			return;
		}
		int heartsTopColor = (count % 20) / 2;
		int heartIndex = count / 20;
		if (count >= 20) {
			renderHeartRow(graphics, left, top, indexOffset, colorOffset(heartIndex - 1), heartOffset, heartsTopColor, 10, false);
		}
		renderHeartRow(graphics, left, top, indexOffset, colorOffset(heartIndex), heartOffset, 0, heartsTopColor, (count & 1) == 1);
	}

	private static void renderHeartsWithDamage(GuiGraphics graphics, int left, int top, int heartOffset, int current, int last) {
		int currentTopRow = current % 20;
		int currentRight = currentTopRow / 2;
		int lastTopRow = last % 20;
		int lastRight = lastTopRow / 2;

		int damageTaken = last - current;
		boolean bigDamage = damageTaken >= 20;
		boolean damageWrapped = bigDamage || lastRight < currentRight;

		if (damageWrapped) {
			renderHeartRow(graphics, left, top, 0, colorOffset(last / 20 - 1), heartOffset + DAMAGE_OFFSET, bigDamage ? lastRight : currentRight, 10, false);
		} else {
			if (current >= 20) {
				renderHeartRow(graphics, left, top, 0, colorOffset(current / 20 - 1), heartOffset, lastRight, 10, false);
			}
			renderHeartRow(graphics, left, top, 0, colorOffset(last / 20), heartOffset + DAMAGE_OFFSET, currentRight, lastRight, (lastTopRow & 1) == 1);
		}

		if (!bigDamage) {
			renderHeartRow(graphics, left, top, 0, colorOffset(current / 20), heartOffset, damageWrapped ? lastRight : 0, currentRight, (currentTopRow & 1) == 1);
		}
		if (damageWrapped) {
			renderHeartRow(graphics, left, top, 0, colorOffset(last / 20), heartOffset + DAMAGE_OFFSET, 0, lastRight, (lastTopRow & 1) == 1);
		}
	}

	private static int colorOffset(int heartIndex) {
		return (Math.floorMod(heartIndex, HEART_VARIANTS)) * 2 * HEART_SIZE;
	}

	private static void renderHeartRow(GuiGraphics graphics, int left, int top, int indexOffset, int uOffset, int vOffset, int start, int end, boolean halfHeart) {
		for (int i = start; i < end; i++) {
			graphics.blit(EXTRA_HEARTS_LOCATION, left + i * HEART_SPACING, top + OFFSETS[i + indexOffset], uOffset, vOffset, HEART_SIZE, HEART_SIZE);
		}
		if (halfHeart && end + indexOffset < OFFSETS.length) {
			graphics.blit(EXTRA_HEARTS_LOCATION, left + end * HEART_SPACING, top + OFFSETS[end + indexOffset], uOffset + HEART_SIZE, vOffset, HEART_SIZE, HEART_SIZE);
		}
	}

	private static void setOffsets(int indexOffset, int end, boolean lowHealth, int regen) {
		for (int i = 0; i < end; i++) {
			int offset = lowHealth ? RANDOM.nextInt(2) : 0;
			if (i == regen) {
				offset -= 2;
			}
			OFFSETS[i + indexOffset] = offset;
		}
	}

	private SingleRowHeartOverlayHandler() {}
}











