package com.sophisticatedmatcher.registry;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.menu.MatcherMenu;
import com.sophisticatedmatcher.menu.MultiMatcherMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SophisticatedMatcherMod.MOD_ID);
    public static final Supplier<MenuType<MatcherMenu>> MATCHER = MENUS.register("matcher",
            () -> new MenuType<>(MatcherMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<MultiMatcherMenu>> MULTI = MENUS.register("multi",
            () -> new MenuType<>(MultiMatcherMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {
    }
}
