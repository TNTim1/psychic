package net.tntim1.psychic.block.entity;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

public abstract class MiniGame {
    protected int width, height;

    public abstract void init(int difficulty); // 0=Easy, 1=Medium, 2=Hard
    public abstract void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, int startX, int startY);
    public abstract void handleInput(double mouseX, double mouseY, int button, int startX, int startY);
    public abstract boolean isWon();
    public abstract boolean isLost();
}