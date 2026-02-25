package com.y271727uy.pomegranate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public final class PomegranateData {
    private static final String ROOT = "spice_of_life_pomegranate_edition_pomegranate";
    private static final String FOOD_COUNTS = "FoodCounts";
    private static final String FOOD_HISTORY = "FoodHistory";
    private static final String STAPLE_COUNT = "StapleCount";
    private static final String PRODUCE_COUNT = "ProduceCount";
    private static final String VARIETY_REWARD_STAGE = "VarietyRewardStage";
    private static final String OVERWEIGHT = "Overweight";

    // synced classic config (so client-side AppleSkin display matches server)
    private static final String CFG_MAX_FOOD_HISTORY_SIZE = "MaxFoodHistorySize";
    private static final String CFG_MAX_SHORT_FOOD_HISTORY_SIZE = "MaxShortFoodHistorySize";
    private static final String CFG_SHORT_DECAY = "ShortfoodDecayModifiers";
    private static final String CFG_DECAY_LIST = "FoodDecayModifiers";

    public static int getFoodCount(Player player, Item item) {
        String id = itemId(item);
        if (id == null) return 0;
        CompoundTag counts = root(player).getCompound(FOOD_COUNTS);
        return counts.getInt(id);
    }

    public static int incrementFoodCount(Player player, Item item) {
        String id = itemId(item);
        if (id == null) return 0;
        CompoundTag root = root(player);
        CompoundTag counts = root.getCompound(FOOD_COUNTS);
        int updated = counts.getInt(id) + 1;
        counts.putInt(id, updated);
        root.put(FOOD_COUNTS, counts);
        return updated;
    }

    public static int getStapleCount(Player player) {
        return root(player).getInt(STAPLE_COUNT);
    }

    public static int getProduceCount(Player player) {
        return root(player).getInt(PRODUCE_COUNT);
    }

    public static void incrementStapleCount(Player player) {
        CompoundTag root = root(player);
        root.putInt(STAPLE_COUNT, root.getInt(STAPLE_COUNT) + 1);
    }

    public static void incrementProduceCount(Player player) {
        CompoundTag root = root(player);
        root.putInt(PRODUCE_COUNT, root.getInt(PRODUCE_COUNT) + 1);
    }

    public static boolean isOverweight(Player player) {
        return root(player).getBoolean(OVERWEIGHT);
    }

    public static void setOverweight(Player player, boolean value) {
        root(player).putBoolean(OVERWEIGHT, value);
    }

    public static int getVarietyRewardStage(Player player) {
        return root(player).getInt(VARIETY_REWARD_STAGE);
    }

    public static void setVarietyRewardStage(Player player, int stage) {
        root(player).putInt(VARIETY_REWARD_STAGE, stage);
    }

    public static void resetOverweight(Player player) {
        CompoundTag root = root(player);
        root.putBoolean(OVERWEIGHT, false);
        root.putInt(STAPLE_COUNT, 0);
        root.putInt(PRODUCE_COUNT, 0);
    }

    public static void resetAll(Player player) {
        CompoundTag root = root(player);
        root.put(FOOD_COUNTS, new CompoundTag());
        root.putInt(STAPLE_COUNT, 0);
        root.putInt(PRODUCE_COUNT, 0);
        root.putInt(VARIETY_REWARD_STAGE, 0);
        root.putBoolean(OVERWEIGHT, false);
    }

    public static CompoundTag toClientTag(Player player) {
        CompoundTag root = root(player);
        CompoundTag out = new CompoundTag();
        out.put(FOOD_COUNTS, root.getCompound(FOOD_COUNTS).copy());

        // Sync classic food history as a lightweight list of item ids (not full ItemStack NBT)
        // Server stores "FoodHistory" directly on Player persistent data as a ListTag of CompoundTag ItemStacks.
        ListTag historyIds = new ListTag();
        CompoundTag playerData = player.getPersistentData();
        if (playerData.contains(FOOD_HISTORY, Tag.TAG_LIST)) {
            ListTag history = playerData.getList(FOOD_HISTORY, Tag.TAG_COMPOUND);
            for (Tag tag : history) {
                if (!(tag instanceof CompoundTag stackTag)) continue;
                ItemStack stack = ItemStack.of(stackTag);
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (key == null) continue;
                historyIds.add(StringTag.valueOf(key.toString()));
            }
        }
        out.put(FOOD_HISTORY, historyIds);

        // Sync the server-side decay config so client display (AppleSkin) is accurate on dedicated servers.
        out.putInt(CFG_MAX_FOOD_HISTORY_SIZE, PomegranateConfig.getMaxFoodHistorySize());
        out.putInt(CFG_MAX_SHORT_FOOD_HISTORY_SIZE, PomegranateConfig.getMaxShortFoodHistorySize());
        out.putDouble(CFG_SHORT_DECAY, PomegranateConfig.getShortfoodDecayModifiers());
        ListTag decay = new ListTag();
        for (Double d : PomegranateConfig.getFoodDecayModifiers()) {
            if (d == null) continue;
            decay.add(DoubleTag.valueOf(d));
        }
        out.put(CFG_DECAY_LIST, decay);

        out.putInt(STAPLE_COUNT, root.getInt(STAPLE_COUNT));
        out.putInt(PRODUCE_COUNT, root.getInt(PRODUCE_COUNT));
        out.putInt(VARIETY_REWARD_STAGE, root.getInt(VARIETY_REWARD_STAGE));
        out.putBoolean(OVERWEIGHT, root.getBoolean(OVERWEIGHT));
        return out;
    }

    private static CompoundTag root(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ROOT)) {
            data.put(ROOT, new CompoundTag());
        }
        return data.getCompound(ROOT);
    }

    private static String itemId(Item item) {
        return Optional.ofNullable(ForgeRegistries.ITEMS.getKey(item))
            .map(ResourceLocation::toString)
            .orElse(null);
    }

    private PomegranateData() {}
}

