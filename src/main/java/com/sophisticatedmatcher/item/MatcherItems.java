package com.sophisticatedmatcher.item;

import com.sophisticatedmatcher.util.MatcherData;
import com.sophisticatedmatcher.util.MultiMatcherData;
import net.minecraft.world.item.ItemStack;

/** Dispatches filter matching to the single- or multi-rule matcher item. */
public final class MatcherItems {
    private MatcherItems() {
    }

    public static boolean isMatcher(ItemStack stack) {
        return NbtMatcherItem.isMatcher(stack) || MultiMatcherData.isMultiMatcher(stack);
    }

    public static boolean matches(ItemStack filter, ItemStack target) {
        if (MultiMatcherData.isMultiMatcher(filter)) {
            return MultiMatcherData.matches(filter, target);
        }
        if (NbtMatcherItem.isMatcher(filter)) {
            return MatcherData.matches(filter, target);
        }
        return false;
    }
}
