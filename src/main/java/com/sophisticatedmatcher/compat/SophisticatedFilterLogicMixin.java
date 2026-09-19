package com.sophisticatedmatcher.compat;

import com.sophisticatedmatcher.item.NbtMatcherItem;
import com.sophisticatedmatcher.util.MatcherData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic", remap = false)
public abstract class SophisticatedFilterLogicMixin {
    @Inject(method = "stackMatchesFilter", at = @At("HEAD"), cancellable = true, remap = false)
    private void sophisticatedMatcher$match(ItemStack stack, ItemStack filter, CallbackInfoReturnable<Boolean> callback) {
        if (NbtMatcherItem.isMatcher(filter)) {
            callback.setReturnValue(MatcherData.matches(filter, stack));
        }
    }
}
