package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.menu.MultiMatcherMenu;
import com.sophisticatedmatcher.util.MultiMatcherData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Standard 176x166 slot editor for the multi matcher: nine storage slots across the top,
 * a three-position switch under each occupied slot (except the first occupied one, whose
 * join state is ignored), hover tooltips explaining each switch position, and the player
 * inventory.
 */
public final class MultiMatcherScreen extends AbstractContainerScreen<MultiMatcherMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/multi_background.png");
    private static final Identifier JOIN_SWITCH = Identifier.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/join_switch.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private static final int STORAGE_SLOT_X = 7;
    private static final int STORAGE_SLOT_Y = 20;
    private static final int SLOT_PITCH = 18;
    private static final int SWITCH_Y = 40;
    private static final int SWITCH_WIDTH = 8;
    private static final int SWITCH_HEIGHT = 28;
    /** The texture is a horizontal strip of three 8x28 frames: AND, BUT, OR. */
    private static final int SWITCH_TEXTURE_WIDTH = 24;
    private static final int SWITCH_TEXTURE_HEIGHT = 28;
    private static final int HINT_Y = 54;
    private static final int SWITCH_BUTTON_BASE = 300;

    public MultiMatcherScreen(MultiMatcherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    private int firstOccupiedSlot() {
        for (int i = 0; i < MultiMatcherMenu.SLOT_COUNT; i++) {
            if (!menu.storedStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasSwitch(int slot) {
        return !menu.storedStack(slot).isEmpty() && slot != firstOccupiedSlot();
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0,
                GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        graphics.nextStratum();
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        graphics.nextStratum();
        extractJoinSwitches(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 8, 6, 0xFF404040, false);
        if (firstOccupiedSlot() < 0) {
            graphics.text(font,
                    Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".multi_hint"),
                    8, HINT_Y, 0xFF777777, false);
        }
    }

    /** 三段式开关：每个已占用槽（首槽除外）下方一帧，帧 = 当前连接状态。 */
    private void extractJoinSwitches(GuiGraphicsExtractor graphics) {
        for (int i = 0; i < MultiMatcherMenu.SLOT_COUNT; i++) {
            if (!hasSwitch(i)) {
                continue;
            }
            MultiMatcherData.Join join = menu.joinAt(i);
            int x = leftPos + STORAGE_SLOT_X + i * SLOT_PITCH + (SLOT_PITCH - SWITCH_WIDTH) / 2;
            float u = join.ordinal() * SWITCH_WIDTH;
            graphics.blit(RenderPipelines.GUI_TEXTURED, JOIN_SWITCH, x, topPos + SWITCH_Y, u, 0.0F,
                    SWITCH_WIDTH, SWITCH_HEIGHT, SWITCH_TEXTURE_WIDTH, SWITCH_TEXTURE_HEIGHT);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Component> switchTooltip = switchTooltip(mouseX, mouseY);
        if (switchTooltip != null) {
            graphics.setComponentTooltipForNextFrame(font, switchTooltip, mouseX, mouseY);
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    /** Hover tooltip for a switch position: name plus a short description of that join state. */
    private List<Component> switchTooltip(double mouseX, double mouseY) {
        int slot = storageColumn(mouseX);
        int y = (int) mouseY - topPos;
        if (slot < 0 || !hasSwitch(slot)
                || y < SWITCH_Y || y >= SWITCH_Y + SWITCH_HEIGHT) {
            return null;
        }
        int segment = Math.min(2, Math.max(0, (y - SWITCH_Y) * 3 / SWITCH_HEIGHT));
        MultiMatcherData.Join join = MultiMatcherData.Join.values()[segment];
        String prefix = "gui." + SophisticatedMatcherMod.MOD_ID;
        return List.of(
                Component.translatable(prefix + ".join." + join.id()),
                Component.translatable(prefix + ".join_desc." + join.id())
                        .withStyle(style -> style.withItalic(true)));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int slot = storageColumn(event.x());
            int y = (int) event.y() - topPos;
            if (slot >= 0 && hasSwitch(slot) && y >= SWITCH_Y && y < SWITCH_Y + SWITCH_HEIGHT) {
                int segment = Math.min(2, Math.max(0, (y - SWITCH_Y) * 3 / SWITCH_HEIGHT));
                MultiMatcherData.Join join = MultiMatcherData.Join.values()[segment];
                menu.setJoin(slot, join);
                minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, SWITCH_BUTTON_BASE + slot * 3 + segment);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private int storageColumn(double mouseX) {
        int column = (int) ((mouseX - leftPos - STORAGE_SLOT_X) / SLOT_PITCH);
        return column >= 0 && column < MultiMatcherMenu.SLOT_COUNT ? column : -1;
    }
}
