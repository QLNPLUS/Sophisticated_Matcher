package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MatcherButton extends Button {
    private static final Identifier NORMAL = Identifier.fromNamespaceAndPath(SophisticatedMatcherMod.MOD_ID, "textures/gui/button.png");
    private static final Identifier HOVER = Identifier.fromNamespaceAndPath(SophisticatedMatcherMod.MOD_ID, "textures/gui/button_hover.png");
    private static final Identifier PRESSED = Identifier.fromNamespaceAndPath(SophisticatedMatcherMod.MOD_ID, "textures/gui/button_pressed.png");
    private boolean mousePressed;

    public MatcherButton(int x, int y, Component message, OnPress onPress) {
        super(x, y, 60, 16, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Identifier texture = mousePressed ? PRESSED : (isHovered() ? HOVER : NORMAL);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), 0, 0, width, height, 60, 16);
        graphics.centeredText(Minecraft.getInstance().font, getMessage().getString(),
                getX() + width / 2, getY() + 4, getFGColor());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        mousePressed = true;
        super.onClick(event, doubleClick);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        mousePressed = false;
        super.onRelease(event);
    }
}
