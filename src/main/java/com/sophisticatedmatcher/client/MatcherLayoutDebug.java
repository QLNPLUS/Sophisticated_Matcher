package com.sophisticatedmatcher.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sophisticatedmatcher.MatcherConfig;
import com.sophisticatedmatcher.menu.MatcherMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;

/** F8 layout editor. Values are saved as offsets from the normal matcher GUI coordinates. */
public final class MatcherLayoutDebug {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "sophisticated_matcher_layout.json";
    private static final String EDITOR_FILE_NAME = "sophisticated_matcher_editor_layout.json";
    private static final int MAX_OFFSET = 512;
    private static final EnumMap<Widget, Position> DEFAULT_POSITIONS = defaultPositions();
    private static final EnumMap<Widget, Position> EDITOR_DEFAULT_POSITIONS = editorDefaultPositions();
    private static final EnumMap<Widget, Position> POSITIONS = new EnumMap<>(Widget.class);
    private static Widget selected = Widget.TITLE;
    private static boolean enabled;
    private static boolean loaded;
    private static boolean debugStateInitialized;
    private static boolean editorLayout;

    public enum Widget {
        TITLE("Title"),
        PREVIEW_SLOT("Preview slot"),
        SELECTOR("NBT tree content"),
        VERTICAL_SCROLLBAR("Vertical scrollbar"),
        HORIZONTAL_SCROLLBAR("Horizontal scrollbar"),
        DROPDOWN("Right controls"),
        SAVE_BUTTON("Action buttons"),
        INVENTORY("Player inventory"),
        HOTBAR("Hotbar");

        private final String label;

