package com.sophisticatedmatcher.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sophisticatedmatcher.MatcherConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

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
    private static final int MAX_OFFSET = 512;
    private static final EnumMap<Widget, Position> DEFAULT_POSITIONS = defaultPositions();
    private static final EnumMap<Widget, Position> POSITIONS = new EnumMap<>(Widget.class);
    private static Widget selected = Widget.TITLE;
    private static boolean enabled;
    private static boolean loaded;

    public enum Widget {
        TITLE("Title"),
        PREVIEW_SLOT("Preview slot"),
        SELECTOR_LABEL("Selector label"),
        SELECTOR("Selector"),
        DROPDOWN("Dropdown"),
        SAVE_BUTTON("Save button");

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
        loaded = false;
        load();
    }

    public static boolean isEnabled() {
        return enabled && MatcherConfig.layoutDebugEnabled();
    }

    public static boolean isConfiguredEnabled() {
        return MatcherConfig.layoutDebugEnabled();
    }

    public static boolean toggle() {
        if (!MatcherConfig.layoutDebugEnabled()) {
            return false;
        }
        load();
        enabled = !enabled;
        return enabled;
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

    public static void renderOverlay(GuiGraphics graphics, Font font,
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
        graphics.drawString(font, Component.literal(label), panelX, panelY, 0xFFFFD54F, false);
        graphics.drawString(font, Component.literal(offset), panelX,
                panelY + font.lineHeight, 0xFFFFFFFF, false);
    }

    private static Position position(Widget widget) {
        return POSITIONS.getOrDefault(widget,
                DEFAULT_POSITIONS.getOrDefault(widget, new Position(0, 0)));
    }

    private static EnumMap<Widget, Position> defaultPositions() {
        EnumMap<Widget, Position> positions = new EnumMap<>(Widget.class);
        for (Widget widget : Widget.values()) {
            positions.put(widget, new Position(0, 0));
        }
        return positions;
    }

    private static int clamp(int value) {
        return Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, value));
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(FILE_NAME);
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
