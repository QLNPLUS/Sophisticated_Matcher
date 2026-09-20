package com.sophisticatedmatcher.util;

import com.mojang.serialization.Codec;
import com.sophisticatedmatcher.item.NbtMatcherItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MatcherData {
    private static final String DATA_KEY = "sophisticated_matcher";
    private static final String PREVIEW_KEY = "preview";
    private static final String COMPONENT_KEY = "component";
    private static final String VALUE_KEY = "value";

    private MatcherData() {
    }

    public record ComponentEntry(String id, String text, DataComponentType<?> type, Object value) {
    }

    public static List<ComponentEntry> entries(ItemStack stack) {
        List<ComponentEntry> entries = new ArrayList<>();
        if (stack.isEmpty()) {
            return entries;
        }
        for (TypedDataComponent<?> component : stack.getComponents()) {
            Identifier id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type());
            if (id == null) {
                continue;
            }
            String value = encodeComponent(component.type(), component.value()).map(Tag::toString).orElse(String.valueOf(component.value()));
            entries.add(new ComponentEntry(id.toString(), id + " = " + value, component.type(), component.value()));
        }
        return entries;
    }

    public static void save(ItemStack matcher, ItemStack preview, int entryIndex, HolderLookup.Provider registries) {
        List<ComponentEntry> entries = entries(preview);
        if (!NbtMatcherItem.isMatcher(matcher) || entryIndex < 0 || entryIndex >= entries.size()) {
            return;
        }

        ComponentEntry selected = entries.get(entryIndex);
        CompoundTag root = getCustomData(matcher);
        CompoundTag data = root.getCompoundOrEmpty(DATA_KEY);
        data.putString(COMPONENT_KEY, selected.id());
        encodeComponent(selected.type(), selected.value()).ifPresent(value -> data.put(VALUE_KEY, value.copy()));
        data.remove(PREVIEW_KEY);
        root.put(DATA_KEY, data);
        matcher.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static ItemStack preview(ItemStack matcher, HolderLookup.Provider registries) {
        CompoundTag data = getData(matcher);
        if (!data.contains(PREVIEW_KEY)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), data.get(PREVIEW_KEY))
                .result().orElse(ItemStack.EMPTY);
    }

    public static int selectedIndex(ItemStack matcher, ItemStack preview) {
        String selected = getData(matcher).getStringOr(COMPONENT_KEY, "");
        if (selected.isEmpty()) {
            return -1;
        }
        List<ComponentEntry> componentEntries = entries(preview);
        for (int i = 0; i < componentEntries.size(); i++) {
            if (selected.equals(componentEntries.get(i).id())) {
                return i;
            }
        }
        return -1;
    }
    public static void clearLegacyPreview(ItemStack matcher) {
        CompoundTag root = getCustomData(matcher);
        CompoundTag data = root.getCompoundOrEmpty(DATA_KEY);
        if (!data.contains(PREVIEW_KEY)) {
            return;
        }
        data.remove(PREVIEW_KEY);
        root.put(DATA_KEY, data);
        matcher.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static ComponentEntry selectedEntry(ItemStack matcher) {
        CompoundTag data = getData(matcher);
        String key = data.getStringOr(COMPONENT_KEY, "");
        Tag value = data.get(VALUE_KEY);
        if (key.isEmpty() || value == null) {
            return null;
        }
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE
                .getOptional(Identifier.parse(key)).orElse(null);
        return new ComponentEntry(key, key + " = " + value, type, value.copy());
    }

    public static boolean matches(ItemStack matcher, ItemStack target) {
        if (target.isEmpty()) {
            return false;
        }
        return matches(matcher, target.getComponents());
    }

    public static boolean matches(ItemStack matcher, net.minecraft.world.item.Item item, DataComponentMap components) {
        if (item == null) {
            return false;
        }
        return matches(matcher, components);
    }

    private static boolean matches(ItemStack matcher, DataComponentMap components) {
        CompoundTag data = getData(matcher);
        String componentId = data.getStringOr(COMPONENT_KEY, "");
        Tag expected = data.get(VALUE_KEY);
        if (componentId.isEmpty() || expected == null) {
            return false;
        }
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(Identifier.parse(componentId)).orElse(null);
        if (type == null) {
            return false;
        }
        Object actual = components.get(type);
        return actual != null && encodeComponent(type, actual).map(expected::equals).orElse(false);
    }

    private static CompoundTag getData(ItemStack matcher) {
        return getCustomData(matcher).getCompoundOrEmpty(DATA_KEY);
    }

    private static CompoundTag getCustomData(ItemStack matcher) {
        return matcher.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @SuppressWarnings("unchecked")
    private static Optional<Tag> encodeComponent(DataComponentType<?> type, Object value) {
        Codec<Object> codec = (Codec<Object>) type.codec();
        return codec.encodeStart(NbtOps.INSTANCE, value).result();
    }
}
