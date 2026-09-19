package com.sophisticatedmatcher.registry;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.item.NbtMatcherItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SophisticatedMatcherMod.MOD_ID);
    public static final RegistryObject<Item> NBT_MATCHER = ITEMS.register("nbt_matcher",
            () -> new NbtMatcherItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
