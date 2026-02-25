package com.y271727uy.pomegranate;

import com.y271727uy.pomegranate.SOLCarrot;
import com.google.common.collect.Lists;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD;

@Mod.EventBusSubscriber(modid = SOLCarrot.MOD_ID, bus = MOD)
public final class PomegranateConfig {
    private static String localizationPath(String path) {
        return "config." + SOLCarrot.MOD_ID + ".pomegranate." + path;
    }

    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    // cached TagKey lists
    private static List<TagKey<Item>> STAPLE_TAG_KEYS = null;
    private static List<TagKey<Item>> PRODUCE_TAG_KEYS = null;

    static {
        Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = specPair.getLeft();
        SERVER_SPEC = specPair.getRight();
    }

    public static void setUp() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "spice_of_life_pomegranate_edition-pomegranate-server.toml");
    }

    public static int getMaxUniqueFoods() { return SERVER.maxUniqueFoods.get(); }
    public static int getOverweightStapleThreshold() { return SERVER.overweightStapleThreshold.get(); }
    public static int getOverweightProduceResetThreshold() { return SERVER.overweightProduceResetThreshold.get(); }
    public static boolean enablePunishments() { return SERVER.enablePunishments.get(); }
    public static boolean enableRewards() { return SERVER.enableRewards.get(); }
    public static boolean enableOverweight() { return SERVER.enableOverweight.get(); }
    public static List<String> getStapleTags() { return new ArrayList<>(SERVER.stapleTags.get()); }
    public static List<String> getProduceTags() { return new ArrayList<>(SERVER.produceTags.get()); }

    // Classic-style config getters
    public static int getMaxFoodHistorySize() { return SERVER.maxFoodHistorySize.get(); }
    public static int getMaxShortFoodHistorySize() { return SERVER.maxShortFoodHistorySize.get(); }
    public static double getShortfoodDecayModifiers() { return SERVER.ShortfoodDecayModifiers.get(); }
    public static List<Double> getFoodDecayModifiers() { return new ArrayList<>(SERVER.foodDecayModifiers.get()); }

    // Return TagKey<Item> lists built from configured tag strings. Cached.
    public static synchronized List<TagKey<Item>> getStapleTagKeys() {
        if (STAPLE_TAG_KEYS == null) {
            STAPLE_TAG_KEYS = buildTagKeys(getStapleTags());
        }
        return STAPLE_TAG_KEYS;
    }

    public static synchronized List<TagKey<Item>> getProduceTagKeys() {
        if (PRODUCE_TAG_KEYS == null) {
            PRODUCE_TAG_KEYS = buildTagKeys(getProduceTags());
        }
        return PRODUCE_TAG_KEYS;
    }

    private static List<TagKey<Item>> buildTagKeys(List<String> tags) {
        List<TagKey<Item>> keys = new ArrayList<>();
        for (String s : tags) {
            try {
                ResourceLocation rl = new ResourceLocation(s);
                TagKey<Item> key = TagKey.create(Registries.ITEM, rl);
                keys.add(key);
            } catch (Exception e) {
                // ignore malformed entries
            }
        }
        return keys;
    }

    public static class Server {
        public final ForgeConfigSpec.IntValue maxUniqueFoods;
        public final ForgeConfigSpec.IntValue overweightStapleThreshold;
        public final ForgeConfigSpec.IntValue overweightProduceResetThreshold;
        public final ForgeConfigSpec.BooleanValue enablePunishments;
        public final ForgeConfigSpec.BooleanValue enableRewards;
        public final ForgeConfigSpec.BooleanValue enableOverweight;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> stapleTags;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> produceTags;

        // Classic fields
        public final ForgeConfigSpec.IntValue maxFoodHistorySize;
        public final ForgeConfigSpec.IntValue maxShortFoodHistorySize;
        public final ForgeConfigSpec.DoubleValue ShortfoodDecayModifiers;
        public final ForgeConfigSpec.ConfigValue<List<? extends Double>> foodDecayModifiers;

        Server(ForgeConfigSpec.Builder builder) {
            builder.push("pomegranate");

            maxUniqueFoods = builder
                .translation(localizationPath("max_unique_foods"))
                .comment("The maximum number of unique foods tracked before the list resets.")
                .defineInRange("maxUniqueFoods", 150, 1, 1000);

            overweightStapleThreshold = builder
                .translation(localizationPath("overweight_staple_threshold"))
                .comment("How many staple foods cause the player to become overweight.")
                .defineInRange("overweightStapleThreshold", 10, 1, 1000);

            overweightProduceResetThreshold = builder
                .translation(localizationPath("overweight_produce_reset_threshold"))
                .comment("How many fruits/vegetables are needed to clear the overweight state.")
                .defineInRange("overweightProduceResetThreshold", 10, 1, 1000);

            enablePunishments = builder
                .translation(localizationPath("enable_punishments"))
                .comment("If true, repeated foods apply punishments and reduced recovery.")
                .define("enablePunishments", true);

            enableRewards = builder
                .translation(localizationPath("enable_rewards"))
                .comment("If true, variety rewards grant temporary positive effects.")
                .define("enableRewards", true);

            enableOverweight = builder
                .translation(localizationPath("enable_overweight"))
                .comment("If true, eating too many staples causes overweight penalties.")
                .define("enableOverweight", true);

            stapleTags = builder
                .translation(localizationPath("staple_tags"))
                .comment("Item tags used to identify staple foods.")
                .defineList("stapleTags", Lists.newArrayList("spice_of_life_pomegranate_edition:staples", "forge:bread", "forge:grain", "forge:grains", "diet:grain", "diet:grains"), e -> e instanceof String);

            produceTags = builder
                .translation(localizationPath("produce_tags"))
                .comment("Item tags used to identify fruits and vegetables.")
                .defineList("produceTags", Lists.newArrayList("spice_of_life_pomegranate_edition:produce", "forge:vegetables", "forge:fruits", "diet:vegetables", "diet:fruits"), e -> e instanceof String);

            // Classic config group
            builder.push("classic");

            maxFoodHistorySize = builder
                .comment("Maximum number of food history entries to track")
                .defineInRange("maxFoodHistorySize", 100, 5, 1000);

            maxShortFoodHistorySize = builder
                .comment("Maximum number of short food history entries to consider for decay")
                .defineInRange("maxShortFoodHistorySize", 5, 1, 1000);

            ShortfoodDecayModifiers = builder
                .comment("Short decay modifier per repeat (double)")
                .defineInRange("ShortfoodDecayModifiers", 0.01D, 0.0D, 1.0D);

            foodDecayModifiers = builder
                .comment("List of decay modifiers applied per short-history index")
                .defineList("foodDecayModifiers", Lists.newArrayList(1.0D, 0.90D, 0.75D, 0.50D, 0.05D), o -> o instanceof Double);

            builder.pop(); // end classic

            builder.pop();
        }
    }

    private PomegranateConfig() {}
}
