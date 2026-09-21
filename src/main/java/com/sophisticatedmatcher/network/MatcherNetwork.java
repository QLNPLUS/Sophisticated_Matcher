package com.sophisticatedmatcher.network;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.menu.MatcherMenu;
import com.sophisticatedmatcher.util.MatcherData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class MatcherNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static int messageId;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SophisticatedMatcherMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private MatcherNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(messageId++, SaveRuleMessage.class, SaveRuleMessage::encode,
                SaveRuleMessage::decode, SaveRuleMessage::handle);
    }

    public static void sendSaveRule(int containerId, MatcherData.Rule rule) {
        CHANNEL.sendToServer(new SaveRuleMessage(containerId, MatcherData.encodeRule(rule)));
    }

    public record SaveRuleMessage(int containerId, CompoundTag rule) {
        private static void encode(SaveRuleMessage message, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.containerId);
            buffer.writeNbt(message.rule);
        }

        private static SaveRuleMessage decode(net.minecraft.network.FriendlyByteBuf buffer) {
            int containerId = buffer.readVarInt();
            CompoundTag rule = buffer.readNbt();
            return new SaveRuleMessage(containerId, rule == null ? new CompoundTag() : rule);
        }

        private static void handle(SaveRuleMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) {
                context.enqueueWork(() -> {
                    if (player.containerMenu.containerId != message.containerId
                            || !(player.containerMenu instanceof MatcherMenu menu)) {
                        return;
                    }
                    ItemStack matcher = MatcherMenu.findMatcher(player);
                    MatcherData.Rule rule = MatcherData.decodeRule(message.rule);
                    if (matcher != null && MatcherData.saveRule(matcher, menu.previewStack(), rule)) {
                        player.getInventory().setChanged();
                    }
                });
            }
            context.setPacketHandled(true);
        }
    }
}
