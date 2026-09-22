package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "sophisticated_matcher", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MatcherClient {
    private MatcherClient() {
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.MATCHER.get(), MatcherEditorScreen::new);
            MenuScreens.register(ModMenus.MULTI.get(), MultiMatcherScreen::new);
        });
    }
}
