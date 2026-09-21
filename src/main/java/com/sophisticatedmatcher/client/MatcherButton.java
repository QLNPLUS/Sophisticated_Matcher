package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MatcherButton extends Button {
    private static final int EDGE_SIZE = 2;
    private static final ResourceLocation NORMAL = new ResourceLocation(SophisticatedMatcherMod.MOD_ID, "textures/gui/button.png");
    private static final ResourceLocation HOVER = new ResourceLocation(SophisticatedMatcherMod.MOD_ID, "textures/gui/button_hover.png");
    private static final ResourceLocation PRESSED = new ResourceLocation(SophisticatedMatcherMod.MOD_ID, "textures/gui/button_pressed.png");
    private static final ResourceLocation DISABLED = new ResourceLocation(SophisticatedMatcherMod.MOD_ID, "textures/gui/button_disabled.png");
    private boolean mousePressed;
    private final ResourceLocation normalTexture;
    private final ResourceLocation hoverTexture;
    private final ResourceLocation pressedTexture;
    private final ResourceLocation disabledTexture;
    private final int textureWidth;
    private final int textureHeight;

    public MatcherButton(int x, int y, Component message, OnPress onPress) {
        this(x, y, 60, message, onPress, NORMAL, HOVER, PRESSED, DISABLED, 60, 16);
    }

    public MatcherButton(int x, int y, int width, Component message, OnPress onPress,
                         ResourceLocation normalTexture, ResourceLocation hoverTexture,
                         ResourceLocation pressedTexture, int textureWidth, int textureHeight) {
        this(x, y, width, message, onPress, normalTexture, hoverTexture, pressedTexture,
                DISABLED, textureWidth, textureHeight);
    }

    public MatcherButton(int x, int y, int width, Component message, OnPress onPress,
                         ResourceLocation normalTexture, ResourceLocation hoverTexture,
                         ResourceLocation pressedTexture, ResourceLocation disabledTexture,
                         int textureWidth, int textureHeight) {
        super(x, y, width, 16, message, onPress, DEFAULT_NARRATION);
        this.normalTexture = normalTexture;
        this.hoverTexture = hoverTexture;
        this.pressedTexture = pressedTexture;
        this.disabledTexture = disabledTexture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = active && mouseX >= getX() && mouseX < getX() + width
                && mouseY >= getY() && mouseY < getY() + height;
        ResourceLocation texture = !active ? disabledTexture
                : (mousePressed && hovered ? pressedTexture : (hovered ? hoverTexture : normalTexture));
        graphics.blit(texture, getX(), getY(), EDGE_SIZE, height,
                0.0F, 0.0F, EDGE_SIZE, textureHeight, textureWidth, textureHeight);
        graphics.blit(texture, getX() + EDGE_SIZE, getY(), width - EDGE_SIZE * 2, height,
                (float) EDGE_SIZE, 0.0F, textureWidth - EDGE_SIZE * 2, textureHeight,
                textureWidth, textureHeight);
        graphics.blit(texture, getX() + width - EDGE_SIZE, getY(), EDGE_SIZE, height,
                (float) (textureWidth - EDGE_SIZE), 0.0F, EDGE_SIZE, textureHeight,
                textureWidth, textureHeight);
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + 4, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean clicked = super.mouseClicked(mouseX, mouseY, button);
        mousePressed = clicked && button == 0;
        return clicked;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean released = super.mouseReleased(mouseX, mouseY, button);
        if (button == 0) {
            mousePressed = false;
        }
        return released;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            mousePressed = false;
        }
    }
}
