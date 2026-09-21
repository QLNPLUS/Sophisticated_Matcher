package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MatcherButton extends Button {
    private static final int EDGE_SIZE = 2;
    private static final Identifier NORMAL = texture("button.png");
    private static final Identifier HOVER = texture("button_hover.png");
    private static final Identifier PRESSED = texture("button_pressed.png");
    private static final Identifier DISABLED = texture("button_disabled.png");

    private boolean mousePressed;
    private final Identifier normalTexture;
    private final Identifier hoverTexture;
    private final Identifier pressedTexture;
    private final Identifier disabledTexture;
    private final int textureWidth;
    private final int textureHeight;

    public MatcherButton(int x, int y, Component message, OnPress onPress) {
        this(x, y, 60, message, onPress, NORMAL, HOVER, PRESSED, DISABLED, 60, 16);
    }

    public MatcherButton(int x, int y, int width, Component message, OnPress onPress,
                         Identifier normalTexture, Identifier hoverTexture, Identifier pressedTexture,
                         int textureWidth, int textureHeight) {
        this(x, y, width, message, onPress, normalTexture, hoverTexture, pressedTexture,
                DISABLED, textureWidth, textureHeight);
    }

    public MatcherButton(int x, int y, int width, Component message, OnPress onPress,
                         Identifier normalTexture, Identifier hoverTexture, Identifier pressedTexture,
                         Identifier disabledTexture, int textureWidth, int textureHeight) {
        super(x, y, width, 16, message, onPress, DEFAULT_NARRATION);
        this.normalTexture = normalTexture;
        this.hoverTexture = hoverTexture;
        this.pressedTexture = pressedTexture;
        this.disabledTexture = disabledTexture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(SophisticatedMatcherMod.MOD_ID, "textures/gui/" + name);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = active && isHovered();
        Identifier texture = !active ? disabledTexture
                : (mousePressed && hovered ? pressedTexture : (hovered ? hoverTexture : normalTexture));
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), 0, 0,
                EDGE_SIZE, height, EDGE_SIZE, textureHeight, textureWidth, textureHeight);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX() + EDGE_SIZE, getY(), EDGE_SIZE, 0,
                width - EDGE_SIZE * 2, height, textureWidth - EDGE_SIZE * 2, textureHeight,
                textureWidth, textureHeight);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX() + width - EDGE_SIZE, getY(),
                textureWidth - EDGE_SIZE, 0, EDGE_SIZE, height, EDGE_SIZE, textureHeight, textureWidth, textureHeight);
        graphics.centeredText(Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + 4, 0xFFFFFFFF);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        mousePressed = event.button() == 0 && active;
        super.onClick(event, doubleClick);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        mousePressed = false;
        super.onRelease(event);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) mousePressed = false;
    }
}
