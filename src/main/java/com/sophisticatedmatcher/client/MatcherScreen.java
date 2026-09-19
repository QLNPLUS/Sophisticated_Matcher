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
    private static final int GUI_WIDTH = 250;
    private static final int GUI_HEIGHT = 200;
    private static final int LIST_X = 62;
    private static final int LIST_Y = 52;
    private static final int ROW_HEIGHT = 12;
    private static final int VISIBLE_ROWS = 6;
    private int scrollOffset;

    public MatcherScreen(MatcherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new MatcherButton(leftPos + 174, topPos + 20, Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".save"),
                button -> {
                    if (menu.selectedIndex() >= 0) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    }
                }));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("container." + SophisticatedMatcherMod.MOD_ID + ".matcher"), 8, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".component"), LIST_X, 39, 0x404040, false);

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
            String text = font.plainSubstrByWidth(entry.text(), 175);
            graphics.drawString(font, text, LIST_X, LIST_Y + row * ROW_HEIGHT, color, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= leftPos + LIST_X && mouseX < leftPos + GUI_WIDTH - 8
                && mouseY >= topPos + LIST_Y && mouseY < topPos + LIST_Y + VISIBLE_ROWS * ROW_HEIGHT) {
            int row = (int) ((mouseY - (topPos + LIST_Y)) / ROW_HEIGHT);
            int index = scrollOffset + row;
            if (index < menu.entries().size()) {
                menu.selectIndexClient(index);
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (mouseX >= leftPos + LIST_X && mouseX < leftPos + GUI_WIDTH - 8
                && mouseY >= topPos + LIST_Y && mouseY < topPos + LIST_Y + VISIBLE_ROWS * ROW_HEIGHT) {
            int maxOffset = Math.max(0, menu.entries().size() - VISIBLE_ROWS);
            scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
