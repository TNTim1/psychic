package net.tntim1.psychic.block.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.tntim1.psychic.network.ModPackets;
import net.tntim1.psychic.network.SaveGameStatePacket;

import java.util.HashMap;
import java.util.Map;

/**
 * Screen for the Research Table.
 *
 * <p>Layout (250 × 220 pixels):
 * <pre>
 * ┌──────────────────────────────────────────────────────┐
 * │  [L1] [L2] [L3] [L4]  ← left ingredient column      │
 * │                                                      │
 * │          8×8 laser puzzle grid                       │
 * │                                                      │
 * │                        [R5] [R6] [R7] [R8] → right  │
 * │                                                      │
 * │  [Submit]  (only enabled when puzzle is solved)      │
 * │                                                      │
 * │  ─── player inventory (3 rows) ───                   │
 * │  ─── hotbar ───────────────────                      │
 * └──────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>The ingredient slots are rendered by the parent {@link AbstractContainerScreen}
 * automatically (because we registered them in {@link ResearchTableMenu}).
 * We just need to draw slot backgrounds at the correct positions.
 */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {

    // ── States ────────────────────────────────────────────────────────────────
    private enum State { SELECTOR, PLAYING, WON }

    private State currentState = State.SELECTOR;
    private LaserMirrorGame activeGame = null;

    // ── Submit button reference ───────────────────────────────────────────────
    private Button submitButton;

    // ── Slot background colour (drawn behind each ingredient slot) ────────────
    private static final int SLOT_BG         = 0x88000000;
    private static final int SLOT_BG_ACTIVE  = 0x4400FF88;
    private static final int SLOT_BORDER     = 0xFF555577;
    private static final int SLOT_SIZE       = 18; // px (standard MC slot)

    // =========================================================================
    // Constructor
    // =========================================================================

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 420; // was 250
        this.imageHeight = 220;
    }

    // =========================================================================
    // Init
    // =========================================================================

    @Override
    protected void init() {
        super.init();
        // Hide the default "Inventory" label rendered by the parent
        this.inventoryLabelY = Integer.MIN_VALUE;
        this.titleLabelX     = 8;
        this.titleLabelY     = 6;
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();

        int x = leftPos;
        int y = topPos;

        if (currentState == State.SELECTOR) {
            addRenderableWidget(Button.builder(
                    Component.literal("Start Research"),
                    b -> {
                        // Get position from the Menu's BlockEntity
                        if (getMenu().getBlockEntity() != null) {
                            BlockPos pos = getMenu().getBlockEntity().getBlockPos();
                            ModPackets.sendToServer(new RequestPuzzlePacket(pos));
                        }
                    }
            ).bounds(x + 70, y + 100, 110, 20).build());

        } else if (currentState == State.PLAYING) {
            submitButton = addRenderableWidget(Button.builder(
                    Component.literal("Submit"),
                    b -> onSubmitClicked()
            ).bounds(x + 88, y + 195, 74, 14).build());
            submitButton.active = (activeGame != null
                    && activeGame.isWon()
                    && hasRequiredIngredients());

        } else { // WON
            addRenderableWidget(Button.builder(
                    Component.literal("Back"),
                    b -> {
                        currentState = State.SELECTOR;
                        rebuildButtons();
                    }
            ).bounds(x + 75, y + 150, 100, 20).build());
        }
    }
    private boolean hasRequiredIngredients() {
        if (activeGame == null) return false;

        Map<Integer, Integer> requiredCounts = activeGame.getRequiredLaserCounts();

        for (Map.Entry<Integer, Integer> entry : requiredCounts.entrySet()) {
            int laserId = entry.getKey();
            int amountNeeded = entry.getValue();

            // Use the mapping to find the correct slot
            int slotIndex = getSlotIndexForLaser(laserId);
            ItemStack stackInSlot = getMenu().getSlot(slotIndex).getItem();

            if (stackInSlot.isEmpty() || stackInSlot.getCount() < amountNeeded) {
                return false;
            }
        }
        return true;
    }
    /**
     * Maps Laser IDs (1-8 clockwise) to Menu Slot Indices (0-7 top-left to bottom-right).
     */
    private static int getSlotIndexForLaser(int laserId) {
        return switch (laserId) {
            // Left Column
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 3;
            // Right Column
            case 5 -> 4;
            case 6 -> 5;
            case 7 -> 6;
            case 8 -> 7;
            default -> 0;
        };
    }
    // =========================================================================
    // Called by network handler when server grants a puzzle
    // =========================================================================

    /**
     * Starts (or restores) a laser puzzle for the given spell.
     *
     * @param spellId    identifier used to look up goal connections
     * @param difficulty difficulty level passed to {@link LaserMirrorGame#init}
     * @param savedState optional NBT with a previously saved mirror placement;
     *                   pass {@code null} to start fresh
     */

    private int getGridX(int x, int startX) {
        return startX + 12 + x * 16;
    }

    private int getGridY(int y, int startY) {
        return startY + 24 + y * 16;
    }
    public void startLaserGame(String spellId,CompoundTag savedState) {

        LaserMirrorGame game = new LaserMirrorGame();

        // 1. Determine FINAL spellId FIRST
        String finalSpellId = null;

        if (spellId != null) {
            finalSpellId = spellId; // server always wins
        } else if (savedState != null && savedState.contains("spellId")) {
            finalSpellId = savedState.getString("spellId");
        }

        // 2. Apply BEFORE init
        if (finalSpellId != null) {
            game.setSpellId(finalSpellId);
        }

        // 3. Init (uses spellId!)
        game.init(8);

        // 4. Restore board state AFTER
        if (savedState != null) {
            game.deserializeFromNBT(savedState);
        }

        this.activeGame   = game;
        this.currentState = State.PLAYING;
        rebuildButtons();
    }

    // =========================================================================
    // Submit handling
    // =========================================================================

    private void onSubmitClicked() {
        if (activeGame == null || !activeGame.isWon() || !hasRequiredIngredients()) return;

        // Send the packet - let the SERVER handle the inventory
        String spellId = activeGame.getSpellId();
        ModPackets.sendToServer(new CompletePuzzlePacket(spellId));

        // Reset local UI state
        this.currentState = State.SELECTOR;
        this.activeGame = null;
        rebuildButtons();
    }

    // =========================================================================
    // Render
    // =========================================================================

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // Dark backdrop across the whole screen
        g.fill(0, 0, width, height, 0xDD000000);

        int x = leftPos;
        int y = topPos;

        // ── Panel border ──────────────────────────────────────────────────────
        g.fill(x, y, x + imageWidth, y + imageHeight, 0x99111122);
        g.fill(x, y, x + imageWidth, y + 1,           0xFF6666AA);
        g.fill(x, y, x + 1,         y + imageHeight,  0xFF6666AA);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF6666AA);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF6666AA);

        if (currentState == State.SELECTOR) {
            g.drawCenteredString(font, "Research Psychic Ability",
                    x + imageWidth / 2, y + 20, 0xFFFFFFFF);
            return;
        }

        // ── Active puzzle rendering ───────────────────────────────────────────
        if (activeGame != null) {
            activeGame.render(g, font, mouseX, mouseY, x, y);

            // Update submit button state each frame
            if (submitButton != null) {
                boolean canSubmit = activeGame.isWon() && hasRequiredIngredients();
                submitButton.active = canSubmit;
                if (canSubmit) {
                    // Green tint around button
                    g.fill(x + 86, y + 193, x + 164, y + 211, 0x4400FF88);
                }
            }
        }

        // ── Ingredient slot backgrounds ───────────────────────────────────────
        //drawIngredientSlots(g, x, y);

        // ── Player inventory background ───────────────────────────────────────
        drawPlayerInventoryBg(g, x, y);

        // ── Win / fail overlay ────────────────────────────────────────────────
        if (currentState == State.WON) {
            g.fill(0, 0, width, height, 0x88000000);
            g.drawCenteredString(font, "SUCCESS", width / 2, height / 2 - 20, 0xFF55FF55);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Suppress default title + inventory labels (we draw them manually)
    }

    // ── Slot drawing helpers ──────────────────────────────────────────────────

    /**
     * Draws 16×16 slot backgrounds at the positions declared in
     * {@link ResearchTableMenu#LEFT_SLOTS} and {@link ResearchTableMenu#RIGHT_SLOTS}.
     * The actual item icons are rendered by {@link AbstractContainerScreen} automatically.
     */
    private void drawIngredientSlots(GuiGraphics g, int x, int y) {
        g.drawString(font, "Ingredients", x + 6, y + 28, 0xFF8899BB);

        // LEFT side (aligned to laser grid left edge)
        for (int i = 0; i < 4; i++) {
            int sx = x + 4;              // same as grid start offset
            int sy = y + 24 + i * 16;   // align vertically with cells

            drawSlotBg(g, sx, sy, i);
            drawLaserColorDot(g, sx - 6, sy + 4, i);
            g.drawString(font, "#" + (i + 1), sx + SLOT_SIZE + 2, sy + 5, 0xFF6677AA);
        }

        // RIGHT side (aligned to grid width)
        for (int i = 0; i < 4; i++) {
            int slotIdx = i + 4;

            int sx = x + 4 + (8 * 16);   // grid width offset (8 cells)
            int sy = y + 24 + i * 16;

            drawSlotBg(g, sx, sy, slotIdx);
            drawLaserColorDot(g, sx + SLOT_SIZE + 2, sy + 4, slotIdx);
            g.drawString(font, "#" + (slotIdx + 1), sx - 14, sy + 5, 0xFF6677AA);
        }
    }
    private void drawSlotBg(GuiGraphics g, int sx, int sy, int slotIdx) {
        boolean filled = !getMenu().getSlot(slotIdx).getItem().isEmpty();
        int bg = filled ? SLOT_BG_ACTIVE : SLOT_BG;

        g.fill(sx - 1, sy - 1, sx + SLOT_SIZE + 1, sy + SLOT_SIZE + 1, SLOT_BORDER);
        g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, bg);
    }

    /** Small colour square matching the laser's ARGB colour for quick identification. */
    private void drawLaserColorDot(GuiGraphics g, int dotX, int dotY, int slotIdx) {
        int[] laserColors = {
                0xFFFF3232, 0xFFFFA500, 0xFFFFFF32, 0xFF32FF32,
                0xFF32FFFF, 0xFF3264FF, 0xFFAA32FF, 0xFFFF32AA
        };
        if (slotIdx < 0 || slotIdx >= laserColors.length) return;
        g.fill(dotX, dotY, dotX + 4, dotY + 4, laserColors[slotIdx]);
    }

    /** Renders the standard Minecraft player-inventory background panels. */
    private void drawPlayerInventoryBg(GuiGraphics g, int x, int y) {
        int invX = x + 260;   // ⬅ match slot X
        int invY = y + 40;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = invX + col * 18;
                int sy = invY + row * 18;
                g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF333344);
                g.fill(sx, sy, sx + 16, sy + 16, 0xFF1A1A2A);
            }
        }

        for (int col = 0; col < 9; col++) {
            int sx = invX + col * 18;
            int sy = invY + 3 * 18 + 4;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF445566);
            g.fill(sx, sy, sx + 16, sy + 16, 0xFF1A1A2A);
        }

        g.drawString(font, "Inventory", invX, invY - 10, 0xFF888899);
    }

    // =========================================================================
    // Mouse input
    // =========================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        // 1. ✅ BUTTON gets priority
        if (submitButton != null && submitButton.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // 2. ✅ GAME AREA (your grid)
        if (currentState == State.PLAYING && activeGame != null) {
            if (!isOverIngredientSlots(mouseX, mouseY) && !isOverPlayerInventory(mouseX, mouseY)) {

                activeGame.handleInput(mouseX, mouseY, button, leftPos, topPos);
                return true;
            }
        }

        // 3. ✅ EVERYTHING ELSE (slots, inventory)
        return super.mouseClicked(mouseX, mouseY, button);
    }
    // ── Hit-test helpers ──────────────────────────────────────────────────────

    private boolean isOverIngredientSlots(double mx, double my) {
        for (int i = 0; i < 4; i++) {
            if (overSlot(mx, my, leftPos + ResearchTableMenu.LEFT_SLOTS[i][0],
                    topPos + ResearchTableMenu.LEFT_SLOTS[i][1])) return true;
            if (overSlot(mx, my, leftPos + ResearchTableMenu.RIGHT_SLOTS[i][0],
                    topPos + ResearchTableMenu.RIGHT_SLOTS[i][1])) return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        // Added a null check for getBlockEntity() just in case
        if (activeGame != null && getMenu().getBlockEntity() != null) {
            BlockPos pos = getMenu().getBlockEntity().getBlockPos();
            CompoundTag nbt = activeGame.serializeToNBT();
            ModPackets.sendToServer(new SaveGameStatePacket(pos, nbt));
        }
        super.onClose();
    }

    private boolean isOverPlayerInventory(double mx, double my) {
        int invLeft = leftPos + 260;
        int invRight = invLeft + 9 * 18;

        int invTop = topPos + 40;
        int invBottom = invTop + 3 * 18 + 4 + 18;

        return mx >= invLeft && mx < invRight && my >= invTop && my < invBottom;
    }

    private boolean overSlot(double mx, double my, int sx, int sy) {
        return mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE;
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    private boolean allIngredientSlotsFilled() {
        for (int i = 0; i < ResearchTableBlockEntity.SLOT_COUNT; i++) {
            if (getMenu().getSlot(i).getItem().isEmpty()) return false;
        }
        return true;
    }
}