package com.sophisticatedmatcher;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only options for tools used while arranging the matcher screen. */
public final class MatcherConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_LAYOUT_DEBUG = BUILDER
            .comment("DEBUG ONLY. Enables the F8 GUI layout editor.")
            .define("enableLayoutDebug", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MatcherConfig() {
    }

    public static boolean layoutDebugEnabled() {
        return ENABLE_LAYOUT_DEBUG.get();
    }
}
