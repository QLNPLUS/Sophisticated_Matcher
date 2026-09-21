package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "sophisticated_matcher", value = Dist.CLIENT)
public final class MatcherClient {
    private MatcherClient() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MATCHER.get(), MatcherEditorScreen::new);
    }
}
