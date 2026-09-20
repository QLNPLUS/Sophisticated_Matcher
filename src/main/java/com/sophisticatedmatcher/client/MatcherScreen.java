package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.menu.MatcherMenu;
import com.sophisticatedmatcher.util.MatcherData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

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
    private static final int DROPDOWN_HEIGHT = 42;
    private static final int LIST_X = 55;
    private static final int LIST_Y = 26;
    private static final int ROW_HEIGHT = 12;
    private static final int VISIBLE_ROWS = 3;
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
        graphics.drawString(font, Component.translatable("container." + SophisticatedMatcherMod.MOD_ID + ".matcher"), 8, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".component"), SELECTOR_X, 10, 0x404040, false);
        if (!dropdownOpen) {
            graphics.drawString(font, font.plainSubstrByWidth(selectorSummary(), SELECTOR_WIDTH - 10),
                    SELECTOR_X + 5, SELECTOR_Y + 5, 0x404040, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dropdownOpen) {
            if (isInsideDropdown(mouseX, mouseY)) {
                if (button == 0) {
                    int row = (int) ((mouseY - topPos - LIST_Y) / ROW_HEIGHT);
                    int index = scrollOffset + row;
                    if (row >= 0 && row < VISIBLE_ROWS && index < menu.entries().size()) {
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
            scrollOffset = Math.min(scrollOffset, Math.max(0, menu.entries().size() - VISIBLE_ROWS));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (dropdownOpen && isInsideDropdown(mouseX, mouseY)) {
            int maxOffset = Math.max(0, menu.entries().size() - VISIBLE_ROWS);
            scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (saveButton != null) {
            saveButton.active = menu.selectedIndex() >= 0 && !menu.previewStack().isEmpty();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (dropdownOpen) {
            renderDropdown(graphics);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private void renderDropdown(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        graphics.blit(DROPDOWN, leftPos + SELECTOR_X, topPos + SELECTOR_Y,
                0, 0, DROPDOWN_WIDTH, DROPDOWN_HEIGHT, DROPDOWN_WIDTH, DROPDOWN_HEIGHT);

        List<MatcherData.ComponentEntry> entries = menu.entries();
        int maxOffset = Math.max(0, entries.size() - VISIBLE_ROWS);
        scrollOffset = Math.min(scrollOffset, maxOffset);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scrollOffset + row;
            if (index >= entries.size()) {
                break;
            }
            MatcherData.ComponentEntry entry = entries.get(index);
            int color = index == menu.selectedIndex() ? 0xFFFFFF55 : 0x404040;
            String text = font.plainSubstrByWidth(entry.text(), DROPDOWN_WIDTH - 10);
            graphics.drawString(font, text, leftPos + LIST_X, topPos + LIST_Y + row * ROW_HEIGHT, color, false);
        }
        graphics.flush();
        graphics.pose().popPose();
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
        return mouseX >= leftPos + SELECTOR_X && mouseX < leftPos + SELECTOR_X + SELECTOR_WIDTH
                && mouseY >= topPos + SELECTOR_Y && mouseY < topPos + SELECTOR_Y + SELECTOR_HEIGHT;
    }

    private boolean isInsideDropdown(double mouseX, double mouseY) {
        return mouseX >= leftPos + SELECTOR_X && mouseX < leftPos + SELECTOR_X + DROPDOWN_WIDTH
                && mouseY >= topPos + SELECTOR_Y && mouseY < topPos + SELECTOR_Y + DROPDOWN_HEIGHT;
    }
}
