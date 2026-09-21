package com.sophisticatedmatcher.network;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.menu.MatcherMenu;
import com.sophisticatedmatcher.util.MatcherData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class MatcherNetwork {
    private MatcherNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(SaveRulePayload.TYPE, SaveRulePayload.STREAM_CODEC,
                MatcherNetwork::handleSaveRule);
    }

    public static void sendSaveRule(int containerId, MatcherData.Rule rule) {
        ClientPacketDistributor.sendToServer(new SaveRulePayload(containerId, MatcherData.encodeRule(rule)));
    }

    private static void handleSaveRule(SaveRulePayload payload,
                                       net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || player.containerMenu.containerId != payload.containerId()
                || !(player.containerMenu instanceof MatcherMenu menu)) {
            return;
        }
        ItemStack matcher = MatcherMenu.findMatcher(player);
        MatcherData.Rule rule = MatcherData.decodeRule(payload.rule());
        if (matcher != null && MatcherData.saveRule(matcher, menu.previewStack(), rule)) {
            player.getInventory().setChanged();
        }
    }

    public record SaveRulePayload(int containerId, net.minecraft.nbt.CompoundTag rule)
            implements CustomPacketPayload {
        public static final Type<SaveRulePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
                SophisticatedMatcherMod.MOD_ID, "save_rule"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveRulePayload> STREAM_CODEC = StreamCodec.of(
                SaveRulePayload::write, SaveRulePayload::read);

        private static void write(RegistryFriendlyByteBuf buffer, SaveRulePayload payload) {
            buffer.writeVarInt(payload.containerId());
            buffer.writeNbt(payload.rule());
        }

        private static SaveRulePayload read(RegistryFriendlyByteBuf buffer) {
            int containerId = buffer.readVarInt();
            net.minecraft.nbt.CompoundTag rule = buffer.readNbt();
            return new SaveRulePayload(containerId, rule == null ? new net.minecraft.nbt.CompoundTag() : rule);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
