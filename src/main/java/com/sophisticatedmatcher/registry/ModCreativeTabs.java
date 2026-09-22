package com.sophisticatedmatcher.registry;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** The mod's own creative tab, holding both matcher items. */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB, SophisticatedMatcherMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + SophisticatedMatcherMod.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.NBT_MATCHER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.NBT_MATCHER.get());
                        output.accept(ModItems.MULTI_NBT_MATCHER.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
