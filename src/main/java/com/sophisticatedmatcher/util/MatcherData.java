package com.sophisticatedmatcher.util;

import com.mojang.serialization.Codec;
import com.sophisticatedmatcher.item.NbtMatcherItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Stores path-based NBT rules while reading item data through 1.21.1 data components. */
public final class MatcherData {
    private static final String DATA_KEY = "sophisticated_matcher";
    private static final String PREVIEW_KEY = "preview";
    private static final String COMPONENT_KEY = "component";
    private static final String RULE_KEY = "rule";
    private static final String NBT_KEY = "nbt_key";
    private static final String VALUE_KEY = "value";
    private static final String PATH_KEY = "path";
    private static final String OPERATOR_KEY = "operator";
    private static final String EXPECTED_KEY = "expected";
    private static final String MIN_KEY = "min";
    private static final String MAX_KEY = "max";
    private static final String MIN_INCLUSIVE_KEY = "min_inclusive";
    private static final String MAX_INCLUSIVE_KEY = "max_inclusive";

    private MatcherData() {
    }

    public record PathSegment(String key, int index, boolean listIndex) {
        public static PathSegment key(String key) {
            return new PathSegment(key, -1, false);
        }

        public static PathSegment index(int index) {
            return new PathSegment(null, index, true);
        }
    }

    public enum Operator {
        EQUALS("equals", "="),
        NOT_EQUALS("not_equals", "!="),
        GREATER_THAN("greater_than", ">"),
        GREATER_THAN_OR_EQUAL("greater_than_or_equal", ">="),
        LESS_THAN("less_than", "<"),
        LESS_THAN_OR_EQUAL("less_than_or_equal", "<="),
        BETWEEN("between", "between"),
        EXISTS("exists", "exists"),
        NOT_EXISTS("not_exists", "not exists");

        private final String id;
        private final String symbol;

        Operator(String id, String symbol) {
            this.id = id;
            this.symbol = symbol;
        }

        public String id() { return id; }
        public String symbol() { return symbol; }

        public boolean isNumeric() {
            return this == GREATER_THAN || this == GREATER_THAN_OR_EQUAL
                    || this == LESS_THAN || this == LESS_THAN_OR_EQUAL || this == BETWEEN;
        }

        public static Operator fromId(String id) {
            for (Operator operator : values()) {
                if (operator.id.equals(id)) return operator;
            }
            return EQUALS;
        }
    }

    public record Rule(List<PathSegment> path, Operator operator, Tag value, Tag min, Tag max,
                       boolean minInclusive, boolean maxInclusive) {
        public Rule {
            path = List.copyOf(path);
            value = copy(value);
            min = copy(min);
            max = copy(max);
        }

        public static Rule equals(List<PathSegment> path, Tag value) {
            return new Rule(path, Operator.EQUALS, value, null, null, true, true);
        }

        public static Rule exists(List<PathSegment> path, boolean expected) {
            return new Rule(path, expected ? Operator.EXISTS : Operator.NOT_EXISTS, null, null, null, true, true);
        }
    }

    public record TreeNode(List<PathSegment> path, String label, Tag value, List<TreeNode> children) {
        public TreeNode {
            path = List.copyOf(path);
            value = copy(value);
            children = List.copyOf(children);
        }

        public boolean branch() { return !children.isEmpty(); }
        public String pathText() { return MatcherData.pathText(path); }
        public String pathKey() { return MatcherData.pathKey(path); }
        public String valueText() { return value == null ? "" : value.toString(); }
    }

    public record ComponentEntry(String id, String text, DataComponentType<?> type, Object value) {
    }

    public static List<TreeNode> tree(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        List<TypedDataComponent<?>> components = new ArrayList<>();
        for (TypedDataComponent<?> component : stack.getComponents()) components.add(component);
        components.sort(Comparator.comparing(component -> {
            ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type());
            return id == null ? "" : id.toString();
        }));

