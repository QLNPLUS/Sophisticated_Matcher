package com.sophisticatedmatcher;

import com.sophisticatedmatcher.registry.ModItems;
import com.sophisticatedmatcher.registry.ModMenus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SophisticatedMatcherMod.MOD_ID)
public final class SophisticatedMatcherMod {
    public static final String MOD_ID = "sophisticated_matcher";

    public SophisticatedMatcherMod() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT, MatcherConfig.SPEC, "sophisticated_matcher-client.toml");
    }
}
