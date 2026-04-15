package net.tntim1.psychic.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Block entity for the Research Table.
 *
 * <p>Manages:
 * <ul>
 *   <li>8 item slots — one per laser emitter (IDs 1–8), each accepting only one
 *       specific item type defined in {@link #SLOT_FILTERS}.</li>
 *   <li>Hopper-compatible insertion/extraction via {@link WorldlyContainer},
 *       respecting the same per-slot item filters.</li>
 *   <li>Persistent NBT serialization of both the item inventory and the current
 *       {@link LaserMirrorGame} mirror placement state.</li>
 * </ul>
 */
public class ResearchTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    // =========================================================================
    // Slot → allowed item mapping  (edit these to match your actual mod items)
    // =========================================================================

    /**
     * Maps slot index (0–7, corresponding to laser IDs 1–8) to the registry
     * name of the only item allowed in that slot.
     *
     * <p><b>Replace the placeholder resource locations with your real item IDs.</b>
     */
    public static final Map<Integer, ResourceLocation> SLOT_FILTERS = new HashMap<>();

    static {
        // Laser 1 (left side, bottom-most) → slot 0
        SLOT_FILTERS.put(0, new ResourceLocation("psychic", "kyklos_essence"));
        // Laser 2 → slot 1
        SLOT_FILTERS.put(1, new ResourceLocation("psychic", "metabole_essence"));
        // Laser 3 → slot 2
        SLOT_FILTERS.put(2, new ResourceLocation("psychic", "thymos_essence"));
        // Laser 4 → slot 3
        SLOT_FILTERS.put(3, new ResourceLocation("psychic", "phthora_essence"));
        // Laser 5 (right side, top-most) → slot 4
        SLOT_FILTERS.put(4, new ResourceLocation("psychic", "energia_essence"));
        // Laser 6 → slot 5
        SLOT_FILTERS.put(5, new ResourceLocation("psychic", "techne_essence"));
        // Laser 7 → slot 6
        SLOT_FILTERS.put(6, new ResourceLocation("psychic", "mousike_essence"));
        // Laser 8 → slot 7
        SLOT_FILTERS.put(7, new ResourceLocation("psychic", "misos_essence"));
    }

    /** Total number of ingredient slots (one per laser). */
    public static final int SLOT_COUNT = 8;

    // =========================================================================
    // State
    // =========================================================================

    /** The 8 ingredient stacks. Index = laser ID - 1. */
    private final net.minecraft.core.NonNullList<ItemStack> items =
            net.minecraft.core.NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    /** Saved mirror placement state; null means no game has been started yet. */
    @Nullable
    private CompoundTag savedGameState = null;

    // =========================================================================
    // Constructor
    // =========================================================================

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_TABLE.get(), pos, state);
    }

    // =========================================================================
    // BaseContainerBlockEntity
    // =========================================================================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.psychic.research_table");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ResearchTableMenu(id, inventory, this);
    }

    // =========================================================================
    // Container
    // =========================================================================

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override public void clearContent() { items.clear(); setChanged(); }

    // =========================================================================
    // WorldlyContainer — hopper support
    // =========================================================================

    /** All slots are accessible from every direction for simplicity. */
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7};

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        // Allow hoppers to extract items (e.g. for auto-retrieval)
        return true;
    }

    // =========================================================================
    // Item filter
    // =========================================================================

    /**
     * Returns true only when {@code stack} is the item type allowed in {@code slot}.
     * The current stack in the slot must be empty (slots hold at most 1 item).
     */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;

        ResourceLocation allowed = SLOT_FILTERS.get(slot);
        if (allowed == null) return false;

        Item allowedItem = ForgeRegistries.ITEMS.getValue(allowed);
        if (allowedItem == null || !stack.is(allowedItem)) return false;

        ItemStack existing = items.get(slot);

        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(existing, stack)) return false;
            if (existing.getCount() >= existing.getMaxStackSize()) return false;
        }

        return true;
    }

    // =========================================================================
    // Mirror game state persistence
    // =========================================================================

    /**
     * Serializes the mirror placement grid from {@code game} and stores it in
     * {@link #savedGameState}.  Called by the server-side packet handler when
     * the player closes the screen or submits.
     */
    public void saveGameState(LaserMirrorGame game) {
        savedGameState = game.serializeToNBT();
        setChanged();
    }
    /** Stores raw mirror-game NBT (called by SaveGameStatePacket). */
    public void saveGameStateRaw(CompoundTag tag) {
        this.savedGameState = tag;
        this.setChanged(); // Marks the block for saving to the disk
    }

    /**
     * Returns the saved mirror placement NBT, or {@code null} if none exists.
     * The screen calls this to restore a previous session.
     */
    @Nullable
    public CompoundTag getSavedGameState() {
        return savedGameState;
    }

    /** Clears the saved game state (e.g. after a successful submission). */
    public void clearGameState() {
        savedGameState = null;
        setChanged();
    }

    // =========================================================================
    // NBT serialization
    // =========================================================================

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        if (tag.contains("MirrorGame", Tag.TAG_COMPOUND)) {
            savedGameState = tag.getCompound("MirrorGame");
        } else {
            savedGameState = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        if (savedGameState != null) {
            tag.put("MirrorGame", savedGameState);
        }
    }
}