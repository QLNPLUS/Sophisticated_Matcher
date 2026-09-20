package com.sophisticatedmatcher;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only options for tools used while arranging the matcher screen. */
public final class MatcherConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_LAYOUT_DEBUG = BUILDER
            .comment("DEBUG ONLY. Enables the F8 GUI layout editor.")
            .define("enableLayoutDebug", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private MatcherConfig() {
    }

    public static boolean layoutDebugEnabled() {
        return ENABLE_LAYOUT_DEBUG.get();
    }
}
