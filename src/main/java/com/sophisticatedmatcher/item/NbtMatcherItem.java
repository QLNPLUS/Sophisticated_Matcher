package com.sophisticatedmatcher.item;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.menu.MatcherMenu;
import com.sophisticatedmatcher.util.MatcherData;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class NbtMatcherItem extends Item {
    public NbtMatcherItem(Properties properties) {
        super(properties);
    }

    public static boolean isMatcher(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof NbtMatcherItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            MatcherData.clearLegacyPreview(player.getItemInHand(hand));
            MenuProvider provider = new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, ignored) -> new MatcherMenu(id, inventory),
                    Component.translatable("container." + SophisticatedMatcherMod.MOD_ID + ".editor"));
            player.openMenu(provider);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        MatcherData.ComponentEntry selected = MatcherData.selectedEntry(stack);
        if (selected == null) {
            tooltip.add(Component.translatable("tooltip." + SophisticatedMatcherMod.MOD_ID + ".empty")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip." + SophisticatedMatcherMod.MOD_ID + ".selected", selected.text())
                .withStyle(ChatFormatting.GRAY));
    }
}
