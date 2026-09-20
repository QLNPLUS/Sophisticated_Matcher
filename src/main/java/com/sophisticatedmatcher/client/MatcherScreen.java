package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.menu.MatcherMenu;
import com.sophisticatedmatcher.util.MatcherData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class MatcherScreen extends AbstractContainerScreen<MatcherMenu> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(SophisticatedMatcherMod.MOD_ID, "textures/gui/background.png");
    private static final ResourceLocation DROPDOWN = new ResourceLocation(SophisticatedMatcherMod.MOD_ID, "textures/gui/dropdown.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private static final int SELECTOR_X = 50;
    private static final int SELECTOR_Y = 20;
    private static final int SELECTOR_WIDTH = 120;
    private static final int SELECTOR_HEIGHT = 18;
    private static final int DROPDOWN_WIDTH = 120;
    private static final int DROPDOWN_PADDING = 6;
    private static final int ROW_HEIGHT = 12;
    private static final int MAX_VISIBLE_ROWS = 6;
    private int scrollOffset;
    private boolean dropdownOpen;
    private MatcherButton saveButton;

    public MatcherScreen(MatcherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        MatcherLayoutDebug.beginScreen();
        saveButton = new MatcherButton(leftPos + 108, topPos + 48,
                Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".save"),
                button -> {
                    if (menu.selectedIndex() >= 0 && !menu.previewStack().isEmpty()) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    }
                });
        addRenderableWidget(saveButton);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("container." + SophisticatedMatcherMod.MOD_ID + ".matcher"),
                layoutX(MatcherLayoutDebug.Widget.TITLE, 8),
                layoutY(MatcherLayoutDebug.Widget.TITLE, 6), 0x404040, false);
        graphics.drawString(font, Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".component"),
                layoutX(MatcherLayoutDebug.Widget.SELECTOR_LABEL, SELECTOR_X),
                layoutY(MatcherLayoutDebug.Widget.SELECTOR_LABEL, 10), 0x404040, false);
        if (!dropdownOpen) {
            graphics.drawString(font, font.plainSubstrByWidth(selectorSummary(), SELECTOR_WIDTH - 10),
                    layoutX(MatcherLayoutDebug.Widget.SELECTOR, SELECTOR_X) + 5,
                    layoutY(MatcherLayoutDebug.Widget.SELECTOR, SELECTOR_Y) + 5,
                    0x404040, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dropdownOpen) {
            if (isInsideDropdown(mouseX, mouseY)) {
                if (button == 0) {
                    int row = (int) ((mouseY - dropdownY() - DROPDOWN_PADDING) / ROW_HEIGHT);
                    int index = scrollOffset + row;
                    if (row >= 0 && row < visibleRows() && index < menu.entries().size()) {
                        menu.selectIndexClient(index);
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + index);
                        dropdownOpen = false;
                    }
                }
                return true;
            }
            dropdownOpen = false;
        }

        if (button == 0 && isInsideSelector(mouseX, mouseY)) {
            dropdownOpen = true;
            scrollOffset = Math.min(scrollOffset, maxScrollOffset());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (dropdownOpen && isInsideDropdown(mouseX, mouseY)) {
            if (menu.entries().size() > MAX_VISIBLE_ROWS) {
                scrollOffset = Math.max(0, Math.min(maxScrollOffset(),
                        scrollOffset - (int) Math.signum(scrollY)));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (saveButton != null) {
            saveButton.setX(layoutX(MatcherLayoutDebug.Widget.SAVE_BUTTON, 108) + leftPos);
            saveButton.setY(layoutY(MatcherLayoutDebug.Widget.SAVE_BUTTON, 48) + topPos);
            saveButton.active = menu.selectedIndex() >= 0 && !menu.previewStack().isEmpty();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (dropdownOpen) {
            renderDropdown(graphics);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
        renderDebugOverlay(graphics);
    }

    private void renderDropdown(GuiGraphics graphics) {
        int x = dropdownX();
        int y = dropdownY();
        int height = dropdownHeight();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        renderDropdownBackground(graphics, x, y, height);

        List<MatcherData.ComponentEntry> entries = menu.entries();
        scrollOffset = Math.min(scrollOffset, maxScrollOffset());
        int rows = visibleRows();
        for (int row = 0; row < rows; row++) {
            int index = scrollOffset + row;
            if (index >= entries.size()) {
                break;
            }
            MatcherData.ComponentEntry entry = entries.get(index);
            int color = index == menu.selectedIndex() ? 0xFFFFFF55 : 0x404040;
            String text = font.plainSubstrByWidth(entry.text(), DROPDOWN_WIDTH - 10);
            graphics.drawString(font, text, x + 5, y + DROPDOWN_PADDING + row * ROW_HEIGHT,
                    color, false);
        }
        if (entries.size() > MAX_VISIBLE_ROWS) {
            int trackX = x + DROPDOWN_WIDTH - 7;
            int trackY = y + DROPDOWN_PADDING;
            int trackHeight = rows * ROW_HEIGHT;
            graphics.fill(trackX, trackY, trackX + 3, trackY + trackHeight, 0x55333333);
            int knobHeight = Math.max(6, trackHeight * rows / entries.size());
            int knobRange = Math.max(0, trackHeight - knobHeight);
            int knobY = trackY + (maxScrollOffset() == 0 ? 0
                    : knobRange * scrollOffset / maxScrollOffset());
            graphics.fill(trackX, knobY, trackX + 3, knobY + knobHeight, 0xFF777777);
        }
        graphics.flush();
        graphics.pose().popPose();
    }

    private void renderDropdownBackground(GuiGraphics graphics, int x, int y, int height) {
        int textureHeight = 42;
        int edgeHeight = 4;
        graphics.blit(DROPDOWN, x, y, 0, 0, DROPDOWN_WIDTH, edgeHeight,
                DROPDOWN_WIDTH, textureHeight);
        int middleHeight = Math.max(0, height - edgeHeight * 2);
        int middleY = y + edgeHeight;
        while (middleHeight > 0) {
            int chunkHeight = Math.min(middleHeight, textureHeight - edgeHeight * 2);
            graphics.blit(DROPDOWN, x, middleY, 0, edgeHeight,
                    DROPDOWN_WIDTH, chunkHeight, DROPDOWN_WIDTH, textureHeight);
            middleY += chunkHeight;
            middleHeight -= chunkHeight;
        }
        graphics.blit(DROPDOWN, x, y + height - edgeHeight, 0,
                textureHeight - edgeHeight, DROPDOWN_WIDTH, edgeHeight,
                DROPDOWN_WIDTH, textureHeight);
    }

    private String selectorSummary() {
        List<MatcherData.ComponentEntry> entries = menu.entries();
        int selected = menu.selectedIndex();
        if (selected >= 0 && selected < entries.size()) {
            return entries.get(selected).text();
        }
        return Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".select").getString();
    }

    private boolean isInsideSelector(double mouseX, double mouseY) {
        int x = leftPos + layoutX(MatcherLayoutDebug.Widget.SELECTOR, SELECTOR_X);
        int y = topPos + layoutY(MatcherLayoutDebug.Widget.SELECTOR, SELECTOR_Y);
        return mouseX >= x && mouseX < x + SELECTOR_WIDTH
                && mouseY >= y && mouseY < y + SELECTOR_HEIGHT;
    }

    private boolean isInsideDropdown(double mouseX, double mouseY) {
        int x = dropdownX();
        int y = dropdownY();
        return mouseX >= x && mouseX < x + DROPDOWN_WIDTH
                && mouseY >= y && mouseY < y + dropdownHeight();
    }

    private int visibleRows() {
        return Math.max(1, Math.min(MAX_VISIBLE_ROWS, menu.entries().size()));
    }

    private int maxScrollOffset() {
        return Math.max(0, menu.entries().size() - MAX_VISIBLE_ROWS);
    }

    private int dropdownHeight() {
        return DROPDOWN_PADDING + visibleRows() * ROW_HEIGHT;
    }

    private int dropdownX() {
        return leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN, SELECTOR_X);
    }

    private int dropdownY() {
        return topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, SELECTOR_Y);
    }

    private int layoutX(MatcherLayoutDebug.Widget widget, int normalX) {
        return MatcherLayoutDebug.x(widget, normalX);
    }

    private int layoutY(MatcherLayoutDebug.Widget widget, int normalY) {
        return MatcherLayoutDebug.y(widget, normalY);
    }

    private void renderDebugOverlay(GuiGraphics graphics) {
        if (!MatcherLayoutDebug.isEnabled()) {
            return;
        }
        MatcherLayoutDebug.Widget widget = MatcherLayoutDebug.selected();
        DebugBounds bounds = debugBounds(widget);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 900);
        MatcherLayoutDebug.renderOverlay(graphics, font, bounds.x(), bounds.y(),
                bounds.width(), bounds.height());
        graphics.flush();
        graphics.pose().popPose();
    }

    private DebugBounds debugBounds(MatcherLayoutDebug.Widget widget) {
        return switch (widget) {
            case TITLE -> new DebugBounds(leftPos + layoutX(widget, 8),
                    topPos + layoutY(widget, 6), font.width(title), font.lineHeight);
            case PREVIEW_SLOT -> new DebugBounds(leftPos + layoutX(widget, 21),
                    topPos + layoutY(widget, 20), 20, 20);
            case SELECTOR_LABEL -> new DebugBounds(leftPos + layoutX(widget, SELECTOR_X),
                    topPos + layoutY(widget, 10),
                    font.width(Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".component")),
                    font.lineHeight);
            case SELECTOR -> new DebugBounds(leftPos + layoutX(widget, SELECTOR_X),
                    topPos + layoutY(widget, SELECTOR_Y), SELECTOR_WIDTH, SELECTOR_HEIGHT);
            case DROPDOWN -> new DebugBounds(dropdownX(), dropdownY(), DROPDOWN_WIDTH, dropdownHeight());
            case SAVE_BUTTON -> new DebugBounds(leftPos + layoutX(widget, 108),
                    topPos + layoutY(widget, 48), saveButton == null ? 60 : saveButton.getWidth(), 16);
        };
    }

    private record DebugBounds(int x, int y, int width, int height) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F8) {
            if (!MatcherLayoutDebug.isConfiguredEnabled()) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            MatcherLayoutDebug.toggle();
            return true;
        }
        if (MatcherLayoutDebug.isEnabled()) {
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                MatcherLayoutDebug.selectNext(hasShiftDown());
                return true;
            }
            int dx = 0;
            int dy = 0;
            if (keyCode == GLFW.GLFW_KEY_LEFT) dx = -1;
            if (keyCode == GLFW.GLFW_KEY_RIGHT) dx = 1;
            if (keyCode == GLFW.GLFW_KEY_UP) dy = -1;
            if (keyCode == GLFW.GLFW_KEY_DOWN) dy = 1;
            if (dx != 0 || dy != 0) {
                int step = hasAltDown() ? 1 : 5;
                MatcherLayoutDebug.moveSelected(dx * step, dy * step);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
