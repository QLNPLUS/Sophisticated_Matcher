package com.sophisticatedmatcher.compat;

import com.sophisticatedmatcher.client.MatcherSlotPositionAccess;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

/** Allows the client-only layout editor to move container slots without changing slot ids. */
@Mixin(Slot.class)
public abstract class MatcherSlotPositionMixin implements MatcherSlotPositionAccess {
    @Shadow @Final @Mutable public int x;
    @Shadow @Final @Mutable public int y;

    @Override
    public void sophisticatedMatcher$setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
