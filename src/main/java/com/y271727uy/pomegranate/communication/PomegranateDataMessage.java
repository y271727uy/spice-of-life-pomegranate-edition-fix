package com.y271727uy.pomegranate.communication;

import com.y271727uy.pomegranate.PomegranateData;
import com.y271727uy.pomegranate.SOLCarrot;
import com.y271727uy.pomegranate.client.PomegranateClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class PomegranateDataMessage {
    private final CompoundTag tag;

    public PomegranateDataMessage(Player player) {
        this.tag = PomegranateData.toClientTag(player);
    }

    public PomegranateDataMessage(FriendlyByteBuf buffer) {
        this.tag = buffer.readNbt();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Handler.handle(this, context));
    }

    private static class Handler {
        static void handle(PomegranateDataMessage message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> {
                Player player = Minecraft.getInstance().player;
                if (player == null) return;
                // Update client-side cache: convert item id strings to Items and store counts
                // Clear previous cache first so resets are reflected client-side
                PomegranateClientData.clear();
                CompoundTag counts = message.tag.getCompound("FoodCounts");
                int populated = 0;
                for (String key : counts.getAllKeys()) {
                    int value = counts.getInt(key);
                    var rl = net.minecraft.resources.ResourceLocation.tryParse(key);
                    if (rl == null) continue;
                    Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
                    if (item == null) continue;
                    PomegranateClientData.setFoodCount(item, value);
                    populated++;
                }

                // Sync food history (StringTag list of item ids)
                var historyTag = message.tag.getList("FoodHistory", Tag.TAG_STRING);
                java.util.List<Item> history = new java.util.ArrayList<>();
                for (int i = 0; i < historyTag.size(); i++) {
                    String id = historyTag.getString(i);
                    var rl = net.minecraft.resources.ResourceLocation.tryParse(id);
                    if (rl == null) continue;
                    Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
                    if (item == null) continue;
                    history.add(item);
                }
                PomegranateClientData.setFoodHistory(history);

                // Sync classic decay config
                int maxHistory = message.tag.getInt("MaxFoodHistorySize");
                int maxShortHistory = message.tag.getInt("MaxShortFoodHistorySize");
                double shortDecay = message.tag.getDouble("ShortfoodDecayModifiers");
                var decayList = message.tag.getList("FoodDecayModifiers", Tag.TAG_DOUBLE);
                java.util.List<Double> decay = new java.util.ArrayList<>();
                for (int i = 0; i < decayList.size(); i++) {
                    decay.add(decayList.getDouble(i));
                }
                if (!decay.isEmpty()) {
                    PomegranateClientData.setClassicConfig(maxHistory, maxShortHistory, shortDecay, decay);
                }
                SOLCarrot.LOGGER.debug("PomegranateDataMessage received on client: populated {} food counts", populated);
            });
            context.get().setPacketHandled(true);
        }
    }
}
