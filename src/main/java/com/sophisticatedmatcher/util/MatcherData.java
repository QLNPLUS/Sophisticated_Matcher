package com.sophisticatedmatcher.util;

import com.sophisticatedmatcher.item.NbtMatcherItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Stores one top-level legacy item NBT entry on the matcher item. */
public final class MatcherData {
    private static final String DATA_KEY = "sophisticated_matcher";
    private static final String PREVIEW_KEY = "preview";
    private static final String NBT_KEY = "nbt_key";
    private static final String VALUE_KEY = "value";

    private MatcherData() {
    }

    public record ComponentEntry(String id, String text, Tag value) {
    }

    public static List<ComponentEntry> entries(ItemStack stack) {
        List<ComponentEntry> entries = new ArrayList<>();
        if (stack.isEmpty() || !stack.hasTag()) {
            return entries;
        }
        CompoundTag tag = stack.getTag();
        for (String key : tag.getAllKeys()) {
            Tag value = tag.get(key);
            if (value != null) {
                entries.add(new ComponentEntry(key, key + " = " + value, value.copy()));
            }
        }
        return entries;
    }

    public static void save(ItemStack matcher, ItemStack preview, int entryIndex) {
        List<ComponentEntry> entries = entries(preview);
        if (!NbtMatcherItem.isMatcher(matcher) || entryIndex < 0 || entryIndex >= entries.size()) {
            return;
        }

        ComponentEntry selected = entries.get(entryIndex);
        CompoundTag root = matcher.getOrCreateTag();
        CompoundTag data = root.contains(DATA_KEY, Tag.TAG_COMPOUND)
                ? root.getCompound(DATA_KEY)
                : new CompoundTag();
        data.putString(NBT_KEY, selected.id());
        data.put(VALUE_KEY, selected.value().copy());
        data.remove(PREVIEW_KEY);
        root.put(DATA_KEY, data);
        matcher.setTag(root);
    }

    public static void clearLegacyPreview(ItemStack matcher) {
        CompoundTag root = matcher.getTag();
        if (root == null || !root.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag data = root.getCompound(DATA_KEY);
        if (!data.contains(PREVIEW_KEY)) {
            return;
        }
        data.remove(PREVIEW_KEY);
        root.put(DATA_KEY, data);
        matcher.setTag(root);
    }

    public static ComponentEntry selectedEntry(ItemStack matcher) {
        CompoundTag data = getData(matcher);
        String key = data.getString(NBT_KEY);
        Tag value = data.get(VALUE_KEY);
        if (key.isEmpty() || value == null) {
            return null;
        }
        return new ComponentEntry(key, key + " = " + value, value.copy());
    }

    public static boolean matches(ItemStack matcher, ItemStack target) {
        if (target.isEmpty()) {
            return false;
        }
        CompoundTag data = getData(matcher);
        String key = data.getString(NBT_KEY);
        Tag expected = data.get(VALUE_KEY);
        CompoundTag targetTag = target.getTag();
        if (key.isEmpty() || expected == null || targetTag == null) {
            return false;
        }
        Tag actual = targetTag.get(key);
        return actual != null && expected.equals(actual);
    }

    private static CompoundTag getData(ItemStack matcher) {
        CompoundTag root = matcher.getTag();
        if (root == null || !root.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }
        return root.getCompound(DATA_KEY);
    }
}
