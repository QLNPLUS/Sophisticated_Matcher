package com.sophisticatedmatcher.item;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.util.MatcherData;
import com.sophisticatedmatcher.util.MultiMatcherData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/** Holds up to nine configured single matchers, each with an and/but/or join state. */
public final class MultiMatcherItem extends Item {
    public MultiMatcherItem(Properties properties) {
        super(properties);
    }

    public static boolean isMatcher(ItemStack stack) {
        return MultiMatcherData.isMultiMatcher(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            MenuProvider provider = new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, ignored) -> new com.sophisticatedmatcher.menu.MultiMatcherMenu(id, inventory),
                    Component.translatable("container." + SophisticatedMatcherMod.MOD_ID + ".multi"));
            player.openMenu(provider);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        List<MultiMatcherData.Slot> slots = MultiMatcherData.slots(stack);
        List<MultiMatcherData.Slot> occupied = slots.stream()
                .filter(MultiMatcherData.Slot::occupied).toList();
        if (occupied.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip." + SophisticatedMatcherMod.MOD_ID + ".multi_empty")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.accept(Component.translatable("tooltip." + SophisticatedMatcherMod.MOD_ID + ".rule_count",
                        occupied.size())
                .withStyle(ChatFormatting.AQUA));
        boolean first = true;
        for (MultiMatcherData.Slot slot : occupied) {
            MatcherData.Rule rule = slot.rule();
            String text = (first ? "" : joinSymbol(slot.join()) + " ")
                    + MatcherData.ruleText(rule);
            tooltip.accept(Component.literal(text).withStyle(ChatFormatting.GRAY));
            first = false;
        }
    }

    public static String joinSymbol(MultiMatcherData.Join join) {
        return switch (join) {
            case AND -> "&&";
            case BUT -> "&&! ";
            case OR -> "||";
        };
    }
}
