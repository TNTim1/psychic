package net.tntim1.psychic.block.entity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for the Research Table.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>Slots 0–3  — left column, laser IDs 1–4 (top → bottom on-screen)</li>
 *   <li>Slots 4–7  — right column, laser IDs 5–8 (top → bottom on-screen)</li>
 *   <li>Slots 8–34 — player hotbar (9 slots) + main inventory (27 slots)</li>
 * </ul>
 *
 * <p>Each ingredient slot only accepts the item type registered in
 * {@link ResearchTableBlockEntity#SLOT_FILTERS} for that slot index.
 */
public class ResearchTableMenu extends AbstractContainerMenu {

    /** The backing block entity, used for slot validation. */
    private final ResearchTableBlockEntity blockEntity;

    // =========================================================================
    // Slot positions relative to the screen's (startX, startY)
    // Adjust these to fit your GUI texture / layout.
    // =========================================================================

    /**
     * Left-column slot positions (x, y) relative to gui origin.
     * Slots 0–3 = laser IDs 1–4.
     */
    private static final int GRID_CELL = 16;
    private static final int GRID_OFFSET_X = 4;
    private static final int GRID_OFFSET_Y = 40;
    public static final int[][] LEFT_SLOTS = {
            {12, 40},   // row 2
            {12, 72},   // row 4
            {12, 104},  // row 6
            {12, 136},  // row 8
    };

    public static final int[][] RIGHT_SLOTS = {
            {122, 24},  // row 1
            {122, 56},  // row 3
            {122, 88}, // row 5
            {122, 120}, // row 7
    };

    // =========================================================================
    // Constructors
    // =========================================================================

    /** Client-side constructor (called by registry / network). */
    /** Client-side constructor (called by registry / network). */
    public ResearchTableMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        // Read the BlockPos that was sent when the container was opened
        // Then find that Block Entity in the player's current world
        this(id, playerInv, (ResearchTableBlockEntity) playerInv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    /** Server-side constructor (called by the block entity). */
    public ResearchTableMenu(int id, Inventory playerInv, ResearchTableBlockEntity be) {
        super(ModMenus.RESEARCH_TABLE_MENU.get(), id);

        // If 'be' is null (client-side), use a dummy container of the correct size
        Container container = be != null ? be : new net.minecraft.world.SimpleContainer(ResearchTableBlockEntity.SLOT_COUNT);
        this.blockEntity = be;

        // ── Ingredient slots ─────────────────────────────────────────────────
        // Left column (lasers 1–4, slot indices 0–3)
        for (int i = 0; i < 4; i++) {
            final int slotIndex = i;
            // Use 'container' here instead of 'be'
            addSlot(new FilteredSlot(container, be, slotIndex, LEFT_SLOTS[i][0], LEFT_SLOTS[i][1]));
        }

        // Right column (lasers 5–8, slot indices 4–7)
        for (int i = 0; i < 4; i++) {
            final int slotIndex = i + 4;
            // Use 'container' here instead of 'be'
            addSlot(new FilteredSlot(container, be, slotIndex, RIGHT_SLOTS[i][0], RIGHT_SLOTS[i][1]));
        }

        // ── Player inventory (slots 8–34) ────────────────────────────────────
        // Main inventory (3 rows × 9 columns)
        // Main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9,
                        260 + col * 18,   // ⬅ move right (was 8)
                        40 + row * 18));  // ⬅ align vertically
            }
        }

// Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col,
                    260 + col * 18,
                    40 + 3 * 18 + 4));
        }
    }

    // =========================================================================
    // Slot validation / shift-click
    // =========================================================================

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || blockEntity.stillValid(player);
    }

    /**
     * Shift-click: tries to move an item from player inventory into the first
     * ingredient slot that accepts it; if none, does nothing.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < ResearchTableBlockEntity.SLOT_COUNT) {
            // Shift-click from an ingredient slot → try to send to player inventory
            if (!moveItemStackTo(stack, ResearchTableBlockEntity.SLOT_COUNT,
                    slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < ResearchTableBlockEntity.SLOT_COUNT; i++) {
                Slot target = slots.get(i);

                // Check if the item is allowed and if the slot isn't already full
                if (target.mayPlace(stack)) {
                    // Standard Minecraft helper to move items between specific ranges
                    // params: stack, startIndex, endIndex, reverseDirection
                    if (this.moveItemStackTo(stack, i, i + 1, false)) {
                        moved = true;
                        break;
                    }
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }

    // =========================================================================
    // Accessor
    // =========================================================================

    public ResearchTableBlockEntity getBlockEntity() { return blockEntity; }

    // =========================================================================
    // Inner class — filtered slot
    // =========================================================================

    /**
     * A slot that delegates {@link #mayPlace} to
     * {@link ResearchTableBlockEntity#isItemValidForSlot}.
     */
    private static class FilteredSlot extends Slot {
        private final ResearchTableBlockEntity owner;
        private final int slotIndex;

        // Add 'Container' to the constructor
        FilteredSlot(Container container, ResearchTableBlockEntity owner, int index, int xPos, int yPos) {
            super(container, index, xPos, yPos);
            this.owner = owner;
            this.slotIndex = index;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Now 'owner' can safely be null on the client
            if (owner == null) return true;
            return owner.isItemValidForSlot(slotIndex, stack);
        }

        @Override
        public int getMaxStackSize() { return getItem().getMaxStackSize(); }
    }
}