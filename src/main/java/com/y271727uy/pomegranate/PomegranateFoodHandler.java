package com.y271727uy.pomegranate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

import java.util.LinkedList;
import java.util.List;

/**
 * Thin wrapper around persistent PomegranateData for server-side per-player food counting.
 */
public final class PomegranateFoodHandler {
    private static final String FOOD_HISTORY_TAG = "FoodHistory";

    private PomegranateFoodHandler() {}

    public static int incrementAndGetCount(ServerPlayer player, Item item) {
        return PomegranateData.incrementFoodCount(player, item);
    }

    public static int getCount(ServerPlayer player, Item item) {
        return PomegranateData.getFoodCount(player, item);
    }

    public static void resetCountsForPlayer(ServerPlayer player) {
        PomegranateData.resetAll(player);
    }

    // Persistent food history utilities (migrated from classic implementation)
    public static void saveFoodHistory(ServerPlayer player, LinkedList<ItemStack> foodHistory) {
        CompoundTag playerData = player.getPersistentData();
        ListTag foodHistoryTag = new ListTag();
        for (ItemStack stack : foodHistory) {
            CompoundTag stackTag = new CompoundTag();
            stack.save(stackTag);
            foodHistoryTag.add(stackTag);
        }
        playerData.put(FOOD_HISTORY_TAG, foodHistoryTag);
    }

    public static LinkedList<ItemStack> loadFoodHistory(ServerPlayer player) {
        LinkedList<ItemStack> foodHistory = new LinkedList<>();
        CompoundTag playerData = player.getPersistentData();
        if (playerData.contains(FOOD_HISTORY_TAG)) {
            ListTag foodHistoryTag = playerData.getList(FOOD_HISTORY_TAG, Tag.TAG_COMPOUND);
            for (Tag tag : foodHistoryTag) {
                CompoundTag stackTag = (CompoundTag) tag;
                ItemStack stack = ItemStack.of(stackTag);
                foodHistory.add(stack);
            }
        }
        return foodHistory;
    }

    public static void addFoodToHistory(ServerPlayer player, LinkedList<ItemStack> foodHistory, ItemStack eatenItem) {
        int maxHistorySize = PomegranateConfig.getMaxFoodHistorySize();
        if (foodHistory.size() >= Math.max(1, maxHistorySize - 1)) {
            foodHistory.removeFirst();
        }
        foodHistory.addLast(eatenItem);
        saveFoodHistory(player, foodHistory);
    }

    public static int getTimesEatenLong(ServerPlayer player, ItemStack stack) {
        LinkedList<ItemStack> foodHistory = loadFoodHistory(player);
        return (int) foodHistory.stream().filter(item -> item.getItem() == stack.getItem()).count();
    }

    public static int getTimesEatenShort(ServerPlayer player, ItemStack stack) {
        LinkedList<ItemStack> foodHistory = loadFoodHistory(player);
        int maxShort = PomegranateConfig.getMaxShortFoodHistorySize();
        int startIndex = Math.max(0, foodHistory.size() - maxShort);
        List<ItemStack> sub = foodHistory.subList(startIndex, foodHistory.size());
        return (int) sub.stream().filter(item -> item.getItem() == stack.getItem()).count();
    }

    // Compute food data using decay modifiers from config
    public static int computeCustomNutrition(ServerPlayer player, ItemStack stack) {
        FoodProperties food = stack.getItem().getFoodProperties(stack, player);
        if (food == null) return 0;
        int sameFoodCount = getTimesEatenLong(player, stack);
        int sameFoodShort = getTimesEatenShort(player, stack);

        List<Double> decay = PomegranateConfig.getFoodDecayModifiers();
        double shortmodifier = PomegranateConfig.getShortfoodDecayModifiers();

        double modifier;
        if (sameFoodShort < decay.size()) {
            modifier = decay.get(sameFoodShort);
        } else {
            modifier = decay.get(Math.max(0, decay.size() - 1));
        }
        modifier = modifier - shortmodifier * sameFoodCount;
        if (modifier < 0.0) modifier = 0.0;

        int customFoodLevel = Math.max(1, (int) (food.getNutrition() * modifier));
        return customFoodLevel;
    }

    public static float computeCustomSaturation(ServerPlayer player, ItemStack stack) {
        FoodProperties food = stack.getItem().getFoodProperties(stack, player);
        if (food == null) return 0f;
        // For now keep saturation at least 0.01f, similar to classic
        return Math.max(0.01f, food.getSaturationModifier());
    }
}
