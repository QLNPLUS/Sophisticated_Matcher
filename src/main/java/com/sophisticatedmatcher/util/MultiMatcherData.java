package com.sophisticatedmatcher.util;

import com.sophisticatedmatcher.item.MultiMatcherItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores up to {@link #SLOT_COUNT} matcher rules inside the multi matcher item, one per
 * slot index, each with a join state (and/but/or). Only the matcher's rule is copied -
 * never the matcher item itself. Evaluation folds left to right over the occupied slots
 * from lowest index to highest: the first occupied slot's join state is ignored, then
 * AND = acc &amp;&amp; rule, BUT = acc &amp;&amp; !rule, OR = acc || rule.
 *
 * <p>On NeoForge 1.21.1 item data is read and written through the {@code CUSTOM_DATA}
 * component exactly like {@link MatcherData}: the slot list lives under the
 * {@code sophisticated_matcher} compound key {@code slots}, and every occupied entry
 * carries its join id plus the encoded rule it was read from.
 */
public final class MultiMatcherData {
    private static final String DATA_KEY = "sophisticated_matcher";
    private static final String SLOTS_KEY = "slots";
    private static final String LEGACY_ENTRIES_KEY = "entries";
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
        if (data.contains(SLOTS_KEY, Tag.TAG_LIST)) {
            ListTag list = data.getList(SLOTS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(list.size(), SLOT_COUNT); i++) {
                CompoundTag encoded = list.getCompound(i);
                Join join = Join.fromId(encoded.getString(JOIN_KEY));
                MatcherData.Rule rule = readRule(encoded);
                if (rule != null) {
                    result.set(i, new Slot(join, rule));
                }
            }
        } else if (data.contains(LEGACY_ENTRIES_KEY, Tag.TAG_LIST)) {
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
        CompoundTag data = root.getCompound(DATA_KEY);
        data.put(SLOTS_KEY, list);
        data.remove(LEGACY_ENTRIES_KEY);
        root.put(DATA_KEY, data);
        matcher.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(root));
    }

    /** Cycles the join state stored in a display stack's own component data; null removes it. */
    public static Join stackJoin(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Join.AND;
        }
        return Join.fromId(getData(stack).getString(JOIN_KEY));
    }

    public static void setStackJoin(ItemStack stack, Join join) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CompoundTag root = getCustomData(stack);
        CompoundTag data = root.getCompound(DATA_KEY);
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
        if (root.isEmpty()) {
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        } else {
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(root));
        }
    }

    /** Folds all occupied slots left to right; the first occupied slot's join is ignored. */
    public static boolean matches(ItemStack matcher, ItemStack target) {
        if (target == null || target.isEmpty()) {
            return false;
        }
        boolean result = false;
        boolean started = false;
        for (Slot slot : slots(matcher)) {
            if (!slot.occupied()) {
                continue;
            }
            MatcherData.Rule rule = slot.rule();
            Tag actual = MatcherData.resolve(target, rule.path());
            boolean current = MatcherData.matches(rule, actual);
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
        if (encoded.contains(RULE_KEY, Tag.TAG_COMPOUND)) {
            rule = MatcherData.decodeRule(encoded.getCompound(RULE_KEY));
        }
        // The Forge 1.20.1 implementation also recovered a rule from an entry that stored a
        // whole matcher item. That format predates the multi matcher on this branch, so
        // nothing can carry it here, and 1.21.1 only parses an ItemStack with a
        // HolderLookup.Provider that this helper never receives. Slots therefore only ever
        // carry an encoded rule.
        return rule != null && !rule.path().isEmpty() ? rule : null;
    }

    /** Converts the pre-slot rule list format into slot rules. */
    private static void migrateLegacyEntries(CompoundTag data, List<Slot> result) {
        ListTag list = data.getList(LEGACY_ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < SLOT_COUNT; i++) {
            CompoundTag encoded = list.getCompound(i);
            MatcherData.Rule rule = MatcherData.decodeRule(encoded.getCompound(RULE_KEY));
            if (!rule.path().isEmpty()) {
                result.set(i, new Slot(Join.fromId(encoded.getString(JOIN_KEY)), rule));
            }
        }
    }

    private static CompoundTag getData(ItemStack stack) {
        return getCustomData(stack).getCompound(DATA_KEY);
    }

    private static CompoundTag getCustomData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
    }
}