        List<TreeNode> roots = new ArrayList<>();
        for (TypedDataComponent<?> component : components) {
            ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type());
            if (id == null) continue;
            encodeComponent(component.type(), component.value())
                    .ifPresent(value -> roots.add(node(List.of(), PathSegment.key(id.toString()), id.toString(), value)));
        }
        return roots;
    }

    public static List<TreeNode> visibleNodes(List<TreeNode> roots, Set<String> expanded) {
        List<TreeNode> result = new ArrayList<>();
        for (TreeNode root : roots) addVisible(root, expanded, result);
        return result;
    }

    public static List<ComponentEntry> entries(ItemStack stack) {
        List<ComponentEntry> result = new ArrayList<>();
        for (TreeNode node : leaves(tree(stack))) {
            DataComponentType<?> type = componentType(node.path());
            result.add(new ComponentEntry(node.pathText(), node.pathText() + " = " + node.valueText(),
                    type, node.value()));
        }
        return result;
    }

    public static void save(ItemStack matcher, ItemStack preview, int entryIndex, HolderLookup.Provider registries) {
        List<ComponentEntry> entries = entries(preview);
        if (entryIndex < 0 || entryIndex >= entries.size()) return;
        ComponentEntry entry = entries.get(entryIndex);
        saveRule(matcher, preview, Rule.equals(parsePath(entry.id()), (Tag) entry.value()));
    }

    /** Validates a rule against a preview item: path must resolve and numeric checks must hold. */
    public static boolean validRule(ItemStack preview, Rule rule) {
        if (preview.isEmpty() || rule == null || rule.path().isEmpty()) {
            return false;
        }
        Tag sample = resolve(preview, rule.path());
        if (sample == null || (rule.operator().isNumeric() && !(sample instanceof NumericTag))
                || (rule.operator().isNumeric() && (!numericValues(rule) || !validRange(rule)))) {
            return false;
        }
        return true;
    }

    /**
     * Builds a standalone single-matcher item stack carrying the given rule. On 1.21.1 the
     * rule travels in the {@code CUSTOM_DATA} component - the same place {@link #saveRule}
     * writes it - so the display stack is read back by {@link #selectedRule}.
     */
    public static net.minecraft.world.item.ItemStack matcherStack(Rule rule) {
        net.minecraft.world.item.ItemStack stack =
                new net.minecraft.world.item.ItemStack(com.sophisticatedmatcher.registry.ModItems.NBT_MATCHER.get());
        if (rule != null && !rule.path().isEmpty()) {
            CompoundTag root = getCustomData(stack);
            CompoundTag data = root.getCompound(DATA_KEY);
            data.put(RULE_KEY, encodeRule(rule));
            root.put(DATA_KEY, data);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
        return stack;
    }

    public static boolean saveRule(ItemStack matcher, ItemStack preview, Rule rule) {
        if (!NbtMatcherItem.isMatcher(matcher) || !validRule(preview, rule)) {
            return false;
        }

        CompoundTag root = getCustomData(matcher);
        CompoundTag data = root.getCompound(DATA_KEY);
        data.put(RULE_KEY, encodeRule(rule));
        data.remove(PREVIEW_KEY);
        data.remove(NBT_KEY);
        data.remove(VALUE_KEY);
        data.remove(COMPONENT_KEY);
        root.put(DATA_KEY, data);
        matcher.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(root));
        return true;
    }

    public static CompoundTag encodeRule(Rule rule) {
        CompoundTag encoded = new CompoundTag();
        ListTag path = new ListTag();
        for (PathSegment segment : rule.path()) {
            CompoundTag part = new CompoundTag();
            if (segment.listIndex()) {
                part.putString("kind", "index");
                part.putInt("value", segment.index());
            } else {
                part.putString("kind", "key");
                part.putString("value", segment.key());
            }
            path.add(part);
        }
        encoded.put(PATH_KEY, path);
        encoded.putString(OPERATOR_KEY, rule.operator().id());
        putIfPresent(encoded, EXPECTED_KEY, rule.value());
        putIfPresent(encoded, MIN_KEY, rule.min());
        putIfPresent(encoded, MAX_KEY, rule.max());
        encoded.putBoolean(MIN_INCLUSIVE_KEY, rule.minInclusive());
        encoded.putBoolean(MAX_INCLUSIVE_KEY, rule.maxInclusive());
        return encoded;
    }

    public static Rule decodeRule(CompoundTag encoded) {
        List<PathSegment> path = new ArrayList<>();
        if (encoded.contains(PATH_KEY, Tag.TAG_LIST)) {
            ListTag rawPath = encoded.getList(PATH_KEY, Tag.TAG_COMPOUND);
            for (int index = 0; index < rawPath.size(); index++) {
                CompoundTag part = rawPath.getCompound(index);
                path.add("index".equals(part.getString("kind"))
                        ? PathSegment.index(part.getInt("value"))
                        : PathSegment.key(part.getString("value")));
            }
        }
        return new Rule(path, Operator.fromId(encoded.getString(OPERATOR_KEY)),
                encoded.get(EXPECTED_KEY), encoded.get(MIN_KEY), encoded.get(MAX_KEY),
                !encoded.contains(MIN_INCLUSIVE_KEY) || encoded.getBoolean(MIN_INCLUSIVE_KEY),
                !encoded.contains(MAX_INCLUSIVE_KEY) || encoded.getBoolean(MAX_INCLUSIVE_KEY));
    }

    public static Rule selectedRule(ItemStack matcher) {
        CompoundTag data = getData(matcher);
        if (data.contains(RULE_KEY, Tag.TAG_COMPOUND)) return decodeRule(data.getCompound(RULE_KEY));
        String key = data.getString(NBT_KEY);
        Tag value = data.get(VALUE_KEY);
        if (key.isEmpty()) key = data.getString(COMPONENT_KEY);
        return !key.isEmpty() && value != null ? Rule.equals(List.of(PathSegment.key(key)), value) : null;
    }

    public static ComponentEntry selectedEntry(ItemStack matcher) {
        Rule rule = selectedRule(matcher);
        if (rule == null) return null;
        DataComponentType<?> type = componentType(rule.path());
        return new ComponentEntry(pathText(rule.path()), ruleText(rule), type, rule.value());
    }

    public static String ruleText(Rule rule) {
        String path = pathText(rule.path());
        return switch (rule.operator()) {
            case EQUALS -> path + " = " + valueText(rule.value());
            case NOT_EQUALS -> path + " != " + valueText(rule.value());
            case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL ->
                    path + " " + rule.operator().symbol() + " " + valueText(rule.value());
            case BETWEEN -> path + " " + (rule.minInclusive() ? "[" : "(")
                    + valueText(rule.min()) + ", " + valueText(rule.max())
                    + (rule.maxInclusive() ? "]" : ")");
            case EXISTS -> path + " exists";
            case NOT_EXISTS -> path + " not exists";
        };
    }

    public static Component ruleComponent(Rule rule) {
        String path = pathText(rule.path());
        return switch (rule.operator()) {
            case EQUALS -> Component.literal(path + " = " + valueText(rule.value()));
            case NOT_EQUALS -> Component.literal(path + " != " + valueText(rule.value()));
            case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL ->
                    Component.literal(path + " " + rule.operator().symbol() + " " + valueText(rule.value()));
            case BETWEEN -> Component.translatable("gui.sophisticated_matcher.rule.between", path,
                    rule.minInclusive() ? "[" : "(", valueText(rule.min()), valueText(rule.max()),
                    rule.maxInclusive() ? "]" : ")");
            case EXISTS, NOT_EXISTS -> Component.translatable("gui.sophisticated_matcher.rule." + rule.operator().id(), path);
        };
    }

    public static boolean matches(ItemStack matcher, ItemStack target) {
        if (target.isEmpty()) return false;
        Rule rule = selectedRule(matcher);
        return matches(rule, resolve(target.getComponents(), rule == null ? List.of() : rule.path()));
    }

    public static boolean matches(ItemStack matcher, net.minecraft.world.item.Item item, DataComponentMap components) {
        if (item == null) return false;
        Rule rule = selectedRule(matcher);
        return matches(rule, resolve(components, rule == null ? List.of() : rule.path()));
    }

    public static boolean matches(Rule rule, Tag actual) {
        if (rule == null) return false;
        if (rule.operator() == Operator.EXISTS) return actual != null;
        if (rule.operator() == Operator.NOT_EXISTS) return actual == null;
        if (actual == null) return false;
        if (rule.operator().isNumeric()) {
            if (!(actual instanceof NumericTag) || !numericValues(rule) || !validRange(rule)) return false;
            BigDecimal actualNumber = numericValue((NumericTag) actual);
            return switch (rule.operator()) {
                case GREATER_THAN -> actualNumber.compareTo(numericValue((NumericTag) rule.value())) > 0;
                case GREATER_THAN_OR_EQUAL -> actualNumber.compareTo(numericValue((NumericTag) rule.value())) >= 0;
                case LESS_THAN -> actualNumber.compareTo(numericValue((NumericTag) rule.value())) < 0;
                case LESS_THAN_OR_EQUAL -> actualNumber.compareTo(numericValue((NumericTag) rule.value())) <= 0;
                case BETWEEN -> {
                    int lower = actualNumber.compareTo(numericValue((NumericTag) rule.min()));
                    int upper = actualNumber.compareTo(numericValue((NumericTag) rule.max()));
                    yield (rule.minInclusive() ? lower >= 0 : lower > 0)
                            && (rule.maxInclusive() ? upper <= 0 : upper < 0);
                }
                default -> false;
            };
        }
        boolean equal = rule.value() != null && rule.value().equals(actual);
        return rule.operator() == Operator.EQUALS ? equal : !equal;
    }

    public static Tag resolve(ItemStack stack, List<PathSegment> path) {
        return stack.isEmpty() ? null : resolve(stack.getComponents(), path);
    }

    private static Tag resolve(DataComponentMap components, List<PathSegment> path) {
        if (path.isEmpty() || path.get(0).listIndex()) return null;
        ResourceLocation id;
        try {
            id = ResourceLocation.parse(path.get(0).key());
        } catch (RuntimeException exception) {
            return null;
        }
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(id).orElse(null);
        if (type == null) return null;
        Object component = components.get(type);
        if (component == null) return null;
        Tag encoded = encodeComponent(type, component).orElse(null);
        return encoded == null ? null : resolve(encoded, path.subList(1, path.size()));
    }

    public static Tag resolve(Tag root, List<PathSegment> path) {
        Tag current = root;
        for (PathSegment segment : path) {
            if (!segment.listIndex() && current instanceof CompoundTag compound) {
                current = compound.get(segment.key());
            } else if (segment.listIndex() && current instanceof ListTag list
                    && segment.index() >= 0 && segment.index() < list.size()) {
                current = list.get(segment.index());
            } else {
                return null;
            }
            if (current == null) return null;
        }
        return current;
    }

    public static Tag parseNumeric(String value, Tag sample) {
        if (!(sample instanceof NumericTag)) return null;
        String normalized = normalizeNumericInput(value);
        if (normalized.isEmpty()) return null;
        try {
            if (sample instanceof ByteTag) return ByteTag.valueOf(Byte.parseByte(normalized));
            if (sample instanceof ShortTag) return ShortTag.valueOf(Short.parseShort(normalized));
            if (sample instanceof IntTag) return IntTag.valueOf(Integer.parseInt(normalized));
            if (sample instanceof LongTag) return LongTag.valueOf(Long.parseLong(normalized));
            if (sample instanceof FloatTag) {
                float parsed = Float.parseFloat(normalized);
                return Float.isFinite(parsed) ? FloatTag.valueOf(parsed) : null;
            }
            if (sample instanceof DoubleTag) {
                double parsed = Double.parseDouble(normalized);
                return Double.isFinite(parsed) ? DoubleTag.valueOf(parsed) : null;
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    private static String normalizeNumericInput(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > 1) {
            char suffix = normalized.charAt(normalized.length() - 1);
            if ("bBsSlLfFdD".indexOf(suffix) >= 0) normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    public static String valueText(Tag value) {
        if (value instanceof ByteTag) return Byte.toString(((ByteTag) value).getAsByte()) + "b";
        if (value instanceof ShortTag) return Short.toString(((ShortTag) value).getAsShort()) + "s";
        if (value instanceof IntTag) return Integer.toString(((IntTag) value).getAsInt());
        if (value instanceof LongTag) return Long.toString(((LongTag) value).getAsLong()) + "l";
        if (value instanceof FloatTag) return Float.toString(((FloatTag) value).getAsFloat()) + "f";
        if (value instanceof DoubleTag) return Double.toString(((DoubleTag) value).getAsDouble()) + "d";
        return value == null ? "" : value.toString();
    }

    public static String pathText(List<PathSegment> path) {
        StringBuilder result = new StringBuilder();
        for (PathSegment segment : path) {
            if (segment.listIndex()) result.append('[').append(segment.index()).append(']');
            else {
                if (result.length() > 0) result.append('.');
                result.append(segment.key());
            }
        }
        return result.toString();
    }

    public static String pathKey(List<PathSegment> path) {
        StringBuilder result = new StringBuilder();
        for (PathSegment segment : path) {
            result.append(segment.listIndex() ? "#" + segment.index() : "." + segment.key());
        }
        return result.toString();
    }

    public static ItemStack preview(ItemStack matcher, HolderLookup.Provider registries) {
        CompoundTag data = getData(matcher);
        if (!data.contains(PREVIEW_KEY)) return ItemStack.EMPTY;
        return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), data.get(PREVIEW_KEY))
                .result().orElse(ItemStack.EMPTY);
    }

    public static int selectedIndex(ItemStack matcher, ItemStack preview) {
        Rule selected = selectedRule(matcher);
        if (selected == null) return -1;
        String selectedPath = pathText(selected.path());
        List<ComponentEntry> entries = entries(preview);
        for (int index = 0; index < entries.size(); index++) {
            if (selectedPath.equals(entries.get(index).id())) return index;
        }
        return -1;
    }

    public static void clearLegacyPreview(ItemStack matcher) {
        CompoundTag root = getCustomData(matcher);
        CompoundTag data = root.getCompound(DATA_KEY);
        if (!data.contains(PREVIEW_KEY)) return;
        data.remove(PREVIEW_KEY);
        root.put(DATA_KEY, data);
        matcher.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static List<TreeNode> children(Tag tag, List<PathSegment> parent) {
        List<TreeNode> result = new ArrayList<>();
        if (tag instanceof CompoundTag compound) {
            List<String> keys = new ArrayList<>(compound.getAllKeys());
            keys.sort(Comparator.naturalOrder());
            for (String key : keys) {
                Tag value = compound.get(key);
                if (value != null) result.add(node(parent, PathSegment.key(key), key, value));
            }
        } else if (tag instanceof ListTag list) {
            for (int index = 0; index < list.size(); index++) {
                result.add(node(parent, PathSegment.index(index), "[" + index + "]", list.get(index)));
            }
        }
        return result;
    }

    private static TreeNode node(List<PathSegment> parent, PathSegment segment, String label, Tag value) {
        List<PathSegment> path = new ArrayList<>(parent);
        path.add(segment);
        return new TreeNode(path, label, value, children(value, path));
    }

    private static void addVisible(TreeNode node, Set<String> expanded, List<TreeNode> result) {
        result.add(node);
        if (node.branch() && expanded.contains(node.pathKey())) {
            for (TreeNode child : node.children()) addVisible(child, expanded, result);
        }
    }

    private static List<TreeNode> leaves(List<TreeNode> roots) {
        List<TreeNode> result = new ArrayList<>();
        for (TreeNode node : roots) {
            if (node.branch()) result.addAll(leaves(node.children()));
            else result.add(node);
        }
        return result;
    }

    private static List<PathSegment> parsePath(String path) {
        List<PathSegment> result = new ArrayList<>();
        for (String part : path.split("\\.")) result.add(PathSegment.key(part));
        return result;
    }

    private static boolean numericValues(Rule rule) {
        return switch (rule.operator()) {
            case BETWEEN -> rule.min() instanceof NumericTag && rule.max() instanceof NumericTag;
            case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL -> rule.value() instanceof NumericTag;
            default -> true;
        };
    }

    public static boolean validRange(Rule rule) {
        if (rule == null || rule.operator() != Operator.BETWEEN) return true;
        if (!(rule.min() instanceof NumericTag min) || !(rule.max() instanceof NumericTag max)) return false;
        return numericValue(min).compareTo(numericValue(max)) <= 0;
    }

    private static BigDecimal numericValue(NumericTag value) {
        if (value instanceof ByteTag) return BigDecimal.valueOf(value.getAsByte());
        if (value instanceof ShortTag) return BigDecimal.valueOf(value.getAsShort());
        if (value instanceof IntTag) return BigDecimal.valueOf(value.getAsInt());
        if (value instanceof LongTag) return BigDecimal.valueOf(value.getAsLong());
        return BigDecimal.valueOf(value.getAsDouble());
    }

    private static DataComponentType<?> componentType(List<PathSegment> path) {
        if (path.isEmpty() || path.get(0).listIndex()) return null;
        try {
            return BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(ResourceLocation.parse(path.get(0).key()))
                    .orElse(null);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static CompoundTag getData(ItemStack matcher) {
        return getCustomData(matcher).getCompound(DATA_KEY);
    }

    private static CompoundTag getCustomData(ItemStack matcher) {
        if (matcher == null || matcher.isEmpty()) return new CompoundTag();
        return matcher.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @SuppressWarnings("unchecked")
    private static Optional<Tag> encodeComponent(DataComponentType<?> type, Object value) {
        Codec<Object> codec = (Codec<Object>) type.codec();
        return codec.encodeStart(NbtOps.INSTANCE, value).result();
    }

    private static void putIfPresent(CompoundTag target, String key, Tag value) {
        if (value != null) target.put(key, value.copy());
    }

    private static Tag copy(Tag value) {
        return value == null ? null : value.copy();
    }
}
