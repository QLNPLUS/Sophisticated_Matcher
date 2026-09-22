package com.sophisticatedmatcher.util;

import com.sophisticatedmatcher.item.MultiMatcherItem;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Stores up to {@link #SLOT_COUNT} matcher rules inside the multi matcher item, one per
 * slot index, each with a join state (and/but/or). Only the matcher's rule is copied -
 * never the matcher item itself. Evaluation folds left to right over the occupied slots
 * from lowest index to highest: the first occupied slot's join state is ignored, then
 * AND = acc &amp;&amp; rule, BUT = acc &amp;&amp; !rule, OR = acc || rule.
 *
 * <p>On this branch the data is carried by {@link DataComponents#CUSTOM_DATA}, because item
 * NBT is replaced by data components; the stored compound layout is unchanged.</p>
 */
public final class MultiMatcherData {
    private static final String DATA_KEY = "sophisticated_matcher";
    private static final String SLOTS_KEY = "slots";
    private static final String LEGACY_ENTRIES_KEY = "entries";
    private static final String LEGACY_ITEM_KEY = "item";
    private static final String JOIN_KEY = "join";
    private static final String RULE_KEY = "rule";

    public static final int SLOT_COUNT = 9;

    private MultiMatcherData() {
    }

    public enum Join {
        AND("and"),
        BUT("but"),
        OR("or");

        private final String id;

        Join(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public Join next() {
            Join[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static Join fromId(String id) {
            for (Join join : values()) {
                if (join.id.equals(id)) {
                    return join;
                }
            }
            return AND;
        }
    }

    /** One stored slot. Occupied when it carries a rule. */
    public record Slot(Join join, MatcherData.Rule rule) {
        public boolean occupied() {
            return rule != null && !rule.path().isEmpty();
        }
    }

    public static boolean isMultiMatcher(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof MultiMatcherItem;
    }

    /** Returns exactly {@link #SLOT_COUNT} slots; unoccupied indices have a null rule. */
    public static List<Slot> slots(ItemStack matcher) {
        List<Slot> result = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            result.add(new Slot(Join.AND, null));
        }
        if (!isMultiMatcher(matcher)) {
            return result;
        }
        CompoundTag data = getData(matcher);
        if (data.contains(SLOTS_KEY)) {
            ListTag list = data.getListOrEmpty(SLOTS_KEY);
            for (int i = 0; i < Math.min(list.size(), SLOT_COUNT); i++) {
                CompoundTag encoded = list.getCompoundOrEmpty(i);
                Join join = Join.fromId(encoded.getStringOr(JOIN_KEY, ""));
                MatcherData.Rule rule = readRule(encoded);
                if (rule != null) {
                    result.set(i, new Slot(join, rule));
                }
            }
        } else if (data.contains(LEGACY_ENTRIES_KEY)) {
            migrateLegacyEntries(data, result);
        }
        return result;
    }

    /**
     * Writes the given per-slot joins and rules back to the matcher. Null rules store only
     * their join state so it survives an emptied slot.
     */
    public static void write(ItemStack matcher, List<Join> joins, List<MatcherData.Rule> rules) {
        if (!isMultiMatcher(matcher)) {
            return;
        }
        ListTag list = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            Join join = i < joins.size() && joins.get(i) != null ? joins.get(i) : Join.AND;
            MatcherData.Rule rule = i < rules.size() ? rules.get(i) : null;
            CompoundTag encoded = new CompoundTag();
            encoded.putString(JOIN_KEY, join.id());
            if (rule != null && !rule.path().isEmpty()) {
                encoded.put(RULE_KEY, MatcherData.encodeRule(rule));
            }
            list.add(encoded);
        }
        CompoundTag root = getCustomData(matcher);
        CompoundTag data = root.getCompoundOrEmpty(DATA_KEY);
        data.put(SLOTS_KEY, list);
        data.remove(LEGACY_ENTRIES_KEY);
        root.put(DATA_KEY, data);
        setCustomData(matcher, root);
    }

    /** Reads the join state carried by a display stack; an absent state means AND. */
    public static Join stackJoin(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Join.AND;
        }
        CompoundTag root = getCustomData(stack);
        if (!root.contains(DATA_KEY)) {
            return Join.AND;
        }
        return Join.fromId(root.getCompoundOrEmpty(DATA_KEY).getStringOr(JOIN_KEY, ""));
    }

    /** Sets the join state carried by a display stack; null removes it. */
    public static void setStackJoin(ItemStack stack, Join join) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CompoundTag root = getCustomData(stack);
        CompoundTag data = root.getCompoundOrEmpty(DATA_KEY);
        if (join == null) {
            data.remove(JOIN_KEY);
        } else {
            data.putString(JOIN_KEY, join.id());
        }
        if (data.isEmpty()) {
            root.remove(DATA_KEY);
        } else {
            root.put(DATA_KEY, data);
        }
        setCustomData(stack, root);
    }

    /** Folds all occupied slots left to right; the first occupied slot's join is ignored. */
    public static boolean matches(ItemStack matcher, ItemStack target) {
        if (target == null || target.isEmpty()) {
            return false;
        }
        return fold(matcher, rule -> MatcherData.matches(rule, MatcherData.resolve(target, rule.path())));
    }

    /**
     * Component-map variant used by the Sophisticated Core filter mixin, which receives the
     * candidate item's {@link DataComponentMap} instead of an item stack.
     */
    public static boolean matches(ItemStack matcher, Item item, DataComponentMap components) {
        if (item == null) {
            return false;
        }
        return fold(matcher, rule -> MatcherData.matches(rule, components));
    }

    private static boolean fold(ItemStack matcher, Predicate<MatcherData.Rule> test) {
        boolean result = false;
        boolean started = false;
        for (Slot slot : slots(matcher)) {
            if (!slot.occupied()) {
                continue;
            }
            boolean current = test.test(slot.rule());
            if (!started) {
                result = current;
                started = true;
                continue;
            }
            result = switch (slot.join()) {
                case AND -> result && current;
                case BUT -> result && !current;
                case OR -> result || current;
            };
        }
        return started && result;
    }

    private static MatcherData.Rule readRule(CompoundTag encoded) {
        MatcherData.Rule rule = null;
        if (encoded.contains(RULE_KEY)) {
            rule = MatcherData.decodeRule(encoded.getCompoundOrEmpty(RULE_KEY));
        } else if (encoded.contains(LEGACY_ITEM_KEY)) {
            // Earlier dev format stored the whole matcher item; keep only its rule.
            rule = legacyItemRule(encoded.getCompoundOrEmpty(LEGACY_ITEM_KEY));
        }
        return rule != null && !rule.path().isEmpty() ? rule : null;
    }

    /** Decodes a matcher item that the earlier dev format stored whole, then reads its rule. */
    private static MatcherData.Rule legacyItemRule(CompoundTag itemTag) {
        return ItemStack.CODEC.parse(NbtOps.INSTANCE, itemTag).result()
                .map(MatcherData::selectedRule)
                .orElse(null);
    }

    /** Converts the pre-slot rule list format into slot rules. */
    private static void migrateLegacyEntries(CompoundTag data, List<Slot> result) {
        ListTag list = data.getListOrEmpty(LEGACY_ENTRIES_KEY);
        for (int i = 0; i < list.size() && i < SLOT_COUNT; i++) {
            CompoundTag encoded = list.getCompoundOrEmpty(i);
            MatcherData.Rule rule = MatcherData.decodeRule(encoded.getCompoundOrEmpty(RULE_KEY));
            if (!rule.path().isEmpty()) {
                result.set(i, new Slot(Join.fromId(encoded.getStringOr(JOIN_KEY, "")), rule));
            }
        }
    }

    private static CompoundTag getData(ItemStack matcher) {
        return getCustomData(matcher).getCompoundOrEmpty(DATA_KEY);
    }

    private static CompoundTag getCustomData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void setCustomData(ItemStack stack, CompoundTag root) {
        if (root.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
    }
}
