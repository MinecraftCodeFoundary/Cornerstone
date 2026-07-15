package org.yeyao.cornerstone.operations;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A target-backed menu whose slots categorically reject pickup, placement and quick-move. */
final class ReadOnlyContainerMenu extends AbstractContainerMenu {
    ReadOnlyContainerMenu(int id, Inventory ignoredViewerInventory, Container target, int rows) {
        super(rows == 3 ? MenuType.GENERIC_9x3 : MenuType.GENERIC_9x4, id);
        for (int row = 0; row < rows; row++) for (int column = 0; column < 9; column++) addSlot(new ReadOnlySlot(target, column + row * 9, 8 + column * 18, 18 + row * 18));
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return true; }
    private static final class ReadOnlySlot extends Slot {
        private ReadOnlySlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }
}
