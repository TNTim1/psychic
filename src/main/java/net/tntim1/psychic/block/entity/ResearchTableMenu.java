package net.tntim1.psychic.block.entity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.tntim1.psychic.block.ModBlocks;
import net.tntim1.psychic.block.entity.ModMenus;
import net.tntim1.psychic.block.entity.ResearchTableBlockEntity;

public class ResearchTableMenu extends AbstractContainerMenu {

    // Client-side constructor (used by Registry)
    public ResearchTableMenu(int id, Inventory inv, net.minecraft.network.FriendlyByteBuf data) {
        this(id, inv);
    }

    // Server-side constructor (used by BlockEntity)
    public ResearchTableMenu(int id, Inventory inv) {
        super(ModMenus.RESEARCH_TABLE_MENU.get(), id);

        // DO NOT add addSlot() calls for inv.items or inv.armor
        // By leaving this empty, the player inventory will not appear
        // or interact with your Minesweeper GUI.
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // No slots to move items between
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}