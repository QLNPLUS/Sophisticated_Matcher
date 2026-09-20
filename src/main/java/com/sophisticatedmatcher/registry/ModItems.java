package com.sophisticatedmatcher.registry;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.item.NbtMatcherItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, SophisticatedMatcherMod.MOD_ID);
    public static final Supplier<Item> NBT_MATCHER = ITEMS.register("nbt_matcher",
            key -> new NbtMatcherItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, key))
                    .stacksTo(1)));

    private ModItems() {
    }
}
