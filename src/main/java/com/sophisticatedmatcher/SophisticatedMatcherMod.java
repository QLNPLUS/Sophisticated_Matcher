package com.sophisticatedmatcher;

import com.sophisticatedmatcher.registry.ModItems;
import com.sophisticatedmatcher.registry.ModMenus;
import com.sophisticatedmatcher.compat.CreativeTabCompat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;

@Mod(SophisticatedMatcherMod.MOD_ID)
public final class SophisticatedMatcherMod {
    public static final String MOD_ID = "sophisticated_matcher";

    public SophisticatedMatcherMod(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        modEventBus.addListener(CreativeTabCompat::addItems);
        modContainer.registerConfig(
                ModConfig.Type.CLIENT, MatcherConfig.SPEC, "sophisticated_matcher-client.toml");
    }
}
