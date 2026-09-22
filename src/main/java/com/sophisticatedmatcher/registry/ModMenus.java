package com.sophisticatedmatcher.registry;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.menu.MatcherMenu;
import com.sophisticatedmatcher.menu.MultiMatcherMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, SophisticatedMatcherMod.MOD_ID);
    public static final RegistryObject<MenuType<MatcherMenu>> MATCHER = MENUS.register("matcher",
            () -> new MenuType<>(MatcherMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<MultiMatcherMenu>> MULTI = MENUS.register("multi",
            () -> new MenuType<>(MultiMatcherMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {
    }
}
