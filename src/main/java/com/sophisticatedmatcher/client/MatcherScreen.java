package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.menu.MatcherMenu;
import com.sophisticatedmatcher.util.MatcherData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class MatcherScreen extends AbstractContainerScreen<MatcherMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(SophisticatedMatcherMod.MOD_ID, "textures/gui/background.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private static final int PREVIEW_X = 21;
    private static final int PREVIEW_Y = 20;
    private final Inventory playerInventory;
    private MatcherButton editButton;

    public MatcherScreen(MatcherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        playerInventory = inventory;
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        MatcherLayoutDebug.beginScreen();
        MatcherLayoutDebug.applyMenuLayout(menu);
        editButton = new MatcherButton(leftPos + 50, topPos + 20,
                Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".edit"),
                button -> openEditor());
        addRenderableWidget(editButton);
    }

    private void openEditor() {
        if (!menu.previewStack().isEmpty()) {
            minecraft.setScreen(new MatcherEditorScreen(menu, playerInventory,
                    Component.translatable("container." + SophisticatedMatcherMod.MOD_ID + ".editor")));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("container." + SophisticatedMatcherMod.MOD_ID + ".matcher"),
                MatcherLayoutDebug.x(MatcherLayoutDebug.Widget.TITLE, 8),
                MatcherLayoutDebug.y(MatcherLayoutDebug.Widget.TITLE, 6), 0x404040, false);
        MatcherData.Rule rule = MatcherData.selectedRule(MatcherMenu.findMatcher(minecraft.player));
        Component summary = rule == null
                ? Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".no_rule")
                : MatcherData.ruleComponent(rule);
        graphics.drawString(font, font.plainSubstrByWidth(summary.getString(), 116),
                MatcherLayoutDebug.x(MatcherLayoutDebug.Widget.SELECTOR, 50) + 2,
                MatcherLayoutDebug.y(MatcherLayoutDebug.Widget.SELECTOR, 43), 0x555555, false);
        if (menu.previewStack().isEmpty()) {
            graphics.drawString(font, Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".place_preview"),
                    MatcherLayoutDebug.x(MatcherLayoutDebug.Widget.SELECTOR, 50) + 2,
                    MatcherLayoutDebug.y(MatcherLayoutDebug.Widget.SELECTOR, 58), 0x777777, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        MatcherLayoutDebug.applyMenuLayout(menu);
        if (editButton != null) {
            editButton.setX(leftPos + MatcherLayoutDebug.x(MatcherLayoutDebug.Widget.SELECTOR, 50));
            editButton.setY(topPos + MatcherLayoutDebug.y(MatcherLayoutDebug.Widget.SELECTOR, 20));
            editButton.active = !menu.previewStack().isEmpty() && !MatcherData.tree(menu.previewStack()).isEmpty();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderDebugOverlay(graphics);
    }

    private void renderDebugOverlay(GuiGraphics graphics) {
        if (!MatcherLayoutDebug.isEnabled()) {
            return;
        }
        MatcherLayoutDebug.Widget widget = MatcherLayoutDebug.selected();
        int x = leftPos + MatcherLayoutDebug.x(widget, 0);
        int y = topPos + MatcherLayoutDebug.y(widget, 0);
        MatcherLayoutDebug.renderOverlay(graphics, font, x, y, 60, 16);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MatcherLayoutDebug.keyPressed(keyCode, hasShiftDown(), hasAltDown())) {
            MatcherLayoutDebug.applyMenuLayout(menu);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