        Widget(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private MatcherLayoutDebug() {
    }

    public static void beginScreen() {
        beginScreen(false);
    }

    public static void beginEditorScreen() {
        beginScreen(true);
    }

    private static void beginScreen(boolean editor) {
        editorLayout = editor;
        loaded = false;
        load();
        if (!debugStateInitialized) {
            enabled = MatcherConfig.layoutDebugEnabledByDefault();
            debugStateInitialized = true;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean toggle() {
        load();
        enabled = !enabled;
        return enabled;
    }

    public static boolean keyPressed(int keyCode, boolean reverseSelection, boolean fineAdjustment) {
        if (keyCode == GLFW.GLFW_KEY_F8) {
            toggle();
            return true;
        }
        if (!isEnabled()) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            selectNext(reverseSelection);
            return true;
        }
        int dx = 0;
        int dy = 0;
        if (keyCode == GLFW.GLFW_KEY_LEFT) dx = -1;
        if (keyCode == GLFW.GLFW_KEY_RIGHT) dx = 1;
        if (keyCode == GLFW.GLFW_KEY_UP) dy = -1;
        if (keyCode == GLFW.GLFW_KEY_DOWN) dy = 1;
        if (dx == 0 && dy == 0) {
            return false;
        }
        int step = fineAdjustment ? 1 : 5;
        moveSelected(dx * step, dy * step);
        return true;
    }

    public static Widget selected() {
        return selected;
    }

    public static void selectNext(boolean reverse) {
        Widget[] widgets = Widget.values();
        int next = Math.floorMod(selected.ordinal() + (reverse ? -1 : 1), widgets.length);
        selected = widgets[next];
    }

    public static int x(Widget widget, int normalX) {
        return normalX + position(widget).x();
    }

    public static int y(Widget widget, int normalY) {
        return normalY + position(widget).y();
    }

    public static Position offset(Widget widget) {
        return position(widget);
    }

    public static void moveSelected(int dx, int dy) {
        Position current = position(selected);
        POSITIONS.put(selected, new Position(
                clamp(current.x() + dx), clamp(current.y() + dy)));
        save();
    }

    public static void applyMenuLayout(MatcherMenu menu) {
        if (menu.slots.size() < 37) {
            return;
        }
        applySlot(menu.slots.get(0), PREVIEW_SLOT_NORMAL_X, PREVIEW_SLOT_NORMAL_Y,
                Widget.PREVIEW_SLOT);
        for (int index = 1; index <= 27; index++) {
            int column = (index - 1) % 9;
            int row = (index - 1) / 9;
            applySlot(menu.slots.get(index), 8 + column * 18, 84 + row * 18,
                    Widget.INVENTORY);
        }
        for (int index = 28; index < 37; index++) {
            int column = index - 28;
            applySlot(menu.slots.get(index), 8 + column * 18, 142, Widget.HOTBAR);
        }
    }

    public static void renderOverlay(GuiGraphicsExtractor graphics, Font font,
                                     int x, int y, int width, int height) {
        int right = x + Math.max(1, width);
        int bottom = y + Math.max(1, height);
        graphics.fill(x, y, right, y + 1, 0xFFFFD54F);
        graphics.fill(x, bottom - 1, right, bottom, 0xFFFFD54F);
        graphics.fill(x, y, x + 1, bottom, 0xFFFFD54F);
        graphics.fill(right - 1, y, right, bottom, 0xFFFFD54F);

        String label = "F8 Debug | Tab: " + selected.label()
                + " | arrows: 5px | Alt: 1px";
        Position position = position(selected);
        String offset = "offset " + position.x() + ", " + position.y();
        int textWidth = Math.max(font.width(label), font.width(offset));
        int panelX = 4;
        int panelY = 4;
        graphics.fill(panelX - 2, panelY - 2, panelX + textWidth + 4,
                panelY + font.lineHeight * 2 + 3, 0xCC111111);
        graphics.text(font, Component.literal(label), panelX, panelY, 0xFFFFD54F, false);
        graphics.text(font, Component.literal(offset), panelX,
                panelY + font.lineHeight, 0xFFFFFFFF, false);
    }

    private static Position position(Widget widget) {
        return POSITIONS.getOrDefault(widget,
                (editorLayout ? EDITOR_DEFAULT_POSITIONS : DEFAULT_POSITIONS)
                        .getOrDefault(widget, new Position(0, 0)));
    }

    private static EnumMap<Widget, Position> defaultPositions() {
        EnumMap<Widget, Position> positions = new EnumMap<>(Widget.class);
        positions.put(Widget.TITLE, new Position(0, 0));
        positions.put(Widget.PREVIEW_SLOT, new Position(-1, 13));
        positions.put(Widget.SELECTOR, new Position(0, 11));
        positions.put(Widget.VERTICAL_SCROLLBAR, new Position(0, 0));
        positions.put(Widget.HORIZONTAL_SCROLLBAR, new Position(0, 0));
        positions.put(Widget.DROPDOWN, new Position(0, 11));
        positions.put(Widget.SAVE_BUTTON, new Position(0, 11));
        positions.put(Widget.INVENTORY, new Position(0, 0));
        positions.put(Widget.HOTBAR, new Position(0, 0));
        return positions;
    }

    private static EnumMap<Widget, Position> editorDefaultPositions() {
        EnumMap<Widget, Position> positions = new EnumMap<>(Widget.class);
        positions.put(Widget.TITLE, new Position(0, 0));
        positions.put(Widget.PREVIEW_SLOT, new Position(-71, -21));
        positions.put(Widget.SELECTOR, new Position(-1, -16));
        positions.put(Widget.VERTICAL_SCROLLBAR, new Position(-1, -19));
        positions.put(Widget.HORIZONTAL_SCROLLBAR, new Position(-1, -18));
        positions.put(Widget.DROPDOWN, new Position(0, 40));
        positions.put(Widget.SAVE_BUTTON, new Position(0, 1));
        positions.put(Widget.INVENTORY, new Position(0, -26));
        positions.put(Widget.HOTBAR, new Position(0, -26));
        return positions;
    }

    private static final int PREVIEW_SLOT_NORMAL_X = 21;
    private static final int PREVIEW_SLOT_NORMAL_Y = 20;

    private static void applySlot(Slot slot, int normalX, int normalY, Widget widget) {
        if (slot instanceof MatcherSlotPositionAccess access) {
            access.sophisticatedMatcher$setPosition(x(widget, normalX), y(widget, normalY));
        }
    }

    private static int clamp(int value) {
        return Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, value));
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(editorLayout ? EDITOR_FILE_NAME : FILE_NAME);
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        POSITIONS.clear();
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return;
            }
            JsonObject widgets = root.getAsJsonObject().getAsJsonObject("widgets");
            if (widgets == null) {
                return;
            }
            for (Widget widget : Widget.values()) {
                JsonElement raw = widgets.get(widget.name().toLowerCase(Locale.ROOT));
                if (raw == null || !raw.isJsonObject()) {
                    continue;
                }
                JsonObject value = raw.getAsJsonObject();
                POSITIONS.put(widget, new Position(
                        clamp(readInt(value, "x")), clamp(readInt(value, "y"))));
            }
        } catch (Exception exception) {
            System.err.println("[sophisticated_matcher] Could not read layout debug JSON: "
                    + exception.getMessage());
        }
    }

    private static int readInt(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isNumber() ? value.getAsInt() : 0;
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("description", "Component offsets relative to the normal GUI layout.");
        JsonObject widgets = new JsonObject();
        for (Widget widget : Widget.values()) {
            Position position = position(widget);
            JsonObject value = new JsonObject();
            value.addProperty("x", position.x());
            value.addProperty("y", position.y());
            widgets.add(widget.name().toLowerCase(Locale.ROOT), value);
        }
        root.add("widgets", widgets);

        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            System.err.println("[sophisticated_matcher] Could not save layout debug JSON: "
                    + exception.getMessage());
        }
    }

    public record Position(int x, int y) {
    }
}
