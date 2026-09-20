package com.sophisticatedmatcher;

import com.sophisticatedmatcher.registry.ModItems;
import com.sophisticatedmatcher.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;

@Mod(SophisticatedMatcherMod.MOD_ID)
public final class SophisticatedMatcherMod {
    public static final String MOD_ID = "sophisticated_matcher";

    public SophisticatedMatcherMod(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT, MatcherConfig.SPEC, "sophisticated_matcher-client.toml");
    }
}
