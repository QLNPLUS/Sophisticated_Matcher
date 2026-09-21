package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.menu.MatcherMenu;
import com.sophisticatedmatcher.util.MatcherData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public final class MatcherScreen extends AbstractContainerScreen<MatcherMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/background.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private final Inventory playerInventory;
    private MatcherButton editButton;

    public MatcherScreen(MatcherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        playerInventory = inventory;
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
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0,
                GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        graphics.nextStratum();
        MatcherLayoutDebug.applyMenuLayout(menu);
        if (editButton != null) {
            editButton.setX(leftPos + MatcherLayoutDebug.x(MatcherLayoutDebug.Widget.SELECTOR, 50));
            editButton.setY(topPos + MatcherLayoutDebug.y(MatcherLayoutDebug.Widget.SELECTOR, 20));
            editButton.active = !menu.previewStack().isEmpty()
                    && !MatcherData.tree(menu.previewStack()).isEmpty();
        }
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        renderDebugOverlay(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.translatable("container." + SophisticatedMatcherMod.MOD_ID + ".matcher"),
                MatcherLayoutDebug.x(MatcherLayoutDebug.Widget.TITLE, 8),
                MatcherLayoutDebug.y(MatcherLayoutDebug.Widget.TITLE, 6), 0xFF404040, false);
    }

    private void renderDebugOverlay(GuiGraphicsExtractor graphics) {
        if (!MatcherLayoutDebug.isEnabled()) return;
        graphics.nextStratum();
        MatcherLayoutDebug.Widget widget = MatcherLayoutDebug.selected();
        int x = leftPos + MatcherLayoutDebug.x(widget, 0);
        int y = topPos + MatcherLayoutDebug.y(widget, 0);
        MatcherLayoutDebug.renderOverlay(graphics, font, x, y, 60, 16);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (MatcherLayoutDebug.keyPressed(event.key(), event.hasShiftDown(), event.hasAltDown())) {
            MatcherLayoutDebug.applyMenuLayout(menu);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_F8) return true;
        return super.keyPressed(event);
    }
}
