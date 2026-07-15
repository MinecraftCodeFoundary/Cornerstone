package org.yeyao.cornerstone.operations;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.GameType;

/** Safe operational utilities. Inventory viewers expose read-only slots backed by the real container. */
public final class UtilityService {
    public void openInventory(ServerPlayer viewer, ServerPlayer target) { open(viewer, target.getInventory(), 4, target.getGameProfile().getName() + "'s inventory"); }
    public void openEnderChest(ServerPlayer viewer, ServerPlayer target) { open(viewer, target.getEnderChestInventory(), 3, target.getGameProfile().getName() + "'s ender chest"); }
    public void clearInventory(ServerPlayer target) { target.getInventory().clearContent(); target.containerMenu.broadcastChanges(); target.inventoryMenu.broadcastChanges(); }
    public void heal(ServerPlayer target) { target.setHealth(target.getMaxHealth()); target.clearFire(); }
    public void feed(ServerPlayer target) { target.getFoodData().setFoodLevel(20); target.getFoodData().setSaturation(5.0F); }
    public void setGameMode(ServerPlayer target, String mode) {
        GameType gameType = switch (mode.toLowerCase(java.util.Locale.ROOT)) { case "survival", "s" -> GameType.SURVIVAL; case "creative", "c" -> GameType.CREATIVE; case "adventure", "a" -> GameType.ADVENTURE; case "spectator", "sp" -> GameType.SPECTATOR; default -> throw new IllegalArgumentException("Game mode must be survival, creative, adventure, or spectator."); };
        target.setGameMode(gameType);
    }
    private void open(ServerPlayer viewer, Container target, int rows, String title) {
        viewer.openMenu(new SimpleMenuProvider((id, inventory, player) -> new ReadOnlyContainerMenu(id, inventory, target, rows), Component.literal(title)));
    }
}
