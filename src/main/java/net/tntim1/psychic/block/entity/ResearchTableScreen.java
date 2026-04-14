package net.tntim1.psychic.block.entity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.tntim1.psychic.network.ModPackets;

import java.util.List;
import java.util.Random;

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

            this.addRenderableWidget(Button.builder(Component.literal("Start Research"), b -> {
                ModPackets.sendToServer(new RequestPuzzlePacket());
            }).bounds(x + 70, y + 100, 110, 20).build());

        } else if (currentState != State.PLAYING) {

            this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
                currentState = State.SELECTOR;
                addButtons();
            }).bounds(x + 75, y + 150, 100, 20).build());
        }
    }
    public void startLaserGame(String spellId, int difficulty) {
        LaserMirrorGame game = new LaserMirrorGame();
        game.setSpellId(spellId);
        game.init(difficulty);

        this.activeGame = game;
        this.currentState = State.PLAYING;
        addButtons();
    }



    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentState == State.PLAYING && activeGame != null) {
            activeGame.handleInput(mouseX, mouseY, button, (width - imageWidth) / 2, (height - imageHeight) / 2);
            if (activeGame.isLost()) { currentState = State.LOST; addButtons(); }
            if (activeGame.isWon()) {
                currentState = State.WON;

                String spellId = ((LaserMirrorGame) activeGame).getSpellId();
                ModPackets.sendToServer(new CompletePuzzlePacket(spellId));

                addButtons();
            }
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
            graphics.drawCenteredString(font, "Research Psychic Ability", width / 2, y + 20, 0xFFFFFF);
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