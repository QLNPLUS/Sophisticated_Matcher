package com.sophisticatedmatcher.creative;

import com.sophisticatedmatcher.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class CreativeTabCompat {
    private static final ResourceKey<CreativeModeTab> SOPHISTICATED_CORE_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, Identifier.parse("sophisticatedcore:main"));

    private CreativeTabCompat() {
    }

    public static void addItems(BuildCreativeModeTabContentsEvent event) {
        if (SOPHISTICATED_CORE_TAB.equals(event.getTabKey())) {
            event.accept(ModItems.NBT_MATCHER.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.MULTI_NBT_MATCHER.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
