package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MatcherButton extends Button {
    private static final ResourceLocation NORMAL = new ResourceLocation(SophisticatedMatcherMod.MOD_ID, "textures/gui/button.png");
    private static final ResourceLocation HOVER = new ResourceLocation(SophisticatedMatcherMod.MOD_ID, "textures/gui/button_hover.png");
    private static final ResourceLocation PRESSED = new ResourceLocation(SophisticatedMatcherMod.MOD_ID, "textures/gui/button_pressed.png");

    public MatcherButton(int x, int y, Component message, OnPress onPress) {
        super(x, y, 60, 16, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation texture = isFocused() && isHovered() ? PRESSED : (isHovered() ? HOVER : NORMAL);
        graphics.blit(texture, getX(), getY(), 0, 0, width, height, 60, 16);
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + 4, 0xFFFFFFFF);
    }
}
