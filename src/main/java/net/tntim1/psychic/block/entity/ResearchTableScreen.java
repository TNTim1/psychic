package net.tntim1.psychic.block.entity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private enum State { SELECTOR, PLAYING, WON, LOST }
    private State currentState = State.SELECTOR;
    private MiniGame activeGame = null;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 250;
        this.imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();
        this.addButtons();
    }

    private void addButtons() {
        this.clearWidgets();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (currentState == State.SELECTOR) {
            // --- MINESWEEPER ROW ---
            addDifficultyRow("Minesweeper", 0, x, y, new MinesweeperGame());

            // --- GAME 2 ROW (Placeholder) ---
            addDifficultyRow("Memory Game", 1, x, y, null);

            // --- GAME 3 ROW (Placeholder) ---
            addDifficultyRow("Laser Logic", 2, x, y, new LaserMirrorGame());

        } else if (currentState != State.PLAYING) {
            this.addRenderableWidget(Button.builder(Component.literal("Back to Menu"), b -> {
                currentState = State.SELECTOR;
                addButtons();
            }).bounds(x + 75, y + 150, 100, 20).build());
        } else {

        }
    }

    private void addDifficultyRow(String label, int row, int x, int y, MiniGame gameInstance) {
        int rowY = y + 40 + (row * 45);
        // Label
        // Easy
        this.addRenderableWidget(Button.builder(Component.literal("E"), b -> startGame(gameInstance, 0))
                .bounds(x + 110, rowY, 30, 20).build()).active = (gameInstance != null);
        // Medium
        this.addRenderableWidget(Button.builder(Component.literal("M"), b -> startGame(gameInstance, 1))
                .bounds(x + 145, rowY, 30, 20).build()).active = (gameInstance != null);
        // Hard
        this.addRenderableWidget(Button.builder(Component.literal("H"), b -> startGame(gameInstance, 2))
                .bounds(x + 180, rowY, 30, 20).build()).active = (gameInstance != null);
    }

    private void startGame(MiniGame game, int difficulty) {
        if (game == null) return;
        this.activeGame = game;
        this.activeGame.init(difficulty);
        this.currentState = State.PLAYING;
        addButtons();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentState == State.PLAYING && activeGame != null) {
            activeGame.handleInput(mouseX, mouseY, button, (width - imageWidth) / 2, (height - imageHeight) / 2);
            if (activeGame.isLost()) { currentState = State.LOST; addButtons(); }
            if (activeGame.isWon()) { currentState = State.WON; addButtons(); }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0xDD000000);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (currentState == State.SELECTOR) {
            graphics.drawCenteredString(font, "Select a Research Project", width / 2, y + 20, 0xFFFFFF);
            graphics.drawString(font, "Minesweeper:", x + 30, y + 46, 0xAAAAAA);
            graphics.drawString(font, "Memory:", x + 30, y + 91, 0xAAAAAA);
            graphics.drawString(font, "Laser Logic:", x + 30, y + 136, 0xAAAAAA);
        } else {
            activeGame.render(graphics, font, mouseX, mouseY, x, y);
            if (currentState == State.WON || currentState == State.LOST) {
                graphics.fill(0, 0, width, height, 0x88000000);
                Component msg = currentState == State.WON ? Component.literal("SUCCESS") : Component.literal("FAILED");
                graphics.drawCenteredString(font, msg, width / 2, height / 2 - 20, currentState == State.WON ? 0x55FF55 : 0xFF5555);
            }
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int x, int y) {}
}